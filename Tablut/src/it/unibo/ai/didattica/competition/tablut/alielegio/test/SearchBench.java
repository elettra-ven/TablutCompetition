package it.unibo.ai.didattica.competition.tablut.alielegio.test;

import it.unibo.ai.didattica.competition.tablut.alielegio.Evaluator;
import it.unibo.ai.didattica.competition.tablut.alielegio.Search;
import it.unibo.ai.didattica.competition.tablut.alielegio.engine.Board;
import it.unibo.ai.didattica.competition.tablut.alielegio.engine.MoveGen;

public final class SearchBench {
    private SearchBench() { }
    public static void main(String[] args) {
        if(Search.budgetForTimeoutSeconds(5)!=3500L||Search.budgetForTimeoutSeconds(10)!=8500L||Search.budgetForTimeoutSeconds(60)!=52800L)
            throw new AssertionError("timeout budget reserve calculation mismatch");
        System.out.println("usableBudgetsMs timeout5="+Search.budgetForTimeoutSeconds(5)+" timeout10="+Search.budgetForTimeoutSeconds(10)+" timeout60="+Search.budgetForTimeoutSeconds(60));
        long budget=args.length>0?Long.parseLong(args[0]):1000;
        int checks=args.length>1?Integer.parseInt(args[1]):500;
        long checkBudget=args.length>2?Long.parseLong(args[2]):20;
        Search search=new Search(new Evaluator());
        run(search,Board.initial(),budget,"initial");
        Board mid=Board.initial();int[] moves=new int[1024];
        for(int ply=0;ply<12;ply++){int n=MoveGen.generate(mid,moves);if(n==0)break;MoveGen.make(mid,moves[(ply*37+11)%n],new MoveGen.Undo());}
        run(search,mid,budget,"midgame-ply12");
        long worst=0,total=0;
        for(int i=0;i<checks;i++) {
            Board b=(i&1)==0?Board.initial():new Board(mid);
            long start=System.nanoTime();int move=search.bestMove(b,checkBudget);long elapsedNanos=System.nanoTime()-start;
            long elapsed=(elapsedNanos+999999L)/1000000L;
            if(move<0)throw new AssertionError("no move in timed check "+i);
            total+=elapsed;if(elapsed>worst)worst=elapsed;
            if(elapsedNanos>checkBudget*1000000L)throw new AssertionError("budget exceeded at check "+i+": "+elapsedNanos+" ns > "+checkBudget+" ms");
        }
        if(checks>0)System.out.println("timedChecks="+checks+" budgetMs="+checkBudget+" averageMs="+(total/(double)checks)+" worstMs="+worst+" overruns=0");
    }
    private static void run(Search search,Board board,long budget,String name) {
        int move=search.bestMove(board,budget);
        System.out.println("position="+name+" budgetMs="+budget+" move="+move+" depth="+search.completedDepth+" nodes="+search.nodes+" elapsedMs="+search.elapsedMillis+" nodesPerSecond="+search.nodesPerSecond);
    }
}
