/* 
   Copyright 2025 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.modulation.gui;

import seq.motif.modulation.*;
import seq.engine.*;
import seq.gui.*;
import seq.util.*;
import java.awt.*;
import java.awt.geom.*;
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
        
    JPanel inspectorPane;
    JMenu menu;
        
    JPanel modulationOuter;
    TitledBorder modulationBorder;
    ModulationInspector modulationInspector;
        
    JPanel modulationGrid = new JPanel();
    JPanel functionGrid = new JPanel();
    
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
        modulationGrid.setLayout(new GridLayout(1, 1));
        functionGrid.setLayout(new FlowLayout());
        }

    public void build()
        {
        super.build();
        loadChildren();         // build() populates with blanks, so this must be afterwards
        }
        
    public void loadChildren()
        { 
        ArrayList<Motif.Child> children = modulation.getChildren();
        if (children.size() == 0) return;
        
        MotifList list = sequi.getMotifList();   
        for(int i = 0; i < children.size(); i++)                // hopefully this is the size of numModulationChildren?
            {                
            Motif motif = children.get(i).getMotif();
            
            if (motif instanceof seq.motif.blank.Blank)
                {
                //ModulationButton blankButton = new ModulationButton(sequi, this, i);
                //modulationGrid.add(blankButton, i);
                }
            else
                {
                MotifUI motifui = list.getOrAddMotifUIFor(motif);

                ModulationButton newButton = new ModulationButton(sequi, motifui, ModulationUI.this, i);
                Motif.Child child = null;

                newButton.addActionListener(new ActionListener()
                    {
                    public void actionPerformed(ActionEvent e)
                        {
                        select(newButton);
                        }
                    });

                MotifButton button = (MotifButton)(modulationGrid.getComponent(i));
                button.disconnect();
                        
                // This is outside the lock but I think it's okay as we're doing this while loading from a file
                child = modulation.getChildren().get(i);
                                                                                
                newButton.setAuxiliary(child);

                modulationGrid.remove(i);
                modulationGrid.add(newButton, i);
                }
            }
        }
        
    
    // We override MotifUI, which always repaints
    // FIXME: should we examine other motifuis?
    public void redraw(boolean inResponseToStep) 
        { 
        if (!inResponseToStep)
            {
            primaryScroll.repaint(); 
            }
        }
    
    // we tap into this to rebuild the modulations as well, since they need new
    // menus for changes to parameter names
    public void rebuildInspectors(int count) 
        { 
        super.rebuildInspectors(count);
        functionGrid.removeAll();
        for(int i = 0; i < Motif.NUM_PARAMETERS; i++)
            {
            FunctionInspector functionInspector = new FunctionInspector(seq, sequi, modulation, i);
            functionGrid.add(functionInspector);
            }
        repaint();
        }

    public void buildInspectors(JScrollPane scroll)
        {
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
                
        scroll.setViewportView(inspectorPane);
        }
                
    Line2D.Double line = new Line2D.Double();

    FunctionInspector.SubInspector[] subinspectors = new FunctionInspector.SubInspector[Motif.NUM_PARAMETERS];
    
    public void buildPrimary(JScrollPane scroll)
        {
        JPanel inner = new JPanel();
        inner.setLayout(new BorderLayout());
        inner.add(new JLabel(" Child "), BorderLayout.WEST);
        inner.add(modulationGrid, BorderLayout.CENTER);

        JPanel outer = new JPanel();
        outer.setLayout(new BorderLayout());
        outer.add(inner, BorderLayout.NORTH);
        scroll.setViewportView(outer);
        
        modulationGrid.setDropTarget(new DropTarget(this, buildDropTargetListener()));
        modulationGrid.setLayout(new GridLayout(1, 1));
        modulationGrid.add(new ModulationButton(sequi, this, 0));         // A "blank" button
        
        FlowLayout flowlayout = new FlowLayout(FlowLayout.LEADING);
        flowlayout.setAlignOnBaseline(true);

        functionGrid.setLayout(flowlayout);             // it's this by default anyway
        functionGrid.setBackground(BACKGROUND);
        outer.add(functionGrid, BorderLayout.CENTER);
                
        modulation = (Modulation)getMotif();
        JPanel subpanels[] = new JPanel[Motif.NUM_PARAMETERS];
                                
        for(int i = 0; i < Motif.NUM_PARAMETERS; i++)
            {
            FunctionInspector functionInspector = new FunctionInspector(seq, sequi, modulation, i);
            functionGrid.add(functionInspector);
            }
        }           
        
    public JPanel buildConsole()
        {
        PushButton removeButton = new PushButton(new ImageIcon(PushButton.class.getResource("icons/minus.png")), true)
            {
            public void perform()
                {
                doRemove();
                }
            };
        removeButton.getButton().setPreferredSize(new Dimension(24, 24));
        removeButton.setToolTipText(REMOVE_BUTTON_TOOLTIP);

        JPanel console = new JPanel();
        console.setLayout(new BorderLayout());
                
        Box addRemoveBox = new Box(BoxLayout.X_AXIS);
        addRemoveBox.add(removeButton);
        console.add(addRemoveBox, BorderLayout.WEST);   
                
        return console; 
        }

    public void doRemove()
        {
        // Where is the button?
        Component[] c = modulationGrid.getComponents();
        int at = -1;
        for(int i = 0; i < c.length; i++)
            {
            if (((ModulationButton)c[i]).isSelected()) { at = i; break; }
            }
        if (at != -1)
            {
            sequi.push();
            seq.getLock().lock();
            try
                {
                modulation.replaceChild(new seq.motif.blank.Blank(seq), at);
                }
            finally
                {
                seq.getLock().unlock();
                }

            MotifButton button = (MotifButton)(modulationGrid.getComponent(at));
            button.disconnect();
                        
            // We'll swap in a blank ModulationButton
            modulationGrid.remove(at);
            ModulationButton blankButton = new ModulationButton(sequi, this, at);
            modulationGrid.add(blankButton, at);
            deselectAll();

            sequi.getMotifList().rebuildClipsForMotif(getMotif());
            modulationGrid.revalidate();
            }
        else System.err.println("ModulationUI.moveChild: button not in list");
        }
    
        
    public void addChild(MotifUI motifui, int at)
        {  
        ModulationButton newButton = new ModulationButton(sequi, motifui, ModulationUI.this, at);
        Motif.Child child = null;

        newButton.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                select(newButton);
                }
            });
                        
        MotifButton button = (MotifButton)(modulationGrid.getComponent(at));
        button.disconnect();
                
        seq.getLock().lock();
        try
            {
            if (motifui.getMotif().containsOrIs(getMotif()))    // No cycles permitted!
                return;
                
            modulation.replaceChild(motifui.getMotif(), at);
            child = modulation.getChildren().get(at);
            }
        finally
            {
            seq.getLock().unlock();
            }
        
        newButton.setAuxiliary(child);

        // Copy over the button
        modulationGrid.remove(at);                                  
        modulationGrid.add(newButton, at);
        select(newButton);  
        newButton.updateText();
          
        revalidate();
        sequi.getMotifList().rebuildClipsForMotif(getMotif());
        }

    public void select(MotifButton button)  
        {
        deselectAll();
        button.setSelected(true);
        int at = ((ModulationButton)button).getAt();
        }

    public void deselectAll()
        {
        for(Component c : modulationGrid.getComponents())
            {
            if (c instanceof MotifButton)
                {
                ((MotifButton)c).setSelected(false);
                }
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
                    else if (comp instanceof ModulationButton)
                        {
                        int at = ((ModulationButton)comp).getAt();
                        sequi.push();
                        if (dropped instanceof ModulationButton)
                            {
                            //moveChild((ModulationButton)dropped, at);
                            }
                        else
                            {
                            addChild(dropped.getMotifUI(), at);
                            }
                        }
                    modulationGrid.revalidate(); 
                    modulationGrid.repaint();
                                                
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
        "Removes the selected motif from the Modulation Child slot.</html>";
        

    }
