package it.unibo.ai.didattica.competition.tablut.alielegio;

import java.util.Locale;
import it.unibo.ai.didattica.competition.tablut.alielegio.engine.Board;
import it.unibo.ai.didattica.competition.tablut.alielegio.engine.Converter;
import it.unibo.ai.didattica.competition.tablut.alielegio.engine.MoveGen;
import it.unibo.ai.didattica.competition.tablut.alielegio.engine.Rules;
import it.unibo.ai.didattica.competition.tablut.client.TablutClient;
import it.unibo.ai.didattica.competition.tablut.domain.Action;
import it.unibo.ai.didattica.competition.tablut.domain.State;
import it.unibo.ai.didattica.competition.tablut.domain.State.Turn;

/** Network loop for the tournament player. */
public final class OurClient extends TablutClient {
    public static final String TEAM_NAME="AliEleGio";
    private final Search search;
    private final int timeoutSeconds;
    private final boolean verbose;

    public OurClient(String role,String name,int timeoutSeconds,String serverIp,boolean verbose) throws Exception {
        super(normalizeRole(role),validateName(name),validateTimeout(timeoutSeconds),validateIp(serverIp));
        this.timeoutSeconds=timeoutSeconds;
        this.verbose=verbose;
        this.search=new Search(new Evaluator(Weights.defaults()));
    }

    public OurClient(String role,String name,int timeoutSeconds,String serverIp) throws Exception {
        this(role,name,timeoutSeconds,serverIp,Boolean.getBoolean("alielegio.verbose"));
    }

    @Override
    public void run() {
        try {
            declareName();
            final Turn me=getPlayer();
            while(true) {
                read(); // TablutClient.read blocks until the server sends a state.
                State state=getCurrentState();
                if(state==null)throw new IllegalStateException("server returned no state");
                Turn turn=state.getTurn();
                if(isTerminal(turn))return;
                if(turn!=me)continue;

                Board board=Converter.fromState(state);
                long budgetMs=Search.budgetForTimeoutSeconds(timeoutSeconds);
                int move=-1;
                try {
                    Action chosen=search.bestMove(state,budgetMs);
                    if(chosen!=null)move=moveFromAction(chosen,board);
                } catch(Throwable searchFailure) {
                    if(verbose)System.err.println("search failed; using legal fallback: "+searchFailure.getClass().getSimpleName());
                }

                // The generator is the legality oracle and also provides a cheap guaranteed fallback.
                move=safeMove(board,move);
                if(move<0) {
                    if(verbose)System.err.println("no legal move available");
                    return;
                }
                if(!MoveGen.isLegal(board,move)) {
                    if(verbose)System.err.println("engine legality guard rejected move");
                    return;
                }
                Action action=Converter.toAction(move,board.turn);
                write(action);
                if(verbose)System.out.println("move="+action.getFrom()+"-"+action.getTo()+" role="+me);
            }
        } catch(Throwable failure) {
            if(verbose)System.err.println("client stopped: "+failure.getClass().getSimpleName()+": "+failure.getMessage());
        }
    }

    public static int firstLegalMove(Board board) {
        int[] moves=new int[1024];
        int count=MoveGen.generate(board,moves);
        return count==0?-1:moves[0];
    }

    /** Returns the proposed legal move, or a generated legal fallback. */
    public static int safeMove(Board board,int proposed) {
        return proposed>=0&&MoveGen.isLegal(board,proposed)?proposed:firstLegalMove(board);
    }

    private static int moveFromAction(Action action,Board board) {
        if(action==null||action.getFrom()==null||action.getTo()==null)return -1;
        int from=parseSquare(action.getFrom()),to=parseSquare(action.getTo());
        if(from<0||to<0)return -1;
        int move=(from<<7)|to;
        return MoveGen.isLegal(board,move)?move:-1;
    }

    private static int parseSquare(String box) {
        if(box.length()!=2)return -1;
        char file=Character.toLowerCase(box.charAt(0));char rank=box.charAt(1);
        if(file<'a'||file>'i'||rank<'1'||rank>'9')return -1;
        return Rules.index(rank-'1',file-'a');
    }

    private static boolean isTerminal(Turn turn) {
        return turn==Turn.WHITEWIN||turn==Turn.BLACKWIN||turn==Turn.DRAW;
    }
    public static String normalizeRole(String role) {
        if(role==null)throw new IllegalArgumentException("role must be WHITE or BLACK");
        String normalized=role.trim().toUpperCase(Locale.ROOT);
        if(!"WHITE".equals(normalized)&&!"BLACK".equals(normalized))throw new IllegalArgumentException("role must be WHITE or BLACK");
        return normalized;
    }
    public static int parseTimeout(String timeout) {
        int seconds=Integer.parseInt(timeout);
        return validateTimeout(seconds);
    }
    private static String validateName(String name) {
        if(name==null||name.trim().length()==0)throw new IllegalArgumentException("team name is required");
        return name;
    }
    private static int validateTimeout(int timeout) {
        if(timeout<1)throw new IllegalArgumentException("timeout must be positive seconds");
        return timeout;
    }
    private static String validateIp(String ip) {
        if(ip==null||ip.trim().length()==0)throw new IllegalArgumentException("server IP is required");
        return ip.trim();
    }

    private static boolean verboseFromArgs(String[] args) {
        return Boolean.getBoolean("alielegio.verbose")||(args.length>3&&Boolean.parseBoolean(args[3]));
    }
    public static void main(String[] args) {
        if(args==null||args.length<3||args.length>4) {
            System.err.println("Usage: OurClient <WHITE|BLACK> <timeout-seconds> <server-ip> [verbose]");
            return;
        }
        try {
            String role=normalizeRole(args[0]);
            int timeout=parseTimeout(args[1]);
            validateIp(args[2]);
            new OurClient(role,TEAM_NAME,timeout,args[2],verboseFromArgs(args)).run();
        } catch(Throwable failure) {
            System.err.println("AliEleGio client startup failed: "+failure.getMessage());
        }
    }
}
