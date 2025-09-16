/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.macro;

import seq.engine.*;
import seq.motif.blank.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

public class Macro extends Motif
    {
    private static final long serialVersionUID = 1;

    // The root of the embedded DAG
    Motif macroRoot;
    ArrayList<MacroChild> macroChildren = new ArrayList<>();
        
    public int getNumMacroChildren() { return macroChildren.size(); }
            
    public void setMacroRoot(Motif macroRoot) 
        { 
        this.macroRoot = macroRoot; 
        
        // update backpointers
        ArrayList<Motif> list = macroRoot.getDescendants();
        list.add(macroRoot);
        for(Motif motif : list)
            {
            if (motif instanceof MacroChild)
                {
                ((MacroChild)motif).macro = this;
                }
            }
        }
        
    public Motif getMacroRoot() { return macroRoot; }
        
    public Macro(Seq seq)
        {
        super(seq);
        setMacroRoot(new Blank(seq));
        }
                
    public Macro(Seq seq, JSONObject from) throws JSONException
        {
        super(seq);
        load(from);
        }

    public Motif copy()
        {
        Macro other = (Macro)(super.copy());
        
        // Build the new internal DAG.  setMacroRoot will update the backpointers to other
        other.setMacroRoot(macroRoot.copyDAG());
                
        return other;
        }
        
    public void add(Motif motif)
        {
        addChild(motif);
        } 

    public Clip buildClip(Clip parent)
        {
        return new MacroClip(seq, this, parent);
        }
        
    public void save(JSONObject to) throws JSONException
        {
        to.put("motifs", macroRoot.saveRoot());
        }

    public void load(JSONObject from) throws JSONException
        {
        JSONArray motifs = from.getJSONArray("motifs");
        ArrayList<Motif> roots = new ArrayList<>();
        roots = load(seq, motifs);              // only load rooted dag
        setMacroRoot(roots.get(0));  // also sets the backpointers

        // collect and index the macrochildren
        int index = 0;
        macroChildren.clear();
        if (macroRoot instanceof MacroChild)
            {
            MacroChild kid = ((MacroChild)macroRoot);
            kid.setIndex(index++); 
            macroChildren.add(kid);
            }
        ArrayList<Motif> list = macroRoot.getDescendants();
        for (Motif motif : list)
            {
            if (motif instanceof MacroChild)
                {
                MacroChild kid = ((MacroChild)motif);
                kid.setIndex(index++); 
                macroChildren.add(kid);
                }
            }
            
        // build children if they're not there yet (it's a new macro)
        while (getChildren().size() < macroChildren.size())
            {
            addChild(new Blank(seq));
            }
        }
        
    /** Return all children. */
    public ArrayList<MacroChild> getMacroChildren() { return macroChildren; }
        
    public String getParameterName(int param) 
    	{ 
    	if (macroRoot != null && !(macroRoot instanceof Blank))
    		{
    		return macroRoot.getParameterName(param);
    		}
    	else
    		{
    		return super.getParameterName(param);
	    	}
    	}

    static int document = 0;
    static int counter = 1;
    public int getNextCounter() { if (document < Seq.getDocument()) { document = Seq.getDocument(); counter = 1; } return counter++; }
        
    public String getBaseName() { return "Macro"; }
    }
        
