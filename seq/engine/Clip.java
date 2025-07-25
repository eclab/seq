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
    // The motif corresponding to the Clip
    Motif motif;
    // The Clip's parent.  If the clip is the root, it parent is null.
    Clip parent;
    // The current timestamp of a playing Clip relative to the start of the Clip.
    int position;
    // Is the clip playing at the moment?
    boolean playing = false;
    // The Macro which owns this clip if any, else null
    seq.motif.macro.MacroClip owner = null;
    // The current version of the motif this Clip is designed to work with
    protected int version = 0;
    
    public Clip(Seq seq, Motif motif, Clip parent)
        {
        this.seq = seq;
        this.motif = motif;
        this.parent = parent;
        if (parent != null) 
            this.owner = parent.owner;
        }
    
    /** Returns true if the clip is playing at the moment. */
    public boolean isPlaying() { return playing; }
    
    
    
    ///// RELATIONSHIP WITH MACRO OWNER [IF ANY]
    
    /** Returns the Macro Clip owner of the clip, if the clip is inside a Macro.  If not, returns null. */
    public seq.motif.macro.MacroClip getOwner() { return owner; }

    /** Sets the Macro Clip owner of the clip, if the clip is inside a Macro.  If not, returns null. */
    public void setOwner(seq.motif.macro.MacroClip val) { owner = val; }
    
    
    
    ///// RELATIONSHIP WITH MOTIF
    /** Returns the clip's motif */
    public Motif getMotif() { return motif; }

    /** Rebuilds the clip to match its Motif. */
    public abstract void rebuild();

    /** Rebuilds the clip, or rebuilds any children and further descendant clips, only if they belong to the given Motif.
        The code should look something like:
        -  If this is MY MOTIF, then rebuild();
        -  Else for each child in my children, child.rebuild(motif);
    */ 
    public abstract void rebuild(Motif motif);
    
    /** Returns the current clip version. */
    public int getVersion() { return version; }




    ///// RELATIONSHIP WITH PARENTS

    /** Returns the parent. */
    public Clip getParent() { return parent; }
    
    /** Sets the parent. */
    public void setParent(Clip parent) { this.parent = parent; }






    ///// DYNAMIC PLAYING
    
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
    
    /** Resets the clip position to 0 and informs it that it will starting anew. */
    public void reset() 
        {
        position = 0;
        resetTriggers();
        if (this == seq.root) loadRootRandomValue();       // this is done only at reset and loop
        }

    /** Resets the clip position to 0 but doesn't reset the clip.  */
    public void loop() 
        {
        position = 0;
        if (this == seq.root) loadRootRandomValue();       // this is done only at reset and loop
        }
        
    /** Gets the current position */
    public int getPosition() { return position; }

    /** Plays the clip for the sliver of time at its current position.  The clip has already
        been set as its motif's playing clip, and the parameters have already been loaded. 
        Returns TRUE if the Clip believes it is FINISHED at the conclusion of this processing 
        (or beforehand). */
    public abstract boolean process();
                   
    /** Processes the current step and then advances the time.
        Returns TRUE if the Clip believes it has FINISHED at the
        conclusion of this step (or beforehand). */
    public boolean advance()
        {
        if (getMotif().getVersion() > version) rebuild();
        getMotif().setPlayingClip(this);
        if (this == seq.root) loadRootParameterValues();             // I am root, need to manually load parameters
        boolean result = process();

        // we increment the play count during advance() rather than reset()
        // so we can reset multiple times, particularly after rebuilding the clips after restructuring them motif topology,
        // This frees up reset() a bit.  This way we can terminate all the old clips, then
        // rebuild, then just reset ALL the new clips, but they don't turn on until they start
        // advancing.
        if (!playing) 
            {
            motif.incrementPlayCount();
            playing = true;
            }

        position++;
        return result;
        }
    
    
    
    

    //// PARAMETERS
    
    
    //// ABOUT PARAMETERS
    //// 
    //// Before a Clip is processed, its parameters are updated.  There are Motif.NUM_PARAMETERS
    //// total parameters plus a "random parameter".  The Clip can read its parameters and use
    //// them to change features of the Clip, or even pass them down to various parameters of
    //// its children.
    ////
    //// Parameters are doubles between 0.0 and 1.0 inclusive.  A Clip has access to the current
    //// value of each of its parameters and also to the PREVIOUS value, primarily to compare
    //// against the current value to see if it has changed.
    ////
    //// Each parameter also has a TRIGGER, which is a boolean initially false, meaning that it
    //// has not been triggered yet.  This enables parameters to indicate boolean values as
    //// events.  When a clip is RESET, all the triggers are set to false.  When a parameter is
    //// set to a value >= 0.5 and the previous value was < 0.5, meaning that it has "gone high",
    //// its trigger is set to true.  A clip can detect this and reset it back to false again.
    //// When the parameter "goes low" nothing happens.
    ////
    //// Parameter values are set as follows:
    ////        - If the parameter is bound to "Rand", it is set to a random value from 0.0 to 1.0
    ////    - Else if the parameter is bound to a parent parameter, the Clip's parent is queried
    ////      as to the value of its paranet parameter, and the parameter is then set to that value.
    ////    - Else the parameter is set to a fixed value as specified by the user.
    ////
    //// Unlike other parameters, the "random parameter" is only updated when the Clip is reset,
    //// not when it is processed.  The value is chosen between the Clip's "min" and "max" random 
    //// parameter values, which the user can specify.
    ////
    //// The "random parameter" cannot be set -- it's set at random -- but the "min" and "max"
    //// values can be set, in exactly the same way as the Parameter Values above.
    ////
    //// Parameters can have NAMES, specified in the Motif.
    ////
    //// The values passed into a parameter from their parent are called ARGUMENTS.  That is the
    //// correct computer science term but might be confusing to the user, so we might change them
    //// to VALUES or something, I dunno...
    ////
    //// A clip has various exposed variables -- the volume of a drum hit, or the pitch of a note, say --
    //// and the values of these variables are stored in the clip's MOTIF.  These values can be
    //// doubles or ints, and are either values >= 0.0, or they are set to specific negative integers:
    ////
    ////     -1:                The value is bound to the Clip's current random parameter value
    ////         -N (< -1):     An integer. The value is bound to the current value of parameter -N - 2 
    ////                [parameter numbers start at 0]
    ////
    //// To determine the current value of an exposed variable, you can call getCorrectedValInt()
    //// or getCorrectedValDouble(), which will return the "revised" value if the value is negative.
    
     
    // The CURRENT values of the parameters
    double[] parameterValues = new double[Motif.NUM_PARAMETERS];
    // The PREVIOUS values of the parameters
    double[] lastParameterValues = new double[Motif.NUM_PARAMETERS];
    // Are the triggers set?
    boolean[] triggers = new boolean[Motif.NUM_PARAMETERS];
    // The random parameter value
    double randomValue = 0.0;
        

    /** Returns the Random Value. */
    public double getRandomValue() { return randomValue; }
    
    /** Sets the Random Value. */
    public void setRandomValue(double val) { randomValue = val; }
        
    /** Computes and loads the random value into a child of this clip. */
    public void loadRandomValue(Clip child, Motif.Child motifChild)
        {
        double max = motifChild.getRandomMax();
        double randomMax = 0;
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
            System.err.println("Clip.loadRandomValue() error: random max was " + max);
            randomMax = 0;  
            }
                
        double min = motifChild.getRandomMin();
        double randomMin = 0;
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
            System.err.println("Clip.loadRandomValue() error: random min was " + min);
            randomMin = 0;  
            }

        child.setRandomValue(child.getMotif().generateRandomValue(randomMin, randomMax));
        }

    /** Loads the random value of this clip, assuming it is a root, and thus using the root's random min and max */
    public void loadRootRandomValue()
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
                
    /** 
        Loads the parameter values assuming that we are root, and thus using the root's values. Current options are:
        -  >=0: a ground value
        -       -1: bound to random
        -       -2 ... -(NUM_PARAMETERS + 1) inclusive: a link to a parent parameter
    */
    public void loadRootParameterValues()
        {
        double[] params = seq.getParameterValues();
        for(int i = 0; i < Motif.NUM_PARAMETERS; i++)
            {
            setParameterValue(i, params[i]);
            }
        }

    /** Sets a parameter to a given value. */
    public void setParameterValue(int param, double val) 
        { 
        lastParameterValues[param] = parameterValues[param];
        parameterValues[param] = val;
        if (lastParameterValues[param] < 0.5 && val >= 0.5)
            triggers[param] = true;
        }
                
    /** Returns the previous setting of this parameter. */
    public double getLastParameterValue(int param) 
        { 
        return lastParameterValues[param];
        }

    /** Returns the current setting of this parameter. */
    public double getParameterValue(int param) 
        { 
        return parameterValues[param]; 
        }
                
    /** Resets the trigger (turns it off). */
    public void resetTrigger(int param)
        {
        triggers[param] = false;
        }

    /** Resets all triggers (turns them off). */
    public void resetTriggers()
        {
        for(int i = 0; i < triggers.length; i++) 
            {
            triggers[i] = false;
            }
        }

    /** Returns true if the trigger is ON. */
    public boolean isTriggered(int param)
        {
        return triggers[param];
        }

    /** Returns true if the trigger is ON, and turns it off.  Else returns false. */
    public boolean checkAndResetTrigger(int param)
        {
        boolean val = triggers[param];
        if (val) triggers[param] = false;
        return val;
        }


    // This is a little trick for a tiny bit of inlining.
    // basicVal can be any of:
    //   -  >=0: a ground value
    //   -   -1: bound to random
    //   -   -2 ... -(NUM_PARAMETERS + 1) inclusive: a link to a parent parameter
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

    /** 
        Corrects the given basic value, loading parameter values. Current options are:
        -  >=0: a ground value
        -   -1: bound to random
        -   -2 ... -(NUM_PARAMETERS + 1) inclusive: a link to a parent parameter
    */
    public double getCorrectedValueDouble(double basicVal)
        {
        if (basicVal >= 0)
            {
            return basicVal;
            }
        else return getCorrectedValueDouble2(basicVal);
        }
    
    /** 
        Corrects the given basic value, loading parameter values. Current options are:
        -  >=0: a ground value
        -   -1: bound to random
        -   -2 ... -(NUM_PARAMETERS + 1) inclusive: a link to a parent parameter
    */
    public double getCorrectedValueDouble(double basicVal, double maxVal)
        {
        if (basicVal >= 0)
            {
            return basicVal;
            }
        else return getCorrectedValueDouble2(basicVal) * maxVal;
        }
    
    // This is a little trick for a tiny bit of inlining
    // basicVal can be any of:
    //   -  >=0: a ground value
    //   -   -1: bound to random
    //   -   -2 ... -(NUM_PARAMETERS + 1) inclusive: a link to a parent parameter
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

    /** 
        Corrects the given basic value, loading parameter values. Current options are:
        -  >=0: a ground value
        -   -1: bound to random
        -   -2 ... -(NUM_PARAMETERS + 1) inclusive: a link to a parent parameter
    */
    /// FIXME We May need a minVal too...
    public int getCorrectedValueInt(int basicVal, int maxVal)
        {
        if (basicVal >= 0)
            {
            return basicVal;
            }
        else return getCorrectedValueInt2(basicVal, maxVal);
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
        
    /** Sends a CC to the given Out, associated with a given note (for MPE).
        Returns true if the message was successfully sent.  */
    public boolean cc(int out, int note, int cc, int val) 
        {
        if (seq.root == this) return seq.cc(out, note, cc, val);
        // else if (parent == null) // uhh.....
        else return parent.cc(out, note, cc, val); 
        }
        
    /** Sends a bend to the given Out, associated with a given note (for MPE). Bend goes -8192...8191.
        Returns true if the message was successfully sent.  */
    public boolean bend(int out, int note, int val) 
        {
        if (seq.root == this) return seq.bend(out, note, val);
        // else if (parent == null) // uhh.....
        else return parent.bend(out, note, val); 
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
        You can pass in Out.CHANNEL_AFTERTOUCH for the note, and this will force the message to be sent
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
    }
