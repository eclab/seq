/* 
   Copyright 2025 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.modulation.gui;

import seq.motif.modulation.*;
import seq.engine.*;
import seq.gui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.locks.*;
import seq.motif.parallel.gui.*;

public class ModulationInspector extends WidgetList
    {
    public static final double MAX_RATE = 16.0;

    public static final String[] RATE_OPTIONS = ParallelChildInspector.RATE_OPTIONS; 
    public static final double[] RATES = ParallelChildInspector.RATES;

    Seq seq;
    Modulation modulation;
    ModulationUI modulationui;

    SmallDial repeats;
    SmallDial transpose;
    SmallDial gain;
    SmallDial rate;
    PushButton ratePresets;
    JComboBox out;
    StringField name;

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

    public ModulationInspector(Seq seq, Modulation modulation, ModulationUI modulationui)
        {
        this.seq = seq;
        this.modulation = modulation;
        this.modulationui = modulationui;

        buildDefaults(modulation);

        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            name = new StringField(modulation.getName())
                {
                public String newValue(String newValue)
                    {
                    newValue = newValue.trim();
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { modulation.setName(newValue); }
                    finally { lock.unlock(); }
                    modulationui.updateText();
                    return newValue;
                    }
                                
                public String getValue() 
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return modulation.getName(); }
                    finally { lock.unlock(); }
                    }
                };
            }
        finally
            {
            lock.unlock();
            }
        name.setColumns(MotifUI.INSPECTOR_NAME_DEFAULT_SIZE);
        name.setToolTipText(NAME_TOOLTIP);


            repeats = new SmallDial(-1, defaults)
                {
                protected String map(double val) { return String.valueOf((int)(val * Modulation.MAX_REPEAT_VALUE)); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return (Math.min(Modulation.MAX_REPEAT_VALUE, modulation.getRepeats()) / (double)Modulation.MAX_REPEAT_VALUE); }
                    finally { lock.unlock(); }
                    }
                    
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { modulation.setRepeats((int)(val * Modulation.MAX_REPEAT_VALUE)); }
                    finally { lock.unlock(); }
                    modulationui.updateText();
                    }

                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    // We add 2 to skip PARAMETER_UNBOUND
                    try { if (val != SmallDial.NO_DEFAULT) modulation.setRepeats(-(val + 1)); }
                    finally { lock.unlock(); }
                    modulationui.updateText();
                    }
    
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    // We add 2 to skip PARAMETER_UNBOUND
                    try { double val = modulation.getRepeats(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };
            repeats.setToolTipText(INITIAL_REPEATS_TOOLTIP);

            rate = new SmallDial(modulation.getRate() / MAX_RATE, defaults)
                {
                public String map(double d)
                    {
                    return super.map(d * 16.0);
                    }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return modulation.getRate() / MAX_RATE; }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { modulation.setRate(val * MAX_RATE);}
                    finally { lock.unlock(); }
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) modulation.setRate(-(val + 1)); }
                    finally { lock.unlock(); }
                    }
    
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = modulation.getRate(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
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
                    try { modulation.setRate(ParallelChildInspector.getRate(ModulationInspector.this, val));}
                    finally { lock.unlock(); }
                    rate.redraw();
                    }
                };
            ratePresets.setToolTipText(MIDI_CHANGES_RATE_PRESETS_TOOLTIP);

            transpose = new SmallDial(modulation.getTranspose() / (double)Modulation.MAX_TRANSPOSE / 2.0, defaults)
                {
                protected String map(double val) { return String.valueOf((int)(val * 2 * Modulation.MAX_TRANSPOSE) - Modulation.MAX_TRANSPOSE); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double d = modulation.getTranspose();  if (d < 0) return 0;  else return d / (double)Modulation.MAX_TRANSPOSE / 2.0; }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { modulation.setTranspose((int)(val * 2 * Modulation.MAX_TRANSPOSE)); }
                    finally { lock.unlock(); }
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) modulation.setTranspose(-(val + 1)); }
                    finally { lock.unlock(); }
//                    seriesui.updateText();                  // FIXME: is this needed?
                    }
    
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = modulation.getTranspose(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };
            transpose.setToolTipText(MIDI_CHANGES_TRANSPOSE_TOOLTIP);

            gain = new SmallDial(modulation.getGain() / Modulation.MAX_GAIN, defaults)
                {
                protected String map(double val) { return super.map(val * Modulation.MAX_GAIN); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return modulation.getGain() / Modulation.MAX_GAIN; }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { modulation.setGain(val * Modulation.MAX_GAIN); }
                    finally { lock.unlock(); }
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) modulation.setGain(-(val + 1)); }
                    finally { lock.unlock(); }
//                    seriesui.updateText();                  // FIXME: is this needed?
                    }
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = modulation.getGain(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
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
            out.setSelectedIndex(modulation.getOut() + 1);
            out.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { modulation.setOut(out.getSelectedIndex() - 1); }              // -1 is DISABLED
                    finally { lock.unlock(); }                              
                    }
                });            


        JPanel ratePanel = new JPanel();
        ratePanel.setLayout(new BorderLayout());
        ratePanel.add(rate.getLabelledDial("0.0000"), BorderLayout.CENTER);   // so it stretches
        ratePanel.add(ratePresets, BorderLayout.EAST); 
        ratePanel.setToolTipText(MIDI_CHANGES_RATE_TOOLTIP);

        build(new String[] { "Name", "Repeats", "Child MIDI", "Rate", "Transpose", "Gain", "Out" }, 
            new JComponent[] 
                {
                name,
                repeats.getLabelledDial("127"),
                null,                   // Separator
                ratePanel,
                transpose.getLabelledDial("-24"),
                gain.getLabelledDial("0.0000"),
                out,
                });

        add(new DefaultParameterList(seq, modulationui), BorderLayout.SOUTH);
        }
                
    public void revise()
        {
        Seq old = seq;
        seq = null;
        ReentrantLock lock = old.getLock();
        lock.lock();
        try 
            {
            out.setSelectedIndex(modulation.getOut() + 1); 
            }
        finally { lock.unlock(); }                              
        seq = old;
        name.update();
        rate.redraw();
        transpose.redraw();
        gain.redraw();
        repeats.redraw();
        }

    static final String NAME_TOOLTIP = "<html><b>Name</b><br>" +
        "Sets the name of the Modulation.  This will appear in the Motif List at left.</html>";

    static final String MIDI_CHANGES_RATE_TOOLTIP = "<html><b>Child MIDI Changes: Rate</b><br>" +
        "Changes the rate of the child motif's playing (speeding it up or slowing it down)</html>";

    static final String MIDI_CHANGES_RATE_PRESETS_TOOLTIP = "<html><b>Child MIDI Changes: Rate Presets</b><br>" +
        "Presets for changing the rate of the child motif's playing (speeding it up or slowing it down).<br><br>" +
        "The first preset is <b>Custom...</b>, which allows you to change the playing so that<br>" +
        "Some number of quarter notes would be adjusted to squish or stretch them into a different number<br>" +
        "of quarter notes.  For example you can speed up the playing so that 8 quarter notes would be played<br>" +
        "in the time normally alloted for 4 quarter notes (that is, doubling the speed).</html>";
        
    static final String MIDI_CHANGES_TRANSPOSE_TOOLTIP = "<html><b>Child MIDI Changes: Transpose</b><br>" +
        "Transposes the notes generated by the child motif's playing.</html>";

    static final String MIDI_CHANGES_GAIN_TOOLTIP = "<html><b>Child MIDI Changes: Gain</b><br>" +
        "Changes the gain (volume) of the notes generated by the child motif's playing.</html>";

    static final String MIDI_CHANGES_OUT_TOOLTIP = "<html><b>Child MIDI Changes: Out</b><br>" +
        "Changes the designated output of the MIDI generated by the child motif's playing.</html>";

    static final String INITIAL_REPEATS_TOOLTIP = "<html><b>Initial Repeats</b><br>" +
        "Sets how often the child motif will repeat before the Modulation finishes.</html>";
    }
