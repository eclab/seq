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
    SmallDial euclidK;
	double euclidKVal = 0.0;
    SmallDial euclidRotate;
	double euclidRotateVal = 0.0;
    PushButton doEuclid;

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
            trackExclusive.setToolTipText(EXCLUSIVE_RANDOM_TOOLTIP);

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
            trackNote.setToolTipText(NOTE_TOOLTIP);

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
                    ssui.tracks.get(trackNum).updateLength();
                    ssui.updateSizes();
                    ssui.redraw(false);
                    }
                };
            setNumSteps.setToolTipText(SET_LENGTH_TOOLTIP);


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
                    ssui.redraw(false);
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
                        for(int i = 0; i < len; i++) { ss.setFlam(trackNum, i, StepSequence.DEFAULT); }
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
                        for(int i = 0; i < len; i++) { ss.setWhen(trackNum, i, StepSequence.DEFAULT); }
                        }
                    finally { lock.unlock(); }
                    ssui.getStepInspector().revise();
                    }
                };
            setTrackWhen.setToolTipText(SET_WHEN_TOOLTIP);

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
            trackSwing.setToolTipText(SWING_TOOLTIP);


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
                    ssui.redraw(false);
                    }
                };
            trackVelocity.setToolTipText(VELOCITY_TOOLTIP);
                
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
                
            euclidRotate = new SmallDial(euclidRotateVal)
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
                    return euclidRotateVal;
                    }
                public void setValue(double val) 
                    { 
                    euclidRotateVal = val;
                    }
                };

            doEuclid = new PushButton("Set")
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
                    ssui.getTrack(trackNum).repaint();		// is this sufficient?
                    }
                };
            }
        finally { lock.unlock(); }
                
        JPanel lengthPanel = new JPanel();
        lengthPanel.setLayout(new BorderLayout());
        lengthPanel.add(numSteps.getLabelledDial("128"), BorderLayout.WEST);
        lengthPanel.add(setNumSteps, BorderLayout.EAST);
        lengthPanel.setToolTipText(LENGTH_TOOLTIP);

        JPanel notePanel = new JPanel();
        notePanel.setLayout(new BorderLayout());
        notePanel.add(trackNote.getLabelledDial("C#-2"), BorderLayout.WEST);
        notePanel.add(setTrackNote, BorderLayout.EAST);
        notePanel.setToolTipText(NOTE_TOOLTIP);


        JPanel velocityPanel = new JPanel();
        velocityPanel.setLayout(new BorderLayout());
        velocityPanel.add(trackVelocity.getLabelledDial("<html><i>Default</i></html>"), BorderLayout.WEST);
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
        euclidBox.add(new JLabel("  Rotate  "));
        euclidBox.add(euclidRotate.getLabelledDial("128"));
        euclidPanel.add(euclidBox, BorderLayout.WEST);
        euclidPanel.add(doEuclid, BorderLayout.EAST);
        
        build(new String[] { "Name", "Gain", "Note", "   Velocity", "Flams", "When", "Choke", "Swing", "Ex. Rand.", "Out", "Length", "Euclid" }, 
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

    /*** Tooltips ***/
        
    static final String EXCLUSIVE_RANDOM_TOOLTIP = "<html><b>Exclusive Random</b><br>" +
        "Sets whether the track is part of the exclusive random group.<br><br>" +
        "Only one track in the exclusive random group is played each iteration.<br>" +
        "The others are muted.</html>";
        
    static final String NOTE_TOOLTIP = "<html><b>Note</b><br>" +
        "Sets the <i>default</i> MIDI note output for all steps in the track.<br>" +
        "You can override this value in the individual step's inspector.</html>";
                        
    static final String SET_NOTE_TOOLTIP = "<html><b>Set All Note</b><br>" +
        "Resets all steps to use the MIDI output note as shown at left.</html>";
        
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
        
    static final String VELOCITY_TOOLTIP = "<html><b>Velocity</b><br>" +
        "Sets the <i>default</i> velocity value for all steps in the track.  This overrides<br>" +
        "the default setting in the Step Sequence inspector.  To return to the default,<br>"+ 
        "double-click the dial.<br><br>" + 
        "You can override both the step sequence default and the track default in the<br>"+
        "individual step's inspector.</html>";
        
    static final String SET_VELOCITY_TOOLTIP = "<html><b>Set All Velocity</b><br>" +
        "Resets all steps in the track to use the default velocity value shown at left<br>" + 
        "instead of custom per-step velocity values.</html>";

    static final String OUT_TOOLTIP = "<html><b>Out</b><br>" +
        "Sets the <i>default</i> MIDI output for the track.  This overrides the default<br>" +
        "setting in the Step Sequence inspector.  To return to the default, set the value<br>" + 
        "to \"<i>default</i>\".</html>";
        
    static final String NAME_TOOLTIP = "<html><b>Name</b><br>" +
        "Sets the name of the Track.  This will appear in the <b>Track Header</b>.</html>";
    }
