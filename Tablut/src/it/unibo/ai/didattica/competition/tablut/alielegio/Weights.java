package it.unibo.ai.didattica.competition.tablut.alielegio;

/** Independently tunable, real-valued weights for the two player perspectives. */
public final class Weights {
    public static final int WHITE_COUNT=11, BLACK_COUNT=6;
    public static final String[] WHITE_NAMES={"whiteSoldiers","kingPresent","escapeProximity","openEscapeLines","doubleThreat","twoMoveEscapeRoutes","kingMobility","blackPiecesNeeded","blackHanging","whiteMobility","blackInCamps"};
    public static final String[] BLACK_NAMES={"blackSoldiers","blackAdjacentToKing","escapeCordon","whiteHanging","blackMobility","blackOutsideCamps"};
    private static final double[] WHITE_DEFAULT={3.0,1500.0,35.0,40.0,80.0,6.0,14.0,35.0,20.0,0.35,1.5};
    private static final double[] BLACK_DEFAULT={2.3,12.0,30.0,25.0,0.25,0.6};
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
