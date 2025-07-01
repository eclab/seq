/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.modulation.gui;

import seq.motif.modulation.*;
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



public class ModulationUI extends MotifUI
    {
    Modulation modulation;
    ModulationChildInspector childInspector;
        
    JPanel childOuter;
    TitledBorder childBorder;
    JPanel inspectorPane;
        
    JPanel modulationOuter;
    TitledBorder modulationBorder;
    ModulationInspector modulationInspector;
        
        
    Box modulationBox = new Box(BoxLayout.Y_AXIS);
                
    public static ImageIcon getStaticIcon() { return new ImageIcon(MotifUI.class.getResource("icons/adsr.png")); }        // don't ask
    public ImageIcon getIcon() { return getStaticIcon(); }
    public static String getType() { return "Modulation"; }
    public static MotifUI create(Seq seq, SeqUI ui)
        {
        return new ModulationUI(seq, ui, new Modulation(seq));
        }

    public static MotifUI create(Seq seq, SeqUI ui, Motif motif)
        {
        return new ModulationUI(seq, ui, (Modulation)motif);
        }
        
    public ModulationUI(Seq seq, SeqUI sequi, Modulation modulation)
        {
        super(seq, sequi, modulation);
        this.seq = seq;
        this.modulation = modulation;
        this.sequi = sequi;
        }
        
    public void buildInspectors(JScrollPane scroll)
        {
        // Build the modulation child inspector holder
        childOuter = new JPanel();
        childOuter.setLayout(new BorderLayout());
        childBorder = BorderFactory.createTitledBorder(null, "Child");
        childOuter.setBorder(childBorder);
                
        // Build the modulation inspector holder
        modulationOuter = new JPanel();
        modulationOuter.setLayout(new BorderLayout());
        modulationBorder = BorderFactory.createTitledBorder(null, "Modulation");
        modulationOuter.setBorder(modulationBorder);

        // Add the modulation inspector
        modulationInspector = new ModulationInspector(seq, modulation, this);
        modulationOuter.add(modulationInspector, BorderLayout.CENTER);
                
        // Fill the panes
        inspectorPane = new JPanel();
        inspectorPane.setLayout(new BorderLayout());
        inspectorPane.add(modulationOuter, BorderLayout.NORTH);
        inspectorPane.add(childOuter, BorderLayout.CENTER);
                
        scroll.setViewportView(inspectorPane);
        }
            
    public void build()
        {
        loadChildren();
        super.build();
        }
                    
    public void buildPrimary(JScrollPane scroll)
        {
        scroll.setViewportView(modulationBox);
        modulationBox.setDropTarget(new DropTarget(this, buildDropTargetListener()));
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
        removeButton.setToolTipText("Remove selected motif from modulation list");

        JPanel console = new JPanel();
        console.setLayout(new BorderLayout());
                
        Box addRemoveBox = new Box(BoxLayout.X_AXIS);
        addRemoveBox.add(removeButton);
        console.add(addRemoveBox, BorderLayout.WEST);   
                
        return console; 
        }

    public void deselectAll()
        {
        for(Component c : modulationBox.getComponents())
            {
            ((MotifButton)c).setSelected(false);
            }
        }
                
    public void select(MotifButton button)  
        {
        deselectAll();
        button.setSelected(true);

        Motif.Child node = (Motif.Child)(button.getAuxiliary());
        System.err.println("MODULATION IS " + modulation);
        System.err.println("UI IS " + button.getMotifUI());
        System.err.println("NODE " + node);
        
        setChildInspector(new ModulationChildInspector(seq, modulation, button.getMotifUI(), node, this));
        }


    public void setChildInspector(ModulationChildInspector inspector)
        {
        childInspector = inspector;
        childOuter.removeAll();
        if(inspector!=null) 
            {
            childOuter.add(inspector, BorderLayout.NORTH);
            childBorder.setTitle("Child " + (inspector.getChildNum() + 1));
            }
        else
            {
            childBorder.setTitle("Select a Child");
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
            childBorder.setTitle("Child " + (childInspector.getChildNum() + 1));
            childOuter.setBorder(null);             // this has to be done or it won't immediately redraw!
            childOuter.setBorder(childBorder);
            childInspector.revise();
            }
        }
        
    public void redraw(boolean inResponseToStep) 
        {
        updateText();
        super.redraw(inResponseToStep);
        }
          
    public void doRemove()
        {
        // Where is the button?
        Component[] c = modulationBox.getComponents();
        int at = -1;
        for(int i = 0; i < c.length; i++)
            {
            if (((ModulationButton)c[i]).isSelected()) { at = i; break; }
            }
        if (at != -1)
            {
            ArrayList<Motif.Child> children = null;
            seq.push();
            seq.getLock().lock();
            try
                {
                children = modulation.getChildren();
                modulation.removeChild(at);
                }
            finally
                {
                seq.getLock().unlock();
                }

            MotifButton button = (MotifButton)(modulationBox.getComponent(at));
            button.disconnect();
                        
            modulationBox.remove(at);
            at = at - 1;
            if (at < 0) at = 0;
            if (at < modulationBox.getComponentCount())
                {
                Component comp = modulationBox.getComponent(at);
                select((ModulationButton)comp);
                }
            else
                {
                deselectAll();
                }
            sequi.getMotifList().rebuildClipsForMotif(getMotif());
            }
        else System.err.println("ModulationUI.removeChild: button not in list");
        modulationBox.revalidate();
        modulationBox.repaint();                    // FIXME This has to be forced when it's th only one in the list -- a bug in Java?
        }
              
    MotifButton first = null;

    public void loadChildren()
        { 
        ArrayList<Motif.Child> children = modulation.getChildren();
        if (children.size() == 0) return;
        
        
        MotifList list = sequi.getMotifList();   
        Motif motif = children.get(0).getMotif();
        MotifUI motifui = list.getOrAddMotifUIFor(motif);
                        
        ModulationButton newButton = new ModulationButton(sequi, motifui, ModulationUI.this);
                                        
        newButton.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                select(newButton);
                }
            });
                                                                        
        newButton.setAuxiliary(children.get(0));
        modulationBox.add(newButton);
        }
        

    public void addChild(MotifUI motifui)
        {  
        seq.getLock().lock();
        ArrayList<Motif.Child> children = null;
        Motif.Child child = null;
        boolean atEnd = false;
        try
            {
            if (motifui.getMotif().containsOrIs(getMotif()))    // No cycles permitted!
                return;
            
            children = modulation.getChildren();
            if (children.size() > 0) children.clear();
            modulation.addChild(motifui.getMotif());
            child = modulation.getChildren().get(0);
            }
        finally
            {
            seq.getLock().unlock();
            }
        
        ModulationButton newButton = new ModulationButton(sequi, motifui, ModulationUI.this);

        newButton.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                select(newButton);
                }
            });
                        
        newButton.setAuxiliary(child);

        modulationBox.removeAll();
        modulationBox.add(newButton);
        select((MotifButton)newButton);
        modulationBox.revalidate();
        revalidate();
        sequi.getMotifList().rebuildClipsForMotif(getMotif());
        }
        
    public void updateText()
        {
        super.updateText();
        for(int i = 0; i < modulationBox.getComponentCount(); i++)
            {
            ((ModulationButton)(modulationBox.getComponent(i))).updateText();
            }
        }

                
    //// DRAG AND DROP
        
    public DropTargetListener buildDropTargetListener() { return new ModulationUIDropTargetListener(); }
        
    class ModulationUIDropTargetListener extends DropTargetAdapter 
        {
        public void dragOver(DropTargetDragEvent dtde)
            {
            try
                {
                Component comp = dtde.getDropTargetContext().getComponent();
                if (comp == null) return;
                else if (comp instanceof Box)
                    {
                    Box box = (Box) comp;
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
                else if (comp instanceof Box)
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
                    else if (comp instanceof ModulationButton)
                        {
                        Component[] c = modulationBox.getComponents();
                        int at = -1;
                        for(int i = 0; i < c.length; i++)
                            {
                            if (c[i] == comp) { at = i; break; }
                            }
                        if (at != -1)
                            {
                            seq.push();
                            if (dropped instanceof ModulationButton)
                                {
//                                moveChild((ModulationButton)dropped, at);
                                }
                            else
                                {
                                addChild(dropped.getMotifUI());
                                }
                            }
                        }
                    else if (comp instanceof Box)           // it's an empty space 
                        {
                        seq.getLock().lock();
                        int at = 0;
                        try
                            {
                            at = modulation.getChildren().size();
                            }
                        finally
                            {
                            seq.getLock().unlock();
                            }
                        seq.push();
                        if (dropped instanceof ModulationButton)
                            {
//                            moveChild((ModulationButton)dropped, at);
                            }
                        else
                            {
                            addChild(dropped.getMotifUI());
                            }
                        }
                    modulationBox.revalidate(); 
                    modulationBox.repaint();
                                                
                    /// FIXME: This doesn't handle drag-and-drop within the modulation
                    }
                }
            catch (Exception ex)
                {
                ex.printStackTrace();
                }
            }
        }
    }
