/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.modulation;

import seq.engine.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

public class Modulation extends Motif
    {
    static final long serialVersionUID = 1;

    public static class Data
        {
        public static final int DISABLED = -1;

        public static final int MAX_TRANSPOSE = 24;
        public static final double MAX_GAIN = 4.0;
        public static final double MAX_RATE = 16.0;
        public static final double DEFAULT_RATE = 1.0;
    
        public static final int[][] scales = 
            {
            /// COMMON SCALES
            //C  Db D  Eb E  F  Gb G  Ab A  Bb B
            { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 },         // Chromatic
            { 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 0, 1 },         // Major
            { 1, 0, 1, 1, 0, 1, 0, 1, 1, 0, 0, 1 },         // Harmonic Minor
            { 1, 0, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1 },         // Melodic Minor
            { 1, 0, 1, 1, 0, 1, 0, 1, 0, 1, 1, 0 },         // Dorian
            { 1, 1, 0, 1, 0, 1, 0, 1, 1, 0, 1, 0 },         // Phrygian
            { 1, 0, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1 },         // Lydian
            { 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 1, 0 },         // Mixolydian
            { 1, 0, 1, 1, 0, 1, 0, 1, 1, 0, 1, 0 },         // Aeolian (Relative Minor)
            { 1, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 0 },         // Locrian

            /// OTHER SCALES
            //C  Db D  Eb E  F  Gb G  Ab A  Bb B
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

            // CHORDS
            //C  Db D  Eb E  F  Gb G  Ab A  Bb B
            { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },         // Octave
            { 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0 },         // 5th + Octave 
            { 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0 },         // Major Triad
            { 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0 },         // Minor Triad
            { 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0 },         // Augmented Triad
            { 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0 },         // 7
            { 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1 },         // Major 7
            { 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0 },         // Minor 7
            { 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0 },         // Diminished 7
            { 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1 },         // Minor-Major 7
            };

        public static final int[][] scalesShifter =
            {
            /// COMMON SCALES
            // C  Db   D  Eb  E   F   Gb  G   Ab  A   Bb  B
            { +0, +0, +0, +0, +0, +0, +0, +0, +0, +0, +0, +0 },         // Chromatic
            { +0, -1, +0, -1, +0, +0, -1, +0, -1, +0, -1, +0 },         // Major
            { +0, -1, +0, +0, -1, +0, -1, +0, +0, -1, +1, +0 },         // Harmonic Minor
            { +0, -1, +0, +0, -1, +0, -1, +0, -1, +0, -1, +0 },         // Melodic Minor
            { +0, -1, +0, +0, -1, +0, -1, +0, -1, +0, +0, -1 },         // Dorian
            { +0, +0, -1, +0, -1, +0, -1, +0, +0, -1, +0, -1 },         // Phrygian
            { +0, -1, +0, -1, +0, -1, +0, +0, -1, +0, -1, +0 },         // Lydian
            { +0, -1, +0, -1, +0, +0, -1, +0, -1, +0, +0, -1 },         // Mi-xolydian
            { +0, -1, +0, +0, -1, +0, -1, +0, +0, -1, +0, -1 },         // Aeolian (Relative Minor)
            { +0, +0, -1, +0, -1, +0, +0, -1, +0, -1, +0, -1 },         // Locrian

            /// OTHER SCALES
            // C  Db   D  Eb  E   F   Gb  G   Ab  A   Bb  B
            { +0, -1, +1, +0, -1, +0, +0, +0, -1, +1, +0, -1 },         // Blues Minor
            { +0, -1, +0, -1, +0, -1, +1, +0, +1, +0, -1, +1 },         // Pentatonic
            { +0, -1, +1, +0, -1, +0, -1, +0, -1, +1, +0, -1 },         // Minor Pentatonic
            { +0, +0, -1, -2, +1, +0, -1, +0, +0, -1, -2, +1 },         // Japanese Pentatonic
            { +0, -1, +0, -1, +0, -1, +0, -1, +0, -1, +0, -1 },         // Whole Tone
            { +0, -1, +0, +0, -1, +1, +0, +0, +0, -1, +0, -1 },         // Hungarian Gypsy
            { +0, -1, +1, +0, +0, +0, -1, +0, +0, -1, +0, -1 },         // Phrygian Dominant
            { +0, +0, -1, +1, +0, +0, +0, -1, +0, -1, +1, +0 },         // Persian
            { +0, -1, +0, +0, -1, +0, +0, -1, +0, +0, -1, +0 },         // Diminished (Octatonic)
            { +0, -1, +1, +0, +0, -1, +1, +0, +0, -1, +1, +0 },         // Augmented (He-xatonic)

            // CHORDS
            // C  Db   D  Eb  E   F   Gb  G   Ab  A   Bb  B
            { +0, -1, -2, -3, -4, -5, -6, +5, +4, +3, +2, +1 },         // Octave
            { +0, -1, -2, -3, +3, +2, +1, +0, -1, -2, +2, +1 },         // 5th + Octave
            { +0, -1, -2, +1, +0, -1, +1, +0, -1, -2, +2, +1 },         // Major Triad
            { +0, -1, +1, +0, -1, -2, +1, +0, -1, -2, +2, +1 },         // Minor Triad
            { +0, -1, -2, +1, +0, -1, -2, +1, +0, -1, -2, +1 },         // Augmented Triad
            { +0, -1, -2, +1, +0, -1, +1, +0, -1, +1, +0, -1 },         // 7
            { +0, -1, -2, +1, +0, -1, +1, +0, -1, -2, +1, +0 },         // Major 7
            { +0, -1, +1, +0, -1, -2, +1, +0, -1, +1, +0, -1 },         // Minor 7
            { +0, -1, +1, +0, -1, +1, +0, -1, +1, +0, -1, +1 },         // Diminished 7
            { +0, -1, +1, +0, -1, -2, +1, +0, -1, -2, +1, +0 },         // Minor-Major 7
            };

        public static final String[] scaleNames =
            {
            "Chromatic", "Major", "Harmonic Minor", "Melodic Minor", "Dorian", "Phrygian", "Lydian", "Mixolydian", "Relative Minor", "Locrian",
            "Blues Minor", "Pentatonic", "Minor Pentatonic", "Japanese Pentatonic", "Whole Tone", "Hungarian Gypsy", "Phrygian Dominant", "Persian", "Diminished", "Augmented",
            "Octave", "5th + Octave", "Major Triad", "Minor Triad", "Augmented Triad", "7th", "Major 7th", "Minor 7th", "Diminished 7th", "Minor-Major 7th"
            };

        public static final String[] rootNames =
            {
            "C", "D\u266D", "D", "E\u266D", "E", "F", "G\u266D", "G", "A\u266D", "A", "B\u266D", "B"
            };

        int scale = 0;         // CHROMATIC
        int root = 0;
         
        int repeatAtLeast;
        double repeatProbability;
        boolean repeatUntilTrigger;
        double rate = DEFAULT_RATE;
        int transpose = MAX_TRANSPOSE;                                        // ranges 0 ... MAX_TRANSPOSE * 2 inclusive, representing -MAX_TRANSPOSE ... MAX_TRANSPOSE
        double gain = 1;
        int out = DISABLED;

        public void setTranspose(int transpose) { this.transpose = transpose; }
        public int getTranspose() { return transpose; }

        public void setScale(int scale) { this.scale = scale; }
        public int getScale() { return scale; }
        
        public void setRoot(int root) { this.root = root; }
        public int getRoot() { return root; }

        public int adjustNote(int note)
            {
            // Do this for inlining
            if (scale == 0) // chromatic
                return note;
            else return _adjustNote(note);
            }
        
        int _adjustNote(int note)
            {
            // finds the closes note in the scale (going the least half steps down or up) (if tied goes down)
            // this is now O(1)
            int[] _shift = scalesShifter[scale];
            int scaleGrade = (note - root)%12;
            return note + _shift[scaleGrade];
            // Otherwise this is O(n)
            // We need to find the first note in the scale that is below the given note
            // FIXME we should modify scales to make this an O(1) lookup
            /*int[] _scale = scales[scale];
//              int n = note % 12;
//              for(int i = n; i > n - 12; i--)
//                      {
//                      int _i = i + root;
//                      if (_i >= 12) _i -= 12;
//                      if (_i < 0) i += 12;
//                      if (_scale[_i] == 1)    // found it
//                              {
//                              return note - n + i;
//                              }
//                      }
//              return note;            // should never happen*/
            }

        public void setGain(double gain) { this.gain = gain; }
        public double getGain() { return gain; }

        public void setOut(int out) { this.out = out; }
        public int getOut() { return out; }
        
        public double getRate() { return rate; }
        public void setRate(double val) { rate = val; }
        
        public Data() { }
        
        public Data(Data other)
            {
            rate = other.rate;
            transpose = other.transpose;
            gain = other.gain;
            out = other.out;
            scale = other.scale;
            root = other.root;
            }
        }

    // CHILD MANIPULATION

    protected Object buildData(Motif motif) { return new Data(); }

    protected Object copyData(Motif motif, Object data) 
        { 
        Data other = new Data((Data)data); 
        return other;
        }

    protected void saveData(Object data, Motif motif, JSONObject to) 
        {
        Data d = (Data)data;
        to.put("rate", d.rate);
        to.put("tran", d.transpose);
        to.put("gain", d.gain);
        to.put("out", d.out);
        }

    protected Object loadData(Motif motif, JSONObject from) 
        { 
        Data d = (Data)buildData(motif);
        d.transpose = from.optInt("tran", 0);
        d.rate = from.optDouble("rate", 1.0);
        d.gain = from.optDouble("gain", 1.0);
        d.out = from.optInt("out", 0);
        return d;
        }

 
    public Clip buildClip(Clip parent)
        {
        return new ModulationClip(seq, this, parent);
        }
        

    public Modulation(Seq seq)
        {
        super(seq);
        }
                
                
    public void save(JSONObject to) throws JSONException 
        {
        //to.put("mode", mode); 
        }

    public void load(JSONObject from) throws JSONException
        {
        //mode = from.optInt("mode", MODE_SERIES);
        }
        
    static int counter = 1;
    public int getNextCounter() { return counter++; }
        
    public String getBaseName() { return "Modulation"; }
    }
        
