/* 
   Copyright 2025 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.arpeggio.gui;

import seq.motif.arpeggio.*;
import seq.engine.*;
import seq.gui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.locks.*;

public class ArpeggioInspector extends WidgetList
    {
    public static final String[] ARP_TYPES = { "Up", "Down", "Up-Down", "Up-Down-Plus", "Random", "Pattern" };
    Seq seq;
    Arpeggio arpeggio;
    ArpeggioUI arpeggioui;

    StringField name;
    JComboBox out;
    JCheckBox omni;
    JCheckBox newChordReset;
    JComboBox arp;
    SmallDial length;
    SmallDial octaves;
    TimeDisplay rate;
                
    public ArpeggioInspector(Seq seq, Arpeggio arpeggio, ArpeggioUI arpeggioui)
        {
        this.seq = seq;
        this.arpeggio = arpeggio;
        this.arpeggioui = arpeggioui;

        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            name = new StringField(arpeggio.getName())
                {
                public String newValue(String newValue)
                    {
                    newValue = newValue.trim();
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { arpeggio.setName(newValue); }
                    finally { lock.unlock(); }
                    arpeggioui.updateText();
                    return newValue;
                    }
                                
                public String getValue() 
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return arpeggio.getName(); }
                    finally { lock.unlock(); }
                    }
                };
            name.setColumns(MotifUI.INSPECTOR_NAME_DEFAULT_SIZE);
 
 
            Out[] seqOuts = seq.getOuts();
            String[] outs = new String[seqOuts.length];
            for(int i = 0; i < seqOuts.length; i++)
                {
                outs[i] = "" + (i + 1) + ": " + seqOuts[i].toString();
                }

            out = new JComboBox(outs);
            out.setSelectedIndex(arpeggio.getOut());
            out.setMaximumRowCount(outs.length);
            out.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { arpeggio.setOut(out.getSelectedIndex()); }
                    finally { lock.unlock(); }                              
                    }
                });

            omni = new JCheckBox();
            omni.setSelected(arpeggio.isOmni());
            omni.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { arpeggio.setOmni(omni.isSelected()); }
                    finally { lock.unlock(); }                              
                    }
                });

            newChordReset = new JCheckBox();
            newChordReset.setSelected(arpeggio.getNewChordReset());
            newChordReset.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { arpeggio.setNewChordReset(newChordReset.isSelected()); }
                    finally { lock.unlock(); }                              
                    }
                });

            arp = new JComboBox(ARP_TYPES);
            arp.setSelectedIndex(arpeggio.getArpeggioType());
            arp.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { arpeggio.setArpeggioType(arp.getSelectedIndex()); }
                    finally { lock.unlock(); }                              
                    }
                });


            octaves = new SmallDial((arpeggio.getOctaves() - 1) / ((double)Arpeggio.MAX_OCTAVES - 1))
                {
                protected String map(double val) 
                    {
                    return "" + (int)(val * ((double)Arpeggio.MAX_OCTAVES - 1) + 1);
                    }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return (arpeggio.getOctaves() - 1) / ((double)Arpeggio.MAX_OCTAVES - 1); }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { arpeggio.setOctaves((int)(val * ((double)Arpeggio.MAX_OCTAVES - 1) + 1)); }
                    finally { lock.unlock(); }
                    arpeggioui.getPatternGrid().repaint();
                    }
                };

            length = new SmallDial((arpeggio.getPatternLength() - 1) / ((double)Arpeggio.MAX_PATTERN_LENGTH - 1))
                {
                protected String map(double val) 
                    {
                    return "" + (int)(val * ((double)Arpeggio.MAX_PATTERN_LENGTH - 1) + 1);
                    }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return (arpeggio.getPatternLength() - 1) / ((double)Arpeggio.MAX_PATTERN_LENGTH - 1); }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { arpeggio.setPatternLength((int)(val * ((double)Arpeggio.MAX_PATTERN_LENGTH - 1) + 1)); }
                    finally { lock.unlock(); }
                    arpeggioui.getPatternGrid().repaint();
                    }
                };

            /// FIXME: TimeDisplay cannot have a minimum right now.  So we just hack it so
            /// so that 0 = 1...
                
            rate = new TimeDisplay(arpeggio.getRate(), seq, true)
                {
                public int getTime()
                    {
                    return arpeggio.getRate();
                    }
                        
                public void setTime(int time)
                    {
                    if (time == 0) time = 1;
                    arpeggio.setRate(time);
                    }
                };
                                                                        
            }
        finally { lock.unlock(); }

        name.setToolTipText(NAME_TOOLTIP);

        build(new String[] { "Name", "Out", "Omni Input", "Step Rate", "Arpeggio Type", "Octaves", "Pattern Length", "New Chord Reset"}, 
            new JComponent[] 
                {
                name,
                out,
                omni,
                rate,
                arp,
                octaves.getLabelledDial("" + Arpeggio.MAX_OCTAVES),
                length.getLabelledDial("" + Arpeggio.MAX_PATTERN_LENGTH),
                newChordReset,
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
            out.setSelectedIndex(arpeggio.getOut()); 
            omni.setSelected(arpeggio.isOmni()); 
            newChordReset.setSelected(arpeggio.getNewChordReset());
            }
        finally { lock.unlock(); }                              
        seq = old;
        name.update();
        if (length != null) length.redraw();
        if (octaves != null) octaves.redraw();
        }

    static final String NAME_TOOLTIP = "<html><b>Name</b><br>" +
        "Sets the name of the Arpeggio.  This will appear in the Motif List at left.</html>";

    }
