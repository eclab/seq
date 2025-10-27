/* 
   Copyright 2025 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.modulation;

import seq.engine.*;
import seq.util.*;
import java.util.*;
import java.util.concurrent.*;

public class ModulationClip extends Clip
    {
    private static final long serialVersionUID = 1;
    
    /// The four nodes representing our modulation functions
    ArrayList<Node> nodes = new ArrayList<>();
    // Our child, if any
    Clip clip;
    
    int lastPos;
    double cumulativeRate = 0.0;
    int numTimes = 0;

    public class Node
        {
        // This is the lat value BEFORE mapping.  It's used by Same to extract the value from other nodes
        double lastValue;
        
        // Informs the Node that we have been reset
        public void reset(int index) { }

        public void process(int index) 
            {
            Modulation modulation = (Modulation)getMotif();
            lastValue = update(index, getPosition());
            setParameterValue(index, modulation.getFunction(index).map(lastValue));
            }

        public double update(int index, int position) { return getParameterValue(index); }
        
        public double getLastValue() { return lastValue; }
        }


    public class Same extends Node
        {
        public double update(int index, int position) 
            {
            Modulation modulation = (Modulation)getMotif();
            Modulation.Same func = (Modulation.Same)(modulation.getFunction(index));
            int as = func.getAs();
            if (as < index)
                {
                return nodes.get(as).getLastValue();                    // getParameterValue(as);
                }
            else return super.update(index, position);
            }
        }
        
        
    public class Constant extends Node
        {
        public double update(int index, int position) 
            {
            Modulation modulation = (Modulation)getMotif();
            Modulation.Constant func = (Modulation.Constant)(modulation.getFunction(index));
            return func.getValue();
            }
        }
        
        
    public class LFO extends Node
        {
        double target = -1;
        double lastTarget = -1;
        double lastX = -1;
        public void reset(int index) 
            {
            target = -1;
            lastTarget = -1; 
            lastX = -1;
            }

        public double update(int index, int position) 
            {
            Modulation modulation = (Modulation)getMotif();
            Modulation.LFO lfo = (Modulation.LFO)modulation.getFunction(index);
            
            int pos = 0;
            if (position >= lfo.getStart())
                {
                pos = position - lfo.getStart();
                }

            int type = lfo.getLFOType();
            int period = lfo.getPeriod();
            if (period == 0) period = 1;                        // we can't have a 0 length period

            double phase = lfo.getPhase();
            double variance = phase;
            if (type >= Modulation.TYPE_RANDOM)
                {
                phase = 0;
                }

            // Compute our current instantaneous phase, not modded 
            double postX = ((pos % period) / (double)period + phase);
            double x = postX % 1.0;         // instantaneous phase 
            double val = 0;
 
            double initial = lfo.getInitial();
            switch (type)
                {
                case Modulation.TYPE_SAW_UP:
                    val = x;
                    break;
                case Modulation.TYPE_SAW_DOWN:
                    val = 1.0 - x;
                    break;
                case Modulation.TYPE_SQUARE:
                    val = (x < 0.5 ? 0.0 : 1.0);
                    break;
                case Modulation.TYPE_TRIANGLE:
                    val = (x < 0.5 ? x * 2.0 : 1.0 - (x - 0.5) * 2.0);
                    break;
                case Modulation.TYPE_SINE:
                    val = (Math.sin(x * Math.PI * 2.0) + 1.0) / 2.0; 
                    break;
                case Modulation.TYPE_RANDOM:                    // Fall Thru
                case Modulation.TYPE_SAMPLE_AND_HOLD:
                    // Compute our last instantaneous phase, not modded 
                    double preX = (pos <= 0 ? -1 : (((pos - 1) % period) / (double)period + phase));
                    if (position < lfo.getStart())
                        {
                        val = initial;
                        lastTarget = initial;
                        target = initial;
                        break;
                        }
                    else if (position == lfo.getStart())
                        {
                        if (lastTarget == -1) 
                            {
                            lastTarget = seq.getDeterministicRandom().nextDouble();
                            }
                        target = seq.getDeterministicRandom().nextDouble();
                        }
                    else if (preX < 0.0 || (preX > postX) || period == 1)            // time to update.  period==1 is a special case 
                        {
                        double p = (seq.getDeterministicRandom().nextDouble() - 0.5) * variance;
                        double newTarget = lastTarget + p;
                        if (newTarget >= 1.0 || newTarget < 0.0)
                            {
                            newTarget = lastTarget - p;
                            }
                        lastTarget = target;
                        target = newTarget ;     
                        }
                                                
                    val = lastTarget + (type == Modulation.TYPE_RANDOM ? (target - lastTarget) * x : 0);
                    break;
                }
                        
            int start = lfo.getStart();
            int fadeIn = lfo.getFadeIn() + start;
            int length = lfo.getLength() + fadeIn;
            int fadeOut = lfo.getFadeOut() + length;
                        
            if (position >= start && position < fadeIn)
                {
                double alpha = (position - start) / (double)lfo.getFadeIn();
                return (alpha * val) + (1.0 - alpha) * initial;
                }
            else if (position >= fadeIn && position < length)
                {
                return val;
                }
            else if (position >= length && position < fadeOut)
                {
                double alpha = (position - length) / (double)lfo.getFadeOut();
                return ((1.0 - alpha) * val) + alpha * initial;
                }
            else
                {
                return initial;
                }
            }
        }


        
    public class Envelope extends Node
        {
        int stage;
        
        public void reset(int index) 
            {
            stage = -1;
            }
                        
        public double update(int index, int position) 
            {
            Modulation modulation = (Modulation)getMotif();
            Modulation.Envelope envelope = (Modulation.Envelope)modulation.getFunction(index);
            
            int startTime = envelope.getStart();
            double startTarget = envelope.getInitial();
            int numStages = envelope.getNumStages();
        
            // First handle when we haven't reached the first stage yet
            if (position < startTime || numStages == 0)
                {
                return startTarget;
                }
            
            // Next, we compute the raw time of each of the stages.  This is O(n) but whatever.
            int[] time = new int[numStages];
            int current = startTime;
            for(int i = 0; i < numStages; i++)
                {
                time[i] = envelope.getTime(i) + current;
                current = time[i];
                }
            int finalTime = time[numStages - 1];
            double finalTarget = 0;

            // Now we handle the situation where the position is beyond the envelope.
            if (position >= finalTime)
                {
                finalTarget = envelope.getTarget(numStages - 1);
                
                if (envelope.getRepeat())
                    {
                    // We're repeating.  We need to mod the position to back within the envelope region 
                    if (finalTime > 0)
                        {
                        position = (position - finalTime) % (finalTime - startTime) + startTime;
                        }
                    // There is no envelope -- they're all zero values.  So just reset to startTime
                    else
                        {
                        position = startTime;
                        }
                    startTarget = finalTarget;
                    }
                // We're NOT repeating.  So we just return the final target.
                else
                    {
                    return finalTarget;
                    }
                }
            

            // If our stage is negative -- we just started the envelope -- we set stage to 0.
            if (stage < 0)
                {
                stage = 0;
                }
                
            // If we are outside of our stage, we need to compute the next stage.
            else if ((stage == 0 && !(position >= startTime && position < time[0])) ||
                (stage > 0 && !(position >= time[stage - 1] && position < time[stage])))
                {
                // We have to figure out the stage.  First try the next stage
                if (stage < numStages - 1 && position >= time[stage] && position < time[stage + 1])
                    {
                    stage++;
                    }
                else  // Okay, search
                    {
                    for(int i = 0; i < numStages; i++)
                        {
                        if (position < time[i]);    
                            {
                            stage = i;
                            break;
                            }
                        }
                    }
                }
                

            // We now know what stage we're in, and the position is within the stage boundaries.
            // So now we perform interpolation.
            
            int time0 = 0;
            int time1 = 0;
            double target0 = 0;
            double target1 = 0;
            if (stage == 0)
                {
                time0 = startTime;
                target0 = startTarget;
                time1 = time[0];
                target1 = envelope.getTarget(0);
                }
            else
                {
                time0 = time[stage - 1];
                target0 = envelope.getTarget(stage - 1);
                time1 = time[stage];
                target1 = envelope.getTarget(stage);
                }
            
            if (envelope.getHold())
                {
                return target0;
                }
            else
                {
                return (((position - time0) / (double) (time1 - time0)) * (target1 - target0) + target0);
                }
            }
        }
        

    public class Step extends Node
        {
        public double update(int index, int position) 
            {
            Modulation modulation = (Modulation)getMotif();
            Modulation.Step step = (Modulation.Step)modulation.getFunction(index);
            
            int start = step.getStart();
            
            if (position < start)
                {
                return step.getInitial();
                }
            
            // determine stage
            int period = step.getPeriod();
            if (period == 0) period = 1;
            int numSteps = step.getNumSteps();
                        
            // If we're not repeating and have gone to far...
            if (!step.getRepeat() && position >= start + period * numSteps)
                {
                if (step.getTrigger())
                    {
                    return 0.0;
                    }
                else
                    {
                    return step.getInitial();           // step.getStep(numSteps - 1);
                    }
                }
                        
            // okay, we're repeating.  Figure the position and output i.
            int stage = (position - start) % (period * numSteps) / period;
            int remainder = (position - start) % (period * numSteps) % period;

            double val = step.getStep(stage);
            if (step.getTrigger())
                {
                if (remainder == 0) return (val > 0.0 ? 1.0 : 0.0);
                else return 0.0;
                }
            else
                {
                return val;
                }
            }
        }


    public ModulationClip(Seq seq, Modulation modulation, Clip parent)
        {
        super(seq, modulation, parent);
        rebuild();
        }
        
    public void rebuild(Motif motif)
        {
        if (this.getMotif() == motif)
            {
            rebuild();
            }
        }
                
    public void rebuild() 
        { 
        if (version < getMotif().getVersion())
            {
            buildClip();
            version = getMotif().getVersion();
            }
        }
    
    void buildClip()
        {
        Modulation modulation = (Modulation)getMotif();
        ArrayList<Motif.Child> children = modulation.getChildren();
        if (children.size() > 0)
            {
            Motif.Child child = children.get(0);
            clip = child.getMotif().buildClip(this);
            }
        buildNodes(modulation);
        }
    
    public void buildNodes(Modulation modulation)
    	{
        nodes.clear();
        for(int i = 0; i < Motif.NUM_PARAMETERS; i++)
            {
            nodes.add(buildNode(modulation, i));
            }
        }
        
    public Node buildNode(Modulation trans, int index)
        {
        String type = trans.getFunction(index).getType();
        if (Modulation.IDENTITY.equals(type))
            {
            return new Node();
            }
        else if (Modulation.LFO.equals(type))
            {
            return new LFO();
            }
        else if (Modulation.ENVELOPE.equals(type))
            {
            return new Envelope();
            }
        else if (Modulation.STEP.equals(type))
            {
            return new Step();
            }
        else if (Modulation.SAME.equals(type))
            {
            return new Same();
            }
        else if (Modulation.CONSTANT.equals(type))
            {
            return new Constant();
            }
        else
            {
            System.err.println("ModulationClip.buildNode() ERROR: unknown type " + type);
            return new Node();
            }
        }
    
    public void reset()
        {
        super.reset();
        Modulation modulation = (Modulation)getMotif();

        if (clip == null)
            {
            buildClip();
            }
        loadParameterValues(clip, modulation.getChildren().get(0));
        // FIXME: are these two in the right position?  Normally they're in resetNode()
        lastPos = -1;
        cumulativeRate = 0;
        
        numTimes = 0;
        clip.reset();

        for(int i = 0; i < Motif.NUM_PARAMETERS; i++)
            {
            nodes.get(i).reset(i);
            }
        }
        
    public void loop()
        {
        super.loop();
        Modulation modulation = (Modulation)getMotif();

        if (clip == null)
            {
            buildClip();
            }
        loadParameterValues(clip, modulation.getChildren().get(0));
        numTimes = 0;
        clip.reset();

        for(int i = 0; i < Motif.NUM_PARAMETERS; i++)
            {
            nodes.get(i).reset(i);
            }
        }
        
    public void terminate()
        {
        super.terminate();

        if (clip != null)
            {
            clip.terminate();
            }
        }
    
    public void cut() 
        {
        super.cut();
        
        if (clip != null)
            {
            clip.cut();
            }
        }
    

    public void release() 
        { 
        super.release();

        if (clip != null)
            {
            clip.release();
            }
        }
        

    boolean advanceChild(Modulation modulation)
        {
        loadParameterValues(clip, modulation.getChildren().get(0));
                
        double rate = modulation.getRate();   
        if (rate == 1.0) return clip.advance();
        else
            {
            boolean result = false;
            cumulativeRate += rate;
            for( /* Empty */ ; lastPos + 1.0 < cumulativeRate; lastPos++)
                {
                result = result || clip.advance();
                }
            return result;
            }
        }

	public int getNumTimes() { return numTimes; }
	
    public boolean process()
        {
        Modulation modulation = (Modulation)getMotif();

        boolean done = true;
        
        for(int i = 0; i < Motif.NUM_PARAMETERS; i++)
            {
            nodes.get(i).process(i);
            }

        if (clip != null)
            {
            //loadParameterValues(clip, modulation.getChildren().get(0));
            boolean val = advanceChild(modulation);
            if (val)		// all done.  Should we loop?
            	{
            	numTimes++;
            	if (numTimes > modulation.getRepeats())		// for example, if repeats is 4, then numTimes must have reached 5 to be done
            		{
            		return val;
            		}
            	else
            		{
            		clip.loop();
            		return false;
            		}
            	}
            else return val;
            }
        else
            {
            return true;
            }
        }


    public void noteOn(int out, int note, double vel, int id) 
        {
        Modulation modulation = (Modulation)getMotif();

        if (modulation.getOut() != Modulation.DISABLED)
                {
                out = modulation.getOut();
                }
		note += getCorrectedValueInt(modulation.getTranspose(), Modulation.MAX_TRANSPOSE * 2) - Modulation.MAX_TRANSPOSE;
		if (note > 127) note = 127;                 // FIXME: should we instead just not play the note?
		if (note < 0) note = 0;                             // FIXME: should we instead just not play the note?
		vel *= getCorrectedValueDouble(modulation.getGain(), Modulation.MAX_GAIN);
		if (vel > 127) vel = 127;                   // FIXME: should we check for vel = 0?
        super.noteOn(out, note, vel, id);
        }
        
    public void noteOff(int out, int note, double vel, int id) 
        {
        Modulation modulation = (Modulation)getMotif();

        if (modulation.getOut() != Modulation.DISABLED)
                {
                out = modulation.getOut();
                }
            note += getCorrectedValueInt(modulation.getTranspose(), Modulation.MAX_TRANSPOSE * 2) - Modulation.MAX_TRANSPOSE;
            if (note > 127) note = 127;                 // FIXME: should we instead just not play the note?
            if (note < 0) note = 0;                             // FIXME: should we instead just not play the note?
        super.noteOff(out, note, vel, id);
        }
        
    public void scheduleNoteOff(int out, int note, double vel, int time, int id) 
        {
        Modulation modulation = (Modulation)getMotif();

            if (modulation.getOut() != Modulation.DISABLED)
                {
                out = modulation.getOut();
                }
            note += getCorrectedValueInt(modulation.getTranspose(), Modulation.MAX_TRANSPOSE * 2) - Modulation.MAX_TRANSPOSE;
            if (note > 127) note = 127;                 // FIXME: should we instead just not play the note?
            if (note < 0) note = 0;                             // FIXME: should we instead just not play the note?
            super.scheduleNoteOff(out, note, vel, (int)(time / getCorrectedValueDouble(modulation.getRate())), id);
        }
        
    public int scheduleNoteOn(int out, int note, double vel, int time) 
        {
        Modulation modulation = (Modulation)getMotif();

            if (modulation.getOut() != Modulation.DISABLED)
                {
                out = modulation.getOut();
                }
            note += getCorrectedValueInt(modulation.getTranspose(), Modulation.MAX_TRANSPOSE * 2) - Modulation.MAX_TRANSPOSE;
            if (note > 127) note = 127;                 // FIXME: should we instead just not play the note?
            if (note < 0) note = 0;                             // FIXME: should we instead just not play the note?
            return super.scheduleNoteOn(out, note, vel, (int)(time / getCorrectedValueDouble(modulation.getRate())));
        }
        

    }




