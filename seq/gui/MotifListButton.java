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

// For Drag and Drop
import java.awt.dnd.*;
import java.awt.datatransfer.*;


public class MotifListButton extends MotifButton
    {
    boolean compressed = false;
    
    public void buildIconAndText()
        {
        if (sequi.getMotifList().isCompressed())
            {
            setMargin(new Insets(6, 6, 6, 6));                                  /// FIXME: Root Buttons still are a little taller than others
            Image image = ((ImageIcon)motifui.getIcon()).getImage().getScaledInstance(ICON_WIDTH / 2, ICON_WIDTH / 2, java.awt.Image.SCALE_SMOOTH); 
            setIcon(new ImageIcon(image));
            setIconTextGap(12);
            updateText();
            }
        else super.buildIconAndText();
        }

    public void updateText()
        {
        if (sequi.getMotifList().isCompressed())
            {
            String name = null;
                
            if (motifui != null)
                {
                Motif motif = motifui.getMotif();
                ReentrantLock lock = sequi.getSeq().getLock();
                lock.lock();
                try 
                    { 
                    name = motif.getDisplayedName();
                    }
                finally { lock.unlock(); }
                name = StringUtility.sanitize(name);
                boolean root = (this instanceof MotifListButton && sequi.getMotifList().getRoot() == this);
                String text = 
                    "<html><font color=" + 
                    (motifui.isPlaying() && shouldHighlight() ? PLAYING_TEXT_COLOR : 
                    (shouldPreHighlight() ? PREHIGHLIGHT_TEXT_COLOR : DEFAULT_TEXT_COLOR)) + ">" +
                    (root ? "<b>" + name + "</b>" : name) +
                    "</font>" + 
                    (owner == null ? "</html>" : ("<br>" + getSubtext() + "</html>"));
                setText(text);
                }
            }
        else super.updateText();
        }

    /// Drag-and-drop data flavor
    public static DataFlavor dataFlavor = null;
    
    static
        {
        try
            {
            dataFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=seq.gui.MotifListButton");
            }
        catch (ClassNotFoundException ex)
            {
            ex.printStackTrace();
            }
        }

    public Object getTransferData(DataFlavor flavor) 
        {
        if (flavor.equals(MotifListButton.dataFlavor))
            return this;
        else return super.getTransferData(flavor);
        }
                
    public DataFlavor[] getTransferDataFlavors() 
        {
        return new DataFlavor[] { MotifButton.dataFlavor, MotifListButton.dataFlavor };
        }

    public boolean isDataFlavorSupported(DataFlavor flavor) 
        {
        // This is a stupid method
        return (flavor.equals(MotifListButton.dataFlavor) || super.isDataFlavorSupported(flavor));
        }
    
    public MotifListButton(SeqUI sequi, MotifUI motifui, MotifUI owner)
        {
        super(sequi, motifui, owner);
        
        addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                dragCount = 0;
                sequi.setMotifUI(motifui);
                }
            });
            
        /*
          addMouseListener(new MouseAdapter()
          {
          public void mouseReleased(MouseEvent e)
          {
          if (e.getClickCount() >= 2)
          {
          sequi.getMotifList().doRoot(MotifListButton.this.motifui);
          }
          }
          });
        */

        addMouseMotionListener(new MouseMotionAdapter()
            {
            public void mouseDragged(MouseEvent e)
                {
                dragCount++;
                if (dragCount == MAX_DRAG_COUNT)
                    getTransferHandler().exportAsDrag(MotifListButton.this, e, TransferHandler.MOVE);
                }
            });
            
        setTransferHandler(buildTransferHandler());
        }
    }
        
