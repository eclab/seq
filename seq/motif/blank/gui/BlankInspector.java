/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.blank.gui;

import seq.motif.blank.*;
import seq.engine.*;
import seq.gui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.locks.*;

public class BlankInspector extends WidgetList
    {
    Seq seq;
    Blank blank;
    BlankUI blankui;

    StringField name;
     
    public BlankInspector(Seq seq, Blank blank, BlankUI blankui)
        {
        this.seq = seq;
        this.blank = blank;
        this.blankui = blankui;

        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            name = new StringField(blank.getName())
                {
                public String newValue(String newValue)
                    {
                    newValue = newValue.trim();
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { blank.setName(newValue); }
                    finally { lock.unlock(); }
                    blankui.updateText();
                    return newValue;
                    }
                                
                public String getValue() 
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return blank.getDisplayedName(); }
                    finally { lock.unlock(); }
                    }
                };
            name.setColumns(MotifUI.INSPECTOR_NAME_DEFAULT_SIZE);
            }
        finally { lock.unlock(); }

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
        
    }
