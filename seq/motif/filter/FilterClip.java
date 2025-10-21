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
        
        // this function exists so both versions of noteOn(...) can call it, rather than each other, 
        // ans do so we can override either one of them, or both of them, in subclasses.  The big
        // problematic class which necessitated this was Delay.
        void _noteOn(int out, int note, double vel, int id, int index)
            {
            Filter filter = (Filter)getMotif();
            if (index == filter.NUM_TRANSFORMERS - 1) 
                {
                sendNoteOn(out, note, vel, id);
                }
            else nodes.get(index + 1).noteOn(out, note, vel, id, index + 1);
            }
            
        public void noteOn(int out, int note, double vel, int id, int index)    
            {
            _noteOn(out, note, vel, id, index);
            }
            
        public int noteOn(int out, int note, double vel, int index)     
            {
            int id = noteID++;
            _noteOn(out, note, vel, id, index);
            return id;
            }
            
        public void noteOff(int out, int note, double vel, int id, int index)
            {
            Filter filter = (Filter)getMotif();
            if (index == filter.NUM_TRANSFORMERS - 1) 
                {
                sendNoteOff(out, note, vel, id);
                }
            else nodes.get(index + 1).noteOff(out, note, vel, id, index + 1);
            }
            
        public void scheduleNoteOff(int out, int note, double vel, int time, int id, int index)
            {
            Filter filter = (Filter)getMotif();
            if (index == filter.NUM_TRANSFORMERS - 1) 
                {
                sendScheduleNoteOff(out, note, vel, time, id);
                }
            else nodes.get(index + 1).scheduleNoteOff(out, note, vel, time, id, index + 1);
            }
            
        public int scheduleNoteOn(int out, int note, double vel, int time, int index)
            {
            Filter filter = (Filter)getMotif();
            if (index == filter.NUM_TRANSFORMERS - 1) 
                {
                return sendScheduleNoteOn(out, note, vel, time);
                }
            else return nodes.get(index + 1).scheduleNoteOn(out, note, vel, time, index + 1);
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
        HashMap<Integer, Integer> outs = new HashMap<>();                // Maps IDs to outs
        HashMap<Integer, Integer> mapScheduled = new HashMap<>();       // Maps IDs to revised pitches
        
        public void release(int index)
            {
            // We ought to send a NoteOff to everyone in the map, since we never cleared them.  This SHOULD NOT HAPPEN.
            if (map.size() > 0)
                {
                System.err.println("FilterClip.ChangeNote.release(): non-released notes.");
                for(Integer id : map.keySet())
                    {
                    Integer note = map.get(id);
                    Integer out = outs.get(id);
                    super.noteOff(out.intValue(), note.intValue(), 0x40, id.intValue(), index);             // should we send it this way or bypass and go straight to parent?
                    }
                }
                
            map.clear();
            outs.clear();
            mapScheduled.clear();
            }
        
        public void cut(int index)
            {
            // We ought to send a NoteOff to everyone in the map, since we never cleared them.  This SHOULD NOT HAPPEN.
            if (map.size() > 0)
                {
                System.err.println("FilterClip.ChangeNote.cut(): non-released notes.");
                for(Integer id : map.keySet())
                    {
                    Integer note = map.get(id);
                    Integer out = outs.get(id);
                    super.noteOff(out.intValue(), note.intValue(), 0x40, id.intValue(), index);             // should we send it this way or bypass and go straight to parent?
                    }
                }
                
            map.clear();
            outs.clear();
            mapScheduled.clear();
            }
        
        public void bend(int out, int val, int index)
            {
            Filter.ChangeNote func = (Filter.ChangeNote)(((Filter)getMotif()).getFunction(index));
            if (func.isAllOut())
                {
                int _out = func.getOut();
                if (_out != Filter.ChangeNote.NO_OUT_CHANGE) out = _out;
                }
            super.bend(out, val, index);
            }
        public void cc(int out, int cc, int val, int index)
            {
            Filter.ChangeNote func = (Filter.ChangeNote)(((Filter)getMotif()).getFunction(index));
            if (func.isAllOut())
                {
                int _out = func.getOut();
                if (_out != Filter.ChangeNote.NO_OUT_CHANGE) out = _out;
                }
            super.cc(out, cc, val, index);
            }
        public void pc(int out, int val, int index)
            {
            Filter.ChangeNote func = (Filter.ChangeNote)(((Filter)getMotif()).getFunction(index));
            if (func.isAllOut())
                {
                int _out = func.getOut();
                if (_out != Filter.ChangeNote.NO_OUT_CHANGE) out = _out;
                }
            super.pc(out, val, index);
            }

        public void nrpn(int out, int nrpn, int val, int index)
            {
            Filter.ChangeNote func = (Filter.ChangeNote)(((Filter)getMotif()).getFunction(index));
            if (func.isAllOut())
                {
                int _out = func.getOut();
                if (_out != Filter.ChangeNote.NO_OUT_CHANGE) out = _out;
                }
            super.nrpn(out, nrpn, val, index);
            }
        public void nrpnCoarse(int out, int nrpn, int msb, int index)
            {
            Filter.ChangeNote func = (Filter.ChangeNote)(((Filter)getMotif()).getFunction(index));
            if (func.isAllOut())
                {
                int _out = func.getOut();
                if (_out != Filter.ChangeNote.NO_OUT_CHANGE) out = _out;
                }
            super.nrpnCoarse(out, nrpn, msb, index);
            }

        public void rpn(int out, int rpn, int val, int index)
            {
            Filter.ChangeNote func = (Filter.ChangeNote)(((Filter)getMotif()).getFunction(index));
            if (func.isAllOut())
                {
                int _out = func.getOut();
                if (_out != Filter.ChangeNote.NO_OUT_CHANGE) out = _out;
                }
            super.rpn(out, rpn, val, index);
            }

        public void noteOn(int out, int note, double vel, int id, int index)    
            {
            Filter filter = (Filter)getMotif();
            Filter.ChangeNote func = (Filter.ChangeNote)(filter.getFunction(index));
            int _out = func.getOut();
            int transpose = getCorrectedValueInt(func.getTranspose(), Filter.MAX_TRANSPOSE * 2);
            double transposeV = getCorrectedValueDouble(func.getTransposeVariance(), 1.0);
            double gain = getCorrectedValueDouble(func.getGain(), Filter.MAX_GAIN);
            double gainV = getCorrectedValueDouble(func.getGainVariance(), 1.0);
            int length = func.getLength();
            boolean changeLength = func.getChangeLength();
                        
            if (_out != Filter.ChangeNote.NO_OUT_CHANGE) out = _out;
            note += (transpose - Filter.MAX_TRANSPOSE);         // this centers it
            if (transposeV != 0) note += (seq.getDeterministicRandom().nextDouble() * transposeV * 2 - 1) * Filter.MAX_TRANSPOSE_NOISE;
            if (note > 127) note = 127;
            if (note < 0) note = 0;
            vel *= gain;
            if (gainV != 0) 
                {
                double val = seq.getDeterministicRandom().nextDouble() * gainV * Filter.MAX_GAIN + 1.0;
                if (seq.getDeterministicRandom().nextBoolean()) val = 1.0 / val;
                vel *= val;
                }
            super.noteOn(out, note, vel, id, index);
                        
            if (changeLength)
                {
                super.scheduleNoteOff(out, note, (double)0x40, length, id, index);
                mapScheduled.put(id, note);                                     // WARNING this could create a memory leak, better that than stuck notes?
                }
            else
                {
                map.put(id, note);
                outs.put(id, out);
                }
            }
            
        public void noteOff(int out, int note, double vel, int id, int index)
            {
            Filter filter = (Filter)getMotif();
            Filter.ChangeNote func = (Filter.ChangeNote)(filter.getFunction(index));
                        
            double releaseGain = getCorrectedValueDouble(func.getReleaseGain(), Filter.MAX_GAIN);
            double releaseGainV = getCorrectedValueDouble(func.getReleaseGainVariance(), 1.0);

            vel *= releaseGain;
            if (releaseGainV != 0) 
                {
                double val = seq.getDeterministicRandom().nextDouble() * releaseGainV * Filter.MAX_GAIN + 1.0;
                if (seq.getDeterministicRandom().nextBoolean()) val = 1.0 / val;
                vel *= val;
                }
            
            Integer newNote = map.remove(id);
            outs.remove(id);
            if (newNote != null)                                                // revise note pitch?
                {
                note = newNote.intValue();
                super.noteOff(out, note, vel, id, index);
                }
            else
                {
                Integer scheduledNote = mapScheduled.remove(id);
                if (scheduledNote == null)                      // not scheduled already
                    {
                    super.noteOff(out, note, vel, id, index);              // hope for the best
                    }
                }
            }
            
        public void scheduleNoteOff(int out, int note, double vel, int time, int id, int index)
            {
            Filter filter = (Filter)getMotif();
            Filter.ChangeNote func = (Filter.ChangeNote)(filter.getFunction(index));
                        
            double releaseGain = getCorrectedValueDouble(func.getReleaseGain(), Filter.MAX_GAIN);
            double releaseGainV = getCorrectedValueDouble(func.getReleaseGainVariance(), 1.0);
            vel *= releaseGain;
            
            Integer newNote = map.remove(id);
            outs.remove(id);
            if (newNote != null)                            // revise note pitch?
                {
                note = newNote.intValue();
                super.scheduleNoteOff(out, note, vel, time, id, index);
                }
            else
                {
                Integer scheduledNote = mapScheduled.remove(id);
                if (scheduledNote == null)                      // not scheduled already
                    {
                    super.scheduleNoteOff(out, note, vel, time, id, index); // hope for the best
                    }
                }
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
            int pos = getPosition();
            
            double probability = getCorrectedValueDouble(func.getProbability(), 1.0);
            if (func.getCut() || seq.getDeterministicRandom().nextDouble() < probability)
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

        public void bend(int out, int val, int index)
            {
            Filter filter = (Filter)getMotif();
            Filter.Drop func = (Filter.Drop)(filter.getFunction(index));

            if (func.getCut())
                {
                // drop
                }
            else
                {
                // don't drop
                super.bend(out, val, index);
                }
            }
        public void cc(int out, int cc, int val, int index)
            {
            Filter filter = (Filter)getMotif();
            Filter.Drop func = (Filter.Drop)(filter.getFunction(index));

            if (func.getCut())
                {
                // drop
                }
            else
                {
                // don't drop
                super.cc(out, cc, val, index);
                }
            }
        public void pc(int out, int val, int index)
            {
            Filter filter = (Filter)getMotif();
            Filter.Drop func = (Filter.Drop)(filter.getFunction(index));

            if (func.getCut())
                {
                // drop
                }
            else
                {
                // don't drop
                super.pc(out, val, index);
                }
            }

        public void nrpn(int out, int nrpn, int val, int index)
            {
            Filter filter = (Filter)getMotif();
            Filter.Drop func = (Filter.Drop)(filter.getFunction(index));

            if (func.getCut())
                {
                // drop
                }
            else
                {
                // don't drop
                super.nrpn(out, nrpn, val, index);
                }
            }
        public void nrpnCoarse(int out, int nrpn, int msb, int index)
            {
            Filter filter = (Filter)getMotif();
            Filter.Drop func = (Filter.Drop)(filter.getFunction(index));

            if (func.getCut())
                {
                // drop
                }
            else
                {
                // don't drop
                super.nrpnCoarse(out, nrpn, msb, index);
                }
            }

        public void rpn(int out, int rpn, int val, int index)
            {
            Filter filter = (Filter)getMotif();
            Filter.Drop func = (Filter.Drop)(filter.getFunction(index));

            if (func.getCut())
                {
                // drop
                }
            else
                {
                // don't drop
                super.rpn(out, rpn, val, index);
                }
            }



        }


public class Delay extends Node
	{
	class DelayNote
		{
		public int out;						// output of the original note and the delayed notes
		public int note;					// pitch of the original note and the delayed notes
		public int originalID = -1;			// id of the original note
		public int[] delays;				// delays of the delayed notes
		public int[] ids;					// id of the delayed notes
		
		public DelayNote(int out, int note, int numDelays, int originalID)
			{
			this.out = out;
			this.note = note;
			this.originalID = originalID;
			this.ids = new int[numDelays];				// these will have to be set later
			this.delays = new int[numDelays];			// these will have to be set later
			}
		}
		
	public void reset(int index)
		{
		clearNotes();
		}
	
	public void cut(int index)
		{
		clearNotes();		// this doesn't clear, but rather releases, but we have to do it after the note-on onsets...
		}
		
	public void release(int index)
		{
		clearNotes();		// sadly we don't know when the release should be!  So we have to fake it.
		}
		

	// A map, of originalID -> DelayNote, of currently playing notes
	HashMap<Integer, DelayNote> notePlaying = new HashMap<Integer, DelayNote>();
	
	// Turn off all playing notes.  We try to turn them off by a large enough delay
	// that the note-off is AFTER the note-on.
	void clearNotes()
		{
		for(Integer id : notePlaying.keySet())
			{
			DelayNote note = notePlaying.get(id);

			// Clear original LENGTH after the onset, which is NOW
			if (note.originalID >= 0)
				{
				sendNoteOff(note.out, note.note, 0x40, note.originalID);		// do it now
				}
			
			// Clear the delays LENGTH after their corresponding onsets
			for(int i = 0; i < note.delays.length; i++)
				{
				sendScheduleNoteOff(note.out, note.note, 0x40, note.delays[i], note.ids[i]);
				}
			}
			
		notePlaying.clear();
		}
		
		
	/** Play the original note if instructed to, then schedule all the delays.
		Create a DelayNote and add it to the notePlaying hash.
	*/
	public void noteOn(int out, int note, double vel, int id, int index)    
		{
		Filter filter = (Filter)getMotif();
		Filter.Delay func = (Filter.Delay)(filter.getFunction(index));
		
		int numTimes = func.getNumTimes();
		double cut = func.getCut();
		int delayInterval = func.getDelayInterval();
		boolean random = func.getRandom();
		boolean original = func.getOriginal();
		
		DelayNote dnote = new DelayNote(out, note, numTimes, original ? id : -1);
		
		// Send original
		if (func.getOriginal())
			{
			sendNoteOn(out, note, vel, id);
			}
		
		// Send delays
		for(int i = 0; i < numTimes; i++)
			{
			vel *= cut;
			dnote.delays[i] = 
				random ?
					(int)(seq.getDeterministicRandom().nextDouble() * ((i + 1) * delayInterval)) :
					(i + 1) * delayInterval;
			dnote.ids[i] = sendScheduleNoteOn(out, note, vel, dnote.delays[i]);
			notePlaying.put(id, dnote);
			}
		}
                        
	/** Stop playing the original note if instructed to, then schedule all the delays to be turned off.
		Remove the DelayNote from the hash.
	*/
	public void noteOff(int out, int note, double vel, int id, int index)
		{
		Filter filter = (Filter)getMotif();
		Filter.Delay func = (Filter.Delay)(filter.getFunction(index));
		
		DelayNote dnote = notePlaying.remove(id);
		if (dnote != null)
			{
			// Send original
			if (dnote.originalID >= 0)
				{
				sendNoteOff(dnote.out, dnote.note, vel, dnote.originalID);
				}
				
			for(int i = 0; i < dnote.delays.length; i++)
				{
				sendScheduleNoteOff(dnote.out, dnote.note, vel, dnote.delays[i], dnote.ids[i]);
				}
			}
        }      
	}
	
	
	
	
/*
    /// Delay Node
    public class Delay extends Node
        {
        final static int STATE_NOT_PLAYED = 0;
        final static int STATE_PLAYED = 1;
        final static int STATE_RELEASED = 2;
        
        class DelayNote implements Comparable
            {
            int pos;                                // The timestamp at which I was inserted into the queue
            int state = STATE_NOT_PLAYED;           // Have I been not played yet, played, or released?
            int playPos;							// The timestamp at which I am supposed to be played
            int releasePos = -1;                    // the timestamp when I am supposed to be released.  If -1, then this time has not yet been determined.
            int out;                                // My out
            int note;                               // My pitch
            double vel;                             // My velocity
            double releaseVel = 64;                 // My release velocity
            int id;                                 // My id -- this is only set when I am played

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
                return state == STATE_NOT_PLAYED ? pos : (state == STATE_RELEASED ? Seq.MIN_MAX_TIME : releasePos);
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
        
        public void reset(int index)
        	{
        	release(index);
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
                    if (note.state == STATE_NOT_PLAYED)
                        {
                        note.state = STATE_PLAYED;
                        super.noteOn(note.out, note.note, note.vel, note.id, index);
                        
                        // reinsert only if we have a release pos!
                        // This is tricky.  If we have a release pos,
                        // the MIDI has already arrived, but we couldn't add the
                        // released note then because it was already in the heap.  Now
                        // that it's been removed from the delay, we can finally
                        // re-add it.  So we want to add it now.
                        //
                        // But if we don't have a release pos, the MIDI has not
                        // yet arrived, so we can't add it yet, and we'll add it
                        // when the MIDI shows up.
                        if (note.releasePos != -1)
                        	{
                        	delay.add(note, Integer.valueOf(note.releasePos));
                        	}
                        	                        
                        // this is tricky.  We can't call super.noteOn(...) without an ID because it will
                        // call noteOn(with ID), which calls OUR overridden version.  So we need to directly
                        // call super.noteOn(with ID), meaning that we need to generate our own ID here.
                        //note.id = noteID++;
                        //super.noteOn(note.out, note.note, note.vel, note.id, index);
                        }
                    else if (note.state == STATE_PLAYED)
                        {
                        note.state = STATE_RELEASED;
                        super.noteOff(note.out, note.note, note.releaseVel, note.id, index);
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
                if (note.state == STATE_PLAYED)
                    {
                    sendNoteOff(note.out, note.note, note.releaseVel, note.id);
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
                if (note.state == STATE_PLAYED)
                    {
                    if (note.releasePos > 0)  // we have a release pos
                    	{
	                    System.err.println("Schedule for " + (note.releasePos - pos));
                   	 	sendScheduleNoteOff(note.out, note.note, note.vel, note.releasePos - pos, note.id); 
                   	 	}
                   	else					// we should cut now
                   		{
	                    System.err.println("Cutting Now?");
                   	 	sendNoteOff(note.out, note.note, note.vel, note.id); 
                   		}
                    }
                else if (note.state == STATE_NOT_PLAYED)
                	{
                    if (note.releasePos > 0)  // we have a release pos
                    	{
                     	System.err.println("Schedule ON for " + (note.playPos - pos));
                    	note.id = sendScheduleNoteOn(note.out, note.note, note.vel, note.playPos - pos); 
                   		System.err.println("Schedule OFF for " + (note.releasePos - pos));
	                    sendScheduleNoteOff(note.out, note.note, note.vel, note.releasePos - pos, note.id);
                    	}
                    else
                    	{
                    	System.err.println("Not Playing");
                    	}
                	}
                }
            delayedNotes.clear();
            }
                
        public void noteOn(int out, int note, double vel, int id, int index)    
            {
            Filter filter = (Filter)getMotif();
            Filter.Delay func = (Filter.Delay)(filter.getFunction(index));
                        
            boolean original = func.getOriginal();
            boolean random = func.getRandom();
            double cut = getCorrectedValueDouble(func.getCut(), 1.0);
            int numTimes = getCorrectedValueInt(func.getNumTimes(), Filter.MAX_DELAY_NUM_TIMES);
            int delayInterval = func.getDelayInterval();
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
                    if (random)
                    	{
                    	notes[i].playPos = pos + (int)(seq.getDeterministicRandom().nextDouble() * ((i + 1) * delayInterval));
                    	}
                    else
                    	{
                    	notes[i].playPos = pos + (i + 1) * delayInterval;
                    	}
                    delay.add(notes[i], Integer.valueOf(notes[i].playPos));
                    if (delayInterval == 0) // have to do it NOW
                        {
	                    notes[i].state = STATE_PLAYED;
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
                        
            int delayInterval = func.getDelayInterval();

            // Add releases to the notes in hash
            int pos = getPosition();
            DelayNote[] notes = delayedNotes.remove(id);
            if (notes != null)
                {
                for(int i = 0; i < notes.length; i++)
                    {
                    notes[i].releaseVel = vel;
                    notes[i].releasePos = pos + notes[i].playPos;
                    if (delayInterval == 0) // have to do it NOW
                        {
	                    notes[i].state = STATE_RELEASED;
                        super.noteOff(out, notes[i].note, vel, id, index);
                        }
                    else
                    	{
                    	// We only add the released note if it's not already in
                    	// the delay and being used as a play-note.  Otherwise
                    	// we'll wait and add it after the play-note has played.
                    	// See process() above. 
                    	
                    	System.err.println("Added for " + notes[i].releasePos);
                    	if (notes[i].state != STATE_NOT_PLAYED)		// it's no longer in the heap
                    		{
                    		delay.add(notes[i], Integer.valueOf(notes[i].releasePos));
                    		}
                    	// else we'll wait until it comes up in the heap and then play it
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
                        
            int delayInterval = func.getDelayInterval();

            // Add releases to the notes in hash
            int pos = getPosition();
            DelayNote[] notes = delayedNotes.remove(id);
            if (notes != null)
                {
                for(int i = 0; i < notes.length; i++)
                    {
                    notes[i].releaseVel = vel;
                    notes[i].releasePos = pos + notes[i].playPos;
                    System.err.println("Schedule OFF for " + (pos - notes[i].releasePos));
                    notes[i].state = STATE_RELEASED;
                    super.scheduleNoteOff(out, notes[i].note, vel, (pos - notes[i].releasePos), id, index);
                    }
                }
            else
                {
                // We don't have this note, we need to pass it through
                // Or if we have an initial delay of 0 we have to pass it through NOW
                super.scheduleNoteOff(out, note, vel, time, id, index); 
                }
            }

        public int scheduleNoteOn(int out, int note, double vel, int time, int index)
            {
            Filter filter = (Filter)getMotif();
            Filter.Delay func = (Filter.Delay)(filter.getFunction(index));
            int id = noteID++;
                        
                        //// FIXME
                        
            int delayInterval = func.getDelayInterval();

            // Add releases to the notes in hash
            int pos = getPosition();
            DelayNote[] notes = delayedNotes.remove(id);
            if (notes != null)
                {
                for(int i = 0; i < notes.length; i++)
                    {
                    notes[i].vel = vel;
                    notes[i].pos = pos + notes[i].playPos;
                    System.err.println("Schedule ON for " + (pos - notes[i].playPos));
                    // Now the big issue is what my state is.
                    if (notes[i].state == STATE_NOT_PLAYED)
                    	{
                    	notes[i].state = STATE_PLAYED;
	                    super.scheduleNoteOn(out, notes[i].note, vel, (pos - notes[i].playPos), index);
	                    }
	                // else, um....?
                    }
                }
            else
                {
                // We don't have this note, we need to pass it through
                // Or if we have an initial delay of 0 we have to pass it through NOW
                return super.scheduleNoteOn(out, note, vel, time, index); 
                }
            }
        }
    */


    /// Noise Node
    public class Noise extends Node
        {
        public static final int NUM_TRIES = 8;
        
        int lastValue = -1;                     // The last value of the parameter I received from MIDI.  If -1, no value has been received.
        int lastIndex = 0;                      // The index of the last MIDI message for the parameter
        int lastOut = 0;                        // The out of the last MIDI message for the parameter
        int lastNote = 0;                       // The note of the last MIDI message for the parameter, assuming it's Aftertouch
        int lastTime = 0;                       // The time (position) of the last MIDI message for the parameter
        int lastRandom = 0;             // The last chosen random value.
               
        public void reset(int index)
            {
            lastValue = -1;
            lastRandom = 0;
            lastIndex = 0;
            lastOut = 0;
            lastNote = 0;
            lastTime = 0;
            }
                
        public double generateRandomNoise(Filter.Noise func, Random rand)
            {
            double distVar = getCorrectedValueDouble(func.getDistVar(), 1.0);
            return rand.nextDouble() * (distVar * 2) - distVar;
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
                            int rnd = (int)(generateRandomNoise(func, rand) * 8192);
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
                            int rnd = (int)(generateRandomNoise(func, rand) * 128);
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
                            int rnd = (int)(generateRandomNoise(func, rand) * 16384);
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
                            int rnd = (int)(generateRandomNoise(func, rand) * 16384);
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
                            int rnd = (int)(generateRandomNoise(func, rand) * 128);
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
           
                

    /// Map Node
    public class Map extends Node
        {
        public void bend(int out, int val, int index)
            {
            Filter filter = (Filter)getMotif();
            Filter.Map func = (Filter.Map)(filter.getFunction(index));
            if (func.getParameterType() == Filter.Map.TYPE_BEND)
                {
                boolean negative = (val < 0);
                if (negative) val = 0 - val;
                int min = (int)(func.getMin() * 8192);
                int max = (int)(func.getMax() * 8192);
                if (min > max) { int swap = min; min = max; max = swap; }
                if (val < min) val = min;
                if (val > max) val = max;
                if (max != min)
                    {
                    double _val = (val - min) / (double) (max - min);
                    _val = func.map(_val, getCorrectedValueDouble(func.getVariable(), 1.0));
                    val = min + (int)(_val * (max - min));
                    if (negative) val = 0 - val;
                    if (val >= 8192) val = 8191;
                    }
                }
            super.bend(out, val, index);
            }
        public void cc(int out, int cc, int val, int index)
            {
            Filter filter = (Filter)getMotif();
            Filter.Map func = (Filter.Map)(filter.getFunction(index));
            if (func.getParameterType() == Filter.Map.TYPE_CC)
                {
                int min = (int)(func.getMin() * 127);
                int max = (int)(func.getMax() * 127);
                if (min > max) { int swap = min; min = max; max = swap; }
                if (val < min) val = min;
                if (val > max) val = max;
                if (max != min)
                    {
                    double _val = (val - min) / (double) (max - min);
                    _val = func.map(_val, getCorrectedValueDouble(func.getVariable(), 1.0));
                    val = min + (int)(_val * (max - min));
                    }
                }
            super.cc(out, cc, val, index);
            }
        public void aftertouch(int out, int note, int val, int index)
            {
            Filter filter = (Filter)getMotif();
            Filter.Map func = (Filter.Map)(filter.getFunction(index));
            if (func.getParameterType() == Filter.Map.TYPE_AFTERTOUCH)
                {
                int min = (int)(func.getMin() * 127);
                int max = (int)(func.getMax() * 127);
                if (min > max) { int swap = min; min = max; max = swap; }
                if (val < min) val = min;
                if (val > max) val = max;
                if (max != min)
                    {
                    double _val = (val - min) / (double) (max - min);
                    _val = func.map(_val, getCorrectedValueDouble(func.getVariable(), 1.0));
                    val = min + (int)(_val * (max - min));
                    }
                }
            super.aftertouch(out, note, val, index);
            }
        public void nrpn(int out, int nrpn, int val, int index)
            {
            Filter filter = (Filter)getMotif();
            Filter.Map func = (Filter.Map)(filter.getFunction(index));
            if (func.getParameterType() == Filter.Map.TYPE_NRPN)
                {
                int min = (int)(func.getMin() * 16383);
                int max = (int)(func.getMax() * 16383);
                if (min > max) { int swap = min; min = max; max = swap; }
                if (val < min) val = min;
                if (val > max) val = max;
                if (max != min)
                    {
                    double _val = (val - min) / (double) (max - min);
                    _val = func.map(_val, getCorrectedValueDouble(func.getVariable(), 1.0));
                    val = min + (int)(_val * (max - min));
                    }
                }
            super.nrpn(out, nrpn, val, index);
            }
        public void nrpnCoarse(int out, int nrpn, int msb, int index)
            {
            Filter filter = (Filter)getMotif();
            Filter.Map func = (Filter.Map)(filter.getFunction(index));
            if (func.getParameterType() == Filter.Map.TYPE_NRPN)
                {
                int min = (int)(func.getMin() * 127);
                int max = (int)(func.getMax() * 127);
                if (min > max) { int swap = min; min = max; max = swap; }
                if (msb < min) msb = min;
                if (msb > max) msb = max;
                if (max != min)
                    {
                    double _val = (msb - min) / (double) (max - min);
                    _val = func.map(_val, getCorrectedValueDouble(func.getVariable(), 1.0));
                    msb = min + (int)(_val * (max - min));
                    }
                }
            super.nrpnCoarse(out, nrpn, msb, index);
            }
        public void rpn(int out, int rpn, int val, int index)
            {
            Filter filter = (Filter)getMotif();
            Filter.Map func = (Filter.Map)(filter.getFunction(index));
            if (func.getParameterType() == Filter.Map.TYPE_RPN)
                {
                int min = (int)(func.getMin() * 16383);
                int max = (int)(func.getMax() * 16383);
                if (min > max) { int swap = min; min = max; max = swap; }
                if (val < min) val = min;
                if (val > max) val = max;
                if (max != min)
                    {
                    double _val = (val - min) / (double) (max - min);
                    _val = func.map(_val, getCorrectedValueDouble(func.getVariable(), 1.0));
                    val = min + (int)(_val * (max - min));
                    }
                }
            super.rpn(out, rpn, val, index);
            }
        }



    /// Scale Node
    public class Scale extends Node
        {
        HashMap<Integer, Integer> map = new HashMap<>();                // Maps IDs to revised pitches
        HashMap<Integer, Integer> outs = new HashMap<>();                // Maps IDs to outs
        HashMap<Integer, Integer> mapScheduled = new HashMap<>();       // Maps IDs to revised pitches
        
        public void release(int index)
            {
            // We ought to send a NoteOff to everyone in the map, since we never cleared them.  This SHOULD NOT HAPPEN.
            if (map.size() > 0)
                {
                System.err.println("FilterClip.Scale.release(): non-released notes.");
                for(Integer id : map.keySet())
                    {
                    Integer note = map.get(id);
                    Integer out = outs.get(id);
                    super.noteOff(out.intValue(), note.intValue(), 0x40, id.intValue(), index);             // should we send it this way or bypass and go straight to parent?
                    }
                }
                
            map.clear();
            outs.clear();
            mapScheduled.clear();
            }
        
        public void cut(int index)
            {
            // We ought to send a NoteOff to everyone in the map, since we never cleared them.  This SHOULD NOT HAPPEN.
            if (map.size() > 0)
                {
                System.err.println("FilterClip.Scale.cut(): non-released notes.");
                for(Integer id : map.keySet())
                    {
                    Integer note = map.get(id);
                    Integer out = outs.get(id);
                    super.noteOff(out.intValue(), note.intValue(), 0x40, id.intValue(), index);             // should we send it this way or bypass and go straight to parent?
                    }
                }
                
            map.clear();
            outs.clear();
            mapScheduled.clear();
            }
        
        public void noteOn(int out, int note, double vel, int id, int index)    
            {
            Filter filter = (Filter)getMotif();
            Filter.Scale func = (Filter.Scale)(filter.getFunction(index));
            int rounded = func.getRoundedNote(note);
            map.put(id, rounded);
            outs.put(id, out);
            super.noteOn(out, rounded, vel, id, index);
            }
            
        public void noteOff(int out, int note, double vel, int id, int index)
            {
            Filter filter = (Filter)getMotif();
            Filter.Scale func = (Filter.Scale)(filter.getFunction(index));
            Integer newNote = map.remove(id);
            outs.remove(id);
            if (newNote != null)
                {
                note = newNote.intValue();
                super.noteOff(out, note, vel, id, index);
                }
            else
                {
                Integer scheduledNote = mapScheduled.remove(id);
                if (scheduledNote == null)                      // not scheduled already
                    {
                    super.noteOff(out, note, vel, id, index);              // hope for the best
                    }
                }
            }
            
        public void scheduleNoteOff(int out, int note, double vel, int time, int id, int index)
            {
            Filter filter = (Filter)getMotif();
            Filter.Scale func = (Filter.Scale)(filter.getFunction(index));
            Integer newNote = map.remove(id);
            outs.remove(id);
            if (newNote != null)
                {
                note = newNote.intValue();
                super.scheduleNoteOff(out, note, vel, time, id, index);
                }
            else
                {
                Integer scheduledNote = mapScheduled.remove(id);
                if (scheduledNote == null)                      // not scheduled already
                    {
                    super.scheduleNoteOff(out, note, vel, time, id, index); // hope for the best
                    }
                }
            }
        }


    /// Chord Node
    public class Chord extends Node
        {
        HashMap<Integer, Integer> outs = new HashMap<>();                // Maps IDs to outs
        HashMap<Integer, Integer> map = new HashMap<>();                        // Maps IDs to notes
        HashMap<Integer, Object> chords = new HashMap<>();                // Maps IDs to chords
        
        void noteOffChord(int id, double vel, int index)
            {
            int[] chord = (int[])(chords.remove(id));
            Integer note = map.remove(id);
            Integer out = outs.remove(id);
            if (chord == null) return;
                
            for(int i = 0; i < chord.length; i++)
                {
                if (chord[i] >= 0 && note != null)
                    {
                    if (note + i < 128)
                        {
                        super.noteOff(out, note + i, vel, chord[i], index);
                        }
                    }
                }
            }

        void scheduleNoteOffChord(int id, double vel, int time, int index)
            {
            int[] chord = (int[])(chords.remove(id));
            Integer note = map.remove(id);
            Integer out = outs.remove(id);
            if (chord == null) return;
                
            for(int i = 0; i < chord.length; i++)
                {
                if (chord[i] >= 0 && note != null)
                    {
                    if (note + i < 128)
                        {
                        super.scheduleNoteOff(out, note + i, vel, time, chord[i], index);
                        }
                    }
                }
            }

        void noteOnChord(int id, int out, int note, double vel, int[] chord, int index)
            {
            int[] c = new int[chord.length];
            for(int i = 0; i < chord.length; i++)
                {
                if (chord[i] == 1)
                    {
                    if (note + i < 128)
                        {
                        c[i] = super.noteOn(out, note + i, vel, index); // store id in chord slot
                        }
                    }
                else c[i] = -1;         // no id
                }
                
            chords.put(id, c);
            outs.put(id, out);
            map.put(id, note);
            }
        
        public void release(int index)
            {
            // We ought to send a NoteOff to everyone in the chord map, since we never cleared them.  This SHOULD NOT HAPPEN.
            if (map.size() > 0)
                {
                System.err.println("FilterClip.Chord.release(): non-released notes.");
                for(Integer id : map.keySet())
                    {
                    noteOffChord(id, 0x40, index);
                    }
                }
                
            outs.clear();
            map.clear();
            chords.clear();
            }
        
        public void cut(int index)
            {
            // We ought to send a NoteOff to everyone in the chord map, since we never cleared them.  This SHOULD NOT HAPPEN.
            if (map.size() > 0)
                {
                System.err.println("FilterClip.Chord.release(): non-released notes.");
                for(Integer id : map.keySet())
                    {
                    noteOffChord(id, 0x40, index);
                    }
                }
                
            outs.clear();
            map.clear();
            chords.clear();            
            }
        
        public void noteOn(int out, int note, double vel, int id, int index)    
            {
            Filter filter = (Filter)getMotif();
            Filter.Chord func = (Filter.Chord)(filter.getFunction(index));
            int[] chord = Filter.CHORDS[func.getChord()];
            noteOnChord(id, out, note, vel, chord, index);
            }
            
        public void noteOff(int out, int note, double vel, int id, int index)
            {
            noteOffChord(id, vel, index);
            }
            
        public void scheduleNoteOff(int out, int note, double vel, int time, int id, int index)
            {
            scheduleNoteOffChord(id, vel, time, index);
            }
        }


	/// MIDI INPUT

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

    public int scheduleNoteOn(int out, int note, double vel, int time)
        {
        if (active())
            return nodes.get(0).scheduleNoteOn(out, note, vel, time, 0);
        else 
            return super.scheduleNoteOn(out, note, vel, time);
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
        else if (Filter.MAP.equals(type))
            {
            return new Map();
            }
        else if (Filter.SCALE.equals(type))
            {
            return new Scale();
            }
        else if (Filter.CHORD.equals(type))
            {
            return new Chord();
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
        Filter filter = (Filter)getMotif();
        
        if (clip == null)
            {
            buildClip();
            }
        loadParameterValues(clip, filter.getChildren().get(0));
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
       Filter filter = (Filter)getMotif();

        if (clip == null)
            {
            buildClip();
            }
        loadParameterValues(clip, filter.getChildren().get(0));
        clip.reset();
        childDone = false;

        for(int i = 0; i < Filter.NUM_TRANSFORMERS; i++)
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
        
        // cut the kid first, so we get the remaining notes
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


        // release the kid first, so we get the remaining notes
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
        return filter.isAlways() || 
            (filter.getTo() > filter.getFrom() && (position >= filter.getFrom() && position < filter.getTo())) ||
            (filter.getTo() < filter.getFrom() && (position >= filter.getFrom() || position < filter.getTo()));
        }
    
            
    public boolean process()
        {
        Filter filter = (Filter)getMotif();

        boolean done = true;
        if (clip != null)
            {
            if (!childDone)
                {
                loadParameterValues(clip, filter.getChildren().get(0));
                childDone = clip.advance();
                }
            done = childDone;

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
        
        // Our child may be done but we're not done yet...
        /*
        for(int i = 0; i < Filter.NUM_TRANSFORMERS; i++)
            {
            done = done && nodes.get(i).finished();
            }
        */
          
                
        return done;
        }
    }




