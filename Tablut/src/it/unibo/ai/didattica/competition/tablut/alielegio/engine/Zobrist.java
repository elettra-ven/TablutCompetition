package it.unibo.ai.didattica.competition.tablut.alielegio.engine;

import java.util.Random;

public final class Zobrist {
    static final long[][] PIECE = new long[81][5];
    static final long BLACK_TO_MOVE;
    static {
        Random random = new Random(0x414c49454c454749L);
        for (int s=0;s<81;s++) for (int p=0;p<5;p++) PIECE[s][p]=random.nextLong();
        BLACK_TO_MOVE=random.nextLong();
    }
    private Zobrist() { }
}
