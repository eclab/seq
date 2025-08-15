/* 
   Copyright 2025 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.filter;

import seq.engine.*;
import seq.util.*;
import java.util.*;
import java.util.concurrent.*;

public class FilterClip extends Clip
    {
    private static final long serialVersionUID = 1;
    
    /// The four nodes representing our filter functions
    ArrayList<Node> nodes = new ArrayList<>();
    // Our child, if any
    Clip clip;

    public class Node
        {
        // These are all identity functions.  They either forward to the next filter function
        // or they go to parent() or they pass to seq.  We can't just call super() and let it handle
        // seq because we're in an inner class, so we have to explicitly inline it here.  :-( 
                
        public void noteOn(int out, int note, double vel, int id, int index)    
            {
            Filter filter = (Filter)getMotif();
            if (index == filter.NUM_TRANSFORMERS - 1) 
                {
                if (seq.getRoot() == FilterClip.this) seq.noteOn(out, note, vel);
                else getParent().noteOn(out, note, vel, id);
                }
            else nodes.get(index + 1).noteOn(out, note, vel, id, index + 1);
            }
        public int noteOn(int out, int note, double vel, int index)     
            {
            int id = noteID++;
            noteOn(out, note, vel, id, index);
            return id;
            }
        public void noteOff(int out, int note, double vel, int id, int index)
            {
            Filter filter = (Filter)getMotif();
            if (index == filter.NUM_TRANSFORMERS - 1) 
                {
                if (seq.getRoot() == FilterClip.this) seq.noteOff(out, note, vel);
                else getParent().noteOff(out, note, vel, id);
                }
            else nodes.get(index + 1).noteOff(out, note, vel, id, index + 1);
            }
        public void scheduleNoteOff(int out, int note, double vel, int time, int id, int index)
            {
            Filter filter = (Filter)getMotif();
            if (index == filter.NUM_TRANSFORMERS - 1) 
                {
                if (seq.getRoot() == FilterClip.this) seq.scheduleNoteOff(out, note, vel, time);
                else getParent().scheduleNoteOff(out, note, vel, time, id);
                }
            else nodes.get(index + 1).scheduleNoteOff(out, note, vel, time, id, index + 1);
            }
        public void sysex(int out, byte[] sysex, int index)
            {
            Filter filter = (Filter)getMotif();
            if (index == filter.NUM_TRANSFORMERS - 1)  
                {
                if (seq.getRoot() == FilterClip.this) seq.sysex(out, sysex);
                else getParent().sysex(out, sysex);
                }
            else nodes.get(index + 1).sysex(out, sysex, index + 1);
            }
        public void bend(int out, int val, int index)
            {
            Filter filter = (Filter)getMotif();
            if (index == filter.NUM_TRANSFORMERS - 1)  
                {
                if (seq.getRoot() == FilterClip.this) seq.bend(out, val);
                else getParent().bend(out, val);
                }
            else nodes.get(index + 1).bend(out, val, index + 1);
            }
        public void cc(int out, int cc, int val, int index)
            {
            Filter filter = (Filter)getMotif();
            if (index == filter.NUM_TRANSFORMERS - 1)  
                {
                if (seq.getRoot() == FilterClip.this) seq.cc(out, cc, val);
                else getParent().cc(out, cc, val);
                }
            else nodes.get(index + 1).cc(out, cc, val, index + 1);
            }
        public void pc(int out, int val, int index)
            {
            Filter filter = (Filter)getMotif();
            if (index == filter.NUM_TRANSFORMERS - 1)  
                {
                if (seq.getRoot() == FilterClip.this) seq.pc(out, val);
                else getParent().pc(out, val);
                }
            else nodes.get(index + 1).pc(out, val, index + 1);
            }
        public void aftertouch(int out, int note, int val, int index)
            {
            Filter filter = (Filter)getMotif();
            if (index == filter.NUM_TRANSFORMERS - 1)  
                {
                if (seq.getRoot() == FilterClip.this) seq.aftertouch(out, note, val);
                else getParent().aftertouch(out, note, val);
                }
            else nodes.get(index + 1).aftertouch(out, note, val, index + 1);
            }
        public void nrpn(int out, int nrpn, int val, int index)
            {
            Filter filter = (Filter)getMotif();
            if (index == filter.NUM_TRANSFORMERS - 1)  
                {
                if (seq.getRoot() == FilterClip.this) seq.nrpn(out, nrpn, val);
                else getParent().nrpn(out, nrpn, val);
                }
            else nodes.get(index + 1).nrpn(out, nrpn, val, index + 1);
            }
        public void nrpnCoarse(int out, int nrpn, int msb, int index)
            {
            Filter filter = (Filter)getMotif();
            if (index == filter.NUM_TRANSFORMERS - 1)  
                {
                if (seq.getRoot() == FilterClip.this) seq.nrpnCoarse(out, nrpn, msb);
                else getParent().nrpnCoarse(out, nrpn, msb);
                }
            else nodes.get(index + 1).nrpnCoarse(out, nrpn, msb, index + 1);
            }
        public void rpn(int out, int rpn, int val, int index)
            {
            Filter filter = (Filter)getMotif();
            if (index == filter.NUM_TRANSFORMERS - 1)  
                {
                if (seq.getRoot() == FilterClip.this) seq.rpn(out, rpn, val);
                else getParent().rpn(out, rpn, val);
                }
            else nodes.get(index + 1).rpn(out, rpn, val, index + 1);
            }
        
        // Informs the Node that we have been cut
        public void cut(int index) { }
        // Informs the Node that we have been released
        public void release(int index) { }
        // Called each timestep to let the node update itself, AFTER MIDI has been pushed through
        public void process(int index) { }
        // Asks the node if it believes it is finished yet
        public boolean finished() { return true; }
        // Informs the Node that we have been reset
        public void reset(int index) { }
        }




	/// ChangeNote Node
    public class ChangeNote extends Node
        {
        HashMap<Integer, Integer> map = new HashMap<>();                // Maps IDs to revised pitches
        
        public void release(int index)
            {
            map.clear();
            }
        
        public void cut(int index)
            {
            map.clear();
            }
        
        public void noteOn(int out, int note, double vel, int id, int index)    
            {
            Filter filter = (Filter)getMotif();
            Filter.ChangeNote func = (Filter.ChangeNote)(filter.getFunction(index));
            int _out = func.getOut();
            int transpose = func.getTranspose();
            double transposeV = func.getTransposeVariance();
            double gain = func.getGain();
            double gainV = func.getGainVariance();
            int length = func.getLength();
                        
            if (_out != Filter.ChangeNote.NO_OUT_CHANGE) out = _out;
            note += (transpose - Filter.MAX_TRANSPOSE);         // this centers it
            if (transposeV != 0) note += (seq.getDeterministicRandom().nextDouble() * transposeV * 2 - 1) * Filter.MAX_TRANSPOSE_NOISE;
            if (note > 127) note = 127;
            if (note < 0) note = 0;
            vel *= gain;
            if (gainV != 0) 
                {
                double val = seq.getDeterministicRandom().nextDouble() * gainV * Filter.MAX_TRANSPOSE_GAIN + 1.0;
                if (seq.getDeterministicRandom().nextBoolean()) val = 1.0 / val;
                vel *= val;
                }
            super.noteOn(out, note, vel, id, index);
                        
            if (length > 0)
                {
                super.scheduleNoteOff(out, note, 0x64, length, id, index);
                }
            else
                {
                map.put(id, note);
                }
            }
        public void noteOff(int out, int note, double vel, int id, int index)
            {
            Filter filter = (Filter)getMotif();
            Filter.ChangeNote func = (Filter.ChangeNote)(filter.getFunction(index));
                        
            int length = func.getLength();
            if (length > 0) return;         // we block it
                        
            double releaseGain = func.getReleaseGain();
            double releaseGainV = func.getReleaseGainVariance();

            vel *= releaseGain;
            if (releaseGainV != 0) 
                {
                double val = seq.getDeterministicRandom().nextDouble() * releaseGainV * Filter.MAX_TRANSPOSE_GAIN + 1.0;
                if (seq.getDeterministicRandom().nextBoolean()) val = 1.0 / val;
                vel *= val;
                }
                
            Integer newNote = map.remove(id);
            if (newNote != null)                            // revise note pitch?
                {
                note = newNote.intValue();
                }
                        
            super.noteOff(out, note, vel, id, index);               // hope for the best
            }
            
        public void scheduleNoteOff(int out, int note, double vel, int time, int id, int index)
            {
            Filter filter = (Filter)getMotif();
            Filter.ChangeNote func = (Filter.ChangeNote)(filter.getFunction(index));
                        
            int length = func.getLength();
            if (length > 0) return;         // we block it
                        
            double releaseGain = func.getReleaseGain();
            double releaseGainV = func.getReleaseGainVariance();
            vel *= releaseGain;
            
            Integer newNote = map.remove(id);
            if (newNote != null)                            // revise note pitch?
                {
                note = newNote.intValue();
                }
                        
            // FIXME: we can't screw with time in a meaningfuly way due to how Seq is set up for note generation.  :-(
            super.scheduleNoteOff(out, note, vel, time, id, index);
            }
        }
               
               
               
	/// Drop Node
    public class Drop extends Node
        {
        HashSet<Integer> dropped = new HashSet<>();
                
        public void noteOn(int out, int note, double vel, int id, int index)    
            {
            Filter filter = (Filter)getMotif();
            Filter.Drop func = (Filter.Drop)(filter.getFunction(index));
                        
            if (seq.getDeterministicRandom().nextDouble() < func.getProbability())
                {
                // drop
                dropped.add(id);
                }
            else
                {
                // don't drop
                super.noteOn(out, note, vel, id, index);
                }
            }
        public void noteOff(int out, int note, double vel, int id, int index)
            {
            Filter filter = (Filter)getMotif();
            Filter.Drop func = (Filter.Drop)(filter.getFunction(index));
                        
            if (dropped.remove(id))
                {
                // drop
                }
            else
                {
                // don't drop
                super.noteOff(out, note, vel, id, index);
                }
            }
        public void scheduleNoteOff(int out, int note, double vel, int time, int id, int index)
            {
            Filter filter = (Filter)getMotif();
            Filter.Drop func = (Filter.Drop)(filter.getFunction(index));
                        
            if (dropped.remove(id))
                {
                // drop
                }
            else
                {
                // don't drop
                super.scheduleNoteOff(out, note, vel, time, id, index);
                }
            }
        }




	/// Delay Node
    public class Delay extends Node
        {
        class DelayNote implements Comparable
            {
            int pos;                                                // the timestamp at which I was inserted into the queue (not played -- that's the key for me in the heap)
            int releasePos = -1;                    // the timestamp when I am supposed to be released
            boolean played = false;                 // have I been played yet, and am now waiting to release?
            int out;                                                // My out
            int note;                                               // My pitch
            double vel;                                             // My velocity
            int id;                                                 // My id -- this is only set when I am played
            int delay;
            double releaseVel = 64;                    // My release velocity

            public DelayNote(int out, int note, double vel, int pos) 
                { 
                this.out = out; 
                this.note = note; 
                this.vel = vel; 
                this.pos = pos; 
                }
                
            // the timestamp we should be sorted by
            public int sortTime()
                {
                return played ? releasePos : pos;
                }

            public int compareTo(Object obj)
                {
                if (obj == null) return -1;
                if (!(obj instanceof DelayNote)) return -1;
                DelayNote note = (DelayNote)obj;
                int st = sortTime();
                int nst = note.sortTime();
                return st < nst ? -1 : st == nst ? 0 : +1;
                }
            }
                        
        // All notes waiting to play or waiting to release, keyed by when they should be played or released
        Heap delay = new Heap();
        // Arrays of delayed notes for a given incoming note, keyed by the id of the incoming note
        HashMap<Integer, DelayNote[]> delayedNotes = new HashMap<>();           // indexed by id
                
                
        public boolean finished()
            {
            return delay.size() == 0;
            }
                
        // This is called when the Clip is processed, BEFORE noteOn/NoteOff/etc. may show up
        public void process(int index)
            {
            int pos = getPosition();
            while(true)
                {
                Comparable comp = delay.getMinKey();
                if (comp == null) break;
                int i = ((Integer)comp).intValue();
                if (i <= pos)
                    {
                    DelayNote note = (DelayNote)(delay.extractMin());
                    if (!note.played)
                        {
                        // this is tricky.  We can't call super.noteOn(...) without an ID because it will
                        // call noteOn(with ID), which calls OUR overridden version.  So we need to directly
                        // call super.noteOn(with ID), meaning that we need to generate our own ID here.
                        note.id = noteID++;
                        super.noteOn(note.out, note.note, note.vel, note.id, index);
                        note.played = true;
                        delay.add(note, note.releasePos);
                        }
                    else
                        {
                        super.noteOff(note.out, note.note, note.vel, note.id, index);                   // we do noteOff, not scheduleNoteOff
                        }
                    }
                else break;
                }
            }
                        
        public void cut(int index)
            {
            while(true)             // clear out all of them
                {
                Comparable comp = delay.getMinKey();
                if (comp == null) break;
                int i = ((Integer)comp).intValue();
                DelayNote note = (DelayNote)(delay.extractMin());
                if (note.played)
                    {
                    FilterClip.this.sendNoteOff(note.out, note.note, note.vel, note.id);                       // we do noteOff, not scheduleNoteOff
                    }
                }
            delayedNotes.clear();
            }
                
        public void release(int index)
            {
            int pos = getPosition();
            while(true)             // clear out all of them
                {
                Comparable comp = delay.getMinKey();
                if (comp == null) break;
                int i = ((Integer)comp).intValue();
                DelayNote note = (DelayNote)(delay.extractMin());
                if (note.played && note.releasePos != -1)
                    {
                    FilterClip.this.scheduleNoteOff(note.out, note.note, note.vel, note.releasePos - pos, note.id);                    // we do noteOff, not scheduleNoteOff
                    }
                }
            delayedNotes.clear();
            }
                
        public void noteOn(int out, int note, double vel, int id, int index)    
            {
            Filter filter = (Filter)getMotif();
            Filter.Delay func = (Filter.Delay)(filter.getFunction(index));
                        
            //int initialDelay = func.getInitialDelay();
            boolean original = func.getOriginal();
            double cut = func.getCut();
            int numTimes = func.getNumTimes();
            int laterDelay = func.getLaterDelay();
            int pos = getPosition();
                        
            if (original)
                {
                super.noteOn(out, note, vel, id, index);
                }            

            // Add delays to the heap and hash
            if (numTimes > 0)
                {
                DelayNote[] notes = new DelayNote[numTimes];
                for(int i = 0; i < numTimes; i++)
                    {
                    vel = vel * cut;
                    notes[i] = new DelayNote(out, note, vel, pos);
                    notes[i].delay = (i + 1) * laterDelay;
                    delay.add(notes[i], Integer.valueOf(notes[i].delay));
                    if (laterDelay == 0) // have to do it NOW
                        {
                        super.noteOn(out, note, vel, id, index);
                        }
                    }
                delayedNotes.put(id, notes);
                }
            }

        public void noteOff(int out, int note, double vel, int id, int index)
            {
            Filter filter = (Filter)getMotif();
            Filter.Delay func = (Filter.Delay)(filter.getFunction(index));
                        
            int laterDelay = func.getLaterDelay();

            // Add releases to the notes in hash
            DelayNote[] notes = delayedNotes.remove(id);
            if (notes != null)
                {
                for(int i = 0; i < notes.length; i++)
                    {
                    notes[i].releaseVel = vel;
                    notes[i].releasePos = notes[i].pos + notes[i].delay;
                    if (laterDelay == 0) // have to do it NOW
                        {
                        super.noteOff(out, notes[i].note, vel, id, index);
                        }
                    }
                }
            else
                {
                // We don't have this note, we need to pass it through
                // Or if we have an initial delay of 0 we have to pass it through NOW
                super.noteOff(out, note, vel, id, index);
                }
            }
            
        public void scheduleNoteOff(int out, int note, double vel, int time, int id, int index)
            {
            Filter filter = (Filter)getMotif();
            Filter.Delay func = (Filter.Delay)(filter.getFunction(index));
                        
            int laterDelay = func.getLaterDelay();

            // Add releases to the notes in hash
            DelayNote[] notes = delayedNotes.remove(id);
            if (notes != null)
                {
                for(int i = 0; i < notes.length; i++)
                    {
                    notes[i].releaseVel = vel;
                    notes[i].releasePos = notes[i].pos + time + notes[i].delay;
                    super.scheduleNoteOff(out, notes[i].note, vel, notes[i].releasePos, id, index);
                    }
                }
            else
                {
                // We don't have this note, we need to pass it through
                // Or if we have an initial delay of 0 we have to pass it through NOW
                super.scheduleNoteOff(out, note, vel, time, id, index); 
                }
            }
        }

	/// Noise Node
    public class Noise extends Node
        {
	    public static final int NUM_TRIES = 8;
        
        int lastValue = -1;			// The last value of the parameter I received from MIDI.  If -1, no value has been received.
        int lastIndex = 0;			// The index of the last MIDI message for the parameter
        int lastOut = 0;			// The out of the last MIDI message for the parameter
        int lastNote = 0;			// The note of the last MIDI message for the parameter, assuming it's Aftertouch
        int lastTime = 0;			// The time (position) of the last MIDI message for the parameter
        int lastRandom = 0; 		// The last chosen random value.
               
        public void reset(int index)
            {
            lastValue = -1;
            lastRandom = 0;
            lastIndex = 0;
            lastOut = 0;
            lastNote = 0;
            lastTime = 0;
            }
                
        void emit(int index)     
            {
            if (lastValue >= 0)
                {
                Filter filter = (Filter)getMotif();
                Filter.Noise func = (Filter.Noise)(filter.getFunction(index));

                boolean updateRandom = false;
                int position = getPosition();
                if (position == 0 || position - lastTime >= func.getRate())
                    {
                    updateRandom = true;
                    lastTime = position;
                    }
                
                Random rand = seq.getDeterministicRandom();
                                
                if (func.getParameterType() == Filter.Noise.TYPE_BEND)
                    {
                    int newVal = lastValue;
                    if (updateRandom)
                        {
                        for(int i = 0; i < NUM_TRIES; i++)
                            {
                            int rnd = (int)(func.generateRandomNoise(rand) * 8192);
                            if (rnd + newVal >= -8192 && rnd + newVal <= 8191)
                                {
                                lastRandom = rnd;
                                break;
                                }
                            }
                        }
                    newVal += lastRandom;
                    if (newVal < -8192 || newVal > 8191) newVal = lastValue;
                    super.bend(lastOut, newVal, lastIndex);
                    }
                else if (func.getParameterType() == Filter.Noise.TYPE_CC)
                    {
                    int newVal = lastValue;
                    if (updateRandom)
                        for(int i = 0; i < NUM_TRIES; i++)
                            {
                            int rnd = (int)(func.generateRandomNoise(rand) * 128);
                            if (rnd + newVal >= 0 && rnd + newVal <= 127)
                                {
                                lastRandom = rnd;
                                break;
                                }
                            }
                    newVal += lastRandom;
                    if (newVal < 0 || newVal > 128) newVal = lastValue;
                    super.cc(lastOut, func.getParameter(), newVal, lastIndex);
                    } 
                else if (func.getParameterType() == Filter.Noise.TYPE_NRPN)
                    {
                    int newVal = lastValue;
                    if (updateRandom)
                        for(int i = 0; i < NUM_TRIES; i++)
                            {
                            int rnd = (int)(func.generateRandomNoise(rand) * 16384);
                            if (rnd + newVal >= 0 && rnd + newVal <= 16383)
                                {
                                lastRandom = rnd;
                                break;
                                }
                            }
                    newVal += lastRandom;
                    if (newVal < 0 || newVal > 16384) newVal = lastValue;
                    super.nrpn(lastOut, func.getParameter(), newVal, lastIndex);
                    } 
                else if (func.getParameterType() == Filter.Noise.TYPE_RPN)
                    {
                    int newVal = lastValue;
                    if (updateRandom)
                        for(int i = 0; i < NUM_TRIES; i++)
                            {
                            int rnd = (int)(func.generateRandomNoise(rand) * 16384);
                            if (rnd + newVal >= 0 && rnd + newVal <= 16383)
                                {
                                lastRandom = rnd;
                                break;
                                }
                            }
                    newVal += lastRandom;
                    if (newVal < 0 || newVal > 16384) newVal = lastValue;
                    super.rpn(lastOut, func.getParameter(), newVal, lastIndex);
                    } 
                else if (func.getParameterType() == Filter.Noise.TYPE_AFTERTOUCH)
                    {
                    int newVal = lastValue;
                    if (updateRandom)
                        for(int i = 0; i < NUM_TRIES; i++)
                            {
                            int rnd = (int)(func.generateRandomNoise(rand) * 128);
                            if (rnd + newVal >= 0 && rnd + newVal <= 127)
                                {
                                lastRandom = rnd;
                                break;
                                }
                            }
                    newVal += lastRandom;
                    if (newVal < 0 || newVal > 128) newVal = lastValue;
                    super.aftertouch(lastOut, lastNote, newVal, lastIndex);
                    } 
                }
            }
                        
                
        public void bend(int out, int val, int index)
            {
            Filter filter = (Filter)getMotif();
            Filter.Noise func = (Filter.Noise)(filter.getFunction(index));
            if (func.getParameterType() == Filter.Noise.TYPE_BEND)
                {
                lastValue = val;
                lastIndex = index;
                lastOut = out;
                }
            emit(index);
            }
        public void cc(int out, int cc, int val, int index)
            {
            Filter filter = (Filter)getMotif();
            Filter.Noise func = (Filter.Noise)(filter.getFunction(index));
            if (func.getParameterType() == Filter.Noise.TYPE_CC)
                {
                lastValue = val;
                lastIndex = index;
                lastOut = out;
                }
            emit(index);
            }
        public void aftertouch(int out, int note, int val, int index)
            {
            Filter filter = (Filter)getMotif();
            Filter.Noise func = (Filter.Noise)(filter.getFunction(index));
            if (func.getParameterType() == Filter.Noise.TYPE_AFTERTOUCH)
                {
                lastValue = val;
                lastIndex = index;
                lastOut = out;
                lastNote = note;
                }
            emit(index);
            }
        public void nrpn(int out, int nrpn, int val, int index)
            {
            Filter filter = (Filter)getMotif();
            Filter.Noise func = (Filter.Noise)(filter.getFunction(index));
            if (func.getParameterType() == Filter.Noise.TYPE_NRPN)
                {
                lastValue = val;
                lastIndex = index;
                lastOut = out;
                }
            emit(index);
            }
        public void nrpnCoarse(int out, int nrpn, int msb, int index)
            {
            Filter filter = (Filter)getMotif();
            Filter.Noise func = (Filter.Noise)(filter.getFunction(index));
            if (func.getParameterType() == Filter.Noise.TYPE_NRPN)
                {
                lastValue = msb * 128;
                lastIndex = index;
                lastOut = out;
                }
            emit(index);
            }
        public void rpn(int out, int rpn, int val, int index)
            {
            Filter filter = (Filter)getMotif();
            Filter.Noise func = (Filter.Noise)(filter.getFunction(index));
            if (func.getParameterType() == Filter.Noise.TYPE_RPN)
                {
                lastValue = val;
                lastIndex = index;
                lastOut = out;
                }
            emit(index);
            }
        }


    /// SENT MESSAGES
    /// These are copies of the same messages in Clip, but with "send" in front.
    /// They allow me to override the Clip messages to route MIDI to the 
    /// filter, but have the filter send messages on with these.
    public int sendNoteOn(int out, int note, double vel) 
        {
        int id = noteID++;
        if (seq.getRoot() == this) seq.noteOn(out, note, vel);
        else getParent().noteOn(out, note, vel, id); 
        return id;
        }
        
    public void sendNoteOff(int out, int note, double vel, int id) 
        {
        if (seq.getRoot() == this) seq.noteOff(out, note, vel);
        else getParent().noteOff(out, note, vel, id); 
        }
        
    public void sendScheduleNoteOff(int out, int note, double vel, int time, int id) 
        {
        if (seq.getRoot() == this) seq.scheduleNoteOff(out, note, vel, time);
        else getParent().scheduleNoteOff(out, note, vel, time, id); 
        }







    public void noteOn(int out, int note, double vel, int id)    
        {
        if (active())
            {
            nodes.get(0).noteOn(out, note, vel, id, 0);
            }
        else
            super.noteOn(out, note, vel, id);
        }
    public void noteOff(int out, int note, double vel, int id)
        {
        if (active())
            nodes.get(0).noteOff(out, note, vel, id, 0);
        else
            super.noteOff(out, note, vel, id);
        }
    public void scheduleNoteOff(int out, int note, double vel, int time, int id)
        {
        if (active())
            nodes.get(0).scheduleNoteOff(out, note, vel, time, id, 0);
        else 
            super.scheduleNoteOff(out, note, vel, time, id);
        }
    public void sysex(int out, byte[] sysex)
        {
        if (active())
            nodes.get(0).sysex(out, sysex, 0);
        else 
            super.sysex(out, sysex);
        }
    public void bend(int out, int val)
        {
        if (active())
            nodes.get(0).bend(out, val, 0);
        else 
            super.bend(out, val);
        }
    public void cc(int out, int cc, int val)
        {
        if (active())
            nodes.get(0).cc(out, cc, val, 0);
        else
            super.cc(out, cc, val);
        }
    public void pc(int out, int val)
        {
        if (active())
            nodes.get(0).pc(out, val, 0);
        else
            super.pc(out, val);
        }
    public void aftertouch(int out, int note, int val)
        {
        if (active())
            nodes.get(0).aftertouch(out, note, val, 0);
        else 
            super.aftertouch(out, note, val);
        }
    public void nrpn(int out, int nrpn, int val)
        {
        if (active())
            nodes.get(0).nrpn(out, nrpn, val, 0);
        else
            super.nrpn(out, nrpn, val);
        }
    public void nrpnCoarse(int out, int nrpn, int msb)
        {
        if (active())
            nodes.get(0).nrpnCoarse(out, nrpn, msb, 0);
        else
            super.nrpn(out, nrpn, msb);
        }
    public void rpn(int out, int rpn, int val)
        {
        if (active())
            nodes.get(0).rpn(out, rpn, val, 0);
        else
            super.rpn(out, rpn, val);
        }
                        
                        



    public FilterClip(Seq seq, Filter filter, Clip parent)
        {
        super(seq, filter, parent);
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
        Filter filter = (Filter)getMotif();
        ArrayList<Motif.Child> children = filter.getChildren();
        if (children.size() > 0)
            {
            Motif.Child child = children.get(0);
            clip = child.getMotif().buildClip(this);
            }
            
        nodes.clear();
        for(int i = 0; i < Filter.NUM_TRANSFORMERS; i++)
            {
            nodes.add(buildNode(filter, i));
            }
        }
        
    public Node buildNode(Filter trans, int index)
        {
        String type = trans.getFunction(index).getType();
        if (Filter.IDENTITY.equals(type))
            {
            return new Node();
            }
        else if (Filter.CHANGE_NOTE.equals(type))
            {
            return new ChangeNote();
            }
        else if (Filter.DELAY.equals(type))
            {
            return new Delay();
            }
        else if (Filter.DROP.equals(type))
            {
            return new Drop();
            }
        else if (Filter.NOISE.equals(type))
            {
            return new Noise();
            }
        else
            {
            System.err.println("FilterClip.buildNode() ERROR: unknown type " + type);
            return new Node();
            }
        }
    
    boolean childDone = false;

    public void reset()
        {
        super.reset();
        if (clip == null)
            {
            buildClip();
            }
        clip.reset();
        childDone = false;

        for(int i = 0; i < Filter.NUM_TRANSFORMERS; i++)
            {
            nodes.get(i).reset(i);
            }
        }
        
    public void loop()
        {
        super.loop();
        if (clip == null)
            {
            buildClip();
            }
        clip.reset();
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

        for(int i = 0; i < Filter.NUM_TRANSFORMERS; i++)
            {
            nodes.get(i).cut(i);
            }
        }
    

    public void release() 
        { 
        super.release();
        if (clip != null)
            {
            clip.release();
            }

        for(int i = 0; i < Filter.NUM_TRANSFORMERS; i++)
            {
            nodes.get(i).release(i);
            }
        }
        
    boolean active()
        {
        Filter filter = (Filter)getMotif();
        int position = getPosition();
        return (filter.isAlways() || (position >= filter.getFrom() && position < filter.getTo()));
        }
    
            
    public boolean process()
        {
        Filter filter = (Filter)getMotif();

        boolean done = true;
        if (clip != null)
            {
            if (!childDone)
                {
                childDone = clip.advance();
                }
            done = childDone;

            if (filter.isAlways() || filter.getTo() > filter.getFrom())
                {
                int position = getPosition();
                                
                if (position == filter.getTo() - 1 && !filter.isAlways())
                    {
                    // we just completed the range
                    for(int i = 0; i < Filter.NUM_TRANSFORMERS; i++)
                        {
                        nodes.get(i).process(i);
                        }           
                    // release all notes
                    for(int i = 0; i < Filter.NUM_TRANSFORMERS; i++)
                        {
                        nodes.get(i).release(i);
                        }           
                    }
                else if (active())
                    {
                    // we are in the range
                    for(int i = 0; i < Filter.NUM_TRANSFORMERS; i++)
                        {
                        nodes.get(i).process(i);
                        }                            
                    }
                }
            }
        
        // Our child may be done but we're not done yet...
        for(int i = 0; i < Filter.NUM_TRANSFORMERS; i++)
            {
            done = done && nodes.get(i).finished();
            }
                
        return done;
        }
    }




