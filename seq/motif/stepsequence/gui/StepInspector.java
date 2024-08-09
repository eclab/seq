package seq.motif.stepsequence.gui;

import seq.motif.stepsequence.*;
import seq.engine.*;
import seq.gui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.locks.*;

public class StepInspector extends WidgetList
    {
    static final int NO_DEFAULT = Dial.NO_DEFAULT;
    static final int DEFAULT = Dial.DEFAULT;
    
    StepSequenceUI ssui;
    Seq seq;
    StepSequence ss;
    int trackNum;
    int stepNum;
        
    SmallDial stepNote;
    SmallDial stepVelocity;
    JComboBox stepFlam;
    JComboBox stepWhen;


    static final String[] WHEN_STRINGS =
        { "<html><i>Default</i></html>", "Always", "0.1 Probability", "0.2 Probability", "0.3 Probability", "0.4 Probability", "0.5 Probability", "0.6 Probability", "0.7 Probability", "0.8 Probability", "0.9 Probability",
          "1/Tracks Probability", "1 - 1/Tracks Prob", 
          "X O", "O X", 
          "X O O", "O X O", "O O X", "X X O", "X O X", "O X X",
          "X O O O", "O X O O", "O O X O", "O O O X",
          "X X O O", "X O X O", "X O O X", "O X X O", "O X O X", "O O X X",
          "X X X O", "X O X X", "X X O X", "O X X X" };
                
    static final String[] FLAM_STRINGS = { "<html><i>Default</i></html>", "None", "2", "3", "4", "6", "8", "12", "16", "24", "48" };
    static final String[] NOTES = { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B" };

    public int getTrackNum() { return trackNum; }
    public int getStepNum() { return stepNum; }
        

    public StepInspector(Seq seq, StepSequence ss, StepSequenceUI ssui, int trackNum, int stepNum)
        {
        this.seq = seq;
        this.ss = ss;
        this.ssui = ssui;
        this.trackNum = trackNum;
        this.stepNum = stepNum;
                
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            stepNote = new SmallDial(-1, 0)
                {
                protected String map(double val) 
                    { 
//                    if (getDefault()) return "<html><i>Default</i></html>";
                    int n = (int)(val * 127); 
                    return NOTES[n % 12] + ((n / 12) - 2); 
                    }
                public int getDefault() { int val = ss.getNote(trackNum, stepNum); return (val == -1 ? DEFAULT : NO_DEFAULT); }
                public void setDefault(int val) { if (val == DEFAULT) ss.setNote(trackNum, stepNum, -1); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return ss.getNote(trackNum, stepNum) / 127.0; }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { ss.setNote(trackNum, stepNum, (int)(val * 127)); }
                    finally { lock.unlock(); }
                    }
                };

            stepFlam = new JComboBox(FLAM_STRINGS);
            stepFlam.setSelectedIndex(ss.getFlam(trackNum, stepNum) + 1);
            stepFlam.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { ss.setFlam(trackNum, stepNum, stepFlam.getSelectedIndex() - 1); }                 // include Default
                    finally { lock.unlock(); }                              
                    }
                });

            stepWhen = new JComboBox(WHEN_STRINGS);
            stepWhen.setSelectedIndex(ss.getWhen(trackNum, stepNum) + 1);
            stepWhen.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { ss.setWhen(trackNum, stepNum, stepWhen.getSelectedIndex() - 1); }         // include Default
                    finally { lock.unlock(); }                              
                    }
                });

            stepVelocity = new SmallDial(-1, 0)
                {
                protected String map(double val) 
                    { 
                    //if (getDefault()) return "<html><i>Default</i></html>"; else 
                    return String.valueOf((int)(val * 127)); 
                    }
                public int getDefault() { int val = ss.getVelocity(trackNum, stepNum); return (val == -1 ? DEFAULT : NO_DEFAULT); }
                public void setDefault(int val) { if (val == DEFAULT) ss.setVelocity(trackNum, stepNum, -1); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return ss.getVelocity(trackNum, stepNum) / 127.0; }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { ss.setVelocity(trackNum, stepNum, (int)(val * 127)); }
                    finally { lock.unlock(); }
                    ssui.redraw();
                    }
                };
                        
            }
        finally { lock.unlock(); }

        build(new String[] { "Note", "   Velocity", "Flams", "When"}, 
            new JComponent[] 
                { 
                stepNote.getLabelledDial("<html><i>Default</i></html>"), 
                stepVelocity.getLabelledDial("<html><i>Default</i></html>"), 
                stepFlam, 
                stepWhen 
                });
        }

    public void revise()
        {
        Seq old = seq;
        seq = null;
        ReentrantLock lock = old.getLock();
        //System.err.println("TrackInspector Acquiring Lock");
        lock.lock();
        //System.err.println("TrackInspector Locking Lock");
        try 
            { 
            stepFlam.setSelectedIndex(ss.getFlam(trackNum, stepNum) + 1); 
            stepWhen.setSelectedIndex(ss.getWhen(trackNum, stepNum) + 1); 
            }
        finally { lock.unlock(); }                              
        //System.err.println("TrackInspector Releasing Lock");
        seq = old;
        stepNote.redraw();
        stepVelocity.redraw();
        }
    }
