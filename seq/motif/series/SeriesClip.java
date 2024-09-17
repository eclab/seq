/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.series;

import seq.engine.*;
import java.util.*;
import java.util.concurrent.*;

public class SeriesClip extends Clip
    {
    private static final long serialVersionUID = 1;

    // This is the parameter the musician can set to stipulate WHICH child to play if we're in variation mode
    public static final int VARIATION_PARAMETER = 0;

    protected Clip playing = null;                              // What is the current playing clip?
    protected Clip previous = null;                             // What was the last clip that played?
    protected int playingIndex = -1;                    // Which node is playing?
    protected int playingRepeat;                                // How many times has the node repeated so far?
    int counter = 0;                                                    // How many times have we iterated (for round robin)?  Used to compute the playingIndex in that case.
    boolean roundRobinFinished = false;                 // Has the round robin child finished yet?

    public void terminate() 
        { 
        super.terminate();
        if (playing != null) playing.terminate();
        }

    public static class Node
        {
        Series.Child child;
        Clip clip;
        // This the Node's last position in its internal frame of reference.  We want
        // this to keep up with cumulativeRate.
        int lastPos = 0;
        // This is the internal expected position of the node in its frame of reference.  It's
        // computed by adding the current rate into the cumulativeRate each time we are pulsed.
        double cumulativeRate = 0.0;
        boolean finishedPlaying;                // node finished last timestep and this timestep needs to clear
        boolean finallyFinishedPlaying;
        public Clip getClip() { return clip; }
        public Series.Data getData() { return (Series.Data)(child.getData()); }
        public Node(Clip clip, Series.Child node) { this.clip = clip; this.child = node;  }
        public boolean finishedPlaying(int position)
            {
            if (finallyFinishedPlaying) return true;
            if (!finishedPlaying) return false;
            int endingQuantization = getData().getEndingQuantization();
            if (endingQuantization == Series.QUANTIZATION_NONE) { finallyFinishedPlaying = true; return true; }
            else if (endingQuantization == Series.QUANTIZATION_SIXTEENTH && position % (Seq.PPQ / 4) == (Seq.PPQ / 4) - 1)  { finallyFinishedPlaying = true; return true; }
            else if (endingQuantization == Series.QUANTIZATION_QUARTER && position % Seq.PPQ == Seq.PPQ - 1)  { finallyFinishedPlaying = true; return true; }
            else if (endingQuantization == Series.QUANTIZATION_FOUR_QUARTERS && position % (Seq.PPQ * 4) == (Seq.PPQ * 4) - 1)  { finallyFinishedPlaying = true; return true; }
            else return false;
            }
        public void resetFinishedPlaying() { finishedPlaying = false; finallyFinishedPlaying = false; }
        }
    
    int[] ordering = null;
    int orderingIndex = 0;
    ArrayList<Node> nodes = new ArrayList<>();
    public ArrayList<Node> getNodes() { return nodes; }
        
    public SeriesClip(Seq seq, Series series, Clip parent)
        {
        super(seq, series, parent);
        rebuild();
        }
    
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
                if (node.clip != null) 
                    node.clip.rebuild(motif);
                else System.err.println("Warning: SeriesClip node " + node + " has no clip");
                }
            }
        }

    public void rebuild()
        {
        release();
        terminate();

        nodes.clear();
        version = getMotif().getVersion();
        }
        
    public int getPlayingIndex() { return playingIndex; }
    public Clip getPlayingClip() { return playing; }
    public int getPlayingRepeat() { return playingRepeat; }
            
    public Node getNode(int child)
        {
        int size = nodes.size();
        if (size <= child)
            {
            for(int i = size; i <= child; i++)
                {
                nodes.add(null);
                }
            }

        Node node = nodes.get(child);
        if (node == null)
            {
            Series series = (Series)getMotif();
            Series.Child _child = series.getChildren().get(child);
            Series.Data data = (Series.Data)(_child.getData());
            node = new Node(_child.getMotif().buildClip(this), _child);
            nodes.set(child, node);
            reset(node);
            }
                        
        return node;
        }
          
    public void cut()  
        {
        if (previous != null)
            {
            previous.cut();
            }
        if (playing != null) 
            {
            playing.cut();
            }
        }
        
    public void release()  
        {
        if (previous != null)
            {
            previous.release();
            }
        if (playing != null)
            {
            playing.release();
            }
        }
                
    boolean advance(Node node, double rate)
        {
        loadParameterValues(node.clip, node.child);                                     // this is done every time it's advanced
                   
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

    public void loop()
        {
        super.loop();
        resetChild();
        }
                
    public void reset()  
        {
        super.reset();
        counter = -1;           // set here so reset() increments it properly if we're doing round robin
        resetChild();
        Series series = (Series) getMotif();
//        lfo.reset(series.getLFO());
        }
                
    void reset(Node node)
        {
        resetTriggers();
        loadRandomValues(node.clip, node.child);
        node.clip.reset();
        node.lastPos = -1;
        node.cumulativeRate = 0;
        node.finallyFinishedPlaying = false;
        node.finishedPlaying = false;
        }
        
    /** Shuffles an int array properly. */
    public void shuffle(int[] array, java.util.Random random)
        {
        for(int x=array.length - 1; x >= 1 ; x--)
            {
            int rand = random.nextInt(x+1);
            int obj = array[x];
            array[x] = array[rand];
            array[rand] = obj;
            }
        }


    void rebuildOrdering(ArrayList<Motif.Child> kids)
        {
        ordering = new int[kids.size()];
        for(int i = 0; i < ordering.length; i++) ordering[i] = i;
        shuffle(ordering, ThreadLocalRandom.current());
        orderingIndex = 0;
        playingIndex = ordering[orderingIndex];
        }

    public void resetChild()
        {
        Series series = (Series)getMotif();
        ArrayList<Motif.Child> kids = series.getChildren(); 
        roundRobinFinished = false;
        
        previous = null;
        if (kids.size() > 0)
            {
            int mode = ((Series)getMotif()).getMode();
            if (mode == Series.MODE_SERIES || mode == Series.MODE_SHUFFLE)
                {
                previous = playing;
                if (previous != null) previous.terminate();
                playingIndex = 0;
                
                if (mode == Series.MODE_SHUFFLE)
                    {
                    rebuildOrdering(kids);
                    }
                
                if (seq.getData() == series)
                    {
                    for(int i = 0; i < kids.size(); i++)
                        {
                        if (((Series.Data)kids.get(i).getData()).getStart())
                            {
                            playingIndex = i;
                            break;
                            }
                        }
                    }
                    
                Node playingNode = getNode(playingIndex);
                playing = playingNode.clip;
                reset(playingNode);
                playingRepeat = 0;
                playingNode.resetFinishedPlaying();
                }
            else if (mode == Series.MODE_RANDOM || mode == Series.MODE_MARKOV)
                {
                playingIndex = ThreadLocalRandom.current().nextInt(kids.size());
                previous = playing;
                if (previous != null) previous.terminate();
                Node playingNode = getNode(playingIndex);
                playing = playingNode.clip;
                reset(playingNode);
                playingRepeat = 0;
                playingNode.resetFinishedPlaying();
                }
            else if (mode == Series.MODE_ROUND_ROBIN)
                {
                counter++;
                playingIndex = counter % kids.size();
                previous = playing;
                if (previous != null) previous.terminate();
                Node playingNode = getNode(playingIndex);
                playing = playingNode.clip;
                reset(playingNode);
                playingRepeat = 0;
                playingNode.resetFinishedPlaying();
                }
            else if (mode == Series.MODE_VARIATION)
                {
                playingIndex = (int)(getParameterValue(VARIATION_PARAMETER) * kids.size());
                if (playingIndex == kids.size()) playingIndex--;
                previous = playing;
                if (previous != null) previous.terminate();
                Node playingNode = getNode(playingIndex);
                playing = playingNode.clip;
                reset(playingNode);
                playingRepeat = 0;
                playingNode.resetFinishedPlaying();
                }
            else if (mode == Series.MODE_RANDOM_VARIATION)
                {
                playingIndex = (int)(getRandomValue() * kids.size());
                if (playingIndex == kids.size()) playingIndex--;
                previous = playing;
                if (previous != null) previous.terminate();
                Node playingNode = getNode(playingIndex);
                playing = playingNode.clip;
                reset(playingNode);
                playingNode.resetFinishedPlaying();
                playingRepeat = 0;
                }
            }
        else 
            {
            playing = null;
            playingIndex = -1;
            }
        }

    // returns -1 if there are no children
    public int selectRandomChild(Series series, int from, java.util.Random random)
        {
        ArrayList<Motif.Child> c = series.getChildren();
        if (c.size() == 0) // uh oh
            return -1;
        if (c.size() == 1) // hmmm
            return 0;
        
        double[] weights = ((Series.Data)(c.get(from).getData())).getWeights();
        // we'll do it the dumb way
        double sum = 0;
        for(int i = 0; i < weights.length; i++)
            {
            sum += weights[i];
            }
        if (sum == 0) // pick at random
            {
            return random.nextInt(weights.length);
            }
        else    
            {
            double pivot = random.nextDouble() * sum;
            double cdf = 0;
            for(int i = 0; i < weights.length - 1; i++)
                {
                cdf += weights[i];
                if (pivot < cdf) return i;
                }
            return weights.length - 1;  // last one
            }
        }
    
    public static final int TRIGGER_PARAMETER = 7;
        
    public boolean process()
        {
        Series series = (Series) getMotif();
//        currentLFOValue = lfo.getCurrentValue(series.getLFO());
        
        ArrayList<Motif.Child> kids = series.getChildren(); 
        if (playingIndex >= kids.size()) return true;  // all done

        if (playing == null) return true;   // no more clips
                
        if (playing.getPosition() == 0) // we're about to start the next clip
            {
            if (previous != null) 
                {
                previous.release();
                }
            }
        
        Series.Data data = (Series.Data)(kids.get(playingIndex).getData());
        Node node = getNode(playingIndex);
        boolean done = advance(node, getCorrectedValueDouble(data.getRate(), Series.Data.MAX_RATE));                                            // done == we have JUST finished playing notes
        node.finishedPlaying = node.finishedPlaying || done;
                
        if (node.finishedPlaying(getPosition())) // all done with this clip's iteration
            {
            playingRepeat++;
            boolean repeatUntilTrigger = data.getRepeatUntilTrigger();
        
            java.util.Random random = ThreadLocalRandom.current();
            if ((repeatUntilTrigger && checkAndResetTrigger(TRIGGER_PARAMETER)) ||
                    (!repeatUntilTrigger && (playingRepeat > getCorrectedValueInt(data.getRepeatAtLeast(), Series.Data.MAX_REPEAT_VALUE)) && 
                    (random.nextDouble() >= getCorrectedValueDouble(data.getRepeatProbability()))))
                {
                // NO MORE ITERATIONS

                int mode = ((Series)getMotif()).getMode();
                if (mode == Series.MODE_SERIES)
                    {
                    playing.release();
                    playing.terminate();
                    // next clip
                    playingIndex++;
                    if (playingIndex >= kids.size()) return true;  // all done
                                                
                    previous = playing;
                    previous.terminate();
                    Node playingNode = getNode(playingIndex);
                    playing = playingNode.clip;
                    reset(playingNode);
                    playingRepeat = 0;
                    return false;
                    }
                else if (mode == Series.MODE_SHUFFLE)
                    {
                    playing.release();
                    playing.terminate();
                    // next clip
                    if (ordering == null || ordering.length != kids.size())
                        {
                        rebuildOrdering(kids);
                        }
                    else orderingIndex++;
                    if (orderingIndex >= kids.size()) return true;  // all done
                    playingIndex = ordering[orderingIndex];
                     
                    previous = playing;
                    previous.terminate();
                    Node playingNode = getNode(playingIndex);
                    playing = playingNode.clip;
                    reset(playingNode);
                    playingRepeat = 0;
                    return false;
                    }
                else if (mode == Series.MODE_RANDOM || mode == Series.MODE_MARKOV)
                    {
                    // find someone new
                    playing.release();
                    playing.terminate();
                    int old = playingIndex;
                    for(int i = 0; i < 10; i++)
                        {
                        if (mode == Series.MODE_RANDOM)
                            {
                            playingIndex = random.nextInt(kids.size());
                            }
                        else
                            {
                            playingIndex = selectRandomChild(series, old, random);
                            }
                        if (playingIndex != old && playingIndex >= 0) break;
                        }
                    previous = playing;
                    previous.terminate();
                    Node playingNode = getNode(playingIndex);
                    playing = playingNode.clip;
                    reset(playingNode);
                    playingRepeat = 0;
                    return false;
                    }
                else if (mode == Series.MODE_ROUND_ROBIN)
                    {
                    if (roundRobinFinished) return true;
                
                    playing.release();
                    playing.terminate();
                    roundRobinFinished = true;
                    return false;
                    }
                else    // mode == Series.MODE_VARIATION
                    {
                    playing.release();
                    playing.terminate();
                    return true;                            // all done
                    }
                }
            else
                {
                Node playingNode = getNode(playingIndex);
                playingNode.resetFinishedPlaying();
                playing.loop();
                return false;
                }
            }
        else return false;
        }
   
    public boolean noteOn(int out, int note, double vel) 
        {
        if (playingIndex >= 0)
            {
            Node node = getNode(playingIndex);
            Series.Data data = node.getData();
            if (data.getOut() != Series.Data.DISABLED)
                {
                out = data.getOut();
                }
            note += getCorrectedValueInt(data.getTranspose(), Series.Data.MAX_TRANSPOSE * 2) - Series.Data.MAX_TRANSPOSE;
            note = data.adjustNote(note);
            if (note > 127) note = 127;                 // FIXME: should we instead just not play the note?
            if (note < 0) note = 0;                             // FIXME: should we instead just not play the note?
            vel *= getCorrectedValueDouble(data.getGain(), Series.Data.MAX_GAIN);
            if (vel > 127) vel = 127;                   // FIXME: should we check for vel = 0?
            }
        return super.noteOn(out, note, vel);
        }
        
    public boolean noteOff(int out, int note, double vel) 
        {
        if (playingIndex >= 0)
            {
            Node node = getNode(playingIndex);
            Series.Data data = node.getData();
            if (data.getOut() != Series.Data.DISABLED)
                {
                out = data.getOut();
                }
            note += getCorrectedValueInt(data.getTranspose(), Series.Data.MAX_TRANSPOSE * 2) - Series.Data.MAX_TRANSPOSE;
            note = data.adjustNote(note);
            if (note > 127) note = 127;                 // FIXME: should we instead just not play the note?
            if (note < 0) note = 0;                             // FIXME: should we instead just not play the note?
            }
        return super.noteOff(out, note, vel);
        }
        
    public void scheduleNoteOff(int out, int note, double vel, int time) 
        {
        if (playingIndex >= 0)
            {
            Node node = getNode(playingIndex);
            Series.Data data = node.getData();
            if (data.getOut() != Series.Data.DISABLED)
                {
                out = data.getOut();
                }
            note += getCorrectedValueInt(data.getTranspose(), Series.Data.MAX_TRANSPOSE * 2) - Series.Data.MAX_TRANSPOSE;
            note = data.adjustNote(note);
            if (note > 127) note = 127;                 // FIXME: should we instead just not play the note?
            if (note < 0) note = 0;                             // FIXME: should we instead just not play the note?
            super.scheduleNoteOff(out, note, vel, (int)(time / getCorrectedValueDouble(data.getRate())));
            }
        else System.err.println("SeriesClip.scheduleNoteOff: playingIndex was " + playingIndex);
        }
        
    // TESTING
    public static void main(String[] args) throws Exception
        {
        // Set up Seq
        Seq seq = new Seq();            // starts timer running
        seq.setupForMIDI(SeriesClip.class, args, 0, 2);   // sets up MIDI in and out
        seq.setOut(0, new Out(seq, 0));         // Out 0 points to device 0 in the tuple.  This is too complex.
        seq.setOut(1, new Out(seq, 1));         // Out 0 points to device 0 in the tuple.  This is too complex.
        
        // Set up our module structure
        Series series = new Series(seq);

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

        // Load into series
        series.add(dSeq, 0, 0.5);
        series.add(dSeq2, 1, 0.0);
        series.add(dSeq, 2, 0.0);
                
        // Build Clip Tree
        seq.setData(series);

        seq.reset();
        seq.play();

        seq.waitUntilStopped();
        }

    }
