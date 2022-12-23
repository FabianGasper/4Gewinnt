package org.fg.VierGewinnt.VierGEngine;

import org.fg.VierGewinnt.VierGModel.Board;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.FutureTask;


public class AI {
    //knapp kleiner als Integer.MAX_VALUE damit wir beim Multiplizieren keine Overflows bekommen
    private static final int MAX_INT = Integer.MAX_VALUE - 100;

    private final AISettings settings;
    private final Board sb;
    private final Map<Integer, long[]> map;

    private final int[] lastBest;
    private int lastValue = 0;
    private int lastChoice = 0;

    public AI(AISettings s, Board sb) {
        settings = s;
        this.sb = sb;

        //Wenn wir die Map nicht brauchen, reservieren wir keinen massiven Speicherplatz
        if (s.useHash) {
            map = new HashMap<>(1048576);  //2^20 a 4 long ~32MB
        } else {
            map = new HashMap<>();
        }
        int currDepth = settings.maxDepth;
        this.lastBest = new int[currDepth];
        for (int i = 0; i < currDepth; i++) {
            lastBest[i] = 3;
        }
    }

    public AISettings getSettings() {
        return settings;
    }

    public int getLastValue() {
        return lastValue;
    }

    public int getLastChoice() {
        return lastChoice;
    }

    public int advance(FutureTask<?> t) {

        int currDepth = settings.maxDepth;

        try {
            lastValue = alphaBeta(sb.getRed(), sb.getYellow(), sb.getStat(), sb.getTurn(), currDepth, -MAX_INT, MAX_INT, lastBest, -1, -1, t);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(Arrays.toString(lastBest));
        lastChoice = lastBest[currDepth - 1];

        //lastBest speichert die bestbewertete Zugfolge aus dem letzten Lauf.
        //für den nächsten Lauf verschieben wir um 2, um mit dem Wert zu starten,
        //der laut letzter Zugfolge nun dran wäre
        for (int i = currDepth - 1; i >= 0; i--) {
            if (i > 1) {
                lastBest[i] = lastBest[i - 2];
            } else {
                lastBest[i] = 3;
            }
        }
        //Der nächste beste Wert darf keine volle Spalte sein, sonst kann es sein, dass versucht wird in die
        //volle Spalte einzuwerfen, wenn alle Alternativen durch AB-Pruning abgeschnitten werden.
        for (int i : new int[]{lastBest[currDepth - 1], 3, 2, 4, 1, 5, 0, 6}) {
            if (!AIBitAlgebra.isFull(sb.getRed(), sb.getYellow(), i)) {
                lastBest[currDepth - 1] = i;
                break;
            }
        }
        return lastChoice;
    }

    public int alphaBeta(
            long r,             //rote Steine
            long y,             //gelbe Steine
            int stat,           //Belegungstiefe (performance-verbesserung)
            boolean turn,       //rot (Spieler 1) am Zug?
            int depth,          //aktuelle Tiefe im Spielbaum
            int alpha,          //obere Schranke beim AB-Pruning
            int beta,           //untere Schranke beim AB-Pruning
            int[] bestMoves,    //Rückgabe der besten Züge im Sub-Tree die zu dieser Bewertung geführt hat
            int last_col,       //letzter Wurf col (performance für isWon)
            int last_row,
            FutureTask<?> t) {  //Task zum Stoppen des Threads - wird auch zur Zeitbegrenzung verwendet.

        int hash = 0;
        if (settings.useHash) {
            hash = AIBitAlgebra.getHash(r, y);
            if (map.containsKey(hash)) {
                long[] res = map.get(hash);
                if (res[0] == r && res[1] == y && depth <= res[2]) {
                    return (int) res[3];
                }
            }
        }

        if (settings.useABPruning) alpha = -MAX_INT;
        boolean stop = false;
        int value = 0;


        if (last_col >= 0 && AIBitAlgebra.isWon(turn ? y : r, last_col, last_row)) {
            if (AIBitAlgebra.count(r, y) != AIBitAlgebra.count(stat)) System.out.println("Fehler in count");
            value = -100000000 + AIBitAlgebra.count(stat);
            stop = true;
        }
        if (AIBitAlgebra.isDrawNotWon(r, y) != AIBitAlgebra.isDrawNotWon(stat)) System.out.println("Fehler in dnw");

        if (!stop && AIBitAlgebra.isDrawNotWon(stat)) {
            stop = true;
        }
        if (!stop && depth <= 0) {
            value = (turn ? 1 : -1) * AIBitAlgebra.eval(r, y, settings.useStatWeight);
            stop = true;
        }

        if (stop) {
            if (settings.useHash) {
                map.put(hash, new long[]{r, y, depth, value});
            }
            return value;
        }


        int oldBest = bestMoves[depth - 1];
        int[] moves = {oldBest, 3, 2, 4, 1, 5, 0, 6};
        int[] subBest = Arrays.copyOfRange(bestMoves, 0, depth - 1);

        for (int j : moves) {

            if (AIBitAlgebra.isFull(r, y, j) != AIBitAlgebra.isFull(stat, j)) System.out.println("Fehler in isFull");
            if (AIBitAlgebra.getRowPos(r, y, j) != AIBitAlgebra.getRowPos(stat, j)) System.out.println("Fehler in dnw");


            if (AIBitAlgebra.isFull(stat, j)) continue;
            int row = AIBitAlgebra.getRowPos(stat, j) + 1;
            long new_val = AIBitAlgebra.set(turn ? r : y, j, row);
            int new_stat = AIBitAlgebra.addStat(stat, j);
            value = -alphaBeta(turn ? new_val : r, turn ? y : new_val, new_stat, !turn, depth - 1, -beta, -alpha, subBest, j, row, t);


            if (settings.useABPruning && value >= beta)
                return beta;

            if (value > alpha) {
                alpha = value;
                bestMoves[depth - 1] = j;
                System.arraycopy(subBest, 0, bestMoves, 0, depth - 1);
            }

            //Wenn Thread abgebrochen wird, wird der aktuell beste Wert
            //nach Unten propagiert. Die Rekursion wird gestoppt.
            if (t.isCancelled()) return alpha;
        }
        return alpha;
    }
}
