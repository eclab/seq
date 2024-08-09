package seq.motif.stepsequence.gui;

import seq.motif.stepsequence.*;
import seq.engine.*;
import seq.gui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.locks.*;

public class StepSequenceInspector extends WidgetList
    {
    Seq seq;
    StepSequence ss;
    StepSequenceUI ssui;
        
    StringField name;
    SmallDial numBeats;
    SmallDial defaultSwing;
    PushButton setDefaultSwing;
    SmallDial defaultVelocity;
    PushButton setDefaultVelocity;
    SmallDial initialNumSteps;
    PushButton setNumSteps;
    double nt;
    JComboBox defaultOut;
    JComboBox in;
        
    public StepSequenceInspector(Seq seq, StepSequence ss, StepSequenceUI ssui)
        {
        this.seq = seq;
        this.ss = ss;
        this.ssui = ssui;
                
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            name = new StringField(ss.getName())
                {
                public String newValue(String newValue)
                    {
                    newValue = newValue.trim();
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { ss.setName(newValue); }
                    finally { lock.unlock(); }
                    ssui.updateText();
                    return newValue;
                    }
                                
                public String getValue() 
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return ss.getName(); }
                    finally { lock.unlock(); }
                    }
                };
            name.setColumns(MotifUI.INSPECTOR_NAME_DEFAULT_SIZE);
                        
            numBeats = new SmallDial(-1)                // (StepSequence.DEFAULT_LENGTH_IN_STEPS - 1) / 127)
                {
                protected String map(double val) { return String.valueOf((int)(val * 127) + 1);  }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return (ss.getLengthInSteps() - 1) / 127.0; }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { ss.setLengthInSteps((int)(val * 127) + 1); }
                    finally { lock.unlock(); }
                    ssui.redraw();
                    }
                };

            initialNumSteps = new SmallDial(-1)
                {
                protected String map(double val) { return String.valueOf((int)(val * 127) + 1); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return (ss.getInitialNumSteps() - 1) / 127.0; }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { ss.setInitialNumSteps((int)(val * 127) + 1); }
                    finally { lock.unlock(); }
                    }
                };
                        
            setNumSteps = new PushButton("Set")
                {
                public void perform()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try
                        {
                        int len = ss.getInitialNumSteps();
                        for(int i = 0; i < ss.getNumTracks(); i++)
                            {
                            ss.setNumSteps(i, len);
                            }
                        }
                    finally { lock.unlock(); }
                    for(int i=0; i < ssui.tracks.size();i++)
                        {
                        ssui.tracks.get(i).updateLength();
                        }
                    ssui.updateSizes();
                    ssui.redraw();
                    }
                };
                        
            defaultSwing = new SmallDial(-1)
                {
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return ss.getDefaultSwing(); }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { ss.setDefaultSwing(val); }
                    finally { lock.unlock(); }
                    }
                };
                        
            setDefaultSwing = new PushButton("Set All")
                {
                public void perform()
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try 
                        {
                        int len = ss.getNumTracks();
                        for(int i = 0; i < len; i++) { ss.setTrackSwing(i, StepSequence.DEFAULT); }
                        }
                    finally { lock.unlock(); }
                    ssui.getTrackInspector().revise();
                    }
                };

            defaultVelocity = new SmallDial(-1)
                {
                protected String map(double val) { return String.valueOf((int)(val * 127)); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return ss.getDefaultVelocity() / 127.0; }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { ss.setDefaultVelocity((int)(val * 127)); }
                    finally { lock.unlock(); }
                    ssui.redraw();
                    }
                };
                        
            setDefaultVelocity = new PushButton("Set All")
                {
                public void perform()
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try 
                        {
                        int len = ss.getNumTracks();
                        for(int i = 0; i < len; i++)
                            {
                            ss.setTrackVelocity(i, StepSequence.DEFAULT);
                            int trackLen = ss.getNumSteps(i);
                            for(int j = 0; j < trackLen; j++) { ss.setVelocity(i, j, StepSequence.DEFAULT); }
                            }
                        }
                    finally { lock.unlock(); }
                    ssui.getTrackInspector().revise();
                    ssui.getStepInspector().revise();
                    ssui.redraw();
                    }
                };

            Out[] seqOuts = seq.getOuts();
            String[] outs = new String[seqOuts.length];
            for(int i = 0; i < seqOuts.length; i++)
                {
                outs[i] = "" + (i + 1) + ": " + seqOuts[i].toString();
                }

            defaultOut = new JComboBox(outs);
            defaultOut.setMaximumRowCount(outs.length);
            defaultOut.setSelectedIndex(ss.getDefaultOut());
            defaultOut.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { ss.setDefaultOut(defaultOut.getSelectedIndex()); }
                    finally { lock.unlock(); }                              
                    }
                });

            In[] seqIns = seq.getIns();
            String[] ins = new String[seqIns.length];
            for(int i = 0; i < seqIns.length; i++)
                {
                ins[i] = "" + (i + 1) + ": " + seqIns[i].toString();
                }
                
            in = new JComboBox(ins);
            in.setSelectedIndex(ss.getIn());
            in.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { ss.setIn(in.getSelectedIndex()); }
                    finally { lock.unlock(); }                              
                    }
                });


            }
        finally { lock.unlock(); }

        JPanel lengthPanel = new JPanel();
        lengthPanel.setLayout(new BorderLayout());
        lengthPanel.add(initialNumSteps.getLabelledDial("128"), BorderLayout.WEST);
        lengthPanel.add(setNumSteps, BorderLayout.EAST);
                
        JPanel swingPanel = new JPanel();
        swingPanel.setLayout(new BorderLayout());
        swingPanel.add(defaultSwing.getLabelledDial("0.0000"), BorderLayout.WEST);
        swingPanel.add(setDefaultSwing, BorderLayout.EAST);

        JPanel velocityPanel = new JPanel();
        velocityPanel.setLayout(new BorderLayout());
        velocityPanel.add(defaultVelocity.getLabelledDial("127"), BorderLayout.WEST);
        velocityPanel.add(setDefaultVelocity, BorderLayout.EAST);

        build(new String[] { "Name", "Duration", "Swing", "   Velocity", "Out", "In", "Track Len", /* "Tracks" */ }, 
            new JComponent[] 
                {
                name,
                numBeats.getLabelledDial("128"),
                swingPanel, 
                velocityPanel, 
                defaultOut,
                in,
                lengthPanel, 
                // numTracksPanel
                });
        }
                
    public void revise()
        {
        Seq old = seq;
        seq = null;
        ReentrantLock lock = old.getLock();
        lock.lock();
        try 
            { 
            defaultOut.setSelectedIndex(ss.getDefaultOut()); 
            in.setSelectedIndex(ss.getIn()); 
            }
        finally { lock.unlock(); }                              
        seq = old;
        defaultSwing.redraw();
        defaultVelocity.redraw();
        initialNumSteps.redraw();
        name.update();
//              numTracks.redraw();
        }
    }
