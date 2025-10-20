/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.stepsequence;

import seq.engine.*;
import seq.util.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

public class StepSequence extends Motif
    {
    private static final long serialVersionUID = 1;

    int version = 0;
        
    public static final int MAX_GAIN = 4;
    public static final double DEFAULT_SWING = 0.0;
    public static final int DEFAULT_NUM_STEPS = 16;
    public static final int DEFAULT_NUM_TRACKS = 16;
    public static final int DEFAULT_VELOCITY = 128;
    public static final int DEFAULT_LENGTH_IN_STEPS = DEFAULT_NUM_STEPS;
    public static final int COMBO_DEFAULT = -1;
    // public static final int TIE = 128;
    public static final int DEFAULT_TRACK_NOTE = 60;            // Middle C
    public static final int[] FLAMS = { 48, 24, 16, 12, 8, 6, 4, 3, 2, 1 };
    public static final int MAX_NUM_TRACKS = 32;
        
    // This value signifies that the default should be used rather than a numerical value
    public static final int DEFAULT = 0 - (Motif.NUM_PARAMETERS + 2);           // low enough to avoid the corrected parameter stuff
    public static final int DEFAULT_FLAM = -1;
    public static final int DEFAULT_WHEN = -1;
    
    public static final int TYPE_NOTE = 0;
    public static final int TYPE_CC = 1;
    public static final int TYPE_POLYPHONIC_AFTERTOUCH = 2;
    public static final int TYPE_CHANNEL_AFTERTOUCH = 3;
    public static final int TYPE_PITCH_BEND = 4;
    public static final int TYPE_PC = 5;
    public static final int TYPE_NRPN = 6;
    public static final int TYPE_RPN = 7;

    public static final boolean[][] WHEN = { 
        { true, false }, { false, true },
        { true, false, false }, { false, true, false }, { false, false, true }, { true, true, false }, { true, false, true }, { false, true, true },
        { true, false, false, false }, { false, true, false, false }, { false, false, true, false }, { false, false, false, true }, 
        { true, true, false, false }, { true, false, true, false }, { true, false, false, true }, { false, true, true, false }, { false, true, false, true }, { false, false, true, true },
        { true, true, true, false }, { true, false, true, true }, { true, true, false, true }, { false, true, true, true }};
                
    public static final int ALWAYS = 0;
    public static final int PROB_1 = 1;                             // 1/10 time on
    public static final int PROB_2 = 2;
    public static final int PROB_3 = 3;
    public static final int PROB_4 = 4;
    public static final int PROB_5 = 5;
    public static final int PROB_6 = 6;
    public static final int PROB_7 = 7;
    public static final int PROB_8 = 8;
    public static final int PROB_9 = 9;                             // 9/10 time on
    public static final int PROB_TRACK = 10;                        // 1/NumTracks time on
    public static final int PROB_OFF_TRACK = 11;                    // 1/NumTracks time on
    public static final int A1_2 = 12;                              // play #1 but not #2
    public static final int A2_2 = 13;
    public static final int A1_3 = 14;                              // play #1 but not #2 or #3
    public static final int A2_3 = 15;
    public static final int A3_3 = 16;
    public static final int A12_3 = 17;                             // play #1 and #2 but not #3
    public static final int A13_3 = 18;
    public static final int A23_3 = 19;
    public static final int A1_4 = 20;
    public static final int A2_4 = 21;
    public static final int A3_4 = 22;
    public static final int A4_4 = 23;
    public static final int A12_4 = 24;
    public static final int A13_4 = 25;
    public static final int A14_4 = 26;
    public static final int A23_4 = 27;                             
    public static final int A24_4 = 28;
    public static final int A34_4 = 29;                             // play #3 and #4 but not #1 and #2 etc.
    public static final int A123_4 = 30;
    public static final int A134_4 = 31;
    public static final int A124_4 = 32;
    public static final int A234_4 = 33;
    public static final int MAX_WHEN = 33;
         
    // What's the point of writing out the notes?  Well, my plan is/was to list a bunch
    // of scales below....
    public static final int C = 0;
    public static final int C_S = 1;
    public static final int D = 2;
    public static final int D_S = 3;
    public static final int E = 4;
    public static final int F = 5;
    public static final int F_S = 6;
    public static final int G = 7;
    public static final int G_S = 8;
    public static final int A = 9;
    public static final int A_S = 10;
    public static final int B = 11;
    
    // Default scale when one isn't provided
    public static final int[] CHROMATIC_SCALE = { C, C_S, D, D_S, E, F, F_S, G, G_S, A, A_S, B };
    
    int type = TYPE_NOTE;
    
    double defaultSwing = 0;                // range 0.0 ... 1.0
    int defaultVelocity = 127;              // range 0...127, but but note that velocity = 0 ought to be interpreted as velocity = 1
    int defaultValueLSB = 0;
    int defaultOut = 0;                     // range 0...Seq.getNumOuts()-1
    int initialNumSteps = DEFAULT_NUM_STEPS;
    
    boolean[][] on;
    int[][] notes;    // range -1 (DEFAULT) or 0 ... 127 where 0 is NONE
    int[][] paramLSBs;    // range -1 (DEFAULT) or 0 ... 127 where 0 is NONE
    int[][] velocities;    // range -1 (DEFAULT) or 0 ... 127 where 0 is NONE
    int[][] valueLSBs;    // range -1 (DEFAULT) or 0 ... 127 where 0 is NONE
    int[][] flams;                 // range -1 (DEFAULT) or 0, 1, 2, ..., 10 representing no flams (one note), 2 notes, 3, 4, 6, 8, 12, 16, 24, and 48
    int[][] when;                       // -1 (DEFAULT) or EXCLUSIVE ... A234_4 inclusive
    double[] trackSwings;  // range -1 (DEFAULT) or 0.0 ... 1.0
    int[] trackVelocities; // range -1...127 where 0 is OFF and -1 is DEFAULT
    int[] trackValueLSBs;
    int[] trackOuts;               // range -1 ... Seq.getNumOuts()-1 where -1 is DEFAULT
    int[] trackFlams;              // range 0, 1, 2, ..., 10 representing no flams (one note), 2 notes, 3, 4, 6, 8, 12, 16, 24, and 48
    int[] trackNotes;              // range 0...127 
    int[] trackParamLSBs;              // range 0...127 
    int[] trackChokes;             // range 0...(trackChokes.length) where 0 is NONE and x is trackChokes[x-1]
    boolean[] trackExclusive;  
    int exclusivePattern;         
    double[] trackGains;   // range 0 ... MAX_GAIN
    int[] trackWhen;
    String[] trackNames;
    boolean[] trackMuted;
    boolean[] trackSoloed;
    boolean aTrackSoloed = false;
    boolean[] trackLearning;
    boolean armed;
    int in;
    int controlIn;
    int controlOut;
    int controlDevice;
    
    public boolean isArmed() { return armed; }
    public void setArmed(boolean val) { armed = val; }
    

    public int getIn() { return in; }
    public void setIn(int val) { in = val; Prefs.setLastInDevice(0, val, "seq.motif.stepsequence.StepSequence"); }

    // These values go 0...16 corresponding to NONE and MIDI channels 1...16
    public int getControlIn() { return controlIn; }
    public void setControlIn(int val) { controlIn = val; Prefs.setLastGridIn(0, val, "seq.motif.stepsequence.StepSequence"); }

    // These values go 0...16 corresponding to NONE and MIDI channels 1...16
    public int getControlOut() { return controlOut; }
    public void setControlOut(int val) { controlOut = val; Prefs.setLastGridOut(0, val, "seq.motif.stepsequence.StepSequence"); }
            
    public int getControlDevice() { return controlDevice; }
    public void setControlDevice(int val) { controlDevice = val; Prefs.setLastGridDevice(0, val, "seq.motif.stepsequence.StepSequence"); }
            
            
    /** WARNING: You must rebuild the playing clip after calling this at least once. */
    void swapTracks(int x, int y)
        {
        // we should have had each track be an object...
        
        boolean[] m = on[x];
        on[x] = on[y];
        on[y] = m;
        int[] a = notes[x];
        notes[x] = notes[y];
        notes[y] = a;
        a = velocities[x];
        velocities[x] = velocities[y];
        velocities[y] = a;
        a = flams[x];
        flams[x] = flams[y];
        flams[y] = a;
        a = when[x];
        when[x] = when[y];
        when[y] = a;
        double d = trackSwings[x];
        trackSwings[x] = trackSwings[y];
        trackSwings[y] = d;
        int i = trackVelocities[x];
        trackVelocities[x] = trackVelocities[y];
        trackVelocities[y] = i;
        i = trackOuts[x];
        trackOuts[x] = trackOuts[y];
        trackOuts[y] = i;
        i = trackFlams[x];
        trackFlams[x] = trackFlams[y];
        trackFlams[y] = i;
        i = trackNotes[x];
        trackNotes[x] = trackNotes[y];
        trackNotes[y] = i;
        i = trackChokes[x];
        trackChokes[x] = trackChokes[y];
        trackChokes[y] = i;
        boolean b = trackExclusive[x];
        trackExclusive[x] = trackExclusive[y];
        trackExclusive[y] = b;
        d = trackGains[x];
        trackGains[x] = trackGains[y];
        trackGains[y] = d;
        i = trackWhen[x];
        trackWhen[x] = trackWhen[y];
        trackWhen[y] = i;
        String s = trackNames[x];
        trackNames[x] = trackNames[y];
        trackNames[y] = s;
        b = trackMuted[x];
        trackMuted[x] = trackMuted[y];
        trackMuted[y] = b;
        b = trackSoloed[x];
        trackSoloed[x] = trackSoloed[y];
        trackSoloed[y] = b;
        b = trackLearning[x];
        trackLearning[x] = trackLearning[y];
        trackLearning[y] = b;

        a = paramLSBs[x];
        paramLSBs[x] = paramLSBs[y];
        paramLSBs[y] = a;

        a = valueLSBs[x];
        valueLSBs[x] = valueLSBs[y];
        valueLSBs[y] = a;

        i = trackParamLSBs[x];
        trackParamLSBs[x] = trackParamLSBs[y];
        trackParamLSBs[y] = i;

        i = trackValueLSBs[x];
        trackValueLSBs[x] = trackValueLSBs[y];
        trackValueLSBs[y] = i;
        }
        
    public void addTrack(int after)         // after can be -1
        {
        setNumTracks(getNumTracks() + 1);
        // Push all tracks up starting at after + 1
        // This should bubble down the very top (new empty) track
        // to the position after + 1
        for(int i = getNumTracks() - 1; i >= after + 2; i--)
            {
            swapTracks(i, i - 1);
            }
        incrementVersion();
        }

    public void copyTrack(int after)            // after must be >= 0
        {
        addTrack(after);
        copyTrack(after, after + 1);
        // Don't need to incrementVersion, it was done in addTrack()
        }

    public void removeTrack(int at)
        {
        // Push the track to the top
        for(int i = at; i <= getNumTracks() - 2; i++)
            {
            swapTracks(i, i + 1);
            }
        setNumTracks(getNumTracks() - 1);
        incrementVersion();
        }
        
    public void moveTrack(int at, int after)
        {
        if (at > after + 1)
            {
            for(int i = at; i > after + 1; i--)
                {
                swapTracks(i, i - 1);
                }
            }
        else if (at < after)
            {
            for(int i = at; i < after; i++)
                {
                swapTracks(i, i + 1);
                }
            } 
        incrementVersion();
        }
        
    void copyTrack(int from, int to)
        {
        on[to] = copy(on[from]);
        notes[to] = copy(notes[from]);
        velocities[to] = copy(velocities[from]);
        flams[to] = copy(flams[from]);
        notes[to] = copy(notes[from]);
        trackSwings[to] = trackSwings[from];
        trackVelocities[to] = trackVelocities[from];
        trackOuts[to] = trackOuts[from];
        trackFlams[to] = trackFlams[from];
        trackNotes[to] = trackNotes[from];
        trackChokes[to] = trackChokes[from];
        trackGains[to] = trackGains[from];
        trackWhen[to] = trackWhen[from];
        trackNames[to] = trackNames[from];
        trackMuted[to] = trackMuted[from];
        trackLearning[to] = trackLearning[from];
        trackSoloed[to] = trackSoloed[from];
        when[to] = when[from];
        trackExclusive[to] = trackExclusive[from];
        paramLSBs[to] = copy(paramLSBs[from]);
        valueLSBs[to] = copy(paramLSBs[from]);
        trackParamLSBs[to] = trackParamLSBs[from];
        trackValueLSBs[to] = trackValueLSBs[from];
        }

    public Motif copy()
        {
        StepSequence other = (StepSequence)(super.copy());
        other.on = copy(on);
        other.notes = copy(notes);
        other.velocities = copy(velocities);
        other.flams = copy(flams);
        other.notes = copy(notes);
        other.trackSwings = copy(trackSwings);
        other.trackVelocities = copy(trackVelocities);
        other.trackOuts = copy(trackOuts);
        other.trackFlams = copy(trackFlams);
        other.trackNotes = copy(trackNotes);
        other.trackChokes = copy(trackChokes);
        other.trackGains = copy(trackGains);
        other.trackWhen = copy(trackWhen);
        other.trackNames = copy(trackNames);
        other.trackMuted = copy(trackMuted);
        other.trackLearning = copy(trackLearning);
        other.trackSoloed = copy(trackSoloed);
        other.when = copy(when);
        other.trackExclusive = copy(trackExclusive);
        other.paramLSBs = copy(paramLSBs);
        other.valueLSBs = copy(valueLSBs);
        other.trackParamLSBs = copy(trackParamLSBs);
        other.trackValueLSBs = copy(trackValueLSBs);
        return other;
        }
    
    void setup(int[] lengths, int steps)
        {
        defaultOut = Prefs.getLastOutDevice(0, "seq.motif.stepsequence.StepSequence");
        in = Prefs.getLastInDevice(0, "seq.motif.stepsequence.StepSequence");
        controlIn = Prefs.getLastGridIn(0, "seq.motif.stepsequence.StepSequence");
        controlOut = Prefs.getLastGridOut(0, "seq.motif.stepsequence.StepSequence");
        controlDevice = Prefs.getLastGridDevice(0, "seq.motif.stepsequence.StepSequence");

        setLengthInSteps(steps);
        int numTracks = lengths.length;
        on = new boolean[numTracks][];
        velocities = new int[numTracks][];
        notes = new int[numTracks][];
        flams = new int[numTracks][];
        when = new int[numTracks][];
        paramLSBs = new int[numTracks][];
        valueLSBs = new int[numTracks][];
        for(int i = 0; i < velocities.length; i++)
            {
            on[i] = new boolean[lengths[i]];
            velocities[i] = new int[lengths[i]];
            Arrays.fill(velocities[i], DEFAULT);
            flams[i] = new int[lengths[i]];
            Arrays.fill(flams[i], COMBO_DEFAULT);
            notes[i] = new int[lengths[i]];
            Arrays.fill(notes[i], DEFAULT);
            when[i] = new int[lengths[i]];
            Arrays.fill(when[i], COMBO_DEFAULT);
            paramLSBs[i] = new int[lengths[i]];
            Arrays.fill(paramLSBs[i], DEFAULT);
            valueLSBs[i] = new int[lengths[i]];
            Arrays.fill(valueLSBs[i], DEFAULT);
            }
                        
        trackSwings = new double[numTracks];
        Arrays.fill(trackSwings, DEFAULT);
        trackVelocities = new int[numTracks];
        Arrays.fill(trackVelocities, DEFAULT);
        trackFlams = new int[numTracks];
        trackNotes = new int[numTracks];
        for(int i = 0; i < trackNotes.length; i++)
            {
            trackNotes[i] = DEFAULT_TRACK_NOTE + i;
            }
//        Arrays.fill(trackNotes, DEFAULT_TRACK_NOTE);
        trackChokes = new int[numTracks];               // default is 0 = NONE
        trackGains = new double[numTracks];
        Arrays.fill(trackGains, 1.0);
        trackMuted = new boolean[numTracks];    // default is FALSE
        trackLearning = new boolean[numTracks];    // default is FALSE
        trackSoloed = new boolean[numTracks];   // default is FALSE
        trackOuts = new int[numTracks];
        Arrays.fill(trackOuts, COMBO_DEFAULT); // Prefs.getLastOutDevice(1, "seq.motif.stepsequence.StepSequence", COMBO_DEFAULT));
        trackNames = new String[numTracks];
        Arrays.fill(trackNames, "");
        trackWhen = new int[numTracks];
        Arrays.fill(trackWhen, ALWAYS);
        trackExclusive = new boolean[numTracks];
        trackParamLSBs = new int[numTracks];
        trackValueLSBs = new int[numTracks];
        Arrays.fill(trackValueLSBs, DEFAULT);
        type = TYPE_NOTE;
        }
        
    public StepSequence(Seq seq)
        {
        this(seq, DEFAULT_NUM_TRACKS, DEFAULT_NUM_STEPS);
        }

    public StepSequence(Seq seq, int numTracks, int steps)
        {
        super(seq);
        int[] lengths = new int[numTracks];
        Arrays.fill(lengths, steps);
        setup(lengths, steps);
        }

    public StepSequence(Seq seq, int[] lengths, int steps)
        {
        super(seq);
        setup(lengths, steps);
        }

    int steps = DEFAULT_NUM_STEPS;
        
    public int getLengthInSteps()
        {
        // Length is in PPQ.  We assume that we are in 16th notes
        // return length * 4 / Seq.PPQ;
        return steps;
        }
        
    public int getLength()
        {
        return steps * Seq.PPQ / 4;
        }
                
    public void setLengthInSteps(int steps)
        {
        // Length is in PPQ.  We assume that we are in 16th notes
        // length = steps / 4 * Seq.PPQ;
        this.steps = steps;
        }
                
    public int getNumTracks() { return velocities.length; }
    void setNumTracks(int numTracks)
        {
        numTracks = Math.min(numTracks, MAX_NUM_TRACKS);
        int oldNumTracks = velocities.length;
        // First set up the new arrays with defaults
        boolean[][] _on = new boolean[numTracks][];
        int[][] _velocities = new int[numTracks][];
        int[][] _notes = new int[numTracks][];
        int[][] _flams = new int[numTracks][];
        int[][] _when = new int[numTracks][];
        int[][] _paramLSBs = new int[numTracks][];
        int[][] _valueLSBs = new int[numTracks][];
        for(int i = 0; i < Math.min(oldNumTracks,numTracks); i++) //Copy the tracks (length) that did exist
            {
            _on[i] = new boolean[velocities[i].length];
            _velocities[i] = new int[velocities[i].length];
            Arrays.fill(_velocities[i], DEFAULT);
            _flams[i] = new int[flams[i].length];
            Arrays.fill(_flams[i], COMBO_DEFAULT);
            _notes[i] = new int[notes[i].length];
            Arrays.fill(_notes[i], DEFAULT);
            _when[i] = new int[when[i].length];
            Arrays.fill(_when[i], COMBO_DEFAULT);
            _paramLSBs[i] = new int[when[i].length];
            Arrays.fill(_paramLSBs[i], DEFAULT);
            _valueLSBs[i] = new int[when[i].length];
            Arrays.fill(_valueLSBs[i], DEFAULT);
            }
        for(int i = oldNumTracks; i < numTracks; i++)           //if increasing tracks, create empty ones
            {
            _on[i] = new boolean[initialNumSteps];
            _velocities[i] = new int[initialNumSteps];
            Arrays.fill(_velocities[i], DEFAULT);
            _flams[i] = new int[initialNumSteps];
            Arrays.fill(_flams[i], 0);
            _notes[i] = new int[initialNumSteps];
            Arrays.fill(_notes[i], DEFAULT);
            _when[i] = new int[initialNumSteps];
            Arrays.fill(_when[i], COMBO_DEFAULT);
            _paramLSBs[i] = new int[initialNumSteps];
            Arrays.fill(_paramLSBs[i], DEFAULT);
            _valueLSBs[i] = new int[initialNumSteps];
            Arrays.fill(_valueLSBs[i], DEFAULT);
            }
                        
        double[] _trackSwings = new double[numTracks];
        Arrays.fill(_trackSwings, DEFAULT);
        int[] _trackVelocities = new int[numTracks];
        Arrays.fill(_trackVelocities, DEFAULT);
        int[] _trackFlams = new int[numTracks];
        int[] _trackNotes = new int[numTracks];
        Arrays.fill(_trackNotes, DEFAULT_TRACK_NOTE);
        int[] _trackChokes = new int[numTracks];               // default is 0 = NONE
        double[] _trackGains = new double[numTracks];
        Arrays.fill(_trackGains, 1.0);
        boolean[] _trackMuted = new boolean[numTracks];    // default is FALSE
        boolean[] _trackLearning = new boolean[numTracks];    // default is FALSE
        boolean[] _trackSoloed = new boolean[numTracks];   // default is FALSE
        int[] _trackOuts = new int[numTracks];
        Arrays.fill(_trackOuts, COMBO_DEFAULT);
        String[] _trackNames = new String[numTracks];
        Arrays.fill(_trackNames, "");
        int[] _trackWhen = new int[numTracks];
        Arrays.fill(_trackWhen, ALWAYS);
        boolean[] _trackExclusive = new boolean[numTracks];
        int[] _trackParamLSBs = new int[numTracks];
        Arrays.fill(_trackParamLSBs, DEFAULT);
        int[] _trackValueLSBs = new int[numTracks];
        Arrays.fill(_trackValueLSBs, DEFAULT);
        
        // overwrite defaults with new values
        copyTo(on, _on);
        copyTo(velocities, _velocities);
        copyTo(notes, _notes);
        copyTo(flams, _flams);
        copyTo(when, _when);
        copyTo(paramLSBs, _paramLSBs);
        copyTo(valueLSBs, _valueLSBs);
        copyTo(trackSwings, _trackSwings);
        copyTo(trackVelocities, _trackVelocities);
        copyTo(trackFlams, _trackFlams);
        copyTo(trackNotes, _trackNotes);
        copyTo(trackChokes, _trackChokes);
        copyTo(trackGains, _trackGains);
        copyTo(trackMuted, _trackMuted);
        copyTo(trackLearning, _trackLearning);
        copyTo(trackSoloed, _trackSoloed);
        copyTo(trackOuts, _trackOuts);
        copyTo(trackNames, _trackNames);
        copyTo(trackWhen, _trackWhen);
        copyTo(trackExclusive, _trackExclusive);
        copyTo(trackParamLSBs, _trackParamLSBs);
        copyTo(trackValueLSBs, _trackValueLSBs);
        
        // set
        on = _on;
        velocities = _velocities;
        notes = _notes;
        flams = _flams;
        when = _when;
        paramLSBs = _paramLSBs;
        valueLSBs = _valueLSBs;
        trackSwings = _trackSwings;
        trackVelocities = _trackVelocities;
        trackFlams = _trackFlams;
        trackNotes = _trackNotes;
        trackChokes = _trackChokes;
        trackGains = _trackGains;
        trackMuted = _trackMuted;
        trackLearning = _trackLearning;
        trackSoloed = _trackSoloed;
        trackOuts = _trackOuts;
        trackNames = _trackNames;
        trackWhen = _trackWhen;
        trackExclusive = _trackExclusive;
        trackParamLSBs = _trackParamLSBs;
        trackValueLSBs = _trackValueLSBs;
        }

    public int getInitialNumSteps() { return initialNumSteps; }
    public void setInitialNumSteps(int val) { initialNumSteps = val; }

    // Track Length
    public int getNumSteps(int track) { return velocities[track].length; }
    public void setNumSteps(int track, int len) 
        {
        boolean[] newOn = new boolean[len];
        System.arraycopy(on[track], 0, newOn, 0, Math.min(on[track].length, newOn.length));
        on[track] = newOn;
        int[] newVelocities = new int[len];
        Arrays.fill(newVelocities, DEFAULT);
        System.arraycopy(velocities[track], 0, newVelocities, 0, Math.min(velocities[track].length, newVelocities.length)); 
        velocities[track] = newVelocities;
        int[] newFlams = new int[len];
        System.arraycopy(flams[track], 0, newFlams, 0, Math.min(flams[track].length, newFlams.length)); 
        flams[track] = newFlams;
        int[] newWhen = new int[len];
        Arrays.fill(newWhen, COMBO_DEFAULT);
        System.arraycopy(when[track], 0, newWhen, 0, Math.min(when[track].length, newWhen.length)); 
        when[track] = newWhen;
        int[] newNotes = new int[len];
        Arrays.fill(newNotes, DEFAULT);
        System.arraycopy(notes[track], 0, newNotes, 0, Math.min(notes[track].length, newNotes.length));
        notes[track] = newNotes;
        int[] newParamLSBs = new int[len];
        Arrays.fill(newParamLSBs, DEFAULT);
        System.arraycopy(paramLSBs[track], 0, newParamLSBs, 0, Math.min(paramLSBs[track].length, newParamLSBs.length));
        paramLSBs[track] = newParamLSBs;
        int[] newValueLSBs = new int[len];
        Arrays.fill(newValueLSBs, DEFAULT);
        System.arraycopy(valueLSBs[track], 0, newValueLSBs, 0, Math.min(valueLSBs[track].length, newValueLSBs.length));
        valueLSBs[track] = newValueLSBs;

        incrementVersion();
        }
        
    public void setAllNumSteps(int len)
        {
        for(int i = 0; i<this.getNumTracks(); i++)
            {
            setNumSteps(i,len);
            }
        incrementVersion();
        }
                        
    // Default Setters
    public void setDefaultVelocity(int val) { defaultVelocity = val; }
    public int getDefaultVelocity() { return defaultVelocity; }
    public void setDefaultValueLSB(int val) { defaultValueLSB = val; }
    public int getDefaultValueLSB() { return defaultValueLSB; }
    public void setDefaultSwing(double val) { defaultSwing = val; }
    public double getDefaultSwing() { return defaultSwing; }
    public void setDefaultOut(int val) { defaultOut = val; Prefs.setLastOutDevice(0, val, "seq.motif.stepsequence.StepSequence"); }
    public int getDefaultOut() { return defaultOut; }
    public boolean isATrackSoloed() { return aTrackSoloed; }
	public int getType() { return type; }
	public void setType(int val) { type = val; }

    // Track-Level Setters
    public String getTrackName(int track) { return trackNames[track]; }
    public void setTrackName(int track, String val) { trackNames[track] = val; }
    public double getTrackSwing(int track) { return trackSwings[track]; }
    public void setTrackSwing(int track, double val) { trackSwings[track] = val; }
    public double getTrackGain(int track) { return trackGains[track]; }
    public void setTrackGain(int track, double val) { trackGains[track] = val; }
    public double getFinalSwing(int track) { double swing = trackSwings[track]; if (swing == DEFAULT) return defaultSwing; else return swing; }
    public int getTrackVelocity(int track) { return trackVelocities[track]; }
    public void setTrackVelocity(int track, int val) { trackVelocities[track] = val; }
    public int getFinalVelocity(int track) 
        { 
        int velocity = trackVelocities[track];
        if (velocity == DEFAULT) velocity = defaultVelocity;
        return velocity;
        }
    public int getFinalValueLSB(int track)
    	{
    	int lsb = trackValueLSBs[track];
    	if (lsb == DEFAULT) lsb = defaultValueLSB;
    	return lsb;
    	}
    public int getTrackOut(int track) { return trackOuts[track]; }
    public void setTrackOut(int track, int val) { trackOuts[track] = val; /* Prefs.setLastOutDevice(1, val, "seq.motif.stepsequence.StepSequence"); */ }
    public int getFinalOut(int track) { int out = trackOuts[track]; if (out == COMBO_DEFAULT) return defaultOut; else return out; }
    public int getTrackFlam(int track) { return trackFlams[track]; }
    public void setTrackFlam(int track, int val) { trackFlams[track] = val; }
    public int getTrackNote(int track) { return trackNotes[track]; }
    public void setTrackNote(int track, int val) { trackNotes[track] = val; }
    public int getTrackChoke(int track) { return trackChokes[track]; }
    public void setTrackChoke(int track, int val) { trackChokes[track] = val; }
    public void setTrackWhen(int track, int val) { trackWhen[track] = val; }
    public int getTrackWhen(int track) { return trackWhen[track]; }
    public boolean isTrackExclusive(int track) { return trackExclusive[track]; }
    public void setTrackExclusive(int track, boolean val) { trackExclusive[track] = val; }
    public boolean isTrackMuted(int track) { return trackMuted[track]; }
    public void setTrackMuted(int track, boolean val) { trackMuted[track] = val; }
    public boolean isTrackLearning(int track) { return trackLearning[track]; }
    public void setTrackLearning(int track, boolean val) { trackLearning[track] = val; }
    public boolean isTrackSoloed(int track) { return trackSoloed[track]; }
    public int getTrackParamLSB(int track) { return trackParamLSBs[track]; }
    public void setTrackParamLSB(int track, int val) { trackParamLSBs[track] = val; }
    public int getTrackValueLSB(int track) { return trackValueLSBs[track]; }
    public void setTrackValueLSB(int track, int val) { trackValueLSBs[track] = val; }
    
    public void setTrackSoloed(int track, boolean val)
        {
        trackSoloed[track] = val;
        if (val) { aTrackSoloed = true; }
        else 
            { 
            aTrackSoloed = false; 
            for(int i = 0; i < trackSoloed.length; i++) 
                { 
                if (trackSoloed[i]) 
                    { 
                    aTrackSoloed = true; 
                    break;
                    }
                }
            }
        }
        
    public void clearTrack(int track)
        {
        for(int i = 0; i < on[track].length; i++)
            {
            on[track][i] = false;
            }
        }

    // Per-note Setters
    public boolean isOn(int track, int step) { return on[track][step]; }
    public void setOn(int track, int step, boolean val) { on[track][step] = val; }
    public int getVelocity(int track, int step) { return velocities[track][step]; }
    public void setVelocity(int track, int step, int val) { velocities[track][step] = val; }
    public void setVelocity(int track, int step, int val, boolean on) { setVelocity(track, step, val); setOn(track, step, on); }
    public int getFinalVelocity(int track, int step) { int velocity = velocities[track][step]; return (velocity == DEFAULT) ? getFinalVelocity(track) : velocity; }
    //public int getPlayVelocity(int track, int step) { return (int)((getFinalVelocity(track,step) * trackGains[track])); }
    public int getFlam(int track, int step) { return flams[track][step]; }
    public void setFlam(int track, int step, int val) { flams[track][step] = val; }
    public int getFinalFlam(int track, int step) { int flam = flams[track][step]; return (flam == COMBO_DEFAULT) ? trackFlams[track] : flam; }
    public int getWhen(int track, int step) { return when[track][step]; }
    public void setWhen(int track, int step, int val) { when[track][step] = val; }
    public int getNote(int track, int step) { return notes[track][step]; }
    public void setNote(int track, int step, int val) { notes[track][step] = val; }
    public int getFinalNote(int track, int step) { int note = notes[track][step]; return (note == DEFAULT) ? trackNotes[track] : note; }
    public int getFinalWhen(int track, int step) { int w = when[track][step]; return (w == COMBO_DEFAULT) ? trackWhen[track] : w; }
    public int getParamLSB(int track, int step) { return paramLSBs[track][step]; }
    public void setParamLSB(int track, int step, int val) { paramLSBs[track][step] = val; }
    public int getFinalParamLSB(int track, int step) { int lsb = paramLSBs[track][step]; return (lsb == DEFAULT) ? trackParamLSBs[track] : lsb; }
    public int getValueLSB(int track, int step) { return valueLSBs[track][step]; }
    public void setValueLSB(int track, int step, int val) { valueLSBs[track][step] = val; }
    public int getFinalValueLSB(int track, int step) { int lsb = valueLSBs[track][step]; return (lsb == DEFAULT) ? getFinalValueLSB(track) : lsb; }

	public int getFullParam(int track, int step)
		{
		switch(type)
			{
			case TYPE_NOTE:
			case TYPE_CC:
			case TYPE_POLYPHONIC_AFTERTOUCH:
			return getFinalNote(track, step);

			case TYPE_NRPN:
			case TYPE_RPN:
			return getFinalNote(track, step) * 128 + getFinalParamLSB(track, step);

			case TYPE_CHANNEL_AFTERTOUCH:
			case TYPE_PITCH_BEND:
			case TYPE_PC:
			default:			// never happens
			return 0;
			}
		} 


	public int getFullValue(int track, int step)
		{
		switch(type)
			{
			case TYPE_NOTE:
			case TYPE_CC:
			case TYPE_POLYPHONIC_AFTERTOUCH:
			case TYPE_CHANNEL_AFTERTOUCH:
			case TYPE_PC:
			return getFinalVelocity(track, step);

			case TYPE_PITCH_BEND:
			return (getFinalVelocity(track, step) * 128 + getFinalValueLSB(track, step)) - 8192;

			case TYPE_NRPN:
			case TYPE_RPN:
			default:			// never happens
			return (getFinalVelocity(track, step) * 128 + getFinalValueLSB(track, step));
			}
		}
	    
    public boolean playNow(double invNumTracks, int track, int step, int iteration)
        {
        int w = getFinalWhen(track, step);
        if (w == ALWAYS) return true;
        else if (w < PROB_TRACK) return (ThreadLocalRandom.current().nextDouble(10.0) < w); //w is integer between 1 and 9 hence the bound
        else if (w == PROB_TRACK) return (ThreadLocalRandom.current().nextDouble() < invNumTracks);
        else if (w == PROB_OFF_TRACK) return !(ThreadLocalRandom.current().nextDouble() < invNumTracks);
        else return WHEN[w - A1_2][iteration % WHEN[w - A1_2].length];
        }
          
    public int randomWalk(int start, int min, int max, double probability, ThreadLocalRandom random)
        {
        if (min > max || start < min || start > max) throw new RuntimeException("Invalid start, min, max " + start + " " + min + " " + max);
        if (probability < 0 || probability >= 1) throw new RuntimeException("Invalid probability " + probability);
        if (min == max) return min;
        int val = start;
        while(random.nextDouble() < probability)
            {
            if (random.nextBoolean()) val++;
            else val--;
            if (val > max || val < min) val = start;        // reset, exceeded bounds
            }
        return start;
        }

    public int[] expandScale(int[] scale)
        {
        int[] full = new int[128];
        int fullPos = 0;
        int scalePos = 0;
        int octave = 0;
        while(true)
            {
            int nextFull = scale[scalePos++] + octave;
            if (nextFull >= 128) break;
            full[fullPos++] = nextFull;
            if (scalePos >= scale.length) { octave += 12; scalePos = 0; }
            }
        int[] last = new int[fullPos];
        System.arraycopy(full, 0, last, 0, fullPos);
        return last;
        }
                
        
    public void mutate(int track, double playWeight, double velocityWeight, double flamWeight, double noteWeight, int[] scale)
        {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int[] velocities = this.velocities[track];
        int[] flams = this.flams[track];
        int[] notes = this.notes[track];
        int trackVelocity = trackVelocities[track];
        if (playWeight > 0)
            {
            for(int i = 0; i < velocities.length; i++)
                {
                if (random.nextDouble() < playWeight)
                    {
                    if (velocities[i] == 0) 
                        {
                        velocities[i] = trackVelocity;
                        notes[i] = getTrackNote(track);
                        flams[i] = getTrackFlam(track);
                        }
                    else
                        {
                        velocities[i] = 0;
                        }
                    }
                }
            }
                        
        if (velocityWeight > 0)
            {
            for(int i = 0; i < velocities.length; i++)
                {
                if (velocities[i] > 0)
                    {
                    velocities[i] = randomWalk(velocities[i], 1, 127, velocityWeight, random);
                    }
                }
            }

        if (flamWeight > 0)
            {
            for(int i = 0; i < velocities.length; i++)
                {
                if (velocities[i] > 0)          // note velocities, not notes
                    {
                    flams[i] = randomWalk(flams[i], 0, 8, flamWeight, random);
                    }
                }
            }
        
        if (noteWeight > 0)
            {
            if (scale == null) scale = CHROMATIC_SCALE;
            scale = expandScale(scale);
                
            for(int i = 0; i < velocities.length; i++)
                {
                if (velocities[i] > 0)          // note velocities, not notes
                    {
                    /// FIXME: this can be done better with a binary search
                    
                    // Find the closest note in the scale
                    int baseNote = notes[i] % 12;
                    int closestIndex = 0;
                    for(int index = 1; index < scale.length; index++)
                        {
                        if (Math.abs(scale[index] - baseNote) < Math.abs(scale[closestIndex] - baseNote)) // found something closer
                            closestIndex = index;
                        }
                                        
                    closestIndex = randomWalk(closestIndex, 0, scale.length - 1, noteWeight, random);
                    notes[i] = scale[closestIndex];
                    }
                }
            }
        }
        
        

    /** Returns the note position in the track as a value between 0.0 and 1.0 */
    public double getNotePosition(int track, int step)
        {
        int numNotes = velocities[track].length;
        double baseLen = (1.0 / numNotes);
        double swing = getFinalSwing(track);
        return baseLen * step + swing * baseLen;
        }

    /** Returns the note positions in the track as a value between 0.0 and 1.0 */
    public double[] getNotePositions(int track)
        {
        int numNotes = velocities[track].length;
        double baseLen = (1.0 / numNotes);
        double swing = getFinalSwing(track);
        double[] pos = new double[numNotes];
        for(int i = 0; i < numNotes; i++)
            pos[i] = baseLen * i + swing * baseLen;
        return pos;
        }

    /** Returns the note length in the track as a value between 0.0 and 1.0 */
    public double getNoteLength(int track, int step)
        {
        int numNotes = velocities[track].length;
        double baseLen = (1.0 / numNotes);
        double swing = getFinalSwing(track);
        if (step / 2 == 1)  // swing note we presume
            {
            return baseLen - swing * baseLen;
            }
        else return baseLen;
        }

    /** Returns the note lengths in the track as a value between 0.0 and 1.0 */
    public double[] getNoteLengths(int track)
        {
        int numNotes = velocities[track].length;
        double baseLen = (1.0 / numNotes);
        double swing = getFinalSwing(track);
        double[] len = new double[numNotes];
        for(int i = 0; i < numNotes; i++)
            {
            if (i / 2 == 1)  // swing note we presume
                {
                len[i] = baseLen - swing * baseLen;
                }
            else len[i] = baseLen;
            }
        return len;
        }
                
    public int[] getVelocities(int track)
        {
        return (int[])(velocities[track].clone());
        }
                
    /** Returns the step, if any, corresponding to the hit position between 0.0 and 1.0, else -1 */
    public int getStep(int track, double hit)
        {
        int numNotes = velocities[track].length;
        int baseStep = (int)(numNotes * hit);
        // Now, due to swing, did we hit it or not?
        double notePosition = getNotePosition(track, baseStep);
        double noteLen = getNoteLength(track, baseStep);
        if (hit >= notePosition && hit < notePosition + noteLen)
            {
            return baseStep;
            }
        else return -1;
        }
        
    public Clip buildClip(Clip parent)
        {
        return new StepSequenceClip(seq, this, parent);
        }
        




    public void load(JSONObject obj) throws JSONException
        {
        setLengthInSteps(obj.optInt("lengthinsteps", DEFAULT_NUM_STEPS));
        setDefaultSwing(obj.optDouble("defaultswing", 0));
        setDefaultVelocity(obj.optInt("defaultvelocity", 128));
        setDefaultValueLSB(obj.optInt("defaultvaluelsb", 128));
        setDefaultOut(obj.optInt("defaultout", 0));
        setControlIn(obj.optInt("controlin", 0));
        setControlOut(obj.optInt("controlout", 0));
        setControlDevice(obj.optInt("controldevice", 0));
        aTrackSoloed = obj.getBoolean("atracksoloed");
        
        // Should these be made opt rather than get?
        on = JSONToBooleanArray2(obj.getJSONArray("on"));
        notes = JSONToIntArray2(obj.getJSONArray("notes"));
        velocities = JSONToIntArray2(obj.getJSONArray("velocities"));
        flams = JSONToIntArray2(obj.getJSONArray("flams"));
        when = JSONToIntArray2(obj.getJSONArray("when"));
        JSONArray aa = obj.optJSONArray("plsbs");
        if (aa != null) paramLSBs = JSONToIntArray2(aa);
        JSONArray a = obj.optJSONArray("vlsbs");
        if (aa != null) valueLSBs = JSONToIntArray2(aa);
        trackSwings = JSONToDoubleArray(obj.getJSONArray("trackswings"));
        trackGains = JSONToDoubleArray(obj.getJSONArray("trackgains"));
        trackExclusive = JSONToBooleanArray(obj.getJSONArray("trackexclusive"));
        trackMuted = JSONToBooleanArray(obj.getJSONArray("trackmuted"));
        for(int i = 0; i < trackLearning.length; i++) trackLearning[i] = false;         // clear out
        trackSoloed = JSONToBooleanArray(obj.getJSONArray("tracksoloed"));
        trackVelocities = JSONToIntArray(obj.getJSONArray("trackvelocities"));
        trackNotes = JSONToIntArray(obj.getJSONArray("tracknotes"));
        trackOuts = JSONToIntArray(obj.getJSONArray("trackouts"));
        trackFlams = JSONToIntArray(obj.getJSONArray("trackflams"));
        trackChokes = JSONToIntArray(obj.getJSONArray("trackchokes"));
        trackWhen = JSONToIntArray(obj.getJSONArray("trackwhen"));
        trackNames = JSONToStringArray(obj.getJSONArray("tracknames"));
        trackParamLSBs = JSONToIntArray(obj.getJSONArray("tplsbs"));
        trackValueLSBs = JSONToIntArray(obj.getJSONArray("tvlsbs"));
        }
        
    public void save(JSONObject obj) throws JSONException
        {
        obj.put("on", booleanToJSONArray2(on));
        obj.put("lengthinsteps", getLengthInSteps());
        obj.put("defaultswing", getDefaultSwing());
        obj.put("defaultvelocity", getDefaultVelocity());
        obj.put("defaultvaluelsb", getDefaultValueLSB());
        obj.put("defaultout", getDefaultOut());
        obj.put("controlin", getControlIn());
        obj.put("controlout", getControlOut());
        obj.put("controldevice", getControlDevice());
        obj.put("atracksoloed", isATrackSoloed());
        
        obj.put("notes", intToJSONArray2(notes));
        obj.put("velocities", intToJSONArray2(velocities));
        obj.put("flams", intToJSONArray2(flams));
        obj.put("when", intToJSONArray2(when));
        obj.put("plsbs", intToJSONArray2(paramLSBs));
        obj.put("vlsbs", intToJSONArray2(valueLSBs));
        obj.put("trackswings", doubleToJSONArray(trackSwings));
        obj.put("trackgains", doubleToJSONArray(trackGains));
        obj.put("trackexclusive", booleanToJSONArray(trackExclusive));
        obj.put("trackmuted", booleanToJSONArray(trackMuted));
        // Don't write out trackLearning
        obj.put("tracksoloed", booleanToJSONArray(trackSoloed));
        obj.put("trackvelocities", intToJSONArray(trackVelocities));
        obj.put("tracknotes", intToJSONArray(trackNotes));
        obj.put("trackouts", intToJSONArray(trackOuts));
        obj.put("trackflams", intToJSONArray(trackFlams));
        obj.put("trackchokes", intToJSONArray(trackChokes));
        obj.put("trackwhen", intToJSONArray(trackWhen));
        obj.put("tracknames", stringToJSONArray(trackNames));
        obj.put("tplsbs", intToJSONArray(trackParamLSBs));
        obj.put("tvlsbs", intToJSONArray(trackValueLSBs));
        }


    public static void copyTo(int[][] array, int[][] to) 
        {
        for(int i = 0; i < Math.min(array.length, to.length); i++)
            {
            copyTo(array[i], to[i]);
            }
        }

    public static void copyTo(double[][] array, double[][] to) 
        {
        for(int i = 0; i < Math.min(array.length, to.length); i++)
            {
            copyTo(array[i], to[i]);
            }
        }

    public static void copyTo(boolean[][] array, boolean[][] to) 
        {
        for(int i = 0; i < Math.min(array.length, to.length); i++)
            {
            copyTo(array[i], to[i]);
            }
        }

    public static void copyTo(int[] array, int[] to) 
        {
        System.arraycopy(array, 0, to, 0, Math.min(array.length, to.length));        
        }

    public static void copyTo(double[] array, double[] to) 
        {
        System.arraycopy(array, 0, to, 0, Math.min(array.length, to.length));        
        }

    public static void copyTo(boolean[] array, boolean[] to) 
        {
        System.arraycopy(array, 0, to, 0, Math.min(array.length, to.length));        
        }

    public static void copyTo(String[] array, String[] to) 
        {
        System.arraycopy(array, 0, to, 0, Math.min(array.length, to.length));        
        }
        
    static int document = 0;
    static int counter = 1;
    public int getNextCounter() { if (document < Seq.getDocument()) { document = Seq.getDocument(); counter = 1; } return counter++; }
        
    public String getBaseName() { return "Step Sequence"; }
    
    
    public void rotate(int track, int rotate)
        {
        int len = on[track].length;
        boolean[] _on = new boolean[len];
        int[] _velocities = new int[len];
        int[] _flams = new int[len];
        int[] _when = new int[len];
        int[] _notes = new int[len];
        int[] _paramLSBs = new int[len];
        int[] _valueLSBs = new int[len];
        for(int i = 0; i < len; i++)
            {
            int pos = i + rotate;
            if (pos != 0) { pos = pos % len; }
            if (pos < 0) { pos += len; pos = pos % len; }
            _on[pos] = on[track][i];
            _velocities[pos] = velocities[track][i];
            _flams[pos] = flams[track][i];
            _notes[pos] = notes[track][i];
            _paramLSBs[pos] = paramLSBs[track][i];
            _valueLSBs[pos] = valueLSBs[track][i];
            }
        on[track] = _on;
        velocities[track] = _velocities;
        flams[track] = _flams;
        notes[track] = _notes;
        paramLSBs[track] = _paramLSBs;
        valueLSBs[track] = _valueLSBs;
        }
    

    /** Applies Euclidean Rhythm to the given track, using n = trackLength,
        k = (int)(n * ratioK), and rotate=(int)(n * ratioRotate).
    */
    public void applyEuclideanRhythm(int track, double ratioK, double ratioRotate)
        {
        on[track] = buildEuclideanRhythm(
            on[track].length,
            (int)(on[track].length * ratioK),
            (int)(on[track].length * ratioRotate));
        }
    
    /** Generates a Euclidean Rhythm sequence (true/false) of length n, 
        with exactly k "true" values, the rest being false.  rotate
        indicates the number of steps that the sequence should be rotated by
        after the fact -- this can be any integer but it only makes sense
        to do so for values of  -n < rotate < n.
        
        For more on Euclidean Rhythms, see https://en.wikipedia.org/wiki/Euclidean_rhythm
        
        Godfried Toussaint developed this method using Bjorklund’s algorithm,
        but tried to argue (unconvincingly to me) that it could be derived
        from Euclid's algorithm.  It's since been determined that the same
        results can be produced from Bressenham's Line algorithm, albeit with
        certain rotations (in fact, Toussaint's original algorithm required
        rotations to achieve the published results in several cases, something
        that was not disclosed).  
        
        The code below just does Bressenham's line algorithm using floats and
        floor rather than the cool all-integer way.
    */
    
    boolean[] buildEuclideanRhythm(int n, int k, int rotate)
        {
        // Check for validity, make initial array
        if (n < 0) return new boolean[0];
        boolean[] steps = new boolean[n];
        if (k == 0) return steps;
        if (k >= n)
            {
            for(int i = 0; i < steps.length; i++)
                {
                steps[i] = true;
                }
            return steps;
            }
        
        // fake Bressenham's by just using floor -- we use Math.floor in case i * slope is negative
        int lastY = -1;
        double slope = k / (double) n;
        for(int i = 0; i < n; i++)
            {
            int y = (int)Math.floor(i * slope);
            steps[i] = (lastY != y);
            lastY = y;
            }
        
        // rotate and return
        boolean[] finalSteps = new boolean[steps.length];
        for(int i = 0; i < steps.length; i++)
            {
            int pos = i + rotate;
            if (pos != 0) { pos = pos % steps.length; }
            if (pos < 0) { pos += steps.length; pos = pos % steps.length; }
            finalSteps[pos] = steps[i];
            }
        return finalSteps;
        }
    }
