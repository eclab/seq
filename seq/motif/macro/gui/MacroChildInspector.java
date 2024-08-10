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

public class MacroChildInspector extends WidgetList
    {
    Seq seq;
    MacroChild macroChild;
    MacroUI macroUI = null;             // default
    MacroChildUI macroChildUI = null;   // default
    StringField name;
    JButton revertButton;
    JPanel namePanel;
    int at;
    
    public int getAt() { return at; }
    
    // macroUI can be null, or macroChildUI, and at may be set to -1
    public MacroChildInspector(Seq seq, MacroChild macroChild, MacroChildUI macroChildUI, MacroUI macroUI, int at)
        {
        this.seq = seq;
        this.macroChild = macroChild;
        this.at = at;
        this.macroUI = macroUI;
        this.macroChildUI = macroChildUI;
                
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            name = new StringField(macroChild.getName())
                {
                public String newValue(String newValue)
                    {
                    newValue = newValue.trim();
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try 
                        { 
                        if (macroUI != null)
                            {
                            if (newValue == null || newValue.length() == 0)
                                {
                                newValue = macroChild.getName();
                                }
                            macroUI.setNickname(at, newValue); 
                            newValue = macroUI.getNickname(at);             // because MacroChild may have changed it
                            }
                        else
                            {
                            macroChild.setName(newValue);
                            newValue = macroChild.getName();
                            }
                        }
                    finally { lock.unlock(); }
                    if (macroChildUI != null) macroChildUI.updateText();
                    if (macroUI != null) macroUI.updateMacroChildName(at, newValue);
                    return newValue;
                    }
                                
                public String getValue() 
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return macroChild.getDisplayedName(); }
                    finally { lock.unlock(); }
                    }
                };
            }
        finally { lock.unlock(); }

        name.setColumns(MotifUI.INSPECTOR_NAME_DEFAULT_SIZE);
        namePanel = new JPanel();
        namePanel.setLayout(new BorderLayout());
        namePanel.add(name, BorderLayout.CENTER);

/*
  if (macroUI != null)
  {               
  JButton revertButton = new JButton("Revert");
  namePanel.add(revertButton, BorderLayout.EAST);
  revertButton.addActionListener(new ActionListener()
  {
  public void actionPerformed(ActionEvent e)
  {
  name.setValue(macroChild.getName());
  macroUI.updateMacroChildName(at, macroChild.getName());
  }
  });
  }
*/
        build(new String[] { (macroUI == null ? "Name" : "Nickname"), },
            new JComponent[] 
                {
                namePanel,
                });
        }
                
    public void revise()
        {
        name.update();
        }
        
    }
