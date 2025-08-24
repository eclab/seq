/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.stepsequence.gui;

import seq.motif.stepsequence.*;
import seq.engine.*;
import seq.gui.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.util.concurrent.locks.*;

// For Drag and Drop
import java.awt.dnd.*;
import java.awt.datatransfer.*;

public class TrackHeader extends JPanel implements Transferable
    {
    Seq seq;
    StepSequence ss;
    StepSequenceUI ssui;
    TrackUI track;
        
    JCheckBox trackSolo;
    JCheckBox trackMute;
    JCheckBox trackLearn;
    JLabel trackName;
    public static final char BLACK_CIRCLE = '\u25CF';
    JLabel handle = new JLabel("");
    boolean selected = false;
        
    int MAX_WIDTH = 128;
        
    public JCheckBox getTrackLearn() { return trackLearn; }
        
    Box box = new Box(BoxLayout.X_AXIS);
        
    public static final Border matte = BorderFactory.createCompoundBorder(
        BorderFactory.createEmptyBorder(0,0,0,4),
        BorderFactory.createMatteBorder(1,0,0,0, Color.BLACK));

    public static final Border matteSelected = BorderFactory.createCompoundBorder(
        BorderFactory.createEmptyBorder(0,0,0,4),
        BorderFactory.createMatteBorder(1,0,0,0, Color.RED));

    public static final Border matteTopSelected = BorderFactory.createCompoundBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(0,0,0,4),
            BorderFactory.createMatteBorder(0,0,1,0, Color.RED)),
        BorderFactory.createMatteBorder(1,0,0,0, Color.BLACK));
        
    public void setSelected(boolean val) { selected = val; updateHandle(); }
        
    public void updateHandle()
        {
        if (!selected)
            handle.setText("  " + (track.getTrackNum() + 1));
        else handle.setText("<html><font color=red>&nbsp;&nbsp;" + (track.getTrackNum() + 1) + "</font></html>"); 
        
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            { 
            int trackNum = track.getTrackNum();
            trackName.setText(trackName(trackNum));             // so it's in color as appropriate
            }
        finally { lock.unlock(); }                              
        }

    public TrackHeader(Seq seq, StepSequence ss, StepSequenceUI ssui, TrackUI track)
        {
        this.seq = seq;
        this.ss = ss;
        this.track = track;
        this.ssui = ssui;

        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            int trackNum = track.getTrackNum();
            trackMute = new JCheckBox("M");
            trackMute.setSelected(ss.isTrackMuted(trackNum));
            trackMute.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { ss.setTrackMuted(trackNum, trackMute.isSelected()); }
                    finally { lock.unlock(); }                              
                    }
                });

            trackLearn = new JCheckBox("L");
            trackLearn.setSelected(ss.isTrackLearning(trackNum));
            trackLearn.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { ss.setTrackLearning(trackNum, trackLearn.isSelected()); }
                    finally { lock.unlock(); }                              
                    }
                });
            trackSolo = new JCheckBox("S");
            trackSolo.setSelected(ss.isTrackSoloed(trackNum));
            trackSolo.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { ss.setTrackSoloed(trackNum, trackSolo.isSelected()); }
                    finally { lock.unlock(); }                              
                    }
                });

            trackName = new JLabel(getName(), SwingConstants.RIGHT)
                {
                public Dimension getMaximumSize()
                    {
                    Dimension size = super.getMaximumSize();
                    size.setSize(MAX_WIDTH, size.getHeight());
                    return size;
                    }
                
                public Dimension getPreferredSize()
                    {
                    Dimension size = super.getPreferredSize();
                    size.setSize(MAX_WIDTH, size.getHeight());
                    return size;
                    }               
                };

            trackName.addMouseMotionListener(new MouseMotionAdapter()
                {
                public void mouseDragged(MouseEvent e)
                    {
                    getTransferHandler().exportAsDrag(TrackHeader.this, e, TransferHandler.MOVE);
                    }
                });
            }
        finally { lock.unlock(); }
                
        box.add(trackLearn);
        box.add(trackSolo);
        box.add(trackMute);
        setLayout(new BorderLayout());
        JPanel right = new JPanel();
        right.setLayout(new BorderLayout());
        right.add(box, BorderLayout.EAST);
        JPanel console = new JPanel();
        console.setLayout(new BorderLayout());
        console.add(right, BorderLayout.SOUTH);
        console.add(trackName, BorderLayout.NORTH);
        add(console, BorderLayout.EAST);
        add(handle, BorderLayout.CENTER);
                
        addDragListeners(this);
        addDragListeners(handle);
        addDragListeners(trackName);

        setTransferHandler(new TrackHeaderTransferHandler());
        setDropTarget(new DropTarget(this, new TrackHeaderDropTargetListener()));
        setBorder(matte);
       
        // Tooltips
        trackSolo.setToolTipText(SOLO_BUTTON_TOOLTIP);
        trackMute.setToolTipText(MUTE_BUTTON_TOOLTIP);
        trackLearn.setToolTipText(LEARN_BUTTON_TOOLTIP);
        trackName.setToolTipText(TRACK_NAME_TOOLTIP);
        handle.setToolTipText(TRACK_NUMBER_TOOLTIP);
        setToolTipText(TRACK_HEADER_TOOLTIP);
        }
                                
    void addDragListeners(JComponent component)
        {
        component.addMouseListener(new MouseAdapter()
            {
            public void mousePressed(MouseEvent e)
                {
                ssui.setSelectedTrackNum(track.getTrackNum());
                }
            });
                        
        component.addMouseMotionListener(new MouseMotionAdapter()
            {
            public void mouseDragged(MouseEvent e)
                {
                getTransferHandler().exportAsDrag(TrackHeader.this, e, TransferHandler.MOVE);
                }
            });
        }



    public String getName()
        {
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            { 
            return ss.getTrackName(track.getTrackNum());
            }
        finally { lock.unlock(); }                              
        }
    
    // Assumes we already have a lock
    String trackName(int trackNum)
        {
        String name = ss.getTrackName(trackNum);
        if (name == null) name = "";
        else name = name.trim();
        if (name == "") name = "Track " + trackNum;
        if (selected) return "<html><font color=red>"+name+"</font></html>";
        else return "<html>"+name+"</html>";
        }
        
    public void revise()
        {
        ReentrantLock lock = seq.getLock();
        Seq old = seq;
        seq = null;
        lock.lock();
        try 
            { 
            int trackNum = track.getTrackNum();
            trackSolo.setSelected(ss.isTrackSoloed(trackNum)); 
            trackMute.setSelected(ss.isTrackMuted(trackNum)); 
            trackLearn.setSelected(ss.isTrackLearning(trackNum));
            trackName.setText(trackName(trackNum));
            }
        finally { lock.unlock(); }                              
        seq = old;
        repaint();      // update the dials
        }
                
                
    //// DRAG AND DROP
        
    /// Drag-and-drop data flavor
    static DataFlavor dataFlavor = null;
    
    static
        {
        try
            {
            dataFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=seq.motif.stepsequence.gui.TrackHeader");
            }
        catch (ClassNotFoundException ex)
            {
            ex.printStackTrace();
            }
        }

    public Object getTransferData(DataFlavor flavor) 
        {
        if (flavor.equals(TrackHeader.dataFlavor))
            return this;
        else
            return null;
        }
                
    public DataFlavor[] getTransferDataFlavors() 
        {
        return new DataFlavor[] { TrackHeader.dataFlavor };
        }

    public boolean isDataFlavorSupported(DataFlavor flavor) 
        {
        // This is a stupid method
        return (flavor.equals(TrackHeader.dataFlavor));
        }
        
    class TrackHeaderTransferHandler extends TransferHandler implements DragSourceMotionListener 
        {
        public Transferable createTransferable(JComponent c) 
            {
            if (c instanceof TrackHeader) 
                {
                return (Transferable) c;
                }
            else return null;
            }

        public int getSourceActions(JComponent c) 
            {
            if (c instanceof TrackHeader) 
                {
                return TransferHandler.MOVE;
                }
            else return TransferHandler.NONE;
            }
        
                
        //public Image getDragImage()
        //      {
        //      return new ImageIcon(AppMenu.class.getResource("About.png")).getImage();
        //      }

        public void dragMouseMoved(DragSourceDragEvent dsde) {}
        } 
        
    class TrackHeaderDropTargetListener extends DropTargetAdapter 
        {
        public void dragOver(DropTargetDragEvent dtde)
            {
            try
                {
                Component comp = dtde.getDropTargetContext().getComponent();
                if (comp == null) return;
                else if (comp instanceof TrackHeader)
                    {
                    Point p = dtde.getLocation();
                    int trackNum = track.getTrackNum();
                    if (p.getY() < comp.getBounds().getHeight() / 2)
                        {
                        setBorder(matteSelected);
                        if (trackNum + 1 < ssui.getTracks().size())
                            {
                            ssui.getTrack(trackNum + 1).getHeader().setBorder(matte);
                            }
                        ssui.trackHeaders.setBorder(ssui.matte);
                        }
                    else
                        {
                        setBorder(matte);
                        if (trackNum + 1 < ssui.getTracks().size())
                            {
                            ssui.getTrack(trackNum + 1).getHeader().setBorder(matteSelected);
                            ssui.trackHeaders.setBorder(ssui.matte);
                            }
                        else 
                            {
                            ssui.trackHeaders.setBorder(ssui.matteSelected);
                            }
                        }
                    }
                }
            catch (Exception ex)
                {
                ex.printStackTrace();
                }
            }
                        
                
        // Hopefully this happens FIRST
        public void dragExit(DropTargetEvent dtde)
            {
            try
                {
                Component comp = dtde.getDropTargetContext().getComponent();
                if (comp == null) return;
                else if (comp instanceof TrackHeader)
                    {
                    int trackNum = track.getTrackNum();
                    setBorder(matte);
                    if (trackNum + 1 < ssui.getTracks().size())
                        {
                        ssui.getTrack(trackNum + 1).getHeader().setBorder(matte);
                        }
                    else if (trackNum > 0)
                        {
                        ssui.getTrack(trackNum - 1).getHeader().setBorder(matte);
                        }
                    else if (trackNum == ssui.getTracks().size())
                        {
                        ssui.trackHeaders.setBorder(ssui.matte);
                        }
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
                    if (dtde.getTransferable().isDataFlavorSupported(TrackHeader.dataFlavor))
                        {
                        transferableObj = dtde.getTransferable().getTransferData(TrackHeader.dataFlavor);
                        } 
                    } 
                catch (Exception ex) {  System.err.println("Can't drag and drop that"); }
                                
                if (transferableObj != null && transferableObj instanceof TrackHeader)
                    {
                    // After dropping, we don't tell the last header that we did a dragExit
                    // so we might as well just manually turn the headers off for everyone
                    for(TrackUI trackui : ssui.tracks)
                        {
                        trackui.getHeader().setBorder(matte);
                        }
                                
                    TrackHeader droppedPanel = (TrackHeader)transferableObj;

                    Component comp = dtde.getDropTargetContext().getComponent();
                    if (comp == null) return;
                    else if (comp instanceof TrackHeader)
                        {
                        Point p = dtde.getLocation();
                        int onto = ((TrackHeader)comp).track.getTrackNum();
                        ssui.getSeqUI().push();
                        seq.getLock().lock();
                        try
                            {
                            ssui.doMove(droppedPanel.track.getTrackNum(), 
                                (p.getY() < comp.getBounds().getHeight() / 2 ? onto - 1 : onto));
                            }
                        finally
                            {
                            seq.getLock().unlock();
                            }
                        ssui.revalidate();              // needed?
                        ssui.repaint();
                        }
                    }
                }
            catch (Exception ex)
                {
                ex.printStackTrace();
                }
            }
        }



    /*** Tooltips ***/
        
    static final String TRACK_NAME_TOOLTIP = "<html><b>Track Name</b><br>" +
        "The track's name." + 
        "<p>To set the name, select the track (click on the name for example),<br>" +
        "then change the <b>Name</b> field in the <b>Track</b> inspector</b> at right.<br>" + 
        "Remember to press <i>Return</i>.</html>";
        
    static final String TRACK_HEADER_TOOLTIP = "<html><b>Track Header</b><br>" +
        "Drag the track header to move it to another position in the stequence.<br>" +
        "A red line will show where it will be inserted.<br>" +
        "The track's current number in the sequence is shown at left."; 
        
    static final String TRACK_NUMBER_TOOLTIP = TRACK_HEADER_TOOLTIP; 

    static final String MUTE_BUTTON_TOOLTIP = "<html><b>Mute</b><br>" +
        "Mutes this track while playing.</html>";
        
    static final String SOLO_BUTTON_TOOLTIP = "<html><b>Solo</b><br>" +
        "Mutes all tracks but this one while playing.</html>";
        
    static final String LEARN_BUTTON_TOOLTIP = "<html><b>Learn</b><br>" +
        "Sets note learning on or off.<br><br>" +
        "While note learning is on, and the step sequence is playing or recording,<br>" +
        "if you play a note the track will learn to respond to this note.";
    }
