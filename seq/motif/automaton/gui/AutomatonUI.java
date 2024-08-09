package seq.motif.automaton.gui;

import seq.motif.automaton.*;
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



public class AutomatonUI extends MotifUI
    {
    Automaton automaton;

    ArrayList<InputJack> inputJacks = new ArrayList<>();
    public void addInputJack(InputJack jack) { inputJacks.add(jack); }
    public ArrayList<InputJack> getInputJacks() { return inputJacks; }

    ArrayList<OutputJack> outputJacks = new ArrayList<>();
    public void addOutputJack(OutputJack jack) { outputJacks.add(jack); }
    public void removeOutputJack(OutputJack jack) { outputJacks.remove(jack); }
    
    AutomatonNodeInspector nodeInspector;
    
    JMenu menu;
    
    class AutomatonGrid extends JPanel
        {
        public void paint(Graphics g)
            {
            super.paint(g);
                
            Graphics2D g2D = (Graphics2D) g;
            for(Component c : getComponents())
                {
                AutomatonButton.ButtonHolder holder = (AutomatonButton.ButtonHolder) c;
                AutomatonButton automatonbutton = holder.button;
                for(OutputJack jack : automatonbutton.outputJacks)
                    {
                    Wire outgoing = jack.getOutgoing();
                    if (outgoing != null) outgoing.draw(g2D);
                    }
                }
            } 
        };
        
    JPanel childOuter;
    TitledBorder childBorder;
    JPanel inspectorPane;
        
    JPanel automatonOuter;
    TitledBorder automatonBorder;
    AutomatonInspector automatonInspector;
    
    public static final int GRID_WIDTH = 8;
    public static final int GRID_SIZE = GRID_WIDTH * GRID_WIDTH;
    public static final int MIN_HEADER_SPACE = 16;
    public static final Color HEADER_LINE_COLOR = new Color(200, 200, 200);
        
    AutomatonGrid automatonGrid = new AutomatonGrid();
    JPanel nodeGrid = new JPanel();
    
    public AutomatonGrid getAutomatonGrid() { return automatonGrid; }
    
    public Automaton getAutomaton() { return automaton; }
                
    public static ImageIcon getStaticIcon() { return new ImageIcon(MotifUI.class.getResource("icons/automaton.png")); }        // don't ask
    public ImageIcon getIcon() { return getStaticIcon(); }
    public static String getType() { return "Automaton"; }
    public static MotifUI create(Seq seq, SeqUI ui)
        {
        return new AutomatonUI(seq, ui, new Automaton(seq));
        }
        
    public static MotifUI create(Seq seq, SeqUI ui, Motif motif)
        {
        return new AutomatonUI(seq, ui, (Automaton)motif);
        }
        
    public AutomatonUI(Seq seq, SeqUI sequi, Automaton automaton)
        {
        super(seq, sequi, automaton);
        this.seq = seq;
        this.automaton = automaton;
        this.sequi = sequi;
        //build();
        }
 
    public void buildMenu()
        {
        menu = new JMenu("Automaton");
        JMenuItem load = new JMenuItem("Make Start Node");
        load.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                AutomatonButton button = getSelectedButton();
                if (button == null) return;                             // FIXME: maybe popup a warning dialog?
                seq.push();
                seq.getLock().lock();
                try
                    {
                    automaton.setStart(button.getNode());
                    }
                finally { seq.getLock().unlock(); }
                
                // reset all the buttons 
                select(button);                 // redraws its inspector to reflect the change
                resetStart();                   // re-assigns status to each of the buttons
                redraw();
                }
            });
        menu.add(load);
        }
                
    public JMenu getMenu()
        {
        return menu;
        }
        

    public void resetStart()
        {
        for(Component c : automatonGrid.getComponents())
            {
            AutomatonButton.ButtonHolder holder = (AutomatonButton.ButtonHolder) c;
            AutomatonButton automatonbutton = holder.button;
            automatonbutton.reviseToStart();
            }
        }
        
    public void buildInspectors(JScrollPane scroll)
        {
        // Build the series child inspector holder
        childOuter = new JPanel();
        childOuter.setLayout(new BorderLayout());
        childBorder = BorderFactory.createTitledBorder(null, "Node");
        childOuter.setBorder(childBorder);
                
        // Build the automaton inspector holder
        automatonOuter = new JPanel();
        automatonOuter.setLayout(new BorderLayout());
        automatonBorder = BorderFactory.createTitledBorder(null, "Automaton");
        automatonOuter.setBorder(automatonBorder);

        // Add the automaton inspector
        automatonInspector = new AutomatonInspector(seq, automaton, this);
        automatonOuter.add(automatonInspector, BorderLayout.CENTER);
                
        // Fill the panes
        inspectorPane = new JPanel();
        inspectorPane.setLayout(new BorderLayout());
        inspectorPane.add(automatonOuter, BorderLayout.NORTH);
        inspectorPane.add(childOuter, BorderLayout.CENTER);
                
        scroll.setViewportView(inspectorPane);
        }
        
    public JPanel buildHeader()
        {
        nodeGrid.removeAll();
        nodeGrid.add(new AutomatonNodeButton(AutomatonNodeButton.TYPE_DELAY));
        nodeGrid.add(new AutomatonNodeButton(AutomatonNodeButton.TYPE_CHORD));
        nodeGrid.add(new AutomatonNodeButton(AutomatonNodeButton.TYPE_FINISHED));
        nodeGrid.add(new AutomatonNodeButton(AutomatonNodeButton.TYPE_ITERATE));
        nodeGrid.add(new AutomatonNodeButton(AutomatonNodeButton.TYPE_RANDOM));
        nodeGrid.add(new AutomatonNodeButton(AutomatonNodeButton.TYPE_FORK));
        nodeGrid.add(new AutomatonNodeButton(AutomatonNodeButton.TYPE_JOIN));
        return nodeGrid;
        }
        
    public AutomatonButton getButton(int i)
        {
        AutomatonButton.ButtonHolder holder = (AutomatonButton.ButtonHolder)(automatonGrid.getComponent(i));
        return holder.button;
        }
                
    public void buildPrimary(JScrollPane scroll)
        {
        JPanel outer = new JPanel();
        outer.setLayout(new BorderLayout());
        outer.add(automatonGrid, BorderLayout.NORTH);
        outer.setBackground(BACKGROUND);
        JPanel outer2 = new JPanel();
        outer2.setLayout(new BorderLayout());
        outer2.add(outer, BorderLayout.WEST);
        outer2.setBackground(BACKGROUND);
        scroll.setViewportView(outer2);
        
        automatonGrid.setLayout(new GridLayout(GRID_WIDTH, GRID_WIDTH));
        automatonGrid.setDropTarget(new DropTarget(this, buildDropTargetListener()));

        seq.getLock().lock();
        try
            {
            automatonGrid.removeAll();          // just in case?

            for(int i = 0; i < GRID_SIZE; i++)
                {
                AutomatonButton button = new AutomatonButton(sequi, this, i);
                automatonGrid.add(button.getButtonHolder());
                }

            // Load nodes
            for(Automaton.Node node : automaton.getNodes())
                {
                int i = node.getPosition();
                if (i < 0)      // not been assigned yet?  Assign it to 0 for the moment
                    i = 0;
                        
                while(!getButton(i).isBlank())
                    i++;            // find a spot that's empty
                                        
                automatonGrid.remove(i);
                AutomatonButton newButton = AutomatonButton.buildButton(sequi, node, this);
                newButton.setAt(i);
                automatonGrid.add(newButton.getButtonHolder(), i);
                newButton.addActionListener(new ActionListener()
                    {
                    public void actionPerformed(ActionEvent e)
                        {
                        select(newButton);
                        }
                    });
                }
                
            // Load edges
            for(Automaton.Node node : automaton.getNodes())
                {
                int pos = node.getPosition();
                AutomatonButton button = getButton(pos);
                Automaton.Node[] out = node.getOut();
                for(int i = 0; i < out.length; i++)
                    {
                    if (out[i] != null)
                        {
                        AutomatonButton toButton = getButton(out[i].getPosition());

                        // Build a Wire
                        Wire wire = new Wire(automatonGrid);
                        wire.setStart(button.outputJacks[i]);
                        wire.setEnd(toButton.inputJack);
                        button.outputJacks[i].outgoing = wire;
                        toButton.inputJack.getIncoming().add(wire);
                        }
                    }
                }
            }
        finally
            {
            seq.getLock().unlock();
            }
            
        resetStart();
        automatonGrid.repaint();
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
        PushButton copyButton = new PushButton(new StretchIcon(PushButton.class.getResource("icons/copy.png")))
            {
            public void perform()
                {
                doCopy();
                }
            };
        copyButton.getButton().setPreferredSize(new Dimension(24, 24));

        JPanel console = new JPanel();
        console.setLayout(new BorderLayout());
                
        Box addRemoveBox = new Box(BoxLayout.X_AXIS);
        addRemoveBox.add(removeButton);
        addRemoveBox.add(copyButton);
        console.add(addRemoveBox, BorderLayout.WEST);   
                
        return console; 
        }
        
    public void deselectAll()
        {
        for(Component c : automatonGrid.getComponents())
            {
            AutomatonButton.ButtonHolder holder = (AutomatonButton.ButtonHolder) c;
            
            if (holder.button instanceof MotifButton)
                {
                ((MotifButton)(holder.button)).setSelected(false);
                }
            }
        setNodeInspector(null);
        }
                
    public void select(MotifButton button)  
        {
        deselectAll();
        button.setSelected(true);
        Automaton.Node node = (Automaton.Node)(button.getAuxiliary());
        setNodeInspector(new AutomatonNodeInspector(seq, automaton, button, this, node));
        }
        
    public AutomatonButton getSelectedButton()
        {
        for(Component c : automatonGrid.getComponents())
            {
            if (c instanceof AutomatonButton.ButtonHolder)
                {
                AutomatonButton button = ((AutomatonButton.ButtonHolder)c).getButton();
                if (button.isSelected()) // got it
                    return button;
                }
            }
        return null;
        }

    public void setNodeInspector(AutomatonNodeInspector inspector)
        {
        nodeInspector = inspector;
        childOuter.removeAll();
        if (inspector != null) 
            {
            childOuter.add(inspector, BorderLayout.NORTH);
            int num = inspector.getNodeNum();
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
        }

    public void revise()
        {
        if (nodeInspector != null) 
            {
            int num = nodeInspector.getNodeNum();
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
            nodeInspector.revise();
            }
        }
        
    public void redraw() 
        {
        updateText();
        super.redraw();
        }
    
    public void updateText()
        {
        super.updateText();
        
        for(Component c : automatonGrid.getComponents())
            {
            AutomatonButton.ButtonHolder holder = (AutomatonButton.ButtonHolder)c;
            holder.button.updateText();
            }
        }
        
    JPanel stub = new JPanel();
    public void doMove(AutomatonButton button, int to)
        {
        int from = button.getAt();
        if (from < 0)           // uhmmmm...
            {
            System.err.println("AutomatonUI.doMove: button not in list");
            return;
            }
                
        AutomatonButton toButton = getButton(to);
        if (!toButton.isBlank())
            {
            // FIXME: Cannot move, there's something already there
            System.err.println("AutomatonUI.doMove: cannot move on top of existing node");
            return;
            }
        
        /*
        // Do the swap in the Automaton
        seq.getLock().lock();
        try
        {
        int toPos = toButton.getAt();
        toButton.setAt(button.getAt());
        button.setAt(toPos);
        }
        finally
        {
        seq.getLock().unlock();
        }
        */
                        
        // Do the swap in the grid
        //
        // To swap, to be careful we'll first remove TO and replace with
        // a stub.  Then we'll remove FROM and replace with TO.  Then we'll
        // remove the stub and replace with FROM.
        automatonGrid.remove(to);
        automatonGrid.add(stub, to);
                
        automatonGrid.remove(from);
        automatonGrid.add(toButton.getButtonHolder(), from);
                
        automatonGrid.remove(to);
        automatonGrid.add(button.getButtonHolder(), to);
                
        button.setAt(to);
        toButton.setAt(from);

        select(button);
        sequi.getMotifList().rebuildClipsForMotif(getMotif());
        automatonGrid.revalidate();
        updateText();
        }

    public void doRemove()
        {
        // Where is the button?
        Component[] c = automatonGrid.getComponents();
        int at = -1;
        for(int i = 0; i < c.length; i++)
            {
            //if (((AutomatonButton)c[i]).isSelected()) { at = i; break; }
            if (getButton(i).isSelected()) { at = i; break; }
            }
        if (at < 0)
            {
            System.err.println("AutomatonUI.doRemove: button not in list");
            return;
            }

        AutomatonButton button = getButton(at);
        if (button.isBlank())
            {
            System.err.println("AutomatonUI.doRemove: cannot remove a blank node");
            return;
            }
        
        seq.push();
        // First, we remove all incoming and outgoing edges
        button.disconnectJacks();
        
        seq.getLock().lock();
        try
            {
            automaton.remove(button.getNode());
            }
        finally
            {
            seq.getLock().unlock();
            }
                        
        button.disconnect();
                        
        // We'll swap in a blank AutomatonButton
        automatonGrid.remove(at);
        AutomatonButton blankButton = new AutomatonButton(sequi, this, at);
        automatonGrid.add(blankButton.getButtonHolder(), at);
        deselectAll();

        resetStart(); // maybe we deleted the start node
                
        sequi.getMotifList().rebuildClipsForMotif(getMotif());
        automatonGrid.revalidate();
        automatonGrid.repaint();                // FIXME we have to repaint EVERYTHING
        updateText();
        // FIXME: do we need to rebuild the inspectors too?
        }
              
    public void doCopy()
        {
        // Where is the button?
        Component[] c = automatonGrid.getComponents();
        int from = -1;
        for(int i = 0; i < c.length; i++)
            {
            //if (((AutomatonButton)c[i]).isSelected()) { from = i; break; }
            if (getButton(i).isSelected()) { from = i; break; }
            }
        if (from < 0)
            {
            System.err.println("AutomatonUI.doRemove: button not in list");
            return;
            }

        AutomatonButton fromButton = getButton(from);
        
        // where do we put it?
        int to = -1;
        for(int x = 1; x < GRID_SIZE; x++)
            {
            int possibility = from + x;
            if (possibility >= GRID_SIZE) possibility -= GRID_SIZE;
           
            // Next make sure that there is space
            //AutomatonButton toButton = (AutomatonButton)c[possibility];
            AutomatonButton toButton = getButton(possibility);
            if (toButton.isBlank()) { to = possibility; break; }
            }
                
        // did we find a spot?
        if (to == -1)
            {
            sequi.showSimpleError("Error Duplicating", "There was no space available to add a duplicate.");
            return;
            }

        seq.push();
        // okay, we can make the copy, go ahead and do it
        seq.getLock().lock();
        Automaton.Node node = null;
        try
            {
            node = fromButton.getNode().copy();
            automaton.addNode(node);
            }
        finally
            {
            seq.getLock().unlock();
            }

        // Copy over the button
        AutomatonButton newButton = AutomatonButton.buildButton(sequi, node, this);
        newButton.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                select(newButton);
                }
            });
        AutomatonButton oldButton = getButton(to);
        automatonGrid.remove(to);
        oldButton.disconnect();
        newButton.setAt(to);
        automatonGrid.add(newButton.getButtonHolder(), to);
        newButton.reviseToStart();     
        select(newButton);            
        automatonGrid.revalidate();
        sequi.getMotifList().rebuildClipsForMotif(getMotif());
        updateText();
        }

    public void doAdd(int type, int at)
        {
        boolean isStart = false;
        
        AutomatonButton toButton = getButton(at);
        if (!toButton.isBlank())
            {
            // FIXME: Cannot move, there's something already there
            System.err.println("AutomatonUI.doMove: cannot move on top of existing node");
            return;
            }
        
        // Do the swap in the Automaton
        Automaton.Node node = null;
        seq.getLock().lock();
        try
            {
            if (type == AutomatonNodeButton.TYPE_DELAY) node = automaton.addDelay();
            else if (type == AutomatonNodeButton.TYPE_CHORD) node = automaton.addChord();
            else if (type == AutomatonNodeButton.TYPE_FINISHED) node = automaton.addFinished();
            else if (type == AutomatonNodeButton.TYPE_ITERATE) node = automaton.addIterate();
            else if (type == AutomatonNodeButton.TYPE_RANDOM) node = automaton.addRandom();
            else if (type == AutomatonNodeButton.TYPE_FORK) node = automaton.addFork();
            else if (type == AutomatonNodeButton.TYPE_JOIN) node = automaton.addJoin();
            else
                {
                System.err.println("AutomatonUI.doAdd: unknown type " + type);
                return;
                }
            }
        finally
            {
            seq.getLock().unlock();
            }
                        
        AutomatonButton newButton = AutomatonButton.buildButton(sequi, node, this);
        newButton.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                select(newButton);
                }
            });

        AutomatonButton button = getButton(at);
        automatonGrid.remove(at);
        button.disconnect();
        
        newButton.setAt(at);
        automatonGrid.add(newButton.getButtonHolder(), at);
        select(newButton);       
        newButton.reviseToStart();     
        revalidate();
        repaint();
        sequi.getMotifList().rebuildClipsForMotif(getMotif());
        updateText();
        }

    public void doAdd(MotifUI motifui, int at)
        {
        AutomatonButton toButton = getButton(at);
        if (!toButton.isBlank())
            {
            // FIXME: Cannot move, there's something already there
            System.err.println("AutomatonUI.doMove: cannot move on top of existing node");
            return;
            }
        
        // Do the swap in the Automaton
        Automaton.MotifNode motifnode = null;
        seq.getLock().lock();
        try
            {
            motifnode = automaton.add(motifui.getMotif());
            }
        finally
            {
            seq.getLock().unlock();
            }
                        
        AutomatonButton newButton = new AutomatonButton(sequi, motifnode, motifui, AutomatonUI.this, at);
        newButton.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                select(newButton);
                }
            });
        
        AutomatonButton button = getButton(at);
        automatonGrid.remove(at);
        button.disconnect();
        
        newButton.setAt(at);
        automatonGrid.add(newButton.getButtonHolder(), at);
        select(newButton);            
        newButton.reviseToStart();     
        revalidate();
        repaint();
        sequi.getMotifList().rebuildClipsForMotif(getMotif());
        updateText();
        }
        
    public void launchThread(Automaton.Node node)
        {
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            AutomatonClip playingClip = (AutomatonClip)(getDisplayClip());
            if (playingClip != null)
                {
                playingClip.launchThread(node);
                }
            }
        finally
            {
            lock.unlock();
            }
        }

    
    //// DRAG AND DROP FOR AUTOMATON GRID
        
    public DropTargetListener buildDropTargetListener() { return new AutomatonUIDropTargetListener(); }
        
    class AutomatonUIDropTargetListener extends DropTargetAdapter 
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
                /*
                  Component comp = dtde.getDropTargetContext().getComponent();
                  if (comp == null) return;
                  else if (comp instanceof JPanel)
                  {
                  deselectAll();
                  }
                */
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
                    else if (dtde.getTransferable().isDataFlavorSupported(AutomatonNodeButton.dataFlavor))
                        {
                        transferableObj = dtde.getTransferable().getTransferData(AutomatonNodeButton.dataFlavor);
                        } 
                    } 
                catch (Exception ex) {  System.err.println("Can't drag and drop that"); }
                                
                if (transferableObj != null && transferableObj instanceof MotifButton)
                    {
                    MotifButton dropped = (MotifButton)transferableObj;
                                
                    seq.getLock().lock();
                    try
                        {
                        if (!(dropped instanceof AutomatonButton))  // AutomatonButtons have other kinds of nodes and can't be recursive anyway
                            {
                            if (dropped.getMotifUI().getMotif().containsOrIs(getMotif()))   // can't add me into me etc.
                                {
                                recursiveDragError(dropped, sequi);
                                return;
                                }
                            }
                        }
                    finally
                        {
                        seq.getLock().unlock();
                        }
                                
                    Component comp = dtde.getDropTargetContext().getComponent();
                    if (comp == null) return;
                    else if (comp instanceof AutomatonButton)
                        {
                        int at = ((AutomatonButton)comp).getAt();
                        seq.push();
                        if (dropped instanceof AutomatonButton)
                            {
                            doMove((AutomatonButton)dropped, at);
                            }
                        else
                            {
                            doAdd(dropped.getMotifUI(), at);
                            }
                        }
                    automatonGrid.revalidate(); 
                    automatonGrid.repaint();
                                                
                    /// FIXME: This doesn't handle drag-and-drop within the automaton
                    }
                else if (transferableObj != null && transferableObj instanceof AutomatonNodeButton)
                    {
                    AutomatonNodeButton dropped = (AutomatonNodeButton)transferableObj;
                                
                    Component comp = dtde.getDropTargetContext().getComponent();
                    if (comp == null) return;
                    else if (comp instanceof AutomatonButton)
                        {
                        int at = ((AutomatonButton)comp).getAt();
                        seq.push();
                        if (dropped instanceof AutomatonNodeButton)
                            {
                            doAdd(((AutomatonNodeButton)dropped).getType(), at);
                            }
                        }
                    automatonGrid.revalidate(); 
                    automatonGrid.repaint();               
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
        seq.setupForMIDI(AutomatonClip.class, args, 1, 2);   // sets up MIDI in and out
        seq.setOut(0, new Out(seq, 0));         // Out 0 points to device 0 in the tuple.  This is too complex.
        seq.setOut(1, new Out(seq, 1));         // Out 0 points to device 0 in the tuple.  This is too complex.
        
        // Set up our module structure
        Automaton automaton = new Automaton(seq);

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

        // Load into automaton
        //automaton.add(dSeq, 0, 0.5);
        //automaton.add(dSeq2, 1);
        //automaton.add(dSeq, 2);
                
        // Build Clip Tree
        seq.setData(automaton);

        // Build GUI
        SeqUI ui = new SeqUI(seq);

        seq.motif.stepsequence.gui.StepSequenceUI ssui = new seq.motif.stepsequence.gui.StepSequenceUI(seq, ui, dSeq);
        seq.motif.stepsequence.gui.StepSequenceUI ssui2 = new seq.motif.stepsequence.gui.StepSequenceUI(seq, ui, dSeq2);
        AutomatonUI automatonui = new AutomatonUI(seq, ui, automaton);
        automatonui.doAdd(ssui, 0);
        automatonui.doAdd(ssui2, 1);
        automatonui.doAdd(ssui, 2);
        ui.addMotifUI(automatonui);
        ui.addMotifUI(ssui);
        ui.addMotifUI(ssui2);
        seq.sequi = ui;
        JFrame frame = new JFrame();
        ui.setupMenu(frame);
        frame.getContentPane().add(ui);
        frame.pack();
        frame.setVisible(true);

        seq.reset();
//      automaton.revise();
        automatonui.revise();
                    
        //Toolkit.getDefaultToolkit().getSystemEventQueue().push(new MyEventQueue());
        
        //seq.play();

        //seq.waitUntilStopped();
        }

    }
