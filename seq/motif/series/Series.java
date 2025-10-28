/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.series;

import seq.engine.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

public class Series extends Motif
    {
    static final long serialVersionUID = 1;

    public static final int QUANTIZATION_NONE = 0;
    public static final int QUANTIZATION_SIXTEENTH = 1;
    public static final int QUANTIZATION_QUARTER = 2;
    public static final int QUANTIZATION_FOUR_QUARTERS = 3;


    public static final int NONE = 0;
    public static final int CC7 = 1;
    public static final int CC14 = 2;
    public static final int NRPN = 3;
    public static final int NRPN_COARSE = 4;
    public static final int RPN = 5;
    public static final int BEND = 6;
    public static final int AFTERTOUCH = 7;
        
    int[] midiTypes = new int[NUM_PARAMETERS];
    int[] midiParameters = new int[NUM_PARAMETERS];
    int midiParameterOut = 0;
    int midiParameterRate = Seq.PPQ / 4;
        
    public void setMIDIType(int index, int type) 
        { 
        midiTypes[index] = type; 
        if (type == NONE || type == BEND || type == AFTERTOUCH) setMIDIParameter(index, 0);
        if (type == CC14 && getMIDIParameter(index) > 31) setMIDIParameter(index, 0);
        if (type == CC7 && getMIDIParameter(index) > 127) setMIDIParameter(index, 0);
        }
    public int getMIDIType(int index) { return midiTypes[index]; }
    
    public int getMIDIParameterRate() { return midiParameterRate; }
    public void setMIDIParameterRate(int val) { midiParameterRate = val; } 

    public void setMIDIParameter(int index, int param) { midiParameters[index] = param; }
    public int getMIDIParameter(int index) { return midiParameters[index]; }
        
    public void setMIDIParameterOut(int val) { midiParameterOut = val; }
    public int getMIDIParameterOut() { return midiParameterOut; }

    public static class Data
        {
        public static final int DISABLED = -1;
        
        public static final int MAX_REPEAT_VALUE = 127;                // values go 0..127 inclusive
 
        // public static final double MAX_DELAY = 256.0;               // in beats
        public static final int MAX_TRANSPOSE = 24;
        public static final double MAX_GAIN = 4.0;
        public static final double MAX_RATE = 16.0;
        public static final double DEFAULT_RATE = 1.0;
    
        /*
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
        */
         
        int repeatAtLeast;
        double repeatProbability;
        boolean repeatUntilTrigger;
        double rate = DEFAULT_RATE;
        int transpose = MAX_TRANSPOSE;                                        // ranges 0 ... MAX_TRANSPOSE * 2 inclusive, representing -MAX_TRANSPOSE ... MAX_TRANSPOSE
        double gain = 1;
        int out = DISABLED;
        //int length = DISABLED;
        double[] weights = new double[0];
        int endingQuantization = QUANTIZATION_NONE;
        boolean start = false;    
        
        public void setStart(boolean val, Series series) 
            { 
            if (val)
                {
                // clear everyone
                for(Child child : series.getChildren())
                    {
                    ((Data)(child.getData())).start = false;
                    }
                }
            start = val;
            }
        public boolean getStart() { return start; }



        public int getEndingQuantization() { return endingQuantization; }
        public void setEndingQuantization(int val) { endingQuantization = val; }

        public double getRepeatProbability() { return repeatProbability; }
        public int getRepeatAtLeast() { return repeatAtLeast; }
        
        public void setRepeatProbability(double val) { repeatProbability = val; }
        public void setRepeatAtLeast(int val) { repeatAtLeast = val; }

        public boolean getRepeatUntilTrigger() { return repeatUntilTrigger; }
        public void setRepeatUntilTrigger(boolean val) { repeatUntilTrigger = val; }

        public void setTranspose(int transpose) { this.transpose = transpose; }
        public int getTranspose() { return transpose; }

/*
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
  // int[] _scale = scales[scale];
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
//              return note;            // should never happen
}
*/

        public void setGain(double gain) { this.gain = gain; }
        public double getGain() { return gain; }

        public void setOut(int out) { this.out = out; }
        public int getOut() { return out; }
        
        public double getRate() { return rate; }
        public void setRate(double val) { rate = val; }
        
        public double[] getWeights() { return weights; }
        public void setWeights(double[] val) { weights = val; }
        
        public void swapWeights(int indexa, int indexb) 
            {
            double d = weights[indexa];
            weights[indexa] = weights[indexb];
            weights[indexb] = d;
            }
                
        public void removeWeightFrom(int to)
            {
            double[] newWeights = new double[weights.length - 1];
            for(int i = to; i < newWeights.length; i++)
                {
                newWeights[i] = weights[i + 1];
                }
            weights = newWeights;
            }
        
        void moveWeightToEnd(int index)
            {
            if (index == weights.length - 1) return;
                        
            double d = weights[index];
            for(int j = index; j < weights.length - 1; j++)
                {
                weights[j] = weights[j + 1];
                }
            weights[weights.length - 1] = d;
            }
                
        void moveEndToBefore(int before)
            {
            if (before == weights.length) return;

            double d = weights[before];
            for(int j = before; j < weights.length - 1; j++)
                {
                weights[j] = weights[j + 1];
                }
            weights[weights.length - 1] = d;
            }
                
        public void moveWeight(int index, int before)
            {
            if (index == before) return;
            moveWeightToEnd(index);
            moveEndToBefore(before);
            }

        public void copyWeight(int index, int before)
            {
            addWeight(weights[index]);
            moveEndToBefore(before);
            }

        // adds to end
        public void addWeight(double val)
            {
            double[] newWeights = new double[weights.length + 1];
            System.arraycopy(weights, 0, newWeights, 0, weights.length);
            newWeights[newWeights.length - 1] = val;
            weights = newWeights;
            }
        
        // NOTE: Weights is null at this point.  We update it in
        // addWeights to make sure we have the right total number
        public Data() { }
        
        public Data(Data other)
            {
            repeatAtLeast = other.repeatAtLeast;
            repeatProbability = other.repeatProbability;
            rate = other.rate;
            transpose = other.transpose;
            gain = other.gain;
            out = other.out;
            weights = copy(other.weights);
//            scale = other.scale;
//            root = other.root;
            }
        }


    // CHILD MANIPULATION
    // These functions override the originals to also update the weights
        
    // Sets the incoming and outgoing weights to 0
    public Child addChild(Motif motif) 
        {
        Child child = super.addChild(motif);
        ((Data)(child.getData())).setWeights(new double[getChildren().size() - 1]);
        for(Child c : getChildren())
            {
            ((Data)(c.getData())).addWeight(0);
            }
        return child;
        }

    // Sets incoming and outgoing weights to 0
    public Child replaceChild(Motif motif, int at) 
        {
        Child oldChild = super.replaceChild(motif, at);
        Child newChild = getChildren().get(at);
        ((Data)(newChild.getData())).setWeights(new double[getChildren().size()]);
        for(Child c : getChildren())
            {
            ((Data)(c.getData())).weights[at] = 0;
            }
        return oldChild;
        }
    
    // copies the weights as well
    public Child copyChild(int from, int to) 
        {
        Child child = super.copyChild(from, to);
        for(Child c : getChildren())
            {
            ((Data)(c.getData())).copyWeight(from, to);
            }
        return child;
        }
        
    public void moveChild(int from, int toBefore)
        {
        super.moveChild(from, toBefore);
        for(Child child : getChildren())
            {
            ((Data)(child.getData())).moveWeight(from, toBefore);
            }
        }

    public void swapChild(int from, int to)
        {
        super.swapChild(from, to);
        for(Child child : getChildren())
            {
            ((Data)(child.getData())).swapWeights(from, to);
            }
        }

    // Child retains his old weights
    public Child removeChild(int index)
        {
        Child child = super.removeChild(index);
        for(Child c : getChildren())
            {
            ((Data)(c.getData())).removeWeightFrom(index);
            }
        return child;
        }




    protected Object buildData(Motif motif) { return new Data(); }

    protected Object copyData(Motif motif, Object data) 
        { 
        Data other = new Data((Data)data); 
        other.weights = (double[])(((Data)data).weights.clone());
        return other;
        }

    protected void saveData(Object data, Motif motif, JSONObject to) 
        {
        Data d = (Data)data;
        to.put("least", d.repeatAtLeast);
        to.put("prob", d.repeatProbability);
        to.put("trig", d.repeatUntilTrigger);
        to.put("rate", d.rate);
        to.put("tran", d.transpose);
        to.put("gain", d.gain);
        to.put("out", d.out);
        //to.put("len", d.length);
        to.put("start", d.start);
        JSONArray w = new JSONArray();
        for(int j = 0; j < d.weights.length; j++)
            {
            w.put(d.weights[j]);
            }
        to.put("weight", w);
        to.put("equant", d.endingQuantization);
        }

    protected Object loadData(Motif motif, JSONObject from) 
        { 
        Data d = (Data)buildData(motif);
        d.start = from.optBoolean("start", false);
        d.repeatAtLeast = from.optInt("least", 0);
        d.repeatProbability = from.optDouble("prob", 0);
        d.repeatUntilTrigger = from.optBoolean("trig", false);
        d.transpose = from.optInt("tran", Data.MAX_TRANSPOSE);
        d.rate = from.optDouble("rate", 1.0);
        d.gain = from.optDouble("gain", 1.0);
        d.out = from.optInt("out", Data.DISABLED);
        //d.length = from.optInt("len");
        JSONArray w = from.getJSONArray("weight");
        d.weights = new double[w.length()];
        d.endingQuantization = from.getInt("equant");
        for(int j = 0; j < w.length(); j++)
            {
            d.weights[j] = w.getInt(j);
            }
        return d;
        }

 
    public Clip buildClip(Clip parent)
        {
        return new SeriesClip(seq, this, parent);
        }
        

    public Series(Seq seq)
        {
        super(seq);
        }
                
                
    public static final int MODE_SERIES = 0;
    public static final int MODE_SHUFFLE = 1;
    public static final int MODE_RANDOM = 2;
    public static final int MODE_MARKOV = 3;
    public static final int MODE_ROUND_ROBIN = 4;
    public static final int MODE_VARIATION = 5;
    public static final int MODE_RANDOM_VARIATION = 6;
    
    int mode;
    
    public int getMode() { return mode; }
    public void setMode(int val) 
        { 
        mode = val; 
        }

    public void add(Motif motif, int repeatAtLeast, double repeatProbability)
        {
        Motif.Child child = addChild(motif);
        Data data = (Data)(child.getData());
        data.repeatAtLeast = repeatAtLeast;
        data.repeatProbability = repeatProbability;
        } 
        
    public Motif copy()
        {
        Series other = (Series)(super.copy());
        for(int i = 0; i < NUM_PARAMETERS; i++)
            {
            other.setMIDIParameter(i, getMIDIParameter(i));
            other.setMIDIType(i, getMIDIType(i));
            }
        other.midiParameterOut = midiParameterOut;
        return other;
        }

    public void save(JSONObject to) throws JSONException 
        {
        to.put("mode", mode); 
        JSONArray typeArray = new JSONArray();
        for(int i = 0; i < NUM_PARAMETERS; i++)
            {
            JSONObject obj = new JSONObject();
            typeArray.put(getMIDIType(i));
            typeArray.put(getMIDIParameter(i));
            }
        to.put("midi", typeArray);
        to.put("out", getMIDIParameterOut());
        to.put("rate", getMIDIParameterRate());
/*
  JSONObject l = new JSONObject();
  lfo.save(l);
  to.put("lfo", l);
*/
        }

    public void load(JSONObject from) throws JSONException
        {
        mode = from.optInt("mode", MODE_SERIES);

        JSONArray typeArray = from.optJSONArray("midi");
        if (typeArray != null)
            {
            for(int i = 0; i < NUM_PARAMETERS; i++)
                {
                setMIDIType(i, typeArray.optInt(i * 2, NONE));
                setMIDIParameter(i, typeArray.optInt(i * 2 + 1, 0));
                }
            }
        else
            {
            System.err.println("Modulation.save(): Internal error: no LFO array");
            }
        setMIDIParameterOut(from.optInt("out", 0));
        setMIDIParameterRate(from.optInt("rate", Seq.PPQ / 4));


        /*
          JSONObject l = from.getJSONObject("lfo");
          lfo.load(l);
        */
        }
        
    static int document = 0;
    static int counter = 1;
    public int getNextCounter() { if (document < Seq.getDocument()) { document = Seq.getDocument(); counter = 1; } return counter++; }
        
    public String getBaseName() { return "Series"; }
    }
        
