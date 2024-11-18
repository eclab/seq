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
    
    protected Seq seq; 
    protected SeqUI sequi;     
    Motif motif;
    JPanel console;
    JPanel footer;
    JPanel header;
    JTextArea text;
    MotifListButton primaryButton;
    ArrayList<MotifButton> buttons = new ArrayList<>();
        
    protected JScrollPane inspectorScroll = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    protected JScrollPane primaryScroll = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

    private ArrayList<MotifButton> getButtons() { return buttons; }
    
    public Seq getSeq() { return seq; }
    public SeqUI getSeqUI() { return sequi; }
    
    /** Override to provide a new JMenu for this MotifUI */
    public JMenu getMenu() { return null; }
        
    public MotifUI(Seq seq, SeqUI sequi, Motif motif)
        {
        this.seq = seq;
        this.sequi = sequi;
        this.motif = motif;
                
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
        primaryScroll.setDoubleBuffered(true);
        buildPrimary(primaryScroll);
        buildInspectors(inspectorScroll);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(primaryScroll, BorderLayout.CENTER);
        add(panel,BorderLayout.CENTER);
        header = new JPanel();
        header.setLayout(new BorderLayout());
        header.add(buildHeader(), BorderLayout.WEST);
        panel.add(header, BorderLayout.NORTH);
        footer = new JPanel();
        footer.setLayout(new BorderLayout());
        footer.add(buildConsole(), BorderLayout.CENTER);
        panel.add(footer, BorderLayout.SOUTH);
        
        String txt = "";
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            { 
            txt = motif.getText();
            }
        finally { lock.unlock(); }
        
        text = new JTextArea(txt);
        text.setToolTipText("Text notes about this motif");
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        JScrollPane textScroll = new JScrollPane(text);
        textScroll.setMinimumSize(new Dimension(0, 22));
        textScroll.setPreferredSize(textScroll.getMinimumSize());
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true,
            inspectorScroll, textScroll);
        split.setResizeWeight(1.0);     // on resizing, top gets everything
        split.setDividerLocation(-1);           // Honor the top area
        add(split, BorderLayout.EAST);
        buildMenu();
        }       
        
    public Motif getMotif() { return motif; }
        
    //// YOU MUST OVERRIDE...
        
    /** Make your own version of this static method.  Here is an example. 
        public static ImageIcon getStaticIcon() { return new ImageIcon(MotifUI.class.getResource("icons/series.png")); }
    */
    public static ImageIcon getStaticIcon() { return null; }

    /** Make your own version of this static method.  Here is an example: 
        public static String getType() { return "Series"; }
    */
    public static String getType() { return "Put Your Name Here, like Series or Step Sequence"; }

    /** Make your own version of this static method in your subclass.  Here is an example:
        public static MotifUI create(Seq seq, SeqUI ui) { return new SeriesUI(seq, ui, new Series(seq)); }
    */
    public static MotifUI create(Seq seq, SeqUI ui) { return null; }
        
    /** This method should set inspector objects to be the viewport view of the scroll pane */
    public abstract void buildInspectors(JScrollPane scroll);

    /** This method should set the viewport view of the primary scroll pane */
    public abstract void buildPrimary(JScrollPane scroll);
        
    /** This method should build but not install any menu.  The menu should be returned with getMenu(). */
    public void buildMenu() { }
    
    /** This method should return the "console".  This is the area BELOW the primary scroll pane.
        The default just fills it with an empty JPanel. */
    public JPanel buildConsole() { return new JPanel(); }
                        
    /** This method should return the "Header".  This is the area ABOVE the primary scroll pane, and is left-justified.
        The default just fills it with an empty JPanel. */
    public JPanel buildHeader() { return new JPanel(); }
                        
    /** This method should be implemented as { return getStaticIcon(); } */
    public abstract Icon getIcon();
        
    //// END YOU MUST OVERRIDE
                
        
    public void buttonSelected(MotifListButton button) { }
    
    public MotifListButton getPrimaryButton() { return primaryButton; }
    public void setPrimaryButton(MotifListButton button) { this.primaryButton = button; }
    public void addButton(MotifButton button) { buttons.add(button); button.updateList(); }
    public void removeButton(MotifButton button) { ArrayLists.removeFast(buttons, button); button.updateList(); }                        // O(n) search, O(1) remove
    public boolean isPlaying() { return motif.getPlayCount() > 0; }
    /*
      private void setPlaying(boolean playing) 
      {
      if (playing != this.playing)
      {
      this.playing = playing;
      updateText();
      }
      }
    */
    
    public Clip getDisplayClip()
        {
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
    public void disconnect()
        {
        for(MotifButton button : buttons)
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

    public void redraw() 
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
    
    /** Called to give the motifui a chance to revise its inspectors.  
    	Override this as you see fit, the default is empty.
    	FIXME: is this needed any more? */  
    public void revise()
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

    }
