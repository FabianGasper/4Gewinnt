package org.fg.VierGewinnt.VierGModel;

import org.fg.VierGewinnt.VierGEngine.AIBitAlgebra;

import java.util.ArrayList;
import java.util.List;

public class Board {

    private long red = 0L;
    private long yellow = 0L;
    private int stat = 0;
    private boolean turn = true;
    private long zKey =0;
    private final List<Integer> moveHist = new ArrayList<>();

    public boolean isFull(int i) {
        return AIBitAlgebra.isFull(red, yellow, i);
    }

    public void add(int i) {
        if (isFull(i)) return;
        int pos = AIBitAlgebra.getRowPos(red, yellow, i) + 1;
        if (turn) red = AIBitAlgebra.set(red, i, pos);
        else yellow = AIBitAlgebra.set(yellow, i, pos);
        stat = AIBitAlgebra.addStat(stat, i);
        zKey = AIBitAlgebra.getZHash(zKey,i,pos,turn);
        moveHist.add(i);
        turn = !turn;
    }


    public boolean getTurn() {
        return turn;
    }

    public long getRed() {
        return red;
    }

    public long getYellow() {
        return yellow;
    }

    public int getStat() {
        return stat;
    }

    public List<Integer> getMoveHist() {
        return moveHist;
    }

    public Stone get(int i, int j) {
        return AIBitAlgebra.get(red, yellow, i, j);
    }

    public boolean isDraw() {
        return AIBitAlgebra.isDraw(red, yellow);
    }

    public boolean isWon() {
        return AIBitAlgebra.isWon(red) || AIBitAlgebra.isWon(yellow);
    }

    public long getZKey() {
        return zKey;
    }
}
