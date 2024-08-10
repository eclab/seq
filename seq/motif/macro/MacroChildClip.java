/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.macro;

import seq.engine.*;
import java.util.*;
import java.util.concurrent.*;

/**
   NOTE: Every MacroChild in the internal DAG must correspond to exactly one 
   child of the Macro.  You cannot have two MacroChilds in the internal DAG which
   refer to the same child.  To enforce this MacroChildClip checks to see
   if childrenProcessed[myIndex] has already been set, and if so, it just returns
   what childResult[myIndex] returned.
        
   MacroChilds and MacroChildClips may appear in the main DAG of course, as
   the DAG hasn't been rolled into a Macro yet.  In this case, they just return true
   if they've been processed for one step already (like Blank)
**/

public class MacroChildClip extends Clip
    {
    private static final long serialVersionUID = 1;

    public MacroChildClip(Seq seq, MacroChild childarg, Clip parent)
        {
        super(seq, childarg, parent);
        rebuild();
        }
        
    // Returns the index corresponding to me
    int getMyIndex() { return ((MacroChild)getMotif()).getIndex(); }
                                              
    public void rebuild()
        {
        reset();
        version = getMotif().getVersion();
        }

    public void rebuild(Motif motif)
        {
        if (this.getMotif() == motif) rebuild();
        }

    public void loop()
        {
        super.loop();
        MacroClip owner = getOwner();
        if (owner != null) owner.getChild(getMyIndex()).loop();
        }
                
    public void reset()  
        {
        super.reset();
        MacroClip owner = getOwner();
        if (owner != null) owner.getChild(getMyIndex()).reset();
        }

    public void cut()
        {
        MacroClip owner = getOwner();
        if (owner != null) owner.getChild(getMyIndex()).cut();
        }
        
    public void release()
        {
        MacroClip owner = getOwner();
        if (owner != null) owner.getChild(getMyIndex()).release();
        }

    public double getRandomValue()
        {
        MacroClip owner = getOwner();
        if (owner != null) return owner.getChild(getMyIndex()).getRandomValue();
        else return super.getRandomValue();
        }

    public void setRandomValue(double val) 
        {
        MacroClip owner = getOwner();
        if (owner != null) owner.getChild(getMyIndex()).setRandomValue(val);
        super.setRandomValue(val);
        }

    public boolean process()
        {
        MacroClip owner = getOwner();
        if (owner == null) return (getPosition() > 0);          // just like Blank
        
        int index = getMyIndex();
        if (owner.isChildProcessed(index))
            {
            return owner.getChildResult(index);
            }
        else
            {
            boolean result = owner.getChild(index).advance();
            owner.setChildProcessed(index, true);
            owner.setChildResult(index, result);
            return result;
            }
        }
    }
