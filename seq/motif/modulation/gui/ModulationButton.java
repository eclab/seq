/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.modulation.gui;

import seq.engine.*;
import seq.gui.*;
import seq.motif.modulation.*;
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


public class ModulationButton extends MotifButton
    {
    public ModulationButton(SeqUI sequi, MotifUI motifui, ModulationUI owner)
        {
        super(sequi, motifui, owner);
        
        addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                dragCount = 0;
                owner.select(ModulationButton.this);
                }
            });
                
        addMouseMotionListener(new MouseMotionAdapter()
            {
            public void mouseDragged(MouseEvent e)
                {
                dragCount++;
                if (dragCount == MAX_DRAG_COUNT)
                    getTransferHandler().exportAsDrag(ModulationButton.this, e, TransferHandler.MOVE);
                }
            });
            
        setTransferHandler(buildTransferHandler());
        setDropTarget(new DropTarget(this, owner.buildDropTargetListener()));
        }

    public boolean shouldHighlight() 
        {
        Seq seq = sequi.getSeq();
        
        String subname = null;
        ReentrantLock lock = seq.getLock();
        lock.lock();
        boolean playing = false;
        try 
            {
            ModulationClip clip = ((ModulationClip)(owner.getDisplayClip()));
            if (clip != null) 
                {
                playing = clip.isPlaying();
                }
            }
        finally { lock.unlock(); }
        return playing;
        }
        
        
    public String getSubtext()
        {
        Seq seq = sequi.getSeq();
        
        String subname = null;
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            {
            ArrayList<Motif.Child> children = owner.getMotif().getChildren();
            Motif.Child child = children.get(0);                // there's only one
            Modulation.Data data = ((Modulation.Data)(child.getData()));

            subname = child.getNickname();                    // FIXME: could this be made just volatile?
            if (subname != null) 
                { 
                subname = subname.trim();
                if (!subname.equals(""))
                    {
                    subname = StringUtility.sanitize(subname);
                    }
                }
            else subname = "";

            ModulationClip clip = ((ModulationClip)(owner.getDisplayClip()));
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
        
        return subname;
        }

    public void doubleClick(MouseEvent e)
        {
        sequi.showMotifUI(motifui);
        }
    }
        
