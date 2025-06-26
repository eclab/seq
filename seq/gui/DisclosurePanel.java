// Copyright 2008 by Sean Luke and George Mason University
// Licensed under the Apache 2.0 License


package seq.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/** 
    A panel with a small disclosure triangle which toggles between two subJComponents:
    notionally an "abridged" (short) JComponent and an expanded ("disclosed") JComponent.
    The panel can sprout an optional titled label.

    <p>Thanks to:
    http://lists.apple.com/archives/java-dev/2005/Feb/msg00171.html

    ... for the idea.
*/


/*
  f = new JFrame();
  l = new Label("Four score and seven years ago!");
  b = new JButton("yo yo yo!");
  d = new sim.util.gui.DisclosurePanel(l,b);
  f.getContentPane().setLayout(new BorderLayout());
  f.getContentPane().add(d);
  f.pack();
  f.show();
*/

public class DisclosurePanel extends JPanel
    {
    private static final long serialVersionUID = 1;

    JToggleButton disclosureToggle = new JToggleButton();
    JComponent abridgedComponent;
    JComponent disclosedComponent;
    JComponent parentComponent;
    
    public JComponent getParentComponent() { return parentComponent; }
    public void setParentComponent(JComponent comp) { parentComponent = comp; }
    public void setToolTipText(String text) { abridgedComponent.setToolTipText(text); disclosureToggle.setToolTipText(text); }
    public String getToolTipText(String text) { return disclosureToggle.getToolTipText(); }
    
    public DisclosurePanel(String abridgedText, JComponent disclosedComponent)
        {
        this(abridgedText, disclosedComponent, null);
        }
                
    public DisclosurePanel(String abridgedText, JComponent disclosedComponent, String borderLabel)
        {
        this(new JButton(abridgedText), disclosedComponent);
        JButton button = (JButton)abridgedComponent;
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setRequestFocusEnabled(false);
        button.setBorder(BorderFactory.createEmptyBorder(1,0,0,0));             // 1 For the Mac
        button.setVerticalAlignment(SwingConstants.TOP);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                disclosureToggle.doClick();
                }
            });
        }

    public DisclosurePanel(JComponent abridgedComponent, JComponent disclosedComponent)
        {
        this(abridgedComponent, disclosedComponent, null);
        }
        
    public DisclosurePanel(JComponent abridgedComponent, JComponent disclosedComponent, String borderLabel)
        {
        // FlatLightLaf doesn't work well here
        if (UIManager.getLookAndFeel() instanceof com.formdev.flatlaf.FlatLightLaf)
            {
            disclosureToggle.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
            }
        disclosureToggle.setVerticalAlignment(SwingConstants.TOP);
        disclosureToggle.setContentAreaFilled(false);
        disclosureToggle.setFocusPainted(false);
        disclosureToggle.setRequestFocusEnabled(false);
        disclosureToggle.setIcon(UIManager.getIcon("Tree.collapsedIcon"));
        disclosureToggle.setSelectedIcon(UIManager.getIcon("Tree.expandedIcon"));
        this.abridgedComponent = abridgedComponent;
        this.disclosedComponent = disclosedComponent;
        setLayout(new BorderLayout());
        JPanel b = new JPanel();
        b.setLayout(new BorderLayout());
        b.add(disclosureToggle, BorderLayout.NORTH);
        add(b, BorderLayout.WEST);
        add(abridgedComponent, BorderLayout.CENTER);

        if (borderLabel != null)
            setBorder(new javax.swing.border.TitledBorder(borderLabel));
        
        disclosureToggle.addItemListener(new ItemListener()
            {
            public void itemStateChanged(ItemEvent e)
                {
                setDisclosed(disclosureToggle.isSelected());
                }
            });
        }
        
    boolean disclosed = false;  // abridged
        
    public void setDisclosed(boolean disclosed)
        {
        this.disclosed = disclosed;
        if (disclosed) // disclose
            {
            remove(abridgedComponent);
            add(disclosedComponent, BorderLayout.CENTER);
            revalidate();
            if (parentComponent != null) parentComponent.revalidate();
            repaint();
            }
        else // hide
            {
            remove(disclosedComponent);
            add(abridgedComponent, BorderLayout.CENTER);
            revalidate();
            if (parentComponent != null) parentComponent.revalidate();
            repaint();
            }
        disclosureToggle.setSelected(disclosed);
        }
    
    public JToggleButton getToggle() { return disclosureToggle; }
    
    public boolean isDisclosed() { return disclosed; }
        
    public JComponent getAbridgedComponent()
        {
        return abridgedComponent;
        }
                
    public JComponent getDisclosedComponent()
        {
        return disclosedComponent;
        }
                
    public void setAbridgedComponent(JComponent abridgedComponent)
        {
        if (!disclosureToggle.isSelected())
            {
            remove(this.abridgedComponent);
            add(abridgedComponent, BorderLayout.CENTER);
            revalidate();
            if (parentComponent != null) parentComponent.revalidate();
            }
        this.abridgedComponent = abridgedComponent;
        }

    public void setDisclosedComponent(JComponent disclosedComponent)
        {
        if (disclosureToggle.isSelected())
            {
            remove(this.disclosedComponent);
            add(disclosedComponent, BorderLayout.CENTER);
            revalidate();
            if (parentComponent != null) parentComponent.revalidate();
            }
        this.disclosedComponent = disclosedComponent;
        }
    }

