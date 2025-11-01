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
import java.util.concurrent.*;

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
    SmallDial trackNoteLSB;
    SmallDial trackVelocityLSB;
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
    SmallDial euclidK;
    double euclidKVal = 0.0;
    SmallDial euclidRotate;
    double euclidRotateVal = 0.0;
    PushButton doEuclid;
    PushButton randomEuclid;

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

    public TrackInspector(Seq seq, StepSequence ss, StepSequenceUI ssui, int trackNum)
        {
        this.seq = seq;
        this.ss = ss;
        this.trackNum = trackNum;
        this.ssui = ssui;
        buildDefaults(ss);
        int type = 0;
                
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            type = ss.getType();
            final int _type = type;
            
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
            trackExclusive.setToolTipText(EXCLUSIVE_RANDOM_TOOLTIP);

            trackNote = new SmallDial(-1, defaults)
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
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) ss.setTrackNote(trackNum, -(val + 1)); }
                    finally { lock.unlock(); }
                    }
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = ss.getTrackNote(trackNum); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };
            trackNote.setToolTipText(NOTE_TOOLTIP);

            trackNoteLSB = new SmallDial(-1, defaults)
                {
                protected String map(double val) 
                    { 
                    return String.valueOf((int)(val * 127)); 
                    }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return ss.getTrackParamLSB(trackNum) / 127.0; }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { ss.setTrackParamLSB(trackNum, (int)(val * 127)); }
                    finally { lock.unlock(); }
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) ss.setTrackParamLSB(trackNum, -(val + 1)); }
                    finally { lock.unlock(); }
                    }
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = ss.getTrackParamLSB(trackNum); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
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
                        for(int i = 0; i < len; i++) 
                            { 
                            ss.setNote(trackNum, i, StepSequence.DEFAULT); 
                            ss.setParamLSB(trackNum, i, StepSequence.DEFAULT); 
                            }
                        }
                    finally { lock.unlock(); }
                    ssui.getStepInspector().revise();
                    }
                };
            setTrackNote.setToolTipText(SET_NOTE_TOOLTIP);

            trackLen = (ss.getNumSteps(trackNum) - 1) / 127.0;
            numSteps = new SmallDial(-1)
                {
                protected String map(double val) { return String.valueOf((int)(val * 127) + 1); }
                public double getValue() { return trackLen; }
                public void setValue(double val) { trackLen = val; }
                };
            numSteps.setToolTipText(LENGTH_TOOLTIP);

            setNumSteps = new PushButton("Set")
                {
                public void perform()
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { ss.setNumSteps(trackNum, (int)(trackLen * 127) + 1); }
                    finally { lock.unlock(); }
                    ssui.setSelectedStepNum(0);
                    ssui.tracks.get(trackNum).updateLength();
                    ssui.rebuildTracks();
                    //ssui.redraw(false);
                    }
                };
            setNumSteps.setToolTipText(SET_LENGTH_TOOLTIP);


            trackGain = new SmallDial(-1, defaults)
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
                    ssui.redraw(false);
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) ss.setTrackGain(trackNum, -(val + 1)); }
                    finally { lock.unlock(); }
                    }
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = ss.getTrackGain(trackNum); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };
            trackGain.setToolTipText(GAIN_TOOLTIP);

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

            trackFlam.setToolTipText(FLAM_TOOLTIP);

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
                        for(int i = 0; i < len; i++) { ss.setFlam(trackNum, i, StepSequence.DEFAULT_FLAM); }
                        }
                    finally { lock.unlock(); }
                    ssui.getStepInspector().revise();
                    }
                };
            setTrackFlam.setToolTipText(SET_FLAM_TOOLTIP);

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

            trackChoke.setToolTipText(CHOKE_TOOLTIP);

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
            trackWhen.setToolTipText(WHEN_TOOLTIP);

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
                        for(int i = 0; i < len; i++) { ss.setWhen(trackNum, i, StepSequence.DEFAULT_WHEN); }
                        }
                    finally { lock.unlock(); }
                    ssui.getStepInspector().revise();
                    }
                };
            setTrackWhen.setToolTipText(SET_WHEN_TOOLTIP);

            trackSwing = new SmallDial(-1, defaults)            // 0)
                {
                protected String map(double val) 
                    { 
                    //if (getDefault()) return "<html><i>Default</i></html>"; else 
                    return super.map(val); 
                    }
                /*
                  public int getDefault() { double val = ss.getTrackSwing(trackNum); return (val == -1 ? DEFAULT : NO_DEFAULT); }
                  public void setDefault(int val) { super.setDefault(val); if (val == DEFAULT) ss.setTrackSwing(trackNum, -1); }
                */
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
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) ss.setTrackSwing(trackNum, -(val + 1)); }
                    finally { lock.unlock(); }
                    }
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = ss.getTrackSwing(trackNum); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };
            setupFirstDefault(trackSwing);
            trackSwing.setToolTipText(SWING_TOOLTIP);


            trackVelocity = new SmallDial(-1, defaults)         //  0)
                {
                protected String map(double val) 
                    { 
                    return String.valueOf((int)(val * 127)); 
                    }
                /*
                  public int getDefault() { int val = ss.getTrackVelocity(trackNum); return (val == -1 ? DEFAULT : NO_DEFAULT); }
                  public void setDefault(int val) { super.setDefault(val); if (val == DEFAULT) ss.setTrackVelocity(trackNum, -1); }
                */
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
                    ssui.redraw(false);
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) ss.setTrackVelocity(trackNum, -(val + 1)); }
                    finally { lock.unlock(); }
                    ssui.redraw(false);
                    }
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = ss.getTrackVelocity(trackNum); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };
            setupFirstDefault(trackVelocity);
            trackVelocity.setToolTipText(VELOCITY_TOOLTIP);
                
            trackVelocityLSB = new SmallDial(-1, defaults)
                {
                protected String map(double val) 
                    { 
                    return String.valueOf((int)(val * 127)); 
                    }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return ss.getTrackValueLSB(trackNum) / 127.0; }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { ss.setTrackValueLSB(trackNum, (int)(val * 127)); }
                    finally { lock.unlock(); }
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) ss.setTrackValueLSB(trackNum, -(val + 1)); }
                    finally { lock.unlock(); }
                    }
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = ss.getTrackValueLSB(trackNum); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };
            setupFirstDefault(trackVelocityLSB);

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
                        for(int i = 0; i < len; i++) 
                            { 
                            ss.setVelocity(trackNum, i, StepSequence.DEFAULT); 
                            ss.setValueLSB(trackNum, i, StepSequence.DEFAULT); 
                            }
                        }
                    finally { lock.unlock(); }
                    ssui.getStepInspector().revise();
                    }
                };
            setTrackVelocity.setToolTipText(SET_VELOCITY_TOOLTIP);

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
            trackOut.setToolTipText(OUT_TOOLTIP);

                        
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
            trackName.setToolTipText(NAME_TOOLTIP);

            euclidK = new SmallDial(euclidKVal)
                {
                protected String map(double val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try 
                        { 
                        return String.valueOf((int)(val * ss.getNumSteps(trackNum)));
                        }
                    finally
                        {
                        lock.unlock();
                        }
                    }
                public double getValue() 
                    { 
                    return euclidKVal;
                    }
                public void setValue(double val) 
                    { 
                    euclidKVal = val;
                    }
                };
            euclidK.setToolTipText(EUCLID_TOOLTIP);
                
            euclidRotate = new SmallDial(euclidRotateVal)
                {
                protected String map(double val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try 
                        { 
                        return String.valueOf((int)(val * (ss.getNumSteps(trackNum))));
                        }
                    finally
                        {
                        lock.unlock();
                        }
                    }
                public double getValue() 
                    { 
                    return euclidRotateVal;
                    }
                public void setValue(double val) 
                    { 
                    euclidRotateVal = val;
                    }
                };
            euclidRotate.setToolTipText(ROTATE_TOOLTIP);

            doEuclid = new PushButton("S")
                {
                public void perform()
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try 
                        {
                        ss.applyEuclideanRhythm(trackNum, euclidKVal, euclidRotateVal);
                        }
                    finally { lock.unlock(); }
                    ssui.getTrack(trackNum).repaint();          // is this sufficient?
                    }
                };
            doEuclid.setToolTipText(S_TOOLTIP);

            randomEuclid = new PushButton("R")
                {
                public void perform()
                    {
                    if (seq == null) return;
                    double euclid = 0;
                    double rotate = 0;
                    ThreadLocalRandom random = ThreadLocalRandom.current();
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try 
                        {
                        // only allow 1 through N-1, disallow 0 and N which aren't interesting
                        euclid = (random.nextInt(ss.getNumSteps(trackNum) - 2) + 1) / 
                            (double)ss.getNumSteps(trackNum);
                        // only allow 0 through N-1, disallow N, which isn't interesting
                        rotate = random.nextDouble();   
                        ss.applyEuclideanRhythm(trackNum, euclid, rotate);
                        }
                    finally { lock.unlock(); }
                    euclidRotate.setValue(rotate);
                    euclidK.setValue(euclid);
                    euclidRotate.redraw();
                    euclidK.redraw();
                    ssui.getTrack(trackNum).repaint();          // is this sufficient?
                    }
                };
            randomEuclid.setToolTipText(R_TOOLTIP);
            }
        finally { lock.unlock(); }
                
        JPanel lengthPanel = new JPanel();
        lengthPanel.setLayout(new BorderLayout());
        lengthPanel.add(numSteps.getLabelledDial("128"), BorderLayout.WEST);
        lengthPanel.add(setNumSteps, BorderLayout.EAST);
        lengthPanel.setToolTipText(LENGTH_TOOLTIP);

        JPanel notePanel = new JPanel();
        JPanel innerPanel = new JPanel();
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.X_AXIS));
        innerPanel.add(trackNote.getLabelledDial("Param 8"));
        if (type == StepSequence.TYPE_NRPN ||
            type == StepSequence.TYPE_RPN)
            {
            JLabel label = new JLabel(" LSB ");
            label.setToolTipText(PARAMETER_LSB_TOOLTIP);
            innerPanel.add(label);
            innerPanel.add(trackNoteLSB.getLabelledDial("Param 8"));
            }
        notePanel.setLayout(new BorderLayout());
        notePanel.add(innerPanel, BorderLayout.WEST);
        notePanel.add(setTrackNote, BorderLayout.EAST);
        notePanel.setToolTipText(NOTE_TOOLTIP);


        JPanel velocityPanel = new JPanel();
        innerPanel = new JPanel();
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.X_AXIS));
        innerPanel.add(trackVelocity.getLabelledDial("Param 8"));
        if (type == StepSequence.TYPE_NRPN ||
            type == StepSequence.TYPE_RPN ||
            type == StepSequence.TYPE_PITCH_BEND)
            {
            JLabel label = new JLabel(" LSB ");
            label.setToolTipText(VALUE_LSB_TOOLTIP);
            innerPanel.add(label);
            innerPanel.add(trackVelocityLSB.getLabelledDial("Param 8"));
            }
        velocityPanel.setLayout(new BorderLayout());
        velocityPanel.add(innerPanel, BorderLayout.WEST);
        velocityPanel.add(setTrackVelocity, BorderLayout.EAST);
        velocityPanel.setToolTipText(VELOCITY_TOOLTIP);

        JPanel flamPanel = new JPanel();
        flamPanel.setLayout(new BorderLayout());
        flamPanel.add(trackFlam, BorderLayout.CENTER);          // so it stretches
        flamPanel.add(setTrackFlam, BorderLayout.EAST);
        flamPanel.setToolTipText(FLAM_TOOLTIP);

        JPanel whenPanel = new JPanel();
        whenPanel.setLayout(new BorderLayout());
        whenPanel.add(trackWhen, BorderLayout.CENTER);          // so it stretches
        whenPanel.add(setTrackWhen, BorderLayout.EAST);
        whenPanel.setToolTipText(WHEN_TOOLTIP);
        
        JPanel euclidPanel = new JPanel();
        euclidPanel.setLayout(new BorderLayout());
        Box euclidBox = new Box(BoxLayout.X_AXIS);
        euclidBox.add(euclidK.getLabelledDial("128"));
        JLabel label = new JLabel("  Rotate  ");
        label.setToolTipText(ROTATE_TOOLTIP);
        euclidBox.add(label);
        euclidBox.add(euclidRotate.getLabelledDial("128 "));
        Box randBox = new Box(BoxLayout.X_AXIS);
        randBox.add(doEuclid);
        randBox.add(randomEuclid);
        euclidPanel.add(euclidBox, BorderLayout.WEST);
        euclidPanel.add(randBox, BorderLayout.EAST);
        euclidPanel.setToolTipText(EUCLID_TOOLTIP);

        boolean note = (type == StepSequence.TYPE_NOTE);
        
        build(new String[] { "Name", "Gain", (note ? "Note" : "Param"), (note ? "Velocity" : "Value"), "Flams", "When", "Choke", "Swing", "Ex. Rand.", "Out", "Length", "Euclid" }, 
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
                euclidPanel
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
            trackChoke.setSelectedIndex(ss.getTrackChoke(trackNum)); 
            trackFlam.setSelectedIndex(ss.getTrackFlam(trackNum)); 
            trackWhen.setSelectedIndex(ss.getTrackWhen(trackNum));
            trackOut.setSelectedIndex(ss.getTrackOut(trackNum) + 1); 
            trackExclusive.setSelected(ss.isTrackExclusive(trackNum)); 
            trackName.setValue(ss.getTrackName(trackNum));
            }
        finally { lock.unlock(); }                              
        seq = old;
        trackGain.redraw();
        trackNote.redraw();
        trackVelocity.redraw();
        numSteps.redraw();
        trackNoteLSB.redraw();
        trackVelocityLSB.redraw();
        }

    /*** Tooltips ***/
        
    static final String EXCLUSIVE_RANDOM_TOOLTIP = "<html><b>Exclusive Random</b><br>" +
        "Sets whether the track is part of the exclusive random group.<br><br>" +
        "Only one track in the exclusive random group is played each iteration.<br>" +
        "The others are muted.</html>";
        
    static final String NOTE_TOOLTIP = "<html><b>Note / Param</b><br>" +
        "Sets the <i>default</i> MIDI note output or Parameter for all steps in the track.<br>" +
        "You can override this value in the individual step's inspector.<br><br>" + 
        "For some parameters types, the Note/Param dial is the MSB of the parameter, while<br>" +
        "the <b>LSB</b> dial is the LSB of the parameter.</html>";
                        
    static final String SET_NOTE_TOOLTIP = "<html><b>Set All Note / Param</b><br>" +
        "Resets all steps to use the MIDI output note or Parameter as shown at left.</html>";
        
    static final String LENGTH_TOOLTIP = "<html><b>Length</b><br>" +
        "Sets the number of steps in the track.  This doesn't take effect until the <b>Set</b><br>" +
        "button is pressed.  Changing the number of steps overrides the <b>Track Len</b> setting<br>" +
        "in the Step Sequence inspector.  To return to the default, double-click the dial (and press<br>" +
        "the Set button).</html>";
                        
    static final String SET_LENGTH_TOOLTIP = "<html><b>Set All Length</b><br>" +
        "Changes the number of steps in the track to the <b>Track Len</b> value at left.</html>";
        
    static final String GAIN_TOOLTIP = "<html><b>Gain</b><br>" +
        "Sets the gain (volume) for all steps in the track.</html>";
                        
    static final String FLAM_TOOLTIP = "<html><b>Flams</b><br>" +
        "Sets the <i>default</i> number of <i>flams</i> (or <i>ratchets</i>) for all steps in the track.<br>" +
        "You can override this value in the individual step's inspector.<br><br>"+
        "When a step has flams, then it is played multiple times, spaced evenly, within the same step.</html>";
                        
    static final String SET_FLAM_TOOLTIP = "<html><b>Set All Flams</b><br>" +
        "Resets all steps to use the default number of flams as shown at left.</html>";
                
    static final String CHOKE_TOOLTIP = "<html><b>Choke</b><br>" +
        "Sets the choke track for this track.  The choke track can be any of the first 16<br>" +
        "tracks in the sequence, or none.<br><br>" +
        "When a step is playing in this track, it will mute any step playing or about to play<br>" +
        "in the choke track.  This is useful for closed high hats to mute open high hats, for example.</html>";
                
    static final String WHEN_TOOLTIP = "<html><b>When</b><br>" +
        "Sets the <i>default</i> pattern value by each step to determine if it should play.<br>" +
        "You can override this pattern value in each step's inspector. There are many options:<br>" +
        "<ul><li>Always play." + 
        "<li>Play with some probability." +
        "<li>Play with the probability <i>1/Tracks</i> where <i>Tracks<i> is the number of tracks." +
        "<li>Play with the probability <i>1/(1-Tracks)</i> where <i>Tracks<i> is the number of tracks." +
        "<li>Play (X) or don't play (O) with a certain repeating pattern." +
        "</ul>" +
        "Regardless of the pattern, a step doesn't play if it hasn't been enabled in the sequencer grid.</html>";
                        
    static final String SET_WHEN_TOOLTIP = "<html><b>Set All When</b><br>" +
        "Resets all tracks to the <b>Track Len</b> value at left.</html>";
        
    static final String SWING_TOOLTIP = "<html><b>Swing</b><br>" +
        "Sets the swing value for the track.  This overrides the default setting<br>" +
        "in the Step Sequence inspector.  To return to the default, double-click the dial.</html>";
        
    static final String VELOCITY_TOOLTIP = "<html><b>Velocity / Value</b><br>" +
        "Sets the <i>default</i> velocity or parameter value for all steps in the track.  This overrides<br>" +
        "the default setting in the Step Sequence inspector.  To return to the default,<br>"+ 
        "double-click the dial.<br><br>" + 
        "You can override both the step sequence default and the track default in the<br>"+
        "individual step's inspector.<br><br>" + 
        "For some parameters types, the velocity/value dial is the MSB of the value, while<br>" +
        "the <b>LSB</b> dial is the LSB of the value.</html>";
        
    static final String SET_VELOCITY_TOOLTIP = "<html><b>Set All Velocity / Value</b><br>" +
        "Resets all steps in the track to use the default velocity or value shown at left<br>" + 
        "instead of custom per-step velocity values.</html>";

    static final String OUT_TOOLTIP = "<html><b>Out</b><br>" +
        "Sets the <i>default</i> MIDI output for the track.  This overrides the default<br>" +
        "setting in the Step Sequence inspector.  To return to the default, set the value<br>" + 
        "to \"<i>default</i>\".</html>";
        
    static final String NAME_TOOLTIP = "<html><b>Name</b><br>" +
        "Sets the name of the Track.  This will appear in the <b>Track Header</b>.</html>";

    static final String VALUE_LSB_TOOLTIP = "<html><b>Value LSB</b><br>" +
        "Sets the LSB of the default value for all notes in this track.</html>";

    static final String PARAMETER_LSB_TOOLTIP = "<html><b>Parameter LSB</b><br>" +
        "Sets the LSB of the default parameter for all notes in this track.</html>";

    static final String EUCLID_TOOLTIP = "<html><b>Euclid</b><br>" +
        "Defines the Euclidean Sequence value for the track.  To set the track to this<br>" +
        "value, and to the rotation, press the <b>S</b> button.</html>";

    static final String ROTATE_TOOLTIP = "<html><b>Rotate</b><br>" +
        "Defines the Euclidean Sequence value for the track.  To set the track to this<br>" +
        "rotation, and to the given Euclidean Sequence value, press the <b>S</b> button.</html>";

    static final String S_TOOLTIP = "<html><b>Set Euclid Sequence</b><br>" +
        "Sets the the track to the given Euclidean Sequence.</html>";

    static final String R_TOOLTIP = "<html><b>Random Euclid Sequence</b><br>" +
        "Sets the the track to a random Euclidean Sequence.</html>";
    }
