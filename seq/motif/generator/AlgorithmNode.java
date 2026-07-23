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
    
    public void release()
        {
        }
        
    public void cut()
        {
        }
        
    public void terminate()
        {
        }
        
    public void loop()
        {
        }
        
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
        
    public Random getDeterministicRandom()
        {
        return getSeq().getDeterministicRandom();
        }
    
    public int getBar() 
        {
        return getSeq().getBar();
        }
    
    public int getPosition()
        {
        return clip.getPosition();
        }
        
    public Algorithm getAlgorithm() { return algorithm; }
    public GeneratorClip getClip() { return clip; }
    
    public Seq getSeq()
        {
        return clip.getSeq();
        }
    
    public Clip getChildClip()
        {
        return clip.getChild();
        }

    public int getOut()
        {
        Generator generator = (Generator)(getClip().getMotif());
        return generator.getOut();
        }
    
    public void note(int note, double vel, int interval, double velOff)
        {
        GeneratorClip clip = getClip();
        Generator generator = (Generator)(clip.getMotif());
        int out = generator.getOut();
        if (out == -1) out = generator.getIn();
        int id = clip.sendNoteOn(out, note, vel);
        clip.sendScheduleNoteOff(out, note, velOff, interval, id);
        }

    public void scheduleNoteOff(int note, double vel, int time, int id)
        {
        GeneratorClip clip = getClip();
        Generator generator = (Generator)(clip.getMotif());
        int out = generator.getOut();
        if (out == -1) out = generator.getIn();
        clip.sendScheduleNoteOff(out, note, vel, time, id);
        }

    public int noteOn(int note, double vel)
        {
        GeneratorClip clip = getClip();
        Generator generator = (Generator)(clip.getMotif());
        int out = generator.getOut();
        if (out == -1) out = generator.getIn();
        return clip.sendNoteOn(out, note, vel);
        }

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
    }
        
        
        
        
        
        
