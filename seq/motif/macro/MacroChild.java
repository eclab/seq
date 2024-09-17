/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.macro;

import seq.engine.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

public class MacroChild extends Motif
    {
    private static final long serialVersionUID = 1;

    // If null, then the MacroChild is not part of a Macro yet, it's just part of the main sequence
    Macro macro = null;
    int index;
    
    String makeUniqueName(String name)
        {
        if (name == null) name = "";
        name = name.trim();
        
        // First pass: use original name
        boolean unique = true;
        for(Motif motif : seq.getMotifs())
            {
            if (this == motif) continue;    // not me
            if (motif instanceof MacroChild)
                {
                MacroChild other = (MacroChild) motif;
                if (other.getName().trim().equals(name))        // shoot!
                    {
                    unique = false; 
                    break;
                    }
                }
            }
        if (unique) return name;

        // Second pass: build a unique name
        int c = 0;
        while(true)
            {
            c = seq.nextMacroChildCounter();
            unique = true;
            for(Motif motif : seq.getMotifs())
                {
                if (this == motif) continue;    // not me
                if (motif instanceof MacroChild)
                    {
                    MacroChild other = (MacroChild) motif;
                    if (other.getName().trim().equals((name + " " + c).trim()))             // shoot!   [second trim is in case name is ""]
                        {
                        unique = false; 
                        break;
                        }
                    }
                }
            if (unique) return (name + " " + c).trim();
            }               // Better not be an infinite loop!  FIXME
        }
    
    public void setName(String name)
        {
        if (name == null) name = "";
        else name = name.trim();
        
        // First: do we already have this name?
        if (getName().trim().equals(name)) return;
        
        // Second: does anyone else have this name?
        name = makeUniqueName(name);
        
        super.setName(name);
        }
    
    public MacroChild(Seq seq)
        {
        super(seq);
        this.index = 0;
        setName(getBaseName());         // will make unique
        }

    public MacroChild(Seq seq, int index)
        {
        super(seq);
        this.index = index;
        setName(getBaseName());         // will make unique
        }
                
    public Clip buildClip(Clip parent)
        {
        return new MacroChildClip(seq, this, parent);
        }
        
    public Macro getMacro() { return macro; }
    public void setMacro(Macro val) { macro = val; }
    public int getIndex() { return index; }
    public void setIndex(int val) { index = val; }
    
    /// FIXME: these probably don't handle the macro back-pointer
    public void save(JSONObject to) throws JSONException
        {
        to.put("index", index);
        }

    public void load(JSONObject from) throws JSONException
        {
        index = from.getInt("index");
        }
        
    static int counter = 1;
    public int getNextCounter() { return counter++; }
    public String getBaseName() { return "Macro Child"; }
    }
        
