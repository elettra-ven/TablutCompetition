package it.unibo.ai.didattica.competition.tablut.alielegio.test;

import it.unibo.ai.didattica.competition.tablut.alielegio.engine.*;

/** Deterministic perft-style make/unmake throughput check. */
public final class EngineBench {
    private static long nodes;
    private static final int[][] BUFFERS=new int[24][1024];
    private static final MoveGen.Undo[] UNDOS=new MoveGen.Undo[24];
    static { for(int i=0;i<UNDOS.length;i++) UNDOS[i]=new MoveGen.Undo(); }
    private EngineBench() { }
    public static void main(String[] args) {
        int depth=args.length==0?4:Integer.parseInt(args[0]);
        Board b=Board.initial();long start=System.nanoTime();nodes=0;perft(b,depth,0);
        double seconds=(System.nanoTime()-start)/1.0e9;
        System.out.println("position=initial depth="+depth+" nodes="+nodes+" seconds="+seconds+" nodesPerSecond="+(long)(nodes/seconds));
        Board mid=Board.initial();int[] ms=new int[1024];
        for(int ply=0;ply<12;ply++) {int n=MoveGen.generate(mid,ms);if(n==0)break;MoveGen.make(mid,ms[(ply*37+11)%n],new MoveGen.Undo());}
        start=System.nanoTime();nodes=0;perft(mid,depth,0);seconds=(System.nanoTime()-start)/1.0e9;
        System.out.println("position=midgame ply=12 depth="+depth+" nodes="+nodes+" seconds="+seconds+" nodesPerSecond="+(long)(nodes/seconds));
    }
    private static void perft(Board b,int depth,int ply) {
        nodes++;if(depth==0||b.terminal())return;
        int[] moves=BUFFERS[ply];int n=MoveGen.generate(b,moves);MoveGen.Undo u=UNDOS[ply];
        for(int i=0;i<n;i++){MoveGen.make(b,moves[i],u);perft(b,depth-1,ply+1);MoveGen.unmake(b,u);}
    }
}
