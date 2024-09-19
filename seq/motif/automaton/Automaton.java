/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.automaton;

import seq.engine.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;
import seq.util.Prefs;


//   <ArrayList of Nodes> list of NODES
//   Stack of UNPROCESSED THREADS
//   Stack of PROCESSED THREADS
//
//   A special single NODE, returned by process(), is called UNFINISHED.  It is not in the LIST and is just a marker
//
//   A NODE can be
//   	A MOTIF node: when PROCESSED, it returns a node, which could be UNFINISHED, or none.  Motif nodes can repeat.
//
//		Or an Action Node of some sort.  Action Nodes include
//   		FORK: when PROCESSED, it returns a list of follow-on nodes
//			CHORD: when PROCESSED, it returns  node, which could be UNFINISHED, or none
//				Note: CHORD cannot have a zero delay.  It plays a chord.
//   		RANDOM: when PROCESSED, it returns at most one randomly-chosen node
//   		ITERATION: when PROCESSED, it returns at most one new node
//   		DELAY: when PROCESSED, it returns a node, which could be UNFINISHED, or none
//   			Note: a DELAY can have zero delay and serve as a stub
//   		FINISH: when PROCESSED, it raises the finished flag in the clip and returns at most one node
//   		JOIN: when NOTIFIED, increments ONE WAITING.
//   				when PROCESSED, if ONE WAITING >= 2, set ONE WAITING to ONE_WAITING - 2, returns a node or none; else returns UNFINISHED
//
//   Each NODE has the following methods
//   	resetRepeats()          // called at thread.process()
//   	continueRepeats()       // revises repeats and returns true if we should continue
//   	numOutgoingPositions()
//   	addIncoming(Node)       // Node can be added multiple times
//   	attachOutgoing(Node, int position)              // Can be added only once
//   	removeIncoming(Node)    // Node can be added multiple times
//   	removeAllIncomign(Node) // Node can be added multiple times
//   	removeOutgoing(Node, int position)
//   	getIncoming()
//   	<Node> getOutgoing(int position)
//
//   NOTE: a NODE can only be attached ONCE to a given outgoing slot.  Is this a weakness?
//   NOTIFY(thread, previous node (or null))         // lets the node know that the thread is set to it
//
//   A THREAD has a HashSet<Node>
//   	clear() -> clears the hash set
//   	setNode(Node) sets the current node and adds the OLD CURRENT NODE to the hash set if there is one.
//			Also calls node.NOTIFY(thread, previous node)
//   	contains(Node) -> checks the hash set
//   	getSet() -> returns a copy of the hash set
//   	<ArrayList of Nodes> process() -> processes the node as described above
//        
//   Procedure RESET()
//   	Clear Processed and (for good measure) Unprocessed threads
//  	Clear Join counts
//   	Resets finished
//   	For each NODE in the list
//   		node.reset()                            // reset counters etc.
//   	thread <- new Thread(0);
//   	num threads = 1;
//        
//   Procedure STEP:
//   	Swap Processed and Unprocessed threads (all threads are now on Unprocessed)
//   	For each Unprocessed thread
//   		thread.clear()
//   	Repeat until Unprocessed threads is empty
//   		thread <- first thread in UNPROCESSED
//   		// Check for finished
//   		<ArrayList of Nodes> nodes <- thread.process()  // provides list of follow-on nodes
//   		if (nodes is empty or all its nodes are null) 
//   			remove thread from UNPROCESSED          // now dead
//   			numthreads--;
//   		else            // one or more nodes
//   			next <- first element of nodes
//   			if (next is UNFINISHED)								// current node is not finished yet
//   				move thread from UNPROCESSED to PROCESSED
//   			else if we should continue repeats(next)
//					let next know that we're going to loop it
//   				move thread from UNPROCESSED to PROCESSED
//   			else if (thread.contains(next) || next == current node) // cycle, we've been here before
//   				thread.setNode(next)                            // Must be AFTER checking if it contains the element
//   				move thread from UNPROCESSED to PROCESSED
//				else
//   				thread.setNode(next)                            // Must be AFTER checking if it contains the element
//  				if (current node is a MOTIF or a DELAY of length > 0 or a CHORD)
//   					move thread from UNPROCESSED to PROCESSED
//   		// else we stay on the stack and continue to transition
//   		for(n in remaining nodes)
//   			if numthreads does not exceed max yet
//   				newthread <- new Thread(n, thread.getSet())             // Could still have cycles at least this time around
//   				numthreads++;
//   			if (newthread.contains(n)
//					or n == current node)       // cycle
//   					put new thread on PROCESSED
//   			else
//   					put new thread on UNPROCESSED
//
// In general, when we start a thread, it is put on UNPROCESSED, and its initial set node is neither processed nor visited.
// When we process a thread, we process its currently set node, then set the next node, and repeat this until we have processed a non-zero-time node.
// At this point, the current set node is neither processed nor visited either.


public class Automaton extends Motif
    {
    private static final long serialVersionUID = 1;
    
    public static final int MAX_OUT = 4;
    public static final int MAX_THREADS = 8;
    
    // All nodes.  Note that the order does not matter except for the FIRST node.
    ArrayList<Node> nodes = new ArrayList<>();
    
    /** Returns the nodes ArrayList.  Be careful with this. */
    public ArrayList<Node> getNodes() { return nodes; }
    
    /** Returns the current start node [Node 0 in the nodes ArrayList], 
        or NULL if there is no start node (which can only happen if there are no nodes at all) */
    public Node getStart() { if (nodes.size() == 0) return null; else return nodes.get(0); }
    
    /** Sets NODE to be the new start node, adding it to the ArrayList if it is not already there.
        In general it'd be best if you added the node to the ArrayList before calling this method.
        You should NOT add the node AFTER calling this method: that'd add it twice. */
    public void setStart(Node node)
        {
        if (getStart() == node) return;         // what's the point?
        
        if (nodes.isEmpty())
            {
            nodes.add(node);                                // node is at position 0
            }
        else
            {
            Node oldStart = nodes.get(0);
                
            int pos = nodes.indexOf(node);
            if (pos < 0)                                    // node is not part of array yet
                {
                nodes.set(0, node);
                nodes.add(oldStart);
                }
            else                                                    // swap nodes
                {
                nodes.set(0, node);
                nodes.set(pos, oldStart);
                }
            }
        }
        
    public void addNode(Node node)
        {
        nodes.add(node);
        }
    
    public abstract static class Node implements Cloneable
        {
        public abstract int maxOut();
        
        Node[] out;
        double[] aux;
        String nickname = null;
        int position = -1;                              // position on screen.  Has NOTHING to do with the index of the node in the nodes ArrayList
        
        public void setNickname(String val) { nickname = val; }
        public String getNickname() { return nickname; }
        public String getCurrentName() { if (nickname != null && (!nickname.trim().equals(""))) return nickname; else return getBaseName(); }
        public abstract String getBaseName();
        
        /** Return position on screen.  Has NOTHING to do with the index of the node in the nodes ArrayList */
        public int getPosition() { return position; }
        /** Set position on screen.  Has NOTHING to do with the index of the node in the nodes ArrayList */
        public void setPosition(int val) { position = val; }
        
        public Node() 
            { 
            out = new Node[maxOut()]; 
            aux = new double[maxOut()]; 
            //for(int i = 0; i < out.length; i++) out[i] = null;
            }
        
        public Node getOut(int index) { return out[index]; }
        public void setOut(int index, Node node) { out[index] = node; }
        public void setOut(int index, Node node, double aux) { setOut(index, node); setAux(index, aux); }
        public Node[] getOut() { return out; }
        
        public Node[] selectOut()
            {
            int maxOut = maxOut();
            int count = 0;
            for(int i = 0; i < maxOut; i++)
                {
                if (out[i] != null) count++;
                }
            Node[] ret = new Node[count];
            count = 0;
            for(int i = 0; i < maxOut; i++)
                {
                if (out[i] != null) ret[count++] = out[i];
                }
            return ret;
            }
        
        public boolean allDisconnected()
            {
            for(int i = 0; i < out.length; i++)
                {
                if (out[i] != null) return false;
                }
            return true;
            }
            
        public double[] getAux() { return aux; }
        public double getAux(int index) { return aux[index]; }
        public void setAux(int index, double val) { aux[index] = val; }
        
        protected Node copyFrom(Node node)
            {
            position = node.position;
            nickname = node.nickname;
            for(int i = 0; i < out.length; i++)
                {
                out[i] = node.out[i];
                aux[i] = node.aux[i];
                }
            return this;
            }
            
        public void save(JSONObject to, Automaton automaton) throws JSONException 
            {
            JSONArray o = new JSONArray();
            for(int i = 0; i < out.length; i++)
                {
                JSONObject obj = new JSONObject();
                if (out[i] == null) obj.put("n", -1);
                else obj.put("n", automaton.nodes.indexOf(out[i]));               // O(n^2) :-(
                obj.put("a", aux[i]);
                o.put(obj);
                } 
            to.put("out", o); 
            to.put("pos", position);
            }
 
        public void loadFinal(JSONObject from, Automaton automaton) throws JSONException
            {
            JSONArray o = from.getJSONArray("out");
            for(int i = 0; i < out.length; i++)
                {
                JSONObject obj = o.getJSONObject(i);
                int val = obj.optInt("n", -1);
                if (val < 0) out[i] = null;
                else out[i] = automaton.nodes.get(val);
                } 
            }

        public abstract Node copy();
        }

    public Node loadStub(JSONObject from) throws JSONException
        {
        Node node = null;
        String type = from.getString("type");
        if (type.equals("motif")) node = new MotifNode(getChildren().get(from.getInt("child")));
        else if (type.equals("fork")) node = new Fork();
        else if (type.equals("join")) node = new Join();
        else if (type.equals("chord")) node = new Chord();
        else if (type.equals("delay")) node = new Delay();
        else if (type.equals("random")) node = new Random();
        else if (type.equals("iterate")) node = new Iterate();
        else if (type.equals("finished")) node = new Finished();
                
        JSONArray arr = from.getJSONArray("out");
        for(int i = 0; i < arr.length(); i++)
            {
            JSONObject obj = arr.getJSONObject(i);
            node.aux[i] = obj.optDouble("a", 0);
            }
        node.position = from.optInt("pos", -1);         // maybe getInt would be fine
        return node;
        }


    /** Fork contains some N <= MAX_OUT outputs, and returns all of them,
        resulting in new threads on all but the first output (which is the original thread) */
    public static class Fork extends Node
        {
        public int maxOut() { return MAX_OUT; }
        public Node copy() { return new Fork().copyFrom(this); }
        public void save(JSONObject to, Automaton automaton) throws JSONException { to.put("type", "fork"); super.save(to, automaton); }
        public String getBaseName() { return "Fork"; }
        }

    /** Join has a single output.  When it receives the second of N (default 2) input transitions, it then outputs
        on that output. */
    public static class Join extends Node
        {
        public int getJoinNumber() { return (int)aux[0]; }              // we'll abuse aux[0] here.  Value must be >= 2 and <= MAX_THREADS
        public void setJoinNumber(int val) { aux[0] = val; }
        public int maxOut() { return 1; }
        public Join() { this(2); }
        public Join(int joinNumber) { setJoinNumber(joinNumber); }
        public Node copy() { return new Join().copyFrom(this); } 
        public void save(JSONObject to, Automaton automaton) throws JSONException { to.put("type", "join"); super.save(to, automaton); }
        public String getBaseName() { return "Join"; }
        }

    /** Delay has a single output and a delay amount in PPQ.  When it receives an input transition, that transition
        is delayed for the delay amount before an output transition is issued.  Delay can handle input transitions
        for each thread. */
    public static class Delay extends Node
        {
        public int maxOut() { return 1; }
        public int getDelay() { return (int)aux[0]; }           // we'll abuse aux[0] here.
        public void setDelay(int val) { aux[0] = val; }
        public Delay() { this(0); }
        public Delay(int delay) { setDelay(delay); }
        public Node copy() { return new Delay().copyFrom(this); } 
        public void save(JSONObject to, Automaton automaton) throws JSONException { to.put("type", "delay"); super.save(to, automaton); }
        public String getBaseName() { return "Delay"; }
        }

    /** Chord has a single output, a length, a percent time on, a velocity, and up to four note pitches. 
        When it receives an input transition, a chord is played and that transition is delayed for the length 
        amount before an output transition is issued.  Chord can handle input transitions for each thread. */
    public static class Chord extends Node
        {
        public static final int DEFAULT_MIDI_OUT = -1;
        public static final int MAX_NOTES = 4;
        public static final int NO_NOTE = 128;
        int[] notes = new int[MAX_NOTES];
        double timeOn;          // between 0.0 and 1.0 inclusive
        int velocity;
        int release;
        int midiOut;
        
        public int maxOut() { return 1; }                                       // This is number of outputs, not MIDI Outs.  Confusing, I know.
        public int getMIDIOut() { return midiOut; }
        public void setMIDIOut(int val) { midiOut = val; }
        public int getLength() { return (int)aux[0]; }           // we'll abuse aux[0] here.
        public void setLength(int val) { aux[0] = val; }
        public int getNote(int n) { return notes[n]; }
        public void setNote(int n, int val) { notes[n] = val; }
        public int getVelocity() { return velocity; }
        public void setVelocity(int val) { velocity = val; }
        public int getRelease() { return release; }
        public void setRelease(int val) { release = val; }
        public double getTimeOn() { return timeOn; }
        public void setTimeOn(double val) { timeOn = val; }
        public Chord() 
            { 
            midiOut = Prefs.getLastOutDevice(0, "seq.motif.notes.Notes");
            timeOn = 1.0; 
            velocity = 64; 
            release = 64;
            setLength(Seq.PPQ); 
            notes[0] = 60; 
            for(int i = 1; i < MAX_NOTES; i++) notes[i] = NO_NOTE;
            }
                
        protected Node copyFrom(Node node) 
            { 
            Chord other = (Chord)node;
            Chord chord = (Chord)(super.copyFrom(node));

            for(int i = 0; i < chord.notes.length; i++)
                {
                chord.notes[i] = other.notes[i];
                }
            chord.timeOn = other.timeOn;
            chord.velocity = other.velocity;
            chord.release = other.release;
            chord.midiOut = other.midiOut;
            return chord;
            }
        public Node copy() { return new Chord().copyFrom(this); } 
        
        public void save(JSONObject to, Automaton automaton) throws JSONException 
            { 
            to.put("type", "chord"); 
            to.put("vel", velocity);
            to.put("rel", release);
            to.put("mout", midiOut);
            to.put("on", timeOn);
            JSONArray array = new JSONArray();
            for(int i = 0; i < MAX_NOTES; i++) array.put(notes[i]);
            to.put("chord", array);
            super.save(to, automaton); 
            }

        public void loadFinal(JSONObject from, Automaton automaton) throws JSONException
            {
            super.loadFinal(from, automaton);
            velocity = from.optInt("vel", 64);
            release = from.optInt("rel", 64);
            timeOn = from.optDouble("on", 1.0);
            midiOut = from.optInt("mout", DEFAULT_MIDI_OUT);
            JSONArray array = from.optJSONArray("chord");
            if (array == null)
                {
                for(int i = 0; i < MAX_NOTES; i++) notes[i] = NO_NOTE;
                }
            else
                {
                for(int i = 0; i < MAX_NOTES; i++) notes[i] = array.optInt(i, NO_NOTE);
                }
            }
        public String getBaseName() { return "Chord"; }
        }

    /** Random has some N <= MAX_OUT outputs, each associated with a WEIGHT >= 0 (the aux).  The WEIGHT is proportional
        to the relative probability that the output will be selected to transition out when Random receives an 
        input transition.  If the weight is 0 the output will never be selected unless all the other outputs are
        0 as well. */
    public static class Random extends Node
        {
        public int maxOut() { return MAX_OUT; }
        public Node copy() { return new Random().copyFrom(this); } 
        public String getBaseName() { return "Random"; }
        public void save(JSONObject to, Automaton automaton) throws JSONException { to.put("type", "random"); super.save(to, automaton); }
        public Node[] selectOut() { throw new RuntimeException("Automaton.Random does not support selectOut()"); }
        public Node[] selectOut(java.util.Random rand) 
            {
            // Compute total
            double total = 0; 
            int count = 0;
            for(int i = 0; i < out.length; i++)
                {
                if (out[i] != null) 
                    { 
                    count++; 
                    total += aux[i];
                    }
                }
                        
            // If there's nothing, return nothing
            if (count == 0) return new Node[0];
                
            // If all the nodes have zero weight, pick at random
            else if (total == 0)
                {
                int select = rand.nextInt(count);
                count = 0;
                Node last = out[out.length - 1];
                for(int i = 0; i < out.length; i++)
                    {
                    if (out[i] != null)
                        {
                        if (count == select) return new Node[] { out[i] };
                        else count++;
                        }
                    }
                /// FIXME: This should never happen?
                return new Node[] { last };
                }
                        
            // Else select by weight.  We're not building a CDF so this will be O(n)
            else
                {
                double select = rand.nextDouble() * total;
                Node last = null;       // just in case floating point subtraction fails us
                for(int i = 0; i < out.length; i++)
                    {
                    if (out[i] != null)
                        {
                        last = out[i];
                        total -= aux[i];
                        if (select >= total) return new Node[] { out[i] }; 
                        }
                    }
                // hmmmm....
                System.err.println("Automaton.Random Anomaly, select value was " + select + " and reduced total was " + total);
                return new Node[] { last };
                }
            }
        }

    /** Iterate has some N <= MAX_OUT outputs, each associated with a COUNT >= 1 (the aux).  We have a CURRENT OUTPUT
        and a CURRENT ITERATION for that output, both of which start at 0.  When we transition into the interate, it
        goes out the current output and increments the current iteration.  When the current iteration exceeds the COUNT
        for the current output, it is reset to 0 and we go to the next output.  When we exceed the count of the final
        output, we either STOP and not transition any more (the thread dies) or we wrap back around to the first input. */
    public static class Iterate extends Node
        {
        boolean loop;
        
        public int maxOut() { return MAX_OUT; }
        public Node copy() { Iterate copy = new Iterate(); copy.copyFrom(this); copy.loop = loop; return copy; } 
        public String getBaseName() { return "Iterate"; }
        public void save(JSONObject to, Automaton automaton) throws JSONException { to.put("type", "iterate"); to.put("loop", loop); super.save(to, automaton); }
        public void loadFinal(JSONObject from, Automaton automaton) throws JSONException { loop = from.optBoolean("loop", false); super.loadFinal(from, automaton); }
        public void setLoop(boolean loop) { this.loop = loop; }
        public boolean getLoop() { return loop; }
        public Iterate(boolean loop) { setLoop(loop); for(int i = 0; i < aux.length; i++) aux[i] = 1; }
        public Iterate() { this(false); }
        
        public Node[] selectOut() { throw new RuntimeException("Automaton.Iterate does not support selectOut()"); }
        public Node[] selectOut(int counter)  // not to be confused with Automaton.counter
            {
            int total = 0;
            for(int i = 0; i < out.length; i++)
                {
                if (out[i] != null) 
                    {
                    total += (int)aux[i];
                    }
                }
                
            if (total == 0) return new Node[0];
                
            if (counter >= total)
                {
                if (!getLoop()) return new Node[0];
                else
                    {
                    counter = counter % total;              // ugh division
                    }
                }
                
            Node last = null;
            total = 0;
            for(int i = 0; i < out.length; i++)
                {
                if (out[i] != null) { total += (int)aux[i]; last = out[i]; }
                if (counter < total) { return new Node[] { out[i] }; }
                }
                
            // If we got here there's an error
            System.err.println("Automaton.Iterate Anomaly, counter value was " + counter + " and total " + total);
            if (last == null) // we're empty
                return new Node[] { };
            else return new Node[] { last };
            }
        }

    /** Finished has a single output.  When we transition in, Finished immediately transitions out that output,
        but also flags the Automaton as Finished to its parent. */
    public static class Finished extends Node
        {
        public int maxOut() { return 1; };
        public Node copy() { return new Finished().copyFrom(this); } 
        public String getBaseName() { return "Finished"; }
        public void save(JSONObject to, Automaton automaton) throws JSONException { to.put("type", "finished"); super.save(to, automaton); }
        }
        
    /** MotifNode has a single output, a Motif.Child CHILD, a number of REPEATS >= 0, and a REPEAT PROBABILITY.
        When we transition in, MotifNode pulses its child once each step until the child reports FINISHED. 
        Then if repeats > 0, MotifNode resets the child and goes through pulsing sequences REPEATS times.
        Then each time a random boolean is <= repeat probability, it does the same thing.  When these things
        are all exhausted, it then transitions out its output. */
    public static class MotifNode extends Node
        {
        public static final int QUANTIZATION_NONE = 0;
        public static final int QUANTIZATION_SIXTEENTH = 1;
        public static final int QUANTIZATION_QUARTER = 2;
        public static final int QUANTIZATION_FOUR_QUARTERS = 3;
        public static final int[] QUANTIZATIONS = { 1,  Seq.PPQ / 4, Seq.PPQ, Seq.PPQ * 4 };
    
    public static final int MAX_REPEATS = 64;
        public static final int MAX_TRANSPOSE = 24;
        public static final double MAX_GAIN = 4.0;
        public static final double MAX_RATE = 16.0;
        public static final double DEFAULT_RATE = 1.0;
        public static final int DISABLED = -1;

        public int transpose = MAX_TRANSPOSE;                                   // ranges 0 ... MAX_TRANSPOSE * 2 inclusive, representing -MAX_TRANSPOSE ... MAX_TRANSPOSE
        public double rate = 1;
        public double gain = 1;
        public int outMIDI = DISABLED;		// It's called outMIDI so as not to be confused with Node.out

        public void setTranspose(int transpose) { this.transpose = transpose; }
        public int getTranspose() { return transpose; }

        public void setGain(double gain) { this.gain = gain; }
        public double getGain() { return gain; }

        public void setOutMIDI(int out) { this.outMIDI = out; }
        public int getOutMIDI() { return outMIDI; }
        
        public double getRate() { return rate; }
        public void setRate(double val) { rate = val; }
        
        // Should the selected child delay to a quantization boundary?
        int quantization = QUANTIZATION_SIXTEENTH;
        public int repeats = 0;
        public double repeatProbability = 0;
        public boolean repeatUntilTrigger = false;
        public Motif.Child child;
        
        public MotifNode(Motif.Child child) { this.child = child; }
        public Motif.Child getChild() { return child; }
        public Motif getMotif() { return child.getMotif(); }
        public int maxOut() { return 1; };

        protected Node copyFrom(Node node) 
            { 
            MotifNode other = (MotifNode)node;
            MotifNode motifNode = (MotifNode)(super.copyFrom(node));
                
            motifNode.repeats = other.repeats;
            motifNode.repeatProbability = other.repeatProbability;
            motifNode.quantization = other.quantization;
            motifNode.repeatUntilTrigger = other.repeatUntilTrigger;
            // keep the child
            motifNode.child = other.child;
            motifNode.rate = other.rate;
            motifNode.transpose = other.transpose;
            motifNode.gain = other.gain;
            motifNode.outMIDI = other.outMIDI;
            
            return motifNode;
            }
        public Node copy() { return new MotifNode(child).copyFrom(this); } 
        public void save(JSONObject to, Automaton automaton) throws JSONException 
            {
            to.put("type", "motif");
            to.put("child", automaton.getChildren().indexOf(child)); 
            to.put("repeats", repeats); 
            to.put("prob", repeatProbability); 
            to.put("trig", repeatUntilTrigger);
            to.put("quant", quantization);
        	to.put("rate", rate);
        	to.put("tran", transpose);
        	to.put("gain", gain);
        	to.put("outm", outMIDI);
            super.save(to, automaton); 
            }
        public void loadFinal(JSONObject from, Automaton automaton) throws JSONException 
            { 
            // "child" loaded at stub time
            repeats = from.optInt("repeats", 0);
            repeatProbability = from.optDouble("prob", 0.0);
            repeatUntilTrigger = from.optBoolean("trig", false);
            quantization = from.optInt("quant", QUANTIZATION_SIXTEENTH);
        transpose = from.optInt("tran", 0);
        rate = from.optDouble("rate", 1.0);
        gain = from.optDouble("gain", 1.0);
        outMIDI = from.optInt("outm", 0);
            super.loadFinal(from, automaton); 
            }
        public void setRepeats(int val) { repeats = val; }
        public int getRepeats() { return repeats; }
        public void setRepeatProbability(double val) { repeatProbability = val;  }
        public double getRepeatProbability() { return repeatProbability; }
        public void setRepeatUntilTrigger(boolean val) { repeatUntilTrigger = val; }
        public boolean getRepeatUntilTrigger() { return repeatUntilTrigger; }

        /** Return the quantization of the onset of new children.  This one of
            QUANTIZATION_NONE, QUANTIZATION_SIXTEENTH, QUANTIZATION_QUARTER, or QUANTIZATION_FOUR_QUARTERS
            presently. */
        public int getQuantization() { return quantization; }
        
        /** Set the quantization of the onset of new children.  This one of
            QUANTIZATION_NONE, QUANTIZATION_SIXTEENTH, QUANTIZATION_QUARTER, or QUANTIZATION_FOUR_QUARTERS
            presently. */
        public void setQuantization(int val) { quantization = val; }

        public String getBaseName() 
            { 
            String name = child.getMotif().getName(); 
            if (name == null || name.equals(""))
                {
                name = child.getMotif().getBaseName();
                }
            return name;
            }

        }

    public Motif copy()
        {
        Automaton other = (Automaton)(super.copy());
        other.nodes = new ArrayList<Node>();
        
        // Make copy of nodes with map from the old ones
        HashMap<Node, Node> map = new HashMap<>();        
        for(Node node : nodes)
            {
            Node copy = node.copy();
            map.put(node, copy);
            other.nodes.add(copy);
            }
        
        // Revise the outputs
        for(Node node : other.nodes)
            {
            Node[] out = node.getOut();
            for(int i = 0; i < out.length; i++)
                {
                if (out[i] != null)
                    {
                    out[i] = map.get(out[i]);
                    }
                }
            }
        
        // At this point, OTHER has nodes which have pointers to the
        // original children in THIS, but OTHER's children have been
        // copied and replaced.  We need to update OTHER's nodes to point
        // to those children.
        
        // We now have new children, but other's nodes are pointing to the original ones.
        // Update the children in the Motif Nodes to the new copies
        ArrayList<Motif.Child> originalChildren = getChildren();
        ArrayList<Motif.Child> newChildren = other.getChildren();
        HashMap<Motif.Child, Motif.Child> map2 = new HashMap<>();
        
        for(int i = 0; i < originalChildren.size(); i++)
            {
//            System.err.println("--- Mapping children " + originalChildren.get(i) + " -> " + newChildren.get(i));
            map2.put(originalChildren.get(i), newChildren.get(i));
            }
                
        for(Node node : other.nodes)
            {
            if (node instanceof MotifNode)
                {
                MotifNode motifnode = (MotifNode)node;
//                System.err.println("--- Changing child " + motifnode.child + " -> " + map2.get(motifnode.child));
                motifnode.child = map2.get(motifnode.child);
                }
            }           

        return other;
        }
        
    public Clip buildClip(Clip parent)
        {
        return new AutomatonClip(seq, this, parent);
        }
        
    public Automaton(Seq seq)
        {
        super(seq);
        }
        
    public MotifNode add(Motif motif)
        {
        Motif.Child child = addChild(motif);
        MotifNode node = new MotifNode(child);
        nodes.add(node);
        return node;
        } 

    public Fork addFork()
        {
        Fork node = new Fork();
        nodes.add(node);
        return node;
        } 

    public Join addJoin()
        {
        Join node = new Join();
        nodes.add(node);
        return node;
        } 

    public Random addRandom()
        {
        Random node = new Random();
        nodes.add(node);
        return node;
        } 

    /** Same as addDelay(0); */
    public Delay addDelay()
        {
        return addDelay(0);
        }

    public Delay addDelay(int delay)
        {
        Delay node = new Delay();
        nodes.add(node);
        return node;
        }

    public Chord addChord()
        {
        Chord node = new Chord();
        nodes.add(node);
        return node;
        }

    /** Same as addIterate(true); */
    public Iterate addIterate()
        {
        return addIterate(true);
        } 

    public Iterate addIterate(boolean loop)
        {
        Iterate node = new Iterate(loop);
        nodes.add(node);
        return node;
        } 

    public Finished addFinished()
        {
        Finished node = new Finished();
        nodes.add(node);
        return node;
        } 

    /// FIXME: Used to have a "Select"

    public void connect(Node from, Node to, int at, double aux)
        {
        from.out[at] = to;
        from.aux[at] = aux;
        }

    public void connect(Node from, Node to, int at)
        {
        from.out[at] = to;
        from.aux[at] = 0;
        }

    public void connect(int from, int to, int at, double aux)
        {
        connect(nodes.get(from), nodes.get(to), at, aux);
        }

    public void connect(int from, int to, int at)
        {
        connect(nodes.get(from), nodes.get(to), at);
        }

    public void disconnect(int from, int at)
        {
        Node fromNode = nodes.get(from);
        connect(fromNode, null, at, 0);
        }

    /** Warning: if NODE is the START NODE, a new node will be chosen to be the start node. */
    public void remove(Node node)
        {
        remove(nodes.indexOf(node));
        }
                
    /** Warning: if INDEX is 0 (and thus the node is the START NODE), a new node will be chosen to be the start node. */
    public void remove(int index)
        {
        Node removedNode = nodes.get(index);
        
        // First erase the node from connections
        for(Node node : nodes)
            {
            int len = node.out.length;
            for(int i = 0; i < len; i++)
                {
                if (node.out[i] == removedNode)       // disconnect!
                    {
                    node.out[i] = null;
                    }
                }
            }
                        
        /// Next remove node
        nodes.remove(index);
                        
        // Next remove from children
        if (removedNode instanceof MotifNode)
            {
            MotifNode removedMotifNode = (MotifNode) removedNode;
            removeChild(removedMotifNode.child);
            }
        }

    public void save(JSONObject to) throws JSONException 
        {
        super.save(to);
        JSONArray array = new JSONArray();
        for(Node node : nodes)
            {
            JSONObject saveTo = new JSONObject();
            node.save(saveTo, this);
            array.put(saveTo);
            }
        to.put("nodes", array);
        }

    public void load(JSONObject from) throws JSONException
        {
        super.load(from);
        nodes.clear();
        JSONArray array = from.getJSONArray("nodes");
        // Load stubs first
        for(int i = 0; i < array.length(); i++)
            {
            JSONObject obj = array.getJSONObject(i);
            nodes.add(loadStub(obj));
            }
        // Hook up stubs
        for(int i = 0; i < array.length(); i++)
            {
            JSONObject obj = array.getJSONObject(i);
            nodes.get(i).loadFinal(obj, this);
            }
        }
        
    static int counter = 1;
    public int getNextCounter() { return counter++; }
        
    public String getBaseName() { return "Automaton"; }
    }
        
