/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.automaton.gui;

import seq.engine.*;
import seq.gui.*;
import seq.motif.automaton.*;
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


public class AutomatonButton extends MotifButton
    {
    public static final Color START_COLOR = new Color(128, 200, 128); // new Color(0x20, 0xFF, 0x20);
    public static final int MINIMUM_SIZE = 80;
    int at = -1;                // default
    
    public static class ButtonHolder extends JPanel
        {
        AutomatonButton button;
        public AutomatonButton getButton() { return button; }
        };
    
    ButtonHolder buttonHolder;
    
    Box buildTransparentColumn()
        {
        Box transparentColumn = new Box(BoxLayout.Y_AXIS)
            {
            public Dimension getMinimumSize()
                {
                return new Dimension(InputOutput.LABELLED_DIAL_WIDTH(), InputOutput.LABELLED_DIAL_WIDTH());
                }
                
            public Dimension getPreferredSize()
                {
                return new Dimension(InputOutput.LABELLED_DIAL_WIDTH(), InputOutput.LABELLED_DIAL_WIDTH());
                }
            };
        transparentColumn.setOpaque(false);
        return transparentColumn;
        }
    
    public JPanel buildButtonHolder()
        {
        buttonHolder = new ButtonHolder();
        buttonHolder.setOpaque(false);
        buttonHolder.setLayout(new BorderLayout());
        buttonHolder.add(this, BorderLayout.CENTER);
        buttonHolder.button = this;
        
        // load jacks
        Box inputPanel = buildTransparentColumn();
        if (inputJack != null) 
            {
            inputPanel.add(inputJack);
            ((AutomatonUI)owner).addInputJack(inputJack);
            }
        buttonHolder.add(inputPanel, BorderLayout.WEST);

        Box outputPanel = buildTransparentColumn();
        if (outputJacks != null) 
            {
            for(int i = 0; i < outputJacks.length; i++)
                {
                outputPanel.add(outputJacks[i]);
                ((AutomatonUI)owner).addOutputJack(outputJacks[i]);
                }
            }
        buttonHolder.add(outputPanel, BorderLayout.EAST);
                
        return buttonHolder;
        }
        
    public JPanel getButtonHolder() { return buttonHolder; }
        
    public OutputJack[] outputJacks;
    public InputJack inputJack = null;
    
    public void disconnectJacks()
        {
        if (inputJack != null)
            {
            inputJack.disconnectAll();
            }
        for(int i = 0; i < outputJacks.length; i++)
            {
            if (outputJacks[i] != null) outputJacks[i].disconnect();
            }
        }
    
    // Changes the color depending on whether we're start or not
    public void reviseToStart()
        {
        AutomatonUI automatonui = (AutomatonUI) owner;
        Automaton.Node node = getNode();
        if (node == null) setBackground(null);
        else
            {
            sequi.getSeq().getLock().lock();
            try
                {
                if (automatonui.automaton.getStart() == node)
                    setBackground(START_COLOR);
                else setBackground(null);
                }
            finally
                {
                sequi.getSeq().getLock().unlock();
                }
            }
        }
        
    public int getAt() { return at; }
    public void setAt(int val) 
        {
        at = val; 
        
        if (getNode() != null)
            {
            sequi.getSeq().getLock().lock();
            try
                {
                getNode().setPosition(at);
                }
            finally
                {
                sequi.getSeq().getLock().unlock();
                }
            }
        }
    
    public Dimension getMinimumSize() { return new Dimension(MINIMUM_SIZE, MINIMUM_SIZE); }
    public Dimension getMaximumSize() { return getMinimumSize(); }
    public Dimension getPreferredSize() { return getMinimumSize(); }
    
    public boolean isBlank() { return getNode() == null; }
    public Automaton.Node getNode() { return (Automaton.Node)getAuxiliary(); }
    
    // Make a "Blank" button
    public AutomatonButton(SeqUI sequi, AutomatonUI owner, int at)   
        {
        super();
        
        this.sequi = sequi;
        this.owner = owner;
        this.at = at;           // it's okay, we're building

        setEnabled(false);
        setDropTarget(new DropTarget(this, owner.buildDropTargetListener()));
        outputJacks = new OutputJack[0];
        inputJack = null;
        
        buildButtonHolder();
        updateText();           // MotifButton does this but it's too early
        }
        
    // Make a Button for a Motif
    public AutomatonButton(SeqUI sequi, Automaton.Node node, MotifUI motifui, AutomatonUI owner, int at)
        {
        super(sequi, motifui, owner);
        this.at = at;           // it's okay, we're building
        setAuxiliary(node);

        inputJack = new InputJack(0, owner.getAutomatonGrid(), this);
        outputJacks = new OutputJack[1];
        outputJacks[0] = new OutputJack(0, owner, owner.getAutomatonGrid(), this);
        buildButtonHolder();

        setHorizontalAlignment(SwingConstants.CENTER);
        setVerticalTextPosition(SwingConstants.BOTTOM);
        setHorizontalTextPosition(SwingConstants.CENTER);
        setMargin(new Insets(10, 0, 10, 0));
        setFocusPainted(false);
        setEnabled(true);
        setIconTextGap(0);
        
        addMouseListener(new MouseAdapter()
            {
            public void mousePressed(MouseEvent e)
                {
                dragCount = 0;                          // sometimes the actionListener isn't triggered, so we do this here too
                int modifiers = e.getModifiers();
                if (SwingUtilities.isRightMouseButton(e) ||
					SwingUtilities.isMiddleMouseButton(e) ||
                    ((modifiers & InputEvent.SHIFT_MASK) == InputEvent.SHIFT_MASK) ||
                    ((modifiers & InputEvent.CTRL_MASK) == InputEvent.CTRL_MASK) ||
                    ((modifiers & InputEvent.META_MASK) == InputEvent.META_MASK) ||
                    ((modifiers & InputEvent.ALT_MASK) == InputEvent.ALT_MASK))
                    {
                    // Do nothing for now?
                    }
                }
            });

        addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                dragCount = 0;
                    {
                    owner.select(AutomatonButton.this);
                    }
                }
            });
                
        addMouseMotionListener(new MouseMotionAdapter()
            {
            public void mouseDragged(MouseEvent e)
                {
                if (AutomatonButton.this.isEnabled())
                    {
                    dragCount++;
                    if (dragCount == MAX_DRAG_COUNT)
                        getTransferHandler().exportAsDrag(AutomatonButton.this, e, TransferHandler.MOVE);
                    }
                }
            });
            
        setTransferHandler(buildTransferHandler());
        setDropTarget(new DropTarget(this, owner.buildDropTargetListener()));
        updateText();           // MotifButton does this but it's too early
        }


    public SeqUI getSeqUI() { return sequi; }

    public static AutomatonButton buildButton(SeqUI sequi, Automaton.Node node, AutomatonUI owner)
        {
        if (node instanceof Automaton.MotifNode)
            {
            Automaton.MotifNode motifnode = (Automaton.MotifNode)node;
            AutomatonButton button = new AutomatonButton(sequi, node, sequi.getMotifList().getOrAddMotifUIFor(motifnode.getMotif()), owner, node.getPosition());
            button.setToolTipText(MOTIF_TOOLTIP);
            return button;
            }
        else
            {
            final AutomatonButton button = new AutomatonButton(sequi, owner, node.getPosition());
            button.inputJack = new InputJack(0, owner.getAutomatonGrid(), button);
            button.setAuxiliary(node);
            button.setTransferHandler(button.buildTransferHandler());
            if (node instanceof Automaton.Delay)
                {
                button.setIcon(AutomatonNodeButton.delayIcon);
                button.outputJacks = new OutputJack[1];
                button.outputJacks[0] = new OutputJack(0, owner, owner.getAutomatonGrid(), button);
                button.setToolTipText(DELAY_TOOLTIP);
                }
            else if (node instanceof Automaton.Chord)
                {
                button.setIcon(AutomatonNodeButton.chordIcon);
                button.outputJacks = new OutputJack[1];
                button.outputJacks[0] = new OutputJack(0, owner, owner.getAutomatonGrid(), button);
                button.setToolTipText(CHORD_TOOLTIP);
                }
            else if (node instanceof Automaton.Finished)
                {
                button.setIcon(AutomatonNodeButton.finishedIcon);
                button.outputJacks = new OutputJack[1];
                button.outputJacks[0] = new OutputJack(0, owner, owner.getAutomatonGrid(), button);
                button.setToolTipText(FINISHED_TOOLTIP);
                }
            else if (node instanceof Automaton.Iterate)
                {
                button.setIcon(AutomatonNodeButton.iterateIcon);
                button.outputJacks = new OutputJack[Automaton.MAX_OUT];
                for(int i = 0; i < button.outputJacks.length; i++)
                    {
                    button.outputJacks[i] = new OutputJack(i, owner, owner.getAutomatonGrid(), button);
                    }
                button.setToolTipText(ITERATE_TOOLTIP);
                }
            else if (node instanceof Automaton.Random)
                {
                button.setIcon(AutomatonNodeButton.randomIcon);
                button.outputJacks = new OutputJack[Automaton.MAX_OUT];
                for(int i = 0; i < button.outputJacks.length; i++)
                    {
                    button.outputJacks[i] = new OutputJack(i, owner, owner.getAutomatonGrid(), button);
                    }
                button.setToolTipText(RANDOM_TOOLTIP);
                }
            else if (node instanceof Automaton.Fork)
                {
                button.setIcon(AutomatonNodeButton.forkIcon);
                button.outputJacks = new OutputJack[Automaton.MAX_OUT];
                for(int i = 0; i < button.outputJacks.length; i++)
                    {
                    button.outputJacks[i] = new OutputJack(i, owner, owner.getAutomatonGrid(), button);
                    }
                button.setToolTipText(FORK_TOOLTIP);
                }
            else if (node instanceof Automaton.Join)
                {
                button.setIcon(AutomatonNodeButton.joinIcon);
                button.outputJacks = new OutputJack[1];
                button.outputJacks[0] = new OutputJack(0, owner, owner.getAutomatonGrid(), button);
                button.setToolTipText(JOIN_TOOLTIP);
                }
            else 
                {
                System.err.println("AutomatonButton.buildButton: unknown node type " + node);
                }
                        
            button.buildButtonHolder();
        
            button.setHorizontalAlignment(SwingConstants.CENTER);
            button.setVerticalTextPosition(SwingConstants.BOTTOM);
            button.setHorizontalTextPosition(SwingConstants.CENTER);
            button.setMargin(new Insets(10, 10, 10, 10));
            button.setFocusPainted(false);
            button.setEnabled(true);
            button.setIconTextGap(0);
        
            button.addMouseListener(new MouseAdapter()
                {
                public void mousePressed(MouseEvent e)
                    {
                    button.dragCount = 0;                           // sometimes the actionListener isn't triggered, so we do this here too
                    int modifiers = e.getModifiers();
                    if (SwingUtilities.isRightMouseButton(e) ||
                        SwingUtilities.isMiddleMouseButton(e) ||
                        ((modifiers & InputEvent.SHIFT_MASK) == InputEvent.SHIFT_MASK) ||
                        ((modifiers & InputEvent.CTRL_MASK) == InputEvent.CTRL_MASK) ||
                        ((modifiers & InputEvent.META_MASK) == InputEvent.META_MASK) ||
                        ((modifiers & InputEvent.ALT_MASK) == InputEvent.ALT_MASK))
                        {
                        // Do nothing for now?
                        }
                    }
                });

            button.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    button.dragCount = 0;
                        {
                        owner.select(button);
                        }
                    }
                });
                
            button.addMouseMotionListener(new MouseMotionAdapter()
                {
                public void mouseDragged(MouseEvent e)
                    {
                    if (button.isEnabled())
                        {
                        button.dragCount++;
                        if (button.dragCount == MAX_DRAG_COUNT)
                            {
                            button.getTransferHandler().exportAsDrag(button, e, TransferHandler.MOVE);
                            }
                        }
                    }
                });

            button.updateText();            // MotifButton does this but it's too early
            return button;
            }
        }

    public boolean shouldPreHighlight()
        {
        return false;
        }
    
    public void updateText()
        {
        if (isBlank()) return;
        
        String name = null;
        int playingCount = 0;
        boolean seqPlaying = false;
        
        // We can't use HTML because it won't wrap overflow.
        ReentrantLock lock = sequi.getSeq().getLock();
        lock.lock();
        try 
            { 
            Motif ownerMotif = owner.getMotif();
            AutomatonClip ownerClip = (AutomatonClip)(owner.getDisplayClip());
            Automaton.Node node = getNode();
            name = node.getCurrentName();
            if (ownerClip != null)
                {
                seqPlaying = sequi.getSeq().isPlaying();
          
                if (node instanceof Automaton.Join)
                    {
                    playingCount = ownerClip.getCurrentJoinCount((Automaton.Join)node);
                    }
                else
                    {
                    // This is ultimately O(n^2) :-(
                    for(AutomatonClip.AutomatonThread thread : ownerClip.getProcessedThreads())
                        {
                        if (thread.getNode() == node)
                            {
                            playingCount++;
                            if (playingCount == 2) break;               // At most BLUE!
                            }
                        }
                    }
                }
            }
        finally { lock.unlock(); }

        if (seqPlaying && playingCount == 1) setForeground(Color.RED);
        else if (seqPlaying && playingCount > 1) setForeground(Color.BLUE);
        else setForeground(null);
        setText("<html><center>" + StringUtility.sanitize(name) + "</center></html>");
        }

    public void disconnect()
        {
        super.disconnect();
        if (motifui != null)
            {
            super.updateText();
            }
        }

    public String getSubtext()
        {
        Seq seq = sequi.getSeq();
        
        String subname = null;
        int repeats = 0;
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            {
            ArrayList<Motif.Child> children = owner.getMotif().getChildren();
            if (at >= children.size())  // uh
                return "BAD CHILD? " + at;
            Motif.Child child = children.get(at);
            subname = child.getNickname();                    // FIXME: could this be made just volatile?
            }
        finally { lock.unlock(); }

        if (subname != null) 
            { 
            subname = subname.trim();
            if (!subname.equals(""))
                {
                subname = StringUtility.sanitize(subname);
                }
            }
        else subname = "";
        
        return subname;
        }

    public void doubleClick(MouseEvent e)
        {
        if (motifui != null)
            {
            // pop up the motifui
            sequi.showMotifUI(motifui);
            }
        }



    static final String MOTIF_TOOLTIP = "<html><b>Motif Node</b><br>" +
        "This represents a single motif.  When the automaton transitions to this node, it begins repeatedly<br>" +
        "playing the motif, and then ultimately transitioning to the node connected to the output.<br>" +
        "Repeating works as follows:" +
        "<ol>" +
        "<li>The motif node is played once." +
        "<li>Then the motif node is played <b>Initial Repeats</b> more times." +
        "<li>Then a coin is flipped repeatedly with <b>Repeat Probability</b> of being heads.<br>" +
        "If it comes up heads <i>N</i> times before the first tails, then the motif node is<br>" +
        "played <i>N</i> more times." + 
        "<li>Then the motif node transitions to the next motif." +
        "</ol>" +
        "All the while, if a 1.0 is sent to <b>Parameter 8</b> and <b>Until Trigger 8</b> is<br>" +
        "selected, then the motif node transitions to the next motif with no further repeats.</html>";

    static final String DELAY_TOOLTIP = "<html><b>Delay Node</b><br>" +
        "This represents a time delay.  When the automaton transitions to this node, it waits for<br>" +
        "some amount of time before transitioning to the next node connected to the output.</html>";

    static final String CHORD_TOOLTIP = "<html><b>Chord Node</b><br>" +
        "This represents a chord, interval, or single note.  When the automaton transitions to this node, it plays<br>" +
        "the chord for the specified amount of time before transitioning to the next node connected to the output.</html>";

    static final String FINISHED_TOOLTIP = "<html><b>Finished Node</b><br>" +
        "This node tells the automaton's parent motifs that the automaton believes it has finished playing.</html>";

    static final String ITERATE_TOOLTIP = "<html><b>Iterate Node</b><br>" +
        "This node iterates through up to four outputs, transitioning out different ones each time<br>" +
        "the automaton transitions to it. Iterations works as follows:" + 
        "<ol>" +
        "<li>When first played, we immediately transition to the first output that is connected to us." +
        "<li>Each time we are played, we continue transiting out that input until we have done so<br>" +
        "<b>Iterations</b> total times. Thereafter we continue to the next output that is connected to us.<br>" +
        "<li>When we have finished with all our connected outputs, if <b>Loop</b> is checked, then we<br>" +
        "loop back and continue again with our first connectd output. Else we stop transitioning entirely.</html>";

    static final String RANDOM_TOOLTIP = "<html><b>Random Node</b><br>" +
        "This node transitions out a random output each time the automaton transitions to it.<br>" +
        "This works as follows.  When played, we take all the connected outputs and normalize<br>" + 
        "their weights into probabilities (divide them by their sum).  If they're all 0, they're<br>" +
        "all treated as equal probability.  Then we select an output at random according to the probabilities<br>" +
        "and transition to that output.  If no outputs are connected, we don't transition at all.</html>";

    static final String FORK_TOOLTIP = "<html><b>Fork Node</b><br>" +
        "This node creates one or more playing threads each time the automaton transitions to it.<br>" +
        "These threads output in parallel along separate outputs and continue playing and transitioning.<br>" +
        "in parallel.  You can create no more than 8 threads at a time: further threads are ignored.<br>" +
        "You an eliminate threads by <b>Joining</b> them with one another.</html>";

    static final String JOIN_TOOLTIP = "<html><b>Join Node</b><br>" +
        "This node joins one or more playing threads each time the automaton transitions to it.<br>" +
        "Joined threads are collapsed to a single thread, which is then transitioned along the output.<br>" +
        "When a thread arrives at the input, Join may do nothing yet: it will wait until the right<br>" +
        "number of threads have arrived before joining them (see <b>Threads</b> in the inspector at right).</html>";
    }
        
