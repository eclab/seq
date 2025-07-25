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
            name.setToolTipText(NAME_TOOLTIP);
                
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
            mode.setToolTipText(MODE_TOOLTIP);

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
            quantization.setToolTipText(QUANTIZATION_TOOLTIP);

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
            out.setToolTipText(CONTROL_OUT_TOOLTIP);

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
            in.setToolTipText(CONTROL_IN_TOOLTIP);
      
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
            device.setToolTipText(CONTROL_DEVICE_TOOLTIP);

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
            playFirst.setToolTipText(AUTO_PLAY_FIRST_TOOLTIP);


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
            cut.setToolTipText(CUT_NOTES_TOOLTIP);

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
        finish.setToolTipText(FINISH_BUTTON_TOOLTIP);

        Box actions = new Box(BoxLayout.X_AXIS);
        //actions.add(release);
        actions.add(finish);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(actions, BorderLayout.WEST);
        panel.setToolTipText(FINISH_BUTTON_TOOLTIP);

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

    static final String FINISH_BUTTON_TOOLTIP = "<html><b>Finish</b><br>" +
        "Indicates to the parent motif that this Select believes it has finished playing.</html>";
        
    static final String NAME_TOOLTIP = "<html><b>Name</b><br>" +
        "Sets the name of the Select.  This will appear in the Motif List at left.</html>";

    static final String MODE_TOOLTIP = "<html><b>Mode</b><br>" +
        "Sets the Select's mode of operation, to one of:" +
        "<ul>" + 
        "<li><b>Single</b> only allows one child node to play at a time.  When a node finishes<br>" +
        "playing, it stops.  If a second node  is selected while another is playing, the second<br>" +
        "node will begin playing after the first one finishes." +
        "<li><b>Single Repeating</b> only allows one child node to play at a time, but when a node<br>" +
        "finishes playing, starts up again, unless another node has been selected, in which case<br>" +
        "that second node starts playing instead.  You can stop a node from repeating by deselecting it." +
        "<li><b>Multi</b> allows multiple nodes to be selected to play simultaneously. When a node<br>" +
        "finishes playing, it stops." +
        "<li><b>Multi Repeating</b> allows multiple nodes to be selected to play simultaneously. When a node<br>" +
        "finishes playing, it stops.  You can stop a node from repeating by deselecting it." +
        "</ul></html>";

    static final String CONTROL_IN_TOOLTIP = "<html><b>Control In</b><br>" +
        "The MIDI Input for the optional controller device.  This should be set to the same<br>" +
        "device as the <b>Control Out</b>.</html>";

    static final String CONTROL_OUT_TOOLTIP = "<html><b>Control In</b><br>" +
        "The MIDI Output for the optional controller device.  This should be set to the same<br>" +
        "device as the <b>Control In</b>.</html>";

    static final String CONTROL_DEVICE_TOOLTIP = "<html><b>Control Device</b><br>" +
        "The particular kind of controller device used.  At present this is one of:" +
        "<ul>" +
        "<li><b>Launchpad MK 1</b>: Launchpad (original), Launchpad S" +
        "<li><b>Launchpad MK 3</b>: Launchpad Mini MK 3" +
        "</ul></html>";

    static final String QUANTIZATION_TOOLTIP = "<html><b>Quantization</b><br>" +
        "The time boundary at which nodes begin playing.  One of:" +
        "<ul>" +
        "<li>None (playing begins immediately)" +
        "<li>The next 16th note" +
        "<li>The next quarter note" +
        "<li>The next measure (bar) boundary" +
        "</ul></html>";

    static final String AUTO_PLAY_FIRST_TOOLTIP = "<html><b>Auto-Play First</b><br>" +
        "If selected, then when the Select begins playing, and there is a node in the top-left<br>" +
        "corner (position 1,1), it immediately starts playing.</html>";

    static final String CUT_NOTES_TOOLTIP = "<html><b>Cut Notes</b><br>" +
        "If selected, then when a node is terminated early, it stops playing notes immediately<br>" +
        "instead of letting them finish for their normal length.</html>";
    }
