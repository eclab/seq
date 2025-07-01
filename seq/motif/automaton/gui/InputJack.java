/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.automaton.gui;

import seq.motif.automaton.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.border.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.*;

/** 
    An InputOutput which represents an underlying input Unit.
    Contains a single Jack, the Unit in question, and at most one incoming wire.
*/

public class InputJack extends InputOutput
    {
    Jack jack;
    
    // True if we're (temporarily) highlighting ourselves
    boolean highlight;
    
    // The incoming Wire attached to us, if any
    ArrayList<Wire> incoming = new ArrayList<>();
    
    public ArrayList<Wire> getIncoming() { return incoming; }
    
    /** Removes a wire attached to this InputJack.  This should only be called by OutputJack.
        To disconnect a wire you should instead ask the OutputJack to do it.  */
    public void disconnect(Wire wire)
        {
        // delete wire from arraylist
        incoming.remove(wire);
                        
        paintComponent.repaint();
        }

    /** Removes all wires attached to this InputJack. */
    public void disconnectAll()
        {
        ArrayList<Wire> temp = new ArrayList<>(incoming);
        for(Wire wire : temp)
            {
            // Tell the output jack at the other end to disconnect me.  He's doing all the work.
            wire.getStart().disconnect();
            }
        }


    /** Constructor, given an owning unit and ModPanel, plus which unit input number we are in our owner. 
        If number is ConstraintsChooser.INDEX, then we draw somewhat differently.  */
    public InputJack(int number, JComponent paintComponent, AutomatonButton button)
        {
        this.paintComponent = paintComponent;
        this.number = number;
        setButton(button);
        
        jack = new Jack(this)
            {
            public Color getFillColor()
                {
                if (highlight)
                    {
                    return Color.RED;
                    }
                else if (true)                  // testing
                    {
                    return Color.YELLOW;
                    }
                else
                    {
                    return title.getBackground();
                    }
                }
            };

        title = new JLabel("" + number);
        title.setFont(SMALL_FONT());
        
        setLayout(new BorderLayout());
        add(title, BorderLayout.CENTER);
        add(jack, BorderLayout.WEST);

        MouseAdapter mouseAdapter = new MouseAdapter()
            {
            public void mouseClicked(MouseEvent e)
                {
                for (Wire wire : incoming)
                    {
                    wire.chooseColor();
                    }
                        
                paintComponent.repaint();
                }
            };
            
        addMouseListener(mouseAdapter);
        jack.setToolTipText(INPUT_TOOLTIP);
        }

    public String toString()
        {
        return "InputJack[name=" + title.getText() + ", number=" + number + ", panel=" + paintComponent + "]"; 
        }

    static final String INPUT_TOOLTIP = "<html><b>Input</b><br>" +
        "The input for this node.  You can connect this input to an <b>output</b> (a gray port)<br>" +
        "by dragging from the output to the input.  You can disconnect the input from an output by<br>" +
        "clicking on the output.  You can change the color of the connection by clicking on the input.<br><br>" +
        "When the node transitions out the output, play continues at the node with the connected input.</html>";
    }
