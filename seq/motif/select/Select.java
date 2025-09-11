/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.select;

import seq.engine.*;
import seq.motif.blank.*;
import seq.util.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/**
   Select has four Modes.
        
   <ul>
   <li>MODE_SINGLE_ONE_SHOT.  Only one sound can play at a time, or none.  A sound plays at most once
   and then stops: it does not repeat.  If a new sound is chosen, it displaces the existing sound
   if one is playing.  If IMMEDIATE, then the existing sound is displaced immediately,
   else it is displaced after it has finished playing.  If a sound is displaced immediately, 
   it can be displaced by CLEARING it -- stopping all outstanding notes -- or RELEASING it -- 
   allowing outstanding notes to release on their own.  Either way, the new sound then starts 
   immediately, or on the next sixteenth note, or on the next quarter note, or on the next 
   4-quarter-note measure boundary, depending on the QUANTIZATION.  If the RELEASE ALL NOTES
   key is pressed, then instead of a next sound, no sound is played.  If the FINISH key is pressed,
   then instead of a next sound, the Select declares it has finished.

   <p>
   <li>MODE_SINGLE_REPEATING.  Only one sound can play at a time, or none.  Unless displaced, a sound
   plays until completion, and then repeats over and over.  If a new sound is chosen, it 
   displaces the existing sound if one is playing.  If IMMEDIATE, then the existing sound is 
   displaced immediately, else it is displaced after it has finished playing.  If a sound is displaced immediately, 
   it can be displaced by CLEARING it -- stopping all outstanding notes -- or RELEASING it -- 
   allowing outstanding notes to release on their own.  Either way, the new sound then starts 
   immediately, or on the next sixteenth note, or on the next quarter note, or on the next 
   4-quarter-note measure boundary, depending on the QUANTIZATION.  If the RELEASE ALL NOTES
   key is pressed, then instead of a next sound, no sound is played.  If the FINISH key is pressed,
   then instead of a next sound, the Select declares it has finished.

   <p>
   <li>MODE_MULTI_ONE_SHOT.  Many sounds can play simultaneously.  A sound plays at most once
   and then stops: it does not repeat.  If a new sound is chosen, it starts playing independently
   of existing playing sounds.  The new sound then starts 
   immediately, or on the next sixteenth note, or on the next quarter note, or on the next 
   4-quarter-note measure boundary, depending on the QUANTIZATION.   If the RELEASE ALL NOTES key 
   is pressed, and IMMEDIATE is chosen, then sounds are terminated immediately. A sound is terminated
   by CLEARING it -- stopping all outstanding notes -- or RELEASING it -- allowing outstanding notes 
   to release on their own.  If the FINISH key is pressed, then the Select declares it has finished.
   This is also either IMMEDIATE or at the next QUANTIZATION.

   <p>
   <li>MODE_MULTI_REPEATING.  Many sounds can play simultaneously.  A sound plays until completion, 
   and then repeats over and over.  If a new sound is chosen, it starts playing independently of
   existing playing sounds.  The new sound then starts 
   immediately, or on the next sixteenth note, or on the next quarter note, or on the next 
   4-quarter-note measure boundary, depending on the QUANTIZATION.  If a sound is selected which
   is already currently playing, then it is terminated after it has finished playing, unlesss
   IMMEDIATE, in which case it is terminated immediately.  Immediate termination can occur by
   by CLEARING it -- stopping all outstanding notes -- or RELEASING it -- allowing outstanding 
   notes to release on their own.  If the RELEASE ALL NOTES key is pressed, then all current
   playing songs are terminated when they finish, or immediately via IMMEDIATE (with the choice
   of CLEAR).  If the FINISH key is pressed, then instead of a next sound, the Select declares 
   it has finished.  If the FINISH key is pressed, then the Select declares it has finished.
   This is also either IMMEDIATE or at the next QUANTIZATION.
        
   </ul>
        
   <p>If PLAYFIRST is chosen, then Select will begin by immediately playing child #0. 
        
   <p>The STARTNOTE indicates the key that correpsonds with child 0.  Child 1 is one half-step
   up and so on.  The key that triggers a RELEASE ALL NOTES is one half-step down, and the
   key that triggers a FINISH is two half-steps down.

**/

public class Select extends Motif
    {
    private static final long serialVersionUID = 1;

    public static class Data
        {
        public static final int DISABLED = -1;
        
        public static final int MAX_TRANSPOSE = 24;
        public static final double MAX_GAIN = 4.0;
        public static final double MAX_RATE = 16.0;
        public static final double DEFAULT_RATE = 1.0;

        public int transpose = MAX_TRANSPOSE;                                   // ranges 0 ... MAX_TRANSPOSE * 2 inclusive, representing -MAX_TRANSPOSE ... MAX_TRANSPOSE
        public double rate = 1;
        public double gain = 1;
        public int out = DISABLED;

        public void setTranspose(int transpose) { this.transpose = transpose; }
        public int getTranspose() { return transpose; }

        public void setGain(double gain) { this.gain = gain; }
        public double getGain() { return gain; }

        public void setOut(int out) { this.out = out; }
        public int getOut() { return out; }
        
        public double getRate() { return rate; }
        public void setRate(double val) { rate = val; }
        
        public Data() { }
        
        public Data(Data other)
            {
            rate = other.rate;
            transpose = other.transpose;
            gain = other.gain;
            out = other.out;
            }
        }

    protected Object buildData(Motif motif) { return new Data(); }

    protected Object copyData(Motif motif, Object data) { return new Data((Data)data); }

    protected void saveData(Object data, Motif motif, JSONObject to) 
        {
        Data d = (Data)data;
        to.put("rate", d.rate);
        to.put("tran", d.transpose);
        to.put("gain", d.gain);
        to.put("out", d.out);
        }

    protected Object loadData(Motif motif, JSONObject from) 
        { 
        Data d = (Data)buildData(motif);
        d.transpose = from.optInt("tran", 0);
        d.rate = from.optDouble("rate", 1.0);
        d.gain = from.optDouble("gain", 1.0);
        d.out = from.optInt("out", 0);
        return d;
        }


    public static final int QUANTIZATION_NONE = 0;
    public static final int QUANTIZATION_SIXTEENTH = 1;
    public static final int QUANTIZATION_QUARTER = 2;
    public static final int QUANTIZATION_FOUR_QUARTERS = 3;
    
    public static final int[] QUANTIZATIONS = { 1,  Seq.PPQ / 4, Seq.PPQ, Seq.PPQ * 4 };
    public static final int DEFAULT_START_NOTE = 60;                            // Middle C
    
    public static final int MODE_SINGLE_ONE_SHOT = 0;
    public static final int MODE_SINGLE_REPEATING = 1;
    public static final int MODE_MULTI_ONE_SHOT = 2;
    public static final int MODE_MULTI_REPEATING = 3;

    /*
      int[] cc = new int[NUM_PARAMETERS];
      public int getCC(int index) { return cc[index]; }
      public void setCC(int index, int val) { cc[index] = val; }
    */
        
    int in = 0;
    //int ccIn = 0;
    int out = 0;
    int startNote = DEFAULT_START_NOTE;
    
    public static final int MAX_CHILDREN = 64;
    int fillPointer = 0;
    
    int mode = MODE_SINGLE_ONE_SHOT;
    // Should we automatically start playing the first child?
    boolean playFirst = true;
    // Should the selected child delay to a quantization boundary?
    int quantization = QUANTIZATION_SIXTEENTH;
    // Should we cut or release the previous child?
    boolean cut = false;
        
    /** Return Select's mode, one of MODE_SINGLE_ONE_SHOT, MODE_SINGLE_REPEATING, 
        MODE_MULTI_ONE_SHOT, or MODE_MULTI_REPEATING.  */
    public int getMode() { return mode; }
    /** Set Select's mode, one of MODE_SINGLE_ONE_SHOT, MODE_SINGLE_REPEATING, 
        MODE_MULTI_ONE_SHOT, or MODE_MULTI_REPEATING.  */
    public void setMode(int val) { mode = val; }
        
    /** Return whether children should be displaced via cut as opposed to release.  */
    public boolean getCut() { return cut; }
    /** Set whether children should be displaced via cut as opposed to release.  */
    public void setCut(boolean val) { cut = val; }
    
    /** Return whether child #0 should automatically start playing on start.  */
    public boolean getPlayFirst() { return playFirst; }
    /** Set whether child #0 should automatically start playing on start.  */
    public void setPlayFirst(boolean val) { playFirst = val; }
    
    /** Return the quantization of the onset of new children.  This one of
        QUANTIZATION_NONE, QUANTIZATION_SIXTEENTH, QUANTIZATION_QUARTER, or QUANTIZATION_FOUR_QUARTERS
        presently. */
    public int getQuantization() { return quantization; }
    
    /** Set the quantization of the onset of new children.  This one of
        QUANTIZATION_NONE, QUANTIZATION_SIXTEENTH, QUANTIZATION_QUARTER, or QUANTIZATION_FOUR_QUARTERS
        presently. */
    public void setQuantization(int val) { quantization = val; }
    
    /* Return the index in the Seq's In array for the In used to set parameters. */
    //public int getCCIn() { return ccIn; }
    
    /* Set the index in the Seq's In array for the In used to set parameters. */
    //public void setCCIn(int val) { ccIn = val; Prefs.setLastControlDevice(0, val, "seq.motif.select.Select"); }

    /** Return the index in the Seq's In array for the In used to read MIDI notes for triggering. */
    public int getIn() { return in; }
    
    /** Set the index in the Seq's In array for the In used to read MIDI notes for triggering. */
    public void setIn(int val) { in = val; Prefs.setLastInDevice(0, val, "seq.motif.select.Select"); }

    /** Return the index in the Seq's Out array for the Out used to light pads. */
    public int getOut() { return out; }
    
    /** Set the index in the Seq's Out array for the Out used to light pads. */
    public void setOut(int val) { out = val; Prefs.setLastOutDevice(0, val, "seq.motif.select.Select"); }

    int gridDevice = Pad.DEVICE_LAUNCHPAD_MKIII;
        
    /** Gets the grid device type. */
    public int getGridDevice() { return gridDevice; }
        
    /** Returns the grid device type. */
    public void setGridDevice(int val) { gridDevice = val; Prefs.setLastOutDevice(0, val, "seq.motif.select.Select"); }

    public Select(Seq seq)
        {
        super(seq);
        for(int i = 0; i < MAX_CHILDREN; i++)
            {
            addChild(new Blank(seq));
            }
        // Load devices. Note I'm not using setOut(...) etc. which would write the device to prefs
        out = (Prefs.getLastOutDevice(0, "seq.motif.select.Select"));
        in = (Prefs.getLastInDevice(0, "seq.motif.select.Select"));
        gridDevice = (Prefs.getLastGridDevice(0, "seq.motif.select.Select"));
        //ccIn = (Prefs.getLastControlDevice(0, "seq.motif.select.Select"));
        }
                
    public Clip buildClip(Clip parent)
        {
        return new SelectClip(seq, this, parent);
        }
    
    public void load(JSONObject from) throws JSONException
        {
        setPlayFirst(from.optBoolean("playfirst", true));
        setMode(from.optInt("mode", MODE_SINGLE_ONE_SHOT));
        setQuantization(from.optInt("quant", QUANTIZATION_SIXTEENTH));
        setCut(from.optBoolean("cut", false));
//        setStartNote(from.optInt("startnote", DEFAULT_START_NOTE));
        setIn(from.optInt("in", 0));
        setOut(from.optInt("out", 0));
        setGridDevice(from.optInt("grid", 0));
        /*
          JSONArray param = from.getJSONArray("cc");
          for(int i = 0; i < NUM_PARAMETERS; i++)
          {
          setCC(i, param.getInt(i));
          }
        */
        }
        
    public void save(JSONObject to) throws JSONException
        {       
        to.put("playfirst", getPlayFirst());
        to.put("mode", getMode());
        to.put("quant", getQuantization());
        to.put("cut", getCut());
//        to.put("startnote", getStartNote());
        to.put("in", getIn());
        to.put("out", getOut());
        to.put("grid", getGridDevice());
        /*
          JSONArray cc = new JSONArray();
          for(int i = 0; i < NUM_PARAMETERS; i++)
          {
          cc.put(getCC(i));
          }
          to.put("cc", cc);
        */
        }

    static int document = 0;
    static int counter = 1;
    public int getNextCounter() { if (document < Seq.getDocument()) { document = Seq.getDocument(); counter = 1; } return counter++; }
        
    public String getBaseName() { return "Select"; }
    }
        
