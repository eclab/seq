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

public class EventUI extends JComponent
    {
    // The Event's Height.
    public static final int HEIGHT = 9;
    // The Event's Width.
    public static final int WIDTH = 9;
    // Stroke for drawing me normally
    public static final Stroke stroke = new BasicStroke(1.0f);
    // Stroke for drawing me while selected
    public static final Stroke selectedStroke = new BasicStroke(3.0f);
    // Stroke color for drawing me normally
    public static final Color strokeColor = new Color(64, 64, 64); 
    // Stroke color for drawing me while selected
    public static final Color selectedColor = Color.BLUE; 
    // Mapping of value to color
    public static final SimpleColorMap valueMap = new SimpleColorMap(0, 127, Color.GRAY, Color.RED);

    // The parameterui owner
    ParameterUI parameterui;
    // The event
    Notes.Event event;
    // Event parameter, also used for pitch by NoteUI
    //int parameter;
    // Normalized Event value, also used for velocity by NoteUI
    double value;
    // Am I selected?
    boolean selected;
    // Temporary storage of old time and value to allow us to compute new locations while moving events
    int originalWhen;
    double originalValue;               // reused as originalPitch
    

    // These are carefully chosen to be static variables so we can save some memory
    
    // Handles mouse global released messages (because they don't always go to the right place)
    static AWTEventListener releaseListener = null;
    // True if we're expecting a release, so the releaseListener's messages are valid 
    static boolean mouseDown = false;
    // Where the mouse was originally pressed
    static MouseEvent mouseDownEvent = null;
    // Have we dragged yet
    static boolean dragged = false;
    // Was I selected prior to this mouse event?
    static boolean originallySelected = false;
            
    
    // Returns the Seq
    Seq getSeq() { return parameterui.getEventsUI().getSeq(); }

    // Returns the GridUI
    GridUI getGridUI() { return parameterui.getEventsUI().getGridUI(); }

    // Returns the NotesUI
    NotesUI getNotesUI() { return parameterui.getEventsUI().getGridUI().getNotesUI(); }

    // Retuerns the EventsUI
    EventsUI getEventsUI() { return parameterui.getEventsUI(); }
        
    ParameterUI getParameterUI() { return parameterui; }
        
    /** Returns the Note */
    public Notes.Event getEvent() { return event; }
        
    /** Returns the Note */
    public Notes.Note getNote() { return (Notes.Note)event; }

    /** Sets whether the event is selected, and repaints the event. 
        The lock is provided to make this a tiny bit faster*/
    public void setSelected(boolean val, ReentrantLock lock) 
        { 
        lock.lock();
        try 
            {
            event.selected = val; 
            }
        finally
            {
            lock.unlock();
            }

        selected = val; 
        repaint(); 
        }
        
    /** Returns the original when (a temporary variable for GridUI to compute moving notes */
    public int getOriginalWhen() { return originalWhen; }

    /** Sets the original when (a temporary variable for GridUI to compute moving notes */
    public void setOriginalWhen(int when) { originalWhen = when; }

    /** Returns the original value (a temporary variable for us to compute moving events */
    public double getOriginalValue() { return originalValue; }
    public void setOriginalValue(double value) { originalValue = value; }
    
    int computeY()
        {
        // Centery ranges from HEIGHT / 2 to bounds.height - HEIGHT/2 corresponding to the value
        return HEIGHT / 2 + (int)((parameterui.getBounds().height - HEIGHT) * value);
        }
    
    
    /** Reloads the EventUI to a new time, using the provided values.  Reloading
        simply sets the bounds of the EventUI.  */ 
    public void reload(int when)
        {
        // We subtract WIDTH / 2 to center it
        setBounds((int)(when / getGridUI().getScale()) - WIDTH / 2, computeY() - HEIGHT / 2, WIDTH, HEIGHT);
        }
                    
    /** Reloads the EventUI to a new time, value, velocity, selected, and length,
        using the values in the Event.  Reloading
        simply sets the bounds of the EventUI and moves it to the proper EventsUI if needed,
        as well as the velocity, and selected values.
    */ 
    public void reload()
        {
        int when = 0;
        ReentrantLock lock = getSeq().getLock();
        lock.lock();
        try 
            {
            when = event.when;
            //parameter = event.getParameter();
            value = event.getNormalizedValue(parameterui.getNotesUI().getNotes().getLog());
            selected = event.selected;
            }
        finally
            {
            lock.unlock();
            }
            
        reload(when);
        if (selected) getGridUI().addEventToSelected(this, getEventsUI().getParameterUIs().indexOf(parameterui));
        }
        
        
        
        
    ///// MOUSE EVENTS
    
    // So NoteUI can subclass but not use the main constructor
    protected EventUI()
        {
        }
      
    public EventUI(ParameterUI parameterui, Notes.Event event)
        {
        super();
        
        this.event = event;
        this.parameterui = parameterui;
        reload();
        
        addMouseListener(new MouseAdapter()
            {   
            public void mousePressed(MouseEvent e)
                {
                mouseDownEvent = SwingUtilities.convertMouseEvent(EventUI.this, e, getEventsUI());
                    
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
                        getGridUI().addEventToSelected(EventUI.this, getEventsUI().getParameterUIs().indexOf(parameterui));
                        }
                    }
                else if (!selected)
                    {
                    getGridUI().clearSelected();
                    getGridUI().addEventToSelected(EventUI.this, getEventsUI().getParameterUIs().indexOf(parameterui));
                    }                

                getGridUI().loadOriginal();

                mouseDown = true;
                getNotesUI().updateChildInspector(true);
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
                    if (!dragged && originallySelected)
                        {
                        getGridUI().removeFromSelected(EventUI.this);
                        }
                    }
                else
                    {
                    }
                
                dragged = false;
                originallySelected = false;
                getNotesUI().updateChildInspector(true);
                }
            });
                        
                        
                        
        // NONE:        Adjust Value, not Time
        // META:        Adjust Time, Not Value
        // ALT:         Adjust Time AND Value
        // CTRL:        Set value of everyone to the value
        
        addMouseMotionListener(new MouseMotionAdapter()
            {
            public void mouseDragged(MouseEvent e)
                {
                e = SwingUtilities.convertMouseEvent(EventUI.this, e, getEventsUI());
                
                double newValue = getEventValue(mouseDownEvent, e);
                boolean timeChanged = eventTimeWouldChange(mouseDownEvent, e, EventUI.this);
                
                ReentrantLock lock = getSeq().getLock();
                for(EventUI eventui : getGridUI().getSelected())
                    {
                    if ((e.getModifiers() & InputEvent.META_MASK) == InputEvent.META_MASK)
                        {
                        // Do not adjust value
                        }
                    else if ((e.getModifiers() & InputEvent.CTRL_MASK) == InputEvent.CTRL_MASK)
                        {
                        lock.lock();
                        try 
                            {
                            eventui.event.setNormalizedValue(newValue, parameterui.getNotesUI().getNotes().getLog());
                            }
                        finally
                            {
                            lock.unlock();
                            }
                        }
                    else
                        {
                        eventui.changeEventValue(mouseDownEvent, e);
                        }

                    if ((e.getModifiers() & InputEvent.ALT_MASK) == InputEvent.ALT_MASK ||
                        (e.getModifiers() & InputEvent.META_MASK) == InputEvent.META_MASK)
                        {
                        eventui.changeEventTime(mouseDownEvent, e, EventUI.this);    
                        }        
                    eventui.reload();    
                    }
                
                if (timeChanged)
                    {
                    lock.lock();
                    try
                        {
                        getNotesUI().getNotes().sortEvents();
                        }
                    finally 
                        {
                        lock.unlock();
                        }
                    }
                dragged = true;
                getNotesUI().updateChildInspector(false);
                }
            });
        }
        
    /** Return the new time based on the origin and event, as well as the original time from the eventui */
    public int getEventTime(MouseEvent origin, MouseEvent evt, EventUI dragEventUI)
        {
        int time = 0;
        
        ReentrantLock lock = getSeq().getLock();
        lock.lock();
        try 
            {
            time = event.when;
            }
        finally
            {
            lock.unlock();
            }

        int timeDiff = 0;
        if (getGridUI().getSnapBy()) 
            {
            timeDiff = getGridUI().getQuantizedTimeDiff(origin, evt);
            }
        else
            {
            timeDiff = getGridUI().getTimeDiff(origin, evt);
            int w = dragEventUI.getOriginalWhen() + timeDiff;
            w = getGridUI().getQuantizedTime(w);
            timeDiff = w - dragEventUI.getOriginalWhen();
            }

        int newTime = originalWhen + timeDiff;

        /// FIXME: should there be a maximum?
        if (newTime < 0) newTime = 0;
        return newTime;        
        }

    /** Returns whether the time has changed based on the origin and event, as well as the original time from the eventui */
    public boolean eventTimeWouldChange(MouseEvent origin, MouseEvent evt, EventUI dragEventUI)
        {
        int oldTime;
        int newTime = getEventTime(origin, evt, dragEventUI);
        ReentrantLock lock = getSeq().getLock();
        lock.lock();
        try 
            {
            oldTime = event.when;
            }
        finally
            {
            lock.unlock();
            }
        return (oldTime != newTime);
        }

    /** Sets the time has changed based on the origin and event, as well as the original time from the eventui, and returns if it has changed. */
    public boolean changeEventTime(MouseEvent origin, MouseEvent evt, EventUI dragEventUI)
        {
        int oldTime;
        int newTime = getEventTime(origin, evt, dragEventUI);
        ReentrantLock lock = getSeq().getLock();
        lock.lock();
        try 
            {
            oldTime = event.when;
            event.when = newTime;
            }
        finally
            {
            lock.unlock();
            }
        return (oldTime != newTime);
        }
    
    /** Returns the event value based on the origin and mouse event. */
    public double getEventValue(MouseEvent origin, MouseEvent evt)
        {
        double valueDiff = parameterui.getValueDiff(origin, evt);
        double parameterHeight = getEventsUI().getParameterHeight();
        valueDiff /= (parameterHeight - HEIGHT) / parameterHeight;
        double newValue = originalValue + valueDiff;
        if (newValue < 0) newValue = 0;
        if (newValue > 0.999999999) newValue = 0.999999999;     // we want this UNDER 1.0
        return newValue;
        }
    
    
    /** Sets the event value based on the origin and mouse event.  Returns the value it was set to. */
    public double changeEventValue(MouseEvent origin, MouseEvent evt)
        {
        double newValue = getEventValue(origin, evt);
        ReentrantLock lock = getSeq().getLock();
        lock.lock();
        try 
            {
            event.setNormalizedValue(newValue, parameterui.getNotesUI().getNotes().getLog());
            }
        finally
            {
            lock.unlock();
            }
        return newValue;
        }
    
    public void paintComponent(Graphics _g)
        {
        Graphics2D g = (Graphics2D) _g;
        Rectangle bounds = getBounds();
        bounds.x = 0;
        bounds.y = 0;
        bounds.height = HEIGHT;
        
        g.setPaint(valueMap.getColor(value));
        g.fill(bounds);
        
        if (selected)
            {
            bounds.x++;
            bounds.y++;
            bounds.width -=2 ;
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
