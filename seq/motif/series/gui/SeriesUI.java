/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.series.gui;

import seq.motif.series.*;
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



public class SeriesUI extends MotifUI
    {
    Series series;
    SeriesChildInspector childInspector;
        
    JPanel childOuter;
    TitledBorder childBorder;
    JPanel inspectorPane;
        
    JPanel seriesOuter;
    TitledBorder seriesBorder;
    SeriesInspector seriesInspector;
        
        
    Box seriesBox = new Box(BoxLayout.Y_AXIS);
                
    public static ImageIcon getStaticIcon() { return new ImageIcon(MotifUI.class.getResource("icons/series.png")); }        // don't ask
    public ImageIcon getIcon() { return getStaticIcon(); }
    public static String getType() { return "Series"; }
    public static MotifUI create(Seq seq, SeqUI ui)
        {
        return new SeriesUI(seq, ui, new Series(seq));
        }

    public static MotifUI create(Seq seq, SeqUI ui, Motif motif)
        {
        return new SeriesUI(seq, ui, (Series)motif);
        }
        
    public SeriesUI(Seq seq, SeqUI sequi, Series series)
        {
        super(seq, sequi, series);
        this.seq = seq;
        this.series = series;
        this.sequi = sequi;
        }
        
    public void buildInspectors(JScrollPane scroll)
        {
        // Build the series child inspector holder
        childOuter = new JPanel();
        childOuter.setLayout(new BorderLayout());
        childBorder = BorderFactory.createTitledBorder(null, "Child");
        childOuter.setBorder(childBorder);
                
        // Build the series inspector holder
        seriesOuter = new JPanel();
        seriesOuter.setLayout(new BorderLayout());
        seriesBorder = BorderFactory.createTitledBorder(null, "Series");
        seriesOuter.setBorder(seriesBorder);

        // Add the series inspector
        seriesInspector = new SeriesInspector(seq, series, this);
        seriesOuter.add(seriesInspector, BorderLayout.CENTER);
                
        // Fill the panes
        inspectorPane = new JPanel();
        inspectorPane.setLayout(new BorderLayout());
        inspectorPane.add(seriesOuter, BorderLayout.NORTH);
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
        scroll.setViewportView(seriesBox);
        seriesBox.setDropTarget(new DropTarget(this, buildDropTargetListener()));
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

        PushButton copyButton = new PushButton(new ImageIcon(PushButton.class.getResource("icons/copy.png")), true)
            {
            public void perform()
                {
                doCopy();
                }
            };
        copyButton.getButton().setPreferredSize(new Dimension(24, 24));
        copyButton.setToolTipText(COPY_BUTTON_TOOLTIP);

        JPanel console = new JPanel();
        console.setLayout(new BorderLayout());
                
        Box addRemoveBox = new Box(BoxLayout.X_AXIS);
        //addRemoveBox.add(addButton);
        addRemoveBox.add(removeButton);
        addRemoveBox.add(copyButton);
        console.add(addRemoveBox, BorderLayout.WEST);   
                
        return console; 
        }

    public void deselectAll()
        {
        for(Component c : seriesBox.getComponents())
            {
            ((MotifButton)c).setSelected(false);
            }
        }
                
    public void select(MotifButton button)  
        {
        deselectAll();
        button.setSelected(true);

        Motif.Child node = (Motif.Child)(button.getAuxiliary());
        setChildInspector(new SeriesChildInspector(seq, series, button.getMotifUI(), node, this));
        }


    public void setChildInspector(SeriesChildInspector inspector)
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
        if (!inResponseToStep)          // we're trying to cut down on the redrawing
            {
            super.redraw(inResponseToStep);
            }
        }
          
    public void moveChild(SeriesButton button, int to)
        {
        // Where is the button?
        Component[] c = seriesBox.getComponents();
        int from = -1;
        for(int i = 0; i < c.length; i++)
            {
            if (c[i] == button) { from = i; break; }
            }
        if (from != -1)
            {
            if (from == to) return; // duh
            boolean atEnd = false;
            ArrayList<Motif.Child> children = null;
            seq.getLock().lock();
            try
                {
                children = series.getChildren();
                series.moveChild(from, to);
                if (to == children.size())         // last position
                    atEnd = true;
                }
            finally
                {
                seq.getLock().unlock();
                }

            seriesBox.remove(button);
            // Now we have to figure out where to put it back
            if (atEnd) seriesBox.add(button);  // easy, put it at end
            else if (to < from)
                seriesBox.add(button, to);
            else
                seriesBox.add(button, to - 1);          // I think?
            select((MotifButton)button);
            sequi.getMotifList().rebuildClipsForMotif(getMotif());
            }
        else System.err.println("SeriesUI.moveChild: button not in list");
        seriesBox.revalidate();
        updateIndexes();
        }

    public void doRemove()
        {
        // Where is the button?
        Component[] c = seriesBox.getComponents();
        int at = -1;
        for(int i = 0; i < c.length; i++)
            {
            if (((SeriesButton)c[i]).isSelected()) { at = i; break; }
            }
        if (at != -1)
            {
            ArrayList<Motif.Child> children = null;
            sequi.push();
            seq.getLock().lock();
            try
                {
                children = series.getChildren();
                series.removeChild(at);
                }
            finally
                {
                seq.getLock().unlock();
                }

            MotifButton button = (MotifButton)(seriesBox.getComponent(at));
            button.disconnect();
                        
            seriesBox.remove(at);
            at = at - 1;
            if (at < 0) at = 0;
            if (at < seriesBox.getComponentCount())
                {
                Component comp = seriesBox.getComponent(at);
                select((SeriesButton)comp);
                }
            else
                {
                deselectAll();
                }
            sequi.getMotifList().rebuildClipsForMotif(getMotif());
            }
        else System.err.println("SeriesUI.doRemove: button not in list");
        seriesBox.revalidate();
        seriesBox.repaint();                    // FIXME This has to be forced when it's th only one in the list -- a bug in Java?
        updateIndexes();
        }
              
    public void doCopy()
        {
        // Where is the button?
        Component[] c = seriesBox.getComponents();
        int at = -1;
        for(int i = 0; i < c.length; i++)
            {
            if (((SeriesButton)c[i]).isSelected()) { at = i; break; }
            }
        if (at != -1)
            {
            Motif motif = null;
            int end = 0;
            sequi.push();
            seq.getLock().lock();
            try
                {
                ArrayList<Motif.Child> children = series.getChildren();
                end = children.size();
                motif = children.get(at).getMotif();
                }
            finally
                {
                seq.getLock().unlock();
                }

            addChild(sequi.getMotifList().getMotifUIFor(motif), end);
            }
        else System.err.println("SeriesUI.doCopy: button not in list");
        }

    MotifButton first = null;

    public void loadChildren()
        { 
        ArrayList<Motif.Child> children = series.getChildren();
        if (children.size() == 0) return;
        
        seriesBox.removeAll();
        MotifList list = sequi.getMotifList();   
        for(int i = 0; i < children.size(); i++)
            {
            Motif motif = children.get(i).getMotif();
            MotifUI motifui = list.getOrAddMotifUIFor(motif);
                
            SeriesButton newButton = new SeriesButton(sequi, motifui, SeriesUI.this, i);
                        
            newButton.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    select(newButton);
                    }
                });
                                        
            newButton.setAuxiliary(children.get(i));
            seriesBox.add(newButton);
            }
        updateIndexes();
        }
        

    public void addChild(MotifUI motifui, int at)
        {  
        seq.getLock().lock();
        ArrayList<Motif.Child> children = null;
        Motif.Child child = null;
        boolean atEnd = false;
        try
            {
            if (motifui.getMotif().containsOrIs(getMotif()))    // No cycles permitted!
                return;
                
            series.add(motifui.getMotif(), 0, 0);
            children = series.getChildren();
            child = children.get(children.size() - 1);
            if (at != children.size())             // last position, it's already there
                series.moveChild(children.size() - 1, at);              // move to right place
            else
                atEnd = true;
            }
        finally
            {
            seq.getLock().unlock();
            }
        
        SeriesButton newButton = new SeriesButton(sequi, motifui, SeriesUI.this, at);

        newButton.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                select(newButton);
                }
            });
                        
        newButton.setAuxiliary(child);

        if (atEnd) seriesBox.add(newButton);
        else seriesBox.add(newButton, at);
        select((MotifButton)newButton);
        seriesBox.revalidate();
        revalidate();
        updateIndexes();
        sequi.getMotifList().rebuildClipsForMotif(getMotif());
        }
        
    public void updateIndexes()
        {
        for(int i = 0; i < seriesBox.getComponentCount(); i++)
            {
            ((SeriesButton)(seriesBox.getComponent(i))).setAt(i);
            }
        updateText();
        }

    public void updateText()
        {
        super.updateText();
        for(int i = 0; i < seriesBox.getComponentCount(); i++)
            {
            ((SeriesButton)(seriesBox.getComponent(i))).updateText();
            }
        }

    public void uiWasSet()
        {
        super.uiWasSet();
        // update the Markov labels
        if (childInspector != null) childInspector.updateMarkovLabels();
        }
                
    //// DRAG AND DROP
        
    public DropTargetListener buildDropTargetListener() { return new SeriesUIDropTargetListener(); }
        
    class SeriesUIDropTargetListener extends DropTargetAdapter 
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
                    else if (comp instanceof SeriesButton)
                        {
                        Component[] c = seriesBox.getComponents();
                        int at = -1;
                        for(int i = 0; i < c.length; i++)
                            {
                            if (c[i] == comp) { at = i; break; }
                            }
                        if (at != -1)
                            {
                            sequi.push();
                            if (dropped instanceof SeriesButton)
                                {
                                moveChild((SeriesButton)dropped, at);
                                }
                            else
                                {
                                addChild(dropped.getMotifUI(), at);
                                }
                            }
                        }
                    else if (comp instanceof Box)           // it's an empty space 
                        {
                        seq.getLock().lock();
                        int at = 0;
                        try
                            {
                            at = series.getChildren().size();
                            }
                        finally
                            {
                            seq.getLock().unlock();
                            }
                        sequi.push();
                        if (dropped instanceof SeriesButton)
                            {
                            moveChild((SeriesButton)dropped, at);
                            }
                        else
                            {
                            addChild(dropped.getMotifUI(), at);
                            }
                        }
                    seriesBox.revalidate(); 
                    seriesBox.repaint();
                                                
                    /// FIXME: This doesn't handle drag-and-drop within the series
                    }
                }
            catch (Exception ex)
                {
                ex.printStackTrace();
                }
            }
        }
                
                
                
                
                
                
                
                
                
                
                
                
                
                
                
        
    // TESTING
    public static void main(String[] args) throws Exception
        {
        // Set up Menu and FlatLAF
        SeqUI.setupGUI();
                        
        // Set up Seq
        Seq seq = new Seq();            // starts timer running
        seq.setupForMIDI(SeriesClip.class, args, 1, 2);   // sets up MIDI in and out
        seq.setOut(0, new Out(seq, 0));         // Out 0 points to device 0 in the tuple.  This is too complex.
        seq.setOut(1, new Out(seq, 1));         // Out 0 points to device 0 in the tuple.  This is too complex.
        
        // Set up our module structure
        Series series = new Series(seq);

        // Set up first StepSequence
        seq.motif.stepsequence.StepSequence dSeq = new seq.motif.stepsequence.StepSequence(seq, 2, 16);
        
        // Specify notes
        dSeq.setTrackNote(0, 60);
        dSeq.setTrackNote(1, 120);
        dSeq.setTrackOut(0, 0);
        dSeq.setTrackOut(1, 1);
        dSeq.setDefaultSwing(0.33);
        
        // Load the StepSequence with some data
        dSeq.setVelocity(0, 0, 1);
        dSeq.setVelocity(0, 4, 5);
        dSeq.setVelocity(0, 8, 9);
        dSeq.setVelocity(0, 12, 13);
        dSeq.setVelocity(1, 1, 2);
        dSeq.setVelocity(1, 2, 3);
        dSeq.setVelocity(1, 3, 4);
        dSeq.setVelocity(1, 5, 6);
        dSeq.setVelocity(1, 7, 8);
        dSeq.setVelocity(1, 9, 10);
        dSeq.setVelocity(1, 10, 11);
        dSeq.setVelocity(1, 15, 16);
        

        // Set up second StepSequence
        seq.motif.stepsequence.StepSequence dSeq2 = new seq.motif.stepsequence.StepSequence(seq, 2, 16);
        
        // Specify notes
        dSeq2.setTrackNote(0, 45);
        dSeq2.setTrackNote(1, 90);
        dSeq2.setTrackOut(0, 0);
        dSeq2.setTrackOut(1, 1);
        dSeq2.setDefaultSwing(0);
        
        // Load the StepSequence with some data
        dSeq2.setVelocity(0, 0, 1);
        dSeq2.setVelocity(0, 2, 5);
        dSeq2.setVelocity(0, 4, 9);
        dSeq2.setVelocity(0, 6, 13);
        dSeq2.setVelocity(1, 8, 2);
        dSeq2.setVelocity(1, 9, 3);
        dSeq2.setVelocity(1, 10, 4);
        dSeq2.setVelocity(1, 11, 6);
        dSeq2.setVelocity(1, 12, 2);
        dSeq2.setVelocity(1, 13, 3);
        dSeq2.setVelocity(1, 14, 4);
        dSeq2.setVelocity(1, 15, 6);

        // Load into series
        //series.add(dSeq, 0, 0.5);
        //series.add(dSeq2, 1);
        //series.add(dSeq, 2);
                
        // Build Clip Tree
        seq.setData(series);

        // Build GUI
        SeqUI ui = new SeqUI(seq);

        seq.motif.stepsequence.gui.StepSequenceUI ssui = new seq.motif.stepsequence.gui.StepSequenceUI(seq, ui, dSeq);
        seq.motif.stepsequence.gui.StepSequenceUI ssui2 = new seq.motif.stepsequence.gui.StepSequenceUI(seq, ui, dSeq2);
        SeriesUI seriesui = new SeriesUI(seq, ui, series);
        seriesui.addChild(ssui, 0);
        seriesui.addChild(ssui2, 1);
        seriesui.addChild(ssui, 2);
        ui.addMotifUI(seriesui);
        ui.addMotifUI(ssui);
        ui.addMotifUI(ssui2);
        seq.sequi = ui;
        JFrame frame = new JFrame();
        ui.setupMenu(frame);
        frame.getContentPane().add(ui);
        frame.pack();
        frame.setVisible(true);
                
        seq.reset();
//      series.revise();
        seriesui.revise();
                    
        //Toolkit.getDefaultToolkit().getSystemEventQueue().push(new MyEventQueue());
        
        //seq.play();

        //seq.waitUntilStopped();
        }

    static final String REMOVE_BUTTON_TOOLTIP = "<html><b>Remove Motif</b><br>" +
        "Removes the selected motif from the Series.</html>";
        
    static final String COPY_BUTTON_TOOLTIP = "<html><b>Copy Motif</b><br>" +
        "Duplicates the selected motif from in the Series.</html>";
        
    }
