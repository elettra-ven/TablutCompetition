package it.unibo.ai.didattica.competition.tablut.alielegio.test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import it.unibo.ai.didattica.competition.tablut.alielegio.engine.*;
import it.unibo.ai.didattica.competition.tablut.domain.*;
import it.unibo.ai.didattica.competition.tablut.domain.State.Turn;

public final class DifferentialTest {
    private static long positions, legalMoves, candidatePairs;
    private static final Random RANDOM=new Random(20261120L);
    private DifferentialTest() { }
    public static void main(String[] args) throws Exception {
        int target=args.length==0?100000:Integer.parseInt(args[0]);
        PrintStream original=System.out;
        System.setOut(new PrintStream(new OutputStream(){public void write(int b){}},true,"US-ASCII"));
        Logger.getLogger("GameLog").setLevel(Level.OFF);
        File logDir=new File(System.getProperty("java.io.tmpdir"),"alielegio-phase1-oracle");
        int games=0;
        try {
            runEdgeCases(logDir);
            checkRepetition(logDir);
            while(positions<target) { runGame(logDir); games++; }
        } finally { System.setOut(original); }
        System.out.println("DifferentialTest PASS positions="+positions+" legalMoves="+legalMoves+" candidates="+candidatePairs+" games="+games+" seed=20261120 mismatches=0");
    }
    private static void checkRepetition(File logDir) throws Exception {
        StateTablut state=emptyState(Turn.WHITE);kingAt(state,4,4);put(state,1,0,State.Pawn.WHITE);put(state,7,8,State.Pawn.BLACK);
        GameAshtonTablut oracle=new GameAshtonTablut(state.clone(),0,-1,logDir.getAbsolutePath(),"repeat","test");
        Logger.getLogger("GameLog").setLevel(Level.OFF);
        Board board=Board.fromState(state);board.repetitionLimit=0;board.cacheLimit=-1;
        int[][] sequence={{1,0,2,0},{7,8,6,8},{2,0,1,0},{6,8,7,8},{1,0,2,0}};
        for(int i=0;i<sequence.length;i++) {
            int[] q=sequence[i];int move=Move.of(Rules.index(q[0],q[1]),Rules.index(q[2],q[3]));
            Action action=Converter.toAction(move,board.turn);state=(StateTablut)oracle.checkMove(state,action);
            MoveGen.make(board,move,new MoveGen.Undo());compareState(state,board,"repetition ply "+(i+1));
        }
        if(state.getTurn()!=Turn.DRAW)fail(state,"server oracle did not draw on first repeated state");
        positions++;
    }
    private static void runEdgeCases(File logDir) throws Exception {
        StateTablut s=emptyState(Turn.BLACK);
        kingAt(s,4,4);put(s,3,4,State.Pawn.BLACK);put(s,4,3,State.Pawn.BLACK);put(s,5,4,State.Pawn.BLACK);put(s,4,5,State.Pawn.BLACK);
        checkEdge(s,logDir,"king on throne");
        s=emptyState(Turn.BLACK);kingAt(s,3,4);put(s,2,4,State.Pawn.BLACK);put(s,3,3,State.Pawn.BLACK);put(s,3,5,State.Pawn.BLACK);
        checkEdge(s,logDir,"king adjacent throne");
        s=emptyState(Turn.BLACK);kingAt(s,2,2);put(s,2,1,State.Pawn.BLACK);put(s,1,2,State.Pawn.BLACK);put(s,2,3,State.Pawn.BLACK);
        checkEdge(s,logDir,"king outside throne zone");
        s=emptyState(Turn.WHITE);put(s,1,4,State.Pawn.WHITE);put(s,3,4,State.Pawn.BLACK);
        checkEdge(s,logDir,"soldier adjacent to throne");
        s=emptyState(Turn.WHITE);put(s,4,3,State.Pawn.WHITE);put(s,4,1,State.Pawn.BLACK);
        checkEdge(s,logDir,"soldier adjacent to camp");
        s=emptyState(Turn.WHITE);put(s,1,3,State.Pawn.WHITE);put(s,2,2,State.Pawn.BLACK);put(s,2,4,State.Pawn.BLACK);put(s,2,1,State.Pawn.WHITE);put(s,2,5,State.Pawn.WHITE);
        checkEdge(s,logDir,"multiple soldier capture");
        s=emptyState(Turn.BLACK);put(s,0,3,State.Pawn.BLACK);put(s,2,1,State.Pawn.KING);
        checkEdge(s,logDir,"black camp movement");
        s=emptyState(Turn.BLACK);put(s,1,3,State.Pawn.BLACK);put(s,2,1,State.Pawn.KING);
        checkEdge(s,logDir,"black outside camp movement");
        s=emptyState(Turn.WHITE);kingAt(s,2,1);
        checkEdge(s,logDir,"king escape to corner");
    }
    private static StateTablut emptyState(Turn turn) {
        StateTablut s=new StateTablut();for(int r=0;r<9;r++)for(int c=0;c<9;c++)s.getBoard()[r][c]=State.Pawn.EMPTY;
        s.getBoard()[4][4]=State.Pawn.THRONE;s.getBoard()[6][6]=State.Pawn.KING;s.setTurn(turn);return s;
    }
    private static void put(StateTablut s,int r,int c,State.Pawn p) { s.getBoard()[r][c]=p; }
    private static void kingAt(StateTablut s,int r,int c) { for(int y=0;y<9;y++)for(int x=0;x<9;x++)if(s.getBoard()[y][x]==State.Pawn.KING)s.getBoard()[y][x]=State.Pawn.EMPTY;put(s,r,c,State.Pawn.KING); }
    private static void checkEdge(StateTablut s,File logDir,String name) throws Exception {
        GameAshtonTablut oracle=new GameAshtonTablut(s.clone(),999999,0,logDir.getAbsolutePath(),name,"test");
        Logger.getLogger("GameLog").setLevel(Level.OFF);
        Board board=Board.fromState(s);board.repetitionLimit=999999;comparePosition(s,board,oracle);positions++;
    }
    private static void runGame(File logDir) throws Exception {
        StateTablut state=new StateTablut(); state.setTurn(Turn.WHITE);
        GameAshtonTablut oracle=new GameAshtonTablut(state.clone(),999999,0,logDir.getAbsolutePath(),"test","test");
        Logger.getLogger("GameLog").setLevel(Level.OFF);
        Board board=Board.fromState(state); board.repetitionLimit=999999; board.cacheLimit=0;
        int plies=0;
        while(state.getTurn()==Turn.WHITE||state.getTurn()==Turn.BLACK) {
            comparePosition(state,board,oracle);
            if(++positions>=Long.MAX_VALUE) throw new IllegalStateException();
            int[] moves=new int[1024]; int n=MoveGen.generate(board,moves);
            if(n==0) break;
            int chosen=moves[RANDOM.nextInt(n)];
            Action action=Converter.toAction(chosen,board.turn);
            State next=oracle.checkMove(state.clone(),action);
            MoveGen.Undo undo=new MoveGen.Undo(); MoveGen.make(board,chosen,undo);
            compareState(next,board,"trajectory move "+action);
            state=(StateTablut)next;
            if(++plies>180) break;
        }
    }
    private static void comparePosition(StateTablut state,Board board,GameAshtonTablut oracle) throws Exception {
        compareState(state,board,"position");
        int[] moves=new int[1024];int n=MoveGen.generate(board,moves);Set<Integer> generated=new HashSet<Integer>();
        for(int i=0;i<n;i++)generated.add(moves[i]);
        if(n>0) {
            Board rewind=new Board(board);long oldHash=rewind.hash,oldHistoryHash=rewind.historyHash;int oldHistory=rewind.historySize;MoveGen.Undo undo=new MoveGen.Undo();
            MoveGen.make(rewind,moves[0],undo);MoveGen.unmake(rewind,undo);
            compareState(state,rewind,"make/unmake");
            if(rewind.hash!=oldHash||rewind.historyHash!=oldHistoryHash||rewind.historySize!=oldHistory)fail(state,"make/unmake metadata mismatch");
        }
        Set<Integer> accepted=new HashSet<Integer>();
        int side=board.turn==Rules.WHITE_TURN?Rules.WHITE:Rules.BLACK;
        for(int from=0;from<81;from++) {
            byte p=board.cells[from];if(p!=side&&!(side==Rules.WHITE&&p==Rules.KING))continue;
            for(int to=0;to<81;to++) {
                if(from==to||(Rules.row(from)!=Rules.row(to)&&Rules.col(from)!=Rules.col(to)))continue;
                candidatePairs++;
                int move=Move.of(from,to); Action a=Converter.toAction(move,board.turn);
                State result=null;boolean ok=false;
                try { result=oracle.checkMove(state.clone(),a);ok=true; } catch(Exception expected) { }
                if(ok) {
                    accepted.add(move);legalMoves++;
                    Board actual=Board.fromState(result);
                    Board predicted=new Board(board);MoveGen.Undo u=new MoveGen.Undo();MoveGen.make(predicted,move,u);
                    compareState(result,predicted,"result of "+a);
                    if(!generated.contains(move))fail(state,"oracle accepts absent move "+a);
                } else if(generated.contains(move)) fail(state,"engine accepts oracle-rejected move "+a);
            }
        }
        if(!accepted.equals(generated))fail(state,"legal move set differs: oracle="+accepted.size()+" engine="+generated.size());
    }
    private static void compareState(State oracle,Board engine,String where) {
        long expectedHash=engine.hash; engine.recomputeHash(); if(engine.hash!=expectedHash)fail(oracle,where+" Zobrist hash mismatch");
        for(int r=0;r<9;r++)for(int c=0;c<9;c++) {
            byte a=engine.cells[r*9+c];State.Pawn p=oracle.getPawn(r,c);
            int b=p==State.Pawn.WHITE?Rules.WHITE:p==State.Pawn.BLACK?Rules.BLACK:p==State.Pawn.KING?Rules.KING:p==State.Pawn.THRONE?Rules.THRONE:Rules.EMPTY;
            if(a!=b)fail(oracle,where+" board mismatch at "+oracle.getBox(r,c)+" engine="+a+" oracle="+p);
        }
        if(engine.whiteSoldiers!=oracle.getNumberOf(State.Pawn.WHITE)||engine.blackSoldiers!=oracle.getNumberOf(State.Pawn.BLACK))fail(oracle,where+" material counters mismatch");
        byte t=oracle.getTurn()==Turn.WHITE?Rules.WHITE_TURN:oracle.getTurn()==Turn.BLACK?Rules.BLACK_TURN:oracle.getTurn()==Turn.WHITEWIN?Rules.WHITE_WIN:oracle.getTurn()==Turn.BLACKWIN?Rules.BLACK_WIN:Rules.DRAW;
        if(engine.turn!=t)fail(oracle,where+" turn mismatch engine="+engine.turn+" oracle="+oracle.getTurn());
    }
    private static void fail(State s,String message) { throw new AssertionError(message+"\n"+s.toString()); }
}
