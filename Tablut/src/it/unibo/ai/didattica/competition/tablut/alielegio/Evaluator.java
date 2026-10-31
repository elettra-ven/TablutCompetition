package it.unibo.ai.didattica.competition.tablut.alielegio;

import it.unibo.ai.didattica.competition.tablut.domain.State;
import it.unibo.ai.didattica.competition.tablut.domain.State.Pawn;
/**
 * Evaluator: GA
 */
public class Evaluator {
    public final double[] w;

    public Evaluator(double[] weights) {
        this.w = weights.clone();
    }

    public double eval(State s){
        Pawn[][] b = s.getBoard();
        int n = b.length;
        int white = 0;
        int black = 0;
        int kr = -1; //king row
        int kc = -1; //king col

        for (int row = 0; row<n; row++){
            for (int col = 0; col <n; col++){
                Pawn p = b[row][col];
                if (p == Pawn.WHITE) white++;
                else if (p == Pawn.BLACK) black++;
                else if (p == Pawn.KING) { kr = r; kc = c; }
            }
        }

        if (kr<0) return -1e5; //game-over, king in invalid pos

        // finish aaaaaaaaaaaaaaaaaa
    }

}