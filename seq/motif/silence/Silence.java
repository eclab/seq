/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.silence;

import seq.engine.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Silence is used whenever we need a stub.  Macro uses it as the default
    value in its internal DAG.  Silence can also be used to substitute for
    a missing module type.  In this case you can use setWas(...) to indicate
    the module that was missing, and Silence can report that later as needed.
*/

/// FIXME: should we refuse child connections?  Or just ignore them when processing?

public class Silence extends Motif
    {
    private static final long serialVersionUID = 1;

    public int getLength() { return length; }
    public void setLength(int val) { length = (val < 1 ? 1 : val); }
    public void recomputeLength() { } 

    /*
      String was = "";
        
      public String getWas() { return was; }
      public void setWas(String val) { was = val; }
      public void setWasToClass(Class val) 
      {
      if (val == null) was = "";
      else was = val.getSimpleName(); 
      }
      public void setWasToClass(String classname)
      {
      if (classname == null) was = "";
      else
      {
      String[] v = classname.split(".");
      if (v.length > 0) was = v[0];
      else was = "";
      }
      }
    */

    public Silence(Seq seq)
        {
        super(seq);
        length = 1;
        }
                
    public Clip buildClip(Clip parent)
        {
        return new SilenceClip(seq, this, parent);
        }

    public void load(JSONObject obj) throws JSONException
        {
        setLength(obj.optInt("len", 1));
        //setWas(obj.optString("was", ""));
        }
        
    public void save(JSONObject obj) throws JSONException
        {
        obj.put("len", getLength());
        //obj.put("was", getWas());
        }


    static int counter = 1;
    public int getNextCounter() { return counter++; }
        
    public String getBaseName() { return "Silence"; }
    }
        
