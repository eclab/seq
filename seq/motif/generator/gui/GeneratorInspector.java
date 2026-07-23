/* 
   Copyright 2026 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.generator.gui;

import seq.motif.generator.*;
import seq.engine.*;
import seq.gui.*;
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
    JCheckBox omni;
    JCheckBox pass;
    JComboBox algorithm;
    TimeDisplay end;
    
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
            String[] outs = new String[seqOuts.length + 1];
            outs[0] = "<html><i>Same as Received</i></html>";
            for(int i = 0; i < seqOuts.length; i++)
                {
                outs[i + 1] = "" + (i + 1) + ": " + seqOuts[i].toString();
                }

            out = new JComboBox(outs);
            out.setSelectedIndex(generator.getOut() + 1);
            out.setMaximumRowCount(outs.length);
            out.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { generator.setOut(out.getSelectedIndex() - 1); }
                    finally { lock.unlock(); }                              
                    }
                });

            String[] ins = new String[seqOuts.length];
            for(int i = 0; i < seqOuts.length; i++)
                {
                ins[i] = "" + (i + 1) + ": " + seqOuts[i].toString();
                }

            in = new JComboBox(ins);
            in.setSelectedIndex(generator.getIn());
            in.setMaximumRowCount(ins.length);
            in.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { generator.setIn(in.getSelectedIndex()); }
                    finally { lock.unlock(); }                              
                    }
                });


            omni = new JCheckBox();
            omni.setSelected(generator.isOmni());
            omni.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { generator.setOmni(omni.isSelected()); }
                    finally { lock.unlock(); }                              
                    }
                });

            pass = new JCheckBox();
            pass.setSelected(generator.isPass());
            pass.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { generator.setPass(pass.isSelected()); }
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

                    if (lastSelectedAlgorithm > 0)
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
            }
        finally { lock.unlock(); }
        
        name.setToolTipText(NAME_TOOLTIP);
        out.setToolTipText(OUT_TOOLTIP);
        omni.setToolTipText(OMNI_TOOLTIP);
        in.setToolTipText(IN_TOOLTIP);
        end.setToolTipText(END_TOOLTIP);
        pass.setToolTipText(PASS_TOOLTIP);

        build(new String[] { "Name", "Algorithm", "Out", "Received", "Omni", "Pass", "Max End Time" }, 
            new JComponent[] 
                {
                name,
                algorithm,
                out,
                in,
                omni,
                pass,
                end,
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
            omni.setSelected(generator.isOmni()); 
            pass.setSelected(generator.isPass()); 
            algorithm.setSelectedIndex(generator.getAlgorithmIndex()); 
            }
        finally { lock.unlock(); }                              
        seq = old;
        name.update();
        if (end != null) end.revise();
        }


    static final String NAME_TOOLTIP = "<html><b>Name</b><br>" +
        "Sets the name of the Generator.  This will appear in the Motif List at left.</html>";

    static final String OUT_TOOLTIP = "<html><b>Out</b><br>" +
        "Sets the MIDI device for the Generator. Can be set to any device or to <b>Same as Received</b>.<br>" +
        "If set to Same as Received, then output will be the same as Received, even if Received itself<br>" +
        "has been overridden with <b>Omni</b>.</html>";
        
    static final String END_TOOLTIP = "<html><b>Max End Time</b><br>" +
        "Sets the maximum time that the Generator will play.  The Generator algorithm may choose<br>" +
        "to play for a shorter period than this.</html>";
        
    static final String OMNI_TOOLTIP = "<html><b>Omni</b><br>" +
        "If checked, all notes from the underlying child Motif will be sent to the generator<br>" +
        "algorithm.  Otherwise, only the notes designated for <b>Received</b> will be sent.</html>";

    static final String IN_TOOLTIP = "<html><b>Received</b><br>" +
        "Sets the device that the notes from the underlying child Motif must be assigned to<br>" +
        "in order to be sent to the generator algorithm.  This can be overridden by <b>Omni</b>.</html>";

    static final String PASS_TOOLTIP = "<html><b>Pass</b><br>" +
        "If set, then all MIDI from the underlying child Motif will be passed to the parent Motif<br>" +
        "in addition to MIDI from the generator algorithm.</html>";
    }
