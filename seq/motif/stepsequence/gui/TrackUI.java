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

public class TrackUI extends JPanel
    {
    Seq seq;
    StepSequenceUI ssui;
    StepSequence ss;
    int trackNum;

    static int trackNumCounter = 0; 

    TrackHeader trackHeader;
    Box trackBody;
    ArrayList<StepUI> steps = new ArrayList<>();
        
    public void setSelected(boolean val) { trackHeader.setSelected(val); }

    public void setTrackNum(int num) 
        { 
        trackNum = num; 
        trackHeader.revise();
        for(StepUI step : steps)
            {
            step.setTrackNum(num);
            }
        repaint();
        trackHeader.repaint();
        }
                
    public int getTrackNum() { return trackNum; }
        
    public TrackUI(Seq seq, StepSequence ss, StepSequenceUI ssui, int trackNum)
        {
        setOpaque(false);
        this.seq = seq;
        this.ss = ss;
        this.ssui = ssui;
        this.trackNum = trackNum;
        seq.getLock().lock();
        try
            {
            if (trackNum < ss.getNumTracks())
                {
                if (ss.getTrackName(trackNum) == null || ss.getTrackName(trackNum).equals(""))
                    {
                    String possibility = null;
                    while(true)
                        {
                        possibility = "Track " + (++trackNumCounter);
                        boolean failed = false;
                        for(int i = 0; i < ss.getNumTracks(); i++)
                            {
                            if (possibility.equals(ss.getTrackName(i))) { failed = true; break; }
                            }
                        if (!failed) break;
                        }
                    ss.setTrackName(trackNum, possibility);
                    }
                }
            }
        finally
            {
            seq.getLock().unlock();
            }
                
        trackHeader = new TrackHeader(seq, ss, ssui, this);
        trackBody = new Box(BoxLayout.X_AXIS)           // JPanel with GridLayout won't work, it has bugs
            {
            // Needs to be exactly the same height as the header    
            public Dimension getMinimumSize()
                {
                Dimension size = super.getMinimumSize();
                if (trackHeader != null) size.setSize(size.getWidth(), trackHeader.getMinimumSize().getHeight());
                return size;
                }
                                
            // Needs to be exactly the same height as the header    
            public Dimension getPreferredSize()
                {
                Dimension size = super.getPreferredSize();
                if (trackHeader != null) size.setSize(size.getWidth(), trackHeader.getPreferredSize().getHeight());
                return size;
                }
            };
        setLayout(new BorderLayout());
        add(trackBody, BorderLayout.CENTER);
                
        for(int i = 0; i < ss.getNumSteps(trackNum); i++)
            {
            StepUI step = new StepUI(seq, ss, ssui, this, trackNum, i);
            steps.add(step);
            trackBody.add(step);
            }
        }

    public void updateLength()
        {
        trackBody.removeAll();
        steps.clear();
        for(int i = 0; i < ss.getNumSteps(trackNum); i++)
            {
            StepUI step = new StepUI(seq, ss, ssui, this, trackNum, i);
            steps.add(step);
            trackBody.add(step);
            }
        this.revalidate();
        this.repaint();
        }

    public Box getTrackBody() { return trackBody; }
        
    public ArrayList<StepUI> getSteps() { return steps; }

    public StepUI getStep(int stepNum) { return steps.get(stepNum); }
        
    public TrackHeader getHeader() { return trackHeader; }
        
    TrackInspector getInspector() { return new TrackInspector(seq, ss, ssui, trackNum); }   
    }
