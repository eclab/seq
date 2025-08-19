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

/*
  public static final int QUANTIZE = 0;               // quantize to scales and chords
  public static final int BLOCK = 0;                  // prevent entirely
  public static final int UNISON = 0;                 // do unison notes
*/


    public static final int NUM_TRANSFORMERS = 4;
    public static final int MAX_TRANSPOSE_NOISE = 24;       // 12 notes
    public static final double MAX_TRANSPOSE_GAIN = 4.0;    // 4x, since we add 1
    public static final int MAX_TRANSPOSE = 24;
    public static final int MAX_DELAY_NUM_TIMES = 16;
        

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
        int transpose = MAX_TRANSPOSE;          // this would center it at 0
        double transposeVariance;
        double gain = 1.0;
        double gainVariance;
        double releaseGain = 1.0;                       // if length is changes, the base release will be changed to 0x64
        double releaseGainVariance;
        int length;                                                             // length = 0 means we don't change the length
        boolean allOut = true;
                
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
            return obj;
            }
        }

    public class Delay extends Function
        {
        boolean original;
        //int initialDelay;
        int numTimes;                   // min is 0, max is MAX_DELAY_TIMES
        int laterDelay = Seq.PPQ / 4;                   // sixteenth note
        double cut;
        
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
        public int getLaterDelay() { return laterDelay; }
        public void setLaterDelay(int val) { laterDelay = val; }

        public Delay() { }
        public String getType() { return DELAY; }
        public Delay(JSONObject obj)
            {
            System.err.println("Load Delay " + obj);
            original = obj.optBoolean("o", true);
            //initialDelay = obj.optInt("i", 0);
            numTimes = obj.optInt("n", 0);
            laterDelay = obj.optInt("d", 0);
            cut = obj.optDouble("c", 0);
            }
        public JSONObject save() throws JSONException
            {
            JSONObject obj = super.save();
            obj.put("o", original);
            //obj.put("i", initialDelay);
            obj.put("n", numTimes);
            obj.put("d", laterDelay);
            obj.put("c", cut);
            System.err.println("Save Delay " + obj);
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
        int rate;                       // How often is the Noise updated?
                
        public double getDistVar() { return distVar; }
        public void setDistVar(double val) { distVar = val; }
        public int getParameterType() { return parameterType; }
        public void setParameterType(int val) { parameterType = val; }
        public int getParameter() { return parameter; }
        public void setParameter(int val) { parameter = val; }
        public int getRate() { return rate; }
        public void setRate(int val) { rate = val; }
                
        public double generateRandomNoise(Random rand)
            {
            return rand.nextDouble() * (distVar * 2) - distVar;
            }

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
                
    public void add(Motif motif)
        {
        addChild(motif);
        } 

        
    Function[] functions = new Function[NUM_TRANSFORMERS];
    int from = 0;
    int to = 0;
    boolean always = true;
    
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
        incrementVersion();
        Clip clip = getPlayingClip();
        if (clip != null) clip.rebuild();
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
        }

    static int document = 0;
    static int counter = 1;
    public int getNextCounter() { if (document < Seq.getDocument()) { document = Seq.getDocument(); counter = 1; } return counter++; }
        
    public String getBaseName() { return "Filter"; }
    }
        
