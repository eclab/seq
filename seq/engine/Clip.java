/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.engine;

import seq.util.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import javax.sound.midi.*;

/**
   Abstract superclass of the Dynamic Data portion of all modules in the sequencer.
        
   <p>"Static Data" means data and parameters set by the user, as opposed to "Dynamic Data",
   which is the current play state information (such as the current timestep, or which
   sub-module is presently playing).  Static Data is handled by Motifs, and Dynamic Data 
   is handled by Clips.
        
   <p>The most important part of dynamic data is the position: this is the current playing 
   time relative to the start of the Clip, or 0 if we have not started yet.
        
   <p>Each Clip points to a single Motif which it uses when it is running.  Multiple Clips
   can point to the same Motif.

   <p>As it is part of a tree, a Clip has a single parent.
        
   <p>Clips play notes or generate other MIDI by calling various methods here in Clip.java.
   These methods in turn call the equivalent methods in their parents, and so on all the way
   up to the root, which then calls the appropriate methods in one or more Out objects in 
   the Seq.  Why don't Clips just call the methods in Out directly?  Because there may
   exist conversion or transformation parent or ancestor clips which need a chance to modify
   the MIDI data.  This is important though it does incur an O(lg n) cost per MIDI emit. 
**/

public abstract class Clip
    {
    private static final long serialVersionUID = 1;


    // The Seq
    protected Seq seq;
    // The name of the Clip.  By default it's the same as the name of the Motif, but can be changed
    String name;
    // The motif corresponding to the Clip
    Motif motif;
    // The Clip's parent.  If the clip is the root, it parent is null.
    Clip parent;
    // The current timestamp of a playing Clip relative to the start of the Clip.
    int position;
    boolean armed;
    boolean playing = false;
    // The Macro which owns this clip if any
    seq.motif.macro.MacroClip owner;
    // The current version of the motif this Clip is designed to work with
    protected int version = 0;
    
    public Clip(Seq seq, Motif motif, Clip parent)
        {
        this.seq = seq;
        this.motif = motif;
        this.name = motif.getName();
        this.parent = parent;
        if (parent != null) 
            this.owner = parent.owner;
        }
    
    
    public boolean isPlaying() { return playing; }
    
    ///// MACRO OWNER
    public seq.motif.macro.MacroClip getOwner() { return owner; }
    public void setOwner(seq.motif.macro.MacroClip val) { owner = val; }
    
    
    ///// NAME
    public String getName() { return name; }
    public void setName(String val) { if (val == null) val = ""; name = val; }
    
    
    ///// RELATIONSHIP WITH MOTIF
    public Motif getMotif() { return motif; }

    /** Rebuilds the clip to match its Motif. */
    public abstract void rebuild();

    /** Rebuilds the clip, or rebuilds any descendant clips, if they belong to the given Motif. */
    public abstract void rebuild(Motif motif);
    
    /** Returns the current clip version. */
    public int getVersion() { return version; }

    ///// DYNAMIC PLAYING
    
    private void clear() { }
    /** Sends NOTE OFF to clear all notes if need be */
    public void cut() { }
    
    /** Send delayed NOTE OFFs to the Seq to eventually clear them when their notes are finished */
    public void release() { }
    
    /** Inform the Clip that it is being destroyed or set aside. 
        By default this simply goes to the motif and optionally 
        removes the Clip as a playing clip. */
    public void terminate() 
        { 
        Motif motif = getMotif();
        boolean result = motif.removePlayingClip(this); 
        if (playing)
            {
            motif.decrementPlayCount();
            playing = false;
            }
        else 
            {
            //System.err.println("Terminated multiple times " + this);
            //new Throwable().printStackTrace();
            }
        }
    
    /** Returns TRUE if the Clip believes it is FINISHED at the conclusion
        of this processing (or beforehand). */
    public abstract boolean process();
                   
    /** Resets the clip position to 0 and informs it that it will starting anew. */
    public void reset() 
        {
        position = 0;
        resetTriggers();
        if (this == seq.root) loadRootRandomParameters();       // this is done only at reset
        }

    /** Resets the clip position to 0 but doesn't reset.  */
    public void loop() 
        {
        position = 0;
        }
        
    /** Gets the current position */
    public int getPosition() { return position; }

    /** Processes the current step and then advances the time.
        Returns TRUE if the Clip believes it has FINISHED at the
        conclusion of this step (or beforehand). */
    public boolean advance()
        {
        if (getMotif().getVersion() > version) rebuild();
        getMotif().setPlayingClip(this);
        if (this == seq.root) loadRootParameters();             // I am root, need to manually load parameters
        boolean result = process();

        // we increment the play count during advance() rather than reset()
        // so we can reset multiple times, particularly after rebuilding the clips after restructuring them motif topology,
        // This frees up reset() a bit.  This way we can terminate all the old clips, then
        // rebuild, then just reset ALL the new clips, but they don't turn on until they start
        // advancing.
        if (!playing) 
            {
            motif.incrementPlayCount();
//                      System.err.println("Play Count " + motif.getPlayCount());
            playing = true;
            }

        position++;
        return result;
        }
    
    
    
    


    ///// RELATIONSHIP WITH PARENTS

    /** Returns the parent. */
    public Clip getParent() { return parent; }
    /** Sets the parent. */
    public void setParent(Clip parent) { this.parent = parent; }




    //// PARAMETERS

    double[] parameterValues = new double[Motif.NUM_PARAMETERS];
    double[] lastParameterValues = new double[Motif.NUM_PARAMETERS];
    boolean[] triggers = new boolean[Motif.NUM_PARAMETERS];
    double randomValue = 0.0;
    public double getRandomValue() { return randomValue; }
    public void setRandomValue(double val) { randomValue = val; }
        
    double randomMin;
    double randomMax;
        
    public void loadRandomValues(Clip child, Motif.Child motifChild)
        {
        double max = motifChild.getRandomMax();
        if (max == Motif.Child.PARAMETER_RANDOM)        // it's bound to random
            {
            randomMax = getRandomValue();
            }
        else if (max < Motif.Child.PARAMETER_RANDOM)    // it's a back link, copy through
            {
            int backlink = -(int)(max - (Motif.Child.PARAMETER_RANDOM - 1));
            randomMax = parameterValues[backlink];
            }
        else if (max >= 0)      // it's a ground value
            {
            randomMax = max;
            }
        else 
            {
            System.err.println("Clip.loadRandomValues() error: random max was " + max);
            randomMax = 0;  
            }
                
        double min = motifChild.getRandomMin();
        if (min == Motif.Child.PARAMETER_RANDOM)        // it's bound to random
            {
            randomMin = getRandomValue();
            }
        else if (min < Motif.Child.PARAMETER_RANDOM)    // it's a back link, copy through
            {
            int backlink = -(int)(min - (Motif.Child.PARAMETER_RANDOM - 1));
            randomMin = parameterValues[backlink];
            }
        else if (min >= 0)      // it's a ground value
            {
            randomMin = min;
            }
        else 
            {
            System.err.println("Clip.loadRandomValues() error: random min was " + min);
            randomMin = 0;  
            }

        child.setRandomValue(child.getMotif().generateRandomValue(randomMin, randomMax));
        }

    public void loadRootRandomParameters()
        {
        double min = seq.getRandomMin();
        double max = seq.getRandomMax();
        setRandomValue(getMotif().generateRandomValue(min, max));
        }
        
    /** 
        Loads the child's parameter values based on our own. Current options are:
        -  >=0: a ground value
        -       -1: bound to random
        -       -2 ... -(NUM_PARAMETERS + 1) inclusive: a link to a parent parameter
    */
    public void loadParameterValues(Clip child, Motif.Child motifChild) 
        {
        double[] params = motifChild.getParameters();
        Motif childMotif = child.getMotif();
        for(int i = 0; i < params.length; i++)
            {
            double newVal = 0;
            if (params[i] == Motif.Child.PARAMETER_RANDOM)  // it's bound to random
                {
                newVal = getRandomValue();
                }
            else if (params[i] < Motif.Child.PARAMETER_RANDOM)      // it's a back link, copy through
                {
                int backlink = -(int)(params[i] - (Motif.Child.PARAMETER_RANDOM - 1));
                newVal = parameterValues[backlink];
                }
            else if (params[i] >= 0)        // it's a ground value
                {
                newVal = params[i];
                }
            else 
                {
                System.err.println("Clip.loadParameterValues() error: parameter " + i + " attempted set to value " + params[i]);
                newVal = 0;     
                }
            child.setParameterValue(i, newVal);
            }
        }
                
    public void loadRootParameters()
        {
        double[] params = seq.getParameterValues();
        for(int i = 0; i < Motif.NUM_PARAMETERS; i++)
            {
            setParameterValue(i, params[i]);
            }
        }

    public void setParameterValue(int param, double val) 
        { 
        lastParameterValues[param] = parameterValues[param];
        parameterValues[param] = val;
        if (lastParameterValues[param] < 0.5 && val >= 0.5)
            triggers[param] = true;
        }
                
    public double getLastParameterValue(int param) 
        { 
        return lastParameterValues[param];
        }

    public double getParameterValue(int param) 
        { 
        return parameterValues[param]; 
        }
                
    public void resetTrigger(int param)
        {
        triggers[param] = false;
        }

    public void resetTriggers()
        {
        for(int i = 0; i < triggers.length; i++) 
            {
            triggers[i] = false;
            }
        }

    public boolean isTriggered(int param)
        {
        return triggers[param];
        }

    public boolean checkAndResetTrigger(int param)
        {
        boolean val = triggers[param];
        if (val) triggers[param] = false;
        return val;
        }


    /** 
        Corrects the given basic value, loading parameter values. Current options are:
        -  >=0: a ground value
        -       -1: bound to random
        -       -2 ... -(NUM_PARAMETERS + 1) inclusive: a link to a parent parameter
    */
    public double getCorrectedValueDouble(double basicVal)
        {
        if (basicVal >= 0)
            {
            return basicVal;
            }
        else return getCorrectedValueDouble2(basicVal);
        }
    
    public double getCorrectedValueDouble(double basicVal, double maxVal)
        {
        if (basicVal >= 0)
            {
            return basicVal;
            }
        else return getCorrectedValueDouble2(basicVal) * maxVal;
        }
    
    // This is a little trick for a tiny bit of inlining
    double getCorrectedValueDouble2(double basicVal)
        {
        if (basicVal == Motif.Child.PARAMETER_RANDOM)
            {
            return getRandomValue();
            }
        else if (basicVal < Motif.Child.PARAMETER_RANDOM)
            {
            return getParameterValue(-(int)(basicVal - Motif.Child.PARAMETER_RANDOM + 1));
            }
        else
            {
            System.err.println("Clip.getCorrectValue: invalid param index " + basicVal + " for clip " + this);
            return 0;
            }
        }


    /// FIXME We May need a minVal too...
        
    public int getCorrectedValueInt(int basicVal, int maxVal)
        {
        if (basicVal >= 0)
            {
            return basicVal;
            }
        else return getCorrectedValueInt2(basicVal, maxVal);
        }
    
    // This is a little trick for a tiny bit of inlining
    int getCorrectedValueInt2(int basicVal, int maxVal)
        {
        if (basicVal == Motif.Child.PARAMETER_RANDOM)
            {
            int val = (int)(getRandomValue() * (maxVal + 1));
//              if (val > maxVal) val = maxVal;         // 1.0 case
            return val;
            }
        else if (basicVal < Motif.Child.PARAMETER_RANDOM)
            {
            int val = (int)(getParameterValue(-(int)(basicVal - Motif.Child.PARAMETER_RANDOM + 1)) * maxVal);
//              if (val > maxVal) val = maxVal;         // 1.0 case
            return val;
            }
        else
            {
            System.err.println("Clip.getCorrectValue: invalid param index " + basicVal + " for clip " + this);
            return 0;
            }
        }
        
    ///// MIDI METHODS
    
    /*
      protected class NoteDelay
      {
      int out;
      int note;
      double vel;
      int delay = 0;
      public static final int TYPE_ON = 0;
      public static final int TYPE_OFF = 1;
      public static final int TYPE_OFF_NO_VEL = 2; 
      int type;
                
      public void step()
      {
      if (delay > 0)
      {
      delay--;
      if (delay == 0) playDelay();
      }
      }
                        
      void playDelay()
      {
      if (type == TYPE_ON)
      {
      noteOn(out, note, vel);
      }
      else if (type == TYPE_OFF)
      {
      noteOff(out, note, vel);
      }
      else    // TYPE_OFF_VEL
      {
      noteOff(out, note);
      }
      }
                
      public void delayNoteOn(int out, int note, double vel, int delay)
      {
      if (this.delay > 0) playDelay();
      type = TYPE_ON;
      this.vel = vel;
      this.note = note;
      this.out = out;
      this.delay = delay;
      }
                        
      public void delayNoteOff(int out, int note, double vel, int delay)
      {
      if (this.delay > 0) playDelay();
      type = TYPE_OFF;
      this.vel = vel;
      this.note = note;
      this.out = out;
      this.delay = delay;
      }
                        
      public void delayNoteOff(int out, int note, int delay)
      {
      if (this.delay > 0) playDelay();
      type = TYPE_OFF_NO_VEL;
      this.note = note;
      this.out = out;
      this.delay = delay;
      }
      }
    */
        
    /** Sends a sysex message to the given out.  
        Returns true if the message was successfully sent.  */
    public boolean sysex(int out, byte[] sysex)
        {
        if (seq.root == this) return seq.sysex(out, sysex);
        // else if (parent == null) // uhh.....
        else return parent.sysex(out, sysex); 
        }

    /** Sends a note on to the given Out.  Note that velocity is expressed as a double.
        This is because it can go above 127 or between 0.0 and 1.0 if multiplied by various 
        gains, and then returned to reasonable values.  Ultimately it will be floored 
        to an int.  Returns true if the message was successfully sent.  */
    public boolean noteOn(int out, int note, double vel) 
        {
        if (seq.root == this) return seq.noteOn(out, note, vel);
        // else if (parent == null) // uhh.....
        else return parent.noteOn(out, note, vel); 
        }
        
    /** Sends a note off to the given Out.  Note that velocity is expressed as a double.
        this is because it can go above 127 or between 0.0 and 1.0 if multiplied by various 
        gains, and then returned to reasonable values.  Ultimately it will be floored 
        to an int.  Returns true if the message was successfully sent.  */
    public boolean noteOff(int out, int note, double vel) 
        {
        if (seq.root == this) return seq.noteOff(out, note, vel);
        // else if (parent == null) // uhh.....
        else return parent.noteOff(out, note, vel); 
        }
        
    /** Sends a note off to the given Out with default velocity. 
        Returns true if the message was successfully sent.  */
    public boolean noteOff(int out, int note) 
        {
        return noteOff(out, note, 64);
        }

    /** Sends a bend to the given Out. Bend goes -8192...8191.
        Returns true if the message was successfully sent.  */
    public boolean bend(int out, int val) 
        {
        if (seq.root == this) return seq.bend(out, val);
        // else if (parent == null) // uhh.....
        else return parent.bend(out, val); 
        }
        
    /** Sends a CC to the given Out. 
        Returns true if the message was successfully sent.  */
    public boolean cc(int out, int cc, int val) 
        {
        if (seq.root == this) return seq.cc(out, cc, val);
        // else if (parent == null) // uhh.....
        else return parent.cc(out, cc, val); 
        }
        
    /** Sends a polyphonic aftertouch change to the given Out.  If the Out is set
        up for only channel aftertouch, this will be converted to channel aftertouch. 
        Returns true if the message was successfully sent.  
        You can pass in CHANNEL_AFTERTOUCH for the note, and this will force the message to be sent
        as a channel aftertouch message regardless. */
    public boolean aftertouch(int out, int note, int val) 
        {
        if (seq.root == this) return seq.aftertouch(out, note, val);
        // else if (parent == null) // uhh.....
        else return parent.aftertouch(out, note, val); 
        }

    /** Sends an NRPN (MSB+LSB) to the given Out. 
        Returns true if the message was successfully sent.  */
    public boolean nrpn(int out, int nrpn, int val) 
        {
        if (seq.root == this) return seq.nrpn(out, nrpn, val);
        // else if (parent == null) // uhh.....
        else return parent.nrpn(out, nrpn, val); 
        }
        
    /** Sends an NRPN (MSB only) to the given Out. Thus if you send
        NRPN coarse parameter 42, it'll be sent as parameter 42 * 128.  
        Returns true if the message was successfully sent.  */
    public boolean nrpnCoarse(int out, int nrpn, int msb) 
        {
        if (seq.root == this) return seq.nrpnCoarse(out, nrpn, msb);
        // else if (parent == null) // uhh.....
        else return parent.nrpnCoarse(out, nrpn, msb); 
        }

    /** Sends an RPN (MSB+LSB) to the given Out. 
        Returns true if the message was successfully sent.  */
    public boolean rpn(int out, int rpn, int val) 
        {
        if (seq.root == this) return seq.rpn(out, rpn, val);
        // else if (parent == null) // uhh.....
        else return parent.rpn(out, rpn, val); 
        }
        
    /** Schedules a note off, with the given note pitch value and velocity, to be sent to the given Out at a time in the future RELATIVE to the current position.  
        Note that velocity is expressed as a double.
        this is because it can go above 127 or between 0.0 and 1.0 if multiplied by various 
        gains, and then returned to reasonable values.  Ultimately it will be floored 
        to an int. */
    public void scheduleNoteOff(int out, int note, double vel, int time) 
        {
        if (seq.root == this) seq.scheduleNoteOff(out, note, vel, time);
        // else if (parent == null) // uhh.....
        else parent.scheduleNoteOff(out, note, vel, time); 
        }
        
    /** Schedules a note off, with the given note pitch value and with velocity 64, to be sent to the given Out at a time in the future RELATIVE to the current position. 
        Don't override this one. */
    public void scheduleNoteOff(int out, int note, int time) 
        {
        scheduleNoteOff(out, note, 64, time);
        }
        

    ///// UTILITY
    ///// This is mostly copying code.  See the Step Sequencer for heavy use of this code.

    /** Returns true if the given message is a non-null ShortMessage of the type NOTE_ON with a non-zero velocity. */
    public static boolean isNoteOn(MidiMessage message)
        {
        if (message == null) return false;
        if (!(message instanceof ShortMessage)) return false;
        ShortMessage shortmessage = (ShortMessage) message;
        return (shortmessage.getCommand() == ShortMessage.NOTE_ON && shortmessage.getData2() > 0);
        }

    /** Returns true if the given message is a non-null ShortMessage of the type NOTE_OFF or
        is a NOTE_ON with a zero velocity. */
    public static boolean isNoteOff(MidiMessage message)
        {
        if (message == null) return false;
        if (!(message instanceof ShortMessage)) return false;
        ShortMessage shortmessage = (ShortMessage) message;
        return ((shortmessage.getCommand() == ShortMessage.NOTE_ON && shortmessage.getData2() == 0) ||
            (shortmessage.getCommand() == ShortMessage.NOTE_OFF));
        }

    /** Returns true if the given message is a CC. */
    public static boolean isCC(MidiMessage message)
        {
        if (message == null) return false;
        if (!(message instanceof ShortMessage)) return false;
        ShortMessage shortmessage = (ShortMessage) message;
        return (shortmessage.getCommand() == ShortMessage.CONTROL_CHANGE);
        }

    /** Returns true if the given message is a pitchbend. */
    public static boolean isPitchBend(MidiMessage message)
        {
        if (message == null) return false;
        if (!(message instanceof ShortMessage)) return false;
        ShortMessage shortmessage = (ShortMessage) message;
        return (shortmessage.getCommand() == ShortMessage.PITCH_BEND);
        }

    /** Returns true if the given message is channel aftertouch. */
    public static boolean isChannelAftertouch(MidiMessage message)
        {
        if (message == null) return false;
        if (!(message instanceof ShortMessage)) return false;
        ShortMessage shortmessage = (ShortMessage) message;
        return (shortmessage.getCommand() == ShortMessage.CHANNEL_PRESSURE);
        }

    /** Returns true if the given message is poly aftertouch. */
    public static boolean isPolyphonicAftertouch(MidiMessage message)
        {
        if (message == null) return false;
        if (!(message instanceof ShortMessage)) return false;
        ShortMessage shortmessage = (ShortMessage) message;
        return (shortmessage.getCommand() == ShortMessage.POLY_PRESSURE);
        }

    /** Copies an int[][] array exactly. */
    public static int[][] copy(int[][] array) { return Motif.copy(array); }
    /** Copies a double[][] array exactly. */
    public static double[][] copy(double[][] array) { return Motif.copy(array); }
    /** Copies a boolean[][] array exactly. */
    public static boolean[][] copy(boolean[][] array) { return Motif.copy(array); }
    /** Copies an int[] array exactly. */
    public static int[] copy(int[] array) { return Motif.copy(array); }
    /** Copies a double[] array exactly. */
    public static double[] copy(double[] array) { return Motif.copy(array); }
    /** Copies a String[] array exactly. */
    public static String[] copy(String[] array) { return Motif.copy(array); }
    /** Copies a boolean[] array exactly. */
    public static boolean[] copy(boolean[] array) { return Motif.copy(array); }




    }
