package it.unibo.ai.didattica.competition.tablut.alielegio.engine;

/** A move packed as six-bit from and to square indices. */
public final class Move {
    private Move() { }
    public static int of(int from, int to) { return (from << 7) | to; }
    public static int from(int move) { return (move >>> 7) & 127; }
    public static int to(int move) { return move & 127; }
}
