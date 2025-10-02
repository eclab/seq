/* 
   Copyright 2025 by Sean Luke and George Mason University
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

public class GridUI extends JComponent
    {
    public static final Color COLOR_CLEAR = new Color(0,0,0,0);
    // Default scaling factor.  Scaling factors are powers of 2
    public static final double DEFAULT_SCALE = 8.0;
    // Highest (most zoomed out) scaling factor.  Scaling factors are powers of 2
    public static final double MAX_SCALE = 32.0;
    // Lowest (most zoomed in) scaling factor.  Scaling factors are powers of 2
    public static final double MIN_SCALE = 0.5;
    // The default maximum time length -- 64 bars
    public static final int DEFAULT_MAXIMUM_TIME = Seq.PPQ * 4 * 64;
    // Keyboard Width
    public static final int KEYBOARD_WIDTH = 40;                // Enough to draw the text for the EventUI Headers
    // This is the size of new notes when snap is turned off
    public static final int DEFAULT_NO_SNAP_NEW_NOTE = Seq.PPQ;
        
    // The parent NotesUI
    NotesUI notesui;
    // The Seq
    Seq seq;
    // All PitchUIs
    ArrayList<PitchUI> pitchuis = new ArrayList<>();
    // The current scale
    double scale = DEFAULT_SCALE;
    // The current maximum time length
    int maximumTime = DEFAULT_MAXIMUM_TIME;
    // Current snap
    int snap = Seq.PPQ;
    // Snap by or snap to?
    boolean snapBy = false;
    
    // The list of selected NoteUIs.  This is  LinkedHashSet to make it faster to iterate through.  Overkill?
    LinkedHashSet<EventUI> selected = new LinkedHashSet<>();
    // The temporary list of previously selected NoteUIs before a rubber-band operation, to do XOR over
    HashSet<EventUI> backupSelected = new HashSet<>();
    
    public static final int SELECTED_SOURCE_NONE = -1;
    public static final int SELECTED_SOURCE_NOTES = Notes.NUM_EVENT_PARAMETERS;
    // Otherwise, the source is the parameter number
    int selectedSource = SELECTED_SOURCE_NONE;
    
    // Handles mouse global released messages (because they don't always go to the right place)
    AWTEventListener releaseListener = null;
    // True if we're expecting a release, so the releaseListener's messages are valid 
    boolean mouseDown = false;
    // Where the mouse was originally pressed
    MouseEvent mouseDownEvent = null;
    // The original pitch of the selected note at time of mouse press, for doing diff computations
    int originalPitch;
    // The original time of the selected note at time of mouse press, for doing diff computations
    int originalTime;
    // Have we begun to drag?
    boolean dragging;
    // Is the user holding down the SHIFT key during selection?
    boolean shifted;
    
    // rubber band pitches -- we use these to determine whether to erase the rubber band in the old PitchUI
    int oldMinPitch = -1;
    int oldMaxPitch = -1;
    
    
    /** Returns the Seq */
    public Seq getSeq() { return seq; }
    /** Returns the PitchUIs */
    public ArrayList<PitchUI> getPitchUIs() { return pitchuis; }
    /** Returns the current scale. */
    public double getScale() { return scale; }
    /** Sets the current scale only if it is betewen MAX_SCALE and MIN_SCALE.  It should be a power of 2 */
    public void setScale(double val) { if (val <= MAX_SCALE && val >= MIN_SCALE) scale = val; }
    /** Returns the NotesUI */
    public NotesUI getNotesUI() { return notesui; }
    /** Returns the maximum time */
    public int getMaximumTime() { return maximumTime; }
    /** Sets the maximum time */
    public void setMaximumTime(int val) { maximumTime = val; }
    /** Returns the current snap.*/
    public int getSnap() { return snap; }
    /** Sets the current snap. */
    public void setSnap(int val) { snap = val; }
    /** Returns the current snap-by. */
    public boolean getSnapBy() { return snapBy; }
    /** Sets the current snap. */
    public void setSnapBy(boolean val) { snapBy = val; }
    /** Returns the selected source */
    public int getSelectedSource() { return selectedSource; }
    
    public GridUI(NotesUI notesui)
        {
        this.notesui = notesui;
        seq = notesui.getSeq();
        setLayout(new BorderLayout());
        //Box box = new Box(BoxLayout.Y_AXIS);
        JComponent box = new JComponent()               // Can't use JPanel, Box, or BoxLayout, they max out at 32768 width
            {
            };
        box.setBorder(null);
        box.setLayout(new GridLayout(128, 1));  // So we're using GridLayout, which doesn't appear to have this problem
        for(int i = 0; i < 128; i++)
            {
            PitchUI pitchui = new PitchUI(this, i);
            pitchuis.add(pitchui);
            }
        // add to box in reverse order
        for(int i = 127; i >= 0; i--)
            {
            box.add(pitchuis.get(i));
            }
        add(box, BorderLayout.CENTER);
        rebuild();



        addMouseListener(new MouseAdapter()
            {   
            public void mouseClicked(MouseEvent e)
                {
                if (e.getClickCount() == 2)
                    {
                    int pitch = getPitch(e);
                    int when = getQuantizedTime(e);
                    
                    int length = snap;
                    if (length <= 1)    // no snap
                        {
                        length = DEFAULT_NO_SNAP_NEW_NOTE;
                        }
                        
                    int velocity = 0;
                    int releaseVelocity = 0;
                    Notes notes = null;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try
                        {
                        notes = getNotesUI().getNotes();
                        velocity = notes.getDefaultVelocity();
                        releaseVelocity = notes.getDefaultReleaseVelocity();
                        }
                    finally
                        {
                        lock.unlock();
                        }

                    Notes.Note note = new Notes.Note(pitch, velocity, when, length, releaseVelocity);
                                     
                    lock.lock();
                    try
                        {
                        notes.getEvents().add(note);
                                
                        // now comes the costly part
                        recomputeMaxTime();             // so they're in the same lock
                        notes.sortEvents();
                        }
                    finally
                        {
                        lock.unlock();
                        }

                    PitchUI pitchui = pitchuis.get(pitch);
                    NoteUI noteui = new NoteUI(pitchui, note);
                    pitchui.addNoteUI(noteui);
                    pitchuis.get(pitch).repaint();
                    clearSelected();
                    addEventToSelected(noteui, SELECTED_SOURCE_NOTES);

                    getNotesUI().updateChildInspector(noteui, true);
                    }
                }
            
            public void mousePressed(MouseEvent e)
                {
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
                mouseDown = true;
                mouseDownEvent = e;
                
                /// End Fix
                
                originalPitch = getPitch(e);
                originalTime = getTime(e);

                shifted = (MotifUI.shiftOrRightMouseButton(e));
                
                if (shifted)
                    {
                    backupSelected.addAll(selected);
                    }
                else
                    {
                    clearSelected();
                    }
                
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

                if (!dragging && !shifted)
                    {
                    clearSelected();
                    }
                else
                    {
                    dragging = false;
                    }

                if (oldMinPitch != -1)
                    {
                    pitchuis.get(oldMinPitch).clearRubberBand();
                    pitchuis.get(oldMinPitch).repaint();
                    oldMinPitch = -1;
                    }
                                        
                if (oldMaxPitch != -1)
                    {
                    pitchuis.get(oldMaxPitch).clearRubberBand();
                    pitchuis.get(oldMaxPitch).repaint();
                    oldMaxPitch = -1;
                    }

                backupSelected.clear();
                 
                notesui.updateChildInspector(true);
                }
 
            });


        addMouseMotionListener(new MouseMotionAdapter()
            {
            public void mouseDragged(MouseEvent e)
                {
                dragging = true;
                                
                // this will be costly
                int pitch = getPitch(e);
                int time = getTime(e);
                
                int lowPitch = Math.min(originalPitch, pitch);
                int highPitch = Math.max(originalPitch, pitch);
                int lowTime = Math.min(originalTime, time);
                int highTime = Math.max(originalTime, time);
                
                clearSelected();

                for(EventUI eventui : backupSelected)
                    {
                    addEventToSelected(eventui, SELECTED_SOURCE_NOTES);
                    }

                ReentrantLock lock = seq.getLock();
                for(PitchUI pitchui : pitchuis)
                    {
                    for(NoteUI noteui : pitchui.getNoteUIs())
                        {
                        lock.lock();
                        try 
                            {
                            Notes.Note note = noteui.getNote();
                            if (note.pitch >= lowPitch &&
                                note.pitch <= highPitch &&
                                note.when + note.length >= lowTime &&
                                note.when <= highTime)
                                {
                                if (backupSelected.contains(noteui))
                                    {
                                    removeFromSelected(noteui);
                                    }
                                else
                                    {
                                    addEventToSelected(noteui, SELECTED_SOURCE_NOTES);
                                    }
                                }
                            }
                        finally
                            {
                            lock.unlock();
                            }
                        }
                    }

                int minX = Math.min(mouseDownEvent.getX(), e.getX());
                int maxX = Math.max(mouseDownEvent.getX(), e.getX());
                int minY = Math.min(mouseDownEvent.getY(), e.getY());
                int maxY = Math.max(mouseDownEvent.getY(), e.getY());

                int minPitch = getPitch(minY);
                if (minPitch < 0) minPitch = 0;
                if (minPitch > 127) minPitch = 127;
                if (oldMinPitch != -1 && oldMinPitch != minPitch)
                    {
                    pitchuis.get(oldMinPitch).clearRubberBand();
                    pitchuis.get(oldMinPitch).repaint();
                    }
                oldMinPitch = minPitch;

                pitchuis.get(minPitch).setRubberBandTop(minX, maxX, minY % PitchUI.getPitchHeight());
                pitchuis.get(minPitch).repaint();

                int maxPitch = getPitch(maxY);
                if (maxPitch < 0) maxPitch = 0;
                if (maxPitch > 127) maxPitch = 127;
                if (oldMaxPitch != -1 && oldMaxPitch != maxPitch)
                    {
                    pitchuis.get(oldMaxPitch).clearRubberBand();
                    pitchuis.get(oldMaxPitch).repaint();
                    }
                oldMaxPitch = maxPitch;

                pitchuis.get(maxPitch).setRubberBandBottom(minX , maxX, maxY % PitchUI.getPitchHeight());
                pitchuis.get(maxPitch).repaint();

                repaint();
                }
            });
        }
        
    /** Builds the Ruler object which appears above the GridUI. */
    public Ruler buildRuler()
        {
        return new Ruler(seq, notesui);
        }

    /** Builds the Keyboard object which appears to the left of the GridUI. */
    public JComponent buildKeyboard()
        {
        Box box = new Box(BoxLayout.Y_AXIS);
        // add to box in reverse order
        int pitchHeight = PitchUI.getPitchHeight();
        for(int i = 127; i >= 0; i--)
            {
            PitchUI pitchui = pitchuis.get(i);
            int height = pitchui.getPreferredSize().height;
            final int width = KEYBOARD_WIDTH;
            final Dimension dim = new Dimension(width, height);

            int mod = i % 12;
            final boolean crack = ((mod == 0 || mod == 5) && i != 0);

            JPanel panel = new JPanel()
                {
                final Line2D.Double cSeparator = new Line2D.Double(0, pitchHeight - 1, width, pitchHeight - 1);
                public Dimension getPreferredSize()
                    {
                    return dim;
                    }
                                        
                public void paintComponent(Graphics _g)
                    {
                    super.paintComponent(_g);
                    if (crack)
                        {
                        Graphics2D g = (Graphics2D) _g;
                
                        g.setPaint(Color.BLACK);
                        g.draw(cSeparator);
                        }
                    }
                };

            panel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.GRAY));

            if (mod == 0)
                {
                JLabel label = null;
                if (pitchHeight <= PitchUI.DEFAULT_PITCH_HEIGHT / 2)
                    {
                    label = new JLabel("<html><font size=1>&nbsp;C" + i/12 + "</font></html>");
                    }
                else if (pitchHeight < PitchUI.DEFAULT_PITCH_HEIGHT)
                    {
                    label = new JLabel("<html><font size=2>&nbsp;C" + i/12 + "</font></html>");
                    }
                else
                    {
                    label = new JLabel("<html>&nbsp;C" + i/12 + "</html>");
                    }
                // pushing up makes the small notes a little clearer
                label.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, COLOR_CLEAR));
                panel.setLayout(new BorderLayout());
                panel.add(label, BorderLayout.WEST);
                }
                                
            panel.setBackground(pitchui.isBlack() ? Color.BLACK : Color.WHITE);
            box.add(panel);
            }
        return box;
        }

    /** Rebuilds the GridUI entirely.  This discards all the NoteUIs and rebuilds them with
        the proper PitchUIs. */
    public void rebuild()
        {
        selected = new LinkedHashSet<>();
        backupSelected = new HashSet<>();
                
        ArrayList<ArrayList<Notes.Note>> notes = null;
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            {
            notes = notesui.getNotes().getNotesByPitch();
            }
        finally
            {
            lock.unlock();
            }
                
        for(int i = 0; i < 128; i++)
            {
            pitchuis.get(i).rebuild(notes.get(i));
            }
        }

    /** Returns pitch corresponding to the given event. */
    public int getPitch(MouseEvent evt)
        {
        return (127 - evt.getY() / PitchUI.getPitchHeight());
        }

    /** Returns pitch corresponding to the given pixel */
    public int getPitch(int y)
        {
        return (127 - y / PitchUI.getPitchHeight());
        }

    /** Returns number of pixels correponding to the given time. */
    public int getPixels(int time)
        {
        return (int)(time / scale);
        }
                
    /** Returns the time corresponding to the given event. */
    public int getTime(MouseEvent evt)
        {
        return getTime(evt.getX());
        }
                
    /** Returns the time corresponding to the given pixel. */
    public int getTime(int x)
        {
        return (int)(x * scale);
        }

    /** Returns the quantized time of the given event given the current snap */
    public int getQuantizedTime(MouseEvent evt)
        {
        return getQuantizedTime((int)(evt.getX() * scale));
        }

    /** Returns the quantized time given the current snap */
    public int getQuantizedTime(int time)
        {
        return (time / snap) * snap;
        }

    /** Returns the difference, in pitch, between the origin and the event. */
    public int getPitchDiff(MouseEvent origin, MouseEvent evt)
        {
        return getPitch(evt) - getPitch(origin);
        }

    /** Returns the difference, in time, between the origin and the event. */
    public int getTimeDiff(MouseEvent origin, MouseEvent evt)
        {
        // FIXME -- is this right or should we floor rather than truncate?
        return (int)((evt.getX() - origin.getX()) * scale);
        }

    /** Returns the difference, in time, between the origin and the event, considering snap quantization. */
    public int getQuantizedTimeDiff(MouseEvent origin, MouseEvent evt)
        {
        // FIXME -- is this right or should we floor rather than truncate?
        return (((int)((evt.getX() - origin.getX()) * scale)) / snap) * snap;
        }

    void recomputeMaxTime()
        {
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            notesui.getNotes().computeMaxTime();
            }
        finally
            {
            lock.unlock();
            }
        }

    /** Resizes the selected notes to a length indicated by the difference from the old origin
        to the new event, quantized. */
    public void resizeSelectedNotes(NoteUI originalNote, MouseEvent evt)
        {
        int time = getTime(evt);
        int origin = 0;
        int currentLen = 0;
        
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            {
            Notes.Note note = originalNote.getNote();
            origin = note.when;
            currentLen = note.length;
            }
        finally
            {
            lock.unlock();
            }
                        
        if (time >= origin)
            {
            int len = 0;
            if (snapBy)
                {
                len = getQuantizedTime(time - origin);
                }
            else
                {
                len = getQuantizedTime(time) - origin;
                }

            boolean[] parameteruisToRepaint = new boolean[Notes.NUM_EVENT_PARAMETERS];
            boolean[] pitchuisToRepaint = new boolean[128];
                        
            if (len > 0 && len != currentLen)               // don't want to go down to zero, it's inconvenient
                {
                for(EventUI eventui : selected)
                    {
                    if (eventui instanceof NoteUI)
                        {
                        lock.lock();
                        try 
                            {
                            Notes.Note note = eventui.getNote();
                            note.length = len;
                            // should this be outside the lock?
                            ((NoteUI)eventui).reload(note.when, note.length);
                            }
                        finally
                            {
                            lock.unlock();
                            }
                            
                        // We need to repaint the whole parameterui or pitchui.  We'll gather here
                        if (eventui instanceof NoteUI)
                            {
                            pitchuisToRepaint[((NoteUI)eventui).pitchui.getPitch()] = true;
                            }
                        else
                            {
                            // This is O(4) but whatever...
                            parameteruisToRepaint[notesui.getEventsUI().getParameterUIs().indexOf(eventui.parameterui)] = true;
                            }
                        }
                    else 
                        { 
                        System.err.println("GridUI.resizeSelectedNotes(): Attempt to resize Events");
                        break;
                        }
                    }
                recomputeMaxTime();
                
                // now let's repaint
                for(int i = 0; i < Notes.NUM_EVENT_PARAMETERS; i++)
                    {
                    if (parameteruisToRepaint[i])
                        {
                        notesui.getEventsUI().getParameterUIs().get(i).repaint();
                        }
                    }
                for(int i = 0; i < 128; i++)
                    {
                    if (pitchuisToRepaint[i])
                        {
                        pitchuis.get(i).repaint();
                        }
                    }
                }
            }
        }
        
    // used internal by moveSelectedNotes to determine which PitchUIs should be
    // repainted as a result of moving the notes.
    boolean[] pitchesToRepaint = new boolean[128];
        
        
    /** Loads and caches the original when and pitch (temporary variables for GridUI to compute moving notes */
    public void loadOriginal()
        {
        ReentrantLock lock = getSeq().getLock();
        for(EventUI eventui : selected)
            {
            lock.lock();
            try 
                {
                Notes.Event event = eventui.event;
                eventui.setOriginalWhen(event.when);
                if (event instanceof Notes.Note)
                    {
                    ((NoteUI)eventui).setOriginalPitch(((Notes.Note)event).pitch);
                    }
                else if (event instanceof Notes.Bend && getNotesUI().getNotes().getWarped())
                    {
                    double val = ((Notes.Bend)event).getWarpedNormalizedValue();
                    eventui.setOriginalValue(val < 0 ? 0.5 : val);
                    }
                else
                    {
                    double val = event.getNormalizedValue();
                    eventui.setOriginalValue(val < 0 ? 0.5 : val);
                    }
                }
            finally
                {
                lock.unlock();
                }
            }
        }
    
       
    // NONE:        Adjust Pitch AND Time
    // META:        Adjust Pitch and NOT Time
        
    /** Moves the selected NOTES, not EVENTS to a new location in pitch and time indicated by the difference from the old origin
        to the new event, quantized. */
    public void moveSelectedNotes(MouseEvent origin, MouseEvent evt, EventUI dragEventUI)
        {
        int timeDiff = 0;
        if (getSnapBy()) 
            {
            timeDiff = getQuantizedTimeDiff(origin, evt);
            }
        else
            {
            if (snap == 1)
                {
                timeDiff = getTimeDiff(origin, evt);
                int w = dragEventUI.getOriginalWhen() + timeDiff;
                w = getQuantizedTime(w);
                timeDiff = w - dragEventUI.getOriginalWhen();
                }
            else
                {
                timeDiff = getQuantizedTime(evt) - dragEventUI.getOriginalWhen();
                }
            }

        boolean dontChangeTime = (MotifUI.optionOrMiddleMouseButton(evt));

        int pitchDiff = getPitchDiff(origin, evt);

        int oldWhen = -1;
        int oldPitch = -1;
        int newWhen = -1;
        int newPitch = -1;
        int length = -1;
        EventUI repaintEventUI = null;
        
        ReentrantLock lock = seq.getLock();
        for(EventUI eventui : selected)
            {
            lock.lock();
            try 
                {
                Notes.Note note = eventui.getNote();
                
                oldWhen = note.when;
                note.when = eventui.getOriginalWhen() + timeDiff;

                if (dontChangeTime)
                    {
                    note.when = oldWhen;
                    }
            
                /// FIXME: should there be a maximum?
                if (note.when < 0) note.when = 0;
                newWhen = note.when;
                                
                length = note.length;

                if (note.when != oldWhen)
                    {
                    pitchesToRepaint[note.pitch] = true;    // slide the note
                    }

                oldPitch = note.pitch;
                note.pitch = ((NoteUI)eventui).getOriginalPitch() + pitchDiff;
                if (note.pitch < 0) note.pitch = 0;
                if (note.pitch > 127) note.pitch = 127;
                newPitch = note.pitch;
                        
                if (note.pitch != oldPitch)
                    {
                    pitchesToRepaint[oldPitch] = true;      // remove the note
                    pitchesToRepaint[note.pitch] = true;    // add the note
                    }
                }
            finally
                {
                lock.unlock();
                }
            
            ((NoteUI)eventui).reload(oldWhen, newWhen, oldPitch, newPitch, length);
            }
        
        // re-sort -- this is going to be EXPENSIVE
        lock.lock();
        if (!MotifUI.optionOrMiddleMouseButton(evt))
            {
            recomputeMaxTime();             // so they're in the same lock
            }
        try
            {
            getNotesUI().getNotes().sortEvents();
            }
        finally
            {
            lock.unlock();
            }
        
        // Time to repaint!  Just the affected pitches
        for(int i = 0; i < pitchesToRepaint.length; i++)
            {
            if (pitchesToRepaint[i])
                {
                pitchuis.get(i).repaint();
                }
            pitchesToRepaint[i] = false;
            }
        }
        
    /** Entirely deletes from the GridUI the provided events. */
    public void deleteEvents(ArrayList<Notes.Event> events)
        {
        deleteEvents(new HashSet<Notes.Event>(events));
        }
        
    /** Entirely deletes from the GridUI the provided events. */
    public void deleteEvents(HashSet<Notes.Event> events)
        {        
        ReentrantLock lock = seq.getLock();
        if (events.size() > 0)
            {
            lock.lock();
            try 
                {
                notesui.getNotes().remove(events);
                }
            finally
                {
                lock.unlock();
                }
                
            rebuild();
            notesui.getEventsUI().rebuild();
            }
        }

    /** Entirely deletes from the GridUI the selected events. */
    public void deleteSelectedEvents()
        {
        HashSet<Notes.Event> cut = new HashSet<>();
        
        ReentrantLock lock = seq.getLock();
        for(EventUI eventui : selected)
            {
            lock.lock();
            try 
                {
                // For the time being, this will be COSTLY
                cut.add(eventui.event);
                }
            finally
                {
                lock.unlock();
                }
            }
        
        deleteEvents(cut);
        }

    /** Duplicates the selected notes, making new ones just beyond them in time. 
        Deselects the selected notes and selects the new notes. */
    public void copySelectedEvents()
        {
        HashSet<Notes.Event> newNotes = new HashSet<>();
        boolean isNote = false;
                
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            {
            ArrayList<Notes.Event> events = notesui.getNotes().getEvents();  
            for(EventUI eventui : selected)
                {
                Notes.Event newNote = (Notes.Event)(eventui.event.copy());
                if (newNote instanceof Notes.Note)
                    {
                    isNote = true;
                    newNote.when = newNote.when + newNote.getLength();
                    }
                else
                    {
                    newNote.when = newNote.when + ParameterUI.COPY_DISTANCE;
                    }
                events.add(newNote);
                newNotes.add(newNote);
                }
            recomputeMaxTime();             // so they're in the same lock
            notesui.getNotes().sortEvents();                // costly
            }
        finally
            {
            lock.unlock();
            }

                
        if (newNotes.size() > 0)
            {
            clearSelected();

            // rebuild first so the note is now in the pitches / parameters
            rebuild();
            notesui.getEventsUI().rebuild();

            if (isNote)
                {
                addNotesToSelected(newNotes);
                }
            else
                {
                addEventsToSelected(newNotes, selectedSource);
                }
                
            repaint();
            notesui.getEventsUI().repaint();
            }
        }

    /** Returns a bounding box around given notes.  If SELECTED EVENTS, then
        the bounding box is around the selected notes or events; else it is around
        all notes (not events).  If there are no notes to build a bounding box around,
        null is returned. 
        
        If the bounding box has a Y value < 0, then this box is
        with regard to a non-note EVENT and only has a TIME, not a PITCH. 
    */
    public Rectangle getEventBoundingBox(boolean selectedEvents)
        {
        int minx = -1;
        int miny = -1;
        int maxx = -1;
        int maxy = -1;
        ReentrantLock lock = seq.getLock();
        for(EventUI eventui : (selectedEvents ? selected : getAllNoteUIs()))
            {
            lock.lock();
            try 
                {
                Notes.Event event = eventui.event;
                if (event instanceof Notes.Note)
                    {
                    Notes.Note note = (Notes.Note)event;
                    if (minx == -1)
                        {
                        miny = note.pitch;
                        maxy = note.pitch;
                        minx = note.when;
                        maxx = note.when + note.length;
                        }
                    else
                        {
                        if (note.pitch < miny) miny = note.pitch;
                        if (note.pitch > maxy) maxy = note.pitch;
                        if (note.when < minx) minx = note.when;
                        if (note.when + note.length > maxx) maxx = note.when + note.length;
                        }
                    }
                else
                    {
                    if (minx == -1)
                        {
                        minx = event.when;
                        maxx = event.when;
                        }
                    else
                        {
                        if (event.when < minx) minx = event.when;
                        if (event.when > maxx) maxx = event.when;
                        }
                    }
                }
            finally
                {
                lock.unlock();
                }
            }
        if (minx == -1) return null;
        else return new Rectangle(minx, miny, maxx - minx, maxy - miny);
        }
    
    /** Removes all NoteUIs from selected and deselects them. */
    public void clearSelected()
        {
        // Deselect everyone
        for(EventUI eventui : selected)
            {
            eventui.setSelected(false, seq.getLock());      // so we don't have to reload
            }
        selected.clear();
        selectedSource = SELECTED_SOURCE_NONE;
        }
       
       
    /** Makes the selected notes appear on top of other notes. */
    public void moveSelectedToTop()
        {
        // Can't believe I have to do this
        HashSet<NoteUI> notes = new HashSet<>();
        for(EventUI event : getSelected())
            {
            if (event instanceof NoteUI)
                {
                notes.add((NoteUI)event);
                }
            }
        moveToTop(notes);
        }

    /** Makes the selected notes appear on top of other notes. */
    public void moveSelectedToBottom()
        {
        // Can't believe I have to do this
        HashSet<NoteUI> notes = new HashSet<>();
        for(EventUI event : getSelected())
            {
            if (event instanceof NoteUI)
                {
                notes.add((NoteUI)event);
                }
            }
        moveToBottom(notes);
        }
       
    /** Makes the given notes appear on top of other notes. */
    public void moveToTop(HashSet<NoteUI> move)
        {
        if (move.size() > 0)
            {
            for(PitchUI pitchui : pitchuis)
                {
                pitchui.moveToPosition(move, true);
                }
            }
        }
        
    /** Makes the given notes appear beneath other notes. */
    public void moveToBottom(HashSet<NoteUI> move)
        {
        if (move.size() > 0)
            {
            for(PitchUI pitchui : pitchuis)
                {
                pitchui.moveToPosition(move, false);
                }
            }
        }
        
    /** Finds the NoteUIs for the given note, then adds them to selected and selects them. */
    public void addNotesToSelected(HashSet<Notes.Event> notes)
        {
        HashSet<NoteUI> move = new HashSet<>();
        for(PitchUI pitchui : pitchuis)
            {
            move.clear();
            for(NoteUI noteui : pitchui.getNoteUIs())
                {
                if (notes.contains(noteui.event))
                    {
                    addEventToSelected(noteui, SELECTED_SOURCE_NOTES);
                    move.add(noteui);
                    }
                }
            }
        moveToTop(move);                // I think this is SLIGHTLY more efficent than moveSelectedToTop?
        }

    /** Finds the EventUIs for the given event, then adds them to selected and selects them. */
    public void addEventsToSelected(HashSet<Notes.Event> events, int source)
        {
        for(ParameterUI parameterui : notesui.getEventsUI().parameteruis)
            {
            for(EventUI eventui : parameterui.getEventUIs())
                {
                if (events.contains(eventui.event))
                    {
                    addEventToSelected(eventui, source);
                    }
                }
            }
        }
        
    /** Returns the first or sole selected EventUI, or none if there is none. */
    public EventUI getFirstSelected()
        {
        if (selected.size() == 0) return null;
        else return (EventUI)(selected.iterator().next());
        }
        
    /** Clears all selected if their type is not the same as the given eventui. */
    public void checkSelectedAndClear(EventUI eventui)
        {
        if (selected.size() > 0)
            {
            EventUI test = getFirstSelected();
            if (eventui instanceof NoteUI)
                {
                if (!(test instanceof NoteUI))
                    {
                    clearSelected();
                    }
                }
            else            // note a note.  Which parameter am I?
                {
                if (test.getParameterUI() != eventui.getParameterUI())
                    {
                    clearSelected();
                    }
                }
            }
        }

    /** Adds the NoteUI to selected and selects it */
    public void addEventToSelected(EventUI eventui, int source)
        {
        checkSelectedAndClear(eventui);
        eventui.setSelected(true, seq.getLock());       // so we don't have to reload
        selected.add(eventui);
        notesui.getRuler().clearRange();
        selectedSource = source;
        }

    public void addToSelected(ArrayList<NoteUI> noteuis)
        {
        for(NoteUI noteui : noteuis)
            {
            addEventToSelected(noteui, SELECTED_SOURCE_NOTES);
            }
        }

    /** Removes the NoteUI from selected and deselects it. */
    public void removeFromSelected(EventUI eventui)
        {
        eventui.setSelected(false, seq.getLock());      // so we don't have to reload
        selected.remove(eventui);
        if (selected.size() == 0) 
            {
            selectedSource = SELECTED_SOURCE_NONE;
            }
        }
        
    /** Returns all selected events or ones within the ruler range */
    public ArrayList<Notes.Event> getSelectedOrRangeEvents()                            // FIXME should we include overlaps?
        {
        if (notesui.getRuler().getHasRange())
            {
            ArrayList<Notes.Event> events = new ArrayList<>();
            int min = notesui.getRuler().getRangeLow();
            int max = notesui.getRuler().getRangeHigh();
            ReentrantLock lock = seq.getLock();
            for(Notes.Event event : notesui.getNotes().getEvents())
                {
                lock.lock();
                try
                    {
                    if (event.when >= min && event.when < max)              // FIXME, should this be < or <= ?
                        {
                        events.add(event);
                        }
                    }
                finally
                    {
                    lock.unlock();
                    }
                }
            return events;
            }
        else
            {
            return getSelectedEvents();
            }
        }
        
    /** Returns all selected events */
    public ArrayList<Notes.Event> getSelectedEvents()                            // FIXME should we include overlaps?
        {
        ArrayList<Notes.Event> events = new ArrayList<>();
        for(EventUI eventui : getSelected())
            {
            events.add(eventui.event);
            }
        return events;
        }
        

    /** Returns the Selected NoteUIs */
    public LinkedHashSet<EventUI> getSelected()
        {
        return selected;
        }
        
    /** Returns the Selected NoteUIs Backup */
    public HashSet<EventUI> getBackupSelected()
        {
        return backupSelected;
        }
        
    /** Returns the maximum time of any selected event, including length.  Returns 0 if no selected events. */
    public int getMaximumSelectedTime()
        {
        int max = -1;
        ReentrantLock lock = seq.getLock();
        for(EventUI eventui : getSelected())
            {
            int val;
            lock.lock();
            try
                {
                val = eventui.getEvent().when + eventui.getEvent().getLength();
                }
            finally 
                {
                lock.unlock();
                }
            if (max < val)
                {
                max = val;
                }
            }
        return (max == -1 ? 0 : max);
        }

    /** Returns the minimum time of any selected event, Returns 0 if no selected events. */
    public int getMinimumSelectedTime()
        {
        int min = -1;
        ReentrantLock lock = seq.getLock();
        for(EventUI eventui : getSelected())
            {
            int val;
            lock.lock();
            try
                {
                val = eventui.getEvent().when;
                }
            finally 
                {
                lock.unlock();
                }
                        
            if (min == -1 || min > val)
                {
                min = val;
                }
            }
        return (min == -1 ? 0 : min);
        }
    
    /** Builds and returns a list of all NoteUIs */
    public ArrayList<NoteUI> getAllNoteUIs()
        {
        ArrayList<NoteUI> all = new ArrayList<>();
        for(PitchUI pitchui : pitchuis)
            {
            all.addAll(pitchui.getNoteUIs());
            }
        return all;
        }

    /** Finds and returns the NoteUI for the given note, in the PitchUI representing pitch */
    public NoteUI getNoteUIFor(Notes.Note note, int pitch)
        {
        return pitchuis.get(pitch).getNoteUIFor(note);
        } 

    /** Recaches note information for the selected NoteUIs only. */
    public void reloadSelected()
        {
        for(EventUI event : selected)
            {
            event.reload();
            }
        }
        
    /** Re-caches all NoteUIs to the corresponding events. */
    public void reload(ArrayList<Notes.Event> events)
        {
        HashSet<Notes.Event> hash = new HashSet(events);
        for(PitchUI pitchui : pitchuis)
            {
            pitchui.reload(hash);
            }
        }
                   
    /** Adds the given NoteUI to the proper PitchUI and returns it.  
        Presumably a new NoteUI, as we don't remove it from any old PitchUIs. */
    public PitchUI addNoteUI(NoteUI noteui, int pitch)
        {
        PitchUI pitchui = pitchuis.get(pitch);
        pitchui.addNoteUI(noteui);
        // make sure it's up front
        pitchui.moveToTop(noteui);
        return pitchui;
        }
                
    /** Recaches note information in ALL NoteUIs in ALL PitchUIs. */
    public void reload()
        {
        for(PitchUI pitchui : pitchuis)
            {
            pitchui.reload();
            }
        }
    }
