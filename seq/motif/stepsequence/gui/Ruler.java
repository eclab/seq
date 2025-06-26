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
    Dimension minSize = new Dimension(0, RULER_HEIGHT);
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
        
        StepSequenceClip clip = (StepSequenceClip)(ssui.getDisplayClip());
        if (clip != null)
            {
            Graphics2D g = (Graphics2D) _g;
            g.setPaint(Color.BLACK);
            g.setStroke(new BasicStroke(3.0f));
            double pos = width * clip.getCurrentPos(0) - x;         // FIXME this is wrong
            g.draw(new Line2D.Double(pos, 0, pos, getBounds().height));
            }
        }

	/*** Tooltips ***/
	
	static final String TOOLTIP = "<html><b>Ruler</b><br>" +
		"Indicates the current time in the step sequence.</html>";
    }
