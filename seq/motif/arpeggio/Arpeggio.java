/* 
   Copyright 2025 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.arpeggio;

import seq.engine.*;
import seq.motif.blank.*;
import seq.util.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;


public class Arpeggio extends Motif
    {
    private static final long serialVersionUID = 1;
    
    public static final int TYPE_UP = 0;
    public static final int TYPE_DOWN = 1;
    public static final int TYPE_UP_DOWN = 2;
    public static final int TYPE_UP_DOWN_2 = 3;
    public static final int TYPE_RANDOM = 4;
    public static final int TYPE_PATTERN = 5;
    public static final int MAX_PATTERN_LENGTH = 32;
    public static final int PATTERN_NOTES = 24;
    public static final int MAX_OCTAVES = 4;

    int arpeggioType = TYPE_UP;
    int octaves = 1;
    int patternLength = MAX_PATTERN_LENGTH;
    //int intercept;
    int out;
    int rate = Seq.PPQ / 4;		// 16th notes
    boolean omni = true;
    boolean newChordReset = true;
    
    // pattern[PATTERN_NOTES/2] is the lowest note in the chord
    // BELOW that we go BELOW the chord
    public boolean[][] pattern = new boolean[MAX_PATTERN_LENGTH][PATTERN_NOTES];

    public Clip buildClip(Clip parent)
        {
        return new ArpeggioClip(seq, this, parent);
        }
        
    public Arpeggio(Seq seq)
        {
        super(seq);
        out = (Prefs.getLastOutDevice(0, "seq.motif.arpeggio.Arpeggio.out"));
        //int = (Prefs.getLastInDevice(0, "seq.motif.arpeggio.Arpeggio.in"));
        add(new Blank(seq));
        // do this for the others?
        }
    
    public void setRate(int rate)
        {
        this.rate = rate;
        }
        
    public int getRate()
        {
        return rate;
        }
    
    public void setArpeggioType(int type)
        {
        arpeggioType = type;
        }
        
    public int getArpeggioType()
        {
        return arpeggioType;
        }
        
    public void setOctaves(int octaves)
        {
        this.octaves = octaves;
        }
        
    public int getOctaves()
        {
        return octaves;
        }
        
    public boolean isOmni()
    	{
    	return omni;
    	}
    	
    public void setOmni(boolean val)
    	{
    	omni = val;
    	}

    public boolean getNewChordReset()
    	{
    	return newChordReset;
    	}
    	
    public void setNewChordReset(boolean val)
    	{
    	newChordReset = val;
    	}
    
    public void setPatternLength(int length)
        {
        patternLength = length;
        }
        
    public int getPatternLength()
        {
        return patternLength;
        }
    
    public void setPattern(int i, int j, boolean val)
        {
        pattern[i][j] = val;
        }
        
    public boolean getPattern(int i, int j)
        {
        return pattern[i][j];
        }
    
    public void clearPattern()
        {
        for(int i = 0; i < pattern.length; i++)
            {
            for(int j = 0; j < pattern[i].length; j++)
                {
                pattern[i][j] = false;
                }
            }
        }
        
    /** Returns the output device. */
    public int getOut() { return out; }
    
    /** Sets the output device. */
    public void setOut(int val) { out = val; Prefs.setLastOutDevice(0, val, "seq.motif.arpeggio.Arpeggio.out"); }

    /** Returns the input device. */
    //public int getIntercept() { return intercept; }

    /** Sets the input device. */
    //public void setIntercept(int val) { intercept = val; Prefs.setLastInDevice(0, val, "seq.motif.arpeggio.Arpeggio.intercept"); }
        


    public void add(Motif motif)
        {
        Motif.Child child = addChild(motif);
        } 

    public void load(JSONObject obj) throws JSONException
        {
        setArpeggioType(obj.optInt("arp", TYPE_UP));		// can't use "type", it's used in Motif.java
        setOctaves(obj.optInt("oct", 1));
        setRate(obj.optInt("rate", Seq.PPQ / 4));
        setPatternLength(obj.optInt("len", MAX_PATTERN_LENGTH));
        setOmni(obj.optBoolean("omni", true));
        setNewChordReset(obj.optBoolean("new", true));
        setOut(obj.optInt("out", 0));
        //setIn(obj.optInt("int", 0));
        JSONArray array = obj.getJSONArray("pattern");
        clearPattern();
        if (array == null)
            {
            System.err.println("Arpeggio.load() error: no pattern array");
            }
        else
            {
            int pos = 0;
            for(int i = 0; i < getPatternLength(); i++)
                {
                for(int j = 0; j < pattern[i].length; j++)
                    {
                    setPattern(i, j, array.optBoolean(pos++, false));
                    }
                }
            }
        }
        
        public void save(JSONObject obj) throws JSONException
            {
            obj.put("arp", getArpeggioType());		// can't use "type", it's used in Motif.java
            obj.put("oct", getOctaves());
            obj.put("len", getPatternLength());
            obj.put("rate", getRate());
            obj.put("omni", isOmni());
            obj.put("out", getOut());
            obj.put("new", getNewChordReset());
           // obj.put("int", getIn());
            JSONArray array = new JSONArray();
            int pos = 0;
            for(int i = 0; i < getPatternLength(); i++)
                {
                for(int j = 0; j < pattern[i].length; j++)
                    {
                    array.put(getPattern(i, j));
                    }
                }
            obj.put("pattern", array);
            }

        static int document = 0;
        static int counter = 1;
        public int getNextCounter() { if (document < Seq.getDocument()) { document = Seq.getDocument(); counter = 1; } return counter++; }
        
        public String getBaseName() { return "Arpeggio"; }
        }
        
