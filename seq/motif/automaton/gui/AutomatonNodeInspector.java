/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.automaton.gui;

import seq.motif.automaton.*;
import seq.engine.*;
import seq.gui.*;
import seq.motif.parallel.gui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.locks.*;

public class AutomatonNodeInspector extends WidgetList
    {
    public static final String[] QUANTIZATIONS = { "None", "16th Note", "Quarter Note", "Measure" };
    public static final int MAX_ITERATIONS = 64;
    public static final int MAX_REPEATS = 64;
    public static final int MAX_JOINS = 8;                      // this should be >= 2 and <= Automaton.MAX_THREADS
    
    public static final String[] KEYS = new String[] { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B" };

    Seq seq;
    Automaton automaton;
    MotifUI motifui;
    AutomatonUI owner;
    Automaton.Node node;
        
    // ALL              Nickname, isStart [radio button]
    // FORK
    // JOIN
    // DELAY        Delay Amount
    // CHORD            4 Pitches, Velocity, Release, Length, % Time On, 
    // RANDOM       Weight 1 .. 4
    // ITERATE      Count 1 .. 4, Loop
    // FINISHED
    // MOTIFNODE Repeats, Repeat Probability, Quantize options

    JRadioButton start;
    TimeDisplay delay;
    SmallDial[] aux = new SmallDial[Automaton.MAX_OUT];
    SmallDial repeats;
    JCheckBox untilTrigger;
    JPanel repeatPanel;
    SmallDial repeatProbability;
    JCheckBox loop;
    SmallDial join;
    StringField name;
    JComboBox quantization;
    JButton launch;
    SmallDial gate;
    SmallDial velocity;
    SmallDial release;
    SmallDial pitch1;
    SmallDial pitch2;
    SmallDial pitch3;
    SmallDial pitch4;
            
    public AutomatonNodeInspector(Seq seq, final Automaton automaton, final MotifButton button, AutomatonUI owner, Automaton.Node node)
        {
        this.seq = seq;
        this.automaton = automaton;
        this.motifui = button.getMotifUI();
        this.owner = owner;
        this.node = node;
                
        String[] strs = null;
        JComponent[] comps = null;

        if (node != null)
            {
            ReentrantLock lock = seq.getLock();
            lock.lock();
            try
                {
                if (node.getNickname() == null) node.setNickname("");
                name = new StringField(node.getNickname())
                    {
                    public String newValue(String newValue)
                        {
                        newValue = newValue.trim();
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { node.setNickname(newValue); }
                        finally { lock.unlock(); }
                        if (node instanceof Automaton.MotifNode)
                            {
                            owner.updateText();                           // FIXME It's not clear what utility node buttons have as their motifui
                            }
                        else            // Some other kind of node
                            {
                            button.updateText();
                            }
                        return newValue;
                        }
                                                                
                    public String getValue() 
                        {
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { return node.getNickname(); }
                        finally { lock.unlock(); }
                        }
                    };
                name.setColumns(MotifUI.INSPECTOR_NAME_DEFAULT_SIZE);

                // start is a Radio Button because, once set, it can't be unset as there is no other Radio Button            
                start = new JRadioButton("");
                start.setSelected(automaton.getStart() == node);
                start.addActionListener(new ActionListener()
                    {
                    public void actionPerformed(ActionEvent e)
                        {
                        seq.push();
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try
                            {
                            if (start.isSelected()) // always will be
                                {
                                automaton.setStart(node);
                                }
                            }
                        finally
                            {
                            lock.unlock();
                            }
                        // reset all the buttons 
                        owner.resetStart();
                        owner.redraw();
                        }
                    });

                launch = new JButton("New Thread");
                launch.addActionListener(new ActionListener()
                    {
                    public void actionPerformed(ActionEvent e)
                        {
                        if (seq == null) return;
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try 
                            { 
                            owner.launchThread(node);
                            }
                        finally { lock.unlock(); }                              
                        }
                    });

                Box actions = new Box(BoxLayout.X_AXIS);
                actions.add(launch);
                JPanel panel = new JPanel();
                panel.setLayout(new BorderLayout());
                panel.add(actions, BorderLayout.WEST);


                if (node instanceof Automaton.Delay)
                    {
                    final Automaton.Delay ndelay = (Automaton.Delay)node;
                    delay = new TimeDisplay(ndelay.getDelay(), seq)
                        {
                        public void updateTime(int time)
                            {
                            // called from inside lock
                            ndelay.setDelay(time);
                            }
                        };
                    strs = new String[] { "Type", "Nickname", "Actions", "Start Node", "Delay" };
                    comps = new JComponent[] 
                        {
                        new JLabel("Delay"),
                        name,
                        actions,
                        start,
                        delay,
                        };
                    }
                else if (node instanceof Automaton.Chord)
                    {
                    final Automaton.Chord nchord = (Automaton.Chord) node;

                    delay = new TimeDisplay(nchord.getLength(), seq)
                        {
                        public void updateTime(int time)
                            {
                            // called from inside lock
                            nchord.setLength(time);
                            }
                        };
                    velocity = new SmallDial(nchord.getVelocity()/127.0)
                        {
                        protected String map(double val) { return String.valueOf((int)(val * 127)); }
                        public double getValue() 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { return nchord.getVelocity() / 127.0; }
                            finally { lock.unlock(); }
                            }
                        public void setValue(double val) 
                            { 
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { nchord.setVelocity((int)(val * 127));}
                            finally { lock.unlock(); }
                            }
                        };
                    release = new SmallDial(nchord.getRelease())
                        {
                        protected String map(double val) { return String.valueOf((int)(val * 127)); }
                        public double getValue() 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { return nchord.getRelease() / 127; }
                            finally { lock.unlock(); }
                            }
                        public void setValue(double val) 
                            { 
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { nchord.setRelease((int)(val * 127)); }
                            finally { lock.unlock(); }
                            }
                        };
                                                
                    gate = new SmallDial(nchord.getTimeOn())
                        {
                        protected String map(double val) { return String.format("%.4f", val); }
                        public double getValue() 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { return nchord.getTimeOn(); }
                            finally { lock.unlock(); }
                            }
                        public void setValue(double val) 
                            { 
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { nchord.setTimeOn(val); }
                            finally { lock.unlock(); }
                            }
                        };
                                                
                    /// FIXME: Okay this should be done with a for-loop....
                                        
                    pitch1 = new SmallDial(nchord.getNote(0)/128.0)
                        {
                        protected String map(double val) 
                            { 
                            int p = (int)(val * 128);
                            if (p == 128) return "--";
                            return "" + ( KEYS[p % 12] + (p / 12));
                            }
                        public double getValue() 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { return nchord.getNote(0) / 128.0; }
                            finally { lock.unlock(); }
                            }
                        public void setValue(double val) 
                            { 
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { nchord.setNote(0, (int)(val * 128)); }
                            finally { lock.unlock(); }
                            }
                        };
                    pitch2 = new SmallDial(nchord.getNote(1) / 128.0)
                        {
                        protected String map(double val) 
                            { 
                            int p = (int)(val * 128);
                            if (p == 128) return "--";
                            return "" + ( KEYS[p % 12] + (p / 12));
                            }
                        public double getValue() 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { return nchord.getNote(1) / 128.0; }
                            finally { lock.unlock(); }
                            }
                        public void setValue(double val) 
                            { 
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { nchord.setNote(1, (int)(val * 128)); }
                            finally { lock.unlock(); }
                            }
                        };
                    pitch3 = new SmallDial(nchord.getNote(2) / 128.0)
                        {
                        protected String map(double val) 
                            { 
                            int p = (int)(val * 128);
                            if (p == 128) return "--";
                            return "" + ( KEYS[p % 12] + (p / 12));
                            }
                        public double getValue() 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { return nchord.getNote(2) / 128.0; }
                            finally { lock.unlock(); }
                            }
                        public void setValue(double val) 
                            { 
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { nchord.setNote(2, (int)(val * 128)); }
                            finally { lock.unlock(); }
                            }
                        };
                    pitch4 = new SmallDial(nchord.getNote(3) / 128.0)
                        {
                        protected String map(double val) 
                            { 
                            int p = (int)(val * 128);
                            if (p == 128) return "--";
                            return "" + ( KEYS[p % 12] + (p / 12));
                            }
                        public double getValue() 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { return nchord.getNote(3) / 128.0; }
                            finally { lock.unlock(); }
                            }
                        public void setValue(double val) 
                            { 
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { nchord.setNote(3, (int)(val * 128)); }
                            finally { lock.unlock(); }
                            }
                        };
                    Out[] seqOuts = seq.getOuts();
                    String[] outs = new String[seqOuts.length];
                    for(int i = 0; i < seqOuts.length; i++)
                        {
                        outs[i] = "" + (i + 1) + ": " + seqOuts[i].toString();
                        }
                    JComboBox out = new JComboBox(outs);
                    out.setSelectedIndex(nchord.getMIDIOut());
                    out.addActionListener(new ActionListener()
                        {
                        public void actionPerformed(ActionEvent e)
                            {
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { nchord.setMIDIOut(out.getSelectedIndex()); }
                            finally { lock.unlock(); }
                            }
                        });
                    strs = new String[] { "Type", "Nickname", "Actions", "Start Node", "Out" , "Length", "Gate %", "Velocity", "Release", "Pitch 1", "Pitch 2", "Pitch 3", "Pitch 4"  };
                    comps = new JComponent[] 
                        {
                        new JLabel("Chord"),
                        name,
                        actions,
                        start,
                        out,
                        delay,
                        gate.getLabelledDial("0.0000"),
                        velocity.getLabelledDial("128"),
                        release.getLabelledDial("128"),
                        pitch1.getLabelledDial("G#"),
                        pitch2.getLabelledDial("G#"),
                        pitch3.getLabelledDial("G#"),
                        pitch4.getLabelledDial("G#"),
                        };
                    }
                else if (node instanceof Automaton.Random)
                    {
                    final Automaton.Random nrandom = (Automaton.Random)node;
                    for(int i = 0; i < aux.length; i++)
                        {
                        final int _i = i;
                        aux[i] = new SmallDial(nrandom.getAux(_i))
                            {
                            protected String map(double val) { return String.format("%.4f", val); }
                            public double getValue() 
                                { 
                                ReentrantLock lock = seq.getLock();
                                lock.lock();
                                try { return nrandom.getAux(_i); }
                                finally { lock.unlock(); }
                                }
                            public void setValue(double val) 
                                { 
                                if (seq == null) return;
                                ReentrantLock lock = seq.getLock();
                                lock.lock();
                                try { nrandom.setAux(_i, val); }
                                finally { lock.unlock(); }
                                }
                            };
                        }
                    strs = new String[4 + aux.length];
                    comps = new JComponent[4 + aux.length];
                    strs[0] = "Type";
                    strs[1] = "Nickname";
                    strs[2] = "Actions";
                    strs[3] = "Start Node";
                    comps[0] = new JLabel("Random");
                    comps[1] = name;
                    comps[2] = actions;
                    comps[3] = start;
                    for(int i = 0; i < aux.length; i++)
                        {
                        strs[i + 4] = "Weight " + (i + 1);
                        comps[i + 4] = aux[i].getLabelledDial("0.0000");
                        } 
                    }
                else if (node instanceof Automaton.Iterate)
                    {
                    final Automaton.Iterate niterate = (Automaton.Iterate)node;
                    for(int i = 0; i < aux.length; i++)
                        {
                        final int _i = i;
                        aux[i] = new SmallDial((niterate.getAux(_i) - 1) / (double)MAX_ITERATIONS)
                            {
                            protected String map(double val) { return "" + (1 + (int)(val * (MAX_ITERATIONS - 1))); }
                            public double getValue() 
                                { 
                                ReentrantLock lock = seq.getLock();
                                lock.lock();
                                try { return (niterate.getAux(_i) - 1) / (double)(MAX_ITERATIONS - 1); }
                                finally { lock.unlock(); }
                                }
                            public void setValue(double val) 
                                { 
                                if (seq == null) return;
                                ReentrantLock lock = seq.getLock();
                                lock.lock();
                                try { niterate.setAux(_i, 1 + (int)(val * (MAX_ITERATIONS - 1)));  }
                                finally { lock.unlock(); }
                                }
                            };
                        }

                    loop = new JCheckBox("");
                    loop.setSelected(niterate.getLoop());
                    loop.addActionListener(new ActionListener()
                        {
                        public void actionPerformed(ActionEvent e)
                            {
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try
                                {
                                niterate.setLoop(loop.isSelected());
                                }
                            finally
                                {
                                lock.unlock();
                                }
                            }
                        });

                    strs = new String[5 + aux.length];
                    comps = new JComponent[5 + aux.length];
                    strs[0] = "Type";
                    strs[1] = "Nickname";
                    strs[2] = "Actions";
                    strs[3] = "Loop";
                    strs[4] = "Start Node";
                    comps[0] = new JLabel("Iterate");
                    comps[1] = name;
                    comps[2] = actions;
                    comps[3] = loop;
                    comps[4] = start;
                    for(int i = 0; i < aux.length; i++)
                        {
                        strs[i + 5] = "Iterations " + (i + 1);
                        comps[i + 5] = aux[i].getLabelledDial("88");
                        } 
                    }
                else if (node instanceof Automaton.Join)
                    {
                    final Automaton.Join njoin = (Automaton.Join)node;
                    
                    // We want the joins to go from 2 to MAX_JOINS inclusive
                    join = new SmallDial((njoin.getJoinNumber() - 2) / (double)(MAX_JOINS - 2))
                        {
                        protected String map(double val) { return "" + (2 + (int)(val * (MAX_JOINS - 2))); }
                        public double getValue() 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { return (njoin.getAux(0) - 2) / (double)(MAX_JOINS - 2); }
                            finally { lock.unlock(); }
                            }
                        public void setValue(double val) 
                            { 
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { njoin.setAux(0, 2 + (int)(val * (MAX_JOINS - 2)));  }
                            finally { lock.unlock(); }
                            }
                        };
                    strs = new String[] { "Type", "Nickname", "Actions", "Start Node", "Threads" };
                    comps = new JComponent[] 
                        {
                        new JLabel("Join"),
                        name,
                        actions,
                        start,
                        join.getLabelledDial("8"),
                        };
                    }
                else if (node instanceof Automaton.MotifNode)
                    {
                    final Automaton.MotifNode nmotifnode = (Automaton.MotifNode)node;
                    repeats = new SmallDial(nmotifnode.getRepeats() / (double)MAX_REPEATS)
                        {
                        protected String map(double val) { return "" + (int)(val * MAX_REPEATS); }
                        public double getValue() 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { return nmotifnode.getRepeats() / (double)MAX_REPEATS; }
                            finally { lock.unlock(); }
                            }
                        public void setValue(double val) 
                            { 
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { nmotifnode.setRepeats((int)(val * MAX_REPEATS));  }
                            finally { lock.unlock(); }
                            }
                        };
                    untilTrigger = new JCheckBox("Until Trigger " + ((AutomatonClip.TRIGGER_PARAMETER) + 1));
                    untilTrigger.setSelected(nmotifnode.getRepeatUntilTrigger());
                    untilTrigger.addActionListener(new ActionListener()
                        {
                        public void actionPerformed(ActionEvent e)
                            {
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { nmotifnode.setRepeatUntilTrigger(untilTrigger.isSelected()); }
                            finally { lock.unlock(); }                              
                            }
                        });
                                
                    repeatPanel = new JPanel();
                    repeatPanel.setLayout(new BorderLayout());
                    repeatPanel.add(repeats.getLabelledDial("128"), BorderLayout.WEST);
                    repeatPanel.add(untilTrigger, BorderLayout.EAST);

                    repeatProbability = new SmallDial(nmotifnode.getRepeatProbability())
                        {
                        protected String map(double val) { return String.format("%.4f", val); }
                        public double getValue() 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { return nmotifnode.getRepeatProbability(); }
                            finally { lock.unlock(); }
                            }
                        public void setValue(double val) 
                            { 
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { nmotifnode.setRepeatProbability(val);  }
                            finally { lock.unlock(); }
                            }
                        };
                    quantization = new JComboBox(QUANTIZATIONS);
                    quantization.setSelectedIndex(nmotifnode.getQuantization());
                    quantization.addActionListener(new ActionListener()
                        {
                        public void actionPerformed(ActionEvent e)
                            {
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { nmotifnode.setQuantization(quantization.getSelectedIndex()); }
                            finally { lock.unlock(); }                              
                            }
                        });

                    strs = new String[] { "Type", "Nickname", "Actions", "Start Node", "Initial Repeats", "Repeat Probability", "Quantization" };
                    comps = new JComponent[] 
                        {
                        new JLabel(nmotifnode.getMotif().getName()),
                        name,
                        actions,
                        start,
                        repeatPanel,
                        //repeats.getLabelledDial("88"),
                        repeatProbability.getLabelledDial("0.0000"),
                        quantization
                        };
                    }
                else
                    {
                    strs = new String[] { "Type", "Nickname", "Actions", "Start Node" };
                    comps = new JComponent[] 
                        {
                        new JLabel((node instanceof Automaton.Fork ? "Fork" :
                                        (node instanceof Automaton.Join ? "Join" :
                                        node instanceof Automaton.Finished ? "Finished" : 
                                        "Unknown"))),
                        name,
                        actions,
                        start,
                        };
                    }
                }
            finally { lock.unlock(); }
            build(strs, comps);
            }
        }
                
    public int getNodeNum()
        {
        ReentrantLock lock = seq.getLock();
        lock.lock();
        // O(n) :-(
        // try { return automaton.getNodes().indexOf(node); }
        try { return node.getPosition(); }
        finally { lock.unlock(); }
        }


    public void revise()
        {
        if (node == null) return;              // it's blank
        Seq old = seq;
        seq = null;
        ReentrantLock lock = old.getLock();
        lock.lock();
        try 
            { 
            start.setSelected(automaton.getStart() == node);
            if (loop != null) loop.setSelected(((Automaton.Iterate)node).getLoop());
            if (quantization != null) quantization.setSelectedIndex(((Automaton.MotifNode)node).getQuantization()); 
            }
        finally { lock.unlock(); }                              
        seq = old;
        name.update();
        if (delay != null) delay.revise();
        if (repeats != null) repeats.redraw();
        if (repeatProbability != null) repeatProbability.redraw();
        for(int i = 0; i < aux.length; i++)
            {
            if (aux[i] != null) aux[i].redraw();
            }
        }
    }
