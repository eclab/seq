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
                
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            stepNote = new SmallDial(-1, defaults)		// 0)
                {
                protected String map(double val) 
                    { 
//                    if (getDefault()) return "<html><i>Default</i></html>";
                    int n = (int)(val * 127); 
                    return NOTES[n % 12] + ((n / 12) - 2); 
                    }
                /*
                public int getDefault() { int val = ss.getNote(trackNum, stepNum); return (val == -1 ? DEFAULT : NO_DEFAULT); }
                public void setDefault(int val) { if (val == DEFAULT) ss.setNote(trackNum, stepNum, -1); }
                */
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

            stepVelocity = new SmallDial(-1, defaults)		// 0)
                {
                protected String map(double val) 
                    { 
                    //if (getDefault()) return "<html><i>Default</i></html>"; else 
                    return String.valueOf((int)(val * 127)); 
                    }
                /*
                public int getDefault() { int val = ss.getVelocity(trackNum, stepNum); return (val == -1 ? DEFAULT : NO_DEFAULT); }
                public void setDefault(int val) { if (val == DEFAULT) ss.setVelocity(trackNum, stepNum, -1); }
                */
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
        lock.lock();
        try 
            { 
            stepFlam.setSelectedIndex(ss.getFlam(trackNum, stepNum) + 1); 
            stepWhen.setSelectedIndex(ss.getWhen(trackNum, stepNum) + 1); 
            }
        finally { lock.unlock(); }                              
        seq = old;
        stepNote.redraw();
        stepVelocity.redraw();
        }


    static final String NOTE_TOOLTIP = "<html><b>Note</b><br>" +
        "Sets the MIDI note output for this track.  This overrides the <b>Note</b> setting<br>" + 
        "in the track inspector.  To return to the default, double-click the dial.</html>";
                        
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
                        
    static final String VELOCITY_TOOLTIP = "<html><b>Velocity</b><br>" +
        "Sets the velocity value for this step.  This overridesthe <b>Velocity</b> setting<br>" +
        "in the track inspector.  To return to the default, double-click the dial.</html>";
    }
