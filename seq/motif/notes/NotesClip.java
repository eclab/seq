/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.notes;

import seq.engine.*;
import java.util.*;
import javax.sound.midi.*;
import java.util.concurrent.locks.*;
import seq.util.*;
import javax.swing.*;
import seq.gui.*;
import seq.motif.notes.gui.*;

public class NotesClip extends Clip
    {
    private static final long serialVersionUID = 1;

    // When notes are recorded, they are stored here when receiving a NOTE ON
    // and then completed and removed when receiving a NOTE OFF
    Notes.Note[] recordedNoteOn = new Notes.Note[128];
    
    // Where in notes.events was my LAST EVENT.  If I have NO EVENTS, then this value is -1  
    int index = -1;  
    // Did we just record notes?
    boolean didRecord;

    /** Returns if we just recorded notes. */
    public boolean getDidRecord() { return didRecord; }
    /** Sets if we just recorded notes. */
    public void setDidRecord(boolean val) { didRecord = val; }
    
    public NotesClip(Seq seq, Motif motif, Clip parent)
        {
        super(seq, motif, parent);
        rebuild();
        }

    /** Updates the version. */
    public void rebuild(Motif motif)
        {
        if (this.getMotif() == motif)
            {
            rebuild();
            }
        }
                
    /** Updates the version. */
    public void rebuild()
        {
        version = getMotif().getVersion();
        }

    /** Returns -1 if we've not yet started. */
    public int getIndex() { return index; }
    
    /** Returns the number of notes and events in the Notes. */
    public int getSize() { return ((Notes) getMotif()).events.size(); }
        
    /** Updates the index to the first note at the given position in time. */
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

    /** Updates the index */
    public void loop()
        {
        super.loop();
        // push position
        Notes notes = (Notes) getMotif();
        setPosition(notes.getStart());
        updateIndex();
        }
                
    /** Updates the index and moves over recorded notes */
    public void reset()  
        {
        super.reset();
//        moveRecording();                // also calls updateIndex() if nonempty
        // push position
        Notes notes = (Notes) getMotif();
        setPosition(notes.getStart());
        updateIndex();
        }

    // Moves over recorded notes, including converting NPN and RPN         
    void moveRecording()
        {
        // Move the recorded notes over 
        Notes notes = (Notes) getMotif();
        ArrayList<Notes.Event> recording = notes.getRecording();
        if (recording.size() > 0)
            {
            if (!recording.isEmpty())
                {
                if (Prefs.getLastBoolean("QuantizeOnRecord", false))
                    {
                    notes.quantize(recording,   
                        Notes.QUANTIZE_DIVISORS[Prefs.getLastInt("QuantizeToOnRecord", 1)],
                        Prefs.getLastBoolean("QuantizeNoteEndsOnRecord", false),
                        Prefs.getLastBoolean("QuantizeNonNotesOnRecord", false),
                        Prefs.getLastDouble("QuantizeBiasOnRecord", 0.5));
                    }
                                
                final SeqUI sequi = seq.getSeqUI();
                int integration = notes.getRecordIntegration();
                boolean[] error = new boolean[1];

                recording = notes.parseEvents(recording, error);  // parse NRPN/RPN maybe
                        
                if (error[0])
                    {
                    SwingUtilities.invokeLater(new Runnable()
                        {
                        public void run()
                            {
                            sequi.showSimpleError("Errors in Converting to NRPN/RPN",
                                "There were errors in converting certain CC messages to NRPN or RPN.\nThese CC messages will be removed.");
                            }
                        });
                    }
                
                // Now integrate
                if (integration == Notes.INTEGRATE_REPLACE)     // replace all events
                    {
                    notes.setEvents(recording);
                    }
                else if (integration == Notes.INTEGRATE_REPLACE_TRIM)   // replace all events, trime to zero
                    {
                    notes.setEvents(recording);
                    notes.trim();
                    }
                else if (integration == Notes.INTEGRATE_MERGE)
                    {
                    notes.merge(recording);
                    }
                else // if (integration == Notes.INTEGRATE_OVERWRITE)   // overwrite same-time events
                    {
                    // Doesn't work right now and I'm not sure how to implement it in a way useful to the user
                    notes.overwrite(recording, true, true);
                    }

                notes.clearRecording();
//                updateIndex();
                setDidRecord(true);
                }
            else 
                {
                setDidRecord(false);
                }
            }
        else
            {
            setDidRecord(false);
            }
        }

    /** Updates the index and moves over recorded notes. */
    public void terminate()  
        {
        super.terminate();
        moveRecording();
//        updateIndex();
        }
        
    /** Cuts all current echoed notes. */
    public void clear()
        {
        Notes notes = (Notes) getMotif();
        int out = notes.getOut();
        for(int pitch = 0; pitch < recordedNoteOn.length; pitch++)
            {
            Notes.Note noteOn = recordedNoteOn[pitch];
            if (noteOn != null)
                {
                // if (notes.getEcho()) noteOff(out, pitch, 0x40, noteOn.release);
                recordedNoteOn[pitch] = null;
                }
            }
        }

    /** Moves all current echoed to the release queue. */
    public void release()
        {
        Notes notes = (Notes) getMotif();
        int out = notes.getOut();
        for(int pitch = 0; pitch < recordedNoteOn.length; pitch++)
            {
            Notes.Note noteOn = recordedNoteOn[pitch];
            if (noteOn != null)
                {
                // if (notes.getEcho()) noteOff(out, pitch, 0x40, noteOn.release);
                recordedNoteOn[pitch] = null;
                }
            }
        }
    
    public void addRecorded(final Notes.Note note)
        {
        if (note == null) return;        // uh.....
        
        SwingUtilities.invokeLater(new Runnable()
            {
            public void run()
                {
                int pitch = 0;
                int when = 0;
                NoteUI noteui = null;
                NotesUI notesui = null;
                Seq seq = getMotif().getSeq();
                MotifUI motifui = seq.getSeqUI().getMotifUI();
                ReentrantLock lock = seq.getLock();
                lock.lock();
                try
                    {
                    pitch = note.pitch;
                    when = note.when;
                    if (motifui.getMotif() == getMotif())           // it's my motifui being displayed
                        {
                        notesui = (NotesUI)motifui;
                        noteui = notesui.addRecordedNoteUI(note);
                        }
                    }
                finally 
                    {
                    lock.unlock();
                    }
                                        
                if (noteui != null) 
                    {
                    if (!notesui.isPositionVisible(when))
                        {
                        notesui.doScrollToPosition(when);
                        }
                    noteui.repaint();               // is this sufficient?
                    }
                }
            });
        }
    
    public boolean process()
        {
        Notes notes = (Notes) getMotif();
        int out = notes.getOut();
        int pos = getPosition();
        
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
                                Notes.Note noteOn = new Notes.Note(pitch, shortmessage.getData2(), pos, 1);             // gotta have something for length
                                recording.add(noteOn);
                                //if (notes.getEcho()) noteOn(out, pitch, noteOn.velocity, NO_NOTE_ID);
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
                                //if (notes.getEcho()) noteOff(out, pitch, release, NO_NOTE_ID);
                                addRecorded(noteOn);
                                }
                            else if (isPitchBend(shortmessage) && notes.getRecordBend())
                                {
                                int lsb = shortmessage.getData1();
                                int msb = shortmessage.getData2();

                                Notes.Bend bend = new Notes.Bend(msb * 128 + lsb - 8192, pos);
                                recording.add(bend);
                                //if (notes.getEcho()) bend(out, bend.value);
                                }
                            else if (isCC(shortmessage) && notes.getRecordCC())
                                {
                                int parameter = shortmessage.getData1();
                                int value = shortmessage.getData2();

                                Notes.CC cc = new Notes.CC(parameter, value, pos);
                                recording.add(cc);
                                //if (notes.getEcho()) cc(out, parameter, value);
                                }
                            else if (isPC(shortmessage) && notes.getRecordPC())
                                {
                                int value = shortmessage.getData1();

                                Notes.PC pc = new Notes.PC(value, pos);
                                recording.add(pc);
                                //if (notes.getEcho()) pc(out, value);
                                }
                            else if (isChannelAftertouch(shortmessage) && notes.getRecordAftertouch())
                                {
                                int value = shortmessage.getData1();

                                Notes.Aftertouch aftertouch = new Notes.Aftertouch(value, pos);
                                recording.add(aftertouch);
                                //if (notes.getEcho()) aftertouch(out, Out.CHANNEL_AFTERTOUCH, value);
                                }
                            else if (isPolyphonicAftertouch(shortmessage) && notes.getRecordAftertouch())
                                {
                                int pitch = shortmessage.getData1();
                                int value = shortmessage.getData2();

                                Notes.Aftertouch aftertouch = new Notes.Aftertouch(pitch, value, pos);
                                recording.add(aftertouch);
                                //if (notes.getEcho()) aftertouch(out, pitch, value);
                                }
                            }
                        }
                    }
                }
            return false;           // we're never done
            }
        else
            {
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
                    int velocity = getCorrectedValueInt(note.velocity, 126);            // 0 .. 126 representing 1...127 because we can't have 0 velocity, that is a note off
                    int release = getCorrectedValueInt(note.release, 127);
                    // at present we're not doing pitch because we'd have to move the note
                    // at present we're also not doing the length for the same reason
                    if (velocity > 0)
                        {
                        int id = noteOn(out, note.pitch, velocity);
                        // NOTE: We have to schedule the note off, rather than turn it off at a later time,
                        // because the musician could change the transpose before the note was turned off
                        // and then the note to turn off would be a different note and we'd be in a whole,
                        // um, HEAP of trouble
                        
                        if (id >= 0) scheduleNoteOff(out, note.pitch, release, note.length, id);
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
                else if (event instanceof Notes.PC)
                    {
                    Notes.PC pc = (Notes.PC)event;
                    pc(out, pc.value);
                    }
                else if (event instanceof Notes.Aftertouch)
                    {
                    Notes.Aftertouch aftertouch = (Notes.Aftertouch)event;
                    aftertouch(out, aftertouch.pitch, aftertouch.value);
                    }
                else if (event instanceof Notes.NRPN)
                    {
                    Notes.NRPN nrpn = (Notes.NRPN)event;
                    nrpn(out, nrpn.parameter, nrpn.value);
                    }
                else if (event instanceof Notes.RPN)
                    {
                    Notes.RPN rpn = (Notes.RPN)event;
                    rpn(out, rpn.parameter, rpn.value);
                    }
                }
                                 
            // at this point, index points to the LAST EVENT that we emitted
            
            // at present we declare we're finished when all our NOTES OFF have completed
            return finishedPlaying();
            }
        }
        
    /** Returns true if we have finished playing. */
    public boolean finishedPlaying() { return getPosition() >= ((Notes) getMotif()).getEndTime(); }

    // TESTING
    public static void main(String[] args) throws Exception
        {
        // Set up Seq
        Seq seq = new Seq();            // starts timer running
        seq.setupForMIDI(Notes.class, args, 1, 1);   // sets up MIDI in and out
        
        seq.setBar(4);
        seq.setMetronome(Seq.METRONOME_RECORDING_ONLY);
        seq.setCountInMode(Seq.COUNT_IN_RECORDING_ONLY);            // count in on recording only
        
        // Set up our module structure
        boolean autoArm = Prefs.getLastBoolean("ArmNewNotesMotifs", false);
        Notes dSeq = new Notes(seq, autoArm);
        seq.setOut(0, new Out(seq, 0));         // Out 0 points to device 0 in the tuple.  This is too complex.
    
        // Build Clip Tree
        seq.setData(dSeq);

        // Wait for a little bit so the user can prepare to enter some notes.
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
