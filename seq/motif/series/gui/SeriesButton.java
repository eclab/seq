package seq.motif.series.gui;

import seq.engine.*;
import seq.gui.*;
import seq.motif.series.*;
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


public class SeriesButton extends MotifButton
    {
    int at;
    
    public int getAt() { return at; }
    public void setAt(int val) { at = val; }
    
    public SeriesButton(SeqUI sequi, MotifUI motifui, SeriesUI owner, int at)
        {
        super(sequi, motifui, owner);
        this.at = at;
        
        addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                dragCount = 0;
                owner.select(SeriesButton.this);
                }
            });
                
        addMouseMotionListener(new MouseMotionAdapter()
            {
            public void mouseDragged(MouseEvent e)
                {
                dragCount++;
                if (dragCount == MAX_DRAG_COUNT)
                    getTransferHandler().exportAsDrag(SeriesButton.this, e, TransferHandler.MOVE);
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
            SeriesClip clip = ((SeriesClip)(owner.getDisplayClip()));
            if (clip != null) 
                {
                playing = (clip.getPlayingClip() != null && clip.getPlayingIndex() == at);
                }
            }
        finally { lock.unlock(); }
        return playing;
        }
        
        
    public String getSubtext()
        {
        Seq seq = sequi.getSeq();
        
        String subname = null;
        int repeats = 0;
        double probability = 0;
        int currentRepeat = 0;
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            {
            ArrayList<Motif.Child> children = owner.getMotif().getChildren();
            if (at >= children.size())  // uh
                return "BAD CHILD? " + at;
            Motif.Child child = children.get(at);
            Series.Data data = ((Series.Data)(child.getData()));

            subname = child.nickname;                    // FIXME: could this be made just volatile?
            if (subname != null) 
                { 
                subname = subname.trim();
                if (!subname.equals(""))
                    {
                    subname = StringUtility.sanitize(subname);
                    }
                }
            else subname = "";

            SeriesClip clip = ((SeriesClip)(owner.getDisplayClip()));
            if (clip != null && clip.getPlayingIndex() == at) 
                {
                repeats = clip.getCorrectedValueInt(data.getRepeatAtLeast(), Series.Data.MAX_REPEAT_VALUE);
                probability = clip.getCorrectedValueDouble(data.getRepeatProbability());
                currentRepeat = clip.getPlayingRepeat() + 1;
                }
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
        
        return "" + currentRepeat + "/" + (repeats + 1) + " (" + String.format("%.4f", probability)  + ")&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + subname;
        }

    public void doubleClick(MouseEvent e)
        {
        sequi.showMotifUI(motifui);
        }
    }
        
