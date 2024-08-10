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

public class TrackInspector extends WidgetList
    {
    Seq seq;
    StepSequence ss;
    StepSequenceUI ssui;
    int trackNum;
        
    JCheckBox trackSolo;
    JCheckBox trackMute;
    JCheckBox trackArm;
    SmallDial trackGain;
    SmallDial trackNote;
    PushButton setTrackNote;
    SmallDial trackVelocity;
    PushButton setTrackVelocity;
    JComboBox trackFlam;
    PushButton setTrackFlam;
    JComboBox trackChoke;
    SmallDial trackSwing;
    SmallDial numSteps;
    double trackLen;
    PushButton setNumSteps;
    JComboBox trackWhen;
    PushButton setTrackWhen;
    JCheckBox trackExclusive;
    JComboBox trackOut;
    StringField trackName;

    static final String[] WHEN_STRINGS =
        { "Always", "0.1 Probability", "0.2 Probability", "0.3 Probability", "0.4 Probability", "0.5 Probability", "0.6 Probability", "0.7 Probability", "0.8 Probability", "0.9 Probability",
          "1/Tracks Prob", "1 - 1/Tracks Prob", 
          "X O", "O X", 
          "X O O", "O X O", "O O X", "X X O", "X O X", "O X X",
          "X O O O", "O X O O", "O O X O", "O O O X",
          "X X O O", "X O X O", "X O O X", "O X X O", "O X O X", "O O X X",
          "X X X O", "X O X X", "X X O X", "O X X X" };
                
    static final String[] FLAM_STRINGS = { "None", "2", "3", "4", "6", "8", "12", "16", "24", "48" };
    static final String[] NOTES = { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B" };

    public int getTrackNum() { return trackNum; }
        
    public TrackInspector(Seq seq, StepSequence ss, StepSequenceUI ssui, int trackNum)
        {
        this.seq = seq;
        this.ss = ss;
        this.trackNum = trackNum;
        this.ssui = ssui;

        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            trackExclusive = new JCheckBox();
            trackExclusive.setSelected(ss.isTrackExclusive(trackNum));
            trackExclusive.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    boolean result = trackExclusive.isSelected();
                    lock.lock();
                    try { ss.setTrackExclusive(trackNum, result); }
                    finally { lock.unlock(); }                              
                    }
                });

            trackNote = new SmallDial(-1)
                {
                protected String map(double val) 
                    { 
                    //if (getDefault()) return "<html><i>Default</i></html>";
                    int n = (int)(val * 127); 
                    return NOTES[n % 12] + ((n / 12) - 2); 
                    }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return ss.getTrackNote(trackNum) / 127.0; }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { ss.setTrackNote(trackNum, (int)(val * 127)); }
                    finally { lock.unlock(); }
                    }
                };

            setTrackNote = new PushButton("Set All")
                {
                public void perform()
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try 
                        {
                        int len = ss.getNumSteps(trackNum);
                        for(int i = 0; i < len; i++) { ss.setNote(trackNum, i, StepSequence.DEFAULT); }
                        }
                    finally { lock.unlock(); }
                    ssui.getStepInspector().revise();
                    }
                };

            trackLen = (ss.getNumSteps(trackNum) - 1) / 127.0;
            numSteps = new SmallDial(-1)
                {
                protected String map(double val) { return String.valueOf((int)(val * 127) + 1); }
                public double getValue() { return trackLen; }
                public void setValue(double val) { trackLen = val; }
                };

            setNumSteps = new PushButton("Set")
                {
                public void perform()
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { ss.setNumSteps(trackNum, (int)(trackLen * 127) + 1); }
                    finally { lock.unlock(); }
                    ssui.tracks.get(trackNum).updateLength();
                    ssui.updateSizes();
                    ssui.redraw();
                    }
                };


            trackGain = new SmallDial(-1)
                {
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return ss.getTrackGain(trackNum); }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { ss.setTrackGain(trackNum, val); }
                    finally { lock.unlock(); }
                    ssui.redraw();
                    }
                };

            trackFlam = new JComboBox(FLAM_STRINGS);
            trackFlam.setSelectedIndex(ss.getTrackFlam(trackNum));
            trackFlam.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    int result = trackFlam.getSelectedIndex();
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { ss.setTrackFlam(trackNum, result); }
                    finally { lock.unlock(); }                              
                    }
                });

            setTrackFlam = new PushButton("Set All")
                {
                public void perform()
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try 
                        {
                        int len = ss.getNumSteps(trackNum);
                        for(int i = 0; i < len; i++) { ss.setFlam(trackNum, i, StepSequence.DEFAULT); }
                        }
                    finally { lock.unlock(); }
                    ssui.getStepInspector().revise();
                    }
                };

            String[] chokes = new String[ss.getNumTracks() + 1];
            chokes[0] = "None";
            for(int i = 0; i < ss.getNumTracks(); i++)
                chokes[i + 1] = "Track " + i;
            trackChoke = new JComboBox(chokes);
            trackChoke.setSelectedIndex(ss.getTrackChoke(trackNum));
            trackChoke.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    int result = trackChoke.getSelectedIndex();
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { ss.setTrackChoke(trackNum, result); }
                    finally { lock.unlock(); }                              
                    }
                });

            trackWhen = new JComboBox(WHEN_STRINGS);
            trackWhen.setSelectedIndex(ss.getTrackWhen(trackNum));
            trackWhen.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    int result = trackWhen.getSelectedIndex();
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { ss.setTrackWhen(trackNum, result); }
                    finally { lock.unlock(); }                              
                    }
                });

            setTrackWhen = new PushButton("Set All")
                {
                public void perform()
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try 
                        {
                        int len = ss.getNumSteps(trackNum);
                        for(int i = 0; i < len; i++) { ss.setWhen(trackNum, i, StepSequence.DEFAULT); }
                        }
                    finally { lock.unlock(); }
                    ssui.getStepInspector().revise();
                    }
                };
                        
            trackSwing = new SmallDial(-1, 0)
                {
                protected String map(double val) 
                    { 
                    //if (getDefault()) return "<html><i>Default</i></html>"; else 
                    return super.map(val); 
                    }
                public int getDefault() { double val = ss.getTrackSwing(trackNum); return (val == -1 ? DEFAULT : NO_DEFAULT); }
                public void setDefault(int val) { super.setDefault(val); if (val == DEFAULT) ss.setTrackSwing(trackNum, -1); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return ss.getTrackSwing(trackNum); }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { ss.setTrackSwing(trackNum, val); }
                    finally { lock.unlock(); }
                    }
                };
            trackVelocity = new SmallDial(-1, 0)
                {
                protected String map(double val) 
                    { 
                    return String.valueOf((int)(val * 127)); 
                    }
                public int getDefault() { int val = ss.getTrackVelocity(trackNum); return (val == -1 ? DEFAULT : NO_DEFAULT); }
                public void setDefault(int val) { super.setDefault(val); if (val == DEFAULT) ss.setTrackVelocity(trackNum, -1); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return ss.getTrackVelocity(trackNum) / 127.0; }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { ss.setTrackVelocity(trackNum, (int)(val * 127)); }
                    finally { lock.unlock(); }
                    ssui.redraw();
                    }
                };
                
            setTrackVelocity = new PushButton("Set All")
                {
                public void perform()
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try 
                        {
                        int len = ss.getNumSteps(trackNum);
                        for(int i = 0; i < len; i++) { ss.setVelocity(trackNum, i, StepSequence.DEFAULT); }
                        }
                    finally { lock.unlock(); }
                    ssui.getStepInspector().revise();
                    }
                };

            Out[] seqOuts = seq.getOuts();
            String[] outs = new String[seqOuts.length + 1];
            outs[0] = "<html><i>Default</i></html>";
            for(int i = 0; i < seqOuts.length; i++)
                {
                outs[i+1] = "" + (i + 1) + ": " + seqOuts[i].toString();
                }
                
            trackOut = new JComboBox(outs);
            trackOut.setMaximumRowCount(outs.length);
            trackOut.setSelectedIndex(ss.getTrackOut(trackNum) + 1);
            trackOut.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    int result = trackOut.getSelectedIndex() - 1;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { ss.setTrackOut(trackNum, result); }              // -1 is DEFAULT
                    finally { lock.unlock(); }                              
                    }
                });
                        
            trackName = new StringField(null, ss.getTrackName(trackNum))
                {
                public String newValue(String newValue)
                    {
                    if (seq == null) return newValue;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try 
                        { 
                        ss.setTrackName(trackNum, newValue); 
                        ssui.getTrack(trackNum).getHeader().revise();   // update track header name
                        }
                    finally { lock.unlock(); return newValue; }                             
                    }
                };
            trackName.setColumns(MotifUI.INSPECTOR_NAME_DEFAULT_SIZE);

            }
        finally { lock.unlock(); }
                
        JPanel lengthPanel = new JPanel();
        lengthPanel.setLayout(new BorderLayout());
        lengthPanel.add(numSteps.getLabelledDial("128"), BorderLayout.WEST);
        lengthPanel.add(setNumSteps, BorderLayout.EAST);


        JPanel notePanel = new JPanel();
        notePanel.setLayout(new BorderLayout());
        notePanel.add(trackNote.getLabelledDial("C#-2"), BorderLayout.WEST);
        notePanel.add(setTrackNote, BorderLayout.EAST);


        JPanel velocityPanel = new JPanel();
        velocityPanel.setLayout(new BorderLayout());
        velocityPanel.add(trackVelocity.getLabelledDial("<html><i>Default</i></html>"), BorderLayout.WEST);
        velocityPanel.add(setTrackVelocity, BorderLayout.EAST);


        JPanel flamPanel = new JPanel();
        flamPanel.setLayout(new BorderLayout());
        flamPanel.add(trackFlam, BorderLayout.CENTER);          // so it stretches
        flamPanel.add(setTrackFlam, BorderLayout.EAST);

        JPanel whenPanel = new JPanel();
        whenPanel.setLayout(new BorderLayout());
        whenPanel.add(trackWhen, BorderLayout.CENTER);          // so it stretches
        whenPanel.add(setTrackWhen, BorderLayout.EAST);

        build(new String[] { "Name", "Gain", "Note", "   Velocity", "Flams", "When", "Choke", "Swing", "Ex. Rand.", "Out", "Length" }, 
            new JComponent[] 
                { 
                trackName,
                trackGain.getLabelledDial("0.0000"), 
                notePanel, 
                velocityPanel,
                flamPanel, 
                whenPanel, 
                trackChoke, 
                trackSwing.getLabelledDial("<html><i>Default</i></html>"), 
                trackExclusive, 
                trackOut,
                lengthPanel,
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
            trackChoke.setSelectedIndex(ss.getTrackChoke(trackNum)); 
            trackFlam.setSelectedIndex(ss.getTrackFlam(trackNum)); 
            trackWhen.setSelectedIndex(ss.getTrackWhen(trackNum));
            trackOut.setSelectedIndex(ss.getTrackOut(trackNum) + 1); 
            trackExclusive.setSelected(ss.isTrackExclusive(trackNum)); 
            trackName.setValue(ss.getTrackName(trackNum));
            }
        finally { lock.unlock(); }                              
        //System.err.println("TrackInspector Releasing Lock");
        seq = old;
        trackGain.redraw();
        trackNote.redraw();
        trackVelocity.redraw();
        numSteps.redraw();
        }
    }
