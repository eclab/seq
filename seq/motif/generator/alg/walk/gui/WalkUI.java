/* 
   Copyright 2026 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.generator.alg.walk.gui;

import seq.motif.generator.*;
import seq.engine.*;
import seq.gui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.locks.*;
import seq.motif.generator.alg.walk.*;
import seq.motif.generator.gui.*;

public class WalkUI extends AlgorithmUI
    {
    JComboBox scale;
    JComboBox pitchMethod;    
	SmallDial startPitch;
	SmallDial startWeight;
	SmallDial pitchWeight;
	SmallDial pitchIncrementProbability;
	SmallDial tether;
	
	SmallDial restProbability;
	
	JComboBox lengthMethod;
	SmallDial lengthWeight;
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
        
    public WalkUI(Seq seq, Generator generator, GeneratorUI generatorUI, Algorithm algorithm)
        {
        super(seq, generator, generatorUI, algorithm);
        final Walk walk = (Walk) algorithm;
                
        setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
		buildDefaults(generator);
                
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            scale = new JComboBox(Walk.SCALE_NAMES);
            scale.setMaximumRowCount(Walk.SCALE_NAMES.length);
            scale.setSelectedIndex(walk.getScale());
            scale.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try
                        {
                        walk.setScale(scale.getSelectedIndex());
                        }
                    finally { lock.unlock(); }
                    }
                });

            pitchMethod = new JComboBox(Walk.PITCH_METHOD_NAMES);
            pitchMethod.setSelectedIndex(walk.getPitchMethod());
            pitchMethod.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try
                        {
                        walk.setPitchMethod(pitchMethod.getSelectedIndex());
                        }
                    finally { lock.unlock(); }
                    }
                });

			startPitch = new SmallDial(-1, defaults)
				{
				protected String map(double val) 
					{
					int note = (int)(val * 127);
					return KEYS[note % 12] + (note / 12);
					}
				public double getValue() 
					{ 
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try { return walk.getStartPitch() / 127.0; }
					finally { lock.unlock(); }
					}
				public void setValue(double val) 
					{ 
					if (seq == null) return;
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try { walk.setStartPitch((int)(val * 127)); }
					finally { lock.unlock(); }
					}
				public void setDefault(int val) 
					{ 
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try { if (val != SmallDial.NO_DEFAULT) walk.setStartPitch(-(val + 1)); }
					finally { lock.unlock(); }
					}
				public int getDefault()
					{
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try { double val = walk.getStartPitch(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
					finally { lock.unlock(); }
					}
				};


			startWeight = new SmallDial(-1, defaults)
				{
				public double getValue() 
					{ 
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try { return walk.getStartWeight(); }
					finally { lock.unlock(); }
					}
				public void setValue(double val) 
					{ 
					if (seq == null) return;
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try { walk.setStartWeight(val); }
					finally { lock.unlock(); }
					}
				public void setDefault(int val) 
					{ 
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try { if (val != SmallDial.NO_DEFAULT) walk.setStartWeight(-(val + 1)); }
					finally { lock.unlock(); }
					}
				public int getDefault()
					{
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try { double val = walk.getStartWeight(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
					finally { lock.unlock(); }
					}
				};

			pitchWeight = new SmallDial(-1, defaults)
				{
				public double getValue() 
					{ 
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try { return walk.getPitchWeight(); }
					finally { lock.unlock(); }
					}
				public void setValue(double val) 
					{ 
					if (seq == null) return;
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try { walk.setPitchWeight(val); }
					finally { lock.unlock(); }
					}
				public void setDefault(int val) 
					{ 
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try { if (val != SmallDial.NO_DEFAULT) walk.setPitchWeight(-(val + 1)); }
					finally { lock.unlock(); }
					}
				public int getDefault()
					{
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try { double val = walk.getPitchWeight(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
					finally { lock.unlock(); }
					}
				};

			pitchIncrementProbability = new SmallDial(-1, defaults)
				{
				public double getValue() 
					{ 
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try { return walk.getPitchIncrementProbability(); }
					finally { lock.unlock(); }
					}
				public void setValue(double val) 
					{ 
					if (seq == null) return;
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try { walk.setPitchIncrementProbability(val); }
					finally { lock.unlock(); }
					}
				public void setDefault(int val) 
					{ 
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try { if (val != SmallDial.NO_DEFAULT) walk.setPitchIncrementProbability(-(val + 1)); }
					finally { lock.unlock(); }
					}
				public int getDefault()
					{
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try { double val = walk.getPitchIncrementProbability(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
					finally { lock.unlock(); }
					}
				};


			tether = new SmallDial(-1, defaults)
				{
				public double getValue() 
					{ 
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try { return walk.getTether(); }
					finally { lock.unlock(); }
					}
				public void setValue(double val) 
					{ 
					if (seq == null) return;
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try { walk.setTether(val); }
					finally { lock.unlock(); }
					}
				public void setDefault(int val) 
					{ 
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try { if (val != SmallDial.NO_DEFAULT) walk.setTether(-(val + 1)); }
					finally { lock.unlock(); }
					}
				public int getDefault()
					{
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try { double val = walk.getTether(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
					finally { lock.unlock(); }
					}
				};


			restProbability = new SmallDial(-1, defaults)
				{
				public double getValue() 
					{ 
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try { return walk.getRestProbability(); }
					finally { lock.unlock(); }
					}
				public void setValue(double val) 
					{ 
					if (seq == null) return;
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try { walk.setRestProbability(val); }
					finally { lock.unlock(); }
					}
				public void setDefault(int val) 
					{ 
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try { if (val != SmallDial.NO_DEFAULT) walk.setRestProbability(-(val + 1)); }
					finally { lock.unlock(); }
					}
				public int getDefault()
					{
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try { double val = walk.getRestProbability(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
					finally { lock.unlock(); }
					}
				};

            lengthMethod = new JComboBox(Walk.LENGTH_METHOD_NAMES);
            lengthMethod.setSelectedIndex(walk.getPitchMethod());
            lengthMethod.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try
                        {
                        walk.setLengthMethod(lengthMethod.getSelectedIndex());
                        }
                    finally { lock.unlock(); }
                    }
                });

			lengthWeight = new SmallDial(-1, defaults)
				{
				public double getValue() 
					{ 
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try { return walk.getLengthWeight(); }
					finally { lock.unlock(); }
					}
				public void setValue(double val) 
					{ 
					if (seq == null) return;
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try { walk.setLengthWeight(val); }
					finally { lock.unlock(); }
					}
				public void setDefault(int val) 
					{ 
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try { if (val != SmallDial.NO_DEFAULT) walk.setLengthWeight(-(val + 1)); }
					finally { lock.unlock(); }
					}
				public int getDefault()
					{
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try { double val = walk.getLengthWeight(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
					finally { lock.unlock(); }
					}
				};



			gate = new SmallDial(-1, defaults)
				{
				public double getValue() 
					{ 
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try { return walk.getGate(); }
					finally { lock.unlock(); }
					}
				public void setValue(double val) 
					{ 
					if (seq == null) return;
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try { walk.setGate(val); }
					finally { lock.unlock(); }
					}
				public void setDefault(int val) 
					{ 
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try { if (val != SmallDial.NO_DEFAULT) walk.setGate(-(val + 1)); }
					finally { lock.unlock(); }
					}
				public int getDefault()
					{
					ReentrantLock lock = seq.getLock();
					lock.lock();
					try { double val = walk.getGate(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
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
                    try { return walk.getVelocity() / 127.0; }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { walk.setVelocity((int)(val * 127)); }
                    finally { lock.unlock(); }
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) walk.setVelocity(-(val + 1)); }
                    finally { lock.unlock(); }
                    }
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = walk.getVelocity(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };

            transpose = new SmallDial(-1, defaults)
                {
                protected String map(double val) 
                    { 
                    int trans = (int)(val * Walk.MAX_TRANSPOSE) - (Walk.MAX_TRANSPOSE / 2);
                    return String.valueOf(trans);
                    }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return walk.getTranspose() / (double)(Walk.MAX_TRANSPOSE); }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { walk.setTranspose((int)(val * (Walk.MAX_TRANSPOSE))); }
                    finally { lock.unlock(); }
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { if (val != SmallDial.NO_DEFAULT) walk.setTranspose(-(val + 1)); }
                    finally { lock.unlock(); }
                    }
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { double val = walk.getTranspose(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                    finally { lock.unlock(); }
                    }
                };


            rate = new JComboBox(Walk.RATE_NAMES);
            rate.setSelectedIndex(walk.getRateIndex());
            rate.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try
                        {
                        walk.setRate(walk.RATES[rate.getSelectedIndex()]);
                        }
                    finally { lock.unlock(); }
                    }
                });     
            }
        finally { lock.unlock(); }

/*
        volume.setToolTipText(VOLUME_TOOLTIP);
        gate.setToolTipText(GATE_TOOLTIP);
        rate.setToolTipText(RATE_TOOLTIP);
        transpose.setToolTipText(TRANSPOSE_TOOLTIP);
        rest.setToolTipText(RESTS_TOOLTIP);
        legato.setToolTipText(LEGATO_TOOLTIP);
        preset.setToolTipText(PRESET_TOOLTIP);
        scale.setToolTipText(SCALE_TOOLTIP);
*/

        build(new String[] { "Pitch", "Scale", "Method", "Start Pitch", "Start Spread", "Weight", "Increment Probability", "Tether",
        					 "Length", "Method", "Weight", "Gate",
        					 "Rests", "Rest Probability",
        					 "General", "Transpose", "Volume", "Rate"
        					},
            new JComponent[] 
                {
                null,
                scale,
                pitchMethod,
                startPitch.getLabelledDial("Ab10"),
                startWeight.getLabelledDial("0.8888"),
                pitchWeight.getLabelledDial("0.8888"),
                pitchIncrementProbability.getLabelledDial("0.8888"),
                tether.getLabelledDial("0.8888"),
                null,
                lengthMethod,
                lengthWeight.getLabelledDial("0.8888"),
                gate.getLabelledDial("0.8888"),
                null,
                restProbability.getLabelledDial("0.8888"),
                null,
                transpose.getLabelledDial("24"),
                velocity.getLabelledDial("127"),
                rate
                });
        }
                
    public void revise()
        {
        Walk walk = (Walk)algorithm;
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            {
            rate.setSelectedIndex(walk.getRateIndex()); 
            scale.setSelectedIndex(walk.getScale()); 
            pitchMethod.setSelectedIndex(walk.getPitchMethod()); 
            lengthMethod.setSelectedIndex(walk.getLengthMethod()); 
            }
        finally { lock.unlock(); }                              
        if (tether != null) tether.redraw();
        if (startPitch != null) startPitch.redraw();
        if (startWeight != null) startWeight.redraw();
        if (pitchWeight != null) pitchWeight.redraw();
        if (pitchIncrementProbability != null) pitchIncrementProbability.redraw();
        if (restProbability != null) restProbability.redraw();
        if (lengthWeight != null) lengthWeight.redraw();
        if (velocity != null) velocity.redraw();
        if (transpose != null) transpose.redraw();        
        }

    static final String THEMES_TOOLTIP = "<html><b>Theme</b><br>" +
        "Sets one of the four theme slider settings.  These affect the LFSR B1 seed value.</html>";

    static final String INTERVALS_TOOLTIP = "<html><b>Interval</b><br>" +
        "Sets one of the four interval slider settings.  These affect the note pitches.</html>";
        
    static final String VOLUME_TOOLTIP = "<html><b>Velocity</b><br>" +
        "Sets the overall note velocity.</html>";
        
    static final String GATE_TOOLTIP = "<html><b>Gate</b><br>" +
        "Sets the note gate value (its length).  This is overridden if you have set <b>Legato</b>.</html>";
        
    static final String RATE_TOOLTIP = "<html><b>Rate</b><br>" +
        "Sets the rate at which the algorithm produces notes.</html>";

    static final String TRANSPOSE_TOOLTIP = "<html><b>Transpose</b><br>" +
        "Transposes the notes by semitones.</html>";

    static final String RESTS_TOOLTIP = "<html><b>Rests</b><br>" +
        "Turns on the ability to output rests as well as notes.</html>";

    static final String LEGATO_TOOLTIP = "<html><b>Legato</b><br>" +
        "Forces legato (long notes).  This is the default behavior for the Muse.</html>";

    static final String SCALE_TOOLTIP = "<html><b>Scale</b><br>" +
        "Sets the scale used to produce the melody.</html>";

    static final String PRESET_TOOLTIP = "<html><b>Preset...</b><br>" +
        "Song presets for the Muse.</html>";
    }
