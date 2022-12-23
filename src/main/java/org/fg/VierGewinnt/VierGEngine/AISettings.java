package org.fg.VierGewinnt.VierGEngine;

public class AISettings {
    //Ich bin nicht sicher, wie gut das in Java funktioniert, aber ich will dem
    //Compiler klarmachen, dass sich die variablen nicht ändern und im Register bleiben können.
    //Denn die Variablen werden von jedem Knoten und somit millionenfach abgefragt.
    public final int tLimit;
    public final int maxDepth;
    public final int numThreads;
    public final boolean useHash;
    public final boolean useABPruning;
    public final boolean usePreSort;
    public final boolean useStatWeight;

    public AISettings(AISettingsParameter s) {
        tLimit = s.tLimit;
        maxDepth = s.maxDepth;
        numThreads = s.numThreads;
        useHash = s.useHash;
        useABPruning = s.useABPruning;
        usePreSort = s.usePreSort;
        useStatWeight = s.useStatWeight;
    }
}
