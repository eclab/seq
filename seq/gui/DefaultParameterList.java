/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.gui;

import seq.engine.*;
import seq.util.*;
import java.awt.*;
import javax.swing.border.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.concurrent.locks.*;
import java.util.*;
import com.formdev.flatlaf.*;

public class DefaultParameterList extends JPanel
    {
    StringField[] names = new StringField[Motif.NUM_PARAMETERS];
    SeqUI sequi;
        
    public DefaultParameterList(Seq seq, MotifUI motifui)
        {
        this(seq, motifui.getSeqUI(), motifui.getMotif());
        }

    DefaultParameterList(Seq seq, SeqUI sequi, Motif motif)
        {
        this.sequi = sequi;
        JComponent[] comp = new JComponent[Motif.NUM_PARAMETERS];       //  + 2];
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            { 
            for(int i = 0; i < Motif.NUM_PARAMETERS; i++)
                {
                final int _i = i;
                String defName = motif.getParameterName(i);
                if (defName == null) defName = "";
                names[i] = new StringField(defName)
                    {
                    public String newValue(String newValue)
                        {
                        newValue = newValue.trim();
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { motif.setParameterName(_i, newValue.equals("") ? null : newValue); }
                        finally { lock.unlock(); }
                        reopen = true;          // ugh, see below
                        sequi.incrementRebuildInspectorsCount();                // force inspectors to update
                        return newValue;
                        }
                                                                
                    public String getValue() 
                        {
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { String d = motif.getParameterName(_i); if (d == null) return ""; else return d; }
                        finally { lock.unlock(); }
                        }
                    };
                names[i].setToolTipText("<html><b>Name of Parameter " + (i + 1) + "</b><br>" +
                    "Here you can customize the name for Parameter " + (i + 1) + ".<br>" +
                    "This name will appear in the pop-up menus of dials you can bind to this Parameter.<br>" +
                    "It will also appear as the <b>Arguments List</b> for assigning values to arguments passed in.</html>");                  
                names[i].setColumns(MotifUI.PARAMETER_LIST_NAME_DEFAULT_SIZE);
                JPanel panel = new JPanel();
                panel.setLayout(new BorderLayout());
                panel.add(names[i], BorderLayout.CENTER);
                comp[i] = panel;
                }
            }
        finally { lock.unlock(); }
        String[] strs = new String[Motif.NUM_PARAMETERS];
        for(int i = 0; i < Motif.NUM_PARAMETERS; i++)
            {
            strs[i] = "Param " + (i + 1);
            }
        WidgetList list = new WidgetList(strs, comp);
        list.makeBorder("Parameter Names");
        //new javax.swing.border.TitledBorder("<html><i>Parameter Names</i></html>"));
        setLayout(new BorderLayout());
        DisclosurePanel disclosure = new DisclosurePanel("Parameter Names", list);
        disclosure.setParentComponent(sequi);
        add(disclosure, BorderLayout.CENTER);
        if (reopen)
            {
            disclosure.setDisclosed(true);
            }
        reopen = false;
        }
        
    /// THIS UGLY HACK
    /// Allows the NEXT DefaultParameterList to make itself open initially,
    /// So DefaultParameterList doesn't constantly close itself when you change one name
    static boolean reopen = false;
    
    /*
      public void revise()
      {
      for(int i = 0; i < Motif.NUM_PARAMETERS; i++)
      {
      names[i].update();
      }
      }
    */
    }
