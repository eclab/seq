/* 
   Copyright 2025 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.notes.gui;

import seq.engine.*;
import seq.gui.*;
import seq.util.*;
import seq.motif.notes.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.locks.*;
import java.awt.geom.*;
import java.util.*;

public class EventsUI extends JComponent
    {
    // The border color between the EventsUI and the GridUI
    public static final Color BORDER_COLOR = Color.BLACK;
    // The types of the parameteruis (including "none")
    int[] types = new int[Notes.NUM_EVENT_PARAMETERS];
    // How tall are my parameters by default?
    public static final int DEFAULT_PARAMETER_HEIGHT = 32 + EventUI.HEIGHT;
    
    // The parent GridUI
    GridUI gridui;
    // The Seq
    Seq seq;
    // All ParameterUIs
    ArrayList<ParameterUI> parameteruis = new ArrayList<>();
    JComponent parameterBox;
    GridLayout parameterBoxLayout;
    
    // Handles mouse global released messages (because they don't always go to the right place)
    AWTEventListener releaseListener = null;
    // True if we're expecting a release, so the releaseListener's messages are valid 
    boolean mouseDown = false;
    // Where the mouse was originally pressed
    MouseEvent mouseDownEvent = null;
    // The original value of the selected event at time of mouse press, for doing diff computations
    int originalValue;
    // The original time of the selected event at time of mouse press, for doing diff computations
    int originalTime;
    // Have we begun to drag?
    boolean dragging;
    // Is the user holding down the SHIFT key during selection?
    boolean shifted;
    // Current parameter height
    int parameterHeight;
    
    /** Returns the Seq */
    public Seq getSeq() { return seq; }
    /** Returns the parent GridUI */
    public GridUI getGridUI() { return gridui; }
    /** Returns the NotesUI */
    public NotesUI getNotesUI() { return gridui.getNotesUI(); }
    /** Returns the ParameterUIs */
    public ArrayList<ParameterUI> getParameterUIs() { return parameteruis; }
    /** Returns the current scale. */
    public double getScale() { return gridui.getScale(); }
    /** Returns the maximum time */
    public int getMaximumTime() { return gridui.getMaximumTime(); }
    /** Returns the current snap */
    public int getSnap() { return gridui.getSnap(); }
    /** Returns the current parameter height */
    public int getParameterHeight() { return parameterHeight; }
    /** Sets the current parameter height */
    public void setParameterHeight(int val) 
        { 
        parameterHeight = val; 
        Prefs.setLastInt("ParameterHeight", val);
        for(ParameterUI parameterui : parameteruis)
            {
            parameterui.revalidate();
            parameterui.updateHeader();
            parameterui.repaint();
            }
        }
    
    public EventsUI(GridUI gridui)
        {
        this.gridui = gridui;
        seq = gridui.getSeq();
        setLayout(new BorderLayout());
        parameterHeight = Prefs.getLastInt("ParameterHeight", DEFAULT_PARAMETER_HEIGHT);
        
        parameterBox = new JComponent()               // Can't use JPanel, Box, or BoxLayout, they max out at 32768 width
            {
            };
        parameterBox.setBorder(null);
        parameterBoxLayout = new GridLayout(0, 1);
        parameterBox.setLayout(parameterBoxLayout);  // So we're using GridLayout, which doesn't appear to have this problem
        add(parameterBox, BorderLayout.CENTER);
        rebuild();
        repaint();
        }

    /** Rebuilds the EventsUI entirely.  This discards all the EventUIs and rebuilds them with
        the proper ParameterUIs. */
    public void rebuild()
        {
        ArrayList<ArrayList<Notes.Event>> events = null;
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            {
            events = getNotesUI().getNotes().getEventsOfTypes(types);
            }
        finally
            {
            lock.unlock();
            }
        
        int size = events.size();
        for(int i = 0; i < parameteruis.size(); i++)
            {
            parameteruis.get(i).rebuild(events.get(i));
            }
        }
        
    /** Returns the given ParameterUI, or null if none. */
    public ParameterUI getParameterUIFor(int type)
        {
        for(int i = 0; i < parameteruis.size(); i++)
            {
            if (parameteruis.get(i).type == type)
                {
                return parameteruis.get(i);
                }
            }
        return null;
        }
                
    /** Returned by getEventBoundingBox.  Contains a START and an END value (which are inclusive). */
    public static class Interval
        {
        public Interval(int start, int end) { this.start = start; this.end = end; }
        public int start;
        public int end;
        }
                
    /** Returns a bounding box around given events.  If SELECTED NOTES, then
        the bounding box is around the selected events; else it is around
        all events.  If there are no events to build a bounding box around,
        null is returned. */
    public Interval getEventBoundingBox(boolean selectedEvents)
        {
        int minx = -1;
        int maxx = -1;
        ReentrantLock lock = seq.getLock();
        for(EventUI eventui : (selectedEvents ? gridui.getSelected() : getAllEventUIs()))
            {
            lock.lock();
            try 
                {
                Notes.Event event = eventui.getEvent();
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
            finally
                {
                lock.unlock();
                }
            }
        if (minx == -1) return null;
        else return new Interval(minx, maxx);
        }
    

    /** Finds the EventUI corresponding to the given event, with the given parameter type. */
    public EventUI getEventUIFor(Notes.Event event, int type)
        {
        int pos = 0;
        for(int i = 0; i < types.length; i++)
            {
            if (types[i] == type)
                {
                return parameteruis.get(pos).getEventUIFor(event);
                }
            else if (types[i] != Notes.EVENT_PARAMETER_NONE)
                {
                pos++;
                }
            }
        return null;
        } 

    /** Builds and returns a list of all EventUIs */
    public ArrayList<EventUI> getAllEventUIs()
        {
        ArrayList<EventUI> all = new ArrayList<>();
        for(ParameterUI parameterui : parameteruis)
            {
            all.addAll(parameterui.getEventUIs());
            }
        return all;
        }

    /** Adds the given EventUI to the proper ParameterUI and returns it.  
        Presumably a new EventUI, as we don't remove it from any old ParameterUIs. */
    public ParameterUI addEventUI(EventUI eventui, int value)
        {
        ParameterUI parameterui = parameteruis.get(value);
        parameterui.addEventUI(eventui);
        return parameterui;
        }
                
    /** Recaches event information in ALL EventUIs in ALL ParameterUIs. */
    public void reload()
        {
        for(ParameterUI parameterui : parameteruis)
            {
            parameterui.reload();
            }
        }

    /** Re-caches all EventUIs to the corresponding events. */
    public void reload(ArrayList<Notes.Event> events)
        {
        HashSet<Notes.Event> hash = new HashSet(events);
        for(ParameterUI parameterui : parameteruis)
            {
            parameterui.reload(hash);
            }
        }
                   
    /** Returns the header for the entire EventsUI */
    public JComponent getHeader()
        {
        Box box = new Box(BoxLayout.Y_AXIS);
        box.add(Ruler.getHeader());
        for(ParameterUI parameterui : parameteruis)
            {
            box.add(parameterui.getHeader());
            }
        JComponent comp = new JComponent() {  };
        comp.setLayout(new BorderLayout());
        comp.add(box, BorderLayout.CENTER);
        return comp;
        }
    }
