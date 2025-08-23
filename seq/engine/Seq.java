/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.engine;

import javax.swing.*;
import seq.util.*;
import java.io.*;
import java.util.*;
import javax.sound.midi.*;
import java.util.concurrent.locks.*;
import org.json.*;

/**
   The top-level class for the hierarchical music sequencer.
   Seq primarily holds (1) some kind of MIDI Input, (2) some kind
   of MIDI Output -- this could be the same object as the Input, 
   or optionally a MidiEmitter and SeqSynth to emit to its internal
   synthesizer (3) the timer, which pulses the sequencer (4) the top-level
   Clip object which represents the real-time sequence data state.
**/

public class Seq
    {
    private static final long serialVersionUID = 1;

    ///// FILE
    File file = null;

    ///// TIMING
    
    /** The time division resolution of the sequencer, in Parts Per Quarter-note.
        The actual time division is determined by the BPM (Beats Per Minute) of the
        sequencer as 1 / BPM * 60 / PPQ / 1000 milliseconds per tick. */
    public static final int PPQ = 192;
    /** The time division resolution of a MIDI clock, in Parts Per quarter-note. */
    public static final int MIDI_CLOCK = 24;
    /** The maximum number of beats per bar */
    // At 192 PPQ, if we have 256 bars and 256 parts
    // then we have 192 * 128 * 256 * 256 = 1610612736
    // Maximum positive integer is 2147483648, so we can't exceed that
    public static final int MAX_BEATS_PER_BAR = 128;
    public static final int NUM_BARS_PER_PART = 256;
    public static final int NUM_PARTS = 256;
    public static final int DEFAULT_BPM = 120;
    public static final int MAX_BPM = 480;
    /** The smallest possible maximum time, with subparts rounded to 0. */
    public static final int MIN_MAX_TIME = PPQ * 1 * NUM_BARS_PER_PART * NUM_PARTS;
    // Current BPM
    int bpm = DEFAULT_BPM;
    // The timer, pulses at the approporiate amount to do 192 PPQ at the current BPM
    //HighResolutionTimer timer;                        // park uses too much CPU  :-(
    java.util.Timer timer;
    // The timer lock.  This lock must be acquired in order to manipulate the data stored in clip or clip.motif
        
    Track[] tracks = null;
    boolean[] validTracks = null;
    public Track[] getTracks() { return tracks; }
    public void setTracks(Track[] val) { tracks = val; if (val != null) validTracks = new boolean[val.length]; }
    public boolean isValidTrack(int track) { return validTracks[track]; }
    public void setValidTrack(int track, boolean val) { validTracks[track] = val; }
    
    Out[] uniqueOuts = new Out[0];
    
    ReentrantLock lock = new ReentrantLock(true);
/*
  {
  public void lock()
  {
  if (tryLock()) 
  { 
  System.err.println("Locked " + Thread.currentThread()); 
  new Throwable().printStackTrace();
  }
  else
  {
  System.err.println("Waiting... "  + Thread.currentThread());
  super.lock();
  System.err.println("Success! " + Thread.currentThread());
  new Throwable().printStackTrace();
  }
  }
  public void unlock()
  {
  System.err.println("Unlocked"  + Thread.currentThread());
  super.unlock();
  }
  };
*/
        
    ///// PLAY STATE
    
    // Is the sequencer currently actively playing?
    boolean playing;    
    // Is the sequencer set up to do recording while playing?  [To record, playing must also be true]
    boolean recording;
    // If, when playing, will the sequencer do an infinite loop as opposed to play once and then terminate?
    boolean looping;
    // Am I done playing but waiting for the next measure boundary before I loop again?
    boolean endOfLoop;
    // Have I stopped after playing once?  This is used by waitUntilStopped() to block until the next time we have finished playing
    boolean stopped = true;
    // Am I resuming after a pause?
    boolean resuming = false;
    // The lock for the stopped parameter above.  BTW, it's an Object[0] because arrays are automatically serializable, so that's a useful trick.
    Object stoppedLock = new Object[0];
    int notificationTime = -1;
    // The current absolute playing time, in PPQ.  Note that individual modules have a "position" parameter, which is the relative time for them, where 0 is the start of the module
    int time = 0;
    // Which clips are playing RIGHT NOW?
//    ArrayList<Clip> playingClips = new ArrayList<>();
    

    ///// MIDI ROUTING
        
    // List of current routings as selected by the user
    Midi.Tuple tuple;
    // Midi facility for sending/receiving MIDI 
    Midi midi;
    // Optional facility for sending MIDI to the internal synthesizer (not set up yet)
    //MidiEmitter emitter;
    // Optional internal synthesizer (not set up yet)
    //SeqSynth synth;
    // The Out objects for each output device in the tuple
    Out[] outs;
    // The In objects for each input device in the tuple
    In[] ins;
    // The names for each output device in the tuple
    String[] outNames;
    // The In objects for each input device in the tuple
    String[] inNames;
    // Routing
    int routeIn;
    int routeOut;
    
    class UndoStuff
        {
        Motif display;
        Motif data;
        ArrayList<Motif> motifs;
        
        public UndoStuff(Motif data, Motif display, ArrayList<Motif> motifs) 
            { 
            // Make sure existing has all the motif copies
            HashMap<Motif, Motif> existing = new HashMap<>(); 
            for(Motif motif : motifs)
                {
                motif.copyDAG(existing);
                }
                                
            // Dump them into the new ArrayList
            this.motifs = new ArrayList<Motif>();
            for(Motif motif : motifs)
                {
                this.motifs.add(existing.get(motif));
                }
                                
            // Add Data and Display
            this.data = existing.get(data);
            this.display = existing.get(display);
            }
        }
    
    Undo<UndoStuff> undo;
    
    /** Pushes the current state of the sequencer on the undo stack, using the currently displayed motif,
        for purposes of going back to when we undo/redo. */
    public void push() 
        {
        push(sequi.getMotifUI().getMotif());
        }

    /** Pushes the current state of the sequencer on the undo stack. displayMotif
        is the currently displayed Motif, for purposes of going back to when we undo/redo. */
    public void push(Motif displayMotif) 
        {
        undo.push(new UndoStuff(data, displayMotif, motifs));
        sequi.updateUndoMenus();
        }

    /** Clears the undo/redo stacks. */
    public void clearUndo() 
        {
        undo.clear();
        sequi.updateUndoMenus();
        }
        
    public boolean canUndo() { return undo.canUndo(); }
    public boolean canRedo() { return undo.canRedo(); }
    public Motif undo(Motif displayMotif) 
        { 
        if (undo.canUndo())
            {
            UndoStuff stuff = (UndoStuff)undo.undo(new UndoStuff(data, displayMotif, motifs));
            motifs = stuff.motifs;
            data = stuff.data;
            sequi.updateUndoMenus();
            return stuff.display;
            }
        else return null;
        }
 
    public Motif redo(Motif displayMotif) 
        { 
        if (undo.canRedo())
            {
            UndoStuff stuff = (UndoStuff)undo.redo(new UndoStuff(data, displayMotif, motifs));
            motifs = stuff.motifs;
            data = stuff.data;
            sequi.updateUndoMenus();
            return stuff.display;
            }
        else return null;
        }

/*
    public Motif undo(Motif displayMotif) 
        { 
        if (undo.canUndo())
            {
            if (undo.onUndo().motifs != null)			// heavyweight node
            	{
            	UndoStuff stuff = (UndoStuff)undo.undo(new UndoStuff(data, displayMotif, motifs));
				motifs = stuff.motifs;
				data = stuff.data;
				}
			else
				{
				// push the display motif to 
            	UndoStuff stuff = (UndoStuff)undo.undo(new UndoStuff(displayMotif));
				Motif.substituteMotif(stuff.data, motifs);
				if (data == stuff.display)
					{
					data = stuff.data;
					}
				}
			sequi.updateUndoMenus();
			return stuff.display;
            }
        else return null;
        }
 
    public Motif redo(Motif displayMotif) 
        { 
        if (undo.canRedo())
            {
            if (undo.onRedo().motifs != null)			// heavyweight node
            	{
            	UndoStuff stuff = (UndoStuff)undo.redo(new UndoStuff(data, displayMotif, motifs));
            	motifs = stuff.motifs;
           		data = stuff.data;
           		}
           	else
           		{
            	UndoStuff stuff = (UndoStuff)undo.redo(new UndoStuff(displayMotif));
				Motif.substituteMotif(stuff.data, motifs);
				if (data == stuff.display)
					{
					data = stuff.data;
					}
           		}
            sequi.updateUndoMenus();
            return stuff.display;
            }
        else return null;
        }
*/
        
   
    ///// SEQUENCE DATA
    
    // All Motifs
    ArrayList<Motif> motifs = new ArrayList<>();
    // The root clip
    Clip root;
    // The root motif
    Motif data;

    ///// CLOCK
    public static final int CLOCK_IGNORE = 0;
    public static final int CLOCK_OUT = 1;
    public static final int CLOCK_IN = 2;                   // FIXME Not implemented
    
    // What is the clock mode?
    int clock = CLOCK_IGNORE;


    ///// DETERMINISTIC RANDOMNESS
    static int deterministicRandomSeed = (int)System.currentTimeMillis();
    Random deterministicRandom = new Random(deterministicRandomSeed);
    public void seedDeterministicRandom(int seed) { deterministicRandom.setSeed(deterministicRandomSeed = seed); }
    public void seedDeterministicRandom() { seedDeterministicRandom((int)System.currentTimeMillis()); }
    public void resetDeterministicRandom() { seedDeterministicRandom(deterministicRandomSeed); }
    public int getDeterministicRandomSeed() { return deterministicRandomSeed; }
    public Random getDeterministicRandom() { return deterministicRandom; }

    ///// PLAYING
    public static final int COUNT_IN_NONE = 0;
    public static final int COUNT_IN_RECORDING_ONLY = 1;
    public static final int COUNT_IN_RECORDING_AND_PLAYING = 2;
    public static final int METRONOME_NONE = 0;
    public static final int METRONOME_RECORDING_ONLY = 1;
    public static final int METRONOME_RECORDING_AND_PLAYING = 2;
        
    public static final int DEFAULT_BAR = 4;
    
    // How many beats per bar there are
    int bar = DEFAULT_BAR;
    // What our count-in mode is
    int countInMode = COUNT_IN_RECORDING_ONLY;          //COUNT_IN_NONE;
    // What is our metronome mode?
    int metronome = COUNT_IN_RECORDING_ONLY;
    // The current count-in count-down, in PPQ
    int currentCountIn;
    
    ///// BEEP
    //public static final int BEEP_COUNT_IN_FREQUENCY = 440;
    //public static final int BEEP_COUNT_IN_BAR_FREQUENCY = 880 * 2;
    // spublic static final int BEEP_FREQUENCY = 440;
    //public static final int BEEP_BAR_FREQUENCY = 880;
    public static final double BEEP[] =                         // Frequencies for pitches -12 ... 0 ... +12
        {
        220.00000, 233.08188, 246.94165, 261.62557, 277.18263, 293.66477,
        311.12698, 329.62756, 349.22823, 369.99442, 391.99544, 415.30470,
        440.00000, 466.16376, 493.88330, 523.25113, 554.36526, 587.32954,
        622.25397, 659.25511, 698.45646, 739.98885, 783.99087, 830.60940,
        880.00000
        };


    // Our beep, which creates its own thread.
    Beep beep = new Beep();
    // Beep volume, 0...1.0 as a fraction of maximum volume
    double beepVolume = 1.0;
    // Beep pitch, -12...12, with 0 centered at A 440
    int beepPitch = 0;
    
    ///// HOOKS FOR CHANGES IN STATE
    ArrayList<SeqListener> listeners = new ArrayList<>();
    
    ///// SHUTDOWN HOOK
    Thread shutdownHook = null;
    
    
    ///// MACRO CHILDREN
    int macroChildCounter = 1;          // Macro children need a unique namecounter that is ideally stored with the Seq so it can be saved
    
    ///// GUI
    public seq.gui.SeqUI sequi;                 // This is only public to allow various main() test methods to set up the sequi manually
    
    static final int TIMER_WARM_UP = 1000;          // we need some time before the timer is stable
    TimerTask timertask;
    int ppqtick = 0;
    long tick = 0;
    
    public Midi getMIDI() { return midi; }
    public Midi.Tuple getMIDITuple() { return tuple; }
    public void setMIDITuple(Midi.Tuple tuple) { this.tuple = tuple; }
    
    public File getFile() { return file; }
    /** Sets the file associated with this Seq.  Can be set to null. */
    public void setFile(File file) { this.file = file; }
    
    public int getBeepPitch()
        {
        return beepPitch;
        }
        
    public void setBeepPitch(int val)
        {
        beepPitch = val;
        }
    
    /** Returns frequencies for pitch deviations ranging from -12 to +12 */
    public double getBeepBarFrequency()
        {
        return BEEP[getBeepPitch() + 12];
        }
    
    /** Prepares Seq to be thrown away. */
    public void shutdown()
        {
        killTimerTask();
        }
        
    // Destroys the timer task if any (but NOT the timer)
    void killTimerTask()
        {
        if (timertask != null)
            {
            timertask.cancel();
            timer.purge();
            timer.cancel();
            }
        timertask = null;               // let it (and the Seq instance!) GC
        }
    
    // Creates the timer task using the existing tick value and bpm, and with the given warmup time
    void startTimerTask(int warmup)
        {
        killTimerTask();  // just in case
        
        timertask = new java.util.TimerTask()
            {
            public void run()
                {
                // how many ppqticks have transpired?
                while(tick * PPQ * bpm / 1000 / 60 >= ppqtick)
                    {
                    try
                        {
                        step();
                        }
                    catch (Exception ex)
                        {
                        System.err.println("Exception thrown during step: " + ex);
                        ex.printStackTrace();
                        }
                    ppqtick++;
                    }
                tick++;
                }
            };
        
        // Pushes the ppqtick of the timer to just in front of current timer tick.
        // We need to do this when changing the bpm so that the ppqtick is computed
        // properly with respect to the new bpm

        ppqtick = (int) Math.ceil(tick * PPQ * bpm / 1000 / 60.0);
        
        
        // If we have an exception for some reason, or for some other weird internal Java thing,
        // the timertask will CANCEL the timer, but we
        // cannot determine if the timer has been cancelled.  That is frustrating.  So here we
        // catch an exception and rebuild the timer if need be.

        try
            {
            timer.scheduleAtFixedRate(timertask, warmup, 1);                       // as fast as we can go
            }
        catch (IllegalStateException ex)
            {
            timer.cancel();
            timer.purge();
            timer = new java.util.Timer("Seq Sequencer Thread", true);                          // run as daemon
            timer.scheduleAtFixedRate(timertask, warmup, 1);                       // as fast as we can go
            }
        }
        
    public int nextMacroChildCounter() { return macroChildCounter++; }
        
        
        
    public Seq() throws Exception 
        {
        // Let's try to clean up notes if we can when we get a control-C
        Runtime.getRuntime().addShutdownHook(shutdownHook = new Thread(new Runnable()
            {
            public void run() { try { if (root != null) root.cut(); root.terminate(); } catch (Exception ex) { } }  // not threadsafe of course
            }));
        shutdownHook.setName("Seq Shutdown Hook");

        seedDeterministicRandom();
                
        // 60 sec/min * 1000000000 nanos/sec / 120 beat/min / 192 PPQ/beat = 2604167 nanos/PPQ
        // timer = new HighResolutionTimer(true, 2604167L, true);
        /*
          timer.start(new Runnable()
          {
          public void run()
          {
           ;
          }
          });
        */

        // Start up the timer with a 1 second warmup time
        timer = new java.util.Timer("Seq Sequencer Thread", true);
        //startTimerTask(TIMER_WARM_UP);
        undo = new Undo<UndoStuff>();
        }
        
    // doesn't kill the old timer task, you'll need to do that manually
    public Seq(Seq old) throws Exception
        {
        this();
        outs = old.outs;
        ins = old.ins;
        outNames = old.outNames;
        inNames = old.inNames;
        sequi = old.sequi;
        listeners.addAll(old.listeners);
        this.tuple = old.tuple;
        this.midi = old.midi;
//        this.emitter = old.emitter;
//        this.synth = old.synth;
        }
    
    ///// GETTERS/SETTERS
    
    // LOCK
    public ReentrantLock getLock() { return lock; }

    // CLOCK
    public int getClock() { return clock; }
    public void setClock(int val) { clock = val; }

    /** Converts time into an array of steps (ticks), beats, bars, and parts */
    public static int[] convertTime(int time, int beatsPerBar)
        {
        if (time <= 0) time = 0;
        int ticks = time % PPQ;
        if (time < 0) time = 0;
        int beats = time % (PPQ * beatsPerBar) / PPQ;
        if (time < 0) time = 0;
        int bars = time % (PPQ * beatsPerBar * NUM_BARS_PER_PART) / (PPQ * beatsPerBar);
        if (time <= 0) time = 0;
        int parts = time / (PPQ * beatsPerBar * NUM_BARS_PER_PART);
        
        return new int[] { ticks, beats, bars, parts };
        }
    
    public static String timeToString(int time, int beatsPerBar, StringBuilder builder)
        {
        if (time <= 0) time = 0;
        int ticks = time % PPQ;
        if (time < 0) time = 0;
        int beats = time % (PPQ * beatsPerBar) / PPQ;
        if (time < 0) time = 0;
        int bars = time % (PPQ * beatsPerBar * NUM_BARS_PER_PART) / (PPQ * beatsPerBar);
        if (time <= 0) time = 0;
        int parts = time / (PPQ * beatsPerBar * NUM_BARS_PER_PART);
        
        return builder.
            append(parts).append(" : ").
            append(bars).append(" : ").
            append(beats).append(" : ").
            append(ticks).toString();
        }

    public static String timeToString(int time, int beatsPerBar)
        {
        return timeToString(time, beatsPerBar, new StringBuilder());
        }
        
    public static int convertTime(int steps, int beats, int bars, int parts, int beatsPerBar)
        {
        return steps + 
            beats * PPQ + 
            bars * beatsPerBar * PPQ +
            parts * NUM_BARS_PER_PART * beatsPerBar * PPQ;
        }

    public static int convertTime(int[] array, int beatsPerBar)
        {
        return convertTime(array[0], array[1], array[2], array[3], beatsPerBar);
        }


    public static final int NUM_OUTS = 16;
    public static final int NUM_INS = 4;

    // OUT
    public Out getOut(int out) { return outs[out]; }
    public Out[] getOuts() { return outs; }
    public String[] getOutNames() { return outNames; }
    public void setOut(int out, Out val) { outs[out] = val; }
    public String getOutName(int out) { return outNames[out]; }
    public void setOutName(int out, String name) { outNames[out] = name; }
    public int getNumOuts() { return outs.length; }

    // IN
    public In getIn(int in) { return ins[in]; }
    public In[] getIns() { return ins; }
    public String[] getInNames() { return inNames; }
    public void setIn(int in, In val) { ins[in] = val; }
    public int getNumIns() { return ins.length; }

	// ROUTING
	public static final int ROUTE_IN_NONE = NUM_INS;
	public int getRouteIn() { return routeIn; }
	public void setRouteIn(int val) { routeIn = val; }
	public int getRouteOut() { return routeOut; }
	public void setRouteOut(int val) { routeOut = val; }
	

    // PLAYING
    /** Returns whether the sequencer is currently playing. */
    public boolean isPlaying() { return playing; }
    /** Returns whether the sequencer is currently stopped. */
    public boolean isStopped() { return stopped; }
    /** Returns whether the sequencer is trying to stop. */
    public boolean isReleasing() { return releasing; }
    /** Returns whether the sequencer has finished the sequence is waiting for the next measure boundary before it reloops. */
    public boolean isEndOfLoop() { return endOfLoop; }
    /** Returns whether the sequencer is currently paused.  The sequencer is paused when it is NOT playing but NOT stopped. */
    public boolean isPaused() { return !stopped && !playing; }
    /** Returns whether the sequencer is resuming from a pause. */
    public boolean isResuming() { return resuming; }
    /** Sets the number of beats per measure for purposes count-in and for looping quantization. */
    public void setBar(int val) { bar = Math.min(Math.max(1, val), MAX_BEATS_PER_BAR); }
    /** Returns the number of beats per measure for purposes count-in and for looping quantization. */
    public int getBar() { return bar; }
    /** Returns the metronome mode. */
    public int getMetronome() { return metronome; }
    /** Sets the metronome mode.  */
    public void setMetronome(int val) { metronome = val; }
    /** Returns the count-in mode, one of COUNT_IN_NONE, COUNT_IN_RECORDING_ONLY, or COUNT_IN_RECORDING_AND_PLAYING.
        The length of the count-in is one BAR as determined by getBar(). The metronome must be ON in order
        to get a count-in.  */
    public int getCountInMode() { return countInMode; }
    /** Sets the count-in mode, one of COUNT_IN_NONE, COUNT_IN_RECORDING_ONLY, or COUNT_IN_RECORDING_AND_PLAYING.
        The length of the count-in is one BAR as determined by getBar().   The metronome must be ON in order
        to get a count-in.  */
    public void setCountInMode(int val) { countInMode = val; }

/*
  public void addPlayingClip(Clip clip) { playingClips.add(clip); }
  public ArrayList<Clip> getPlayingClips() { return playingClips; }
*/
      
    // LOOPING
    /** Sets whether we loop after playing and continue playing.  Looping ends at the next measure bar, as determined by setBar(). */
    public void setLooping(boolean val) { looping = val; }
    /** Returns whether we loop after playing and continue playing.  Looping ends at the next measure bar, as determined by setBar(). */
    public boolean isLooping() { return looping; }

    // RECORDING    
    /** Sets recording and also throws away any existing input MIDI Messages */
    public void setRecording(boolean val) { for(int i = 0; i < ins.length; i++) ins[i].getMessages(); recording = val; updateGUI(false); }
    /** Returns recording. */
    public boolean isRecording() { return recording; }

    public void addListener(SeqListener listener) { listeners.add(listener); }


    // MOTIFS
        
    /** Adds a motif to the motif set.  Returns false if the motif was already there.  Note that the order may change in the set, do not rely on it. */
    public boolean addMotif(Motif motif)
        {
        if (motifs.contains(motif)) return false;
        motifs.add(motif);
        return true;
        }

    /** Returns true if the given motif is in the motif set. */
    public boolean getContainsMotif(Motif motif)
        {
        return motifs.contains(motif);
        }

    /** Removes a motif from the motif set.  Returns false if the motif was not there.  Note that the order may change in the set, do not rely on it. */
    public boolean removeMotif(Motif motif)
        {
        if (!motifs.contains(motif)) return false;
        motifs.remove(motif);
        return true;
        }

    /** Returns the motif set.  Note that the order may change in the set, do not rely on it. */
    public ArrayList<Motif> getMotifs()
        {
        return motifs;
        }

    // ROOT MOTIF
    public Clip getRoot() { return root; }
    public Motif getData() { return data; }
    /** 
        If stopped, sets data and root, and returns true.  
        If not stopped, does nothing and returns false;
    */
    public boolean setData(Motif val) 
        { 
        if (!stopped) return false;
        data = val;
        if (root != null && root.isPlaying()) root.terminate(); // probably won't happen
        root = data.buildClip(null);
        return true;
        }
        
    // BEEP
    public double getBeepVolume() { return beepVolume; }
    public void setBeepVolume(double val) { beepVolume = val; }
    
    // BPM
    /** Sets the BPM and modifies the timer appropriately.  */
    public void setBPM(int val) 
        {
        // To set the BPM, we have to reset the timer task so its timerticks are caught up to the
        // the appropriate ticks for the new bpm value.
    
        killTimerTask();
        bpm = val;
        startTimerTask(0);
        //timer.setInterval(((60 * 1000000000L) / val) / PPQ);    // hope this works
        }
        
    public int getBPM() { return bpm; } 
    
    void setCountIn(final int val)
        {
        Seq.this.sequi.setCountIn(val, recording);
        }

    public void issueRunnable(Runnable runnable)
        {
        SwingUtilities.invokeLater(runnable);
        }
                
    public seq.gui.SeqUI getSeqUI() { return sequi; }

    void updateGUI(boolean inResponseToStep) 
        {
        SwingUtilities.invokeLater(new Runnable()
            {
            public void run()
                {
                if (sequi != null) sequi.redraw(inResponseToStep);
                for(SeqListener listener : listeners)
                    {
                    listener.stateChanged(Seq.this);
                    }
                }
            });
        }

    void informStopped() 
        {
        SwingUtilities.invokeLater(new Runnable()
            {
            public void run()
                {
                sequi.stopped();
                }
            });
        }
    
    public void resetPlayingClips()
        {
        lock.lock();
        try
            {
            for(Motif motif : motifs)
                {
                motif.resetPlayCount();         // this is cheating...
                motif.removePlayingClip();      // also cheating...
                }
            }
        finally
            {
            lock.unlock();
            }
        }
    
    /** Stops the Sequence, resets the time to 0, and clears all outstanding NOTE ON messages.  
        Notifies anyone waiting and blocked via waitUntilStopped. */
    public void stop()
        {
        killTimerTask();
        lock.lock();
        try
            {
            if (stopped) return;                // no need
            
            beep.setAmplitude(0);
            beep.setRunning(false);
            reset();
            root.cut();
            root.terminate();
            cut();
            resetPlayingClips();
            playing = false;
            recording = false;
            resuming = false;
            if (clock == CLOCK_OUT)
                {
                for(int i = 0; i < uniqueOuts.length; i++)
                    {
                    uniqueOuts[i].clockStop();
                    }
                }

            }
        finally
            {
            lock.unlock();
            synchronized(stoppedLock)
                {
                stopped = true;
                stoppedLock.notifyAll();
                }
            }
//        playingClips.clear();
        informStopped();
        updateGUI(false);
        }

    /** Starts the Sequence from being stopped or paused. */
    public void play()
        {
        startTimerTask(0);
        lock.lock();
        try
            {
            uniqueOuts = gatherUniqueOuts();            // Load so we can use them in step() and stop()

            // This should be first so we can send MIDI during reset()
            playing = true;

            endOfLoop = false;
            resetDeterministicRandom();
            if (stopped) 
                { 
                root.reset();
                currentCountIn = (countInMode != COUNT_IN_NONE ? 1 : 0) * bar * PPQ;                // reset, we're counting in
                resuming = false;
                }
            else 
                {
                resuming = true;
                if (currentCountIn > 0)        // we're currently counting in, the behavior at present should be to reset the count
                    {
                    currentCountIn = (countInMode != COUNT_IN_NONE ? 1 : 0) * bar * PPQ;                // reset, we're counting in
                    }
                }
            beep.setAmplitude(0);
            beep.setRunning(true);
            
            stopped = false;

            // Grab latest incoming MIDI messages and clear 
            if (ins != null)            // they may not be set up yet?
                {
                for(int i = 0; i < ins.length; i++)
                    {
                    ins[i].pullMessages();
                    }
                }
                /*
			// Route
			if (routeIn != ROUTE_IN_NONE)
				{
				for(MidiMessage message : ins[routeIn].getMessages())
					{
					outs[routeOut].sendMIDI(message);
					]
				}
			*/
            }
        finally
            {
            lock.unlock();
            }
        updateGUI(false);
        }
        
    /** Stops (pauses) the Sequence.  Clears all outstanding NOTE ON messages  Does not reset the time.  
        Does NOT notify anyone waiting and blocked via waitUntilStopped. */
    public void pause()
        {
        killTimerTask();
        lock.lock();
        try
            {
            if (clock == CLOCK_OUT)
                {
                for(int i = 0; i < uniqueOuts.length; i++)
                    {
                    uniqueOuts[i].clockStop();
                    }
                }

            if (stopped)                 // not sure this is necessary
                { 
                // stop cleared the root and reset it so we're fine
                }
            beep.setAmplitude(0);
            beep.setRunning(false);
            cut(); 
            playing = false;
            resuming = false;
            }
        finally
            {
            lock.unlock();
            }
        updateGUI(false);
        }
    
    /** Blocks until the next time the sequence has stopped. */
    public void waitUntilStopped()
        {
        waitUntilTime(-1);
        }

    /** Blocks until the following time has been reached, or until the sequence has stopped. */
    public void waitUntilTime(int notificationTime)
        {
        this.notificationTime = notificationTime;
        synchronized(stoppedLock)
            {
            int t = 0;
            boolean s = false;
            lock.lock();
            try
                {
                s = stopped;
                t = this.time;
                }
            finally
                {
                lock.unlock();
                }

            while((notificationTime == -1 || t < notificationTime) && !s)
                {
                try
                    {
                    stoppedLock.wait();
                    }
                catch (InterruptedException ex) { }

                // reacquire
                lock.lock();
                try
                    {
                    s = stopped;
                    t = this.time;
                    }
                finally
                    {
                    lock.unlock();
                    }
                }

            // clear
            lock.lock();
            try
                {
                stopped = false;
                playing = false;
                notificationTime = -1;
                }
            finally
                {
                lock.unlock();
                }
            }
        }

    /** Resets the sequence time to 0, clears, resets beep, and updates the UI. */ 
    public void reset()
        {
        resetTime();
        cut();
//        data.endArmed();
        updateGUI(false);
        }
        
    /** Clears all outstanding NOTE_ON messages (sends NOTE_OFF messages corresponding to them).  */
    public void cut()
        {
        lock.lock();
        try
            {
            beep.setAmplitude(0);
            if (root != null)
                {
                root.cut();
                processNoteOffs(true);
                releasing = false;
                updateGUI(false);
                }
            }
        finally
            {
            lock.unlock();
            }
        }


    /** Sets all motifs to not be armed. */
    public void disarmAll()
        {
        for(Motif motif : motifs)
            {
            motif.setArmed(false);
            }
        }
        
    /** Returns the current absolute time. */
    public int getTime()
        {
        lock.lock();
        try
            {
            return time;
            }
        finally
            {
            lock.unlock();
            }
        }
                
    /** Sets the current absolute time to 0 and resets beep. */
    public void resetTime()
        {
        lock.lock();
        try
            {
            beep.setAmplitude(0);
            time = 0;
            }
        finally
            {
            lock.unlock();
            }
        }

    void doBeep(int time, double beepFrequency, double beepBarFrequency)
        {
        if (time % PPQ == 0)
            {
            if (((time / bar) % PPQ) == 0)  // measure beep
                {
                beep.setFrequencyAndAmplitude(beepBarFrequency, beepVolume);
                }
            else
                {
                beep.setFrequencyAndAmplitude(beepFrequency, beepVolume);
                }
            }
        else if (Math.abs(time % PPQ) == PPQ / 8) // always, just in case
            {
            beep.setAmplitude(0);
            }
        }
    
    boolean releasing = false;
    
    
    Out[] gatherUniqueOuts()
        {
        HashMap<Midi.MidiDeviceWrapper, Out> unique = new HashMap<>();
        for(int i = 0; i < outs.length; i++)
            {
            unique.put(outs[i].getWrapper(),outs[i]);
            }
        return (Out[])(unique.values().toArray(new Out[0]));
        }
    
    int ccount = 0;
    long lastCCTime = 0;
    
    // Called by the timer to advance the sequencer one step.  
    // Returns TRUE if advance() believes that it is now finished after processing 
    // this step, that is, its NEXT step would exceed its length
    void step()
        {
        lock.lock();
        try
            {
            if (playing)
                {
                if (currentCountIn > 0 &&  // we're counting in
                        ((recording && (countInMode != COUNT_IN_NONE)) ||                   // we're recording, and the count-in is for recording 
                        ((playing && (countInMode == COUNT_IN_RECORDING_AND_PLAYING)))))    // we're playing, and teh count-in is for playing
                    {
                    // we're in the count-in phase
                    doBeep(Math.abs(bar) * PPQ - currentCountIn, getBeepBarFrequency(), getBeepBarFrequency() * 4);
                    if ((currentCountIn % PPQ) == 0)
                        setCountIn(currentCountIn / PPQ);
                    currentCountIn--;
                    
                    // send out clock even though we've not started yet as a good measure 
                    if (currentCountIn % (PPQ / 24) == 0)           // issue a pulse
                        {
                        for(int i = 0; i < uniqueOuts.length; i++)
                            {
                            uniqueOuts[i].clockPulse();
                            }
                        }
                    }

                else
                    {
                    if (currentCountIn == 0)
                        {
                        setCountIn(currentCountIn);
                        currentCountIn--;                                                       // we're done with count-ins
                        }
                    
                    if (clock == CLOCK_OUT)
                        {
                        if (isResuming())
                            {
                            for(int i = 0; i < uniqueOuts.length; i++)
                                {
                                uniqueOuts[i].clockContinue();
                                }
                            }
                        else if (time == 0)             // issue a start
                            {
                            for(int i = 0; i < uniqueOuts.length; i++)
                                {
                                uniqueOuts[i].clockStart();
                                }
                            }
                                                        
                        if (time % (PPQ / 24) == 0)             // issue a pulse
                            {
                            for(int i = 0; i < uniqueOuts.length; i++)
                                {
                                uniqueOuts[i].clockPulse();
                                }
                            }
                        }
                    resuming = false;                       // we're done resuming
                                               
                    if (time == notificationTime && notificationTime != -1) 
                        {
                        synchronized(stoppedLock)
                            {
                            stoppedLock.notifyAll();
                            }
                        }
                                                
                    // Grab latest incoming MIDI messages
                    if (ins != null)            // they may not be set up yet?
                        {
                        for(int i = 0; i < ins.length; i++)
                            {
                            ins[i].pullMessages();
                            }
                        }
                        
                        /*
                    // Route
                    if (routeIn != ROUTE_IN_NONE)
                    	{
                    	for(MidiMessage message : ins[routeIn].getMessages())
                    		{
                    		outs[routeOut].sendMIDI(message);
                    		]
                    	}
                        */
                                                              
                    // Handle beep
                    if (metronome == METRONOME_RECORDING_AND_PLAYING ||
                        (metronome == METRONOME_RECORDING_ONLY && recording))
                        {
                        doBeep(time, getBeepBarFrequency(), getBeepBarFrequency() * 2);
                        }
                        
                    processNoteOffs(false);
//                    playingClips.clear();
                    boolean atEnd = (time == NUM_BARS_PER_PART * NUM_PARTS * bar - 1);
                    if (ccount == 0)
                        {
                        long cur = System.currentTimeMillis();
                        //System.err.println(cur - lastCCTime);
                        lastCCTime = cur;
                        }
                    ccount++;
                    if (ccount >= 24) ccount = 0;
                    
                    if (releasing || endOfLoop || root.advance()) 
                        {
                        if (atEnd)
                            {
                            terminate();
                            }
                        else if (isLooping())
                            {
                            if (time % (PPQ * 4) == (PPQ * 4 - 1))
                                {
                                root.release();
                                processNoteOffs(true);
//                                data.endArmed();
                                resetTime();
                                recording = false;
//                                playingClips.clear();
                                root.terminate();
                                root.reset();
                                endOfLoop = false;
                                }
                            else 
                                {
                                endOfLoop = true;
                                time++;
                                }
                            }
                        else
                            {
                            root.release();
                            releasing = true;
                            if (noteOff.isEmpty())
                                {
                                releasing = false;
                                stop();
                                }
                            else 
                                {
                                time++;
                                }
                            }
                        }
                    else
                        {
                        if (atEnd)
                            {
                            terminate();
                            }
                        else
                            {
                            time++;
                            }
                        }
                    }
                }
            }
        finally
            {
            lock.unlock();
            }
        updateGUI(true);                            // should we do this at this rate?
        }
    
    // called when we're finished because we ran out of time -- this is a rare situation
    void terminate()
        {
        if (isLooping())
            {
            root.release();
//            data.endArmed();
            resetTime();
            recording = false;
//            playingClips.clear();
            root.terminate();
            root.reset();
            }
        else    
            {
            cut();              // this will also processNoteOffs(true)
            stop();
            }
        updateGUI(false);                            // should we do this at this rate?
        }

    public void sendPanic()
        {
        for(int i = 0; i < outs.length; i++)
            {
            forceCC(i, 120, 0);     // all sound off
            forceCC(i, 123, 0);     // all notes off
            }
        }

    /** Sends a sysex message to the given out.  
        Returns true if the message was successfully sent.  */
    public boolean sysex(int out, byte[] sysex)
        {
        if (isPlaying()) return outs[out].sysex(sysex);
        else return false;
        }

    /** Sends a sysex message to the given out regardless of whether we are playing.  
        Returns true if the message was successfully sent.  */
    public boolean forceSysex(int out, byte[] sysex)
        {
        return outs[out].sysex(sysex);
        }

    /** Sends a note on to the given Out.  Note that velocity is expressed as a double.
        This is because it can go above 127 or between 0.0 and 1.0 if multiplied by various 
        gains, and then returned to reasonable values.  Ultimately it will be floored 
        to an int.  Returns true if the message was successfully sent.  */
    public boolean noteOn(int out, int note, double vel) 
        {
        if (isPlaying()) return outs[out].noteOn(note, vel);
        else return false;
        }
        
    /** Sends a note off to the given Out.  Note that velocity is expressed as a double.
        this is because it can go above 127 or between 0.0 and 1.0 if multiplied by various 
        gains, and then returned to reasonable values.  Ultimately it will be floored 
        to an int.  Returns true if the message was successfully sent.  */
    public boolean noteOff(int out, int note, double vel) 
        {
        if (isPlaying()) return outs[out].noteOff(note, vel);
        else return false;
        }
        
    /** Sends a note off to the given Out with default velocity. 
        Returns true if the message was successfully sent.  */
    public boolean noteOff(int out, int note) 
        {
        if (isPlaying()) return outs[out].noteOff(note);
        else return false;
        }

    /** Sends a note on to the given Out regardless of whether Seq is playing.  
        Note that velocity is expressed as a double.
        This is because it can go above 127 or between 0.0 and 1.0 if multiplied by various 
        gains, and then returned to reasonable values.  Ultimately it will be floored 
        to an int.  Returns true if the message was successfully sent.  */
    public boolean forceNoteOn(int out, int note, double vel) 
        {
        return outs[out].noteOn(note, vel);
        }
        
    /** Sends a note off to the given Out regardles of whether Seq is playing.  
        Note that velocity is expressed as a double.
        this is because it can go above 127 or between 0.0 and 1.0 if multiplied by various 
        gains, and then returned to reasonable values.  Ultimately it will be floored 
        to an int.  Returns true if the message was successfully sent.  */
    public boolean forceNoteOff(int out, int note, double vel) 
        {
        return outs[out].noteOff(note, vel);
        }
        
    /** Sends a note off to the given Out with default velocity regardless of whether Seq is playing. 
        Returns true if the message was successfully sent.  */
    public boolean forceNoteOff(int out, int note) 
        {
        return outs[out].noteOff(note);
        }

    /** Sends a note on to the given Out on the given channel regardless of whether Seq is playing.  
        Note that velocity is expressed as a double.
        This is because it can go above 127 or between 0.0 and 1.0 if multiplied by various 
        gains, and then returned to reasonable values.  Ultimately it will be floored 
        to an int.  Returns true if the message was successfully sent.  */
    public boolean forceNoteOn(int out, int note, double vel, int channel) 
        {
        return outs[out].noteOn(note, vel, channel);
        }
        
    /** Sends a note off to the given Out on the given channel regardless of whether Seq is playing.  
        Note that velocity is expressed as a double.
        this is because it can go above 127 or between 0.0 and 1.0 if multiplied by various 
        gains, and then returned to reasonable values.  Ultimately it will be floored 
        to an int.  Returns true if the message was successfully sent.  */
    public boolean forceNoteOff(int out, int note, double vel, int channel) 
        {
        return outs[out].noteOff(note, vel, channel);
        }
        
    /** Sends a note off to the given Out on the given channel with default velocity regardless of whether Seq is playing. 
        Returns true if the message was successfully sent.  */
    public boolean forceNoteOff(int out, int note, int channel) 
        {
        return outs[out].noteOff(note, channel);
        }

    /** Sends a bend to the given Out.   Bend goes -8192...8191.
        Returns true if the message was successfully sent.  */
    public boolean bend(int out, int val) 
        {
        if (isPlaying()) return outs[out].bend(val);
        else return false;
        }
        
    /** Sends a CC to the given Out regardless of whether we're playing. 
        Returns true if the message was successfully sent.  */
    public boolean forceCC(int out, int cc, int val) 
        {
        return outs[out].cc(cc, val);
        }

    /** Sends a CC to the given Out. 
        Returns true if the message was successfully sent.  */
    public boolean cc(int out, int cc, int val) 
        {
        if (isPlaying()) return outs[out].cc(cc, val);
        else return false;
        }
        
    /** Sends a PC to the given Out. 
        Returns true if the message was successfully sent.  */
    public boolean pc(int out, int val) 
        {
        if (isPlaying()) return outs[out].pc(val);
        else return false;
        }
        
    /** Sends a bend to the given Out, associated with a given note (for MPE).   Bend goes -8192...8191.
        Returns true if the message was successfully sent.  */
    /*
      public boolean bend(int out, int note, int val) 
      {
      if (isPlaying()) return outs[out].bend(note, val);
      else return false;
      }
    */
        
    /** Sends a CC to the given Out, associated with a given note (for MPE). 
        Returns true if the message was successfully sent.  */
    /*
      public boolean cc(int out, int note, int cc, int val) 
      {
      if (isPlaying()) return outs[out].cc(note, cc, val);
      else return false;
      }
    */
        

    /** Sends a polyphonic aftertouch change to the given Out.  If the Out is set
        up for only channel aftertouch, this will be converted to channel aftertouch. 
        Returns true if the message was successfully sent.  
        You can pass in Out.CHANNEL_AFTERTOUCH for the note, and this will force the message to be sent
        as a channel aftertouch message regardless. */
    public boolean aftertouch(int out, int note, int val) 
        {
        if (isPlaying()) return outs[out].aftertouch(note, val);
        else return false;
        }

    /** Sends an NRPN (MSB+LSB) to the given Out. 
        Returns true if the message was successfully sent.  */
    public boolean nrpn(int out, int nrpn, int val) 
        {
        if (isPlaying()) return outs[out].nrpn(nrpn, val);
        else return false;
        }
        
    /** Sends an NRPN (MSB only) to the given Out. Thus if you send
        NRPN coarse parameter 42, it'll be sent as parameter 42 * 128.  
        Returns true if the message was successfully sent.  */
    public boolean nrpnCoarse(int out, int nrpn, int msb) 
        {
        if (isPlaying()) return outs[out].nrpnCoarse(nrpn, msb);
        else return false;
        }

    /** Sends an RPN (MSB+LSB) to the given Out. 
        Returns true if the message was successfully sent.  */
    public boolean rpn(int out, int rpn, int val) 
        {
        if (isPlaying()) return outs[out].rpn(rpn, val);
        else return false;
        }
        

    static class NoteOff
        {
        int out;
        int note;
        double velocity;
        public NoteOff(int out, int note, double velocity) { this.out = out; this.note = note; this.velocity = velocity; }
        public NoteOff(int out, int note) { this.out = out; this.note = note; this.velocity = 64; }
        public String toString() { return "NoteOff[" + note + "," + velocity + "," + out + "]"; }
        }
    
    Heap noteOff = new Heap();

    /** Schedules a note off, with the given note pitch value and velocity, to be sent to the given Out at a time in the future RELATIVE to the current position.  
        Note that velocity is expressed as a double.
        this is because it can go above 127 or between 0.0 and 1.0 if multiplied by various 
        gains, and then returned to reasonable values.  Ultimately it will be floored 
        to an int. */
    public void scheduleNoteOff(int out, int note, double velocity, int time)
        {
        lock.lock();
        try
            {
            noteOff.add(new NoteOff(out, note, velocity), Integer.valueOf(time + getTime()));
            }
        finally
            {
            lock.unlock();
            }
        }
        
    void processNoteOffs(boolean all)
        {
        while(true)
            {
            Integer i = (Integer)(noteOff.getMinKey());
            if (i == null) return;
            if (all || time >= i.intValue())
                {
                NoteOff note = (NoteOff)(noteOff.extractMin());
                Out out = outs[note.out];
                if (out != null)
                    {
                    out.noteOff(note.note, note.velocity);
                    }
                }
            else break;
            }       
        }
        
        
    /// PARAMETERS

    double[] parameterValues = new double[Motif.NUM_PARAMETERS];
    double randomMin = 0.0;
    double randomMax = 1.0;
                
    public void setRandomMin(double val) { randomMin = val; }
    public double getRandomMin() { return randomMin; }
    public void setRandomMax(double val){ randomMax = val; }
    public double getRandomMax() { return randomMax; }
    public void setParameterValue(int index, double val) { parameterValues[index] = val; }
    public double getParameterValue(int index) { return parameterValues[index]; }
    public double[] getParameterValues() { return parameterValues; }

    /////// JSON SERIALIZATION
    
    public JSONObject save(boolean saveAll) throws JSONException
        {
        JSONObject obj = new JSONObject();
        obj.put("bpm", bpm);
        obj.put("loop", looping);
        /// FIXME: How do I save / load the ins and outs?
        obj.put("clock", clock);
        obj.put("bar", bar);
        obj.put("countin", countInMode);
        obj.put("metronome", metronome);
        obj.put("beepvolume", beepVolume);
        obj.put("beeppitch", beepPitch);
        obj.put("macrochildcounter", macroChildCounter);
        JSONArray params = Motif.doubleToJSONArray(parameterValues);
        obj.put("params", params);
        obj.put("rmax", randomMax);
        obj.put("rmin", randomMin);
        obj.put("seed", getDeterministicRandomSeed());
                
        // Save the motifs
        JSONArray array = new JSONArray();
        HashSet<Motif> savedMotifs = new HashSet<>();
        int[] nextID = { 0 };
        // First, save the root, so it's ID 0
        data.save(savedMotifs, array, nextID);
        
        if (saveAll)
            {
            // Next, save any remaining motifs
            for(Motif motif : motifs)
                {
                if (!savedMotifs.contains(motif))
                    {
                    motif.save(savedMotifs, array, nextID);
                    }
                }
            }
        obj.put("motifs", array);

        
        // Save MIDI
        lock.lock();
        try
            {
            JSONArray outDevs = new JSONArray();
            JSONArray inDevs = new JSONArray();
            Midi.saveTupleToJSON(tuple, outDevs, inDevs);
            obj.put("out", outDevs);
            obj.put("in", inDevs);
            }
        finally
            {
            lock.unlock();  
            }


        return obj;
        }    
    
    public void merge(ArrayList<Motif> other)
        {
        for(Motif m : other)
            {
            motifs.add(m);
            }
        }
    
    /** May throw an exception on creating a Seq, but it's unlikely */
    public static Seq load(Seq old, JSONObject obj) throws Exception        
        {
        Seq seq = new Seq(old);
        
        seq.bpm = obj.optInt("bpm", DEFAULT_BPM);
        seq.looping = obj.optBoolean("loop", false);
        seq.clock = obj.optInt("clock", CLOCK_IGNORE);
        seq.bar = obj.optInt("bar", DEFAULT_BAR);
        seq.countInMode = obj.optInt("countin", COUNT_IN_RECORDING_ONLY);
        seq.metronome = obj.optInt("metronome", METRONOME_RECORDING_ONLY);
        seq.beepVolume = obj.optDouble("beepvolume", 1.0);
        seq.beepPitch = obj.optInt("beeppitch", 0);
        seq.macroChildCounter = obj.getInt("macrochildcounter");                // note not optInt
        seq.parameterValues = Motif.JSONToDoubleArray(obj.getJSONArray("params"));      // note not getJSONArray
        seq.randomMax = obj.optDouble("rmax", 0.0);
        seq.randomMin = obj.optDouble("rmin", 1.0);
        
        // seed
        int seed = obj.optInt("seed", 0);
        if (seed == -1)
            {
            System.err.println("Motif.load WARNING: no random seed in sequence, so we're entirely reseeding.");
            seq.seedDeterministicRandom();
            }
        else
            {
            seq.seedDeterministicRandom(seed);
            }
        
        // Load the motifs
        seq.motifs = Motif.load(seq, obj.getJSONArray("motifs"), true);
        seq.data = seq.motifs.get(0);
        seq.root = seq.data.buildClip(null);
        
        // Update MIDI
        seq.getLock().lock();
        try
            {
            JSONArray outDevs = obj.getJSONArray("out");
            JSONArray inDevs = obj.getJSONArray("in");
            if (outDevs != null && inDevs != null)
                {
                seq.tuple = Midi.loadTupleFromJSON(outDevs, inDevs, seq.getIns());
                }
            }
        catch(org.json.JSONException ex)
            {
            // this is incorrectly thrown by getJSONArray when "out" or "in" don't exist.
            // That's fine, we'll just drop here and not bother loading the tuple.
            }
        finally
            {
            seq.getLock().unlock();  
            }
        return seq;
        }


    public void fireMIDIIn()
        {
        SwingUtilities.invokeLater(new Runnable()
            {
            public void run()
                {
                if (sequi != null)
                    {
                    sequi.getTransport().fireMIDIIn();
                    }
                }
            });
        }
                 
    /** Convenience method to set up the sequencer to use the SeqSynth as output.  
        Don't use this for the time being. */
    /* public void setupForSynth(Class mainClass, Synth synth, String[] args, int numMIDIInput, int numMIDIOutput, boolean includesInput) 
       {
       lock.lock();
       try
       {
       outs = new Out[numMIDIOutput];
       ins = new In[numMIDIInput];
       midi = new Midi(numMIDIOutput, numMIDIInput);
       javax.sound.sampled.Mixer.Info[] mixers = synth.getSupportedMixers();
       javax.sound.sampled.Mixer.Info[] inputs = synth.getSupportedInputs();

       if (args.length == 0) 
       {
       showDevices(mainClass, numMIDIOutput, numMIDIInput, midi, mixers, inputs, includesInput, true);
       System.exit(0);
       }
       else if (args.length == (numMIDIInput * 2 + numMIDIOutput * 2 + (includesInput ? 1 : 0) + 1))
       {
       int pos = 0;

       ArrayList<Midi.MidiDeviceWrapper> out = midi.getOutDevices();
       int[] outChannels = new int[numMIDIOutput];
       Midi.MidiDeviceWrapper[] outWrappers = new Midi.MidiDeviceWrapper[numMIDIOutput];
            
       for(int i = 0; i < numMIDIOutput; i++)
       {
       int x = getInt(args[pos++]);
       if (x >= 0 && x < out.size()) 
       {
       outWrappers[i] = (out.get(x));
       }
       else 
       {
       System.err.println("Invalid MIDI number " + x + "\n");
       showDevices(mainClass, numMIDIOutput, numMIDIInput, midi, mixers, inputs, includesInput, true);
       System.exit(1);
       }
       x = getInt(args[pos++]);
       if (x >= 1 && x <= 16)
       {
       outChannels[i] = x;
       }
       else
       {
       System.err.println("Invalid MIDI channel " + x + "\n");
       showDevices(mainClass, numMIDIOutput, numMIDIInput, midi, mixers, inputs, includesInput, true);
       System.exit(1);
       }                                       
       outs[i] = new Out(this, i);
       System.err.println("Out MIDI " + i + ": " + outWrappers[i] + "Channel: " + outChannels[i]);
       }

       ArrayList<Midi.MidiDeviceWrapper> in = midi.getInDevices();
       int[] inChannels = new int[numMIDIInput];
       Midi.MidiDeviceWrapper[] inWrappers = new Midi.MidiDeviceWrapper[numMIDIInput];
       ins = new In[numMIDIInput];
            
       for(int i = 0; i < numMIDIInput; i++)
       {
       int x = getInt(args[pos++]);
       if (x >= 0 && x < in.size()) 
       {
       inWrappers[i] = (in.get(x));
       }
       else 
       {
       System.err.println("Invalid MIDI number " + x + "\n");
       showDevices(mainClass, numMIDIOutput, numMIDIInput, midi, mixers, inputs, includesInput, true);
       System.exit(1);
       }
       x = getInt(args[pos++]);
       if (x >= 1 && x <= 16)
       {
       inChannels[i] = x;
       }
       else
       {
       System.err.println("Invalid MIDI channel " + x + "\n");
       showDevices(mainClass, numMIDIOutput, numMIDIInput, midi, mixers, inputs, includesInput, true);
       System.exit(1);
       }                                       
       System.err.println("In MIDI " + i + ": " + inWrappers[i] + "Channel: " + inChannels[i]);
       }
                
       tuple = new Midi.Tuple(inWrappers, inChannels, outWrappers, outChannels);
       // ins have to be set up after the tuple
       for(int i = 0; i < numMIDIInput; i++)
       {
       ins[i] = new In(this, i);
       }

       if (mixers == null)
       {
       System.err.println("No output found which supports the desired sampling rate and bit depth\n");
       showDevices(mainClass, numMIDIOutput, numMIDIInput, midi, mixers, inputs, includesInput, true);
       System.exit(1);
       }
       else
       {
       int x = getInt(args[pos++]);
       if (x >= 0 && x < mixers.length)
       {
       synth.setMixer(mixers[x]);
       System.err.println("Output Audio: " + mixers[x].getName());  
                                        
       if (includesInput)
       {
       if (inputs == null)
       {
       System.err.println("No input found which supports the desired sampling rate and bit depth\n");
       showDevices(mainClass, numMIDIOutput, numMIDIInput, midi, inputs, inputs, includesInput, true);
       System.exit(1);
       }
       else
       {
       x = getInt(args[pos++]);
       if (x >= 0 && x < inputs.length)
       {
       synth.setInput(inputs[x]);
       System.err.println("Input Audio: " + inputs[x].getName());
       }
       }  
       } 
       }
       else
       {
       System.err.println("Invalid Audio number " + x + "\n");
       showDevices(mainClass, numMIDIOutput, numMIDIInput, midi, mixers, inputs, includesInput, true);
       System.exit(1);
       }
       }                             

       synth.setMidi(emitter);
       synth.setIncludesInput(includesInput);
       synth.setup();

       /// FIXME: synth.shutdown isn't synchronized with this
       Thread thread = new Thread(new Runnable()
       {
       public void run()
       {
       synth.go();
       }
       });
       thread.setName("Seq Synthesizer");
       thread.setDaemon(true);
       thread.start();
       }
       else
       {
       System.err.println("Invalid argument format\n");
       showDevices(mainClass, numMIDIOutput, numMIDIInput, midi, mixers, inputs, includesInput, true);
       System.exit(1);
       }
        
       System.err.println("Should not be reachable -- WARNING");
       System.exit(1);
       }
       finally
       {
       lock.unlock();  
       }
       }
    */
        
    public void setupForMIDI()
        {
        lock.lock();
        try
            {
            outs = new Out[NUM_OUTS];
            ins = new In[NUM_INS];
            midi = new Midi(NUM_OUTS, NUM_INS);

            // eliminate old receivers, else we'll have duplicate messages!
            for (Object indevW : midi.getInDevices())
                {
                if (indevW instanceof Midi.MidiDeviceWrapper)
                    {
                    ((Midi.MidiDeviceWrapper)indevW).removeAllFromTransmitter();
                    }
                }
                        
            // eliminate old receivers, else we'll have duplicate messages!
            for (Object outdevW : midi.getOutDevices())             // necessary?
                {
                if (outdevW instanceof Midi.MidiDeviceWrapper)
                    {
                    ((Midi.MidiDeviceWrapper)outdevW).removeAllFromTransmitter();
                    }
                }

            Midi.MidiDeviceWrapper[] outWrappers = new Midi.MidiDeviceWrapper[NUM_OUTS];
            int[] outChannels = new int[NUM_OUTS];
            int[] inChannels = new int[NUM_INS];
            Midi.MidiDeviceWrapper[] inWrappers = new Midi.MidiDeviceWrapper[NUM_INS];
            String[] inNames = new String[NUM_INS];
            String[] outNames = new String[NUM_OUTS];
            
            // Initially all NULL.  We gotta set this up with something smarter
            tuple = new Midi.Tuple(inWrappers, inChannels, outWrappers, outChannels, inNames, outNames);

            for(int i = 0; i < NUM_OUTS; i++)
                {
                outChannels[i] = 1;             // minimum value
                outs[i] = new Out(this, i);
                }
            for(int i = 0; i < NUM_INS; i++)
                {
                inChannels[i] = 0;
                ins[i] = new In(this, i);
                }
                
            tuple = Midi.loadTupleFromPreferences(this, ins);
            }
        finally
            {
            lock.unlock();
            }
        }
                
    /** Convenience method to set up the sequencer to use MIDI as input and output from the command line. */
    public void setupForMIDI(Class mainClass, String[] args, int numMIDIInput, int numMIDIOutput) 
        {
        lock.lock();
        try
            {
            outs = new Out[numMIDIOutput];
            ins = new In[numMIDIInput];
            midi = new Midi(numMIDIOutput, numMIDIInput);

            // eliminate old receivers, else we'll have duplicate messages!
            for (Object indevW : midi.getInDevices())
                {
                if (indevW instanceof Midi.MidiDeviceWrapper)
                    {
                    ((Midi.MidiDeviceWrapper)indevW).removeAllFromTransmitter();
                    }
                }
                        
            if (args.length == 0) 
                {
                showDevices(mainClass, numMIDIOutput, numMIDIInput, midi, null, null, false, false);
                System.exit(0);
                }
            else if (args.length == (numMIDIInput * 2 + numMIDIOutput * 2))
                {
                int pos = 0;

                ArrayList<Midi.MidiDeviceWrapper> out = midi.getOutDevices();
                int[] outChannels = new int[numMIDIOutput];
                Midi.MidiDeviceWrapper[] outWrappers = new Midi.MidiDeviceWrapper[numMIDIOutput];
                        
                for(int i = 0; i < numMIDIOutput; i++)
                    {
                    int x = getInt(args[pos++]);
                    if (x >= 0 && x < out.size()) 
                        {
                        outWrappers[i] = (out.get(x));
                        }
                    else 
                        {
                        System.err.println("Invalid MIDI number " + x + "\n");
                        showDevices(mainClass, numMIDIOutput, numMIDIInput, midi, null, null, false, false);
                        System.exit(1);
                        }
                    x = getInt(args[pos++]);
                    if (x >= 1 && x <= 16)
                        {
                        outChannels[i] = x;
                        }
                    else
                        {
                        System.err.println("Invalid MIDI channel " + x + "\n");
                        showDevices(mainClass, numMIDIOutput, numMIDIInput, midi, null, null, false, false);
                        System.exit(1);
                        }                                       
                    outs[i] = new Out(this, i);
                    System.err.println("Out MIDI " + i + ": " + outWrappers[i] + "Channel: " + outChannels[i]);
                    }

                ArrayList<Midi.MidiDeviceWrapper> in = midi.getInDevices();
                int[] inChannels = new int[numMIDIInput];
                Midi.MidiDeviceWrapper[] inWrappers = new Midi.MidiDeviceWrapper[numMIDIInput];
                        
                for(int i = 0; i < numMIDIInput; i++)
                    {
                    int x = getInt(args[pos++]);
                    if (x >= 0 && x < in.size()) 
                        {
                        inWrappers[i] = (in.get(x));
                        }
                    else 
                        {
                        System.err.println("Invalid MIDI number " + x + "\n");
                        showDevices(mainClass, numMIDIOutput, numMIDIInput, midi, null, null, false, false);
                        System.exit(1);
                        }
                    x = getInt(args[pos++]);
                    if (x >= 0 && x <= 16)
                        {
                        inChannels[i] = x;
                        }
                    else
                        {
                        System.err.println("Invalid MIDI channel " + x + "\n");
                        showDevices(mainClass, numMIDIOutput, numMIDIInput, midi, null, null, false, false);
                        System.exit(1);
                        }                                       
                    System.err.println("In MIDI " + i + ": " + inWrappers[i] + "Channel: " + inChannels[i]);
                    }
                        
                String[] inNames = new String[NUM_INS];
                String[] outNames = new String[NUM_OUTS];

                tuple = new Midi.Tuple(inWrappers, inChannels, outWrappers, outChannels, inNames, outNames);
                // ins have to be set up after the tuple
                for(int i = 0; i < numMIDIInput; i++)
                    {
                    ins[i] = new In(this, i);
                    }
                }
            else
                {
                System.err.println("Invalid argument format\n");
                showDevices(mainClass, numMIDIOutput, numMIDIInput, midi, null, null, false, false);
                System.exit(1);
                }
            }
        finally
            {
            lock.unlock();
            }
        }

    // Parses an integer from a String.  Used by the methods above.
    static int getInt(String s) 
        {
        try {
            return (Integer.parseInt(s));
            } catch (NumberFormatException ex) 
            {
            return -1;
            }
        }

    // Displays all current available input and output MIDI and Audio devices
    static void showDevices(Class myClass, int numMidiOutput, int numMidiInput, Midi midi, 
        javax.sound.sampled.Mixer.Info[] mixers, javax.sound.sampled.Mixer.Info[] inputs, 
        boolean includesInput, boolean includesOutput)
        {
        midi.displayDevices();
        if (includesOutput)
            {
            System.err.println("\nAUDIO DEVICES:");
            for (int i = 0; i < mixers.length; i++)
                System.err.println("" + i + ":\t" + mixers[i].getName());
            }
        if (includesInput)
            {
            System.err.println("\nINPUT DEVICES:");
            for (int i = 0; i < inputs.length; i++)
                System.err.println("" + i + ":\t" + inputs[i].getName());
            }
        System.err.println("\nMIDI CHANNELS:");
        System.err.println("\tOutput: 1 ... 16");
        if (includesInput)
            System.err.println("\t Input: 0 ... 16  [0 = Any Channel]");

        String error = "\nFormat:\n\tjava " + myClass + "\t\t\t[displays available devices]\n\n\tjava " + myClass + " \\\n";
        error += "\t\t";
        for(int i = 0; i < numMidiOutput; i++)
            error += ("[midi out " + (i + 1) + "] [chan out " + (i + 1) + "] ");
        if (numMidiInput > 0) error += "\\\n\t\t";
        for(int i = 0; i < numMidiInput; i++)
            error += ("[midi in " + (i + 1) + "] [chan in " + (i + 1) + "] ");
        if (includesOutput || includesInput)
            {
            error += "\\\n\t\t";
            error += (includesOutput ? "[audio out] " : "") + (includesInput ? "[audio in]" : "");
            }
        System.err.println(error);
        }
        
    // This is the document counter.  It increases every time we call SeqUI.doNew() or SeqUI.doLoad(), but not SeqUI.doMerge().
    // Motifs use it to determine whether to reset their numbering back to 1.
    static int document = 0;
    /** Increments the document counter.  Only SeqUI.doNew() or SeqUI.doLoad() should do this. */
    public static void incrementDocument() { document++; }
    /** Checks the document counter.  If it's larger than the motif's internal document counter, 
        it knows that we have a new document, so the motif's motif counter should be reset. */
    public static int getDocument() { return document; }
    }

