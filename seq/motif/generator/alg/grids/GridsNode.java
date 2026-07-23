/* 
   Copyright 2026 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.generator.alg.grids;

import seq.engine.*;
import seq.motif.blank.*;
import seq.motif.generator.*;
import seq.util.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;


public class GridsNode extends AlgorithmNode
    {
    private static final long serialVersionUID = 1;

    public static final int NUM_STEPS_PER_PATTERN = 32;
    public static final int NUM_DRUMS = 3;

    public GridsNode(Seq seq, Generator generator, GeneratorClip clip, Algorithm algorithm)
        {
        super(seq, generator, clip, algorithm);
        reset();
        }
        
    public void loop()
        {
        reset();
        }
        
    public boolean process(ArrayList<GeneratorClip.Note> notes)
        {
        Grids grids = (Grids)getAlgorithm();
        
        int rate = grids.getRate();
        if (getPosition() % rate != 0) return false;
        else clock(grids);
        
        return false;
        }

    public void pulse(Grids grids, int drum, boolean accent)
        {
        int pitch = getCorrectedValueInt(grids.getNote(drum), 127);             
        int velocity = accent ? 
            getCorrectedValueInt(grids.getAccentVelocity(drum), 127) :
            getCorrectedValueInt(grids.getVelocity(drum), 127);     
        int rate = grids.getRate();     
        note(pitch, velocity, rate, 64);
        }

////////// COMPUTING AND PLAYING DRUMS
//// This code is largely lifted from Mutable Instrument's code.

// I think this should work?  It's missing from Mutable's code
    int u8Mix(int a, int b, int balance)
        {
        int alpha = balance;
        return (((a * alpha) + (b * (255 - alpha))) >>> 8);
        }


//// This code from Mutable Instruments Grids's code.  It extracts the drum trigger data.
    int readDrumMap(int step, int instrument, int x, int y) 
        {
        int i = x >>> 6;
        int j = y >>> 6;
        int[] a_map = drum_map[i][j];
        int[] b_map = drum_map[i + 1][j];
        int[] c_map = drum_map[i][j + 1];
        int[] d_map = drum_map[i + 1][j + 1];
        int offset = (instrument * NUM_STEPS_PER_PATTERN) + step;
        int a = a_map[offset];
        int b = b_map[offset];
        int c = c_map[offset];
        int d = d_map[offset];
        int val = u8Mix(u8Mix(a, b, x << 2), u8Mix(c, d, x << 2), y << 2);
        return val;
        }

// current random values, changed each sequence cycle
    int[] rnd = { 0, 0, 0 };

    void play(Grids grids, int step, int x, int y, int[] complexity, int chaos) 
        {
        for (int drum = 0; drum < NUM_DRUMS; ++drum) 
            {
            int threshold = 255 - complexity[drum];
            int level = readDrumMap(step, drum, x, y);
        
// NOTE: I don't like Mutable's approach here, since randomness only INCREASES
// note complexity, never DECREASES it below the base value.  Better would be
// to center the randomness at the base value, but whatever -- Sean
            if (level < 255 - rnd[drum]) 
                {
                level += rnd[drum];
                } 
            else 
                {
                level = 255;
                }
            if (level > threshold) 
                {
                pulse(grids, drum, level > Grids.ACCENT_THRESHOLD);
                }
            }
        }
    
//// End lifted code


    int step = 0;

//// Resets the sequencer to step 0 and turns off all drums
    public void reset()
        {
        step = 0;
        }

//// Clocks the sequencer and increments the step.
//// At the beginning of a sequence, we pick new random values for chaos
    int[] complexity = { 0, 0, 0 };

    void clock(Grids grids)
        {
        int x = getCorrectedValueInt(grids.getX(), 127);
        int y = getCorrectedValueInt(grids.getY(), 127);
        int chaos = getCorrectedValueInt(grids.getChaos(), 127);    
        for(int i = 0; i < NUM_DRUMS; i++)
            {
            complexity[i] = getCorrectedValueInt(grids.getComplexity(i), 127);
            }   
    
        if (step >= NUM_STEPS_PER_PATTERN) 
            {
            step = 0;
            for(int drum = 0; drum < NUM_DRUMS; drum++)
                {
                rnd[drum] = ((seq.getDeterministicRandom().nextInt(64)  * chaos) >>> 8);
                }
            }
        play(grids, step, x, y, complexity, chaos);
        step++;
        }



//// This drum data is from Mutable Instruments Grids's code (resources.cc)

    public static final int node_0[] =
        {
        255,      0,      0,      0,      0,      0,    145,      0,
        0,      0,      0,      0,    218,      0,      0,      0,
        72,      0,     36,      0,    182,      0,      0,      0,
        109,      0,      0,      0,     72,      0,      0,      0,
        36,      0,    109,      0,      0,      0,      8,      0,
        255,      0,      0,      0,      0,      0,     72,      0,
        0,      0,    182,      0,      0,      0,     36,      0,
        218,      0,      0,      0,    145,      0,      0,      0,
        170,      0,    113,      0,    255,      0,     56,      0,
        170,      0,    141,      0,    198,      0,     56,      0,
        170,      0,    113,      0,    226,      0,     28,      0,
        170,      0,    113,      0,    198,      0,     85,      0,
        };

    public static final int node_1[] =
        {
        229,      0,     25,      0,    102,      0,     25,      0,
        204,      0,     25,      0,     76,      0,      8,      0,
        255,      0,      8,      0,     51,      0,     25,      0,
        178,      0,     25,      0,    153,      0,    127,      0,
        28,      0,    198,      0,     56,      0,     56,      0,
        226,      0,     28,      0,    141,      0,     28,      0,
        28,      0,    170,      0,     28,      0,     28,      0,
        255,      0,    113,      0,     85,      0,     85,      0,
        159,      0,    159,      0,    255,      0,     63,      0,
        159,      0,    159,      0,    191,      0,     31,      0,
        159,      0,    127,      0,    255,      0,     31,      0,
        159,      0,    127,      0,    223,      0,     95,      0,
        };

    public static final int node_2[] =
        {
        255,      0,      0,      0,    127,      0,      0,      0,
        0,      0,    102,      0,      0,      0,    229,      0,
        0,      0,    178,      0,    204,      0,      0,      0,
        76,      0,     51,      0,    153,      0,     25,      0,
        0,      0,    127,      0,      0,      0,      0,      0,
        255,      0,    191,      0,     31,      0,     63,      0,
        0,      0,     95,      0,      0,      0,      0,      0,
        223,      0,      0,      0,     31,      0,    159,      0,
        255,      0,     85,      0,    148,      0,     85,      0,
        127,      0,     85,      0,    106,      0,     63,      0,
        212,      0,    170,      0,    191,      0,    170,      0,
        85,      0,     42,      0,    233,      0,     21,      0,
        };

    public static final int node_3[] =
        {
        255,      0,    212,      0,     63,      0,      0,      0,
        106,      0,    148,      0,     85,      0,    127,      0,
        191,      0,     21,      0,    233,      0,      0,      0,
        21,      0,    170,      0,      0,      0,     42,      0,
        0,      0,      0,      0,    141,      0,    113,      0,
        255,      0,    198,      0,      0,      0,     56,      0,
        0,      0,     85,      0,     56,      0,     28,      0,
        226,      0,     28,      0,    170,      0,     56,      0,
        255,      0,    231,      0,    255,      0,    208,      0,
        139,      0,     92,      0,    115,      0,     92,      0,
        185,      0,     69,      0,     46,      0,     46,      0,
        162,      0,     23,      0,    208,      0,     46,      0,
        };

    public static final int node_4[] =
        {
        255,      0,     31,      0,     63,      0,     63,      0,
        127,      0,     95,      0,    191,      0,     63,      0,
        223,      0,     31,      0,    159,      0,     63,      0,
        31,      0,     63,      0,     95,      0,     31,      0,
        8,      0,      0,      0,     95,      0,     63,      0,
        255,      0,      0,      0,    127,      0,      0,      0,
        8,      0,      0,      0,    159,      0,     63,      0,
        255,      0,    223,      0,    191,      0,     31,      0,
        76,      0,     25,      0,    255,      0,    127,      0,
        153,      0,     51,      0,    204,      0,    102,      0,
        76,      0,     51,      0,    229,      0,    127,      0,
        153,      0,     51,      0,    178,      0,    102,      0,
        };

    public static final int node_5[] =
        {
        255,      0,     51,      0,     25,      0,     76,      0,
        0,      0,      0,      0,    102,      0,      0,      0,
        204,      0,    229,      0,      0,      0,    178,      0,
        0,      0,    153,      0,    127,      0,      8,      0,
        178,      0,    127,      0,    153,      0,    204,      0,
        255,      0,      0,      0,     25,      0,     76,      0,
        102,      0,     51,      0,      0,      0,      0,      0,
        229,      0,     25,      0,     25,      0,    204,      0,
        178,      0,    102,      0,    255,      0,     76,      0,
        127,      0,     76,      0,    229,      0,     76,      0,
        153,      0,    102,      0,    255,      0,     25,      0,
        127,      0,     51,      0,    204,      0,     51,      0,
        };

    public static final int node_6[] =
        {
        255,      0,      0,      0,    223,      0,      0,      0,
        31,      0,      8,      0,    127,      0,      0,      0,
        95,      0,      0,      0,    159,      0,      0,      0,
        95,      0,     63,      0,    191,      0,      0,      0,
        51,      0,    204,      0,      0,      0,    102,      0,
        255,      0,    127,      0,      8,      0,    178,      0,
        25,      0,    229,      0,      0,      0,     76,      0,
        204,      0,    153,      0,     51,      0,     25,      0,
        255,      0,    226,      0,    255,      0,    255,      0,
        198,      0,     28,      0,    141,      0,     56,      0,
        170,      0,     56,      0,     85,      0,     28,      0,
        170,      0,     28,      0,    113,      0,     56,      0,
        };

    public static final int node_7[] =
        {
        223,      0,      0,      0,     63,      0,      0,      0,
        95,      0,      0,      0,    223,      0,     31,      0,
        255,      0,      0,      0,    159,      0,      0,      0,
        127,      0,     31,      0,    191,      0,     31,      0,
        0,      0,      0,      0,    109,      0,      0,      0,
        218,      0,      0,      0,    182,      0,     72,      0,
        8,      0,     36,      0,    145,      0,     36,      0,
        255,      0,      8,      0,    182,      0,     72,      0,
        255,      0,     72,      0,    218,      0,     36,      0,
        218,      0,      0,      0,    145,      0,      0,      0,
        255,      0,     36,      0,    182,      0,     36,      0,
        182,      0,      0,      0,    109,      0,      0,      0,
        };

    public static final int node_8[] =
        {
        255,      0,      0,      0,    218,      0,      0,      0,
        36,      0,      0,      0,    218,      0,      0,      0,
        182,      0,    109,      0,    255,      0,      0,      0,
        0,      0,      0,      0,    145,      0,     72,      0,
        159,      0,      0,      0,     31,      0,    127,      0,
        255,      0,     31,      0,      0,      0,     95,      0,
        8,      0,      0,      0,    191,      0,     31,      0,
        255,      0,     31,      0,    223,      0,     63,      0,
        255,      0,     31,      0,     63,      0,     31,      0,
        95,      0,     31,      0,     63,      0,    127,      0,
        159,      0,     31,      0,     63,      0,     31,      0,
        223,      0,    223,      0,    191,      0,    191,      0,
        };

    public static final int node_9[] =
        {
        226,      0,     28,      0,     28,      0,    141,      0,
        8,      0,      8,      0,    255,      0,      8,      0,
        113,      0,     28,      0,    198,      0,     85,      0,
        56,      0,    198,      0,    170,      0,     28,      0,
        8,      0,     95,      0,      8,      0,      8,      0,
        255,      0,     63,      0,     31,      0,    223,      0,
        8,      0,     31,      0,    191,      0,      8,      0,
        255,      0,    127,      0,    127,      0,    159,      0,
        115,      0,     46,      0,    255,      0,    185,      0,
        139,      0,     23,      0,    208,      0,    115,      0,
        231,      0,     69,      0,    255,      0,    162,      0,
        139,      0,    115,      0,    231,      0,     92,      0,
        };

    public static final int node_10[] =
        {
        145,      0,      0,      0,      0,      0,    109,      0,
        0,      0,      0,      0,    255,      0,    109,      0,
        72,      0,    218,      0,      0,      0,      0,      0,
        36,      0,      0,      0,    182,      0,      0,      0,
        0,      0,    127,      0,    159,      0,    127,      0,
        159,      0,    191,      0,    223,      0,     63,      0,
        255,      0,     95,      0,     31,      0,     95,      0,
        31,      0,      8,      0,     63,      0,      8,      0,
        255,      0,      0,      0,    145,      0,      0,      0,
        182,      0,    109,      0,    109,      0,    109,      0,
        218,      0,      0,      0,     72,      0,      0,      0,
        182,      0,     72,      0,    182,      0,     36,      0,
        };

    public static final int node_11[] =
        {
        255,      0,      0,      0,      0,      0,      0,      0,
        0,      0,      0,      0,      0,      0,      0,      0,
        255,      0,      0,      0,    218,      0,     72,     36,
        0,      0,    182,      0,      0,      0,    145,    109,
        0,      0,    127,      0,      0,      0,     42,      0,
        212,      0,      0,    212,      0,      0,    212,      0,
        0,      0,      0,      0,     42,      0,      0,      0,
        255,      0,      0,      0,    170,    170,    127,     85,
        145,      0,    109,    109,    218,    109,     72,      0,
        145,      0,     72,      0,    218,      0,    109,      0,
        182,      0,    109,      0,    255,      0,     72,      0,
        182,    109,     36,    109,    255,    109,    109,      0,
        };

    public static final int node_12[] =
        {
        255,      0,      0,      0,    255,      0,    191,      0,
        0,      0,      0,      0,     95,      0,     63,      0,
        31,      0,      0,      0,    223,      0,    223,      0,
        0,      0,      8,      0,    159,      0,    127,      0,
        0,      0,     85,      0,     56,      0,     28,      0,
        255,      0,     28,      0,      0,      0,    226,      0,
        0,      0,    170,      0,     56,      0,    113,      0,
        198,      0,      0,      0,    113,      0,    141,      0,
        255,      0,     42,      0,    233,      0,     63,      0,
        212,      0,     85,      0,    191,      0,    106,      0,
        191,      0,     21,      0,    170,      0,      8,      0,
        170,      0,    127,      0,    148,      0,    148,      0,
        };

    public static final int node_13[] =
        {
        255,      0,      0,      0,      0,      0,     63,      0,
        191,      0,     95,      0,     31,      0,    223,      0,
        255,      0,     63,      0,     95,      0,     63,      0,
        159,      0,      0,      0,      0,      0,    127,      0,
        72,      0,      0,      0,      0,      0,      0,      0,
        255,      0,      0,      0,      0,      0,      0,      0,
        72,      0,     72,      0,     36,      0,      8,      0,
        218,      0,    182,      0,    145,      0,    109,      0,
        255,      0,    162,      0,    231,      0,    162,      0,
        231,      0,    115,      0,    208,      0,    139,      0,
        185,      0,     92,      0,    185,      0,     46,      0,
        162,      0,     69,      0,    162,      0,     23,      0,
        };

    public static final int node_14[] =
        {
        255,      0,      0,      0,     51,      0,      0,      0,
        0,      0,      0,      0,    102,      0,      0,      0,
        204,      0,      0,      0,    153,      0,      0,      0,
        0,      0,      0,      0,     51,      0,      0,      0,
        0,      0,      0,      0,      8,      0,     36,      0,
        255,      0,      0,      0,    182,      0,      8,      0,
        0,      0,      0,      0,     72,      0,    109,      0,
        145,      0,      0,      0,    255,      0,    218,      0,
        212,      0,      8,      0,    170,      0,      0,      0,
        127,      0,      0,      0,     85,      0,      8,      0,
        255,      0,      8,      0,    170,      0,      0,      0,
        127,      0,      0,      0,     42,      0,      8,      0,
        };

    public static final int node_15[] =
        {
        255,      0,      0,      0,      0,      0,      0,      0,
        36,      0,      0,      0,    182,      0,      0,      0,
        218,      0,      0,      0,      0,      0,      0,      0,
        72,      0,      0,      0,    145,      0,    109,      0,
        36,      0,     36,      0,      0,      0,      0,      0,
        255,      0,      0,      0,    182,      0,      0,      0,
        0,      0,      0,      0,      0,      0,      0,    109,
        218,      0,      0,      0,    145,      0,     72,     72,
        255,      0,     28,      0,    226,      0,     56,      0,
        198,      0,      0,      0,      0,      0,     28,     28,
        170,      0,      0,      0,    141,      0,      0,      0,
        113,      0,      0,      0,     85,     85,     85,     85,
        };

    public static final int node_16[] =
        {
        255,      0,      0,      0,      0,      0,     95,      0,
        0,      0,    127,      0,      0,      0,      0,      0,
        223,      0,     95,      0,     63,      0,     31,      0,
        191,      0,      0,      0,    159,      0,      0,      0,
        0,      0,     31,      0,    255,      0,      0,      0,
        0,      0,     95,      0,    223,      0,      0,      0,
        0,      0,     63,      0,    191,      0,      0,      0,
        0,      0,      0,      0,    159,      0,    127,      0,
        141,      0,     28,      0,     28,      0,     28,      0,
        113,      0,      8,      0,      8,      0,      8,      0,
        255,      0,      0,      0,    226,      0,      0,      0,
        198,      0,     56,      0,    170,      0,     85,      0,
        };

    public static final int node_17[] =
        {
        255,      0,      0,      0,      8,      0,      0,      0,
        182,      0,      0,      0,     72,      0,      0,      0,
        218,      0,      0,      0,     36,      0,      0,      0,
        145,      0,      0,      0,    109,      0,      0,      0,
        0,      0,     51,     25,     76,     25,     25,      0,
        153,      0,      0,      0,    127,    102,    178,      0,
        204,      0,      0,      0,      0,      0,    255,      0,
        0,      0,    102,      0,    229,      0,     76,      0,
        113,      0,      0,      0,    141,      0,     85,      0,
        0,      0,      0,      0,    170,      0,      0,      0,
        56,     28,    255,      0,      0,      0,      0,      0,
        198,      0,      0,      0,    226,      0,      0,      0,
        };

    public static final int node_18[] =
        {
        255,      0,      8,      0,     28,      0,     28,      0,
        198,      0,     56,      0,     56,      0,     85,      0,
        255,      0,     85,      0,    113,      0,    113,      0,
        226,      0,    141,      0,    170,      0,    141,      0,
        0,      0,      0,      0,      0,      0,      0,      0,
        255,      0,      0,      0,    127,      0,      0,      0,
        0,      0,      0,      0,      0,      0,      0,      0,
        63,      0,      0,      0,    191,      0,      0,      0,
        255,      0,      0,      0,    255,      0,    127,      0,
        0,      0,     85,      0,      0,      0,    212,      0,
        0,      0,    212,      0,     42,      0,    170,      0,
        0,      0,    127,      0,      0,      0,      0,      0,
        };

    public static final int node_19[] =
        {
        255,      0,      0,      0,      0,      0,    218,      0,
        182,      0,      0,      0,      0,      0,    145,      0,
        145,      0,     36,      0,      0,      0,    109,      0,
        109,      0,      0,      0,     72,      0,     36,      0,
        0,      0,      0,      0,    109,      0,      8,      0,
        72,      0,      0,      0,    255,      0,    182,      0,
        0,      0,      0,      0,    145,      0,      8,      0,
        36,      0,      8,      0,    218,      0,    182,      0,
        255,      0,      0,      0,      0,      0,    226,      0,
        85,      0,      0,      0,    141,      0,      0,      0,
        0,      0,      0,      0,    170,      0,     56,      0,
        198,      0,      0,      0,    113,      0,     28,      0,
        };

    public static final int node_20[] =
        {
        255,      0,      0,      0,    113,      0,      0,      0,
        198,      0,     56,      0,     85,      0,     28,      0,
        255,      0,      0,      0,    226,      0,      0,      0,
        170,      0,      0,      0,    141,      0,      0,      0,
        0,      0,      0,      0,      0,      0,      0,      0,
        255,      0,    145,      0,    109,      0,    218,      0,
        36,      0,    182,      0,     72,      0,     72,      0,
        255,      0,      0,      0,      0,      0,    109,      0,
        36,      0,     36,      0,    145,      0,      0,      0,
        72,      0,     72,      0,    182,      0,      0,      0,
        72,      0,     72,      0,    218,      0,      0,      0,
        109,      0,    109,      0,    255,      0,      0,      0,
        };

    public static final int node_21[] =
        {
        255,      0,      0,      0,    218,      0,      0,      0,
        145,      0,      0,      0,     36,      0,      0,      0,
        218,      0,      0,      0,     36,      0,      0,      0,
        182,      0,     72,      0,      0,      0,    109,      0,
        0,      0,      0,      0,      8,      0,      0,      0,
        255,      0,     85,      0,    212,      0,     42,      0,
        0,      0,      0,      0,      8,      0,      0,      0,
        85,      0,    170,      0,    127,      0,     42,      0,
        109,      0,    109,      0,    255,      0,      0,      0,
        72,      0,     72,      0,    218,      0,      0,      0,
        145,      0,    182,      0,    255,      0,      0,      0,
        36,      0,     36,      0,    218,      0,      8,      0,
        };

    public static final int node_22[] =
        {
        255,      0,      0,      0,     42,      0,      0,      0,
        212,      0,      0,      0,      8,      0,    212,      0,
        170,      0,      0,      0,     85,      0,      0,      0,
        212,      0,      8,      0,    127,      0,      8,      0,
        255,      0,     85,      0,      0,      0,      0,      0,
        226,      0,     85,      0,      0,      0,    198,      0,
        0,      0,    141,      0,     56,      0,      0,      0,
        170,      0,     28,      0,      0,      0,    113,      0,
        113,      0,     56,      0,    255,      0,      0,      0,
        85,      0,     56,      0,    226,      0,      0,      0,
        0,      0,    170,      0,      0,      0,    141,      0,
        28,      0,     28,      0,    198,      0,     28,      0,
        };

    public static final int node_23[] =
        {
        255,      0,      0,      0,    229,      0,      0,      0,
        204,      0,    204,      0,      0,      0,     76,      0,
        178,      0,    153,      0,     51,      0,    178,      0,
        178,      0,    127,      0,    102,     51,     51,     25,
        0,      0,      0,      0,      0,      0,      0,     31,
        0,      0,      0,      0,    255,      0,      0,     31,
        0,      0,      8,      0,      0,      0,    191,    159,
        127,     95,     95,      0,    223,      0,     63,      0,
        255,      0,    255,      0,    204,    204,    204,    204,
        0,      0,     51,     51,     51,     51,      0,      0,
        204,      0,    204,      0,    153,    153,    153,    153,
        153,      0,      0,      0,    102,    102,    102,    102,
        };

    public static final int node_24[] =
        {
        170,      0,      0,      0,      0,    255,      0,      0,
        198,      0,      0,      0,      0,     28,      0,      0,
        141,      0,      0,      0,      0,    226,      0,      0,
        56,      0,      0,    113,      0,     85,      0,      0,
        255,      0,      0,      0,      0,    113,      0,      0,
        85,      0,      0,      0,      0,    226,      0,      0,
        141,      0,      0,      8,      0,    170,     56,     56,
        198,      0,      0,     56,      0,    141,     28,      0,
        255,      0,      0,      0,      0,    191,      0,      0,
        159,      0,      0,      0,      0,    223,      0,      0,
        95,      0,      0,      0,      0,     63,      0,      0,
        127,      0,      0,      0,      0,     31,      0,      0,
        };

    public static final int[][][] drum_map = 
        {
        { node_10, node_8, node_0, node_9, node_11 },
        { node_15, node_7, node_13, node_12, node_6 },
        { node_18, node_14, node_4, node_5, node_3 },
        { node_23, node_16, node_21, node_1, node_2 },
        { node_24, node_19, node_17, node_20, node_22 },
        };

//// End drum data
    }
        
        
        
        
        
        
