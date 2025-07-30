/* 
   Copyright 2025 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.arpeggio.gui;

import seq.motif.arpeggio.*;
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



public class ArpeggioUI extends MotifUI
    {
    public static final int MIN_HEADER_SPACE = 16;
    public static final Color HEADER_LINE_COLOR = new Color(200, 200, 200);
    public static final int LABEL_PADDING = 4;
    
    public static final Color PATTERN_ON_COLOR = Color.RED;
    public static final Color PATTERN_ON_DISABLED_COLOR = new Color(220, 160, 160);
    public static final Color PATTERN_OFF_COLOR = new Color(220, 220, 220);
    public static final Color PATTERN_BORDER_COLOR = Color.GRAY;
    public static final Stroke PATTERN_STROKE = new BasicStroke(1.0f);
    public static final Stroke DIVIDER_STROKE = new BasicStroke(2.0f);
    public static final int OUTER_BORDER_THICKNESS = 1;
    public static final Color PATTERN_DIVIDER_COLOR = Color.BLUE;
    public static final Color PATTERN_END_COLOR = new Color(180, 0, 180);
    public static final int PATTERN_WIDTH = 24;
/*

    //// NODE STATES
    public static final int UNUSED = 0;
    public static final int ON = 1;
    public static final int DISABLED = 2;
    
    public static final int PAD_UNUSED_MKIII = 0;
    public static final int PAD_DISABLED_MKIII = 1;
    public static final int PAD_ON_MKIII = 5;

    public static final int PAD_UNUSED_MKI = 0;
    public static final int PAD_DISABLED_MKI = 28;
    public static final int PAD_ON_MKI = 15;
    
    // Given a Novation Launchpad pad at the provided OUT, sets the pad NOTE to the given STATE
    void setPad(int out, int note, int state)
        {
        int gridDevice = ((Select)getMotif()).getGridDevice();
        if (gridDevice == Select.DEVICE_LAUNCHPAD_MKIII)
            {
            if (state == UNUSED)
                {
                seq.forceNoteOn(out, note, PAD_UNUSED_MKIII, 1);                          // Turn the Light Off
                }
            else if (state == ON)                                                   
                {
                seq.forceNoteOn(out, note, PAD_ON_MKIII, 1);              // RED
                }
            else if (state == DISABLED)
                {
                seq.forceNoteOn(out, note, PAD_DISABLED_MKIII, 1);                             // Gray
                }
            }l
        else if (gridDevice == Select.DEVICE_LAUNCHPAD_MKI)
            {
            if (state == UNUSED)
                {
                seq.forceNoteOn(out, note, PAD_UNUSED_MKI, 1);                          // Turn the Light Off
                }
            else if (state == ON)                                                   
                {
                seq.forceNoteOn(out, note, PAD_ON_MKI, 1);              // RED
                }
            else if (state == DISABLED)
                {
                seq.forceNoteOn(out, note, PAD_DISABLED_MKI, 1);                             // Green Low
                }
            }
        }
*/

    Arpeggio arpeggio;
        
//    ArpeggioChildInspector childInspector;

//    JPanel childOuter;
//    TitledBorder childBorder;

    JPanel inspectorPane;
        
    JPanel arpeggioOuter;
    TitledBorder arpeggioBorder;
    ArpeggioInspector arpeggioInspector;
        
    JPanel arpeggioGrid = new JPanel();
    
    JPanel patternGrid = new JPanel();
    
    public JPanel getPatternGrid() { return patternGrid; }
    
    public static ImageIcon getStaticIcon() { return new ImageIcon(MotifUI.class.getResource("icons/arpeggio.png")); }        // don't ask
    public ImageIcon getIcon() { return getStaticIcon(); }
    public static String getType() { return "Arpeggio"; }
    public static MotifUI create(Seq seq, SeqUI ui)
        {
        return new ArpeggioUI(seq, ui, new Arpeggio(seq));
        }
        
    public static MotifUI create(Seq seq, SeqUI ui, Motif motif)
        {
        return new ArpeggioUI(seq, ui, (Arpeggio)motif);
        }
        
    public ArpeggioUI(Seq seq, SeqUI sequi, Arpeggio arpeggio)
        {
        super(seq, sequi, arpeggio);
        this.seq = seq;
        this.arpeggio = arpeggio;
        this.sequi = sequi;
        arpeggioGrid.setLayout(new GridLayout(1, 1));
        patternGrid.setLayout(new GridLayout(Arpeggio.PATTERN_NOTES, Arpeggio.MAX_PATTERN_LENGTH));
        }
        
    public void build()
        {
        super.build();
        loadChildren();         // build() populates with blanks, so this must be afterwards
        }
        
    public void loadChildren()
        { 
        ArrayList<Motif.Child> children = arpeggio.getChildren();
        if (children.size() == 0) return;
        
        MotifList list = sequi.getMotifList();   
        for(int i = 0; i < children.size(); i++)                // hopefully this is the size of numArpeggioChildren?
            {                
            Motif motif = children.get(i).getMotif();
            
            if (motif instanceof seq.motif.blank.Blank)
                {
                //ArpeggioButton blankButton = new ArpeggioButton(sequi, this, i);
                //arpeggioGrid.add(blankButton, i);
                }
            else
                {
                MotifUI motifui = list.getOrAddMotifUIFor(motif);

                ArpeggioButton newButton = new ArpeggioButton(sequi, motifui, ArpeggioUI.this, i);
                Motif.Child child = null;

                newButton.addActionListener(new ActionListener()
                    {
                    public void actionPerformed(ActionEvent e)
                        {
                        select(newButton);
                        }
                    });

                MotifButton button = (MotifButton)(arpeggioGrid.getComponent(i));
                button.disconnect();
                        
                // This is outside the lock but I think it's okay as we're doing this while loading from a file
                child = arpeggio.getChildren().get(i);
                                                                                
                newButton.setAuxiliary(child);

                arpeggioGrid.remove(i);
                arpeggioGrid.add(newButton, i);
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
/*
// Build the series child inspector holder
childOuter = new JPanel();
childOuter.setLayout(new BorderLayout());
childBorder = BorderFactory.createTitledBorder(null, "Child");
childOuter.setBorder(childBorder);
*/
                
        // Build the arpeggio inspector holder
        arpeggioOuter = new JPanel();
        arpeggioOuter.setLayout(new BorderLayout());
        arpeggioBorder = BorderFactory.createTitledBorder(null, "Arpeggio");
        arpeggioOuter.setBorder(arpeggioBorder);

        // Add the arpeggio inspector
        arpeggioInspector = new ArpeggioInspector(seq, arpeggio, this);
        arpeggioOuter.add(arpeggioInspector, BorderLayout.CENTER);
                
        // Fill the panes
        inspectorPane = new JPanel();
        inspectorPane.setLayout(new BorderLayout());
        inspectorPane.add(arpeggioOuter, BorderLayout.NORTH);
//        inspectorPane.add(childOuter, BorderLayout.CENTER);
                
        scroll.setViewportView(inspectorPane);
        }
                
    Line2D.Double line = new Line2D.Double();
    
    public void buildPrimary(JScrollPane scroll)
        {
        JPanel outer = new JPanel();
        outer.setLayout(new BorderLayout());
        outer.add(arpeggioGrid, BorderLayout.NORTH);
        outer.setBackground(BACKGROUND);
        scroll.setViewportView(outer);
        
        arpeggioGrid.setDropTarget(new DropTarget(this, buildDropTargetListener()));
        arpeggioGrid.setLayout(new GridLayout(1, 1));
        arpeggioGrid.add(new ArpeggioButton(sequi, this, 0));         // A "blank" button
        
        // yes, this is backwards
        for(int i = 0; i < Arpeggio.PATTERN_NOTES; i++)
            {
            for(int j = 0; j < Arpeggio.MAX_PATTERN_LENGTH; j++)
                {
                final int _i = i;
                final int _j = j;
                JCheckBox button = new JCheckBox()
                    {
                    public void paintComponent(Graphics _g)
                        {
                        Graphics2D g = (Graphics2D) _g;
                        boolean state = false;
                        int length = 0;
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try
                            {
                            Arpeggio arp = (Arpeggio)getMotif();
                            length = arp.getPatternLength();
                            state = arp.getPattern(_j, Arpeggio.PATTERN_NOTES - _i - 1);                    // note backwards
                            }
                        finally
                            {
                            lock.unlock();
                            }
                
                        Rectangle bounds = getBounds();
                        bounds.x = 0;
                        bounds.y = 0;
                        g.setPaint(state ? (_j < length ? PATTERN_ON_COLOR : PATTERN_ON_DISABLED_COLOR) : PATTERN_OFF_COLOR);
                        g.fill(bounds);

                        if (_i  == Arpeggio.PATTERN_NOTES / 2)
                            {
                            g.setStroke(DIVIDER_STROKE);
                            g.setColor(PATTERN_DIVIDER_COLOR);
                            }
                        else
                            {
                            g.setStroke(PATTERN_STROKE);
                            g.setColor(PATTERN_BORDER_COLOR);
                            }
                        line.setLine(0, 0, bounds.width, 0);
                        g.draw(line);
                                                
                        if (_j == length - 1)
                            {
                            g.setStroke(DIVIDER_STROKE);
                            g.setColor(PATTERN_END_COLOR);
                            line.setLine(bounds.width - 1, 0, bounds.width - 1, bounds.height);
                            }
                        else
                            {
                            g.setStroke(PATTERN_STROKE);
                            g.setColor(PATTERN_BORDER_COLOR);
                            line.setLine(bounds.width, 0, bounds.width, bounds.height);
                            }
                        g.draw(line);
                        }
                    };
                button.setPreferredSize(new Dimension(PATTERN_WIDTH, PATTERN_WIDTH));
                button.setBorderPainted(true);
                button.setOpaque(true);
                                                                        
                ReentrantLock lock = seq.getLock();
                lock.lock();
                try
                    {
                    Arpeggio arp = (Arpeggio)getMotif();
                    button.setSelected(arp.getPattern(_j, Arpeggio.PATTERN_NOTES - _i - 1));
                    }
                finally
                    {
                    lock.unlock();
                    }
                                        
                button.addActionListener(new ActionListener()
                    {
                    public void actionPerformed(ActionEvent e)
                        {
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try
                            {
                            Arpeggio arp = (Arpeggio)getMotif();
                            arp.setPattern(_j, Arpeggio.PATTERN_NOTES - _i - 1, button.isSelected());
                            }
                        finally
                            {
                            lock.unlock();
                            }
                        repaint();
                        }
                    });
                                        
                patternGrid.add(button);

                patternGrid.setBorder(BorderFactory.createMatteBorder(1, 1, 0, 0, PATTERN_BORDER_COLOR));
                                
                }
            }

        JPanel one = new JPanel();
        one.setBackground(BACKGROUND);
        one.setLayout(new BorderLayout());
        one.add(patternGrid, BorderLayout.CENTER);
        
        JPanel two = new JPanel();
        two.setBackground(BACKGROUND);
        two.setLayout(new BorderLayout());
        two.add(one, BorderLayout.CENTER);
                
        // Add headers
        JPanel topHeader = new JPanel();
        topHeader.setLayout(new GridLayout(1, Arpeggio.MAX_PATTERN_LENGTH));
        for(int i = 0; i < Arpeggio.MAX_PATTERN_LENGTH; i++)
            {
            JLabel label = new JLabel("" + (i + 1), SwingConstants.CENTER);
            label.setPreferredSize(new Dimension(PATTERN_WIDTH, PATTERN_WIDTH));
            topHeader.add(label);
            }
        one.add(topHeader, BorderLayout.NORTH);

        /*
          JPanel leftHeader = new JPanel();
          leftHeader.setLayout(new GridLayout(Arpeggio.PATTERN_NOTES + 1, 1));
          JLabel corner = new JLabel(" ");
          corner.setBackground(BACKGROUND);
          corner.setPreferredSize(new Dimension(PATTERN_WIDTH, PATTERN_WIDTH));
          leftHeader.add(corner);
          for(int i = 0; i < Arpeggio.PATTERN_NOTES; i++)
          {
          JLabel label = new JLabel("" + (i + 1));
          label.setPreferredSize(new Dimension(PATTERN_WIDTH, PATTERN_WIDTH));
          leftHeader.add(label);
          }
          two.add(leftHeader, BorderLayout.WEST);
        */
                
        JPanel three = new JPanel();
        three.setBackground(BACKGROUND);
        three.setLayout(new BorderLayout());
        three.add(two, BorderLayout.NORTH);

        outer.add(three, BorderLayout.WEST);
        }
                
    public void setNickname(int index, String val)
        {
        ArrayList<Arpeggio.Child> children = arpeggio.getChildren(); 
        children.get(index).setNickname(val);
        }
              
    public String getNickname(int index)
        {
        ArrayList<Arpeggio.Child> children = arpeggio.getChildren(); 
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

        JPanel console = new JPanel();
        console.setLayout(new BorderLayout());
                
        Box addRemoveBox = new Box(BoxLayout.X_AXIS);
        addRemoveBox.add(removeButton);
        console.add(addRemoveBox, BorderLayout.WEST);   
                
        return console; 
        }




/*
  JPanel stub = new JPanel();
  public void moveChild(ArpeggioButton button, int to)
  {
  // Where is the button?
  Component[] c = arpeggioGrid.getComponents();
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
  arpeggio.swapChild(from, to);
  }
  finally
  {
  seq.getLock().unlock();
  }
            
  // To swap, to be careful we'll first remove TO and replace with
  // a stub.  Then we'll remove FROM and replace with TO.  Then we'll
  // remove the stub and replace with FROM.
  ArpeggioButton toButton = (ArpeggioButton)c[to];
  arpeggioGrid.remove(to);
  arpeggioGrid.add(stub, to);
            
  arpeggioGrid.remove(from);
  arpeggioGrid.add(toButton, from);
            
  arpeggioGrid.remove(to);
  arpeggioGrid.add(button, to);
            
  button.setAt(to);
  toButton.setAt(from);
              
  select(button);
  sequi.getMotifList().rebuildClipsForMotif(getMotif());
  arpeggioGrid.revalidate();
  }
  else System.err.println("ArpeggioUI.moveChild: button not in list");
  }
*/

    public void doRemove()
        {
        // Where is the button?
        Component[] c = arpeggioGrid.getComponents();
        int at = -1;
        for(int i = 0; i < c.length; i++)
            {
            if (((ArpeggioButton)c[i]).isSelected()) { at = i; break; }
            }
        if (at != -1)
            {
            seq.push();
            seq.getLock().lock();
            try
                {
                arpeggio.replaceChild(new seq.motif.blank.Blank(seq), at);
                }
            finally
                {
                seq.getLock().unlock();
                }

            MotifButton button = (MotifButton)(arpeggioGrid.getComponent(at));
            button.disconnect();
                        
            // We'll swap in a blank ArpeggioButton
            arpeggioGrid.remove(at);
            ArpeggioButton blankButton = new ArpeggioButton(sequi, this, at);
            arpeggioGrid.add(blankButton, at);
            deselectAll();

            sequi.getMotifList().rebuildClipsForMotif(getMotif());
            arpeggioGrid.revalidate();
            }
        else System.err.println("ArpeggioUI.moveChild: button not in list");
        }
    
        
    public void addChild(MotifUI motifui, int at)
        {  
        ArpeggioButton newButton = new ArpeggioButton(sequi, motifui, ArpeggioUI.this, at);
        Motif.Child child = null;

        newButton.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                select(newButton);
                }
            });
                        
        MotifButton button = (MotifButton)(arpeggioGrid.getComponent(at));
        button.disconnect();
                
        seq.getLock().lock();
        try
            {
            if (motifui.getMotif().containsOrIs(getMotif()))    // No cycles permitted!
                return;
                
            arpeggio.replaceChild(motifui.getMotif(), at);
            child = arpeggio.getChildren().get(at);
            }
        finally
            {
            seq.getLock().unlock();
            }
        
        newButton.setAuxiliary(child);

        // Copy over the button
        arpeggioGrid.remove(at);                                  
        arpeggioGrid.add(newButton, at);
        select(newButton);  
        newButton.updateText();
          
        revalidate();
        sequi.getMotifList().rebuildClipsForMotif(getMotif());
        }

    public void select(MotifButton button)  
        {
        deselectAll();
        button.setSelected(true);
        int at = ((ArpeggioButton)button).getAt();
//        setChildInspector(new ArpeggioChildInspector(seq, arpeggioChild, null, this, at));
        }

    public void deselectAll()
        {
        for(Component c : arpeggioGrid.getComponents())
            {
            if (c instanceof MotifButton)
                {
                ((MotifButton)c).setSelected(false);
                }
            }
        }
    
/*
  public void setChildInspector(ArpeggioChildInspector inspector)
  {
  childInspector = inspector;
  childOuter.removeAll();
  if (inspector!=null) 
  {
  childOuter.add(inspector, BorderLayout.NORTH);
  childBorder.setTitle("Child");
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
*/

/*
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
*/


    //// DRAG AND DROP
        
    public DropTargetListener buildDropTargetListener() { return new ArpeggioUIDropTargetListener(); }
        
    class ArpeggioUIDropTargetListener extends DropTargetAdapter 
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
                    else if (comp instanceof ArpeggioButton)
                        {
                        int at = ((ArpeggioButton)comp).getAt();
                        seq.push();
                        if (dropped instanceof ArpeggioButton)
                            {
                            //moveChild((ArpeggioButton)dropped, at);
                            }
                        else
                            {
                            addChild(dropped.getMotifUI(), at);
                            }
                        }
                    arpeggioGrid.revalidate(); 
                    arpeggioGrid.repaint();
                                                
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
        "Removes the selected motif from the Arpeggio Child slot.</html>";
        

    }
