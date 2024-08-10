/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.parallel.gui;

import seq.motif.parallel.*;
import seq.engine.*;
import seq.gui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.locks.*;

public class ParallelInspector extends WidgetList
    {
    Seq seq;
    Parallel parallel;
    ParallelUI parallelui;

    StringField name;
    JComboBox childrenToSelect;
    
    public static final String[] CHILDREN_TO_SELECT_STRINGS = 
        { 
        "Independent", "1", "2", "3", "4", "5", "6", "7", "8", 
        "9", "10", "11", "12", "13", "14", "15", "16", "All", "All, Finish After First" 
        };
                
    public ParallelInspector(Seq seq, Parallel parallel, ParallelUI parallelui)
        {
        this.seq = seq;
        this.parallel = parallel;
        this.parallelui = parallelui;

        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            name = new StringField(parallel.getName())
                {
                public String newValue(String newValue)
                    {
                    newValue = newValue.trim();
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { parallel.setName(newValue); }
                    finally { lock.unlock(); }
                    parallelui.updateText();
                    return newValue;
                    }
                                
                public String getValue() 
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return parallel.getName(); }
                    finally { lock.unlock(); }
                    }
                };
            name.setColumns(MotifUI.INSPECTOR_NAME_DEFAULT_SIZE);
                
            childrenToSelect = new JComboBox(CHILDREN_TO_SELECT_STRINGS);
            childrenToSelect.setSelectedIndex(parallel.getNumChildrenToSelect());
            childrenToSelect.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { parallel.setNumChildrenToSelect(childrenToSelect.getSelectedIndex()); }
                    finally { lock.unlock(); }                              
                    }
                });
            childrenToSelect.setMaximumRowCount(CHILDREN_TO_SELECT_STRINGS.length);
            }
        finally { lock.unlock(); }

        JPanel result = build(new String[] { "Name", "Children Playing"}, 
            new JComponent[] 
                {
                name,
                childrenToSelect,
                });
        remove(result);
        add(result, BorderLayout.CENTER);               // re-add it as center

        add(new DefaultParameterList(seq, parallelui), BorderLayout.NORTH);
        }
                
    public void revise()
        {
        Seq old = seq;
        seq = null;
        ReentrantLock lock = old.getLock();
        lock.lock();
        try 
            { 
            childrenToSelect.setSelectedIndex(parallel.getNumChildrenToSelect()); 
            }
        finally { lock.unlock(); }                              
        seq = old;

        name.update();
        }
    }
