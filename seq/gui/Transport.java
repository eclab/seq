/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.gui;

import seq.engine.*;
import java.awt.*;
import javax.swing.border.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.concurrent.locks.*;
import java.util.*;
import com.formdev.flatlaf.*;

public class Transport extends JPanel implements SeqListener
    {
    public static final int ICON_WIDTH = 20;
    public static final int INSET_SIZE = 5;
    public static final int MIDI_IN_DISPLAY_TIME = 100;
    
    JButton playButton;
    JButton stopButton;
    JButton pauseButton;
    JButton recordButton;
    JToggleButton loopButton;
    JLabel midiInLabel;
    javax.swing.Timer midiInTimer;
    Box playBox;
    Seq seq;
    SeqUI sequi;
    SmallDial beatsPerBar;
    SmallDial bpm;
    SmallDial beepVolume;
    SmallDial beepPitch;
    JComboBox countIn;
    JCheckBox metronome;
    JComboBox clock;
    WidgetList options;
    JLabel time;
    DisclosurePanel clockOptions;
    
    public static final String[] CLOCK_STRINGS = { "Internal", "Emit Clock" };          // FIXME: need to add  "External" };
    public static final String[] COUNT_IN_STRINGS = { "None", "Record", "Rec/Play" };
                
    static ImageIcon playing = buildIcon("icons/playing2.png");
    static ImageIcon notPlaying = buildIcon("icons/notplaying2.png");
    static ImageIcon stopped = buildIcon("icons/stopped2.png");
    static ImageIcon notStopped = buildIcon("icons/notstopped2.png");
    static ImageIcon paused = buildIcon("icons/paused2.png");
    static ImageIcon notPaused = buildIcon("icons/notpaused2.png");
    static ImageIcon recording = buildIcon("icons/recording2.png");
    static ImageIcon notRecording = buildIcon("icons/notrecording2.png");
    static ImageIcon releasing = buildIcon("icons/releasing2.png");
    static ImageIcon looping = buildIcon("icons/loopon.png");
    static ImageIcon notLooping = buildIcon("icons/loop.png");
    static ImageIcon midiIn = buildIcon("icons/MidiIn.png");
    static ImageIcon midiInOff = buildIcon("icons/MidiInOff.png");

    public static final String[] BEEP_PITCHES = { "A 220", "-Bb", "-B", "-C", "-Db", "-D", "-Eb", "-E", "-F", "-Gb", "-G", "-Ab", "A 440", "+Bb", "+B", "+C", "+Db", "+D", "+Eb", "+E", "+F", "+Gb", "+G", "+Ab", "A 880" };

    public static ImageIcon buildIcon(String file)
        {
        ImageIcon original = new ImageIcon(Transport.class.getResource(file));
        return new ImageIcon(original.getImage().getScaledInstance(ICON_WIDTH, ICON_WIDTH, java.awt.Image.SCALE_SMOOTH));
        }
        
    public Transport(Seq seq, SeqUI sequi)
        {
        this.seq = seq;
        this.sequi = sequi;
        rebuildTransport();
        
        // Tooltips
        playButton.setToolTipText(PLAY_BUTTON_TOOLTIP);
        pauseButton.setToolTipText(PAUSE_BUTTON_TOOLTIP);
        stopButton.setToolTipText(STOP_BUTTON_TOOLTIP);
        recordButton.setToolTipText(RECORD_BUTTON_TOOLTIP);
        loopButton.setToolTipText(LOOP_BUTTON_TOOLTIP);
        midiInLabel.setToolTipText(MIDI_IN_TOOLTIP);
        time.setToolTipText(TIME_TOOLTIP);
        clockOptions.setToolTipText(CLOCK_OPTIONS_TOOLTIP);
        beatsPerBar.setToolTipText(BEATS_PER_BAR_TOOLTIP);
        bpm.setToolTipText(BPM_TOOLTIP);
        beepVolume.setToolTipText(BEEP_VOLUME_TOOLTIP);
        beepPitch.setToolTipText(BEEP_PITCH_TOOLTIP);
        countIn.setToolTipText(COUNT_IN_TOOLTIP);
        metronome.setToolTipText(METRONOME_TOOLTIP);
        clock.setToolTipText(CLOCK_TOOLTIP);
        options.updateToolTips();               // update the labels
        }
    
    public void rebuildTransport()
        {
        removeAll();
        
        playButton = new JButton(notPlaying); 
        playButton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doPlay(); } });
        playButton.setSelectedIcon(playing);
        playButton.setMargin(new Insets(INSET_SIZE, INSET_SIZE, INSET_SIZE, INSET_SIZE));
        stopButton = new JButton(stopped); 
        stopButton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doStop(); } });
        stopButton.setSelectedIcon(stopped);
        stopButton.setMargin(new Insets(INSET_SIZE, INSET_SIZE, INSET_SIZE, INSET_SIZE));
        pauseButton = new JButton(notPaused); 
        pauseButton.setSelectedIcon(paused);
        pauseButton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doPause(); } });
        pauseButton.setMargin(new Insets(INSET_SIZE, INSET_SIZE, INSET_SIZE, INSET_SIZE));
        recordButton = new JButton(notRecording); 
        recordButton.setSelectedIcon(recording);
        recordButton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doRecord(); } });
        recordButton.setMargin(new Insets(INSET_SIZE, INSET_SIZE, INSET_SIZE, INSET_SIZE));
        loopButton = new JToggleButton(notLooping); 
        loopButton.setSelectedIcon(looping);
        loopButton.setMargin(new Insets(INSET_SIZE, INSET_SIZE, INSET_SIZE, INSET_SIZE));
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try { loopButton.setSelected(seq.isLooping()); }
        finally { lock.unlock(); }

        midiInLabel = new JLabel();
        midiInLabel.setIcon(midiInOff);
        playBox = new Box(BoxLayout.X_AXIS);
        playBox.add(recordButton);
        playBox.add(playButton);
        playBox.add(pauseButton);
        playBox.add(stopButton);
        playBox.add(loopButton);
        setLayout(new BorderLayout());
        add(playBox, BorderLayout.NORTH);
        
        JPanel pane = new JPanel();
        pane.setLayout(new BorderLayout());
        time = new JLabel("0:0:0:0");
        pane.add(midiInLabel, BorderLayout.WEST);
        pane.add(time, BorderLayout.CENTER);
        add(pane, BorderLayout.CENTER);
                
        loopButton.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e) 
                { 
                ReentrantLock lock = seq.getLock();
                lock.lock();
                try { seq.setLooping(loopButton.isSelected()); }
                finally { lock.unlock(); }
                }
            }); 

        lock = seq.getLock();
        lock.lock();
        try
            {
            seq.addListener(this);

            final int bpmL = Seq.MAX_BPM - 1;
            bpm = new SmallDial((Math.min(bpmL, seq.getBPM() - 1) / (double)bpmL))
                {
                protected String map(double val) { return String.valueOf((int)(val * bpmL) + 1) + " BPM"; }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return (Math.min(bpmL, seq.getBPM() - 1) / (double)bpmL); }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { seq.setBPM((int)(val * bpmL) + 1); }
                    finally { lock.unlock(); }
                    }
                };
            bpm.setScale(480.0);                // Increase the scale for our tempo range
                
            clock = new JComboBox(CLOCK_STRINGS);
            clock.setSelectedIndex(seq.getClock());
            clock.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { seq.setClock(clock.getSelectedIndex()); }
                    finally { lock.unlock(); }                              
                    }
                });

            beatsPerBar = new SmallDial((seq.getBar() - 1) / 15.0)
                {
                protected String map(double val) { return String.valueOf((int)(val * 15) + 1); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return ((seq.getBar() - 1) / 15.0); }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { seq.setBar((int)(val * 15.0) + 1); }
                    finally { lock.unlock(); }
                    SwingUtilities.invokeLater(new Runnable()
                        {
                        public void run()
                            {
                            // Update the motif
                            MotifUI motifui = sequi.getMotifUI();
                            if (motifui != null) motifui.redraw(false);
                            }
                        });
                    }
                };
                
            countIn = new JComboBox(COUNT_IN_STRINGS);
            countIn.setSelectedIndex(seq.getCountInMode());
            countIn.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { seq.setCountInMode(countIn.getSelectedIndex()); }
                    finally { lock.unlock(); }                              
                    }
                });

            metronome = new JCheckBox();
            metronome.setSelected(seq.getMetronome());
            metronome.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { seq.setMetronome(metronome.isSelected()); }
                    finally { lock.unlock(); }                              
                    }
                });

            beepVolume = new SmallDial(seq.getBeepVolume())
                {
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return seq.getBeepVolume(); }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { seq.setBeepVolume(val); }
                    finally { lock.unlock(); }
                    }
                };
                
            beepPitch = new SmallDial((seq.getBeepPitch() + 12) / 24.0)
                {
                protected String map(double val) 
                    {
                    return BEEP_PITCHES[(int)(val * 24.0)];
                    }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return (seq.getBeepPitch() + 12) / 24.0; }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { seq.setBeepPitch((int)(val * 24.0) - 12); }
                    finally { lock.unlock(); }
                    }
                };

            }
        finally
            {
            lock.unlock();
            }
        
        options = new WidgetList(new String[] { "Tempo", "Clock", "Beats/Bar", "Count-In", "Metronome", "Beep Vol.", "Beep Pitch" }, 
            new JComponent[] 
                { 
                bpm.getLabelledDial("bpmL BPM"), 
                clock,
                beatsPerBar.getLabelledDial("16"),
                countIn,
                metronome,
                beepVolume.getLabelledDial("0.000"),
                beepPitch.getLabelledDial("A 440"),
                });
                        
        clockOptions = new DisclosurePanel("Clock Options", options);
        add(clockOptions, BorderLayout.SOUTH);
        
        midiInTimer = new javax.swing.Timer(MIDI_IN_DISPLAY_TIME, new ActionListener()
            {
            public void actionPerformed(ActionEvent e) { midiInLabel.setIcon(midiInOff); midiInTimer.stop(); }
            });
        } 
        
    public void updateClock(int val)
        {
        if (seq == null) return;
        ReentrantLock lock = seq.getLock();
        lock.lock();
        int beatsPerBar = 0;
        try { beatsPerBar = seq.getBar(); }
        finally { lock.unlock(); }
                
        if (val <= 0) val = 0;
        int ticks = 1 + val % Seq.PPQ;
        if (val < 0) val = 0;
        int beats = 1 + val % (Seq.PPQ * beatsPerBar) / Seq.PPQ;
        if (val < 0) val = 0;
        int bars = 1 + val % (Seq.PPQ * beatsPerBar * Seq.NUM_BARS_PER_PART) / (Seq.PPQ * beatsPerBar);
        if (val <= 0) val = 0;
        int parts = 1 + val / (Seq.PPQ * beatsPerBar * Seq.NUM_BARS_PER_PART);
        
        time.setText("  " + parts + "  :  " + bars + "  :  " + beats + "  :  " + ticks);
        }
                
    public void stateChanged(Seq seq)       
        {
        if (seq == null) return;
        boolean _playing;
        boolean _stopped;
        boolean _recording;
        boolean _paused;
        boolean _releasing;
                
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            _playing = seq.isPlaying();
            _stopped = seq.isStopped();
            _recording = seq.isRecording();
            _paused = seq.isPaused();
            _releasing = seq.isReleasing();
            }
        finally { lock.unlock(); }
                
        recordButton.setIcon(_recording ? recording : notRecording);
        stopButton.setIcon(_releasing ? releasing : (_stopped ? stopped : notStopped));
        playButton.setIcon(_playing ? playing : notPlaying);
        pauseButton.setIcon(_paused ? paused : notPaused);
        loopButton.setEnabled(_stopped || !_recording);
        recordButton.setEnabled(!_paused && ((_playing && _recording) || _stopped));
        }
        
    public void doRecord()
        {
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            if (seq.isPlaying()) return;
            if (seq.isPaused()) return;                 // should never happen
            if (seq.isRecording()) return;              // should never happen
            
            if (seq.isStopped())                // we can just start recording
                {
                seq.setRecording(true);
                recordButton.setIcon(recording);
                pauseButton.setEnabled(false);
                loopButton.setEnabled(false);
                seq.setLooping(false);          // for the moment
                doPlay();
                }
            }
        finally
            {
            lock.unlock();
            }
        }
        
    public void setCountIn(int val, boolean isRecording)
        {
        if (val < 0)
            {
            recordButton.setIcon(notRecording);
            recordButton.setText(null);
            playButton.setText(null);
            }
        else if (val == 0) 
            {
            recordButton.setIcon(recording);
            recordButton.setText(null);
            playButton.setText(null);
            }
        else
            {
            recordButton.setIcon(recording);
            if (isRecording)
                {
                recordButton.setText(String.valueOf(val));
                playButton.setText(null);
                }
            else
                {
                playButton.setText(String.valueOf(val));
                recordButton.setText(null);
                }
            }
        }

    public void doPlay()
        {
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            seq.play();
            }
        finally
            {
            lock.unlock();
            }
        recordButton.setEnabled(false);
        recordButton.setText(null);
        playButton.setIcon(playing);
        pauseButton.setIcon(notPaused);
        stopButton.setIcon(notStopped);
        }

    public void doStop()
        {
        loopButton.setEnabled(true);

        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            seq.stop();
            seq.setLooping(loopButton.isSelected());
            }
        finally
            {
            lock.unlock();
            }
        playButton.setIcon(notPlaying);
        pauseButton.setIcon(notPaused);
        pauseButton.setEnabled(true);
        stopButton.setIcon(stopped);
        recordButton.setIcon(notRecording);
        recordButton.setText(null);
        playButton.setText(null);
        recordButton.setEnabled(true);
        sequi.redraw(false);             // force a redraw
        }

    public void doPause()
        {
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            if (seq.isRecording()) return;                      // should never happen
            if (seq.isStopped()) return;                                                // should never happen
            /*
              if (seq.isStopped())
              {
              seq.play();
              seq.pause();
              playButton.setIcon(notPlaying);
              pauseButton.setIcon(paused);
              stopButton.setIcon(notStopped);
              }
              else
            */
                {
                if (seq.isPlaying())
                    {
                    seq.pause();
                    playButton.setIcon(notPlaying);
                    pauseButton.setIcon(paused);
                    stopButton.setIcon(notStopped);         // probably already this
                    }
                else
                    {
                    seq.play();
                    playButton.setIcon(playing);
                    pauseButton.setIcon(notPaused);
                    stopButton.setIcon(notStopped);         // probably already this
                    }
                }
            recordButton.setEnabled(false);
            recordButton.setText(null);
            playButton.setText(null);
            }
        finally
            {
            lock.unlock();
            }
        }

    public void fireMIDIIn()
        {
        midiInLabel.setIcon(midiIn);
        midiInTimer.stop();
        midiInTimer.start();
        }


    /*** Tooltips ***/
        
    static final String PLAY_BUTTON_TOOLTIP = "<html><b>Play</b><br>" +
        "Starts playing the sequencer.<br><br>If the <b>Count-In</b> is set to <b>Record/Play</b> in the <b>Clock Options</b>,<br>then the metronome will provide a one-bar count-in before playing.</html>";
        
    static final String PAUSE_BUTTON_TOOLTIP = "<html><b>Pause</b><br>" +
        "Pauses or unpauses the sequencer while it's playing.</html>";
        
    static final String STOP_BUTTON_TOOLTIP = "<html><b>Stop</b><br>" +
        "Stops playing the sequencer.</html>";
        
    static final String RECORD_BUTTON_TOOLTIP = "<html><b>Record</b><br>" +
        "Starts the sequencer playing in record mode.<br><br>If the <b>Count-In</b> is set to <b>Record</b> or <b>Record/Play</b> in the <b>Clock Options</b>,<br>then the metronome will provide a one-bar count-in before playing.</html>";
        
    static final String LOOP_BUTTON_TOOLTIP = "<html><b>Loop</b><br>" +
        "When <b>off</b>, playing will stop at the end of the sequence.<br>When <b>on</b>, playing will instead loop back to the start of the sequence and continue.</html>";
        
    static final String MIDI_IN_TOOLTIP = "<html><b>MIDI IN Indicator</b><br>" +
        "Flashes when incoming MIDI data is detected.</html>";
        
    static final String TIME_TOOLTIP = "<html><b>Time</b><br>" +
        "The current play time of the sequencer.<br>Play time is displayed in <i>Parts</i>&nbsp;:&nbsp;<i>Bars</i>&nbsp;:&nbsp;<i>Beats</i>&nbsp;:&nbsp;<i>Ticks</i>." +
        "<ul><li>There are " + Seq.PPQ + " Ticks per Beat." +
        "<li>You can set the number of <b>Beats per Bar</b> in the Clock Options." + 
        "<li>There are " + Seq.NUM_BARS_PER_PART + " Bars per Part." +
        "<li>There are up to 256 Parts." +
        "</ul></html>";

    static final String CLOCK_OPTIONS_TOOLTIP = "<html><b>Clock Options</b><br>" +
        "Additional options for controlling or displaying the clock.</html>";
        
    static final String BEATS_PER_BAR_TOOLTIP = "<html><b>Beats Per Bar</b><br>" +
        "The number of beats (quarter notes) per bar (measure) in this sequence.</html>";

    static final String BPM_TOOLTIP = "<html><b>Tempo</b><br>" +
        "The tempo of the song, measured in <b>Beats Per Minute (BPM).</html>";
                
    static final String BEEP_VOLUME_TOOLTIP = "<html><b>Beep Volume</b><br>" +
        "The volume of the beeps used in the <b>Metronome</b> and the <b>Count-In</b>.</html>";

    static final String BEEP_PITCH_TOOLTIP = "<html><b>Beep Pitch</b><br>" +
        "The pitch of the beeps used in the <b>Metronome</b> and the <b>Count-In</b>.</html>";

    static final String COUNT_IN_TOOLTIP = "<html><b>Count-In</b><br>" +
        "When should the sequencer provide a one-bar count-in prior to playing or recording?" +
        "<ul><li>Never" +
        "<li>When Recording Only" + 
        "<li>When Recording or Playing" +
        "</ul></html>";

    static final String METRONOME_TOOLTIP = "<html><b>Metronome</b><br>" +
        "Specifies whether the sequencer provide one beep per bar while playing or recording the song.</html>";
                
    static final String CLOCK_TOOLTIP = "<html><b>Clock</b><br>" +
        "Where does control of the tempo, as well as play, pause, and stop controls, come from?"+
        "<ul><li>Internal: specified here" +
        "<li>Emit clock: internal (specified here), and also emitted via MIDI" + 
        "</ul></html>";

    }
