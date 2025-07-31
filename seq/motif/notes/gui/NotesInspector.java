/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.notes.gui;

import seq.motif.notes.*;
import seq.engine.*;
import seq.gui.*;
import seq.util.*;
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
    TimeDisplay end;
    WidgetList recordList1;
    JCheckBox armed;
    JCheckBox echo;
    JComboBox recordIntegration;
    JCheckBox recordBend;
    JCheckBox recordCC;
    JCheckBox recordAftertouch;
    JCheckBox recordPC;
    JCheckBox convertNRPNRPN;
    JCheckBox logBend;
    JComboBox parameterHeight;
    JCheckBox quantize;
    JCheckBox quantizeNoteEnds;
    JCheckBox quantizeNonNotes;
    JComboBox quantizeTo;
    SmallDial quantizeBias;
    
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
    "Pitch Bend Range", "Fine Tuning", "Coarse Tuning", "Tuning Prog. Change", "Tuning Bank Select", "Mod Depth Range"
    // And of course there is RPN_NULL
    };
    
    JComboBox[] eventParameterType = new JComboBox[Notes.NUM_EVENT_PARAMETERS];
    SmallDial[] eventParameterMSB = new SmallDial[Notes.NUM_EVENT_PARAMETERS];
    SmallDial[] eventParameterLSB = new SmallDial[Notes.NUM_EVENT_PARAMETERS];
    JLabel[] eventCombined = new JLabel[Notes.NUM_EVENT_PARAMETERS]; 
    JPanel[] eventParameterPanel = new JPanel[Notes.NUM_EVENT_PARAMETERS];
    public JComponent eventMSB[] = new JComponent[Notes.NUM_EVENT_PARAMETERS];
    public JComponent eventLSB[] = new JComponent[Notes.NUM_EVENT_PARAMETERS];
    public JComponent eventBox[] = new Box[Notes.NUM_EVENT_PARAMETERS];

    public static final String[] EVENT_TYPES = { "None", "CC", "Poly AT", "Channel AT", "Bend", "PC", "NRPN", "RPN" };
    public static final boolean[] EVENT_HAS_LSB = { false, false, false, false, false, false, true, true };
    public static final boolean[] EVENT_HAS_MSB = { false, true, true, false, false, false, true, true };

    public static final String[] PARAMETER_HEIGHT_STRINGS = { "Small", "Medium", "Large" };
    public static final int[] PARAMETER_HEIGHTS = { 32, 64, 128 };
    
    public static final String[] RECORD_INTEGRATION_STRINGS = { "Replace", "Replace/Trim", "Merge" };           // No "OVERWRITE" right now
    
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

            end = new TimeDisplay(0, seq)
                {
                public int getTime()
                    {
                    return notes.getEnd();
                    }
                        
                public void setTime(int time)
                    {
                    notes.setEnd(time);
                    }
                public void setTimeOutside(int time)
                    {
                    notesui.getGridUI().repaint();
                    notesui.getEventsUI().repaint();
                    }
                };
            end.setDisplaysTime(true);
                                                                        
            armed = new JCheckBox();
            armed.setSelected(notes.isArmed());
            armed.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    boolean disarm = false;
                    lock.lock();
                    try 
                        {
                        if (armed.isSelected() && notesui.getSeqUI().getDisarmsAllBeforeArming())
                            {
                            seq.disarmAll();
                            disarm = true;
                            } 
                        notes.setArmed(armed.isSelected()); 
                        }
                    finally { lock.unlock(); }                              
                    if (disarm)         // outside lock
                        {
                        notesui.getSeqUI().incrementRebuildInspectorsCount();           // show disarmed
                        }
                    }
                });

            recordIntegration = new JComboBox(RECORD_INTEGRATION_STRINGS);
            recordIntegration.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { notes.setRecordIntegration(recordIntegration.getSelectedIndex()); }
                    finally { lock.unlock(); }                              
                    }
                });
            if (notes.getRecordIntegration() < Notes.INTEGRATE_PUNCH_IN)
                {
                recordIntegration.setSelectedIndex(notes.getRecordIntegration());
                }

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
                
            recordBend = new JCheckBox();
            recordBend.setSelected(notes.getRecordBend());
            recordBend.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { notes.setRecordBend(recordBend.isSelected()); }
                    finally { lock.unlock(); }                              
                    }
                });
                
            recordCC = new JCheckBox();
            recordCC.setSelected(notes.getRecordCC());
            recordCC.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { notes.setRecordCC(recordCC.isSelected()); }
                    finally { lock.unlock(); }                              
                    }
                });
                
            recordPC = new JCheckBox();
            recordPC.setSelected(notes.getRecordPC());
            recordPC.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { notes.setRecordPC(recordPC.isSelected()); }
                    finally { lock.unlock(); }                              
                    }
                });
                
            convertNRPNRPN = new JCheckBox();
            convertNRPNRPN.setSelected(notes.getConvertNRPNRPN());
            convertNRPNRPN.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { notes.setConvertNRPNRPN(convertNRPNRPN.isSelected()); }
                    finally { lock.unlock(); }                              
                    }
                });
                
            recordAftertouch = new JCheckBox();
            recordAftertouch.setSelected(notes.getRecordAftertouch());
            recordAftertouch.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { notes.setRecordAftertouch(recordAftertouch.isSelected()); }
                    finally { lock.unlock(); }                              
                    }
                });

            quantize = new JCheckBox();
            quantize.setSelected(notes.getQuantize());
            quantize.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { notes.setQuantize(quantize.isSelected()); }
                    finally { lock.unlock(); }                              
                    }
                });

            quantizeNonNotes = new JCheckBox();
            quantizeNonNotes.setSelected(notes.getQuantizeNonNotes());
            quantizeNonNotes.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { notes.setQuantizeNonNotes(quantizeNonNotes.isSelected()); }
                    finally { lock.unlock(); }                              
                    }
                });

            quantizeNoteEnds = new JCheckBox();
            quantizeNoteEnds.setSelected(notes.getQuantizeNoteEnds());
            quantizeNoteEnds.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { notes.setQuantizeNoteEnds(quantizeNoteEnds.isSelected()); }
                    finally { lock.unlock(); }                              
                    }
                });

            quantizeTo = new JComboBox(Notes.QUANTIZE_STRINGS);
            quantizeTo.setSelectedIndex(notes.getQuantizeTo());
            quantizeTo.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { notes.setQuantizeTo(quantizeTo.getSelectedIndex()); }
                    finally { lock.unlock(); }     
                    }
                });
 
            quantizeBias = new SmallDial(notes.getQuantizeBias())
                {
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return notes.getQuantizeBias(); }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { notes.setQuantizeBias(val); }
                    finally { lock.unlock(); }
                    }
                };
           
           
           
            for(int i = 0; i < Notes.NUM_EVENT_PARAMETERS; i++)
                {
                final int _i = i;
                eventCombined[i] = new JLabel("");
                eventParameterType[i] = new JComboBox(EVENT_TYPES);
                eventParameterType[i].addActionListener(new ActionListener()
                    {
                    public void actionPerformed(ActionEvent e)
                        {
                        if (seq == null) return;
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { notes.setEventParameterType(_i, eventParameterType[_i].getSelectedIndex()); }
                        finally { lock.unlock(); }     
                        reviseEventParameters();
                        updateEventFull(_i);
                        notesui.revalidate();           
                        }
                    });
                                        
                eventParameterMSB[i]  = new SmallDial(notes.getEventParameterMSB(_i) / 127.0)
                    {
                    protected String map(double val) 
                        {
                        return "" + (int)(val * 127.0);
                        }
                    public double getValue() 
                        { 
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { return notes.getEventParameterMSB(_i) / 127.0; }
                        finally { lock.unlock(); }
                        }
                    public void setValue(double val) 
                        { 
                        if (seq == null) return;
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { notes.setEventParameterMSB(_i, (int)(val * 127.0)); }
                        finally { lock.unlock(); }
                        reviseEventParameters();                         
                        updateEventFull(_i);             
                        }
                    };

                eventParameterLSB[i]  = new SmallDial(notes.getEventParameterLSB(_i) / 127.0)
                    {
                    protected String map(double val) 
                        {
                        return "" + (int)(val * 127.0);
                        }
                    public double getValue() 
                        { 
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { return notes.getEventParameterLSB(_i) / 127.0; }
                        finally { lock.unlock(); }
                        }
                    public void setValue(double val) 
                        { 
                        if (seq == null) return;
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { notes.setEventParameterLSB(_i, (int)(val * 127.0)); }
                        finally { lock.unlock(); }
                        reviseEventParameters();                         
                        updateEventFull(_i);             
                        }
                    };
                                
                reviseEventParameters();                         
                                
                eventParameterPanel[i] = new JPanel();
                eventParameterPanel[i].setLayout(new BorderLayout());
                eventBox[i] = new Box(BoxLayout.X_AXIS);
                eventBox[i].add(new Collapse(eventParameterType[i]));
                eventBox[i].add(new JLabel(" "));
                eventMSB[i] = eventParameterMSB[i].getLabelledTitledDialVertical("MSB", "127");
                eventLSB[i] = eventParameterLSB[i].getLabelledTitledDialVertical("LSB", "127");
                eventBox[i].add(new JLabel(" "));
                eventParameterPanel[i].add(eventBox[i], BorderLayout.WEST);
                eventParameterPanel[i].add(eventCombined[i], BorderLayout.CENTER);
                }
 
            for(int i = 0; i < Notes.NUM_EVENT_PARAMETERS; i++)
                {
                eventParameterType[i].setSelectedIndex(notes.getEventParameterType(i));
                }

            logBend = new JCheckBox();
            logBend.setSelected(notes.getLog());
            logBend.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { notes.setLog(logBend.isSelected()); }
                    finally { lock.unlock(); }  
                    notesui.getEventsUI().reload();                                     // gotta redraw the bends!                            
                    notesui.getEventsUI().repaint();                                    // gotta redraw the bends!                            
                    }
                });

            parameterHeight = new JComboBox(PARAMETER_HEIGHT_STRINGS);
            parameterHeight.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    notesui.getEventsUI().setParameterHeight(PARAMETER_HEIGHTS[parameterHeight.getSelectedIndex()]);
                    notesui.getEventsUI().rebuild();
                    notesui.getGridUI().clearSelected();
                    }
                });

            }
        finally { lock.unlock(); }

        int height = notesui.getEventsUI().getParameterHeight();
        parameterHeight.setSelectedIndex(height == 32 ? 0 : (height == 64 ? 1 : 2));

        name.setToolTipText(NAME_TOOLTIP);
        out.setToolTipText(OUT_TOOLTIP);
        in.setToolTipText(IN_TOOLTIP);
        end.setToolTipText(END_TOOLTIP);
//        setEnd.setToolTipText(SET_END_TOOLTIP);
        echo.setToolTipText(ECHO_TOOLTIP);
        armed.setToolTipText(ARMED_TOOLTIP);
        recordBend.setToolTipText(RECORD_BEND_TOOLTIP);
        recordAftertouch.setToolTipText(RECORD_AFTERTOUCH_TOOLTIP);
        recordCC.setToolTipText(RECORD_CC_TOOLTIP);
        recordPC.setToolTipText(RECORD_PC_TOOLTIP);
        convertNRPNRPN.setToolTipText(CONVERT_NRPN_RPN_TOOLTIP);

        build(new String[] { "Name", "Out", "In", "End", "Armed", "Echo"}, 
            new JComponent[] 
                {
                name,
                out,
                in,
                end, //endPanel,
                armed,
                echo,
                });
                
        recordList1 = new WidgetList(new String[] { "Integration", "Record Bend", "Record Aftertouch", "Record CC", "Record PC", "Make NRPN/RPN",
                "Quantize On Record", "Quantize To", "Quantize Note Ends", "Quantize Other Events", "Quantize Bias" },  
            new JComponent[] { recordIntegration, recordBend, recordAftertouch, recordCC, recordPC, convertNRPNRPN, quantize, quantizeTo, quantizeNoteEnds, quantizeNonNotes, quantizeBias.getLabelledDial("0.8888")});
        
        recordList1.setBorder(BorderFactory.createTitledBorder("<html><i>Recording</i></html>"));
        DisclosurePanel recordDisclosure = new DisclosurePanel("Recording", recordList1);

        WidgetList widgetList2 = new WidgetList();
        widgetList2.build(new String[] { "1", "2", "3", "4" },
            new JComponent[] { eventParameterPanel[0], eventParameterPanel[1], eventParameterPanel[2], eventParameterPanel[3] });

        WidgetList widgetList1 = new WidgetList();
        widgetList1.build(new String[] { "Logarithmic Pitch Bend", "Display Height" }, new JComponent[] { logBend, parameterHeight });
                
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BorderLayout());
        messagePanel.add(widgetList2, BorderLayout.NORTH);
        messagePanel.add(widgetList1, BorderLayout.SOUTH);
        messagePanel.setBorder(BorderFactory.createTitledBorder("<html><i>Non-Note Display</i></html>"));
        DisclosurePanel parameterDisclosure = new DisclosurePanel("Non-Note Display", messagePanel);

        JPanel finalPanel = new JPanel();
        finalPanel.setLayout(new BorderLayout());
        finalPanel.add(recordDisclosure, BorderLayout.NORTH);
        finalPanel.add(parameterDisclosure, BorderLayout.SOUTH);
        add(finalPanel, BorderLayout.SOUTH);

        recordDisclosure.setParentComponent(notesui);
        parameterDisclosure.setParentComponent(notesui);
        }
    
    boolean validType(EventsUI eventsui, int pos)
        {
        if (eventsui.types[pos] == 0) return false;
        for(int i = 0; i < pos; i++)
            {
            if (eventsui.types[i] == eventsui.types[pos]) return false;
            }
        return true;
        }
        
    public void reviseEventParameters()
        {
        if (eventParameterType[Notes.NUM_EVENT_PARAMETERS - 1] == null) return;                 // not set up yet
        if (eventParameterLSB[Notes.NUM_EVENT_PARAMETERS - 1] == null) return;          // not set up yet
                
        EventsUI eventsui = notesui.getEventsUI();
        int count = 0;
        for(int i = 0; i < Notes.NUM_EVENT_PARAMETERS; i++)
            {
            eventsui.types[i] = getParameterType(i);
            if (validType(eventsui, i))
                {
                count++;
                }
            }
        eventsui.parameteruis.clear();
        eventsui.parameterBox.removeAll();
        eventsui.parameterBoxLayout.setRows(count);
        
        for(int i = 0; i < Notes.NUM_EVENT_PARAMETERS; i++)
            {
            if (validType(eventsui, i))
                {
                ParameterUI parameterui = new ParameterUI(eventsui, eventsui.types[i]);
                eventsui.parameterBox.add(parameterui);
                eventsui.parameteruis.add(parameterui);
                parameterui.repaint();
                }
            }
        eventsui.parameterBox.revalidate();
        eventsui.parameterBox.repaint();
        notesui.scroll.setCorner(JScrollPane.UPPER_LEFT_CORNER, eventsui.getHeader());
        notesui.scroll.revalidate();
        notesui.scroll.repaint();
        }
   
    public void updateEventFull(int param)
        {
        if (eventParameterMSB[param] == null || eventBox[param] == null) return;                // we're not set up yet
        
        eventBox[param].remove(eventMSB[param]);
        eventBox[param].remove(eventLSB[param]);
        if (EVENT_HAS_MSB[eventParameterType[param].getSelectedIndex()])
            {
            eventBox[param].add(eventMSB[param]);
            }
        if (EVENT_HAS_LSB[eventParameterType[param].getSelectedIndex()])
            {
            eventBox[param].add(eventLSB[param]);
            }
        eventBox[param].revalidate();
        eventBox[param].repaint();

        String full = null;
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            { 
            int type = eventParameterType[param].getSelectedIndex();
            int eventParamMSB = (int)(eventParameterMSB[param].getValue() * 127.0);
            int eventParamLSB = (int)(eventParameterLSB[param].getValue() * 127.0);
            if (type == Notes.EVENT_PARAMETER_NONE)
                {
                full = "";
                }
            else if (type == Notes.EVENT_PARAMETER_BEND)
                {
                full = "";
                }
            else if (type == Notes.EVENT_PARAMETER_CC)
                {
                full = "<html>&nbsp;<font size=2>" + CC_7_NAMES[eventParamMSB] + "</font></html>";
                }
            else if (type == Notes.EVENT_PARAMETER_POLY_AT)
                {
                full = " " + Notes.NOTES[eventParamMSB % 12] + (eventParamMSB / 12);
                }
            else if (type == Notes.EVENT_PARAMETER_CHANNEL_AT)
                {
                full = "";
                }
            else if (type == Notes.EVENT_PARAMETER_PC)
                {
                full = "";
                }
/*
  else if (type == Notes.CC_14)
  {
  if (eventParamMSB >= 32) // uh oh
  full = "[Invalid]";
  else
  full = "" + eventParamMSB + " " + CC_14_NAMES[eventParamMSB];
  }
*/
            else if (type == Notes.EVENT_PARAMETER_NRPN)    // it's NRPN
                {
                full = "" + (eventParamMSB * 128 + eventParamLSB);
                }
            else                    // It's RPN
                {
                int p = eventParamMSB * 128 + eventParamLSB;
                if (p == 16383)
                    full = "<html>&nbsp;16383<br>&nbsp;<font size=2>RPN NULL</font></html>";
                else if (p < RPN_NAMES.length)
                    full = "<html>&nbsp;" + (p) + "<br>&nbsp;<font size=2>" + RPN_NAMES[p] + "</font></html>";
                else full = " " + (p);
                }
            }
        finally { lock.unlock(); }
        eventCombined[param].setText(full);
        }
        
    public int getParameterType(int parameterIndex)
        {
        int type = eventParameterType[parameterIndex].getSelectedIndex();
        int eventParamMSB = (int)(eventParameterMSB[parameterIndex].getValue() * 127.0);
        int eventParamLSB = (int)(eventParameterLSB[parameterIndex].getValue() * 127.0);
        if (type == Notes.EVENT_PARAMETER_NONE)
            {
            return 0;               // I guess
            }
        else if (type == Notes.EVENT_PARAMETER_BEND)
            {
            return Notes.TYPE_PITCH_BEND;
            }
        else if (type == Notes.EVENT_PARAMETER_CC)
            {
            return Notes.TYPE_CC + eventParamMSB;
            }
        else if (type == Notes.EVENT_PARAMETER_POLY_AT)
            {
            return Notes.TYPE_POLYPHONIC_AFTERTOUCH + eventParamMSB;
            }
        else if (type == Notes.EVENT_PARAMETER_CHANNEL_AT)
            {
            return Notes.TYPE_CHANNEL_AFTERTOUCH;
            }
        else if (type == Notes.EVENT_PARAMETER_PC)
            {
            return Notes.TYPE_PC;
            }
        else if (type == Notes.EVENT_PARAMETER_NRPN)
            {
            return Notes.TYPE_NRPN + eventParamMSB * 128 + eventParamLSB;
            }
        else                    // It's RPN
            {
            return Notes.TYPE_RPN + eventParamMSB * 128 + eventParamLSB;
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
            out.setSelectedIndex(notes.getOut()); 
            in.setSelectedIndex(notes.getIn()); 
            armed.setSelected(notes.isArmed()); 
            echo.setSelected(notes.getEcho()); 
            recordBend.setSelected(notes.getRecordBend()); 
            recordCC.setSelected(notes.getRecordCC()); 
            recordPC.setSelected(notes.getRecordPC()); 
            recordAftertouch.setSelected(notes.getRecordAftertouch()); 
            quantize.setSelected(notes.getQuantize());
            quantizeNoteEnds.setSelected(notes.getQuantizeNoteEnds());
            quantizeNonNotes.setSelected(notes.getQuantizeNonNotes());
            convertNRPNRPN.setSelected(notes.getConvertNRPNRPN()); 
            logBend.setSelected(notes.getLog());
            
            quantizeTo.setSelectedIndex(notes.getQuantizeTo());
            for(int i = 0; i < Notes.NUM_EVENT_PARAMETERS; i++)
                {
                eventParameterType[i].setSelectedIndex(notes.getEventParameterType(i));
                }
            int height = notesui.getEventsUI().getParameterHeight();
            parameterHeight.setSelectedIndex(height == 32 ? 0 : (height == 64 ? 1 : 2));
            }
        finally { lock.unlock(); }                              
        seq = old;
        name.update();
        if (end != null) end.revise();
        if (quantizeBias != null) quantizeBias.redraw();
        
        for(int i = 0; i < Notes.NUM_EVENT_PARAMETERS; i++)
            {
            if (eventParameterMSB[i] != null) eventParameterMSB[i].redraw();
            if (eventParameterLSB[i] != null) eventParameterLSB[i].redraw();
            }
        }

        
    /*** Tooltips ***/
        
    static final String NAME_TOOLTIP = "<html><b>Name</b><br>" +
        "Sets the name of the Notes.  This will appear in the Motif List at left.</html>";

    static final String OUT_TOOLTIP = "<html><b>Out</b><br>" +
        "Sets the MIDI output for the Notes.</html>";
        
    static final String IN_TOOLTIP = "<html><b>In</b><br>" +
        "Sets the MIDI input for recording to the Notes.</html>";
        
    static final String END_TOOLTIP = "<html><b>End</b><br>" +
        "Sets the End time of the Notes.  The Notes will terminate after the End time,<br>" +
        "or after the last event has completed, whichever is later.</html>";
        
/*
  static final String SET_END_TOOLTIP = "<html><b>Set End</b><br>" +
  "Sets the End of the Notes to the time at left.  The Notes will terminate<br>" +
  "after the End, or after the last event has completed, whichever is later.</html>";
*/
        
    static final String ECHO_TOOLTIP = "<html><b>Echo</b><br>" +
        "Sets whether received MIDI data will be echoed out Output during recording.</html.";
                
    static final String ARMED_TOOLTIP = "<html><b>Arm</b><br>" +
        "Arms the Notes to receive and record MIDI data during recording.</html>";
        
    static final String RECORD_BEND_TOOLTIP = "<html><b>Record Bend</b><br>" +
        "Sets whether the Notes will record Pitch Bend data during recording.</html>";
        
    static final String RECORD_AFTERTOUCH_TOOLTIP = "<html><b>Record Aftertouch</b><br>" +
        "Sets whether the Notes will record Aftertouch data during recording.</html>";
        
    static final String RECORD_CC_TOOLTIP = "<html><b>Record CC</b><br>" +
        "Sets whether the Notes will record CC (Control Change) data during recording.</html>";
        
    static final String RECORD_PC_TOOLTIP = "<html><b>Record PC</b><br>" +
        "Sets whether the Notes will record PC (Program Change) data during recording.</html>";
        
    static final String CONVERT_NRPN_RPN_TOOLTIP = "<html><b>Make NRPN/RPN</b><br>" +
        "Sets whether the Notes will attempt to convert appropriate CC data<br>" + 
        "into NRPN or RPN events after recording.  If not, the CC data will<br>" +
        "be retained as CC events.</html>";
        
    static final String OUTPUT_MIDI_VALUES_TOOLTIP = "<html><b>MIDI Parameters</b><br>" +
        "Convert user parameters into MIDI parameters to be output each timestep, including:" + 
        "<ul>" + 
        "<li>Pitch Bend" + 
        "<li>(7-bit) Control Change or CC" + 
        "<li>(14-bit) Control Change" + 
        "<li>NRPN" + 
        "<li>RPN" + 
        "</ul></html>";

    static final String OUTPUT_ONE_MIDI_VALUE_TOOLTIP = "<html><b>MIDI Parameter</b><br>" +
        "Convert a user parameter into a MIDI parameter to be output each timestep.<br>" +
        "Each parameter is defined by a MSB (Most Significant Byte) and LSB (Least<br>" +
        "Significant Byte), but some parameters only use the MSB, or only use part of<br>" +
        "the range of the MSB.  See below for information on setting parameter numbers:" +
        "<ul>" + 
        "<li><b>Pitch Bend</b>.  Doesn't use MSB or LSB.  Ignore them." +
        "<li><b>(7-bit) Control Change or CC</b>.  Uses only the MSB.  Ignore the LSB." + 
        "<li><b>(14-bit) Control Change</b>.  There are only 32 14-bit Control Change<br>" +
        "parameters (0-31), and so it only uses the first 32 values of the MSB.<br>" + 
        "Ignore the LSB." + 
        "<li><b>NRPN</b>.  Uses the MSB and LSB together to define the parameter." + 
        "<li><b>RPN</b>.  Uses the MSB and LSB together to define the parameter." + 
        "</ul></html>";
        
    }
