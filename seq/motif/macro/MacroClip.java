/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.macro;

import seq.engine.*;
import java.util.*;
import java.util.concurrent.*;

public class MacroClip extends Clip
    {
    private static final long serialVersionUID = 1;

    // The root of the internal tree of clips corresponding to the Macro's internal DAG
    // root.parent is the MacroClip itself, so when the DAG sends up MIDI Data, it can
    // get processed properly by MacroClip sending the MIDI Data to its parent, and so on.
    // FIXME: at present, root.parent is ONLY used for sending up MIDI Data, which is fine,
    // but if we use it later for any possible future structural stuff, it's going to break
    // things because the MacroClip is not really the parent of the root.
    Clip root;
    
    ArrayList<Clip> children = new ArrayList<>();

    // Has the child already been processed this time around?  We don't want to process him twice.
    // This is essentially a sanity check against user error
    boolean[] childProcessed = new boolean[0];
        
    // What was the result when the child was processed?  This allows later incorrect MacroChildClips
    // to return a sane response.
    boolean[] childResult = new boolean[0];
    
    public void terminate() 
        { 
        super.terminate();
        root.terminate();
        for(Clip clip : children)
            {
            clip.terminate();
            }
        }

    public MacroClip(Seq seq, Macro macro, Clip parent)
        {
        super(seq, macro, parent);
        rebuild();
        }
                        
    public void loop()
        {
        super.loop();
        root.reset();                                                                           // I THINK we ought to reset here rather than loop?
        }
                
    public void reset()  
        {
        super.reset();
        root.reset();
        }

    public double getRandomValue()
        {
        return root.getRandomValue();
        }

    public void setRandomValue(double val) 
        {
        root.setRandomValue(val);
        super.setRandomValue(val);              // FIXME this is not really necessary
        }

    public void rebuild(Motif motif)
        {
        root.rebuild(motif);                                    // FIXME Is this necessary?
        if (this.getMotif() == motif)
            {
            rebuild();
            }
        else
            {
            for(Clip child : children)
                {
                child.rebuild(motif);
                }
            }
        }

    public void rebuild()
        {
        Macro macro = (Macro)getMotif();
        // I am the "parent" of the root as well as the owner.
        // This presents a little problem in setup.  We need to
        // properly set the owner, but the owner is set as the parent's owner. 
        // So I will temporarily
        // set my "owner" to **me**, so when the root is created, it'll
        // set its owner to its parent's owner -- this is, me --
        // and then I'll set my owner back afterwards.
        MacroClip oldOwner = getOwner();
        setOwner(this);
        if (root != null) root.terminate();
        root = macro.getMacroRoot().buildClip(this);
        setOwner(oldOwner);
        root.reset();
        version = getMotif().getVersion();
        }
        
    public Clip getChild(int child)
        {
        int size = children.size();
        if (size <= child)
            {
            for(int i = size; i <= child; i++)
                {
                children.add(null);
                }
            }

        Clip clip = children.get(child);
        if (clip == null)
            {
            Macro macro = (Macro)getMotif();
            clip = macro.getChildren().get(child).getMotif().buildClip(this);
            children.set(child, clip);
            }
                        
        return clip;
        }

    public boolean isChildProcessed(int val) { return childProcessed[val]; }

    public void setChildProcessed(int val, boolean to) { childProcessed[val] = to; }

    public boolean getChildResult(int val) { return childResult[val]; }

    public void setChildResult(int val, boolean to) { childResult[val] = to; }
                
    public boolean process()
        {
        int childSize = getMotif().getChildren().size();
        if (childProcessed.length != childSize)
            {
            // build
            childProcessed = new boolean[childSize];
            childResult = new boolean[childSize];
            }
        else 
            {
            // just clear
            for(int i = 0; i < childProcessed.length; i++)
                {
                childProcessed[i] = false;
                // don't care about childResult
                }
            }

        return root.advance();
        }



    // TESTING
    public static void main(String[] args) throws Exception
        {
        // Set up Seq
        Seq seq = new Seq();            // starts timer running
        seq.setupForMIDI(MacroClip.class, args, 0, 2);   // sets up MIDI in and out
        seq.setOut(0, new Out(seq, 0));         // Out 0 points to device 0 in the tuple.  This is too complex.
        seq.setOut(1, new Out(seq, 1));         // Out 0 points to device 0 in the tuple.  This is too complex.

        // Set up macro        
        Macro macro = new Macro(seq);
        
        // Set up our module structure
        seq.motif.series.Series series = new seq.motif.series.Series(seq);

        // Set up first StepSequence
        seq.motif.stepsequence.StepSequence dSeq = new seq.motif.stepsequence.StepSequence(seq, 2, 16);
        
        // Specify notes
        dSeq.setTrackNote(0, 60);
        dSeq.setTrackNote(1, 120);
        dSeq.setTrackOut(0, 0);
        dSeq.setTrackOut(1, 1);
        dSeq.setDefaultSwing(0.33);
        
        // Load the StepSequence with some data
        dSeq.setVelocity(0, 0, 1, true);
        dSeq.setVelocity(0, 4, 5, true);
        dSeq.setVelocity(0, 8, 9, true);
        dSeq.setVelocity(0, 12, 13, true);
        dSeq.setVelocity(1, 1, 2, true);
        dSeq.setVelocity(1, 2, 3, true);
        dSeq.setVelocity(1, 3, 4, true);
        dSeq.setVelocity(1, 5, 6, true);
        dSeq.setVelocity(1, 7, 8, true);
        dSeq.setVelocity(1, 9, 10, true);
        dSeq.setVelocity(1, 10, 11, true);
        dSeq.setVelocity(1, 15, 16, true);
        

        // Set up second StepSequence
        seq.motif.stepsequence.StepSequence dSeq2 = new seq.motif.stepsequence.StepSequence(seq, 2, 16);
        
        // Specify notes
        dSeq2.setTrackNote(0, 45);
        dSeq2.setTrackNote(1, 90);
        dSeq2.setTrackOut(0, 0);
        dSeq2.setTrackOut(1, 1);
        dSeq2.setDefaultSwing(0);
        
        // Load the StepSequence with some data
        dSeq2.setVelocity(0, 0, 1, true);
        dSeq2.setVelocity(0, 2, 5, true);
        dSeq2.setVelocity(0, 4, 9, true);
        dSeq2.setVelocity(0, 6, 13, true);
        dSeq2.setVelocity(1, 8, 2, true);
        dSeq2.setVelocity(1, 9, 3, true);
        dSeq2.setVelocity(1, 10, 4, true);
        dSeq2.setVelocity(1, 11, 6, true);
        dSeq2.setVelocity(1, 12, 2, true);
        dSeq2.setVelocity(1, 13, 3, true);
        dSeq2.setVelocity(1, 14, 4, true);
        dSeq2.setVelocity(1, 15, 6, true);

        // Load into series
//        series.add(dSeq, 0, -0.5);
        series.add(new MacroChild(seq, 0), 0, -0.5);
        macro.add(dSeq);
        series.add(dSeq2, 1, 0.0);
//        series.add(dSeq, 2, 0.0);
        series.add(new MacroChild(seq, 1), 2, 0.0);
        macro.add(dSeq);
        
        macro.setMacroRoot(series);
        seq.setData(macro);

        seq.reset();
        seq.play();

        seq.waitUntilStopped();
        }
    }
