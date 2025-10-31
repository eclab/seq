/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.select.gui;

import seq.motif.select.*;
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



public class SelectUI extends MotifUI
    {
    Select select;
        
    SelectChildInspector childInspector;
    JPanel childOuter;
    TitledBorder childBorder;
    JPanel inspectorPane;
        
    JPanel selectOuter;
    TitledBorder selectBorder;
    SelectInspector selectInspector;
    
    JScrollPane scrollPane;
    
    Dial[] dials = new Dial[Motif.NUM_PARAMETERS];              // also 8
    
    public static final int GRID_WIDTH = 8;
    public static final int GRID_SIZE = GRID_WIDTH * GRID_WIDTH;
    public static final int MIN_HEADER_SPACE = 16;
    public static final Color HEADER_LINE_COLOR = new Color(200, 200, 200);
        
    JPanel selectGrid = new JPanel();
    JPanel horizontalHeader = new JPanel();
    JPanel verticalHeader = new JPanel();
                
    public static ImageIcon getStaticIcon() { return new ImageIcon(MotifUI.class.getResource("icons/select.png")); }        // don't ask
    public ImageIcon getIcon() { return getStaticIcon(); }
    public static String getType() { return "Select"; }
    
    public Dial getDial(int index) { return dials[index]; }
    public void updateDials() { sequi.setMotifUI(this); }
    
    public static MotifUI create(Seq seq, SeqUI ui)
        {
        return new SelectUI(seq, ui, new Select(seq));
        }
        
    public static MotifUI create(Seq seq, SeqUI ui, Motif motif)
        {
        return new SelectUI(seq, ui, (Select)motif);
        }
        
    public SelectUI(Seq seq, SeqUI sequi, Select select)
        {
        super(seq, sequi, select);
        this.seq = seq;
        this.select = select;
        this.sequi = sequi;
        selectGrid.setLayout(new GridLayout(GRID_WIDTH, GRID_WIDTH));
        horizontalHeader.setLayout(new GridLayout(1, GRID_WIDTH));
        verticalHeader.setLayout(new GridLayout(GRID_WIDTH, 2));
        
        //loadChildren(); // this must be AFTER build() because build() creates the grid in the first place
        }
        
    protected void build()
        {
        // This ugly hack prevents build() from replacing the children with Blank -- FIXME we need something more elegant than this
        loadingChildren = true;
        super.build();
        loadingChildren = false;
        // end ugly hack
        loadChildren();
        }
        
        
    // This version is only used in the constructor.
    // It loads new SelectButtons for each child.
    void loadChildren()
        {  
        // We may be building primary because we just loaded from a file.  In this case
        // we have to load all the kids into the SelectUI
        
        // Do we need to do this inside a lock?  I don't think so -- we're not playing when this happens
        ArrayList<Motif.Child> children = select.getChildren();
        if (children.size() == 0) return;

        MotifList list = sequi.getMotifList();
        for(int i = 0; i < children.size(); i++)
            {
            Motif motif = children.get(i).getMotif();
            if (motif instanceof seq.motif.blank.Blank) continue;

            MotifUI motifui = list.getOrAddMotifUIFor(motif);
        
            SelectButton newButton = new SelectButton(sequi, motifui, SelectUI.this, i);

            newButton.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    select(newButton);
                    }
                });
                                                
            newButton.setAuxiliary(children.get(i));

            MotifButton button = (MotifButton)(selectGrid.getComponent(i));
            button.disconnect();

            // Copy over the button
            selectGrid.remove(i);                                  
            selectGrid.add(newButton, i);
            }
        }


    public void buildInspectors(JScrollPane scroll)
        {
        // Build the series child inspector holder
        childOuter = new JPanel();
        childOuter.setLayout(new BorderLayout());
        childBorder = BorderFactory.createTitledBorder(null, "Child");
        childOuter.setBorder(childBorder);
                
        // Build the select inspector holder
        selectOuter = new JPanel();
        selectOuter.setLayout(new BorderLayout());
        selectBorder = BorderFactory.createTitledBorder(null, "Select");
        selectOuter.setBorder(selectBorder);

        // Add the select inspector
        selectInspector = new SelectInspector(seq, select, this);
        selectOuter.add(selectInspector, BorderLayout.CENTER);
                
        // Fill the panes
        inspectorPane = new JPanel();
        inspectorPane.setLayout(new BorderLayout());
        inspectorPane.add(selectOuter, BorderLayout.NORTH);
        inspectorPane.add(childOuter, BorderLayout.CENTER);
                
        scroll.setViewportView(inspectorPane);
        }
                
    boolean loadingChildren = false;
    public void buildPrimary(JScrollPane scroll)
        {
        scrollPane = scroll;
        JPanel outer = new JPanel();
        outer.setLayout(new BorderLayout());
        outer.add(selectGrid, BorderLayout.NORTH);
        outer.setBackground(BACKGROUND);
        JPanel outer2 = new JPanel();
        outer2.setLayout(new BorderLayout());
        outer2.add(outer, BorderLayout.WEST);
        outer2.setBackground(BACKGROUND);
        scroll.setViewportView(outer2);
        
        outer = new JPanel();
        outer.setLayout(new BorderLayout());
        outer.add(verticalHeader, BorderLayout.NORTH);
        outer.setBackground(BACKGROUND);
        scroll.setRowHeaderView(outer);

        outer = new JPanel();
        outer.setLayout(new BorderLayout());
        outer.add(horizontalHeader, BorderLayout.WEST);
        outer.setBackground(BACKGROUND);
        scroll.setColumnHeaderView(outer);

//        scroll.setViewportView(selectGrid);
//        scroll.setColumnHeaderView(horizontalHeader);
//        scroll.setRowHeaderView(verticalHeader);
        selectGrid.setDropTarget(new DropTarget(this, buildDropTargetListener()));

        seq.getLock().lock();
        try
            {
            if (!loadingChildren)
                {
                for(int i = 0; i < GRID_SIZE; i++)
                    {
                    select.replaceChild(new seq.motif.blank.Blank(seq), i);
                    }
                }
            }
        finally
            {
            seq.getLock().unlock();
            }

        for(int i = 0; i < GRID_SIZE; i++)
            {
            selectGrid.add(new SelectButton(sequi, this, i));
            }

        for(int i = 0; i < GRID_WIDTH; i++)
            {
            JLabel p = new JLabel("" + (i + 1))
                {
                public Dimension getPreferredSize() { return new Dimension(SelectButton.MINIMUM_SIZE, MIN_HEADER_SPACE); }
                public Dimension getMaximumSize() { return getPreferredSize(); }
                public Dimension getMinimumSize() { return getPreferredSize(); }
                };
            p.setHorizontalAlignment(JLabel.CENTER);
            p.setBorder(BorderFactory.createMatteBorder(0,0,1,0,HEADER_LINE_COLOR));
            horizontalHeader.add(p);
            
            p = new JLabel("" + (i + 1))
                {
                public Dimension getPreferredSize() { return new Dimension(MIN_HEADER_SPACE, SelectButton.MINIMUM_SIZE); }
                public Dimension getMaximumSize() { return getPreferredSize(); }
                public Dimension getMinimumSize() { return getPreferredSize(); }
                };
            p.setHorizontalAlignment(JLabel.CENTER);
            p.setBorder(BorderFactory.createMatteBorder(0,0,0,1,HEADER_LINE_COLOR));
            verticalHeader.add(p);
            
            Box box = new Box(BoxLayout.X_AXIS);
            box.add(box.createHorizontalStrut(4));
                                
            double init = 0.5;
            seq.getLock().lock();
            try
                {
                if (select.getOverrideParameters(i))
                    {
                    init = select.getPlayingParameter(i);
                    } 
                }
            finally
                {
                seq.getLock().unlock();
                }

            final int _i = i;
            dials[i] = new Dial(init)
                {
                public String map(double val)
                    {
                    return "<html><font size=1>" + super.map(val) + "</font></html>";
                    }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return (select.getOverrideParameters(_i) ? select.getPlayingParameter(_i) : 0.5); }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val)
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try 
                        { 
                        if (select.getOverrideParameters(_i)) { select.setPlayingParameter(_i, val); } 
                        if (select.getJoyX() == _i ||
                            select.getJoyY() == _i)
                            {
                            if (selectInspector != null)
                                {
                                selectInspector.updateJoystick(); 
                                }
                            }
                        }
                    finally { lock.unlock(); }
                    }
                };
            dials[i].setToolTipText(DIAL_TOOLTIP);
                
            boolean enabled = false;
            ReentrantLock lock = seq.getLock();
            lock.lock();
            try { enabled = select.getOverrideParameters(i); }
            finally { lock.unlock(); }
            dials[i].setEnabled(enabled);
            box.add(dials[i].getLabelledDial("0.8888"));
            box.add(p);
            verticalHeader.add(box);
            }
        //updateDials();
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
        removeButton.setToolTipText("Remove selected motif from select grid");

        PushButton copyButton = new PushButton(new StretchIcon(PushButton.class.getResource("icons/copy.png")))
            {
            public void perform()
                {
                doCopy();
                }
            };
        copyButton.getButton().setPreferredSize(new Dimension(24, 24));
        copyButton.setToolTipText("Copy selected motif in select grid");

        JPanel console = new JPanel();
        console.setLayout(new BorderLayout());
                
        Box addRemoveBox = new Box(BoxLayout.X_AXIS);
        //addRemoveBox.add(addButton);
        addRemoveBox.add(removeButton);
        addRemoveBox.add(copyButton);
        console.add(addRemoveBox, BorderLayout.WEST);   
                
        return console; 
        }
        
    public void play(SelectButton button)
        {
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            SelectClip playingClip = (SelectClip)(((SelectUI)(button.getOwner())).getDisplayClip());
            if (playingClip != null)
                {
                playingClip.post(button.getAt());
                }
            }
        finally
            {
            lock.unlock();
            }
        }

    public void deselectAll()
        {
        for(Component c : selectGrid.getComponents())
            {
            if (c instanceof MotifButton)
                {
                ((MotifButton)c).setSelected(false);
                }
            }
        setChildInspector(null);
        }
                
    public void select(MotifButton button)  
        {
        deselectAll();
        button.setSelected(true);
        Motif.Child node = (Motif.Child)(button.getAuxiliary());
        setChildInspector(new SelectChildInspector(seq, select, button.getMotifUI(), node, this));
        }

    public void setChildInspector(SelectChildInspector inspector)
        {
        childInspector = inspector;
        childOuter.removeAll();
        if (inspector != null) 
            {
            childOuter.add(inspector, BorderLayout.NORTH);
            int num = inspector.getChildNum();
            if (num < 0) // uh oh
                {
                childBorder.setTitle("Child [Error]");
                System.err.println("SetChildInspector error: childNum is -1, probably button.getAuxilliary returned null.");
                }
            else
                {
                childBorder.setTitle("Child (" + (num % 8 + 1) + ", " + (num / 8 + 1) + ")");
                }
            }
        else
            {
            childBorder.setTitle("Child");
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
            int num = childInspector.getChildNum();
            if (num < 0) // uh oh
                {
                childBorder.setTitle("Child [Error]");
                System.err.println("SetChildInspector error: childNum is -1, probably button.getAuxilliary returned null.");
                }
            else
                {
                childBorder.setTitle("Child (" + (num % 8 + 1) + ", " + (num / 8 + 1) + ")");
                }
            childOuter.setBorder(null);             // this has to be done or it won't immediately redraw!
            childOuter.setBorder(childBorder);
            childInspector.revise();
            }
        }
        
    public void redraw(boolean inResponseToStep) 
        {
        updateText();
        ReentrantLock lock = seq.getLock();
        for(int i = 0; i < Select.NUM_PARAMETERS; i++)
            {
            boolean override = false;
            lock.lock();
            try
                {
                override = select.getOverrideParameters(i);
                }
            finally
                {
                seq.getLock().unlock();
                }
            if (override)
                {
                dials[i].redraw();
                }
            }
        super.redraw(inResponseToStep);
        }
    
    public void updateText()
        {
        super.updateText();
        for(Component c : selectGrid.getComponents())
            {
            if (c instanceof SelectButton)
                {
                ((SelectButton)c).updateText();
                }
            }
        }
        
    JPanel stub = new JPanel();
    public void moveChild(SelectButton button, int to)
        {
        // Where is the button?
        Component[] c = selectGrid.getComponents();
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
                select.swapChild(from, to);
                }
            finally
                {
                seq.getLock().unlock();
                }
            
            // To swap, to be careful we'll first remove TO and replace with
            // a stub.  Then we'll remove FROM and replace with TO.  Then we'll
            // remove the stub and replace with FROM.
            SelectButton toButton = (SelectButton)c[to];
            selectGrid.remove(to);
            selectGrid.add(stub, to);
            
            selectGrid.remove(from);
            selectGrid.add(toButton, from);
            
            selectGrid.remove(to);
            selectGrid.add(button, to);
            
            button.setAt(to);
            toButton.setAt(from);
              
            select(button);
            sequi.getMotifList().rebuildClipsForMotif(getMotif());
            selectGrid.revalidate();
            }
        else System.err.println("SelectUI.moveChild: button not in list");
        updatePads();
        }

    public void doRemove()
        {
        // Where is the button?
        Component[] c = selectGrid.getComponents();
        int at = -1;
        for(int i = 0; i < c.length; i++)
            {
            if (((SelectButton)c[i]).isSelected()) { at = i; break; }
            }
        if (at != -1)
            {
            sequi.push();
            seq.getLock().lock();
            try
                {
                select.replaceChild(new seq.motif.blank.Blank(seq), at);
                }
            finally
                {
                seq.getLock().unlock();
                }

            MotifButton button = (MotifButton)(selectGrid.getComponent(at));
            button.disconnect();
                        
            // We'll swap in a blank SelectButton
            selectGrid.remove(at);
            SelectButton blankButton = new SelectButton(sequi, this, at);
            selectGrid.add(blankButton, at);
            deselectAll();

            sequi.getMotifList().rebuildClipsForMotif(getMotif());
            selectGrid.revalidate();
            }
        else System.err.println("SelectUI.moveChild: button not in list");
        updatePads();
        }
              
    public void doCopy()
        {
        // Where is the button?
        Component[] c = selectGrid.getComponents();
        int from = -1;
        Motif.Child child = null;

        for(int i = 0; i < c.length; i++)
            {
            if (((SelectButton)c[i]).isSelected()) { from = i; break; }
            }
        if (from != -1)
            {
            // First make sure the copy is there
            MotifUI original = ((SelectButton)c[from]).getMotifUI();
            if (original == null) return;               // it's a blank button

            // where do we put it?

            int to = -1;
            for(int x = 1; x < GRID_SIZE; x++)
                {
                int possibility = from + x;
                if (possibility >= GRID_SIZE) possibility -= GRID_SIZE;
           
                // Next make sure that there is space
                MotifUI neighbor = ((SelectButton)c[possibility]).getMotifUI();
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
                select.copyChild(from, to);
                child = select.getChildren().get(to);
                }
            finally
                {
                seq.getLock().unlock();
                }

            // Copy over the button
            selectGrid.remove(to);
            SelectButton newButton = new SelectButton(sequi, original, this, to);

            newButton.setAuxiliary(child);

            newButton.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    select(newButton);
                    }
                });
                        
            selectGrid.add(newButton, to);
            select(newButton);            
            selectGrid.revalidate();
            sequi.getMotifList().rebuildClipsForMotif(getMotif());
            }
        else System.err.println("SelectUI.doCopy: button not in list");
        updatePads();
        }

    public void addChild(MotifUI motifui, int at)
        {  
        SelectButton newButton = new SelectButton(sequi, motifui, SelectUI.this, at);
        Motif.Child child = null;

        newButton.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                select(newButton);
                }
            });
                        
        MotifButton button = (MotifButton)(selectGrid.getComponent(at));
        button.disconnect();
                
        seq.getLock().lock();
        try
            {
            if (motifui.getMotif().containsOrIs(getMotif()))    // No cycles permitted!
                return;
                
            select.replaceChild(motifui.getMotif(), at);
            child = select.getChildren().get(at);
            }
        finally
            {
            seq.getLock().unlock();
            }
        
        newButton.setAuxiliary(child);

        // Copy over the button
        selectGrid.remove(at);                                  
        selectGrid.add(newButton, at);
        select(newButton);            
        newButton.updateText();
        
        revalidate();
        sequi.getMotifList().rebuildClipsForMotif(getMotif());
        updatePads();
        }

    public void updatePads()
        {
        Clip clip = getDisplayClip();
        seq.getLock().lock();
        try
            {
            if (clip instanceof SelectClip)
                {
                SelectClip playingClip = (SelectClip)clip;
                playingClip.updatePads();
                }
            }
        finally
            {
            seq.getLock().unlock();
            }
        }
                

    public void clearPads()
        {
        Clip clip = getDisplayClip();
        seq.getLock().lock();
        try
            {
            if (clip instanceof SelectClip)
                {
                SelectClip playingClip = (SelectClip)clip;
                playingClip.clearPads();
                }
            }
        finally
            {
            seq.getLock().unlock();
            }
        }
                

    public void uiWasSet()
        {
        super.uiWasSet();
        updatePads();
        }

    public void uiWasUnset()
        {
        super.uiWasUnset();
        clearPads();
        }
    
    //// DRAG AND DROP
        
    public DropTargetListener buildDropTargetListener() { return new SelectUIDropTargetListener(); }
        
    class SelectUIDropTargetListener extends DropTargetAdapter 
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
                    else if (comp instanceof SelectButton)
                        {
                        int at = ((SelectButton)comp).getAt();
                        sequi.push();
                        if (dropped instanceof SelectButton)
                            {
                            moveChild((SelectButton)dropped, at);
                            }
                        else
                            {
                            addChild(dropped.getMotifUI(), at);
                            }
                        }
                    selectGrid.revalidate(); 
                    selectGrid.repaint();
                                                
                    /// FIXME: This doesn't handle drag-and-drop within the select
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
        seq.setupForMIDI(SelectClip.class, args, 1, 2);   // sets up MIDI in and out
        seq.setOut(0, new Out(seq, 0));         // Out 0 points to device 0 in the tuple.  This is too complex.
        seq.setOut(1, new Out(seq, 1));         // Out 0 points to device 0 in the tuple.  This is too complex.
        
        // Set up our module structure
        Select select = new Select(seq);

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

        // Load into select
        //select.add(dSeq, 0, 0.5);
        //select.add(dSeq2, 1);
        //select.add(dSeq, 2);
                
        // Build Clip Tree
        seq.setData(select);

        // Build GUI
        SeqUI ui = new SeqUI(seq);

        seq.motif.stepsequence.gui.StepSequenceUI ssui = new seq.motif.stepsequence.gui.StepSequenceUI(seq, ui, dSeq);
        seq.motif.stepsequence.gui.StepSequenceUI ssui2 = new seq.motif.stepsequence.gui.StepSequenceUI(seq, ui, dSeq2);
        SelectUI selectui = new SelectUI(seq, ui, select);
        selectui.addChild(ssui, 0);
        selectui.addChild(ssui2, 1);
        selectui.addChild(ssui, 2);
        ui.addMotifUI(selectui);
        ui.addMotifUI(ssui);
        ui.addMotifUI(ssui2);
        seq.sequi = ui;
        JFrame frame = new JFrame();
        frame.getContentPane().add(ui);
        frame.pack();
        frame.show();

        seq.reset();
//      select.revise();
//        selectui.revise();
                    
        //Toolkit.getDefaultToolkit().getSystemEventQueue().push(new MyEventQueue());
        
        //seq.play();

        //seq.waitUntilStopped();
        }


	public static final String DIAL_TOOLTIP = "<html><b>Parameter Dial</b><br>" +
        "Sets the value of the given parameter of the playing child or children.</html>";
    }
