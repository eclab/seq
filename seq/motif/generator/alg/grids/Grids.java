/* 
   Copyright 2026 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.generator.alg.grids;

import seq.engine.*;
import seq.motif.blank.*;
import seq.motif.generator.*;
import seq.util.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;
import java.io.*;
import seq.motif.generator.gui.*;
import seq.motif.generator.alg.grids.gui.*;

public class Grids extends Algorithm
    {
    private static final long serialVersionUID = 1;

    public static final int RATE_WHOLE_NOTES = Seq.PPQ * 4;
    public static final int RATE_HALF_NOTES = Seq.PPQ * 2;
    public static final int RATE_QUARTER_NOTES = Seq.PPQ;
    public static final int RATE_EIGHTH_NOTES = Seq.PPQ / 2;
    public static final int RATE_TRIPLET_NOTES = Seq.PPQ / 3;
    public static final int RATE_SIXTEENTH_NOTES = Seq.PPQ / 4;
    public static final int RATE_TRIPLET_SIXTEENTH_NOTES = Seq.PPQ / 6;
    public static final int RATE_THIRTY_SECOND_NOTES = Seq.PPQ / 8;
        
    public static final int ACCENT_THRESHOLD = 192;
    public static final int[] RATES = { RATE_WHOLE_NOTES, RATE_HALF_NOTES, RATE_QUARTER_NOTES, RATE_EIGHTH_NOTES, RATE_TRIPLET_NOTES, RATE_SIXTEENTH_NOTES, RATE_TRIPLET_SIXTEENTH_NOTES, RATE_THIRTY_SECOND_NOTES };

    public int getRateIndex() 
        { 
        for(int i = 0; i < RATES.length; i++) 
            {
            if (RATES[i] == rate) return i;
            }
        return 0;
        }
        
    int[] note = new int[] { 60, 62, 64 };                  // MIDI notes, defaults are middle C, D, and E
    int[] velocity = new int[] { 64, 64, 64 };                              // MIDI velocities
    int[] accentVelocity = new int[] { 127, 127, 127 };             // MIDI velocities
    int x = 0;              // range 0...255
    int y = 0;              // range 0...255
    int[] complexity = new int[] { 127, 127, 127 };         // range 0...255 each
    int chaos = 0;
    int rate = RATE_SIXTEENTH_NOTES;
    boolean accents = true;
    
    public Grids copy()
        {
        Grids lm = (Grids)(super.copy());
        
        for(int i = 0; i < 3; i++)
            {
            lm.complexity[i] = complexity[i];
            }
                
        return lm;
        }
        
    public Grids(Generator generator)
        {
        super(generator);
        }
        
    public Grids(Generator generator, JSONObject obj) throws JSONException
        {
        super(generator, obj);
        JSONArray array = obj.optJSONArray("comp");
        if (array != null)
            {
            for(int i = 0; i < complexity.length; i++)
                {
                complexity[i] = array.optInt(i, 0);
                }
            }
        array = obj.optJSONArray("note");
        if (array != null)
            {
            for(int i = 0; i < note.length; i++)
                {
                note[i] = array.optInt(i, 60);  // default is middle c
                }
            }
        array = obj.optJSONArray("vel");
        if (array != null)
            {
            for(int i = 0; i < velocity.length; i++)
                {
                velocity[i] = array.optInt(i, 64);
                }
            }
        array = obj.optJSONArray("avel");
        if (array != null)
            {
            for(int i = 0; i < accentVelocity.length; i++)
                {
                accentVelocity[i] = array.optInt(i, 64);
                }
            }
        x = obj.optInt("x", 0);
        y = obj.optInt("y", 0);
        chaos = obj.optInt("chaos", 0);
        rate = obj.optInt("rate", RATE_SIXTEENTH_NOTES);
        accents = obj.optBoolean("acc", true);
        }
        
    public void save(JSONObject obj) throws JSONException
        {
        super.save(obj);
        JSONArray array = new JSONArray();
        for(int i = 0; i < complexity.length; i++)
            {
            array.put(complexity[i]);
            }
        obj.put("comp", array);
        array = new JSONArray();
        for(int i = 0; i < note.length; i++)
            {
            array.put(note[i]);
            }
        obj.put("note", array);
        array = new JSONArray();
        for(int i = 0; i < velocity.length; i++)
            {
            array.put(velocity[i]);
            }
        obj.put("vel", array);
        array = new JSONArray();
        for(int i = 0; i < accentVelocity.length; i++)
            {
            array.put(accentVelocity[i]);
            }
        obj.put("avel", array);
        obj.put("x", x);
        obj.put("y", y);
        obj.put("chaos", chaos);
        obj.put("rate", rate);
        obj.put("acc", accents);
        }

    public AlgorithmNode buildNode(Seq seq, GeneratorClip clip)
        {
        return new GridsNode(seq, generator, clip, this);
        }
                
    public AlgorithmUI buildUI(Seq seq, GeneratorUI ui)
        {
        return new GridsUI(seq, generator, ui, this);
        } 

    public int getComplexity(int pos) { return complexity[pos]; }
    public void setComplexity(int pos, int val) 
        { 
        complexity[pos] = val; 
        }

    public int getX() { return x; }
    public void setX(int val) { x = val; }
    public int getY() { return y; }
    public void setY(int val) { y = val; }
    public int getChaos() { return chaos; }
    public void setChaos(int val) { chaos = val; }
    public int getRate() { return rate; }
    public void setRate(int val) { rate = val; }
    public int getNote(int i) { return note[i]; }
    public void setNote(int i, int val) { note[i] = val; }
    public int getVelocity(int i) { return velocity[i]; }
    public void setVelocity(int i, int val) { velocity[i] = val; }
    public int getAccentVelocity(int i) { return accentVelocity[i]; }
    public void setAccentVelocity(int i, int val) { accentVelocity[i] = val; }
    public boolean getAccents() { return accents; }
    public void setAccents(boolean val) { accents = val; }

    public String getHTMLDescription() 
        {
        return getIndexHTMLFileText();
        }
    }
        
        
        
        
        
        
