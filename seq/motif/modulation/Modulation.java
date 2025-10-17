/* 
   Copyright 2025 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.modulation;

import seq.engine.*;
import seq.motif.blank.*;
import seq.util.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;


public class Modulation extends Motif
    {
    private static final long serialVersionUID = 1;
    
    public static final String IDENTITY = "Identity";
    public static final String LFO = "LFO";
    public static final String ENVELOPE = "Envelope";
    public static final String STEP = "Step";                           // Repeating Step Sequence
    public static final String SAME = "Same";
    public static final String CONSTANT = "Constant";

    // this index order is just the order of the combo box in ModulationChildInspector
    public static int typeIndex(String type)
        {
        if (IDENTITY.equals(type))
            {
            return 0;
            }
        else if (LFO.equals(type))
            {
            return 1;
            }
        else if (ENVELOPE.equals(type))
            {
            return 2;
            }
        else if (STEP.equals(type))
            {
            return 3;
            }
        else if (CONSTANT.equals(type))
            {
            return 4;
            }
        else if (SAME.equals(type))
            {
            return 5;
            }
        else
            {
            System.err.println("Modulation.typeIndex: could not find type " + type);
            return 0;
            }
        }
        
    // this index order is just the order of the combo box in ModulationChildInspector
    public Function buildFunction(int index)
        {
        if (index == 0)
            {
            return new Function();
            }
        else if (index == 1)
            {
            return new LFO();
            }
        else if (index == 2)
            {
            return new Envelope();
            }
        else if (index == 3)
            {
            return new Step();
            }
        else if (index == 4)
            {
            return new Constant();
            }
        else if (index == 5)
            {
            return new Same();
            }
        else
            {
            System.err.println("Modulation.loadFunction: could not create function " + index);
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
        else if (LFO.equals(type))
            {
            return new LFO(obj);
            }
        else if (ENVELOPE.equals(type))
            {
            return new Envelope(obj);
            }
        else if (STEP.equals(type))
            {
            return new Step(obj);
            }
        else if (CONSTANT.equals(type))
            {
            return new Constant(obj);
            }
        else if (SAME.equals(type))
            {
            return new Same(obj);
            }
        else
            {
            System.err.println("Modulation.loadFunction: could not load function " + obj);
            return new Function(obj);
            }
        }
        
    public class Function implements Cloneable
        {
        public static final int BY_LINEAR = 0;                          // x
        public static final int BY_SQUARE = 1;                          // x^2
        public static final int BY_SQUARE_SQUARE = 2;           // x^4
        public static final int BY_INV_SQUARE = 3;                      // 1-(1-x)^2
        public static final int BY_INV_SQUARE_SQUARE = 4;       // 1-(1-x)^4

        double mapHigh = 1.0;                   // The high value gets mapped to this value in the interpolation before final output
        double mapLow = 0.0;                    // The low value gets mapped to this value in the interpolation before final output
        int mapBy;                              // This is the mapping function, see above
        
        public double getMapHigh() { return mapHigh; }
        public void setMapHigh(double val) { mapHigh = val; }
        public double getMapLow() { return mapLow; }
        public void setMapLow(double val) { mapLow  = val; }
        public int getMapBy() { return mapBy; }
        public void setMapBy(int val) { mapBy = val; }
        
        public String getType() { return IDENTITY; }
        public Function()
            {
            }
        public Function(JSONObject obj)
            {
            mapHigh = obj.optDouble("hi", 1.0);
            mapLow = obj.optDouble("lo", 0.0);
            mapBy = obj.optInt("hi", BY_LINEAR);
            }
        public JSONObject save() throws JSONException
            {
            JSONObject obj = new JSONObject();
            obj.put("type", getType());
            obj.put("hi", getMapHigh());
            obj.put("lo", getMapLow());
            obj.put("by", getMapBy());
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
            
        public double map(double val)
            {
            switch(mapBy)
                {
                case BY_LINEAR:         // do nothing
                    break;
                case BY_SQUARE:
                    val = val * val;
                    break;
                case BY_SQUARE_SQUARE:
                    val = val * val * val * val;
                    break;
                case BY_INV_SQUARE:
                    val = 1.0 - (1.0 - val) * (1.0 - val);
                    break;
                case BY_INV_SQUARE_SQUARE:
                    val = 1.0 - (1.0 - val) * (1.0 - val) * (1.0 - val) * (1.0 - val);
                    break;
                }
            return mapLow + (mapHigh - mapLow) * val;
            }
        }

    public class Same extends Function
        {
        int as;                                                         // index of the function we're the same as.  If as >= my index, we're just identity
        public int getAs() { return as; }
        public void setAs(int val) { as = val; }
        
        public String getType() { return SAME; }
        public Same()
            {
            }
        public Same(JSONObject obj)
            {
            as = obj.optInt("as", 0);
            }
        public JSONObject save() throws JSONException
            {
            JSONObject obj = super.save();
            obj.put("as", getAs());
            return obj;
            }
        }


    // These are outside LFO because we already have a constant
    // called LFO and Java is not very smart, see the switch
    // in ModulationClip
    public static final int TYPE_SAW_UP = 0;
    public static final int TYPE_SAW_DOWN = 1;
    public static final int TYPE_SQUARE = 2;
    public static final int TYPE_TRIANGLE = 3;
    public static final int TYPE_SINE = 4;
    public static final int TYPE_RANDOM = 5;
    public static final int TYPE_SAMPLE_AND_HOLD = 6;

    public class LFO extends Function
        {
        // LFO starts at a START time.  It then fades in over a FADE IN interval.
        // It then runs for a certain LENGTH interval.  Then it fades out over a FADE OUT interval.
        // Prior to and after all this, it outputs INITIAL [it fades in from and fades out to INITIAL as well).
        // 
        // LFO has a PERIOD for its wave measured in timesteps.  PERIOD should be > 0.
        // LFO also has PHASE which determines how it starts.  The phase goes from 0.0 to 1.0 inclusive.
        // LFO has several TYPES, determining the wave shapes.
        
        int start;
        double initial;
        int length = Seq.MIN_MAX_TIME;
        int fadeOut;
        int fadeIn;
                
        int period = Seq.PPQ;
        double phase = 1.0;
        int lfoType;

        public double getInitial() { return initial; }
        public void setInitial(double val) { initial = val; } 
        public int getStart() { return start; }
        public void setStart(int val) { start = val; }
        public int getFadeIn() { return fadeIn; }
        public void setFadeIn(int val) { fadeIn = val; }
        public int getLength() { return length; }
        public void setLength(int val) { length = val; }
        public int getFadeOut() { return fadeOut; }
        public void setFadeOut(int val) { fadeOut = val; }
        public int getPeriod() { return period; }
        public void setPeriod(int val) { period = val; }
        public int getLFOType() { return lfoType; }
        public void setLFOType(int val) { lfoType = val; }
        public double getPhase() { return phase; }
        public void setPhase(double val) { phase = val; }
                
        public LFO() { }
        public String getType() { return LFO; }
        public LFO(JSONObject obj)
            {
            phase = obj.optDouble("p", 1.0);
            start = obj.optInt("s", 0);
            fadeIn = obj.optInt("fi", 0);
            length = obj.optInt("l", Seq.MIN_MAX_TIME);
            fadeOut = obj.optInt("fo", 0);
            period = obj.optInt("pe", 0);
            lfoType = obj.optInt("lt", 0);
            initial = obj.optDouble("i", 0);
            }
        public JSONObject save() throws JSONException
            {
            JSONObject obj = super.save();
            obj.put("p", phase);
            obj.put("s", start);
            obj.put("fi", fadeIn);
            obj.put("l", length);
            obj.put("fo", fadeOut);
            obj.put("pe", period);
            obj.put("lt", lfoType);
            obj.put("i", initial);
            return obj;
            }
        }

    public static final int MAX_STAGES = 8;
        
    public class Envelope extends Function
        {
        // Envelope starts at a START time.  Prior to that it sets the parameter to INITIAL.
        // There are NUMSTAGES stages.
        // Between the START time and time[0], it interpolates from INITIAL to target[0].
        // Between time[i] and time[i+1], it interpolates from target[i] to target[i+1].
        // If envelope is REPEAT, then after it reaches time[NUMSTAGES - 1] it wraps back around
        // to the START time.
        // If envelope is not REPEAT, then after time[NUMSTAGES - 1] it outputs target[NUMSTAGES - 1]
        // If Envelope is HOLD then it doesn't interpolate but rather outputs target[i], or
        // INTIIAL if i = 0;
        
        int start;
        double initial;
        int numStages = MAX_STAGES;
        boolean repeat;
        boolean hold;
        int[] time = new int[MAX_STAGES];
        double[] target = new double[MAX_STAGES];
        
        public int getStart() { return start; }
        public void setStart(int val) { start = val; }
        public double getInitial() { return initial; }
        public void setInitial(double val) { initial = val; }
        public int getNumStages() { return numStages; }
        public void setNumStages(int val) { numStages = val; }
        public int getTime(int stage) { return time[stage]; }
        public void setTime(int stage, int val) { time[stage] = val; }
        public double getTarget(int stage) { return target[stage]; }
        public void setTarget(int stage, double val) { target[stage] = val; }
        public boolean getHold() { return hold; }
        public void setHold(boolean val) { hold = val; }
        public boolean getRepeat() { return repeat; }
        public void setRepeat(boolean val) { repeat = val; }
                
        public Envelope() { }
        public String getType() { return ENVELOPE; }
        public Envelope(JSONObject obj)
            {
            start = obj.optInt("s", 0);
            numStages = obj.optInt("n", MAX_STAGES);
            initial = obj.optDouble("i", 0);
            hold = obj.optBoolean("h", false);
            repeat = obj.optBoolean("r", false);
            JSONArray array = obj.getJSONArray("ti");
            for(int i = 0; i < Math.min(numStages, array.length()); i++)
                {
                time[i] = array.optInt(i, 0);
                }
            array = obj.getJSONArray("ta");
            for(int i = 0; i < Math.min(numStages, array.length()); i++)
                {
                target[i] = array.optDouble(i, 0);
                }
            }
        public JSONObject save() throws JSONException
            {
            JSONObject obj = super.save();
            obj.put("s", start);
            obj.put("n", numStages);
            obj.put("i", initial);
            obj.put("h", hold);
            obj.put("r", repeat);
            JSONArray array = new JSONArray();
            for(int i = 0; i < numStages; i++)
                {
                array.put(i, time[i]);
                }
            obj.put("ti", array);
            array = new JSONArray();
            for(int i = 0; i < numStages; i++)
                {
                array.put(i, target[i]);
                }
            obj.put("ta", array);
            return obj;
            }

        public Function copy() 
            { 
            Envelope other = (Envelope)(super.copy());
            other.time = (int[])(time.clone());
            other.target = (double[])(target.clone());
            return other;
            }
        }
                
    public static final int MAX_STEPS = 16;
        
    public class Step extends Function
        {
        // Step starts at a START time.  Prior to that it sets the parameter to INITIAL.
        // There are NUMSTEPS steps.  Each step lasts a PERIOD amount of time.
        // After the START time, it determines the current step, and outputs STEP[step].
        // If REPEAT, then it when it finishes the last step, it wraps around to the first step.
        // If NOT REPEAT, then when it finishes the last step, it outputs INITIAL again.
        // If TRIGGER, then instead of outputting STEP[step], if STEP[step] > 0, then it outputs
        // a 1 on the first timestep in STEP, and 0 for the others; if STEP[step] = 0, it then
        // just outputs a 0.
        
        int start;
        double initial;
        int period = Seq.PPQ;
        int numSteps = MAX_STEPS;
        double[] step = new double[MAX_STEPS];
        boolean repeat;
        boolean trigger;
        
        public int getStart() { return start; }
        public void setStart(int val) { start = val; }
        public int getPeriod() { return period; }
        public void setPeriod(int val) { period = val; }
        public double getInitial() { return initial; }
        public void setInitial(double val) { initial = val; }
        public int getNumSteps() { return numSteps; }
        public void setNumSteps(int val) { numSteps = val; }
        public double getStep(int stage) { return step[stage]; }
        public void setStep(int stage, double val) { step[stage] = val; }
        public boolean getRepeat() { return repeat; }
        public void setRepeat(boolean val) { repeat = val; }
        public boolean getTrigger() { return trigger; }
        public void setTrigger(boolean val) { trigger = val; }
                
        public Step() { }
        public String getType() { return STEP; }
        public Step(JSONObject obj)
            {
            start = obj.optInt("s", 0);
            period = obj.optInt("p", Seq.PPQ);
            initial = obj.optDouble("i", 0);
            numSteps = obj.optInt("n", 16);
            repeat = obj.optBoolean("re", true);
            trigger = obj.optBoolean("t", false);
            JSONArray array = obj.getJSONArray("r");
            for(int i = 0; i < Math.min(numSteps, array.length()); i++)
                {
                step[i] = array.optDouble(i, 0);
                }
            }
        public JSONObject save() throws JSONException
            {
            JSONObject obj = super.save();
            obj.put("s", start);
            obj.put("pe", period);
            obj.put("i", initial);
            obj.put("n", numSteps);
            obj.put("re", repeat);
            obj.put("t", trigger);
            JSONArray array = new JSONArray();
            for(int i = 0; i < numSteps; i++)
                {
                array.put(i, step[i]);
                }
            obj.put("st", array);
            return obj;
            }

        public Function copy() 
            { 
            Step other = (Step)(super.copy());
            other.step = (double[])(step.clone());
            return other;
            }
        }
    
    public class Constant extends Function
        {
        double value;

        public double getValue() { return value; }
        public void setValue(double val) { value = val; }
                
        public Constant() { }
        public String getType() { return CONSTANT; }
        
        public Constant(JSONObject obj)
            {
            value = obj.optDouble("v", 0);
            }
        public JSONObject save() throws JSONException
            {
            JSONObject obj = super.save();
            obj.put("v", value);
            return obj;
            }
        }
    
    
                
    public void add(Motif motif)
        {
        addChild(motif);
        } 
        
    Function[] functions = new Function[NUM_PARAMETERS];
    
    public Motif copy()
    	{
    	Modulation other = (Modulation)super.copy();
    	for(int i = 0; i < functions.length; i++)
    		{
    		other.functions[i] = functions[i].copy();
    		}
    	return other;
    	}
    
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
    
    public Modulation(Seq seq)
        {
        super(seq);
        
        for(int i = 0; i < NUM_PARAMETERS; i++)
            {
            functions[i] = new Function();
            }
        add(new Blank(seq));
        }

    public Clip buildClip(Clip parent)
        {
        return new ModulationClip(seq, this, parent);
        }
        
    public void load(JSONObject obj) throws JSONException
        {
        for(int i = 0; i < NUM_PARAMETERS; i++)
            {
            functions[i] = new Function();
            }
        JSONArray array = obj.getJSONArray("func");
        for(int i = 0; i < Math.min(NUM_PARAMETERS, array.length()); i++)
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
        for(int i = 0; i < NUM_PARAMETERS; i++)
            {
            array.put(i, functions[i].save());
            }
        obj.put("func", array);
        obj.put("from", getFrom());
        obj.put("to", getTo());
        obj.put("always", isAlways());
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
        
    public String getBaseName() { return "Modulation"; }
    }
        
