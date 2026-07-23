/* 
   Copyright 2025 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.generator;

import seq.engine.*;
import seq.motif.blank.*;
import seq.util.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;


public class Generator extends Motif
    {
    private static final long serialVersionUID = 1;
    
    public static final int SAME_AS_IN = -1;
    int out = SAME_AS_IN;
    int in;
    int end = Seq.MIN_MAX_TIME;
    boolean omni;
    boolean pass;
    Algorithm algorithm;
    
    public Algorithm getAlgorithm() { return algorithm; }
    public void setAlgorithm(Algorithm val) { algorithm = val; }
    
    public int getAlgorithmIndex()
        {
        String classname = algorithm.getClass().getName();
        return Algorithm.findAlgorithmClassName(classname);
        }

    public void setAlgorithmIndex(int val)
        {
        setAlgorithm(Algorithm.instantiate(this, val));
        }
        
    public int getEnd() { return end; }
    public void setEnd(int val) { end = val; }

    /** Returns the output device. */
    public int getOut() { return out; }
    
    /** Sets the output device. */
    public void setOut(int val) { out = val; Prefs.setLastOutDevice(0, val, "seq.motif.generator.Generator.out"); }        

    /** Returns the In output device. */
    public int getIn() { return in; }
    
    /** Sets the In output device. */
    public void setIn(int val) { in = val; Prefs.setLastInDevice(0, val, "seq.motif.generator.Generator.in"); }        

    public boolean isOmni() { return omni; }
    public void setOmni(boolean val) { omni = val; }

    public boolean isPass() { return pass; }
    public void setPass(boolean val) { pass = val; }
    
    public Motif copy()
        {
        Generator other = (Generator) super.copy();
        return other;
        }

    public Clip buildClip(Clip parent)
        {
        return new GeneratorClip(seq, this, parent);
        }
        
    public Generator(Seq seq)
        {
        super(seq);
        out = (Prefs.getLastOutDevice(0, "seq.motif.generator.Generator.out", SAME_AS_IN));
        in = (Prefs.getLastInDevice(0, "seq.motif.generator.Generator.in"));
        add(new Blank(seq));
        setAlgorithmIndex(0);           // "None"
        }
    
    public void add(Motif motif)
        {
        addChild(motif);
        } 

    public void load(JSONObject obj) throws JSONException
        {
        obj.optInt("out", 0);
        obj.optInt("in", 0);
        obj.optBoolean("omni", true);
        obj.optBoolean("pass", false);

        JSONObject alg = obj.optJSONObject("alg");
        if (alg == null) algorithm = null;
        else algorithm = Algorithm.load(this, alg);             
        }
        
    public void save(JSONObject obj) throws JSONException
        {
        obj.put("out", out);
        obj.put("in", in);
        obj.put("omni", omni);
        obj.put("pass", pass);

        if (algorithm != null) 
            {
            JSONObject alg = new JSONObject();
            algorithm.save(alg);
            obj.put("alg", alg);
            }
        }
        
/*
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
*/

    static int document = 0;
    static int counter = 1;
    public int getNextCounter() { if (document < Seq.getDocument()) { document = Seq.getDocument(); counter = 1; } return counter++; }
        
    public String getBaseName() { return "Generator"; }
    }
        
