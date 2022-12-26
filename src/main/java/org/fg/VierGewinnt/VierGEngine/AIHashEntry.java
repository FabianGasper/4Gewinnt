package org.fg.VierGewinnt.VierGEngine;

public class AIHashEntry {
    public enum HashFlag{EVALUATED, ALPHA_HINT, BETA_HINT}
    public final long red;
    public final long yellow;
    public final int depth;
    public final int value;
    public final int best;
    public final HashFlag flag;

    public AIHashEntry(long red, long yellow, int depth, int value, int best, HashFlag flag) {
        this.red = red;
        this.yellow = yellow;
        this.depth = depth;
        this.value = value;
        this.best = best;
        this.flag = flag;
    }
}
