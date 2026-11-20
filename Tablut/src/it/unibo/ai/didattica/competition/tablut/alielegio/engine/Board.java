package it.unibo.ai.didattica.competition.tablut.alielegio.engine;

import it.unibo.ai.didattica.competition.tablut.domain.State;
import it.unibo.ai.didattica.competition.tablut.domain.StateTablut;

/** Mutable compact position. The 81-cell byte board is copied only at API boundaries. */
public final class Board {
    public final byte[] cells = new byte[81];
    public byte turn;
    public byte result;
    public int whiteSoldiers, blackSoldiers;
    public long hash;
    public long historyHash;
    public long[] history = new long[256];
    public int historySize;
    public int repetitionLimit;
    public int cacheLimit;
    public int noCaptureMoves;
    public Board() { }
    public Board(Board b) {
        System.arraycopy(b.cells,0,cells,0,81); turn=b.turn; result=b.result; whiteSoldiers=b.whiteSoldiers;blackSoldiers=b.blackSoldiers;hash=b.hash;historyHash=b.historyHash;
        history=b.history.clone(); historySize=b.historySize; repetitionLimit=b.repetitionLimit;
        cacheLimit=b.cacheLimit; noCaptureMoves=b.noCaptureMoves;
    }
    public void recomputeHash() {
        long h=turn==Rules.BLACK_TURN?Zobrist.BLACK_TO_MOVE:0;
        for(int s=0;s<81;s++) h ^= Zobrist.PIECE[s][cells[s]];
        hash=h;
    }
    public boolean terminal() { return result >= Rules.WHITE_WIN; }
    public void pushHistory() {
        if(historySize==history.length) { long[] n=new long[history.length*2]; System.arraycopy(history,0,n,0,history.length); history=n; }
        if(cacheLimit>=0 && historySize>cacheLimit) {
            System.arraycopy(history,1,history,0,historySize-1); historySize--;historyHash=0;
            for(int i=0;i<historySize;i++)historyHash=foldHistory(historyHash,history[i]);
        }
        history[historySize++]=hash;
        historyHash=foldHistory(historyHash,hash);
    }
    private static long foldHistory(long h,long value) { return Long.rotateLeft(h,9)^(value*0x9e3779b97f4a7c15L); }
    public int repetitions(long h) { int n=0; for(int i=0;i<historySize;i++) if(history[i]==h)n++; return n; }
    public static Board initial() { Board b=fromState(new StateTablut()); b.turn=Rules.WHITE_TURN; b.recomputeHash(); b.historySize=0; return b; }
    public static Board fromState(State s) { return fromState(s,false); }
    private static Board fromState(State s, boolean serverInitial) {
        Board b=new Board();
        for(int r=0;r<9;r++) for(int c=0;c<9;c++) {
            State.Pawn p=s.getPawn(r,c); byte v=Rules.EMPTY;
            if(p==State.Pawn.WHITE)v=Rules.WHITE; else if(p==State.Pawn.BLACK)v=Rules.BLACK;
            else if(p==State.Pawn.KING)v=Rules.KING; else if(p==State.Pawn.THRONE)v=Rules.THRONE;
            b.cells[r*9+c]=v;
            if(v==Rules.WHITE)b.whiteSoldiers++;else if(v==Rules.BLACK)b.blackSoldiers++;
        }
        b.turn=s.getTurn()==State.Turn.BLACK?Rules.BLACK_TURN:s.getTurn()==State.Turn.WHITE?Rules.WHITE_TURN:s.getTurn()==State.Turn.WHITEWIN?Rules.WHITE_WIN:s.getTurn()==State.Turn.BLACKWIN?Rules.BLACK_WIN:Rules.DRAW;
        b.result=b.turn;
        b.repetitionLimit=0; b.cacheLimit=-1;
        b.recomputeHash(); return b;
    }
    public StateTablut toState() {
        StateTablut s=new StateTablut();
        for(int r=0;r<9;r++) for(int c=0;c<9;c++) {
            byte p=cells[r*9+c]; State.Pawn q=p==Rules.WHITE?State.Pawn.WHITE:p==Rules.BLACK?State.Pawn.BLACK:p==Rules.KING?State.Pawn.KING:p==Rules.THRONE?State.Pawn.THRONE:State.Pawn.EMPTY;
            s.getBoard()[r][c]=q;
        }
        State.Turn t=turn==Rules.BLACK_TURN?State.Turn.BLACK:turn==Rules.WHITE_TURN?State.Turn.WHITE:turn==Rules.WHITE_WIN?State.Turn.WHITEWIN:turn==Rules.BLACK_WIN?State.Turn.BLACKWIN:State.Turn.DRAW;
        s.setTurn(t); return s;
    }
    public String printable() { return toState().toString(); }
}
