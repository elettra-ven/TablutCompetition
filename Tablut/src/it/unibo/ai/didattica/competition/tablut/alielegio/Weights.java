package it.unibo.ai.didattica.competition.tablut.alielegio;

/** Independently tunable, real-valued weights for the two player perspectives. */
public final class Weights {
    public static final int WHITE_COUNT=11, BLACK_COUNT=6;
    public static final String[] WHITE_NAMES={"whiteSoldiers","kingPresent","escapeProximity","openEscapeLines","doubleThreat","twoMoveEscapeRoutes","kingMobility","blackPiecesNeeded","blackHanging","whiteMobility","blackInCamps"};
    public static final String[] BLACK_NAMES={"blackSoldiers","blackAdjacentToKing","escapeCordon","whiteHanging","blackMobility","blackOutsideCamps"};
    // GA champion from docs/tuning/best_weights.csv, seed 20261124.
    private static final double[] WHITE_DEFAULT={2.5860530419838907,1513.350347131091,26.371596239473526,37.16204775161391,92.92659693523596,6.456213508562451,15.175503149203223,31.475227030822072,23.572907492169463,1.1662281174901423,1.5487044448761371};
    private static final double[] BLACK_DEFAULT={0.8411859001111897,21.23497348965061,25.06950728002016,18.56443640643563,0.1560785677806784,0.2733418863349191};
    public final double[] white;
    public final double[] black;
    public Weights() { this(WHITE_DEFAULT,BLACK_DEFAULT); }
    public Weights(double[] whiteWeights,double[] blackWeights) {
        if(whiteWeights==null||whiteWeights.length!=WHITE_COUNT)throw new IllegalArgumentException("white weight count must be "+WHITE_COUNT);
        if(blackWeights==null||blackWeights.length!=BLACK_COUNT)throw new IllegalArgumentException("black weight count must be "+BLACK_COUNT);
        white=whiteWeights.clone();black=blackWeights.clone();
    }
    public static Weights defaults(){return new Weights();}
    public Weights copy(){return new Weights(white,black);}
}
