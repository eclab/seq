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
    TimeDisplay end;
    JButton setEnd;
    JPanel endPanel;
    int endTime;
    JCheckBox armed;
    JCheckBox echo;
    JCheckBox recordBend;
    JCheckBox recordCC;
    JCheckBox recordAftertouch;
    JCheckBox convertNRPNRPN;
    
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

			endTime = notes.getEnd();
			end = new TimeDisplay(endTime, seq)
				{
				public void updateTime(int time)
					{
					// this is already inside the lock but whatever
					endTime = time;
					}
				};
									
			setEnd = new JButton("Set");
			setEnd.addActionListener(new ActionListener()
				{
				public void actionPerformed(ActionEvent evt)
					{
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try
						{
						notes.setEnd(endTime);
						}
					finally
						{
						lock.unlock();
						}
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
                    if (disarm)		// outside lock
                    	{
						notesui.getSeqUI().incrementRebuildInspectorsCount();		// show disarmed
                       	}
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
                
            convertNRPNRPN = new JCheckBox();
            convertNRPNRPN.setSelected(notes.getRecordCC());
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
                parameterPanel[i].setToolTipText(OUTPUT_ONE_MIDI_VALUE_TOOLTIP);
                parameterPanel[i].setLayout(new BorderLayout());
                Box box = new Box(BoxLayout.X_AXIS);
                box.add(new Collapse(parameterType[i]));
                parameterType[i].setToolTipText(OUTPUT_ONE_MIDI_VALUE_TOOLTIP);
                box.add(new JLabel(" "));
                JComponent comp = parameterMSB[i].getLabelledTitledDialVertical("MSB", "127");
                comp.setToolTipText(OUTPUT_ONE_MIDI_VALUE_TOOLTIP);
                box.add(comp);
                comp = parameterLSB[i].getLabelledTitledDialVertical("LSB", "127");
                box.add(comp);
                box.add(new JLabel(" "));
                parameterPanel[i].add(box, BorderLayout.WEST);
                combined[i].setToolTipText(OUTPUT_ONE_MIDI_VALUE_TOOLTIP);
                parameterPanel[i].add(combined[i], BorderLayout.CENTER);
                parameterPanel[i].setToolTipText(OUTPUT_ONE_MIDI_VALUE_TOOLTIP);

                JPanel vert = new JPanel();
                vert.setLayout(new BorderLayout());
                JLabel temp = new JLabel(" ");
                temp.setFont(SmallDial.FONT);
                vert.add(temp, BorderLayout.NORTH);
                vert.add(setEnd, BorderLayout.CENTER);
                temp = new JLabel(" ");
                temp.setFont(SmallDial.FONT);
                vert.add(temp, BorderLayout.SOUTH);
                                        
                endPanel = new JPanel();
                endPanel.setLayout(new BorderLayout());
                endPanel.add(end, BorderLayout.WEST);
                endPanel.add(vert, BorderLayout.EAST);
        		endPanel.setToolTipText(END_TOOLTIP);
                }
            
            }
        finally { lock.unlock(); }

		name.setToolTipText(NAME_TOOLTIP);
		out.setToolTipText(OUT_TOOLTIP);
		in.setToolTipText(IN_TOOLTIP);
		end.setToolTipText(END_TOOLTIP);
		setEnd.setToolTipText(SET_END_TOOLTIP);
		echo.setToolTipText(ECHO_TOOLTIP);
		armed.setToolTipText(ARMED_TOOLTIP);
		recordBend.setToolTipText(RECORD_BEND_TOOLTIP);
		recordAftertouch.setToolTipText(RECORD_AFTERTOUCH_TOOLTIP);
		recordCC.setToolTipText(RECORD_CC_TOOLTIP);
		convertNRPNRPN.setToolTipText(CONVERT_NRPN_RPN_TOOLTIP);

        build(new String[] { "Name", "Out", "In", "End", "Echo", "Armed", "Record Bend", "Record Aftertouch", "Record CC", "Make NRPN/RPN"}, 
            new JComponent[] 
                {
                name,
                out,
                in,
                endPanel,
                echo,
                armed, 
                recordBend,
                recordAftertouch,
                recordCC,
                convertNRPNRPN,
                });
                
        widgetList.build(new String[] { "1", "2", "3", "4", "5", "6", "7", "8" },
            new JComponent[] { parameterPanel[0], parameterPanel[1], parameterPanel[2], parameterPanel[3], 
                             parameterPanel[4], parameterPanel[5], parameterPanel[6], parameterPanel[7] });
        DisclosurePanel disclosure = new DisclosurePanel("MIDI Parameters", widgetList);
        disclosure.setParentComponent(this);
        add(disclosure, BorderLayout.SOUTH);
                disclosure.setToolTipText(OUTPUT_MIDI_VALUES_TOOLTIP);
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
            recordBend.setSelected(notes.getRecordBend()); 
            recordCC.setSelected(notes.getRecordCC()); 
            recordAftertouch.setSelected(notes.getRecordAftertouch()); 
            convertNRPNRPN.setSelected(notes.getConvertNRPNRPN()); 
            for(int i = 0; i < Motif.NUM_PARAMETERS; i++)
                {
                parameterType[i].setSelectedIndex(notes.getMIDIParameterType(i));
                }
            }
        finally { lock.unlock(); }                              
        seq = old;
        name.update();
        if (end != null) end.revise();
        for(int i = 0; i < Motif.NUM_PARAMETERS; i++)
            {
            if (parameterMSB[i] != null) parameterMSB[i].redraw();
            if (parameterLSB[i] != null) parameterLSB[i].redraw();
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
		"Adjusts the End time of the Notes.  To set it, press <b>Set</b>. The Notes will<br>" +
		"terminate after the End, or after the last event has completed, whichever is later.</html>";
	
	static final String SET_END_TOOLTIP = "<html><b>Set End</b><br>" +
		"Sets the End of the Notes to the time at left.  The Notes will terminate<br>" +
		"after the End, or after the last event has completed, whichever is later.</html>";
	
	static final String ECHO_TOOLTIP = "<html><b>Echo</b><br>" +
		"Sets whether received MIDI data will be echoed out Output during recording.</html.";
		
	static final String ARMED_TOOLTIP = "<html><b>Arm</b><br>" +
		"Arms the Notes to receive and record MIDI data during recording.</html>";
	
	static final String RECORD_BEND_TOOLTIP = "<html><b>Record Bend</b><br>" +
		"Sets whether the Notes will record Pitch Bend data during recording.</html>";
	
	static final String RECORD_AFTERTOUCH_TOOLTIP = "<html><b>Record Aftertouch</b><br>" +
		"Sets whether the Notes will record Aftertouch data during recording.</html>";
	
	static final String RECORD_CC_TOOLTIP = "<html><b>Record CC</b><br>" +
		"Sets whether the Notes will record CC data during recording.</html>";
	
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
