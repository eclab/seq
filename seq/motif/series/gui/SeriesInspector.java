/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.series.gui;

import seq.motif.series.*;
import seq.engine.*;
import seq.gui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.locks.*;

public class SeriesInspector extends WidgetList
    {
    Seq seq;
    Series series;
    SeriesUI seriesui;

    StringField name;
    JComboBox mode;

    public static final String[] MODE_STRINGS = { "Series", "Shuffle", "Random", "Random Markov", "Round Robin", "Variation", "Rand Variation" };         // , "Round Robin Shared" };

    public SeriesInspector(Seq seq, Series series, SeriesUI seriesui)
        {
        this.seq = seq;
        this.series = series;
        this.seriesui = seriesui;

        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            name = new StringField(series.getName())
                {
                public String newValue(String newValue)
                    {
                    newValue = newValue.trim();
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { series.setName(newValue); }
                    finally { lock.unlock(); }
                    seriesui.updateText();
                    return newValue;
                    }
                                
                public String getValue() 
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return series.getName(); }
                    finally { lock.unlock(); }
                    }
                };
            name.setColumns(MotifUI.INSPECTOR_NAME_DEFAULT_SIZE);
                
            mode = new JComboBox(MODE_STRINGS);
            mode.setSelectedIndex(series.getMode());
            mode.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { series.setMode(mode.getSelectedIndex()); }
                    finally { lock.unlock(); }                              
                    }
                });
            }
        finally { lock.unlock(); }

        JPanel result = build(new String[] { "Name", "Mode" }, 
            new JComponent[] 
                {
                name,
                mode,
                });
        remove(result);
        add(result, BorderLayout.CENTER);               // re-add it as center

        add(new DefaultParameterList(seq, seriesui), BorderLayout.NORTH);
        }
                
    public void revise()
        {
        Seq old = seq;
        seq = null;
        ReentrantLock lock = old.getLock();
        lock.lock();
        try 
            { 
            mode.setSelectedIndex(series.getMode()); 
            }
        finally { lock.unlock(); }                              
        seq = old;
        name.update();
        }
    }
