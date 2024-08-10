/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.silence.gui;

import seq.motif.silence.*;
import seq.engine.*;
import seq.gui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.locks.*;

public class SilenceInspector extends WidgetList
    {
    Seq seq;
    Silence silence;
    SilenceUI silenceui;

    StringField name;
    TimeDisplay time;
     
    public SilenceInspector(Seq seq, Silence silence, SilenceUI silenceui)
        {
        this.seq = seq;
        this.silence = silence;
        this.silenceui = silenceui;

        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            name = new StringField(silence.getName())
                {
                public String newValue(String newValue)
                    {
                    newValue = newValue.trim();
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { silence.setName(newValue); }
                    finally { lock.unlock(); }
                    silenceui.updateText();
                    return newValue;
                    }
                                
                public String getValue() 
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return silence.getDisplayedName(); }
                    finally { lock.unlock(); }
                    }
                };
            name.setColumns(MotifUI.INSPECTOR_NAME_DEFAULT_SIZE);


            time = new TimeDisplay(seq)
                {
                public void updateTime(int time)
                    {
                    silence.setLength(time);
                    }
                };

            }
        finally { lock.unlock(); }

        build(new String[] { "Name", "Length"  }, 
            new JComponent[] 
                {
                name,
                time,
                });
        }
                
    public void revise()
        {
        name.update();
        time.revise();
        }
        
    }
