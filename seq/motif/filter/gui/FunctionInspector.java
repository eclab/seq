/* 
   Copyright 2025 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.filter.gui;

import seq.motif.filter.*;
import seq.engine.*;
import seq.gui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.locks.*;

public class FunctionInspector extends JPanel
    {
    public static final String[] INSPECTOR_NAMES = { "None", "Note", "Delay", "Drop", "Noise", "Map", "Scale", "Chord"  };
    public static final String[] PARAMETER_TYPES = { "Bend", "CC", "NRPN", "RPN", "Aftertouch" };
    public static final String[] MAP_TYPES = { "None", "BY - X", "X + BY", "X - BY", "X * BY", "Discretize[X, BY]", "1-Discretize[X,BY]", "X^2", "X^4", "1-(1-X)^2", "1-(1-x)^4" };
    // public static final String[] NOTES = { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B" };

    Seq seq;
    Filter filter;
    int index;
    SubInspector subinspector;
    
    
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

    
    public FunctionInspector(Seq seq, Filter filter, int index)
        {
        this.seq = seq;
        this.filter = filter;
        this.index = index;        
        
        buildDefaults(filter);

        ReentrantLock lock = seq.getLock();
        String type = null;
        int typeIndex = 0;
        lock.lock();
        try
            {
            type = filter.getFunction(index).getType();
            typeIndex = filter.typeIndex(type);
            }
        finally { lock.unlock(); }
        subinspector = buildSubinspector(type);

        JComboBox subcombo = new JComboBox(INSPECTOR_NAMES);
        subcombo.setSelectedIndex(typeIndex);
        subcombo.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                String type1 = null;
                if (seq == null) return;
                ReentrantLock lock = seq.getLock();
                lock.lock();
                try 
                    {
                    Filter.Function func1 = filter.buildFunction(subcombo.getSelectedIndex());
                    type1 = func1.getType();
                    filter.setFunction(index, func1); 
                    }
                finally { lock.unlock(); }
                                
                remove(subinspector);
                subinspector =  buildSubinspector(type1);
                add(subinspector, BorderLayout.CENTER);
                revalidate();
                repaint();
                }
            });
                
        setLayout(new BorderLayout());
        add(subcombo, BorderLayout.NORTH);
        add(subinspector, BorderLayout.CENTER);
        setBorder(BorderFactory.createTitledBorder("Stage " + (index + 1)));
        subcombo.setToolTipText(STAGE_TYPE_TOOLTIP);
        }
                
    public void revise()                        // do I need to check if the subinspectors changed?  Do I need this function at all?
        {
        subinspector.revise();
        }
        
    
    public class SubInspector extends WidgetList
        {
        public void revise() { }
        public String getName() { return "None"; }
        }
    
    public class ChangeNoteInspector extends SubInspector
        {
        JComboBox out;
        JCheckBox allOut;
        SmallDial transpose;
        SmallDial transposeVariance;
        SmallDial gain;
        SmallDial gainVariance;
        SmallDial releaseGain;
        SmallDial releaseGainVariance;
        TimeDisplay length;
        JCheckBox changeLength;
                
        public ChangeNoteInspector()
            {
            Filter.ChangeNote func = (Filter.ChangeNote)(filter.getFunction(index));
                        
            Out[] seqOuts = seq.getOuts();
            String[] outs = new String[seqOuts.length + 1];
            outs[0] = "<html><i>Don't Change</i></html>";
            for(int i = 0; i < seqOuts.length; i++)
                {
                // We have to make these strings unique, or else the combo box doesn't give the right selected index, Java bug
                outs[i + 1] = "" + (i + 1) + ": " + seqOuts[i].toString();
                }
                                                
            JComboBox out = new JComboBox(outs);
            out.setMaximumRowCount(outs.length);
            out.setSelectedIndex(func.getOut() + 1);
            out.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { func.setOut(out.getSelectedIndex() - 1); }
                    finally { lock.unlock(); }
                    }
                });            

            allOut = new JCheckBox();
            allOut.setSelected(func.isAllOut());
            allOut.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { func.setAllOut(allOut.isSelected()); }
                    finally { lock.unlock(); }                              
                    }
                });

            transpose = new SmallDial(func.getTranspose() / (Filter.MAX_TRANSPOSE * 2.0), defaults)
                {
                protected String map(double val) { return String.valueOf((int)(val * 2 * Filter.MAX_TRANSPOSE) - Filter.MAX_TRANSPOSE); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return func.getTranspose() / (Filter.MAX_TRANSPOSE * 2.0); }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { func.setTranspose((int)(val * 2 * Filter.MAX_TRANSPOSE)); }
                    finally { lock.unlock(); }
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) func.setTranspose(-(val + 1)); }
                    finally { lock.unlock(); }
                    }
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = func.getTranspose(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };

            transposeVariance = new SmallDial(func.getTransposeVariance(), defaults)
                {
                protected String map(double val) { return String.format("%.4f", val * Filter.MAX_TRANSPOSE); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return func.getTransposeVariance(); }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { func.setTransposeVariance(val); }
                    finally { lock.unlock(); }
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) func.setTransposeVariance(-(val + 1)); }
                    finally { lock.unlock(); }
                    }
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = func.getTransposeVariance(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };

            gain = new SmallDial(func.getGain() / Filter.MAX_GAIN, defaults)
                {
                protected String map(double val) { return String.format("%.4f", val * Filter.MAX_GAIN); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return func.getGain() / Filter.MAX_GAIN; }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { func.setGain(val * Filter.MAX_GAIN); }
                    finally { lock.unlock(); }
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) func.setGain(-(val + 1)); }
                    finally { lock.unlock(); }
                    }
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = func.getGain(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };

            gainVariance = new SmallDial(func.getGainVariance(), defaults)
                {
                protected String map(double val) { return String.format("%.4f", val * Filter.MAX_GAIN); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return func.getGainVariance(); }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { func.setGainVariance(val); }
                    finally { lock.unlock(); }
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) func.setGainVariance(-(val + 1)); }
                    finally { lock.unlock(); }
                    }
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = func.getGainVariance(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };

            releaseGain = new SmallDial(func.getReleaseGain() / Filter.MAX_GAIN, defaults)
                {
                protected String map(double val) { return String.format("%.4f", val * Filter.MAX_GAIN); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return func.getReleaseGain() / Filter.MAX_GAIN; }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { func.setReleaseGain(val * Filter.MAX_GAIN); }
                    finally { lock.unlock(); }
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) func.setReleaseGain(-(val + 1)); }
                    finally { lock.unlock(); }
                    }
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = func.getReleaseGain(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };

            releaseGainVariance = new SmallDial(func.getReleaseGainVariance(), defaults)
                {
                protected String map(double val) { return String.format("%.4f", val); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return func.getReleaseGainVariance(); }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { func.setReleaseGainVariance(val); }
                    finally { lock.unlock(); }
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) func.setReleaseGainVariance(-(val + 1)); }
                    finally { lock.unlock(); }
                    }
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = func.getReleaseGainVariance(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };

            changeLength = new JCheckBox();
            changeLength.setSelected(func.getChangeLength());
            changeLength.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { func.setChangeLength(changeLength.isSelected()); }
                    finally { lock.unlock(); }                              
                    }
                });

            length = new TimeDisplay(Seq.PPQ, seq)
                {
                public int getTime()
                    {
                    return func.getLength();
                    }
                        
                public void setTime(int time)
                    {
                    func.setLength(time);
                    }
                };
            length.setDisplaysTime(false);

            
            out.setToolTipText(NOTE_OUT_TOOLTIP);
            allOut.setToolTipText(NOTE_ALL_OUT_TOOLTIP);
            transpose.setToolTipText(NOTE_TRANSPOSE_TOOLTIP);
            transposeVariance.setToolTipText(NOTE_TRANSPOSE_VARIANCE_TOOLTIP);
            gain.setToolTipText(NOTE_GAIN_TOOLTIP);
            gainVariance.setToolTipText(NOTE_GAIN_VARIANCE_TOOLTIP);
            releaseGain.setToolTipText(NOTE_RELEASE_TOOLTIP);
            releaseGainVariance.setToolTipText(NOTE_RELEASE_VARIANCE_TOOLTIP);
            length.setToolTipText(NOTE_LENGTH_TOOLTIP);
            changeLength.setToolTipText(NOTE_CHANGE_LENGTH_TOOLTIP);
                                                                        
            build(new String[] { "", "Out", "Non-Note Out", "Transpose", "Trans Var", "Gain", "Gain Var", "Release", "Rel Var", "Change Length", "Length"}, 
                new JComponent[] 
                    {
                    null,
                    out,
                    allOut,
                    transpose.getLabelledDial("24"),
                    transposeVariance.getLabelledDial("0.0000"),
                    gain.getLabelledDial("0.0000"),
                    gainVariance.getLabelledDial("0.0000"),
                    releaseGain.getLabelledDial("0.0000"),
                    releaseGainVariance.getLabelledDial("0.0000"),
                    changeLength,
                    length
                    });

            }
                        
        public void revise() 
            {
            Filter.ChangeNote func = (Filter.ChangeNote)(filter.getFunction(index));

            Seq old = seq;
            seq = null;
            ReentrantLock lock = old.getLock();
            lock.lock();
            try 
                { 
                out.setSelectedIndex(func.getOut()); 
                allOut.setSelected(func.isAllOut());
                }
            finally { lock.unlock(); }                              
            seq = old;
            if (transpose != null) transpose.redraw();
            if (transposeVariance != null) transposeVariance.redraw();
            if (gain != null) gain.redraw();
            if (gainVariance != null) gainVariance.redraw();
            if (releaseGain != null) releaseGain.redraw();
            if (releaseGainVariance != null) releaseGainVariance.redraw();
                        
            if (length != null) length.revise();
            }

        public String getName() { return "Note"; }
        }

    public class DelayInspector extends SubInspector
        {
        JCheckBox original;
        TimeDisplay delayInterval;
        SmallDial numTimes;
        SmallDial cut;
                
        public DelayInspector()
            {
            Filter.Delay func = (Filter.Delay)(filter.getFunction(index));
                        
            original = new JCheckBox();
            original.setSelected(func.getOriginal());
            original.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { func.setOriginal(original.isSelected()); }
                    finally { lock.unlock(); }                              
                    }
                });
            delayInterval = new TimeDisplay(func.getDelayInterval(), seq)
                {
                public int getTime()
                    {
                    return func.getDelayInterval();
                    }
                        
                public void setTime(int time)
                    {
                    func.setDelayInterval(time);
                    }
                };
            delayInterval.setDisplaysTime(false);

            cut = new SmallDial(func.getCut(), defaults)
                {
                protected String map(double val) { return String.format("%.4f", val); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return func.getCut(); }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { func.setCut(val); }
                    finally { lock.unlock(); }
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) func.setCut(-(val + 1)); }
                    finally { lock.unlock(); }
                    }
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = func.getCut(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };
            numTimes = new SmallDial(func.getNumTimes() / (double)Filter.MAX_DELAY_NUM_TIMES, defaults)
                {
                protected String map(double val) { return String.valueOf((int)(val * Filter.MAX_DELAY_NUM_TIMES)); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return func.getNumTimes() / (double)Filter.MAX_DELAY_NUM_TIMES; }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { func.setNumTimes((int)(val * Filter.MAX_DELAY_NUM_TIMES)); }
                    finally { lock.unlock(); }
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) func.setNumTimes(-(val + 1)); }
                    finally { lock.unlock(); }
                    }
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = func.getNumTimes(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };

            original.setToolTipText(DELAY_ORIGINAL_TOOLTIP);
            delayInterval.setToolTipText(DELAY_INTERVAL_TOOLTIP);
            cut.setToolTipText(DELAY_CUT_TOOLTIP);
            numTimes.setToolTipText(DELAY_NUM_DELAYS_TOOLTIP);
                                                                        
            build(new String[] { "", "Original", /*"Initial",*/ "Interval", "Cut", "Num Delays"}, 
                new JComponent[] 
                    {
                    null,
                    original,
                    //initialDelay,
                    delayInterval,
                    cut.getLabelledDial("0.0000"),
                    numTimes.getLabelledDial("16")
                    });
            }
                        
        public void revise() 
            {
            Filter.Delay func = (Filter.Delay)(filter.getFunction(index));

            Seq old = seq;
            seq = null;
            ReentrantLock lock = old.getLock();
            lock.lock();
            try 
                { 
                original.setSelected(func.getOriginal());
                }
            finally { lock.unlock(); }                              
            seq = old;
            //if (initialDelay != null) initialDelay.revise();
            if (delayInterval != null) delayInterval.revise();
            if (numTimes != null) numTimes.redraw();
            if (cut != null) cut.redraw();
            }               
        public String getName() { return "Delay"; }
        }

    public class DropInspector extends SubInspector
        {
        JCheckBox cut;
        SmallDial probability;
                
        public DropInspector()
            {
            Filter.Drop func = (Filter.Drop)(filter.getFunction(index));
                        
            cut = new JCheckBox();
            cut.setSelected(func.getCut());
            cut.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { func.setCut(cut.isSelected()); }
                    finally { lock.unlock(); }                              
                    }
                });

            probability = new SmallDial(func.getProbability(), defaults)
                {
                protected String map(double val) { return String.format("%.4f", val); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return func.getProbability(); }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { func.setProbability(val); }
                    finally { lock.unlock(); }
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) func.setProbability(-(val + 1)); }
                    finally { lock.unlock(); }
                    }
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = func.getProbability(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };

            cut.setToolTipText(DROP_CUT_TOOLTIP);
            probability.setToolTipText(DROP_PROBABILITY_TOOLTIP);
                                                                        
            build(new String[] { "", "Delete All", "Note Probability"}, 
                new JComponent[] 
                    {
                    null,
                    cut,
                    probability.getLabelledDial("Param 8"),     //"0.0000"),
                    });
            }
                        
        public void revise() 
            {
            Filter.Drop func = (Filter.Drop)(filter.getFunction(index));

            Seq old = seq;
            seq = null;
            ReentrantLock lock = old.getLock();
            lock.lock();
            try 
                { 
                cut.setSelected(func.getCut());
                }
            finally { lock.unlock(); }                              
            seq = old;
            if (probability != null) probability.redraw();
            }               

        public String getName() { return "Drop"; }
        }

    public class NoiseInspector extends SubInspector
        {
        SmallDial distVar;
        JComboBox parameterType;
        SmallDial parameterMSB;
        SmallDial parameterLSB;
        TimeDisplay rate;
                                
        public NoiseInspector()
            {
            Filter.Noise func = (Filter.Noise)(filter.getFunction(index));
                        
            distVar = new SmallDial(func.getDistVar(), defaults)
                {
                protected String map(double val) { return String.valueOf(val); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return func.getDistVar(); }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { func.setDistVar(val); }
                    finally { lock.unlock(); }
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) func.setDistVar(-(val + 1)); }
                    finally { lock.unlock(); }
                    }
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = func.getDistVar(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };

            parameterLSB = new SmallDial((func.getParameter() % 128) / 127.0)
                {
                protected String map(double val) { return String.valueOf((int)(val * 127)); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return (func.getParameter() % 128) / 127.0; }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { func.setParameter((int)((func.getParameter() / 128) * 128 + val * 127)); }
                    finally { lock.unlock(); }
                    }
                };

            parameterMSB = new SmallDial((func.getParameter() / 128) / 127.0)
                {
                protected String map(double val) 
                    { 
                    int v = (int)(val * 127);
                    return String.valueOf(v) ; // + " / " +  NOTES[v % 12] + (v / 12);
                    }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return (func.getParameter() / 128) / 127.0; }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { func.setParameter((int)((func.getParameter() % 128) + val * 127 * 128)); }
                    finally { lock.unlock(); }
                    }
                };

            rate = new TimeDisplay(Seq.PPQ / 4, seq)
                {
                public int getTime()
                    {
                    return func.getRate();
                    }
                        
                public void setTime(int time)
                    {
                    func.setRate(time);
                    }
                };
            rate.setDisplaysTime(false);

            parameterType = new JComboBox(PARAMETER_TYPES);
            parameterType.setSelectedIndex(func.getParameterType());
            parameterType.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    int result = parameterType.getSelectedIndex();
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { func.setParameterType(result); }
                    finally { lock.unlock(); } 
                    updateParams(result);
                    }
                });
            updateParams(func.getParameterType());

            parameterType.setToolTipText(PARAMETER_TYPE_TOOLTIP);
            parameterMSB.setToolTipText(PARAMETER_PARAM_MSB_TOOLTIP);
            parameterLSB.setToolTipText(PARAMETER_PARAM_LSB_TOOLTIP);
            distVar.setToolTipText(PARAMETER_VARIANCE_TOOLTIP);
            rate.setToolTipText(PARAMETER_RATE_TOOLTIP);

            build(new String[] { "", "Type", "Param/MSB", "LSB", "Variance", "Rate"}, 
                new JComponent[] 
                    {
                    null,
                    parameterType,
                    parameterMSB.getLabelledDial("128"),
                    parameterLSB.getLabelledDial("128"),
                    distVar.getLabelledDial("0.0000"),
                    rate
                    });
            }
                
        void updateParams(int result)
            {
            if (result == 0)                // Bend
                {
                parameterMSB.setEnabled(false);
                parameterLSB.setEnabled(false);
                }
            else if (result == 1)           // CC
                {
                parameterMSB.setEnabled(true);
                parameterLSB.setEnabled(false);
                }
            else if (result == 2)           // NRPN
                {
                parameterMSB.setEnabled(true);
                parameterLSB.setEnabled(true);
                }
            else if (result == 3)           // RPN
                {
                parameterMSB.setEnabled(true);
                parameterLSB.setEnabled(true);
                }
            else if (result == 4)           // Aftertouch
                {
                parameterMSB.setEnabled(false);
                parameterLSB.setEnabled(false);
                }
            }
                
        public void revise() 
            {
            Filter.Noise func = (Filter.Noise)(filter.getFunction(index));

            Seq old = seq;
            seq = null;
            ReentrantLock lock = old.getLock();
            lock.lock();
            try 
                { 
                parameterType.setSelectedIndex(func.getParameterType()); 
                }
            finally { lock.unlock(); }                              
            seq = old;
            if (parameterMSB != null) parameterMSB.redraw();
            if (parameterLSB != null) parameterLSB.redraw();
            if (distVar != null) distVar.redraw();
                        
            if (rate != null) rate.revise();
            }

        public String getName() { return "Parameter"; }
        }



    public class MapInspector extends SubInspector
        {
        JComboBox parameterType;
        SmallDial parameterMSB;
        SmallDial parameterLSB;
        SmallDial variable;
        JComboBox mapType;
        SmallDial min;
        SmallDial max;
        TimeDisplay rate;
                               
        public String mapVal(double val)
            {
            Filter.Map func = (Filter.Map)(filter.getFunction(index));

            int type = 0;
            ReentrantLock lock = seq.getLock();
            lock.lock();
            try { type = func.getParameterType(); }
            finally { lock.unlock(); }
                        
            double mult = 127;
            if (type == Filter.Map.TYPE_BEND) mult = 8191;
            else if (type == Filter.Map.TYPE_NRPN) mult = 16383;
            else if (type == Filter.Map.TYPE_RPN) mult = 16383;
            return String.valueOf((int)(val * mult));
            }
                                
        public MapInspector()
            {
            Filter.Map func = (Filter.Map)(filter.getFunction(index));
                        
            variable = new SmallDial(func.getVariable(), defaults)
                {
                public String map(double val) { return mapVal(val); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return func.getVariable(); }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { func.setVariable(val); }
                    finally { lock.unlock(); }
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) func.setVariable(-(val + 1)); }
                    finally { lock.unlock(); }
                    }
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = func.getVariable(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };

            min = new SmallDial(func.getMin(), defaults)
                {
                public String map(double val) { return mapVal(val); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return func.getMin(); }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { func.setMin(val); }
                    finally { lock.unlock(); }
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) func.setMin(-(val + 1)); }
                    finally { lock.unlock(); }
                    }
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = func.getMin(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };

            max = new SmallDial(func.getMax(), defaults)
                {
                public String map(double val) { return mapVal(val); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return func.getMax(); }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { func.setMax(val); }
                    finally { lock.unlock(); }
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) func.setMax(-(val + 1)); }
                    finally { lock.unlock(); }
                    }
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = func.getMax(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };

            parameterLSB = new SmallDial((func.getParameter() % 128) / 127.0)
                {
                protected String map(double val) { return String.valueOf((int)(val * 127)); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return (func.getParameter() % 128) / 127.0; }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { func.setParameter((int)((func.getParameter() / 128) * 128 + val * 127)); }
                    finally { lock.unlock(); }
                    }
                };

            parameterMSB = new SmallDial((func.getParameter() / 128) / 127.0)
                {
                protected String map(double val) 
                    { 
                    int v = (int)(val * 127);
                    return String.valueOf(v) ; // + " / " +  NOTES[v % 12] + (v / 12);
                    }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return (func.getParameter() / 128) / 127.0; }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { func.setParameter((int)((func.getParameter() % 128) + val * 127 * 128)); }
                    finally { lock.unlock(); }
                    }
                };

            mapType = new JComboBox(MAP_TYPES);
            mapType.setSelectedIndex(func.getMap());
            mapType.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    int result = mapType.getSelectedIndex();
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { func.setMap(result); }
                    finally { lock.unlock(); } 
                    }
                });
            updateParams(func.getMap());

            parameterType = new JComboBox(PARAMETER_TYPES);
            parameterType.setSelectedIndex(func.getParameterType());
            parameterType.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    int result = parameterType.getSelectedIndex();
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { func.setParameterType(result); }
                    finally { lock.unlock(); } 
                    updateParams(result);
                    min.redraw();
                    max.redraw();
                    variable.redraw();
                    }
                });
            updateParams(func.getParameterType());

            parameterType.setToolTipText(PARAMETER_TYPE_TOOLTIP);
            parameterMSB.setToolTipText(PARAMETER_PARAM_MSB_TOOLTIP);
            parameterLSB.setToolTipText(PARAMETER_PARAM_LSB_TOOLTIP);
            mapType.setToolTipText(MAP_MAP_TOOLTIP);
            variable.setToolTipText(MAP_BY_TOOLTIP);
            min.setToolTipText(MAP_MIN_TOOLTIP);
            max.setToolTipText(MAP_MAX_TOOLTIP);

            build(new String[] { "", "Type", "Param/MSB", "LSB", "Map", "Min", "Max", "By"}, 
                new JComponent[] 
                    {
                    null,
                    parameterType,
                    parameterMSB.getLabelledDial("128"),
                    parameterLSB.getLabelledDial("128"),
                    mapType,
                    min.getLabelledDial("16383"),
                    max.getLabelledDial("16383"),
                    variable.getLabelledDial("16383")
                    });
            }
    
        void updateParams(int result)
            {
            if (result == 0)                // Bend
                {
                parameterMSB.setEnabled(false);
                parameterLSB.setEnabled(false);
                }
            else if (result == 1)           // CC
                {
                parameterMSB.setEnabled(true);
                parameterLSB.setEnabled(false);
                }
            else if (result == 2)           // NRPN
                {
                parameterMSB.setEnabled(true);
                parameterLSB.setEnabled(true);
                }
            else if (result == 3)           // RPN
                {
                parameterMSB.setEnabled(true);
                parameterLSB.setEnabled(true);
                }
            else if (result == 4)           // Aftertouch
                {
                parameterMSB.setEnabled(false);
                parameterLSB.setEnabled(false);
                }
            }
                
        public void revise() 
            {
            Filter.Map func = (Filter.Map)(filter.getFunction(index));

            Seq old = seq;
            seq = null;
            ReentrantLock lock = old.getLock();
            lock.lock();
            try 
                { 
                parameterType.setSelectedIndex(func.getParameterType()); 
                mapType.setSelectedIndex(func.getMap()); 
                }
            finally { lock.unlock(); }                              
            seq = old;
            if (parameterMSB != null) parameterMSB.redraw();
            if (parameterLSB != null) parameterLSB.redraw();
            if (variable != null) variable.redraw();
            if (min != null) min.redraw();
            if (max != null) max.redraw();
            }

        public String getName() { return "Parameter"; }
        }
        
 
    public static final String[] KEYS = new String[] { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B" };
    public static final String[] ROUND_TYPES = new String[] {  "Round Down", "Round Up", "Round Near/Down", "Round Near/Up" };
    public static final String[] SCALE_TYPES = new String[] {  "Chromatic", "Major", "Harmonic Minor", "Melodic Minor", "Dorian",
        "Phrygian", "Lydian", "Mixolyedian", "Relative Minor", "Locrian", "Blues Minor", "Pentatonic", "Minor Pentatonic",
        "Japanese Pentatonic", "Whole Tone", "Hungarian Gypsy", "Phrygian Dominant", "Persian", "Diminished (Oct)", "Augmentic (Hex)",
        "Octave", "4+Octave", "5+Octave", "4+5+Octave", "Major Triad", "Minor Triad", "Major 6", "Minor 6", "Augmented Triad",
        "7", "Major 7", "Minor 7", "2+Major 7", "2+Minor 7", "Diminished 7", "Major-Minor 7" };

    public class ScaleInspector extends SubInspector
        {
        PushButton scaleType;
        JCheckBox[] notes = new JCheckBox[12];
        JComboBox roundType;
        SmallDial key;
                               
        public ScaleInspector()
            {
            Filter.Scale func = (Filter.Scale)(filter.getFunction(index));
                        
            key = new SmallDial(func.getKey(), defaults)
                {
                public String map(double val) { return KEYS[(int)(val * 11.0)]; }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return func.getKey() / 11.0; }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { func.setKey((int)(val * 11.0)); }
                    finally { lock.unlock(); }
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) func.setKey(-(val + 1)); }
                    finally { lock.unlock(); }
                    }
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = func.getKey(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };
            key.setToolTipText(SCALE_KEY_TOOLTIP);
                
            for(int i = 0; i < notes.length; i++)
                {
                final int _i = i;
                notes[i] = new JCheckBox();
                notes[i].setSelected(func.getScale(_i));
                notes[i].addActionListener(new ActionListener()
                    {
                    public void actionPerformed(ActionEvent e)
                        {
                        if (seq == null) return;
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { func.setScale(_i, notes[_i].isSelected()); }
                        finally { lock.unlock(); }                              
                        }
                    });
                notes[i].setToolTipText(SCALE_NOTE_TOOLTIP);
                }

            scaleType = new PushButton("Scale/Chord...", SCALE_TYPES)
                {
                public void perform(int index)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { func.setScale(index); }
                    finally { lock.unlock(); } 
                    
                    // update notes
                    for(int i = 0; i < notes.length; i++)
                        {
                        final int _i = i;
                        if (seq == null) return;
                        lock = seq.getLock();
                        boolean val = false;
                        lock.lock();
                        try { val = func.getScale(_i); }
                        finally { lock.unlock(); }  
                        notes[i].setSelected(val);                            
                        }
                    }
                };
            scaleType.setToolTipText(SCALE_PRESETS_TOOLTIP);

            roundType = new JComboBox(ROUND_TYPES);
            roundType.setSelectedIndex(func.getRound());
            roundType.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    int result = roundType.getSelectedIndex();
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { func.setRound(result); }
                    finally { lock.unlock(); } 
                    }
                });
            roundType.setToolTipText(SCALE_ROUND_TOOLTIP);

            build(new String[] { "", "Key", "Round", "Presets", "1", "m2", "M2", "m3", "M3", "4", "Tri", "5", "m6", "M6", "m7", "M7"}, 
                new JComponent[] 
                    {
                    null,
                    key.getLabelledDial("Db"),
                    roundType,
                    scaleType,
                    notes[0],
                    notes[1],
                    notes[2],
                    notes[3],
                    notes[4],
                    notes[5],
                    notes[6],
                    notes[7],
                    notes[8],
                    notes[9],
                    notes[10],
                    notes[11],
                    });
            }
     
        public void revise() 
            {
            Filter.Scale func = (Filter.Scale)(filter.getFunction(index));

            Seq old = seq;
            seq = null;
            ReentrantLock lock = old.getLock();
            lock.lock();
            try 
                { 
                roundType.setSelectedIndex(func.getRound()); 
                for(int i = 0; i < 12; i++)
                    {
                    notes[i].setSelected(func.getScale(i));
                    }
                }
            finally { lock.unlock(); }                              
            seq = old;
            if (key != null) key.redraw();
            }

        public String getName() { return "Scale"; }
        }
              
    public static final String[] CHORD_TYPES = { "m3", "M3", "4", "5", "m6", "M6", "m7", "Octave", "Major Triad", "Maj 1 Inv", "Maj 2 Inv", "Minor Triad", "Min 1 Inv", "Min 2 Inv", "Dom7 Without 3", "Dom7", "Major 7", "Minor 7", "Diminished 7" };   
    public class ChordInspector extends SubInspector
        {
        JComboBox chordType;
                               
        public ChordInspector()
            {
            Filter.Chord func = (Filter.Chord)(filter.getFunction(index));

            chordType = new JComboBox(CHORD_TYPES);
            chordType.setSelectedIndex(func.getChord());
            chordType.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    int result = chordType.getSelectedIndex();
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { func.setChord(result); }
                    finally { lock.unlock(); } 
                    }
                });
            chordType.setMaximumRowCount(CHORD_TYPES.length);

            chordType.setToolTipText(CHORD_INTERVAL_TOOLTIP);
            build(new String[] { "", "Chord/Interval"}, 
                new JComponent[] 
                    {
                    null,
                    chordType,
                    });
            }

        public void revise() 
            {
            Filter.Scale func = (Filter.Scale)(filter.getFunction(index));

            Seq old = seq;
            seq = null;
            ReentrantLock lock = old.getLock();
            lock.lock();
            try 
                { 
                chordType.setSelectedIndex(func.getRound()); 
                }
            finally { lock.unlock(); }                              
            seq = old;
            }

        public String getName() { return "Chord"; }
        }

       
    public SubInspector buildSubinspector(String type)
        {
        if (type.equals(Filter.IDENTITY))
            {
            return new SubInspector();
            }
        else if (type.equals(Filter.CHANGE_NOTE))
            {
            return new ChangeNoteInspector();
            }
        else if (type.equals(Filter.DELAY))
            {
            return new DelayInspector();
            }
        else if (type.equals(Filter.DROP))
            {
            return new DropInspector();
            }
        else if (type.equals(Filter.NOISE))
            {
            return new NoiseInspector();
            }
        else if (type.equals(Filter.MAP))
            {
            return new MapInspector();
            }
        else if (type.equals(Filter.SCALE))
            {
            return new ScaleInspector();
            }
        else if (type.equals(Filter.CHORD))
            {
            return new ChordInspector();
            }
        else // uh...
            {
            return new SubInspector();
            }
        }

    // This is to align to top on flow layout in the FilterUI
    // See https://stackoverflow.com/questions/2743177/top-alignment-for-flowlayout
    public Component.BaselineResizeBehavior getBaselineResizeBehavior() 
        {
        return Component.BaselineResizeBehavior.CONSTANT_ASCENT;
        }

    public int getBaseline(int width, int height) 
        {
        return 0;
        }


    static final String STAGE_TYPE_TOOLTIP = "<html><b>Stage Type</b><br>" +
        "Sets the stage function type, at present one of:" + 
        "<ul>" +
        "<li><b>None</b>&nbsp;&nbsp;No filtering." +
        "<li><b>Note</b>&nbsp;&nbsp;Change note output, velocity, release velocity, pitch, or length." +
        "<li><b>Delay</b>&nbsp;&nbsp;Delay the onset of the note, or repeat it one or more times." +
        "<li><b>Drop</b>&nbsp;&nbsp;With a certain probability, filter out (delete) a note." +
        "<li><b>Parameter</b>&nbsp;&nbsp;Vary a non-note parameter, such as CC or Pitch Bend." +
        "</ul></html>";

    static final String NOTE_OUT_TOOLTIP = "<html><b>Out</b><br>" +
        "Changes the output device of the incoming MIDI notes.</html>";

    static final String NOTE_ALL_OUT_TOOLTIP = "<html><b>All Out</b><br>" +
        "Sets whether <b>Out</b> sets the output device for all incoming events, not just MIDI notes.</html>";
        
    static final String NOTE_TRANSPOSE_TOOLTIP = "<html><b>Transpose</b><br>" +
        "Transposes the pitch of the incoming MIDI notes.</html>";

    static final String NOTE_TRANSPOSE_VARIANCE_TOOLTIP = "<html><b>Transpose Variance</b><br>" +
        "Sets the variance of random noise with which to transpose the pitch of the incoming MIDI notes.</html>";

    static final String NOTE_GAIN_TOOLTIP = "<html><b>Gain</b><br>" +
        "Sets the gain to be multipled against the velocity (volume) of the incoming MIDI notes.</html>";

    static final String NOTE_GAIN_VARIANCE_TOOLTIP = "<html><b>Gain Variance</b><br>" +
        "Sets the variance of random noise to be multipled against the velocity (volume) of<br>" +
        "the incoming MIDI notes.</html>";

    static final String NOTE_RELEASE_TOOLTIP = "<html><b>Release</b><br>" +
        "Sets the gain to be multipled against the release velocity of the incoming MIDI notes.</html>";

    static final String NOTE_RELEASE_VARIANCE_TOOLTIP = "<html><b>Release Variance</b><br>" +
        "Sets the variance of random noise to be multipled against the release velocity of<br>" +
        "the incoming MIDI notes.</html>";

    static final String NOTE_LENGTH_TOOLTIP = "<html><b>Length</b><br>" +
        "Sets the length of all incoming MIDI notes, if <br>Change Length<b> is checked.<br><br>" +
        "Note that MIDI notes cannot realistically have zero length: it will be essentially<br>" +
        "1 Part per Quarter Note.</html>";

    static final String NOTE_CHANGE_LENGTH_TOOLTIP = "<html><b>Change Length</b><br>" +
        "Sets the whether the length of notes will be changed by <b>Length</b>.</html>";

    static final String DELAY_ORIGINAL_TOOLTIP = "<html><b>Original</b><br>" +
        "When checked, the original MIDI note will be played (along with possible delayed versions).</html>";

    static final String DELAY_INTERVAL_TOOLTIP = "<html><b>Interval</b><br>" +
        "Sets the time interval between successive delayed, repeated notes.</html>";

    static final String DELAY_CUT_TOOLTIP = "<html><b>Cut</b><br>" +
        "Sets the amount of reduction in velocity (volume) of the next delayed note relative to<br>" +
        "the current one.</html>";
        
    static final String DELAY_NUM_DELAYS_TOOLTIP = "<html><b>Num Delays</b><br>" +
        "Sets the number of delayed versions of a note to play.</html>";
        
    static final String DROP_CUT_TOOLTIP = "<html><b>Delete All</b><br>" +
        "If checked, then all events (note and non-note) will be deleted entirely.</html>";
        
    static final String DROP_PROBABILITY_TOOLTIP = "<html><b>Note Probability</b><br>" +
        "Sets the probability that notes (not non-note events) will be deleted.<br><br>" +
        "If <b>Cut</b> is checked, then Probability does nothing.</html>";
        
    static final String PARAMETER_TYPE_TOOLTIP = "<html><b>Type</b><br>" +
        "Specifies the type of parameter to be modified.</html>";

    static final String PARAMETER_PARAM_MSB_TOOLTIP = "<html><b>Param/MSB</b><br>" +
        "Sets the parameter number of the parameter to be modified:" +
        "<ul>" +
        "<li><b>Control Change (CC)</b>&nbsp;&nbsp; Sets the CC parameter number (0-127)." +
        "<li><b>Non-Registered Parameter Numbers (NRPN)</b>&nbsp;&nbsp; Sets the Most Significant Byte<br>" +
        "(MSB) of the parameter number." +
        "<li><b>Registered Parameter Numbers (RPN)</b>&nbsp;&nbsp; Sets the Most Significant Byte<br>" +
        "(MSB) of the parameter number." +
        "<li><b>All Others</b>&nbsp;&nbsp; Has no effect (there is no parameter number)." +
        "</ul>" +
        "</html>";

    static final String PARAMETER_PARAM_LSB_TOOLTIP = "<html><b>LSB</b><br>" +
        "Sets the Least Significant Byte (LSB) of the parameter number to be modified. This is only<br>" +
        "relevant for <b>Non-Registered Parameter Numbers (NRPN)</b> and <b>Registered Parameter Numbers (RPN)</b></html>";
        
    static final String PARAMETER_RATE_TOOLTIP = "<html><b>Rate</b><br>" +
        "Sets how often a new random value is chosen to be added to the parameter value for incoming events.<br><br>" + 
        "Though random noise is added to all values, it only changes every once in a while, according to the <b>Rate</b>.</html>";

    static final String PARAMETER_VARIANCE_TOOLTIP = "<html><b>Variance</b><br>" +
        "Sets the variance of random noise to be added to the parameter value for incoming events.<br><br>" + 
        "Though random noise is added to all values, it only changes every once in a while, according to the <b>Rate</b>.</html>";
        
    static final String MAP_MAP_TOOLTIP = "<html><b>Map</b><br>" +
        "Specifies the mapping function to use to modify the parameter value.</html>";

    static final String MAP_BY_TOOLTIP = "<html><b>By</b><br>" +
        "Some functions require that that the parameter value be mapped <b>by some amount</b>.<br>" +
        "The <b>By</b> value specifies that amount.</html>";

    static final String MAP_MIN_TOOLTIP = "<html><b>Min</b><br>" +
        "Specifies the minimum legal parameter value.<br><br>" +
        "This will both bound the parameter value before it goes into the mapping function,<br>" +
        "then scale the result after it comes out so that it's still between min and max.</html>";

    static final String MAP_MAX_TOOLTIP = "<html><b>Max</b><br>" +
        "Specifies the maximum legal parameter value.<br><br>" +
        "This will both bound the parameter value before it goes into the mapping function,<br>" +
        "then scale the result after it comes out so that it's still between min and max.</html>";

    static final String SCALE_KEY_TOOLTIP = "<html><b>Key</b><br>" +
        "Specifies key of the scale to restrict MIDI notes to.</html>";

    static final String SCALE_ROUND_TOOLTIP = "<html><b>Round</b><br>" +
        "Specifies how to constrain MIDI notes to notes in the scale.  Options:" +
        "<ul>" +
        "<li><b>Round Down</b>&nbsp;&nbsp; Sets the MIDI note to the closest note in the scale.<br>" +
        "below or equal to the MIDI note." +
        "<li><b>Round Up</b>&nbsp;&nbsp; Sets the MIDI note to the closest note in the scale.<br>" +
        "above or equal to the MIDI note." +
        "<li><b>Round Nearest/Down</b>&nbsp;&nbsp; Sets the MIDI note to the closest note in the scale,<br>" +
        "breaking ties by Rounding Down." +
        "<li><b>Round Nearest/Up</b>&nbsp;&nbsp; Sets the MIDI note to the closest note in the scale,<br>" +
        "breaking ties by Rounding Up." +
        "</ul>" +
        "</html>";

    static final String SCALE_PRESETS_TOOLTIP = "<html><b>Presets</b><br>" +
        "Various presets for the scale values.  The first presets are scales, and the later ones are chords.</html>";

    static final String SCALE_NOTE_TOOLTIP = "<html><b>Scale Note</b><br>" +
        "Notes in the scale to constrain incoming MIDI notes.</html>";

    static final String CHORD_INTERVAL_TOOLTIP = "<html><b>Chord/Interval</b><br>" +
        "The Interval or Chord to play in lieu of an incoming MIDI note.</html>";


    }
