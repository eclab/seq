/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.notes;

import seq.engine.*;
import java.util.*;
import javax.sound.midi.*;
import java.util.concurrent.*;

public class NotesClip extends Clip
    {
    private static final long serialVersionUID = 1;

    Notes.Note[] recordedNoteOn = new Notes.Note[128];
    
    // Where in notes.events was my LAST EVENT.  If I have NO EVENTS, then this value is -1  
    int index = -1;  
    boolean didRecord;

    public static final int NO_MIDI_VALUE = -1;
    int[] lastMIDIValue = new int[Notes.NUM_PARAMETERS];
    public int getLastMIDIValue(int param) { return lastMIDIValue[param]; }
    public void setLastMIDIValue(int param, int val) { lastMIDIValue[param] = val; }
    public void resetMIDIValues() { for(int i = 0; i < Notes.NUM_PARAMETERS; i++) { lastMIDIValue[i] = NO_MIDI_VALUE; } }
    
    public boolean getDidRecord() { return didRecord; }
    public void setDidRecord(boolean val) { didRecord = val; }
    
    public NotesClip(Seq seq, Motif motif, Clip parent)
        {
        super(seq, motif, parent);
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
        version = getMotif().getVersion();
        }

    /** Returns -1 if we've not yet started. */
    public int getIndex() { return index; }
    
    public int getSize() { return ((Notes) getMotif()).events.size(); }
        
    void updateIndex()
        {
        Notes notes = (Notes) getMotif();

        index = -1;
        int pos = getPosition();
        for(int i = 0; i < notes.events.size(); i++)
            {
            if (notes.events.get(i).when < pos)
                {
                index = i;
                }
            else break;
            }
        }

    public void loop()
        {
        super.loop();
        updateIndex();
        }
                
    public void reset()  
        {
        super.reset();
        moveRecording();                // also calls updateIndex() if nonempty
        updateIndex();
        }
        
    void moveRecording()
        {
        // Move the recorded notes over 
        Notes notes = (Notes) getMotif();
        ArrayList<Notes.Event> recording = notes.getRecording();
        if (!recording.isEmpty())
            {
            notes.setEvents(recording);
            notes.clearRecording();
            updateIndex();
            setDidRecord(true);
            }
        else setDidRecord(false);
        }

    public void terminate()  
        {
        super.terminate();
        moveRecording();
        updateIndex();
        }
        
    public void clear()
        {
        Notes notes = (Notes) getMotif();
        int out = notes.getOut();
        for(int pitch = 0; pitch < recordedNoteOn.length; pitch++)
            {
            Notes.Note noteOn = recordedNoteOn[pitch];
            if (noteOn != null)
                {
                if (notes.getEcho()) noteOff(out, pitch, noteOn.release);
                recordedNoteOn[pitch] = null;
                }
            }
        }

    public void release()
        {
        Notes notes = (Notes) getMotif();
        int out = notes.getOut();
        for(int pitch = 0; pitch < recordedNoteOn.length; pitch++)
            {
            Notes.Note noteOn = recordedNoteOn[pitch];
            if (noteOn != null)
                {
                if (notes.getEcho()) noteOff(out, pitch, noteOn.release);
                recordedNoteOn[pitch] = null;
                }
            }
        }
        
    public void outputMIDIValues(Notes notes)
        {
        for(int i = 0; i < Motif.NUM_PARAMETERS; i++)
            {
            int type = notes.getMIDIParameterType(i);
                        
            if (type != Notes.NO_MIDI_PARAMETER)
                {
                int msb = notes.getMIDIParameterMSB(i);
                int lsb = notes.getMIDIParameterLSB(i);
                                
                double paramVal = getParameterValue(i);
                int last = getLastMIDIValue(i);
                                
                if (type == Notes.BEND) // it's PITCH BEND
                    {
                    int val = (int)(paramVal * 16383 - 8192);
                    if (val != last)
                        {
                        bend(notes.getOut(), val);
                        setLastMIDIValue(i, val);
                        }
                    }
                else if (type == Notes.CC_7)    // it's CC_7
                    {
                    int val = (int)(paramVal * 127);
                    if (val != last)
                        {
                        cc(notes.getOut(), msb, val);
                        setLastMIDIValue(i, val);
                        }
                    }
                else if (type == Notes.CC_14) // it's CC_14
                    {
                    int val = (int)(paramVal * 16383);
                    if (val != last)
                        {
                        cc(notes.getOut(), msb, val >>> 7);
                        cc(notes.getOut(), msb + 32, val & 127);
                        setLastMIDIValue(i, val);
                        }
                    }
                else if (type == Notes.NRPN) // it's NRPN
                    {
                    int val = (int)(paramVal * 16383);
                    if (val != last)
                        {
                        nrpn(notes.getOut(), msb * 128 + lsb, val);
                        setLastMIDIValue(i, val);
                        }
                    }
                else                    // it's RPN
                    {
                    int val = (int)(paramVal * 16383);
                    if (val != last)
                        {
                        rpn(notes.getOut(), msb * 128 + lsb, val);
                        setLastMIDIValue(i, val);
                        }
                    }
                }
            }
        }
                
    public boolean process()
        {
        Notes notes = (Notes) getMotif();
        int out = notes.getOut();
        int pos = getPosition();
        
        if (getPosition() == 0) resetMIDIValues();
        outputMIDIValues(notes);

        
        if (notes.isArmed() && seq.isRecording())
            {
            if (getPosition() == 0) // this is the first step, we have to clear out existing notes first
                {
                clear();        // just in case?
                ArrayList<Notes.Event> r = notes.getRecording();
                if (r != null) r.clear();       // just in case?
                }
                
            In in = seq.getIn(notes.getIn());
            if (in == null) // uh oh
                {
                System.err.println("NotesClip.process() WARNING: no seq.getIn(" + notes.getIn() + ")");
                }
            else
                {
                ArrayList<Notes.Event> recording = notes.getRecording();
                int channel = in.getChannel();
                MidiMessage[] messages = in.getMessages();
                for(int i = 0; i < messages.length; i++)
                    {
                    MidiMessage message = messages[i];
                    if (message instanceof ShortMessage)
                        {
                        ShortMessage shortmessage = (ShortMessage)message;
                        if (channel == Midi.OMNI || channel == shortmessage.getChannel() + 1)
                            {
                            if (isNoteOn(shortmessage))
                                {
                                int pitch = shortmessage.getData1();
                                Notes.Note noteOn = new Notes.Note(pitch,
                                    shortmessage.getData2(),
                                    pos, 1);             // gotta have something for length
                                recording.add(noteOn);
                                if (notes.getEcho()) noteOn(out, pitch, noteOn.velocity);
                                recordedNoteOn[pitch] = noteOn;
                                }
                            else if (isNoteOff(shortmessage))
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
                                if (notes.getEcho()) noteOff(out, pitch, release);
                                }
                            else if (isPitchBend(shortmessage))
                                {
                                int lsb = shortmessage.getData1();
                                int msb = shortmessage.getData2();

                                Notes.Bend bend = new Notes.Bend(msb * 128 + lsb - 8192, pos);
                                recording.add(bend);
                                if (notes.getEcho()) bend(out, bend.value);
                                }
                            else if (isCC(shortmessage))
                                {
                                int parameter = shortmessage.getData1();
                                int value = shortmessage.getData2();

                                Notes.CC cc = new Notes.CC(parameter, value, pos);
                                recording.add(cc);
                                if (notes.getEcho()) cc(out, parameter, value);
                                }
                            else if (isChannelAftertouch(shortmessage))
                                {
                                int value = shortmessage.getData1();

                                Notes.Aftertouch aftertouch = new Notes.Aftertouch(value, pos);
                                recording.add(aftertouch);
                                if (notes.getEcho()) aftertouch(out, Out.CHANNEL_AFTERTOUCH, value);
                                }
                            else if (isPolyphonicAftertouch(shortmessage))
                                {
                                int pitch = shortmessage.getData1();
                                int value = shortmessage.getData2();

                                Notes.Aftertouch aftertouch = new Notes.Aftertouch(pitch, value, pos);
                                recording.add(aftertouch);
                                if (notes.getEcho()) aftertouch(out, pitch, value);
                                }
                            }
                        }
                    }
                }
            return false;           // we're never done
            }
        else
            {
            System.err.println("Process " + pos);
            ArrayList<Notes.Event> playing = notes.events;
            int size = playing.size();
            
            // At present, index is where I LAST had an event, or -1            
            while (size > (index + 1) && playing.get(index + 1).when <= pos)            // is there another event, and is it time for it?
                {
                index++;                                                                                                                        // advance to that event
                
                // Now emit it
                Notes.Event event = playing.get(index);
                if (event instanceof Notes.Note)
                    {
                    Notes.Note note = (Notes.Note)event;
                    if (note.velocity > 0)
                        {
                        System.err.println("NOTE ON " + out + " " + note.pitch + " " + note.velocity);
                        noteOn(out, note.pitch, note.velocity);
                        // NOTE: We have to schedule the note off, rather than turn it off at a later time,
                        // because the musician could change the transpose before the note was turned off
                        // and then the note to turn off would be a different note and we'd be in a whole,
                        // um, HEAP of trouble
                                   
                        scheduleNoteOff(out, note.pitch, note.release, note.length);
                        }
                    }
                else if (event instanceof Notes.Bend)
                    {
                    Notes.Bend bend = (Notes.Bend)event;
                    bend(out, bend.value);
                    }
                else if (event instanceof Notes.CC)
                    {
                    Notes.CC cc = (Notes.CC)event;
                    cc(out, cc.parameter, cc.value);
                    }
                else if (event instanceof Notes.Aftertouch)
                    {
                    Notes.Aftertouch aftertouch = (Notes.Aftertouch)event;
                    aftertouch(out, aftertouch.pitch, aftertouch.value);
                    }
                }
                                
            // at this point, index points to the LAST EVENT that we emitted
            
            // at present we declare we're finished when all our NOTES OFF have completed
            return finishedPlaying();
            }
        }
        
    public boolean finishedPlaying() { return getPosition() >= ((Notes) getMotif()).getMaxNoteOffPosition(); }

    // TESTING
    public static void main(String[] args) throws Exception
        {
        // Set up Seq
        Seq seq = new Seq();            // starts timer running
        seq.setupForMIDI(Notes.class, args, 1, 1);   // sets up MIDI in and out
        
        seq.setBar(4);
        seq.setMetronome(true);
        seq.setCountInMode(Seq.COUNT_IN_RECORDING_ONLY);            // count in on recording only
        
        // Set up our module structure
        Notes dSeq = new Notes(seq);
//        seq.setIn(0, new In(seq, 0));         // Out 0 points to device 0 in the tuple.  This is too complex.
        seq.setOut(0, new Out(seq, 0));         // Out 0 points to device 0 in the tuple.  This is too complex.
    
        // Build Clip Tree
        seq.setData(dSeq);

        try
            {
            Thread.currentThread().sleep(1000);
            }
        catch (InterruptedException ex) { }

        // Arm the Clip and start Recording
        seq.getData().setArmed(true);
        seq.setRecording(true);
        seq.reset();
        seq.play();
        
        // Wait 1 measure
        seq.waitUntilTime(Seq.PPQ * 4);
                        
        // Reset the clip and Play!
        seq.stop();
        seq.getData().setArmed(false);
        seq.setLooping(true);
        seq.play();

        seq.waitUntilStopped();         // we're looping so this will never exit
        }
                        
    }
