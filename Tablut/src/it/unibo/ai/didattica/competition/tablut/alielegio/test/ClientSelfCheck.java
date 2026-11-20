package it.unibo.ai.didattica.competition.tablut.alielegio.test;

import it.unibo.ai.didattica.competition.tablut.alielegio.OurClient;
import it.unibo.ai.didattica.competition.tablut.alielegio.Weights;
import it.unibo.ai.didattica.competition.tablut.alielegio.engine.Board;
import it.unibo.ai.didattica.competition.tablut.alielegio.engine.MoveGen;

/** In-process checks for startup parsing and the no-illegal-move guard. */
public final class ClientSelfCheck {
    private ClientSelfCheck() { }
    public static void main(String[] args) {
        require("WHITE".equals(OurClient.normalizeRole("wHitE")),"mixed-case white role");
        require("BLACK".equals(OurClient.normalizeRole("bLaCk")),"mixed-case black role");
        require(OurClient.parseTimeout("5")==5,"timeout parsing");
        boolean rejected=false;
        try { OurClient.normalizeRole("W"); } catch(IllegalArgumentException expected) { rejected=true; }
        require(rejected,"invalid role rejection");
        rejected=false;
        try { OurClient.parseTimeout("0"); } catch(IllegalArgumentException expected) { rejected=true; }
        require(rejected,"non-positive timeout rejection");

        Board board=Board.initial();
        int fallback=OurClient.safeMove(board,-1);
        require(fallback>=0&&MoveGen.isLegal(board,fallback),"legal fallback");
        int legal=OurClient.safeMove(board,fallback);
        require(legal==fallback,"preserve legal search result");
        int illegal=OurClient.safeMove(board,(80<<7)|0);
        require(illegal>=0&&MoveGen.isLegal(board,illegal),"illegal proposal replaced");
        require(Math.abs(Weights.defaults().white[1]-1513.350347131091)<0.000001,"champion defaults loaded");
        System.out.println("ClientSelfCheck OK: role, timeout, fallback, legality guard, champion weights");
    }
    private static void require(boolean condition,String label) {
        if(!condition)throw new AssertionError(label);
    }
}
