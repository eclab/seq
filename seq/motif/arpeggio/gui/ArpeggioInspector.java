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
    TimeDisplay activeFrom;
    TimeDisplay activeTo;
    JCheckBox activeAlways;
    SmallDial velocity;
    JCheckBox asPlayed;

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
                    return String.valueOf((int)(val * ((double)Arpeggio.MAX_OCTAVES - 1) + 1));
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
                    return String.valueOf((int)(val * ((double)Arpeggio.MAX_PATTERN_LENGTH - 1) + 1));
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

            velocity = new SmallDial(arpeggio.getVelocity() / 127.0)
                {
                protected String map(double val) 
                    {
                    return String.valueOf((int)(val * 127.0));
                    }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return arpeggio.getVelocity() / 127.0; }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { arpeggio.setVelocity((int)(val * 127.0)); }
                    finally { lock.unlock(); }
                    }
                };

            asPlayed = new JCheckBox("As Played");
            asPlayed.setSelected(arpeggio.getVelocityAsPlayed());
            asPlayed.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { arpeggio.setVelocityAsPlayed(asPlayed.isSelected()); }
                    finally { lock.unlock(); }                              
                    }
                });

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

            activeFrom = new TimeDisplay(arpeggio.getFrom(), seq, true)
                {
                public int getTime()
                    {
                    return arpeggio.getFrom();
                    }
                        
                public void setTime(int time)
                    {
                    arpeggio.setFrom(time);
                    }
                };
            activeFrom.setDisplaysTime(true);

            activeTo = new TimeDisplay(arpeggio.getTo(), seq, true)
                {
                public int getTime()
                    {
                    return arpeggio.getTo();
                    }
                        
                public void setTime(int time)
                    {
                    arpeggio.setTo(time);
                    }
                };
            activeTo.setDisplaysTime(true);                                            

            activeAlways = new JCheckBox();
            activeAlways.setSelected(arpeggio.isAlways());
            activeAlways.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { arpeggio.setAlways(activeAlways.isSelected()); }
                    finally { lock.unlock(); }                              
                    }
                });

            }
        finally { lock.unlock(); }
        
        JPanel velocityPanel = new JPanel();
        velocityPanel.setLayout(new BorderLayout());
        velocityPanel.add(velocity.getLabelledDial("128"), BorderLayout.WEST);
        velocityPanel.add(asPlayed, BorderLayout.EAST);

        name.setToolTipText(NAME_TOOLTIP);
        out.setToolTipText(OUT_TOOLTIP);
        omni.setToolTipText(OMNI_INPUT_TOOLTIP);
        rate.setToolTipText(STEP_RATE_TOOLTIP);
        arp.setToolTipText(ARPEGGIO_TYPE_TOOLTIP);
        octaves.setToolTipText(OCTAVES_TOOLTIP);
        length.setToolTipText(PATTERN_LENGTH_TOOLTIP);
        velocity.setToolTipText(VELOCITY_TOOLTIP);
        asPlayed.setToolTipText(AS_PLAYED_TOOLTIP);
        velocityPanel.setToolTipText(VELOCITY_TOOLTIP);
        newChordReset.setToolTipText(NEW_CHORD_RESET_TOOLTIP);
        activeAlways.setToolTipText(ALWAYS_TOOLTIP);
        activeFrom.setToolTipText(FROM_TOOLTIP);
        activeTo.setToolTipText(FROM_TOOLTIP);

        build(new String[] { "Name", "Out", "Omni Input", "Step Rate", "Arpeggio Type", "Octaves", "Pattern Length", "Velocity", "New Chord Reset", 
                "Activity", "Always", "From", "To"}, 
            new JComponent[] 
                {
                name,
                out,
                omni,
                rate,
                arp,
                octaves.getLabelledDial(String.valueOf(Arpeggio.MAX_OCTAVES)),
                length.getLabelledDial(String.valueOf(Arpeggio.MAX_PATTERN_LENGTH)),
                velocityPanel,
                newChordReset,
                null,                           // separator
                activeAlways,
                activeFrom,
                activeTo
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
            activeAlways.setSelected(arpeggio.isAlways());
            }
        finally { lock.unlock(); }                              
        seq = old;
        name.update();
        if (velocity != null) velocity.redraw();
        if (length != null) length.redraw();
        if (octaves != null) octaves.redraw();
        if (activeFrom != null) activeFrom.revise();
        if (activeTo != null) activeTo.revise();
        }


    static final String NAME_TOOLTIP = "<html><b>Name</b><br>" +
        "Sets the name of the Arpeggio.  This will appear in the Motif List at left.</html>";

    static final String OUT_TOOLTIP = "<html><b>Out</b><br>" +
        "Sets the MIDI output for the Arpeggio.  This also may restrict which notes are<br>"+
        "arpeggiated (see <b>Omni Input</b>).</html>";
        
    static final String OMNI_INPUT_TOOLTIP = "<html><b>Omni Input</b><br>" +
        "If checked, all notes from the underlying child Motif will be converted into arpeggios.<br>" +
        "Otherwise, only the notes designated for the <b>Out</b> will be arpeggiated,<br>" +
        "and the others will be simply passed through.</html>";

    static final String STEP_RATE_TOOLTIP = "<html><b>Step Rate</b><br>" +
        "Sets amount of time between each step of the arpeggio.</html>";
        
    static final String ARPEGGIO_TYPE_TOOLTIP = "<html><b>Arpeggio Type</b><br>" +
        "Sets the arpeggio type" + 
        "<ul>" +
        "<li><b>Up</b>&nbsp;&nbsp;Each note in the chord is played in order, lowest to highest." +
        "<li><b>Down</b>&nbsp;&nbsp;Each note in the chord is played in order, highest to lowest." +
        "<li><b>Up-Down</b>&nbsp;&nbsp;Up and then Down, except the top note and bottom note are not played twice." +
        "<li><b>Up-Down-Plus</b>&nbsp;&nbsp;Up, then the lowest note is transposed and played again at the top, then Down." +
        "<li><b>Random</b>&nbsp;&nbsp;Chord notes are played randomly.  Seq tries to not play the same note twice in a row." +
        "<li><b>Pattern</b>&nbsp;&nbsp;Chord notes are using the Pattern Grid at left." +
        "</ul></html>";

    static final String OCTAVES_TOOLTIP = "<html><b>Octaves</b><br>" +
        "Sets the number of octaves that the arpeggio will repeat.</html>";

    static final String PATTERN_LENGTH_TOOLTIP = "<html><b>Pattern Length</b><br>" +
        "Sets the length of the Pattern in the Pattern Grid at left.</html>";

    static final String VELOCITY_TOOLTIP = "<html><b>Velocity</b><br>" +
        "Sets the Velocity (Volume) of all arpeggiated notes.<br><br>" +
        "Only has an effect if <b>As Played</b> is unchecked.</html>";

    static final String AS_PLAYED_TOOLTIP = "<html><b>As Played</b><br>" +
        "Determines whether the Velocity (Volume) of the arpeggiated notes is determined<br>" +
        "by the underlying notes of the child Motif, or by the <b>Velocity</b> knob.</html>";

    static final String NEW_CHORD_RESET_TOOLTIP = "<html><b>New Chord Reset</b><br>" +
        "Sets whether the arpeggio resets when all underlying notes are finished</br>" +
        "and new ones are played in the underlying child Motif.</html>";

    static final String ALWAYS_TOOLTIP = "<html><b>Always</b><br>" +
        "Sets whether the Arpeggio plays for the full length of time of its underlying Child Motif.</html>";

    static final String FROM_TOOLTIP = "<html><b>From</b><br>" +
        "Sets when in the underlying Child Motif the Arpeggio starts arpeggiating its notes.<br><br>" +
        "Only has an effect if <b>Always</b> is unchecked.</html>";
        
    static final String TO_TOOLTIP = "<html><b>To</b><br>" +
        "Sets when in the underlying Child Motif the Arpeggio stops arpeggiating its notes.<br><br>" +
        "Only has an effect if <b>Always</b> is unchecked.</html>";
    }
