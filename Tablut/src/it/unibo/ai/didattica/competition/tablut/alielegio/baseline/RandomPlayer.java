package it.unibo.ai.didattica.competition.tablut.alielegio.baseline;

import java.util.Random;
import it.unibo.ai.didattica.competition.tablut.alielegio.engine.Board;
import it.unibo.ai.didattica.competition.tablut.alielegio.engine.MoveGen;

/** Seeded uniform random legal-move baseline. */
public final class RandomPlayer {
    private final Random random;
    private final int[] moves=new int[1024];
    public RandomPlayer(long seed){random=new Random(seed);}
    public void setSeed(long seed){random.setSeed(seed);}
    public int choose(Board board){int n=MoveGen.generate(board,moves);return n==0?-1:moves[random.nextInt(n)];}
}
