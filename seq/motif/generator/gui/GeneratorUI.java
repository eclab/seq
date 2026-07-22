/* 
   Copyright 2026 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.generator.gui;

import seq.motif.generator.*;
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
import java.beans.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;

// For Drag and Drop
import java.awt.dnd.*;
import java.awt.datatransfer.*;



public class GeneratorUI extends MotifUI
    {
    Generator generator;
    JPanel generatorGrid = new JPanel();
    AlgorithmUI algorithmUI;
    JPanel topPanel;

    JPanel inspectorPane;
    JSplitPane split;
    //JTextPane text;
    HTMLBrowser text;
	JScrollPane algorithmScroll;
	JComponent algorithmContainer;
        
    JPanel generatorOuter;
    TitledBorder generatorBorder;
    GeneratorInspector generatorInspector;
            
    public static ImageIcon getStaticIcon() { return new ImageIcon(MotifUI.class.getResource("icons/generator.png")); }        // don't ask
    public ImageIcon getIcon() { return getStaticIcon(); }
    public static String getType() { return "Generator"; }
    public static MotifUI create(Seq seq, SeqUI ui)
        {
        return new GeneratorUI(seq, ui, new Generator(seq));
        }
        
    public static MotifUI create(Seq seq, SeqUI ui, Motif motif)
        {
        return new GeneratorUI(seq, ui, (Generator)motif);
        }
        
    public GeneratorUI(Seq seq, SeqUI sequi, Generator generator)
        {
        super(seq, sequi, generator);
        this.generator = generator;
        add(generatorGrid, BorderLayout.NORTH);
        }
        
    public void build()
        {
        super.build();
        loadChildren();         // build() populates with blanks, so this must be afterwards
        }
        
    public void loadChildren()
        { 
        ArrayList<Motif.Child> children = generator.getChildren();
        if (children.size() == 0) return;
        
        MotifList list = sequi.getMotifList();   
        for(int i = 0; i < children.size(); i++)                // hopefully this is the size of numGeneratorChildren?
            {                
            Motif motif = children.get(i).getMotif();
            
            if (motif instanceof seq.motif.blank.Blank)
                {
                //GeneratorButton blankButton = new GeneratorButton(sequi, this, i);
                //generatorGrid.add(blankButton, i);
                }
            else
                {
                MotifUI motifui = list.getOrAddMotifUIFor(motif);

                GeneratorButton newButton = new GeneratorButton(sequi, motifui, GeneratorUI.this, i);
                newButton.setToolTipText(CHILD_TOOLTIP);
                Motif.Child child = null;

                newButton.addActionListener(new ActionListener()
                    {
                    public void actionPerformed(ActionEvent e)
                        {
                        select(newButton);
                        }
                    });

                MotifButton button = (MotifButton)(generatorGrid.getComponent(i));
                button.disconnect();
                        
                // This is outside the lock but I think it's okay as we're doing this while loading from a file
                child = generator.getChildren().get(i);
                                                                                
                newButton.setAuxiliary(child);

                generatorGrid.remove(i);
                generatorGrid.add(newButton, i);
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
        // Build the generator inspector holder
        generatorOuter = new JPanel();
        generatorOuter.setLayout(new BorderLayout());
        generatorBorder = BorderFactory.createTitledBorder(null, "Generator");
        generatorOuter.setBorder(generatorBorder);

        // Add the generator inspector
        generatorInspector = new GeneratorInspector(seq, generator, this);
        generatorOuter.add(generatorInspector, BorderLayout.CENTER);
                
        // Fill the panes
        inspectorPane = new JPanel();
        inspectorPane.setLayout(new BorderLayout());
        inspectorPane.add(generatorOuter, BorderLayout.NORTH);
                
        scroll.setViewportView(inspectorPane);
        }
                
    
    public JComponent buildPrimary()
        {
        topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());

        algorithmContainer = new JPanel();
        algorithmContainer.setLayout(new BorderLayout());
		JPanel algorithmContainer2 = new JPanel();
		algorithmContainer2.setLayout(new BorderLayout());
		algorithmContainer2.add(algorithmContainer, BorderLayout.NORTH);
		algorithmScroll = new JScrollPane(algorithmContainer2, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);       
        
        generatorGrid.setDropTarget(new DropTarget(this, buildDropTargetListener()));
        generatorGrid.setLayout(new GridLayout(1, 1));
        GeneratorButton generatorButton = new GeneratorButton(sequi, this, 0);
        generatorButton.setToolTipText(CHILD_TOOLTIP);
        generatorGrid.add(generatorButton);         // A "blank" button

        text = new HTMLBrowser();
        // JTextPane();
		//text.setContentType("text/html");
		//text.setEditable(false);
		//text.getCaret().setVisible(false);
        //JScrollPane textScroll = new JScrollPane(text);
        //textScroll.setMinimumSize(new Dimension(0, 0));
        //textScroll.setPreferredSize(textScroll.getMinimumSize());

        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, algorithmScroll, text);
        double weight = Prefs.getLastDouble("seq.motif.generator.Generator.split", 0.75);
        split.setResizeWeight(weight);     // on resizing, top gets everything
        split.setDividerLocation(weight);     // on resizing, top gets everything
        
        // We're responding to mouseReleased on the bar, rather than a PropertyChangeListener,
        // so we ONLY get updated when the user moves the bar, no other reason.  This is because
        // setResizeWeight and setDividerLocation don't correspond exadtly with getDividerLocation,
        // believe it or not -- it's slightly off, partly because of the divider size.  Very annoying.
		SplitPaneUI spui = split.getUI();
			if (spui instanceof BasicSplitPaneUI) 
				{
			  // Setting a mouse listener directly on split pane does not work, because no events are being received.
			  ((BasicSplitPaneUI) spui).getDivider().addMouseListener(new MouseAdapter() 
			  	{
				public void mouseReleased(MouseEvent e) 
					{
           		double weight = (split.getDividerLocation() + split.getDividerSize() / 2 - split.getMinimumDividerLocation()) / 
           						(double)(split.getMaximumDividerLocation() - split.getMinimumDividerLocation());
           		Prefs.setLastDouble("seq.motif.generator.Generator.split", weight);
					}
				});
				}
        
        topPanel.add(generatorGrid, BorderLayout.NORTH);
        topPanel.add(split, BorderLayout.CENTER);

		updateAlgorithmUI();
		
        return topPanel;
        }
        
    AlgorithmUI lastAlgorithmUI = null;
    
    /** Called whenever the algorithm is updated, either when the generator ui is first built,
    	or when the algorithm is changed from the combo box. This builds a new AlgorithmUI and also
    	updates the HTML description. */
    public void updateAlgorithmUI()
    	{
        algorithmUI = generator.getAlgorithm().buildUI(seq, this);
        if (algorithmUI != lastAlgorithmUI)
        	{
        	// need to revise
			algorithmContainer.removeAll();
			algorithmContainer.add(algorithmUI, BorderLayout.WEST);        
			algorithmContainer.revalidate();
			algorithmContainer.repaint();
			text.setText(generator.getAlgorithm().getHTMLDescription());
			}
        lastAlgorithmUI = algorithmUI;
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
        Component[] c = generatorGrid.getComponents();
        int at = -1;
        for(int i = 0; i < c.length; i++)
            {
            if (((GeneratorButton)c[i]).isSelected()) { at = i; break; }
            }
        if (at != -1)
            {
            sequi.push();
            seq.getLock().lock();
            try
                {
                generator.replaceChild(new seq.motif.blank.Blank(seq), at);
                }
            finally
                {
                seq.getLock().unlock();
                }

            MotifButton button = (MotifButton)(generatorGrid.getComponent(at));
            button.disconnect();
                        
            // We'll swap in a blank GeneratorButton
            generatorGrid.remove(at);
            GeneratorButton blankButton = new GeneratorButton(sequi, this, at);
            blankButton.setToolTipText(CHILD_TOOLTIP);
            generatorGrid.add(blankButton, at);
            deselectAll();

            sequi.getMotifList().rebuildClipsForMotif(getMotif());
            generatorGrid.revalidate();
            }
        else System.err.println("GeneratorUI.moveChild: button not in list");
        }
    
        
    public void addChild(MotifUI motifui, int at)
        {  
        GeneratorButton newButton = new GeneratorButton(sequi, motifui, GeneratorUI.this, at);
        Motif.Child child = null;

        newButton.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                select(newButton);
                }
            });
                        
        MotifButton button = (MotifButton)(generatorGrid.getComponent(at));
        button.disconnect();
                
        seq.getLock().lock();
        try
            {
            if (motifui.getMotif().containsOrIs(getMotif()))    // No cycles permitted!
                return;
                
            generator.replaceChild(motifui.getMotif(), at);
            child = generator.getChildren().get(at);
            }
        finally
            {
            seq.getLock().unlock();
            }
        
        newButton.setAuxiliary(child);

        // Copy over the button
        generatorGrid.remove(at);                                  
        generatorGrid.add(newButton, at);
        select(newButton);  
        newButton.updateText();
          
        revalidate();
        sequi.getMotifList().rebuildClipsForMotif(getMotif());
        }

    public void select(MotifButton button)  
        {
        deselectAll();
        button.setSelected(true);
        int at = ((GeneratorButton)button).getAt();
        }

    public void deselectAll()
        {
        for(Component c : generatorGrid.getComponents())
            {
            if (c instanceof MotifButton)
                {
                ((MotifButton)c).setSelected(false);
                }
            }
        }
    
    //// DRAG AND DROP
        
    public DropTargetListener buildDropTargetListener() { return new GeneratorUIDropTargetListener(); }
        
    class GeneratorUIDropTargetListener extends DropTargetAdapter 
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
                    else if (comp instanceof GeneratorButton)
                        {
                        int at = ((GeneratorButton)comp).getAt();
                        sequi.push();
                        if (dropped instanceof GeneratorButton)
                            {
                            //moveChild((GeneratorButton)dropped, at);
                            }
                        else
                            {
                            addChild(dropped.getMotifUI(), at);
                            }
                        }
                    generatorGrid.revalidate(); 
                    generatorGrid.repaint();
                                                
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
        "Removes the selected motif from the Generator Child slot.</html>";
        
    static final String CHILD_TOOLTIP = "<html><b>Child Motif</b><br>" +
        "Drag a Motif here to make it Generator's child Motif.<br><br>"+
        "Generator will use MIDI notes generated from this Child Motif to inform its generator<br>" +
        "algorithms.  Only some algorithms will use this MIDI data: other algorithms will produce<br>" +
        "music on their own, and so will entirel ignore the Child (if any).</html>";
    }
