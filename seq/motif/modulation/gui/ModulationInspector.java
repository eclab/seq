/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.modulation.gui;

import seq.motif.modulation.*;
import seq.engine.*;
import seq.gui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.locks.*;

public class ModulationInspector extends WidgetList
    {
    Seq seq;
    Modulation modulation;
    ModulationUI modulationui;

    StringField name;

    public ModulationInspector(Seq seq, Modulation modulation, ModulationUI modulationui)
        {
        this.seq = seq;
        this.modulation = modulation;
        this.modulationui = modulationui;

        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            name = new StringField(modulation.getName())
                {
                public String newValue(String newValue)
                    {
                    newValue = newValue.trim();
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { modulation.setName(newValue); }
                    finally { lock.unlock(); }
                    modulationui.updateText();
                    return newValue;
                    }
                                
                public String getValue() 
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return modulation.getName(); }
                    finally { lock.unlock(); }
                    }
                };
            name.setColumns(MotifUI.INSPECTOR_NAME_DEFAULT_SIZE);
            }
        finally { lock.unlock(); }

        JPanel result = build(new String[] { "Name", }, 
            new JComponent[] 
                {
                name,
                });
        remove(result);
        add(result, BorderLayout.CENTER);               // re-add it as center

        add(new DefaultParameterList(seq, modulationui), BorderLayout.NORTH);
        }
                
    public void revise()
        {
        Seq old = seq;
        seq = null;
        ReentrantLock lock = old.getLock();
        lock.lock();
        try 
            { 
            }
        finally { lock.unlock(); }                              
        seq = old;
        name.update();
        }
    }
