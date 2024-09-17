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
        boolean finishedPlaying;                // node finished last timestep and this timestep needs to clear
        boolean finallyFinishedPlaying;
        public Clip getClip() { return clip; }
        public Node(Clip clip, Parallel.Child node) { this.clip = clip; this.child = node;  }
        public boolean isPlaying() { return playing; }
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
        for(Node node : nodes)
            {
            if (node.clip.isPlaying()) 
                {
                node.clip.terminate();
                }
            }
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
        loadParameterValues(node.clip, node.child);
        node.clip.reset();
        node.lastPos = -1;
        node.cumulativeRate = 0;
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
        }

    public void determineMute()
        {
        Parallel parallel = (Parallel)getMotif();

        boolean[] muted = null;
        double[] prob = null;
        int numChildren = parallel.getNumChildrenToSelect();
        ArrayList<Motif.Child> children = parallel.getChildren();
        
        if (numChildren == Parallel.ALL_CHILDREN || numChildren == Parallel.ALL_CHILDREN_STOP_AFTER_FIRST || numChildren < children.size())
            {
            // do nothing
            }
        else if (numChildren == Parallel.INDEPENDENT)
            {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            int numRemainingChildren = children.size();
            for(int i = 0; i < numChildren; i++)
                {
                double weight = getCorrectedValueDouble(((Parallel.Data)(children.get(i).getData())).getProbability());
                nodes.get(i).muted = (weight == 1.0 ? true : (weight == 0.0 ? false : random.nextDouble() < weight));
                }
            }
        else    // need to build distribution
            {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            int numRemainingChildren = children.size();
            prob = new double[numRemainingChildren];
                
            for(int i = 0; i < muted.length; i++) 
                {
                nodes.get(i).muted = true;
                }
                        
            // Pick numChildren random children
            for(int i = 0; i < numChildren; i++)
                {
                double totalWeight = 0;
                // Build the distribution
                for(int j = 0; j < numChildren; j++)
                    {
                    Node node = nodes.get(i);
                    if (node.muted)         // hasn't been picked yet
                        {
                        totalWeight += getCorrectedValueDouble(((Parallel.Data)(children.get(j).getData())).getProbability());
                        }
                    }
                        
                if (totalWeight == 0.0)                                         // select uniformly, ugh O(n)
                    {
                    double count = numChildren;
                    for(int j = 0; j < numChildren; j++)
                        {
                        Node node = nodes.get(i);
                        if (node.muted)         // hasn't been picked yet
                            {
                            if (random.nextDouble() <= 1.0 / count)
                                {
                                // selected!
                                node.muted = false;
                                break;
                                }
                            }
                        count--;
                        }
                    }
                else                                                                            // select from distribution, ugh O(n)
                    {
                    double rand = random.nextDouble() * totalWeight;
                    double cdf = 0.0;
                    double val = random.nextDouble() * totalWeight;
                    for(int j = 0; j < numChildren; j++)
                        {
                        Node node = nodes.get(i);
                        if (node.muted)         // hasn't been picked yet
                            {
                            cdf += getCorrectedValueDouble(((Parallel.Data)(children.get(j).getData())).getProbability());
                            if (rand < cdf)
                                {
                                node.muted = false;
                                break;
                                }
                            }
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
        for(Node node : nodes)
            {
            node.clip.release();
            }
        }
    
    
    boolean advance(Node node, double rate)
        {
        if (rate == 1.0) return node.clip.advance();
        else
            {
            boolean result = false;
            node.cumulativeRate += rate;
            for( /* Empty */ ; node.lastPos < node.cumulativeRate; node.lastPos++)
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
            Parallel.Data data = ((Parallel.Data)(parallel.getChildren().get(i)).data);
            
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
                if (!node.finallyFinishedPlaying)
                    {
                    boolean done = advance(node, getCorrectedValueDouble(data.getRate(), Parallel.Data.MAX_RATE));                                          // done == we have JUST finished playing notes
                    node.finishedPlaying = node.finishedPlaying || done;

                    if (done)                                               
                        {
                        node.clip.terminate();
                        node.clip.release();
                        }
                    }
                    
                boolean finallyDone = node.finishedPlaying(getPosition());              // finallyDone == we have OFFICIALLY finished playing
                if (i == 0)
                    {
                    firstAdvanced = !finallyDone;
                    }
                    
                node.playing = !finallyDone;
                somebodyAdvanced = node.playing || somebodyAdvanced;           // Bug note: somebodyAdvanced must be SECOND or ParallelClip will act in series!
                }
                
            testOverriding = false;                                                                             // we're not testing any more
            }
        current = -1;
        return (!somebodyAdvanced || (!firstAdvanced && numChildren == Parallel.ALL_CHILDREN_STOP_AFTER_FIRST));
        }
 
    // Returns if the currently playing node is an override node
    boolean currentDataIsOverriding()
        {
        return ((Parallel.Data)(((Parallel)getMotif()).getChildren().get(current).data)).override;
        }
 
    // Releases all children greater than index which are not set to be overriding
    void releaseLaterChildren(int index)
        {
        Parallel parallel = (Parallel)getMotif();
        int len = nodes.size();
        for(int i = index; i < len; i++)
            {
            Parallel.Data data = ((Parallel.Data)(parallel.getChildren().get(index)).data);
            if (!data.override)
                {
                Node node = (Node)(nodes.get(i));
                node.clip.release();
                }
            }
        }

    public boolean noteOn(int out, int note, double vel) 
        {
        if (overriding && !currentDataIsOverriding())                   // If we're already overriding and we're not an override node, MUTE
            {
            return false;           // I'm being overridden
            }
        
        if (testOverriding)                                                             // If we're testing, we just passed the test
            {
            Node node = (Node)(nodes.get(current));
            node.localOverride = true;                                                  // turn on overriding
            overriding = true;                                                                  // turn on overriding
            releaseLaterChildren(current);                                          // get rid of existing notes
            }
        if (current >= 0)
            {
            Node node = nodes.get(current);
            Parallel.Data data = node.getData();
            if (data.getMute()) return false;            
            
            if (data.getOut() != Parallel.Data.DISABLED)
                {
                out = data.getOut();
                }
            note += getCorrectedValueInt(data.getTranspose() , Parallel.Data.MAX_TRANSPOSE * 2) - Parallel.Data.MAX_TRANSPOSE;
            if (note > 127) note = 127;                 // FIXME: should we instead just not play the note?
            if (note < 0) note = 0;                             // FIXME: should we instead just not play the note?
            vel *= getCorrectedValueDouble(data.getGain(), Parallel.Data.MAX_GAIN);
            if (vel > 127) vel = 127;                   // FIXME: should we check for vel = 0?
            }
        return super.noteOn(out, note, vel);
        }
        
    public boolean noteOff(int out, int note, double vel) 
        {
        if (overriding && !currentDataIsOverriding())                   // If we're already overriding and we're not an override node, MUTE
            {
            return false;           // I'm being overridden
            }
        if (current >= 0)
            {
            Node node = nodes.get(current);
            Parallel.Data data = node.getData();
            if (data.getOut() != Parallel.Data.DISABLED)
                {
                out = data.getOut();
                }
            note += getCorrectedValueInt(data.getTranspose(), Parallel.Data.MAX_TRANSPOSE * 2) - Parallel.Data.MAX_TRANSPOSE;
            if (note > 127) note = 127;                 // FIXME: should we instead just not play the note?
            if (note < 0) note = 0;                             // FIXME: should we instead just not play the note?
            }
        return super.noteOff(out, note, vel);
        }
        
    public void scheduleNoteOff(int out, int note, double vel, int time) 
        {
        if (overriding && !currentDataIsOverriding())                   // If we're already overriding and we're not an override node, MUTE
            {
            return;         // I'm being overridden
            }
        if (current >= 0)
            {
            Node node = nodes.get(current);
            Parallel.Data data = node.getData();
            if (data.getOut() != Parallel.Data.DISABLED)
                {
                out = data.getOut();
                }
            note += getCorrectedValueInt(data.getTranspose(), Parallel.Data.MAX_TRANSPOSE * 2) - Parallel.Data.MAX_TRANSPOSE;
            if (note > 127) note = 127;                 // FIXME: should we instead just not play the note?
            if (note < 0) note = 0;                             // FIXME: should we instead just not play the note?
            super.scheduleNoteOff(out, note, vel, (int)(time / getCorrectedValueDouble(data.getRate())));
            }
        else System.err.println("ParallelClip.scheduleNoteOff: current was " + current);
        }
 
    public boolean sysex(int out, byte[] sysex)
        {
        if (overriding && !currentDataIsOverriding())                   // If we're already overriding and we're not an override node, MUTE
            {
            return false;           // I'm being overridden
            }
        else return super.sysex(out, sysex);
        }

    public boolean bend(int out, int val) 
        {
        if (overriding && !currentDataIsOverriding())                   // If we're already overriding and we're not an override node, MUTE
            {
            return false;           // I'm being overridden
            }
        else return super.bend(out, val);
        }
        
    public boolean cc(int out, int cc, int val) 
        {
        if (overriding && !currentDataIsOverriding())                   // If we're already overriding and we're not an override node, MUTE
            {
            return false;           // I'm being overridden
            }
        else return super.cc(out, cc, val);
        }
        
    public boolean aftertouch(int out, int note, int val) 
        {
        if (overriding && !currentDataIsOverriding())                   // If we're already overriding and we're not an override node, MUTE
            {
            return false;           // I'm being overridden
            }
        else return super.aftertouch(out, note, val);
        }

    public boolean nrpn(int out, int nrpn, int val) 
        {
        if (overriding && !currentDataIsOverriding())                   // If we're already overriding and we're not an override node, MUTE
            {
            return false;           // I'm being overridden
            }
        else return super.nrpn(out, nrpn, val);
        }
        
    public boolean nrpnCoarse(int out, int nrpn, int msb) 
        {
        if (overriding && !currentDataIsOverriding())                   // If we're already overriding and we're not an override node, MUTE
            {
            return false;           // I'm being overridden
            }
        else return super.nrpnCoarse(out, nrpn, msb);
        }

    public boolean rpn(int out, int rpn, int val) 
        {
        if (overriding && !currentDataIsOverriding())                   // If we're already overriding and we're not an override node, MUTE
            {
            return false;           // I'm being overridden
            }
        else return super.rpn(out, rpn, val);
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
