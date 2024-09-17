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
    public static final int MAX_JOINS = 8;                      // this should be >= 2 and <= Automaton.MAX_THREADS
    
    public static final double MAX_RATE_LOG = Math.log(Automaton.MotifNode.MAX_RATE);
    public static final String[] RATE_OPTIONS = ParallelChildInspector.RATE_OPTIONS; 
    public static final double[] RATES = ParallelChildInspector.RATES;

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
    SmallDial transpose;
    SmallDial gain;
    SmallDial rate;
    PushButton ratePresets;
    JComboBox out;
                
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

    public AutomatonNodeInspector(Seq seq, final Automaton automaton, final MotifButton button, AutomatonUI owner, Automaton.Node node)
        {
        this.seq = seq;
        this.automaton = automaton;
        this.motifui = button.getMotifUI();
        this.owner = owner;
        this.node = node;
                
        String[] strs = null;
        JComponent[] comps = null;

        buildDefaults(owner.getMotif());

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

/*
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
*/

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
                    strs = new String[] { "Type", "Nickname", "Delay" };
                    comps = new JComponent[] 
                        {
                        new JLabel("Delay"),
                        name,
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
                    out = new JComboBox(outs);
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
                    strs = new String[] { "Type", "Nickname", "Out" , "Length", "Gate %", "Velocity", "Release", "Pitch 1", "Pitch 2", "Pitch 3", "Pitch 4"  };
                    comps = new JComponent[] 
                        {
                        new JLabel("Chord"),
                        name,
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
                    strs = new String[2 + aux.length];
                    comps = new JComponent[2 + aux.length];
                    strs[0] = "Type";
                    strs[1] = "Nickname";
                    comps[0] = new JLabel("Random");
                    comps[1] = name;
                    for(int i = 0; i < aux.length; i++)
                        {
                        strs[i + 2] = "Weight " + (i + 1);
                        comps[i + 2] = aux[i].getLabelledDial("0.0000");
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

                    strs = new String[3 + aux.length];
                    comps = new JComponent[3 + aux.length];
                    strs[0] = "Type";
                    strs[1] = "Nickname";
                    strs[2] = "Loop";
                    comps[0] = new JLabel("Iterate");
                    comps[1] = name;
                    comps[2] = loop;
                    for(int i = 0; i < aux.length; i++)
                        {
                        strs[i + 3] = "Iterations " + (i + 1);
                        comps[i + 3] = aux[i].getLabelledDial("88");
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
                    strs = new String[] { "Type", "Nickname", "Threads" };
                    comps = new JComponent[] 
                        {
                        new JLabel("Join"),
                        name,
                        join.getLabelledDial("8"),
                        };
                    }
                else if (node instanceof Automaton.MotifNode)
                    {
                    final Automaton.MotifNode motifnode = (Automaton.MotifNode)node;
                    
                    repeats = new SmallDial(motifnode.getRepeats() / (double)Automaton.MotifNode.MAX_REPEATS, defaults)
                        {
                        protected String map(double val) { return "" + (int)(val * Automaton.MotifNode.MAX_REPEATS); }
                        public double getValue() 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { return motifnode.getRepeats() / (double)Automaton.MotifNode.MAX_REPEATS; }
                            finally { lock.unlock(); }
                            }
                        public void setValue(double val) 
                            { 
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { motifnode.setRepeats((int)(val * Automaton.MotifNode.MAX_REPEATS));  }
                            finally { lock.unlock(); }
                            }
                        public void setDefault(int val) 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { if (val != SmallDial.NO_DEFAULT) motifnode.setRepeats(-(val + 1)); }
                            finally { lock.unlock(); }
                            owner.updateText();                  // FIXME: is this needed?
                            }
                        public int getDefault()
                            {
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { double val = motifnode.getRepeats(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                            finally { lock.unlock(); }
                            }
                        };
                    untilTrigger = new JCheckBox("Until Trigger " + ((AutomatonClip.TRIGGER_PARAMETER) + 1));
                    untilTrigger.setSelected(motifnode.getRepeatUntilTrigger());
                    untilTrigger.addActionListener(new ActionListener()
                        {
                        public void actionPerformed(ActionEvent e)
                            {
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { motifnode.setRepeatUntilTrigger(untilTrigger.isSelected()); }
                            finally { lock.unlock(); }                              
                            }
                        });
                                
                    repeatPanel = new JPanel();
                    repeatPanel.setLayout(new BorderLayout());
                    repeatPanel.add(repeats.getLabelledDial("128"), BorderLayout.CENTER);
                    repeatPanel.add(untilTrigger, BorderLayout.EAST);

                    repeatProbability = new SmallDial(motifnode.getRepeatProbability(), defaults)
                        {
                        protected String map(double val) { return String.format("%.4f", val); }
                        public double getValue() 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { return motifnode.getRepeatProbability(); }
                            finally { lock.unlock(); }
                            }
                        public void setValue(double val) 
                            { 
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { motifnode.setRepeatProbability(val);  }
                            finally { lock.unlock(); }
                            }
                        public void setDefault(int val) 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { if (val != SmallDial.NO_DEFAULT) motifnode.setRepeatProbability(-(val + 1)); }
                            finally { lock.unlock(); }
                            owner.updateText();                  // FIXME: is this needed?
                            }
                        public int getDefault()
                            {
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { double val = motifnode.getRepeatProbability(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                            finally { lock.unlock(); }
                            }
                        };
                    quantization = new JComboBox(QUANTIZATIONS);
                    quantization.setSelectedIndex(motifnode.getQuantization());
                    quantization.addActionListener(new ActionListener()
                        {
                        public void actionPerformed(ActionEvent e)
                            {
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { motifnode.setQuantization(quantization.getSelectedIndex()); }
                            finally { lock.unlock(); }                              
                            }
                        });

                    // This requires some explanation.  We are mapping the values we receive (0...1) in the Dial
                    // into values 0.0625...16 using an exponential mapping.  However we also store negative values for
                    // defaults.  This causes problems when we're queried for our value, but we're currently negative so
                    // we compute the log of a negative value.  So instead here, in the initialization and in getValue(),
                    // we return DEFAULT_RATE instead.   This issue doesn't come up when just doing 0...1 as normal.
                    double d = motifnode.getRate(); 
                    if (d < 0) d = Automaton.MotifNode.DEFAULT_RATE;
                    rate = new SmallDial((Math.log(d) + MAX_RATE_LOG) / MAX_RATE_LOG / 2.0, defaults)
                        {
                        protected String map(double val) 
                            {
                            double d = Math.exp(val * 2 * MAX_RATE_LOG - MAX_RATE_LOG);
                            return super.map(d);
                            }
                        public double getValue() 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { double d = motifnode.getRate(); if (d < 0) return Automaton.MotifNode.DEFAULT_RATE; else return (Math.log(d) + MAX_RATE_LOG) / MAX_RATE_LOG / 2.0;}
                            finally { lock.unlock(); }
                            }
                        public void setValue(double val) 
                            { 
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { motifnode.setRate(Math.exp(val * 2 * MAX_RATE_LOG - MAX_RATE_LOG));}
                            finally { lock.unlock(); }
                            }
                        public void setDefault(int val) 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { if (val != SmallDial.NO_DEFAULT) motifnode.setRate(-(val + 1)); }
                            finally { lock.unlock(); }
                            owner.updateText();                  // FIXME: is this needed?
                            }
    
                        public int getDefault()
                            {
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { double val = motifnode.getRate(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                            finally { lock.unlock(); }
                            }
                        };
                
                    ratePresets = new PushButton("Presets...", RATE_OPTIONS)
                        {
                        public void perform(int val)
                            {
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { motifnode.setRate(ParallelChildInspector.getRate(AutomatonNodeInspector.this, val));}
                            finally { lock.unlock(); }
                            rate.redraw();
                            }
                        };

                    transpose = new SmallDial(motifnode.getTranspose() / (double)Automaton.MotifNode.MAX_TRANSPOSE / 2.0, defaults)
                        {
                        protected String map(double val) { return String.valueOf((int)(val * 2 * Automaton.MotifNode.MAX_TRANSPOSE) - Automaton.MotifNode.MAX_TRANSPOSE); }
                        public double getValue() 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { double d = motifnode.getTranspose();  if (d < 0) return 0;  else return d / (double)Automaton.MotifNode.MAX_TRANSPOSE / 2.0; }
                            finally { lock.unlock(); }
                            }
                        public void setValue(double val) 
                            { 
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { motifnode.setTranspose((int)(val * 2 * Automaton.MotifNode.MAX_TRANSPOSE)); }
                            finally { lock.unlock(); }
                            }
                        public void setDefault(int val) 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { if (val != SmallDial.NO_DEFAULT) motifnode.setTranspose(-(val + 1)); }
                            finally { lock.unlock(); }
                            owner.updateText();                  // FIXME: is this needed?
                            }
    
                        public int getDefault()
                            {
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { double val = motifnode.getTranspose(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                            finally { lock.unlock(); }
                            }
                        };

                    gain = new SmallDial(motifnode.getGain() / Automaton.MotifNode.MAX_GAIN, defaults)
                        {
                        protected String map(double val) { return super.map(val * Automaton.MotifNode.MAX_GAIN); }
                        public double getValue() 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { return motifnode.getGain() / Automaton.MotifNode.MAX_GAIN; }
                            finally { lock.unlock(); }
                            }
                        public void setValue(double val) 
                            { 
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { motifnode.setGain(val * Automaton.MotifNode.MAX_GAIN); }
                            finally { lock.unlock(); }
                            }
                        public void setDefault(int val) 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { if (val != SmallDial.NO_DEFAULT) motifnode.setGain(-(val + 1)); }
                            finally { lock.unlock(); }
                            owner.updateText();                  // FIXME: is this needed?
                            }
                        public int getDefault()
                            {
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { double val = motifnode.getGain(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                            finally { lock.unlock(); }
                            }
                        };

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
                    out.setSelectedIndex(motifnode.getOutMIDI() + 1);
                    out.addActionListener(new ActionListener()
                        {
                        public void actionPerformed(ActionEvent e)
                            {
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { motifnode.setOutMIDI(out.getSelectedIndex() - 1); }              // -1 is DISABLED
                            finally { lock.unlock(); }                              
                            }
                        });            

                    JPanel ratePanel = new JPanel();
                    ratePanel.setLayout(new BorderLayout());
                    ratePanel.add(rate.getLabelledDial("0.0000"), BorderLayout.CENTER);   // so it stretches
                    ratePanel.add(ratePresets, BorderLayout.EAST); 

                
                    strs = new String[] { "Type", "Nickname", "Initial Repeats", "Repeat Probability", "Quantization", "MIDI Changes", "Rate", "Transpose",  "Gain", "Out" };
                    comps = new JComponent[] 
                        {
                        new JLabel(motifnode.getMotif().getName()),
                        name,
                        repeatPanel,
                        //repeats.getLabelledDial("88"),
                        repeatProbability.getLabelledDial("0.0000"),
                        quantization,
                        null,                   // Separator
                        ratePanel,
                        transpose.getLabelledDial("-24"),
                        gain.getLabelledDial("0.0000"),
                        out,
                        };
                    }
                else
                    {
                    strs = new String[] { "Type", "Nickname" };
                    comps = new JComponent[] 
                        {
                        new JLabel((node instanceof Automaton.Fork ? "Fork" :
                                        (node instanceof Automaton.Join ? "Join" :
                                        node instanceof Automaton.Finished ? "Finished" : 
                                        "Unknown"))),
                        name,
                        };
                    }
                }
            finally { lock.unlock(); }
            JPanel result = build(strs, comps);

            if (node instanceof Automaton.MotifNode)
                {
                remove(result);
                add(result, BorderLayout.CENTER);
                add(new ArgumentList(seq, ((Automaton.MotifNode)node).getChild(), owner.getMotif()), BorderLayout.NORTH);
                }
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
