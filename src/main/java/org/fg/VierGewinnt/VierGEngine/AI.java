package org.fg.VierGewinnt.VierGEngine;

import org.fg.VierGewinnt.VierGModel.Board;

import java.util.*;
import java.util.concurrent.FutureTask;

import static org.fg.VierGewinnt.VierGEngine.AIHashEntry.HashFlag.*;

public class AI {
    //knapp kleiner als Integer.MAX_VALUE damit wir beim Multiplizieren keine Overflows bekommen
    private static final int MAX_INT = Integer.MAX_VALUE - 100;
    private static final int[] initialMoveOrder = new int[]{3, 2, 4, 1, 5, 0, 6};

    private final AISettings settings;
    private final Board sb;
    private final Map<Integer, AIHashEntry> map;
    private final int[] lastBest;

    private int lastValue = 0;
    private int lastChoice = 0;

    private int c1 = 0, c2 = 0, c3 = 0;
    private long startTime;

    public AI(AISettings s, Board sb) {
        settings = s;
        this.sb = sb;

        //Wenn wir die Map nicht brauchen, reservieren wir keinen massiven Speicherplatz
        if (s.useHash) {
            map = new HashMap<>(1048576);  //2^20 a 4 long ~32MB
        } else {
            map = new HashMap<>();
        }
        lastBest = new int[settings.maxDepth];
        Arrays.fill(lastBest, initialMoveOrder[0]);
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
        //Wir ziehen 50ms ab um Reserve für Abarbeitung des letzten Knotens und der Rekursion zu haben.
        startTime = System.currentTimeMillis()-50;
        lastChoice=-1;
        //wir starten mit d=3 für das erste Zwischenergebnis
        for (int d=3 ; d<= getSettings().maxDepth; d++){
            System.out.println("run level: "+d);
            int[] bestMoves = prepareMoveOrder(d);
            lastValue = alphaBeta(sb.getRed(), sb.getYellow(), sb.getStat(), sb.getTurn(), d,
                    -MAX_INT, MAX_INT, bestMoves, -1, -1, sb.getZKey(), t);

            //Wenn der Lauf abgebrochen wird, dann holen wir uns nur den Zug aus dem letzten Lauf,
            //wenn es der initiale Lauf war.
            //Ansonsten vertrauen wir eher auf das Ergebnis aus dem Lauf-1
            if (t.isCancelled()){
                if(lastChoice>0)
                    break;
            }
            lastChoice = bestMoves[d - 1];
            //lastBest speichert die bestbewertete Zugfolge aus dem letzten Lauf, abzüglich der letzten 2,
            //da diese bis zum nächsten Zug verbraucht sind.
            for(int i=0; i<d ; i++){
                lastBest[(d-1)-i] = bestMoves[i];
            }
        }

        //System.out.println("set:" + c1 + " used:" + (c2 - c3) + " hint:" + c3 + " load:" + map.size());
        //System.out.println(Arrays.toString(lastBest));

        return lastChoice;
    }

    private int[] prepareMoveOrder(int d){
        int[] bestMoves = new int[d];
        //Die Liste der initialen besten Züge wird aus den besten Würfen des letzten Laufes initialisiert
        //und die fehlenden Stellen mit dem Default-Erst-Zug initialisiert.
        for(int i=0; i<d ; i++){
            if(settings.usePreSort)
                bestMoves[i] = lastBest[(d-1)-i];
            else
                bestMoves[i] = initialMoveOrder[0];

            //Der nächste beste Wert darf keine volle Spalte sein, sonst kann es sein, dass versucht wird in die
            //volle Spalte einzuwerfen, wenn alle Alternativen durch AB-Pruning abgeschnitten werden.
            if (AIBitAlgebra.isFull(sb.getRed(), sb.getYellow(), bestMoves[i])) {
                for (int j : initialMoveOrder) {
                    if (!AIBitAlgebra.isFull(sb.getRed(), sb.getYellow(), j)) {
                        bestMoves[i] = j;
                        break;
                    }
                }
            }
        }

        return bestMoves;
    }


    public int alphaBeta(
            long r,             //rote Steine
            long y,             //gelbe Steine
            int stat,           //Belegungstiefe (performance-Verbesserung)
            boolean turn,       //rot (Spieler 1) am Zug?
            int depth,          //aktuelle Tiefe im Spielbaum
            int alpha,          //obere Schranke beim AB-Pruning
            int beta,           //untere Schranke beim AB-Pruning
            int[] bestMoves,    //Rückgabe der besten Züge im Sub-Tree die zu dieser Bewertung geführt hat
            int last_col,       //letzter Wurf col (performance für isWon)
            int last_row,
            long zKey,          //hash-Key für die Position
            FutureTask<?> t) {  //Task zum Stoppen des Threads - wird auch zur Zeitbegrenzung verwendet.

        int hash = 0;
        int hashBest = -1;
        if (settings.useHash) {
            hash = (int) zKey;
            if (map.containsKey(hash)) {
                AIHashEntry res = map.get(hash);
                if (res.red == r && res.yellow == y && depth <= res.depth) {
                    c2++;
                    if (res.flag == EVALUATED) return res.value;
                    if (res.flag == ALPHA_HINT && res.value <= alpha) return alpha;
                    if (res.flag == BETA_HINT && res.value >= beta) return beta;

                    c3++;
                    if (res.flag == ALPHA_HINT && res.value < beta) beta = res.value;
                    if (res.flag == BETA_HINT && res.value > alpha) alpha = res.value;
                }
                if(settings.usePreSort)
                    hashBest = res.best;
            }
        }

        if (!settings.useABPruning) alpha = -MAX_INT;
        boolean stop = false;
        int value = 0;

        if (last_col >= 0 && AIBitAlgebra.isWon(turn ? y : r, last_col, last_row)) {
            value = -100000000 + AIBitAlgebra.count(stat);
            stop = true;
        }
        if (!stop && AIBitAlgebra.isDrawNotWon(stat)) {
            stop = true;
        }
        if (!stop && depth <= 0) {
            value = (turn ? 1 : -1) * AIBitAlgebra.eval(r, y, settings.useStatWeight);
            stop = true;
        }

        if (stop) {
            if (settings.useHash) {
                map.put(hash, new AIHashEntry(r,y,depth,value,-1,EVALUATED));
                c1++;
            }
            return value;
        }

        int[] subBest = Arrays.copyOfRange(bestMoves, 0, depth - 1);
        boolean improvedNode = false;

        //checke, ob die Zeit abgelaufen ist.
        //wir testen nur in internen Knoten. Ein Endknoten und seine Blätter werden noch ausgewertet.
        if(System.currentTimeMillis()-startTime>= settings.tLimit){
            t.cancel(false);
        }
        for (int j : sortMoves(hashBest, bestMoves[depth - 1])) {

            //Wenn Spalte voll, dann direkt nächster Wurf
            if (j == -1 || AIBitAlgebra.isFull(stat, j)) continue;
            //Berechne den Index der einzuwerfenden Reihe
            int row = AIBitAlgebra.getRowPos(stat, j) + 1;
            //Werfe ein
            long new_val = AIBitAlgebra.set(turn ? r : y, j, row);
            int new_stat = AIBitAlgebra.addStat(stat, j);

            value = -alphaBeta(turn ? new_val : r, turn ? y : new_val, new_stat, !turn,
                    depth - 1, -beta, -alpha, subBest, j, row, AIBitAlgebra.getZHash(zKey, j, row, !turn), t);

            //Beta-Cut - der Gegner hat bessere Optionen
            if (settings.useABPruning && value >= beta) {
                if (settings.useHash) {
                    map.put(hash, new AIHashEntry(r,y,depth,value,j, BETA_HINT) );
                    c1++;
                }
                return beta;
            }

            if (value > alpha) {
                alpha = value;
                bestMoves[depth - 1] = j;
                System.arraycopy(subBest, 0, bestMoves, 0, depth - 1);
                improvedNode = true;
            }

            //Wenn Thread abgebrochen wird, wird der aktuell beste Wert
            //nach Unten propagiert. Die Rekursion wird gestoppt.
            if (t.isCancelled()) break;
        }
        if (settings.useHash) {
            map.put(hash, new AIHashEntry(r,y,depth,value,improvedNode ? bestMoves[depth - 1] : -1,improvedNode?EVALUATED: ALPHA_HINT));
            c1++;
        }
        return alpha;
    }

    private int[] sortMoves(int hashBest, int oldBest) {
        int[] moves = new int[9];
        moves[0] = hashBest;
        moves[1] = oldBest;
        for (int i = 0; i < 7; i++) {
            if (initialMoveOrder[i] == hashBest || initialMoveOrder[i] == oldBest) moves[i + 2] = -1;
            else moves[i + 2] = initialMoveOrder[i];
        }

        return moves;
    }

    public List<String> report() {
        return List.of("Bewertung der Position: " + lastValue);
    }
}
