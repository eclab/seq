/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.parallel;

import seq.engine.*;
import java.util.*;
import java.util.concurrent.*;

public class ParallelClip extends Clip
    {
    private static final long serialVersionUID = 1;

    public ParallelClip(Seq seq, Parallel parallel, Clip parent)
        {
        super(seq, parallel, parent);
        rebuild();
        }
        
    public static class Node
        {
        Parallel.Child child;
        Clip clip;
        // This the ParallelClip's last position in its internal frame of reference.  We want
        // this to keep up with cumulativeRate.
        int lastPos = 0;
        // This is the internal expected position of the node in its frame of reference.  It's
        // computed by adding the current rate into the cumulativeRate each time we are pulsed.
        double cumulativeRate = 0.0;
        boolean muted;
        boolean playing;
        boolean repeating;
        boolean finishedPlaying;                // node finished last timestep and this timestep needs to clear
        boolean finallyFinishedPlaying;
        public Clip getClip() { return clip; }
        public Node(Clip clip, Parallel.Child node) { this.clip = clip; this.child = node;  }
        public boolean isPlaying() { return playing; }
        public boolean isRepeating() { return repeating; }
        public Parallel.Data getData() { return (Parallel.Data)(child.getData()); }
        public boolean finishedPlaying(int position)
            {
            if (finallyFinishedPlaying) return true;
            if (!finishedPlaying) return false;
            int endingQuantization = getData().getEndingQuantization();
            if (endingQuantization == Parallel.QUANTIZATION_NONE) { finallyFinishedPlaying = true; return true; }
            else if (endingQuantization == Parallel.QUANTIZATION_SIXTEENTH && position % (Seq.PPQ / 4) == (Seq.PPQ / 4) - 1)  { finallyFinishedPlaying = true; return true; }
            else if (endingQuantization == Parallel.QUANTIZATION_QUARTER && position % Seq.PPQ == Seq.PPQ - 1)  { finallyFinishedPlaying = true; return true; }
            else if (endingQuantization == Parallel.QUANTIZATION_FOUR_QUARTERS && position % (Seq.PPQ * 4) == (Seq.PPQ * 4) - 1)  { finallyFinishedPlaying = true; return true; }
            else return false;
            }
        public void resetFinishedPlaying() { finishedPlaying = false; finallyFinishedPlaying = false; }
        // Indicates that the Node is currently overriding nodes below it.
        boolean localOverride;
        }
        
    ArrayList<Node> nodes = new ArrayList<>();

    /** The currently playing node in the nodes array at any particular time, or -1. */
    int current;
    
    /** Are we currently overriding other children's MIDI data? */
    boolean overriding;
    /** Is the current override node listening for his own  incoming notes to trigger the start of overriding? */
    boolean testOverriding;
    
    boolean ended;
    
    public ArrayList<Node> getNodes() { return nodes; }
    
    public void rebuild(Motif motif)
        {
        if (this.getMotif() == motif)
            {
            rebuild();
            }
        else
            {
            for(Node node : nodes)
                {
                if (node.clip != null) node.clip.rebuild(motif);
                else System.err.println("Warning: ParallelClip node " + node + " has no clip");
                }
            }
        }

    public void terminate() 
        { 
        super.terminate();
        int oldCurrent = current;
        // Certain clips, such as ArpeggioClip, attempt to schedule a note off
        // during termination.  So we need to temporarily set current to that 
        // node so it can successfully make it through scheduleNoteOff().
        // As a result we have to go through the nodes by index rather than
        // use an iterator here.
        for(int i = 0; i < nodes.size(); i++)
        	{
            if (nodes.get(i).clip.isPlaying()) 
                {
                current = i;
                nodes.get(i).clip.terminate();
                }
        	}
        current = oldCurrent;
        }

    public void rebuild()
        {
        release();
        terminate();

        Parallel parallel = (Parallel)getMotif();
        
        nodes.clear();
        for(Parallel.Child child : parallel.getChildren())
            {
            Node node = new Node(child.getMotif().buildClip(this), child);
            nodes.add(node);
            reset(node);
            }
        version = getMotif().getVersion();
        }
    
    void reset(Node node)
        {
        loadRandomValue(node.clip, node.child);
        node.clip.reset();
        node.lastPos = -1;
        node.cumulativeRate = 0;
        node.repeating = false;
        node.resetFinishedPlaying();
        node.localOverride = false;             // we start not overriding
        }

    public void loop()
        {
        super.loop();
        for(Node node : nodes)
            {
            node.clip.terminate();
            reset(node);            
            }
        determineMute();
        overriding = false;                             // we're not overriding
        testOverriding = false;
        }
                
    public void reset()  
        {
        super.reset();
        for(Node node : nodes)
            {
            reset(node);            
            }
        determineMute();
        overriding = false;                             // we're not overriding
        testOverriding = false;
        ended = false;
        }

    public void determineMute()
        {
        Parallel parallel = (Parallel)getMotif();

        boolean[] muted = null;
        double[] prob = null;
        int numChildrenToSelect = parallel.getNumChildrenToSelect();
        ArrayList<Motif.Child> children = parallel.getChildren();
                
        if (numChildrenToSelect == Parallel.ALL_CHILDREN)
            {
            int numChildren = children.size();
            for(int i = 0; i < numChildren; i++)
                {
                double weight = getCorrectedValueDouble(((Parallel.Data)(children.get(i).getData())).getProbability());
                if (weight == 0.0) nodes.get(i).muted = true;
                else if (weight == 1.0) nodes.get(i).muted = false;
                else nodes.get(i).muted = ThreadLocalRandom.current().nextDouble() < weight;
                }
            }
        else if (numChildrenToSelect == Parallel.ALL_CHILDREN_STOP_AFTER_FIRST)
            {
            int numChildren = children.size();
            nodes.get(0).muted = false;
            for(int i = 1; i < numChildren; i++)
                {
                double weight = getCorrectedValueDouble(((Parallel.Data)(children.get(i).getData())).getProbability());
                if (weight == 0.0) nodes.get(i).muted = true;
                else if (weight == 1.0) nodes.get(i).muted = false;
                else nodes.get(i).muted = ThreadLocalRandom.current().nextDouble() < weight;
                }
            }
        else if (numChildrenToSelect >= children.size())
            {
            int numChildren = children.size();
            for(int i = 0; i < numChildren; i++)
                {
                nodes.get(0).muted = false;
                }
            }
        else    // need to build distribution.  We do this in stupid O(n^2) fashion 
            {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            ArrayList<Node> candidates = new ArrayList<>(nodes);
            ArrayList<Double> weights = new ArrayList<>();
            
            // prepare weights
            for(int i = 0; i < children.size(); i++)
                {
                double weight = getCorrectedValueDouble(((Parallel.Data)(candidates.get(i).child.getData())).getProbability());
                weights.add(weight);
                }

            // prepare children
            for(int i = 0; i < candidates.size(); i++)
                {
                candidates.get(i).muted = true;
                }
            
            // Pick n children.  This will be O(n^2) because we have to update the distribution
            // each time.  If we were doing uniform selection we could do this in O(n) :-( :-(
            for(int i = 0; i < numChildrenToSelect; i++)
                {
                // Normalize weights
                double sum = 0.0;
                for(int j = 0; j < weights.size(); j++)
                    {
                    double weight = weights.get(j);
                    sum += weights.get(j);
                    }
                if (sum == 0)
                    {
                    for(int j = 0; j < weights.size(); j++)
                        {
                        weights.set(j, 1.0 / weights.size());
                        }
                    }
                else
                    {
                    for(int j = 0; j < weights.size(); j++)
                        {
                        weights.set(j, weights.get(j) / sum);
                        }
                    }

                // Find and remove node  - I suppose we could do this in O(lg n)....
                double pivot = random.nextDouble();
                sum = 0.0;
                for(int j = 0; j < weights.size(); j++)
                    {
                    sum += weights.get(j);
                    if (j == weights.size() - 1 || pivot < sum)     // it's the last one or we found the pivot
                        {
                        candidates.get(j).muted = false;
                        candidates.remove(j);
                        weights.remove((int)j);
                        break;
                        }
                    }
                }
            }
        }

    public void cut()  
        {
        for(Node node : nodes)
            {
            node.clip.cut();
            }
        }

    public void release()  
        {
        int oldCurrent = current;
        // Certain clips, such as ArpeggioClip, attempt to schedule a note off
        // during termination.  So we need to temporarily set current to that 
        // node so it can successfully make it through scheduleNoteOff().
        // As a result we have to go through the nodes by index rather than
        // use an iterator here.
        for(int i = 0; i < nodes.size(); i++)
        	{
            if (nodes.get(i).clip.isPlaying()) 
                {
                current = i;
                nodes.get(i).clip.release();
                }
        	}
        current = oldCurrent;
        }
    
    
    boolean advance(Node node, double rate)
        {
        loadParameterValues(node.clip, node.child);

        if (rate == 1.0) return node.clip.advance();
        else
            {
            boolean result = false;
            node.cumulativeRate += rate;
            for( /* Empty */ ; node.lastPos + 1.0 < node.cumulativeRate; node.lastPos++)
                {
                result = result || node.clip.advance();
                }
            return result;
            }
        }
        
    public boolean process()
        {
        Parallel parallel = (Parallel)getMotif();
        
        boolean somebodyAdvanced = false;
        boolean firstAdvanced = false;
        if (nodes.isEmpty()) return true;
        int len = nodes.size();
        
        int numChildren = parallel.getNumChildrenToSelect();
        ArrayList<Motif.Child> children = parallel.getChildren();

        overriding = false;                                             // we start with the assumption that we're not overriding, but this may change
        for(int i = 0; i < len; i++)
            {
            Node node = (Node)(nodes.get(i));
            Parallel.Data data = ((Parallel.Data)(parallel.getChildren().get(i)).getData());
            
            if (data.override && !node.localOverride)                                       // if we're an override node and we're not overriding yet
                {
                testOverriding = true;                                                                  // let's test for the moment
                }
                                
            if (node.localOverride)                                                                         // if we ARE overriding
                {
                overriding = true;                                                                              // let's make that happen.  We don't need to release, already done
                }

            if (!node.muted)
                {
                current = i;

                node.playing = false;
                if (data.getDelay() > getPosition())                                    // not yet
                    {
                    somebodyAdvanced = true;
                    if (i == 0)
                        {
                        firstAdvanced = true;
                        }
                    continue;
                    }
                
                // We only terminate/release if we haven't done so
                if (!node.finallyFinishedPlaying || node.repeating)
                    {
                    boolean done = advance(node, getCorrectedValueDouble(data.getRate(), Parallel.Data.MAX_RATE));                                          // done == we have JUST finished playing notes
                    node.finishedPlaying = node.finishedPlaying || done;

                    if (done)                                               
                        {
                        if (data.repeat)
                            {
                            node.clip.loop();
                            node.repeating = true;
                            }
                        else
                            {
                            node.clip.terminate();
                            node.clip.release();
                            }
                        }
                    }
                    
                boolean finallyDone = node.finishedPlaying(getPosition());              // finallyDone == we have OFFICIALLY finished playing
                if (i == 0)
                    {
                    firstAdvanced = !finallyDone;
                    }
                    
                node.playing = !finallyDone || node.repeating;
                somebodyAdvanced = (node.playing && !node.repeating) || somebodyAdvanced;           // Bug note: somebodyAdvanced must be SECOND or ParallelClip will act in series!
                }
                
            testOverriding = false;                                                                             // we're not testing any more
            }
        current = -1;

		// check if next time we'd be at our designated end.  If so, return true from now on.
        if (getPosition() >= parallel.getEnd() - 1)
        	{
        	if (!ended)
        		{
        		release();
        		ended = true;
        		}
        	return true;
        	}

        return (!somebodyAdvanced || (!firstAdvanced && numChildren == Parallel.ALL_CHILDREN_STOP_AFTER_FIRST));
        }
 
    // Returns if the currently playing node is an override node
    boolean currentDataIsOverriding()
        {
        return ((Parallel.Data)(((Parallel)getMotif()).getChildren().get(current).getData())).override;
        }
 
    // Releases all children greater than index which are not set to be overriding
    void releaseLaterChildren(int index)
        {
        Parallel parallel = (Parallel)getMotif();
        int len = nodes.size();
        for(int i = index; i < len; i++)
            {
            Parallel.Data data = ((Parallel.Data)(parallel.getChildren().get(index)).getData());
            if (!data.override)
                {
                Node node = (Node)(nodes.get(i));
                node.clip.release();
                }
            }
        }

    public void noteOn(int out, int note, double vel, int id) 
        {
        if (overriding && !currentDataIsOverriding())                   // If we're already overriding and we're not an override node, MUTE
            {
            return;
            }
        
        Node node = (Node)(nodes.get(current));
        if (testOverriding)                                                             // If we're testing, we just passed the test
            {
            node.localOverride = true;                                                  // turn on overriding
            overriding = true;                                                                  // turn on overriding
            releaseLaterChildren(current);                                          // get rid of existing notes
            }
            
        if (current >= 0)
            {
            Parallel.Data data = node.getData();
            if (data.getMute()) 
                {
                return;
                }        
            
            if (data.getOut() != Parallel.Data.DISABLED)
                {
                out = data.getOut();
                }
            note += getCorrectedValueInt(data.getTranspose() , Parallel.Data.MAX_TRANSPOSE * 2) - Parallel.Data.MAX_TRANSPOSE;
            if (note > 127) note = 127;                 // FIXME: should we instead just not play the note?
            if (note < 0) note = 0;                             // FIXME: should we instead just not play the note?
            vel *= getCorrectedValueDouble(data.getGain(), Parallel.Data.MAX_GAIN);
            // if (vel > 127) vel = 127;                   // FIXME: should we check for vel = 0?
            }

        Parallel parallel = (Parallel) getMotif();
        if (parallel.getCrossFadeOn())
            {
            if (current == 0)
                {
                vel = vel * (1.0 - getCorrectedValueDouble(parallel.getCrossFade()));
                }
            else if (current == 1)
                {
                vel = vel * getCorrectedValueDouble(parallel.getCrossFade());
                }
            }

        super.noteOn(out, note, vel, id);
        }
        
    public void noteOff(int out, int note, double vel, int id) 
        {
        if (overriding && !currentDataIsOverriding())                   // If we're already overriding and we're not an override node, MUTE
            {
            return;
            }
        if (current >= 0)			// Arpeggio will try to schedule during termination, when current == -1
            {
            Node node = nodes.get(current);
            Parallel.Data data = node.getData();
            if (data.getMute()) 
                {
                return;
                }        
            if (data.getOut() != Parallel.Data.DISABLED)
                {
                out = data.getOut();
                }
            note += getCorrectedValueInt(data.getTranspose(), Parallel.Data.MAX_TRANSPOSE * 2) - Parallel.Data.MAX_TRANSPOSE;
            if (note > 127) note = 127;                 // FIXME: should we instead just not play the note?
            if (note < 0) note = 0;                             // FIXME: should we instead just not play the note?
            }
        super.noteOff(out, note, vel, id);
        }
        
    public void scheduleNoteOff(int out, int note, double vel, int time, int id) 
        {
        if (overriding && !currentDataIsOverriding())                   // If we're already overriding and we're not an override node, MUTE
            {
            return;         // I'm being overridden
            }
        if (current >= 0)			// Arpeggio will try to schedule during termination, when current == -1
            {
            Node node = nodes.get(current);
            Parallel.Data data = node.getData();
            if (data.getMute()) 
                {
                return;
                }        
            if (data.getOut() != Parallel.Data.DISABLED)
                {
                out = data.getOut();
                }
            note += getCorrectedValueInt(data.getTranspose(), Parallel.Data.MAX_TRANSPOSE * 2) - Parallel.Data.MAX_TRANSPOSE;
            if (note > 127) note = 127;                 // FIXME: should we instead just not play the note?
            if (note < 0) note = 0;                             // FIXME: should we instead just not play the note?
            super.scheduleNoteOff(out, note, vel, (int)(time / getCorrectedValueDouble(data.getRate())), id);
            }
        }

    public int scheduleNoteOn(int out, int note, double vel, int time) 
        {
        if (overriding && !currentDataIsOverriding())                   // If we're already overriding and we're not an override node, MUTE
            {
            return noteID++;         // I'm being overridden
            }
        if (current >= 0)			// Arpeggio will try to schedule during termination, when current == -1
            {
            Node node = nodes.get(current);
            Parallel.Data data = node.getData();
            if (data.getMute()) 
                {
                return noteID++;
                }        
            if (data.getOut() != Parallel.Data.DISABLED)
                {
                out = data.getOut();
                }
            note += getCorrectedValueInt(data.getTranspose(), Parallel.Data.MAX_TRANSPOSE * 2) - Parallel.Data.MAX_TRANSPOSE;
            if (note > 127) note = 127;                 // FIXME: should we instead just not play the note?
            if (note < 0) note = 0;                             // FIXME: should we instead just not play the note?
            return super.scheduleNoteOn(out, note, vel, (int)(time / getCorrectedValueDouble(data.getRate())));
            }
        else
        	{
        	System.err.println("ParallelClip.scheduleNoteOn: current is " + current);
        	return noteID++;
        	}
        }
 
    public void sysex(int out, byte[] sysex)
        {
        if (overriding && !currentDataIsOverriding())                   // If we're already overriding and we're not an override node, MUTE
            {
            return;
            }
        else super.sysex(out, sysex);
        }

    public void bend(int out, int val) 
        {
        if (overriding && !currentDataIsOverriding())                   // If we're already overriding and we're not an override node, MUTE
            {
            return;
            }
        else super.bend(out, val);
        }
        
    public void cc(int out, int cc, int val) 
        {
        if (overriding && !currentDataIsOverriding())                   // If we're already overriding and we're not an override node, MUTE
            {
            return;
            }
        else super.cc(out, cc, val);
        }
        
    public void aftertouch(int out, int note, int val) 
        {
        if (overriding && !currentDataIsOverriding())                   // If we're already overriding and we're not an override node, MUTE
            {
            return;
            }
        else super.aftertouch(out, note, val);
        }

    public void nrpn(int out, int nrpn, int val) 
        {
        if (overriding && !currentDataIsOverriding())                   // If we're already overriding and we're not an override node, MUTE
            {
            return;
            }
        else super.nrpn(out, nrpn, val);
        }
        
    public void nrpnCoarse(int out, int nrpn, int msb) 
        {
        if (overriding && !currentDataIsOverriding())                   // If we're already overriding and we're not an override node, MUTE
            {
            return;
            }
        else super.nrpnCoarse(out, nrpn, msb);
        }

    public void rpn(int out, int rpn, int val) 
        {
        if (overriding && !currentDataIsOverriding())                   // If we're already overriding and we're not an override node, MUTE
            {
            return;
            }
        else super.rpn(out, rpn, val);
        }
        

      
    // TESTING
    public static void main(String[] args) throws Exception
        {
        // Set up Seq
        Seq seq = new Seq();            // starts timer running
        seq.setupForMIDI(ParallelClip.class, args, 0, 3);   // sets up MIDI in and out
        seq.setOut(0, new Out(seq, 0));         // Out 0 points to device 0 in the tuple.  This is too complex.
        seq.setOut(1, new Out(seq, 1));         // Out 0 points to device 0 in the tuple.  This is too complex.
        seq.setOut(2, new Out(seq, 2));         // Out 0 points to device 0 in the tuple.  This is too complex.
        
        // Set up our module structure
        Parallel parallel = new Parallel(seq);

        // Set up first StepSequence
        seq.motif.stepsequence.StepSequence dSeq = new seq.motif.stepsequence.StepSequence(seq, 2, 16);
        
        // Specify notes
        dSeq.setTrackNote(0, 60);
        dSeq.setTrackNote(1, 120);
        dSeq.setTrackOut(0, 0);
        dSeq.setTrackOut(1, 1);
        dSeq.setDefaultSwing(0.33);
        
        // Load the StepSequence with some data
        dSeq.setVelocity(0, 0, 1, true);
        dSeq.setVelocity(0, 4, 5, true);
        dSeq.setVelocity(0, 8, 9, true);
        dSeq.setVelocity(0, 12, 13, true);
        dSeq.setVelocity(1, 1, 2, true);
        dSeq.setVelocity(1, 2, 3, true);
        dSeq.setVelocity(1, 3, 4, true);
        dSeq.setVelocity(1, 5, 6, true);
        dSeq.setVelocity(1, 7, 8, true);
        dSeq.setVelocity(1, 9, 10, true);
        dSeq.setVelocity(1, 10, 11, true);
        dSeq.setVelocity(1, 15, 16, true);
        

        // Set up second StepSequence
        seq.motif.stepsequence.StepSequence dSeq2 = new seq.motif.stepsequence.StepSequence(seq, 2, 16);
        
        // Specify notes
        dSeq2.setTrackNote(0, 45);
        dSeq2.setTrackNote(1, 90);
        dSeq2.setTrackOut(0, 0);
        dSeq2.setTrackOut(1, 1);
        dSeq2.setDefaultSwing(0);
        
        // Load the StepSequence with some data
        dSeq2.setVelocity(0, 0, 1, true);
        dSeq2.setVelocity(0, 2, 5, true);
        dSeq2.setVelocity(0, 4, 9, true);
        dSeq2.setVelocity(0, 6, 13, true);
        dSeq2.setVelocity(1, 8, 2, true);
        dSeq2.setVelocity(1, 9, 3, true);
        dSeq2.setVelocity(1, 10, 4, true);
        dSeq2.setVelocity(1, 11, 6, true);
        dSeq2.setVelocity(1, 12, 2, true);
        dSeq2.setVelocity(1, 13, 3, true);
        dSeq2.setVelocity(1, 14, 4, true);
        dSeq2.setVelocity(1, 15, 6, true);

        // Load into Series
        seq.motif.series.Series series = new seq.motif.series.Series(seq);
        series.add(dSeq, 1, 0.0);
        series.add(dSeq2, 2, 0.0);

        // Set up third StepSequence
        seq.motif.stepsequence.StepSequence dSeq3 = new seq.motif.stepsequence.StepSequence(seq, 1, 32);
        
        // Specify notes
        dSeq3.setTrackNote(0, 120);
        dSeq3.setTrackOut(0, 2);
        dSeq3.setDefaultSwing(0);
        
        // Load the StepSequence with some data
        dSeq3.setVelocity(0, 1, 1, true);
        dSeq3.setVelocity(0, 3, 5, true);
        dSeq3.setVelocity(0, 5, 9, true);
        dSeq3.setVelocity(0, 7, 13, true);
        dSeq3.setVelocity(0, 9, 2, true);
        dSeq3.setVelocity(0, 11, 3, true);
        dSeq3.setVelocity(0, 13, 4, true);
        dSeq3.setVelocity(0, 15, 6, true);
        dSeq3.setVelocity(0, 17, 1, true);
        dSeq3.setVelocity(0, 19, 5, true);
        dSeq3.setVelocity(0, 21, 9, true);
        dSeq3.setVelocity(0, 23, 13, true);
        dSeq3.setVelocity(0, 25, 2, true);
        dSeq3.setVelocity(0, 27, 3, true);
        dSeq3.setVelocity(0, 29, 4, true);
        dSeq3.setVelocity(0, 31, 6, true);

        // Load into Parallel
        parallel.add(dSeq3, 1000);
        parallel.add(series, 0);
                
        // Build Clip Tree
        seq.setData(parallel);

        seq.reset();
        seq.play();

        seq.waitUntilStopped();
        }

    }
