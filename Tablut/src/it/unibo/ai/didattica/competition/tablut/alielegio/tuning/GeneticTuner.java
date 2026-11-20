package it.unibo.ai.didattica.competition.tablut.alielegio.tuning;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import it.unibo.ai.didattica.competition.tablut.alielegio.Weights;

/** Real-valued genetic search over the separate White and Black evaluation weights. */
public final class GeneticTuner {
    private static final int GENES=Weights.WHITE_COUNT+Weights.BLACK_COUNT;
    private static final double[] LOW={0,300,2,0,0,0,0,0,0,0,0, 0,0,0,0,0,0};
    private static final double[] HIGH={10,5000,100,150,300,30,60,100,100,3,10, 10,100,100,100,3,10};
    private static final String[] OPPONENTS={"search","search","greedy","random","legacy"};
    private GeneticTuner() { }
    private static final class Individual {
        double[] genes; double fitness;
        Individual(double[] g){genes=g.clone();fitness=Double.NaN;}
    }
    private static Weights weights(double[] genes) {
        double[] w=new double[Weights.WHITE_COUNT],b=new double[Weights.BLACK_COUNT];
        System.arraycopy(genes,0,w,0,w.length);System.arraycopy(genes,w.length,b,0,b.length);
        return new Weights(w,b);
    }
    private static double[] genes(Weights w) {
        double[] g=new double[GENES];System.arraycopy(w.white,0,g,0,w.white.length);System.arraycopy(w.black,0,g,w.white.length,w.black.length);return g;
    }
    private static double clip(double v,int i){return Math.max(LOW[i],Math.min(HIGH[i],v));}

    public static void main(String[] args) throws Exception {
        int population=args.length>0?Integer.parseInt(args[0]):8;
        int generations=args.length>1?Integer.parseInt(args[1]):4;
        int gamesEach=args.length>2?Integer.parseInt(args[2]):8;
        long nodes=args.length>3?Long.parseLong(args[3]):512;
        long seed=args.length>4?Long.parseLong(args[4]):20261120L;
        int moveCap=args.length>5?Integer.parseInt(args[5]):200;
        File directory=new File(args.length>6?args[6]:"docs/tuning");
        String resume=args.length>7?args[7]:"";
        if(population<4||generations<1||gamesEach<2||(gamesEach&1)!=0)throw new IllegalArgumentException("population >= 4, generations >= 1 and even gamesEach >= 2 required");
        if(!directory.exists()&&!directory.mkdirs())throw new IllegalStateException("cannot create "+directory);
        Random random=new Random(seed);List<Individual> current=new ArrayList<Individual>();int firstGeneration=0;Individual champion=null;
        if(resume.length()>0) {
            File f=new File(resume);firstGeneration=load(f,current)+1;
            if(current.size()!=population)throw new IllegalArgumentException("resume population size differs from requested population");
            for(Individual x:current)if(champion==null||x.fitness>champion.fitness){champion=new Individual(x.genes);champion.fitness=x.fitness;}
            System.out.println("Resumed generation "+(firstGeneration-1)+" from "+f);
        } else {
            current.add(new Individual(genes(Weights.defaults())));
            double[] base=genes(Weights.defaults());
            for(int n=1;n<population;n++) {
                double[] g=base.clone();
                for(int i=0;i<GENES;i++)g[i]=clip(g[i]+random.nextGaussian()*Math.max(1.0,Math.abs(g[i])*0.22),i);
                current.add(new Individual(g));
            }
        }
        File statsFile=new File(directory,"stats.csv");
        PrintWriter stats=new PrintWriter(new FileWriter(statsFile,firstGeneration>0));
        if(firstGeneration==0)stats.println("generation,best,mean,champion_index,seed,games_per_individual,node_budget");
        for(int gen=firstGeneration;gen<firstGeneration+generations;gen++) {
            Weights previousBest=champion==null?Weights.defaults():weights(champion.genes);
            double total=0;Individual best=null;int bestIndex=0;
            for(int i=0;i<current.size();i++) {
                Individual x=current.get(i);x.fitness=fitness(x,previousBest,gen,i,seed,gamesEach,nodes,moveCap);
                total+=x.fitness;if(best==null||x.fitness>best.fitness){best=x;bestIndex=i;}
                System.out.println("generation="+gen+" individual="+i+" fitness="+x.fitness);
            }
            if(champion==null||best.fitness>champion.fitness){champion=new Individual(best.genes);champion.fitness=best.fitness;}
            double mean=total/current.size();
            stats.println(gen+","+best.fitness+","+mean+","+bestIndex+","+seed+","+(gamesEach*OPPONENTS.length)+","+nodes);stats.flush();
            savePopulation(new File(directory,"population_gen"+gen+".csv"),gen,current,best);
            saveWeights(new File(directory,"best_weights.csv"),champion,gen);
            exportJava(new File(directory,"champion/Weights.java"),champion);
            System.out.println("generation="+gen+" best="+best.fitness+" mean="+mean+" champion="+champion.fitness);
            List<Individual> ranked=new ArrayList<Individual>(current);Collections.sort(ranked,new Comparator<Individual>(){public int compare(Individual a,Individual b){return Double.compare(b.fitness,a.fitness);}});
            List<Individual> next=new ArrayList<Individual>();next.add(new Individual(ranked.get(0).genes));
            double sigma=0.16*Math.pow(0.78,gen-firstGeneration);
            while(next.size()<population) {
                Individual p1=select(ranked,random),p2=select(ranked,random);double[] child=new double[GENES];double blend=random.nextDouble();
                for(int k=0;k<GENES;k++){child[k]=clip(blend*p1.genes[k]+(1.0-blend)*p2.genes[k],k);if(random.nextDouble()<0.28){double scale=Math.max(1.0,HIGH[k]-LOW[k]);child[k]=clip(child[k]+random.nextGaussian()*scale*sigma,k);}}
                next.add(new Individual(child));
            }
            current=next;
        }
        stats.close();
        System.out.println("Champion exported to "+new File(directory,"champion/Weights.java").getPath()+" weights="+new File(directory,"best_weights.csv").getPath());
    }
    private static Individual select(List<Individual> ranked,Random r) {
        Individual best=null;for(int i=0;i<3;i++){Individual candidate=ranked.get(r.nextInt(ranked.size()));if(best==null||candidate.fitness>best.fitness)best=candidate;}return best;
    }
    private static double fitness(Individual x,Weights previousBest,int gen,int index,long seed,int gamesEach,long nodes,int cap) throws Exception {
        double points=0;int played=0;long baseSeed=seed+0x9e3779b97f4a7c15L*(gen*1000L+index);
        for(int o=0;o<OPPONENTS.length;o++) {
            SelfPlay.AgentSpec candidate=new SelfPlay.AgentSpec("search",weights(x.genes),nodes);
            Weights opponentWeights=o==1?previousBest:Weights.defaults();
            SelfPlay.AgentSpec opponent=new SelfPlay.AgentSpec(OPPONENTS[o],opponentWeights,nodes);
            SelfPlay.Summary s=SelfPlay.run(candidate,opponent,gamesEach,baseSeed+o*7919L,cap,0,2);
            points+=s.aPoints;played+=gamesEach*3;
        }
        return points/(double)played;
    }
    private static void savePopulation(File f,int generation,List<Individual> population,Individual best) throws Exception {
        PrintWriter out=new PrintWriter(new FileWriter(f));out.println("generation,"+generation);
        for(int i=0;i<population.size();i++){Individual x=population.get(i);out.print(i+","+x.fitness);for(double g:x.genes)out.print(","+g);out.println();}out.close();
    }
    private static int load(File f,List<Individual> population) throws Exception {
        BufferedReader in=new BufferedReader(new FileReader(f));String line=in.readLine();if(line==null||!line.startsWith("generation,"))throw new IllegalArgumentException("invalid resume file");int gen=Integer.parseInt(line.substring(11));
        while((line=in.readLine())!=null){String[] p=line.split(",");if(p.length!=GENES+2)throw new IllegalArgumentException("invalid population row");double[] g=new double[GENES];for(int i=0;i<GENES;i++)g[i]=Double.parseDouble(p[i+2]);Individual x=new Individual(g);x.fitness=Double.parseDouble(p[1]);population.add(x);}in.close();return gen;
    }
    private static void saveWeights(File f,Individual x,int gen) throws Exception {
        PrintWriter out=new PrintWriter(new FileWriter(f));out.println("generation,"+gen);out.println("white");for(int i=0;i<Weights.WHITE_COUNT;i++)out.println(Weights.WHITE_NAMES[i]+","+x.genes[i]);out.println("black");for(int i=0;i<Weights.BLACK_COUNT;i++)out.println(Weights.BLACK_NAMES[i]+","+x.genes[Weights.WHITE_COUNT+i]);out.close();
    }
    private static void exportJava(File f,Individual x) throws Exception {
        File parent=f.getParentFile();if(!parent.exists()&&!parent.mkdirs())throw new IllegalStateException("cannot create "+parent);
        PrintWriter out=new PrintWriter(new FileWriter(f));out.println("package it.unibo.ai.didattica.competition.tablut.alielegio;");out.println();out.println("/** Generated by GeneticTuner; copy these arrays into the runtime Weights defaults. */");out.println("public final class Weights {");out.println("    public static final int WHITE_COUNT=11, BLACK_COUNT=6;");out.println("    public static final double[] CHAMPION_WHITE={");
        array(out,x.genes,0,Weights.WHITE_COUNT);out.println("    };\n    public static final double[] CHAMPION_BLACK={");array(out,x.genes,Weights.WHITE_COUNT,GENES);out.println("    };\n    private Weights() { }\n}");out.close();
    }
    private static void array(PrintWriter out,double[] g,int from,int to) {for(int i=from;i<to;i++){out.print("        "+Double.toString(g[i]));if(i+1<to)out.print(",");out.println();}}
}
