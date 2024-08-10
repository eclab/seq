/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.blank.gui;

import seq.motif.blank.*;
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



public class BlankUI extends MotifUI
    {
    Blank blank;
    BlankInspector blankInspector;
        
    JPanel blankOuter;
    TitledBorder blankBorder;
    JPanel inspectorPane;
                        
    public static ImageIcon getStaticIcon() { return new ImageIcon(MotifUI.class.getResource("icons/blank.png")); }        // don't ask
    public ImageIcon getIcon() { return getStaticIcon(); }
    public static String getType() { return "Blank"; }
    
    public static MotifUI create(Seq seq, SeqUI ui)
        {
        return new BlankUI(seq, ui, new Blank(seq));
        }
        
    public static MotifUI create(Seq seq, SeqUI ui, Motif motif)
        {
        return new BlankUI(seq, ui, (Blank)motif);
        }
        
    public BlankUI(Seq seq, SeqUI sequi, Blank blank)
        {
        super(seq, sequi, blank);
        this.seq = seq;
        this.blank = blank;
        this.sequi = sequi;
        //build();
        }
        
    public void buildInspectors(JScrollPane scroll)
        {
        // Build the blank inspector holder
        blankOuter = new JPanel();
        blankOuter.setLayout(new BorderLayout());
        blankBorder = BorderFactory.createTitledBorder(null, "Blank");
        blankOuter.setBorder(blankBorder);

        // Add the blank inspector
        blankInspector = new BlankInspector(seq, blank, this);
        blankOuter.add(blankInspector, BorderLayout.CENTER);
                
        // Fill the panes
        inspectorPane = new JPanel();
        inspectorPane.setLayout(new BorderLayout());
        inspectorPane.add(blankOuter, BorderLayout.NORTH);
                
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
