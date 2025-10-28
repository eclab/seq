/* 
   Copyright 2025 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.filter;

import seq.engine.*;
import seq.motif.blank.*;
import seq.util.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;


public class Filter extends Motif
    {
    private static final long serialVersionUID = 1;
    
    public static final String IDENTITY = "Identity";
    public static final String CHANGE_NOTE = "Note";            // Change Velocity, Release Velocity, transpose pitch
    public static final String DELAY = "Delay";
    public static final String DROP = "Drop";
    public static final String NOISE = "Noise";
    public static final String MAP = "Map";
    public static final String SCALE = "Scale";
    public static final String CHORD = "Chord";

    public static final int NUM_TRANSFORMERS = 4;
    public static final int MAX_TRANSPOSE_NOISE = 24;       // 12 notes
    public static final double MAX_GAIN = 4.0;    // 4x, since we add 1
    public static final int MAX_TRANSPOSE = 24;
    public static final int MAX_DELAY_NUM_TIMES = 16;
        

    public static final int SCALES[][] = 
        {
        /// COMMON SCALES
        //    C  Db D  Eb E  F  Gb G  Ab A  Bb B
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

        // INTERVALS AND CHORDS
        //    C  Db D  Eb E  F  Gb G  Ab A  Bb B
        { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },         // Octave
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


    public static final int CHORDS[][] = 
        {
        // INTERVALS AND CHORDS
        //    C  Db D  Eb E  F  Gb G  Ab A  Bb B  C 
        { 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 },              // m3
        { 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0 },              // M3
        { 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0 },              // 4
        { 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0 },              // 5 
        { 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0 },              // m6 
        { 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0 },              // M6
        { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0 },              // m7
        { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 },              // Octave
        { 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0 },              // Major Triad
        { 1, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0 },              // Major Triad 1st Inv
        { 1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0 },              // Major Triad 2nd Inv
        { 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0 },              // Minor Triad
        { 1, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0 },              // Minor Triad 1st Inv
        { 1, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0 },              // Minor Triad 2nd Inv
        { 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0 },              // 7 Without 3rd
        { 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0 },              // 7
        { 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0 },              // Major 7
        { 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0 },              // Minor 7
        { 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 0 },              // Diminished 7
        };


    // this index order is just the order of the combo box in FilterChildInspector
    public int typeIndex(String type)
        {
        if (IDENTITY.equals(type))
            {
            return 0;
            }
        else if (CHANGE_NOTE.equals(type))
            {
            return 1;
            }
        else if (DELAY.equals(type))
            {
            return 2;
            }
        else if (DROP.equals(type))
            {
            return 3;
            }
        else if (NOISE.equals(type))
            {
            return 4;
            }
        else if (MAP.equals(type))
            {
            return 5;
            }
        else if (SCALE.equals(type))
            {
            return 6;
            }
        else if (CHORD.equals(type))
            {
            return 7;
            }
        else
            {
            System.err.println("Filter.typeIndex: could not find type " + type);
            return 0;
            }
        }
        
    // this index order is just the order of the combo box in FilterChildInspector
    public Function buildFunction(int index)
        {
        if (index == 0)
            {
            return new Function();
            }
        else if (index == 1)
            {
            return new ChangeNote();
            }
        else if (index == 2)
            {
            return new Delay();
            }
        else if (index == 3)
            {
            return new Drop();
            }
        else if (index == 4)
            {
            return new Noise();
            }
        else if (index == 5)
            {
            return new Map();
            }
        else if (index == 6)
            {
            return new Scale();
            }
        else if (index == 7)
            {
            return new Chord();
            }
        else
            {
            System.err.println("Filter.loadFunction: could not create function " + index);
            return new Function();
            }
        }
        
    public Function loadFunction(JSONObject obj)
        {
        String type = obj.optString("type", null);
        if (IDENTITY.equals(type))
            {
            return new Function(obj);
            }
        else if (CHANGE_NOTE.equals(type))
            {
            return new ChangeNote(obj);
            }
        else if (DELAY.equals(type))
            {
            return new Delay(obj);
            }
        else if (DROP.equals(type))
            {
            return new Drop(obj);
            }
        else if (NOISE.equals(type))
            {
            return new Noise(obj);
            }
        else if (MAP.equals(type))
            {
            return new Map(obj);
            }
        else if (SCALE.equals(type))
            {
            return new Scale(obj);
            }
        else if (CHORD.equals(type))
            {
            return new Chord(obj);
            }
        else
            {
            System.err.println("Filter.loadFunction: could not load function " + obj);
            return new Function(obj);
            }
        }
        
    public class Function implements Cloneable
        {
        public String getType() { return IDENTITY; }
        public Function()
            {
            }
        public Function(JSONObject obj)
            {
            }
        public JSONObject save() throws JSONException
            {
            JSONObject obj = new JSONObject();
            obj.put("type", getType());
            return obj;
            }
        public Function copy() 
            { 
            try
                {
                return (Function)(this.clone());
                }
            catch (CloneNotSupportedException ex) { return null; }          // will not happen
            }
        }

    public class ChangeNote extends Function
        {
        public static final int NO_OUT_CHANGE = -1;
        
        int out = NO_OUT_CHANGE;
        int transpose = MAX_TRANSPOSE;          // transpose goes 0... MAX_TRANSPOSE * 2, so this is centered
        double transposeVariance;
        double gain = 1.0;
        double gainVariance;
        double releaseGain = 1.0;                       // if length is changes, the base release will be changed to 0x64
        double releaseGainVariance;
        int length;                                     // length = 0 means as fast as possible, but it will be length 1 because Seq.java processes note-offs BEFORE the next step 
        boolean changeLength = false;
        boolean allOut = true;
        boolean add = false;
                
        public boolean isAllOut() { return allOut; }
        public void setAllOut(boolean val) { allOut = val; }
        public int getLength() { return length; }
        public void setLength(int val) { length = val; }
        public int getOut() { return out; }
        public void setOut(int val) { out = val; }
        public int getTranspose() { return transpose; }
        public void setTranspose(int val) { transpose = val; }
        public double getTransposeVariance() { return transposeVariance; }
        public void setTransposeVariance(double val) { transposeVariance = val; }
        public double getGain() { return gain; }
        public void setGain(double val) { gain = val; }
        public double getGainVariance() { return gainVariance; }
        public void setGainVariance(double val) { gainVariance = val; }
        public double getReleaseGain() { return releaseGain; }
        public void setReleaseGain(double val) { releaseGain = val; }
        public double getReleaseGainVariance() { return releaseGainVariance; }
        public void setReleaseGainVariance(double val) { releaseGainVariance = val; }
        public boolean getChangeLength() { return changeLength; }
        public void setChangeLength(boolean val) { changeLength = val; }
        public boolean getAdd() { return add; }
        public void setAdd(boolean val) { add = val; }

        public ChangeNote() { }
        public String getType() { return CHANGE_NOTE; }
        public ChangeNote(JSONObject obj)
            {
            allOut= obj.optBoolean("a", true);
            transpose = obj.optInt("t", 0);
            transposeVariance = obj.optDouble("tv", 0);
            gain = obj.optDouble("g", 1.0);
            gainVariance = obj.optDouble("gv", 0);
            releaseGain = obj.optDouble("r", 1.0);
            releaseGainVariance = obj.optDouble("rv", 0);
            length = obj.optInt("l", 0);
            changeLength = obj.optBoolean("c", false);
            add = obj.optBoolean("d", false);
            }
        public JSONObject save() throws JSONException
            {
            JSONObject obj = super.save();
            obj.put("a", allOut);
            obj.put("t", transpose);
            obj.put("tv", transposeVariance);
            obj.put("g", gain);
            obj.put("gv", gainVariance);
            obj.put("r", releaseGain);
            obj.put("rv", releaseGainVariance);
            obj.put("l", length);
            obj.put("c", changeLength);
            obj.put("d", add);
            return obj;
            }
        }

    public class Delay extends Function
        {
        boolean original;
        //int initialDelay;
        int numTimes;                   // min is 0, max is MAX_DELAY_TIMES
        int delayInterval = Seq.PPQ / 4;                   // sixteenth note
        double cut;
        boolean random;
        
        public boolean getRandom() { return random; }
        public void setRandom(boolean val) { random = val; }
        
        public boolean getOriginal() { return original; }
        public void setOriginal(boolean val) { original = val; }        
        /*
          public int getInitialDelay() { return initialDelay; }
          public void setInitialDelay(int val) { initialDelay = val; }
        */
        public double getCut() { return cut; }
        public void setCut(double val) { cut = val; }
        public int getNumTimes() { return numTimes; }
        public void setNumTimes(int val) { numTimes = val; }
        public int getDelayInterval() { return delayInterval; }
        public void setDelayInterval(int val) { delayInterval = val; }

        public Delay() { }
        public String getType() { return DELAY; }
        public Delay(JSONObject obj)
            {
            original = obj.optBoolean("o", true);
            //initialDelay = obj.optInt("i", 0);
            numTimes = obj.optInt("n", 0);
            delayInterval = obj.optInt("d", 0);
            cut = obj.optDouble("c", 0);
            random = obj.optBoolean("r", false);
            }
        public JSONObject save() throws JSONException
            {
            JSONObject obj = super.save();
            obj.put("o", original);
            //obj.put("i", initialDelay);
            obj.put("n", numTimes);
            obj.put("d", delayInterval);
            obj.put("c", cut);
            obj.put("r", random);
            return obj;
            }
        }
                
    public class Drop extends Function
        {
        boolean cut;
        double probability = 0.0;

        public boolean getCut() { return cut; }
        public void setCut(boolean val) { cut = val; }
        public double getProbability() { return probability; }
        public void setProbability(double val) { probability = val; }

        public Drop() { }
        public String getType() { return DROP; }
        public Drop(JSONObject obj)
            {
            probability = obj.optDouble("p", 0);
            cut = obj.optBoolean("c", false);
            }
        public JSONObject save() throws JSONException
            {
            JSONObject obj = super.save();
            obj.put("p", probability);
            obj.put("c", cut);
            return obj;
            }
        }
    
    public class Noise extends Function
        {
        public static final int TYPE_BEND = 0;
        public static final int TYPE_CC = 1;
        public static final int TYPE_NRPN = 2;
        public static final int TYPE_RPN = 3;                   // why would you need this?
        public static final int TYPE_AFTERTOUCH = 4;
            
        double distVar;
        int parameterType;
        int parameter;
        int rate = Seq.PPQ / 4;                       // How often is the Noise updated?
                
        public double getDistVar() { return distVar; }
        public void setDistVar(double val) { distVar = val; }
        public int getParameterType() { return parameterType; }
        public void setParameterType(int val) { parameterType = val; }
        public int getParameter() { return parameter; }
        public void setParameter(int val) { parameter = val; }
        public int getRate() { return rate; }
        public void setRate(int val) { rate = val; }
                
        public Noise() { }
        public String getType() { return NOISE; }
        public Noise(JSONObject obj)
            {
            distVar = obj.optDouble("v", 1.0);
            parameterType = obj.optInt("pt", TYPE_BEND);
            parameter = obj.optInt("p", 0);
            rate = obj.optInt("r", Seq.PPQ);
            }
        public JSONObject save() throws JSONException
            {
            JSONObject obj = super.save();
            obj.put("v", distVar);
            obj.put("pt", parameterType);
            obj.put("p", parameter);
            obj.put("r", rate);
            return obj;
            }
        }
                
    public class Map extends Function
        {
        public static final int TYPE_BEND = 0;
        public static final int TYPE_CC = 1;
        public static final int TYPE_NRPN = 2;
        public static final int TYPE_RPN = 3;                   // why would you need this?
        public static final int TYPE_AFTERTOUCH = 4;
            

        /*
          X + A;
          A - X;
          X - A;
          X * A;
          Discretize[x,A]:   Round[x*a]/a
          1-Discretize[x,A]
          X ^ 2;
          X ^ 4;
          1-(1-X) ^ 2;
          1-(1-X) ^ 4;
        */
            
        public static final int MAP_NONE = 0;
        public static final int MAP_INV = 1;
        public static final int MAP_ADD = 2;
        public static final int MAP_SUBTRACT = 3;
        public static final int MAP_MULTIPLY = 4;
        public static final int MAP_DISCRETIZE = 5;
        public static final int MAP_INV_DISCRETIZE = 6;
        public static final int MAP_SQUARE = 7;
        public static final int MAP_SQUARE_SQUARE = 8;
        public static final int MAP_INV_SQUARE = 9;
        public static final int MAP_INV_SQUARE_SQUARE = 10;
        
        public static final int MAX_DISCRETIZATION = 16;

        int parameterType;
        int parameter;
        double variable;
        int map;
        double min;
        double max = 1.0;
                
        public double getMin() { return min; }
        public void setMin(double val) { min = val; }
        public double getMax() { return max; }
        public void setMax(double val) { max = val; }
        public int getMap() { return map; }
        public void setMap(int val) { map = val; }
        public int getParameterType() { return parameterType; }
        public void setParameterType(int val) { parameterType = val; }
        public int getParameter() { return parameter; }
        public void setParameter(int val) { parameter = val; }
        public double getVariable() { return variable; }
        public void setVariable(double val) { variable = val; }
                
                
        public double map(double val, double variable)
            {
            switch(map)
                {
                case MAP_NONE:
                    return val;
                case MAP_INV:
                    return Math.max(0.0, variable - val);
                case MAP_ADD:
                    return Math.min(1.0, val + variable);
                case MAP_SUBTRACT:
                    return Math.max(0.0, val + variable);
                case MAP_MULTIPLY:
                    return val * variable;
                case MAP_DISCRETIZE:
                {
                double v = (int)(variable * MAX_DISCRETIZATION) + 1;            // note double and int
                if (v > 16) v = 16;             // When variable == 1.0
                return Math.round(val * v) / v;
                }
                case MAP_INV_DISCRETIZE:
                    double v = (int)(variable * MAX_DISCRETIZATION) + 1;            // note double and int
                    if (v > 16) v = 16;             // When variable == 1.0
                    return 1.0 - Math.round(val * v) / v;
                case MAP_SQUARE:
                    return val * val;
                case MAP_SQUARE_SQUARE:
                    return val * val * val * val;
                case MAP_INV_SQUARE:
                    return 1 - (1 - val) * (1 - val);
                case MAP_INV_SQUARE_SQUARE:
                    return 1 - (1 - val) * (1 - val) * (1 - val) * (1 - val);
                }
            System.err.println("Filter.map(), unreachable position");
            return val;             // uhm...
            }
                
        public Map() { }
        public String getType() { return MAP; }
        public Map(JSONObject obj)
            {
            min = obj.optDouble("min", 0.0);
            max = obj.optDouble("max", 1.0);
            map = obj.optInt("map", 1);
            parameterType = obj.optInt("pt", TYPE_BEND);
            parameter = obj.optInt("p", 0);
            variable = obj.optDouble("v", 1.0);
            }
        public JSONObject save() throws JSONException
            {
            JSONObject obj = super.save();
            obj.put("map", map);
            obj.put("min", min);
            obj.put("max", max);
            obj.put("pt", parameterType);
            obj.put("p", parameter);
            obj.put("v", variable);
            return obj;
            }
        }
                
    public class Scale extends Function
        {
        public static final int ROUND_DOWN = 0;
        public static final int ROUND_UP = 1;
        public static final int ROUND_NEAREST_DOWN = 2;
        public static final int ROUND_NEAREST_UP = 3;
        
        int key = 0;                            // C is 0
        boolean[] scale = new boolean[12];
        int round = ROUND_DOWN;

        public boolean getScale(int index) { return scale[index]; }
        public void setScale(int index, boolean val) { scale[index] = val; }
        public void setScale(int scale)
            {
            for(int i = 0; i < 12; i++)
                {
                this.scale[i] = (SCALES[scale][i] == 1);
                }
            }
        public int getKey() { return key; }
        public void setKey(int val) { key = val; }
        public int getRound() { return round; }
        public void setRound(int val) { round = val; }
        public int getRoundedNote(int note)
            {
            int under;
            int over;
            
            // Transpose note to position where key = C
            
            int pos = (((note % 12) - getKey()) + 12) % 12;
            
            // Easiest situation: we're a legal note in the scale
            if (scale[pos]) // we're done
                return note;
                        
            // Find under
            for(under = pos; !scale[(under % 12 + 12) % 12]; under--) 
                { 
                if (under + 12 == pos) return note; // this happens if we have NO notes selected, oops
                } 
                
            if (round == ROUND_DOWN) 
                {
                int val = note - pos + under;
                return (val < 0 ? 0 : val > 127 ? 127 : val);
                }
                        
            // Find over
            for(over = pos; !scale[over % 12]; over++)
                { 
                if (over - 12 == pos) return note; // this happens if we have NO notes selected, oops
                } 

            if (round == ROUND_UP) 
                {
                int val = note - pos + over;
                return (val < 0 ? 0 : val > 127 ? 127 : val);
                }

            // (round == ROUND_NEAREST_DOWN || round == ROUND_NEAREST_UP)
            int val = 0;
            if (pos - under > over - pos)
                {
                val = note - pos + over;
                }
            else if (pos - under < over - pos)
                {
                val = note - pos + under;
                }
            else if (round == ROUND_NEAREST_DOWN)
                {
                val = note - pos + under;
                }
            else            // round == ROUND_NEAREST_UP
                {
                val = note - pos + over;
                }
            return (val < 0 ? 0 : val > 127 ? 127 : val);
                        
//            return note;            // uh, bug
            }
        

        public Scale() 
            { 
            for(int i = 0; i < 12; i++)
                {
                scale[i] = true;
                }
            }
        public String getType() { return SCALE; }
        public Scale(JSONObject obj)
            {
            round = obj.optInt("r", 0);
            key = obj.optInt("k", 0);
            JSONArray array = obj.getJSONArray("s");
            if (array != null)
                {
                for(int i = 0; i < 12; i++)
                    {
                    scale[i] = array.optBoolean(i, false);
                    }
                }
            }
        public JSONObject save() throws JSONException
            {
            JSONObject obj = super.save();
            obj.put("r", round);
            obj.put("k", key);
            JSONArray array = new JSONArray();
            for(int i = 0; i < 12; i++)
                {
                array.put(i, scale[i]);
                }
            obj.put("s", array);
            return obj;
            }
        public Scale copy() 
            { 
            Scale other = (Scale)(super.copy());
            for(int i = 0; i < scale.length; i++)
                {
                other.scale[i] = scale[i];
                }
            return other;
            }
        }

    public class Chord extends Function
        {
        int chord = 0;

        public int getChord() { return chord; }
        public void setChord(int val) { chord = val; }

        public Chord() { }
        public String getType() { return CHORD; }
        public Chord(JSONObject obj)
            {
            chord = obj.optInt("c", 0);
            }
                
        public JSONObject save() throws JSONException
            {
            JSONObject obj = super.save();
            obj.put("c", chord);
            return obj;
            }
        }


    public void add(Motif motif)
        {
        addChild(motif);
        } 

        
    public static int ALL = -1;
    
    Function[] functions = new Function[NUM_TRANSFORMERS];
    int from = 0;
    int to = 0;
    boolean always = true;
    int outRestriction = ALL;
    
    public int getOutRestriction() { return outRestriction; }
    public void setOutRestriction(int val) { outRestriction = val; }
    
    public int getFrom()
        {
        return from;
        }
    
    public void setFrom(int val)
        {
        from = val;
        }
    
    public int getTo()
        {
        return to;
        }
    
    public void setTo(int val)
        {
        to = val;
        }
    
    public boolean isAlways()
        {
        return always;
        }
    
    public void setAlways(boolean val)
        {
        always = val;
        }
    
    public Function getFunction(int index)
        {
        return functions[index];
        }
        
    public void setFunction(int index, Function function)
        {
        functions[index] = function;
        // incrementVersion();
       // Clip clip = getPlayingClip();
       // if (clip != null) clip.rebuild();
        }
    
    public Filter(Seq seq)
        {
        super(seq);
        
        for(int i = 0; i < NUM_TRANSFORMERS; i++)
            {
            functions[i] = new Function();
            }
        add(new Blank(seq));
        }

    public Clip buildClip(Clip parent)
        {
        return new FilterClip(seq, this, parent);
        }
        
    public void load(JSONObject obj) throws JSONException
        {
        for(int i = 0; i < NUM_TRANSFORMERS; i++)
            {
            functions[i] = new Function();
            }
        JSONArray array = obj.getJSONArray("func");
        for(int i = 0; i < Math.min(NUM_TRANSFORMERS, array.length()); i++)
            {
            functions[i] = loadFunction(array.getJSONObject(i));
            }
        setFrom(obj.optInt("from", 0));
        setTo(obj.optInt("to", 0));
        setAlways(obj.optBoolean("always", false));
        setOutRestriction(obj.optInt("out", ALL));
        }
                        
    public void save(JSONObject obj) throws JSONException
        {
        JSONArray array = new JSONArray();
        for(int i = 0; i < NUM_TRANSFORMERS; i++)
            {
            array.put(i, functions[i].save());
            }
        obj.put("func", array);
        obj.put("from", getFrom());
        obj.put("to", getTo());
        obj.put("always", isAlways());
        obj.put("out", getOutRestriction());
        }

    public String getParameterName(int param) 
        { 
        if (getChildren().size() > 0)
            {
            Motif motif = getChild(0).getMotif();
            if (motif instanceof Blank)
                {
                return super.getParameterName(param);
                }
            else
                {
                return motif.getParameterName(param);
                }
            }
        else
            {
            return super.getParameterName(param);
            }
        }

    static int document = 0;
    static int counter = 1;
    public int getNextCounter() { if (document < Seq.getDocument()) { document = Seq.getDocument(); counter = 1; } return counter++; }
        
    public Motif copy()
        {
        Filter other = (Filter)(super.copy());
        for(int i = 0; i < functions.length; i++)
            {
            other.functions[i] = functions[i].copy();
            }
        return other;
        }

    public String getBaseName() { return "Filter"; }
    }
        
