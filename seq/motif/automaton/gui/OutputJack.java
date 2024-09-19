/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.automaton.gui;

import seq.motif.automaton.*;
import seq.engine.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.border.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.font.*;
import java.util.*;

/** 
    An InputOutput which represents an underlying output Unit.
    Contains a single Jack, the Unit in question, and a list of outgoing wires.
*/

public class OutputJack extends InputOutput
    {
    AutomatonUI automatonui;
    
    // Outgoing wire
    Wire outgoing = null;
    
    // Holds the temporary wire being drawn by the user
    Wire temp;
    
    // Handles mouse global released messages (because they don't always go to the right place)
    AWTEventListener releaseListener = null;
    
    // True if we're expecting a release, so the releaseListener's messages are valid 
    boolean mouseDown = false;
    
    // The previous InputJack which was highlighted as we passed over it during drawing, so we can unhighlight it
    InputJack lastInputJack = null;
    
    public Wire getOutgoing() { return outgoing; }
    
    /** Removes the wire attached to the OutputJack */
    public void disconnect()
        {
        if (outgoing != null) 
            {
            removeEdge();
            outgoing.end.disconnect(outgoing);
            outgoing = null;
            }
        }
    
    /** Constructor, given an owning unit and ModPanel, plus which unit output number we are in our owner. */
    public OutputJack(int number, AutomatonUI automatonui, final JComponent paintComponent, AutomatonButton button)
        {
        this.number = number;
        this.paintComponent = paintComponent;
        this.automatonui = automatonui;
        setButton(button);
        
        jack = new Jack();

        title = new JLabel("" + number, SwingConstants.RIGHT);
        title.setFont(SMALL_FONT());
        
        setLayout(new BorderLayout());
        add(title, BorderLayout.CENTER);
        add(jack, BorderLayout.EAST);
        
        MouseAdapter mouseAdapter = new MouseAdapter()
            {
            public void mousePressed(MouseEvent e)
                {
                automatonui.getSeq().push();
                disconnect();
                                
                temp = new Wire(paintComponent);
                temp.setStart(OutputJack.this);
                paintComponent.repaint();

                if (releaseListener != null)
                    Toolkit.getDefaultToolkit().removeAWTEventListener(releaseListener);

                // This gunk fixes a BAD MISFEATURE in Java: mouseReleased isn't sent to the
                // same component that received mouseClicked.  What the ... ? Asinine.
                // So we create a global event listener which checks for mouseReleased and
                // calls our own private function.  EVERYONE is going to do this.
                                
                Toolkit.getDefaultToolkit().addAWTEventListener( releaseListener = new AWTEventListener()
                    {
                    public void eventDispatched(AWTEvent e)
                        {
                        if (e instanceof MouseEvent && e.getID() == MouseEvent.MOUSE_RELEASED)
                            {
                            OutputJack.this.mouseReleased((MouseEvent)e);
                            }
                        }
                    }, AWTEvent.MOUSE_EVENT_MASK);

                outgoing = new Wire(paintComponent);
                outgoing.setStart(OutputJack.this);
                mouseDown = true;
                }
                                        
            public void mouseDragged(MouseEvent e)
                {
                if (lastInputJack != null)
                    lastInputJack.highlight = false;
                
                InputJack input = findInputJackFor(e.getPoint());
                if (input != null)
                    {
                    lastInputJack = input;
                    input.highlight = true; 
                    }
                paintComponent.repaint();
                }
                                        
            public void mouseReleased(MouseEvent e)
                {
                OutputJack.this.mouseReleased(e);
                }
            
            /*
              public void mouseClicked(MouseEvent e)
              {
              if (outgoing != null) outgoing.chooseColor();
              }
            */
            };
            
        jack.addMouseMotionListener(mouseAdapter);
        jack.addMouseListener(mouseAdapter);
        }



    /** Attaches a Wire connected to the given InputJack. */
    public void attach(InputJack input, Wire wire)
        {
        outgoing = wire;
        if (wire != null) 
            {
            input.disconnect(wire);         // remove wire if it's already there just in case
            wire.setEnd(input);
            input.incoming.add(wire);
            addEdge();
            }
        }
           
           
    public void removeEdge()
        {
        Automaton.Node out = getButton().getNode();
        Automaton.Node in = outgoing.getEnd().getButton().getNode();
        Seq seq = ((AutomatonUI)(getButton().getOwner())).getSeq();
                
        seq.getLock().lock();
        try
            {
            out.setOut(number, null);
            }
        finally
            {
            seq.getLock().unlock();
            }
        }     
                
    public void addEdge()
        {
        Automaton.Node out = getButton().getNode();
        Automaton.Node in = outgoing.getEnd().getButton().getNode();
        Seq seq = ((AutomatonUI)(getButton().getOwner())).getSeq();
        Automaton automaton = (Automaton)(getButton().getOwner().getMotif());
                
        seq.getLock().lock();
        try
            {
            out.setOut(number, in);
            out.selectOut();
            
            ArrayList<Automaton.Node> nodes = automaton.getNodes();
            for(Automaton.Node node : nodes)
            	{
            	System.err.println();
            	System.err.println(node);
            	node.selectOut();
            	}
            
            }
        finally
            {
            seq.getLock().unlock();
            }
        }     
                
    public InputJack findInputJackFor(Point p)
        {
        Point p2 = SwingUtilities.convertPoint(OutputJack.this, p, paintComponent);
                
        // Find the input jack
        for(InputJack inputJack : automatonui.getInputJacks())
            {
            Point p3 = SwingUtilities.convertPoint(paintComponent, p2, inputJack);
            if (p3.getX() >= 0 && p3.getY() >= 0 &&
                p3.getX() < inputJack.getWidth() &&
                p3.getY() < inputJack.getHeight())              // inputJack.getBounds().contains(p3)) // got it
                {
                return inputJack;
                } 
            }
        return null;
        }
                
    public void mouseReleased(MouseEvent e)
        {
        if (!mouseDown) return;
                
        if (releaseListener != null)
            Toolkit.getDefaultToolkit().removeAWTEventListener(releaseListener);
        mouseDown = false;

        InputJack input = findInputJackFor(e.getPoint());

        if (input != null)
            {
            attach(input, temp);
            }
        else
            {
            //input.disconnect(outgoing);
            //disconnect();
            outgoing = null;
            }
                
        temp = null;
        if (lastInputJack != null)
            lastInputJack.highlight = false;
        lastInputJack = null;
                                                
        paintComponent.repaint();
        }
        
    public String toString()
        {
        return "OutputJack[name=" + title.getText() + ", number=" + number + ", panel=" + paintComponent + "]"; 
        }
    }
