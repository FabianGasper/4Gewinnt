package org.fg.VierGewinnt.VierGEngine;

import org.fg.VierGewinnt.VierGModel.Stone;

import java.util.Random;

public class AIBitAlgebra {

    //Anzahl der möglichen 4er die von dieser Position aus möglich sind
    private static final int[][] weightMatrix = {
            {3, 4, 5, 5, 4, 3}, {4, 6, 8, 8, 6, 4}, {5, 8, 11, 11, 8, 5},
            {7, 10, 13, 13, 10, 7}, {5, 8, 11, 11, 8, 5}, {4, 6, 8, 8, 6, 4}, {3, 4, 5, 5, 4, 3}};

    private static final long[][][] zKeyMap = new long[7][6][2];
    static {
        for(int i=0;i<7;i++)
            for(int j=0;j<6;j++)
                for(int k=0;k<2;k++){
                    zKeyMap[i][j][k]=new Random().nextLong();
                }
    }

    public static boolean isFull(long r, long y, int col) {
        // Bit-shift um 8Bit * Anzahl Columns um gesuchte Spalte auf col1 zu ziehen
        // &255, um alle Belegungen folgender Spalten auf 0 zu setzen
        long r1 = (r >> (col << 3)) & 255L;
        long y1 = (y >> (col << 3)) & 255L;
        return (r1 | y1) == 63L;
    }

    public static boolean isEmpty(long r, long y, int col) {
        // Bit-shift um 8Bit * Anzahl Columns um gesuchte Spalte auf col1 zu ziehen
        // &255, um alle Belegungen folgender Spalten auf 0 zu setzen
        long r1 = (r >> (col << 3)) & 255L;
        long y1 = (y >> (col << 3)) & 255L;
        return (r1 | y1) == 0L;
    }

    public static boolean isFull(int stat, int col) {
        return getRowPos(stat, col) == 5;
    }

    public static boolean isEmpty(int stat, int col) {
        return getRowPos(stat, col) == -1;
    }

    public static boolean isWon(long v, int col, int row) {
        //untersuche nur den bereich um die Einwurfstelle. Könnte effizienter sein.
        //erfordert nur 13 Bit-Vergleiche anstelle von 69
        //Zunächst ziehen wir den Einwurfbereich auf die Mitte auf Position 3;3
        int shift_l = 3 - col;
        int shift_u = 3 - row;
        long v1;
        if (shift_l >= 0) {
            v1 = v << (shift_l << 3);
        } else {
            v1 = v >> (-shift_l << 3);
        }
        if (shift_u >= 0) {
            v1 = v1 << shift_u;
        } else {
            v1 = v1 >> -shift_u;
        }

        if ((v1 & 251658240L) == 251658240L) return true; // Senkrecht nach unten

        if ((v1 & 134744072L) == 134744072L) return true; // Waagerecht -3
        if ((v1 & 34494482432L) == 34494482432L) return true; // Waagerecht -2
        if ((v1 & 8830587502592L) == 8830587502592L) return true; // Waagerecht -1
        if ((v1 & 2260630400663552L) == 2260630400663552L) return true; // Waagerecht

        if ((v1 & 134480385L) == 134480385L) return true; // Diagonal LU-RO -3
        if ((v1 & 68853957120L) == 68853957120L) return true; // Diagonal LU-RO -2
        if ((v1 & 35253226045440L) == 35253226045440L) return true; // Diagonal LU-RO -1
        if ((v1 & 18049651735265280L) == 18049651735265280L) return true; // Diagonal LU-RO

        if ((v1 & 135274560L) == 135274560L) return true; // Diagonal LO-RU -3
        if ((v1 & 17315143680L) == 17315143680L) return true; // Diagonal LO-RU -2
        if ((v1 & 2216338391040L) == 2216338391040L) return true; // Diagonal LO-RU -1
        return (v1 & 283691314053120L) == 283691314053120L; // Diagonal LO-RU
    }

    public static boolean isWon(long v) {
        //teste spalten
        //  |  = 15L
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 3; j++) {
                long shift = v >> (i << 3) >> j;
                if ((shift & 15L) == 15L) return true;
            }
        }

        //teste reihen
        //  - = 16843009L
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 4; j++) {
                long shift = v >> (j << 3) >> i;
                if ((shift & 16843009L) == 16843009L) return true;
            }
        }

        //teste diagonal
        //   / = 134480385L  \ = 16909320L
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 3; j++) {
                long shift = v >> (i << 3) >> j;
                if ((shift & 134480385L) == 134480385L || (shift & 16909320L) == 16909320L) return true;
            }
        }

        return false;
    }

    public static boolean isDraw(long r, long y) {
        //true, wenn nicht gewonnen und alle Spalten voll
        if (isWon(r)) return false;
        if (isWon(y)) return false;
        return isDrawNotWon(r, y);
    }

    public static boolean isDrawNotWon(int stat) {
        return stat == 107374182;
    }

    public static boolean isDrawNotWon(long r, long y) {
        //true, wenn alle Spalten voll
        //true setzt voraus, dass auf isWon bereits getestet wurde
        for (int i = 0; i < 7; i++) {
            if (!isFull(r, y, i)) return false;
        }
        return true;
    }

    public static int getRowPos(long r, long y, int col) {
        // Bit-shift um 8Bit * Anzahl Columns um gesuchte Spalte auf col1 zu ziehen
        // &255, um alle Belegungen folgender Spalten auf 0 zu setzen
        long r1 = (r >> (col << 3)) & 255L;
        long y1 = (y >> (col << 3)) & 255L;
        long val = r1 | y1;

        //teste Reihen von groß nach klein, ob sie belegt sind. Wenn ja, dann ist das die Pos
        for (int i = 5; i >= 0; i--) {
            if ((val >> i) == 1L) return i;
        }
        //Wenn Spalte nicht belegt ist
        return -1;
    }

    public static int getRowPos(int stat, int col) {
        return ((stat >> (col << 2)) & 15) - 1;
    }

    public static long set(long v, int col, int row) {
        //setzt voraus, dass Spalte nicht voll ist, ansonsten stimmt das Ergebnis nicht
        long bit = 1L << ((col << 3) + row);
        return v | bit;
    }

    public static int addStat(int stat, int col) {
        return stat + (1 << (col << 2));
    }

    public static int removeStat(int stat, int col) {
        return stat - (1 << (col << 2));
    }

    public static long remove(long v, int col, int row) {
        //setzt voraus, dass Spalte nicht leer ist, hat ansonsten keinen effekt
        long bit = 1L << ((col << 3) + row);
        return v & (~bit);
    }

    public static Stone get(long r, long y, int col, int row) {
        long bit = 1L << ((col << 3) + row);
        if ((r & bit) >= 1L) return Stone.RED;
        if ((y & bit) >= 1L) return Stone.YELLOW;
        else return Stone.EMPTY;
    }

    public static int count(long r, long y) {
        int retVal = 0;
        for (int i = 0; i < 7; i++) {
            retVal += getRowPos(r, y, i) + 1;
        }
        return retVal;
    }

    public static int count(int stat) {
        int retVal = 0;
        for (int i = 0; i < 7; i++) {
            retVal += getRowPos(stat, i) + 1;
        }
        return retVal;
    }

    public static int eval(long r, long y, boolean weighted) {
        long occupied = r | y;
        long free = ~occupied;

        int rv = 0, yv = 0;

        long shift;

        //teste spalten
        //  |  = 15L
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 3; j++) {
                shift = (r | free) >> (i << 3) >> j;
                if ((shift & 15L) == 15L) {
                    rv += calcUp((int) (r >> (i << 3) >> j), i, j, weighted);
                }
                shift = (y | free) >> (i << 3) >> j;
                if ((shift & 15L) == 15L) {
                    yv += calcUp((int) (y >> (i << 3) >> j), i, j, weighted);
                }
            }
        }

        //teste reihen
        //  - = 16843009L
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 6; j++) {
                shift = (r | free) >> (i << 3) >> j;
                if ((shift & 16843009L) == 16843009L) {
                    rv += calcRight((int) (r >> (i << 3) >> j), i, j, weighted);
                }
                shift = (y | free) >> (i << 3) >> j;
                if ((shift & 16843009L) == 16843009L) {
                    yv += calcRight((int) (y >> (i << 3) >> j), i, j, weighted);
                }
            }
        }

        //teste diagonal
        //   / = 134480385L  \ = 16909320L
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 3; j++) {
                shift = (r | free) >> (i << 3) >> j;
                if ((shift & 134480385L) == 134480385L) {
                    rv += calcDiagonal1((int) (r >> (i << 3) >> j), i, j, weighted);
                }
                if ((shift & 16909320L) == 16909320L) {
                    rv += calcDiagonal2((int) (r >> (i << 3) >> j), i, j, weighted);
                }
                shift = (y | free) >> (i << 3) >> j;
                if ((shift & 134480385L) == 134480385L) {
                    yv += calcDiagonal1((int) (y >> (i << 3) >> j), i, j, weighted);
                }
                if ((shift & 16909320L) == 16909320L) {
                    yv += calcDiagonal2((int) (y >> (i << 3) >> j), i, j, weighted);
                }
            }
        }

        return rv - yv;
    }

    private static int calcUp(int l, int i, int j, boolean weighted) {
        final int[][] stamps = {{1, 2, 4, 8}, {3, 5, 6, 9, 10, 12}, {7, 11, 13, 14}};
        final int[] values = {10, 150, 1000};
        return calc(l, (weighted ? weightMatrix[i][j] : 1), stamps, values);
    }

    private static int calcRight(int l, int i, int j, boolean weighted) {
        final int[][] stamps = {{1, 256, 65536, 16777216}, {257, 65537, 65792, 16777217, 16777472, 16842752,}, {65793, 16777473, 16842753, 16843008}};
        final int[] values = {10, 1500, 10000};
        return calc(l, (weighted ? weightMatrix[i][j] : 1), stamps, values);
    }

    private static int calcDiagonal1(int l, int i, int j, boolean weighted) {
        final int[][] stamps = {{1, 512, 262144, 134217728}, {513, 262145, 262656, 134217729, 134218240, 134479872},
                {262657, 134218241, 134479873, 134480384}};
        final int[] values = {10, 1500, 10000};
        return calc(l, (weighted ? weightMatrix[i][j] : 1), stamps, values);
    }

    private static int calcDiagonal2(int l, int i, int j, boolean weighted) {
        final int[][] stamps = {{8, 1024, 131072, 16777216}, {1032, 131080, 132096, 16777224, 16778240, 16908288},
                {132104, 16778248, 16908296, 16909312}};
        final int[] values = {10, 1500, 10000};
        return calc(l, (weighted ? weightMatrix[i][j] : 1), stamps, values);
    }

    private static int calc(int target, int weight, int[][] stamps, int[] values) {
        int val = 0;
        for (int i = 0; i < 3; i++) {
            for (int s : stamps[i]) {
                if ((target & s) == s) val += weight * values[i];
            }
        }
        return val;
    }

    public static int getHash(long r, long y) {
        //Keine Ahnung was das bewirkt, aber alle eigenen Experimente hatten einen
        //schlechteren load
        return 31 * (int) (r ^ (r >>> 32)) + (int) (y ^ (y >>> 32));
    }

    public static long getZHash(long zKey, int col, int row,boolean turn) {
        //https://en.wikipedia.org/wiki/Zobrist_hashing
        return (zKey ^ zKeyMap[col][row][turn?0:1]);
    }

    public static long mergeInts(int i1, int i2){
        return ((long) i1) & (((long) i2) <<32);
    }
}
