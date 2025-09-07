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
import java.util.*;

public class EventInspector extends WidgetList
    {
    Seq seq;
    Notes notes;
    NotesUI notesui;
    
    Notes.Event event;
    
    TimeDisplay when;
    TimeDisplay length;
    SmallDial pitch;
    SmallDial velocity;
    SmallDial release;
    SmallDial value;
    SmallDial parameter;
    SmallDial parameterMSB;
    SmallDial parameterLSB;
    SmallDial valueMSB;
    SmallDial valueLSB;
    JButton setParameter;
    JLabel parameterResult;
    JLabel valueResult;
    PushButton valuePresets;
    int param;    
    int pitchTemp;
    int typeTemp;
    int type;

    public static final String[] BEND_OPTIONS = new String[] { "-4096", "-1024", "-256", "-64", "-16", "-8", "-4", "-2", "-1", "0", "1", "2", "4", "8", "16", "64", "256", "1024", "4096" };
    public static final int[] BEND_VALUES = new int[] { -4096, -1024, -256, -64, -16, -8, -4, -2, -1, 0, 1, 2, 4, 8, 16, 64, 256, 1024, 4096 };
        
    public static final String[] KEYS = new String[] { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B" };

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


    public Notes.Event getEvent() { return event; }

    void updateTable()
        {
        notesui.repaint();
        }
        
    public String getName() 
        { 
        if (type < Notes.TYPE_CC)  // it's a note, don't show the pitch
            {
            return Notes.getTypeInitial(type); 
            }
        else
            {
            return Notes.getTypeName(type); 
            }
        }
    
    public EventInspector(Seq seq, Notes notes, NotesUI notesui, Notes.Event event)
        {
        String[] strs = null;
        JComponent[] comps = null;
        this.seq = seq;
        this.notes = notes;
        this.notesui = notesui;
        this.type = event.getType();
        this.event = event;
        buildDefaults(notes);

        if (event == null)
            {
            //strs = new String[] { "Type" };
            //comps = new JComponent[] { new JLabel("Missing") };
            //build(strs, comps);
            }
        else
            {
            ReentrantLock lock = seq.getLock();
            lock = seq.getLock();
            lock.lock();
            try
                {
                when = new TimeDisplay(event.when, seq)
                    {
                    public int getTime()
                        {
                        return event.when;
                        }

                    public void setTime(int time)
                        {
                        // this is already inside the lock but whatever
                        event.when = time; 
                        notes.computeMaxTime();       // note off time has just changed
                        if (event instanceof Notes.Note)
                            {
                            pitchTemp = ((Notes.Note)event).pitch;
                            }
                        typeTemp = event.getType();
                        }
                        
                    public void setTimeOutside(int time)
                        {
                        updateTable();
                        if (event instanceof Notes.Note)
                            {
                            NoteUI noteui = notesui.getNoteUIFor((Notes.Note)event, pitchTemp);
                            if (noteui != null) 
                                {
                                noteui.reload();
                                notesui.getGridUI().getPitchUIs().get(pitchTemp).repaint();
                                }
                            }
                        else
                            {
                            EventUI eventui = notesui.getEventUIFor(event, typeTemp);
                            if (eventui != null) 
                                {
                                eventui.reload();
                                ParameterUI parameterui = notesui.getEventsUI().getParameterUIFor(typeTemp);
                                if (parameterui != null)
                                    {
                                    parameterui.repaint();
                                    }
                                }
                            }
                        }
                    };
                when.setToolTipText(WHEN_TOOLTIP);
                when.setDisplaysTime(true);

                JPanel vert = new JPanel();
                vert.setLayout(new BorderLayout());
                JLabel temp = new JLabel(" ");
                temp.setFont(SmallDial.FONT);
                vert.add(temp, BorderLayout.NORTH);
                vert.add(when, BorderLayout.CENTER);
                temp = new JLabel(" ");
                temp.setFont(SmallDial.FONT);
                vert.add(temp, BorderLayout.SOUTH);
                                        
                if (event instanceof Notes.Note)
                    {
                    Notes.Note note = (Notes.Note) event;
                    length = new TimeDisplay(note.length, seq)
                        {
                        public int getTime()
                            {
                            return note.length;
                            }
                        
                        public void setTime(int time)
                            {
                            // this is already inside the lock but whatever
                            note.length = time; 
                            notes.computeMaxTime();       // note off time has just changed
                            }
                        
                        public void setTimeOutside(int time)
                            {
                            updateTable();
                            notesui.getGridUI().reloadSelected();
                            }
                        };
                    length.setToolTipText(LENGTH_TOOLTIP);
                                        
                    pitch = new SmallDial(note.pitch / 127.0)
                        {
                        protected String map(double val) 
                            { 
                            int p = (int)(val * 127.0);
                            return "" + ( KEYS[p % 12] + (p / 12));
                            }
                        public double getValue() 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { return note.pitch / 127.0; }
                            finally { lock.unlock(); }
                            }
                        public void setValue(double val) 
                            { 
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { note.pitch = (int)(val * 127.0); }
                            finally { lock.unlock(); }
                            updateTable(); 
                            notesui.getGridUI().reloadSelected();
                            }
                        };
                    pitch.setToolTipText(PITCH_TOOLTIP);

                    velocity = new SmallDial((note.velocity == 0 ? 0 : note.velocity - 1) / 126.0, defaults)              // we dont allow 0 velocity
                        {
                        protected String map(double val) { return "" + ((int)(val * 126.0) + 1); }
                        public double getValue() 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { return (note.velocity == 0 ? 0 : note.velocity - 1) / 126.0; }
                            finally { lock.unlock(); }
                            }
                        public void setValue(double val) 
                            { 
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { note.velocity = (int)(val * 126.0) + 1;  }
                            finally { lock.unlock(); }
                            updateTable(); 
                            notesui.getGridUI().reloadSelected();
                            }
						public void setDefault(int val) 
							{ 
							ReentrantLock lock = seq.getLock();
							lock.lock();
							try { if (val != SmallDial.NO_DEFAULT) note.velocity = (-(val + 1)); }
							finally { lock.unlock(); }
                            updateTable(); 
                            notesui.getGridUI().reloadSelected();
							}
						public int getDefault()
							{
							ReentrantLock lock = seq.getLock();
							lock.lock();
							try { double val = note.velocity; return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
							finally { lock.unlock(); }
							}
                        };
                    velocity.setToolTipText(VELOCITY_TOOLTIP);

                    release = new SmallDial(note.velocity / 127.0, defaults)
                        {
                        protected String map(double val) { return "" + (int)(val * 127.0); }
                        public double getValue() 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { return note.release / 127.0; }
                            finally { lock.unlock(); }
                            }
                        public void setValue(double val) 
                            { 
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { note.release = (int)(val * 127.0);  }
                            finally { lock.unlock(); }
                            updateTable(); 
                            notesui.getGridUI().reloadSelected();               // Maybe not necessary?
                            }
						public void setDefault(int val) 
							{ 
							ReentrantLock lock = seq.getLock();
							lock.lock();
							try { if (val != SmallDial.NO_DEFAULT) note.release = (-(val + 1)); }
							finally { lock.unlock(); }
                            updateTable(); 
                            notesui.getGridUI().reloadSelected();
							}
						public int getDefault()
							{
							ReentrantLock lock = seq.getLock();
							lock.lock();
							try { double val = note.release; return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
							finally { lock.unlock(); }
							}
                        };
                    release.setToolTipText(RELEASE_VELOCITY_TOOLTIP);

                    strs = new String[] { /*"Type",*/ "When", "Length", "Pitch", "Velocity", "Release" };
                    comps = new JComponent[] 
                        {
                        //new JLabel("Note"),
                        when,
                        length,
                        pitch.getLabelledDial("A#"),
                        velocity.getLabelledDial("127"),
                        release.getLabelledDial("127")
                        };
                    }
                else if (event instanceof Notes.Bend)
                    {
                    Notes.Bend bend = (Notes.Bend) event;
                    value = new SmallDial(bend.value / 16383.0, defaults)
                        {
                        protected String map(double val) { return "" + (((int)(val * 16383.0)) - 8192); }
                        public double getValue() 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { return bend.value / 16383.0; }
                            finally { lock.unlock(); }
                            }
                        public void setValue(double val) 
                            { 
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { bend.value = (int)(val * 16383.0);  }
                            finally { lock.unlock(); }
                            updateTable(); 
                            EventUI eventui = notesui.getEventsUI().getEventUIFor(event, event.getType());
                            if (eventui != null) eventui.reload();
                            }
 						public void setDefault(int val) 
							{ 
							ReentrantLock lock = seq.getLock();
							lock.lock();
							try { if (val != SmallDial.NO_DEFAULT) bend.value = (-(val + 1)); }
							finally { lock.unlock(); }
                            updateTable(); 
                            EventUI eventui = notesui.getEventsUI().getEventUIFor(event, event.getType());
                            if (eventui != null) eventui.reload();
							}
						public int getDefault()
							{
							ReentrantLock lock = seq.getLock();
							lock.lock();
							try { double val = bend.value; return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
							finally { lock.unlock(); }
							}
                       };
                    value.setScale(512.0);      // increase resolution
                    value.setToolTipText(VALUE_TOOLTIP);

                    valuePresets = new PushButton("Presets...", BEND_OPTIONS)
                        {
                        public void perform(int val)
                            {
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { bend.value = BEND_VALUES[val]; }
                            finally { lock.unlock(); }
                            value.redraw();
                            updateTable();
                            EventUI eventui = notesui.getEventsUI().getEventUIFor(event, event.getType());
                            if (eventui != null) eventui.reload();
                            }
                        };
                    valuePresets.setToolTipText(VALUE_PRESETS_TOOLTIP);
                                                
                    JPanel valuePanel = new JPanel();
                    valuePanel.setLayout(new BorderLayout());
                    valuePanel.add(value.getLabelledDial("-8192"), BorderLayout.CENTER);   // so it stretches
                    valuePanel.add(valuePresets, BorderLayout.EAST); 
                    valuePanel.setToolTipText(VALUE_TOOLTIP);
        
                    strs = new String[] { /*"Type",*/ "When", "Bend" };
                    comps = new JComponent[] 
                        {
                        //new JLabel("Bend"),
                        when,
                        valuePanel,
                        //value.getLabelledDial("-8192")
                        };

                    }
                else if (event instanceof Notes.CC)
                    {
                    Notes.CC cc = (Notes.CC) event;
                    parameter = new SmallDial(cc.parameter / 127.0)
                        {
                        protected String map(double val) { return "" + (int)(val * 127.0); }
                        public double getValue() 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { return cc.parameter / 127.0; }
                            finally { lock.unlock(); }
                            }
                        public void setValue(double val) 
                            { 
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { cc.parameter = (int)(val * 127.0); }
                            finally { lock.unlock(); }
                            updateTable();  
//                            notesui.getEventsUI().rebuild();
                            }
                        };
                    parameter.setToolTipText(PARAMETER_TOOLTIP);

                    value = new SmallDial(cc.value / 127.0, defaults)
                        {
                        protected String map(double val) { return "" + (int)(val * 127.0); }
                        public double getValue() 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { return cc.value / 127.0; }
                            finally { lock.unlock(); }
                            }
                        public void setValue(double val) 
                            { 
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { cc.value = (int)(val * 127.0); }
                            finally { lock.unlock(); }
                            updateTable();  
                            EventUI eventui = notesui.getEventsUI().getEventUIFor(event, event.getType());
                            if (eventui != null) eventui.reload();
                            }
						public void setDefault(int val) 
							{ 
							ReentrantLock lock = seq.getLock();
							lock.lock();
							try { if (val != SmallDial.NO_DEFAULT) cc.value = (-(val + 1)); }
							finally { lock.unlock(); }
                            updateTable();  
                            EventUI eventui = notesui.getEventsUI().getEventUIFor(event, event.getType());
                            if (eventui != null) eventui.reload();
							}
						public int getDefault()
							{
							ReentrantLock lock = seq.getLock();
							lock.lock();
							try { double val = cc.value; return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
							finally { lock.unlock(); }
							}
                        };
                    value.setToolTipText(VALUE_TOOLTIP);

                    strs = new String[] { /*"Type",*/ "When", /*"Parameter",*/ "Value" };
                    comps = new JComponent[] 
                        {
                        //new JLabel("CC"),
                        when,
                        //parameter.getLabelledDial("127"),
                        value.getLabelledDial("127")
                        };
                    }
                else if (event instanceof Notes.NRPN)
                    {
                    Notes.NRPN nrpn = (Notes.NRPN) event;
                    parameterResult = new JLabel("");
                    parameterResult.setToolTipText(PARAMETER_RESULT_TOOLTIP);
                    valueResult = new JLabel("");
                    valueResult.setToolTipText(VALUE_RESULT_TOOLTIP);
                    parameterMSB = new SmallDial((nrpn.parameter >> 7) / 127.0)
                        {
                        protected String map(double val) { return "" + (int)(val * 127.0); }
                        public double getValue() 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { return (nrpn.parameter >> 7) / 127.0; }
                            finally { lock.unlock(); }
                            }
                        public void setValue(double val) 
                            { 
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            int param;
                            try { param = nrpn.parameter = (((int)(val * 127.0)) << 7) + (nrpn.parameter & 127); }
                            finally { lock.unlock(); }
                            parameterResult.setText(" " + param);
                            updateTable();  
//                            notesui.getEventsUI().rebuild();
                            }
                        };
                    parameterMSB.setToolTipText(PARAMETER_MSB_TOOLTIP);

                    parameterLSB = new SmallDial((nrpn.parameter & 127) / 127.0)
                        {
                        protected String map(double val) { return "" + (int)(val * 127.0); }
                        public double getValue() 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { return (nrpn.parameter & 127) / 127.0; }
                            finally { lock.unlock(); }
                            }
                        public void setValue(double val) 
                            { 
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            int param;
                            try { param = nrpn.parameter = ((nrpn.parameter >>> 7) << 7) + ((int)(val * 127.0)); }
                            finally { lock.unlock(); }
                            parameterResult.setText(" " + param);
                            updateTable();  
                            // WARNING: this causes the EventUI to be rebuilt multiple times
//                            notesui.getEventsUI().rebuild();
                            }
                        };
                    parameterLSB.setToolTipText(PARAMETER_LSB_TOOLTIP);

                    valueMSB = new SmallDial((nrpn.value >> 7) / 127.0)
                        {
                        protected String map(double val) { return "" + (int)(val * 127.0); }
                        public double getValue() 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { return (nrpn.value >> 7) / 127.0; }
                            finally { lock.unlock(); }
                            }
                        public void setValue(double val) 
                            { 
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            int value;
                            try { value = nrpn.value = (((int)(val * 127.0)) << 7) + (nrpn.value & 127); }
                            finally { lock.unlock(); }
                            valueResult.setText(" " + value);
                            updateTable();  
                            EventUI eventui = notesui.getEventsUI().getEventUIFor(event, event.getType());
                            if (eventui != null) eventui.reload();
                            }
                        };
                    valueMSB.setToolTipText(VALUE_MSB_TOOLTIP);

                    valueLSB = new SmallDial((nrpn.value & 127) / 127.0)
                        {
                        protected String map(double val) { return "" + (int)(val * 127.0); }
                        public double getValue() 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { return (nrpn.value & 127) / 127.0; }
                            finally { lock.unlock(); }
                            }
                        public void setValue(double val) 
                            { 
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            int value;
                            try { value = nrpn.value = ((nrpn.value >>> 7) << 7) + ((int)(val * 127.0)); }
                            finally { lock.unlock(); }
                            valueResult.setText(" " + value);
                            updateTable();  
                            EventUI eventui = notesui.getEventsUI().getEventUIFor(event, event.getType());
                            if (eventui != null) eventui.reload();
                            }
                        };
                    valueLSB.setToolTipText(VALUE_LSB_TOOLTIP);

                    Box parameterBox = new Box(BoxLayout.X_AXIS);
                    parameterBox.add(parameterMSB.getLabelledTitledDialVertical("MSB", " 888 "));
                    parameterBox.add(parameterLSB.getLabelledTitledDialVertical("LSB", " 888 "));
                    JPanel pan2 = new JPanel();
                    pan2.setLayout(new BorderLayout());
                    pan2.add(parameterResult, BorderLayout.WEST);
                    JPanel pan1 = new JPanel();
                    pan1.setLayout(new BorderLayout());
                    pan1.add(parameterBox, BorderLayout.WEST);
                    pan1.add(pan2, BorderLayout.CENTER);
                    pan1.setToolTipText(PARAMETER_RESULT_TOOLTIP);

                    Box valueBox = new Box(BoxLayout.X_AXIS);
                    valueBox.add(valueMSB.getLabelledTitledDialVertical("MSB", " 888 "));
                    valueBox.add(valueLSB.getLabelledTitledDialVertical("LSB", " 888 "));
                    JPanel pan4 = new JPanel();
                    pan4.setLayout(new BorderLayout());
                    pan4.add(valueResult, BorderLayout.WEST);
                    JPanel pan3 = new JPanel();
                    pan3.setLayout(new BorderLayout());
                    pan3.add(valueBox, BorderLayout.WEST);
                    pan3.add(pan4, BorderLayout.CENTER);
                    pan3.setToolTipText(VALUE_RESULT_TOOLTIP);

                    strs = new String[] { /*"Type",*/ "When", /*"Parameter",*/ "Value" };
                    comps = new JComponent[] 
                        {
                        //new JLabel("NRPN"),
                        when,
                        //pan1,
                        pan3,
                        };
                    }

                else if (event instanceof Notes.RPN)
                    {
                    Notes.RPN rpn = (Notes.RPN) event;
                    parameterResult = new JLabel("");
                    parameterResult.setToolTipText(PARAMETER_RESULT_TOOLTIP);
                    valueResult = new JLabel("");
                    valueResult.setToolTipText(VALUE_RESULT_TOOLTIP);
                    parameterMSB = new SmallDial((rpn.parameter >> 7) / 127.0)
                        {
                        protected String map(double val) { return "" + (int)(val * 127.0); }
                        public double getValue() 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { return (rpn.parameter >> 7) / 127.0; }
                            finally { lock.unlock(); }
                            }
                        public void setValue(double val) 
                            { 
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            int param;
                            try { param = rpn.parameter = (((int)(val * 127.0)) << 7) + (rpn.parameter & 127); }
                            finally { lock.unlock(); }
                            parameterResult.setText(" " + param);
                            updateTable();
//                            notesui.getEventsUI().rebuild();
                            }
                        };
                    parameterMSB.setToolTipText(PARAMETER_MSB_TOOLTIP);

                    parameterLSB = new SmallDial((rpn.parameter & 127) / 127.0)
                        {
                        protected String map(double val) { return "" + (int)(val * 127.0); }
                        public double getValue() 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { return (rpn.parameter & 127) / 127.0; }
                            finally { lock.unlock(); }
                            }
                        public void setValue(double val) 
                            { 
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            int param;
                            try { param = rpn.parameter = ((rpn.parameter >>> 7) << 7) + ((int)(val * 127.0)); }
                            finally { lock.unlock(); }
                            parameterResult.setText(" " + param);
                            updateTable();  
//                            notesui.getEventsUI().rebuild();
                            }
                        };
                    parameterLSB.setToolTipText(PARAMETER_LSB_TOOLTIP);

                    valueMSB = new SmallDial((rpn.value >> 7) / 127.0)
                        {
                        protected String map(double val) { return "" + (int)(val * 127.0); }
                        public double getValue() 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { return (rpn.value >> 7) / 127.0; }
                            finally { lock.unlock(); }
                            }
                        public void setValue(double val) 
                            { 
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            int value;
                            try { value = rpn.value = (((int)(val * 127.0)) << 7) + (rpn.value & 127); }
                            finally { lock.unlock(); }
                            valueResult.setText(" " + value);
                            updateTable();  
                            EventUI eventui = notesui.getEventsUI().getEventUIFor(event, event.getType());
                            if (eventui != null) eventui.reload();
                            }
                        };
                    valueMSB.setToolTipText(VALUE_MSB_TOOLTIP);

                    valueLSB = new SmallDial((rpn.value & 127) / 127.0)
                        {
                        protected String map(double val) { return "" + (int)(val * 127.0); }
                        public double getValue() 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { return (rpn.value & 127) / 127.0; }
                            finally { lock.unlock(); }
                            }
                        public void setValue(double val) 
                            { 
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            int value;
                            try { value = rpn.value = ((rpn.value >>> 7) << 7) + ((int)(val * 127.0)); }
                            finally { lock.unlock(); }
                            valueResult.setText(" " + value);
                            updateTable();  
                            EventUI eventui = notesui.getEventsUI().getEventUIFor(event, event.getType());
                            if (eventui != null) eventui.reload();
                            }
                        };
                    valueLSB.setToolTipText(VALUE_LSB_TOOLTIP);

                    Box parameterBox = new Box(BoxLayout.X_AXIS);
                    parameterBox.add(parameterMSB.getLabelledTitledDialVertical("MSB", " 888 "));
                    parameterBox.add(parameterLSB.getLabelledTitledDialVertical("LSB", " 888 "));
                    JPanel pan2 = new JPanel();
                    pan2.setLayout(new BorderLayout());
                    pan2.add(parameterResult, BorderLayout.WEST);
                    JPanel pan1 = new JPanel();
                    pan1.setLayout(new BorderLayout());
                    pan1.add(parameterBox, BorderLayout.WEST);
                    pan1.add(pan2, BorderLayout.CENTER);
                    pan1.setToolTipText(PARAMETER_RESULT_TOOLTIP);

                    Box valueBox = new Box(BoxLayout.X_AXIS);
                    valueBox.add(valueMSB.getLabelledTitledDialVertical("MSB", " 888 "));
                    valueBox.add(valueLSB.getLabelledTitledDialVertical("LSB", " 888 "));
                    JPanel pan4 = new JPanel();
                    pan4.setLayout(new BorderLayout());
                    pan4.add(valueResult, BorderLayout.WEST);
                    JPanel pan3 = new JPanel();
                    pan3.setLayout(new BorderLayout());
                    pan3.add(valueBox, BorderLayout.WEST);
                    pan3.add(pan4, BorderLayout.CENTER);
                    pan3.setToolTipText(VALUE_RESULT_TOOLTIP);

                    strs = new String[] { /*"Type",*/ "When", /*"Parameter",*/ "Value" };
                    comps = new JComponent[] 
                        {
                        //new JLabel("RPN"),
                        when,
                        //pan1,
                        pan3,
                        };
                    }

                else if (event instanceof Notes.Aftertouch)
                    {
                    Notes.Aftertouch aftertouch = (Notes.Aftertouch) event;
                    pitch = new SmallDial((aftertouch.pitch + 1) / 128.0)
                        {
                        protected String map(double val) 
                            {
                            int p = (int)((val * 128.0) - 1);
                            if (p == -1) return "[Channel AT]";
                            else return "" + ( KEYS[p % 12] + (p / 12));
                            }
                        public double getValue() 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { return (aftertouch.pitch + 1) / 128.0; }
                            finally { lock.unlock(); }
                            }
                        public void setValue(double val) 
                            { 
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { aftertouch.pitch = (int)((val * 128.0) - 1); }
                            finally { lock.unlock(); }
                            updateTable();  
                            }
                        };

                    pitch.setToolTipText(AFTERTOUCH_PITCH_TOOLTIP);

                    value = new SmallDial(aftertouch.value / 127.0, defaults)
                        {
                        protected String map(double val) { return "" + (int)(val * 127.0); }
                        public double getValue() 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { return aftertouch.value / 127.0; }
                            finally { lock.unlock(); }
                            }
                        public void setValue(double val) 
                            { 
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { aftertouch.value = (int)(val * 127.0); }
                            finally { lock.unlock(); }
                            updateTable();  
                            EventUI eventui = notesui.getEventsUI().getEventUIFor(event, event.getType());
                            if (eventui != null) eventui.reload();
                            }
						public void setDefault(int val) 
							{ 
							ReentrantLock lock = seq.getLock();
							lock.lock();
							try { if (val != SmallDial.NO_DEFAULT) aftertouch.value = (-(val + 1)); }
							finally { lock.unlock(); }
                            updateTable();  
                            EventUI eventui = notesui.getEventsUI().getEventUIFor(event, event.getType());
                            if (eventui != null) eventui.reload();
							}
						public int getDefault()
							{
							ReentrantLock lock = seq.getLock();
							lock.lock();
							try { double val = aftertouch.value; return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
							finally { lock.unlock(); }
							}
                        };
                    value.setToolTipText(VALUE_TOOLTIP);

					if (aftertouch.pitch == Out.CHANNEL_AFTERTOUCH)
						{
						strs = new String[] { /*"Type",*/ "When", "Value" };
						comps = new JComponent[] 
							{
							//new JLabel("Aftertouch"),
							when,
							value.getLabelledDial("127")
							};
						}
					else
						{
						strs = new String[] { /*"Type",*/ "When", "Pitch", "Value" };
						comps = new JComponent[] 
							{
							//new JLabel("Aftertouch"),
							when,
							pitch.getLabelledDial("A#"),
							value.getLabelledDial("127")
							};
						}
                    }
                 else if (event instanceof Notes.PC)
                    {
                    Notes.PC pc = (Notes.PC) event;
                    value = new SmallDial(pc.value / 127.0, defaults)
                        {
                        protected String map(double val) { return "" + (int)(val * 127.0); }
                        public double getValue() 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { return pc.value / 127.0; }
                            finally { lock.unlock(); }
                            }
                        public void setValue(double val) 
                            { 
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { pc.value = (int)(val * 127.0); }
                            finally { lock.unlock(); }
                            updateTable();  
                            EventUI eventui = notesui.getEventsUI().getEventUIFor(event, event.getType());
                            if (eventui != null) eventui.reload();
                            }
						public void setDefault(int val) 
							{ 
							ReentrantLock lock = seq.getLock();
							lock.lock();
							try { if (val != SmallDial.NO_DEFAULT) pc.value = (-(val + 1)); }
							finally { lock.unlock(); }
                            updateTable();  
                            EventUI eventui = notesui.getEventsUI().getEventUIFor(event, event.getType());
                            if (eventui != null) eventui.reload();
							}
						public int getDefault()
							{
							ReentrantLock lock = seq.getLock();
							lock.lock();
							try { double val = pc.value; return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
							finally { lock.unlock(); }
							}
                        };
                    value.setToolTipText(VALUE_TOOLTIP);

                    strs = new String[] {  "Value" };
                    comps = new JComponent[] 
                        {
                        value.getLabelledDial("127")
                        };
                    }
               }
            finally { lock.unlock(); }
            build(strs, comps);
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
            //nothing for now
            }
        finally { lock.unlock(); }                              
        seq = old;
        if (when != null) when.revise();
        if (length != null) length.revise();
        if (pitch != null) pitch.redraw();
        if (velocity != null) velocity.redraw();
        if (release != null) release.redraw();
        if (value != null) value.redraw();
        if (parameter != null) parameter.redraw();
        if (parameterMSB != null) parameterMSB.redraw();
        if (parameterLSB != null) parameterLSB.redraw();
        if (valueMSB != null) valueMSB.redraw();
        if (valueLSB != null) valueLSB.redraw();
        if (parameterResult != null) parameterResult.repaint();
        if (valueResult != null) valueResult.repaint();
        }
 
 
    static final String WHEN_TOOLTIP = "<html><b>When</b><br>" +
        "Adjusts when this event occurs.  To set it, press <b>Set</b>.</html>";

    static final String SET_WHEN_TOOLTIP = "<html><b>Set When</b><br>" +
        "Sets when this event occurs to the time at left.</html>";
        
    static final String LENGTH_TOOLTIP = "<html><b>Length</b><br>" +
        "Sets the length of this note.</html>";
        
    static final String PITCH_TOOLTIP = "<html><b>Pitch</b><br>" +
        "Sets the pitch of this note.</html>";
        
    static final String AFTERTOUCH_PITCH_TOOLTIP = "<html><b>Pitch</b><br>" +
        "Sets the pitch of this note receiving aftertouch.<br><br>" +
        "Set to <b>[Channel AT]</b> to set this to channel (whole keyboard) aftertouch.</html>";
        
    static final String VELOCITY_TOOLTIP = "<html><b>Velocity</b><br>" +
        "Sets the velocity (volume) of this note.</html>";

    static final String RELEASE_VELOCITY_TOOLTIP = "<html><b>Release Velocity</b><br>" +
        "Sets the release velocity of this note.  This is the velocity with<br>" +
        "which the key was released.  In MIDI, by default this is 64.</html>";
        
    static final String PARAMETER_TOOLTIP = "<html><b>Parameter</b><br>" +
        "Sets the parameter number.</html.";
                
    static final String PARAMETER_MSB_TOOLTIP = "<html><b>Parameter MSB</b><br>" +
        "Sets the MSB (\"Most Significant Byte\") of the parameter number.<br><br>" +
        "The parameter number is determined by the MSB \u00D7 128 + LSB.</html>";
        
    static final String PARAMETER_LSB_TOOLTIP = "<html><b>Parameter LSB</b><br>" +
        "Sets the LSB (\"Least Significant Byte\") of the parameter number.<br><br>" +
        "The parameter number is determined by the MSB \u00D7 128 + LSB.</html>";
        
    static final String VALUE_TOOLTIP = "<html><b>Value</b><br>" +
        "Sets the aftertouch value.</html>";
        
    static final String VALUE_MSB_TOOLTIP = "<html><b>Value MSB</b><br>" +
        "Sets the MSB (\"Most Significant Byte\") of the parameter's value.<br><br>" +
        "The value is determined by the MSB \u00D7 128 + LSB, as shown at right.</html>";
        
    static final String VALUE_LSB_TOOLTIP = "<html><b>Value LSB</b><br>" +
        "Sets the LSB (\"Least Significant Byte\") of the parameter's value.<br><br>" +
        "The value is determined by the MSB \u00D7 128 + LSB, as shown at right.</html>";
        
    static final String PARAMETER_RESULT_TOOLTIP = "<html><b>Parameter</b><br>" +
        "The parameter number.<br><br>" + 
        "The parameter number is determined by the MSB \u00D7 128 + LSB.</html>";

    static final String VALUE_RESULT_TOOLTIP = "<html><b>Value</b><br>" +
        "The parameter's value.<br><br>" + 
        "The value is determined by the MSB \u00D7 128 + LSB.</html>";

    static final String VALUE_PRESETS_TOOLTIP = "<html><b>Pitch Bend Presets</b><br>" +
        "Presets for the Pitch Bend value.</html>";

    }
