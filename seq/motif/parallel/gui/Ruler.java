/* 
   Copyright 2025 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.parallel.gui;

import seq.motif.parallel.*;
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
    // The color of the play marker on the ruler
    public static final Color BAR_COLOR = Color.BLUE;
    // The range color
    public static final BasicStroke PLAY_STROKE = new BasicStroke(3.0f);

    // The owner ParallelUI
    ParallelUI parallelui;
    // The Seq
    Seq seq;

    public Ruler(Seq seq, ParallelUI parallelui)
        {
        this.seq = seq;
        this.parallelui= parallelui;
        }
        
    public Dimension getPreferredSize()
        {
        return getMinimumSize();
        }

    public Dimension getMinimumSize()
        {
        // The width is the largest current time, in pixels
        Box buttonBox = parallelui.getButtonBox();
        return new Dimension((int)buttonBox.getPreferredSize().getWidth(), RULER_HEIGHT);
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
    void drawVerticalBars(int startWhen, int endWhen, int divisor, Color color, Graphics2D g, int beatsPerBar)
        {
        int starty = 4;
        if (startWhen % divisor != 0)
            {
            startWhen = (startWhen / divisor) * divisor;
            }
                        
        for(int i = startWhen; i < endWhen; i += divisor)
            {
            double _i = ParallelButton.timeToPixels(i);
            if (i != 0) 
                {
                if ((i / (divisor * 4)) * (divisor * 4) == i)
                    {
                    starty = 4;
                    g.setPaint(color);
                    }
                else
                    {
                    starty = 12;
                    g.setPaint(Color.BLACK);                        
                    }
                vertical.setLine(_i, starty, _i, RULER_HEIGHT);
                g.draw(vertical);
                }
                                
            if (starty == 4)                // Bar Text
                {
                if ((i / (divisor * 8)) * (divisor * 8) == i)   // every 4 measures if zoomed out
                    {
                    g.setPaint(Color.BLACK);
                    int bar = (i / (Seq.PPQ * beatsPerBar)) + 1;
                    g.drawString("" + bar, (float)_i + 1, RULER_HEIGHT - 6);
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
        
        int beatsPerBar = 0;
        Seq seq = parallelui.getSeqUI().getSeq();
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
                
        // draw bars
        drawVerticalBars(ParallelButton.pixelsToTime(start), ParallelButton.pixelsToTime(end), Seq.PPQ * beatsPerBar, BAR_COLOR, g, beatsPerBar);

        // draw play
        // FIXME: Merge this lock with the one above?
        ParallelClip clip = (ParallelClip)(parallelui.getDisplayClip());
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
            double gpos = ParallelButton.pixelsToTime(pos);
            g.draw(new Line2D.Double(gpos, 0, gpos, getBounds().height));
            }

        }
    }
