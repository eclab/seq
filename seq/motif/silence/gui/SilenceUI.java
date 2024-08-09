package seq.motif.silence.gui;

import seq.motif.silence.*;
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



public class SilenceUI extends MotifUI
    {
    Silence silence;
    SilenceInspector silenceInspector;
        
    JPanel silenceOuter;
    TitledBorder silenceBorder;
    JPanel inspectorPane;
                        
    public static ImageIcon getStaticIcon() { return new ImageIcon(MotifUI.class.getResource("icons/blank.png")); }        // don't ask
    public ImageIcon getIcon() { return getStaticIcon(); }
    public static String getType() { return "Silence"; }
    
    public static MotifUI create(Seq seq, SeqUI ui)
        {
        return new SilenceUI(seq, ui, new Silence(seq));
        }
        
    public static MotifUI create(Seq seq, SeqUI ui, Motif motif)
        {
        return new SilenceUI(seq, ui, (Silence)motif);
        }
        
    public SilenceUI(Seq seq, SeqUI sequi, Silence silence)
        {
        super(seq, sequi, silence);
        this.seq = seq;
        this.silence = silence;
        this.sequi = sequi;
        //build();
        }
        
    public void buildInspectors(JScrollPane scroll)
        {
        // Build the silence inspector holder
        silenceOuter = new JPanel();
        silenceOuter.setLayout(new BorderLayout());
        silenceBorder = BorderFactory.createTitledBorder(null, "Silence");
        silenceOuter.setBorder(silenceBorder);

        // Add the silence inspector
        silenceInspector = new SilenceInspector(seq, silence, this);
        silenceOuter.add(silenceInspector, BorderLayout.CENTER);
                
        // Fill the panes
        inspectorPane = new JPanel();
        inspectorPane.setLayout(new BorderLayout());
        inspectorPane.add(silenceOuter, BorderLayout.NORTH);
                
        scroll.setViewportView(inspectorPane);
        }
                
    public void buildPrimary(JScrollPane scroll)
        {
        }
                
    public JPanel buildConsole()
        {
        JPanel console = new JPanel();
        return console; 
        }
    }
