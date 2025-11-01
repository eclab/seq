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
    SmallDial stepNoteLSB;
    SmallDial stepVelocityLSB;
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
        

    String[] defaults = new String[1 + Motif.NUM_PARAMETERS];

    public void buildDefaults(Motif parent)
        {
        defaults[0] = "Rand";
        for(int i = 0; i < Motif.NUM_PARAMETERS; i++)
            {
            String name = parent.getParameterName(i);
            if (name == null || name.length() == 0)
                {
                defaults[1 + i] = "Param " + (i + 1);
                }
            else
                {
                defaults[1 + i] = "" + (i + 1) + ": " + name;
                }
            }
        }
        
    void setupFirstDefault(SmallDial dial)
        {
        dial.setFirstDefault("<html><i>Default</i></html>", Motif.NUM_PARAMETERS + 1);
        }

    public StepInspector(Seq seq, StepSequence ss, StepSequenceUI ssui, int trackNum, int stepNum)
        {
        this.seq = seq;
        this.ss = ss;
        this.ssui = ssui;
        this.trackNum = trackNum;
        this.stepNum = stepNum;
        buildDefaults(ss);
        
        int type = 0;
                
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            type = ss.getType();
            final int _type = type;
            
            stepNote = new SmallDial(-1, defaults)
                {
                protected String map(double val) 
                    {                                   
                    int n = (int)(val * 127); 

                    if (_type == StepSequence.TYPE_NOTE ||
                        _type == StepSequence.TYPE_POLYPHONIC_AFTERTOUCH)
                        {
                        return NOTES[n % 12] + ((n / 12) - 2); 
                        }
                    else
                        {
                        return "" + n;
                        }
                    }
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
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) ss.setNote(trackNum, stepNum, -(val + 1)); }
                    finally { lock.unlock(); }
                    }
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = ss.getNote(trackNum, stepNum); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };
            setupFirstDefault(stepNote);
            stepNote.setToolTipText(NOTE_TOOLTIP);

            stepNoteLSB = new SmallDial(-1, defaults)
                {
                protected String map(double val) 
                    { 
                    return String.valueOf((int)(val * 127)); 
                    }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return ss.getParamLSB(trackNum, stepNum) / 127.0; }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { ss.setParamLSB(trackNum, stepNum, (int)(val * 127)); }
                    finally { lock.unlock(); }
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) ss.setParamLSB(trackNum, stepNum, -(val + 1)); }
                    finally { lock.unlock(); }
                    }
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = ss.getParamLSB(trackNum, stepNum); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };
            setupFirstDefault(stepNoteLSB);
            stepNoteLSB.setToolTipText(PARAMETER_LSB_TOOLTIP);

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
            stepFlam.setToolTipText(FLAM_TOOLTIP);


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
            stepWhen.setToolTipText(WHEN_TOOLTIP);

            stepVelocity = new SmallDial(-1, defaults)
                {
                protected String map(double val) 
                    { 
                    return String.valueOf((int)(val * 127)); 
                    }
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
                    ssui.redraw(false);
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) ss.setVelocity(trackNum, stepNum, -(val + 1)); }
                    finally { lock.unlock(); }
                    ssui.redraw(false);
                    }
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = ss.getVelocity(trackNum, stepNum); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };
            setupFirstDefault(stepVelocity);
            stepVelocity.setToolTipText(VELOCITY_TOOLTIP);

            stepVelocityLSB = new SmallDial(-1, defaults)
                {
                protected String map(double val) 
                    { 
                    return String.valueOf((int)(val * 127)); 
                    }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return ss.getValueLSB(trackNum, stepNum) / 127.0; }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { ss.setValueLSB(trackNum, stepNum, (int)(val * 127)); }
                    finally { lock.unlock(); }
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) ss.setValueLSB(trackNum, stepNum, -(val + 1)); }
                    finally { lock.unlock(); }
                    }
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = ss.getValueLSB(trackNum, stepNum); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };
            setupFirstDefault(stepVelocityLSB);
            stepVelocityLSB.setToolTipText(VALUE_LSB_TOOLTIP);
            
            }
        finally { lock.unlock(); }
        
        
        JPanel notePanel = new JPanel();
        notePanel.setToolTipText(NOTE_TOOLTIP);
        JPanel innerPanel = new JPanel();
        innerPanel.setToolTipText(NOTE_TOOLTIP);
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.X_AXIS));
        innerPanel.add(stepNote.getLabelledDial("Param 8"));
        if (type == StepSequence.TYPE_NRPN ||
            type == StepSequence.TYPE_RPN)
            {
            JLabel label = new JLabel(" LSB ");
            label.setToolTipText(PARAMETER_LSB_TOOLTIP);
            innerPanel.add(label);
            innerPanel.add(stepNoteLSB.getLabelledDial("Param 8"));
            }
        notePanel.setLayout(new BorderLayout());
        notePanel.add(innerPanel, BorderLayout.WEST);

        JPanel velocityPanel = new JPanel();
        velocityPanel.setToolTipText(VELOCITY_TOOLTIP);
        innerPanel = new JPanel();
        innerPanel.setToolTipText(VELOCITY_TOOLTIP);
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.X_AXIS));
        innerPanel.add(stepVelocity.getLabelledDial("Param 8"));                // so it lines up with the notes
        if (type == StepSequence.TYPE_NRPN ||
            type == StepSequence.TYPE_RPN ||
            type == StepSequence.TYPE_PITCH_BEND)
            {
            JLabel label = new JLabel(" LSB ");
            label.setToolTipText(VALUE_LSB_TOOLTIP);
            innerPanel.add(label);
            innerPanel.add(stepVelocityLSB.getLabelledDial("Param 8"));
            }
        velocityPanel.setLayout(new BorderLayout());
        velocityPanel.add(innerPanel, BorderLayout.WEST);

        boolean note = (type == StepSequence.TYPE_NOTE);
        
        build(new String[] { (note ? "Note" : "Param"), (note ? "Velocity" : "Value"), "     Flams", "When"}, 
            new JComponent[] 
                { 
                notePanel,
                velocityPanel,
                stepFlam, 
                stepWhen 
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
            stepFlam.setSelectedIndex(ss.getFlam(trackNum, stepNum) + 1); 
            stepWhen.setSelectedIndex(ss.getWhen(trackNum, stepNum) + 1); 
            }
        finally { lock.unlock(); seq = old; }                              
        stepNote.redraw();
        stepVelocity.redraw();
        stepNoteLSB.redraw();
        stepVelocityLSB.redraw();
        }


    static final String NOTE_TOOLTIP = "<html><b>Note / Param</b><br>" +
        "Sets the MIDI note output or Parameter for this track, depending on the Sequence type.<br>" +
        "This overrides the <b>Note /Param </b> setting in the track inspector.  To return to the<br>" +
        "default, double-click the dial.<br><br>" + 
        "For some parameters types, the Note/Param dial is the MSB of the parameter, while<br>" +
        "the <b>LSB</b> dial is the LSB of the parameter.</html>";
                        
    static final String FLAM_TOOLTIP = "<html><b>Flams</b><br>" +
        "Sets the number of <i>flams</i> (or <i>ratchets</i>) for this step.  This<br>" +
        "overrides the <b>Flams</b> setting in the track inspector.  To return to<br>" +
        "the default, double-click the dial.</html>";
                        
    static final String WHEN_TOOLTIP = "<html><b>When</b><br>" +
        "Sets the pattern value for this step to determine if it should play.  This overrides<br>" +
        "the <b>When</b> setting in the track inspector.  To return to the default, double-click<br>" +
        "the dial. There are many pattern options:<br>" +
        "<ul><li>Always play." + 
        "<li>Play with some probability." +
        "<li>Play with the probability <i>1/Tracks</i> where <i>Tracks<i> is the number of tracks." +
        "<li>Play with the probability <i>1/(1-Tracks)</i> where <i>Tracks<i> is the number of tracks." +
        "<li>Play (X) or don't play (O) with a certain repeating pattern." +
        "</ul>" +
        "Regardless of the pattern, a step doesn't play if it hasn't been enabled in the sequencer grid.</html>";
                        
    static final String VELOCITY_TOOLTIP = "<html><b>Velocity / Value</b><br>" +
        "Sets the velocity or parameter value for this step, depending on the Sequence type.<br>" +
        "This overridesthe <b>Velocity / Value</b> setting in the track inspector.  To return<br>" + 
        "to the default, double-click the dial.<br><br>" + 
        "For some parameters types, the velocity/value dial is the MSB of the value, while<br>" +
        "the <b>LSB</b> dial is the LSB of the value.</html>";

    static final String VALUE_LSB_TOOLTIP = "<html><b>Value LSB</b><br>" +
        "Sets the LSB of the value for this step.</html>";

    static final String PARAMETER_LSB_TOOLTIP = "<html><b>Parameter LSB</b><br>" +
        "Sets the LSB of the parameter for this step.</html>";
    }
