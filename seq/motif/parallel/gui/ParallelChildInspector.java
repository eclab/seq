/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.parallel.gui;

import seq.motif.parallel.*;
import seq.engine.*;
import seq.gui.*;
import seq.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.locks.*;

public class ParallelChildInspector extends WidgetList
    {
    public static final double MAX_RATE_LOG = Math.log(Parallel.Data.MAX_RATE);
    public static final String[] FINE_DELAY_OPTIONS = 
        { 
        "1/32",
        "1/16",
        "1/8",
        "1/6",
        "1/4",
        "1/3",
        "1/2",
        "2/3",
        "3/4"
        };

    public static final double[] FINE_DELAYS = 
        {
        1/32.0 + 1.0 / Seq.PPQ, 
        1/16.0 + 1.0 / Seq.PPQ,
        1/8.0 + 1.0 / Seq.PPQ,
        1/6.0 + 1.0 / Seq.PPQ,
        1/4.0 + 1.0 / Seq.PPQ,
        1/3.0 + 1.0 / Seq.PPQ,
        1/2.0 + 1.0 / Seq.PPQ,
        2/3.0 + 1.0 / Seq.PPQ,
        3/4.0 + 1.0 / Seq.PPQ,
        };

    public static final String[] RATE_OPTIONS = 
        {
        "Custom...",
        "1/16",
        "1/15",
        "1/14",
        "1/13",
        "1/12",
        "1/11",
        "1/10",
        "1/9",
        "1/8",
        "1/7",
        "1/6",
        "1/5",
        "1/4",
        "1/3",
        "1/2",
        "3/5",
        "2/3",
        "3/4",
        "4/5",
        "None",
        "5/4",
        "4/3",
        "3/2",
        "5/3",
        "2",
        "3",
        "4",
        "5",
        "6",
        "7",
        "8",
        "9",
        "10",
        "11",
        "12",
        "13",
        "14",
        "15",
        "16" 
        };

    public static final double[] RATES = 
        {
        -1,
        0.0625,
        0.06666666666666667,
        0.07142857142857142,
        0.07692307692307693,
        0.08333333333333333,
        0.09090909090909091,
        0.1,
        0.11111111111111111,
        0.125,
        0.14285714285714285,
        0.16666666666666667,
        0.2,
        0.25,
        0.33333333333333333,
        0.5,
        0.6,
        0.66666666666666667,
        0.75,
        0.8,
        1.0,
        1.25,
        1.33333333333333333,
        1.5,
        1.66666666666666667,
        2.0,
        3.0,
        4.0,
        5.0,
        6.0,
        7.0,
        8.0,
        9.0,
        10.0,
        11.0,
        12.0,
        13.0,
        14.0,
        15.0,
        1.0,
        };

    public static final String[] QUANTIZATIONS = { "None", "16th Note", "Quarter Note", "Measure" };

    Seq seq;
    Parallel parallel;
    ParallelUI parallelui;
    MotifUI motifui;
    Motif.Child child;
    ParallelButton button;
    JComboBox quantization;
    JCheckBox override;
    JCheckBox mute;
    JPanel gainPanel;
    
    SmallDial coarseDelay;
    SmallDial fineDelay;
    SmallDial transpose;
    SmallDial gain;
    SmallDial rate;
    SmallDial probability;
    PushButton fineDelayPresets;
    PushButton ratePresets;
    JComboBox out;
    StringField name;
    
    String[] defaults = new String[1 + Motif.NUM_PARAMETERS];

    public void setButton(ParallelButton button) { this.button = button; }

    Parallel.Data getData() { return ((Parallel.Data)(child.getData())); }
 
    public void buildDefaults(Motif child, Motif parent)
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

    
    public static double getRate(JComponent parent, int val)
        {
        if (val == 0) // custom
            {
            SmallDial from = new SmallDial(Prefs.getLastDouble("RateFrom", 0.5))
                {
                double value;
                public double getValue() { return value; }
                public void setValue(double val) { value = val; }
                public String map(double val) { return "" + (int)(val * 31 + 1); }
                };

            SmallDial to = new SmallDial(Prefs.getLastDouble("RateTo", 0.5))
                {
                double value;
                public double getValue() { return value; }
                public void setValue(double val) { value = val; }
                public String map(double val) { return "" + (int)(val * 31 + 1); }
                };

            String[] names = { "Fit", "Into" };
            JComponent[] components = new JComponent[] { to.getLabelledDial("32"), from.getLabelledDial("32"), };
            int result = Dialogs.showMultiOption(parent, names, components, new String[] {  "Change", "Cancel" }, 0, "Change Rate Fraction", "Enter Rate Fraction Settings");
                
            if (result == 0)
                {
                double _from = (from.getValue() * 31 + 1);
                double _to = (to.getValue() * 31 + 1);

                Prefs.setLastDouble("RateFrom", from.getValue());
                Prefs.setLastDouble("RateTo", to.getValue());
                return _to / _from;
                }
            else return 1.0;        // no change
            }
        else return RATES[val];
        }
                
    public ParallelChildInspector(Seq seq, Parallel parallel, MotifUI motifui, Motif.Child child, ParallelUI parallelui)
        {
        this.seq = seq;
        this.parallel = parallel;
        this.motifui = motifui;
        this.child = child;
        buildDefaults(child.getMotif(), parallelui.getMotif());

        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            if (child.getNickname() == null) child.setNickname("");
            name = new StringField(child.getNickname())
                {
                public String newValue(String newValue)
                    {
                    newValue = newValue.trim();
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { child.setNickname(newValue); }
                    finally { lock.unlock(); }
                    parallelui.updateText();
                    return newValue;
                    }
                                
                public String getValue() 
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return child.getNickname(); }
                    finally { lock.unlock(); }
                    }
                };
            name.setColumns(MotifUI.INSPECTOR_NAME_DEFAULT_SIZE);

            coarseDelay = new SmallDial((getData().getDelay() / Seq.PPQ) / Parallel.Data.MAX_DELAY)
                {
                protected String map(double val) 
                    {
                    int beats = (int)(val * Parallel.Data.MAX_DELAY);
                    int beatsPerBar = 0;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { beatsPerBar = seq.getBar(); }
                    finally { lock.unlock(); }
                    return String.valueOf("" + beats + " (" + (beats / beatsPerBar) + " . " + (beats % beatsPerBar) + ")");
                    }
                public double getValue() 
                    {
                    double val = 0;
                    int delay = 0;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try 
                        {
                        delay = getData().getDelay();
                        val = (delay / Seq.PPQ) / Parallel.Data.MAX_DELAY; 
                        }
                    finally { lock.unlock(); }
                    if (button != null) button.setDelay(delay);
                    return val;
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    int delay = 0;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try 
                        { 
                        int oldDelay = getData().getDelay();
                        int oldBeats = oldDelay / Seq.PPQ;
                        int oldPPQ = oldDelay - (oldBeats * Seq.PPQ);           // Faster than %
                        delay = oldPPQ + (int)(val * Parallel.Data.MAX_DELAY) * Seq.PPQ;
                        getData().setDelay(delay);
                        }
                    finally { lock.unlock(); }
                    if (button != null) button.setDelay(delay);
                    }
                };

            fineDelay = new SmallDial((getData().getDelay() % Seq.PPQ) / (double)(Seq.PPQ - 1))
                {
                protected String map(double val) 
                    {
                    int ppq = (int)(val * (Seq.PPQ - 1));
                    String ppqVal = String.valueOf(ppq) + " / " + Seq.PPQ;
                    if (ppq == 0) return ppqVal;
                    if (ppq % (Seq.PPQ / 2) == 0)       // divisible by 2
                        return ppqVal + " (" + String.valueOf(ppq / (Seq.PPQ / 2)) + " / 2" + ")";
                    else if (ppq % (Seq.PPQ / 3) == 0)  // divisible by 3
                        return ppqVal + " (" + String.valueOf(ppq / (Seq.PPQ / 3)) + " / 3" + ")";
                    else if (ppq % (Seq.PPQ / 4) == 0)  // divisible by 4
                        return ppqVal + " (" + String.valueOf(ppq / (Seq.PPQ / 4)) + " / 4" + ")";
                    else if (ppq % (Seq.PPQ / 6) == 0)  // divisible by 6
                        return ppqVal + " (" + String.valueOf(ppq / (Seq.PPQ / 6)) + " / 6" + ")";
                    else if (ppq % (Seq.PPQ / 8) == 0)  // divisible by 8
                        return ppqVal + " (" + String.valueOf(ppq / (Seq.PPQ / 8)) + " / 8" + ")";
                    else if (ppq % (Seq.PPQ / 12) == 0) // divisible by 12
                        return ppqVal + " (" + String.valueOf(ppq / (Seq.PPQ / 12)) + " / 12" + ")";
                    else if (ppq % (Seq.PPQ / 16) == 0) // divisible by 16
                        return ppqVal + " (" + String.valueOf(ppq / (Seq.PPQ / 16)) + " / 16" + ")";
                    else if (ppq % (Seq.PPQ / 24) == 0) // divisible by 24
                        return ppqVal + " (" + String.valueOf(ppq / (Seq.PPQ / 24)) + " / 24" + ")";
                    else if (ppq % (Seq.PPQ / 32) == 0) // divisible by 32
                        return ppqVal + " (" + String.valueOf(ppq / (Seq.PPQ / 32)) + " / 32" + ")";
                    else if (ppq % (Seq.PPQ / 36) == 0) // divisible by 36
                        return ppqVal + " (" + String.valueOf(ppq / (Seq.PPQ / 36)) + " / 36" + ")";
                    else if (ppq % (Seq.PPQ / 48) == 0) // divisible by 48
                        return ppqVal + " (" + String.valueOf(ppq / (Seq.PPQ / 48)) + " / 48" + ")";
                    else if (ppq % (Seq.PPQ / 96) == 0) // divisible by 96
                        return ppqVal + " (" + String.valueOf(ppq / (Seq.PPQ / 96)) + " / 96" + ")";
                    else return String.valueOf(ppq) + " / " + Seq.PPQ;
                    }
                public double getValue() 
                    { 
                    double val = 0;
                    int delay = 0;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try 
                        {
                        delay = getData().getDelay();
                        val = (delay % Seq.PPQ) / (double)(Seq.PPQ - 1); 
                        }
                    finally { lock.unlock(); }
                    if (button != null) button.setDelay(delay);
                    return val;
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    int delay = 0;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try 
                        { 
                        int oldDelay = getData().getDelay();
                        int oldBeats = oldDelay / Seq.PPQ;
                        int oldPPQ = oldDelay - (oldBeats * Seq.PPQ);           // Faster than %
                        delay = oldBeats * Seq.PPQ + (int)(val * (Seq.PPQ - 1));
                        getData().setDelay(delay);
                        }
                    finally { lock.unlock(); }
                    if (button != null) button.setDelay(delay);
                    }
                };

            fineDelayPresets = new PushButton("Presets...", FINE_DELAY_OPTIONS)
                {
                public void perform(int val)
                    {
                    if (seq == null) return;
                    fineDelay.update(FINE_DELAYS[val], false);
                    }
                };


            quantization = new JComboBox(QUANTIZATIONS);
            quantization.setSelectedIndex(getData().getEndingQuantization());
            quantization.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { getData().setEndingQuantization(quantization.getSelectedIndex()); }
                    finally { lock.unlock(); }                              
                    }
                });


            // This requires some explanation.  We are mapping the values we receive (0...1) in the Dial
            // into values 0.0625...16 using an exponential mapping.  However we also store negative values for
            // defaults.  This causes problems when we're queried for our value, but we're currently negative so
            // we compute the log of a negative value.  So instead here, in the initialization and in getValue(),
            // we return DEFAULT_RATE instead.   This issue doesn't come up when just doing 0...1 as normal.
            double d = getData().getRate(); 
            if (d < 0) d = Parallel.Data.DEFAULT_RATE;
            rate = new SmallDial((Math.log(d) + MAX_RATE_LOG) / MAX_RATE_LOG / 2.0, defaults)
                {
                protected String map(double val) 
                    {
                    double d = Math.exp(val * 2 * MAX_RATE_LOG - MAX_RATE_LOG);
                    return super.map(d);
                    }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double d = getData().getRate(); if (d < 0) return Parallel.Data.DEFAULT_RATE; else return (Math.log(d) + MAX_RATE_LOG) / MAX_RATE_LOG / 2.0;}
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { getData().setRate(Math.exp(val * 2 * MAX_RATE_LOG - MAX_RATE_LOG));}
                    finally { lock.unlock(); }
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) getData().setRate(-(val + 1)); }
                    finally { lock.unlock(); }
                    parallelui.updateText();                        // FIXME: is this needed?
                    }
    
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = getData().getRate(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };
                
            ratePresets = new PushButton("Presets...", RATE_OPTIONS)
                {
                public void perform(int val)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { getData().setRate(getRate(ParallelChildInspector.this, val));}
                    finally { lock.unlock(); }
                    rate.redraw();
                    }
                };

            transpose = new SmallDial(getData().getTranspose() / (double)Parallel.Data.MAX_TRANSPOSE / 2.0, defaults)
                {
                protected String map(double val) { return String.valueOf((int)(val * 2 * Parallel.Data.MAX_TRANSPOSE) - Parallel.Data.MAX_TRANSPOSE); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double d = getData().getTranspose();  if (d < 0) return 0;  else return d / (double)Parallel.Data.MAX_TRANSPOSE / 2.0; }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { getData().setTranspose((int)(val * 2 * Parallel.Data.MAX_TRANSPOSE)); }
                    finally { lock.unlock(); }
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) getData().setTranspose(-(val + 1)); }
                    finally { lock.unlock(); }
                    parallelui.updateText();                        // FIXME: is this needed?
                    }
    
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = getData().getTranspose(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };

            gain = new SmallDial(getData().getGain() / Parallel.Data.MAX_GAIN, defaults)
                {
                protected String map(double val) { return super.map(val * Parallel.Data.MAX_GAIN); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return getData().getGain() / Parallel.Data.MAX_GAIN; }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { getData().setGain(val * Parallel.Data.MAX_GAIN); }
                    finally { lock.unlock(); }
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) getData().setGain(-(val + 1)); }
                    finally { lock.unlock(); }
                    parallelui.updateText();                        // FIXME: is this needed?
                    }
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = getData().getGain(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };


            mute = new JCheckBox("Mute");
            mute.setSelected(getData().getMute());
            mute.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try 
                        { 
                        getData().setMute(mute.isSelected()); 
                        }
                    finally { lock.unlock(); }                              
                    }
                });

            gainPanel = new JPanel();
            gainPanel.setLayout(new BorderLayout());
            gainPanel.add(gain.getLabelledDial("0.0000"), BorderLayout.CENTER);
            gainPanel.add(mute, BorderLayout.EAST);


            Out[] seqOuts = seq.getOuts();
            String[] outs = new String[seqOuts.length + 1];
            outs[0] = "<html><i>Don't Change</i></html>";
            for(int i = 0; i < seqOuts.length; i++)
                {
                // We have to make these strings unique, or else the combo box doesn't give the right selected index, Java bug
                outs[i+1] = "" + (i + 1) + ": " + seqOuts[i].toString();
                }
                
            out = new JComboBox(outs);
            out.setMaximumRowCount(outs.length);
            out.setSelectedIndex(getData().getOut() + 1);
            out.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { getData().setOut(out.getSelectedIndex() - 1); }              // -1 is DISABLED
                    finally { lock.unlock(); }                              
                    }
                });
 
            probability = new SmallDial(-1, defaults)
                {
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return getData().getProbability(); }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { getData().setProbability(val);}
                    finally { lock.unlock(); }
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    // We add 2 to skip PARAMETER_UNBOUND
                    try { if (val != SmallDial.NO_DEFAULT) getData().setProbability(-(val + 1)); }
                    finally { lock.unlock(); }
                    parallelui.updateText();                        // FIXME: is this needed?
                    }
    
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    // We add 2 to skip PARAMETER_UNBOUND
                    try { double val = getData().getProbability(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };

            override = new JCheckBox();
            override.setSelected(getData().getOverride());
            override.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { getData().setOverride(override.isSelected()); }
                    finally { lock.unlock(); }                              
                    }
                });

            }
        finally { lock.unlock(); }

        JPanel ratePanel = new JPanel();
        ratePanel.setLayout(new BorderLayout());
        ratePanel.add(rate.getLabelledDial("0.0000"), BorderLayout.CENTER);   // so it stretches
        ratePanel.add(ratePresets, BorderLayout.EAST);       

        JPanel fineDelayPanel = new JPanel();
        fineDelayPanel.setLayout(new BorderLayout());
        fineDelayPanel.add(fineDelay.getLabelledDial("158 / 192 (79 / 96)"), BorderLayout.CENTER);   // so it stretches
        fineDelayPanel.add(fineDelayPresets, BorderLayout.EAST);       


        JPanel result = build(new String[] { "Nickname", "Coarse Delay", "Fine Delay", "Probability", "End Quantization", "Override", "MIDI Changes", "Rate", "Transpose", "Gain", "Out" }, 
            new JComponent[] 
                {
                name,
                coarseDelay.getLabelledDial("256 (64 . 4)"),
                fineDelayPanel,
                probability.getLabelledDial("0.0000"),
                quantization,
                override,
                null,                           // separator
                ratePanel,
                transpose.getLabelledDial("-24"),
                //gain.getLabelledDial("0.0000"),
                gainPanel,
                out,
                });

        // the widgetlist is added SOUTH -- we need to change that to CENTER
        remove(result);
        add(result, BorderLayout.CENTER);

        add(new ArgumentList(seq, child, parallelui.getMotif()), BorderLayout.NORTH);
        }
                
    public int getChildNum()
        {
        ReentrantLock lock = seq.getLock();
        lock.lock();
        // O(n) :-(
        try { return parallel.getChildren().indexOf(child); }
        finally { lock.unlock(); }
        }
                
    public void revise()
        {
        Seq old = seq;
        seq = null;
        ReentrantLock lock = old.getLock();
        lock.lock();
        try 
            { 
            out.setSelectedIndex(getData().getOut() + 1); 
            override.setSelected(getData().getOverride()); 
            }
        finally { lock.unlock(); }                              
        seq = old;
        name.update();
        rate.redraw();
        probability.redraw();
        coarseDelay.redraw();
        fineDelay.redraw();
        transpose.redraw();
        gain.redraw();
        }
    }
