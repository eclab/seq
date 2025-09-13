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
            }
        finally { lock.unlock(); }

        name.setToolTipText(NAME_TOOLTIP);

        JPanel result = build(new String[] { "Name" }, 
            new JComponent[] 
                {
                name,
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
        }


    static final String NAME_TOOLTIP = "<html><b>Name</b><br>" +
        "Sets the name of the Automaton.  This will appear in the Motif List at left.</html>";
        
    }
