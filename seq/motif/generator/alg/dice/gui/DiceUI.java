/* 
   Copyright 2026 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.generator.alg.dice.gui;

import seq.motif.generator.*;
import seq.engine.*;
import seq.gui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.locks.*;
import seq.motif.generator.alg.dice.*;
import seq.motif.generator.gui.*;

public class DiceUI extends AlgorithmUI
    {
	SmallDial gate;
	SmallDial transpose;
	SmallDial velocity;
	JComboBox rate;

public static final String[] KEYS = { "C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B" };
	    
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
        
    public DiceUI(Seq seq, Generator generator, GeneratorUI generatorUI, Algorithm algorithm)
        {
        super(seq, generator, generatorUI, algorithm);
        final Dice dice = (Dice) algorithm;
                
        setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
		buildDefaults(generator);
                
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
			gate = new SmallDial(-1, defaults)
				{
				public double getValue() 
					{ 
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try { return dice.getGate(); }
					finally { lock.unlock(); }
					}
				public void setValue(double val) 
					{ 
					if (seq == null) return;
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try { dice.setGate(val); }
					finally { lock.unlock(); }
					}
				public void setDefault(int val) 
					{ 
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try { if (val != SmallDial.NO_DEFAULT) dice.setGate(-(val + 1)); }
					finally { lock.unlock(); }
					}
				public int getDefault()
					{
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try { double val = dice.getGate(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
					finally { lock.unlock(); }
					}
				};


            velocity = new SmallDial(-1, defaults)
                {
                protected String map(double val) 
                    { 
                    return "" + (int)(val * 127.0);  
                    }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return dice.getVelocity() / 127.0; }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { dice.setVelocity((int)(val * 127)); }
                    finally { lock.unlock(); }
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) dice.setVelocity(-(val + 1)); }
                    finally { lock.unlock(); }
                    }
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = dice.getVelocity(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };

            transpose = new SmallDial(-1, defaults)
                {
                protected String map(double val) 
                    { 
                    int trans = (int)(val * Dice.MAX_TRANSPOSE) - (Dice.MAX_TRANSPOSE / 2);
                    return String.valueOf(trans);
                    }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return dice.getTranspose() / (double)(Dice.MAX_TRANSPOSE); }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { dice.setTranspose((int)(val * (Dice.MAX_TRANSPOSE))); }
                    finally { lock.unlock(); }
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) dice.setTranspose(-(val + 1)); }
                    finally { lock.unlock(); }
                    }
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = dice.getTranspose(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };


            rate = new JComboBox(Dice.RATE_NAMES);
            rate.setSelectedIndex(dice.getRateIndex());
            rate.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try
                        {
                        dice.setRate(dice.RATES[rate.getSelectedIndex()]);
                        }
                    finally { lock.unlock(); }
                    }
                });     
            }
        finally { lock.unlock(); }

        gate.setToolTipText(GATE_TOOLTIP);
        rate.setToolTipText(RATE_TOOLTIP);
        transpose.setToolTipText(TRANSPOSE_TOOLTIP);
        velocity.setToolTipText(VELOCITY_TOOLTIP);

        build(new String[] { "Gate", "", "Transpose", "Volume", "Rate" },
            new JComponent[] 
                {
                gate.getLabelledDial("0.8888"),
                null,
                transpose.getLabelledDial("24"),
                velocity.getLabelledDial("127"),
                rate
                });
        }
                
    public void revise()
        {
        Dice dice = (Dice)algorithm;
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            {
            rate.setSelectedIndex(dice.getRateIndex()); 
            }
        finally { lock.unlock(); }                              
        if (gate != null) gate.redraw();
        if (velocity != null) velocity.redraw();
        if (transpose != null) transpose.redraw();        
        }

    static final String GATE_TOOLTIP = "<html><b>Gate</b><br>" +
        "Sets the note gate value (its length as a percentage of the total note).</html>";
        
    static final String RATE_TOOLTIP = "<html><b>Rate</b><br>" +
        "Sets the rate at which the algorithm produces notes.</html>";

    static final String TRANSPOSE_TOOLTIP = "<html><b>Transpose</b><br>" +
        "Transposes the notes by semitones.</html>";

    static final String VELOCITY_TOOLTIP = "<html><b>Volume</b><br>" +
        "Sets the volume (MIDI Velocity) of a note.</html>";
    }
