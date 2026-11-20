package it.unibo.ai.didattica.competition.tablut.alielegio.baseline;

import it.unibo.ai.didattica.competition.tablut.alielegio.engine.Board;
import it.unibo.ai.didattica.competition.tablut.alielegio.engine.Move;
import it.unibo.ai.didattica.competition.tablut.alielegio.engine.MoveGen;
import it.unibo.ai.didattica.competition.tablut.alielegio.engine.Rules;

public final class GreedyPlayer {
    private final int[] moves=new int[1024];
    private final MoveGen.Undo undo=new MoveGen.Undo();
    public int choose(Board board) {
        int n=MoveGen.generate(board,moves);if(n==0)return -1;
        boolean white=board.turn==Rules.WHITE_TURN;int best=moves[0];double bestScore=white?Double.NEGATIVE_INFINITY:Double.POSITIVE_INFINITY;
        for(int i=0;i<n;i++) {
            int move=moves[i];MoveGen.make(board,move,undo);double score=score(board);MoveGen.unmake(board,undo);
            if((white&&score>bestScore)||(!white&&score<bestScore)){bestScore=score;best=move;}
        }
        return best;
    }
    private double score(Board b) {
        if(b.result==Rules.WHITE_WIN)return 1000000;if(b.result==Rules.BLACK_WIN)return -1000000;if(b.result==Rules.DRAW)return 0;
        int king=-1;for(int i=0;i<81;i++)if(b.cells[i]==Rules.KING){king=i;break;}
        if(king<0)return -1000000;
        int r=Rules.row(king),c=Rules.col(king),dist=Math.min(Math.min(r,8-r),Math.min(c,8-c));
        int open=0;for(int d=0;d<4;d++){int rr=r+(d==0?-1:d==1?1:0),cc=c+(d==2?-1:d==3?1:0);while(Rules.inside(rr,cc)){int s=Rules.index(rr,cc);if(b.cells[s]!=Rules.EMPTY||Rules.camp(s))break;if(Rules.edge(s)){open++;break;}rr+=(d==0?-1:d==1?1:0);cc+=(d==2?-1:d==3?1:0);}}
        return 100.0*(b.whiteSoldiers-b.blackSoldiers)+12.0*(8-dist)+80.0*open;
    }
}
