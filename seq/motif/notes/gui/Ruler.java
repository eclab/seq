/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.notes.gui;

import seq.motif.notes.*;
import seq.engine.*;
import seq.gui.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.locks.*;
import java.util.*;

public class Ruler extends JComponent
    {
    // The ruler height
    public static final int RULER_HEIGHT = 16;
    // The color of the lower border of the ruler
    public static final Color BORDER_COLOR = Color.BLACK;
    // The ruler's background color
    public static final Color BACKGROUND_COLOR = Color.WHITE;
    // The color of the play marker on the ruler
    public static final Color PLAY_COLOR = Color.BLACK;
    // The range color
    public static final Color RANGE_COLOR = new Color(200, 200, 230);
    // The stroke of the play marker on the ruler
    public static final BasicStroke PLAY_STROKE = new BasicStroke(3.0f);

    // The owner NotesUI
    NotesUI notesui;
    // The Seq
    Seq seq;

    // Handles mouse global released messages (because they don't always go to the right place)
    AWTEventListener releaseListener = null;
    // True if we're expecting a release, so the releaseListener's messages are valid 
    boolean mouseDown = false;
    // Where the mouse was originally pressed
    MouseEvent mouseDownEvent = null;
    boolean dragged = false;
    boolean shifted = false;
        
    // The original time of the selected event at time of mouse press, for doing diff computations
    int originalTime = 0;
    int finalTime = 0;
    
    /** Returns the GridUi */    
    public GridUI getGridUI() { return notesui.getGridUI(); }
 
    public Ruler(Seq seq, NotesUI notesui)
        {
        this.seq = seq;
        this.notesui = notesui;
        setToolTipText(TOOLTIP);
        
        addMouseListener(new MouseAdapter()
            {   
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
                
                shifted = ((e.getModifiers() & InputEvent.SHIFT_MASK) == InputEvent.SHIFT_MASK);
                if (shifted && (originalTime != finalTime))
                    {
                    int newTime = getGridUI().getTime(e);
                    // We want to manipulate the FINAL TIME
                    if (Math.abs(originalTime - newTime) < Math.abs(finalTime - newTime))
                        {
                        // swap original and final time
                        int swap = originalTime;
                        originalTime = finalTime;
                        finalTime = swap;
                        }
                    finalTime = newTime;
                    }
                else
                    {
                    originalTime = getGridUI().getQuantizedTime(e);
                    finalTime = originalTime;
                    }
                
                dragged = false;
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
                
                if (dragged || shifted)
                    {
                    finalTime = getGridUI().getQuantizedTime(e);
                    }
                else
                    {
                    finalTime = originalTime;
                    }
                dragged = false;
                shifted = false;
                repaint();
                }
            });
       
        addMouseMotionListener(new MouseMotionAdapter()
            {
            public void mouseDragged(MouseEvent e)
                {
                dragged = true;

                getGridUI().clearSelected();
                finalTime = getGridUI().getQuantizedTime(e);
                repaint();
                }
            });
   
        }
        
    /** Returns whether the range has been set */
    public boolean getHasRange()
        {
        return (finalTime != originalTime);
        }
    
    /** Returns the low value of the range */
    public int getRangeLow()
        {
        return Math.min(finalTime, originalTime);
        }
        
    /** Returns the high value of the range */
    public int getRangeHigh()
        {
        return Math.max(finalTime, originalTime);
        }
        
    /** Unsets the range */
    public void clearRange()
        {
        boolean rangeSet = (finalTime != originalTime);
        finalTime = originalTime = 0;
        if (rangeSet)
            {
            repaint();
            }
        }

    public Dimension getPreferredSize()
        {
        return getMinimumSize();
        }

    public Dimension getMinimumSize()
        {
        // The width is the largest current time, in pixels
        GridUI gridui = getGridUI();
        return new Dimension(gridui.getPixels(gridui.getMaximumTime()), RULER_HEIGHT);
        }
        
    // Vertical lines
    static final Line2D.Double vertical = new Line2D.Double(0, 0, 0, 0);
    // The horizontal lower border line
    static final Line2D.Double horizontal = new Line2D.Double(0, 0, 0, 0);

    // Draws vertical bars for a given divisor.  The divisors at present are:
    // 192 / 4                  Quarter notes
    // 192                              Beats
    // 192 * numBeats   Bars
    // You indicate what portion of the drawing (in time) should be done, plus the
    // divisor and the color for the bar, plus the current scale and graphics.
    //
    // This method is called multiple times in paintComponent to paint different divisors.
    // Note that the order matters: 16th notes first if any, then beats if any, then bars if any,
    // because they will overwrite each other.
    // 
    // Additionally, the order matters because the bars have different heights
    void drawVerticalBars(int startWhen, int endWhen, int divisor, Color color, double scale, Graphics2D g, int beatsPerBar)
        {
        int starty = 4;
        if (divisor == Seq.PPQ / 4)             // 16th notes
            {
            starty = 12;
            }
        else if (divisor == Seq.PPQ)            // beats
            {
            starty = 8;
            }
                
        if (startWhen % divisor != 0)
            {
            startWhen = (startWhen / divisor) * divisor;
            }
                        
        g.setPaint(color);
        for(int i = startWhen; i < endWhen; i += divisor)
            {
            double _i = i / scale;
            if (i != 0) 
                {
                vertical.setLine(_i, starty, _i, RULER_HEIGHT);
                g.draw(vertical);
                }
                                
            if (starty == 4)                // Bar Text
                {
                if (scale < 32.0 || (i / (divisor * 4)) * (divisor * 4) == i)   // every 4 measures if zoomed out
                    {
                    g.setPaint(Color.BLACK);
                    int bar = (i / (Seq.PPQ * beatsPerBar)) + 1;
                    g.drawString("" + bar, (float)_i + 1, RULER_HEIGHT - 2);
                    g.setPaint(color);
                    }
                }
            }
                
        Rectangle rect = g.getClipBounds();
        g.setPaint(BORDER_COLOR);
        horizontal.setLine(rect.x, RULER_HEIGHT, rect.x + rect.width, RULER_HEIGHT);
        g.draw(horizontal);
        }
    
    public void paintComponent(Graphics _g)
        {
        Graphics2D g = (Graphics2D) _g;
        
        Rectangle bounds = g.getClipBounds();
        int start = bounds.x;
        int end = bounds.x + bounds.width;

        g.setPaint(BACKGROUND_COLOR);
        g.fill(bounds);
        
        if (originalTime != finalTime)
            {
            int _originalTime = originalTime;
            int _finalTime = finalTime;
                
            if (_originalTime > _finalTime) // need to swap to draw
                {
                int swap = _originalTime;
                _originalTime = _finalTime;
                _finalTime = swap;
                }
            int o = getGridUI().getPixels(_originalTime);
            int f =  getGridUI().getPixels(_finalTime);
                
            Rectangle range = new Rectangle(o, 0, f - o, RULER_HEIGHT);
            g.setColor(RANGE_COLOR);
            g.fill(range);
            }
        
        
        GridUI gridui = getGridUI();
                
        int startWhen = gridui.getTime(start);
        int endWhen = gridui.getTime(end);
        
        // What is the minimum size?
        // Zooming in (more micro) makes the scale go DOWN
        // FIXME this is costly.  Should we push the bars onto the PitchUIs?
        double scale = gridui.getScale();
        int beatsPerBar = 0;
        Seq seq = notesui.getSeqUI().getSeq();
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            beatsPerBar = seq.getBar();
            }
        finally
            {
            lock.unlock();
            }
                
        // We draw:
        // 16th Notes           (1/4 of PPQ, 48 ticks)  // 4.0 scale or below
        // Quarter Notes        (1 PPQ, 192 ticks)              // 16.0 scale or below?
        // Bars                         (1 * beatsPerPar PPQ, 192 * beatsPerPar ticks)  // 8.0 * beatsPerBar or below?
        
        if (scale < 4.0)                // draw 16th notes
            {
            drawVerticalBars(startWhen, endWhen, Seq.PPQ / 4, PitchUI.COLOR_16TH_NOTE, scale, g, beatsPerBar);
            }
        
        if (scale < 16.0)               // draw beats
            {
            drawVerticalBars(startWhen, endWhen, Seq.PPQ, PitchUI.COLOR_BEAT, scale, g, beatsPerBar);
            }

        // draw bars
        drawVerticalBars(startWhen, endWhen, Seq.PPQ * beatsPerBar, PitchUI.COLOR_BAR, scale, g, beatsPerBar);

        // draw play
        // FIXME: Merge this lock with the one above?
        NotesClip clip = (NotesClip)(notesui.getDisplayClip());
        if (clip != null)
            {
            int pos = 0;
            lock.lock();
            try
                {
                pos = clip.getPosition();
                }
            finally
                {
                lock.unlock();
                }

            g.setPaint(PLAY_COLOR);
            g.setStroke(PLAY_STROKE);
            double gpos = gridui.getPixels(pos);
            g.draw(new Line2D.Double(gpos, 0, gpos, getBounds().height));
            }

        }
    
    /** Builds and returns the header. */
    public static JComponent getHeader()
        {
        JComponent rulerspace = new JComponent()        
            {
            public void paintComponent(Graphics _g)
                {
                Graphics2D g = (Graphics2D) _g;
                Rectangle bounds = getBounds();
                g.setColor(BACKGROUND_COLOR);
                g.fill(bounds);
                g.setColor(BORDER_COLOR);
                g.draw(new Line2D.Double(0, bounds.height, bounds.width, bounds.height));
                }
            };
        rulerspace.setPreferredSize(new Dimension(1, Ruler.RULER_HEIGHT));
        return rulerspace;
        }

    /*** Tooltips ***/
        
    static final String TOOLTIP = "<html><b>Ruler</b><br>" +
        "Indicates the current time in the Notes timeline.</html>";
    }
