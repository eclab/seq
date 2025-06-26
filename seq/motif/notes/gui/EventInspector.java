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

public class EventInspector extends WidgetList
    {
    Seq seq;
    Notes notes;
    NotesUI notesui;
    
    Notes.Event event;
    int index;
    
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
    JButton setWhen;
    JPanel whenPanel;
    JLabel parameterResult;
    JLabel valueResult;
    PushButton valuePresets;
    int time;
    
    public static final String[] BEND_OPTIONS = new String[] { "-4096", "-1024", "-256", "-64", "-16", "-8", "-4", "-2", "-1", "0", "1", "2", "4", "8", "16", "64", "256", "1024", "4096" };
    public static final int[] BEND_VALUES = new int[] { -4096, -1024, -256, -64, -16, -8, -4, -2, -1, 0, 1, 2, 4, 8, 16, 64, 256, 1024, 4096 };
        
    public static final String[] KEYS = new String[] { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B" };

    public int getIndex() { return index; }
    
    void updateTable()
        {
        notesui.repaint();
        }
    
    public EventInspector(Seq seq, Notes notes, NotesUI notesui, int index)
        {
        String[] strs = null;
        JComponent[] comps = null;

        this.seq = seq;
        this.notes = notes;
        this.notesui = notesui;
        if (index < 0 || index >= notes.getEvents().size())
            {
            this.index = -1;
            event = null;
            }
        else
            {
            this.index = index;
            event = notes.getEvents().get(index);
            }
        
        if (event == null)
            {
            strs = new String[] { "Type" };
            comps = new JComponent[] { new JLabel("Missing") };
            build(strs, comps);
            }
        else
            {
            ReentrantLock lock = seq.getLock();
            lock.lock();
            try
                {
                time = event.when;
                when = new TimeDisplay(event.when, seq, false)
                    {
                    public void updateTime(int time)
                        {
                        // this is already inside the lock but whatever
                        EventInspector.this.time = time;
                        }
                    };
                                        
                setWhen = new JButton("Set");
                setWhen.addActionListener(new ActionListener()
                    {
                    public void actionPerformed(ActionEvent evt)
                        {
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try
                            {
                            event.when = time;
                            notes.computeMaxTime();	  // note on/off time has just changed
                            notes.sortEvents(notes.getEvents());
                            }
                        finally
                            {
                            lock.unlock();
                            }
                        notesui.reloadTable();
                        notesui.getTable().setSelection(notes.getEvents().indexOf(event));		// re-select the event after sorting and reloading
                        }
                    });

                JPanel vert = new JPanel();
                vert.setLayout(new BorderLayout());
                JLabel temp = new JLabel(" ");
                temp.setFont(SmallDial.FONT);
                vert.add(temp, BorderLayout.NORTH);
                vert.add(setWhen, BorderLayout.CENTER);
                temp = new JLabel(" ");
                temp.setFont(SmallDial.FONT);
                vert.add(temp, BorderLayout.SOUTH);
                                        
                whenPanel = new JPanel();
                whenPanel.setLayout(new BorderLayout());
                whenPanel.add(when, BorderLayout.WEST);
                whenPanel.add(vert, BorderLayout.EAST);



                                        
                if (event instanceof Notes.Note)
                    {
                    Notes.Note note = (Notes.Note) event;
                    length = new TimeDisplay(note.length, seq)
                        {
                        public void updateTime(int time)
                            {
                            // this is already inside the lock but whatever
                            note.length = time; 
                            notes.computeMaxTime();	  // note off time has just changed
                            updateTable();
                            }
                        };

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
                            }
                        };

                    velocity = new SmallDial((note.velocity == 0 ? 0 : note.velocity - 1) / 126.0)              // we dont allow 0 velocity
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
                            }
                        };

                    release = new SmallDial(note.velocity / 127.0)
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
                            }
                        };
                    strs = new String[] { "Type", "When", "Length", "Pitch", "Velocity", "Release" };
                    comps = new JComponent[] 
                        {
                        new JLabel("Note"),
                        whenPanel,
                        length,
                        pitch.getLabelledDial("A#"),
                        velocity.getLabelledDial("127"),
                        release.getLabelledDial("127")
                        };
                    }
                else if (event instanceof Notes.Bend)
                    {
                    Notes.Bend bend = (Notes.Bend) event;
                    value = new SmallDial((bend.value + 8192) / 16383.0)
                        {
                        protected String map(double val) { return "" + (((int)(val * 16383.0)) - 8192); }
                        public double getValue() 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { return (bend.value + 8192) / 16383.0; }
                            finally { lock.unlock(); }
                            }
                        public void setValue(double val) 
                            { 
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { bend.value = (((int)(val * 16383.0)) - 8192);  }
                            finally { lock.unlock(); }
                            updateTable(); 
                            }
                        };
                    value.setScale(512.0);	// increase resolution

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
							}
						};
						
					JPanel valuePanel = new JPanel();
        			valuePanel.setLayout(new BorderLayout());
        			valuePanel.add(value.getLabelledDial("-8192"), BorderLayout.CENTER);   // so it stretches
    			    valuePanel.add(valuePresets, BorderLayout.EAST); 
	
                    strs = new String[] { "Type", "When", "Bend" };
                    comps = new JComponent[] 
                        {
                        new JLabel("Bend"),
                        whenPanel,
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
                            }
                        };
                    value = new SmallDial(cc.value / 127.0)
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
                            }
                        };
                    strs = new String[] { "Type", "When", "Parameter", "Value" };
                    comps = new JComponent[] 
                        {
                        new JLabel("CC"),
                        whenPanel,
                        parameter.getLabelledDial("127"),
                        value.getLabelledDial("127")
                        };
                    }
                else if (event instanceof Notes.NRPN)
                    {
                    Notes.NRPN nrpn = (Notes.NRPN) event;
                    parameterResult = new JLabel("");
                    valueResult = new JLabel("");
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
                            }
                        };
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
                            }
                        };
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
                            }
                        };
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
                            }
                        };
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

                    strs = new String[] { "Type", "When", "Parameter", "Value" };
                    comps = new JComponent[] 
                        {
                        new JLabel("NRPN"),
                        whenPanel,
                        pan1,
                        pan3,
                        };
                    }

                else if (event instanceof Notes.RPN)
                    {
                    Notes.RPN rpn = (Notes.RPN) event;
                    parameterResult = new JLabel("");
                    valueResult = new JLabel("");
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
                            }
                        };
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
                            }
                        };
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
                            }
                        };
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
                            }
                        };
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

                    strs = new String[] { "Type", "When", "Parameter", "Value" };
                    comps = new JComponent[] 
                        {
                        new JLabel("RPN"),
                        whenPanel,
                        pan1,
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
                    value = new SmallDial(aftertouch.value / 127.0)
                        {
                        protected String map(double val) { return "" + (int)(aftertouch.value * 127.0); }
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
                            }
                        };
                    strs = new String[] { "Type", "When", "Pitch", "Value" };
                    comps = new JComponent[] 
                        {
                        new JLabel("Aftertouch"),
                        whenPanel,
                        pitch.getLabelledDial("A#"),
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
    }
