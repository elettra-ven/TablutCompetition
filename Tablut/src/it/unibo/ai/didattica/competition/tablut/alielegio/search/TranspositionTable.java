package it.unibo.ai.didattica.competition.tablut.alielegio.search;

public final class TranspositionTable {
    public static final byte NONE=0, UPPER=1, EXACT=2, LOWER=3;
    private final long[] keys;
    private final int[] scores, moves, generations;
    private final short[] depths;
    private final byte[] bounds;
    private final int mask;
    private int generation=1;
    public TranspositionTable() { this(1<<20); }
    public TranspositionTable(int entries) {
        int n=1; while(n<entries)n<<=1;
        keys=new long[n];scores=new int[n];moves=new int[n];generations=new int[n];depths=new short[n];bounds=new byte[n];mask=n-1;
    }
    public void newSearch() {
        generation++;
        if(generation==0) { for(int i=0;i<generations.length;i++)generations[i]=0;generation=1; }
    }
    public int generation() { return generation; }
    public int slot(long key) { return (int)(mix(key)&mask); }
    public boolean matches(int slot,long key) { return generations[slot]==generation&&bounds[slot]!=NONE&&keys[slot]==key; }
    public int depth(int slot) { return depths[slot]&0xffff; }
    public int score(int slot) { return scores[slot]; }
    public int move(int slot) { return moves[slot]; }
    public byte bound(int slot) { return bounds[slot]; }
    public void store(long key,int depth,int score,byte bound,int move) {
        int s=slot(key);
        if(generations[s]==generation&&keys[s]==key&&depth(s)>depth&&bound!=EXACT)return;
        keys[s]=key;scores[s]=score;moves[s]=move;depths[s]=(short)Math.min(32767,Math.max(0,depth));bounds[s]=bound;generations[s]=generation;
    }
    private static long mix(long z) {
        z=(z^(z>>>30))*0xbf58476d1ce4e5b9L;
        z=(z^(z>>>27))*0x94d049bb133111ebL;
        return z^(z>>>31);
    }
}
