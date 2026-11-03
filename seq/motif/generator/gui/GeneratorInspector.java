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
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.locks.*;

public class GeneratorInspector extends WidgetList
    {
    Seq seq;
    Generator generator;
    GeneratorUI generatorui;

    StringField name;
    JComboBox out;
    JComboBox in;
    JComboBox pass;
    JComboBox algorithm;
    TimeDisplay end;
    SmallDial seed;
    
    public static final String[] PASS_OPTIONS = { "None", "All", "Unused" };
    
    int lastSelectedAlgorithm = 0;

    public GeneratorInspector(Seq seq, Generator generator, GeneratorUI generatorui)
        {
        this.seq = seq;
        this.generator = generator;
        this.generatorui = generatorui;

        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            name = new StringField(generator.getName())
                {
                public String newValue(String newValue)
                    {
                    newValue = newValue.trim();
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { generator.setName(newValue); }
                    finally { lock.unlock(); }
                    generatorui.updateText();
                    return newValue;
                    }
                                
                public String getValue() 
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return generator.getName(); }
                    finally { lock.unlock(); }
                    }
                };
            name.setColumns(MotifUI.INSPECTOR_NAME_DEFAULT_SIZE);
 
 
            Out[] seqOuts = seq.getOuts();
            String[] outs = new String[seqOuts.length];
            for(int i = 0; i < seqOuts.length; i++)
                {
                outs[i] = "" + (i + 1) + ": " + seqOuts[i].toString();
                }

            out = new JComboBox(outs);
            out.setSelectedIndex(generator.getOut());
            out.setMaximumRowCount(outs.length);
            out.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { generator.setOut(out.getSelectedIndex()); }
                    finally { lock.unlock(); }                              
                    }
                });

            String[] ins = new String[seqOuts.length + 1];
            ins[0] = "<html><i>All Devices</i></html>";
            for(int i = 0; i < seqOuts.length; i++)
                {
                ins[i + 1] = "" + (i + 1) + ": " + seqOuts[i].toString();
                }

            in = new JComboBox(ins);
            in.setSelectedIndex(generator.getIn() + 1);
            in.setMaximumRowCount(ins.length);
            in.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { generator.setIn(in.getSelectedIndex() - 1); }
                    finally { lock.unlock(); }                              
                    }
                });


            pass = new JComboBox(PASS_OPTIONS);
            pass.setSelectedIndex(generator.getPass());
            pass.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { generator.setPass(pass.getSelectedIndex()); }
                    finally { lock.unlock(); }                              
                    }
                });

            algorithm = new JComboBox(Algorithm.getAlgorithmNames());
            algorithm.setSelectedIndex(generator.getAlgorithmIndex());
            
            // we need to be told what the last item WAS
            algorithm.addItemListener(new ItemListener()
                {
                public void itemStateChanged(ItemEvent e)
                    {
                    if (e.getStateChange() == ItemEvent.DESELECTED)
                        {
                        lastSelectedAlgorithm = generator.getAlgorithmIndex();
                        }
                    }
                });
                
            algorithm.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    int alg = 0;
                    try
                        {
                        alg = generator.getAlgorithmIndex();
                        }
                    finally { lock.unlock(); }

                    //if (lastSelectedAlgorithm > 0)
                        {
                        // warn that we're about to reset everything
                        if (!generatorui.getSeqUI().showSimpleConfirm("Change Algorithm", "Change the algorithm?\n\nChanging the Algorithm will reset all of your Generator settings.", "Change"))
                            {
                            return; 
                            }
                        }
                    
                    // Okay, let's go ahead and change it
                    lock.lock();
                    try 
                        { 
						Prefs.setLastInt("GeneratorAlgorithm", algorithm.getSelectedIndex());
                        GeneratorClip clip = (GeneratorClip)(generator.getPlayingClip());
                        if (clip != null)
                            {
                            clip.release();         // force the algorithm node to release all its notes
                            }
                        // Now we can change the algorithm and algorithm node 
                        generator.setAlgorithmIndex(algorithm.getSelectedIndex()); 
                        // Rebuild algorithm
                        if (clip != null)
                            {
                            clip.resetAlgorithmNode();
                            clip.release();         // force a rebuild of algorithm node
                            }
                        }
                    finally { lock.unlock(); }  
                    generatorui.updateAlgorithmUI();
                    }
                });


            end = new TimeDisplay(generator.getEnd() , seq)
                {
                public int getTime()
                    {
                    return generator.getEnd(); 
                    }
                        
                public void setTime(int time)
                    {
                    generator.setEnd(time);
                    }
                };
            end.setDisplaysTime(true);
            end.setToolTipText(END_TOOLTIP);


            seed = new SmallDial(0)
                {
                protected String map(double val) 
                    {
                    if (val == 0) return "Global";
                    else return String.format("%.4f", val);
                    }
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return generator.getLocalRandomSeed(); }
                    finally { lock.unlock(); }
                    }
                public void setValue(double val) 
                    { 
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { generator.setLocalRandomSeed(val); }
                    finally { lock.unlock(); }
                    }
                };



            }
        finally { lock.unlock(); }
        
        name.setToolTipText(NAME_TOOLTIP);
        algorithm.setToolTipText(ALGORITHM_TOOLTIP);
        out.setToolTipText(OUT_TOOLTIP);
        in.setToolTipText(IN_TOOLTIP);
        end.setToolTipText(END_TOOLTIP);
        pass.setToolTipText(PASS_TOOLTIP);
        seed.setToolTipText(SEED_TOOLTIP);

        build(new String[] { "Name", "Algorithm", "Out", "Received", "Pass", "Max End Time", "Seed" }, 
            new JComponent[] 
                {
                name,
                algorithm,
                out,
                in,
                pass,
                end,
                seed.getLabelledDial("0.8888"),
                });
        add(new DefaultParameterList(seq, generatorui), BorderLayout.SOUTH);
        generatorui.revalidate();                            
        }
                
    public void revise()
        {
        Seq old = seq;
        seq = null;
        ReentrantLock lock = old.getLock();
        lock.lock();
        try 
            {
            out.setSelectedIndex(generator.getOut()); 
            in.setSelectedIndex(generator.getIn()); 
            pass.setSelectedIndex(generator.getPass()); 
            algorithm.setSelectedIndex(generator.getAlgorithmIndex()); 
            }
        finally { lock.unlock(); }                              
        seq = old;
        name.update();
        if (end != null) end.revise();
        if (seed != null) seed.redraw();
        }


    static final String NAME_TOOLTIP = "<html><b>Name</b><br>" +
        "Sets the name of the Generator.  This will appear in the Motif List at left.</html>";

    static final String ALGORITHM_TOOLTIP = "<html><b>Algorithm</b><br>" +
        "Sets the Algorithm used by the Generator to generate music.</html>";

    static final String OUT_TOOLTIP = "<html><b>Out</b><br>" +
        "Sets the MIDI device for the Generator.</html>";
        
    static final String END_TOOLTIP = "<html><b>Max End Time</b><br>" +
        "Sets the maximum time that the Generator will play.  The Generator algorithm may choose<br>" +
        "to play for a shorter period than this.</html>";
        
    static final String IN_TOOLTIP = "<html><b>Received</b><br>" +
        "Sets the device that the notes from the underlying child Motif must be assigned to<br>" +
        "in order to be sent to the generator algorithm.  If this is set to <i>All Devices</i>,<br>"+
        "then all notes will be sent to the generator regardless of their devices.</html>";

    static final String PASS_TOOLTIP = "<html><b>Pass</b><br>" +
        "Sets what MIDI is passed directly from the child Motif to the parent Motif.<br>" +
        "The options are:<ul>" +
        "<li><b>None</b>: No MIDI is passed from the child to the parent." + 
        "<p><li><b>All</b>: All MIDI is passed from the child to the parent, including any MIDI used by the generator." + 
        "<p><li><b>Unused</b>: MIDI is passed from the child to the parent if it was not used by the generator.</ul></html>";

    static final String SEED_TOOLTIP = "<html><b>Random Number Seed</b><br>" +
        "Specifies the seed of the Generator's local random number generator.<br>" +
        "If the value is 0 (or \"Global\"), then Seq's global random number generator will be used instead.</html>";
    }
