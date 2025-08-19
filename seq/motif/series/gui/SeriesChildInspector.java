/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.series.gui;

import seq.motif.series.*;
import seq.engine.*;
import seq.gui.*;
import seq.motif.parallel.gui.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.locks.*;

public class SeriesChildInspector extends WidgetList
    {
    public static final double MAX_RATE_LOG = Math.log(Series.Data.MAX_RATE);
    public static final String[] RATE_OPTIONS = ParallelChildInspector.RATE_OPTIONS; 
    public static final double[] RATES = ParallelChildInspector.RATES;
    
    public static final String[] QUANTIZATIONS = { "None", "16th Note", "Quarter Note", "Measure" };

    Seq seq;
    Series series;
    SeriesUI seriesui;
    MotifUI motifui;
    Motif.Child child;
        
    SmallDial repeats;
    SmallDial probability;
    JCheckBox untilTrigger;
    SmallDial transpose;
    SmallDial gain;
    SmallDial rate;
    PushButton ratePresets;
    JComboBox out;
    StringField name;
    JComboBox quantization;
    JComboBox scale;
    JComboBox root;
    Box scaleAndRoot;
    JPanel repeatPanel;
    JCheckBox start;

    WidgetList weights;
    String[] defaults = new String[1 + Motif.NUM_PARAMETERS];
        
    Series.Data getData() { return ((Series.Data)(child.getData())); }
        
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

    public SeriesChildInspector(Seq seq, Series series, MotifUI motifui, Motif.Child child, SeriesUI seriesui)
        {
        this.seq = seq;
        this.series = series;
        this.seriesui = seriesui;
        this.motifui = motifui;
        this.child = child;
        buildDefaults(child.getMotif(), seriesui.getMotif());
                
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
                    try 
                        { 
                        child.setNickname(newValue); 
                        seriesui.setChildInspector(new SeriesChildInspector(seq, series, motifui, child, seriesui));            // this should reset the markov weight names
                        }
                    finally { lock.unlock(); }
                    seriesui.updateText();
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
            name.setToolTipText(NAME_TOOLTIP);

            probability = new SmallDial(-1, defaults)
                {
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return getData().getRepeatProbability(); }
                    finally { lock.unlock(); }
                    }
                    
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { getData().setRepeatProbability(val);}
                    finally { lock.unlock(); }
                    seriesui.updateText();
                    }

                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    // We add 2 to skip PARAMETER_UNBOUND
                    try { if (val != SmallDial.NO_DEFAULT) getData().setRepeatProbability(-(val + 1)); }
                    finally { lock.unlock(); }
                    seriesui.updateText();                  // FIXME: is this needed?
                    }
    
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    // We add 2 to skip PARAMETER_UNBOUND
                    try { double val = getData().getRepeatProbability(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };
            probability.setToolTipText(REPEAT_PROBABILITY_TOOLTIP);

            repeats = new SmallDial(-1, defaults)
                {
                protected String map(double val) { return String.valueOf((int)(val * Series.Data.MAX_REPEAT_VALUE)); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return (Math.min(Series.Data.MAX_REPEAT_VALUE, getData().getRepeatAtLeast()) / (double)Series.Data.MAX_REPEAT_VALUE); }
                    finally { lock.unlock(); }
                    }
                    
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { getData().setRepeatAtLeast((int)(val * Series.Data.MAX_REPEAT_VALUE)); }
                    finally { lock.unlock(); }
                    seriesui.updateText();
                    }

                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    // We add 2 to skip PARAMETER_UNBOUND
                    try { if (val != SmallDial.NO_DEFAULT) getData().setRepeatAtLeast(-(val + 1)); }
                    finally { lock.unlock(); }
                    seriesui.updateText();
                    }
    
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    // We add 2 to skip PARAMETER_UNBOUND
                    try { double val = getData().getRepeatAtLeast(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };
            repeats.setToolTipText(INITIAL_REPEATS_TOOLTIP);

            untilTrigger = new JCheckBox("Until Trigger " + ((SeriesClip.TRIGGER_PARAMETER) + 1));
            untilTrigger.setSelected(getData().getRepeatUntilTrigger());
            untilTrigger.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { getData().setRepeatUntilTrigger(untilTrigger.isSelected()); }
                    finally { lock.unlock(); }                              
                    }
                });
            untilTrigger.setToolTipText(UNTIL_TRIGGER_8_TOOLTIP);

                
            repeatPanel = new JPanel();
            repeatPanel.setLayout(new BorderLayout());
            repeatPanel.add(repeats.getLabelledDial("128"), BorderLayout.CENTER );
            repeatPanel.add(untilTrigger, BorderLayout.EAST);
            repeatPanel.setToolTipText(INITIAL_REPEATS_TOOLTIP);

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
            quantization.setToolTipText(QUANTIZATION_TOOLTIP);


            // This requires some explanation.  We are mapping the values we receive (0...1) in the Dial
            // into values 0.0625...16 using an exponential mapping.  However we also store negative values for
            // defaults.  This causes problems when we're queried for our value, but we're currently negative so
            // we compute the log of a negative value.  So instead here, in the initialization and in getValue(),
            // we return DEFAULT_RATE instead.   This issue doesn't come up when just doing 0...1 as normal.
            double d = getData().getRate(); 
            if (d < 0) d = Series.Data.DEFAULT_RATE;
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
                    try { double d = getData().getRate(); if (d < 0) return Series.Data.DEFAULT_RATE; else return (Math.log(d) + MAX_RATE_LOG) / MAX_RATE_LOG / 2.0;}
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
                    seriesui.updateText();                  // FIXME: is this needed?
                    }
    
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = getData().getRate(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };
            rate.setToolTipText(MIDI_CHANGES_RATE_TOOLTIP);

            ratePresets = new PushButton("Presets...", RATE_OPTIONS)
                {
                public void perform(int val)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { getData().setRate(ParallelChildInspector.getRate(SeriesChildInspector.this, val));}
                    finally { lock.unlock(); }
                    rate.redraw();
                    }
                };
            ratePresets.setToolTipText(MIDI_CHANGES_RATE_PRESETS_TOOLTIP);

            transpose = new SmallDial(getData().getTranspose() / (double)Series.Data.MAX_TRANSPOSE / 2.0, defaults)
                {
                protected String map(double val) { return String.valueOf((int)(val * 2 * Series.Data.MAX_TRANSPOSE) - Series.Data.MAX_TRANSPOSE); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double d = getData().getTranspose();  if (d < 0) return 0;  else return d / (double)Series.Data.MAX_TRANSPOSE / 2.0; }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { getData().setTranspose((int)(val * 2 * Series.Data.MAX_TRANSPOSE)); }
                    finally { lock.unlock(); }
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) getData().setTranspose(-(val + 1)); }
                    finally { lock.unlock(); }
                    seriesui.updateText();                  // FIXME: is this needed?
                    }
    
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = getData().getTranspose(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };
            transpose.setToolTipText(MIDI_CHANGES_TRANSPOSE_TOOLTIP);

            root = new JComboBox(Series.Data.rootNames);
            root.setMaximumRowCount(Series.Data.rootNames.length);
            root.setSelectedIndex(getData().getRoot());
            root.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { getData().setRoot(root.getSelectedIndex()); }
                    finally { lock.unlock(); }                              
                    }
                });            

            scale = new JComboBox(Series.Data.scaleNames);
            scale.setMaximumRowCount(Series.Data.scaleNames.length);
            scale.setSelectedIndex(getData().getScale());
            scale.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { getData().setScale(scale.getSelectedIndex()); }
                    finally { lock.unlock(); }                              
                    }
                }); 
                
            scaleAndRoot = new Box(BoxLayout.X_AXIS);
            scaleAndRoot.add(root);
            scaleAndRoot.add(scale);           

            gain = new SmallDial(getData().getGain() / Series.Data.MAX_GAIN, defaults)
                {
                protected String map(double val) { return super.map(val * Series.Data.MAX_GAIN); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return getData().getGain() / Series.Data.MAX_GAIN; }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { getData().setGain(val * Series.Data.MAX_GAIN); }
                    finally { lock.unlock(); }
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) getData().setGain(-(val + 1)); }
                    finally { lock.unlock(); }
                    seriesui.updateText();                  // FIXME: is this needed?
                    }
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = getData().getGain(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };
            gain.setToolTipText(MIDI_CHANGES_GAIN_TOOLTIP);

            Out[] seqOuts = seq.getOuts();
            String[] outs = new String[seqOuts.length + 1];
            outs[0] = "<html><i>Don't Change</i></html>";
            for(int i = 0; i < seqOuts.length; i++)
                {
                // We have to make these strings unique, or else the combo box doesn't give the right selected index, Java bug
                outs[i+1] = "" + (i+1) + ": " + seqOuts[i].toString();
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
            }
        finally { lock.unlock(); }
        out.setToolTipText(MIDI_CHANGES_OUT_TOOLTIP);
        
        start = new JCheckBox("");
        start.setSelected(getData().getStart());
        start.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                if (seq == null) return;
                ReentrantLock lock = seq.getLock();
                lock.lock();
                try { getData().setStart(start.isSelected(), series); }         // FIXME: is this the series or the child I should be passing in?
                finally { lock.unlock(); }                              
                }
            });
        start.setToolTipText(START_HERE_TOOLTIP);

        weights = new WidgetList();
        lock = seq.getLock();
        lock.lock();
        try
            {
            ArrayList<Motif.Child> children = series.getChildren();
            int len = children.size();
            String[] names = new String[len];
            JComponent[] comps = new JComponent[len];
            for(int i = 0; i < len; i++)
                {
                final int _i = i;
                names[i] = children.get(i).getCurrentName();
                SmallDial foo = new SmallDial(getData().getWeights()[i])
                    {
                    public double getValue() 
                        { 
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { return getData().getWeights()[_i]; }
                        finally { lock.unlock(); }
                        }
                    public void setValue(double val) 
                        { 
                        if (seq == null) return;
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { getData().getWeights()[_i] = val; }
                        finally { lock.unlock(); }
                        seriesui.updateText();
                        }
                    };
                foo.setToolTipText(MARKOV_WEIGHTS_TOOLTIP);
                comps[i] = foo.getLabelledDial("0.0000");
                }
            weights.build(names, comps);
            }
        finally { lock.unlock(); }

        JPanel ratePanel = new JPanel();
        ratePanel.setLayout(new BorderLayout());
        ratePanel.add(rate.getLabelledDial("0.0000"), BorderLayout.CENTER);   // so it stretches
        ratePanel.add(ratePresets, BorderLayout.EAST); 
        ratePanel.setToolTipText(MIDI_CHANGES_RATE_TOOLTIP);

        JPanel result = build(new String[] { "Nickname", "Initial Repeats", "Repeat Probability", "End Quantization", "Start Here", "MIDI Changes", "Rate", "Transpose", /*"Restrict",*/ "Gain", "Out"}, 
            new JComponent[] 
                {
                name,
//                repeats.getLabelledDial("128"),
                repeatPanel,
                probability.getLabelledDial("0.0000"),
                quantization,
                start,
                null,                   // Separator
                ratePanel,
                transpose.getLabelledDial("-24"),
                //scaleAndRoot,
                gain.getLabelledDial("0.0000"),
                out,
                });

        // the widgetlist is added NORTH -- we need to change that to CENTER
        remove(result);
        add(result, BorderLayout.CENTER);
                
        // Now we add the markov weights
        weights.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8, 8, 0, 8),
                BorderFactory.createTitledBorder("<html><i>&nbsp;Markov Weights&nbsp;</i></html")));
        add(weights, BorderLayout.SOUTH);
        
        add(new ArgumentList(seq, child, seriesui.getMotif()), BorderLayout.NORTH);
        }
                
    public int getChildNum()
        {
        ReentrantLock lock = seq.getLock();
        lock.lock();
        // O(n) :-(
        try { return series.getChildren().indexOf(child); }
        finally { lock.unlock(); }
        }
          
          
    public void updateMarkovLabels()
        {
        String[] l = null;
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            { 
            // is this too slow?
            ArrayList<Motif.Child> children = series.getChildren();
            int len = children.size();
            l = new String[len];
            for(int i = 0; i < len; i++)
                {
                l[i] = children.get(i).getCurrentName();
                }
            }
        finally { lock.unlock(); }   

        JLabel[] labels = weights.getLabels();
        for(int i = 0; i < l.length; i++)
            {
            labels[i].setText(l[i]);
            }
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
            scale.setSelectedIndex(getData().getScale()); 
            root.setSelectedIndex(getData().getRoot()); 
            start.setSelected(getData().getStart());
            }
        finally { lock.unlock(); }   

        seq = old;
        name.update();
        rate.redraw();
        transpose.redraw();
        gain.redraw();
        probability.redraw();
        repeats.redraw();
        }

    static final String NAME_TOOLTIP = "<html><b>Nickname</b><br>" +
        "Sets the nickname of the child in the Series.</html>";

    static final String INITIAL_REPEATS_TOOLTIP = "<html><b>Repeat Probability</b><br>" +
        "Sets how often the child motif will repeat before transitioning to the next child.<br><br>" + 
        "Repeating works as follows:" +
        "<ol>" +
        "<li>The child motif is played once." +
        "<li>Then the child motif is played <b>Initial Repeats</b> more times." +
        "<li>Then a coin is flipped repeatedly with <b>Repeat Probability</b> of being heads.<br>" +
        "If it comes up heads <i>N</i> times before the first tails, then the child motif is<br>" +
        "played <i>N</i> more times." + 
        "<li>Then the child motif transitions to the next motif." +
        "</ol>" +
        "All the while, if a 1.0 is sent to <b>Parameter 8</b> and <b>Until Trigger 8</b> is<br>" +
        "selected, then the child motif transitions to the next motif with no further repeats.</html>";
        
    static final String REPEAT_PROBABILITY_TOOLTIP = "<html><b>Initial Repeats</b><br>" +
        "Sets how often the child motif will repeat before transitioning to the next child.<br><br>" + 
        "Repeating works as follows:" +
        "<ol>" +
        "<li>The child motif is played once." +
        "<li>Then the child motif is played <b>Initial Repeats</b> more times." +
        "<li>Then a coin is flipped repeatedly with <b>Repeat Probability</b> of being heads.<br>" +
        "If it comes up heads <i>N</i> times before the first tails, then the child motif is<br>" +
        "played <i>N</i> more times." + 
        "<li>Then the child motif transitions to the next motif." +
        "</ol>" +
        "All the while, if a 1.0 is sent to <b>Parameter 8</b> and <b>Until Trigger 8</b> is<br>" +
        "selected, then the child motif transitions to the next motif with no further repeats.</html>";
        
    static final String UNTIL_TRIGGER_8_TOOLTIP = "<html><b>Until Trigger 8</b><br>" +
        "If checked, when a 1.0 is sent to <b>Parameter 8</b>, the child motif will transition to<br>" + 
        "the next node regardless of the number of <b>initial repeats</b> or the <b>repeat probability</b>.<br>" + 
        "Repeating works as follows:" +
        "<ol>" +
        "<li>The child motif is played once." +
        "<li>Then the child motif is played <b>Initial Repeats</b> more times." +
        "<li>Then a coin is flipped repeatedly with <b>Repeat Probability</b> of being heads.<br>" +
        "If it comes up heads <i>N</i> times before the first tails, then the child motif is<br>" +
        "played <i>N</i> more times." + 
        "<li>Then the child motif transitions to the next motif." +
        "</ol>" +
        "All the while, if a 1.0 is sent to <b>Parameter 8</b> and <b>Until Trigger 8</b> is<br>" +
        "selected, then the child motif transitions to the next motif with no further repeats.</html>";
        
    static final String QUANTIZATION_TOOLTIP = "<html><b>Quantization</b><br>" +
        "Delays the transition to the next child until it falls on a given time event:" + 
        "<ul>" +
        "<li>None (transition is not delayed)" +
        "<li>The next 16th note" +
        "<li>The next quarter note" + 
        "<li>The start of the next measure (bar)</html>";
        
    static final String START_HERE_TOOLTIP = "<html><b>Start Here</b><br>" +
        "When checked, instructs the Series to start with the given child motif rather than<br>" +
        "the child motif it normally starts with.</html>";
        
    static final String MIDI_CHANGES_RATE_TOOLTIP = "<html><b>MIDI Changes: Rate</b><br>" +
        "Changes the rate of the child motif's playing (speeding it up or slowing it down)</html>";

    static final String MIDI_CHANGES_RATE_PRESETS_TOOLTIP = "<html><b>MIDI Changes: Rate Presets</b><br>" +
        "Presets for changing the rate of the child motif's playing (speeding it up or slowing it down).<br><br>" +
        "The first preset is <b>Custom...</b>, which allows you to change the playing so that<br>" +
        "Some number of quarter notes would be adjusted to squish or stretch them into a different number<br>" +
        "of quarter notes.  For example you can speed up the playing so that 8 quarter notes would be played<br>" +
        "in the time normally alloted for 4 quarter notes (that is, doubling the speed).</html>";
        
    static final String MIDI_CHANGES_TRANSPOSE_TOOLTIP = "<html><b>MIDI Changes: Transpose</b><br>" +
        "Transposes the notes generated by the child motif's playing.</html>";

    static final String MIDI_CHANGES_GAIN_TOOLTIP = "<html><b>MIDI Changes: Gain</b><br>" +
        "Changes the gain (volume) of the notes generated by the child motif's playing.</html>";

    static final String MIDI_CHANGES_OUT_TOOLTIP = "<html><b>MIDI Changes: Out</b><br>" +
        "Changes the designated output of the MIDI generated by the child motif's playing.</html>";

    static final String MARKOV_WEIGHTS_TOOLTIP = "<html><b>Markov Weight</b><br>" +
        "The likelihood that after this child has finished, we transition to the child indicated.<br>" +
        "This only has an impact if the <b>Mode</b> is set to <b>Random Markov</b>.  Probabilities are<br>" +
        "determined by adding all the weights up and dividing each of them by the sum.  If the weights<br>" +
        "are all zero, a new child is picked uniformly at random.</html>";
        
    }
