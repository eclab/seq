/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.parallel;

import seq.engine.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

public class Parallel extends Motif
    {
    private static final long serialVersionUID = 1;

    public static final int QUANTIZATION_NONE = 0;
    public static final int QUANTIZATION_SIXTEENTH = 1;
    public static final int QUANTIZATION_QUARTER = 2;
    public static final int QUANTIZATION_FOUR_QUARTERS = 3;

    public static class Data
        {
        public static final int DISABLED = -1;

        public static final int MAX_TRANSPOSE = 24;
        public static final double MAX_GAIN = 4.0;
        public static final double MAX_RATE = 16.0;
        public static final double DEFAULT_RATE = 1.0;
        
        public boolean mute = false;
        public double rate = 1;
        public int delay;
        public double probability = 1.0;
        public int transpose = MAX_TRANSPOSE;                                   // ranges 0 ... MAX_TRANSPOSE * 2 inclusive, representing -MAX_TRANSPOSE ... MAX_TRANSPOSE
        public double gain = 1;
        public int out = DISABLED;
        int endingQuantization = QUANTIZATION_NONE;
        boolean override = false;
        boolean repeat = false;
        
        public boolean getMute() { return mute; }
        public void setMute(boolean val) { mute = val; }
        
        public boolean getOverride() { return override; }
        public void setOverride(boolean val) { override = val; }
        
        public int getDelay() { return delay; }
        public void setDelay(int val) { delay = val; }

        public double getProbability() { return probability; }
        public void setProbability(double val) { probability = val; }

        public void setTranspose(int transpose) { this.transpose = transpose; }
        public int getTranspose() { return transpose; }

        public void setGain(double gain) { this.gain = gain; }
        public double getGain() { return gain; }

        public void setOut(int out) { this.out = out; }
        public int getOut() { return out; }
        
        public double getRate() { return rate; }
        public void setRate(double val) { rate = val; }
 
        public boolean getRepeat() { return repeat; }
        public void setRepeat(boolean val) { repeat = val; }
                
        public int getEndingQuantization() { return endingQuantization; }
        public void setEndingQuantization(int val) { endingQuantization = val; }
       
        public Data() { }

        public Data(Data other)
            {
            mute = other.mute;
            rate = other.rate;
            delay = other.delay;
            probability = other.probability;
            transpose = other.transpose;
            gain = other.gain;
            out = other.out;
            endingQuantization = other.endingQuantization;
            repeat = other.repeat;
            override = other.override;
            }
        }
        
    protected Object buildData(Motif motif) { return new Data(); }

    protected Object copyData(Motif motif, Object data) { return new Data((Data)data); }

    protected void saveData(Object data, Motif motif, JSONObject to) 
        {
        Data d = (Data)data;
        to.put("mute", d.mute);
        to.put("rate", d.rate);
        to.put("delay", d.delay);
        to.put("prob", d.probability);
        to.put("tran", d.transpose);
        to.put("gain", d.gain);
        to.put("out", d.out);
        to.put("equant", d.endingQuantization);
        to.put("repeat", d.repeat);
        to.put("over", d.override);
        }

    protected Object loadData(Motif motif, JSONObject from) 
        { 
        Data d = (Data)buildData(motif);
        d.mute = from.optBoolean("mute", false);
        d.delay = from.optInt("delay", 0);
        d.transpose = from.optInt("tran", 0);
        d.rate = from.optDouble("rate", 1.0);
        d.gain = from.optDouble("gain", 1.0);
        d.out = from.optInt("out", 0);
        d.probability = from.optDouble("prob", 1.0);
        d.endingQuantization = from.optInt("equant", QUANTIZATION_NONE);
        d.repeat = from.optBoolean("repeat", false);
        d.override = from.optBoolean("over", false);
        return d;
        }
        
        
    public static final int ALL_CHILDREN = 0;
    public static final int ALL_CHILDREN_STOP_AFTER_FIRST = 17;
    int numChildrenToSelect = ALL_CHILDREN;
    
    public int getNumChildrenToSelect() { return numChildrenToSelect; }
    public void setNumChildrenToSelect(int val) { numChildrenToSelect = val; }
        

    double crossFade = 0.5;
    boolean crossFadeOn = false;

    public double getCrossFade() { return crossFade; }
    public void setCrossFade(double val) { crossFade = val; }
        
    public boolean getCrossFadeOn() { return crossFadeOn; }
    public void setCrossFadeOn(boolean val) { crossFadeOn = val; }
        
    int end = Seq.MIN_MAX_TIME;
    public int getEnd() { return end; }
    public void setEnd(int val) { end = val; }


    public Clip buildClip(Clip parent)
        {
        return new ParallelClip(seq, this, parent);
        }
        
    public Parallel(Seq seq)
        {
        super(seq);
        }
        
    public Motif copy()
        {
        Parallel other = (Parallel)(super.copy());
        other.crossFade = crossFade;
        other.crossFadeOn = crossFadeOn;
        return other;
        }

    public void add(Motif motif, int delay)
        {
        Motif.Child child = addChild(motif);
        Data data = (Data)(child.getData());
        data.delay = delay;
        } 

    public void load(JSONObject obj) throws JSONException
        {
        setNumChildrenToSelect(obj.getInt("num"));
        crossFade = obj.optDouble("xfade", 0.5);
        crossFadeOn = obj.optBoolean("xfon", false);
        end = obj.optInt("end", Seq.MIN_MAX_TIME);
        }
        
    public void save(JSONObject obj) throws JSONException
        {
        obj.put("num", getNumChildrenToSelect());
        obj.put("xfade", crossFade);
        obj.put("xfon", crossFadeOn);
        obj.put("end", end);
        }

    static int document = 0;
    static int counter = 1;
    public int getNextCounter() { if (document < Seq.getDocument()) { document = Seq.getDocument(); counter = 1; } return counter++; }
        
    public String getBaseName() { return "Parallel"; }
    }
        
