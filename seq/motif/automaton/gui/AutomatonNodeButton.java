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
    }
