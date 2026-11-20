package it.unibo.ai.didattica.competition.tablut.alielegio.engine;

/** Allocation-free legal move generator */
public final class MoveGen {
    private static final int[] DR={-1,1,0,0}, DC={0,0,-1,1};
    private static final int[] KING_DR={0,0,1,-1}, KING_DC={-1,1,0,0};
    private MoveGen() { }
    public static int generate(Board b, int[] out) {
        if(b.terminal()) return 0;
        int n=0; byte side=b.turn==Rules.WHITE_TURN?Rules.WHITE:Rules.BLACK;
        for(int from=0;from<81;from++) {
            byte p=b.cells[from]; if(p!=side && !(side==Rules.WHITE&&p==Rules.KING)) continue;
            int r=Rules.row(from),c=Rules.col(from);
            for(int d=0;d<4;d++) {
                int rr=r+DR[d],cc=c+DC[d];
                while(Rules.inside(rr,cc)) {
                    int to=Rules.index(rr,cc);
                    if(b.cells[to]!=Rules.EMPTY || Rules.throne(to)) break;
                    if(Rules.camp(to) && !Rules.camp(from)) break;
                    if(!Rules.camp(to) || (Math.abs(rr-r)<=5 && Math.abs(cc-c)<=5)) out[n++]=Move.of(from,to);
                    rr+=DR[d]; cc+=DC[d];
                }
            }
        }
        return n;
    }
    public static boolean isLegal(Board b,int move) {
        int f=Move.from(move),t=Move.to(move);if(b.terminal()||f<0||f>=81||t<0||t>=81||f==t)return false;
        byte side=b.turn==Rules.WHITE_TURN?Rules.WHITE:Rules.BLACK;byte piece=b.cells[f];
        if(piece!=side&&!(side==Rules.WHITE&&piece==Rules.KING))return false;
        if(b.cells[t]!=Rules.EMPTY||Rules.throne(t))return false;
        int fr=Rules.row(f),fc=Rules.col(f),tr=Rules.row(t),tc=Rules.col(t);
        if(fr!=tr&&fc!=tc)return false;
        int dr=Integer.compare(tr,fr),dc=Integer.compare(tc,fc),r=fr+dr,c=fc+dc;
        while(true) {
            int sq=Rules.index(r,c);
            if(b.cells[sq]!=Rules.EMPTY||Rules.throne(sq))return false;
            if(Rules.camp(sq)&&!Rules.camp(f))return false;
            if(sq==t)return !Rules.camp(sq)||(Math.abs(tr-fr)<=5&&Math.abs(tc-fc)<=5);
            r+=dr;c+=dc;
        }
    }
    public static final class Undo {
        private final byte[] old=new byte[81];
        private long hash,historyHash; private byte turn,result; private int hist,noCapture,white,black;
    }
    public static void make(Board b,int move,Undo u) {
        System.arraycopy(b.cells,0,u.old,0,81); u.hash=b.hash;u.historyHash=b.historyHash;u.turn=b.turn;u.result=b.result;u.hist=b.historySize;u.noCapture=b.noCaptureMoves;u.white=b.whiteSoldiers;u.black=b.blackSoldiers;
        int f=Move.from(move),t=Move.to(move); byte p=b.cells[f];
        setCell(b,f,Rules.throne(f)?Rules.THRONE:Rules.EMPTY); setCell(b,t,p);
        b.turn=b.turn==Rules.WHITE_TURN?Rules.BLACK_TURN:Rules.WHITE_TURN;
        b.hash^=Zobrist.BLACK_TO_MOVE;
        int captured=resolve(b,t,u.turn);
        if(captured>0) { b.noCaptureMoves=0; b.historySize=0;b.historyHash=0; } else b.noCaptureMoves++;
        if(p==Rules.KING && Rules.edge(t)) b.turn=Rules.WHITE_WIN;
        if(findKing(b)<0) b.turn=Rules.BLACK_WIN;
        if(b.turn==Rules.WHITE_TURN || b.turn==Rules.BLACK_TURN) {
            if(b.repetitions(b.hash)>b.repetitionLimit) b.turn=Rules.DRAW;
            if(b.turn==Rules.DRAW && u.turn==Rules.WHITE_TURN) b.hash^=Zobrist.BLACK_TO_MOVE;
            b.pushHistory();
        } else if(u.turn==Rules.WHITE_TURN) b.hash^=Zobrist.BLACK_TO_MOVE;
        b.result=(byte)b.turn;
    }
    public static void unmake(Board b,Undo u) {
        System.arraycopy(u.old,0,b.cells,0,81);b.hash=u.hash;b.historyHash=u.historyHash;b.turn=u.turn;b.result=u.result;b.historySize=u.hist;b.noCaptureMoves=u.noCapture;b.whiteSoldiers=u.white;b.blackSoldiers=u.black;
    }
    private static void setCell(Board b,int sq,byte p) {
        byte old=b.cells[sq]; if(old==p)return;
        if(old==Rules.WHITE)b.whiteSoldiers--;else if(old==Rules.BLACK)b.blackSoldiers--;
        if(p==Rules.WHITE)b.whiteSoldiers++;else if(p==Rules.BLACK)b.blackSoldiers++;
        b.hash^=Zobrist.PIECE[sq][old]^Zobrist.PIECE[sq][p]; b.cells[sq]=p;
    }
    private static int findKing(Board b) { for(int i=0;i<81;i++)if(b.cells[i]==Rules.KING)return i; return -1; }
    private static int resolve(Board b,int to,byte mover) {
        return mover==Rules.WHITE_TURN?captureWhite(b,to):captureBlackAndKing(b,to);
    }
    private static boolean hostileWhite(Board b,int sq) { return b.cells[sq]==Rules.WHITE||b.cells[sq]==Rules.KING||b.cells[sq]==Rules.THRONE||Rules.whiteHostileCamp(sq); }
    private static boolean hostileBlack(Board b,int sq) { return b.cells[sq]==Rules.BLACK||b.cells[sq]==Rules.THRONE||Rules.camp(sq)||sq==40; }
    private static int captureWhite(Board b,int to) {
        int r=Rules.row(to),c=Rules.col(to);
        int captures=0;
        for(int d=0;d<4;d++) { int ar=r+DR[d],ac=c+DC[d],br=ar+DR[d],bc=ac+DC[d];
            if(Rules.inside(ar,ac)&&Rules.inside(br,bc)) {int a=Rules.index(ar,ac),z=Rules.index(br,bc);if(b.cells[a]==Rules.BLACK&&hostileWhite(b,z)){setCell(b,a,Rules.EMPTY);captures++;}}
        }
        if(b.cells[to]==Rules.KING && Rules.edge(to)) b.turn=Rules.WHITE_WIN;
        return captures;
    }
    private static int captureBlackAndKing(Board b,int to) {
        int r=Rules.row(to),c=Rules.col(to);
        int captures=0;
        for(int d=0;d<4;d++) { int ar=r+DR[d],ac=c+DC[d],br=ar+DR[d],bc=ac+DC[d];
            if(Rules.inside(ar,ac)&&Rules.inside(br,bc)) {int a=Rules.index(ar,ac),z=Rules.index(br,bc);if(b.cells[a]==Rules.WHITE&&hostileBlack(b,z)){setCell(b,a,Rules.EMPTY);captures++;}}
        }
        for(int d=0;d<4;d++) {
            int kr=r+KING_DR[d],kc=c+KING_DC[d]; if(!Rules.inside(kr,kc))continue;
            int king=Rules.index(kr,kc); if(b.cells[king]!=Rules.KING)continue;
            if(kingCapturedByMove(b,to,d,kr,kc)) b.turn=Rules.BLACK_WIN;
        }
        return captures;
    }
    // Direction index: 0 king left, 1 right, 2 down, 3 up from the moved piece.
    private static boolean kingCapturedByMove(Board b,int to,int d,int r,int c) {
        if(r==4&&c==4) {
            if(d==0)return isB(b,3,4)&&isB(b,4,3)&&isB(b,5,4);
            if(d==1)return isB(b,3,4)&&isB(b,4,5)&&isB(b,5,4);
            if(d==2)return isB(b,5,4)&&isB(b,4,5)&&isB(b,4,3);
            return isB(b,3,4)&&isB(b,4,5)&&isB(b,4,3);
        }
        if(d==0) {
            if(r==3&&c==4)return isB(b,2,4)&&isB(b,3,3);
            if(r==4&&c==5)return isB(b,5,5)&&isB(b,3,5);
            if(r==5&&c==4)return isB(b,6,4)&&isB(b,5,3);
            int z=to-2; return !((r==4&&c==4)||(r==5&&c==4)||(r==3&&c==4)||(r==4&&c==5)) && (b.cells[z]==Rules.BLACK||Rules.camp(z));
        }
        if(d==1) {
            if(r==3&&c==4)return isB(b,2,4)&&isB(b,3,5);
            if(r==5&&c==4)return isB(b,5,5)&&isB(b,6,4);
            if(r==4&&c==3)return isB(b,3,3)&&isB(b,5,3);
            int z=to+2; return !((r==3&&c==4)||(r==5&&c==4)||(r==4&&c==3)||(r==4&&c==4)) && (b.cells[z]==Rules.BLACK||Rules.camp(z));
        }
        if(d==2) {
            if(r==3&&c==4)return isB(b,3,3)&&isB(b,3,5);
            if(r==4&&c==3)return isB(b,4,2)&&isB(b,5,3);
            if(r==4&&c==5)return isB(b,4,6)&&isB(b,5,5);
            int z=to+18; return !((r==4&&c==4)||(r==3&&c==4)||(r==4&&c==3)||(r==4&&c==5)) && (b.cells[z]==Rules.BLACK||Rules.camp(z));
        }
        if(r==5&&c==4)return isB(b,5,3)&&isB(b,5,5);
        if(r==4&&c==3)return isB(b,4,2)&&isB(b,3,3);
        if(r==4&&c==5)return isB(b,4,6)&&isB(b,3,5);
        int z=to-18; return !((r==4&&c==4)||(r==5&&c==4)||(r==4&&c==3)||(r==4&&c==5)) && (b.cells[z]==Rules.BLACK||Rules.camp(z));
    }
    private static boolean isB(Board b,int r,int c) { return b.cells[Rules.index(r,c)]==Rules.BLACK; }
}
