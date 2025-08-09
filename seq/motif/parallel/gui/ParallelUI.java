/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.parallel.gui;

import seq.motif.parallel.*;
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



public class ParallelUI extends MotifUI
    {
    Parallel parallel;
    ParallelChildInspector childInspector;
        
    JPanel childOuter;
    TitledBorder childBorder;
    JPanel inspectorPane;
        
    JPanel parallelOuter;
    TitledBorder parallelBorder;
    ParallelInspector parallelInspector;
        
        
    Box parallelBox = new Box(BoxLayout.Y_AXIS);
    
    public Box getButtonBox() { return parallelBox; }
                
    public static ImageIcon getStaticIcon() { return new ImageIcon(MotifUI.class.getResource("icons/parallel.png")); }        // don't ask
    public ImageIcon getIcon() { return getStaticIcon(); }
    public static String getType() { return "Parallel"; }
    public static MotifUI create(Seq seq, SeqUI ui)
        {
        return new ParallelUI(seq, ui, new Parallel(seq));
        }
        
    public static MotifUI create(Seq seq, SeqUI ui, Motif motif)
        {
        return new ParallelUI(seq, ui, (Parallel)motif);
        }
        
    public ParallelUI(Seq seq, SeqUI sequi, Parallel parallel)
        {
        super(seq, sequi, parallel);
        this.seq = seq;
        this.parallel = parallel;
        this.sequi = sequi;

        //build();
        }
        
    public void buildInspectors(JScrollPane scroll)
        {
        // Build the parallel child inspector holder
        childOuter = new JPanel();
        childOuter.setLayout(new BorderLayout());
        childBorder = BorderFactory.createTitledBorder(null, "Child");
        childOuter.setBorder(childBorder);
                
        // Build the parallel inspector holder
        parallelOuter = new JPanel();
        parallelOuter.setLayout(new BorderLayout());
        parallelBorder = BorderFactory.createTitledBorder(null, "Parallel");
        parallelOuter.setBorder(parallelBorder);

        // Add the parallel inspector
        parallelInspector = new ParallelInspector(seq, parallel, this);
        parallelOuter.add(parallelInspector, BorderLayout.CENTER);
                
        // Fill the panes
        inspectorPane = new JPanel();
        inspectorPane.setLayout(new BorderLayout());
        inspectorPane.add(parallelOuter, BorderLayout.NORTH);
        inspectorPane.add(childOuter, BorderLayout.CENTER);
                
        scroll.setViewportView(inspectorPane);
        }
    
    protected void build()
        {
        super.build();
        loadChildren();
        }
        
    public void buildPrimary(JScrollPane scroll)
        {
        scroll.setViewportView(parallelBox);
        parallelBox.setDropTarget(new DropTarget(this, buildDropTargetListener()));
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

        PushButton copyButton = new PushButton(new StretchIcon(PushButton.class.getResource("icons/copy.png")))
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
        for(Component c : parallelBox.getComponents())
            {
            ((MotifButton)c).setSelected(false);
            }
        }
                
    public void select(MotifButton button)  
        {
        deselectAll();
        button.setSelected(true);

        Motif.Child node = (Motif.Child)(button.getAuxiliary());
        ParallelChildInspector inspector = new ParallelChildInspector(seq, parallel, button.getMotifUI(), node, this);
        inspector.setButton((ParallelButton)button);
        setChildInspector(inspector);
        }


    public void setChildInspector(ParallelChildInspector inspector)
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
            childBorder.setTitle("Select a Step");
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
          
    public void moveChild(ParallelButton button, int to)
        {
        // Where is the button?
        Component[] c = parallelBox.getComponents();
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
                children = parallel.getChildren();
                parallel.moveChild(from, to);
                if (to == children.size())         // last position
                    atEnd = true;
                }
            finally
                {
                seq.getLock().unlock();
                }

            parallelBox.remove(button);
            // Now we have to figure out where to put it back
            if (atEnd) parallelBox.add(button);  // easy, put it at end
            else if (to < from)
                parallelBox.add(button, to);
            else
                parallelBox.add(button, to - 1);          // I think?
            select((MotifButton)button);
            sequi.getMotifList().rebuildClipsForMotif(getMotif());
            }
        else System.err.println("ParallelUI.moveChild: button not in list");
        parallelBox.revalidate();
        updateIndexes();
        }

    public void doRemove()
        {
        // Where is the button?
        Component[] c = parallelBox.getComponents();
        int at = -1;
        for(int i = 0; i < c.length; i++)
            {
            if (((ParallelButton)c[i]).isSelected()) { at = i; break; }
            }
        if (at != -1)
            {
            seq.push();
            seq.getLock().lock();
            try
                {
                parallel.removeChild(at);
                }
            finally
                {
                seq.getLock().unlock();
                }

            MotifButton button = (MotifButton)(parallelBox.getComponent(at));
            button.disconnect();
                        
            parallelBox.remove(at);
            at = at - 1;
            if (at < 0) at = 0;
            if (at < parallelBox.getComponentCount())
                {
                Component comp = parallelBox.getComponent(at);
                select((ParallelButton)comp);
                }
            else
                {
                deselectAll();
                }
            sequi.getMotifList().rebuildClipsForMotif(getMotif());
            }
        else System.err.println("ParallelUI.moveChild: button not in list");
        parallelBox.revalidate();
        parallelBox.repaint();                  // FIXME This has to be forced when it's th only one in the list -- a bug in Java?
        updateIndexes();
        }
              
    public void doCopy()
        {
        // Where is the button?
        Component[] c = parallelBox.getComponents();
        int at = -1;
        for(int i = 0; i < c.length; i++)
            {
            if (((ParallelButton)c[i]).isSelected()) { at = i; break; }
            }
        if (at != -1)
            {
            Motif motif = null;
            int end = 0;
            seq.push();
            seq.getLock().lock();
            try
                {
                ArrayList<Motif.Child> children = parallel.getChildren();
                end = children.size();
                motif = children.get(at).getMotif();
                }
            finally
                {
                seq.getLock().unlock();
                }

            addChild(sequi.getMotifList().getMotifUIFor(motif), end);
            sequi.getMotifList().rebuildClipsForMotif(getMotif());
            }
        else System.err.println("ParallelUI.doCopy: button not in list");
        }

    // This version is only used in the constructor.
    // It loads new ParallelButtons for each child.
    void loadChildren()
        {  
        // We may be building primary because we just loaded from a file.  In this case
        // we have to load all the kids into the ParallelUI
        
        // Do we need to do this inside a lock?  I don't think so -- we're not playing when this happens
        ArrayList<Motif.Child> children = parallel.getChildren();
        if (children.size() == 0) return;

        MotifList list = sequi.getMotifList();
        for(int i = 0; i < children.size(); i++)
            {
            Motif motif = children.get(i).getMotif();
            MotifUI motifui = list.getMotifUIFor(motif);
        
            ParallelButton newButton = new ParallelButton(sequi, motifui, ParallelUI.this, i);

            ReentrantLock lock = parallel.getSeq().getLock();
            lock.lock();
            try 
                {
                newButton.setDelay(((Parallel.Data)(parallel.getChild(i).getData())).getDelay());
                }
            finally
                {
                lock.unlock();
                }

            newButton.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    select(newButton);
                    }
                });
                                                
            newButton.setAuxiliary(children.get(i));
            parallelBox.add(newButton);
            }
        updateIndexes();
        }


    public void addChild(MotifUI motifui, int at)
        {  
        ParallelButton newButton = new ParallelButton(sequi, motifui, ParallelUI.this, at);

        newButton.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                select(newButton);
                }
            });
                        
        seq.getLock().lock();
        ArrayList<Motif.Child> children = null;
        boolean atEnd = false;
        try
            {
            if (motifui.getMotif().containsOrIs(getMotif()))    // No cycles permitted!
                return;
            
            parallel.add(newButton.getMotifUI().getMotif(), 0);
            children = parallel.getChildren();
            newButton.setAuxiliary(children.get(children.size() - 1));      // last one
            if (at != children.size())             // last position, it's already there
                parallel.moveChild(children.size() - 1, at);              // move to right place
            else
                atEnd = true;
            }
        finally
            {
            seq.getLock().unlock();
            }
        
        if (atEnd) parallelBox.add(newButton);
        else parallelBox.add(newButton, at);
        select((MotifButton)newButton);
        parallelBox.revalidate();
        revalidate();
        updateIndexes();
        sequi.getMotifList().rebuildClipsForMotif(getMotif());
        }
        
    public void updateIndexes()
        {
        for(int i = 0; i < parallelBox.getComponentCount(); i++)
            {
            ((ParallelButton)(parallelBox.getComponent(i))).setAt(i);
            }
        updateText();
        }

    public void updateText()
        {
        super.updateText();
        for(int i = 0; i < parallelBox.getComponentCount(); i++)
            {
            ((ParallelButton)(parallelBox.getComponent(i))).updateText();
            }
        }
                
    //// DRAG AND DROP
        
    public DropTargetListener buildDropTargetListener() { return new ParallelUIDropTargetListener(); }
        
    class ParallelUIDropTargetListener extends DropTargetAdapter 
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
                    else if (comp instanceof ParallelButton)
                        {
                        Component[] c = parallelBox.getComponents();
                        int at = -1;
                        for(int i = 0; i < c.length; i++)
                            {
                            if (c[i] == comp) { at = i; break; }
                            }
                        if (at != -1)
                            {
                            seq.push();
                            if (dropped instanceof ParallelButton)
                                {
                                moveChild((ParallelButton)dropped, at);
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
                            at = parallel.getChildren().size();
                            }
                        finally
                            {
                            seq.getLock().unlock();
                            }
                        seq.push();
                        if (dropped instanceof ParallelButton)
                            {
                            moveChild((ParallelButton)dropped, at);
                            }
                        else
                            {
                            addChild(dropped.getMotifUI(), at);
                            }
                        }
                    parallelBox.revalidate(); 
                    parallelBox.repaint();
                                                
                    /// FIXME: This doesn't handle drag-and-drop within the parallel
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
        // Set up FlatLaf
        FlatLightLaf.setup();
                
        // Set up Seq
        Seq seq = new Seq();            // starts timer running
        seq.setupForMIDI(ParallelClip.class, args, 0, 2);   // sets up MIDI in and out
        seq.setOut(0, new Out(seq, 0));         // Out 0 points to device 0 in the tuple.  This is too complex.
        seq.setOut(1, new Out(seq, 1));         // Out 0 points to device 0 in the tuple.  This is too complex.
        
        // Set up our module structure
        Parallel parallel = new Parallel(seq);

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

        // Load into parallel
        //parallel.add(dSeq, 0, 0.5);
        //parallel.add(dSeq2, 1);
        //parallel.add(dSeq, 2);
                
        // Build Clip Tree
        seq.setData(parallel);

        // Build GUI
        SeqUI ui = new SeqUI(seq);

        seq.motif.stepsequence.gui.StepSequenceUI ssui = new seq.motif.stepsequence.gui.StepSequenceUI(seq, ui, dSeq);
        seq.motif.stepsequence.gui.StepSequenceUI ssui2 = new seq.motif.stepsequence.gui.StepSequenceUI(seq, ui, dSeq2);
        ParallelUI parallelui = new ParallelUI(seq, ui, parallel);
        parallelui.addChild(ssui, 0);
        parallelui.addChild(ssui2, 1);
        parallelui.addChild(ssui, 2);
        ui.addMotifUI(parallelui);
        ui.addMotifUI(ssui);
        ui.addMotifUI(ssui2);
        seq.sequi = ui;
        JFrame frame = new JFrame();
        frame.getContentPane().add(ui);
        frame.pack();
        frame.show();

        seq.reset();
//      parallel.revise();
        parallelui.revise();
                    
        //Toolkit.getDefaultToolkit().getSystemEventQueue().push(new MyEventQueue());
        
        //seq.play();

        //seq.waitUntilStopped();
        }

    static final String REMOVE_BUTTON_TOOLTIP = "<html><b>Remove Motif</b><br>" +
        "Removes the selected motif from the Parallel.</html>";
        
    static final String COPY_BUTTON_TOOLTIP = "<html><b>Copy Motif</b><br>" +
        "Duplicates the selected motif from in the Parallel.</html>";
        
    }
