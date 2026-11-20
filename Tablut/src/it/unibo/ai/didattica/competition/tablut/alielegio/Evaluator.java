package it.unibo.ai.didattica.competition.tablut.alielegio;

import it.unibo.ai.didattica.competition.tablut.alielegio.engine.Board;
import it.unibo.ai.didattica.competition.tablut.alielegio.engine.Rules;
import it.unibo.ai.didattica.competition.tablut.domain.State;

/** Allocation-free Ashton evaluation with separate white and black feature weights. */
public final class Evaluator {
    private static final int[] DR={-1,1,0,0},DC={0,0,-1,1};
    private static final int LOST_KING=-500000;
    private final int[] whiteScratch=new int[Weights.WHITE_COUNT];
    private final int[] blackScratch=new int[Weights.BLACK_COUNT];
    private final boolean[] whiteReach=new boolean[81],blackReach=new boolean[81],firstKingMoves=new boolean[81];
    private final int[] queue=new int[81],distance=new int[81];
    public final Weights weights;

    public static final class FeatureVector {
        public final int[] white;
        public final int[] black;
        private FeatureVector(int[] w,int[] b){white=w;black=b;}
    }

    public Evaluator(){this(Weights.defaults());}
    public Evaluator(Weights weights){if(weights==null)throw new IllegalArgumentException("weights required");this.weights=weights.copy();}

    /** Returns a white-positive, zero-sum score. Scratch arrays are private to this evaluator. */
    public synchronized double eval(Board board) {
        if(board.result==Rules.DRAW)return 0.0;
        if(board.result==Rules.WHITE_WIN)return 500000.0;
        if(board.result==Rules.BLACK_WIN)return LOST_KING;
        int king=findKing(board);if(king<0)return LOST_KING;
        collect(board,king,whiteScratch,blackScratch);
        double score=0;
        for(int i=0;i<whiteScratch.length;i++)score+=weights.white[i]*whiteScratch[i];
        for(int i=0;i<blackScratch.length;i++)score-=weights.black[i]*blackScratch[i];
        return score;
    }
    public double eval(State state){return eval(Board.fromState(state));}

    /** Allocating feature snapshot intended for demos and tests, not search. */
    public synchronized FeatureVector features(Board board) {
        int[] w=new int[Weights.WHITE_COUNT],b=new int[Weights.BLACK_COUNT];int king=findKing(board);
        if(king>=0)collect(board,king,w,b);
        return new FeatureVector(w,b);
    }
    public FeatureVector features(State state){return features(Board.fromState(state));}

    private void collect(Board b,int king,int[] w,int[] z) {
        for(int i=0;i<w.length;i++)w[i]=0;for(int i=0;i<z.length;i++)z[i]=0;
        int blackInCamps=0;
        for(int sq=0;sq<81;sq++)if(b.cells[sq]==Rules.BLACK&&Rules.camp(sq))blackInCamps++;
        int whiteMobility=countMoves(b,Rules.WHITE,whiteReach);
        int blackMobility=countMoves(b,Rules.BLACK,blackReach);
        int distanceToEscape=kingDistance(b,king);
        int openLines=countOpenLines(b,king);
        int twoMoveRoutes=countTwoMoveRoutes(b,king);
        int adjacentBlack=0;
        for(int d=0;d<4;d++){int r=Rules.row(king)+DR[d],c=Rules.col(king)+DC[d];if(Rules.inside(r,c)&&b.cells[Rules.index(r,c)]==Rules.BLACK)adjacentBlack++;}
        int captureNeeds=blackPiecesNeeded(b,king,blackReach);
        int blackHanging=countHanging(b,Rules.BLACK,whiteReach);
        int whiteHanging=countHanging(b,Rules.WHITE,blackReach);
        int cordon=countCordon(b,king);
        int kingMobility=countKingMoves(b,king);

        w[0]=b.whiteSoldiers;w[1]=1;w[2]=Math.max(0,10-distanceToEscape);w[3]=openLines;
        w[4]=openLines>=2?1:0;w[5]=twoMoveRoutes;w[6]=kingMobility;w[7]=captureNeeds;
        w[8]=blackHanging;w[9]=whiteMobility;w[10]=blackInCamps;
        z[0]=b.blackSoldiers;z[1]=adjacentBlack;z[2]=cordon;z[3]=whiteHanging;
        z[4]=blackMobility;z[5]=b.blackSoldiers-blackInCamps;
    }

    private int countMoves(Board b,byte side,boolean[] reachable) {
        for(int i=0;i<81;i++)reachable[i]=false;
        int total=0;
        for(int from=0;from<81;from++) {
            byte p=b.cells[from];if(p!=side&&!(side==Rules.WHITE&&p==Rules.KING))continue;
            int r=Rules.row(from),c=Rules.col(from);
            for(int d=0;d<4;d++) {
                int rr=r+DR[d],cc=c+DC[d];
                while(Rules.inside(rr,cc)) {
                    int to=Rules.index(rr,cc);if(b.cells[to]!=Rules.EMPTY)break;
                    if(Rules.camp(to)&&!Rules.camp(from))break;
                    if(!Rules.camp(to)||(Math.abs(rr-r)<=5&&Math.abs(cc-c)<=5)){reachable[to]=true;total++;}
                    rr+=DR[d];cc+=DC[d];
                }
            }
        }
        return total;
    }

    private int countKingMoves(Board b,int king) {
        int total=0,r=Rules.row(king),c=Rules.col(king);
        for(int d=0;d<4;d++) {
            int rr=r+DR[d],cc=c+DC[d];
            while(Rules.inside(rr,cc)) {
                int to=Rules.index(rr,cc);if(b.cells[to]!=Rules.EMPTY)break;
                if(Rules.camp(to)&&!Rules.camp(king))break;
                if(!Rules.camp(to)||(Math.abs(rr-r)<=5&&Math.abs(cc-c)<=5))total++;
                rr+=DR[d];cc+=DC[d];
            }
        }
        return total;
    }

    private int countOpenLines(Board b,int king) {
        int open=0,r=Rules.row(king),c=Rules.col(king);
        for(int d=0;d<4;d++) {
            int rr=r+DR[d],cc=c+DC[d];
            while(Rules.inside(rr,cc)) {
                int to=Rules.index(rr,cc);if(blockedAfterKingMove(b,to,king))break;
                if(Rules.camp(to)&&!Rules.camp(king))break;
                if(Rules.camp(to)&&(Math.abs(rr-r)>5||Math.abs(cc-c)>5)){rr+=DR[d];cc+=DC[d];continue;}
                if(Rules.edge(to)){open++;break;}
                rr+=DR[d];cc+=DC[d];
            }
        }
        return open;
    }

    private int countTwoMoveRoutes(Board b,int king) {
        for(int i=0;i<81;i++)firstKingMoves[i]=false;
        int r=Rules.row(king),c=Rules.col(king);
        for(int d=0;d<4;d++) {
            int rr=r+DR[d],cc=c+DC[d];
            while(Rules.inside(rr,cc)) {
                int to=Rules.index(rr,cc);if(blockedAfterKingMove(b,to,king))break;
                if(Rules.camp(to)&&!Rules.camp(king))break;
                if(!Rules.camp(to)||(Math.abs(rr-r)<=5&&Math.abs(cc-c)<=5))firstKingMoves[to]=true;
                rr+=DR[d];cc+=DC[d];
            }
        }
        int routes=0;
        for(int first=0;first<81;first++)if(firstKingMoves[first]&&!Rules.edge(first)) {
            int fr=Rules.row(first),fc=Rules.col(first);boolean found=false;
            for(int d=0;d<4&&!found;d++) {
                int rr=fr+DR[d],cc=fc+DC[d];
                while(Rules.inside(rr,cc)) {
                    int to=Rules.index(rr,cc);if(blockedAfterKingMove(b,to,king))break;
                    if(Rules.camp(to)&&!Rules.camp(first))break;
                    if(Rules.camp(to)&&(Math.abs(rr-fr)>5||Math.abs(cc-fc)>5)){rr+=DR[d];cc+=DC[d];continue;}
                    if(Rules.edge(to)){found=true;break;}
                    rr+=DR[d];cc+=DC[d];
                }
            }
            if(found)routes++;
        }
        return routes;
    }

    private int kingDistance(Board b,int king) {
        for(int i=0;i<81;i++)distance[i]=-1;
        int head=0,tail=0;queue[tail++]=king;distance[king]=0;
        while(head<tail) {
            int from=queue[head++],r=Rules.row(from),c=Rules.col(from);
            for(int d=0;d<4;d++) {
                int rr=r+DR[d],cc=c+DC[d];
                while(Rules.inside(rr,cc)) {
                    int to=Rules.index(rr,cc);if(blockedAfterKingMove(b,to,king))break;
                    if(Rules.camp(to)&&!Rules.camp(from))break;
                    if(Rules.camp(to)&&(Math.abs(rr-r)>5||Math.abs(cc-c)>5)){rr+=DR[d];cc+=DC[d];continue;}
                    if(distance[to]<0) {
                        distance[to]=distance[from]+1;
                        if(Rules.edge(to))return distance[to];
                        queue[tail++]=to;
                    }
                    rr+=DR[d];cc+=DC[d];
                }
            }
        }
        return 10;
    }

    private boolean blockedAfterKingMove(Board b,int square,int kingStart) {
        if(square==kingStart&&kingStart!=40)return false;
        return b.cells[square]!=Rules.EMPTY || (square==40&&kingStart==40);
    }

    private int countHanging(Board b,byte targetSide,boolean[] attackerReach) {
        int count=0;byte attacker=targetSide==Rules.WHITE?Rules.BLACK:Rules.WHITE;
        for(int target=0;target<81;target++)if(b.cells[target]==targetSide) {
            int tr=Rules.row(target),tc=Rules.col(target);boolean threatened=false;
            for(int d=0;d<4&&!threatened;d++) {
                int sr=tr+DR[d],sc=tc+DC[d],lr=tr-DR[d],lc=tc-DC[d];
                if(!Rules.inside(sr,sc)||!Rules.inside(lr,lc))continue;
                int support=Rules.index(sr,sc),landing=Rules.index(lr,lc);
                if(b.cells[landing]!=Rules.EMPTY||!attackerReach[landing])continue;
                if(hostileForCapture(b,attacker,support))threatened=true;
            }
            if(threatened)count++;
        }
        return count;
    }

    private boolean hostileForCapture(Board b,byte attacker,int square) {
        if(attacker==Rules.WHITE)return b.cells[square]==Rules.WHITE||b.cells[square]==Rules.KING||b.cells[square]==Rules.THRONE||Rules.whiteHostileCamp(square);
        return b.cells[square]==Rules.BLACK||b.cells[square]==Rules.THRONE||Rules.camp(square)||square==40;
    }

    private int blackPiecesNeeded(Board b,int king,boolean[] blackMoves) {
        int r=Rules.row(king),c=Rules.col(king),best=4;
        for(int d=0;d<4;d++) {
            int ar=r+(d==2?-1:d==3?1:0),ac=c+(d==0?1:d==1?-1:0);
            if(!Rules.inside(ar,ac))continue;
            int attack=Rules.index(ar,ac);if(b.cells[attack]!=Rules.EMPTY||!blackMoves[attack])continue;
            int missing;
            if(king==40) {
                if(d==0)missing=countMissing(b,3,4,4,3,5,4);
                else if(d==1)missing=countMissing(b,3,4,4,5,5,4);
                else if(d==2)missing=countMissing(b,5,4,4,5,4,3);
                else missing=countMissing(b,3,4,4,5,4,3);
            } else if(r==3&&c==4) {
                if(d==0)missing=countMissing(b,2,4,3,3,-1,-1);
                else if(d==1)missing=countMissing(b,2,4,3,5,-1,-1);
                else if(d==2)missing=countMissing(b,3,3,3,5,-1,-1);
                else continue;
            } else if(r==5&&c==4) {
                if(d==0)missing=countMissing(b,6,4,5,3,-1,-1);
                else if(d==1)missing=countMissing(b,5,5,6,4,-1,-1);
                else if(d==3)missing=countMissing(b,5,3,5,5,-1,-1);
                else continue;
            } else if(r==4&&c==3) {
                if(d==1)missing=countMissing(b,3,3,5,3,-1,-1);
                else if(d==2)missing=countMissing(b,4,2,5,3,-1,-1);
                else if(d==3)missing=countMissing(b,4,2,3,3,-1,-1);
                else continue;
            } else if(r==4&&c==5) {
                if(d==0)missing=countMissing(b,5,5,3,5,-1,-1);
                else if(d==2)missing=countMissing(b,4,6,5,5,-1,-1);
                else if(d==3)missing=countMissing(b,4,6,3,5,-1,-1);
                else continue;
            } else {
                int sr=r+(d==2?1:d==3?-1:0),sc=c+(d==0?-1:d==1?1:0);
                if(!Rules.inside(sr,sc))continue;
                int support=Rules.index(sr,sc);byte p=b.cells[support];
                if(p==Rules.BLACK||Rules.camp(support))missing=0;
                else if(p==Rules.EMPTY)missing=1;
                else continue;
            }
            if(missing<best)best=missing;
        }
        return best;
    }
    private static int countMissing(Board b,int r1,int c1,int r2,int c2,int r3,int c3) {
        int n=0;if(b.cells[Rules.index(r1,c1)]!=Rules.BLACK)n++;
        if(b.cells[Rules.index(r2,c2)]!=Rules.BLACK)n++;
        if(r3>=0&&b.cells[Rules.index(r3,c3)]!=Rules.BLACK)n++;
        return n;
    }

    private int countCordon(Board b,int king) {
        int r=Rules.row(king),c=Rules.col(king),count=0;
        for(int d=0;d<4;d++) {
            int rr=r+DR[d],cc=c+DC[d];
            while(Rules.inside(rr,cc)) {
                int sq=Rules.index(rr,cc);
                if(b.cells[sq]==Rules.BLACK){count++;break;}
                if(b.cells[sq]!=Rules.EMPTY||Rules.camp(sq))break;
                rr+=DR[d];cc+=DC[d];
            }
        }
        return count;
    }
    private static int findKing(Board b){for(int i=0;i<81;i++)if(b.cells[i]==Rules.KING)return i;return -1;}
}
