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

public class Silence extends Motif
    {
    private static final long serialVersionUID = 1;

    int length = 1;
    public int getLength() { return length; }
    public void setLength(int val) { length = (val < 1 ? 1 : val); }

    public Silence(Seq seq)
        {
        super(seq);
        }
                
    public Clip buildClip(Clip parent)
        {
        return new SilenceClip(seq, this, parent);
        }

    public void load(JSONObject obj) throws JSONException
        {
        setLength(obj.optInt("len", 1));
        }
        
    public void save(JSONObject obj) throws JSONException
        {
        obj.put("len", getLength());
        }


    static int document = 0;
    static int counter = 1;
    public int getNextCounter() { if (document < Seq.getDocument()) { document = Seq.getDocument(); counter = 1; } return counter++; }
        
    public String getBaseName() { return "Silence"; }
    }
        
