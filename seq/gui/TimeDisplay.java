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


public abstract class TimeDisplay extends JPanel
    {
    SmallDial stepsDial;            // 0...191
    SmallDial beatsDial;            // 0...N, where N is 0...255 and can vary
    SmallDial barsDial;                     // 0...255
    SmallDial partsDial;            // 0...255
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
    boolean displaysTime = false;
    boolean initializing;
    
    public boolean getDisplaysTime() { return displaysTime; }
    public void setDisplaysTime(boolean val) { displaysTime = val; revise(); }
        
    public void setToolTipText(String text) 
        {
        super.setToolTipText(text);
        stepsDial.setToolTipText(text);
        beatsDial.setToolTipText(text);
        barsDial.setToolTipText(text);
        partsDial.setToolTipText(text);
        presets.setToolTipText(text);
        }
        

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
                int time = getCurrentTime();
                beatsPerBar = val;
                revise();
                return true; 
                }
            else return false;
            }
        finally { lock.unlock(); }              
        }
                    
    int getCurrentTime()
        {
        return steps + beats * Seq.PPQ + bars * beatsPerBar * Seq.PPQ + parts * Seq.NUM_BARS_PER_PART * beatsPerBar * Seq.PPQ;
        }
        
    /** Override this.  It will be called inside the lock. */
    abstract protected int getTime();
        
    /** Override this.  It will be called inside the lock. */
    abstract protected void setTime(int time);
          
    /** Optionally override this.  It will be called OUTSIDE the lock after setTime is called.
        Also note that this method will NOT be called when initializing the timeDisplay, though
        setTime(...) WILL be called. */
    protected void setTimeOutside(int time) { }
          
    /** Changes the underlying time to the time set on the Time Display */
    public void reviseTime()
        {
        int time = getCurrentTime();
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            setTime(time);
            }
        finally { lock.unlock(); }
        
        /// This is a hack which prevents constant calls to setTimeOutside when setting up
        if (!initializing)
            {
            setTimeOutside(time);
            }
        }

    // Changes the Time Display to the underlying time.  Called by revise()
    void updateTime()
        {
        int time = 0;
        int bar = 0;
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            time = getTime();
            bar = seq.getBar();
            }
        finally { lock.unlock(); }

        // FIXME: We have to max out the legal time
        int _maxBeats = Math.min(maxBeats, bar);
        int maxTime = 
            (maxSteps - 1) + 
            (_maxBeats - 1) * Seq.PPQ + 
            (maxBars - 1) * _maxBeats * Seq.PPQ + 
            (maxParts - 1) * maxBars * _maxBeats * Seq.PPQ;
        time = Math.min(maxTime, time);

        parts = Math.min(maxParts, time/(Seq.PPQ * bar * Seq.NUM_BARS_PER_PART));
        time -= (parts * Seq.PPQ * seq.getBar() * Seq.NUM_BARS_PER_PART);
        bars = Math.min(maxBars, time/(Seq.PPQ * bar));
        time -= (bars * Seq.PPQ * bar);
        beats = Math.min(beatsPerBar, Math.min(maxBeats, time/Seq.PPQ));
        time -= (beats * Seq.PPQ);
        steps = Math.min(maxSteps, time);
        }

    void revise(boolean update)
        {
        if (stepsDial == null) return;          // we're not quite ready yet
                
        if (update) updateTime();
        
        stepsDial.setValue(steps / (double)maxSteps, false);
        beatsDial.setValue(beats / (double)Math.min(maxBeats, beatsPerBar - 1), false);
        barsDial.setValue(bars / (double)maxBars, false);
        partsDial.setValue(parts / (double)maxParts, false);
        stepsDial.redraw();
        beatsDial.redraw();
        barsDial.redraw();
        partsDial.redraw();
        }

    public void revise()
        {
        revise(true);
        }
                
    public static final String[] PRESET_OPTIONS = 
        { 
        "  1/16",
        "  2/16   1/8",
        "  3/16",
        "  4/16   2/8   1/4",
        "  5/16",
        "  6/16   3/8",
        "  7/16",
        "  8/16   4/8   2/4   1/2",
        "  9/16",
        "10/16   5/8",
        "11/16",
        "12/16   6/8   3/4",
        "13/16",
        "14/16   7/8",
        "15/16",
        "  1/6",
        "  2/6    1/3",
        "  3/6    1/2",
        "  4/6    2/3",
        "  5/6",
        };

    public static final int[] PRESETS = 
        {
        Seq.PPQ / 16 * 1,
        Seq.PPQ / 16 * 2,
        Seq.PPQ / 16 * 3,
        Seq.PPQ / 16 * 4,
        Seq.PPQ / 16 * 5,
        Seq.PPQ / 16 * 6,
        Seq.PPQ / 16 * 7,
        Seq.PPQ / 16 * 8,
        Seq.PPQ / 16 * 9,
        Seq.PPQ / 16 * 10,
        Seq.PPQ / 16 * 11,
        Seq.PPQ / 16 * 12,
        Seq.PPQ / 16 * 13,
        Seq.PPQ / 16 * 14,
        Seq.PPQ / 16 * 15,
        Seq.PPQ / 6 * 1,
        Seq.PPQ / 6 * 2,
        Seq.PPQ / 6 * 3,
        Seq.PPQ / 6 * 4,
        Seq.PPQ / 6 * 5,
        };


    public TimeDisplay(int time, int maxSteps, int maxBeats, int maxBars, int maxParts, Seq seq)
        {
        this(time, maxSteps, maxBeats, maxBars, maxParts, seq, true);
        }
        
    public TimeDisplay(int time, int maxSteps, int maxBeats, int maxBars, int maxParts, Seq seq, boolean showPresets)
        {
        this(0, maxSteps, 0, maxBeats, 0, maxBars, 0, maxParts, seq, showPresets);
        initializing = true;
        setTime(time);
        revise(true);
        initializing = false;
        }
                
    public TimeDisplay(int time, Seq seq)
        {
        this(time, seq, true);
        }
                
    public TimeDisplay(int time, Seq seq, boolean showPresets)
        {
        this(0, 0, 0, 0, seq, showPresets);
        initializing = true;
        setTime(time);
        revise(true);
        initializing = false;
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
        int parts, int maxParts, Seq seq) 
        { 
        this(steps, maxSteps, beats, maxBeats, bars, maxBars, parts, maxParts, seq, true); 
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
        initializing = true;
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

                if (ppq == 12)  return "1|16";
                else if  (ppq == 24)    return "1|8";
                else if  (ppq == 32)    return "1|6";
                else if  (ppq == 36)    return "3|16";
                else if  (ppq == 48)    return "1|4";
                else if  (ppq == 64)    return "1|3";
                else if  (ppq == 60)    return "5|16";
                else if  (ppq == 72)    return "3|8";
                else if  (ppq == 84)    return "7|16";
                else if  (ppq == 96)    return "1|2";
                else if  (ppq == 108)   return "9|16";
                else if  (ppq == 120)   return "5|8";
                else if  (ppq == 128)   return "2|3";
                else if  (ppq == 132)   return "11|16";
                else if  (ppq == 144)   return "3|4";
                else if  (ppq == 156)   return "13|16";
                else if  (ppq == 160)   return "5|6";
                else if  (ppq == 168)   return "7|8";
                else if  (ppq == 180)   return "15|16";
                else return String.valueOf((displaysTime ? 1 : 0) + ppq);
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
                try { TimeDisplay.this.steps = (int)(val * maxSteps); reviseTime(); }
                finally { lock.unlock(); }
                }
            };

        beatsDial = new SmallDial(beats)
            {
            protected String map(double val) { return String.valueOf((displaysTime ? 1 : 0) + (int)(val * Math.min(maxBeats, beatsPerBar - 1))); }
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
                try { TimeDisplay.this.beats = (int)(val * Math.min(maxBeats, beatsPerBar - 1));  reviseTime(); }
                finally { lock.unlock(); }
                }
            };
                
        barsDial = new SmallDial(bars)
            {
            protected String map(double val) { return String.valueOf((displaysTime ? 1 : 0) + (int)(val * maxBars)); }
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
                try { TimeDisplay.this.bars = (int)(val * maxBars);  reviseTime(); }
                finally { lock.unlock(); }
                }
            };

        partsDial = new SmallDial(parts)
            {
            protected String map(double val) { return String.valueOf((displaysTime ? 1 : 0) + (int)(val *  maxParts)); }
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
                try { TimeDisplay.this.parts = (int)(val * maxParts);  reviseTime(); }
                finally { lock.unlock(); }
                }
            };
                        
        Box box = new Box(BoxLayout.X_AXIS);
        if (maxParts > 0) box.add(partsDial.getLabelledTitledDialVertical("Part", "15|16"));
        if (maxBars > 0) box.add(barsDial.getLabelledTitledDialVertical("Bar", "15|16"));
        if (maxBeats > 0) box.add(beatsDial.getLabelledTitledDialVertical("Beat", "15|16"));            // slightly bigger than " 888 "
        if (maxSteps > 0) box.add(stepsDial.getLabelledTitledDialVertical("Tick", "15|16"));
        setLayout(new BorderLayout());
                
        presets = new PushButton("...", PRESET_OPTIONS)
            {
            public void perform(int val)
                {
                stepsDial.setValue(Math.min(maxSteps, PRESETS[val]) / (double)maxSteps);
                stepsDial.redraw();
                }
            };
        add(box, BorderLayout.WEST);
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
            temp = new JLabel(" ");
            temp.setFont(SmallDial.FONT);
            vert.add(temp, BorderLayout.SOUTH);

            vert.add(Stretch.makeHorizontalStretch(), BorderLayout.CENTER);
            vert.add(presets, BorderLayout.WEST);
            pan.add(vert, BorderLayout.WEST);
            }
        add(pan, BorderLayout.CENTER);
        
        revise(false);               // we're NOW ready to revise
        initializing = false;
        }
    }
