/* 
   Copyright 2025 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.filter.gui;

import seq.motif.filter.*;
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



public class FilterUI extends MotifUI
    {
    Filter filter;
        
    JPanel inspectorPane;
        
    JPanel filterOuter;
    TitledBorder filterBorder;
    FilterInspector filterInspector;
        
    JPanel filterGrid = new JPanel();
    JPanel functionGrid = new JPanel();
    
    public static ImageIcon getStaticIcon() { return new ImageIcon(MotifUI.class.getResource("icons/filter4.png")); }        // don't ask
    public ImageIcon getIcon() { return getStaticIcon(); }
    public static String getType() { return "Filter"; }
    public static MotifUI create(Seq seq, SeqUI ui)
        {
        return new FilterUI(seq, ui, new Filter(seq));
        }
        
    public static MotifUI create(Seq seq, SeqUI ui, Motif motif)
        {
        return new FilterUI(seq, ui, (Filter)motif);
        }
        
    public FilterUI(Seq seq, SeqUI sequi, Filter filter)
        {
        super(seq, sequi, filter);
        this.seq = seq;
        this.filter = filter;
        this.sequi = sequi;
        filterGrid.setLayout(new GridLayout(1, 1));
        functionGrid.setLayout(new FlowLayout());
        }
        
    public void build()
        {
        super.build();
        loadChildren();         // build() populates with blanks, so this must be afterwards
        }
        
    public void loadChildren()
        { 
        ArrayList<Motif.Child> children = filter.getChildren();
        if (children.size() == 0) return;
        
        MotifList list = sequi.getMotifList();   
        for(int i = 0; i < children.size(); i++)                // hopefully this is the size of numFilterChildren?
            {                
            Motif motif = children.get(i).getMotif();
            
            if (motif instanceof seq.motif.blank.Blank)
                {
                //FilterButton blankButton = new FilterButton(sequi, this, i);
                //filterGrid.add(blankButton, i);
                }
            else
                {
                MotifUI motifui = list.getOrAddMotifUIFor(motif);

                FilterButton newButton = new FilterButton(sequi, motifui, FilterUI.this, i);
                Motif.Child child = null;

                newButton.addActionListener(new ActionListener()
                    {
                    public void actionPerformed(ActionEvent e)
                        {
                        select(newButton);
                        }
                    });

                MotifButton button = (MotifButton)(filterGrid.getComponent(i));
                button.disconnect();
                        
                // This is outside the lock but I think it's okay as we're doing this while loading from a file
                child = filter.getChildren().get(i);
                                                                                
                newButton.setAuxiliary(child);

                filterGrid.remove(i);
                filterGrid.add(newButton, i);
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
    

    public void buildInspectors(JScrollPane scroll)
        {
        // Build the filter inspector holder
        filterOuter = new JPanel();
        filterOuter.setLayout(new BorderLayout());
        filterBorder = BorderFactory.createTitledBorder(null, "Filter");
        filterOuter.setBorder(filterBorder);

        // Add the filter inspector
        filterInspector = new FilterInspector(seq, filter, this);
        filterOuter.add(filterInspector, BorderLayout.CENTER);
                
        // Fill the panes
        inspectorPane = new JPanel();
        inspectorPane.setLayout(new BorderLayout());
        inspectorPane.add(filterOuter, BorderLayout.NORTH);
                
        scroll.setViewportView(inspectorPane);
        }
                
    Line2D.Double line = new Line2D.Double();

    FunctionInspector.SubInspector[] subinspectors = new FunctionInspector.SubInspector[Filter.NUM_TRANSFORMERS];
    
    public void buildPrimary(JScrollPane scroll)
        {
        JPanel outer = new JPanel();
        outer.setLayout(new BorderLayout());
        outer.add(filterGrid, BorderLayout.NORTH);
        scroll.setViewportView(outer);
        
        filterGrid.setDropTarget(new DropTarget(this, buildDropTargetListener()));
        filterGrid.setLayout(new GridLayout(1, 1));
        filterGrid.add(new FilterButton(sequi, this, 0));         // A "blank" button
        
        FlowLayout flowlayout = new FlowLayout(FlowLayout.LEADING);
        flowlayout.setAlignOnBaseline(true);

        functionGrid.setLayout(flowlayout);             // it's this by default anyway
        functionGrid.setBackground(BACKGROUND);
        outer.add(functionGrid, BorderLayout.CENTER);
                
        filter = (Filter)getMotif();
        JPanel subpanels[] = new JPanel[Filter.NUM_TRANSFORMERS];
                                
        ReentrantLock lock = seq.getLock();
        for(int i = 0; i < Filter.NUM_TRANSFORMERS; i++)
            {
            FunctionInspector functionInspector = new FunctionInspector(seq, filter, i);
            functionGrid.add(functionInspector);
            }
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
        Component[] c = filterGrid.getComponents();
        int at = -1;
        for(int i = 0; i < c.length; i++)
            {
            if (((FilterButton)c[i]).isSelected()) { at = i; break; }
            }
        if (at != -1)
            {
            seq.push();
            seq.getLock().lock();
            try
                {
                filter.replaceChild(new seq.motif.blank.Blank(seq), at);
                }
            finally
                {
                seq.getLock().unlock();
                }

            MotifButton button = (MotifButton)(filterGrid.getComponent(at));
            button.disconnect();
                        
            // We'll swap in a blank FilterButton
            filterGrid.remove(at);
            FilterButton blankButton = new FilterButton(sequi, this, at);
            filterGrid.add(blankButton, at);
            deselectAll();

            sequi.getMotifList().rebuildClipsForMotif(getMotif());
            filterGrid.revalidate();
            }
        else System.err.println("FilterUI.moveChild: button not in list");
        }
    
        
    public void addChild(MotifUI motifui, int at)
        {  
        FilterButton newButton = new FilterButton(sequi, motifui, FilterUI.this, at);
        Motif.Child child = null;

        newButton.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                select(newButton);
                }
            });
                        
        MotifButton button = (MotifButton)(filterGrid.getComponent(at));
        button.disconnect();
                
        seq.getLock().lock();
        try
            {
            if (motifui.getMotif().containsOrIs(getMotif()))    // No cycles permitted!
                return;
                
            filter.replaceChild(motifui.getMotif(), at);
            child = filter.getChildren().get(at);
            }
        finally
            {
            seq.getLock().unlock();
            }
        
        newButton.setAuxiliary(child);

        // Copy over the button
        filterGrid.remove(at);                                  
        filterGrid.add(newButton, at);
        select(newButton);  
        newButton.updateText();
          
        revalidate();
        sequi.getMotifList().rebuildClipsForMotif(getMotif());
        }

    public void select(MotifButton button)  
        {
        deselectAll();
        button.setSelected(true);
        int at = ((FilterButton)button).getAt();
        }

    public void deselectAll()
        {
        for(Component c : filterGrid.getComponents())
            {
            if (c instanceof MotifButton)
                {
                ((MotifButton)c).setSelected(false);
                }
            }
        }
    

    //// DRAG AND DROP
        
    public DropTargetListener buildDropTargetListener() { return new FilterUIDropTargetListener(); }
        
    class FilterUIDropTargetListener extends DropTargetAdapter 
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
                    else if (comp instanceof FilterButton)
                        {
                        int at = ((FilterButton)comp).getAt();
                        seq.push();
                        if (dropped instanceof FilterButton)
                            {
                            //moveChild((FilterButton)dropped, at);
                            }
                        else
                            {
                            addChild(dropped.getMotifUI(), at);
                            }
                        }
                    filterGrid.revalidate(); 
                    filterGrid.repaint();
                                                
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
        "Removes the selected motif from the Filter Child slot.</html>";
        

    }
