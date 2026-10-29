/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.automaton.gui;

import seq.motif.automaton.*;
import seq.engine.*;
import seq.gui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.locks.*;

public class AutomatonInspector extends WidgetList
    {
    Seq seq;
    Automaton automaton;
    AutomatonUI automatonui;

    StringField name;
    JButton release;
    JButton finish;
    SmallDial seed;
    
    public AutomatonInspector(Seq seq, Automaton automaton, AutomatonUI automatonui)
        {
        this.seq = seq;
        this.automaton = automaton;
        this.automatonui = automatonui;

        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            name = new StringField(automaton.getName())
                {
                public String newValue(String newValue)
                    {
                    newValue = newValue.trim();
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { automaton.setName(newValue); }
                    finally { lock.unlock(); }
                    automatonui.updateText();
                    return newValue;
                    }
                                
                public String getValue() 
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return automaton.getName(); }
                    finally { lock.unlock(); }
                    }
                };
            name.setColumns(MotifUI.INSPECTOR_NAME_DEFAULT_SIZE);


            seed = new SmallDial(0)
                {
                protected String map(double val) 
                    {
                    if (val == 0) return "Global";
                    else return String.format("%.4f", val);
                    }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return automaton.getLocalRandomSeed(); }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { automaton.setLocalRandomSeed(val); }
                    finally { lock.unlock(); }
                    }
                };
            }
        finally { lock.unlock(); }

        name.setToolTipText(NAME_TOOLTIP);
        seed.setToolTipText(SEED_TOOLTIP);

        JPanel result = build(new String[] { "Name", "Seed" }, 
            new JComponent[] 
                {
                name,
                seed.getLabelledDial("0.8888"),
                });
        
        remove(result);
        add(result, BorderLayout.CENTER);               // re-add it as center

        add(new DefaultParameterList(seq, automatonui), BorderLayout.SOUTH);
        }
                
    public void revise()
        {
        Seq old = seq;
        seq = null;
        ReentrantLock lock = old.getLock();
        lock.lock();
        try 
            { 
            // Nothing
            }
        finally { lock.unlock(); }                              
        name.update();
        if (seed != null) seed.redraw();
        }


    static final String NAME_TOOLTIP = "<html><b>Name</b><br>" +
        "Sets the name of the Automaton.  This will appear in the Motif List at left.</html>";
        
    static final String SEED_TOOLTIP = "<html><b>Random Number Seed</b><br>" +
        "Specifies the seed of the Automaton's local random number generator.<br>" +
        "If the value is 0 (or \"Global\"), then Seq's global random number generator will be used instead.</html>";
    }
