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
    public static final double MAX_RATE = 16.0;
//    public static final double MAX_RATE_LOG = Math.log(Parallel.Data.MAX_RATE);
    public static final double RATE_LOG_FIX = 0.000000001;
    /*
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
    */

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
        16.0,
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
    JCheckBox repeat;
    JCheckBox mute;
    JPanel gainPanel;
    TimeDisplay delay;
    
    SmallDial transpose;
    SmallDial gain;
    SmallDial rate;
    SmallDial probability;
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
            name.setToolTipText(NICKNAME_TOOLTIP);

            delay = new TimeDisplay(getData().getDelay() , seq)
                {
                public int getTime()
                    {
                    return getData().getDelay(); 
                    }
                        
                public void setTime(int time)
                    {
                    getData().setDelay(time);
                    if (button != null) button.setDelay(time);
                    }
                };
            delay.setToolTipText(DELAY_TOOLTIP);

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
            quantization.setToolTipText(END_QUANTIZATION_TOOLTIP);


            // This requires some explanation.  We are mapping the values we receive (0...1) in the Dial
            // into values 0.0625...16 using an exponential mapping.  However we also store negative values for
            // defaults.  This causes problems when we're queried for our value, but we're currently negative so
            // we compute the log of a negative value.  So instead here, in the initialization and in getValue(),
            // we return DEFAULT_RATE instead.   This issue doesn't come up when just doing 0...1 as normal.
            /*
              double d = getData().getRate(); 
              if (d < 0) d = Parallel.Data.DEFAULT_RATE;
              rate = new SmallDial((Math.log(d) + MAX_RATE_LOG) / MAX_RATE_LOG / 2.0, defaults)
              {
              protected String map(double val) 
              {
              double d = Math.exp(val * 2 * MAX_RATE_LOG - MAX_RATE_LOG) + RATE_LOG_FIX;
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
              try { getData().setRate(Math.exp(val * 2 * MAX_RATE_LOG - MAX_RATE_LOG) + RATE_LOG_FIX);}
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
            */
            rate = new SmallDial(getData().getRate() / MAX_RATE, defaults)
                {
                public String map(double d)
                    {
                    return super.map(d * 16.0);
                    }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return getData().getRate() / MAX_RATE; }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { getData().setRate(val * MAX_RATE);}
                    finally { lock.unlock(); }
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) getData().setRate(-(val + 1)); }
                    finally { lock.unlock(); }
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
                    try { getData().setRate(getRate(ParallelChildInspector.this, val));}
                    finally { lock.unlock(); }
                    rate.redraw();
                    }
                };
            ratePresets.setToolTipText(MIDI_CHANGES_RATE_PRESETS_TOOLTIP);

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
            transpose.setToolTipText(MIDI_CHANGES_TRANSPOSE_TOOLTIP);

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
            gain.setToolTipText(MIDI_CHANGES_GAIN_TOOLTIP);


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
                    if (button != null) button.updateText();            // display the "MUTE"                        
                    }
                });
            mute.setToolTipText(MIDI_CHANGES_MUTE_TOOLTIP);

            gainPanel = new JPanel();
            gainPanel.setLayout(new BorderLayout());
            gainPanel.add(gain.getLabelledDial("0.0000"), BorderLayout.CENTER);
            gainPanel.add(mute, BorderLayout.EAST);
            gainPanel.setToolTipText(MIDI_CHANGES_GAIN_TOOLTIP);


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
            out.setToolTipText(MIDI_CHANGES_OUT_TOOLTIP);

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
            probability.setToolTipText(PROBABILITY_TOOLTIP);

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
                    if (button != null) button.updateText();
                    }
                });
            override.setToolTipText(OVERRIDE_TOOLTIP);

            repeat = new JCheckBox();
            repeat.setSelected(getData().getRepeat());
            repeat.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { getData().setRepeat(repeat.isSelected()); }
                    finally { lock.unlock(); }   
                    if (button != null) button.updateText();
                    }
                });
            repeat.setToolTipText(REPEAT_TOOLTIP);

            }
        finally { lock.unlock(); }

        JPanel ratePanel = new JPanel();
        ratePanel.setLayout(new BorderLayout());
        ratePanel.add(rate.getLabelledDial("0.0000"), BorderLayout.CENTER);   // so it stretches
        ratePanel.add(ratePresets, BorderLayout.EAST);       
        ratePanel.setToolTipText(MIDI_CHANGES_RATE_TOOLTIP);

        JPanel result = build(new String[] { "Nickname", "Delay", "Probability", "Repeat", "End Quantization", "Override", "MIDI Changes", "Rate", "Transpose", "Gain", "Out" }, 
            new JComponent[] 
                {
                name,
                delay,
                probability.getLabelledDial("0.0000"),
                repeat,
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
        if (delay != null) delay.revise();
        transpose.redraw();
        gain.redraw();
        }




    static final String NICKNAME_TOOLTIP = "<html><b>Nickname</b><br>" +
        "Sets the nickname of the Parallel child.</html>";

    static final String DELAY_TOOLTIP = "<html><b>Delay</b><br>" +
        "Sets the how long we delay before playing the Parallel child.<br><br>" +
        "<b>Note</b> that if the delay is very long (such as many Bars or longer)<br>" +
        "it will slide out of view.  In fact, if you change the Parts, the child may<br>" +
        "suddenly disappear (it has slid out of view).  You can scroll the Parallel<br>" +
        "to see it again.</html>";

    static final String PROBABILITY_TOOLTIP = "<html><b>Probability</b><br>" +
        "Sets the probability for a child.  This is used, along with the <b>Children Playing</b><br>" +
        "option in the Parallel Inspector above, to determine which children will play in parallel.</html>";

    static final String END_QUANTIZATION_TOOLTIP = "<html><b>End Quantization</b><br>" +
        "Sets the quantization for the ending of a child playing.  When a child finishes<br>" +
        "playing, its actual termination will occur at the End Quantization.  If the child<br>" +
        "is the longest playing child, this will impact on when the Parallel itself terminates.<br>"+
        "One of:" +
        "<ul>" +
        "<li>None (playing ends immediately)" +
        "<li>The next 16th note" +
        "<li>The next quarter note" +
        "<li>The next measure (bar) boundary" +
        "</ul></html>";

    static final String OVERRIDE_TOOLTIP = "<html><b>Override</b><br>" +
        "Sets whether this is an <b>override node</b>.  When an override node begins playing,<br>" +
        "all <b>later nodes</b> are immediately terminated and not allowed to play.<br><br>" +
        "Override nodes are useful for creating <b>ending variations</b>.  A node plays<br>" +
        "as normal, but we want to change its ending.  We do that by inserting a node with<br>" +
        "the ending changes, delay it to occur at the right time, and set it to override the<br>" +
        "original node.</html>";

    static final String REPEAT_TOOLTIP = "<html><b>Repeat</b><br>" +
        "Sets whether the child repeats (loops) after it has finished playing and there is time left.<br><br>" + 
        "Repeating children will be displayed in <font color=blue>blue</font> and with the word REPEATING.</html>";

    static final String MIDI_CHANGES_RATE_TOOLTIP = "<html><b>MIDI Changes: Rate</b><br>" +
        "Changes the rate of the motif node's playing (speeding it up or slowing it down)</html>";

    static final String MIDI_CHANGES_RATE_PRESETS_TOOLTIP = "<html><b>MIDI Changes: Rate Presets</b><br>" +
        "Presets for changing the rate of the motif node's playing (speeding it up or slowing it down).<br><br>" +
        "The first preset is <b>Custom...</b>, which allows you to change the playing so that<br>" +
        "Some number of quarter notes would be adjusted to squish or stretch them into a different number<br>" +
        "of quarter notes.  For example you can speed up the playing so that 8 quarter notes would be played<br>" +
        "in the time normally alloted for 4 quarter notes (that is, doubling the speed).</html>";
        
    static final String MIDI_CHANGES_TRANSPOSE_TOOLTIP = "<html><b>MIDI Changes: Transpose</b><br>" +
        "Transposes the notes generated by the motif node's playing.</html>";

    static final String MIDI_CHANGES_GAIN_TOOLTIP = "<html><b>MIDI Changes: Gain</b><br>" +
        "Changes the gain (volume) of the notes generated by the motif node's playing.</html>";

    static final String MIDI_CHANGES_MUTE_TOOLTIP = "<html><b>MIDI Changes: Mute</b><br>" +
        "Mutes all the notes generated by the motif node's playing.</html>";

    static final String MIDI_CHANGES_OUT_TOOLTIP = "<html><b>MIDI Changes: Out</b><br>" +
        "Changes the designated output of the MIDI generated by the motif node's playing.</html>";
    }
