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
import javax.swing.text.*;
import com.formdev.flatlaf.*;

public abstract class MotifUI extends JPanel
    {
    public static final int PARAMETER_LIST_NAME_DEFAULT_SIZE = 8;
    public static final int INSPECTOR_NAME_DEFAULT_SIZE = 8;
    public static final Color BACKGROUND = new Color(128, 128, 128);
    public static final double MINIMUM_DIVIDER_LOCATION_WITH_TEXT = 0.3;
    
    protected Seq seq; 
    protected SeqUI sequi;     
    Motif motif;
    JPanel console;
    JPanel footer;
    JPanel header;
    JTextArea text;
    JSplitPane split;
    MotifListButton primaryButton;
    ArrayList<MotifButton> buttons = new ArrayList<>();                 // These are in ANY ORDER
        
    protected JScrollPane inspectorScroll = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    protected JScrollPane primaryScroll = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

    public ArrayList<MotifButton> getButtons() { return buttons; }
    
    public Seq getSeq() { return seq; }
    public SeqUI getSeqUI() { return sequi; }
    
    /** Override to provide a new JMenu for this MotifUI */
    public JMenu getMenu() { return null; }
        
    public MotifUI(Seq seq, SeqUI sequi, Motif motif)
        {
        this.seq = seq;
        this.sequi = sequi;
        this.motif = motif;
        setOpaque(false);
        setBackground(Color.ORANGE);
                
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            { 
            // Determine name
            if (motif.getName() == null ||
                motif.getName().trim().equals(""))
                motif.setName(motif.getBaseName() + " " + motif.getNextCounter());
            }
        finally { lock.unlock(); }
        }
        
    public static final Font SMALL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 10);

    boolean built = false;
    
    public void buildUI()
        {
        if (!built) { build(); }
        built = true;
        }
        
    public boolean isUIBuilt()
        {
        return built;
        }

    protected void build()
        {      
        setLayout(new BorderLayout());  
        inspectorScroll.setBorder(null);
        JComponent primary = buildPrimary();
        buildInspectors(inspectorScroll);
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setBackground(Color.BLUE);
        panel.setLayout(new BorderLayout());
        panel.add(primary, BorderLayout.CENTER);
        add(panel,BorderLayout.CENTER);
        header = buildHeader();
        if (header != null) panel.add(header, BorderLayout.NORTH);
        footer = buildConsole();
        if (footer != null) panel.add(footer, BorderLayout.SOUTH);
        
        String txt = "";
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            { 
            txt = motif.getText();
            }
        finally { lock.unlock(); }
        
        text = new JTextArea(txt);
        text.setToolTipText(TEXT_NOTES_TOOLTIP);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        JScrollPane textScroll = new JScrollPane(text);
        textScroll.setMinimumSize(new Dimension(0, 22));
        textScroll.setPreferredSize(textScroll.getMinimumSize());
        split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, inspectorScroll, textScroll);
        split.setResizeWeight(1.0);     // on resizing, top gets everything
        add(split, BorderLayout.EAST);
        buildMenu();
        }     
        
    public void displayNotes()
        {
        String txt = text.getText();
        if (txt != null && txt.length() > 0)
            {
            split.setDividerLocation((int)(sequi.getFrame().getContentPane().getSize().getHeight()) * 3 / 4);
            }
        else
            {
            split.setDividerLocation(-1);           // Honor the top area
            }
        }  
        
    public Motif getMotif() { return motif; }
        
    //// YOU MUST OVERRIDE...
        
    /** Make your own version of this static method.  Here is an example. 
        public static ImageIcon getStaticIcon() { return new ImageIcon(MotifUI.class.getResource("icons/series.png")); }
    */
    public static ImageIcon getStaticIcon() { return null; }

    /** Make your own version of this static method.  Here is an example: 
        public static String getType() { return "Series"; }
        This is used to build the Motif list menus.
    */
    public static String getType() { return "Put Your Name Here, like Series or Step Sequence"; }

    /** Make your own version of this static method in your subclass.  Here is an example:
        public static MotifUI create(Seq seq, SeqUI ui) { return new SeriesUI(seq, ui, new Series(seq)); }
    */
    public static MotifUI create(Seq seq, SeqUI ui) { return null; }
        
    /** This method should set inspector objects to be the viewport view of the scroll pane */
    public abstract void buildInspectors(JScrollPane scroll);

    /** This method should set the viewport view of the primary scroll pane.  Alternatively, override buildPrimary(). */
    public void buildPrimary(JScrollPane scroll) { }
    
    /** This creates a primary JComponent and returns it.  If your primary JComponent is just the primaryScroll, you should instead override buildPrimary(scroll); 
        You can still get the primary scroll, to use it somewhere, as getPrimaryScroll(). */
    public JComponent buildPrimary()
        {
        buildPrimary(primaryScroll);
        return primaryScroll;
        }
        
    /** This method should build but not install any menu.  The menu should be returned with getMenu(). */
    public void buildMenu() { }
    
    /** This method should return the "console".  This is the area BELOW the primary scroll pane.
        The default just return null. */
    public JPanel buildConsole() { return null; }
                        
    /** This method should return the "Header".  This is the area ABOVE the primary scroll pane, and is left-justified.
        The default just returns null. */
    public JPanel buildHeader() { return null; }
                        
    /** This method should be implemented as { return getStaticIcon(); } */
    public abstract Icon getIcon();
        
    //// END YOU MUST OVERRIDE
                
        
    public void buttonSelected(MotifListButton button) { }
    
    public MotifListButton getPrimaryButton() { return primaryButton; }
    public void setPrimaryButton(MotifListButton button) { this.primaryButton = button; }
    public void addButton(MotifButton button) { buttons.add(button); button.updateList(); }
    public void removeButton(MotifButton button) { buttons.remove(button); button.updateList(); }
    public boolean isPlaying() 
        { 
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            { 
            return motif.getPlayCount() > 0; 
            }
        finally { lock.unlock(); }
        }
    
    public Clip getDisplayClip()
        {
        // If I'm not being displayed right now, I do not have a display clip
        if (sequi.getMotifUI() != this) return null;
        
        // Okay, I'm being displayed.  So which clip of mine is currently playing if any?
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            { 
            return motif.getPlayingClip();
            }
        finally { lock.unlock(); }
        }
        
    /** Updates for all buttons */
    public void updateText()
        {
        if (primaryButton != null) 
            {
            primaryButton.updateText();
            }
            
        for(MotifButton button : buttons)
            {
            button.updateText();
            }
        }
                
    /** Called by the MotifList when the motif is being removed entirely from the
        sequence.  This is called IMMEDIATELY BEFORE the motif is removed. 
        If you override this, be sure to call super.disconnect().  */     
    public void disconnectButtons()
        {
        // copy buttons2 because the buttons will be removing themselves from me
        ArrayList<MotifButton> buttons2 = new ArrayList<>(buttons);
        
        for(MotifButton button : buttons2)
            {
            button.disconnect();
            }
        }
        
    public boolean shouldHighlight(MotifButton button) { return true; }
        
    public String getSubtext(MotifButton button)
        {
        return "";              // default
        }
                
    public JScrollPane getPrimaryScroll() { return primaryScroll; }

    public void redraw(boolean inResponseToStep) 
        { 
        primaryScroll.repaint(); 
        }
    
    /** Called by the SeqUI to let the MotifUI know that it is now being displayed.  
        If you override this, be sure to call super().   */
    public void uiWasSet() 
        {
        buildUI(); 
        }
                
    /** Called by the SeqUI to let the MotifUI know that it is no longer being displayed.
        If you override this, be sure to call super().   This is called even if the UI hasn't
        been built, so you might check that. */
    public void uiWasUnset() 
        { 
        if (!isUIBuilt()) return;
        
        ReentrantLock lock = seq.getLock();
        Document doc = text.getDocument();
        try
            {
            String txt = doc.getText(0, doc.getLength());
            lock.lock();
            try 
                { 
                motif.setText(txt);
                }
            catch (Exception ex) { }
            finally { lock.unlock(); }
            }
        catch (javax.swing.text.BadLocationException e)
            {
            // do nothing
            }
        }
        
    /** Called by the SeqUI to let the MotifUI know that it is being saved out.
        If you override this, be sure to call super(). */
    public void isSaving()
        {
        if (isUIBuilt())
            {
            ReentrantLock lock = seq.getLock();
            Document doc = text.getDocument();
            try
                {
                String txt = doc.getText(0, doc.getLength());
                lock.lock();
                try
                    {
                    motif.setText(txt);
                    }
                catch (Exception ex) { }
                finally { lock.unlock(); }
                }
            catch (javax.swing.text.BadLocationException e)
                {
                // do nothing
                }
            }
        }

    public void frameCreated()
        {
        }
    
    public void recursiveDragError(MotifButton dropped, SeqUI sequi)
        {
        sequi.showSimpleError("Cannot Drag", 
            dropped.getMotifUI().getMotif() == getMotif() ?
            "You cannot copy/move an item into itself." :
            "You cannot copy/move an item into a descendant of itself.");
        System.err.println("Error: Can't add " + dropped.getMotifUI().getMotif() + " into " + getMotif() + " as it's contained within it already ");
        }    

    /** Called when we have stopped.  This allows armed Motif UIs to revise themselves. */
    public void stopped() { }

    int rebuildCount = 0;
        
    /** Rebuilds inspectors already built. */        
    public void rebuildInspectors(int count) { if (count > rebuildCount) { buildInspectors(inspectorScroll); rebuildCount = count; }}

    public String toString() 
        {
        return this.getClass().getSimpleName() + "@" + System.identityHashCode(this);
        }

/** A useful method for detecting if SHIFT is held or the right mouse button is clicked */
    public static boolean shiftOrRightMouseButton(MouseEvent evt)
        {
        return ((evt.getModifiers() & InputEvent.SHIFT_MASK) == InputEvent.SHIFT_MASK) ||
            SwingUtilities.isRightMouseButton(evt);
        }
        
/** A useful method for detecting if ALT/OPTION is held or the center mouse button is clicked */
    public static boolean optionOrMiddleMouseButton(MouseEvent evt)
        {
        return ((evt.getModifiers() & InputEvent.ALT_MASK) == InputEvent.ALT_MASK) ||
            SwingUtilities.isMiddleMouseButton(evt);
        }

/** A hook called on the OLD MotifUI prior to doing a push: you can override this method
    to prepare information to restore after the undo or redo (see NotesUI as an example). */
    public void prePush() { }

/** A hook called on the NEW MotifUI after doing an undo or redo: you can override this method
    to restore information after the undo or redo (see NotesUI as an example). */
    public void postUndoOrRedo(MotifUI oldMotifUI) { }


    /** Returns an identifier for the underlying Motif.  This identifier is unique
        between Motifs except for Motifs that were copied from others.
        Used in undo and redo mostly. */
    public int getTag() 
        { 
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            { 
            return motif.getTag();
            }
        finally 
            { 
            lock.unlock(); 
            }
        }

    static final String TEXT_NOTES_TOOLTIP = "<html><b>Notes</b><br>" +
        "Add notes about this motif.</html>";
    }
