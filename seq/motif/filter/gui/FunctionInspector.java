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
    public static final String[] INSPECTOR_NAMES = { "None", "Note", "Delay", "Drop", "Parameter" };
    public static final String[] PARAMETER_TYPES = { "Bend", "CC", "NRPN", "RPN", "Aftertouch" };
    public static final String[] NOTES = { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B" };

    Seq seq;
    Filter filter;
    int index;
    SubInspector subinspector;
    
    
    
    public FunctionInspector(Seq seq, Filter filter, int index)
        {
        this.seq = seq;
        this.filter = filter;
        this.index = index;        
        
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
        SmallDial transpose;
        SmallDial transposeVariance;
        SmallDial gain;
        SmallDial gainVariance;
        SmallDial releaseGain;
        SmallDial releaseGainVariance;
        TimeDisplay length;
                
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

            transpose = new SmallDial((func.getTranspose() + Filter.MAX_TRANSPOSE) / Filter.MAX_TRANSPOSE  / 2.0)
                {
                protected String map(double val) { return String.valueOf((int)(val * 2 * Filter.MAX_TRANSPOSE) - Filter.MAX_TRANSPOSE); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return (func.getTranspose() + Filter.MAX_TRANSPOSE) / Filter.MAX_TRANSPOSE  / 2.0; }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { func.setTranspose((int)(val * 2 * Filter.MAX_TRANSPOSE) - Filter.MAX_TRANSPOSE); }
                    finally { lock.unlock(); }
                    }
                };

            transposeVariance = new SmallDial(func.getTransposeVariance())
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
                };

            gain = new SmallDial(func.getGain() / Filter.MAX_TRANSPOSE_GAIN)
                {
                protected String map(double val) { return String.format("%.4f", val * Filter.MAX_TRANSPOSE_GAIN); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return func.getGain() / Filter.MAX_TRANSPOSE_GAIN; }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { func.setGain(val * Filter.MAX_TRANSPOSE_GAIN); }
                    finally { lock.unlock(); }
                    }
                };

            gainVariance = new SmallDial(func.getGainVariance())
                {
                protected String map(double val) { return String.format("%.4f", val * Filter.MAX_TRANSPOSE_GAIN); }
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
                };

            releaseGain = new SmallDial(func.getReleaseGain() / Filter.MAX_TRANSPOSE_GAIN)
                {
                protected String map(double val) { return String.format("%.4f", val * Filter.MAX_TRANSPOSE_GAIN); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return func.getReleaseGain() / Filter.MAX_TRANSPOSE_GAIN; }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { func.setReleaseGain(val * Filter.MAX_TRANSPOSE_GAIN); }
                    finally { lock.unlock(); }
                    }
                };

            releaseGainVariance = new SmallDial(func.getReleaseGainVariance())
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
                };

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
                                                                        
            build(new String[] { "", "Out", "Transpose", "Trans Var", "Gain", "Gain Var", "Release", "Rel Var", "Length"}, 
                new JComponent[] 
                    {
                    null,
                    out,
                    transpose.getLabelledDial("24"),
                    transposeVariance.getLabelledDial("0.0000"),
                    gain.getLabelledDial("0.0000"),
                    gainVariance.getLabelledDial("0.0000"),
                    releaseGain.getLabelledDial("0.0000"),
                    releaseGainVariance.getLabelledDial("0.0000"),
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
        //TimeDisplay initialDelay;
        TimeDisplay laterDelay;
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
            /*
              initialDelay = new TimeDisplay(Seq.PPQ / 4, seq)
              {
              public int getTime()
              {
              return func.getInitialDelay();
              }
                        
              public void setTime(int time)
              {
              func.setInitialDelay(time);
              }
              };
              initialDelay.setDisplaysTime(false);
            */
                                                                        
            laterDelay = new TimeDisplay(func.getLaterDelay(), seq)
                {
                public int getTime()
                    {
                    return func.getLaterDelay();
                    }
                        
                public void setTime(int time)
                    {
                    func.setLaterDelay(time);
                    }
                };
            laterDelay.setDisplaysTime(false);

            cut = new SmallDial(func.getCut())
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
                };
            numTimes = new SmallDial(func.getNumTimes() / (double)Filter.MAX_DELAY_NUM_TIMES)
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
                };

            build(new String[] { "", "Original", /*"Initial",*/ "Interval", "Cut", "Num Delays"}, 
                new JComponent[] 
                    {
                    null,
                    original,
                    //initialDelay,
                    laterDelay,
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
            if (laterDelay != null) laterDelay.revise();
            if (numTimes != null) numTimes.redraw();
            if (cut != null) cut.redraw();
            }               
        public String getName() { return "Delay"; }
        }

    public class DropInspector extends SubInspector
        {
        SmallDial probability;
                
        public DropInspector()
            {
            Filter.Drop func = (Filter.Drop)(filter.getFunction(index));
                        
            probability = new SmallDial(func.getProbability())
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
                };

            build(new String[] { "", "Probability"}, 
                new JComponent[] 
                    {
                    null,
                    probability.getLabelledDial("0.0000"),
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
                        
            distVar = new SmallDial(func.getDistVar())
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
                    return String.valueOf(v) + " / " +  NOTES[v % 12] + (v / 12);
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

            build(new String[] { "", "Type", "Param/Note/MSB", "LSB", "Variance", "Rate"}, 
                new JComponent[] 
                    {
                    null,
                    parameterType,
                    parameterMSB.getLabelledDial("128 / Cb10"),
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
                parameterMSB.setEnabled(true);
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


    static final String NAME_TOOLTIP = "<html><b>Name</b><br>" +
        "Sets the name of the Filter Child.  This will appear in the Motif List at left.</html>";

    static final String NICKNAME_TOOLTIP = "<html><b>Nickname</b><br>" +
        "Sets a nickname for the Filter Child, overriding its name as originally set.</html>";


    }
