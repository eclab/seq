/* 
   Copyright 2026 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.generator.alg.walk;

import seq.engine.*;
import seq.motif.blank.*;
import seq.motif.generator.*;
import seq.util.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;
import java.io.*;
import seq.motif.generator.gui.*;
import seq.motif.generator.alg.walk.gui.*;

public class Walk extends Algorithm
    {
    private static final long serialVersionUID = 1;
    
    public static final int RATE_WHOLE_NOTES = Seq.PPQ * 4;
    public static final int RATE_HALF_NOTES = Seq.PPQ * 2;
    public static final int RATE_QUARTER_NOTES = Seq.PPQ;
    public static final int RATE_EIGHTH_NOTES = Seq.PPQ / 2;
    public static final int RATE_TRIPLET_NOTES = Seq.PPQ / 3;
    public static final int RATE_SIXTEENTH_NOTES = Seq.PPQ / 4;
    public static final int RATE_TRIPLET_SIXTEENTH_NOTES = Seq.PPQ / 6;
    public static final int RATE_THIRTY_SECOND_NOTES = Seq.PPQ / 8;
        
    public static final int[] RATES = { RATE_WHOLE_NOTES, RATE_HALF_NOTES, RATE_QUARTER_NOTES, RATE_EIGHTH_NOTES, RATE_TRIPLET_NOTES, RATE_SIXTEENTH_NOTES, RATE_TRIPLET_SIXTEENTH_NOTES, RATE_THIRTY_SECOND_NOTES };
    public static final String[] RATE_NAMES = { "Whole Note", "Half Note", "Quarter Note", "Eighth Note", "Triplet", "Sixteenth Note", "Triplet Sixteenth Note", "Thirty-Second Note" };

/*
	public static final int[][] SCALES = 
		{
		{ 0, 2, 4, 5, 7, 9, 11 },			// Major
		{ 0, 2, 3, 5, 7, 9, 10 },			// Dorian
		{ 0, 1, 3, 5, 7, 8, 10 },			// Phrygian
		{ 0, 2, 4, 6, 7, 9, 10 },			// Lydian
		{ 0, 2, 4, 5, 7, 9, 10 },			// Mixolydian
		{ 0, 2, 3, 5, 7, 8, 10 },			// Natural Minor
		{ 0, 1, 3, 5, 6, 8, 10 },			// Locrian
		{ 0, 2, 3, 5, 7, 8, 11 },			// Harmonic Minor
		{ 0, 2, 3, 5, 7, 9, 11 },			// Melodic Minor
		{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 },			// Chromatic
		{ 0, 7 },			// Fifths
		{ 0, 4, 7 },		// Major
		{ 0, 3, 7 },		// Minor
		{ 0, 4, 8 },		// Aug
		{ 0, 4, 7, 11 },	// Maj7
		{ 0, 3, 7, 10 },	// Min7
		{ 0, 4, 7, 10 },	// Dom7
		{ 0, 3, 6, 9 },		// Dim7
		{ 0, 2, 4, 7, 9 },	// Pentatonic
		};
*/
    public static final int SCALES[][] = 
        {
        /// COMMON SCALES
        //C  Db D  Eb E  F  Gb G  Ab A  Bb B
        { 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 0, 1 },         // Major
        { 1, 0, 1, 1, 0, 1, 0, 1, 1, 0, 0, 1 },         // Harmonic Minor
        { 1, 0, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1 },         // Melodic Minor
        { 1, 0, 1, 1, 0, 1, 0, 1, 0, 1, 1, 0 },         // Dorian
        { 1, 1, 0, 1, 0, 1, 0, 1, 1, 0, 1, 0 },         // Phrygian
        { 1, 0, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1 },         // Lydian
        { 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 1, 0 },         // Mixolydian
        { 1, 0, 1, 1, 0, 1, 0, 1, 1, 0, 1, 0 },         // Aeolian (Relative Minor)
        { 1, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 0 },         // Locrian
        { 1, 0, 0, 1, 0, 1, 1, 1, 0, 0, 1, 0 },         // Blues Minor
        
        { 1, 0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 0 },         // Pentatonic
        { 1, 0, 0, 1, 0, 1, 0, 1, 0, 0, 1, 0 },         // Minor Pentatonic
        { 1, 1, 0, 0, 0, 1, 0, 1, 1, 0, 0, 0 },         // Japanese Pentatonic
        { 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0 },         // Whole Tone
        { 1, 0, 1, 1, 0, 0, 1, 1, 1, 0, 1, 0 },         // Hungarian Gypsy
        { 1, 0, 0, 1, 1, 1, 0, 1, 1, 0, 1, 0 },         // Phrygian Dominant
        { 1, 1, 0, 0, 1, 1, 1, 0, 1, 0, 0, 1 },         // Persian
        { 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1 },         // Diminished (Octatonic)
        { 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1 },         // Augmented (Hexatonic)
        { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 },         // Chromatic

        // INTERVALS AND CHORDS
        //C  Db D  Eb E  F  Gb G  Ab A  Bb B
        { 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0 },         // M3
        { 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0 },         // m3
        { 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0 },         // 4th + Octave 
        { 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0 },         // 5th + Octave 
        { 1, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0 },         // 4th + 5th + Octave 
        { 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0 },         // Major Triad
        { 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0 },         // Minor Triad
        { 1, 0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0 },         // Major 6
        { 1, 0, 0, 1, 0, 0, 0, 1, 0, 1, 0, 0 },         // Minor 6
        { 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0 },         // Augmented Triad
        
        { 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0 },         // 7
        { 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1 },         // Major 7
        { 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0 },         // Minor 7
        { 1, 0, 1, 0, 1, 0, 0, 1, 0, 0, 0, 1 },         // Major 7 + 2nd 
        { 1, 0, 1, 1, 0, 0, 0, 1, 0, 0, 1, 0 },         // Minor 7 + 2nd 
        { 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0 },         // Diminished 7
        { 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1 },         // Minor-Major 7
        };
        
    public static final String[] PITCH_METHOD_NAMES = { "1/f (Pink Noise)", "Binomial" };
    public static final String[] LENGTH_METHOD_NAMES = { "1/f (Pink Noise)", "Binomial" };
		
	public static final String[] SCALE_NAMES = 
		{
		"Major", "Harmonic Minor", "Melodic Minor", "Dorian", "Phrygian", "Lydian", "Mixolydian", "Aeolean (Natural Minor)", "Locrian", "Blues Minor", 
		"Pentatonic", "Minor Pentatonic", "Japanese Pentatonic", "Whole Tone", "Hungarian Gypsy", "Phrygian Dominant", "Persian", "Diminished Scale (Octatonic)", "Augmented Scale (Hexatonic)", "Chromatic", 
		"M3 Interval", "m3 Interval", "4th Interval", "5th Interval", "4th + 5th", "Major Triad", "Minor Triad", "Major Triad + 6th", "Minor Triad + 6th", "Augmented Triad",
		"Dominant 7", "Major 7", "Minor 7", "Major 7 + 2nd", "Minor 7 + 2nd", "Diminished 7", "Minor-Major 7"
		};
		
    public int getRateIndex()
        { 
        for(int i = 0; i < RATES.length; i++) 
            {
            if (RATES[i] == rate) return i;
            }
        return 0;
        }
        
    public static final int MAX_TRANSPOSE = 48;
    public static final int LOW_NOTE = 60 - 24;

	public static final int PITCH_METHOD_PINK = 0;
	public static final int PITCH_METHOD_BINOMIAL = 1;
	public static final int LENGTH_METHOD_PINK = 0;
	public static final int LENGTH_METHOD_BINOMIAL = 1;

	int startPitch = 60;		// MIDDLE C
	int pitchMethod = PITCH_METHOD_PINK;
	int lengthMethod = LENGTH_METHOD_PINK;
	int scale = 0;				// Major
    int transpose = 24;
    int velocity = 64;
    int rate = RATE_EIGHTH_NOTES;
    double pitchWeight = 0.1;
    double lengthWeight = 0.1;
    double gate = 0.5;
    double startWeight = 0.1;
    double pitchIncrementProbability = 1.0;
    double restProbability = 0.1;
    double tether = 0.25;
    
    /** Given a current note (in the scale), and a number of notes in the scale to shift up or down, and a scale,
    	returns the resulting note.  If the shift would exceed note bounds, returns -1. */
    public int shiftNotes(int currentNote, int shift)
    	{
    	if (shift == 0) return currentNote;
    	else if (shift > 0) 
    		{
    		if (currentNote == 127) return -1;
    		int count = 0;
    		for(int i = currentNote + 1; i < 128; i++)
    			{
    			if (SCALES[scale][i % 12] == 1) { count++; }
    			if (count == shift) return i;
    			}
    		return -1;
    		}
    	else
    		{
    		if (currentNote == 0) return -1;
    		int last = currentNote;
    		int count = 0;
    		for(int i = currentNote - 1; i >= 0; i--)
    			{
    			if (SCALES[scale][i % 12] == 1) { count++; }
    			if (count == 0 - shift) return i;
    			}
    		return -1;
    		}
    	}
    
    /*
    public Walk copy()
        {
        Walk lm = (Walk)(super.copy());
        return lm;
        }
    */
        
    public Walk(Generator generator)
        {
        super(generator);
        }
        
    public Walk(Generator generator, JSONObject obj) throws JSONException
        {
        super(generator, obj);
        transpose = obj.optInt("trans", 0);
        velocity = obj.optInt("vel", 64);
        rate = obj.optInt("rate", RATE_EIGHTH_NOTES);
        scale = obj.optInt("sca", 0);
        pitchWeight = obj.optDouble("pweight", 0.1);
        gate = obj.optDouble("gweight", 1.0);
        lengthWeight = obj.optDouble("lweight", 0.1);
        startWeight = obj.optDouble("sweight", 0.1);
        pitchIncrementProbability = obj.optDouble("inc", 1.0);
        startPitch = obj.optInt("spitch", 60);
        pitchMethod = obj.optInt("pmeth", PITCH_METHOD_PINK);
        lengthMethod = obj.optInt("lmeth", LENGTH_METHOD_PINK);
        restProbability = obj.optDouble("rest", 0.1);
        tether = obj.optDouble("tether", 0.25);
        }
        
    public void save(JSONObject obj) throws JSONException
        {
        super.save(obj);
        obj.put("trans", transpose);
        obj.put("vel", velocity);
        obj.put("rate", rate);
        obj.put("sca", scale);
        obj.put("sweight", startWeight);
        obj.put("pweight", pitchWeight);
        obj.put("gweight", gate);
        obj.put("lweight", lengthWeight);
        obj.put("inc", pitchIncrementProbability);
        obj.put("pmeth", pitchMethod);
        obj.put("lmeth", lengthMethod);
        obj.put("spitch", startPitch);
        obj.put("rest", restProbability);
        obj.put("tether", tether);
        }

    public AlgorithmNode buildNode(Seq seq, GeneratorClip clip)
        {
        return new WalkNode(seq, generator, clip, this);
        }
                
    public AlgorithmUI buildUI(Seq seq, GeneratorUI ui)
        {
        return new WalkUI(seq, generator, ui, this);
        } 

    public int getTranspose() { return transpose; }
    public void setTranspose(int val) { transpose = val; }
    public int getVelocity() { return velocity; }
    public void setVelocity(int val) { velocity = val; }
    public int getRate() { return rate; }
    public void setRate(int val) { rate = val; }
    public int getScale() { return scale; }
    public void setScale(int val) { scale = val; }
    public double getStartWeight() { return startWeight; }
    public void setStartWeight(double val) { startWeight = val; }
    public double getPitchWeight() { return pitchWeight; }
    public void setStartPitch(int val) { startPitch = val; }
    public int getStartPitch() { return startPitch; }
    public void setPitchWeight(double val) { pitchWeight = val; }
    public double getPitchIncrementProbability() { return pitchIncrementProbability; }
    public void setPitchIncrementProbability(double val) { pitchIncrementProbability = val; }
    public int getPitchMethod() { return pitchMethod; }
    public void setPitchMethod(int val) { pitchMethod = val; }
    public int getLengthMethod() { return lengthMethod; }
    public void setLengthMethod(int val) { lengthMethod = val; }
    public double getLengthWeight() { return lengthWeight; }
    public void setLengthWeight(double val) { lengthWeight = val; }
    public double getGate() { return gate; }
    public void setGate(double val) { gate = val; }
    public double getRestProbability() { return restProbability; }
    public void setRestProbability(double val) { restProbability = val; }
    public double getTether() { return tether; }
    public void setTether(double val) { tether = val; }

    public String getHTMLDescription() 
        {
        return getIndexHTMLFileText();
        }
                
    }
        
        
        
        
        
        
