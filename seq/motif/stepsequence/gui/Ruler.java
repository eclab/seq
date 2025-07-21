/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.stepsequence.gui;

import seq.motif.stepsequence.*;
import seq.engine.*;
import seq.gui.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.locks.*;
import java.util.*;

public class Ruler extends JPanel
    {
    public static final int RULER_HEIGHT = 16;
    public static final Color PLAY_COLOR = Color.BLACK;
    public static final BasicStroke PLAY_STROKE = new BasicStroke(3.0f);
    public static final Dimension minSize = new Dimension(0, RULER_HEIGHT);
    Seq seq;
    StepSequenceUI ssui;
    StepSequence ss;

    public Ruler(Seq seq, StepSequence ss, StepSequenceUI ssui)
        {
        this.seq = seq;
        this.ss = ss;
        this.ssui = ssui;
        setToolTipText(TOOLTIP);
        }

    public Dimension getMinimumSize()
        {
        return minSize;
        }
                                
    public void paint(Graphics _g)
        {
        super.paint(_g);
        int width = ssui.getTrackWidth();
        int x = ssui.getTrackX();
        double pos = 0;
        
        StepSequenceClip clip = (StepSequenceClip)(ssui.getDisplayClip());
        if (clip != null)
            {
            ReentrantLock lock = seq.getLock();
            lock.lock();
            try
                {
                pos = clip.getCurrentPos(0);
                }
            finally
                {
                lock.unlock();
                }

            Graphics2D g = (Graphics2D) _g;
            g.setPaint(PLAY_COLOR);
            g.setStroke(PLAY_STROKE);
            double gpos = width * pos - x;
            g.draw(new Line2D.Double(gpos, 0, gpos, getBounds().height));
            }
        }

    /*** Tooltips ***/
        
    static final String TOOLTIP = "<html><b>Ruler</b><br>" +
        "Indicates the current time in the step sequence.</html>";
    }
