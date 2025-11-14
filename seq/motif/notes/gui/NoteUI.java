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

public class NoteUI extends EventUI implements Comparable
    {
    // Smallest width I'm permitted to be.  Maybe this should be 5.
    public static final int MINIMUM_WIDTH = 3;
    // Stroke color for drawing me normally -- overrides EventUI ?
    public static final Color STROKE_COLOR = Color.BLACK; // new Color(64, 64, 64); 
    // Stroke color for drawing me while selected
    public static final Color RECORDED_COLOR = Color.WHITE; 
    // Region at the far right of me where clicking in that region results in me trying to be resized
    public static final int RESIZE_REGION_WIDTH = 6;

    // Color of Velocity start
    public static final Color VELOCITY_START_COLOR = Theme.isDark() ? Theme.GRAY_70 : Color.GRAY;
    // Color of Velocity midpoint
    public static final Color VELOCITY_MID_COLOR = Theme.isDark() ? Theme.ORANGE : Color.RED;
    // Color of Velocity end
    public static final Color VELOCITY_END_COLOR = Theme.isDark() ? Theme.RED : Color.YELLOW;

    // Mapping of velocity to color
    public static final SimpleColorMap VELOCITY_MAP = //new SimpleColorMap(0, 127, Color.GRAY, Color.RED);
        new SimpleColorMap(0, 127, 64, 
            new SimpleColorMap(0, 64, VELOCITY_START_COLOR, VELOCITY_MID_COLOR),
            new SimpleColorMap(64, 127, VELOCITY_MID_COLOR, VELOCITY_END_COLOR));
               
    // backpointer to the owner PitchUI
    PitchUI pitchui;
    // is this a temporary displayed noteui for a recorded note?
    boolean recorded;
    
    // Returns the Seq
    Seq getSeq() { return pitchui.getSeq(); }

    // Returns the GridUI
    GridUI getGridUI() { return pitchui.getGridUI(); }

    // Returns the NotesUI
    NotesUI getNotesUI() { return getGridUI().getNotesUI(); }
    
    PitchUI getPitchUI() { return pitchui; }

    void setPitchUI(PitchUI val) { pitchui = val; }

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
        setBounds((int)(when / getGridUI().getScale()), 0, width, pitchui.getPitchHeight());
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
            value = note.velocity;
            selected = note.selected;
            }
        finally
            {
            lock.unlock();
            }

        if (pitch != pitchui.getPitch() && !recorded)
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
        this(pitchui, note, false);
        }
        
    public NoteUI(PitchUI pitchui, Notes.Note note, boolean recorded)
        {
        this.event = note;
        this.pitchui = pitchui;
        this.recorded = recorded;
        
        reload();
        
        addMouseListener(new MouseAdapter()
            {   
            public void mouseEntered(MouseEvent e)
                {
                if (recorded) return;
                if (!mouseDown) updateCursor(e);
                }

            public void mouseExited(MouseEvent e)
                {
                if (recorded) return;
                if (!mouseDown && cursor != Cursor.DEFAULT_CURSOR)
                    {
                    cursor = Cursor.DEFAULT_CURSOR;
                    setCursor(new Cursor(cursor));
                    }       
                }

            public void mousePressed(MouseEvent e)
                {
                if (recorded) return;
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

                if (MotifUI.shiftOrRightMouseButton(e))
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
                if (recorded) return;
                if (!mouseDown) return;                                 // not released on me

                if (releaseListener != null)
                    {
                    Toolkit.getDefaultToolkit().removeAWTEventListener(releaseListener);
                    }
                            
                mouseDown = false;
                mouseDownEvent = null;


                if (MotifUI.shiftOrRightMouseButton(e))
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
                

                // FIXME: Should this be in mouseDragged?  It's be less buggy there but
                // much less efficient.
                if (dragged)
                    {
                    Notes notes = getNotesUI().getNotes();
                    ReentrantLock lock = getSeq().getLock();
                    lock.lock();
                    try 
                        {
                        notes.computeMaxTime();
                        }
                    finally
                        {
                        lock.unlock();
                        }
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
                if (recorded) return;
                if (!mouseDown) updateCursor(e);                // FIXME: it'd never be mouseDown...
                }
                        
            public void mouseDragged(MouseEvent e)
                {
                if (recorded) return;
                GridUI gridui = getGridUI();
                e = SwingUtilities.convertMouseEvent(NoteUI.this, e, gridui);
                if (isResizing())
                    {
                    if (!dragged)
                        {
                        gridui.getNotesUI().getSeqUI().push();
                        }
                    gridui.resizeSelectedNotes(NoteUI.this, e);
                    }
                else
                    {
                    if (!dragged)
                        {
                        gridui.getNotesUI().getSeqUI().push();
                        gridui.moveSelectedToTop();
                        }
                    gridui.moveSelectedNotes(mouseDownEvent, e, NoteUI.this);
                    }
                dragged = true;

                getNotesUI().updateChildInspector(false);
                }
            });
        }
        
    
    /** This REQUIRES that a lock has been obtained */
    public int compareTo(Object other)
        {
        if (other == null) return -1;           // should not happen
        if (other instanceof NoteUI)
            {
            NoteUI o = (NoteUI)other;
            if (o.event.when > event.when)
                {
                return -1;
                }
            else if (o.event.when < event.when)
                {
                return 1;
                }
            else return 0;
            }
        else return -1;                                         // should not happen
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
        if (value < 0)
            {
            g.setPaint(DEFAULT_COLOR);
            }
        else
            {
            g.setPaint(VELOCITY_MAP.getColor((int)value));
            }
        g.fill(bounds);
        
        if (recorded)
            {
            bounds.x++;
            bounds.y++;
            bounds.width -=2;
            bounds.height -=2;
            g.setPaint(RECORDED_COLOR);
            g.setStroke(SELECTED_STROKE);
            g.draw(bounds);
            }
        else if (selected)
            {
            bounds.x++;
            bounds.y++;
            bounds.width -=2;
            bounds.height -=2;
            g.setPaint(SELECTED_COLOR);
            g.setStroke(SELECTED_STROKE);
            g.draw(bounds);
            }
        else    
            {
            g.setPaint(STROKE_COLOR);
            g.setStroke(STROKE);
            g.draw(bounds);
            }
        }

    }
