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


public class WalkNode extends AlgorithmNode
    {
    private static final long serialVersionUID = 1;
    
    int lastPitch;
    int lastPosition;
    boolean atStart = false;

    public void reset()
        {
        atStart = true;
        lastPosition = 0;
        }
        
    public WalkNode(Seq seq, Generator generator, GeneratorClip clip, Algorithm algorithm)
        {
        super(seq, generator, clip, algorithm);
        reset();
        }
        
    public WalkNode copy()
        {
        WalkNode lmn = (WalkNode)(super.copy());
        return lmn;
        }
    
    public void loop()
        {
        reset();
        }
        
    public boolean process(ArrayList<GeneratorClip.Note> notes)
        {
        Walk walk = (Walk)getAlgorithm();
    	Random rand = getDeterministicRandom();
        
        if (getPosition() < lastPosition)			// not ready yet
        	{
        	return false;
        	}
        
        /// Figure rest
        
        boolean isRest = false;
        if (rand.nextDouble() < getCorrectedValueDouble(walk.getRestProbability()))
        	{
        	isRest = true;
        	}
        else
        	{
			/// Figure pitch
		
			// Figure out if we're starting and adjust pitch and weights accordingly
			int pitchDiff = 0;
			double pitchWeight = atStart? getCorrectedValueDouble(walk.getStartWeight()) : getCorrectedValueDouble(walk.getPitchWeight());  // are we starting?
			if (atStart)
				{
				lastPitch = getCorrectedValueInt(walk.getStartPitch(), 127);
				}
		
			// Figure out the jump
			switch(walk.getPitchMethod())
				{
				case Walk.PITCH_METHOD_PINK:
				pitchDiff = nextPinkInterval(pitchWeight);
				break;
				case Walk.PITCH_METHOD_BINOMIAL:
				pitchDiff = nextBinomialInterval(pitchWeight);
				break;
				}
								
			// Increment by 1 if we need to
			if (rand.nextDouble() < getCorrectedValueDouble(walk.getPitchIncrementProbability()))
				{
				// need to increment
				if (pitchDiff > 0) pitchDiff++;
				else if (pitchDiff < 0) pitchDiff--;
				else if (rand.nextDouble() < 0.5) pitchDiff++;
				else pitchDiff--;
				}
			
			// tether
			if (rand.nextDouble() < getCorrectedValueDouble(walk.getTether()))
				{
				double startPitch = getCorrectedValueDouble(walk.getStartPitch());
				if (pitchDiff > 0 && lastPitch > startPitch ||
					pitchDiff < 0 && lastPitch < startPitch) 
					{
					pitchDiff =  0 - pitchDiff;
					}
				}

 			// are we out of bounds?
 			int shift = walk.shiftNotes(lastPitch, pitchDiff);
 			if (shift < 0)
 				shift = walk.shiftNotes(lastPitch, 0 - pitchDiff);
 			if (shift < 0) // uh oh
 				shift = lastPitch;
 				
			// update
			lastPitch = shift;
			atStart = false;
			}
        
        
        // Figure Length
        
        int length = 0;
        double lengthWeight = getCorrectedValueDouble(walk.getLengthWeight());
        
        switch(walk.getLengthMethod())
        	{
        	case Walk.LENGTH_METHOD_PINK:
        	length = nextPinkInterval(lengthWeight) + 1;
        	break;
        	case Walk.LENGTH_METHOD_BINOMIAL:
        	length = nextBinomialInterval(lengthWeight) + 1;
        	break;
        	}
        
        int time = walk.getRate() * length;
        
        
        // Emit the note unless it's a rest
        
        if (!isRest)
        	{
        	note(lastPitch, getCorrectedValueInt(walk.getVelocity(), 127), (int)(time * getCorrectedValueDouble(walk.getGate())), 64);
        	}
        
        // update position regardless of note or rest
        lastPosition = getPosition() + time;
        
        return false;
        }



    //// NOISE METHODS
    
    public static final int INTERVAL_MAX = 12;
    
    public boolean nextRest(double weight)
    	{
    	Random rand = getDeterministicRandom();
    	return (rand.nextDouble() < weight);
    	}
    
    /// BINOMIAL DISTRIBUTION NOISE
    // Returns values from -INTERVAL_MAX to +INTERVAL_MAX inclusive
    public int nextBinomialInterval(double weight)
    	{
    	int count = 0;
    	Random rand = getDeterministicRandom();
    	for(int i = 0; i < INTERVAL_MAX; i++)
    		{
    		if (rand.nextDouble() >= weight) break;
    		else count++;
    		}
    	if (rand.nextDouble() < 0.5) return count;
    	else return 0 - count;
    	}
    

    /// PINK NOISE
    /// This is an implementation of Wagner-Voss, and is always between -1 and +1
    /// https://hjkl11.hashnode.dev/the-voss-mccartney-algorithm-in-rust-lang
    
	final static int NUM_PINK_TRIES = 4;
	final static int NUM_PINK_ROWS = 12;
     double[] pinkRows = null;		// initially null, will get initialized first time in nextPink()
     double runningPinkSum = 0.0;	// will get initialized 
     int pinkIndex = 0;

    Random rand = new Random();

    double nextPink() 
    	{
    	if (pinkRows == null)
    		{
    		pinkRows = new double[NUM_PINK_ROWS];
			for (int i = 0; i < NUM_PINK_ROWS; i++) 
				{
				pinkRows[i] = (2.0 * rand.nextDouble() - 1.0);
				runningPinkSum += pinkRows[i];
	    		}
	    	}
    		
    	for(int i = 0;i < NUM_PINK_TRIES; i++)
    		{
			pinkIndex++;
			int trailingZeros = Integer.numberOfTrailingZeros(pinkIndex);
			int rowToUpdate = trailingZeros % NUM_PINK_ROWS;
			runningPinkSum -= pinkRows[rowToUpdate];
			pinkRows[rowToUpdate] = (2.0 * rand.nextDouble() - 1.0);
			runningPinkSum += pinkRows[rowToUpdate];
			double extraWhiteNoise = (2.0 * rand.nextDouble() - 1.0);
			double val = (runningPinkSum + extraWhiteNoise) / NUM_PINK_ROWS;
			if (val * 2 <= 1.0 && val * 2 >= -1.0) return val * 2;
			}
		return 0;
    	}

    // Returns values from -INTERVAL_MAX to +INTERVAL_MAX inclusive
    public int nextPinkInterval(double weight)
    	{
    	int interval = (int)(nextPink() * (INTERVAL_MAX + 1) * weight);
    	if (interval >= INTERVAL_MAX) interval = INTERVAL_MAX;
    	else if (interval <= 0 - INTERVAL_MAX) interval = 0 - INTERVAL_MAX;
    	return interval;
    	}


/*
	// This is an implementation of Paul Kellet's algorithm, but it's not centered
	// anywhere near zero, so we can't use it. 
    double[] b = { 0, 0, 0, 0, 0, 0, 0 };
    public static final int PINK_TRIES = 4;
    
    public double nextPink()
    	{
    	Random rand = getDeterministicRandom();
    	for(int i = 0; i < PINK_TRIES; i++)
    		{
			double white = rand.nextDouble() * 2.0 - 1.0;
			b[0] =  0.99886 * b[0] + white * 0.0555179;
			b[1] =  0.99332 * b[1] + white * 0.0750759;
			b[2] =  0.96900 * b[2] + white * 0.1538520;
			b[3] =  0.86650 * b[3] + white * 0.3104856;
			b[4] =  0.55000 * b[4] + white * 0.5329522;
			b[5] = -0.76160 * b[5] - white * 0.0168980;
			double pink = b[0] + b[1] + b[2] + b[3] + b[4] + b[5] + b[6] + white * 0.5362; 
			b[6] = white * 0.115926;

			// This needs to be scaled down.  We'll do it by 1/6, and reject large events
			pink = pink * 0.1666666666
			if (pink <= 1.0 && pink >= -1.0) return pink;
			}
		return 0;
    	}
    	
    public int nextPinkInterval(double weight)
    	{
    	int interval = nextPink() * (INTERVAL_MAX + 1) * weight;
    	if (interval >= INTERVAL_MAX) interval = INTERVAL_MAX;
    	if (interval <= 0 - INTERVAL_MAX) interval = 0 - INTERVAL_MAX;
    	return jump;
    	}
*/    


        }
        
        
        
        
        
        
