package it.unibo.ai.didattica.competition.tablut.alielegio.baseline;

import it.unibo.ai.didattica.competition.tablut.alielegio.OldEvaluator;
import it.unibo.ai.didattica.competition.tablut.alielegio.engine.Board;
import it.unibo.ai.didattica.competition.tablut.alielegio.engine.MoveGen;

public final class OldPlayer {
    private final OldEvaluator evaluator=new OldEvaluator();
    private final int[] moves=new int[1024];
    private final MoveGen.Undo undo=new MoveGen.Undo();
    public int choose(Board board) {
        int n=MoveGen.generate(board,moves);if(n==0)return -1;
        boolean white=board.turn==0;int best=moves[0];double scoreBest=white?Double.NEGATIVE_INFINITY:Double.POSITIVE_INFINITY;
        for(int i=0;i<n;i++){int move=moves[i];MoveGen.make(board,move,undo);double score=evaluator.eval(board.toState());MoveGen.unmake(board,undo);
            if((white&&score>scoreBest)||(!white&&score<scoreBest)){scoreBest=score;best=move;}}
        return best;
    }
}
