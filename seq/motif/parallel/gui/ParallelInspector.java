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
        "All", "1", "2", "3", "4", "5", "6", "7", "8", 
        "9", "10", "11", "12", "13", "14", "15", "16", "All, Finish After First" 
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
            name.setToolTipText(NAME_TOOLTIP);
                
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
            childrenToSelect.setToolTipText(CHILDREN_PLAYING_TOOLTIP);
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


    static final String NAME_TOOLTIP = "<html><b>Name</b><br>" +
        "Sets the name of the Parallel.  This will appear in the Motif List at left.</html>";

    static final String CHILDREN_PLAYING_TOOLTIP = "<html><b>Children Playing</b><br>" +
        "States how many children play in parallel (picked at random) when the Parallel plays.<br>" +
        "The children not picked to play will be muted.  Which children are picked changes each<br>" +
        "time the Parallel plays anew.  The options are:" +
        "<ul>" +
        "<li>All children may play (the default).  A child plays according to its <b>probability</b>." + 
        "<li>N = 1 ... 16 children play.  The likelihood that a child is selected to play is<br>" +
        "according to its <b>probability</b> compared to the probabilities of the other children.<br>" + 
        "If there are fewer than N children, all of them play." +
        "<li>All children may play, but all stop when the first child is finished.  The first child<br>" + 
        "always plays.  Each of the other children plays according to its <b>probability</b>. " +
        "</ul>" +
        "The <b>probability</b> of each child is set in its inspector below.</html>";

    }
