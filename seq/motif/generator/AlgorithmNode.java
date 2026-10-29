/* 
   Copyright 2026 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.generator;

import seq.engine.*;
import seq.motif.blank.*;
import seq.util.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;


/** 
	AlgorithmNode is to Algorithm as Clip is to Motif.  It is called to advance the algorithm
	over time.
*/

public class AlgorithmNode implements Cloneable
    {
    public Seq seq;
    public GeneratorClip clip;
    public Generator generator;
    public Algorithm algorithm;
    
    public AlgorithmNode(Seq seq, Generator generator, GeneratorClip clip, Algorithm algorithm)
        {
        this.seq = seq;
        this.generator = generator;
        this.clip = clip;
        this.algorithm = algorithm;
        }
    
    public AlgorithmNode copy()
        {
        AlgorithmNode other = null;
        try { other = (AlgorithmNode)(this.clone()); }
        catch (CloneNotSupportedException ex) { }
        return other;
        }

	/** Called when the Generator's clip.release() method is called. */
    public void release()
        {
        }
        
	/** Called when the Generator's clip.cut() method is called. */
    public void cut()
        {
        }
        
	/** Called when the Generator's clip.terminate() method is called. */
    public void terminate()
        {
        }
        
	/** Called when the Generator's clip.loop() method is called. */
    public void loop()
        {
        }
        
	/** Called when the Generator's clip.reset() method is called. */
    public void reset()
        {
        }
        
    public boolean process(ArrayList<GeneratorClip.Note> notes)
        {
        if ((getPosition() % Seq.PPQ) == 0)
            {
            note(60, 127, Seq.PPQ / 2, 127);
            }
        return false;
        }
        
	/** Returns Seq's deterministic random object. */
    public Random getRandom()
        {
        return clip.getRandom();
        }
    
	/** Returns the number of beats per measure. */
    public int getBar() 
        {
        return getSeq().getBar();
        }
    
	/** Returns the current time position in the song. */
    public int getPosition()
        {
        return clip.getPosition();
        }
        
	/** Returns the algorithm. */
    public Algorithm getAlgorithm() { return algorithm; }

	/** Returns the Generator's clip. */
    public GeneratorClip getClip() { return clip; }
    
	/** Returns the Seq. */
    public Seq getSeq()
        {
        return clip.getSeq();
        }
    
	/** Returns the Generator's child clip, if it exists, else null. */
    public Clip getChildClip()
        {
        return clip.getChild();
        }

	/** Returns the out device for the generator. */
    public int getOut()
        {
        Generator generator = (Generator)(getClip().getMotif());
        return generator.getOut();
        }
    
	/** Submits a note on and off message to Seq. */
    public void note(int note, double vel, int interval, double velOff)
        {
        GeneratorClip clip = getClip();
        Generator generator = (Generator)(clip.getMotif());
        int out = generator.getOut();
        if (out == -1) out = generator.getIn();
        int id = clip.sendNoteOn(out, note, vel);
        clip.sendScheduleNoteOff(out, note, velOff, interval, id);
        }

	/** Submits a note on message to Seq, and returns the ID for the message. */
	public int noteOn(int note, double vel)
        {
        GeneratorClip clip = getClip();
        Generator generator = (Generator)(clip.getMotif());
        int out = generator.getOut();
        if (out == -1) out = generator.getIn();
        return clip.sendNoteOn(out, note, vel);
        }

	/** Schedules a note off message to Seq.  The ID should match the ID of the corresponding Note On. */
	public void scheduleNoteOff(int note, double vel, int time, int id)
        {
        GeneratorClip clip = getClip();
        Generator generator = (Generator)(clip.getMotif());
        int out = generator.getOut();
        if (out == -1) out = generator.getIn();
        clip.sendScheduleNoteOff(out, note, vel, time, id);
        }

	/** Submit a note off message to Seq.  The ID should match the ID of the corresponding Note On. */
    public void noteOff(int note, double vel, int id)
        {
        GeneratorClip clip = getClip();
        Generator generator = (Generator)(clip.getMotif());
        int out = generator.getOut();
        if (out == -1) out = generator.getIn();
        clip.sendNoteOff(out, note, vel, id);
        }

    /** 
        Corrects the given basic value, loading parameter values. Current options are:
        -  >=0: a ground value
        -   -1: bound to random
        -   -2 ... -(NUM_PARAMETERS + 1) inclusive: a link to a parent parameter
    */
    public int getCorrectedValueInt(int basicVal, int maxVal)
        {
        return clip.getCorrectedValueInt(basicVal, maxVal);
        }
    
    /** 
        Corrects the given basic value, loading parameter values. Current options are:
        -  >=0: a ground value
        -   -1: bound to random
        -   -2 ... -(NUM_PARAMETERS + 1) inclusive: a link to a parent parameter
    */
    public double getCorrectedValueDouble(double basicVal, double maxVal)
        {
        return clip.getCorrectedValueDouble(basicVal, maxVal);
        }

    /** 
        Corrects the given basic value, loading parameter values. Current options are:
        -  >=0: a ground value
        -   -1: bound to random
        -   -2 ... -(NUM_PARAMETERS + 1) inclusive: a link to a parent parameter
    */
    public double getCorrectedValueDouble(double basicVal)
        {
        return clip.getCorrectedValueDouble(basicVal);
        }

    /** Writes the initial AlgorithmNode state to the the given JSONObject.  
    	If your algorithm can save runtime state, implement this and return OBJ.  Otherwise don't and return NULL (the default behavior). */
    public JSONObject save(JSONObject obj) throws JSONException
        {
        // does nothing by default
        return null;
        }

    /** Reads the initial AlgorithmNode state from the given JSObject.  
    	If your algorithm can save runtime state, implement this.  Otherwise don't (the default behavior). */
    public void load(JSONObject obj) throws JSONException
        {
        // does nothing by default
        }
    }
        
        
        
        
        
        
