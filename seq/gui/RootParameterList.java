package seq.gui;

import seq.engine.*;
import seq.util.*;
import java.awt.*;
import javax.swing.border.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.concurrent.locks.*;
import java.util.*;
import com.formdev.flatlaf.*;

public class RootParameterList extends JPanel
    {
    public static final int SEED_COLUMNS = 8;

    SmallDial min;
    SmallDial max;
    StringField seed;
    SmallDial[] dials = new SmallDial[Motif.NUM_PARAMETERS];
        
    public RootParameterList(Seq seq)
        {
        JComponent[] comp = new JComponent[Motif.NUM_PARAMETERS + 3];
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            { 

            seed = new StringField(String.valueOf(seq.getDeterministicRandomSeed()))
                {
                public String newValue(String newValue)
                    {
                    newValue = newValue.trim();
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try 
                        {
                        try { int val = Integer.parseInt(newValue); seq.seedDeterministicRandom(val); }
                        catch (NumberFormatException ex) { newValue = String.valueOf(seq.getDeterministicRandomSeed()); }
                        }
                    finally { lock.unlock(); }
                    return newValue;
                    }
                                                                
                public String getValue() 
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return "" + seq.getDeterministicRandomSeed(); }
                    finally { lock.unlock(); }
                    }
                };
            seed.setColumns(SEED_COLUMNS);            
            comp[0] = seed;

            min = new SmallDial(seq.getRandomMin())
                {
                protected String map(double val) { return String.format("%.4f", val); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return seq.getRandomMin(); }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { seq.setRandomMin(val); }
                    finally { lock.unlock(); }
                    }
                };
            comp[1] = min.getLabelledDial("0.0000  ");
 
            max = new SmallDial(seq.getRandomMax())
                {
                protected String map(double val) { return String.format("%.4f", val); }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return seq.getRandomMax(); }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { seq.setRandomMax(val); }
                    finally { lock.unlock(); }
                    }
                };
            comp[2] = max.getLabelledDial("0.0000  ");
 
            for(int i = 0; i < Motif.NUM_PARAMETERS; i++)
                {
                final int _i = i;
                dials[i] = new SmallDial(seq.getParameterValue(i))
                    {
                    protected String map(double val) { return String.format("%.4f", val); }
                    public double getValue() 
                        { 
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { return seq.getParameterValue(_i); }
                        finally { lock.unlock(); }
                        }
                    public void setValue(double val) 
                        { 
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { seq.setParameterValue(_i, val); }
                        finally { lock.unlock(); }
                        }
                    };

                JPanel panel = new JPanel();
                panel.setLayout(new BorderLayout());
                panel.add(dials[i].getLabelledDial("0.0000  "), BorderLayout.WEST);
                comp[i + 3] = panel;
                }
            }
        finally { lock.unlock(); }
        String[] strs = new String[Motif.NUM_PARAMETERS + 3];
        strs[0] = "Rand Seed";
        strs[1] = "Rand Min";
        strs[2] = "Rand Max";
        for(int i = 0; i < Motif.NUM_PARAMETERS; i++)
            {
            strs[i + 3] = "Param " + (i + 1);
            }
        WidgetList list = new WidgetList(strs, comp);
        setLayout(new BorderLayout());
        add(new DisclosurePanel("Root Arguments", list), BorderLayout.CENTER);
        }
    
    public void revise()
        {
        min.redraw();
        max.redraw();
        seed.update();
        for(int i = 0; i < Motif.NUM_PARAMETERS; i++)
            {
            dials[i].redraw();
            }
        }
    }
