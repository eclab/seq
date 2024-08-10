/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.notes.gui;

import seq.motif.notes.*;
import seq.engine.*;
import seq.gui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.locks.*;

public class NotesInspector extends WidgetList
    {
    Seq seq;
    Notes notes;
    NotesUI notesui;
    Notes.Event event;
    
    StringField name;
    JComboBox in;
    JComboBox out;
    JCheckBox armed;
    JCheckBox echo;
    WidgetList widgetList = new WidgetList();
    
    public static final String[] CC_7_NAMES = new String[]
    {
    "Bank Select MSB", "Modulation Wheel MSB", "Breath Controller MSB", "", "Foot Pedal MSB", "Portamento Time MSB", "Data Entry MSB", "Volume MSB",
    "Balance MSB", "", "Pan MSB", "Expression MSB", "Effect Controller 1 MSB", "Effect Controller 2 MSB", "", "", 
    "", "", "", "", "", "", "", "",
    "", "", "", "", "", "", "", "",
    "Bank Select LSB", "Modulation Wheel LSB", "Breath Controller LSB", "", "Foot Pedal LSB", "Portamento Time LSB", "Data Entry LSB", "Volume LSB",
    "Balance LSB", "", "Pan LSB", "Expression LSB", "Effect Controller 1 LSB", "Effect Controller 2 LSB", "", "", 
    "", "", "", "", "", "", "", "",
    "", "", "", "", "", "", "", "",
    "Damper Pedal", "Portamento Switch", "Sostenuto Pedal", "Soft Pedal", "Legato Footswitch", "Hold 2", "Sound Variation", "Resonance", 
    "Release", "Attack", "MPE / Cutoff", "", "", "", "", "",
    "", "", "", "", "Portamento Control", "", "", "",
    "High Resolution Velocity", "", "", "Effect 1 Depth", "Effect 2 Depth", "Effect 3 Depth", "Effect 4 Depth", "Effect 5 Depth",
    "Data Increment", "Data Decrement", "NRPN LSB", "NRPN MSB", "RPN LSB", "RPN MSB", "", "",
    "", "", "", "", "", "", "", "",
    "", "", "", "", "", "", "", "",
    "All Sound Off", "Reset All Controllers", "Local On/Off", "All Notes Off", "Omni Off", "Omni On", "Mono Mode", "Poly Mode",
    };
    
    public static final String[] CC_14_NAMES = new String[]
    {
    "Bank Select", "Modulation Wheel", "Breath Controller", "", "Foot Pedal", "Portamento Time", "Data Entry", "Volume",
    "Balance", "", "Pan", "Expression", "Effect Controller 1", "Effect Controller 2", "", "", 
    "", "", "", "", "", "", "", "",
    "", "", "", "", "", "", "", "",
    };

    public static final String[] RPN_NAMES = new String[]
    {
    "Pitch Bend Range", "Fine Tuning", "Coarse Tuning", "Tuning Program Change", "Tuning Bank Select", "Modulation Depth Range"
    // And of course there is RPN_NULL
    };
    
    JComboBox[] parameterType = new JComboBox[Motif.NUM_PARAMETERS];
    SmallDial[] parameterMSB = new SmallDial[Motif.NUM_PARAMETERS];
    SmallDial[] parameterLSB = new SmallDial[Motif.NUM_PARAMETERS];
    JLabel[] combined = new JLabel[Motif.NUM_PARAMETERS]; 
    JPanel[] parameterPanel = new JPanel[Motif.NUM_PARAMETERS];
    public static final String[] TYPES = { "None", "Bend", "CC", "14-Bit CC", "NRPN", "RPN" };
        
    public NotesInspector(Seq seq, Notes notes, NotesUI notesui)
        {
        this.seq = seq;
        this.notes = notes;
        this.notesui = notesui;
                
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            name = new StringField(notes.getName())
                {
                public String newValue(String newValue)
                    {
                    newValue = newValue.trim();
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { notes.setName(newValue); }
                    finally { lock.unlock(); }
                    notesui.updateText();
                    return newValue;
                    }
                                
                public String getValue() 
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return notesui.getName(); }
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
            out.setSelectedIndex(notes.getOut());
            out.setMaximumRowCount(outs.length);
            out.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { notes.setOut(out.getSelectedIndex()); }
                    finally { lock.unlock(); }                              
                    }
                });
                
            In[] seqIns = seq.getIns();                 // the primary in is seqIns[0]
            String[] ins = new String[seqIns.length];
            for(int i = 0; i < seqIns.length; i++)
                {
                ins[i] = "" + (i + 1) + ": " + seqIns[i].toString();
                }
                
            in = new JComboBox(ins);
            in.setSelectedIndex(notes.getIn());
            in.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { notes.setIn(in.getSelectedIndex()); }
                    finally { lock.unlock(); }                              
                    }
                });


            armed = new JCheckBox();
            armed.setSelected(notes.isArmed());
            armed.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { notes.setArmed(armed.isSelected()); }
                    finally { lock.unlock(); }                              
                    }
                });

            echo = new JCheckBox();
            echo.setSelected(notes.getEcho());
            echo.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { notes.setEcho(echo.isSelected()); }
                    finally { lock.unlock(); }                              
                    }
                });
                
            for(int i = 0; i < Motif.NUM_PARAMETERS; i++)
                {
                final int _i = i;
                combined[i] = new JLabel("");
                parameterType[i] = new JComboBox(TYPES);
                parameterType[i].setSelectedIndex(notes.getMIDIParameterType(i));
                parameterType[i].addActionListener(new ActionListener()
                    {
                    public void actionPerformed(ActionEvent e)
                        {
                        if (seq == null) return;
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { notes.setMIDIParameterType(_i, parameterType[_i].getSelectedIndex()); }
                        finally { lock.unlock(); }     
                        updateFull(_i);                         
                        }
                    });
                                        
                parameterMSB[i]  = new SmallDial(notes.getMIDIParameterMSB(_i) / 127.0)
                    {
                    protected String map(double val) 
                        {
                        return "" + (int)(val * 127.0);
                        }
                    public double getValue() 
                        { 
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { return notes.getMIDIParameterMSB(_i) / 127.0; }
                        finally { lock.unlock(); }
                        }
                    public void setValue(double val) 
                        { 
                        if (seq == null) return;
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { notes.setMIDIParameterMSB(_i, (int)(val * 127.0)); }
                        finally { lock.unlock(); }
                        updateFull(_i);
                        }
                    };

                parameterLSB[i]  = new SmallDial(notes.getMIDIParameterLSB(_i) / 127.0)
                    {
                    protected String map(double val) 
                        {
                        return "" + (int)(val * 127.0);
                        }
                    public double getValue() 
                        { 
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { return notes.getMIDIParameterLSB(_i) / 127.0; }
                        finally { lock.unlock(); }
                        }
                    public void setValue(double val) 
                        { 
                        if (seq == null) return;
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { notes.setMIDIParameterLSB(_i, (int)(val * 127.0)); }
                        finally { lock.unlock(); }
                        updateFull(_i);
                        }
                    };
                                
                updateFull(i);
                                
                parameterPanel[i] = new JPanel();
                parameterPanel[i].setLayout(new BorderLayout());
                Box box = new Box(BoxLayout.X_AXIS);
                box.add(new Collapse(parameterType[i]));
                box.add(new JLabel(" "));
                box.add(parameterMSB[i].getLabelledTitledDialVertical("MSB", "127"));
                box.add(parameterLSB[i].getLabelledTitledDialVertical("LSB", "127"));
                box.add(new JLabel(" "));
                parameterPanel[i].add(box, BorderLayout.WEST);
                parameterPanel[i].add(combined[i], BorderLayout.CENTER);
                }
            
            }
        finally { lock.unlock(); }

        build(new String[] { "Name", "Out", "In", "Echo", "Armed" }, 
            new JComponent[] 
                {
                name,
                out,
                in,
                echo,
                armed, 
                });
                
        widgetList.build(new String[] { "1", "2", "3", "4", "5", "6", "7", "8" },
            new JComponent[] { parameterPanel[0], parameterPanel[1], parameterPanel[2], parameterPanel[3], 
                             parameterPanel[4], parameterPanel[5], parameterPanel[6], parameterPanel[7] });
        DisclosurePanel disclosure = new DisclosurePanel("MIDI Parameters", widgetList);
        disclosure.setParentComponent(this);
        add(disclosure, BorderLayout.SOUTH);
        }
        
    public void updateFull(int param)
        {
        String full = null;
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            { 
            int type = notes.getMIDIParameterType(param);
            int midiParamMSB = notes.getMIDIParameterMSB(param);
            int midiParamLSB = notes.getMIDIParameterLSB(param);
            if (type == Notes.NO_MIDI_PARAMETER)
                {
                full = "[None]";
                }
            else if (type == Notes.BEND)
                {
                full = "Bend";
                }
            else if (type == Notes.CC_7)
                {
                full = "" + midiParamMSB + " " + CC_7_NAMES[midiParamMSB];
                }
            else if (type == Notes.CC_14)
                {
                if (midiParamMSB >= 32) // uh oh
                    full = "[Invalid]";
                else
                    full = "" + midiParamMSB + " " + CC_14_NAMES[midiParamMSB];
                }
            else if (type == Notes.NRPN)    // it's NRPN
                {
                full = "" + (midiParamMSB * 128 + midiParamLSB);
                }
            else                    // It's RPN
                {
                int p = midiParamMSB * 128 + midiParamLSB;
                if (p == 16383)
                    full = "16383 RPN NULL";
                else if (p < RPN_NAMES.length)
                    full = "" + (p) + " " + RPN_NAMES[p];
                else full = "" + (p);
                }
            }
        finally { lock.unlock(); }
        combined[param].setText(full);
        }
            
    public void revise()
        {
        Seq old = seq;
        seq = null;
        ReentrantLock lock = old.getLock();
        lock.lock();
        try 
            { 
            out.setSelectedIndex(notes.getOut()); 
            in.setSelectedIndex(notes.getIn()); 
            armed.setSelected(notes.isArmed()); 
            echo.setSelected(notes.getEcho()); 
            for(int i = 0; i < Motif.NUM_PARAMETERS; i++)
                {
                parameterType[i].setSelectedIndex(notes.getMIDIParameterType(i));
                }
            }
        finally { lock.unlock(); }                              
        seq = old;
        name.update();
        for(int i = 0; i < Motif.NUM_PARAMETERS; i++)
            {
            if (parameterMSB[i] != null) parameterMSB[i].redraw();
            if (parameterLSB[i] != null) parameterLSB[i].redraw();
            }
        }
    }
