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
    int rate = Seq.PPQ / 4;             // 16th notes
    boolean omni = true;
    boolean newChordReset = true;
    int from = 0;
    int to = 0;
    boolean always = true;     
    int velocity = 64;            
    boolean velocityAsPlayed = true;                     
    
    public static final int PATTERN_REST = 0;
    public static final int PATTERN_NOTE = 1;
    public static final int PATTERN_TIE = 2;
    
    // pattern[PATTERN_NOTES/2] is the lowest note in the chord
    // BELOW that we go BELOW the chord
    public int[][] pattern = new int[MAX_PATTERN_LENGTH][PATTERN_NOTES];

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
    
    public void setVelocity(int velocity)
        {
        this.velocity = velocity;
        }
        
    public int getVelocity()
        {
        return velocity;
        }
    
    public void setVelocityAsPlayed(boolean val)
        {
        velocityAsPlayed = val;
        }
        
    public boolean getVelocityAsPlayed()
        {
        return velocityAsPlayed;
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
    
    public void setPattern(int i, int j, int val)
        {
        pattern[i][j] = val;
        }
        
    public int getPattern(int i, int j)
        {
        return pattern[i][j];
        }
        
    public int getFrom()
        {
        return from;
        }
    
    public void setFrom(int val)
        {
        from = val;
        }
    
    public int getTo()
        {
        return to;
        }
    
    public void setTo(int val)
        {
        to = val;
        }
    
    public boolean isAlways()
        {
        return always;
        }
    
    public void setAlways(boolean val)
        {
        always = val;
        }
    


    public void clearPattern()
        {
        for(int i = 0; i < pattern.length; i++)
            {
            for(int j = 0; j < pattern[i].length; j++)
                {
                pattern[i][j] = PATTERN_REST;
                }
            }
        }
        
    /** Returns the output device. */
    public int getOut() { return out; }
    
    /** Sets the output device. */
    public void setOut(int val) { out = val; Prefs.setLastOutDevice(0, val, "seq.motif.arpeggio.Arpeggio.out"); }        


    public void add(Motif motif)
        {
        addChild(motif);
        } 

    public void load(JSONObject obj) throws JSONException
        {
        setArpeggioType(obj.optInt("arp", TYPE_UP));            // can't use "type", it's used in Motif.java
        setOctaves(obj.optInt("oct", 1));
        setRate(obj.optInt("rate", Seq.PPQ / 4));
        setPatternLength(obj.optInt("len", MAX_PATTERN_LENGTH));
        setOmni(obj.optBoolean("omni", true));
        setNewChordReset(obj.optBoolean("new", true));
        setOut(obj.optInt("out", 0));
        setFrom(obj.optInt("from", 0));
        setTo(obj.optInt("to", 0));
        setAlways(obj.optBoolean("always", false));
        setVelocity(obj.optInt("vel", 64));
        setVelocityAsPlayed(obj.optBoolean("play", true));
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
                    setPattern(i, j, array.optInt(pos++, PATTERN_REST));
                    }
                }
            }
        }
        
    public void save(JSONObject obj) throws JSONException
        {
        obj.put("arp", getArpeggioType());          // can't use "type", it's used in Motif.java
        obj.put("oct", getOctaves());
        obj.put("len", getPatternLength());
        obj.put("rate", getRate());
        obj.put("omni", isOmni());
        obj.put("out", getOut());
        obj.put("new", getNewChordReset());
        obj.put("from", getFrom());
        obj.put("to", getTo());
        obj.put("always", isAlways());
        obj.put("vel", getVelocity());
        obj.put("play", getVelocityAsPlayed());
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
        
