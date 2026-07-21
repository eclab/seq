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
							clip.release();		// force the algorithm node to release all its notes
							}
                    	// Now we can change the algorithm and algorithm node 
                    	generator.setAlgorithmIndex(algorithm.getSelectedIndex()); 
                    	// Rebuild algorithm
						if (clip != null)
							{
							clip.resetAlgorithmNode();
							clip.release();		// force a rebuild of algorithm node
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
        
        /*
        name.setToolTipText(NAME_TOOLTIP);
        out.setToolTipText(OUT_TOOLTIP);
        omni.setToolTipText(OMNI_INPUT_TOOLTIP);
        rate.setToolTipText(STEP_RATE_TOOLTIP);
        arp.setToolTipText(ARPEGGIO_TYPE_TOOLTIP);
        octaves.setToolTipText(OCTAVES_TOOLTIP);
        length.setToolTipText(PATTERN_LENGTH_TOOLTIP);
        velocity.setToolTipText(VELOCITY_TOOLTIP);
        asPlayed.setToolTipText(AS_PLAYED_TOOLTIP);
        velocityPanel.setToolTipText(VELOCITY_TOOLTIP);
        newChordReset.setToolTipText(NEW_CHORD_RESET_TOOLTIP);
        activeAlways.setToolTipText(ALWAYS_TOOLTIP);
        activeFrom.setToolTipText(FROM_TOOLTIP);
        activeTo.setToolTipText(FROM_TOOLTIP);
    	*/

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
        "Sets the MIDI output for the Generator.  This also may restrict which notes are<br>"+
        "arpeggiated (see <b>Omni Input</b>).</html>";
        
    static final String END_TOOLTIP = "<html><b>Max End Time</b><br>" +
        "Sets the maximum time that the Generator will play.  The Generator algorithm may choose<br>" +
        "to play for a shorter period than this.</html>";
        
    static final String OMNI_INPUT_TOOLTIP = "<html><b>Omni Input</b><br>" +
        "If checked, all notes from the underlying child Motif will be converted into generators.<br>" +
        "Otherwise, only the notes designated for the <b>Out</b> will be arpeggiated,<br>" +
        "and the others will be simply passed through.</html>";

    static final String STEP_RATE_TOOLTIP = "<html><b>Step Rate</b><br>" +
        "Sets amount of time between each step of the generator.</html>";
        
    static final String ARPEGGIO_TYPE_TOOLTIP = "<html><b>Generator Type</b><br>" +
        "Sets the generator type" + 
        "<ul>" +
        "<li><b>Up</b>&nbsp;&nbsp;Each note in the chord is played in order, lowest to highest." +
        "<li><b>Down</b>&nbsp;&nbsp;Each note in the chord is played in order, highest to lowest." +
        "<li><b>Up-Down</b>&nbsp;&nbsp;Up and then Down, except the top note and bottom note are not played twice." +
        "<li><b>Up-Down-Plus</b>&nbsp;&nbsp;Up, then the lowest note is transposed and played again at the top, then Down." +
        "<li><b>Random</b>&nbsp;&nbsp;Chord notes are played randomly.  Seq tries to not play the same note twice in a row." +
        "<li><b>Pattern</b>&nbsp;&nbsp;Chord notes are using the Pattern Grid at left." +
        "</ul></html>";

    static final String OCTAVES_TOOLTIP = "<html><b>Octaves</b><br>" +
        "Sets the number of octaves that the generator will repeat.</html>";

    static final String PATTERN_LENGTH_TOOLTIP = "<html><b>Pattern Length</b><br>" +
        "Sets the length of the Pattern in the Pattern Grid at left.</html>";

    static final String VELOCITY_TOOLTIP = "<html><b>Velocity</b><br>" +
        "Sets the Velocity (Volume) of all arpeggiated notes.<br><br>" +
        "Only has an effect if <b>As Played</b> is unchecked.</html>";

    static final String AS_PLAYED_TOOLTIP = "<html><b>As Played</b><br>" +
        "Determines whether the Velocity (Volume) of the arpeggiated notes is determined<br>" +
        "by the underlying notes of the child Motif, or by the <b>Velocity</b> knob.</html>";

    static final String NEW_CHORD_RESET_TOOLTIP = "<html><b>New Chord Reset</b><br>" +
        "Sets whether the generator resets when all underlying notes are finished</br>" +
        "and new ones are played in the underlying child Motif.</html>";

    static final String ALWAYS_TOOLTIP = "<html><b>Always</b><br>" +
        "Sets whether the Generator plays for the full length of time of its underlying Child Motif.</html>";

    static final String FROM_TOOLTIP = "<html><b>From</b><br>" +
        "Sets when in the underlying Child Motif the Generator starts arpeggiating its notes.<br><br>" +
        "Only has an effect if <b>Always</b> is unchecked.</html>";
        
    static final String TO_TOOLTIP = "<html><b>To</b><br>" +
        "Sets when in the underlying Child Motif the Generator stops arpeggiating its notes.<br><br>" +
        "Only has an effect if <b>Always</b> is unchecked.</html>";
    }
