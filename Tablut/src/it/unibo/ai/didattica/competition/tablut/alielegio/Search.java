package it.unibo.ai.didattica.competition.tablut.alielegio;

import java.util.*;
import it.unibo.ai.didattica.competition.tablut.domain.*;
import it.unibo.ai.didattica.competition.tablut.domain.State.Pawn;
import it.unibo.ai.didattica.competition.tablut.domain.State.Turn;

public class Search {
    private static final double WIN = 1e6; //max score
    private final Game rules = new GameAshtonTablut(99, 0, "garbage", "fake", "fake");
    private final Evaluator eval; //to-do
    private long deadline;
    public long nodes;
    private static final int max_depth = 40;

    private static class TimeUp extends RuntimeException {
        TimeUp() { super(null, null, false, false); }
    }
    private static class Child {
        final Action a; final State s; double score;
        Child(Action a, State s) { this.a = a; this.s = s; }
    }

    public Search(Evaluator eval) { this.eval = eval; }

    public Action bestMove(State s, long budgetMs) {
        deadline = System.currentTimeMillis() + budgetMs;
        nodes = 0;
        List<Child> kids = children(s);
        if (kids.isEmpty()) return null;
        Action best = kids.get(0).a;
        boolean maxing = s.getTurn() == Turn.WHITE;


        try {
            for (int depth = 1; depth < max_depth; depth++) {
                double alpha = -Double.MAX_VALUE, beta = Double.MAX_VALUE;
                Child bestKid = null;
                for (Child k : kids) {
                    k.score = minimax(k.s, depth - 1, alpha, beta);
                    if (bestKid == null || (maxing ? k.score > bestKid.score : k.score < bestKid.score))
                        bestKid = k;
                    if (maxing) alpha = Math.max(alpha, k.score); else beta = Math.min(beta, k.score);
                }
                best = bestKid.a;
                final boolean m = maxing;
                kids.sort((x, y) -> m ? Double.compare(y.score, x.score) : Double.compare(x.score, y.score));
                if (Math.abs(bestKid.score) >= WIN) break;
            }
        } catch (TimeUp e) { 
            // TO-DO
         }
        return best;
    }

    private double minimax(State s, int depth, double alpha, double beta) {
        if (System.currentTimeMillis() > deadline) throw new TimeUp();
        nodes++;
        Turn t = s.getTurn();
        if (t == Turn.WHITEWIN) return WIN + depth;
        if (t == Turn.BLACKWIN) return -WIN - depth;
        if (t == Turn.DRAW) return 0;
        if (depth == 0) return eval.eval(s);

        List<Child> kids = children(s);
        if (kids.isEmpty()) return t == Turn.WHITE ? -WIN - depth : WIN + depth;
        if (t == Turn.WHITE) {
            double v = -Double.MAX_VALUE;
            for (Child k : kids) {
                v = Math.max(v, minimax(k.s, depth - 1, alpha, beta));
                alpha = Math.max(alpha, v);
                if (alpha >= beta) break;
            }
            return v;
        } else { //BLACK
            double v = Double.MAX_VALUE;
            for (Child k : kids) {
                v = Math.min(v, minimax(k.s, depth - 1, alpha, beta));
                beta = Math.min(beta, v);
                if (alpha >= beta) break;
            }
            return v;
        }
    }


    private List<Child> children(State s) {
        List<Child> out = new ArrayList<>();
        Turn me = s.getTurn();
        Pawn[][] b = s.getBoard();
        int n = b.length;
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        for (int r = 0; r < n; r++)
            for (int c = 0; c < n; c++) {
                Pawn p = b[r][c];
                boolean mine = me == Turn.WHITE ? (p == Pawn.WHITE || p == Pawn.KING) : p == Pawn.BLACK;
                if (!mine) continue;
                String from = s.getBox(r, c);
                for (int[] d : dirs) {
                    int rr = r + d[0], cc = c + d[1];
                    while (rr >= 0 && rr < n && cc >= 0 && cc < n) {
                        Pawn q = b[rr][cc];
                        if (q != Pawn.EMPTY && q != Pawn.THRONE) break;
                        try {
                            Action a = new Action(from, s.getBox(rr, cc), me);
                            State next = rules.checkMove(s.clone(), a);
                            out.add(new Child(a, next));
                        } catch (Exception illegal) { 
                            // to-do
                         }
                        rr += d[0]; cc += d[1];
                    }
                }
            }
        return out;
    }
}