package it.unibo.ai.didattica.competition.tablut.alielegio;

import it.unibo.ai.didattica.competition.tablut.client.TablutClient;
import it.unibo.ai.didattica.competition.tablut.domain.Action;
import it.unibo.ai.didattica.competition.tablut.domain.Game;
import it.unibo.ai.didattica.competition.tablut.domain.GameAshtonTablut;
import it.unibo.ai.didattica.competition.tablut.domain.State;
import it.unibo.ai.didattica.competition.tablut.domain.State.Turn;



public class OurClient extends TablutClient {
    private final Search search = new Search(new Evaluator());

    private final int timeoutSec;
    private final Game rules =
            new GameAshtonTablut(99, 0, "garbage", "fake", "fake");

    public OurClient(String role, String name, int timeout, String ip) throws Exception {
        super(role, name, timeout, ip);
        this.timeoutSec = timeout;
    }

    @Override
    public void run() {
        try {
            declareName();
            Turn me = getPlayer();

            long budgetMs = (long) (timeoutSec * 1000L * 0.7);

            while (true) {
                read();
                State state = getCurrentState();
                Turn t = state.getTurn();

                if (t == Turn.WHITEWIN || t == Turn.BLACKWIN || t == Turn.DRAW) {
                    System.out.println("Game ended: " + t);
                    System.exit(0);
                }
                if (!t.equals(me)) continue; // chiama il turno dell'avversario

                Action a = search.bestMove(state, budgetMs);
                if (a == null) a = firstLegalMove(state, me);
                if (a == null) { System.err.println("No legal moves"); System.exit(2); }
                write(a);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private Action firstLegalMove(State state, Turn me) throws Exception {
        int n = state.getBoard().length;
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                State.Pawn p = state.getPawn(r, c);
                boolean mine = (me == Turn.WHITE)
                        ? (p.equals(State.Pawn.WHITE) || p.equals(State.Pawn.KING))
                        : p.equals(State.Pawn.BLACK);
                if (!mine) continue;
                String from = state.getBox(r, c);
                for (int r2 = 0; r2 < n; r2++) {
                    for (int c2 = 0; c2 < n; c2++) {
                        if ((r2 != r) == (c2 != c)) continue;
                        Action a = new Action(from, state.getBox(r2, c2), me);
                        try {
                            rules.checkMove(state.clone(), a);
                            return a;
                        } catch (Exception ignored) { }
                    }
                }
            }
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        String role = args[0];
        int timeout = Integer.parseInt(args[1]);
        String ip = args[2];
        new OurClient(role, "AliEleGio", timeout, ip).run();
    }
}