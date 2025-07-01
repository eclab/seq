/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

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
                comp[i + 3] = dials[i].getLabelledDial("0.0000  ");
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
        DisclosurePanel panel = new DisclosurePanel("Root Arguments", list);
        add(panel, BorderLayout.CENTER);


        // tooltips
        comp[0].setToolTipText(RAND_SEED_TOOLTIP);
        comp[1].setToolTipText(RAND_MIN_TOOLTIP);
        comp[2].setToolTipText(RAND_MAX_TOOLTIP);
        for(int i = 1; i <= 8; i++)
            {
            comp[i + 2].setToolTipText("<html><b>Value of Parameter " + i + "</b><br>" +
                "The value for <b>parameter " + i + "</b> passed into the <b>root motif</b>.</html>");                  
            }
        list.updateToolTips();          // update the labels
        panel.setToolTipText(ROOT_ARGUMENTS_TOOLTIP);
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



    /*** Tooltips ***/
        
    static final String RAND_SEED_TOOLTIP = "<html><b>Random Number Seed</b><br>" +
        "The initial number used to seed the random number generator<br>" +
        "used to generate values for to the <b>random parameter</b><br>" +
        "passed into the <b>root motif.</b><br><br>" +
        "Starting from a given random number seed, the random sequence<br>" +
        "will always be the same.  So to change the random sequence, just<br>" +
        "pick a different seed.</html>";
                
    static final String RAND_MIN_TOOLTIP = "<html><b>Random Minimum Value</b><br>" +
        "The minimum posssible value for the <b>random parameter</b><br>" +
        "passed into the <b>root motif.</b></html>";
        
    static final String RAND_MAX_TOOLTIP = "<html><b>Random Maximum Value</b><br>" +
        "The maximum posssible value for the <b>random parameter</b><br>" +
        "passed into the <b>root motif.</b></html>";

    static final String ROOT_ARGUMENTS_TOOLTIP = "<html><b>Root Arguments</b><br>" +
        "Settings for the root arguments.<br><br>" +
        "Every motif can receive eight parameters values from its parent motif,<br>" +
        "plus a random number drawn from a distribution with a minimum and maximum<br>" +
        "value and an initial random number seed.<br><br>" +
        "But the root motif has no parent.  So these root arguments specify what<br" +
        "it will receive.</html>";

    }
