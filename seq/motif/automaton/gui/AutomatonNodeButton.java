/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.automaton.gui;

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

// For Drag and Drop
import java.awt.dnd.*;
import java.awt.datatransfer.*;


public class AutomatonNodeButton extends JButton implements Transferable
    {
    public static final int ICON_WIDTH = 32;
    
    public static final int TYPE_DELAY = 0;
    public static final int TYPE_CHORD = 1;
    public static final int TYPE_FINISHED = 2;
    public static final int TYPE_ITERATE = 3;
    public static final int TYPE_RANDOM = 4;
    public static final int TYPE_FORK = 5;
    public static final int TYPE_JOIN = 6;
    
    public static ImageIcon buildIcon(String file)
        {
        ImageIcon original = new ImageIcon(seq.gui.Transport.class.getResource(file));
        return new ImageIcon(original.getImage().getScaledInstance(ICON_WIDTH, ICON_WIDTH, java.awt.Image.SCALE_SMOOTH));
        }
        
    static final ImageIcon delayIcon = buildIcon("icons/delay.png");
    static final ImageIcon chordIcon = buildIcon("icons/chord.png");
    static final ImageIcon finishedIcon = buildIcon("icons/finished.png");
    static final ImageIcon iterateIcon = buildIcon("icons/iterate.png");
    static final ImageIcon randomIcon = buildIcon("icons/random.png");
    static final ImageIcon forkIcon = buildIcon("icons/fork.png");
    static final ImageIcon joinIcon = buildIcon("icons/join.png");
    static final ImageIcon[] icons = { delayIcon, chordIcon, finishedIcon, iterateIcon, randomIcon, forkIcon, joinIcon };
    static final String[] text = { "Delay", "Chord", "Finished", "Iterate", "Random", "Fork", "Join" };

    public int type;
    
    public int getType() { return type; }

    public AutomatonNodeButton(int type, AutomatonNodeButton thisSize)
    	{
    	this(type);
    	setPreferredSize(thisSize.getPreferredSize());
    	setMinimumSize(thisSize.getMinimumSize());
    	setToolTipText(TOOLTIPS[type]);
    	}

    public AutomatonNodeButton(int type)
        {
        this.type = type;
        setHorizontalAlignment(SwingConstants.CENTER);
        setVerticalTextPosition(SwingConstants.BOTTOM);
        setHorizontalTextPosition(SwingConstants.CENTER);
        setMargin(new Insets(6, 6, 6, 6));                                  /// FIXME: Root Buttons still are a little taller than others
        setFocusPainted(false);
        Image image = icons[type].getImage().getScaledInstance(ICON_WIDTH, ICON_WIDTH, java.awt.Image.SCALE_SMOOTH); 
        setIcon(new ImageIcon(image));
        setIconTextGap(6);
        setText(text[type]);
    	setToolTipText(TOOLTIPS[type]);
                
        addMouseMotionListener(new MouseMotionAdapter()
            {
            public void mouseDragged(MouseEvent e)
                {
                if (AutomatonNodeButton.this.isEnabled())
                    {
                    getTransferHandler().exportAsDrag(AutomatonNodeButton.this, e, TransferHandler.MOVE);
                    }
                }
            });
 
        setTransferHandler(buildTransferHandler());
        }
        
    //// DRAG AND DROP
        
    /// Drag-and-drop data flavor
    public static DataFlavor dataFlavor = null;
    
    static
        {
        try
            {
            dataFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=seq.motif.automaton.gui.AutomatonNodeButton");
            }
        catch (ClassNotFoundException ex)
            {
            ex.printStackTrace();
            }
        }

    public Object getTransferData(DataFlavor flavor) 
        {
        if (flavor.equals(AutomatonNodeButton.dataFlavor))
            return this;
        else
            return null;
        }
                
    public DataFlavor[] getTransferDataFlavors() 
        {
        return new DataFlavor[] { AutomatonNodeButton.dataFlavor };
        }

    public boolean isDataFlavorSupported(DataFlavor flavor) 
        {
        // This is a stupid method
        return (flavor.equals(AutomatonNodeButton.dataFlavor));
        }
    
    public TransferHandler buildTransferHandler() { return new AutomatonNodeButtonTransferHandler(); }
    
    class AutomatonNodeButtonTransferHandler extends TransferHandler implements DragSourceMotionListener 
        {
        public Transferable createTransferable(JComponent c) 
            {
            if (c instanceof AutomatonNodeButton) 
                {
                return (Transferable) c;
                }
            else return null;
            }

        public int getSourceActions(JComponent c) 
            {
            if (c instanceof AutomatonNodeButton) 
                {
                return TransferHandler.MOVE;
                }
            else return TransferHandler.NONE;
            }
                
        public Image getDragImage()
            {
            return ((ImageIcon)(AutomatonNodeButton.this.getIcon())).getImage();
            }

        public void dragMouseMoved(DragSourceDragEvent dsde) {}
        } 


	static final String DELAY_BUTTON_TOOLTIP = "<html><b>Delay Button</b><br>" +
		"Drag from this button into the grid to create a Delay Node.<br><br>" +
		"This represents a time delay.  When the automaton transitions to this node, it waits for<br>" +
		"some amount of time before transitioning to the next node connected to the output.</html>";

	static final String CHORD_BUTTON_TOOLTIP = "<html><b>Chord Button</b><br>" +
		"Drag from this button into the grid to create a Chord Node.<br><br>" +
		"This represents a chord, interval, or single note.  When the automaton transitions to this node, it plays<br>" +
		"the chord for the specified amount of time before transitioning to the next node connected to the output.</html>";

	static final String FINISHED_BUTTON_TOOLTIP = "<html><b>Finished Button</b><br>" +
		"Drag from this button into the grid to create a Finished Node.<br><br>" +
		"This node tells the automaton's parent motifs that the automaton believes it has finished playing.</html>";

	static final String ITERATE_BUTTON_TOOLTIP = "<html><b>Iterate Button</b><br>" +
		"Drag from this button into the grid to create an Iterate Node.<br><br>" +
		"This node iterates through up to four outputs, transitioning out different ones each time<br>" +
		"the automaton transitions to it. Iterations works as follows:" + 
		"<ol>" +
		"<li>When first played, we immediately transition to the first output that is connected to us." +
		"<li>Each time we are played, we continue transiting out that input until we have done so<br>" +
		"<b>Iterations</b> total times. Thereafter we continue to the next output that is connected to us.<br>" +
		"<li>When we have finished with all our connected outputs, if <b>Loop</b> is checked, then we<br>" +
		"loop back and continue again with our first connectd output. Else we stop transitioning entirely.</html>";

	static final String RANDOM_BUTTON_TOOLTIP = "<html><b>Random Button</b><br>" +
		"Drag from this button into the grid to create a Random Node.<br><br>" +
		"This node transitions out a random output each time the automaton transitions to it.<br>" +
		"This works as follows.  When played, we take all the connected outputs and normalize<br>" + 
		"their weights into probabilities (divide them by their sum).  If they're all 0, they're<br>" +
		"all treated as equal probability.  Then we select an output at random according to the probabilities<br>" +
		"and transition to that output.  If no outputs are connected, we don't transition at all.</html>";

	static final String FORK_BUTTON_TOOLTIP = "<html><b>Fork Button</b><br>" +
		"Drag from this button into the grid to create a Fork Node.<br><br>" +
		"This node creates one or more playing threads each time the automaton transitions to it.<br>" +
		"These threads output in parallel along separate outputs and continue playing and transitioning.<br>" +
		"in parallel.  You can create no more than 8 threads at a time: further threads are ignored.<br>" +
		"You an eliminate threads by <b>Joining</b> them with one another.</html>";

	static final String JOIN_BUTTON_TOOLTIP = "<html><b>Join Button</b><br>" +
		"Drag from this button into the grid to create a Join Node.<br><br>" +
		"This node joins one or more playing threads each time the automaton transitions to it.<br>" +
		"Joined threads are collapsed to a single thread, which is then transitioned along the output.<br>" +
		"When a thread arrives at the input, Join may do nothing yet: it will wait until the right<br>" +
		"number of threads have arrived before joining them (see <b>Threads</b> in the inspector at right).</html>";

	public static final String[] TOOLTIPS = { DELAY_BUTTON_TOOLTIP, CHORD_BUTTON_TOOLTIP, FINISHED_BUTTON_TOOLTIP, ITERATE_BUTTON_TOOLTIP, RANDOM_BUTTON_TOOLTIP, FORK_BUTTON_TOOLTIP, JOIN_BUTTON_TOOLTIP };

    }
