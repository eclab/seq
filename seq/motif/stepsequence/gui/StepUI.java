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

public class StepUI extends JComponent
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
    
    boolean dirty;
    public boolean isDirty() { return dirty; }
    public void setDirty(boolean val) { dirty = val; repaint(); }
    
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
                setDirty(true);
                }
            public void mouseExited(MouseEvent e)
                {
                mouseOver = false;
                setDirty(true);
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
                    setDirty(true);
                    }
                if (oldStepNum != StepUI.this.stepNum || oldTrackNum != StepUI.this.trackNum)
                    {
                    ssui.setSelectedTrackNum(StepUI.this.trackNum);
                    ssui.setSelectedStepNum(StepUI.this.stepNum);
                    }
                }
            });
        setToolTipText(TOOLTIP);
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
        // NOTE FROM JUNE 10 2025: Dirtying code works, but Java is erasing the entire background of the JScrollPane
        // and filling it with the background of the enclosing pane, regardless of setting of opacity or double buffering.
        // I've spent a long time working on trying to prevent this, but it doesn't work.  So we have to just cut out
        // setting dirty.
        //
        // I *did* manage to get the code to repaint much faster by turning off antialising.  To make things look better,
        // I changed the step boxes to rectangles rather than round rects.  Also the fine-grained per-step locking doesn't
        // seem to have a big impact on total speed, so it's retained for now.
        
        //if (!isDirty() && !ssui.isAllDirty()) return;
        //setDirty(false);
        
        // Not needed, looks like, since the background is getting erased anyway :-(
        //super.paintComponent(_g);

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
        
        boolean selected = ssui.getSelectedTrackNum() == trackNum && ssui.getSelectedStepNum() == stepNum;
        boolean beat = (selected || stepNum % 4 == 0);

        g.setPaint(on ? ssui.getStepVelocityMap().getColor(finalVelocity) : offColor);
        
        int inset = (beat ? 1 : 0);
        Rectangle2D.Double rect = new Rectangle2D.Double(2 + inset, 2 + inset, getBounds().width - 4 - (inset * 2), getBounds().height - 4 - (inset * 2));
        //RoundRectangle2D.Double rect = new RoundRectangle2D.Double(2, 2, getBounds().width - 4, getBounds().height - 4, 8, 8);
        g.fill(rect);
        g.setPaint(selected ? Color.BLUE : (mouseOver ? Color.GREEN : strokeColor));
        g.setStroke(beat ? beatStroke : offStroke);
        g.draw(rect);
                
        if (iAmCurrentStep)
            {
            Ellipse2D.Double ellipse = new Ellipse2D.Double(getBounds().width / 2 - 4, getBounds().height / 2 - 4, 8, 8);
            g.setPaint(stepColor);
            g.fill(ellipse);
            }
        }


    static final String TOOLTIP = "<html><b>Step</b><br>" +
        "Click on the step to enable or disable it and also select it.  Shift-Click or Right-Click<br>"+
        "the step to just select it.  When selected, the step inspector is shown at right,<br>" +
        "along with the step's track inspector.<br><br>" +
        "The step color indicates its velocity.  Set the velocity in the step inspector, or set<br>" +
        "the track's default velocity in the track inspector, or set the step sequence's default<br>" +
        "velocity in the step sequencer inspector, all at right.";
        
    }
