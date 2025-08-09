/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.notes.gui;

import seq.engine.*;
import seq.gui.*;
import seq.motif.notes.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.locks.*;
import java.awt.geom.*;
import java.util.*;

public class NoteUI extends EventUI
    {
    // Smallest width I'm permitted to be.  Maybe this should be 5.
    public static final int MINIMUM_WIDTH = 3;
    // Stroke for drawing me normally
    public static final Stroke stroke = new BasicStroke(1.0f);
    // Stroke for drawing me while selected
    public static final Stroke selectedStroke = new BasicStroke(3.0f);
    // Stroke color for drawing me normally
    public static final Color strokeColor = Color.BLACK; // new Color(64, 64, 64); 
    // Stroke color for drawing me while selected
    public static final Color selectedColor = Color.BLUE; 
    // Region at the far right of me where clicking in that region results in me trying to be resized
    public static final int RESIZE_REGION_WIDTH = 6;
    // Mapping of velocity to color
    public static final SimpleColorMap velocityMap = //new SimpleColorMap(0, 127, Color.GRAY, Color.RED);
        new SimpleColorMap(0, 127, 64, 
            new SimpleColorMap(0, 64, Color.GRAY, Color.RED),
            new SimpleColorMap(64, 127, Color.RED, Color.YELLOW));
                        
    // backpointer to the owner PitchUI
    PitchUI pitchui;
        
    // Returns the Seq
    Seq getSeq() { return pitchui.getSeq(); }

    // Returns the GridUI
    GridUI getGridUI() { return pitchui.getGridUI(); }

    // Returns the NotesUI
    NotesUI getNotesUI() { return getGridUI().getNotesUI(); }

    /** Returns the original pitch (a temporary variable for GridUI to compute moving notes */
    public int getOriginalPitch() { return (int)originalValue; }
    /** Sets the original pitch (a temporary variable for GridUI to compute moving notes */
    public void setOriginalPitch(int pitch) { originalValue = pitch; }

    /** Reloads the NoteUI to a new time and length, using the provided values.  Reloading
        simply sets the bounds of the NoteUI.  */ 
    public void reload(int when, int length)
        {
        int width = (int)(length / getGridUI().getScale());
        if (width < MINIMUM_WIDTH) width = MINIMUM_WIDTH;
        setBounds((int)(when / getGridUI().getScale()), 0, width, pitchui.getHeight());
        }
        
    /** Reloads the NoteUI to a new time, pitch, and length, using the provided values.  This
        version of the method expects that you provide the old time and old pitch; it compares
        them against the new when and pitch to avoid doing unnecessary work.  Reloading
        simply sets the bounds of the NoteUI and moves it to the proper PitchUI if needed. */ 
    public void reload(int oldWhen, int newWhen, int oldPitch, int newPitch, int length)
        {
        if (oldPitch != newPitch)
            {
            pitchui.removeNoteUI(this);
            pitchui = getGridUI().addNoteUI(this, newPitch);
            }
        if (oldWhen != newWhen)
            {
            reload(newWhen, length);
            }
        }
            
    /** Reloads the NoteUI to a new time, pitch, velocity, selected, and length,
        using the values in the Note.  Reloading
        simply sets the bounds of the NoteUI and moves it to the proper PitchUI if needed,
        as well as the velocity, and selected values.
    */ 
    public void reload()
        {
        int when = 0;
        int length = 0;
        int pitch = 0;
        Notes.Note note = getNote();
        ReentrantLock lock = getSeq().getLock();
        lock.lock();
        try 
            {
            pitch = note.pitch;
            when = note.when;
            length = note.length;
            //velocity = note.velocity;
            value = note.velocity;
            selected = note.selected;
            }
        finally
            {
            lock.unlock();
            }
        if (pitch != pitchui.getPitch())
            {
            // move me to new pitch
            pitchui.removeNoteUI(this);
            pitchui = getGridUI().addNoteUI(this, pitch);
            // try again
            reload();
            }
        else
            {
            reload(when, length);
            if (selected) getGridUI().addEventToSelected(this, GridUI.SELECTED_SOURCE_NOTES);
            }
        }
        
        
        
        
        
    ///// MOUSE EVENTS
    
    // Current cursor, can only be DEFAULT_CURSOR or W_RESIZE_CURSOR at present
    int cursor = Cursor.DEFAULT_CURSOR;
    
    // Am I resizing in this event?  This is determined by the cursor being W_RESIZE_CURSOR
    boolean isResizing() { return cursor == Cursor.W_RESIZE_CURSOR; }
            
    // Set the cursor to its proper value
    void updateCursor(MouseEvent e)
        {
        if (selected)
            {
            double width = getBounds().getWidth();
            int x = e.getX();
            if ((width >= MINIMUM_WIDTH + RESIZE_REGION_WIDTH &&
                    x >= width - RESIZE_REGION_WIDTH) ||
                x >= width - 1) // emergency resize region
                {
                if (cursor != Cursor.W_RESIZE_CURSOR) 
                    {
                    cursor = Cursor.W_RESIZE_CURSOR;
                    setCursor(new Cursor(cursor));
                    }
                }
            else
                {
                cursor = Cursor.DEFAULT_CURSOR;
                setCursor(new Cursor(cursor));
                }
            }
        }
        
    public NoteUI(PitchUI pitchui, Notes.Note note)
        {
        this.event = note;
        this.pitchui = pitchui;
        reload();
        
        addMouseListener(new MouseAdapter()
            {   
            public void mouseEntered(MouseEvent e)
                {
                if (!mouseDown) updateCursor(e);
                }

            public void mouseExited(MouseEvent e)
                {
                if (!mouseDown && cursor != Cursor.DEFAULT_CURSOR)
                    {
                    cursor = Cursor.DEFAULT_CURSOR;
                    setCursor(new Cursor(cursor));
                    }       
                }

            public void mousePressed(MouseEvent e)
                {
                mouseDownEvent = SwingUtilities.convertMouseEvent(NoteUI.this, e, getGridUI());
                    
                originallySelected = selected;
                                
                // This gunk fixes a BAD MISFEATURE in Java: mouseReleased isn't sent to the
                // same component that received mouseClicked.  What the ... ? Asinine.
                // So we create a global event listener which checks for mouseReleased and
                // calls our own private function.  EVERYONE is going to do this.
                                
                if (releaseListener != null)
                    {
                    Toolkit.getDefaultToolkit().removeAWTEventListener(releaseListener);
                    }
                Toolkit.getDefaultToolkit().addAWTEventListener( releaseListener = new AWTEventListener()
                    {
                    public void eventDispatched(AWTEvent e)
                        {
                        if (e instanceof MouseEvent && e.getID() == MouseEvent.MOUSE_RELEASED)
                            {
                            mouseReleased((MouseEvent)e);
                            }
                        }
                    }, AWTEvent.MOUSE_EVENT_MASK);

                if ((e.getModifiers() & InputEvent.SHIFT_MASK) == InputEvent.SHIFT_MASK)
                    {
                    if (!selected)
                        {
                        getGridUI().addEventToSelected(NoteUI.this, GridUI.SELECTED_SOURCE_NOTES);
                        }
                    }
                else if (!selected)
                    {
                    getGridUI().clearSelected();
                    getGridUI().addEventToSelected(NoteUI.this, GridUI.SELECTED_SOURCE_NOTES);
                    }                

                getGridUI().loadOriginal();

                mouseDown = true;
                getNotesUI().updateChildInspector(true);
                repaint();
                }
                        
            public void mouseReleased(MouseEvent e)
                {
                if (!mouseDown) return;                                 // not released on me

                if (releaseListener != null)
                    {
                    Toolkit.getDefaultToolkit().removeAWTEventListener(releaseListener);
                    }
                            
                mouseDown = false;
                mouseDownEvent = null;


                if ((e.getModifiers() & InputEvent.SHIFT_MASK) == InputEvent.SHIFT_MASK)
                    {
                    if (!originallySelected)
                        {
                        getGridUI().addEventToSelected(NoteUI.this, GridUI.SELECTED_SOURCE_NOTES);
                        }
                    else if (!dragged)
                        {
                        getGridUI().removeFromSelected(NoteUI.this);
                        }
                    }
                else if (!selected)
                    {
                    getGridUI().clearSelected();
                    getGridUI().addEventToSelected(NoteUI.this, GridUI.SELECTED_SOURCE_NOTES);
                    }
                else
                    {
                    }
                
                dragged = false;
                originallySelected = false;
                getNotesUI().updateChildInspector(true);
                repaint();
                }
            });
                        
        addMouseMotionListener(new MouseMotionAdapter()
            {
            public void mouseMoved(MouseEvent e)
                {
                if (!mouseDown) updateCursor(e);                // FIXME: it'd never be mouseDown...
                }
                        
            public void mouseDragged(MouseEvent e)
                {
                e = SwingUtilities.convertMouseEvent(NoteUI.this, e, getGridUI());
                if (isResizing())
                    {
                    getGridUI().resizeSelectedNotes(NoteUI.this, e);
                    }
                else
                    {
                    getGridUI().moveSelectedNotes(mouseDownEvent, e, NoteUI.this);
                    }
                dragged = true;

                getNotesUI().updateChildInspector(false);
                }
            });
        }
                
    public void paintComponent(Graphics _g)
        {
        Graphics2D g = (Graphics2D) _g;
        Rectangle bounds = getBounds();
        bounds.x = 0;
        bounds.y = 0;
        if (bounds.width < MINIMUM_WIDTH) 
            {
            bounds.width = MINIMUM_WIDTH;
            }
        g.setPaint(velocityMap.getColor((int)value));
        g.fill(bounds);
        
        if (selected)
            {
            bounds.x++;
            bounds.y++;
            bounds.width -=2;
            bounds.height -=2;
            g.setPaint(selectedColor);
            g.setStroke(selectedStroke);
            g.draw(bounds);
            }
        else    
            {
            g.setPaint(strokeColor);
            g.setStroke(stroke);
            g.draw(bounds);
            }
        }

    }
