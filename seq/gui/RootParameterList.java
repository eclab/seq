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
    SmallDial[] ccDials = new SmallDial[Motif.NUM_PARAMETERS];
    JComboBox ccIn;
    JPanel ccInPanel = new JPanel();

    public RootParameterList(Seq seq)
        {
        JComponent[] comp = new JComponent[Motif.NUM_PARAMETERS + 4];
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
                    //protected String map(double val) { return String.format("%.4f", val); }
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
                }

            for(int i = 0; i < Motif.NUM_PARAMETERS; i++)
                {
                final int _i = i;
                ccDials[i] = new SmallDial(seq.getParameterCC(i) / 128.0)
                    {
                    protected String map(double val) { if (val == 1.0) return "None"; else return "" + (int)(val * 128.0); }
                    public double getValue() 
                        { 
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { return seq.getParameterCC(_i) / 128.0; }
                        finally { lock.unlock(); }
                        }
                    public void setValue(double val) 
                        { 
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { seq.setParameterCC(_i, (int)(val * 128.0)); }
                        finally { lock.unlock(); }
                        }
                    };
                JPanel combined = new JPanel();
                combined.setLayout(new BorderLayout());
                combined.add(dials[i].getLabelledDial("0.0000  "), BorderLayout.WEST);
                combined.add(new JLabel("  CC "), BorderLayout.CENTER);
                combined.add(ccDials[i].getLabelledDial("0.0000  "), BorderLayout.EAST);
                comp[i + 3] = combined;
                }
            }
        finally { lock.unlock(); }
        ccInPanel.setLayout(new BorderLayout());
        rebuildCCIn(seq);
        comp[comp.length - 1] = ccInPanel;
        
        String[] strs = new String[Motif.NUM_PARAMETERS + 4];
        strs[0] = "Rand Seed";
        strs[1] = "Rand Min";
        strs[2] = "Rand Max";
        for(int i = 0; i < Motif.NUM_PARAMETERS; i++)
            {
            strs[i + 3] = "Param " + (i + 1);
            }
        strs[strs.length - 1] = "CC In";
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
        
    
    public void rebuildCCIn(final Seq seq)
        {
        if (ccIn != null) ccInPanel.remove(ccIn);
        
        String[] ins = null;
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            In[] seqIns = seq.getIns();
            ins = new String[seqIns.length + 1];
            ins[0] = "<html><i>None</i></html>";
            for(int i = 0; i < seqIns.length; i++)
                {
                ins[i + 1] = "" + (i + 1) + ": " + seqIns[i].toString();
                }
            }
        finally { lock.unlock(); }                              

        ccIn = new JComboBox(ins);
        ccIn.setSelectedIndex(seq.getParameterCCIn());
        ccIn.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                if (seq == null) return;
                ReentrantLock lock = seq.getLock();
                lock.lock();
                try { seq.setParameterCCIn(ccIn.getSelectedIndex()); }
                finally { lock.unlock(); }                              
                }
            });
                
        ccInPanel.add(ccIn, BorderLayout.CENTER);
        }
    
    /*
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
    */



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
