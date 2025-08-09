/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.automaton;

import seq.engine.*;
import seq.util.*;
import java.util.*;
import java.util.concurrent.*;
import javax.sound.midi.*;

public class AutomatonClip extends Clip
    {
    private static final long serialVersionUID = 1;
    
    public void rebuild(Motif motif)
        {
        if (this.getMotif() == motif)
            {
            rebuild();
            }
            
        for(AutomatonThread thread : unprocessed)
            {
            if (thread.child != null) thread.child.rebuild(motif);
            }

        for(AutomatonThread thread : processed)
            {
            if (thread.child != null) thread.child.rebuild(motif);
            }
        }

    public void rebuild()
        {
        release();
        terminate();

        clearClips();
        version = getMotif().getVersion();
        // FIXME: Anything else?
        }
        
    public AutomatonClip(Seq seq, Automaton automaton, Clip parent)
        {
        super(seq, automaton, parent);
        rebuild();
        }
        
    
    /// CLIP POOL
    /// We often will build entire new clip trees and that is costly, so
    /// here we're maintaining an allocation pool to avoid rebuilding them
    
    HashMap<Motif, ArrayList<Clip>> pool = new HashMap<>();
    
    void terminateClips()
        {
        // We don't need to terminate pooled clips, they're already terminated.
        // We need to terminate active clips, those will be in processed and unprocessed
        // For chords, we need to manually turn off the notes as there is no child to terminate.
        for(AutomatonThread thread : unprocessed)
            {
            if (thread.child != null)
                {
                thread.child.terminate();
                }
            if (thread.node instanceof Automaton.Chord){
                Automaton.Chord achord = (Automaton.Chord) thread.node;
                    {
                    int release = achord.getRelease();
                    int out = achord.getMIDIOut();
                    for(int i = 0; i < Automaton.Chord.MAX_NOTES; i++)
                        {
                        int pitch = thread.pitches[i];
                        int id = thread.ids[i];
                        if (pitch != Automaton.Chord.NO_NOTE)
                            {
                            super.noteOff(out, pitch, release, id);
                            }
                        thread.pitches[i] = Automaton.Chord.NO_NOTE;
                        thread.ids[i] = NO_NOTE_ID;
                        }
                    }
                }
            }
        for(AutomatonThread thread : processed)
            {
            if (thread.child != null)
                {
                thread.child.terminate();
                }

            if (thread.node instanceof Automaton.Chord){
                Automaton.Chord achord = (Automaton.Chord) thread.node;
                    {
                    int release = achord.getRelease();
                    int out = achord.getMIDIOut();
                    for(int i = 0; i < Automaton.Chord.MAX_NOTES; i++)
                        {
                        int pitch = thread.pitches[i];
                        int id = thread.ids[i];
                        if (pitch != Automaton.Chord.NO_NOTE)
                            {
                            super.noteOff(out, pitch, release, id);
                            }
                        thread.pitches[i] = Automaton.Chord.NO_NOTE;
                        thread.ids[i] = NO_NOTE_ID;
                        
                        }
                    }
                }
            }
        }
        
    // Clears the pool
    void clearClips()
        {
        terminateClips();
        pool = new HashMap<>();
        }
        
    // Produces a clip for the given motif, either new or taken from the pool.
    // You'll need to reset this.
    Clip makeClip(Motif child)
        {
        ArrayList<Clip> clips = pool.get(child);
        if (clips == null || clips.size() == 0) return child.buildClip(this);
        else
            {
            Clip clip = clips.remove(clips.size() - 1);
            clip.reset();   // or whatever?
            return clip;
            }
        }
        
    // Returns a clip to the pool to be used later.
    // The thread should have been terminated prior to pooling.
    void poolClip(Clip clip)
        {
        Motif motif = clip.getMotif();
        ArrayList<Clip> clips = pool.get(motif);
        if (clips == null) 
            { 
            clips = new ArrayList<Clip>(); 
            pool.put(motif, clips); 
            }
        clips.add(clip);
        }
        
    
    
    
    // NOTIFICATIONS AND JOIN COUNTS
    // At present only a single node need to be notified that a thread has
    // entered it: Join.  So we'll make this a Join-oriented thing here for now.
    // Joins need to know how many times they have been entered. We keep
    // track of that here.
    static class Join
        {
        int count = 0;
        }
        
    HashMap<Automaton.Join, Join> joins = null;
        
    Join getJoinFor(Automaton.Join ajoin)
        {
        if (joins == null) joins = new HashMap<>();
        Join join = joins.get(ajoin);
        if (join == null)
            {
            join = new Join();
            joins.put(ajoin, join);
            }
        return join;
        }
        
    public int getCurrentJoinCount(Automaton.Join ajoin)
        {
        Join join = getJoinFor(ajoin);
        if (join == null) return -1;
        else return join.count;
        }
    
    public void terminate() 
        { 
        super.terminate();
        terminateClips();
        }




    // THREADS
    // A thread holds two items: (1) the current node it's processing and
    // (2) the clip of the Motif it's processing ONLY IF its current node is a Motif.
    // The thread also maintains a list of nodes it's visited in the graph to 
    // determine that it's in a fast cycle.  Thread are processed, and in the process
    // method is the majority of work here in AutomatonClip



    public static final int TRIGGER_PARAMETER = 7;

    public static final Automaton.Node[] EMPTY_AUTOMATON_NODE = { };
    public final static Automaton.Node[] UNFINISHED_AUTOMATON_NODE = { null };
    boolean finished = false;
        

    // AUTOMATON THREAD ITERATION COUNTS
    // Iterations are per thread at present.  FIXME maybe this should be an option.
                        
    static class Iterate            // Note this class used to be inside AutomatonThread, but Java 8 doesn't like that.
        {
        int count = -1;         // we'll get incremented the first time
        }
                
    // Set temporarily so the MIDI methods know what node is sending MIDI right now
    Automaton.MotifNode currentNode = null; 
        
    /// This is NOT static, and I think that's okay?  FIXME
    public class AutomatonThread
        {
        /// PITCHES AND IDS for Chords
        int[] pitches = new int[Automaton.Chord.MAX_NOTES];
        int[] ids = new int[Automaton.Chord.MAX_NOTES];
                
                
        void notifyNode(Automaton.Node currentNode, Automaton.Node previousNode)
            {
            if (currentNode instanceof Automaton.Join)
                {
                getJoinFor((Automaton.Join)currentNode).count++;
                }
            }

        public AutomatonThread(Automaton.Node start)
            {
            this.visited = new HashSet<Automaton.Node>();
            setNode(start);
            Arrays.fill(ids, NO_NOTE_ID);
            }

        public AutomatonThread(Automaton.Node start, HashSet<Automaton.Node> visited)
            {
            this.visited = visited;
            setNode(start);
            Arrays.fill(ids, NO_NOTE_ID);
            }
                        
        HashMap<Automaton.Iterate, Iterate> iterates = null;
        
        Iterate getIterateFor(Automaton.Iterate aiterate)
            {
            if (iterates == null) iterates = new HashMap<>();
            Iterate iterate = iterates.get(aiterate);
            if (iterate == null)
                {
                iterate = new Iterate();
                iterates.put(aiterate, iterate);
                }
            return iterate;
            }
            
        // Rates
        double cumulativeRate = 0.0;
        int lastPos = 0;

        // DELAY COUNTS
        // Delays are per thread.
        int delayCount;


        // REPEATS (INTERNAL LOOPS) OF MOTIF NODES BEYOND THE FIRST PROCESS

        int repeatCount = 0;

        public boolean shouldRepeat()
            {
            if (node != null && node instanceof Automaton.MotifNode)
                {
                Automaton.MotifNode mnode = (Automaton.MotifNode)node;
                if (mnode.getRepeatUntilTrigger())
                    {
                    shouldResetTriggers = true;             // we have multiple threads so we can't check and reset now, just check, then reset everyone later after process()
                    return !isTriggered(TRIGGER_PARAMETER);
                    }
                else if (repeatCount >= getCorrectedValueInt(mnode.getRepeats(), Automaton.MotifNode.MAX_REPEATS))
                    {
                    java.util.Random random = ThreadLocalRandom.current();
                    double prob = getCorrectedValueDouble(mnode.getRepeatProbability(), 1.0);
                    if (prob == 0) 
                        {
                        return false;
                        }
                    else    
                        {
                        return random.nextDouble() < prob;
                        }
                    }
                else
                    {
                    repeatCount++;
                    return true;
                    }
                }
            else return false;
            }
                        
        Automaton.Node node = null;
        Clip child = null;
        HashSet<Automaton.Node> visited = null;
        public HashSet<Automaton.Node> getVisited() { return visited; }
        public void clearVisited() { visited.clear(); }
        public boolean contains(Automaton.Node n) { return visited.contains(n); }
                
        public Clip getChild() { return child; }
        public Automaton.Node getNode() { return node; }
        public void setNode(Automaton.Node n) 
            { 
            if (node != null) visited.add(node); 
            this.notifyNode(n, node);
            node = n; 
                        
            if (child != null)
                {
                child.release();
                child.terminate();
                poolClip(child);
                child = null;
                }
                
            if (n instanceof Automaton.Iterate)
                {
                //getIterateFor((Automaton.Iterate)n).count++;
                }
            else if (n instanceof Automaton.Delay)
                {
                delayCount = 0;
                }
            else if (n instanceof Automaton.Chord)
                {
                delayCount = 0;
                }
            else if (n instanceof Automaton.MotifNode)
                {
                repeatCount = 0;
                }
            }
                        
        void reset(Automaton.MotifNode node, Clip clip)
            {
            resetTriggers();
            loadRandomValue(clip, node.child);
            loadParameterValues(clip, node.child);
            clip.reset();
            cumulativeRate = 0.0;
            lastPos = -1;
            }

        public boolean advance(Automaton.MotifNode node, Clip child, double rate)
            {
            loadParameterValues(child, node.child);                                     // this is done every time it's advanced
                                   
            if (rate == 1.0) return child.advance();
            else
                {
                boolean result = false;
                cumulativeRate += rate;
                for( /* Empty */ ; lastPos < cumulativeRate; lastPos++)
                    {
                    result = result || child.advance();
                    }
                return result;
                }
            }
        
        public Automaton.Node[] processThread()
            {
            if (node instanceof Automaton.MotifNode)
                {
                Automaton.MotifNode motifnode = (Automaton.MotifNode)node;
                if (child == null)
                    {
                    child = makeClip(motifnode.getMotif());
                    reset(motifnode, child);
                    }
                    
                currentNode = motifnode;                 
                if (advance(motifnode, child, getCorrectedValueDouble(motifnode.getRate(), Automaton.MotifNode.MAX_RATE)))    // all done
                    {
                    currentNode = null;
                    // FIXME: is the +1 right?  I think it is?
                    if ((getPosition() + 1) % Automaton.MotifNode.QUANTIZATIONS[((Automaton.MotifNode)node).getQuantization()] == 0)
                        {
                        // we're just before a quantization boundary, time to transition!
                        return node.selectOut();
                        }
                    else 
                        {
                        return UNFINISHED_AUTOMATON_NODE;          // gotta wait until we hit quantization boundary
                        }
                    }
                else                                    // not done yet
                    {
                    currentNode = null;
                    return UNFINISHED_AUTOMATON_NODE;
                    }
                }
            else if (node instanceof Automaton.Finished)
                {
                finished = true;
                return node.selectOut();
                }
            else if (node instanceof Automaton.Iterate)
                {
                java.util.Random random = ThreadLocalRandom.current();
                Automaton.Iterate aiterate = (Automaton.Iterate) node;
                Iterate iterate = getIterateFor(aiterate);
                return aiterate.selectOut(++iterate.count);
                }
            else if (node instanceof Automaton.Random)
                {
                java.util.Random random = ThreadLocalRandom.current();
                return (((Automaton.Random)node).selectOut(random));
                }
            else if (node instanceof Automaton.Delay)
                {
                Automaton.Delay adelay = (Automaton.Delay) node;
                int d = adelay.getDelay();
                delayCount++;
                if (delayCount > d)     // note >
                    {
                    delayCount = 0;
                    return adelay.selectOut();              // it's possible to have zero delay
                    }
                else return UNFINISHED_AUTOMATON_NODE;
                }
            else if (node instanceof Automaton.Chord)
                {
                Automaton.Chord achord = (Automaton.Chord) node;
                int d = achord.getLength();
                if (delayCount == 0) // play the chord
                    {
                    int velocity = achord.getVelocity();
                    int out = achord.getMIDIOut();
                    for(int i = 0; i < Automaton.Chord.MAX_NOTES; i++)
                        {
                        int pitch = achord.getNote(i);
                        if (pitch != Automaton.Chord.NO_NOTE)
                            {
                            ids[i] = AutomatonClip.super.noteOn(out, pitch, velocity);
                            }
                        else
                            {
                            ids[i] = NO_NOTE_ID;
                            }
                        pitches[i] = pitch;
                        }
                    }
                delayCount++;
                if (delayCount >= d * achord.getTimeOn())        // stop playing the chord
                    {
                    int release = achord.getRelease();
                    int out = achord.getMIDIOut();
                    for(int i = 0; i < Automaton.Chord.MAX_NOTES; i++)
                        {
                        int pitch = pitches[i];
                        int id = ids[i];
                        if (pitch != Automaton.Chord.NO_NOTE)
                            {
                            AutomatonClip.super.noteOff(out, pitch, release, id);
                            }
                        pitches[i] = Automaton.Chord.NO_NOTE;
                        ids[i] = NO_NOTE_ID;
                        }
                    }
                if (delayCount >= d)                                             // note >
                    {
                    delayCount = 0;
                    return achord.selectOut();              // it's possible to have zero delay
                    }
                else return UNFINISHED_AUTOMATON_NODE;
                }
            else if (node instanceof Automaton.Fork)
                {
                return node.selectOut();
                }
            else if (node instanceof Automaton.Join)
                {
                Join join = getJoinFor((Automaton.Join)node);
                if (join.count >= 2)
                    {
                    join.count -= 2;
                    return node.selectOut();
                    }
                else
                    {
                    return EMPTY_AUTOMATON_NODE;            // we're dead
                    }
                }
            else // Must be UNFINISHED, which cannot be right
                {
                System.err.println("INTERNAL ERROR: in AutomatonThread.processThread(), node is " + node + " which should not happen.");
                return EMPTY_AUTOMATON_NODE;
                }
            }
 
        public void release()
            {
            Clip child = getChild();
            if (child != null) child.release();
            else if (node != null && node instanceof Automaton.Chord)
                {
                Automaton.Chord achord = (Automaton.Chord) node;
                double timeOn = achord.getTimeOn();
                int release = achord.getRelease();
                int out = achord.getMIDIOut();
                for(int i = 0; i < Automaton.Chord.MAX_NOTES; i++)
                    {
                    int pitch = pitches[i];
                    int id = ids[i];
                    int d = achord.getLength();
                    if (pitch != Automaton.Chord.NO_NOTE)
                        {
                        int time = (int)(delayCount + 1 - d * timeOn);          // FIXME, is this right?
                        if (time >= 0)
                            {
                            AutomatonClip.super.scheduleNoteOff(out, pitch, release, time, id);
                            }
                        }
                    pitches[i] = Automaton.Chord.NO_NOTE;
                    ids[i] = NO_NOTE_ID;
                    }
                }
            }

        public void cut()
            {
            Clip child = getChild();
            if (child != null) child.cut();
            else if (node != null && node instanceof Automaton.Chord)
                {
                Automaton.Chord achord = (Automaton.Chord) node;
                int out = achord.getMIDIOut();
                int release = achord.getRelease();
                for(int i = 0; i < Automaton.Chord.MAX_NOTES; i++)
                    {
                    int pitch = pitches[i];
                    int id = ids[i];
                    if (pitch != Automaton.Chord.NO_NOTE)
                        {
                        AutomatonClip.super.noteOff(out, pitch, release, id);
                        }
                    pitches[i] = Automaton.Chord.NO_NOTE;
                    ids[i] = NO_NOTE_ID;
                    }
                }
            }
        }
    
    ArrayList<AutomatonThread> unprocessed = new ArrayList<>();
    ArrayList<AutomatonThread> processed = new ArrayList<>();
        
    public ArrayList<AutomatonThread> getUnprocessedThreads() { return unprocessed; }    
    public ArrayList<AutomatonThread> getProcessedThreads() { return processed; }    

    public int numThreads() { return unprocessed.size() + processed.size(); }
        
    // Move from unprocessed to processed.  Thread is presumed to be
    // the last thread in the unprocessed list
    void moveThread()
        {
        processed.add(unprocessed.remove(unprocessed.size() - 1));
        }
    
    public void loop()
        {
        super.loop();
        resetPosition();
        }
                
    public void reset()  
        {
        super.reset();
        resetPosition();
        }

    public void release()
        {
        for(AutomatonThread thread : unprocessed)
            {
            thread.release();
            }
        }

    public void cut()
        {
        for(AutomatonThread thread : unprocessed)
            {
            thread.cut();
            }
        }
                
    public void resetPosition()
        {
        Automaton automaton = (Automaton)getMotif();
        // Reset everything
        terminateClips();
        processed.clear();
        unprocessed.clear();
        joins = null;
        Automaton.Node start = automaton.getStart();
        if (start == null)
            {
            finished = true;
            }
        else
            {
            finished = false;
            AutomatonThread thread = new AutomatonThread(start);
            processed.add(thread);
            }
        }
        
    boolean shouldResetTriggers = false;
    public boolean process()
        {
        shouldResetTriggers = false;
       
        // This is "PROCEDURE STEP" in the pseudocode
                
        // Swap
        ArrayList<AutomatonThread> temp = unprocessed;
        unprocessed = processed;
        processed = temp;
                
        // Clear
        for(AutomatonThread thread : unprocessed)
            {
            thread.clearVisited();
            }
                
        // Is there anything left?  Are we entirely empty?
        if (unprocessed.isEmpty())
            {
            return true;    // we are DONE
            }
        
        while(!unprocessed.isEmpty())
            {
            AutomatonThread thread = unprocessed.get(unprocessed.size() - 1);       // last one
            Automaton.Node anode = thread.getNode();
            Automaton.Node[] nodes = thread.processThread();
            if (nodes.length == 0)
                {
                thread.setNode(null);
                // thread is now dead, nowhere to go
                unprocessed.remove(unprocessed.size() - 1);
                }
            else
                {
                Automaton.Node next = nodes[0];
                if (next == null)                                       // UNFINISHED
                    {
                    // Stay where we are
                    moveThread();
                    }
                else if (thread.shouldRepeat())         // REPEATING
                    {
                    // Stay where we are, but reset
                    thread.child.release();
                    thread.child.loop();

                    Automaton.Node node = thread.getNode();
                    if (node instanceof Automaton.MotifNode)
                        {
                        loadRandomValue(thread.getChild(), ((Automaton.MotifNode)(thread.getNode())).getChild());              // loop
                        }

                    moveThread();
                    }
                else if (next == anode || thread.contains(next))                        // we do next == anode because anode may not yet be in visited list
                    {
                    // This is a cycle, we've been here before.  Stay at 'next'.
                    // 'next' should NEVER be a MotifNode or a Delay with delay > 0.
/*
  if (next instanceof Automaton.MotifNode ||
  next instanceof Automaton.Chord ||
  (next instanceof Automaton.Delay &&
  ((Automaton.Delay)next).getDelay() > 0))
  {
  System.err.println("ERROR: cycling in a node: " + next);
  }
*/
                    thread.setNode(next);
                    moveThread();
                    }
                else    // we're going somewhere new
                    {
                    thread.setNode(next);
                    if (anode instanceof Automaton.MotifNode ||
                        anode instanceof Automaton.Chord ||
                            (anode instanceof Automaton.Delay &&
                            ((Automaton.Delay)anode).getDelay() > 0))
                        {
                        moveThread();
                        }
                    // Otherwise it's a zero-time node like a Random or Fork or whatever, we keep him on the unprocessed list
                    // so he's processed AGAIN at the top of this loop
                    }
                                        
                // Finish rest of nodes
                for(int i = 1; i < nodes.length; i++)
                    {
                    if (numThreads() < Automaton.MAX_THREADS)
                        {
                        Automaton.Node n = nodes[i];
                        @SuppressWarnings("unchecked")
                            AutomatonThread newthread = new AutomatonThread(n, (HashSet<Automaton.Node>)(thread.getVisited().clone()));
                        if (//n instanceof Automaton.MotifNode ||
                            //n instanceof Automaton.Chord ||
                            //(n instanceof Automaton.Delay && ((Automaton.Delay)n).getDelay() > 0) ||
                            n == anode ||                                                                                       // it's a cycle.  We do n == anode because anode may not yet be in visited list
                            newthread.contains(n)                                       // it's a cycle
                            )
                            {
                            // put new thread on processed
                            processed.add(newthread);
                            }
                        else
                            {
                            // put new thread on unprocessed
                            unprocessed.add(newthread);
                            }
                        }
                    else
                        {
                        // Do nothing.  New thread dies.
                        }
                    }
                }
            }

        // Reset triggers?
        if (shouldResetTriggers) checkAndResetTrigger(TRIGGER_PARAMETER);

        // Is there anything left?  Are we entirely empty?
        if (processed.isEmpty())
            {
            return true;    // we are DONE
            }
                        
        if (finished) 
            {
            // System.err.println("ALL DONE FINISHED = TRUE");
            }
        return finished;
        }

    // Force-adds a thread, for debugging and user functions only.
    public void launchThread(Automaton.Node node)
        {
        processed.add(new AutomatonThread(node));
        }

    public void noteOn(int out, int note, double vel, int id) 
        {
        if (currentNode != null)
            {
            if (currentNode.getOutMIDI() != Automaton.MotifNode.DISABLED)
                {
                out = currentNode.getOutMIDI();
                }
            note += getCorrectedValueInt(currentNode.getTranspose(), Automaton.MotifNode.MAX_TRANSPOSE * 2) - Automaton.MotifNode.MAX_TRANSPOSE;
            //note = currentNode.adjustNote(note);
            if (note > 127) note = 127;                 // FIXME: should we instead just not play the note?
            if (note < 0) note = 0;                             // FIXME: should we instead just not play the note?
            vel *= getCorrectedValueDouble(currentNode.getGain(), Automaton.MotifNode.MAX_GAIN);
            if (vel > 127) vel = 127;                   // FIXME: should we check for vel = 0?
            }
        super.noteOn(out, note, vel, id);
        }
        
    public void noteOff(int out, int note, double vel, int id) 
        {
        if (currentNode != null)
            {
            if (currentNode.getOutMIDI() != Automaton.MotifNode.DISABLED)
                {
                out = currentNode.getOutMIDI();
                }
            note += getCorrectedValueInt(currentNode.getTranspose(), Automaton.MotifNode.MAX_TRANSPOSE * 2) - Automaton.MotifNode.MAX_TRANSPOSE;
            //note = currentNode.adjustNote(note);
            if (note > 127) note = 127;                 // FIXME: should we instead just not play the note?
            if (note < 0) note = 0;                             // FIXME: should we instead just not play the note?
            }
        super.noteOff(out, note, vel, id);
        }
        
    public void scheduleNoteOff(int out, int note, double vel, int time, int id) 
        {
        if (currentNode != null)
            {
            if (currentNode.getOutMIDI() != Automaton.MotifNode.DISABLED)
                {
                out = currentNode.getOutMIDI();
                }
            note += getCorrectedValueInt(currentNode.getTranspose(), Automaton.MotifNode.MAX_TRANSPOSE * 2) - Automaton.MotifNode.MAX_TRANSPOSE;
            //note = currentNode.adjustNote(note);
            if (note > 127) note = 127;                 // FIXME: should we instead just not play the note?
            if (note < 0) note = 0;                             // FIXME: should we instead just not play the note?
            super.scheduleNoteOff(out, note, vel, (int)(time / getCorrectedValueDouble(currentNode.getRate())), id);
            }
        else System.err.println("SeriesClip.scheduleNoteOff: currentNode is null");
        }
        

    // TESTING
    public static void main(String[] args) throws Exception
        {
        // Set up Seq
        Seq seq = new Seq();            // starts timer running
        seq.setupForMIDI(AutomatonClip.class, args, 0, 2);   // sets up MIDI in and out
        seq.setOut(0, new Out(seq, 0));         // Out 0 points to device 0 in the tuple.  This is too complex.
        seq.setOut(1, new Out(seq, 1));         // Out 0 points to device 0 in the tuple.  This is too complex.
        
        // Set up our module structure
        Automaton fsa = new Automaton(seq);

        // Set up first StepSequence
        seq.motif.stepsequence.StepSequence dSeq = new seq.motif.stepsequence.StepSequence(seq, 2, 16);
        
        // Specify notes
        dSeq.setTrackNote(0, 60);
        dSeq.setTrackNote(1, 80);
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
        
        // Set up another StepSequence
        seq.motif.stepsequence.StepSequence dSeq1 = new seq.motif.stepsequence.StepSequence(seq, 2, 32);

        // Specify notes
        dSeq1.setTrackNote(0, 40);
        dSeq1.setTrackNote(1, 70);
        dSeq1.setTrackOut(0, 0);
        dSeq1.setTrackOut(1, 1);
        
        // Load the StepSequence with some data
        dSeq1.setVelocity(1, 0, 16, true);
        dSeq1.setVelocity(1, 4, 16, true);
        dSeq1.setVelocity(1, 8, 16, true);
        dSeq1.setVelocity(1, 12, 16, true);
        dSeq1.setVelocity(1, 16, 16, true);
        dSeq1.setVelocity(1, 18, 16, true);
        dSeq1.setVelocity(1, 20, 16, true);
        dSeq1.setVelocity(1, 22, 16, true);
        dSeq1.setVelocity(1, 24, 16, true);
        dSeq1.setVelocity(1, 26, 16, true);
        dSeq1.setVelocity(1, 28, 16, true);
        dSeq1.setVelocity(0, 30, 16, true);

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


        // Set up second StepSequence
        seq.motif.stepsequence.StepSequence dSeq3 = new seq.motif.stepsequence.StepSequence(seq, 2, 16);
        
        // Specify notes
        dSeq3.setTrackNote(0, 45);
        dSeq3.setTrackNote(1, 90);
        dSeq3.setTrackOut(0, 0);
        dSeq3.setTrackOut(1, 1);
        dSeq3.setDefaultSwing(0);
        
        // Load the StepSequence with some data
        dSeq3.setVelocity(0, 0, 1, true);
        dSeq3.setVelocity(0, 2, 5, true);
        dSeq3.setVelocity(0, 6, 13, true);
        dSeq3.setVelocity(1, 8, 2, true);
        dSeq3.setVelocity(1, 10, 4, true);
        dSeq3.setVelocity(1, 11, 6, true);
        dSeq3.setVelocity(1, 13, 3, true);
        dSeq3.setVelocity(1, 14, 4, true);

        // Load into series
        Automaton.MotifNode n = fsa.add(dSeq);                  // START
        Automaton.MotifNode n1 = fsa.add(dSeq1);
        Automaton.MotifNode n2 = fsa.add(dSeq2);
        Automaton.MotifNode n3 = fsa.add(dSeq3);
                
        /// Rather than use fsa.connect(n, iter, 0) etc.
        /// I'm being lazy and just setting the out value directly
        /// via n.setOut(0, iter) etc.        
        
        /*
        // Test Iter
        // dSeq -> Iterate LOOP -> [dSeq2->dSeq] or [dSeq3->dSeq]
        
        Automaton.Iterate iter = fsa.addIterate();
        n.setOut(0, iter);
        iter.setOut(0, n2, 2);
        iter.setOut(1, n3, 3);
        n2.setOut(0, n);
        n3.setOut(0, n);
        */

        /*
        // dSeq -> Fork -> [dseq1 and dSeq2] -> Join -> dSeq
                
        Automaton.Fork fork = fsa.addFork();
        n.setOut(0, fork);
        fork.setOut(0, n1);
        fork.setOut(1, n2);
        Automaton.Join join = fsa.addJoin();
        n1.setOut(0, join);
        n2.setOut(0, join);
        join.setOut(0, n);
        */
            
        /*
        // dSeq -> Random -> [dSeq1 and dSeq2] -> dSeq
                
        Automaton.Random rand = fsa.addRandom();
        n.setOut(0, rand);
        rand.setOut(0, n1, 0.5);
        rand.setOut(1, n2, 0.5);
        n1.setOut(0, n);
        n2.setOut(0, n);
        */
        
        /*
        // Test number of repeats.  dSeq is iterated 4 times (1 basic + 3 more)
        // dSeq1 is iterated 3 times (1 basic + 2 more), then continues to iterate with 0.5 probability,
        // then we go back to dSeq
        // [dSeq]*(3) -> [dSeq1]*(2,0.5) -> dSeq
                
        n.setOut(0, n2);
        n.setRepeats(3);
        n2.setOut(0, n);
        n2.setRepeats(2);
        n2.setRepeatProbability(0.5);
        */
                
        /*
        // dSeq -> Delay(192 * 4) -> dSeq2 -> Delay(192 * 8) -> Delay(0) -> Delay(0) -> dSeq
                
        Automaton.Delay delay1 = fsa.addDelay();
        delay1.setDelay(Seq.PPQ * 4);
        Automaton.Delay delay2 = fsa.addDelay();
        delay2.setDelay(Seq.PPQ * 8);
        Automaton.Delay delay3 = fsa.addDelay();
        Automaton.Delay delay4 = fsa.addDelay();
        n.setOut(0, delay1);
        delay1.setOut(0, n1);
        n1.setOut(0, delay2);
        delay2.setOut(0, delay3);
        delay3.setOut(0, delay4);
        delay4.setOut(0, n);
        */
        
        /*
        // dSeq -> dSeq2 -> Finished -> dSeq
        
        Automaton.Finished finished = fsa.addFinished();
        n.setOut(0, n2);
        n2.setOut(0, finished);
        finished.setOut(0, n);
        seq.setData(fsa);
        */
        

        // Build Clip Tree
        seq.setData(fsa);

        seq.reset();
        seq.play();

        seq.waitUntilStopped();
        }



    }

        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
