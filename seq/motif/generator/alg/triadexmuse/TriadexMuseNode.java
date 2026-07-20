/* 
   Copyright 2026 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.generator.alg.triadexmuse;

import seq.engine.*;
import seq.motif.blank.*;
import seq.motif.generator.*;
import seq.util.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;


public class TriadexMuseNode extends AlgorithmNode
    {
    private static final long serialVersionUID = 1;

	boolean lfsrAdvance;
    int lfsr;
    int[] counter = new int[7];
    
    int[] PITCHES = { 0, 2, 4, 5, 7, 9, 11, 12, 14, 16, 17, 19, 21, 23, 24 };
    String[] KEYS = { "C", "D", "E", "F", "G", "A", "B" };
    int getNote()
    	{
    	int note = 0;
    	int multiplicand = 1;
    	TriadexMuse triadexmuse = (TriadexMuse)algorithm;
    	for(int i = 0; i < 4; i++)
    		{
    		int in = clip.getCorrectedValueInt(triadexmuse.getInterval(i), 39);

    		if (in == 0) { /* System.err.print("0 "); */ note += 0; }
    		else if (in == 1) { /* System.err.print("1 "); */ note += multiplicand; }
    		else if (in < 9) { /* System.err.print("" + (counter[in - 2] >= 0 ? 1 : 0) + " "); */ note += multiplicand * (counter[in - 2] >= 0 ? 1 : 0); }
    		else { /* System.err.print("" + ((lfsr >>> (in - 9)) & 0x1) + " "); */ note += multiplicand * ((lfsr >>> (in - 9)) & 0x1); }
    		
    		if (i == 3) multiplicand = 7;
    		else multiplicand *= 2;
    		}
/*
System.err.print(" " + KEYS[note % 7] + (note / 7 + 1));

System.err.print(" -> 0 1 |");
for(int i = 0; i < 7; i++)
	{
	System.err.print(" " + (counter[i] >= 0 ? 1 : 0));
	}
System.err.print(" |");
for(int i = 0; i < 31; i++)
	{
	System.err.print(" " + ((lfsr >>> i) & 0x1));
	}
System.err.println();
*/
    	return PITCHES[note];
    	}
    
    void advanceLFSR()
    	{
    	if (!lfsrAdvance) { lfsrAdvance = true; return; }
    	else lfsrAdvance= false;
    	
    	int count = 0;
    	TriadexMuse triadexmuse = (TriadexMuse)algorithm;
    	
    	// FIRST we must compute the count, before we advance the LFSR
    	for(int i = 0; i < 4; i++)
    		{
    		int th = clip.getCorrectedValueInt(triadexmuse.getTheme(i), 39);
    		if (th == 0) count += 0;
    		else if (th == 1) count += 1;
    		else if (th < 9) count += (counter[th - 2] >= 0 ? 1 : 0);
    		else count += ((lfsr >>> (th - 9)) & 0x1);
    		}

		// SECOND we must advance the LFSR
    	int out = (lfsr & 0x1);
    	lfsr = lfsr << 1;
    	
		// LAST we fill in the first bit based on the count
		// Note that this is the opposite of what the manual (which is wrong) says
    	if (count == 0 || count == 2 || count == 4) lfsr = lfsr | 0x1;
    	else lfsr = lfsr | 0x0;
    	}
    	
    public void reset()
    	{
    	lfsrAdvance = true;
    	lfsr = 0;
    	counter[0] = 0;
    	counter[1] = -1;
    	counter[2] = -3;
    	counter[3] = -7;
    	counter[4] = -15;
    	counter[5] = -5;
    	counter[6] = -11;
    	}
    	
    void advanceCounter()
    	{
    	counter[0]++;
    	if (counter[0] >= 1) counter[0] = -1;
    	counter[1]++;
    	if (counter[1] >= 2) counter[1] = -2;
    	counter[2]++;
    	if (counter[2] >= 4) counter[2] = -4;
    	counter[3]++;
    	if (counter[3] >= 8) counter[3] = -8;
    	counter[4]++;
    	if (counter[4] >= 16) counter[4] = -16;
    	counter[5]++;
    	if (counter[5] >= 6) counter[5] = -6;
    	counter[6]++;
    	if (counter[6] >= 12) counter[6] = -12;
    	}
    
    public TriadexMuseNode(Seq seq, Generator generator, GeneratorClip clip, Algorithm algorithm)
    	{
    	super(seq, generator, clip, algorithm);
    	reset();
    	}
    	
    public TriadexMuseNode copy()
        {
        TriadexMuseNode lmn = (TriadexMuseNode)(super.copy());
        return lmn;
        }
    
    public void loop()
    	{
    	reset();
    	}
    	
    public void release()
    	{
    	if (lastNote != REST)
    		{
    		noteOff(lastNote, 0x64, lastNoteID);
    		lastNote = REST;
    		}
    	}
    	
    public void cut()
    	{
    	if (lastNote != REST)
    		{
    		noteOff(lastNote, 0x64, lastNoteID);
    		lastNote = REST;
    		}
    	}
    	
    public static final int LOW_NOTE = 0x60;
    
    public static final int REST = -1;
    int lastNote = REST;
    int lastNoteID = 0;
    
    public boolean process(ArrayList<GeneratorClip.Note> notes)
    	{
    	TriadexMuse triadexmuse = (TriadexMuse)getAlgorithm();
    	
    	int rate = triadexmuse.getRate();
    	if (getPosition() % rate != 0) return false;
    	
    	advanceCounter();
    	advanceLFSR();				// counter advances first then LFSR is based on it
    	int note = getNote();
    	if (note == 0 && triadexmuse.getRest()) return false;
    	
    	int trans = clip.getCorrectedValueInt(triadexmuse.getTranspose(), TriadexMuse.MAX_TRANSPOSE);
    	note = note + trans + LOW_NOTE;
    	while (note > 127) note -= 12;
    	
    	int velocity = triadexmuse.getVelocity();
    	double gate = triadexmuse.getGate();
    	boolean legato = triadexmuse.getLegato();
    	
    	if (lastNote == note)
    		{
    		// don't do anything
    		}
    	else
    		{
    		if (legato)
    			{
    			if (lastNote != REST)
    				{
    				noteOff(lastNote, 0x64, lastNoteID);
    				lastNoteID = -1;  	// probably not needed
    				}
    			if (note != REST)
    				{
    				lastNoteID = noteOn(note, velocity);	
    				}
    			}
    		else
    			{
    			if (note != REST)
    				{
	    			note(note, velocity, (int)Math.max(rate * gate, 1), 64);
	    			}
    			lastNote = REST;
    			lastNoteID = -1;		// probably not needed
    			}
    		}
    	lastNote = note;
    	
    	return false;
    	}
	}
	
	
	
	
	
	