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

    // JRadioButton start;
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
                name.setToolTipText(NICKNAME_TOOLTIP);
                                
                /*
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
                owner.redraw(false);
                }
                });
                */

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
                    delay.setToolTipText(DELAY_DELAY_TOOLTIP);
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
                    delay.setToolTipText(CHORD_LENGTH_TOOLTIP);
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
                    velocity.setToolTipText(CHORD_VELOCITY_TOOLTIP);
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
                    release.setToolTipText(CHORD_RELEASE_TOOLTIP);
                                                
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
                    gate.setToolTipText(CHORD_GATE_TOOLTIP);
                                                
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
                    pitch1.setToolTipText(CHORD_PITCH_1_TOOLTIP);
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
                    pitch2.setToolTipText(CHORD_PITCH_2_TOOLTIP);
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
                    pitch3.setToolTipText(CHORD_PITCH_3_TOOLTIP);
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
                    pitch4.setToolTipText(CHORD_PITCH_4_TOOLTIP);
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
                    out.setToolTipText(CHORD_OUT_TOOLTIP);
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
                    aux[0].setToolTipText(RANDOM_WEIGHT_1_TOOLTIP);
                    aux[1].setToolTipText(RANDOM_WEIGHT_2_TOOLTIP);
                    aux[2].setToolTipText(RANDOM_WEIGHT_3_TOOLTIP);
                    aux[3].setToolTipText(RANDOM_WEIGHT_4_TOOLTIP);
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
                    aux[0].setToolTipText(ITERATE_ITERATIONS_1_TOOLTIP);
                    aux[1].setToolTipText(ITERATE_ITERATIONS_2_TOOLTIP);
                    aux[2].setToolTipText(ITERATE_ITERATIONS_3_TOOLTIP);
                    aux[3].setToolTipText(ITERATE_ITERATIONS_4_TOOLTIP);

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
                    loop.setToolTipText(ITERATE_LOOP_TOOLTIP);

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
                    join.setToolTipText(JOIN_THREADS_TOOLTIP);
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
                    repeats.setToolTipText(MOTIF_INITIAL_REPEATS_TOOLTIP);
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
                    untilTrigger.setToolTipText(MOTIF_UNTIL_TRIGGER_8_TOOLTIP);
                               
                    repeatPanel = new JPanel();
                    repeatPanel.setLayout(new BorderLayout());
                    repeatPanel.add(repeats.getLabelledDial("128"), BorderLayout.CENTER);
                    repeatPanel.add(untilTrigger, BorderLayout.EAST);
                    repeatPanel.setToolTipText(MOTIF_INITIAL_REPEATS_TOOLTIP);

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
                    repeatProbability.setToolTipText(MOTIF_REPEAT_PROBABILITY_TOOLTIP);
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
                    quantization.setToolTipText(MOTIF_QUANTIZATION_TOOLTIP);

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
                    rate.setToolTipText(MIDI_CHANGES_RATE_TOOLTIP);
                
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
                    ratePresets.setToolTipText(MIDI_CHANGES_RATE_PRESETS_TOOLTIP);

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
                    transpose.setToolTipText(MIDI_CHANGES_TRANSPOSE_TOOLTIP);

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
                    gain.setToolTipText(MIDI_CHANGES_GAIN_TOOLTIP);

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
                    out.setToolTipText(MIDI_CHANGES_OUT_TOOLTIP);

                    JPanel ratePanel = new JPanel();
                    ratePanel.setLayout(new BorderLayout());
                    ratePanel.add(rate.getLabelledDial("0.0000"), BorderLayout.CENTER);   // so it stretches
                    ratePanel.add(ratePresets, BorderLayout.EAST); 
                    ratePanel.setToolTipText(MIDI_CHANGES_RATE_TOOLTIP);

                
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
            // start.setSelected(automaton.getStart() == node);
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




    static final String NICKNAME_TOOLTIP = "<html><b>Nickname</b><br>" +
        "Sets the nickname of the node, which will appear as a label beneath its icon.</html>";

    static final String MOTIF_INITIAL_REPEATS_TOOLTIP = "<html><b>Initial Repeats</b><br>" +
        "Sets how often the motif node will repeat before transitioning to the next node.<br><br>" + 
        "Repeating works as follows:" +
        "<ol>" +
        "<li>The motif node is played once." +
        "<li>Then the motif node is played <b>Initial Repeats</b> more times." +
        "<li>Then a coin is flipped repeatedly with <b>Repeat Probability</b> of being heads.<br>" +
        "If it comes up heads <i>N</i> times before the first tails, then the motif node is<br>" +
        "played <i>N</i> more times." + 
        "<li>Then the motif node transitions to the next motif." +
        "</ol>" +
        "All the while, if a 1.0 is sent to <b>Parameter 8</b> and <b>Until Trigger 8</b> is<br>" +
        "selected, then the motif node transitions to the next motif with no further repeats.</html>";
        
    static final String MOTIF_REPEAT_PROBABILITY_TOOLTIP = "<html><b>Repeat Probability</b><br>" +
        "Sets how often the motif node will repeat before transitioning to the next node.<br><br>" + 
        "Repeating works as follows:" +
        "<ol>" +
        "<li>The motif node is played once." +
        "<li>Then the motif node is played <b>Initial Repeats</b> more times." +
        "<li>Then a coin is flipped repeatedly with <b>Repeat Probability</b> of being heads.<br>" +
        "If it comes up heads <i>N</i> times before the first tails, then the motif node is<br>" +
        "played <i>N</i> more times." + 
        "<li>Then the motif node transitions to the next motif." +
        "</ol>" +
        "All the while, if a 1.0 is sent to <b>Parameter 8</b> and <b>Until Trigger 8</b> is<br>" +
        "selected, then the motif node transitions to the next motif with no further repeats.</html>";

    static final String MOTIF_UNTIL_TRIGGER_8_TOOLTIP = "<html><b>Until Trigger 8</b><br>" +
        "If Checked, when a 1.0 is sent to <b>Parameter 8</b>, the motif node will transition to<br>" + 
        "the next node regardless of the number of <b>initial repeats</b> or the <b>repeat probability</b>." + 
        "Repeating works as follows:" +
        "<ol>" +
        "<li>The motif node is played once." +
        "<li>Then the motif node is played <b>Initial Repeats</b> more times." +
        "<li>Then a coin is flipped repeatedly with <b>Repeat Probability</b> of being heads.<br>" +
        "If it comes up heads <i>N</i> times before the first tails, then the motif node is<br>" +
        "played <i>N</i> more times." + 
        "<li>Then the motif node transitions to the next motif." +
        "</ol>" +
        "All the while, if a 1.0 is sent to <b>Parameter 8</b> and <b>Until Trigger 8</b> is<br>" +
        "selected, then the motif node transitions to the next motif with no further repeats.</html>";
        
    static final String MOTIF_QUANTIZATION_TOOLTIP = "<html><b>Quantization</b><br>" +
        "Delays the transition to the next motif node until it falls on a given time event:" + 
        "<ul>" +
        "<li>None (transition is not delayed)" +
        "<li>The next 16th note" +
        "<li>The next quarter note" + 
        "<li>The start of the next measure (bar)</html>";
        
    static final String MIDI_CHANGES_RATE_TOOLTIP = "<html><b>MIDI Changes: Rate</b><br>" +
        "Changes the rate of the motif node's playing (speeding it up or slowing it down)</html>";

    static final String MIDI_CHANGES_RATE_PRESETS_TOOLTIP = "<html><b>MIDI Changes: Rate Presets</b><br>" +
        "Presets for changing the rate of the motif node's playing (speeding it up or slowing it down).<br><br>" +
        "The first preset is <b>Custom...</b>, which allows you to change the playing so that<br>" +
        "Some number of quarter notes would be adjusted to squish or stretch them into a different number<br>" +
        "of quarter notes.  For example you can speed up the playing so that 8 quarter notes would be played<br>" +
        "in the time normally alloted for 4 quarter notes (that is, doubling the speed).</html>";
        
    static final String MIDI_CHANGES_TRANSPOSE_TOOLTIP = "<html><b>MIDI Changes: Transpose</b><br>" +
        "Transposes the notes generated by the motif node's playing.</html>";

    static final String MIDI_CHANGES_GAIN_TOOLTIP = "<html><b>MIDI Changes: Gain</b><br>" +
        "Changes the gain (volume) of the notes generated by the motif node's playing.</html>";

    static final String MIDI_CHANGES_OUT_TOOLTIP = "<html><b>MIDI Changes: Out</b><br>" +
        "Changes the designated output of the MIDI generated by the motif node's playing.</html>";

    static final String DELAY_DELAY_TOOLTIP = "<html><b>Delay</b><br>" +
        "Sets the amount of time to delay (do nothing) until transitioning to the next motif node.</html>";

    static final String CHORD_OUT_TOOLTIP = "<html><b>Output</b><br>" +
        "Sets the MIDI output to receive the notes or chords.</html>";

    static final String CHORD_LENGTH_TOOLTIP = "<html><b>Length</b><br>" +
        "Sets the length of time of the MIDI note or chord.<br><br>" +
        "Note that the chord may only play for a percentage of this time (see <b>Gate %</b> below).</html>";

    static final String CHORD_GATE_TOOLTIP = "<html><b>Gate %</b><br>" +
        "Sets the gate period of the MIDI note or chord.  This is the amount of the <b>Length</b><br>"+
        "that is actually spent playing the MIDI note or chord: the rest of the time is silence.</html>";

    static final String CHORD_VELOCITY_TOOLTIP = "<html><b>Velocity%</b><br>" +
        "Sets the velocity (volume) of the MIDI note or chord.</html>";

    static final String CHORD_RELEASE_TOOLTIP = "<html><b>Release%</b><br>" +
        "Sets the release velocity of the MIDI note or chord.  This is how fast the musician notionally<br>" +
        "released the keys of the MIDI note or chord.</html>";

    static final String CHORD_PITCH_1_TOOLTIP = "<html><b>Pitch 1</b><br>" +
        "Sets the pitch of the first note in the chord.  The highest pitch value is \"no note\" (--).</html>";

    static final String CHORD_PITCH_2_TOOLTIP = "<html><b>Pitch 2</b><br>" +
        "Sets the pitch of the second note in the chord.  The highest pitch value is \"no note\" (--).</html>";

    static final String CHORD_PITCH_3_TOOLTIP = "<html><b>Pitch 3</b><br>" +
        "Sets the pitch of the third note in the chord.  The highest pitch value is \"no note\" (--).</html>";

    static final String CHORD_PITCH_4_TOOLTIP = "<html><b>Pitch 4</b><br>" +
        "Sets the pitch of the fourth note in the chord.  The highest pitch value is \"no note\" (--).</html>";

    static final String ITERATE_LOOP_TOOLTIP = "<html><b>Loop</b><br>" +
        "Sets whether, when the final transition has occurred, the Iterate should loop back to continue,<br>" +
        "or else should imply not transition any more.<br><br>" +
        "Iterations works as follows:" + 
        "<ol>" +
        "<li>When first played, we immediately transition to the first output that is connected to us." +
        "<li>Each time we are played, we continue transiting out that input until we have done so<br>" +
        "<b>Iterations</b> total times. Thereafter we continue to the next output that is connected to us.<br>" +
        "<li>When we have finished with all our connected outputs, if <b>Loop</b> is checked, then we<br>" +
        "loop back and continue again with our first connectd output. Else we stop transitioning entirely.</html>";

    static final String ITERATE_ITERATIONS_1_TOOLTIP = "<html><b>Iterations 1</b><br>" +
        "Sets how many transitions occur for output 1 before continuing on to output 2 and beyond.<br>" +
        "If there is nothing attached to output 1, it is ignored and we continue immediately.<br><br>" +
        "Iterations works as follows:" + 
        "<ol>" +
        "<li>When first played, we immediately transition to the first output that is connected to us." +
        "<li>Each time we are played, we continue transiting out that input until we have done so<br>" +
        "<b>Iterations</b> total times. Thereafter we continue to the next output that is connected to us.<br>" +
        "<li>When we have finished with all our connected outputs, if <b>Loop</b> is checked, then we<br>" +
        "loop back and continue again with our first connectd output. Else we stop transitioning entirely.</html>";

    static final String ITERATE_ITERATIONS_2_TOOLTIP = "<html><b>Iterations 2</b><br>" +
        "Sets how many transitions occur for output 2 before continuing on the next output.<br>" +
        "If there is nothing attached to output 2, it is ignored and we continue immediately.<br><br>" +
        "Iterations works as follows:" + 
        "<ol>" +
        "<li>When first played, we immediately transition to the first output that is connected to us." +
        "<li>Each time we are played, we continue transiting out that input until we have done so<br>" +
        "<b>Iterations</b> total times. Thereafter we continue to the next output that is connected to us.<br>" +
        "<li>When we have finished with all our connected outputs, if <b>Loop</b> is checked, then we<br>" +
        "loop back and continue again with our first connectd output. Else we stop transitioning entirely.</html>";

    static final String ITERATE_ITERATIONS_3_TOOLTIP = "<html><b>Iterations 3</b><br>" +
        "Sets how many transitions occur for output 3 before continuing on the next output.<br>" +
        "If there is nothing attached to output 3, it is ignored and we continue immediately.<br><br>" +
        "Iterations works as follows:" + 
        "<ol>" +
        "<li>When first played, we immediately transition to the first output that is connected to us." +
        "<li>Each time we are played, we continue transiting out that input until we have done so<br>" +
        "<b>Iterations</b> total times. Thereafter we continue to the next output that is connected to us.<br>" +
        "<li>When we have finished with all our connected outputs, if <b>Loop</b> is checked, then we<br>" +
        "loop back and continue again with our first connectd output. Else we stop transitioning entirely.</html>";

    static final String ITERATE_ITERATIONS_4_TOOLTIP = "<html><b>Iterations 4</b><br>" +
        "Sets how many transitions occur for output 4 before stopping or looping and continuing on the next output.<br>" +
        "If there is nothing attached to output 4, it is ignored and we continue (or stop) immediately.<br><br>" +
        "Iterations works as follows:" + 
        "<ol>" +
        "<li>When first played, we immediately transition to the first output that is connected to us." +
        "<li>Each time we are played, we continue transiting out that input until we have done so<br>" +
        "<b>Iterations</b> total times. Thereafter we continue to the next output that is connected to us.<br>" +
        "<li>When we have finished with all our connected outputs, if <b>Loop</b> is checked, then we<br>" +
        "loop back and continue again with our first connectd output. Else we stop transitioning entirely.</html>";

    static final String RANDOM_WEIGHT_1_TOOLTIP = "<html><b>Weight 1</b><br>" +
        "Sets the weight for output 1.  If output 1 is not connected, this is ignored.<br><br>" + 
        "Random works as follows.  When played, we take all the connected outputs and normalize<br>" + 
        "their weights into probabilities (divide them by their sum).  If they're all 0, they're<br>" +
        "all treated as equal probability.  Then we select an output at random according to the probabilities<br>" +
        "and transition to that output.  If no outputs are connected, we don't transition at all.</html>";

    static final String RANDOM_WEIGHT_2_TOOLTIP = "<html><b>Weight 2</b><br>" +
        "Sets the weight for output 2.  If output 2 is not connected, this is ignored.<br><br>" + 
        "Random works as follows.  When played, we take all the connected outputs and normalize<br>" + 
        "their weights into probabilities (divide them by their sum).  If they're all 0, they're<br>" +
        "all treated as equal probability.  Then we select an output at random according to the probabilities<br>" +
        "and transition to that output.  If no outputs are connected, we don't transition at all.</html>";

    static final String RANDOM_WEIGHT_3_TOOLTIP = "<html><b>Weight 3</b><br>" +
        "Sets the weight for output 3.  If output 3 is not connected, this is ignored.<br><br>" + 
        "Random works as follows.  When played, we take all the connected outputs and normalize<br>" + 
        "their weights into probabilities (divide them by their sum).  If they're all 0, they're<br>" +
        "all treated as equal probability.  Then we select an output at random according to the probabilities<br>" +
        "and transition to that output.  If no outputs are connected, we don't transition at all.</html>";

    static final String RANDOM_WEIGHT_4_TOOLTIP = "<html><b>Weight 4</b><br>" +
        "Sets the weight for output 4.  If output 4 is not connected, this is ignored.<br><br>" + 
        "Random works as follows.  When played, we take all the connected outputs and normalize<br>" + 
        "their weights into probabilities (divide them by their sum).  If they're all 0, they're<br>" +
        "all treated as equal probability.  Then we select an output at random according to the probabilities<br>" +
        "and transition to that output.  If no outputs are connected, we don't transition at all.</html>";

    static final String JOIN_THREADS_TOOLTIP = "<html><b>Join</b><br>" +
        "Sets the number of playing threads that must transition to the Join before it transitions.</html>";
    }
