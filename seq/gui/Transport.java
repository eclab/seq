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
    
    JButton playButton;
    JButton stopButton;
    JButton pauseButton;
    JButton recordButton;
    JToggleButton loopButton;
    Box playBox;
    Seq seq;
    SeqUI sequi;
    SmallDial beatsPerBar;
    SmallDial bpm;
    SmallDial beepVolume;
    JComboBox countIn;
    JCheckBox metronome;
    JComboBox clock;
    WidgetList options;
    JLabel time;
    
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

    public static ImageIcon buildIcon(String file)
        {
        ImageIcon original = new ImageIcon(Transport.class.getResource(file));
        return new ImageIcon(original.getImage().getScaledInstance(ICON_WIDTH, ICON_WIDTH, java.awt.Image.SCALE_SMOOTH));
        }
        
    public Transport(Seq seq, SeqUI sequi)
        {
        this.seq = seq;
        this.sequi = sequi;
                
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
        playBox = new Box(BoxLayout.X_AXIS);
        playBox.add(recordButton);
        playBox.add(playButton);
        playBox.add(pauseButton);
        playBox.add(stopButton);
        playBox.add(loopButton);
        setLayout(new BorderLayout());
        add(playBox, BorderLayout.NORTH);
        
        time = new JLabel("0:0:0:0");
        add(time, BorderLayout.CENTER);

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

        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            seq.addListener(this);

            bpm = new SmallDial((Math.min(299, seq.getBPM() - 1) / 299.0))
                {
                protected String map(double val) { return String.valueOf((int)(val * 299) + 1) + " BPM"; }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return (Math.min(299, seq.getBPM() - 1) / 299.0); }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { seq.setBPM((int)(val * 299) + 1); }
                    finally { lock.unlock(); }
                    }
                };
                
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

            }
        finally
            {
            lock.unlock();
            }
        
        options = new WidgetList(new String[] { "Tempo", "Clock", "Beats/Bar", "Count-In", "Metronome", "Beep Vol." }, 
            new JComponent[] 
                { 
                bpm.getLabelledDial("299 BPM"), 
                clock,
                beatsPerBar.getLabelledDial("16"),
                countIn,
                metronome,
                beepVolume.getLabelledDial("0.000"),
                });
                        
        DisclosurePanel dp = new DisclosurePanel("Clock Options", options);
        add(dp, BorderLayout.SOUTH);
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
        int ticks = val % Seq.PPQ;
        if (val < 0) val = 0;
        int beats = val % (Seq.PPQ * beatsPerBar) / Seq.PPQ;
        if (val < 0) val = 0;
        int bars = val % (Seq.PPQ * beatsPerBar * Seq.NUM_BARS_PER_PART) / (Seq.PPQ * beatsPerBar);
        if (val <= 0) val = 0;
        int parts = val / (Seq.PPQ * beatsPerBar * Seq.NUM_BARS_PER_PART);
        
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
        sequi.redraw(true);             // force a redraw
        }

    public void doPause()
        {
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            if (seq.isRecording()) return;                      // should never happen
            
            if (seq.isStopped())
                {
                seq.play();
                seq.pause();
                playButton.setIcon(notPlaying);
                pauseButton.setIcon(paused);
                stopButton.setIcon(notStopped);
                }
            else
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

    }
