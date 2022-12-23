package org.fg.VierGewinnt.VierGEngine;

import org.fg.VierGewinnt.VierGModel.Board;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AIBitAlgebraTest {

    @Test
    public void testIsFull(){
        long rv =  2306498101509L;
        long gv = 5660783673866L;
        assertTrue(AIBitAlgebra.isFull(rv,gv,4));
        assertFalse(AIBitAlgebra.isFull(rv,gv,1));
    }

    @Test
    public void testIsFullBasics(){

        //test Oder-Operation. Oder-Verknüpfung von 2 Farben sollte
        //ergeben ob Positionen belegt sind. Sind alle 6 Reihen der ersten Spalte belegt?
        //ist das Ergebnis = 63?
        long rv1 = 11L;
        long gv1 = 52L;

        long cv1 = rv1 | gv1;
        assertEquals(cv1,63L);

        //Wollen wir eine andere Spalte testen, verschieben wir die Bits um 8 * col
        //um auf die erste spalte zu testen

        long rv5 = 163208757248L;
        long gv5 = 107374182400L;

        long rv5t1 = rv5 >> (4 * 8);
        long gv5t1 = gv5 >> (4 * 8);

        long cv5t1 = rv5t1 | gv5t1;
        assertEquals(cv5t1,63L);
    }

    @Test
    public void checkWinCond(){
        Board b = new Board();
        AISettingsParameter s = new AISettingsParameter();
        int val;
        s.maxDepth=8;
        s.useHash=true;
        s.useStatWeight=true;
        AI ai = new AI(new AISettings(s), b);
        b.add(3);//r
        //b.add(5);//  y
        b.add(ai.advance(null));
        b.add(4);//r
        //b.add(3);//  y
        b.add(ai.advance(null));
        b.add(2);//r
        //b.add(1);//  y
        b.add(ai.advance(null));
        b.add(3);//r
        //b.add(3);//  y
        b.add(ai.advance(null));
        b.add(0);//r
        //b.add(1);//  y
        b.add(ai.advance(null));
        b.add(1);//r
        System.out.println("Rot: "+b.getRed());
        System.out.println("Gelb: "+b.getYellow());
        val = ai.advance(null);
        System.out.println("Wurf: "+val);
        System.out.println("Bewertung: "+ai.getLastValue());

    }

}
