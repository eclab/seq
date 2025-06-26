/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.blank;

import seq.engine.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Blank is used whenever we need a stub.  Macro uses it as the default
    value in its internal DAG.  Blank can also be used to substitute for
    a missing module type.  In this case you can use setWas(...) to indicate
    the module that was missing, and Blank can report that later as needed.
*/

/// FIXME: should we refuse child connections?  Or just ignore them when processing?

public class Blank extends Motif
    {
    private static final long serialVersionUID = 1;

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

    public Blank(Seq seq)
        {
        super(seq);
        }
                
    public Clip buildClip(Clip parent)
        {
        return new BlankClip(seq, this, parent);
        }

    public void load(JSONObject obj) throws JSONException
        {
        setWas(obj.optString("was", ""));
        }
        
    public void save(JSONObject obj) throws JSONException
        {
        obj.put("was", getWas());
        }


	static int document = 0;
    static int counter = 1;
    public int getNextCounter() { if (document < Seq.getDocument()) { document = Seq.getDocument(); counter = 1; } return counter++; }
        
    public String getBaseName() { return "Blank"; }
    }
        
