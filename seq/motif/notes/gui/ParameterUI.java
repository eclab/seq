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

public class ParameterUI extends JComponent
    {
    // How far away should the new copied parameter be from me in time?
    public static final int COPY_DISTANCE = 48;
    // My background color if I am a black event pitch
    public static final Color BACKGROUND_COLOR = new Color(210, 210, 210);
    // The color of the lower border of the ruler
    public static final Color BORDER_COLOR = Color.BLACK;

    // The GridUI
    GridUI gridui;
    // The parent EventsUI of this ParameterUI
    EventsUI eventsui;
    // The parameter type
    int type;
    // All eventuis in the ParameterUI.  Note that these may not be in any order.
    ArrayList<EventUI> eventuis = new ArrayList<>();
    // Handles mouse global released messages (because they don't always go to the right place)
    AWTEventListener releaseListener = null;
    // True if we're expecting a release, so the releaseListener's messages are valid 
    boolean mouseDown = false;
    // Where the mouse was originally pressed
    MouseEvent mouseDownEvent = null;
    // Have we begun to drag?
    boolean dragging;
    // Is the user holding down the SHIFT key during selection?
    boolean shifted;
    // The original time of the selected event at time of mouse press, for doing diff computations
    int originalTime;
    
    // The rubber band goes from (startx, starty) to (endx, endy).
    // If these values are -1, they have not been set and should not be drawn.
    int rubberBandStartX = -1;
    int rubberBandEndX = -1;
        
    /** Sets the top line of the rubber band */
    public void setRubberBand(int startX, int endX)
        {
        rubberBandStartX = startX;
        rubberBandEndX = endX;
        }
    
    /** Clears the rubber band */
    public void clearRubberBand()
        {
        rubberBandStartX = -1;
        rubberBandEndX = -1;
        }
                
    /** Returns the EventsUI */
    public EventsUI getEventsUI() { return eventsui; }

    /** Returns the NotesUI */
    public NotesUI getNotesUI() { return getGridUI().getNotesUI(); }

    /** Returns the GridUI */
    public GridUI getGridUI() { return gridui; }

    /** Returns the Seq */
    public Seq getSeq() { return eventsui.getSeq(); }

    /** Returns the type */
    public int getType() { return type; }

    public ParameterUI(EventsUI eventsui, int type)
        {
        this.eventsui = eventsui;
        this.gridui = eventsui.getGridUI();
        this.type = type;
        rebuild();
        updateHeader();

        addMouseListener(new MouseAdapter()
            {   
            public void mouseClicked(MouseEvent e)
                {
                if (e.getClickCount() == 2)
                    {
                    double value = getValue(e);
                    int when = getQuantizedTime(e);
                        
                    Notes.Event event = Notes.buildEvent(type, when, value);
                                     
                    ReentrantLock lock = getSeq().getLock();
                    lock.lock();
                    try
                        {
                        Notes events = getNotesUI().getNotes();
 
                        if (event instanceof Notes.Bend && events.getLog())
                            {
                            ((Notes.Bend)event).setNormalizedLogValue(value);
                            }

                        events.getEvents().add(event);
                                
                        // now comes the costly part
                        events.sortEvents();
                        events.computeMaxTime();
                        }
                    finally
                        {
                        lock.unlock();
                        }

//                    getNotesUI().table.reload();                                 // we will change the table
//        getEventsUI().table.setSelection(where);                                       // FIXME I'm not sure how to do this any more

                    EventUI eventui = new EventUI(ParameterUI.this, event);
                    addEventUI(eventui);
                    repaint();
                    getGridUI().clearSelected();
                    getGridUI().addEventToSelected(eventui, eventsui.getParameterUIs().indexOf(ParameterUI.this));

                    getNotesUI().updateChildInspector(eventui, true);
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
                
                // originalPitch = getPitch(e);
                originalTime = getTime(e);

                shifted = MotifUI.shiftOrRightMouseButton(e);
                
                if (shifted)
                    {
                    getGridUI().getBackupSelected().addAll(getGridUI().getSelected());
                    }
                else
                    {
                    getGridUI().clearSelected();
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
                    gridui.clearSelected();
                    }
                else
                    {
                    dragging = false;
                    }

                clearRubberBand();
                repaint();

                gridui.getBackupSelected().clear();
                 
                getNotesUI().updateChildInspector(true);
                }
            });



        addMouseMotionListener(new MouseMotionAdapter()
            {
            public void mouseDragged(MouseEvent e)
                {
                dragging = true;
                                
                // this will be costly
                int time = getTime(e);
                
                int lowTime = Math.min(originalTime, time);
                int highTime = Math.max(originalTime, time);
                
                gridui.clearSelected();

                for(EventUI eventui : gridui.getBackupSelected())
                    {
                    gridui.addEventToSelected(eventui, eventsui.getParameterUIs().indexOf(ParameterUI.this));
                    }

                ReentrantLock lock = getSeq().getLock();
                for(EventUI eventui : getEventUIs())
                    {
                    lock.lock();
                    try 
                        {
                        Notes.Event event = eventui.getEvent();
                        if (event.when >= lowTime && event.when <= highTime)
                            {
                            if (gridui.getBackupSelected().contains(eventui))
                                {
                                gridui.removeFromSelected(eventui);
                                }
                            else
                                {
                                gridui.addEventToSelected(eventui, eventsui.getParameterUIs().indexOf(ParameterUI.this));
                                }
                            }
                        }
                    finally
                        {
                        lock.unlock();
                        }
                    }

                int minX = Math.min(mouseDownEvent.getX(), e.getX());
                int maxX = Math.max(mouseDownEvent.getX(), e.getX());

                setRubberBand(minX, maxX);
                repaint();
                }
            });

        }

    /** Rebuilds the ParameterUI entirely.  This discards all the EventUIs and creates new ones from the given events. */
    public void rebuild(ArrayList<Notes.Event> events)
        {
        this.eventuis.clear();
        removeAll();
        if (events != null)
            {
            for(Notes.Event event : events)
                {
                EventUI eventui = new EventUI(this, event);
                addEventUI(eventui);
                }
            }
        }
        
    /** Rebuilds the ParameterUI entirely.  This discards all the EventUIs and creates new ones. */
    public void rebuild()
        {
        rebuild(getNotesUI().getNotes().getEventsOfType(type));
        }

    /** Returns the EventUIs. Note that these may not be in any order. */
    public ArrayList<EventUI> getEventUIs() { return eventuis; }
    
    /** Removes the EventUI from the ParameterUI */
    public void removeEventUI(EventUI eventui)
        {
        remove(eventui);
        eventuis.remove(eventui);
        }
                
    /** Adds the EventUI to the ParameterUI */
    public void addEventUI(EventUI eventui)
        {
        add(eventui);
        eventuis.add(eventui);
        }
                
    /** Finds and returns the EventUI for the given Event.  This is O(n) */
    public EventUI getEventUIFor(Notes.Event event)
        {
        for(EventUI eventui : eventuis)
            {
            if (eventui.getEvent() == event)
                {
                return eventui;
                }
            }
        return null;
        } 

    /** Recaches event information in all the underlying EventUIs */
    public void reload()
        {
        for(EventUI eventui : eventuis)
            {
            eventui.reload();
            }
        }
    
    /** Recaches note information in all the underlying NoteUIs */
    public void reload(HashSet<Notes.Event> events)
        {
        for(EventUI eventui : eventuis)
            {
            if (events.contains(eventui.event))
                {
                eventui.reload();
                } 
            }
        }

    public Dimension getPreferredSize()
        {
        return getMinimumSize();
        }

    public Dimension getMinimumSize()
        {
        // The width is the largest current time, in pixels
        return new Dimension(getPixels(eventsui.getMaximumTime()), eventsui.getParameterHeight());
        }
        
        
    // Vertical lines
    static final Line2D.Double vertical = new Line2D.Double(0, 0, 0, 0);
    // Ruber band lines
    static final Line2D.Double rubberBand = new Line2D.Double(0, 0, 0, 0);
        
    // Draws vertical bars for a given divisor.  The divisors at present are:
    // 192 / 4                  Quarter events
    // 192                              Beats
    // 192 * numBeats   Bars
    // You indicate what portion of the drawing (in time) should be done, plus the
    // divisor and the color for the bar, plus the current scale and graphics.
    //
    // This method is called multiple times in  onent to paint different divisors.
    // Note that the order matters: 16th events first if any, then beats if any, then bars if any,
    // because they will overwrite each other.
    void drawVerticalBars(int startWhen, int endWhen, int divisor, Color color, double scale, Graphics2D g)
        {
        if (startWhen % divisor != 0)
            {
            startWhen = (startWhen / divisor) * divisor;
            }
        if (startWhen == 0) startWhen = divisor;
                        
        g.setPaint(color);
        for(int i = startWhen; i < endWhen; i += divisor)
            {
            double _i = i / scale;
            vertical.setLine(_i, 0, _i, eventsui.getParameterHeight());
            g.draw(vertical);
            }
        }
        
    public void paintComponent(Graphics _g)
        {
        Graphics2D g = (Graphics2D) _g;
        Rectangle bounds = g.getClipBounds();
        int start = bounds.x;
        int end = bounds.x + bounds.width;
        
        g.setPaint(BACKGROUND_COLOR);
        g.fill(bounds);
        
        int startWhen = getTime(start);
        int endWhen = getTime(end);
        Notes notes = eventsui.getNotesUI().getNotes();
        
        // What is the minimum size?
        // Zooming in (more micro) makes the scale go DOWN
        // FIXME this is costly.  Should we push the bars onto the ParameterUIs?
        double scale = eventsui.getScale();
        int beatsPerBar = 0;
        int endTime = 0;
        Seq seq = getSeq();
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            beatsPerBar = seq.getBar();
            endTime = notes.getEnd();
            }
        finally
            {
            lock.unlock();
            }
                        
        // We draw:
        // 16th Events           (1/4 of PPQ, 48 ticks)  // 4.0 scale or below
        // Quarter Events        (1 PPQ, 192 ticks)              // 16.0 scale or below?
        // Bars                         (1 * beatsPerPar PPQ, 192 * beatsPerPar ticks)  // 8.0 * beatsPerBar or below?
        
        int divisor = Seq.PPQ * beatsPerBar;    // bars
        if (scale < 16.0) divisor = Seq.PPQ;    // beats
        if (scale < 4.0) divisor = Seq.PPQ / 4;         // 16th events
        
        if (scale < 4.0)                // draw 16th events
            {
            drawVerticalBars(startWhen, endWhen, Seq.PPQ / 4, PitchUI.COLOR_16TH_NOTE, scale, g);
            }
                
        if (scale < 16.0)               // draw beats
            {
            drawVerticalBars(startWhen, endWhen, Seq.PPQ, PitchUI.COLOR_BEAT, scale, g);
            }

        // draw bars
        drawVerticalBars(startWhen, endWhen, Seq.PPQ * beatsPerBar, PitchUI.COLOR_BAR, scale, g);


        // draw end
        int endX = gridui.getPixels(endTime);
        if (endX > 0)
            {
            g.setColor(PitchUI.END_COLOR);
            vertical.setLine(endX, 0, endX, eventsui.getParameterHeight());
            g.draw(vertical);
            }
        }



    // We override paint so we can draw the ParameterUI, which in turn draws all the 
    // EventUIs, but then we can draw on top of the EventUIs to do the rubber band
    public void paint(Graphics _g)
        {
        super.paint(_g);

        Graphics2D g = (Graphics2D) _g;
        Rectangle bounds = getBounds();
            
        g.setColor(BORDER_COLOR);
        g.draw(new Line2D.Double(bounds.getX(), bounds.getHeight(), bounds.getX() + bounds.getWidth(), bounds.getHeight()));

        /// Draw Rubber Band
        /// FIXME: if we  broke this out to a separate drawing method we might be able to redraw only the events that mattered
                
        if (rubberBandStartX != -1)
            {
            g.setPaint(PitchUI.RUBBER_BAND_COLOR);
            // FIXME   Make a custom stroke?  I dunno
            g.setStroke(PitchUI.RUBBER_BAND_STROKE);
            rubberBand.setLine(rubberBandStartX, 0, rubberBandStartX, bounds.width);   
            g.draw(rubberBand);
            rubberBand.setLine(rubberBandEndX, 0, rubberBandEndX, bounds.width);   
            g.draw(rubberBand);
            }
        } 
        

    /** Returns value corresponding to the given event. */
    public double getValue(MouseEvent evt)
        {
        return 1.0 - (evt.getY() / (double) eventsui.getParameterHeight());
        }

    /** Returns value corresponding to the given pixel */
    public double getValue(int y)
        {
        return 1.0 - (y / (double) eventsui.getParameterHeight());
        }

    /** Returns number of pixels correponding to the given time. */
    public int getPixels(int time)
        {
        return (int)(time / gridui.scale);
        }
                
    /** Returns the time corresponding to the given event. */
    public int getTime(MouseEvent evt)
        {
        return getTime((int) (evt.getX()));
        }
                
    /** Returns the time corresponding to the given pixel. */
    public int getTime(int x)
        {
        return (int)(x * gridui.scale);
        }

    /** Returns the quantized time of the given event given the current snap */
    public int getQuantizedTime(MouseEvent evt)
        {
        return getQuantizedTime((int)(evt.getX() * gridui.scale));
        }

    /** Returns the quantized time given the current snap */
    public int getQuantizedTime(int time)
        {
        return (time / gridui.snap) * gridui.snap;
        }

    /** Returns the difference, in value, between the origin and the event. */
    public double getValueDiff(MouseEvent origin, MouseEvent evt)
        {
        return getValue(evt) - getValue(origin); //  - getValue(evt);           // note swapped
        }

    /** Returns the difference, in time, between the origin and the event. */
    public int getTimeDiff(MouseEvent origin, MouseEvent evt)
        {
        // FIXME -- is this right or should we floor rather than truncate?
        return (int)((evt.getX() - origin.getX()) * gridui.scale);
        }

    /** Returns the difference, in time, between the origin and the event, considering snap quantization. */
    public int getQuantizedTimeDiff(MouseEvent origin, MouseEvent evt)
        {
        // FIXME -- is this right or should we floor rather than truncate?
        return (((int)((evt.getX() - origin.getX()) * gridui.scale)) / gridui.snap) * gridui.snap;
        }
        
    
    // The text label of the header
    JLabel headerLabel;
    // The panel which determines the header's dimensions
    JPanel headerPanel;
    
    /** Revises the header to reflect the current ParameterUI */
    public void updateHeader()
        {
        if (headerLabel == null)
            {
            headerLabel = new JLabel();
            headerLabel.setOpaque(true);
            headerLabel.setBackground(BACKGROUND_COLOR);
            headerLabel.setBorder(BorderFactory.createMatteBorder(0, 2, 0, 1, BACKGROUND_COLOR));
            }
        // Using HTML prevents it from doing "..."
        headerLabel.setText(type < 0 ? "" : "<html>" + Notes.getTypeInitialShort(type) + "<br><font size='2'>" +
            Notes.getTypeFinal(type) + "</font></html>");
        if (headerPanel != null)
            {
            Dimension d = headerPanel.getPreferredSize();
            headerPanel.setPreferredSize(new Dimension(d.width, eventsui.getParameterHeight()));
            headerPanel.revalidate();
            headerPanel.repaint();
            }
        }
    
    /** Builds and returns a new header. */
    public JComponent getHeader()
        {
        updateHeader();
        headerPanel = new JPanel()
            {
            public void paint(Graphics _g)
                {
                super.paint(_g);
                Graphics2D g = (Graphics2D) _g;
                Rectangle bounds = getBounds();
                g.setColor(BORDER_COLOR);
                g.draw(new Line2D.Double(bounds.width, 0, bounds.width, bounds.height));
                g.draw(new Line2D.Double(0, bounds.height, bounds.width, bounds.height));
                }
            };
        headerPanel.setBackground(BACKGROUND_COLOR);
        headerPanel.setLayout(new BorderLayout());
        headerPanel.add(headerLabel, BorderLayout.NORTH);
        Dimension d = headerPanel.getPreferredSize();
        headerPanel.setPreferredSize(new Dimension(d.width, eventsui.getParameterHeight()));
        return headerPanel;
        }     
    }
