/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.macro.gui;

import seq.motif.macro.*;
import seq.engine.*;
import seq.gui.*;
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



public class MacroUI extends MotifUI
    {
    public static final int MIN_HEADER_SPACE = 16;
    public static final Color HEADER_LINE_COLOR = new Color(200, 200, 200);
    public static final int LABEL_PADDING = 4;

    Macro macro;
        
    MacroChildInspector childInspector;

    JPanel childOuter;
    TitledBorder childBorder;
    JPanel inspectorPane;
        
    JPanel macroOuter;
    TitledBorder macroBorder;
    MacroInspector macroInspector;
    ArrayList<JLabel> labels = new ArrayList<JLabel>();
        
    JPanel macroGrid = new JPanel();
    JPanel verticalHeader = new JPanel();
    
    public static ImageIcon getStaticIcon() { return new ImageIcon(MotifUI.class.getResource("icons/macro.png")); }        // don't ask
    public ImageIcon getIcon() { return getStaticIcon(); }
    public static String getType() { return "Macro"; }
    public static MotifUI create(Seq seq, SeqUI ui)
        {
        return new MacroUI(seq, ui, new Macro(seq));
        }
        
    public static MotifUI create(Seq seq, SeqUI ui, Motif motif)
        {
        return new MacroUI(seq, ui, (Macro)motif);
        }
        
    public MacroUI(Seq seq, SeqUI sequi, Macro macro)
        {
        super(seq, sequi, macro);
        this.seq = seq;
        this.macro = macro;
        this.sequi = sequi;
        macroGrid.setLayout(new GridLayout(macro.getNumMacroChildren(), 1));
        verticalHeader.setLayout(new GridLayout(macro.getNumMacroChildren(), 1));
        }
        
    public void build()
        {
        super.build();
        loadChildren();         // build() populates with blanks, so this must be afterwards
        }
        
    public void loadChildren()
        { 
        ArrayList<Motif.Child> children = macro.getChildren();
        if (children.size() == 0) return;
        
        MotifList list = sequi.getMotifList();   
        for(int i = 0; i < children.size(); i++)                // hopefully this is the size of numMacroChildren?
            {                
            Motif motif = children.get(i).getMotif();
            
            if (motif instanceof seq.motif.blank.Blank)
                {
                //MacroButton blankButton = new MacroButton(sequi, this, i);
                //macroGrid.add(blankButton, i);
                }
            else
                {
                MotifUI motifui = list.getOrAddMotifUIFor(motif);

                MacroButton newButton = new MacroButton(sequi, motifui, MacroUI.this, i);
                Motif.Child child = null;

                newButton.addActionListener(new ActionListener()
                    {
                    public void actionPerformed(ActionEvent e)
                        {
                        select(newButton);
                        }
                    });

                MotifButton button = (MotifButton)(macroGrid.getComponent(i));
                button.disconnect();
                        
                // This is outside the lock but I think it's okay as we're doing this while loading from a file
                child = macro.getChildren().get(i);
                                                                                
                newButton.setAuxiliary(child);

                macroGrid.remove(i);
                macroGrid.add(newButton, i);
                }
            }
        }
        

    public void buildInspectors(JScrollPane scroll)
        {
        // Build the series child inspector holder
        childOuter = new JPanel();
        childOuter.setLayout(new BorderLayout());
        childBorder = BorderFactory.createTitledBorder(null, "Child");
        childOuter.setBorder(childBorder);
                
        // Build the macro inspector holder
        macroOuter = new JPanel();
        macroOuter.setLayout(new BorderLayout());
        macroBorder = BorderFactory.createTitledBorder(null, "Macro");
        macroOuter.setBorder(macroBorder);

        // Add the macro inspector
        macroInspector = new MacroInspector(seq, macro, this);
        macroOuter.add(macroInspector, BorderLayout.CENTER);
                
        // Fill the panes
        inspectorPane = new JPanel();
        inspectorPane.setLayout(new BorderLayout());
        inspectorPane.add(macroOuter, BorderLayout.NORTH);
        inspectorPane.add(childOuter, BorderLayout.CENTER);
                
        scroll.setViewportView(inspectorPane);
        }
                
    public void buildPrimary(JScrollPane scroll)
        {
        JPanel outer = new JPanel();
        outer.setLayout(new BorderLayout());
        outer.add(macroGrid, BorderLayout.NORTH);
        outer.setBackground(BACKGROUND);
        scroll.setViewportView(outer);
        
        outer = new JPanel();
        outer.setLayout(new BorderLayout());
        outer.add(verticalHeader, BorderLayout.NORTH);
        outer.setBackground(BACKGROUND);
        scroll.setRowHeaderView(outer);
        macroGrid.setDropTarget(new DropTarget(this, buildDropTargetListener()));

        macroGrid.setLayout(new GridLayout(0, 1));
        
        seq.getLock().lock();
        int numChildren = 0;
        try
            {
            numChildren = macro.getNumMacroChildren();
            }
        finally
            {
            seq.getLock().unlock();
            }

        for(int i = 0; i < macro.getNumMacroChildren(); i++)
            {
            macroGrid.add(new MacroButton(sequi, this, i));         // A "blank" button
            }      
                        
        // Fill the left hand side
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            { 
            labels.clear();
            for(MacroChild kid : macro.getMacroChildren())
                {
                int index = kid.getIndex();
                String nickname = getNickname(index);  // will likely be in order
                if (nickname == null || nickname.trim().equals(""))
                    nickname = kid.getName();
                if (nickname == null || nickname.trim().equals(""))     // uh
                    nickname = "[Untitled]";
                JLabel p = new JLabel(" " + index + ": " + nickname)
                    {
                    public Dimension getPreferredSize() 
                        { 
                        Dimension d = super.getPreferredSize();
                        return new Dimension(d.width + LABEL_PADDING, macroGrid.getComponent(index).getPreferredSize().height); 
                        }
                    public Dimension getMaximumSize() { return getPreferredSize(); }
                    public Dimension getMinimumSize() { return getPreferredSize(); }
                    };
                p.setBorder(BorderFactory.createMatteBorder(0,0,0,1,HEADER_LINE_COLOR));
                verticalHeader.add(p);
                labels.add(p);
                }
            }
        finally { lock.unlock(); }
        }
                
    public void setNickname(int index, String val)
        {
        ArrayList<Macro.Child> children = macro.getChildren(); 
        children.get(index).setNickname(val);
        }
              
    public String getNickname(int index)
        {
        ArrayList<Macro.Child> children = macro.getChildren(); 
        return children.get(index).getNickname();
        }

    public JPanel buildConsole()
        {
        PushButton removeButton = new PushButton(new StretchIcon(PushButton.class.getResource("icons/minus.png")))
            {
            public void perform()
                {
                doRemove();
                }
            };
        removeButton.getButton().setPreferredSize(new Dimension(24, 24));
        removeButton.setToolTipText(REMOVE_BUTTON_TOOLTIP);

/*
  PushButton copyButton = new PushButton(new StretchIcon(PushButton.class.getResource("icons/copy.png")))
  {
  public void perform()
  {
  doCopy();
  }
  };
  copyButton.getButton().setPreferredSize(new Dimension(24, 24));
  copyButton.setToolTipText("Copy selected motif in macro list");
*/

        JPanel console = new JPanel();
        console.setLayout(new BorderLayout());
                
        Box addRemoveBox = new Box(BoxLayout.X_AXIS);
        //addRemoveBox.add(addButton);
        addRemoveBox.add(removeButton);
//        addRemoveBox.add(copyButton);
        console.add(addRemoveBox, BorderLayout.WEST);   
                
        return console; 
        }





    JPanel stub = new JPanel();
    public void moveChild(MacroButton button, int to)
        {
        // Where is the button?
        Component[] c = macroGrid.getComponents();
        int from = -1;
        for(int i = 0; i < c.length; i++)
            {
            if (c[i] == button) { from = i; break; }
            }
        if (from != -1)
            {
            if (from == to) return; // duh
            ArrayList<Motif.Child> children = null;
            seq.getLock().lock();
            try
                {
                macro.swapChild(from, to);
                }
            finally
                {
                seq.getLock().unlock();
                }
            
            // To swap, to be careful we'll first remove TO and replace with
            // a stub.  Then we'll remove FROM and replace with TO.  Then we'll
            // remove the stub and replace with FROM.
            MacroButton toButton = (MacroButton)c[to];
            macroGrid.remove(to);
            macroGrid.add(stub, to);
            
            macroGrid.remove(from);
            macroGrid.add(toButton, from);
            
            macroGrid.remove(to);
            macroGrid.add(button, to);
            
            button.setAt(to);
            toButton.setAt(from);
              
            select(button);
            sequi.getMotifList().rebuildClipsForMotif(getMotif());
            macroGrid.revalidate();
            }
        else System.err.println("MacroUI.moveChild: button not in list");
        }

    public void doRemove()
        {
        // Where is the button?
        Component[] c = macroGrid.getComponents();
        int at = -1;
        for(int i = 0; i < c.length; i++)
            {
            if (((MacroButton)c[i]).isSelected()) { at = i; break; }
            }
        if (at != -1)
            {
            sequi.push();
            seq.getLock().lock();
            try
                {
                macro.replaceChild(new seq.motif.blank.Blank(seq), at);
                }
            finally
                {
                seq.getLock().unlock();
                }

            MotifButton button = (MotifButton)(macroGrid.getComponent(at));
            button.disconnect();
                        
            // We'll swap in a blank MacroButton
            macroGrid.remove(at);
            MacroButton blankButton = new MacroButton(sequi, this, at);
            macroGrid.add(blankButton, at);
            deselectAll();

            sequi.getMotifList().rebuildClipsForMotif(getMotif());
            macroGrid.revalidate();
            }
        else System.err.println("MacroUI.moveChild: button not in list");
        }
    
    /*     
           public void doCopy()
           {
           // Where is the button?
           Component[] c = macroGrid.getComponents();
           int from = -1;
           for(int i = 0; i < c.length; i++)
           {
           if (((MacroButton)c[i]).isSelected()) { from = i; break; }
           }
           if (from != -1)
           {
           // First make sure the copy is there
           MotifUI original = ((MacroButton)c[from]).getMotifUI();
           if (original == null) return;               // it's a blank button

           // where do we put it?

           int size = macro.getNumMacroChildren();
           int to = -1;
           for(int x = 1; x < size; x++)
           {
           int possibility = from + x;
           if (possibility >= size) possibility -= size;
           
           // Next make sure that there is space
           MotifUI neighbor = ((MacroButton)c[possibility]).getMotifUI();
           if (neighbor == null) { to = possibility; break; }
           }
            
           // did we find a spot?
           if (to == -1)
           {
           sequi.showSimpleError("Error Duplicating", "There was no space available to add a duplicate.");
           return;
           }
                
           sequi.push();
           // okay, we can make the copy, go ahead and do it
           seq.getLock().lock();
           try
           {
           macro.copyChild(from, to);
           }
           finally
           {
           seq.getLock().unlock();
           }

           // Copy over the button
           macroGrid.remove(to);
           MacroButton newButton = new MacroButton(sequi, original, this, to);

           newButton.addActionListener(new ActionListener()
           {
           public void actionPerformed(ActionEvent e)
           {
           select(newButton);
           }
           });
                        
           macroGrid.add(newButton, to);
           select(newButton);            
           macroGrid.revalidate();
           sequi.getMotifList().rebuildClipsForMotif(getMotif());
           }
           else System.err.println("MacroUI.doCopy: button not in list");
           }
    */
        
    public void addChild(MotifUI motifui, int at)
        {  
        MacroButton newButton = new MacroButton(sequi, motifui, MacroUI.this, at);
        Motif.Child child = null;

        newButton.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                select(newButton);
                }
            });
                        
        MotifButton button = (MotifButton)(macroGrid.getComponent(at));
        button.disconnect();
                
        seq.getLock().lock();
        try
            {
            if (motifui.getMotif().containsOrIs(getMotif()))    // No cycles permitted!
                return;
                
            macro.replaceChild(motifui.getMotif(), at);
            child = macro.getChildren().get(at);
            }
        finally
            {
            seq.getLock().unlock();
            }
        
        newButton.setAuxiliary(child);

        // Copy over the button
        macroGrid.remove(at);                                  
        macroGrid.add(newButton, at);
        select(newButton);  
        newButton.updateText();
          
        revalidate();
        sequi.getMotifList().rebuildClipsForMotif(getMotif());
        }

    public void select(MotifButton button)  
        {
        deselectAll();
        button.setSelected(true);
        int at = ((MacroButton)button).getAt();
        MacroChild macroChild = macro.getMacroChildren().get(at);
        setChildInspector(new MacroChildInspector(seq, macroChild, null, this, at));
        }

    public void deselectAll()
        {
        for(Component c : macroGrid.getComponents())
            {
            if (c instanceof MotifButton)
                {
                ((MotifButton)c).setSelected(false);
                }
            }
        }
    
    public void updateMacroChildName(int at, String newValue)
        {
        labels.get(at).setText(" " + at + ": " + newValue);
        labels.get(at).revalidate();
        labels.get(at).repaint();
        }     


    public void setChildInspector(MacroChildInspector inspector)
        {
        childInspector = inspector;
        childOuter.removeAll();
        if (inspector!=null) 
            {
            childOuter.add(inspector, BorderLayout.NORTH);
            childBorder.setTitle("Child (" + inspector.getAt() + ")");
            }
        else
            {
            childBorder.setTitle("Empty");
            }

        childOuter.setBorder(null);             // this has to be done or it won't immediately redraw!
        childOuter.setBorder(childBorder);
        if (inspector!=null) inspector.revise();
        revalidate();
        }

    public void revise()
        {
        if (childInspector != null) 
            {
            childBorder.setTitle("Child (" + childInspector.getAt() + ")");
            childOuter.setBorder(null);             // this has to be done or it won't immediately redraw!
            childOuter.setBorder(childBorder);
            childInspector.revise();
            }
        }
        


    //// DRAG AND DROP
        
    public DropTargetListener buildDropTargetListener() { return new MacroUIDropTargetListener(); }
        
    class MacroUIDropTargetListener extends DropTargetAdapter 
        {
        public void dragOver(DropTargetDragEvent dtde)
            {
            try
                {
            
                Component comp = dtde.getDropTargetContext().getComponent();
                if (comp == null) return;
                else if (comp instanceof JPanel)
                    {
                    JPanel box = (JPanel) comp;
                    Point p = dtde.getLocation();
                    Component spot = box.getComponentAt(p);
                    if (spot instanceof MotifButton)
                        {
                        select((MotifButton)spot);
                        }
                    }
                }
            catch (Exception ex)
                {
                ex.printStackTrace();
                }
            }
                        
        public void dragExit(DropTargetEvent dtde)
            {
            try
                {
                Component comp = dtde.getDropTargetContext().getComponent();
                if (comp == null) return;
                else if (comp instanceof JPanel)
                    {
                    deselectAll();
                    }
                }
            catch (Exception ex)
                {
                ex.printStackTrace();
                }
            }
                
        public void drop(DropTargetDropEvent dtde) 
            {
            try
                {
                Object transferableObj = null;
                
                try 
                    {
                    if (dtde.getTransferable().isDataFlavorSupported(MotifButton.dataFlavor))
                        {
                        transferableObj = dtde.getTransferable().getTransferData(MotifButton.dataFlavor);
                        } 
                    } 
                catch (Exception ex) {  System.err.println("Can't drag and drop that"); }
                                
                if (transferableObj != null && transferableObj instanceof MotifButton)
                    {
                    MotifButton dropped = (MotifButton)transferableObj;
                                
                    seq.getLock().lock();
                    try
                        {
                        if (dropped.getMotifUI().getMotif().containsOrIs(getMotif()))   // can't add me into me etc.
                            {
                            recursiveDragError(dropped, sequi);
                            return;
                            }
                        }
                    finally
                        {
                        seq.getLock().unlock();
                        }
                                
                    Component comp = dtde.getDropTargetContext().getComponent();
                    if (comp == null) return;
                    else if (comp instanceof MacroButton)
                        {
                        int at = ((MacroButton)comp).getAt();
                        sequi.push();
                        if (dropped instanceof MacroButton)
                            {
                            moveChild((MacroButton)dropped, at);
                            }
                        else
                            {
                            addChild(dropped.getMotifUI(), at);
                            }
                        }
                    macroGrid.revalidate(); 
                    macroGrid.repaint();
                                                
                    /// FIXME: This doesn't handle drag-and-drop within the select
                    }
                }
            catch (Exception ex)
                {
                ex.printStackTrace();
                }
            }
        }

    static final String REMOVE_BUTTON_TOOLTIP = "<html><b>Remove Motif</b><br>" +
        "Removes the selected motif from the Macro Child slot.</html>";
        

    }
