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

public class TrackHeaderPanel extends JPanel
    {
    Seq seq;
    StepSequence ss;
    StepSequenceUI ssui;
        
    public TrackHeaderPanel(Seq seq, StepSequence ss, StepSequenceUI ssui)
        {
        this.seq = seq;
        this.ss = ss;
        this.ssui = ssui;
        setDropTarget(new DropTarget(this, new TrackHeaderDropTargetListener()));
        }

    class TrackHeaderDropTargetListener extends DropTargetAdapter 
        {
        public void dragEnter(DropTargetDragEvent dtde)
            {
            try
                {
                Component comp = dtde.getDropTargetContext().getComponent();
                if (comp == null) return;
                else if (comp instanceof TrackHeaderPanel)
                    {
                    for(TrackUI track : ssui.getTracks())
                        {
                        track.getHeader().setBorder(track.getHeader().matte);
                        }
                    ssui.trackHeaders.setBorder(ssui.matteSelected);
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
                else if (comp instanceof TrackHeaderPanel)
                    {
                    ssui.trackHeaders.setBorder(ssui.matte);
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
                    TrackHeader droppedPanel = (TrackHeader)transferableObj;
                                
                    Point p = dtde.getLocation();
                    Component comp = dtde.getDropTargetContext().getComponent();
                    if (comp == null) return;
                    else if (comp instanceof TrackHeaderPanel)
                        {
                        seq.getLock().lock();
                        try
                            {
                            int before = ss.getNumTracks();
                            ssui.doMove(droppedPanel.track.getTrackNum(), before - 1);
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
    }
