/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.parallel.gui;

import seq.engine.*;
import seq.gui.*;
import seq.motif.parallel.*;
import seq.util.*;
import java.awt.*;
import javax.swing.border.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.concurrent.locks.*;
import java.util.*;
import com.formdev.flatlaf.*;

// For Drag and Drop
import java.awt.dnd.*;
import java.awt.datatransfer.*;


public class ParallelButton extends MotifButton
    {
    int at;
    int delay = 0;
    
    public int getAt() { return at; }
    public void setAt(int val) { at = val; }
    
    Border originalBorder;
    Color originalBackground;
    
    public void updateDelay()
        {
        updateText();
        int slide = ((ParallelUI)owner).timeToPixels(delay);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0,slide,0,0, /*originalBackground*/ Color.GRAY),
                originalBorder));
        }
        
    public void setDelay(int val)
        {
        delay = val;
        updateDelay();
        }
    
    public ParallelButton(SeqUI sequi, MotifUI motifui, ParallelUI owner, int at)
        {
        super(sequi, motifui, owner);
        this.at = at;
        
        addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                dragCount = 0;
                owner.select(ParallelButton.this);
                }
            });
                
        addMouseMotionListener(new MouseMotionAdapter()
            {
            public void mouseDragged(MouseEvent e)
                {
                dragCount++;
                if (dragCount == MAX_DRAG_COUNT)
                    getTransferHandler().exportAsDrag(ParallelButton.this, e, TransferHandler.MOVE);
                }
            });
            
        setTransferHandler(buildTransferHandler());
        setDropTarget(new DropTarget(this, owner.buildDropTargetListener()));
        originalBorder = getBorder();
        originalBackground = owner.getButtonBox().getBackground();
        }

    public boolean shouldHighlight() 
        {
        Seq seq = sequi.getSeq();
        
        String subname = null;
        boolean playing = false;
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            {
            ParallelClip clip = ((ParallelClip)(owner.getDisplayClip()));
            if (clip != null) 
                {
                ParallelClip.Node n = null;
                try { n = clip.getNodes().get(at); } 
                catch (java.lang.IndexOutOfBoundsException ex) { System.err.println(ex); return false; }                // FIXME: This appears to be a bug...
                if (n != null)
                    {
                    playing = n.isPlaying() && !n.isRepeating();
                    }
                }
            }
        finally { lock.unlock(); }
        return playing;
        }
    
    public boolean shouldPreHighlight()
        {
        boolean playing = false;
        Seq seq = sequi.getSeq();
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            {
            ParallelClip clip = ((ParallelClip)(owner.getDisplayClip()));
            if (clip != null) 
                {
                ParallelClip.Node n = null;
                try { n = clip.getNodes().get(at); } 
                catch (java.lang.IndexOutOfBoundsException ex) { System.err.println(ex); return false; }                // FIXME: This appears to be a bug...
                if (n != null)
                    {
                    playing = n.isRepeating();
                    }
                }
            }
        finally { lock.unlock(); }
        return playing;
        }
        
    public String getSubtext()
        {
        Seq seq = sequi.getSeq();
        
        String subname = null;
        int bar = 0;
        boolean muted = false;
        boolean override = false;
        boolean repeating = false;
        
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            {
            ParallelClip clip = ((ParallelClip)(owner.getDisplayClip()));
            if (clip != null) 
                {
                ParallelClip.Node n = null;
                try { n = clip.getNodes().get(at); } 
                catch (java.lang.IndexOutOfBoundsException ex) { System.err.println(ex); }                // FIXME: This appears to be a bug...
                if (n != null)
                    {
                    repeating = n.isRepeating();
                    }
                }

            ArrayList<Motif.Child> children = owner.getMotif().getChildren();
            if (at >= children.size())  // uh
                return "BAD CHILD? " + at;
            if (at < children.size())
                {
                Motif.Child child = children.get(at);
                subname = child.getNickname();                    // FIXME: could this be made just volatile?
                Parallel.Data data = (Parallel.Data)(child.getData());
                bar = seq.getBar();
                muted = data.getMute();
                override = data.getOverride();
                }
            else subname = "";
            }
        finally { lock.unlock(); }

        if (subname != null) 
            { 
            subname = subname.trim();
            if (!subname.equals(""))
                {
                subname = StringUtility.sanitize(subname);
                }
            }
        else subname = "";
        
        int ticks = (delay % Seq.PPQ) + 1;
        int _delay = delay / Seq.PPQ;
        int beats = (_delay % bar) + 1;
		_delay = _delay / bar;
		int bars = (_delay % Seq.NUM_BARS_PER_PART) + 1;
		int parts = (_delay / Seq.NUM_BARS_PER_PART) + 1;
        
        return (override ? "Override&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" : "") + (delay == 0 ? "" : (parts + " : " + bars + " : " + beats + " : " + ticks + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;")) + (muted ? "Muted&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" : "") +
            (repeating ? "Repeating&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" : "") + subname;
        }

    public void doubleClick(MouseEvent e)
        {
        sequi.showMotifUI(motifui);
        }
    }
        
