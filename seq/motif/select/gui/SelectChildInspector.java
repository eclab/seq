/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.select.gui;

import seq.motif.select.*;
import seq.engine.*;
import seq.gui.*;
import seq.motif.parallel.gui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.locks.*;

public class SelectChildInspector extends WidgetList
    {
    public static final double MAX_RATE_LOG = Math.log(Select.Data.MAX_RATE);
    public static final String[] RATE_OPTIONS = ParallelChildInspector.RATE_OPTIONS; 
    public static final double[] RATES = ParallelChildInspector.RATES;

    Seq seq;
    Select select;
    SelectUI selectui;
    MotifUI motifui;
    Motif.Child child;
        
    SmallDial transpose;
    SmallDial gain;
    SmallDial rate;
    PushButton ratePresets;
    JComboBox out;
    StringField name;
        
    String[] defaults = new String[1 + Motif.NUM_PARAMETERS];

    Select.Data getData() { return ((Select.Data)(child.getData())); }
        
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

       
    public SelectChildInspector(Seq seq, Select select, MotifUI motifui, Motif.Child child, SelectUI selectui)
        {
        this.seq = seq;
        this.select = select;
        this.motifui = motifui;
        this.selectui = selectui;
        this.child = child;
        
        if (child == null)      // it's blank?
            {
            return;
            }
 
        buildDefaults(child.getMotif(), selectui.getMotif());
       
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
                    selectui.updateText();
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

            // This requires some explanation.  We are mapping the values we receive (0...1) in the Dial
            // into values 0.0625...16 using an exponential mapping.  However we also store negative values for
            // defaults.  This causes problems when we're queried for our value, but we're currently negative so
            // we compute the log of a negative value.  So instead here, in the initialization and in getValue(),
            // we return DEFAULT_RATE instead.   This issue doesn't come up when just doing 0...1 as normal.
            double d = getData().getRate(); 
            if (d < 0) d = Select.Data.DEFAULT_RATE;
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
                    try { double d = getData().getRate(); if (d < 0) return Select.Data.DEFAULT_RATE; else return (Math.log(d) + MAX_RATE_LOG) / MAX_RATE_LOG / 2.0;}
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
                    selectui.updateText();                  // FIXME: is this needed?
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
                    try { getData().setRate(ParallelChildInspector.getRate(SelectChildInspector.this, val));}
                    finally { lock.unlock(); }
                    rate.redraw();
                    }
                };
            ratePresets.setToolTipText(MIDI_CHANGES_RATE_PRESETS_TOOLTIP);

            transpose = new SmallDial(getData().getTranspose() / (double)Select.Data.MAX_TRANSPOSE / 2.0, defaults)
                {
                protected String map(double val) { return String.valueOf((int)(val * 2 * Select.Data.MAX_TRANSPOSE) - Select.Data.MAX_TRANSPOSE); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double d = getData().getTranspose();  if (d < 0) return 0;  else return d / (double)Select.Data.MAX_TRANSPOSE / 2.0; }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { getData().setTranspose((int)(val * 2 * Select.Data.MAX_TRANSPOSE)); }
                    finally { lock.unlock(); }
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) getData().setTranspose(-(val + 1)); }
                    finally { lock.unlock(); }
                    selectui.updateText();                  // FIXME: is this needed?
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

            gain = new SmallDial(getData().getGain() / Select.Data.MAX_GAIN, defaults)
                {
                protected String map(double val) { return super.map(val * Select.Data.MAX_GAIN); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return getData().getGain() / Select.Data.MAX_GAIN; }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { getData().setGain(val * Select.Data.MAX_GAIN); }
                    finally { lock.unlock(); }
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) getData().setGain(-(val + 1)); }
                    finally { lock.unlock(); }
                    selectui.updateText();                  // FIXME: is this needed?
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
                });            }
        finally { lock.unlock(); }
        out.setToolTipText(MIDI_CHANGES_OUT_TOOLTIP);

        JPanel ratePanel = new JPanel();
        ratePanel.setLayout(new BorderLayout());
        ratePanel.add(rate.getLabelledDial("0.0000"), BorderLayout.CENTER);   // so it stretches
        ratePanel.add(ratePresets, BorderLayout.EAST);       
        ratePanel.setToolTipText(MIDI_CHANGES_RATE_TOOLTIP);

        JPanel result = build(new String[] { "Nickname", "MIDI Changes", "Rate", "Transpose", "Gain", "Out"  }, 
            new JComponent[] 
                {
                name,
                null,                           // separator
                ratePanel,
                transpose.getLabelledDial("-24"),
                gain.getLabelledDial("0.0000"),
                out,
                });

        // the widgetlist is added SOUTH -- we need to change that to CENTER
        remove(result);
        add(result, BorderLayout.CENTER);

        add(new ArgumentList(seq, child, selectui.getMotif()), BorderLayout.NORTH);
        }
                
    public int getChildNum()
        {
        ReentrantLock lock = seq.getLock();
        lock.lock();
        // O(n) :-(
        try { return select.getChildren().indexOf(child); }
        finally { lock.unlock(); }
        }
                
    public void revise()
        {
        if (child == null) return;              // it's blank
        Seq old = seq;
        seq = null;
        ReentrantLock lock = old.getLock();
        lock.lock();
        try 
            { 
            out.setSelectedIndex(getData().getOut() + 1); 
            }
        finally { lock.unlock(); }                              
        seq = old;
        name.update();
        rate.redraw();
        transpose.redraw();
        gain.redraw();
        }

    static final String NICKNAME_TOOLTIP = "<html><b>Nickname</b><br>" +
        "Sets the nickname of the child in the Select.</html>";

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

    static final String MIDI_CHANGES_OUT_TOOLTIP = "<html><b>MIDI Changes: Out</b><br>" +
        "Changes the designated output of the MIDI generated by the motif node's playing.</html>";

    }
