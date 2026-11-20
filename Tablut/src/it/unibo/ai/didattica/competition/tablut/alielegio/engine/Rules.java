package it.unibo.ai.didattica.competition.tablut.alielegio.engine;

/** Ashton board constants and source-compatible rule predicates. */
public final class Rules {
    public static final int SIZE = 9, CELLS = 81;
    public static final byte EMPTY = 0, WHITE = 1, BLACK = 2, KING = 3, THRONE = 4;
    public static final byte WHITE_TURN = 0, BLACK_TURN = 1, WHITE_WIN = 2, BLACK_WIN = 3, DRAW = 4;
    private static final boolean[] CAMP = new boolean[CELLS];
    private static final boolean[] WHITE_HOSTILE_CAMP = new boolean[CELLS];
    static {
        int[][] c = {{0,3},{0,4},{0,5},{1,4},{8,3},{8,4},{8,5},{7,4},
                {3,0},{4,0},{5,0},{4,1},{3,8},{4,8},{5,8},{4,7}};
        for (int i=0;i<c.length;i++) CAMP[index(c[i][0],c[i][1])] = true;
        for (int i=0;i<CELLS;i++) WHITE_HOSTILE_CAMP[i] = CAMP[i];
        WHITE_HOSTILE_CAMP[index(4,0)] = false;
        WHITE_HOSTILE_CAMP[index(0,4)] = false;
        WHITE_HOSTILE_CAMP[index(8,4)] = false;
        WHITE_HOSTILE_CAMP[index(4,8)] = false;
    }
    private Rules() { }
    public static int index(int row, int col) { return row * SIZE + col; }
    public static int row(int sq) { return sq / SIZE; }
    public static int col(int sq) { return sq % SIZE; }
    public static boolean inside(int r, int c) { return r >= 0 && r < SIZE && c >= 0 && c < SIZE; }
    public static boolean camp(int sq) { return CAMP[sq]; }
    public static boolean whiteHostileCamp(int sq) { return WHITE_HOSTILE_CAMP[sq]; }
    public static boolean edge(int sq) { int r=row(sq), c=col(sq); return r==0||r==8||c==0||c==8; }
    public static boolean throne(int sq) { return sq == 40; }
    public static int terminalFromTurn(byte t) { return t; }
}
