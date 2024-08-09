package seq.motif.notes;

import seq.engine.*;
import seq.util.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;
import javax.sound.midi.*;
import javax.sound.midi.spi.*;
import java.io.*;
import java.net.*;

public class Notes extends Motif
    {
    private static final long serialVersionUID = 1;

    public static final String MIDI_FILE_EXTENSION = ".mid";

    public abstract static class Event
        {
        public static final int MAX_LENGTH = Seq.PPQ * 256;
        public int when;
        public int length;
        public Event(int when, int length)
            {
            this.when = when;
            this.length = length;
            }
        public Event(JSONObject obj)
            {
            when = obj.optInt("w", 0);
            length = obj.optInt("l", 0);
            }
                        
        public static Event load(JSONObject obj) throws JSONException
            {
            String type = obj.getString("t");
            if (type.equals("n"))
                return new Note(obj);
            else if (type.equals("b"))
                return new Bend(obj);
            else if (type.equals("c"))
                return new CC(obj);
            else if (type.equals("a"))
                return new Aftertouch(obj);
            else 
                {
                System.err.println("Notes.Event.load: unknown type " + type);
                return null;
                }
            }
        public abstract void write(Track track) throws InvalidMidiDataException;
                        
        public JSONObject save() throws JSONException
            {
            JSONObject obj = new JSONObject();
            obj.put("w", when);
            obj.put("l", length);
            return obj;
            }
        
        public abstract Event copy();
        }

    public static class Note extends Event
        {
        public int pitch;
        public int velocity;
        public int release;             // release velocity, default is 0x40 (64)
        public Note(int pitch, int velocity, int when, int length, int release)
            {
            super(when, length);
            this.pitch = pitch;
            this.velocity = velocity;
            this.release = release;
            }
        public Note(int pitch, int velocity, int when, int length)
            {
            super(when, length);
            this.pitch = pitch;
            this.velocity = velocity;
            this.release = 64;
            }
        public Note(JSONObject obj)
            {
            super(obj);
            pitch = obj.optInt("p", 0);
            velocity = obj.optInt("v", 64);
            release = obj.optInt("r", 64);
            }
        public Event copy()
            {
            return new Note(pitch, velocity, when, length, release);
            }
        public void write(Track track) throws InvalidMidiDataException
            {
            track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, pitch, velocity), when));
            if (release == 64)
                track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, pitch, 0), when + length));
            else
                track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, pitch, release), when + length));                       
            }
                        
        public JSONObject save() throws JSONException
            {
            JSONObject obj = super.save();
            obj.put("t", "n");
            obj.put("p", pitch);
            obj.put("v", velocity);
            obj.put("r", release);
            return obj;
            }
        
        static final String[] NOTES = { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B" };
        public String toString() { return NOTES[pitch % 12] + (pitch / 12) + " : " +  velocity; }       // We don't include Length because it would appear in the table
        }
        
    public static class Bend extends Event
        {
        public int value;
        public Bend(int value, int when)
            {
            super(when, 0);
            this.value = value;
            }
        public Bend(JSONObject obj)
            {
            super(obj);
            value = obj.optInt("v", 0);
            }
        public Event copy()
            {
            return new Bend(value, when);
            }
        public void write(Track track) throws InvalidMidiDataException
            {
            int v = value + 8192;
            track.add(new MidiEvent(new ShortMessage(ShortMessage.PITCH_BEND, (v & 127), ((v >>> 7) & 127)), when));
            }
        public JSONObject save() throws JSONException
            {
            JSONObject obj = super.save();
            obj.put("t", "b");
            obj.put("v", value);
            return obj;
            }
        public String toString() { return "Bend[" + value + "]"; }
        }

    public static class CC extends Event
        {
        public int parameter;
        public int value;
        public CC(int parameter, int value, int when)
            {
            super(when, 0);
            this.value = value;
            this.parameter = parameter;
            }
        public CC(JSONObject obj)
            {
            super(obj);
            parameter = obj.optInt("p", 0);
            value = obj.optInt("v", 0);
            }
        public Event copy()
            {
            return new CC(parameter, value, when);
            }
        public void write(Track track) throws InvalidMidiDataException
            {
            track.add(new MidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, parameter, value), when));
            }
        public JSONObject save() throws JSONException
            {
            JSONObject obj = super.save();
            obj.put("t", "b");
            obj.put("p", parameter);
            obj.put("v", value);
            return obj;
            }
        public String toString() { return "CC[" + parameter + "->" + value + "]"; }
        }

    public static class Aftertouch extends Event
        {
        public int pitch;
        public int value;
        public Aftertouch(int pitch, int value, int when)
            {
            super(when, 0);
            this.value = value;
            this.pitch = pitch;
            }
        public Aftertouch(int value, int when)
            {
            super(when, 0);
            this.value = value;
            this.pitch = Out.CHANNEL_AFTERTOUCH;
            }
        public Aftertouch(JSONObject obj)
            {
            super(obj);
            pitch = obj.optInt("p", 0);
            value = obj.optInt("v", 0);
            }
        public Event copy()
            {
            return new Aftertouch(pitch, value, when);
            }
        public void write(Track track) throws InvalidMidiDataException
            {
            if (pitch == Out.CHANNEL_AFTERTOUCH)
                track.add(new MidiEvent(new ShortMessage(ShortMessage.CHANNEL_PRESSURE, value, 0), when));
            else
                track.add(new MidiEvent(new ShortMessage(ShortMessage.POLY_PRESSURE, pitch, value), when));
            }
        public JSONObject save() throws JSONException
            {
            JSONObject obj = super.save();
            obj.put("t", "a");
            obj.put("p", pitch);
            obj.put("v", value);
            return obj;
            }
        public String toString() { return "AT[" + (pitch == Out.CHANNEL_AFTERTOUCH ? "" : ("" + pitch + "->")) + value + "]"; }
        }
        
    ArrayList<Event> events = new ArrayList<>();                // FIXME maybe this should be a list of lists to allow for fragmentation
    ArrayList<ArrayList<Event>> recording = new ArrayList<>();                // FIXME maybe this should be a list of lists to allow for fragmentation
    int out;
    int in;
    int maxNoteOnPosition = 0;
    int maxNoteOffPosition = 0;
    // Do I play out notes as I am recording them?
    boolean echo = true;
	
	
	public static final int NO_MIDI_PARAMETER = 0;
	public static final int BEND = 1;
	public static final int CC_7 = 2;
	public static final int CC_14 = 3;
	public static final int NRPN = 4;
	public static final int RPN = 5;
    int[] midiParameterType = new int[NUM_PARAMETERS];
    int[] midiParameterMSB = new int[NUM_PARAMETERS];
    int[] midiParameterLSB = new int[NUM_PARAMETERS];
    
    public int getMIDIParameterType(int param) { return midiParameterType[param]; }
    public void setMIDIParameterType(int param, int val) { midiParameterType[param] = val; }
    public int getMIDIParameterMSB(int param) { return midiParameterMSB[param]; }
    public void setMIDIParameterMSB(int param, int val) { midiParameterMSB[param] = val; }
    public int getMIDIParameterLSB(int param) { return midiParameterLSB[param]; }
    public void setMIDIParameterLSB(int param, int val) { midiParameterLSB[param] = val; }
    
    public int getOut() { return out; }
    public void setOut(int val) { out = val; Prefs.setLastOutDevice(0, val, "seq.motif.notes.Notes"); }

    public int getIn() { return in; }
    public void setIn(int val) { in = val; Prefs.setLastInDevice(0, val, "seq.motif.notes.Notes"); }
        
    public boolean getEcho() { return echo; }
    public void setEcho(boolean val) { echo = val; }
    
    public Motif copy()
        {
        Notes other = (Notes)(super.copy());
        other.events = new ArrayList();
        for(Event event : events)
            other.events.add(event.copy());
        recording = new ArrayList();
        return other;
        }
        
    public Notes(Seq seq)
        {
        super(seq);

        // Load devices. Note I'm not using setOut(...) etc. which would write the device to prefs
        out = (Prefs.getLastOutDevice(0, "seq.motif.notes.Notes"));
        in = (Prefs.getLastInDevice(0, "seq.motif.notes.Notes"));
        for(int i = 0; i < NUM_PARAMETERS; i++) { midiParameterType[i] = NO_MIDI_PARAMETER; }
        }

    public ArrayList<Event> getEvents() { return events; }
    public void setEvents(ArrayList<Event> val) 
        { 
        events = val; 
        computeMaxTime();         
        recomputeLength();
        recomputeAncestorLengths(); 
        }
        

    public void clearRecordings()
        {
        recording = new ArrayList<ArrayList<Event>>();
        }

    public ArrayList<Event> getLastRecording()
        {
        if (recording.isEmpty()) return null;
        return recording.get(recording.size() - 1);
        }

    public ArrayList<Event> pushNewRecording()
        {
        ArrayList<Event> r = new ArrayList<>();
        recording.add(r);
        return r;
        }

    public void setArmed(boolean val) 
        {
        boolean wasArmed = isArmed();
        boolean isArmed = val;
        super.setArmed(val);
        
        // Handle changes in arming
        if (!wasArmed && isArmed)
            {
            // clear recorded notes
            clearRecordings();
            }
        else if (wasArmed && !isArmed)
            {
            // clear recorded notes
            clearRecordings();
            }
        }
        
    public void computeMaxTime()
        {
        maxNoteOnPosition = 0;
        maxNoteOffPosition = 0;
        for(Event event : events)
            {
            if (event instanceof Note)
                {
                Note note = (Note) event;
                maxNoteOnPosition = Math.max(maxNoteOnPosition, note.when);
                maxNoteOffPosition = Math.max(maxNoteOffPosition, note.when + note.length);
                }
            }
        }
                
    public int getMaxNoteOnPosition() { return maxNoteOnPosition; }
    public int getMaxNoteOffPosition() { return maxNoteOffPosition; }
        
    public void sortEvents(ArrayList<Event> events)
        {
        Collections.sort(events, new Comparator<Event>()
                {
                public int compare(Event o1, Event o2) { return o1.when - o2.when; }
                public boolean equals(Comparator<Event> other) { return (this == other); }
            });
        }
    
    /** Removes elements by index, not by time */
    public ArrayList<Event> remove(int startIndex, int endIndex)        // endIndex is inclusive
        {
        ArrayList<Event> newEvents = new ArrayList<Event>();
        ArrayList<Event> cut = new ArrayList<Event>();
        for(int i = 0; i < startIndex; i++)
            {
            Event event = events.get(i);
            newEvents.add(event);
            }
        for(int i = startIndex; i <= endIndex; i++)
            {
            Event event = events.get(i);
            cut.add(event);
            }
        for(int i = endIndex + 1; i < events.size(); i++)
            {
            Event event = events.get(i);
            newEvents.add(event);
            }
        events = newEvents;
        computeMaxTime();
        recomputeLength();
        recomputeAncestorLengths();
        return cut;
        }
    
    public ArrayList<Event> cut(int start, int end, boolean allowStartOverlaps, boolean allowEndOverlaps)
        {
        ArrayList<Event> newEvents = new ArrayList<Event>();
        ArrayList<Event> cut = new ArrayList<Event>();

        // remove overlapping events
        for(int i = 0; i < events.size(); i++)
            {
            Event event = events.get(i);
            // If fully outside...
            if (event.when + event.length < start || event.when >= end)
                {
                newEvents.add(event);
                }
            // if started early but we're allowing start overlaps
            else if (event.when < start && event.when + event.length >= start && allowStartOverlaps)
                {
                newEvents.add(event);
                }
            else if (event.when < end && event.when + event.length >= end && allowEndOverlaps)
                {
                newEvents.add(event);
                }
            else
                {
                cut.add(event);
                }
            }
        events = newEvents;
        computeMaxTime(); 
        recomputeLength();
        recomputeAncestorLengths(); 
        return cut;
        }

    public ArrayList<Event> copy(int start, int end, boolean allowStartOverlaps, boolean allowEndOverlaps)
        {
        ArrayList<Event> copy = new ArrayList<Event>();

        // remove overlapping events
        for(int i = 0; i < events.size(); i++)
            {
            Event event = events.get(i);
            // If fully outside...
            if (event.when + event.length < start || event.when >= end)
                {
                // do nothing
                }
            // if started early but we're allowing start overlaps
            else if (event.when < start && event.when + event.length >= start && allowStartOverlaps)
                {
                // do nothing
                }
            else if (event.when < end && event.when + event.length >= end && allowEndOverlaps)
                {
                // do nothing
                }
            else
                {
                copy.add(event);
                }
            }
        computeMaxTime(); 
        recomputeLength();
        recomputeAncestorLengths(); 
        return copy;
        }

    public void merge(ArrayList<Event> from)
        {
        if (from.isEmpty()) return;
        ArrayList<Event> newEvents = new ArrayList<Event>();
        int plen = events.size();
        int rlen = from.size();
        int pi = 0;
        int ri = 0;
        while(pi < plen && ri < rlen)
            {
            if (pi == plen)
                {
                Event r = from.get(ri);
                newEvents.add(r);
                ri++;
                }
            else if (ri == rlen)
                {
                Event p = events.get(pi);
                newEvents.add(p);
                pi++;
                }
            else
                {
                Event p = events.get(pi);
                Event r = from.get(ri);
                if (p.when < r.when)
                    {
                    newEvents.add(p);
                    pi++;
                    }
                else
                    {
                    newEvents.add(r);
                    ri++;
                    }
                }
            }
        computeMaxTime(); 
        recomputeLength();
        recomputeAncestorLengths(); 
        events = newEvents;
        }

    public void overwrite(ArrayList<Event> from, boolean allowStartOverlaps, boolean allowEndOverlaps)
        {
        if (from.isEmpty()) return;
        int start = Integer.MAX_VALUE;
        int end = 0;
        for(Event event : from)
            {
            if (start > event.when) start = event.when;
            if (end < event.when + event.length) end = event.when + event.length;
            }
        cut(start, end, allowStartOverlaps, allowEndOverlaps);
        merge(from);
        }


    public void trim()
        {
        if (events.size() > 0) 
            {
            shift(0 - events.get(0).when);
            }
        }

    public boolean shift(int by)
        {
        if (events.size() == 0) return true;
        else return shift(0, events.size() - 1, by);
        }

    public boolean shift(int from, int to, int by)
        {
        if (events.isEmpty()) return true;
        if (by == 0) return true;
        
        if (from > to) { int temp = to; to = from; from = temp; }
        
        if (!events.isEmpty() && events.get(from).when < (0 - by)) // uh oh, can't do it backwards by far enough
            {
            return false;           // cannot shift
            }

        // I don't think should be able to change the order, so we're probably still okay?
        for(int i = from; i <= to; i++)
            {
            Event event = events.get(i);
                
            event.when += by;
            }
        
        sortEvents(events);
        computeMaxTime(); 
        recomputeLength();
        recomputeAncestorLengths(); 
        return true;
        }

    public void stretch(int stretchFrom, int stretchTo)
        {
        if (events.size() == 0) return;
        else stretch(0, events.size() - 1, stretchFrom, stretchTo);
        }

    public void stretch(int from, int to, int stretchFrom, int stretchTo)
        {
        if (stretchFrom == stretchTo) return;
        if (events.isEmpty()) return;
        
        if (from > to) { int temp = to; to = from; from = temp; }
        
        for(int i = from; i <= to; i++)
            {
            Event event = events.get(i);
                
            event.when = (int)Math.round((event.when * stretchTo) / (double)stretchFrom);
            if (event instanceof Note)
                {
                Note note = (Note)event;
                note.length = (int)Math.round((event.length * stretchTo) / (double)stretchFrom);
                }
            }
        
        sortEvents(events);
        computeMaxTime(); 
        recomputeLength();
        recomputeAncestorLengths(); 
        }

    public void transpose(int by)
        {
        if (events.size() == 0) return;
        else transpose(0, events.size() - 1, by);
        }

    public void transpose(int from, int to, int by)
        {
        System.err.println(by);
        if (from > to) { int temp = to; to = from; from = temp; }
 
        if (by == 0) return;
        
        // I don't think should be able to change the order, so we're probably still okay?
        for(int i = from; i <= to; i++)
            {
            Event event = events.get(i);
                
            if (event instanceof Note)
                {
                Note note = (Note) event;
                note.pitch += by;
                if (note.pitch > 127) // uh oh
                    {
                    note.pitch = 127;
                    note.velocity = 0;
                    }
                if (note.pitch < 0) // uh oh
                    {
                    note.pitch = 0;
                    note.velocity = 0;
                    }
                }
            }
        }
                        
    public void recomputeLength() 
        { 
        if (events.isEmpty()) return;
        
        int start = Integer.MAX_VALUE;
        int end = 0;
        for(Event event : events)
            {
            if (start > event.when) start = event.when;
            if (end < event.when + event.length) end = event.when + event.length;
            }
        length = end - start;
        }

    public Clip buildClip(Clip parent)
        {
        return new NotesClip(seq, this, parent);
        }

    public void read(File midiFile) { try { read(MidiSystem.getSequence(midiFile)); } catch (InvalidMidiDataException ex) { }  catch (IOException ex) { }}
    public void read(InputStream stream) { try { read(MidiSystem.getSequence(stream));  } catch (InvalidMidiDataException ex) { }  catch (IOException ex) { }}
    public void read(URL url) { try { read(MidiSystem.getSequence(url));  } catch (InvalidMidiDataException ex) { } catch (IOException ex) { }}
    public void read(javax.sound.midi.Sequence sequence)
        {
        Track[] tracks = sequence.getTracks();
        if (tracks.length == 0) return;
        if (sequence.getDivisionType() != Sequence.PPQ) return;         // uh oh
        int resolution = sequence.getResolution();
                
        Notes.Note[] recordedNoteOn = new Notes.Note[128];
        ArrayList<Notes.Event> recording = new ArrayList<>();  

        // FIXME: At present we only load a single track
        for(int i = 0; i < tracks[0].size(); i++)
            {
            MidiEvent e = tracks[0].get(i);
            int pos = (int)((e.getTick() * Seq.PPQ) / resolution);                  // e.getTick() is a long
            MidiMessage message = e.getMessage();
            if (message instanceof ShortMessage)
                {
                ShortMessage shortmessage = (ShortMessage)message;

                if (Clip.isNoteOn(shortmessage))
                    {
                    int pitch = shortmessage.getData1();
                    Notes.Note noteOn = new Notes.Note(pitch,
                        shortmessage.getData2(),
                        pos, 1);             // gotta have something for length
                    recording.add(noteOn);
                    recordedNoteOn[pitch] = noteOn;
                    }
                else if (Clip.isNoteOff(shortmessage))
                    {
                    int pitch = shortmessage.getData1();
                    Notes.Note noteOn = recordedNoteOn[pitch];
                    int release = 64;
                    if (noteOn != null)
                        {
                        noteOn.length = pos - noteOn.when;
                        noteOn.release = shortmessage.getData2();
                        release = noteOn.release;
                        recordedNoteOn[pitch] = null;
                        }
                    }
                else if (Clip.isPitchBend(shortmessage))
                    {
                    int lsb = shortmessage.getData1();
                    int msb = shortmessage.getData2();

                    Notes.Bend bend = new Notes.Bend(msb * 128 + lsb - 8192, pos);
                    recording.add(bend);
                    }
                else if (Clip.isCC(shortmessage))
                    {
                    int parameter = shortmessage.getData1();
                    int value = shortmessage.getData2();

                    Notes.CC cc = new Notes.CC(parameter, value, pos);
                    recording.add(cc);
                    }
                else if (Clip.isChannelAftertouch(shortmessage))
                    {
                    int value = shortmessage.getData1();

                    Notes.Aftertouch aftertouch = new Notes.Aftertouch(value, pos);
                    recording.add(aftertouch);
                    }
                else if (Clip.isPolyphonicAftertouch(shortmessage))
                    {
                    int pitch = shortmessage.getData1();
                    int value = shortmessage.getData2();

                    Notes.Aftertouch aftertouch = new Notes.Aftertouch(pitch, value, pos);
                    recording.add(aftertouch);
                    }
                }
            }
        events = recording;
        computeMaxTime();
        } 
        
    /** Quantize all events to the nearest DIVISOR NOTE in ticks.  For each note we compute the DIVISOR NOTE
        that is LESS THAN OR EQUAL to the note.  Then if the note is less than DIVISOR * PERCENTAGE later
        than the DIVISOR NOTE, we push it to the divisor note, else we push it to the next divisor note.
        Optionally also quantize the ends, and non-notes.  Percentage can be any value 0.0 to 1.0 inclusive.
    */
    public void quantize(int divisor, boolean quantizeEnds, boolean quantizeNonNotes, double percentage)
        {
        if (events.size() == 0) return;
        else quantize(0, events.size() - 1, divisor, quantizeEnds, quantizeNonNotes, percentage);
        }

    /** Quantize all events in the given from...to range (inclusive)
        to the nearest DIVISOR NOTE in ticks.  For each note we compute the DIVISOR NOTE
        that is LESS THAN OR EQUAL to the note.  Then if the note is less than DIVISOR * PERCENTAGE later
        than the DIVISOR NOTE, we push it to the divisor note, else we push it to the next divisor note.
        Optionally also quantize the ends, and non-notes.  Percentage can be any value 0.0 to 1.0 inclusive.
    */
    public void quantize(int from, int to, int divisor, boolean quantizeEnds, boolean quantizeNonNotes, double percentage)
        {
        if (from > to) { int temp = to; to = from; from = temp; }
        
        // I don't think should be able to change the order, so we're probably still okay?
        for(int i = from; i <= to; i++)
            {
            Event event = events.get(i);
                
            // Zero, stash the note off position for later use
            int noteOffPos = 0;
            if (event instanceof Note && quantizeEnds)
                {
                noteOffPos = event.when + event.length;
                }
                        
            // First handle onsets
            if (event instanceof Note || quantizeNonNotes)
                {
                int quantizedTime = (event.when / divisor) * divisor;
                double pivot = (event.when - quantizedTime) / (double)divisor;
                        
                if (pivot < percentage) event.when = quantizedTime; // pull down
                else event.when = quantizedTime + divisor;                // push up
                }
                
            // Next handle NOTE OFF
            if (event instanceof Note && quantizeEnds)
                {
                int quantizedTime = (noteOffPos / divisor) * divisor;
                double pivot = (noteOffPos - quantizedTime) / (double)divisor;
                        
                if (pivot < percentage) noteOffPos = quantizedTime; // pull down
                else noteOffPos = quantizedTime + divisor;                // push up
                        
                // change back to length
                event.length = noteOffPos - event.when;
                if (event.length < 0)
                    {
                    System.err.println("QUANTIZE ERROR: event length went negative: " + event.length); 
                    event.length = 0;
                    }
                }
            }
        sortEvents(events);
        computeMaxTime(); 
        recomputeLength();
        recomputeAncestorLengths(); 
        }

    int getRandomTimeNoise(Random random, double max)
        {
//              return (int)Math.round(random.nextGaussian() * stdev);
        return (int)(random.nextDouble() * max * Seq.PPQ);              // Go up to 1 beat max
        }
                
    /** Quantize the onset, and possibly release time, of all events, possibly including non-notes, by the given
        variance (in ticks).
    */
    public void randomizeTime(double stdev, boolean randomizeLengths, boolean randomizeNonNotes, Random random)
        {
        if (events.size() == 0) return;
        else randomizeTime(0, events.size() - 1, stdev, randomizeLengths, randomizeNonNotes, random);
        }

    /** Quantize the onset, and possibly release time, of all events, possibly including non-notes, by the given
        variance (in ticks), in the event range from FROM to TO. */
    public void randomizeTime(int from, int to, double max, boolean randomizeLengths, boolean randomizeNonNotes, Random random)
        {
        if (from > to) { int temp = to; to = from; from = temp; }

        for(int i = from; i <= to; i++)
            {
            Event event = events.get(i);
                
            // First handle onsets
            if (event instanceof Note || randomizeNonNotes)
                {
                int val = event.when + getRandomTimeNoise(random, max);
                while(val < 0)
                    {
                    val = event.when + getRandomTimeNoise(random, max);
                    }
                event.when = val;
                }
                
            // Next handle LENGTHS
            if (event instanceof Note && randomizeLengths)
                {
                int val = event.length + getRandomTimeNoise(random, max);
                while(val < 0)
                    {
                    val = event.length + getRandomTimeNoise(random, max);
                    }
                event.length = val;
                }
            }
        sortEvents(events);     
        computeMaxTime(); 
        recomputeLength();
        recomputeAncestorLengths(); 
        }

    public void setVelocity(int val)
        {
        if (events.size() == 0) return;
        else setVelocity(0, events.size() - 1, val);
        }

    public void setVelocity(int from, int to, int val)
        {
        if (from > to) { int temp = to; to = from; from = temp; }
        
        for(int i = from; i <= to; i++)
            {
            Event event = events.get(i);
                
            // First handle onsets (which CANNOT have 0 velocity)
            if (event instanceof Note)
                {
                Note note = (Note)event;
                note.velocity = val;
                }
            }
        }

    int getRandomVelocityNoise(Random random, double max)
        {
        return (int)((random.nextDouble() * 127 - 63) * max);           // So we have enough space to fit in above 1...
        }
                
    /** Quantize the velocity, and possibly release velocity, of all events, possibly including non-notes, by the given
        variance (from 0..255). */
    public void randomizeVelocity(double max, boolean randomizeReleases, Random random)
        {
        if (events.size() == 0) return;
        else randomizeVelocity(0, events.size() - 1, max, randomizeReleases, random);
        }

    static final int NUM_TRIES = 8;
        
    /** Quantize the velocity, and possibly release velocity, of all events, possibly including non-notes, by the given
        variance (from 0..255), in the event range from FROM to TO. */
    public void randomizeVelocity(int from, int to, double max, boolean randomizeReleases, Random random)
        {
        if (from > to) { int temp = to; to = from; from = temp; }
        
        for(int i = from; i <= to; i++)
            {
            Event event = events.get(i);
                
            // First handle onsets (which CANNOT have 0 velocity)
            if (event instanceof Note)
                {
                Note note = (Note)event;
                int val = 0;
                boolean success = false;
                for(int tries = 0; tries < NUM_TRIES; tries++)
                    {
                    int rnd = getRandomVelocityNoise(random, max);
                    val = note.velocity + rnd;
                    if (val >= 1 && val <= 127) { success = true; break; }
                                        
                    val = note.velocity - rnd;      // let's try flipping
                    if (val >= 1 && val <= 127) { success = true; break; }
                    }
                if (success) note.velocity = val;
                else {System.err.println("Panic"); note.velocity = random.nextInt(127) + 1;     }       // I don't think this can happen?

                // Next handle releases (which can have 0 velocity)
                if (randomizeReleases)
                    {
                    val = 0;
                    success = false;
                    for(int tries = 0; tries < NUM_TRIES; tries++)
                        {
                        int rnd = getRandomVelocityNoise(random, max);
                        val = note.release + rnd;
                        if (val >= 0 && val <= 127) { success = true; break; }
                                                
                        val = note.release - rnd;       // let's try flipping
                        if (val >= 0 && val <= 127) { success = true; break; }
                        }
                    if (success) note.release = val;
                    else {System.err.println("Panic"); note.release = random.nextInt(128);  }       // I don't think this can happen?
                    }
                }
            }
        }


                
    Sequence write() throws InvalidMidiDataException
        {
        javax.sound.midi.Sequence sequence = new javax.sound.midi.Sequence(javax.sound.midi.Sequence.PPQ, Seq.PPQ);
        Track track = sequence.createTrack();
        for(Event event : events)
            {
            event.write(track);
            }
        return sequence;
        }

    public void write(File out) throws IOException, InvalidMidiDataException
        {
        MidiSystem.write(write(), 0, out);
        }

    public void write(OutputStream out) throws IOException, InvalidMidiDataException
        {
        MidiSystem.write(write(), 0, out);
        }

    public void load(JSONObject obj) throws JSONException
        {
        setEcho(obj.optBoolean("echo", false));
        setOut(obj.optInt("out", 0));
        setIn(obj.optInt("in", 0));
        JSONArray typeArray = obj.optJSONArray("mtype");
        if (typeArray != null)
        	{
			for(int i = 0; i < NUM_PARAMETERS; i++)
				{
				midiParameterType[i] = typeArray.optInt(i, 0);
				}
			JSONArray msbArray = obj.getJSONArray("msb");
			for(int i = 0; i < NUM_PARAMETERS; i++)
				{
				midiParameterMSB[i] = msbArray.optInt(i, 0);
				}
			JSONArray lsbArray = obj.getJSONArray("lsb");
			for(int i = 0; i < NUM_PARAMETERS; i++)
				{
				midiParameterLSB[i] = lsbArray.optInt(i, 0);
        		}
        	}
        JSONArray eventsArray = obj.getJSONArray("events");
        events = new ArrayList<Event>();
        for(int i = 0; i < eventsArray.length(); i++)
            {
            events.add(Event.load(eventsArray.getJSONObject(i)));
            }
        computeMaxTime();               // computs maxNoteOnPosition and maxNoteOffPosition
        }
        
    public void save(JSONObject obj) throws JSONException
        {
        obj.put("echo", getEcho());
        obj.put("out", getOut());
        obj.put("in", getIn());
        JSONArray typeArray = new JSONArray();
        for(int i = 0; i < NUM_PARAMETERS; i++)
        	{
        	typeArray.put(i, midiParameterType[i]);
        	}
        obj.put("mtype", typeArray);
        JSONArray msbArray = new JSONArray();
        for(int i = 0; i < NUM_PARAMETERS; i++)
        	{
        	msbArray.put(i, midiParameterMSB[i]);
        	}
        obj.put("msb", msbArray);
        JSONArray lsbArray = new JSONArray();
        for(int i = 0; i < NUM_PARAMETERS; i++)
        	{
        	lsbArray.put(i, midiParameterLSB[i]);
        	}
        obj.put("lsb", lsbArray);
        JSONArray eventsArray = new JSONArray();
        for(Event event : events)
            {
            eventsArray.put(event.save());
            }
        obj.put("events", eventsArray);
        }

    static int counter = 1;
    public int getNextCounter() { return counter++; }
        
    public String getBaseName() { return "Notes"; }
    }
