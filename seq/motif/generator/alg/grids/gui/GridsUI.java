/* 
   Copyright 2026 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.generator.alg.grids.gui;

import seq.motif.generator.*;
import seq.engine.*;
import seq.gui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.locks.*;
import seq.motif.generator.alg.grids.*;
import seq.motif.generator.gui.*;

public class GridsUI extends AlgorithmUI
    {    
    SmallDial[] complexity = new SmallDial[3];
    SmallDial[] note = new SmallDial[3];
    SmallDial[] velocity = new SmallDial[3];
    SmallDial[] accentVelocity = new SmallDial[3];
    SmallDial x;
    SmallDial y;
    SmallDial chaos;
    JComboBox rate;
    JCheckBox accents;
    
	public static final String[] KEYS = { "C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B" };
    public static final String[] RATES = { "Whole Note", "Half Note", "Quarter Note", "Eighth Note", "Triplet", "Sixteenth Note", "Triplet Sixteenth Note", "Thirty-Second Note" };
    
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
        
    public GridsUI(Seq seq, Generator generator, GeneratorUI generatorUI, Algorithm algorithm)
        {
        super(seq, generator, generatorUI, algorithm);
		final Grids grids = (Grids) algorithm;
		
		setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
		
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            for(int i = 0; i < complexity.length; i++)
            	{
            	final int _i = i;
             	complexity[i] = new SmallDial(-1, defaults)
					{
					protected String map(double val) 
						{ 
						return String.valueOf((int)(val * 255)); 
						}
					public double getValue() 
						{ 
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { return grids.getComplexity(_i) / 255.0; }
						finally { lock.unlock(); }
						}
					public void setValue(double val) 
						{ 
						if (seq == null) return;
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { grids.setComplexity(_i, (int)(val * 255)); }
						finally { lock.unlock(); }
						}
					public void setDefault(int val) 
						{ 
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { if (val != SmallDial.NO_DEFAULT) grids.setComplexity(_i, -(val + 1)); }
						finally { lock.unlock(); }
						}
					public int getDefault()
						{
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { double val = grids.getComplexity(_i); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
						finally { lock.unlock(); }
						}
					};
				}

            for(int i = 0; i < note.length; i++)
            	{
            	final int _i = i;
             	note[i] = new SmallDial(-1, defaults)
					{
					protected String map(double val) 
						{ 
						int key = (int)(val * 127);
						return KEYS[key % 12] + (key / 12); 
						}
					public double getValue() 
						{ 
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { return grids.getNote(_i) / 127.0; }
						finally { lock.unlock(); }
						}
					public void setValue(double val) 
						{ 
						if (seq == null) return;
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { grids.setNote(_i, (int)(val * 127)); }
						finally { lock.unlock(); }
						}
					public void setDefault(int val) 
						{ 
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { if (val != SmallDial.NO_DEFAULT) grids.setNote(_i, -(val + 1)); }
						finally { lock.unlock(); }
						}
					public int getDefault()
						{
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { double val = grids.getNote(_i); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
						finally { lock.unlock(); }
						}
					};
				}
				
            for(int i = 0; i < velocity.length; i++)
            	{
            	final int _i = i;
             	velocity[i] = new SmallDial(-1, defaults)
					{
					protected String map(double val) 
						{ 
						return String.valueOf((int)(val * 127)); 
						}
					public double getValue() 
						{ 
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { return grids.getVelocity(_i) / 127.0; }
						finally { lock.unlock(); }
						}
					public void setValue(double val) 
						{ 
						if (seq == null) return;
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { grids.setVelocity(_i, (int)(val * 127)); }
						finally { lock.unlock(); }
						}
					public void setDefault(int val) 
						{ 
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { if (val != SmallDial.NO_DEFAULT) grids.setVelocity(_i, -(val + 1)); }
						finally { lock.unlock(); }
						}
					public int getDefault()
						{
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { double val = grids.getVelocity(_i); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
						finally { lock.unlock(); }
						}
					};
				}
				
            for(int i = 0; i < accentVelocity.length; i++)
            	{
            	final int _i = i;
             	accentVelocity[i] = new SmallDial(-1, defaults)
					{
					protected String map(double val) 
						{ 
						return String.valueOf((int)(val * 127)); 
						}
					public double getValue() 
						{ 
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { return grids.getAccentVelocity(_i) / 127.0; }
						finally { lock.unlock(); }
						}
					public void setValue(double val) 
						{ 
						if (seq == null) return;
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { grids.setAccentVelocity(_i, (int)(val * 127)); }
						finally { lock.unlock(); }
						}
					public void setDefault(int val) 
						{ 
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { if (val != SmallDial.NO_DEFAULT) grids.setAccentVelocity(_i, -(val + 1)); }
						finally { lock.unlock(); }
						}
					public int getDefault()
						{
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { double val = grids.getAccentVelocity(_i); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
						finally { lock.unlock(); }
						}
					};
				}

             	x = new SmallDial(-1, defaults)
					{
					protected String map(double val) 
						{ 
						return String.valueOf((int)(val * 255)); 
						}
					public double getValue() 
						{ 
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { return grids.getX() / 255.0; }
						finally { lock.unlock(); }
						}
					public void setValue(double val) 
						{ 
						if (seq == null) return;
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { grids.setX((int)(val * 255)); }
						finally { lock.unlock(); }
						}
					public void setDefault(int val) 
						{ 
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { if (val != SmallDial.NO_DEFAULT) grids.setX(-(val + 1)); }
						finally { lock.unlock(); }
						}
					public int getDefault()
						{
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { double val = grids.getX(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
						finally { lock.unlock(); }
						}
					};


             	y = new SmallDial(-1, defaults)
					{
					protected String map(double val) 
						{ 
						return String.valueOf((int)(val * 255)); 
						}
					public double getValue() 
						{ 
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { return grids.getY() / 255.0; }
						finally { lock.unlock(); }
						}
					public void setValue(double val) 
						{ 
						if (seq == null) return;
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { grids.setY((int)(val * 255)); }
						finally { lock.unlock(); }
						}
					public void setDefault(int val) 
						{ 
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { if (val != SmallDial.NO_DEFAULT) grids.setY(-(val + 1)); }
						finally { lock.unlock(); }
						}
					public int getDefault()
						{
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { double val = grids.getY(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
						finally { lock.unlock(); }
						}
					};

             	chaos = new SmallDial(-1, defaults)
					{
					protected String map(double val) 
						{ 
						return String.valueOf((int)(val * 255)); 
						}
					public double getValue() 
						{ 
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { return grids.getChaos() / 255.0; }
						finally { lock.unlock(); }
						}
					public void setValue(double val) 
						{ 
						if (seq == null) return;
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { grids.setChaos((int)(val * 255)); }
						finally { lock.unlock(); }
						}
					public void setDefault(int val) 
						{ 
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { if (val != SmallDial.NO_DEFAULT) grids.setChaos(-(val + 1)); }
						finally { lock.unlock(); }
						}
					public int getDefault()
						{
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { double val = grids.getChaos(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
						finally { lock.unlock(); }
						}
					};

            rate = new JComboBox(RATES);
            rate.setSelectedIndex(grids.getRateIndex());
            rate.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try
                    	{
                    	grids.setRate(grids.RATES[rate.getSelectedIndex()]);
						}
					finally { lock.unlock(); }
					}
				});

            accents = new JCheckBox();
            accents.setSelected(grids.getAccents());
            accents.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { grids.setAccents(accents.isSelected()); }
                    finally { lock.unlock(); }                              
                    }
                });
                
            }
        finally { lock.unlock(); }

        build(new String[] { "X", "Y", "Chaos", "Rate", "Accents", "Complexity", "Drum 1", "Drum 2", "Drum 3", "Note", "Drum 1", "Drum 2", "Drum 3", "Velocity", "Drum 1", "Drum 2", "Drum 3", "Accent Velocity", "Drum 1", "Drum 2", "Drum 3" }, 
            new JComponent[] 
                {
                x.getLabelledDial("127"),
            	y.getLabelledDial("127"),
                chaos.getLabelledDial("127"),
                rate,
                accents,
                null,
                complexity[0].getLabelledDial("255"),
                complexity[1].getLabelledDial("255"),
                complexity[2].getLabelledDial("255"),
                null,
                note[0].getLabelledDial("Bb10"),
                note[1].getLabelledDial("Bb10"),
                note[2].getLabelledDial("Bb10"),
                null,
                velocity[0].getLabelledDial("127"),
                velocity[1].getLabelledDial("127"),
                velocity[2].getLabelledDial("127"),
                null,
                accentVelocity[0].getLabelledDial("127"),
                accentVelocity[1].getLabelledDial("127"),
                accentVelocity[2].getLabelledDial("127"),
                });
        }
                
    public void revise()
        {
        Grids grids = (Grids)algorithm;
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            {
            rate.setSelectedIndex(grids.getRateIndex()); 
            accents.setSelected(grids.getAccents()); 
            }
        finally { lock.unlock(); }                              
        if (x != null) x.redraw();
        if (y != null) y.redraw();
        if (chaos != null) chaos.redraw();
        for(int i = 0; i < complexity.length; i++)
        	{
        	if (complexity[i] != null) complexity[i].redraw();
        	if (note[i] != null) note[i].redraw();
        	if (velocity[i] != null) velocity[i].redraw();
        	if (accentVelocity[i] != null) accentVelocity[i].redraw();
        	}
        }

    }
