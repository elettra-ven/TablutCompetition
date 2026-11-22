package it.unibo.ai.didattica.competition.tablut.alielegio;

import java.io.IOException;
import it.unibo.ai.didattica.competition.tablut.alielegio.engine.Board;
import it.unibo.ai.didattica.competition.tablut.alielegio.engine.Converter;
import it.unibo.ai.didattica.competition.tablut.alielegio.engine.Move;
import it.unibo.ai.didattica.competition.tablut.alielegio.engine.MoveGen;
import it.unibo.ai.didattica.competition.tablut.alielegio.engine.Rules;
import it.unibo.ai.didattica.competition.tablut.alielegio.search.TranspositionTable;
import it.unibo.ai.didattica.competition.tablut.domain.Action;
import it.unibo.ai.didattica.competition.tablut.domain.State;

/**
 * Iterative-deepening minimax (negmax) search with alpha-beta pruning.
 */
public final class Search {
    private static final int MAX_PLY=96, MAX_MOVES=1024;
    private static final int MATE=1000000, INF=1100000, MATE_THRESHOLD=900000;
    private final Evaluator evaluator;
    private final TranspositionTable table;
    private final int[][] moveBuffers=new int[MAX_PLY][MAX_MOVES];
    private final int[][] moveScores=new int[MAX_PLY][MAX_MOVES];
    private final MoveGen.Undo[] undos=new MoveGen.Undo[MAX_PLY];
    private final int[][] killers=new int[MAX_PLY][2];
    private final int[][][] history=new int[2][81][81];
    private long deadlineNanos, startedNanos, budgetNanos;
    private long nodeLimit=Long.MAX_VALUE;
    private boolean useClock=true;
    private long previousIterationNodes, iterationNodes, iterationStartNodes;
    private boolean useKingThreatExtension=true;
    public long nodes;
    public int completedDepth;
    public long elapsedMillis;
    public long nodesPerSecond;

    private static final class TimeUp extends RuntimeException {
        private static final long serialVersionUID=1L;
        TimeUp() { super(null,null,false,false); }
    }

    public Search(Evaluator evaluator) { this(evaluator,new TranspositionTable()); }
    public Search(Evaluator evaluator,TranspositionTable table) {
        this.evaluator=evaluator;this.table=table;
        for(int i=0;i<undos.length;i++)undos[i]=new MoveGen.Undo();
    }
    public void setKingThreatExtension(boolean enabled) { useKingThreatExtension=enabled; }

    /** Budget leaves max(1.5 seconds, 12 percent) for VM and protocol overhead. */
    public static long budgetForTimeoutSeconds(int timeoutSeconds) {
        long total=Math.max(1L,timeoutSeconds)*1000L;
        long reserve=Math.max(1500L,(long)(total*0.12));
        return Math.max(1L,total-reserve);
    }

    public Action bestMove(State state,long budgetMs) throws IOException {
        Board board=Board.fromState(state);
        int move=bestMove(board,budgetMs);
        if(move<0)return null;
        return Converter.toAction(move,board.turn);
    }

    // Returns the move or -1 when the position has no legal move.
    public int bestMove(Board input,long budgetMs) {
        return bestMoveInternal(input,budgetMs,Long.MAX_VALUE,true);
    }

    public int bestMoveNodes(Board input,long budgetLimit) {
        return bestMoveInternal(input,Long.MAX_VALUE,Math.max(1,budgetLimit),false);
    }
    private int bestMoveInternal(Board input,long budgetMs,long limit,boolean clock) {
        Board root=new Board(input);
        nodeLimit=limit;useClock=clock;
        nodes=0;completedDepth=0;elapsedMillis=0;nodesPerSecond=0;previousIterationNodes=0;iterationNodes=0;
        startedNanos=System.nanoTime();budgetNanos=clock?Math.max(1,budgetMs)*1000000L:Long.MAX_VALUE;
        long searchSafety=clock?Math.max(1000000L,budgetNanos/50L):0L;
        deadlineNanos=clock?startedNanos+Math.max(1L,budgetNanos-searchSafety):Long.MAX_VALUE;
        if(root.terminal())return -1;
        if(root.historySize==0||root.history[root.historySize-1]!=root.hash)root.pushHistory();
        table.newSearch();
        clearSearchHeuristics();
        
        int[] rootMoves=moveBuffers[0];int count=MoveGen.generate(root,rootMoves);
        if(count==0)return -1;
        int bestMove=rootMoves[0];
        for(int i=0;i<count;i++) {
            int m=rootMoves[i];
            if(root.cells[Move.from(m)]==Rules.KING&&Rules.edge(Move.to(m))){bestMove=m;updateStats();return bestMove;}
        }
        try {
            int priorBest=0;
            for(int depth=1;depth<MAX_PLY;depth++) {
                if(completedDepth>0 && !nextDepthLikelyFits())break;
                iterationStartNodes=nodes;
                long key=tableKey(root);int slot=table.slot(key);int ttMove=table.matches(slot,key)?table.move(slot):priorBest;
                orderMoves(root,rootMoves,count,0,ttMove);
                int alpha=-INF,beta=INF,iterationBest=0,iterationScore=-INF;
                for(int i=0;i<count;i++) {
                    checkTime();
                    int move=rootMoves[i];MoveGen.make(root,move,undos[0]);
                    int score=-negamax(root,depth-1,-beta,-alpha,1,2);
                    MoveGen.unmake(root,undos[0]);
                    if(score>iterationScore){iterationScore=score;iterationBest=move;}
                    if(score>alpha)alpha=score;
                }
                checkTime();
                if(iterationBest!=0) {
                    bestMove=iterationBest;priorBest=iterationBest;completedDepth=depth;
                    previousIterationNodes=iterationNodes;iterationNodes=nodes-iterationStartNodes;
                    table.store(key,depth,scoreToTable(iterationScore,0),TranspositionTable.EXACT,iterationBest);
                    if(iterationScore>=MATE_THRESHOLD)break;
                }
            }
        } catch(TimeUp timeout) {
            
        }
        updateStats();return bestMove;
    }

    private int negamax(Board b,int depth,int alpha,int beta,int ply,int extensions) {
        nodes++;if(nodes>nodeLimit)throw new TimeUp();if((nodes&1023L)==0)checkTime();
        if(b.terminal())return b.result==Rules.DRAW?0:-MATE+ply;
        if(ply>=MAX_PLY-1)return evaluate(b);
        if(depth<=0) {
            if(useKingThreatExtension&&extensions>0&&kingCanReachEdge(b)) { depth=1;extensions--; }
            else return evaluate(b);
        }
        int alphaOriginal=alpha;
        long key=tableKey(b);int slot=table.slot(key);int ttMove=0;
        if(table.matches(slot,key)) {
            ttMove=table.move(slot);
            if(table.depth(slot)>=depth) {
                int score=scoreFromTable(table.score(slot),ply);byte bound=table.bound(slot);
                if(bound==TranspositionTable.EXACT)return score;
                if(bound==TranspositionTable.LOWER&&score>=beta)return score;
                if(bound==TranspositionTable.UPPER&&score<=alpha)return score;
            }
        }
        int[] moves=moveBuffers[ply];int count=MoveGen.generate(b,moves);
        if(count==0)return -MATE+ply;
        orderMoves(b,moves,count,ply,ttMove);
        int best=-INF,bestMove=0;
        for(int i=0;i<count;i++) {
            int move=moves[i];MoveGen.make(b,move,undos[ply]);
            int score=-negamax(b,depth-1,-beta,-alpha,ply+1,extensions);
            MoveGen.unmake(b,undos[ply]);
            if(score>best){best=score;bestMove=move;}
            if(score>alpha)alpha=score;
            if(alpha>=beta) {
                if(!isCapture(b,move)&&ply<MAX_PLY)recordQuietCutoff(b.turn,move,ply,depth);
                break;
            }
        }
        byte bound=best<=alphaOriginal?TranspositionTable.UPPER:best>=beta?TranspositionTable.LOWER:TranspositionTable.EXACT;
        table.store(key,depth,scoreToTable(best,ply),bound,bestMove);
        return best;
    }

    private void orderMoves(Board b,int[] moves,int count,int ply,int ttMove) {
        int[] scores=moveScores[ply];int side=b.turn==Rules.WHITE_TURN?0:1;
        int before=b.whiteSoldiers+b.blackSoldiers;
        for(int i=0;i<count;i++) {
            if((i&31)==0)checkTime();
            int move=moves[i],score=0;
            if(move==ttMove&&ttMove!=0)score+=2000000;
            int from=Move.from(move),to=Move.to(move);
            if(b.cells[from]==Rules.KING&&Rules.edge(to))score+=1900000;
            MoveGen.make(b,move,undos[ply]);
            int lost=before-b.whiteSoldiers-b.blackSoldiers;
            if(b.result==Rules.BLACK_WIN)score+=1800000;
            if(lost>0)score+=120000+lost*5000;
            if(b.cells[to]==Rules.KING) {
                int oldDist=edgeDistance(from),newDist=edgeDistance(to);
                score+=(oldDist-newDist)*400;
            }
            MoveGen.unmake(b,undos[ply]);
            if(ply<MAX_PLY) {
                if(move==killers[ply][0])score+=80000;
                else if(move==killers[ply][1])score+=60000;
            }
            score+=Math.min(50000,history[side][from][to]);
            scores[i]=score;
        }
        for(int i=1;i<count;i++) {
            int move=moves[i],score=scores[i],j=i-1;
            while(j>=0&&scores[j]<score){moves[j+1]=moves[j];scores[j+1]=scores[j];j--;}
            moves[j+1]=move;scores[j+1]=score;
        }
    }

    private boolean isCapture(Board b,int move) {
        int before=b.whiteSoldiers+b.blackSoldiers;
        MoveGen.make(b,move,undos[MAX_PLY-1]);
        boolean capture=before>b.whiteSoldiers+b.blackSoldiers||b.result==Rules.BLACK_WIN;
        MoveGen.unmake(b,undos[MAX_PLY-1]);return capture;
    }
    private void recordQuietCutoff(byte turn,int move,int ply,int depth) {
        if(killers[ply][0]!=move){killers[ply][1]=killers[ply][0];killers[ply][0]=move;}
        int side=turn==Rules.WHITE_TURN?0:1,from=Move.from(move),to=Move.to(move);
        history[side][from][to]=Math.min(50000,history[side][from][to]+depth*depth);
    }

    private int evaluate(Board b) {
        double white=evaluator.eval(b);
        int score=(int)Math.max(-500000,Math.min(500000,white));
        return b.turn==Rules.WHITE_TURN?score:-score;
    }
    private boolean kingCanReachEdge(Board b) {
        int king=-1;for(int i=0;i<81;i++)if(b.cells[i]==Rules.KING){king=i;break;}
        if(king<0)return false;
        int r=Rules.row(king),c=Rules.col(king);
        for(int d=0;d<4;d++) {
            int rr=r+(d==0?-1:d==1?1:0),cc=c+(d==2?-1:d==3?1:0);
            while(Rules.inside(rr,cc)) {
                int to=Rules.index(rr,cc);
                if(b.cells[to]!=Rules.EMPTY)break;
                if(Rules.camp(to)&&!Rules.camp(king))break;
                if(!Rules.camp(to)||(Math.abs(rr-r)<=5&&Math.abs(cc-c)<=5))if(Rules.edge(to))return true;
                rr+=(d==0?-1:d==1?1:0);cc+=(d==2?-1:d==3?1:0);
            }
        }
        return false;
    }
    private static int edgeDistance(int square) {
        int r=Rules.row(square),c=Rules.col(square);
        return Math.min(Math.min(r,8-r),Math.min(c,8-c));
    }
    private boolean nextDepthLikelyFits() {
        if(!useClock)return true;
        long elapsed=System.nanoTime()-startedNanos;
        if(elapsed>=budgetNanos)return false;
        double growth=previousIterationNodes<=0?3.0:(double)Math.max(1,iterationNodes)/Math.max(1,previousIterationNodes);
        growth=Math.max(1.4,Math.min(8.0,growth));
        long predicted=(long)Math.max(1,elapsed*(growth-1.0));
        return elapsed+predicted*1.20<budgetNanos;
    }
    private void checkTime() { if(useClock&&System.nanoTime()>=deadlineNanos)throw new TimeUp(); }
    private void updateStats() {
        long now=System.nanoTime();elapsedMillis=(now-startedNanos)/1000000L;
        nodesPerSecond=elapsedMillis<=0?nodes:nodes*1000L/elapsedMillis;
    }
    private void clearSearchHeuristics() {
        for(int i=0;i<killers.length;i++){killers[i][0]=0;killers[i][1]=0;}
        for(int s=0;s<2;s++)for(int f=0;f<81;f++)for(int t=0;t<81;t++)history[s][f][t]=0;
    }
    private static int scoreToTable(int score,int ply) {
        if(score>MATE_THRESHOLD)return score+ply;
        if(score< -MATE_THRESHOLD)return score-ply;
        return score;
    }
    private static int scoreFromTable(int score,int ply) {
        if(score>MATE_THRESHOLD)return score-ply;
        if(score< -MATE_THRESHOLD)return score+ply;
        return score;
    }
    private static long tableKey(Board board) { return board.hash^Long.rotateLeft(board.historyHash,23); }
}
