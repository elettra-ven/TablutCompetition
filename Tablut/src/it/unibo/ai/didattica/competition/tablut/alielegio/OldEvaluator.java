package it.unibo.ai.didattica.competition.tablut.alielegio;

import it.unibo.ai.didattica.competition.tablut.domain.State;
import it.unibo.ai.didattica.competition.tablut.domain.State.Pawn;

/** Previous evaluation retained as a reference opponent. */
public final class OldEvaluator {
    public static final int N=9,N_FEATURES=7;
    public static final double[] DEFAULT={3,-2,-1.5,15,60,2,-4};
    private static final boolean[][] CAMP=new boolean[N][N],ESCAPE=new boolean[N][N];
    private static final int[][] ESC_LIST=new int[16][];
    private static final int[][] DIRS={{1,0},{-1,0},{0,1},{0,-1}};
    static {
        for(int i=3;i<=5;i++)CAMP[0][i]=CAMP[8][i]=CAMP[i][0]=CAMP[i][8]=true;
        CAMP[1][4]=CAMP[7][4]=CAMP[4][1]=CAMP[4][7]=true;
        int k=0;int[] e={1,2,6,7};
        for(int i:e){ESCAPE[0][i]=ESCAPE[8][i]=ESCAPE[i][0]=ESCAPE[i][8]=true;ESC_LIST[k++]=new int[]{0,i};ESC_LIST[k++]=new int[]{8,i};ESC_LIST[k++]=new int[]{i,0};ESC_LIST[k++]=new int[]{i,8};}
    }
    public final double[] w;
    public OldEvaluator(){this(DEFAULT);}
    public OldEvaluator(double[] weights){w=weights.clone();}
    public double eval(State s){double[] f=features(s);if(f==null)return -1e5;double v=0;for(int i=0;i<N_FEATURES;i++)v+=w[i]*f[i];return v;}
    public static double[] features(State s){
        Pawn[][] b=s.getBoard();int white=0,black=0,dist=99,open=0,adj=0,routes=0,kr=-1,kc=-1;
        for(int r=0;r<9;r++)for(int c=0;c<9;c++){Pawn p=b[r][c];if(p==Pawn.WHITE)white++;else if(p==Pawn.BLACK)black++;else if(p==Pawn.KING){kr=r;kc=c;}}
        if(kr<0)return null;
        for(int[] t:ESC_LIST)dist=Math.min(dist,Math.abs(t[0]-kr)+Math.abs(t[1]-kc));
        for(int[] d:DIRS){int r=kr+d[0],c=kc+d[1];if(inside(r,c)&&b[r][c]==Pawn.BLACK)adj++;if(canReach(b,kr,kc,d[0],d[1]))open++;
            while(inside(r,c)&&free(b,r,c)){if(canReach(b,r,c,d[1],d[0]))routes++;if(canReach(b,r,c,-d[1],-d[0]))routes++;r+=d[0];c+=d[1];}}
        return new double[]{white,black,dist,open,open>=2?1:0,routes,adj};
    }
    private static boolean inside(int r,int c){return r>=0&&r<9&&c>=0&&c<9;}
    private static boolean free(Pawn[][] b,int r,int c){return b[r][c]==Pawn.EMPTY&&!CAMP[r][c];}
    private static boolean canReach(Pawn[][] b,int r,int c,int dr,int dc){r+=dr;c+=dc;while(inside(r,c)&&free(b,r,c)){if(ESCAPE[r][c])return true;r+=dr;c+=dc;}return false;}
}
