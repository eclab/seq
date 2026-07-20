/* 
   Copyright 2026 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.generator.alg.triadexmuse.gui;

import seq.motif.generator.*;
import seq.engine.*;
import seq.gui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.locks.*;
import seq.motif.generator.alg.triadexmuse.*;
import seq.motif.generator.gui.*;

public class TriadexMuseUI extends AlgorithmUI
    {    
    SmallDial[] themes = new SmallDial[4];
    SmallDial[] intervals = new SmallDial[4];
    SmallDial volume;
    SmallDial gate;
    JComboBox rate;
    SmallDial transpose;
    JCheckBox rest;
    JCheckBox legato;
    PushButton preset;
    
    public static final String[] LABELS = 
    	{ 
    	"Off", "On", 
    	"C 1/2", "C1", "C2", "C4", "C8", "C3", "C6", 
    	"B1", "B2", "B3", "B4", "B5", "B6", "B7", "B8",
    	"B9", "B10", "B11", "B12", "B13", "B14", "B15", "B16",
    	"B17", "B18", "B19", "B20", "B21", "B22", "B23", "B24",
    	"B25", "B26", "B27", "B28", "B29", "B30", "B31"
    	};
    	
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
        
    public TriadexMuseUI(Seq seq, Generator generator, GeneratorUI generatorUI, Algorithm algorithm)
        {
        super(seq, generator, generatorUI, algorithm);
		final TriadexMuse triadexmuse = (TriadexMuse) algorithm;
		
		setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
		
		
		preset = new PushButton("Preset...", TriadexMuse.PRESET_NAMES)
			{
			public void perform(int i)
				{
				ReentrantLock lock = seq.getLock();
				lock.lock();
				try 
					{
					triadexmuse.setRest(TriadexMuse.PRESET_RESTS[i]);
					for(int j = 0; j < 4; j++)
						{
						triadexmuse.setInterval(j, TriadexMuse.PRESET_INTERVALS[i][j]);
						triadexmuse.setTheme(j, TriadexMuse.PRESET_THEMES[i][j]);
						}
					}
				finally { lock.unlock(); }
				revise();
				}
			};
		
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            for(int i = 0; i < themes.length; i++)
            	{
            	final int _i = i;
             	themes[i] = new SmallDial(-1, defaults)
					{
					protected String map(double val) 
						{ 
						return LABELS[SmallDial.toInt(val * 39)]; 
						}
					public double getValue() 
						{ 
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { return triadexmuse.getTheme(_i) / 39.0; }
						finally { lock.unlock(); }
						}
					public void setValue(double val) 
						{ 
						if (seq == null) return;
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { triadexmuse.setTheme(_i, (int)(val * 39)); }
						finally { lock.unlock(); }
						}
					public void setDefault(int val) 
						{ 
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { if (val != SmallDial.NO_DEFAULT) triadexmuse.setTheme(_i, -(val + 1)); }
						finally { lock.unlock(); }
						}
					public int getDefault()
						{
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { double val = triadexmuse.getTheme(_i); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
						finally { lock.unlock(); }
						}
					};

             	intervals[i] = new SmallDial(-1, defaults)
					{
					protected String map(double val) 
						{ 
						return LABELS[(int)(val * 39)]; 
						}
					public double getValue() 
						{ 
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { return triadexmuse.getInterval(_i) / 39.0; }
						finally { lock.unlock(); }
						}
					public void setValue(double val) 
						{ 
						if (seq == null) return;
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { triadexmuse.setInterval(_i, (int)(val * 39)); }
						finally { lock.unlock(); }
						}
					public void setDefault(int val) 
						{ 
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { if (val != SmallDial.NO_DEFAULT) triadexmuse.setInterval(_i, -(val + 1)); }
						finally { lock.unlock(); }
						}
					public int getDefault()
						{
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { double val = triadexmuse.getInterval(_i); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
						finally { lock.unlock(); }
						}
					};
				}

             	volume = new SmallDial(-1, defaults)
					{
					protected String map(double val) 
						{ 
						return String.format("%.4f", val);  
						}
					public double getValue() 
						{ 
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { return triadexmuse.getVelocity() / 127.0; }
						finally { lock.unlock(); }
						}
					public void setValue(double val) 
						{ 
						if (seq == null) return;
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { triadexmuse.setVelocity((int)(val * 127)); }
						finally { lock.unlock(); }
						}
					public void setDefault(int val) 
						{ 
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { if (val != SmallDial.NO_DEFAULT) triadexmuse.setVelocity(-(val + 1)); }
						finally { lock.unlock(); }
						}
					public int getDefault()
						{
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { double val = triadexmuse.getVelocity(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
						finally { lock.unlock(); }
						}
					};

             	transpose = new SmallDial(-1, defaults)
					{
					protected String map(double val) 
						{ 
						return String.format("%.4f", val);  
						}
					public double getValue() 
						{ 
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { return triadexmuse.getTranspose() / 24.0; }
						finally { lock.unlock(); }
						}
					public void setValue(double val) 
						{ 
						if (seq == null) return;
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { triadexmuse.setTranspose((int)(val * 24)); }
						finally { lock.unlock(); }
						}
					public void setDefault(int val) 
						{ 
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { if (val != SmallDial.NO_DEFAULT) triadexmuse.setTranspose(-(val + 1)); }
						finally { lock.unlock(); }
						}
					public int getDefault()
						{
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { double val = triadexmuse.getTranspose(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
						finally { lock.unlock(); }
						}
					};

             	gate = new SmallDial(-1, defaults)
					{
					protected String map(double val) 
						{ 
						return String.format("%.4f", val);  
						}
					public double getValue() 
						{ 
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { return triadexmuse.getGate(); }
						finally { lock.unlock(); }
						}
					public void setValue(double val) 
						{ 
						if (seq == null) return;
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { triadexmuse.setGate(val); }
						finally { lock.unlock(); }
						}
					public void setDefault(int val) 
						{ 
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { if (val != SmallDial.NO_DEFAULT) triadexmuse.setGate(-(val + 1)); }
						finally { lock.unlock(); }
						}
					public int getDefault()
						{
						ReentrantLock lock = seq.getLock();
						lock.lock();
						try { double val = triadexmuse.getGate(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
						finally { lock.unlock(); }
						}
					};

            rate = new JComboBox(RATES);
            rate.setSelectedIndex(triadexmuse.getRateIndex());
            rate.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try
                    	{
                    	triadexmuse.setRate(triadexmuse.RATES[rate.getSelectedIndex()]);
						}
					finally { lock.unlock(); }
					}
				});
				
            rest = new JCheckBox();
            rest.setSelected(triadexmuse.getRest());
            rest.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { triadexmuse.setRest(rest.isSelected()); }
                    finally { lock.unlock(); }                              
                    }
                });
                
            legato = new JCheckBox();
            legato.setSelected(triadexmuse.getLegato());
            legato.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { triadexmuse.setLegato(legato.isSelected()); }
                    finally { lock.unlock(); }                              
                    }
                });
            }
        finally { lock.unlock(); }

        build(new String[] { "", "Volume", "Transpose", "Gate", "Rate", "Rests", "Legato", "Intervals", "A (+1)", "B (+2)", "C (+4)", "D (+Octave)", "Themes", "W", "X", "Y", "Z" }, 
            new JComponent[] 
                {
                preset,
                volume.getLabelledDial("127"),
                transpose.getLabelledDial("24"),
                gate.getLabelledDial("0.8888"),
                rate,
                rest,
                legato,
                null,
                intervals[0].getLabelledDial("C 1/2"),
                intervals[1].getLabelledDial("C 1/2"),
                intervals[2].getLabelledDial("C 1/2"),
                intervals[3].getLabelledDial("C 1/2"),
                null,
                themes[0].getLabelledDial("C 1/2"),
                themes[1].getLabelledDial("C 1/2"),
                themes[2].getLabelledDial("C 1/2"),
                themes[3].getLabelledDial("C 1/2"),
                });
        }
                
    public void revise()
        {
        TriadexMuse triadexmuse = (TriadexMuse)algorithm;
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            {
            rate.setSelectedIndex(triadexmuse.getRateIndex()); 
            rest.setSelected(triadexmuse.getRest()); 
            legato.setSelected(triadexmuse.getLegato()); 
            }
        finally { lock.unlock(); }                              
        if (transpose != null) transpose.redraw();
        if (gate != null) gate.redraw();
        if (volume != null) volume.redraw();
        for(int i = 0; i < themes.length; i++)
        	{
        	if (themes[i] != null) themes[i].redraw();
        	if (intervals[i] != null) intervals[i].redraw();
        	}
        }

    }
