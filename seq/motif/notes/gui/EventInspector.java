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
    JButton setWhen;
    JPanel whenPanel;
    int time;
        
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
                            notes.sortEvents(notes.getEvents());
                            }
                        finally
                            {
                            lock.unlock();
                            }
                        notesui.reloadTable();
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
                            notes.computeMaxTime();
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
                    strs = new String[] { "Type", "When", "Bend" };
                    comps = new JComponent[] 
                        {
                        new JLabel("Bend"),
                        whenPanel,
                        value.getLabelledDial("-8192")
                        };
                    }
                else if (event instanceof Notes.CC)
                    {
                    Notes.CC cc = (Notes.CC) event;
                    parameter = new SmallDial(cc.parameter / 127.0)
                        {
                        protected String map(double val) { return "" + (int)(cc.parameter * 127.0); }
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
                            try { cc.value = (int)(cc.parameter * 127.0); }
                            finally { lock.unlock(); }
                            updateTable();  
                            }
                        };
                    value = new SmallDial(cc.value / 127.0)
                        {
                        protected String map(double val) { return "" + (int)(cc.value * 127.0); }
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
                        parameter,
                        value.getLabelledDial("127")
                        };
                    }
                else if (event instanceof Notes.Aftertouch)
                    {
                    Notes.Aftertouch aftertouch = (Notes.Aftertouch) event;
                    pitch = new SmallDial(aftertouch.pitch / 127.0)
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
                            try { return aftertouch.pitch / 127.0; }
                            finally { lock.unlock(); }
                            }
                        public void setValue(double val) 
                            { 
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { aftertouch.pitch = (int)(val * 127.0); }
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
        }
    }
