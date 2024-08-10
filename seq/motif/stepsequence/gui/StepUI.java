/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.stepsequence.gui;

import seq.motif.stepsequence.*;
import seq.engine.*;
import seq.gui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.locks.*;
import java.awt.geom.*;

public class StepUI extends JPanel
    {
    public static final Stroke beatStroke = new BasicStroke(3.0f);
    public static final Stroke offStroke = new BasicStroke(1.0f);
    public static final Color strokeColor = new Color(64, 64, 64); 
    public static final Color stepColor = Color.BLUE;       // for the moment
    public static final Color offColor = Color.GRAY;        // for the moment
    Seq seq;
    StepSequenceUI ssui;
    TrackUI track;
    int trackNum;
    StepSequence ss;
    int stepNum;
    boolean mouseOver;
    int width;
        
    public void setTrackNum(int num) 
        { 
        trackNum = num; 
        }
                
    public int getTrackNum() { return trackNum; }
        
    public int getWidth() { return width; }
    public void setWidth(int val) { width = val; } 
    
    static int counter = 0;
    
    // local repaint dirtiness only
    boolean dirty;
    public void repaint() { dirty = true; super.repaint(); }
        
    public StepUI(Seq seq, StepSequence ss, StepSequenceUI ssui, TrackUI track, int trackNum, int stepNum)
        {
        this.seq = seq;
        this.ss = ss;
        this.ssui = ssui;
        this.track = track;
        this.trackNum = trackNum;
        this.stepNum = stepNum;
        this.setFocusable(true);
        this.width = ssui.getZoom();    // for the moment
                
        addMouseListener(new MouseAdapter()
            {
            public void mouseEntered(MouseEvent e)
                {
                mouseOver = true;
                repaint();
                }
            public void mouseExited(MouseEvent e)
                {
                mouseOver = false;
                repaint();
                }
                        
            public void mouseReleased(MouseEvent e)
                {
                ssui.mouseDown = false;
                }
                                
            public void mousePressed(MouseEvent e)
                {
                ssui.mouseDown = true;
                int oldStepNum = ssui.getSelectedStepNum();
                int oldTrackNum = ssui.getSelectedTrackNum();
                if ((e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK || 
                    (e.getModifiers() & InputEvent.SHIFT_MASK) == InputEvent.SHIFT_MASK)
                    {
                    // Don't toggle
                    }
                else
                    {
                    toggleOn();
                    repaint();
                    }
                if (oldStepNum != StepUI.this.stepNum || oldTrackNum != StepUI.this.trackNum)
                    {
                    ssui.setSelectedTrackNum(StepUI.this.trackNum);
                    ssui.setSelectedStepNum(StepUI.this.stepNum);
                    }
                }
            });
        }
        
    public boolean isOn()
        {
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try { return ss.isOn(trackNum, stepNum); }
        finally { lock.unlock(); }
        }

    public void setOn(boolean val)
        {
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try { ss.setOn(trackNum, stepNum, val); }
        finally { lock.unlock(); }
        }
                
    public void toggleOn()
        {
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try { ss.setOn(trackNum, stepNum, !ss.isOn(trackNum, stepNum)); }
        finally { lock.unlock(); }
        }
                
    StepInspector getInspector() { return new StepInspector(seq, ss, ssui, trackNum, stepNum); }
        
    public Dimension getMinimumSize() { return new Dimension(width, 32); }
    public Dimension getPreferredSize() { return new Dimension(width, 32); }
    public Dimension getMaximumSize() { return new Dimension(3000, 3000); }
    protected void paintComponent(Graphics _g)
        {
        // First, do I paint myself?
        // If I'm not updating then I definitely need to repaint, it's probably a scroll
        /*
          boolean needToRepaint =
          (!ssui.isUpdating() ||
          // Next, if I'm locally dirty, definitely need to repaint
          dirty ||
          // Next, if I'm in the dirty list, need to repaint
          ssui.isDirty(this));
        */
        boolean needToRepaint = true;           // for the moment, sigh.  Repainting dirty is not working right now...
                
        if (!needToRepaint) return;
        dirty = false;
        
        /// WARNING: NO LOCK.  This is because StepSequenceUI.Paint() locks for us

        //System.err.println("Painting " + trackNum + " " + stepNum + " " + !ssui.isUpdating() + " " + dirty + " " + ssui.isDirty(this));
                
        super.paintComponent(_g);

        // Get data
        boolean on = false;
        int finalVelocity = 0;
        boolean iAmCurrentStep = false;
                
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            { 
            if (trackNum >= ss.getNumTracks() || stepNum >= ss.getNumSteps(trackNum))
                {
                // uh oh
                return;
                }
            on = ss.isOn(trackNum, stepNum);
            finalVelocity = ss.getFinalVelocity(trackNum, stepNum);
            StepSequenceClip clip = (StepSequenceClip)(ssui.getDisplayClip());              // already locks :-(
            iAmCurrentStep = (clip != null && clip.getCurrentStep(trackNum) == stepNum);
            }
        finally { lock.unlock(); }

        Graphics2D g = (Graphics2D) _g;

        g.setPaint(on ? ssui.getStepVelocityMap().getColor(finalVelocity) : offColor);
        RoundRectangle2D.Double rect = new RoundRectangle2D.Double(2, 2, getBounds().width - 4, getBounds().height - 4, 8, 8);
        g.fill(rect);
        boolean selected = ssui.getSelectedTrackNum() == trackNum && ssui.getSelectedStepNum() == stepNum;
        g.setPaint(selected ? Color.BLUE : (mouseOver ? Color.GREEN : strokeColor));
        g.setStroke(selected || stepNum % 4 == 0 ? beatStroke : offStroke);
        g.draw(rect);
                
        if (iAmCurrentStep)
            {
            Ellipse2D.Double ellipse = new Ellipse2D.Double(getBounds().width / 2 - 4, getBounds().height / 2 - 4, 8, 8);
            g.setPaint(stepColor);
            g.fill(ellipse);
            }
        }
    }
