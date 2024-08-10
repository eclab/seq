/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.gui;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.border.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.concurrent.locks.*;
import seq.engine.*;


public class TimeDisplay extends JPanel
    {
    SmallDial stepsDial;            // 0...191
    SmallDial beatsDial;            // 0...N, where N is 0...255 and can vary
    SmallDial barsDial;                     // 0...255
    SmallDial partsDial;            // 0...255
    JLabel totalSteps = new JLabel();
    PushButton presets;
    int steps;
    int maxSteps;
    int beats;
    int maxBeats;
    int bars;
    int maxBars;
    int parts;
    int maxParts;
    int beatsPerBar;
    Seq seq;
        
    // returns true if changed
    boolean reviseBeatsPerBar()
        {
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            int val = seq.getBar();
            if (val != beatsPerBar)
                { 
                int time = getTime();
                beatsPerBar = val;
                setTime(time);
                return true; 
                }
            else return false;
            }
        finally { lock.unlock(); }              
        }
                                
    public int getTime()
        {
        return steps + beats * Seq.PPQ + bars * beatsPerBar * Seq.PPQ + parts * Seq.NUM_BARS_PER_PART * beatsPerBar * Seq.PPQ;
        }
                
    /** Override this.  It will be called inside the lock. */
    public void updateTime(int time) { } 
        
    /** Called from inside lock */
    void updateTime()
        {
        updateTime(getTime());
        }
                     
    /** Handles lock internally */
    public void setTime(int time)
        {
        // break out
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            int b = seq.getBar();

            // FIXME: We have to max out the legal time
            int _maxBeats = Math.min(maxBeats, b);
            int maxTime = 
                (maxSteps - 1) + 
                (_maxBeats - 1) * Seq.PPQ + 
                (maxBars - 1) * _maxBeats * Seq.PPQ + 
                (maxParts - 1) * maxBars * _maxBeats * Seq.PPQ;
            time = Math.min(maxTime, time);

            parts = Math.min(maxParts, time/(Seq.PPQ * b * Seq.NUM_BARS_PER_PART));
            time -= (parts * Seq.PPQ * seq.getBar() * Seq.NUM_BARS_PER_PART);
            bars = Math.min(maxBars, time/(Seq.PPQ * b));
            time -= (bars * Seq.PPQ * b);
            beats = Math.min(beatsPerBar, Math.min(maxBeats, time/Seq.PPQ));
            time -= (beats * Seq.PPQ);
            steps = Math.min(maxSteps, time);
            }
        finally { lock.unlock(); }
        SwingUtilities.invokeLater(new Runnable() { public void run() { revise(); } });
        }

    public void revise()
        {
        stepsDial.setValue(steps / (double)maxSteps, false);
        beatsDial.setValue(beats / (double)Math.min(maxBeats, beatsPerBar - 1), false);
        barsDial.setValue(bars / (double)maxBars, false);
        partsDial.setValue(parts / (double)maxParts, false);
        stepsDial.redraw();
        beatsDial.redraw();
        barsDial.redraw();
        partsDial.redraw();
        updateTotalSteps();
        }
                
    public static final String[] PRESET_OPTIONS = 
        { 
        "1/32",
        "1/16",
        "1/8",
        "1/6",
        "1/4",
        "1/3",
        "3/8",
        "1/2",
        "2/3",
        "5/8",
        "3/4",
        "7/8",
        };

    public static final int[] PRESETS = 
        {
        Seq.PPQ / 32, 
        Seq.PPQ / 16,
        Seq.PPQ / 8,
        Seq.PPQ / 6,
        Seq.PPQ / 4,
        Seq.PPQ / 3,
        Seq.PPQ * 3 / 8,
        Seq.PPQ / 2,
        Seq.PPQ * 2 / 3,
        Seq.PPQ * 5 / 8,
        Seq.PPQ * 3 / 4,
        Seq.PPQ * 7 / 8
        };


    public TimeDisplay(int time, int maxSteps, int maxBeats, int maxBars, int maxParts, Seq seq)
        {
        this(time, maxSteps, maxBeats, maxBars, maxParts, seq, true);
        }
        
    public TimeDisplay(int time, Seq seq)
        {
        this(time, seq, true);
        }
                
    public TimeDisplay(Seq seq)
        {
        this(seq, true);
        }
                
    public TimeDisplay(int steps, int beats, int bars, int parts, Seq seq)
        {
        this(steps, beats, bars, parts, seq, true);
        }
    
    public TimeDisplay(int steps, int maxSteps, 
        int beats, int maxBeats, 
        int bars, int maxBars, 
        int parts, int maxParts, Seq seq) { this(steps, maxSteps, beats, maxBeats, bars, maxBars, parts, maxParts, seq, true); }

    public TimeDisplay(int time, int maxSteps, int maxBeats, int maxBars, int maxParts, Seq seq, boolean showPresets)
        {
        this(0, maxSteps, 0, maxBeats, 0, maxBars, 0, maxParts, seq, showPresets);
        setTime(time);
        }
                
    public TimeDisplay(int time, Seq seq, boolean showPresets)
        {
        this(0, 0, 0, 0, seq, showPresets);
        setTime(time);
        }
                
    public TimeDisplay(Seq seq, boolean showPresets)
        {
        this(0, seq, showPresets);
        }
                
    public TimeDisplay(int steps, int beats, int bars, int parts, Seq seq, boolean showPresets)
        {
        this(steps, Seq.PPQ - 1, beats, Seq.MAX_BEATS_PER_BAR - 1, bars, Seq.NUM_BARS_PER_PART - 1, parts, Seq.NUM_PARTS - 1, seq, showPresets);
        }
    
    public TimeDisplay(int steps, int maxSteps, 
        int beats, int maxBeats, 
        int bars, int maxBars, 
        int parts, int maxParts, Seq seq, boolean showPresets)
        {
        this.seq = seq;
        this.maxSteps = maxSteps;
        this.steps = steps;
        this.maxBeats = maxBeats;
        this.beats = beats;
        this.maxBars = maxBars;
        this.bars = bars;
        this.maxParts = maxParts;
        this.parts = parts;

        stepsDial = new SmallDial(steps)
            {
            protected String map(double val) 
                {
                int ppq = (int)(val * maxSteps);
                return String.valueOf(ppq);
                /*
                  if (ppq == 0) return String.valueOf(ppq);
                  else if (ppq % (Seq.PPQ/2) == 0)       // divisible by 2
                  return "[" + String.valueOf(ppq/(Seq.PPQ/2)) + "/2" + "]";
                  else if (ppq % (Seq.PPQ/3) == 0)  // divisible by 3
                  return "[" + String.valueOf(ppq/(Seq.PPQ/3)) + "/3" + "]";
                  else if (ppq % (Seq.PPQ/4) == 0)  // divisible by 4
                  return "[" + String.valueOf(ppq/(Seq.PPQ/4)) + "/4" + "]";
                  else if (ppq % (Seq.PPQ/6) == 0)  // divisible by 6
                  return "[" + String.valueOf(ppq/(Seq.PPQ/6)) + "/6" + "]";
                  else if (ppq % (Seq.PPQ/8) == 0)  // divisible by 8
                  return "[" + String.valueOf(ppq/(Seq.PPQ/8)) + "/8" + "]";
                  else if (ppq % (Seq.PPQ/12) == 0) // divisible by 12
                  return "[" + String.valueOf(ppq/(Seq.PPQ/12)) + "/12" + "]";
                  else if (ppq % (Seq.PPQ/16) == 0) // divisible by 16
                  return "[" + String.valueOf(ppq/(Seq.PPQ/16)) + "/16" + "]";
                  else if (ppq % (Seq.PPQ/24) == 0) // divisible by 24
                  return "[" + String.valueOf(ppq/(Seq.PPQ/24)) + "/24" + "]";
                  else if (ppq % (Seq.PPQ/32) == 0) // divisible by 32
                  return "[" + String.valueOf(ppq/(Seq.PPQ/32)) + "/32" + "]";
                  else if (ppq % (Seq.PPQ/36) == 0) // divisible by 36
                  return "[" + String.valueOf(ppq/(Seq.PPQ/36)) + "/36" + "]";
                  else if (ppq % (Seq.PPQ/48) == 0) // divisible by 48
                  return "[" + String.valueOf(ppq/(Seq.PPQ/48)) + "/48" + "]";
                  else if (ppq % (Seq.PPQ/96) == 0) // divisible by 96
                  return "[" + String.valueOf(ppq/(Seq.PPQ/96)) + "/96" + "]";
                  else return String.valueOf(ppq) + "/" + Seq.PPQ;
                */
                }
                                
            public double getValue() 
                { 
                reviseBeatsPerBar();
                ReentrantLock lock = seq.getLock();
                lock.lock();
                try { return TimeDisplay.this.steps / (double)maxSteps; }
                finally { lock.unlock(); }
                }
            public void setValue(double val) 
                { 
                if (seq == null) return;
                reviseBeatsPerBar();
                ReentrantLock lock = seq.getLock();
                lock.lock();
                try { TimeDisplay.this.steps = (int)(val * maxSteps); updateTime(); }
                finally { lock.unlock(); }
                updateTotalSteps();
                }
            };

        beatsDial = new SmallDial(beats)
            {
            protected String map(double val) { return String.valueOf((int)(val * Math.min(maxBeats, beatsPerBar - 1))); }
            public double getValue() 
                { 
                reviseBeatsPerBar();
                ReentrantLock lock = seq.getLock();
                lock.lock();
                try { return TimeDisplay.this.beats / (double)Math.min(maxBeats, beatsPerBar - 1); }
                finally { lock.unlock(); }
                }
            public void setValue(double val) 
                { 
                if (seq == null) return;
                reviseBeatsPerBar();
                ReentrantLock lock = seq.getLock();
                lock.lock();
                try { TimeDisplay.this.beats = (int)(val * Math.min(maxBeats, beatsPerBar - 1));  updateTime(); }
                finally { lock.unlock(); }
                updateTotalSteps();
                }
            };
                
        barsDial = new SmallDial(bars)
            {
            protected String map(double val) { return String.valueOf((int)(val * maxBars)); }
            public double getValue() 
                { 
                reviseBeatsPerBar();
                ReentrantLock lock = seq.getLock();
                lock.lock();
                try { return TimeDisplay.this.bars / (double)maxBars; }
                finally { lock.unlock(); }
                }
            public void setValue(double val) 
                { 
                if (seq == null) return;
                reviseBeatsPerBar();
                ReentrantLock lock = seq.getLock();
                lock.lock();
                try { TimeDisplay.this.bars = (int)(val * maxBars);  updateTime(); }
                finally { lock.unlock(); }
                updateTotalSteps();
                }
            };

        partsDial = new SmallDial(parts)
            {
            protected String map(double val) { return String.valueOf((int)(val *  maxParts)); }
            public double getValue() 
                { 
                reviseBeatsPerBar();
                ReentrantLock lock = seq.getLock();
                lock.lock();
                try { return TimeDisplay.this.parts / (double)maxParts; }
                finally { lock.unlock(); }
                }
            public void setValue(double val) 
                { 
                if (seq == null) return;
                reviseBeatsPerBar();
                ReentrantLock lock = seq.getLock();
                lock.lock();
                try { TimeDisplay.this.parts = (int)(val * maxParts);  updateTime(); }
                finally { lock.unlock(); }
                updateTotalSteps();
                }
            };
                        
        Box box = new Box(BoxLayout.X_AXIS);
        if (maxParts > 0) box.add(partsDial.getLabelledTitledDialVertical("Part", " 888 "));
        if (maxBars > 0) box.add(barsDial.getLabelledTitledDialVertical("Bar", " 888 "));
        if (maxBeats > 0) box.add(beatsDial.getLabelledTitledDialVertical("Beat", " 888 "));
        if (maxSteps > 0) box.add(stepsDial.getLabelledTitledDialVertical("Tick", " 888 "));
        setLayout(new BorderLayout());
                
        presets = new PushButton("Presets", PRESET_OPTIONS)
            {
            public void perform(int val)
                {
                stepsDial.setValue(Math.min(maxSteps, PRESETS[val]) / (double)maxSteps);
                stepsDial.redraw();
                }
            };
        add(box, BorderLayout.WEST);
        //add(totalSteps, BorderLayout.CENTER);
        JPanel pan = new JPanel();
        pan.setLayout(new BorderLayout());
        pan.add(Stretch.makeHorizontalStretch(), BorderLayout.CENTER);
        
        if (showPresets)
            {
            JPanel vert = new JPanel();
            vert.setLayout(new BorderLayout());
            JLabel temp = new JLabel(" ");
            temp.setFont(SmallDial.FONT);
            vert.add(temp, BorderLayout.NORTH);
            vert.add(presets, BorderLayout.CENTER);
            temp = new JLabel(" ");
            temp.setFont(SmallDial.FONT);
            vert.add(temp, BorderLayout.SOUTH);
            pan.add(vert, BorderLayout.EAST);
            }
        add(pan, BorderLayout.EAST);
        }
        
    void updateTotalSteps()
        {
        totalSteps.setText(" " + getTime() + " ");
        }
    }
