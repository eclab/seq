/* 
   Copyright 2025 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.arpeggio;

import seq.engine.*;
import seq.util.*;
import java.util.*;
import java.util.concurrent.*;

public class ArpeggioClip extends Clip
    {
    private static final long serialVersionUID = 1;

	public static final int TRIES = 4;		// number of times we try to get a unique random number

	public static class Note implements Comparable
		{
		int pitch;
		int velocity;

		public Note(int pitch, int velocity)
			{
			this.pitch = pitch;
			this.velocity = velocity;
			}
		
		public int compareTo(Object obj)
			{
			if (obj == null) return -1;
			if (!(obj instanceof Note)) return -1;
			Note note = (Note)obj;
			return pitch < note.pitch ? -1 : (pitch > note.pitch ? 1 : 0);
			}
		
		public String toString()
			{
			return "ArpeggioClip.Note pitch " + pitch + " vel " + velocity;
			}
		}

	// the chord currently being played, sorted
	ArrayList<Note> currentNotes = new ArrayList<>();
	// the previous pitch or pitches played last step, so we can turn them off this step
	Note[] oldPitches = new Note[0];
	// the arpeggio's pitch state, incremented each step
	int state;
	// A layout of the currentNotes repeated multiple octaves, to make the math easier
    Note[] notes = new Note[0];
    // How long to wait until the next arp step
    int countdown = 0;
    // Our NoteOff Heap, a copy of Seq's heap
    Heap noteOff = new Heap();
	// Our child, if any
	Clip clip;
	// last note in notes played
	int last = -1;

    public ArpeggioClip(Seq seq, Arpeggio arpeggio, Clip parent)
        {
        super(seq, arpeggio, parent);
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
    	Arpeggio arpeggio = (Arpeggio)getMotif();
		ArrayList<Motif.Child> children = arpeggio.getChildren();
		if (children.size() > 0)
			{
			Motif.Child child = children.get(0);
			clip = child.getMotif().buildClip(this);
			}
		removeAll();
		resetArpeggio();
    	}
    
    public void reset()
    	{
    	super.reset();
    	countdown = 0;
    	if (clip == null)
    		{
    		buildClip();
    		}
    	clip.reset();
    	}
    	
    public void loop()
    	{
    	super.loop();
    	countdown = 0;
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
	    	// kill notes just to be on the safe side    	
    		clip.cut();			// Notice we're cutting here, not releasing
    	 
			// Tell my internal heap to send note off messages to me so I can remove
			// them from the arpeggio        	       
			processNoteOffs(true);

			// Clear the arpeggio chord
			removeAll();

			// terminate
    		clip.terminate();
    		}
    	}
    
    void resetArpeggio()
    	{
        Arpeggio arp = (Arpeggio)getMotif();
    	if (currentNotes.size() == 0)
    		{
    		notes = new Note[0];
    		}
    	else
    		{
    		Collections.sort(currentNotes);
			notes = new Note[currentNotes.size() * arp.getOctaves() + 1];
			int pos = 0;
			int oct = 0;
			for(int i = 0; i < notes.length; i++)
				{
				Note current = currentNotes.get(pos++);
				notes[i] = new Note(current.pitch + oct * 12, current.velocity);
				if (pos >= currentNotes.size())
					{
					pos = 0;
					oct++;
					}
				}
			}
    	}

	void addNote(int pitch, int velocity)
		{
		currentNotes.add(new Note(pitch, velocity));
		Collections.sort(currentNotes);
		// O(n^2) but whatever
		for(int i = 0; i < currentNotes.size() - 1; i++)
			{
			if (currentNotes.get(i).pitch == currentNotes.get(i + 1).pitch)
				{
				currentNotes.remove(i + 1);
				}
			}	
		resetArpeggio();
		}
    	
	void removeNote(int pitch)
		{
        Arpeggio arp = (Arpeggio)getMotif();
		for(int i = 0; i < currentNotes.size(); i++)
			{
			Note note = currentNotes.get(i);
			if (note.pitch == pitch)
				{
				currentNotes.remove(note);
				resetArpeggio();
				if (currentNotes.size() == 0 && arp.getNewChordReset())	
					{
			    	state = 0;
					}
				return;
				}
			}
		}

	void removeAll()
		{
		currentNotes.clear();
		resetArpeggio();
		state = 0;
		}
		
    public void cut() 
    	{
    	super.cut();
         Arpeggio arp = (Arpeggio)getMotif();

    	for(int i = 0; i < oldPitches.length; i++)
    		{
    		sendNoteOff(arp.getOut(), oldPitches[i].pitch);
    		}
    	oldPitches = new Note[0];
    	
    	// We don't want to cut the underlying notes because we could be paused
    	// or something else and when we're unpaused we may want to continue
    	// playing.
    	/*
    	if (clip != null)
    		{
    		clip.cut();
    		}
		*/
		
		// Tell my internal heap to send note off messages to me so I can remove
		// them from the arpeggio        	       
        //processNoteOffs(true);
    	}
    

    public void release() 
    	{ 
    	super.release();
        Arpeggio arp = (Arpeggio)getMotif();
    	for(int i = 0; i < oldPitches.length; i++)
    		{
    		sendScheduleNoteOff(arp.getOut(), oldPitches[i].pitch, 0x40, countdown);
    		}
    	oldPitches = new Note[0];

    	// We don't want to cut the underlying notes because we could be paused
    	// or something else and when we're unpaused we may want to continue
    	// playing.

		/*
    	if (clip != null)
    		{
    		clip.cut();			// Notice we're cutting here, not releasing
    		}
    	*/
    	 
		// Tell my internal heap to send note off messages to me so I can remove
		// them from the arpeggio        	       
        //processNoteOffs(true);
    	}
    
    
    
	/// SENT MESSAGES
	/// These are copies of the same messages in Clip, but with "send" in front.
	/// They allow me to override the Clip messages to route MIDI to the 
	/// arpeggio, but have the arpeggio send messages on with these.
    public boolean sendNoteOn(int out, int note, double vel) 
        {
        if (seq.getRoot() == this) return seq.noteOn(out, note, vel);
        // else if (parent == null) // uhh.....
        else return getParent().noteOn(out, note, vel); 
        }
        
    public boolean sendNoteOff(int out, int note) 
        {
        if (seq.getRoot() == this) return seq.noteOff(out, note, 0x40);
        // else if (parent == null) // uhh.....
        else return getParent().noteOff(out, note, 0x40); 
        }
        
    public void sendScheduleNoteOff(int out, int note, double vel, int time) 
        {
        if (seq.getRoot() == this) seq.scheduleNoteOff(out, note, vel, time);
        // else if (parent == null) // uhh.....
        else getParent().scheduleNoteOff(out, note, vel, time); 
        }


	/// INTERCEPTED MESSAGES
	/// These are overridden to intercept notes and send them to the
	/// arpeggio.  Other MIDI messages just get passed on.
    public boolean noteOn(int out, int note, double vel) 
        {
        Arpeggio arp = (Arpeggio)getMotif();
        if (arp.isOmni() || out == arp.getOut()) 
        	{
	        addNote(note, (int)vel);
	        return true;
	        }
	    else
	    	{
	    	return super.noteOn(out, note, vel);
	    	}
        }
        
    public boolean noteOff(int out, int note, double vel) 
        {
        Arpeggio arp = (Arpeggio)getMotif();
        if (arp.isOmni() || out == arp.getOut()) 
        	{
        	removeNote(note);
	        return true;
	        }
	    else
	    	{
	    	return super.noteOff(out, note, vel);
	    	}
        }
        
    public void scheduleNoteOff(int out, int note, double vel, int time) 
        {
        Arpeggio arp = (Arpeggio)getMotif();
        if (arp.isOmni() || out == arp.getOut()) 
        	{
        	noteOff.add(new Note(note, (int)vel), Integer.valueOf(time + seq.getTime()));
	        }
	    else
	    	{
	    	super.scheduleNoteOff(out, note, vel, time);
	    	}
        }        


	// This is a modified copy oßf the same method in Seq, which removes
	// Note-Off messages and processes them if their time has come up.
    void processNoteOffs(boolean all)
        {
        int position = getPosition();
        while(true)
            {
            Integer i = (Integer)(noteOff.getMinKey());
            if (i == null) return;
            if (all || position >= i.intValue())
                {
            	Note note = (Note)(noteOff.extractMin());
            	removeNote(note.pitch);
			    }
            else break;
            }       
        }

    // A simple temporary pitch value for advance(...) to return when it's not returning a chord
    final Note[] basicPitch = new Note[1];
    final Note[] empty = new Note[0];
    
    // Advance the arpeggio and return notes to play.
    Note[] advanceArpeggio(Arpeggio arp)
    	{
	   	if (currentNotes.size() == 0) return empty;
	   	if (arp.getArpeggioType() != Arpeggio.TYPE_PATTERN)
	   		{
	   		if (notes.length < currentNotes.size() * arp.getOctaves() + 1)
	   			{
				resetArpeggio();			// maybe we're bigger now but haven't been recached?
	   			}
			}
	   		
    	switch(arp.getArpeggioType())
    		{
    		case Arpeggio.TYPE_UP:
    			if (state >= notes.length) { resetArpeggio(); state = 0;}
    			basicPitch[0] = notes[state];
    			state++;
    			if (state >= currentNotes.size() * arp.getOctaves())
    				{
    				state = 0;
    				}
    			return basicPitch;
//    		break;
    		case Arpeggio.TYPE_DOWN:
    			if (state >= notes.length) { resetArpeggio(); state = 0; }
    			basicPitch[0] = notes[currentNotes.size() * arp.getOctaves() - state - 1];
    			state++;
    			if (state >= currentNotes.size() * arp.getOctaves())
    				{
    				state = 0;
    				}
    			return basicPitch;
//    		break;
    		case Arpeggio.TYPE_UP_DOWN:
    			if (state < currentNotes.size() * arp.getOctaves())
    				{
    				// ascending
    				if (state >= notes.length) { resetArpeggio(); state = 0; }
    				basicPitch[0] = notes[state];
    				state++;
    				}
    			else
    				{
    				// descending
    				int pos = 2 * currentNotes.size() * arp.getOctaves() - state - 2;
    				if (pos >= notes.length) { resetArpeggio(); state = 0; pos = 2 * currentNotes.size() * arp.getOctaves() - state - 2; }
    				basicPitch[0] = notes[pos];
    				state++;
    				}
    			if (state >= currentNotes.size() * arp.getOctaves() * 2 - 2)
    				{
    				state = 0;
    				}
    			return basicPitch;
//    		break;
    		case Arpeggio.TYPE_UP_DOWN_2:
    			if (state < currentNotes.size() * arp.getOctaves())
    				{
    				// ascending
    				basicPitch[0] = notes[state];
    				state++;
    				}
    			else
    				{
    				// descending
    				int pos = 2 * currentNotes.size() * arp.getOctaves() - state;
    				if (pos >= notes.length) { resetArpeggio(); state = 0; pos = 2 * currentNotes.size() * arp.getOctaves() - state; }
    				basicPitch[0] = notes[pos];
    				state++;
    				}
    			if (state >= currentNotes.size() * arp.getOctaves() * 2)
    				{
    				state = 0;
    				}
    			return basicPitch;
//    		break;
    		case Arpeggio.TYPE_RANDOM:
    			int total = notes.length - 1;
    			if (total == 1)
    				{
    				basicPitch[0] = notes[0];
    				}
    			else if (total == 2)
    				{
    				if (last == 0)
    					{
    					basicPitch[0] = notes[1];
    					}
    				else
    					{
    					basicPitch[1] = notes[0];
    					}
    				}
    			else
    				{
    				int p = 0;
    				for(int i = 0; i < TRIES; i++)
    					{
    					p = seq.getDeterministicRandom().nextInt(notes.length - 1);
    					if (p != last) break;
    					}
    				last = p;
					basicPitch[0] = notes[p];
    				}
    			return basicPitch;
//    		break;
    		case Arpeggio.TYPE_PATTERN:
    			return advancePattern(arp);
//    		break;
    		}
    	// won't happen
    	System.err.println("ArpeggioClip.advanceArpeggio INVALID ARPEGGIO TYPE " + arp.getArpeggioType());
    	return empty;
    	}
    
    // Advance the arpeggio if it's a patterned arpeggio, and return notes to play
    Note[] advancePattern(Arpeggio arp)
    	{
	   	if (state > arp.getPatternLength())		// something bad happened.  Should we do this or just reset?
	   		{
	   		// For now we're resetting...
	   		state = 0;				// state % (currentNotes.size() * arp.getOctaves());
	   		}

    	int count = 0;
    	for(int i = 0; i < arp.PATTERN_NOTES; i++)
    		{
    		if (arp.getPattern(state, i))
    			{
    			count++;
    			}
    		}
    	Note[] pitches = new Note[count];
    	count = 0;
    	for(int i = 0; i < arp.PATTERN_NOTES; i++)
    		{
    		if (arp.getPattern(state, i))
    			{
    			if (i >= Arpeggio.PATTERN_NOTES / 2)
    				{
    				int pos = (i - Arpeggio.PATTERN_NOTES / 2) % currentNotes.size();
    				int octave = (i - Arpeggio.PATTERN_NOTES / 2) / currentNotes.size();
    				Note current = currentNotes.get(pos);
	    			pitches[count] = new Note(current.pitch + octave * 12, current.velocity);
	    			}
	    		else
	    		
	    		
	    			{
	    			// What an ugly equation
	    			int pos = currentNotes.size() - (((Arpeggio.PATTERN_NOTES / 2) - i - 1) % currentNotes.size()) - 1;
	    			int octave = (((Arpeggio.PATTERN_NOTES / 2) - i - 1) / currentNotes.size()) + 1;
	    			Note current = currentNotes.get(pos);
	    			pitches[count] = new Note(current.pitch - octave * 12, current.velocity);
	    			}
    			count++;
    			}
    		}
    	state++;
    	if (state >= arp.getPatternLength())
    		{
    		state = 0;
    		}
    	return pitches;
    	}    
    
    public boolean process()
        {
        Arpeggio arp = (Arpeggio)getMotif();
        /*
        if (clip == null)
        	{
        	buildClip();
        	}
        */
        	
        if (clip != null)
        	{
        	processNoteOffs(false);
        	
        	// we want to advance the child FIRST so that it sends us new notes
        	// before we advance the arpeggiator
        	boolean done = clip.advance();
        	
        	/// FIXME: should release be releasing in *countdown* or in *countdown-1* given how it's computed here?
        	
	       	if (--countdown <= 0)
        		{
        		countdown = arp.getRate();
        		
        		// Time's up!  Advance the arpeggio
        		
        		// First we release the old notes
          		for(int i = 0; i < oldPitches.length; i++)
        			{
					sendNoteOff(arp.getOut(), oldPitches[i].pitch);
        			}
        		
        		// Next we compute the new notes
          		Note[] newPitches = advanceArpeggio(arp);
          		
          		// Next we play the new notes
				for(int i = 0; i < newPitches.length; i++)
					{
					sendNoteOn(arp.getOut(), newPitches[i].pitch, newPitches[i].velocity);
					}
					
				// Finally we set them to the new "old pitches"
				 oldPitches = newPitches;
        		}
        	        	
        	// I am done if my child is done
			return done;
        	}
        else
        	{
        	return true;
        	}
        }
    }




