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
        }

    public Dimension getMinimumSize()
        {
        return minSize;
        }
                                
    public void paint(Graphics _g)
        {
        super.paint(_g);
        StepSequenceClip clip = (StepSequenceClip)(ssui.getDisplayClip());
        if (clip != null)
            {
            Graphics2D g = (Graphics2D) _g;
            g.setPaint(Color.BLACK);
            g.setStroke(new BasicStroke(3.0f));
            double pos = getBounds().width * clip.getCurrentPos(0);         // FIXME this is wrong
            g.draw(new Line2D.Double(pos, 0, pos, getBounds().height));
            }
        }
    }
