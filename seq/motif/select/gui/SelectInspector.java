/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.select.gui;

import seq.motif.select.*;
import seq.engine.*;
import seq.gui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.locks.*;

public class SelectInspector extends WidgetList
    {
    public static final String[] MODES = { "Single", "Single Repeating", "Multi", "Multi Repeating" };
    public static final String[] QUANTIZATIONS = { "None", "16th Note", "Quarter Note", "Measure" };
    Seq seq;
    Select select;
    SelectUI selectui;

    StringField name;
    JComboBox mode;
    JComboBox quantization;
    JComboBox out;
    JComboBox in;
    JComboBox device;
    JCheckBox playFirst;
    //JCheckBox immediate;
    JCheckBox cut;
    //JButton release;
    JButton finish;
    //SmallDial[] dials = new SmallDial[Motif.NUM_PARAMETERS];
    //JComboBox dialIn;
    
    public SelectInspector(Seq seq, Select select, SelectUI selectui)
        {
        this.seq = seq;
        this.select = select;
        this.selectui = selectui;

        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            name = new StringField(select.getName())
                {
                public String newValue(String newValue)
                    {
                    newValue = newValue.trim();
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { select.setName(newValue); }
                    finally { lock.unlock(); }
                    selectui.updateText();
                    return newValue;
                    }
                                
                public String getValue() 
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return select.getName(); }
                    finally { lock.unlock(); }
                    }
                };
            name.setColumns(MotifUI.INSPECTOR_NAME_DEFAULT_SIZE);
                
            mode = new JComboBox(MODES);
            mode.setSelectedIndex(select.getMode());
            mode.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { select.setMode(mode.getSelectedIndex()); }
                    finally { lock.unlock(); }                              
                    }
                });

            quantization = new JComboBox(QUANTIZATIONS);
            quantization.setSelectedIndex(select.getQuantization());
            quantization.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { select.setQuantization(quantization.getSelectedIndex()); }
                    finally { lock.unlock(); }                              
                    }
                });

            Out[] seqOuts = seq.getOuts();
            String[] outs = new String[seqOuts.length];
            for(int i = 0; i < seqOuts.length; i++)
                {
                outs[i] = "" + (i + 1) + ": " + seqOuts[i].toString();
                }

            out = new JComboBox(outs);
            out.setMaximumRowCount(outs.length);
            out.setSelectedIndex(select.getOut());
            out.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { select.setOut(out.getSelectedIndex()); }              // -1 is DEFAULT
                    finally { lock.unlock(); }                              
                    }
                });

            In[] seqIns = seq.getIns();
            String[] ins = new String[seqIns.length];
            for(int i = 0; i < seqIns.length; i++)
                {
                ins[i] = "" + (i + 1) + ": " + seqIns[i].toString();
                }
                
            in = new JComboBox(ins);
            in.setSelectedIndex(select.getIn());
            in.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { select.setIn(in.getSelectedIndex()); }              // -1 is DEFAULT
                    finally { lock.unlock(); }                              
                    }
                });
                        
            device = new JComboBox(Select.GRID_DEVICE_NAMES);
            device.setSelectedIndex(select.getGridDevice());
            device.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { select.setGridDevice(device.getSelectedIndex()); }
                    finally { lock.unlock(); }                              
                    }
                });

            playFirst = new JCheckBox();
            playFirst.setSelected(select.getPlayFirst());
            playFirst.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { select.setPlayFirst(playFirst.isSelected()); }
                    finally { lock.unlock(); }                              
                    }
                });


            cut = new JCheckBox();
            cut.setSelected(select.getCut());
            cut.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { select.setCut(cut.isSelected()); }
                    finally { lock.unlock(); }                              
                    }
                });

/*
  for(int i = 0; i < 8; i++)
  {
  final int _i = i;
  dials[i] = new SmallDial(select.getCC(i))
  {
  protected String map(double val) 
  {
  return String.valueOf((int)(val * 127));
  }
  public double getValue() 
  { 
  ReentrantLock lock = seq.getLock();
  lock.lock();
  try { return select.getCC(_i) / 127.0; }
  finally { lock.unlock(); }
  }
  public void setValue(double val) 
  { 
  if (seq == null) return;
  ReentrantLock lock = seq.getLock();
  lock.lock();
  try { select.setCC(_i, (int)(val * 127)); }
  finally { lock.unlock(); }
  }
  };
  }
*/

/*
  dialIn = new JComboBox(ins);
  dialIn.setSelectedIndex(select.getCCIn());
  dialIn.addActionListener(new ActionListener()
  {
  public void actionPerformed(ActionEvent e)
  {
  if (seq == null) return;
  ReentrantLock lock = seq.getLock();
  lock.lock();
  try { select.setCCIn(dialIn.getSelectedIndex()); }              // -1 is DEFAULT
  finally { lock.unlock(); }                              
  }
  });
*/
                        
            }
        finally { lock.unlock(); }

/*
  release = new JButton("All Off");
  release.addActionListener(new ActionListener()
  {
  public void actionPerformed(ActionEvent e)
  {
  if (seq == null) return;
  ReentrantLock lock = seq.getLock();
  lock.lock();
  try 
  { 
  SelectClip playingClip = (SelectClip)(selectui.getDisplayClip());
  if (playingClip != null)
  {
  playingClip.doRelease();
  }
  }
  finally { lock.unlock(); }                              
  }
  });
*/
   
        finish = new JButton("Finish");
        finish.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                if (seq == null) return;
                ReentrantLock lock = seq.getLock();
                lock.lock();
                try 
                    { 
                    SelectClip playingClip = (SelectClip)(selectui.getDisplayClip());
                    if (playingClip != null)
                        {
                        playingClip.doFinish();
                        }
                    }
                finally { lock.unlock(); }                              
                }
            });

        Box actions = new Box(BoxLayout.X_AXIS);
        //actions.add(release);
        actions.add(finish);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(actions, BorderLayout.WEST);
        
        /*
          Box dialBox = new Box(BoxLayout.X_AXIS);
          dialBox.add(new WidgetList(new String[] { "Param 1", "Param 2", "Param 3", "Param 4", },
          new JComponent[] { dials[0].getLabelledDial("127"), dials[1].getLabelledDial("127"), dials[2].getLabelledDial("127"), dials[3].getLabelledDial("127"), }));
          dialBox.createHorizontalStrut(16); 
          dialBox.add(new WidgetList(new String[] { "Param 5", "Param 6", "Param 7", "Param 8", },
          new JComponent[] { dials[4].getLabelledDial("127"), dials[5].getLabelledDial("127"), dials[6].getLabelledDial("127"), dials[7].getLabelledDial("127"), }));
        */               

        JPanel result = build(new String[] { "Actions", "Name", "Mode", "Control In", "Control Out", "Control Device", "Quantization", "Auto-Play First", "Cut Notes", }, //  "Param CCs In", "Param CCs" }, 
            new JComponent[] 
                {
                panel,
                name,
                mode,
                in,
                out,
                device,
                quantization,
                playFirst,
                //immediate,
                cut,
                //dialIn,
                //dialBox,
                });

        remove(result);
        add(result, BorderLayout.CENTER);               // re-add it as center

        add(new DefaultParameterList(seq, selectui), BorderLayout.NORTH);
        }
                
    public void revise()
        {
        Seq old = seq;
        seq = null;
        ReentrantLock lock = old.getLock();
        lock.lock();
        try 
            { 
            mode.setSelectedIndex(select.getMode()); 
            quantization.setSelectedIndex(select.getQuantization()); 
            out.setSelectedIndex(select.getOut()); 
            in.setSelectedIndex(select.getIn()); 
            device.setSelectedIndex(select.getGridDevice()); 
            playFirst.setSelected(select.getPlayFirst()); 
            cut.setSelected(select.getCut()); 
            //immediate.setSelected(select.getImmediate()); 
            }
        finally { lock.unlock(); }                              
        seq = old;
        name.update();
        }
    }
