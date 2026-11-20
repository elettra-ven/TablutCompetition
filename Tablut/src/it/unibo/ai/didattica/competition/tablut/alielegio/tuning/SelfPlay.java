package it.unibo.ai.didattica.competition.tablut.alielegio.tuning;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import it.unibo.ai.didattica.competition.tablut.alielegio.Evaluator;
import it.unibo.ai.didattica.competition.tablut.alielegio.Search;
import it.unibo.ai.didattica.competition.tablut.alielegio.Weights;
import it.unibo.ai.didattica.competition.tablut.alielegio.baseline.GreedyPlayer;
import it.unibo.ai.didattica.competition.tablut.alielegio.baseline.OldPlayer;
import it.unibo.ai.didattica.competition.tablut.alielegio.baseline.RandomPlayer;
import it.unibo.ai.didattica.competition.tablut.alielegio.engine.Board;
import it.unibo.ai.didattica.competition.tablut.alielegio.engine.Converter;
import it.unibo.ai.didattica.competition.tablut.alielegio.engine.Move;
import it.unibo.ai.didattica.competition.tablut.alielegio.engine.MoveGen;
import it.unibo.ai.didattica.competition.tablut.alielegio.engine.Rules;
import it.unibo.ai.didattica.competition.tablut.domain.Action;
import it.unibo.ai.didattica.competition.tablut.domain.GameAshtonTablut;
import it.unibo.ai.didattica.competition.tablut.domain.State;
import it.unibo.ai.didattica.competition.tablut.domain.State.Turn;
import it.unibo.ai.didattica.competition.tablut.domain.StateTablut;

/** Deterministic headless games using only the AliEleGio engine. */
public final class SelfPlay {
    public static final int DEFAULT_MOVE_CAP=300;
    private static final int WORKERS=4;
    private SelfPlay() { }

    public static final class AgentSpec {
        public final String type;
        public final Weights weights;
        public final long nodeBudget;
        public final boolean kingThreatExtension;
        public AgentSpec(String type,Weights weights,long nodeBudget) { this(type,weights,nodeBudget,true); }
        public AgentSpec(String type,Weights weights,long nodeBudget,boolean threatExtension) {
            if(!"search".equals(type)&&!"random".equals(type)&&!"greedy".equals(type)&&!"old".equals(type))throw new IllegalArgumentException("agent must be search, random, greedy, or old");
            this.type=type;this.weights=weights==null?Weights.defaults():weights.copy();this.nodeBudget=Math.max(1,nodeBudget);this.kingThreatExtension=threatExtension;
        }
        public AgentSpec(String type,long nodeBudget){this(type,Weights.defaults(),nodeBudget);}
        public static AgentSpec defaults(String type,long nodes){return new AgentSpec(type,Weights.defaults(),nodes);}
    }

    public static final class Summary {
        public int aWins,draws,bWins,checkedGames,outcomeCheckedGames;
        public int aWhiteWins,aWhiteDraws,aWhiteLosses,aBlackWins,aBlackDraws,aBlackLosses;
        public long aPoints,bPoints,elapsedNanos,nodes;
        public double gamesPerHour;
    }

    private static final class GameResult {
        final int index,outcome;
        final int[] moves;
        final int moveCount;
        final Board finalBoard;
        final boolean capped,noMove;
        final long nodes;
        GameResult(int i,int o,int[] m,int n,Board b,boolean cap,boolean nm,long count){index=i;outcome=o;moves=m;moveCount=n;finalBoard=b;capped=cap;noMove=nm;nodes=count;}
    }
    private interface Player { int choose(Board board);void reset(long seed);long nodes(); }
    private static final class SearchAgent implements Player {
        final Search search;final long nodeBudget;long totalNodes;
        SearchAgent(AgentSpec spec){search=new Search(new Evaluator(spec.weights));search.setKingThreatExtension(spec.kingThreatExtension);nodeBudget=spec.nodeBudget;}
        public int choose(Board b){int m=search.bestMoveNodes(b,nodeBudget);totalNodes+=search.nodes;return m;}
        public void reset(long seed) { totalNodes=0; }
        public long nodes(){return totalNodes;}
    }
    private static final class RandomAgent implements Player {
        final RandomPlayer random=new RandomPlayer(1);
        public int choose(Board b){return random.choose(b);}
        public void reset(long seed){random.setSeed(seed);}
        public long nodes(){return 0;}
    }
    private static final class GreedyAgent implements Player {
        final GreedyPlayer greedy=new GreedyPlayer();
        public int choose(Board b){return greedy.choose(b);}
        public void reset(long seed) { }
        public long nodes(){return 0;}
    }
    private static final class OldAgent implements Player {
        final OldPlayer old=new OldPlayer();
        public int choose(Board b){return old.choose(b);}
        public void reset(long seed) { }
        public long nodes(){return 0;}
    }
    private static final class Worker {
        final AgentSpec a,b;final Player playerA,playerB;
        Worker(AgentSpec a,AgentSpec b){this.a=a;this.b=b;playerA=create(a);playerB=create(b);}
        GameResult play(int index,long seed,int cap,int openingPlies) {
            long gameSeed=mix(seed+0x9e3779b97f4a7c15L*index);
            playerA.reset(gameSeed);playerB.reset(mix(gameSeed));
            boolean aWhite=(index&1)==0;
            Player white=aWhite?playerA:playerB,black=aWhite?playerB:playerA;
            Board board=Board.initial();int[] moves=new int[cap];int count=0;long nodes=0;boolean noMove=false,capped=false;MoveGen.Undo undo=new MoveGen.Undo();
            java.util.Random openingRandom=new java.util.Random(mix(gameSeed^0x414c49454c454749L));
            while(!board.terminal()&&count<cap) {
                Player current=board.turn==Rules.WHITE_TURN?white:black;
                int move;
                if(count<openingPlies) {
                    int[] legal=new int[1024];int legalCount=MoveGen.generate(board,legal);
                    if(legalCount==0)move=-1;else move=legal[openingRandom.nextInt(legalCount)];
                } else {
                    long before=current.nodes();move=current.choose(board);nodes+=Math.max(0,current.nodes()-before);
                }
                if(move<0) { noMove=true;board.turn=board.turn==Rules.WHITE_TURN?Rules.BLACK_WIN:Rules.WHITE_WIN;board.result=board.turn;break; }
                moves[count++]=move;MoveGen.make(board,move,undo);
            }
            if(!board.terminal())capped=true;
            int outcome=capped?Rules.DRAW:board.result;
            return new GameResult(index,outcome,moves,count,new Board(board),capped,noMove,nodes);
        }
    }

    private static Player create(AgentSpec spec) {
        if("search".equals(spec.type))return new SearchAgent(spec);
        if("random".equals(spec.type))return new RandomAgent();
        if("greedy".equals(spec.type))return new GreedyAgent();
        return new OldAgent();
    }

    public static Summary run(AgentSpec a,AgentSpec b,int games,long seed,int moveCap,int crossChecks) throws Exception {
        return run(a,b,games,seed,moveCap,crossChecks,0);
    }

    public static Summary run(AgentSpec a,AgentSpec b,int games,long seed,int moveCap,int crossChecks,final int openingPlies) throws Exception {
        if(games<2||(games&1)!=0)throw new IllegalArgumentException("games must be a positive even number");
        if(moveCap<1)throw new IllegalArgumentException("move cap must be positive");
        int workerCount=Math.min(WORKERS,games);GameResult[] results=new GameResult[games];
        ExecutorService pool=Executors.newFixedThreadPool(workerCount);List<Future<?>> futures=new ArrayList<Future<?>>();
        long start=System.nanoTime();
        for(int w=0;w<workerCount;w++) {
            final int workerIndex=w;
            futures.add(pool.submit(new Runnable(){public void run(){
                Worker worker=new Worker(a,b);
                for(int i=workerIndex;i<games;i+=workerCount)results[i]=worker.play(i,seed,moveCap,openingPlies);
            }}));
        }
        try { for(Future<?> f:futures)f.get(); }
        finally {pool.shutdownNow();}
        long elapsed=System.nanoTime()-start;Summary summary=new Summary();summary.elapsedNanos=elapsed;
        for(int i=0;i<games;i++) {
            GameResult g=results[i];summary.nodes+=g.nodes;
            boolean aWhite=(i&1)==0;
            if(g.outcome==Rules.DRAW){summary.draws++;summary.aPoints++;summary.bPoints++;if(aWhite)summary.aWhiteDraws++;else summary.aBlackDraws++;}
            else {
                boolean whiteWon=g.outcome==Rules.WHITE_WIN;
                boolean aWon=(whiteWon==aWhite);
                if(aWon){summary.aWins++;summary.aPoints+=3;if(aWhite)summary.aWhiteWins++;else summary.aBlackWins++;}
                else{summary.bWins++;summary.bPoints+=3;if(aWhite)summary.aWhiteLosses++;else summary.aBlackLosses++;}
            }
        }
        summary.gamesPerHour=games/(elapsed/3600000000000.0);
        if(crossChecks>0) {
            int[] checked=crossCheck(results,Math.min(crossChecks,games));summary.checkedGames=checked[0];summary.outcomeCheckedGames=checked[1];
        }
        return summary;
    }

    private static int[] crossCheck(GameResult[] games,int limit) throws Exception {
        PrintStream original=System.out;System.setOut(new PrintStream(new OutputStream(){public void write(int b){}},true,"US-ASCII"));
        int checked=0,outcomes=0;
        try {
            File logs=new File(System.getProperty("java.io.tmpdir"),"alielegio-selfplay-oracle");
            for(int i=0;i<limit;i++) {
                GameResult game=games[i];if(game.noMove)continue;
                StateTablut state=new StateTablut();state.setTurn(Turn.WHITE);
                GameAshtonTablut oracle=new GameAshtonTablut(state.clone(),0,-1,logs.getAbsolutePath(),"selfplay","oracle");
                Logger logger=Logger.getLogger("GameLog");logger.setLevel(Level.OFF);
                Handler[] handlers=logger.getHandlers();for(Handler handler:handlers){logger.removeHandler(handler);handler.close();}
                for(int m=0;m<game.moveCount;m++) {
                    int move=game.moves[m];byte side=state.getTurn()==Turn.BLACK?Rules.BLACK_TURN:Rules.WHITE_TURN;
                    Action action=Converter.toAction(move,side);
                    try {state=(StateTablut)oracle.checkMove(state,action);}
                    catch(Exception e){throw new AssertionError("oracle rejected game "+i+" move "+m+" "+action+"\n"+state,e);}
                }
                compareState(state,game.finalBoard,i,!game.capped);
                checked++;
                if(!game.capped)outcomes++;
            }
        } finally {System.setOut(original);}
        return new int[]{checked,outcomes};
    }
    private static void compareState(State state,Board board,int game,boolean compareTurn) {
        for(int r=0;r<9;r++)for(int c=0;c<9;c++) {
            State.Pawn p=state.getPawn(r,c);byte value=p==State.Pawn.WHITE?Rules.WHITE:p==State.Pawn.BLACK?Rules.BLACK:p==State.Pawn.KING?Rules.KING:p==State.Pawn.THRONE?Rules.THRONE:Rules.EMPTY;
            if(value!=board.cells[Rules.index(r,c)])throw new AssertionError("cross-check board mismatch game "+game+" at "+state.getBox(r,c));
        }
        byte turn=state.getTurn()==Turn.WHITE?Rules.WHITE_TURN:state.getTurn()==Turn.BLACK?Rules.BLACK_TURN:state.getTurn()==Turn.WHITEWIN?Rules.WHITE_WIN:state.getTurn()==Turn.BLACKWIN?Rules.BLACK_WIN:Rules.DRAW;
        if(compareTurn&&turn!=board.turn)throw new AssertionError("cross-check outcome mismatch game "+game+" oracle="+state.getTurn()+" engine="+board.turn);
    }

    private static long mix(long z){z=(z^(z>>>30))*0xbf58476d1ce4e5b9L;z=(z^(z>>>27))*0x94d049bb133111ebL;return z^(z>>>31);}
    private static AgentSpec spec(String name,long nodes,boolean extension){return new AgentSpec(name,Weights.defaults(),nodes,extension);}

    public static void main(String[] args) throws Exception {
        String a=args.length>0?args[0]:"search",b=args.length>1?args[1]:"greedy";
        int games=args.length>2?Integer.parseInt(args[2]):100;
        long nodes=args.length>3?Long.parseLong(args[3]):512;
        long seed=args.length>4?Long.parseLong(args[4]):20261120L;
        int cap=args.length>5?Integer.parseInt(args[5]):DEFAULT_MOVE_CAP;
        int checks=args.length>6?Integer.parseInt(args[6]):Math.min(8,games);
        int openingPlies=args.length>7?Integer.parseInt(args[7]):0;
        boolean extensionA=args.length<=8||Boolean.parseBoolean(args[8]);
        boolean extensionB=args.length<=9||Boolean.parseBoolean(args[9]);
        Summary s=run(spec(a,nodes,extensionA),spec(b,nodes,extensionB),games,seed,cap,checks,openingPlies);
        System.out.println("SelfPlay agents="+a+"/"+b+" games="+games+" seed="+seed+" nodeBudget="+nodes+" moveCap="+cap+" openingPlies="+openingPlies);
        System.out.println("A(wins/draws/losses)="+s.aWins+"/"+s.draws+"/"+s.bWins+" points="+s.aPoints+" Bpoints="+s.bPoints+" nodes="+s.nodes+" gamesPerHour="+(long)s.gamesPerHour+" crossChecked="+s.checkedGames+" outcomeChecked="+s.outcomeCheckedGames);
        System.out.println("A as White="+s.aWhiteWins+"/"+s.aWhiteDraws+"/"+s.aWhiteLosses+" A as Black="+s.aBlackWins+"/"+s.aBlackDraws+"/"+s.aBlackLosses);
    }
}
