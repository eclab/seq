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
import java.io.*;
import seq.motif.generator.gui.*;
import seq.motif.generator.alg.triadexmuse.gui.*;

public class TriadexMuse extends Algorithm
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
        
    public static final int[] RATES = { RATE_WHOLE_NOTES, RATE_HALF_NOTES, RATE_QUARTER_NOTES, RATE_EIGHTH_NOTES, RATE_TRIPLET_NOTES, RATE_SIXTEENTH_NOTES, RATE_TRIPLET_SIXTEENTH_NOTES, RATE_THIRTY_SECOND_NOTES };

    public int getRateIndex() 
        { 
        for(int i = 0; i < RATES.length; i++) 
            {
            if (RATES[i] == rate) return i;
            }
        return 0;
        }
        
    public static final int MAX_TRANSPOSE = 24;

    int[] interval = new int[4];
    int[] theme = new int[4];
    int transpose;
    int velocity = 64;
    boolean rest = false;
    int rate = RATE_EIGHTH_NOTES;
    double gate = 1.0;
    boolean legato = true;
    
    public TriadexMuse copy()
        {
        TriadexMuse lm = (TriadexMuse)(super.copy());
        
        for(int i = 0; i < 4; i++)
            {
            lm.interval[i] = interval[i];
            lm.theme[i] = theme[i];
            }
                
        return lm;
        }
        
    public TriadexMuse(Generator generator)
        {
        super(generator);
        }
        
    public TriadexMuse(Generator generator, JSONObject obj) throws JSONException
        {
        super(generator, obj);
        JSONArray array = obj.optJSONArray("int");
        if (array != null)
            {
            for(int i = 0; i < interval.length; i++)
                {
                interval[i] = array.optInt(i, 0);
                }
            }
        array = obj.optJSONArray("thm");
        for(int i = 0; i < theme.length; i++)
            {
            theme[i] = array.optInt(i, 0);
            }
        transpose = obj.optInt("trans", 0);
        velocity = obj.optInt("vel", 64);
        rest = obj.optBoolean("rest", false);
        rate = obj.optInt("rate", RATE_EIGHTH_NOTES);
        gate = obj.optDouble("gate", 1.0);
        legato = obj.optBoolean("leg", true);
        }
        
    public void save(JSONObject obj) throws JSONException
        {
        super.save(obj);
        JSONArray array = new JSONArray();
        for(int i = 0; i < interval.length; i++)
            {
            array.put(interval[i]);
            }
        obj.put("int", array);
        array = new JSONArray();
        for(int i = 0; i < theme.length; i++)
            {
            array.put(theme[i]);
            }
        obj.put("thm", array);
        obj.put("trans", transpose);
        obj.put("vel", velocity);
        obj.put("rest", rest);
        obj.put("rate", rate);
        obj.put("gate", gate);
        obj.put("leg", legato);
        }

    public AlgorithmNode buildNode(Seq seq, GeneratorClip clip)
        {
        return new TriadexMuseNode(seq, generator, clip, this);
        }
                
    public AlgorithmUI buildUI(Seq seq, GeneratorUI ui)
        {
        return new TriadexMuseUI(seq, generator, ui, this);
        } 

    public int getInterval(int pos) { return interval[pos]; }
    public void setInterval(int pos, int val) 
        { 
        if (val > 39) val = 39; 
        interval[pos] = val; 
        }

    public int getTheme(int pos) { return theme[pos]; }
    public void setTheme(int pos, int val) 
        { 
        if (val > 39) val = 39; 
        theme[pos] = val; 
        }
    
    public int getTranspose() { return transpose; }
    public void setTranspose(int val) { transpose = val; }
    public int getVelocity() { return velocity; }
    public void setVelocity(int val) { velocity = val; }
    public boolean getRest() { return rest; }
    public void setRest(boolean val) { rest = val; }
    public int getRate() { return rate; }
    public void setRate(int val) { rate = val; }
    public double getGate() { return gate; }
    public void setGate(double val) { gate = val; }
    public boolean getLegato() { return legato; }
    public void setLegato(boolean val) { legato = val; }

    public String getHTMLDescription() 
        {
        return getIndexHTMLFileText();
        }
                
                
    // These are taken from https://till.com/articles/muse/
    // They are supposedly from the Muse's manual
                
    public static final String[] PRESET_NAMES = {
        "Michael's Tune",
        "Muser's Waltz",
        "Scale",
        "Ed's Rhythm Piece",
        "The Crazy Cuckoo",
        "Birds 1",
        "Birds2",
        "Dorian Muse",
        "Mesopotamia",
        "Swiss Yodeler",
        "Ron's Rhapsody",
        "Christmas Bells",
        "Marvin's Yodel",
        "Federal Row",
        "Al's Surprise",
        "Meditiation",
        "Flat Baroque",
        "Polka",
        "Rhyming Couplets",
        "Yodle"
        };



    public static final boolean[] PRESET_RESTS = {
        false, false, false, false, false, false, false, 
        false, false, false, true, false, true, false, 
        false, false, true, true, false, false };


    public static final int[][] PRESET_INTERVALS = {
        { (8 + 7), (8 + 8), (8 + 5), 0 },
        { (8 + 10), (8 + 8), (8 + 7), 0 },
        { 3, 4, 5, 0 },
        { (8 + 6), (8 + 6), (8 + 6), 4 },
        { 3, (8 + 1), (8 + 31), 6 },
        { (8 + 1), (8 + 2), (8 + 3), 5 },
        { (8 + 28), (8 + 29), (8 + 30), (8 + 31) },
        { 1, (8 + 1), (8 + 3), 6 },
        { 4, (8 + 5), (8 + 9), 0 },
        { (8 + 8), 3, (8 + 16), 0 },
        { (8 + 6), (8 + 9), (8 + 6), 2 },
        { (8 + 31), (8 + 30), (8 + 29), (8 + 28) },
        { (8 + 2), (8 + 17), (8 + 9), (8 + 25) },
        { (8 + 14), (8 + 5), (8 + 12), (8 + 2) },
        { (8 + 1), (8 + 5), (8 + 7), 2 },
        { (8 + 1), (8 + 31), (8 + 14), 0 },
        { 3, (8 + 15), (8 + 1), 2 },
        { (8 + 1), (8 + 13), (8 + 11), 2 },
        { (8 + 1), (8 + 2), 5, 6 },
        { 6, (8 + 2), (8 + 5), (8 + 6) }
        };


    public static final int[][] PRESET_THEMES = {
        { 0, (8 + 4), (8 + 23), 0 },
        { 1, 5, (8 + 1), (8 + 2) },
        { 0, 0, 0, 0 },
        { 0, 0, (8 + 1), (8 + 31) },
        { 0, 0, (8 + 1), (8 + 31) },
        { (8 + 30), (8 + 31), (8 + 31), (8 + 31) },
        { (8 + 30), (8 + 31), (8 + 31), (8 + 31) },
        { (8 + 1), (8 + 16), 0, 0 },
        { 6, (8 + 9), (8 + 24), 5 },
        { (8 + 22), (8 + 21), (8 + 16), 0 },
        { (8 + 31), 5, 0, 6 },
        { (8 + 28), (8 + 29), (8 + 30), (8 + 31) },
        { (8 + 16), 0, (8 + 15), 3 },
        { (8 + 21), (8 + 24), 4, 0 },
        { 6, (8 + 1), (8 + 7), (8 + 11) },
        { 0, 0, (8 + 16), (8 + 31) },
        { (8 + 30), (8 + 29), (8 + 24), 0 },
        { 6, (8 + 11), (8 + 7), (8 + 1) },
        { 0, 0, (8 + 31), 5 },
        { 0, 0, (8 + 1), (8 + 31) }
        };      
    }
        
        
        
        
        
        
