/* 
   Copyright 2025 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.filter.gui;

import seq.motif.filter.*;
import seq.engine.*;
import seq.gui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.locks.*;

public class FilterInspector extends WidgetList
    {
    Seq seq;
    Filter filter;
    FilterUI filterui;

    StringField name;
    TimeDisplay activeFrom;
    TimeDisplay activeTo;
    JCheckBox activeAlways;

    public FilterInspector(Seq seq, Filter filter, FilterUI filterui)
        {
        this.seq = seq;
        this.filter = filter;
        this.filterui = filterui;

        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            name = new StringField(filter.getName())
                {
                public String newValue(String newValue)
                    {
                    newValue = newValue.trim();
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { filter.setName(newValue); }
                    finally { lock.unlock(); }
                    filterui.updateText();
                    return newValue;
                    }
                                
                public String getValue() 
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return filter.getName(); }
                    finally { lock.unlock(); }
                    }
                };
            name.setColumns(MotifUI.INSPECTOR_NAME_DEFAULT_SIZE);

            activeFrom = new TimeDisplay(filter.getFrom(), seq, true)
                {
                public int getTime()
                    {
                    return filter.getFrom();
                    }
                        
                public void setTime(int time)
                    {
                    filter.setFrom(time);
                    }
                };
            activeFrom.setDisplaysTime(true);

            activeTo = new TimeDisplay(filter.getTo(), seq, true)
                {
                public int getTime()
                    {
                    return filter.getTo();
                    }
                        
                public void setTime(int time)
                    {
                    filter.setTo(time);
                    }
                };
            activeTo.setDisplaysTime(true);                                            

            activeAlways = new JCheckBox();
            activeAlways.setSelected(filter.isAlways());
            activeAlways.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { filter.setAlways(activeAlways.isSelected()); }
                    finally { lock.unlock(); }                              
                    }
                });
            }
        finally { lock.unlock(); }

        name.setToolTipText(NAME_TOOLTIP);

        build(new String[] { "Name", "Always", "From", "To" }, 
            new JComponent[] 
                {
                name,
                activeAlways,
                activeFrom,
                activeTo
                });
        }
                
    public void revise()
        {
        Seq old = seq;
        seq = null;
        ReentrantLock lock = old.getLock();
        lock.lock();
        try 
            {
            activeAlways.setSelected(filter.isAlways());
            }
        finally { lock.unlock(); }                              
        seq = old;
        name.update();
        if (activeFrom != null) activeFrom.revise();
        if (activeTo != null) activeTo.revise();
        }

    static final String NAME_TOOLTIP = "<html><b>Name</b><br>" +
        "Sets the name of the Filter.  This will appear in the Motif List at left.</html>";

    }
