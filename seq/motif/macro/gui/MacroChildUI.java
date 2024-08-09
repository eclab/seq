package seq.motif.macro.gui;

import seq.motif.macro.*;
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



public class MacroChildUI extends MotifUI
    {
    MacroChild macroChild;
    MacroChildInspector macroChildInspector;
        
    JPanel macroOuter;
    TitledBorder macroBorder;
    JPanel inspectorPane;
                        
    public static ImageIcon getStaticIcon() { return new ImageIcon(MotifUI.class.getResource("icons/macrochild.png")); }        // don't ask
    public ImageIcon getIcon() { return getStaticIcon(); }
    public static String getType() { return "Macro Child"; }
    
    public static MotifUI create(Seq seq, SeqUI ui)
        {
        return new MacroChildUI(seq, ui, new MacroChild(seq));
        }
        
    public static MotifUI create(Seq seq, SeqUI ui, Motif motif)
        {
        return new MacroChildUI(seq, ui, (MacroChild)motif);
        }
        
    public MacroChildUI(Seq seq, SeqUI sequi, MacroChild macroChild)
        {
        super(seq, sequi, macroChild);
        this.seq = seq;
        this.macroChild = macroChild;
        this.sequi = sequi;
        //build();
        }
        
    public void buildInspectors(JScrollPane scroll)
        {
        // Build the macro inspector holder
        macroOuter = new JPanel();
        macroOuter.setLayout(new BorderLayout());
        macroBorder = BorderFactory.createTitledBorder(null, "Macro Child");
        macroOuter.setBorder(macroBorder);

        // Add the macro inspector
        macroChildInspector = new MacroChildInspector(seq, macroChild, this, null, -1);
        macroOuter.add(macroChildInspector, BorderLayout.CENTER);
                
        // Fill the panes
        inspectorPane = new JPanel();
        inspectorPane.setLayout(new BorderLayout());
        inspectorPane.add(macroOuter, BorderLayout.NORTH);
                
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
