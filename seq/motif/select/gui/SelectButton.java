/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.select.gui;

import seq.engine.*;
import seq.gui.*;
import seq.motif.select.*;
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


// FIXME: change this to be a subclass of SeriesButton?

public class SelectButton extends MotifButton
    {
    public static final int MINIMUM_SIZE = 80;
    int at;
    
    public int getAt() { return at; }
    public void setAt(int val) { at = val; }
    
    public Dimension getMinimumSize() { return new Dimension(MINIMUM_SIZE, MINIMUM_SIZE); }
    public Dimension getMaximumSize() { return getMinimumSize(); }
    public Dimension getPreferredSize() { return getMinimumSize(); }
    
    // Make a "Blank" button
    public SelectButton(SeqUI ui, SelectUI owner, int at)   
        {
        super();
        
        this.sequi = sequi;
        this.owner = owner;
        this.at = at;

        setEnabled(false);
        setDropTarget(new DropTarget(this, owner.buildDropTargetListener()));
        }

    public SelectButton(SeqUI sequi, MotifUI motifui, SelectUI owner, int at)
        {
        super(sequi, motifui, owner);
        this.at = at;

        setHorizontalAlignment(SwingConstants.CENTER);
        setVerticalTextPosition(SwingConstants.BOTTOM);
        setHorizontalTextPosition(SwingConstants.CENTER);
        setMargin(new Insets(10, 0, 10, 0));
        setFocusPainted(false);
        setEnabled(true);
        setIconTextGap(0);
        setBackground(null);
        
        addMouseListener(new MouseAdapter()
            {
            public void mousePressed(MouseEvent e)
                {
                int modifiers = e.getModifiers();
                if (SwingUtilities.isRightMouseButton(e) ||
                    SwingUtilities.isMiddleMouseButton(e) ||
                    ((modifiers & InputEvent.SHIFT_MASK) == InputEvent.SHIFT_MASK) ||
                    ((modifiers & InputEvent.CTRL_MASK) == InputEvent.CTRL_MASK) ||
                    ((modifiers & InputEvent.META_MASK) == InputEvent.META_MASK) ||
                    ((modifiers & InputEvent.ALT_MASK) == InputEvent.ALT_MASK))
                    {
                    owner.play(SelectButton.this);
                    }
                }
            });

        addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                dragCount = 0;
                owner.select(SelectButton.this);
                }
            });
                
        addMouseMotionListener(new MouseMotionAdapter()
            {
            public void mouseDragged(MouseEvent e)
                {
                if (SelectButton.this.isEnabled())
                    {
                    dragCount++;
                    if (dragCount == MAX_DRAG_COUNT)
                        getTransferHandler().exportAsDrag(SelectButton.this, e, TransferHandler.MOVE);
                    }
                }
            });
            
        setTransferHandler(buildTransferHandler());
        setDropTarget(new DropTarget(this, owner.buildDropTargetListener()));
        }


    public boolean shouldPreHighlight()
        {
        Seq seq = sequi.getSeq();
        
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            {
            SelectClip clip = ((SelectClip)(owner.getDisplayClip()));
            if (clip != null) 
                {
                ArrayList<SelectClip.Node> playing = clip.getNextNodes();
                for(SelectClip.Node play : playing)
                    {
                    if (play.index == at) return true;
                    }
                }
            }
        finally { lock.unlock(); }
        return false;
        }
    
    String lastText = null;
    
    public void updateText()
        {
        String name = null;
        String subname = null;
        boolean playing = false;
        boolean next = false;
        boolean remove = false;
        
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

                SelectClip ownerClip = (SelectClip)(owner.getDisplayClip());
                if (ownerClip != null)
                    {
                    for(SelectClip.Node node : ownerClip.getRemoveNodes())
                        {
                        if (node.index == at)
                            {
                            remove = true;
                            break;
                            }
                        }
                    if (!remove)
                        {
                        for(SelectClip.Node node : ownerClip.getNextNodes())
                            {
                            if (node.index == at)
                                {
                                next = true;
                                break;
                                }
                            }
                        }
                    if (!remove && !next)
                        {
                        for(SelectClip.Node node : ownerClip.getPlayingNodes())
                            {
                            if (node.index == at)
                                {
                                playing = true;
                                break;
                                }
                            }
                        }
                    }
                }
            finally { lock.unlock(); }

            if (playing) setForeground(PLAYING_COLOR);
            else if (next) setForeground(NEXT_COLOR);
            else if (remove) setForeground(REMOVE_COLOR);
            else setForeground(null);
                        
            if (subname != null)
                {
                subname = subname.trim();
                if (!subname.equals(""))
                    {
                    subname = StringUtility.sanitize(subname);
                    }
                }
            
            String text = null;    
            if (subname == null || subname.equals(""))
                {
                subname = StringUtility.sanitize(name);
                }
            
            
            String sanitized = StringUtility.sanitize(subname);
            if (sanitized.length() > 7)
                {
                text = sanitized;
                //text = "<html><center><font size=1>" + sanitized + "</font></center></html>";
                }
            else
                {
                text = sanitized;
                //text = "<html><center>" + sanitized + "</center></html>";
                }

            if (!text.equals(lastText))
                {
                setText(text);
                lastText = text;
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

/*
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
*/

    public void doubleClick(MouseEvent e)
        {
        // Don't double-click if doing a modifier, we're just launching rapidly
        int modifiers = e.getModifiers();
        if (SwingUtilities.isRightMouseButton(e) ||
            SwingUtilities.isMiddleMouseButton(e) ||
            ((modifiers & InputEvent.SHIFT_MASK) == InputEvent.SHIFT_MASK) ||
            ((modifiers & InputEvent.CTRL_MASK) == InputEvent.CTRL_MASK) ||
            ((modifiers & InputEvent.META_MASK) == InputEvent.META_MASK) ||
            ((modifiers & InputEvent.ALT_MASK) == InputEvent.ALT_MASK))
            {
            // do nothing
            }
        else
            {
            sequi.showMotifUI(motifui);
            }
        }
    }
        
