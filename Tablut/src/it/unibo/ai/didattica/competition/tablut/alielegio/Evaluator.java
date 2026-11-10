package it.unibo.ai.didattica.competition.tablut.alielegio;

import it.unibo.ai.didattica.competition.tablut.domain.State;
import it.unibo.ai.didattica.competition.tablut.domain.State.Pawn;
/**
 * Evaluator
 * The following feature are positive if they benefit WHITE and negative if the benefit BLACK.
 * DEFAULT is the array that stores the weights of these features,
 * in the following order:
 * 
 * 0) White soldiers (white pieces - king)
 * 1) Black pieces
 * 2) Distance king-nearest escape
 * 3) king's available esapes
 * 4) king has at least two open escape routes simultaneously.
 * 5) number of potential escape routes the king can create within two moves
 * 6) black pawns adjacent to the king.
 */
public class Evaluator {

    public static final int N = 9;
    public static final int N_FEATURES = 7;

    public static final double[] DEFAULT = {3, -2, -1.5, 15, 60, 2, -4};

    private static final boolean[][] CAMP = new boolean[N][N];
    private static final boolean[][] ESCAPE = new boolean[N][N];
    private static final int[][] ESC_LIST = new int[16][];
    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    static {
        for (int i = 3; i <= 5; i++) {
            CAMP[0][i] = CAMP[8][i] = CAMP[i][0] = CAMP[i][8] = true;
        }
        CAMP[1][4] = CAMP[7][4] = CAMP[4][1] = CAMP[4][7] = true;
        int k = 0;
        int[] e = {1, 2, 6, 7};
        for (int i : e) {
            ESCAPE[0][i] = ESCAPE[8][i] = ESCAPE[i][0] = ESCAPE[i][8] = true;
            ESC_LIST[k++] = new int[]{0, i};
            ESC_LIST[k++] = new int[]{8, i};
            ESC_LIST[k++] = new int[]{i, 0};
            ESC_LIST[k++] = new int[]{i, 8};
        }
    }

    public final double[] w;

    public Evaluator() { this(DEFAULT); }
    public Evaluator(double[] weights) {
        this.w = weights.clone();
    }

    public double eval(State s){
        double[] f = features(s);
        if (f==null){
            return -1e5;
        }

        double v = 0.0;
        for (int i = 0; i < N_FEATURES; i++){
            v += w[i]*f[i];
        }

        return v;
    }

    public static double[] features(State s){
        Pawn[][] b = s.getBoard();
        int n = b.length;
        int white = 0;
        int black = 0;
        int dist = 99;
        int open = 0;
        int adjBlack = 0;
        int routes = 0;
        int kr = -1; //king row
        int kc = -1; //king col

        for (int row = 0; row<n; row++){
            for (int col = 0; col <n; col++){
                Pawn p = b[row][col];
                if (p == Pawn.WHITE) white++;
                else if (p == Pawn.BLACK) black++;
                else if (p == Pawn.KING) { 
                    kr = row; 
                    kc = col; 
                }
            }
        }

        if (kr<0) {
            return null; //game-over, king in invalid pos
        }

        for (int[] t : ESC_LIST){
            dist = Math.min(dist, Math.abs(t[0] - kr) + Math.abs(t[1] - kc));
        }

        for (int[] d : DIRS) {
            /*System.out.println(
                "Direction (" + d[0] + "," + d[1] + ") => can escape" +
                canReachEscape(b, kr, kc, d)
            );*/
            int r = kr + d[0];
            int c = kc + d[1];

            if (inside(r,c)&& b[r][c] == Pawn.BLACK){
                adjBlack++;
            }
            if (canReachEscape(b,kr,kc,d)){
                open++;
            }

            while (inside(r,c) && free(b,r,c)){
                int[] p1 = {d[1],d[0]};
                int[] p2 = {-d[1],-d[0]};
                if (canReachEscape(b, r, c, p1)){
                    routes++;
                }
                if (canReachEscape(b, r, c, p2)){
                    routes++;
                }

                r += d[0];
                c += d[1];
            }
        }

        double[] f = new double[N_FEATURES];
        f[0] = white;
        f[1] = black;
        f[2] = dist;
        f[3] = open;
        f[4] = open >= 2 ? 1 : 0;
        f[5] = routes;
        f[6] = adjBlack;

        return f;
    }


    private static boolean inside(int r, int c) { 
        return r >= 0 && r < N && c >= 0 && c < N; 
    }

    private static boolean free(Pawn[][] b, int r, int c) { 
        return b[r][c] == Pawn.EMPTY && !CAMP[r][c]; 
    }

    private static boolean canReachEscape(Pawn[][] b, int r, int c, int[] d) {
        r += d[0]; c += d[1];
        while (inside(r, c) && free(b, r, c)) {
            if (ESCAPE[r][c]) return true;
            r += d[0]; c += d[1];
        }
        return false;
    }

}