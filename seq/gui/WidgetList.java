/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.gui;

import java.awt.*;
import javax.swing.*;

/**
	WIDGETLIST
	
	This is a simple component which displays other components vertically, each one with a label to their left,
	as is often the case in object inspectors.  It is designed to easily add a border.  WidgetList itself uses
	a BorderLayout with a single inner object located at BorderLayout.NORTH, so you can add additional objects
	below it.
**/
 
public class WidgetList extends JPanel
    {
    private static final long serialVersionUID = 1;
    
    // The actual container of components
    JPanel panel;
    // The labels for the components
    JLabel[] labels;
    // The components proper
    JComponent[] widgets;
    
    /** Returns all the labels for the components.  Don't modify these. */
    public JLabel[] getLabels() { return labels; }
    
    /** Returns all the components.  Don't modify these. */
    public JComponent[] getWidgets() { return widgets; }
    
    /** Sets WidgetList to have a border with the given label, or no border if the label is null. */
    public void makeBorder(String label)
        {
        if (label == null) setBorder(null);
        else setBorder(new javax.swing.border.TitledBorder("<html><i>" + label + "</i></html>"));
        }
        
    /** Constructs an empty WidgetList. */
    public WidgetList() { }
                
    /** Constructs a WidgetList with the given components and their labels. */
    public WidgetList(String[] labels, JComponent[] widgets)
        {
        build(labels, widgets);
        }
        
    /** Sets the WidgetList to display the given components and their labels. */
    public JPanel build(String[] labels, JComponent[] widgets)
        {
        this.labels = new JLabel[labels.length];
        if (panel != null) remove(panel);
        panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        
        for(int i = 0; i < labels.length; i++)
            {
            c.gridx = 0;
            c.gridy = i;
            c.gridwidth = 1;
            c.gridheight = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.LINE_END;
            c.weightx = 0;
            c.weighty = 1;
            c.insets = new Insets(2, 2, 2, 2);
            
            if (widgets[i] == null)
                {
                panel.add(this.labels[i] = new JLabel("<html><i><font size=-4><br></font>" + labels[i] + "</i> ", SwingConstants.RIGHT), c);
                }
            else
                {
                panel.add(this.labels[i] = new JLabel(labels[i] + " ", SwingConstants.RIGHT), c);
                }
                
            c.gridx = 1;
            c.anchor = GridBagConstraints.LINE_START;
            c.weightx = 1;
            
            if (widgets[i] == null)
                {
                panel.add(new JPanel());
                }
            else
                {
                panel.add(widgets[i], c);
                }
            }
        
        setLayout(new BorderLayout());
        add(panel, BorderLayout.NORTH);
        this.widgets = widgets;
        updateToolTips();
        return panel;
        }

    /** If you have set tooltips for all the widgets, calling this method will cause their corresponding labels
    	to adopt the same tooltips, which looks and feels more proper. */
    public void updateToolTips()
        {
        for(int i = 0; i < labels.length; i++)
            {
            this.labels[i].setToolTipText(widgets[i] != null ? widgets[i].getToolTipText() : null);
            }
        }
    }
        
