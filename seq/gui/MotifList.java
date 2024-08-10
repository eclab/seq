/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.gui;

import seq.engine.*;
import seq.motif.macro.*;
import seq.motif.macro.gui.*;
import java.io.*;
import java.util.zip.*;
import java.awt.*;
import javax.swing.border.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.concurrent.locks.*;
import java.util.*;
import com.formdev.flatlaf.*;
import org.json.*;

// For Drag and Drop
import java.awt.dnd.*;
import java.awt.datatransfer.*;


public class MotifList extends JPanel
    {
    ArrayList<MotifUI> motifuis;
    ArrayList<MotifListButton> buttons;
        
    Box list;
    JScrollPane scroll;
    JPanel console;
    
    PushButton backButton;
    PushButton forwardButton;
    PushButton addButton;
    PushButton removeButton;
    PushButton copyButton;
        
    MotifListButton root = null;
    ArrayList<MotifListButton> backStack = new ArrayList<>();
    ArrayList<MotifListButton> forwardStack = new ArrayList<>();
    MotifListButton selectedButton = null;
    
    boolean compressed = true;
    Seq seq;
    SeqUI sequi;
    
    public static final Color PARENT_COLOR = new Color(0xE0, 0xE0, 0xF0);
    public static final Color CHILD_COLOR = new Color(0xF0, 0xE0, 0xE0);
    // public static final Color ANCESTOR_COLOR = new Color(0xD4, 0xDF, 0xE9);
    // public static final Color DESCENDANT_COLOR = new Color(0xED, 0xDA, 0xDA);
    
    public boolean isCompressed() { return compressed ; }
    public void setCompressed(boolean val) 
        { 
        compressed = val; 
        for(MotifListButton button : buttons)
            {
            button.buildIconAndText();
            button.revalidate();
            button.repaint();
            }
        }
        
    public void reverse()
        {
        list.removeAll();
        ArrayList<MotifListButton> newButtons = new ArrayList<>();
        ArrayList<MotifUI> newUIs = new ArrayList<>();
        
        for(int i = buttons.size() - 1; i >= 0; i--)
            {
            newButtons.add(buttons.get(i));
            newUIs.add(motifuis.get(i));
            list.add(buttons.get(i));
            }
        motifuis = newUIs;
        buttons = newButtons;
        list.revalidate();
        list.repaint();
        }
        
    public void sort()
        {
        ArrayList<Motif> original = new ArrayList<>();
        HashMap<Motif, Integer> map = new HashMap<>();
        
        for(int i = 0; i < motifuis.size(); i++)
            {
            Motif motif = motifuis.get(i).getMotif();
            original.add(motif);
            map.put(motif, Integer.valueOf(i));
            }
                
        ArrayList<Motif> sorted = Motif.topologicalSort(original);
        
        ArrayList<MotifUI> newMotifUIs = new ArrayList<>();
        ArrayList<MotifListButton> newButtons = new ArrayList<>();
        
        for(Motif motif : sorted)
            {
            Integer in = (Integer)map.get(motif);
            if (in == null) // this can happen because some motifs have a fixed number of children with Blanks filling the NaN slot
                continue;
            
            int _in = in.intValue();
            newMotifUIs.add(motifuis.get(_in));
            newButtons.add(buttons.get(_in));
            }
                
        motifuis = newMotifUIs;
        buttons = newButtons;
        list.removeAll();
        for(MotifListButton button : buttons)
            {
            list.add(button);
            }
        list.revalidate();
        list.repaint();
        }
        
        
    public static final Class[] MOTIF_UIS = 
        { 
        seq.motif.stepsequence.gui.StepSequenceUI.class,
        seq.motif.notes.gui.NotesUI.class,
        seq.motif.select.gui.SelectUI.class,
        seq.motif.series.gui.SeriesUI.class,
        seq.motif.parallel.gui.ParallelUI.class,
        seq.motif.automaton.gui.AutomatonUI.class,
        seq.motif.silence.gui.SilenceUI.class,
        seq.motif.macro.gui.MacroChildUI.class,
        seq.motif.macro.gui.MacroUI.class,
        };
    
    public static final Class[] MOTIFS = 
        { 
        seq.motif.stepsequence.StepSequence.class,
        seq.motif.notes.Notes.class,
        seq.motif.select.Select.class,
        seq.motif.series.Series.class,
        seq.motif.parallel.Parallel.class,
        seq.motif.automaton.Automaton.class,
        seq.motif.silence.Silence.class,
        seq.motif.macro.MacroChild.class,
        seq.motif.macro.Macro.class,
        };

    public MotifListButton getRoot() { return root; }
    public void setRoot(MotifListButton button) 
        { 
        if (seq != null) seq.stop();
        
//        seq.getRoot().terminate();
        seq.setData(button.getMotifUI().getMotif());    // we're stopped, so data should get set now
//        seq.getData().setPlayingClip(seq.getRoot());
        root = button;
        
        for(MotifUI ui : motifuis)
            {
            ui.updateText();
            }
        }

    public void setRoot(int button) 
        { 
        setRoot(buttons.get(button));
        }
        
    public void setRoot(MotifUI motifUI) 
        { 
        for(int i = 0; i < motifuis.size(); i++)
            {
            if (motifuis.get(i) == motifUI) // got it
                {
                setRoot(i);
                break;
                }
            }
        }
        
    public void rebuildClipsForMotif(Motif motif)
        {
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            { 
            /// FIXME: This could be slow!  Break the lock up?
            seq.getRoot().rebuild(motif);
            }
        finally { lock.unlock(); }
        // Assuming the motif is the selected motif, we just added / deleted stuff,
        // necessitating rebuilding the clips, so we might as well update ancestry here
        if (selectedButton != null) updateList(selectedButton);
        }
    
    public ArrayList<MotifUI> getMotifUIs() { return motifuis; }
    
    public MotifUI getOrAddMotifUIFor(Motif motif) 
        {
        MotifUI ui = getMotifUIFor(motif);
        if (ui == null)
            {
            ui = buildMotifUIFor(motif);
            if (ui == null) throw new RuntimeException("Null MotifUI on building for " + motif);
        
            doAdd(ui);
            }
        
        return ui;
        }
    

    public MotifUI getMotifUIFor(Motif motif) 
        {
        for(MotifUI ui : motifuis)
            {
            if (ui.getMotif() == motif) 
                {
                return ui;
                }
            }
        return null;
        }
    
    public MotifUI buildMotifUIFor(Motif motif)
        {
        try
            {
            for(int i = 0; i < MOTIFS.length; i++)
                {
                if (MOTIFS[i] == motif.getClass())
                    {
                    return (MotifUI)(MOTIF_UIS[i].getMethod("create", Seq.class, SeqUI.class, Motif.class).invoke(null, seq, sequi, motif));
                    }
                }
            }
        catch (Exception ex) { ex.printStackTrace(); }
        return null;
        }
        
    public MotifList(Seq seq, SeqUI sequi)
        {
        this.seq = seq;
        this.sequi = sequi;
          
        motifuis = new ArrayList<MotifUI>();
        buttons = new ArrayList<MotifListButton>();
        list = new Box(BoxLayout.Y_AXIS);
        scroll = new JScrollPane(list, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setViewportView(list);

        setLayout(new BorderLayout());
        add(scroll, BorderLayout.CENTER);
        console = new JPanel();
        console.setLayout(new BorderLayout());
        add(console, BorderLayout.SOUTH);

        JMenuItem[] items = new JMenuItem[MOTIF_UIS.length];
        try
            {
            for(int i = 0; i < items.length; i++)
                {
                final int _i = i;
                                
                ImageIcon icon = (ImageIcon)(MOTIF_UIS[i].getMethod("getStaticIcon").invoke(null));
                Image image = icon.getImage().getScaledInstance(32, 32,  java.awt.Image.SCALE_SMOOTH); 

                items[i] = new JMenuItem((String)(MOTIF_UIS[i].getMethod("getType").invoke(null)), new ImageIcon(image));
                items[i].setHorizontalAlignment(SwingConstants.LEADING);
                items[i].addActionListener(new ActionListener()
                    {
                    public void actionPerformed(ActionEvent e)      
                        {
                        doAdd(_i);
                        }       
                    });     
                }
            }
        catch (Exception ex) { ex.printStackTrace(); }
                
        addButton = new PushButton(new StretchIcon(PushButton.class.getResource("icons/plus.png")), items);
        addButton.setPopsUpAbove(true);
        addButton.getButton().setPreferredSize(new Dimension(24, 24));

        removeButton = new PushButton(new StretchIcon(PushButton.class.getResource("icons/minus.png")))
            {
            public void perform()
                {
                doRemove();
                }
            };
        removeButton.getButton().setPreferredSize(new Dimension(24, 24));
                
        backButton = new PushButton(new StretchIcon(PushButton.class.getResource("icons/backward.png")))
            {
            public void perform()
                {
                doBack();
                }
            };
        backButton.getButton().setPreferredSize(new Dimension(24, 24));

        forwardButton = new PushButton(new StretchIcon(PushButton.class.getResource("icons/forward.png")))
            {
            public void perform()
                {
                doForward();
                }
            };
        forwardButton.getButton().setPreferredSize(new Dimension(24, 24));

        copyButton = new PushButton(new StretchIcon(PushButton.class.getResource("icons/copy.png")))
            {
            public void perform()
                {
                doCopy();
                }
            };
        copyButton.getButton().setPreferredSize(new Dimension(24, 24));

/*
  rootButton = new PushButton("Root")
  {
  public void perform()
  {
  doRoot();
  }
  };
*/

        Box box = new Box(BoxLayout.X_AXIS);
        box.add(addButton);
        box.add(removeButton);
        box.add(copyButton);
//        box.add(rootButton);
        console.add(box, BorderLayout.WEST);

        box = new Box(BoxLayout.X_AXIS);
        box.add(backButton);
        box.add(forwardButton);
        console.add(box, BorderLayout.EAST);
        backButton.setEnabled(false);
        forwardButton.setEnabled(false);

        list.setDropTarget(new DropTarget(this, buildDropTargetListener()));
        
        setCompressed(sequi.getSmallButtons());
        }
    
    public void doBack()
        {
        if (selectedButton != null) forwardStack.add(selectedButton);
        
        while(true)
            {
            selectedButton = backStack.remove(backStack.size() - 1);
            if (buttons.contains(selectedButton)) break;
            else selectedButton = null;
            }
        if (selectedButton == null)
            {
            while(true)
                {
                selectedButton = forwardStack.remove(forwardStack.size() - 1);
                if (buttons.contains(selectedButton)) break;
                else selectedButton = null;
                }
            }
        // FIXME: selectedButton better not be null now!

        sequi.setMotifUI(selectedButton.motifui, false);
        backButton.setEnabled(!backStack.isEmpty());
        forwardButton.setEnabled(!forwardStack.isEmpty());
        }

    public void doForward()
        {
        if (selectedButton != null) backStack.add(selectedButton);
        
        while(true)
            {
            selectedButton = forwardStack.remove(forwardStack.size() - 1);
            if (buttons.contains(selectedButton)) break;
            else selectedButton = null;
            }
        if (selectedButton == null)
            {
            while(true)
                {
                selectedButton = backStack.remove(backStack.size() - 1);
                if (buttons.contains(selectedButton)) break;
                else selectedButton = null;
                }
            }
        // FIXME: selectedButton better not be null now!

        sequi.setMotifUI(selectedButton.motifui, false);
        backButton.setEnabled(!backStack.isEmpty());
        forwardButton.setEnabled(!forwardStack.isEmpty());
        }
        
    public void select(MotifListButton button)
        {
        select(button, true);
        }
        
        
    public void updateList()
        {
        if (selectedButton != null) updateList(selectedButton);
        }
        
    void updateList(MotifListButton button)
        {
        HashSet<Motif> children = new HashSet(button.getMotifUI().getMotif().getChildrenAsMotifs());
        HashSet<Motif> descendants = new HashSet(button.getMotifUI().getMotif().getDescendants());
        HashSet<Motif> ancestors = new HashSet(button.getMotifUI().getMotif().getAncestors());
        HashSet<Motif> parents = new HashSet(button.getMotifUI().getMotif().getParents());
        for(MotifListButton b : buttons)
            {
            b.setSelected(false);
            Motif m = b.getMotifUI().getMotif();
            if (parents.contains(m))
                {
                b.setBackground(PARENT_COLOR);
                }
            else if (children.contains(m))
                {
                b.setBackground(CHILD_COLOR);
                }
            /*
              else if (ancestors.contains(m))
              {
              b.setBackground(ANCESTOR_COLOR);
              }
              else if (descendants.contains(m))
              {
              b.setBackground(DESCENDANT_COLOR);
              }
            */
            else
                {
                b.setBackground(null);
                }
            }
        button.setSelected(true);
        }

    void select(MotifListButton button, boolean updateStacks)
        {
        updateList(button);
        if (selectedButton != button)
            {
            if (selectedButton != null && updateStacks) 
                {
                if (backStack.isEmpty() ||
                    backStack.get(backStack.size() - 1) != selectedButton)
                    {
                    backStack.add(selectedButton);
                    }
                }
            if (updateStacks) forwardStack.clear();
            selectedButton = button;
            
            if (sequi.getSelectedFrameIsRoot())
                {
                ReentrantLock lock = seq.getLock();
                lock.lock();
                try
                    {
                    if (seq.isStopped())
                        {
                        setRoot(button);
                        }
                    }
                finally { lock.unlock(); }
                }

            backButton.setEnabled(!backStack.isEmpty());
            forwardButton.setEnabled(!forwardStack.isEmpty());
            }
        }
    
    public void doCopy()
        {
        if (selectedButton == null) // uh oh
            return;                                         // FIXME this should not happen!
        
        Motif original = selectedButton.getMotifUI().getMotif();
        Motif motif = null;
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            { 
            motif = original.copy();
            // name?
            if (motif.getName().equals(original.getName())) // it can't be null, see MotifUI constructor
                {
                motif.setName(motif.getName() + " copy");
                }
            }
        finally { lock.unlock(); }

        MotifUI copy = buildMotifUIFor(motif);
        if (copy == null) return;       // uh oh

        seq.push();        
        lock = seq.getLock();
        lock.lock();
        try { seq.addMotif(copy.getMotif()); }
        finally { lock.unlock(); }

        motifuis.add(copy);
        sequi.setMotifUI(copy);
        MotifListButton button = new MotifListButton(sequi, copy, null);
        buttons.add(button);
        copy.setPrimaryButton(button);
        select(button);
        list.add(button);
        list.revalidate();
        list.repaint();
        scroll.revalidate();
        }
    
    
    
    /** This is called when we add a new MotifUI to the list when loading from the SeqUI.reset(Seq)
        function, which builds a new Seq from an old one and resets the UI to use it.  It adds
        a button to the list and sets it as the primary button for the motifui, and that's it.
        No selecting, no repainting.  The motif is not added to the Seq (we presume it's already
        been added) */
    void doAddSimple(MotifUI motifui)
        {
        motifuis.add(motifui);

        MotifListButton button = new MotifListButton(sequi, motifui, null);
        button.setDropTarget(new DropTarget(this, buildDropTargetListener()));
        buttons.add(button);
        motifui.setPrimaryButton(button);
        list.add(button);
        }

    /** This is called when we add a new MotifUI to the list generated by the "+" button. 
        It adds the motif to the seq, adds a button to the list, sets it as the MotifUI's 
        primary button, selects it, and redraws everything. */
    public void doAdd(MotifUI motifui)
        {
        doAdd(motifui, true);
        }

    /** This is called when we add a new MotifUI to the list generated by the "+" button. 
        It adds the motif to the seq, adds a button to the list, sets it as the MotifUI's 
        primary button, selects it, and redraws everything. */
    public void doAdd(MotifUI motifui, boolean pushUndo)
        {
        if (pushUndo) seq.push();
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try { seq.addMotif(motifui.getMotif()); }
        finally { lock.unlock(); }

        motifuis.add(motifui);

        MotifListButton button = new MotifListButton(sequi, motifui, null);
        button.setDropTarget(new DropTarget(this, buildDropTargetListener()));
        buttons.add(button);
        motifui.setPrimaryButton(button);
        select(button);
        list.add(button);
        list.revalidate();
        list.repaint();
        scroll.revalidate();
        if (buttons.size() == 1)
            {
            setRoot(0);
            }
        sequi.setMotifUI(motifui);
        }
        
    public void moveButton(MotifListButton button, int at)
        {
        int pos = buttons.indexOf(button);
        if (at == pos) return;
        
        // remove
        buttons.remove(button);
        motifuis.remove(button.getMotifUI());
        list.remove(button);
        
        // reinsert
        if (at > pos) at--;
        buttons.add(at, button);
        motifuis.add(at, button.getMotifUI());
        list.add(button, at);
        list.revalidate();
        list.repaint();
        }
        

    public void doRoot()
        {
        doRoot(selectedButton.getMotifUI());
        }

    public void doRoot(MotifUI motifui)
        {
        boolean stopped = false;
        // First we test and maybe immediately set root
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try { if ((stopped = seq.isStopped())) seq.setData(motifui.getMotif()); }
        finally { lock.unlock(); }
        
        // Were we successful?
        if (!stopped)
            {
            if (sequi.showSimpleConfirm("Stop Before Setting Root?", "The sequence must be stopped before setting root.", "Stop and Set Root", "Cancel"))
                {
                lock.lock();
                try { seq.stop(); seq.setData(motifui.getMotif()); }
                finally { lock.unlock(); }
                stopped = true;
                }
            }
        
        // Were we successful finally?
        if (stopped)
            {
            setRoot(motifui.getPrimaryButton());
            }
        }
        
    public void doAddMacro()
        {
        FileDialog fd = new FileDialog((JFrame)sequi.getFrame(), "Load Sequence as Macro...", FileDialog.LOAD);
        fd.setFilenameFilter(new FilenameFilter()
            {
            public boolean accept(File dir, String name)
                {
                return SeqUI.ensureFileEndsWith(name, SeqUI.PATCH_EXTENSION).equals(name);
                }
            });

        sequi.disableMenuBar();
        fd.setVisible(true);
        sequi.enableMenuBar();
                
        if (fd.getFile() != null)
            {
            GZIPInputStream stream = null;
            try
                {
                Macro macro = new Macro(seq, new JSONObject(new JSONTokener(stream = new GZIPInputStream(new FileInputStream(fd.getDirectory()+fd.getFile())))));
                seq.push();
                doAdd(MacroUI.create(seq, sequi, macro));
                }
            catch (Exception ex)
                {
                ex.printStackTrace();
                sequi.showSimpleError("Error Loading Macro", "An error occurred loading the macro " + fd.getFile());
                }
            finally
                {
                try { stream.close(); } catch (Exception ex) { }
                }
            }
        }
        
    public void doAdd(int moduleType)
        {
        try
            {
            if (MOTIF_UIS[moduleType] == seq.motif.macro.gui.MacroUI.class)
                {
                doAddMacro();
                }
            else
                {
                MotifUI ui = (MotifUI)(MOTIF_UIS[moduleType].getMethod("create", Seq.class, SeqUI.class).invoke(null, seq, sequi));
                doAdd(ui);
                }
            }
        catch (Exception ex) { ex.printStackTrace(); }
        }
        
    /** Be careful with this method. */
    public void removeAll()
        {
        motifuis.clear();
        buttons.clear();
        list.removeAll();
        }
                
    public void doRemove()
        {
        // Can I remove the selected item?
        MotifButton selectedButton = null;
        for(MotifButton button : buttons)
            {
            if (button.isSelected())
                {
                selectedButton = button;
                break;
                }
            }
        
        // Zeroth question: is anything selected?
        if (selectedButton == null)
            {
            sequi.showSimpleError("No Motif Selected", "You must select a motif first in order to delete it.");
            return;
            }
        
        // First question: is this the only button?
        if (buttons.size() == 1)
            {
            sequi.showSimpleError("Cannot Delete Only Motif", "You must have at least one motif in the sequence.");
            return;
            }
        
        // Next question: does anyone rely on it?
        boolean hasAncestors = false;
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try { hasAncestors = selectedButton.getMotifUI().getMotif().getAncestors().size() > 0; }                // FIXME Can we make a hasAncestors() method in Motif that is more efficient?
        finally { lock.unlock(); }
        if (hasAncestors)
            {
            sequi.showSimpleError("Motif Used", "There are other Motifs using this Motif as a child.\nYou will need to delete them, or disconnect it from them,\nbefore you can delete it.");
            return;
            }
                        
        // Next question: is it the root?  We can change it....
        if (selectedButton.getMotifUI().getPrimaryButton() == root)
            {
            boolean stopped = false;
            lock = seq.getLock();
            lock.lock();
            try { stopped = seq.isStopped(); }
            finally { lock.unlock(); }
                
            // Were we successful?
            if (!stopped)
                {
                if (sequi.showSimpleConfirm("Stop and Delete Root", "To delete the root, we'll need stop the sequence make something else the root first.", "Stop and Change Root", "Cancel"))
                    {
                    // Finally, we'll do the push
                    seq.push();
                    if (buttons.indexOf(selectedButton) == 0)
                        {
                        setRoot(buttons.get(1));
                        }
                    else    
                        {
                        setRoot(buttons.get(0));
                        }
                    }
                else return;
                }
            else 
                {
                //if (sequi.showSimpleConfirm("Delete Root", "To delete the root, we'll need make something else the root first.", "Change Root", "Cancel"))
                    {
                    // Finally, we'll do the push
                    seq.push();
                    if (buttons.indexOf(selectedButton) == 0)
                        {
                        setRoot(buttons.get(1));
                        }
                    else    
                        {
                        setRoot(buttons.get(0));
                        }
                    }
                //else return;
                }
            }
        
        // At this point we can probably delete it.
        selectedButton.getMotifUI().disconnect();

        lock = seq.getLock();
        lock.lock();
        try 
            {
            Motif motif = selectedButton.getMotifUI().getMotif();
            if (seq.removeMotif(motif))
                {
                motif.disconnect();
                }
            }
        finally { lock.unlock(); }

        int buttonPos = buttons.indexOf(selectedButton);
        motifuis.remove(buttonPos);
        list.remove(buttonPos);
        buttons.remove(buttonPos);

        // Figure new button to replace it
                
        if (buttonPos >= buttons.size())    // it was the last button so we have to back off
            {
            buttonPos--;
            }
                
        // Select new MotifUI and button
        selectedButton = null;
        sequi.setMotifUI(motifuis.get(buttonPos));
        }
        
    public void scrollTo(MotifUI ui)
        {
        Rectangle rect = new Rectangle();
        ui.getPrimaryButton().getBounds(rect);
        list.scrollRectToVisible(rect);
        }

      
    //// DRAG AND DROP
        
    public DropTargetListener buildDropTargetListener() { return new MotifListUIDropTargetListener(); }
        
    class MotifListUIDropTargetListener extends DropTargetAdapter 
        {                
        public void drop(DropTargetDropEvent dtde) 
            {
            Object transferableObj = null;
                
            try 
                {
                if (dtde.getTransferable().isDataFlavorSupported(MotifListButton.dataFlavor))
                    {
                    transferableObj = dtde.getTransferable().getTransferData(MotifListButton.dataFlavor);
                    } 
                } 
            catch (Exception ex) {  System.err.println("Can't drag and drop that"); }
                                
            if (transferableObj != null && transferableObj instanceof MotifListButton)
                {
                MotifListButton dropped = (MotifListButton)transferableObj;
                                                     
                Component comp = dtde.getDropTargetContext().getComponent();
                if (comp == null) return;
                else if (comp instanceof MotifListButton)
                    {
                    Component[] c = list.getComponents();
                    int at = -1;
                    for(int i = 0; i < c.length; i++)
                        {
                        if (c[i] == comp) { at = i; break; }
                        }
                    if (at != -1)
                        {
                        if (dropped instanceof MotifListButton)
                            {
                            // Where exactly are we putting it?
                            Point p = dtde.getLocation();
                            if (p.y < ((MotifListButton)dropped).getBounds().height / 2)
                                {
                                moveButton((MotifListButton)dropped, at);
                                }
                            else
                                {
                                moveButton((MotifListButton)dropped, at + 1);
                                }
                            }
                        }
                    }
                else if (comp instanceof Box)           // it's an empty space 
                    {
                    if (dropped instanceof MotifListButton)
                        {
                        moveButton((MotifListButton)dropped, list.getComponentCount());
                        }
                    }
                list.revalidate(); 
                list.repaint();
                }
            }
        }
    }
