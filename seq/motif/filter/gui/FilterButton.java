/* 
   Copyright 2025 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.filter.gui;

import seq.engine.*;
import seq.gui.*;
import seq.motif.filter.*;
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


public class FilterButton extends MotifButton
    {
    public static final int MINIMUM_SIZE = 12 + 12 + MotifButton.ICON_WIDTH;
    int at;
    
    public int getAt() { return at; }
    public void setAt(int val) { at = val; }
    
    public Dimension getMinimumSize() { return new Dimension(MINIMUM_SIZE, MINIMUM_SIZE); }
    public Dimension getPreferredSize() { return getMinimumSize(); }

    // Make a "Blank" button
    public FilterButton(SeqUI sequi, FilterUI owner, int at)   
        {
        super();
        
        this.sequi = sequi;
        this.owner = owner;
        this.at = at;

        setEnabled(false);
        setDropTarget(new DropTarget(this, owner.buildDropTargetListener()));
        }

    public FilterButton(SeqUI sequi, MotifUI motifui, FilterUI owner, int at)
        {
        super(sequi, motifui, owner);
        this.at = at;
        
        addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                dragCount = 0;
                owner.select(FilterButton.this);
                }
            });
                
        addMouseMotionListener(new MouseMotionAdapter()
            {
            public void mouseDragged(MouseEvent e)
                {
                if (FilterButton.this.isEnabled())
                    {
                    dragCount++;
                    if (dragCount == MAX_DRAG_COUNT)
                        getTransferHandler().exportAsDrag(FilterButton.this, e, TransferHandler.MOVE);
                    }
                }
            });
            
        setTransferHandler(buildTransferHandler());
        setDropTarget(new DropTarget(this, owner.buildDropTargetListener()));
        }


/*
  public boolean shouldHighlight()
  {
  Seq seq = sequi.getSeq();
        
  ReentrantLock lock = seq.getLock();
  lock.lock();
  try 
  {
  FilterClip clip = ((FilterClip)(owner.getMotif().getPlayingClip()));
  if (clip != null) 
  {
  ArrayList<FilterClip.Node> playing = clip.getNextClips();
  for(FilterClip.Node play : playing)
  {
  if (play.index == at) return true;
  }
  }
  }
  finally { lock.unlock(); }
  return false;
  }
*/
    
    public void updateText()
        {
        String name = null;
        String subname = null;
        boolean playing = false;
        boolean next = false;
        
        if (motifui != null)
            {
            // We can't use HTML because it won't wrap overflow.
            ReentrantLock lock = sequi.getSeq().getLock();
            lock.lock();
            try 
                { 
                Motif ownerMotif = owner.getMotif();
                Motif.Child child = ownerMotif.getChildren().get(at);

                subname = child.getNickname();
                name = child.getMotif().getDisplayedName();

/*
  FilterClip ownerClip = (FilterClip)(ownerMotif.getPlayingClip());
  if (ownerClip != null)
  {
  for(FilterClip.Node node : ownerClip.getPlayingClips())
  {
  if (node.index == at)
  {
  playing = true;
  break;
  }
  }
  if (!playing)
  {
  for(FilterClip.Node node : ownerClip.getNextClips())
  {
  if (node.index == at)
  {
  next = true;
  break;
  }
  }
  }
  }
*/
                }
            finally { lock.unlock(); }

            if (playing) setForeground(PLAYING_COLOR);
            else if (next) setForeground(NEXT_COLOR);
            else setForeground(null);
                        
            if (subname != null)
                {
                subname = subname.trim();
                if (!subname.equals(""))
                    {
                    subname = StringUtility.sanitize(subname);
                    }
                }
                        
            if (subname == null || subname.equals(""))
                {
                setText(StringUtility.sanitize(name));
                }
            else
                {
                setText(subname);
                }
            }
        }

    public void disconnect()
        {
        super.disconnect();
        if (motifui != null)
            {
            super.updateText();
            }
        }

    public String getSubtext()
        {
        Seq seq = sequi.getSeq();
        
        String subname = null;
        int repeats = 0;
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            {
            ArrayList<Motif.Child> children = owner.getMotif().getChildren();
            if (at >= children.size())  // uh
                return "BAD CHILD? " + at;
            Motif.Child child = children.get(at);
            subname = child.getNickname();                    // FIXME: could this be made just volatile?
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
        
