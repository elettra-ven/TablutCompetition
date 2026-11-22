package it.unibo.ai.didattica.competition.tablut.alielegio.test;

import it.unibo.ai.didattica.competition.tablut.alielegio.Evaluator;
import it.unibo.ai.didattica.competition.tablut.alielegio.Weights;
import it.unibo.ai.didattica.competition.tablut.alielegio.engine.Board;
import it.unibo.ai.didattica.competition.tablut.alielegio.engine.Rules;
import it.unibo.ai.didattica.competition.tablut.domain.State;
import it.unibo.ai.didattica.competition.tablut.domain.State.Turn;
import it.unibo.ai.didattica.competition.tablut.domain.StateTablut;

/** Prints named feature vectors and runs evaluator assertions on reference boards. */
public final class EvalDemo {
    private EvalDemo() { }
    public static void main(String[] args) {
        Evaluator eval=new Evaluator();
        Board start=Board.initial();assertFinite(eval.eval(start),"start");print(eval,"initial",start);
        Board alone=position(2,2);print(eval,"king alone",alone);
        Board two=position(2,2);two.cells[Rules.index(2,1)]=Rules.BLACK;two.cells[Rules.index(1,2)]=Rules.BLACK;two.recomputeHash();two.blackSoldiers=2;print(eval,"king with two open lines",two);
        Board surrounded=new Board(two);surrounded.cells[Rules.index(2,3)]=Rules.BLACK;surrounded.cells[Rules.index(3,2)]=Rules.BLACK;surrounded.blackSoldiers=4;surrounded.recomputeHash();print(eval,"king surrounded",surrounded);
        if(eval.features(two).white[4]<=eval.features(surrounded).white[4])throw new AssertionError("double-threat feature missing");
        if(eval.eval(two)<=eval.eval(surrounded))throw new AssertionError("two open lines should score above no open lines");
        Board losing=position(-1,-1);if(eval.eval(losing)>=eval.eval(surrounded))throw new AssertionError("missing king must be the minimum score");
        Board oneWhite=position(2,2);oneWhite.cells[Rules.index(0,0)]=Rules.WHITE;oneWhite.whiteSoldiers=1;oneWhite.recomputeHash();
        Board oneBlack=position(2,2);oneBlack.cells[Rules.index(0,0)]=Rules.BLACK;oneBlack.blackSoldiers=1;oneBlack.recomputeHash();
        if(!(eval.eval(oneWhite)>eval.eval(alone)&&eval.eval(oneBlack)<eval.eval(alone)))throw new AssertionError("material perspective sign check failed");
        assertFinite(eval.eval(alone),"king alone");assertFinite(eval.eval(surrounded),"surrounded king");
        System.out.println("EvalDemo PASS materialSign=ok doubleThreat=ok kingLoss=minimum");
    }
    private static Board position(int kr,int kc) {
        StateTablut state=new StateTablut();for(int r=0;r<9;r++)for(int c=0;c<9;c++)state.getBoard()[r][c]=State.Pawn.EMPTY;
        state.getBoard()[4][4]=State.Pawn.THRONE;if(kr>=0)state.getBoard()[kr][kc]=State.Pawn.KING;state.setTurn(Turn.WHITE);return Board.fromState(state);
    }
    private static void print(Evaluator evaluator,String name,Board board) {
        Evaluator.FeatureVector f=evaluator.features(board);System.out.println("position="+name+" score="+evaluator.eval(board));
        for(int i=0;i<f.white.length;i++)System.out.println("  white."+Weights.WHITE_NAMES[i]+"="+f.white[i]);
        for(int i=0;i<f.black.length;i++)System.out.println("  black."+Weights.BLACK_NAMES[i]+"="+f.black[i]);
    }
    private static void assertFinite(double score,String name){if(Double.isNaN(score)||Double.isInfinite(score))throw new AssertionError("non-finite score for "+name);}
}
