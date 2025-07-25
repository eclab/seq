/* 
   Copyright 2025 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.arpeggio.gui;

import seq.motif.arpeggio.*;
import seq.engine.*;
import seq.gui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.locks.*;

public class ArpeggioChildInspector extends WidgetList
    {
    Seq seq;
    Motif child;
    ArpeggioUI arpeggioUI = null;             // default
//    ArpeggioChildUI childUI = null;   // default
    StringField name;
    JButton revertButton;
    JPanel namePanel;
    int at;
    
    public int getAt() { return at; }
    
    // arpeggioUI can be null, or childUI, and at may be set to -1
    public ArpeggioChildInspector(Seq seq, Motif child, ArpeggioUI arpeggioUI, int at)
        {
        this.seq = seq;
        this.at = at;
        this.arpeggioUI = arpeggioUI;
        this.child = child;
                
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            name = new StringField(child.getName())
                {
                public String newValue(String newValue)
                    {
                    newValue = newValue.trim();
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try 
                        { 
                        if (arpeggioUI != null)
                            {
                            if (newValue == null || newValue.length() == 0)
                                {
                                newValue = child.getName();
                                }
                            arpeggioUI.setNickname(at, newValue); 
                            newValue = arpeggioUI.getNickname(at);             // because ArpeggioChild may have changed it
                            }
                        else
                            {
                            child.setName(newValue);
                            newValue = child.getName();
                            }
                        }
                    finally { lock.unlock(); }
//                    if (childUI != null) childUI.updateText();
//                    if (arpeggioUI != null) arpeggioUI.updateArpeggioChildName(at, newValue);
                    return newValue;
                    }
                                
                public String getValue() 
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return child.getDisplayedName(); }
                    finally { lock.unlock(); }
                    }
                };
            }
        finally { lock.unlock(); }

        name.setColumns(MotifUI.INSPECTOR_NAME_DEFAULT_SIZE);
        namePanel = new JPanel();
        namePanel.setLayout(new BorderLayout());
        namePanel.add(name, BorderLayout.CENTER);

        if (arpeggioUI == null)
            {
            name.setToolTipText(NAME_TOOLTIP);
            namePanel.setToolTipText(NAME_TOOLTIP);
            }
        else
            {
            name.setToolTipText(NICKNAME_TOOLTIP);
            namePanel.setToolTipText(NICKNAME_TOOLTIP);
            }

        build(new String[] { (arpeggioUI == null ? "Name" : "Nickname"), },
            new JComponent[] 
                {
                namePanel,
                });
        }
                
    public void revise()
        {
        name.update();
        }
        
    static final String NAME_TOOLTIP = "<html><b>Name</b><br>" +
        "Sets the name of the Arpeggio Child.  This will appear in the Motif List at left.</html>";

    static final String NICKNAME_TOOLTIP = "<html><b>Nickname</b><br>" +
        "Sets a nickname for the Arpeggio Child, overriding its name as originally set.</html>";


    }
