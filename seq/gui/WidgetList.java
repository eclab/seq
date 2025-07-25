/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.gui;

import java.awt.*;
import javax.swing.*;
 
public class WidgetList extends JPanel
    {
    private static final long serialVersionUID = 1;
        
    JPanel panel;
    JLabel[] labels;
    JComponent[] widgets;
    
    public JLabel[] getLabels() { return labels; }
    
    public WidgetList() { }
                
    public void makeBorder(String label)
        {
        setBorder(new javax.swing.border.TitledBorder("<html><i>" + label + "</i></html>"));
        }
        
    public WidgetList(String[] labels, JComponent[] widgets)
        {
        build(labels, widgets);
        }
        
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

    public void updateToolTips()
        {
        for(int i = 0; i < labels.length; i++)
            {
            this.labels[i].setToolTipText(widgets[i] != null ? widgets[i].getToolTipText() : null);
            }
        }
    }
        
