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
            name.setToolTipText(NAME_TOOLTIP);
                        
            numBeats = new SmallDial(-1)
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
                    ssui.redraw(false);
                    }
                };
            numBeats.setToolTipText(DURATION_TOOLTIP);

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
            initialNumSteps.setToolTipText(TRACK_LENGTH_TOOLTIP);

            setNumSteps = new PushButton("Set All")
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
                    ssui.redraw(false);
                    }
                };
            setNumSteps.setToolTipText(SET_TRACK_LENGTH_TOOLTIP);

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
            defaultSwing.setToolTipText(SWING_TOOLTIP);

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
            setDefaultSwing.setToolTipText(SET_SWING_TOOLTIP);

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
                    ssui.redraw(false);
                    }
                };
            defaultVelocity.setToolTipText(VELOCITY_TOOLTIP);
                        
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
                    ssui.redraw(false);
                    }
                };
            setDefaultVelocity.setToolTipText(SET_VELOCITY_TOOLTIP);

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
            defaultOut.setToolTipText(OUT_TOOLTIP);

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
            in.setToolTipText(IN_TOOLTIP);


            }
        finally { lock.unlock(); }

        JPanel lengthPanel = new JPanel();
        lengthPanel.setLayout(new BorderLayout());
        lengthPanel.add(initialNumSteps.getLabelledDial("128"), BorderLayout.WEST);
        lengthPanel.add(setNumSteps, BorderLayout.EAST);
        lengthPanel.setToolTipText(TRACK_LENGTH_TOOLTIP);
                
        JPanel swingPanel = new JPanel();
        swingPanel.setLayout(new BorderLayout());
        swingPanel.add(defaultSwing.getLabelledDial("0.0000"), BorderLayout.WEST);
        swingPanel.add(setDefaultSwing, BorderLayout.EAST);
        swingPanel.setToolTipText(SWING_TOOLTIP);

        JPanel velocityPanel = new JPanel();
        velocityPanel.setLayout(new BorderLayout());
        velocityPanel.add(defaultVelocity.getLabelledDial("127"), BorderLayout.WEST);
        velocityPanel.add(setDefaultVelocity, BorderLayout.EAST);
        velocityPanel.setToolTipText(VELOCITY_TOOLTIP);

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


    /*** Tooltips ***/
        
    static final String NAME_TOOLTIP = "<html><b>Name</b><br>" +
        "Sets the name of the Sequence.  This will appear in the <b>Motif List</b>.</html>";
        
    static final String DURATION_TOOLTIP = "<html><b>Duration</b><br>" +
        "Sets the length of time (in sixteenth notes) that the entire sequence<br>" + 
        "takes to complete one iteration.";
        
    static final String TRACK_LENGTH_TOOLTIP = "<html><b>Track Length</b><br>" +
        "Changes the <i>initial</i> number of steps a new track will have.<br>" +
        "After creating a track, you can change the number of steps it has<br>" +
        "(its length) in the track's inspector." ;
                
    static final String SET_TRACK_LENGTH_TOOLTIP = "<html><b>Set All Track Length</b><br>" +
        "Resets all tracks to the <b>Track Len</b> value at left.</html>";
        
    static final String SWING_TOOLTIP = "<html><b>Swing</b><br>" +
        "Sets the <i>default</i> swing value for all tracks.  This can be overridden<br>" +
        "in the track's inspector.</html>";
        
    static final String SET_SWING_TOOLTIP = "<html><b>Set All Swing</b><br>" +
        "Resets all tracks to use the default swing value shown at left instead of" + 
        "custom per-track swing values.</html>";
        
    static final String VELOCITY_TOOLTIP = "<html><b>Velocity</b><br>" +
        "Sets the <i>default</i> velocity value for all tracks.  This can be overridden<br>" +
        "in the track's inspector.</html>";
        
    static final String SET_VELOCITY_TOOLTIP = "<html><b>Set All Velocity</b><br>" +
        "Resets all tracks to use the default velocity value shown at left instead of" + 
        "custom per-track velocity values.</html>";

    static final String OUT_TOOLTIP = "<html><b>Set Out</b><br>" +
        "Sets the <i>default</i> MIDI output for all tracks.  This can be overridden<br>" +
        "in the track's inspector.</html>";
        
    static final String IN_TOOLTIP = "<html><b>Zoom Out</b><br>" +
        "Sets the sequencer's MIDI input, used for a track's <b>Learn</b> functionality.</html>";
    }
