package it.unibo.ai.didattica.competition.tablut.alielegio.engine;

import java.io.IOException;
import it.unibo.ai.didattica.competition.tablut.domain.Action;
import it.unibo.ai.didattica.competition.tablut.domain.State;

public final class Converter {
    private Converter() { }
    public static Board fromState(State state) { return Board.fromState(state); }
    public static Action toAction(int move, byte turn) throws IOException {
        int f=Move.from(move),t=Move.to(move);
        String from=""+(char)('a'+Rules.col(f))+(char)('1'+Rules.row(f));
        String to=""+(char)('a'+Rules.col(t))+(char)('1'+Rules.row(t));
        return new Action(from,to,turn==Rules.BLACK_TURN?State.Turn.BLACK:State.Turn.WHITE);
    }
}
