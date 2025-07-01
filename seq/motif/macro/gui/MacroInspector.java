/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.macro.gui;

import seq.motif.macro.*;
import seq.engine.*;
import seq.gui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.locks.*;

public class MacroInspector extends WidgetList
    {
    Seq seq;
    Macro macro;
    MacroUI macroui;

    StringField name;
                
    public MacroInspector(Seq seq, Macro macro, MacroUI macroui)
        {
        this.seq = seq;
        this.macro = macro;
        this.macroui = macroui;

        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            name = new StringField(macro.getName())
                {
                public String newValue(String newValue)
                    {
                    newValue = newValue.trim();
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { macro.setName(newValue); }
                    finally { lock.unlock(); }
                    macroui.updateText();
                    return newValue;
                    }
                                
                public String getValue() 
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return macro.getName(); }
                    finally { lock.unlock(); }
                    }
                };
            name.setColumns(MotifUI.INSPECTOR_NAME_DEFAULT_SIZE);
            }
        finally { lock.unlock(); }

        name.setToolTipText(NAME_TOOLTIP);

        build(new String[] { "Name" }, 
            new JComponent[] 
                {
                name,
                });

        }
                
    public void revise()
        {
        name.update();
        }

    static final String NAME_TOOLTIP = "<html><b>Name</b><br>" +
        "Sets the name of the Macro.  This will appear in the Motif List at left.</html>";

    }
