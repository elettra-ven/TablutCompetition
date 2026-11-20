package it.unibo.ai.didattica.competition.tablut.alielegio.tuning;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import it.unibo.ai.didattica.competition.tablut.alielegio.Weights;

/** Color-swapped final validation of exported weights against the defaults. */
public final class TuningValidation {
    private TuningValidation() { }
    private static Weights load(File f) throws Exception {
        BufferedReader in=new BufferedReader(new FileReader(f));String line;double[] w=new double[Weights.WHITE_COUNT],b=new double[Weights.BLACK_COUNT];
        while((line=in.readLine())!=null) {
            String[] p=line.split(",");if(p.length!=2)continue;
            for(int i=0;i<Weights.WHITE_COUNT;i++)if(Weights.WHITE_NAMES[i].equals(p[0]))w[i]=Double.parseDouble(p[1]);
            for(int i=0;i<Weights.BLACK_COUNT;i++)if(Weights.BLACK_NAMES[i].equals(p[0]))b[i]=Double.parseDouble(p[1]);
        }
        in.close();return new Weights(w,b);
    }
    public static void main(String[] args) throws Exception {
        File file=new File(args.length>0?args[0]:"docs/tuning/best_weights.csv");
        int games=args.length>1?Integer.parseInt(args[1]):500;
        long nodes=args.length>2?Long.parseLong(args[2]):1024;
        long seed=args.length>3?Long.parseLong(args[3]):20261125L;
        int cap=args.length>4?Integer.parseInt(args[4]):250;
        Weights champion=load(file);
        SelfPlay.AgentSpec a=new SelfPlay.AgentSpec("search",champion,nodes,true);
        SelfPlay.AgentSpec b=new SelfPlay.AgentSpec("search",Weights.defaults(),nodes,true);
        SelfPlay.Summary s=SelfPlay.run(a,b,games,seed,cap,Math.min(8,games),2);
        double scoreRate=(s.aWins+0.5*s.draws)/games;
        double secondMoment=(s.aWins+0.25*s.draws)/(double)games;
        double standardError=Math.sqrt(Math.max(0.0,secondMoment-scoreRate*scoreRate)/games);
        System.out.println("TuningValidation champion/default games="+games+" seed="+seed+" nodeBudget="+nodes+" openingPlies=2");
        System.out.println("champion W/D/L="+s.aWins+"/"+s.draws+"/"+s.bWins+" tournamentPoints="+s.aPoints+" scoreRate=(W+0.5D)/N="+scoreRate+" approximate95pct="+(scoreRate-1.96*standardError)+".."+(scoreRate+1.96*standardError));
        System.out.println("champion White="+s.aWhiteWins+"/"+s.aWhiteDraws+"/"+s.aWhiteLosses+" Black="+s.aBlackWins+"/"+s.aBlackDraws+"/"+s.aBlackLosses+" nodes="+s.nodes+" gamesPerHour="+(long)s.gamesPerHour+" crossChecked="+s.checkedGames);
    }
}
