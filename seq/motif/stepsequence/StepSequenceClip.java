package seq.motif.stepsequence;

import seq.engine.*;
import java.util.*;
import javax.sound.midi.*;
import java.util.concurrent.*;

/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

public class StepSequenceClip extends Clip
    {
    private static final long serialVersionUID = 1;

    public static final int OFF = -1;
    StepSequence dSeq;

    // For each track, does the track currently have an outstanding NOTE_ON?  -1 if NO
    int[] trackNoteOn;
    // For each track, what is the ID of the currently outstanding track note?
    int[] trackNoteID;
    // For each track, is the track step currently playing (for flams, random).  There may be no step playing (due to swing, zero velocity, etc.)
    boolean[] trackPlaying;
    // For each track, which step is currently playing?  (or DEFAULT)   There may be no step playing (due to swing, zero velocity, etc.)
    int[] playingStep;
    // For each track, which step is currently playing?  (or DEFAULT)   There's always a step playing unless we have not yet started.
    int[] currentStep;
    double[] currentPos;
    // What iteration are we playing
    int iteration = 0;
    int trackNoteRecording = OFF;
    int trackVelocityRecording = OFF;
                
    public StepSequenceClip(Seq seq, Motif motif, Clip parent)
        {
        super(seq, motif, parent);
        rebuild();
        }
    
    public void rebuild(Motif motif)
        {
        if (this.getMotif() == motif) rebuild();
        }

    public void rebuild()
        {
        dSeq = (StepSequence)getMotif();
        
        // The number of tracks may have changed or rearranged
        int numTracks = dSeq.getNumTracks();
        trackNoteOn = new int[numTracks];   // default is -1
        Arrays.fill(trackNoteOn, OFF);
        trackNoteID = new int[numTracks];
        Arrays.fill(trackNoteOn, NO_NOTE_ID);
        trackPlaying = new boolean[numTracks];  // default is FALSE
        playingStep = new int[numTracks];
        currentStep = new int[numTracks];
        currentPos = new double[numTracks];
        clear();                                                                // clears the track notes and sets to OFF, sets playingStep to DEFAULT
        version = getMotif().getVersion();
        }

    void clearTrackNote(int track)
        {
        if (trackNoteOn[track] != OFF) 
            {
            noteOff(dSeq.getFinalOut(track), trackNoteOn[track], trackNoteID[track]);
            trackNoteOn[track] = OFF;
            trackNoteID[track] = NO_NOTE_ID;
            }
        }
           
    public void clear()
        {
        int numTracks = dSeq.getNumTracks();

        for(int track = 0; track < numTracks; track++)
            {
            clearTrackNote(track);
            playingStep[track] = OFF;
            trackPlaying[track] = false;
            }
        }

    void releaseTrackNote(int track)
        {
        if (trackNoteOn[track] != OFF) 
            {
            // When do we release it?
            int pos = getPosition();
            if (pos < 0) return;              // uhm....
            int len = dSeq.getLength();

            int numStepsInTrack = dSeq.getNumSteps(track);
            int stepLen = len / numStepsInTrack;
            int step = pos * numStepsInTrack / len;
            int remainder = (pos - step * stepLen);     // compute remainder without swing
            int releaseTime = /*seq.getTime() +*/ remainder;        // absolute time, not relative
            
            for(int i = 0; i < numStepsInTrack; i++)
                {
                int s = i + step + 1;
                while (s >= numStepsInTrack) s -= numStepsInTrack;      // it's possible to do it twice
                
                /*
                if (dSeq.getFinalNote(track, s) != StepSequence.TIE) break;
                else 
                */
                releaseTime += stepLen;
                }
            
            scheduleNoteOff(dSeq.getFinalOut(track), trackNoteOn[track], releaseTime, trackNoteID[track]);
            trackNoteOn[track] = OFF;                                                                       // not sure if we should do this...
            trackNoteID[track] = NO_NOTE_ID;
            }
        }
           
    public void release()
        {
        int numTracks = dSeq.getNumTracks();

        for(int track = 0; track < numTracks; track++)
            {
            releaseTrackNote(track);
            playingStep[track] = OFF;
            }
        }

    public void loop()
        {
        super.loop();
        iteration++;
        }
                
    public void reset()  
        {
        super.reset();
        iteration = 0;
        }
    
    /// Ultimately this should be called if we change the number or ordering
    /// of the tracks
    void clearExclusivity()
        {
        // clear exclusivity
        numExclusiveTracks = 0;
        exclusiveTrack = 0;
        exclusive = EMPTY;
        }
        
    /** Returns the currently playing step in the track, else -1 if no step right now */
    public int getPlayingStep(int track) { return playingStep[track]; }
        
    /** Returns the currently playing step in the track, else -1 */
    public int getCurrentStep(int track) { return currentStep[track]; }

        
    public double getCurrentPos(int track) { return currentPos[track]; }
        
    final static int[] EMPTY = new int[0];
    int numExclusiveTracks = 0;
    int exclusiveTrack = 0;
    int[] exclusive = EMPTY;
    double invNumTracks = 1.0;
    
    
    //// FIXME: we should set this up to allow notes played at any time and rounded to the nearest step
    
    public void record(int track, int step)
        {
        In in = seq.getIn(((StepSequence)getMotif()).getIn());
        if (in == null) return;
        int channel = in.getChannel();
        
        MidiMessage[] messages = in.getMessages();
        // Find last NOTE ON or NOTE OFF of the right channel
        for(int i = messages.length - 1; i >= 0; i--)
            {
            MidiMessage message = messages[i];
            if (message instanceof ShortMessage)
                {
                ShortMessage shortmessage = (ShortMessage)message;
                if (shortmessage.getCommand() == ShortMessage.NOTE_ON && shortmessage.getData2() > 0 && 
                    (channel == Midi.OMNI || channel == shortmessage.getChannel() + 1))
                    {
                    // we might be doing a new note or changing the pitch, either way
                    // we can't be tied any more
                    trackNoteRecording = shortmessage.getData1();
                    trackVelocityRecording = shortmessage.getData2();
                    dSeq.setNote(track, step, trackNoteRecording);
                    dSeq.setVelocity(track, step, trackVelocityRecording);
                    return;
                    }
                else if (shortmessage.getCommand() == ShortMessage.NOTE_OFF ||
                        (shortmessage.getCommand() == ShortMessage.NOTE_ON && shortmessage.getData2() == 0 && 
                        (channel == Midi.OMNI || channel == shortmessage.getChannel() + 1)))
                    {
                    // Need to turn everything off
                    trackNoteRecording = OFF;
                    trackVelocityRecording = OFF;
                    dSeq.setNote(track, step, trackNoteRecording);
                    dSeq.setVelocity(track, step, trackVelocityRecording);
                    return;
                    }
                }
            }
        
        /*
        // At this point we had nothing to change.  But are we tied?
        if (trackNoteRecording != OFF)          // looks like we're tied
            {
            // Need to add a tie
            dSeq.setNote(track, trackNoteRecording, StepSequence.TIE);
            dSeq.setVelocity(track, trackVelocityRecording, 0);
            }
        */
        }
        
    // called if we're doing learning for the track
    void doLearning(final int track)
        {
        In in = seq.getIn(((StepSequence)getMotif()).getIn());
        if (in == null) return;
        int channel = in.getChannel();
        
        MidiMessage[] messages = in.getMessages();
        // Find last NOTE ON of the right channel
        for(int i = messages.length - 1; i >= 0; i--)
            {
            MidiMessage message = messages[i];
            if (message instanceof ShortMessage)
                {
                ShortMessage shortmessage = (ShortMessage)message;
                if (shortmessage.getCommand() == ShortMessage.NOTE_ON && shortmessage.getData2() > 0 && 
                    (channel == Midi.OMNI || channel == shortmessage.getChannel() + 1))
                    {
                    // Set it
                    dSeq.setTrackNote(track, shortmessage.getData1());
                    // turn off learning
                    dSeq.setTrackLearning(track, false);
                    seq.issueRunnable(new Runnable()
                        {
                        public void run() 
                            { 
                            seq.gui.MotifUI mui = seq.getSeqUI().getMotifUI();
                            if (mui != null && mui instanceof seq.motif.stepsequence.gui.StepSequenceUI)
                                {
                                ((seq.motif.stepsequence.gui.StepSequenceUI)mui).unlearn(track);
                                }
                            }
                        });
                    return;
                    }
                }
            }
        }
    
    public boolean process()
        {
        // Length of entire sequence in PPQ
        int len = dSeq.getLength();
        int numTracks = dSeq.velocities.length;

        if (numTracks != currentPos.length)
            { 
            //FIXME this should probably be checked elsewhere when tracks num change
            this.rebuild();
            }

        int pos = getPosition();
        if (pos < 0) return false;              // uhm....
        if (pos >= len) return true;
                
        if (pos == 0)
            {
            // What are our exclusive tracks?
            // A track is exclusive if (1) no tracks are soloed (2) the track isn't muted (3) the track is declared exclusive
            if (!dSeq.isATrackSoloed())
                {
                exclusive = new int[numTracks];         // should we not allocate this each time FIXME
                for(int track = 0; track < numTracks; track++)
                    {
                    if (dSeq.isTrackExclusive(track) && dSeq.isTrackMuted(track))
                        {
                        exclusive[numExclusiveTracks++] = track;
                        }
                    }
                // pick an exclusive track
                if (numExclusiveTracks > 0)
                    {
                    exclusiveTrack = exclusive[ThreadLocalRandom.current().nextInt(numExclusiveTracks)];
                    }
                }
            invNumTracks = 1.0 / numTracks;                 // For now we're just computing this at pos=0, which is suboptimal but it avoids a division every time, it's just used for random computation
            }
        
        for(int track = 0; track < numTracks; track++)
            {
            if (dSeq.isTrackLearning(track)) doLearning(track);
                        
            if (dSeq.isTrackMuted(track)) continue;
            if (dSeq.isATrackSoloed() && !dSeq.isTrackSoloed(track)) continue;
            
            int numStepsInTrack = dSeq.getNumSteps(track);
            float stepLen = (float)len / numStepsInTrack;   //if this was int, too much approx error.
            int step = pos * numStepsInTrack / len;
            currentStep[track] = step;
            currentPos[track] = pos / (double)len;

            // I'm not sure what to do if steps are coming so fast that we skipped a step.
            // We will assume that doesn't happen for now.

            /*
            if (dSeq.getNote(track, step) == StepSequence.TIE) continue;              // we're tied, do nothing, keep playing  FIXME: will this work if the musician changes ties mid-play?
			*/
			
            // compute remainder and subtract swing portion
            int remainder = (int) (pos - step * stepLen) - (step % 2 == 0 ? 0 : (int)(stepLen * getCorrectedValueDouble(dSeq.getFinalSwing(track), 1.0)));
            if (remainder < 0) { playingStep[track] = OFF; continue; }               // we have swing, we're not ready yet
            
                
            // First issue a choke if we're at the start of a step
            if (remainder == 0)
                {
                int choke = dSeq.getTrackChoke(track);
                if (choke != 0) 
                    { 
                    clearTrackNote(choke - 1);
                    }
                
                // Record note if there is one
                if (dSeq.isArmed() && seq.isRecording())
                    {
                    /// FIXME
                    //record(track, step);
                    }
                }
                
            // Issue a note off at the start of every step -- this will be affected by swing, is that right?  FIXME
            if (remainder == 0)
                {
                clearTrackNote(track);
                }
                        
            // Next: what is our velocity?  If 0 we don't play at all
            int velocity = getCorrectedValueInt(dSeq.getFinalVelocity(track, step), 127);
            velocity = (int)(velocity * getCorrectedValueDouble(dSeq.getTrackGain(track), 1.0));

            if (dSeq.isOn(track, step) && velocity > 0 && (numExclusiveTracks == 0 || !dSeq.isTrackExclusive(track) || track == exclusiveTrack))
                {
                playingStep[track] = step;
                // Next are we set to do flams?  Regardless of speed, flam speed will always be the same.
                // We assume that the TOTAL INTERVAL is PPQ / 4 , that is, a full step. 
                int flam = dSeq.getFinalFlam(track, step);
                // yuck another division FIXME

                if ((flam == 0 && remainder == 0) || (flam > 0 && remainder % StepSequence.FLAMS[flam] == 0)) // time to play a note!
                    {
                    if (remainder == 0) 
                        {
                        // check if I need to play (only at beginning of step, the following flams -if any- must happen iff first happens)
                        boolean play = dSeq.playNow(invNumTracks, track, step, (iteration == 0 ? 0 : iteration - 1));
                        if (play) 
                            {
                            trackPlaying[track] = true;
                            } 
                        else 
                            {
                            trackPlaying[track] = false;
                            continue;
                            }
                        }
                    // choke previous flam if any
                    if (flam>0 && trackPlaying[track])           // flam > 0
                        {
                        clearTrackNote(track);
                        }
                        
                    // play if I'm flam 0 (no flam) or if I'm flam > 0 and the track is playing
                    if (flam == 0 || trackPlaying[track])
                        {
                        int note = getCorrectedValueInt(dSeq.getFinalNote(track, step), 127);
                        int id = noteOn(dSeq.getFinalOut(track), note, velocity);
                        trackNoteOn[track] = note;
                        trackNoteID[track] = id;
                        }
                    }
                }
            }
        return (pos >= len - 1);
        }



	/// Modified versions of getCorrectedValue.... to include DEFAULT as an option

    public double getCorrectedValueDouble(double basicVal)
        {
        if (basicVal == StepSequence.DEFAULT)
        	{
        	return basicVal;
        	}
        else return super.getCorrectedValueDouble(basicVal);
        }
    
    public double getCorrectedValueDouble(double basicVal, double maxVal)
        {
        if (basicVal == StepSequence.DEFAULT)
        	{
        	return basicVal;
        	}
        else return super.getCorrectedValueDouble(basicVal, maxVal);
        }
    
    public int getCorrectedValueInt(int basicVal, int maxVal)
        {
        if (basicVal == StepSequence.DEFAULT)
        	{
        	return basicVal;
        	}
        else return super.getCorrectedValueInt(basicVal, maxVal);
        }




    // TESTING
    public static void main(String[] args) throws Exception
        {
        // Set up Seq
        Seq seq = new Seq();            // starts timer running
        seq.setupForMIDI(StepSequenceClip.class, args, 0, 2);   // sets up MIDI in and out
        seq.setLooping(true);
        
        // Set up our module structure
        StepSequence dSeq = new StepSequence(seq, 2, 16);
        seq.setOut(0, new Out(seq, 0));         // Out 0 points to device 0 in the tuple.  This is too complex.
        seq.setOut(1, new Out(seq, 1));         // Out 0 points to device 0 in the tuple.  This is too complex.
        
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
        
        // Build Clip Tree
        seq.setData(dSeq);

        seq.reset();
        seq.play();
        
        seq.waitUntilStopped();         // we're looping so this will never exit
        }
                        
    }
