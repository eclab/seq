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


public class MotifButton extends JToggleButton implements Transferable
    {
    public static final int ICON_WIDTH = 32;
    protected SeqUI sequi;
    protected MotifUI motifui;
    protected MotifUI owner;
    protected Object auxiliary;
    
    public static final String PLAYING_TEXT_COLOR = "red";
    public static final String PREHIGHLIGHT_TEXT_COLOR = "blue";
    public static final String DEFAULT_TEXT_COLOR = "black";
    public static final String MULTIPLE_PLAYING_TEXT_COLOR = "green";
    public static final String WARNING_TEXT_COLOR = "orange";
    public static final Color PLAYING_COLOR = Color.RED;
    public static final Color NEXT_COLOR = Color.BLUE;
    public static final Color REMOVE_COLOR = Color.MAGENTA;

	// DRAG COUNT
	// This counter should be increased with successive mouseDragged messages
	// and reset on mouseDown.  Its purpose is to prevent dragging from happening
	// too soon, in case it's just a sloppy click on the button.  Drags should
	// only be initiated when the dragCount exceeds MAX_DRAG_COUNT
    public static final int MAX_DRAG_COUNT = 4;
    protected int dragCount;
    
    public MotifUI getOwner() { return owner; }

    public void updateText()
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
                (root ? "<br><b>Root</b>" : "") +
                (owner == null ? "</html>" : ("<br>" + getSubtext() + "</html>"));
            setText(text);
            }
        }

    /** Returns whether we should color the primary text to reflect the fact that it is currently playing */
    public boolean shouldHighlight() { return true; }

    /** Returns whether we should color the primary text to reflect the fact that it is WAITING to play */
    public boolean shouldPreHighlight() { return false; }
        
    /** Returns any text that should appear below the primary text */
    public String getSubtext()
        {
        return "";              // default
        }
                

    /** Returns the underlying object associated with this button, if not a motifui. */
    public Object getAuxiliary() { return auxiliary; }
    /** Sets the underlying object associated with this button, if not a motifui. */
    public void setAuxiliary(Object obj) 
        { 
        auxiliary = obj; 
        if (motifui != null) motifui.updateText(); 
        }
        
    /** Returns the motifui associated with this button; can be null. */
    public MotifUI getMotifUI() { return motifui; }
    
    protected MotifButton()
        {
        super("");
        }
    
    
    public void buildIconAndText()
        {
        setMargin(new Insets(12, 12, 12, 12));                                  /// FIXME: Root Buttons still are a little taller than others
        Image image = ((ImageIcon)motifui.getIcon()).getImage().getScaledInstance(ICON_WIDTH, ICON_WIDTH, java.awt.Image.SCALE_SMOOTH); 
        setIcon(new ImageIcon(image));
        setIconTextGap(12);
        updateText();
        }
        
    public MotifButton(SeqUI sequi, MotifUI motifui, MotifUI owner)
        {
        super("");
        this.sequi = sequi;
        this.motifui = motifui;
        this.owner = owner;
        setHorizontalAlignment(SwingConstants.LEFT);
        if (owner != null) owner.addButton(this);
        setFocusPainted(false);
        buildIconAndText();
                
        addMouseListener(new MouseAdapter()
            {
            public void mouseClicked(MouseEvent e)
                {
                if (e.getClickCount() > 1)
                    {
                    doubleClick(e);
                    }
                }
                                
            public void mouseExited(MouseEvent e)
                {
                dragCount = 0;
                }
                                
            public void mouseReleased(MouseEvent e)
                {
                dragCount = 0;
                }
            });
        }
        
    public void disconnect()
        {
        if (motifui != null) motifui.removeButton(this);
        }
        
    public void doubleClick(MouseEvent e) { }

    public void updateList() { sequi.getMotifList().updateList(); }

    //// DRAG AND DROP
        
    /// Drag-and-drop data flavor
    public static DataFlavor dataFlavor = null;
    
    static
        {
        try
            {
            dataFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=seq.gui.MotifButton");
            }
        catch (ClassNotFoundException ex)
            {
            ex.printStackTrace();
            }
        }

    public Object getTransferData(DataFlavor flavor) 
        {
        if (flavor.equals(MotifButton.dataFlavor))
            return this;
        else
            return null;
        }
                
    public DataFlavor[] getTransferDataFlavors() 
        {
        return new DataFlavor[] { MotifButton.dataFlavor };
        }

    public boolean isDataFlavorSupported(DataFlavor flavor) 
        {
        // This is a stupid method
        return (flavor.equals(MotifButton.dataFlavor));
        }
    
    public TransferHandler buildTransferHandler() { return new MotifButtonTransferHandler(); }
    
    class MotifButtonTransferHandler extends TransferHandler implements DragSourceMotionListener 
        {
        public Transferable createTransferable(JComponent c) 
            {
            if (c instanceof MotifButton) 
                {
                return (Transferable) c;
                }
            else return null;
            }

        public int getSourceActions(JComponent c) 
            {
            if (c instanceof MotifButton) 
                {
                return TransferHandler.MOVE;
                }
            else return TransferHandler.NONE;
            }
                
        public Image getDragImage()
            {
            return ((ImageIcon)(MotifButton.this.getIcon())).getImage();
            }

        public void dragMouseMoved(DragSourceDragEvent dsde) {}
        } 
    
    public String toString() 
        {
        return this.getClass().getSimpleName() + "@" + System.identityHashCode(this) + "[MotifUI + " + motifui + " owner " + owner + "]";
        }
    }
