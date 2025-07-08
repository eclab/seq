/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.notes.gui;

import seq.motif.notes.*;
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
import java.io.*;

// For Drag and Drop
import java.awt.dnd.*;
import java.awt.datatransfer.*;



public class NotesUI extends MotifUI
    {
    Notes notes;
    
    // Inspector stuff
    EventInspector childInspector;    
    JPanel childOuter;
    TitledBorder childBorder;
    JPanel inspectorPane;
    JPanel notesOuter;
    TitledBorder notesBorder;
    NotesInspector notesInspector;
    
    // Menu
    JMenu menu;
    JCheckBoxMenuItem autoArmItem;
    
    // The displays
    EventTable table;
    GridUI gridui;
    //JTabbedPane tabs;
    
    public Notes getNotes() { return notes; }
    public static ImageIcon getStaticIcon() { return new ImageIcon(MotifUI.class.getResource("icons/notes.png")); }        // don't ask
    public ImageIcon getIcon() { return getStaticIcon(); }
    public static String getType() { return "Notes"; }
    public static MotifUI create(Seq seq, SeqUI ui)
        {
        boolean autoArm = Prefs.getLastBoolean("ArmNewNotesMotifs", false);
        return new NotesUI(seq, ui, new Notes(seq, autoArm));
        }

  public NoteUI getNoteUIFor(Notes.Note note, int pitch)
  {
  return gridui.getNoteUIFor(note, pitch);
  } 
     
    public static MotifUI create(Seq seq, SeqUI ui, Motif motif)
        {
        return new NotesUI(seq, ui, (Notes)motif);
        }
        
    public NotesUI(Seq seq, SeqUI sequi, Notes notes)
        {
        super(seq, sequi, notes);
        this.seq = seq;
        this.notes = notes;
        this.sequi = sequi;
  
        // disarm others
        if (notes.isArmed() && getSeqUI().getDisarmsAllBeforeArming())
            {
            ReentrantLock lock = seq.getLock();
            lock.lock();
            try
                {
                seq.disarmAll();
                }
            finally { lock.unlock(); }
            getSeqUI().incrementRebuildInspectorsCount();           // show disarmed
            }

        //build();
        }
    
    public void buildMenu()
        {
        menu = new JMenu("Notes");
        JMenuItem load = new JMenuItem("Load MIDI File...");
        load.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                loadMIDIFile();
                }
            });
        
        menu.add(load);

        menu.addSeparator();
        
        JMenuItem quantize = new JMenuItem("Quantize...");
        quantize.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                doQuantize();
                }
            });
        menu.add(quantize);

        JMenuItem shiftTime = new JMenuItem("Shift Time...");
        shiftTime.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                doShiftTime();
                }
            });
        menu.add(shiftTime);
        
        JMenuItem stretchTime = new JMenuItem("Stretch Time...");
        stretchTime.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                doStretchTime();
                }
            });
        menu.add(stretchTime);
                
        JMenuItem trimTime = new JMenuItem("Trim Blank Time from Start");
        trimTime.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                doTrimTime();
                }
            });
        menu.add(trimTime);

        JMenuItem randomizeTime = new JMenuItem("Randomize Time...");
        randomizeTime.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                doRandomizeTime();
                }
            });
                
        menu.add(randomizeTime);

        menu.addSeparator();
        
        JMenuItem transpose = new JMenuItem("Transpose...");
        transpose.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                doTranspose();
                }
            });
        menu.add(transpose);

        JMenuItem setVelocity = new JMenuItem("Set Velocity...");
        setVelocity.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                doSetVelocity();
                }
            });
        menu.add(setVelocity);
        JMenuItem randomizeVelocity = new JMenuItem("Randomize Velocity...");
        randomizeVelocity.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                doRandomizeVelocity();
                }
            });
                
        menu.add(randomizeVelocity);
                


        menu.addSeparator();
        

        JMenuItem delete = new JMenuItem("Delete Selection");
        delete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        delete.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                doRemove();
                }
            });
        menu.add(delete);

        JMenuItem filter = new JMenuItem("Filter...");
        filter.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                doFilter();
                }
            });
        menu.add(filter);

        JMenuItem insertNote = new JMenuItem("Add Note");
        insertNote.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        insertNote.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                doInsert(TYPE_NOTE);
                }
            });
        menu.add(insertNote);
        
        JMenuItem insert = new JMenu("Add...");
        menu.add(insert);

        JMenuItem insertBend = new JMenuItem("Bend");
        insertBend.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                doInsert(TYPE_BEND);
                }
            });
        insert.add(insertBend);

        JMenuItem insertAftertouch = new JMenuItem("Aftertouch");
        insertAftertouch.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                doInsert(TYPE_AFTERTOUCH);
                }
            });
        insert.add(insertAftertouch);

        JMenuItem insertCC = new JMenuItem("CC");
        insertCC.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                doInsert(TYPE_CC);
                }
            });
        insert.add(insertCC);

        JMenuItem insertNRPN = new JMenuItem("NRPN");
        insertNRPN.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                doInsert(TYPE_NRPN);
                }
            });
        insert.add(insertNRPN);

        JMenuItem insertRPN = new JMenuItem("RPN");
        insertRPN.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                doInsert(TYPE_RPN);
                }
            });
        insert.add(insertRPN);

        menu.addSeparator();

        autoArmItem = new JCheckBoxMenuItem("Arm New Notes Motifs");
        autoArmItem.setSelected(Prefs.getLastBoolean("ArmNewNotesMotifs", false));
        menu.add(autoArmItem);
        autoArmItem.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                Prefs.setLastBoolean("ArmNewNotesMotifs", autoArmItem.isSelected());
                }
            });

        }
        
    public void uiWasSet()
        {
        super.uiWasSet();
        // revise arm menu
        autoArmItem.setSelected(Prefs.getLastBoolean("ArmNewNotesMotifs", false));
        }
        
    public JMenu getMenu()
        {
        return menu;
        }
        

    /*
      public static final int NEAREST_32_TRIPLETS = 0;
      public static final int NEAREST_32 = 1;
      public static final int NEAREST_16_TRIPLETS = 2;
      public static final int NEAREST_16 = 3;
      public static final int NEAREST_8_TRIPLETS = 4;
      public static final int NEAREST_8 = 5;
      public static final int NEAREST_4_TRIPLETS = 6;
      public static final int NEAREST_4 = 7;
    */
    
    public static final int[] quantizeDivisors = { 
        Seq.PPQ / 12,           // 32 Triplet
        Seq.PPQ / 8,            // 32
        Seq.PPQ / 6,            // 16 Triplet
        Seq.PPQ / 4,            // 16
        Seq.PPQ / 3,            // 8 Triplet
        Seq.PPQ / 2,            // 8
        (Seq.PPQ * 2) / 3,      // 4 Triplet
        Seq.PPQ                         // 4
        };
    
    public void doQuantize()
        {
        JCheckBox range = new JCheckBox("");

        int[] indices = table.getSelectedIndices();
        Notes.Event[] evts = table.getSelectedEvents(indices);
        boolean hasRange = false;
                
        if (indices.length > 0)
            {
            range.setSelected(Prefs.getLastBoolean("QuantizeRange", true));
            hasRange = true;
            }
        else
            {
            range.setSelected(false);
            range.setEnabled(false);
            }

        String[] names = { "Quantize Selection Only", "To Nearest", "Note Ends", "Non-Note Events", "Bias" };
        JComboBox toNearest = new JComboBox(new String[] {"32nd Trip", "32nd Note", "16th Trip", "16th Note", "8th Trip", "8th Note", "Quarter Trip", "Quarter Note"});
        toNearest.setSelectedIndex(Prefs.getLastInt("QuantizeTo", 1));
        JCheckBox noteEnds = new JCheckBox("");
        noteEnds.setSelected(Prefs.getLastBoolean("QuantizeEnds", true));
        JCheckBox nonNoteEvents = new JCheckBox("");
        nonNoteEvents.setSelected(Prefs.getLastBoolean("QuantizeNonNotes", true));
        SmallDial bias = new SmallDial(Prefs.getLastDouble("QuantizeBias", 0.5))
            {
            double value;
            public double getValue() { return value; }
            public void setValue(double val) { value = val; }
            public String map(double val) { return String.format("%.4f", val - 0.5); }
            };

        JComponent[] components = new JComponent[] { range, toNearest, noteEnds, nonNoteEvents, bias.getLabelledDial("-0.0000") };
        int result = Dialogs.showMultiOption(this, names, components, new String[] {  "Quantize", "Cancel" }, 0, "Quantize", "Enter Note Quantization Settings");
        
        if (result == 0)
            {
            seq.push();
            int _toNearest = toNearest.getSelectedIndex();
            boolean _ends = noteEnds.isSelected();
            boolean _nonNotes = nonNoteEvents.isSelected();
            double _bias = bias.getValue();
            boolean _range = range.isSelected();
            int divisor = quantizeDivisors[_toNearest];
        
            ReentrantLock lock = seq.getLock();
            lock.lock();
            boolean stopped;
            try
                {
                if (_range)
                    {
                    notes.quantize(indices, divisor, _ends, _nonNotes, _bias);
                    }
                else
                    {
                    notes.quantize(divisor, _ends, _nonNotes, _bias);
                    }
                }
            finally
                {
                lock.unlock();
                }
        
            Prefs.setLastInt("QuantizeTo", _toNearest);
            Prefs.setLastBoolean("QuantizeEnds", _ends);
            Prefs.setLastBoolean("QuantizeNonNotes", _nonNotes);
            Prefs.setLastDouble("QuantizeBias", _bias);
            if (hasRange) Prefs.setLastBoolean("QuantizeRange", _range);

            table.reload();             // we could change ordering
            table.setSelection(evts);
            }
        }


    public void doRandomizeTime()
        {
        JCheckBox range = new JCheckBox("");

        int[] indices = table.getSelectedIndices();
        Notes.Event[] evts = table.getSelectedEvents(indices);
        boolean hasRange = false;
                
        if (indices.length > 0)
            {
            range.setSelected(Prefs.getLastBoolean("RandomizeTimeRange", true));
            hasRange = true;
            }
        else
            {
            range.setSelected(false);
            range.setEnabled(false);
            }
        String[] names = { "Randomize Selection Only", "Note Lengths", "Non-Note Events", "Noise" };
        JCheckBox noteLengths = new JCheckBox("");
        noteLengths.setSelected(Prefs.getLastBoolean("RandomizeTimeLengths", true));
        JCheckBox nonNoteEvents = new JCheckBox("");
        nonNoteEvents.setSelected(Prefs.getLastBoolean("RandomizeTimeNonNotes", true));
        SmallDial variance = new SmallDial(Prefs.getLastDouble("RandomizeTimeVariance", 0.1))
            {
            double value;
            public double getValue() { return value; }
            public void setValue(double val) { value = val; }
            public String map(double val) { return String.format("%.4f", val); }
            };
        JComponent[] components = new JComponent[] { range, noteLengths, nonNoteEvents, variance.getLabelledDial("0.0000") };
        int result = Dialogs.showMultiOption(this, names, components, new String[] {  "Randomize", "Cancel" }, 0, "Randomize", "Enter Time Randomization Settings");
        
        if (result == 0)
            {
            seq.push();
            boolean _lengths = noteLengths.isSelected();
            boolean _nonNotes = nonNoteEvents.isSelected();
            double _variance = variance.getValue();
            boolean _range = range.isSelected();
        
            ReentrantLock lock = seq.getLock();
            lock.lock();
            boolean stopped;
            try
                {
                if (_range)
                    {
                    notes.randomizeTime(indices, _variance, _lengths, _nonNotes, seq.getDeterministicRandom());
                    }
                else
                    {
                    notes.randomizeTime(_variance, _lengths, _nonNotes, seq.getDeterministicRandom());
                    }
                }
            finally
                {
                lock.unlock();
                }
        
            Prefs.setLastBoolean("RandomizeTimeLengths", _lengths);
            Prefs.setLastBoolean("RandomizeTimeNonNotes", _nonNotes);
            Prefs.setLastDouble("RandomizeTimeVariance", _variance);
            if (hasRange) Prefs.setLastBoolean("RandomizeTimeRange", _range);

            table.reload();                                     // we could change ordering
            table.setSelection(evts);
            }
        }
        
    public void doRandomizeVelocity()
        {
        JCheckBox range = new JCheckBox("");

        int[] indices = table.getSelectedIndices();
        boolean hasRange = false;
                
        if (indices.length > 0)
            {
            range.setSelected(Prefs.getLastBoolean("RandomizeVelocityRange", true));
            hasRange = true;
            }
        else
            {
            range.setSelected(false);
            range.setEnabled(false);
            }
        String[] names = { "Randomize Selection Only", "Release Velocities", "Noise" };
        JCheckBox noteReleases = new JCheckBox("");
        noteReleases.setSelected(Prefs.getLastBoolean("RandomizeVelocityReleases", true));
        SmallDial variance = new SmallDial(Prefs.getLastDouble("RandomizeVelocityVariance", 0.1))
            {
            double value;
            public double getValue() { return value; }
            public void setValue(double val) { value = val; }
            public String map(double val) { return String.format("%.4f", val); }
            };
        JComponent[] components = new JComponent[] { range, noteReleases, variance.getLabelledDial("0.0000") };
        int result = Dialogs.showMultiOption(this, names, components, new String[] {  "Randomize", "Cancel" }, 0, "Randomize", "Enter Velocity Randomization Settings");
        
        if (result == 0)
            {
            seq.push();
            boolean _releases = noteReleases.isSelected();
            double _variance = variance.getValue();
            boolean _range = range.isSelected();
        
            ReentrantLock lock = seq.getLock();
            lock.lock();
            boolean stopped;
            try
                {
                if (_range)
                    {
                    notes.randomizeVelocity(indices, _variance, _releases, seq.getDeterministicRandom());
                    }
                else
                    {
                    notes.randomizeVelocity(_variance, _releases, seq.getDeterministicRandom());
                    }
                }
            finally
                {
                lock.unlock();
                }
        
            Prefs.setLastBoolean("RandomizeVelocityReleases", _releases);
            Prefs.setLastDouble("RandomizeVelocityVariance", _variance);
            if (hasRange) Prefs.setLastBoolean("RandomizeVelocityRange", _range);
            }
        }

    public void doSetVelocity()
        {
        JCheckBox range = new JCheckBox("");

        int[] indices = table.getSelectedIndices();
        boolean hasRange = false;
                
        if (indices.length > 0)
            {
            range.setSelected(Prefs.getLastBoolean("SetVelocityRange", true));
            hasRange = true;
            }
        else
            {
            range.setSelected(false);
            range.setEnabled(false);
            }
        String[] names = { "Set Selection Only", "Velocity" };
        SmallDial velocity = new SmallDial(Prefs.getLastDouble("SetVelocity", 1.0))
            {
            double value;
            public double getValue() { return value; }
            public void setValue(double val) { value = val; }
            public String map(double val) { return "" + (int)((val * 126) + 1); }
            };
        JComponent[] components = new JComponent[] { range, velocity.getLabelledDial("126") };
        int result = Dialogs.showMultiOption(this, names, components, new String[] {  "Set", "Cancel" }, 0, "Set", "Enter Velocity Settings");
        
        if (result == 0)
            {
            seq.push();
            int _velocity = (int)(velocity.getValue() * 126) + 1;
            boolean _range = range.isSelected();
        
            ReentrantLock lock = seq.getLock();
            lock.lock();
            boolean stopped;
            try
                {
                if (_range)
                    {
                    notes.setVelocity(indices, _velocity);
                    }
                else
                    {
                    notes.setVelocity(_velocity);
                    }
                }
            finally
                {
                lock.unlock();
                }
        
            Prefs.setLastDouble("SetVelocity", _velocity);
            if (hasRange) Prefs.setLastBoolean("SetVelocityRange", _range);
            }
        }


    public void doTranspose()
        {
        JCheckBox range = new JCheckBox("");

        int[] indices = table.getSelectedIndices();
        boolean hasRange = false;
                
        if (indices.length > 0)
            {
            range.setSelected(Prefs.getLastBoolean("TransposeRange", true));
            hasRange = true;
            }
        else
            {
            range.setSelected(false);
            range.setEnabled(false);
            }
        String[] names = { "Transpose Selection Only", "By" };
        SmallDial variance = new SmallDial(Prefs.getLastDouble("TransposeBy", 0.5))
            {
            double value;
            public double getValue() { return value; }
            public void setValue(double val) { value = val; }
            public String map(double val) { return "" + (int)Math.round(val * 48 - 24); }
            };
        JComponent[] components = new JComponent[] { range, variance.getLabelledDial("-24") };
        int result = Dialogs.showMultiOption(this, names, components, new String[] {  "Transpose", "Cancel" }, 0, "Transpose", "Enter Transposition Settings");
        
        if (result == 0)
            {
            seq.push();
            double _variance = variance.getValue();
            int by = (int)Math.round(_variance * 48 - 24);
            boolean _range = range.isSelected();
        
            ReentrantLock lock = seq.getLock();
            lock.lock();
            boolean stopped;
            try
                {
                if (_range)
                    {
                    notes.transpose(indices, by);
                    }
                else
                    {
                    notes.transpose(by);
                    }
                }
            finally
                {
                lock.unlock();
                }
        
            Prefs.setLastDouble("TransposeBy", _variance);
            if (hasRange) Prefs.setLastBoolean("TransposeRange", _range);
            }
        }

    public void doFilter()
        {
        JCheckBox range = new JCheckBox("");

        int[] indices = table.getSelectedIndices();
        boolean hasRange = false;
                
        if (indices.length > 0)
            {
            range.setSelected(Prefs.getLastBoolean("FilterRange", true));
            hasRange = true;
            }
        else
            {
            range.setSelected(false);
            range.setEnabled(false);
            }
        String[] names = { "Filter Selection Only", "Remove Notes", "Remove Bend", "Remove CC", "Remove NRPN", "Remove RPN", "Remove Aftertouch" };
        JCheckBox removeNotes = new JCheckBox("");
        JCheckBox removeBend = new JCheckBox("");
        JCheckBox removeCC = new JCheckBox("");
        JCheckBox removeAftertouch = new JCheckBox("");
        JCheckBox removeNRPN = new JCheckBox("");
        JCheckBox removeRPN = new JCheckBox("");

        removeNotes.setSelected(Prefs.getLastBoolean("FilterRemoveNotes", false));
        removeBend.setSelected(Prefs.getLastBoolean("FilterRemoveBend", false));
        removeCC.setSelected(Prefs.getLastBoolean("FilterRemoveCC", false));
        removeAftertouch.setSelected(Prefs.getLastBoolean("FilterRemoveAftertouch", false));
        removeAftertouch.setSelected(Prefs.getLastBoolean("FilterRemoveNRPN", false));
        removeAftertouch.setSelected(Prefs.getLastBoolean("FilterRemoveRPN", false));
        
        JComponent[] components = new JComponent[] { range, removeNotes, removeBend, removeCC, removeNRPN, removeRPN, removeAftertouch};
        int result = Dialogs.showMultiOption(this, names, components, new String[] {  "Filter", "Cancel" }, 0, "Filter", "Enter Filter Settings");
        
        if (result == 0)
            {
            seq.push();
            boolean _range = range.isSelected();
            boolean _removeNotes = removeNotes.isSelected();
            boolean _removeBend = removeBend.isSelected();
            boolean _removeCC = removeCC.isSelected();
            boolean _removeAftertouch = removeAftertouch.isSelected();
            boolean _removeNRPN = removeNRPN.isSelected();
            boolean _removeRPN = removeRPN.isSelected();
        
            ReentrantLock lock = seq.getLock();
            lock.lock();
            boolean stopped;
            try
                {
                if (_range)
                    {
                    notes.filter(indices, _removeNotes, _removeBend, _removeCC, _removeNRPN, _removeRPN, _removeAftertouch);
                    }
                else
                    {
                    notes.filter(_removeNotes, _removeBend, _removeCC, _removeNRPN, _removeRPN, _removeAftertouch);
                    }
                }
            finally
                {
                lock.unlock();
                }
        
            Prefs.setLastBoolean("FilterRemoveNotes", _removeNotes);
            Prefs.setLastBoolean("FilterRemoveBend", _removeBend);
            Prefs.setLastBoolean("FilterRemoveCC", _removeCC);
            Prefs.setLastBoolean("FilterRemoveAftertouch", _removeAftertouch);
            Prefs.setLastBoolean("FilterRemoveNRPN", _removeNRPN);
            Prefs.setLastBoolean("FilterRemoveRPN", _removeRPN);
            if (hasRange) Prefs.setLastBoolean("FilterRange", _range);

            // we could change everything
            table.reload();
            }
        }


    public void doTrimTime()
        {
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            notes.trim();
            }
        finally
            {
            lock.unlock();
            }
        }

    static final int[] TIME_UNITS = { 1, 192 / 16, 192 / 4, 192, 192 * 4 };
    static final String[] TIME_UNIT_NAMES = { "Ticks", "64th-Notes", "16th-Notes", "Quarter Notes", "Measures" };
    public void doShiftTime()
        {
        JCheckBox range = new JCheckBox("");

        int[] indices = table.getSelectedIndices();
        Notes.Event[] evts = table.getSelectedEvents(indices);
        boolean hasRange = false;
                
        if (indices.length > 0)
            {
            range.setSelected(Prefs.getLastBoolean("ShiftTimeRange", true));
            hasRange = true;
            }
        else
            {
            range.setSelected(false);
            range.setEnabled(false);
            }
                
        JComboBox byUnit = new JComboBox(new String[] { "Ticks", "64th Notes", "16th Notes", "Quarter Notes", "Measures"});
        byUnit.setSelectedIndex(Prefs.getLastInt("ShiftTimeUnits", 4)); // Measures is default

        String[] names = { "Shift Selection Only", "By", "... In"};
        SmallDial variance = new SmallDial(Prefs.getLastDouble("ShiftTimeBy", 0.5))
            {
            double value;
            public double getValue() { return value; }
            public void setValue(double val) { value = val; }
            public String map(double val) { return "" + (int)Math.round(val * 32 - 16); }
            };
        JComponent[] components = new JComponent[] { range, variance.getLabelledDial("-16"), byUnit };
        int result = Dialogs.showMultiOption(this, names, components, new String[] {  "Shift", "Cancel" }, 0, "Shift Time", "Enter Shift Settings");
        
        if (result == 0)
            {
            seq.push();
            double _variance = variance.getValue();
            int _units = byUnit.getSelectedIndex();
            int by = (int)Math.round(_variance * 32 - 16) * TIME_UNITS[_units];
            boolean _range = range.isSelected();
            boolean shiftResult = false;
        
            ReentrantLock lock = seq.getLock();
            lock.lock();
            try
                {
                if (_range)
                    {
                    shiftResult = notes.shift(indices, by);
                    }
                else
                    {
                    shiftResult = notes.shift(by);
                    }
                }
            finally
                {
                lock.unlock();
                }
        
            if (!shiftResult)
                {
                sequi.showSimpleError("Cannot Shift", "Shifting back by " + (0 - by) + " " + TIME_UNIT_NAMES[_units] + " would be before Timestep 0.");
                }
                                
            Prefs.setLastDouble("ShiftTimeBy", _variance);
            Prefs.setLastInt("ShiftTimeUnits", _units);
            if (hasRange) Prefs.setLastBoolean("ShiftTimeRange", _range);
            
            table.reload();                                     // we could change ordering
            table.setSelection(evts);
            }
        }


    public void doStretchTime()
        {
        JCheckBox range = new JCheckBox("");

        int[] indices = table.getSelectedIndices();
        Notes.Event[] evts = table.getSelectedEvents(indices);
        boolean hasRange = false;
                
        if (indices.length > 0)
            {
            range.setSelected(Prefs.getLastBoolean("StretchTimeRange", true));
            hasRange = true;
            }
        else
            {
            range.setSelected(false);
            range.setEnabled(false);
            }
                
        String[] names = { "Stretch Selection Only", "From", "To"};
        SmallDial from = new SmallDial(Prefs.getLastDouble("StretchTimeFrom", 0.5))
            {
            double value;
            public double getValue() { return value; }
            public void setValue(double val) { value = val; }
            public String map(double val) { return "" + (int)(val * 31 + 1); }
            };

        SmallDial to = new SmallDial(Prefs.getLastDouble("StretchTimeTo", 0.5))
            {
            double value;
            public double getValue() { return value; }
            public void setValue(double val) { value = val; }
            public String map(double val) { return "" + (int)(val * 31 + 1); }
            };


        JComponent[] components = new JComponent[] { range, from.getLabelledDial("32"), to.getLabelledDial("32"), };
        int result = Dialogs.showMultiOption(this, names, components, new String[] {  "Stretch", "Cancel" }, 0, "Stretch Time", "Enter Stretch Settings");
        
        if (result == 0)
            {
            seq.push();
            int _from = (int)(from.getValue() * 31 + 1);
            int _to = (int)(to.getValue() * 31 + 1);
            boolean _range = range.isSelected();
        
            seq.push();
            ReentrantLock lock = seq.getLock();
            lock.lock();
            try
                {
                if (_range)
                    {
                    notes.stretch(indices, _from, _to);
                    }
                else
                    {
                    notes.stretch(_from, _to);
                    }
                }
            finally
                {
                lock.unlock();
                }
        
            Prefs.setLastDouble("StretchTimeFrom", from.getValue());
            Prefs.setLastDouble("StretchTimeTo", to.getValue());
            if (hasRange) Prefs.setLastBoolean("StretchTimeRange", _range);
            
            table.reload();                                     // we could change time
            table.setSelection(evts);
            }
        }
        
    public static final int TYPE_NOTE = 0;
    public static final int TYPE_AFTERTOUCH = 1;
    public static final int TYPE_CC = 2;
    public static final int TYPE_NRPN = 3;
    public static final int TYPE_RPN = 4;
    public static final int TYPE_BEND = 5;
        
    public void doInsert(int type)
        {
        seq.push();
        int where = 0;
        int when = 0;
        Notes.Note insertedNote = null;
        int insertedPitch = 0;
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            // Insert after the selection point if possible
            int[] indices = table.getSelectedIndices();
            if (indices.length > 0)
                {
                where = indices[0] + 1;
                when = notes.getEvents().get(indices[0]).when;
                }
            
            switch(type)
                {
                case TYPE_NOTE:
                    notes.getEvents().add(where, insertedNote = new Notes.Note(60, 127, when, 192, 0x40));                // guarantee it's first. It's at time 0 too so we don't need to sort.
                    notes.computeMaxTime();
                    insertedPitch = insertedNote.pitch;
                    break;
                case TYPE_AFTERTOUCH:
                    notes.getEvents().add(where, new Notes.Aftertouch(Out.CHANNEL_AFTERTOUCH, when));                // guarantee it's first. It's at time 0 too so we don't need to sort.
                    break;
                case TYPE_CC:
                    notes.getEvents().add(where, new Notes.CC(0, 0, when));                // guarantee it's first. It's at time 0 too so we don't need to sort.
                    break;
                case TYPE_NRPN:
                    notes.getEvents().add(where, new Notes.NRPN(0, 0, when));                // guarantee it's first. It's at time 0 too so we don't need to sort.
                    break;
                case TYPE_RPN:
                    notes.getEvents().add(where, new Notes.RPN(0, 0, when));                // guarantee it's first. It's at time 0 too so we don't need to sort.
                    break;
                case TYPE_BEND:
                    notes.getEvents().add(where, new Notes.Bend(0, when));                // guarantee it's first. It's at time 0 too so we don't need to sort.
                    break;
                }
            
            // Select
            }
        finally
            {
            lock.unlock();
            }
            
        table.reload();                                 // we will change the table
        table.setSelection(where);

          gridui.rebuild();
          if (insertedNote != null)
          {
          gridui.removeAllSelected();
          gridui.addSelected(getNoteUIFor(insertedNote, insertedPitch));
          }
        }

    public void doRemove()
        {
        int[] indices = table.getSelectedIndices();
                
        if (indices.length > 0)
            {
            seq.push();
            ReentrantLock lock = seq.getLock();
            lock.lock();
            try
                {
                notes.remove(indices);         // do nothing with the result
                }
            finally
                {
                lock.unlock();
                }
            }
            
        table.reload();                         // we will change events
        }


    public void doCopy()
        {
        int[] indices = table.getSelectedIndices();
        int[] newIndices = new int[indices.length];
        
        if (indices.length > 0)
            {
            seq.push();
            ReentrantLock lock = seq.getLock();
            lock.lock();
            try
                {
                ArrayList<Notes.Event> events = notes.getEvents();
                ArrayList<Notes.Event> copy = notes.copyEvents(indices);
                // Add in reverse order so we can insert them properly and retain the indices.
                // This will be extremely costly, O(n^2)
                for(int i = indices.length - 1; i >= 0; i--)
                    {
                    Notes.Event evt = copy.get(i);
                    notes.getEvents().add(indices[i] + 1, evt);
                    }
                // find the events, O(n^2)
                int idx = 0;
                for(Notes.Event evt : copy)
                    {
                    newIndices[idx++] = notes.getEvents().indexOf(evt);
                    }
                }
            finally
                {
                lock.unlock();
                }
            }
            
        table.reload();                         // we will change events
        table.setSelection(newIndices);
        }


    public void loadEvents()
        {
        ArrayList<Notes.Event> events = new ArrayList<Notes.Event>(notes.getEvents());          // copy?
        table.setEvents(events);
        }
        
    void loadMIDIFile()
        {
        ReentrantLock lock = seq.getLock();
        lock.lock();
        boolean stopped;
        try
            {
            stopped = seq.isStopped();
            }
        finally
            {
            lock.unlock();
            }
        
        if (!stopped)
            {
            // Maybe this is not necessary?
            if (sequi.showSimpleConfirm("Stop Sequence", "To Load MIDI, we must stop playing the sequence", "Stop"))
                {
                lock.lock();
                try
                    {
                    seq.stop();
                    }
                finally
                    {
                    lock.unlock();
                    }
                }
            else return;
            }

        FileDialog fd = new FileDialog((JFrame)sequi.getFrame(), "Load MIDI File...", FileDialog.LOAD);
        fd.setFilenameFilter(new FilenameFilter()
            {
            public boolean accept(File dir, String name)
                {
                return SeqUI.ensureFileEndsWith(name, Notes.MIDI_FILE_EXTENSION).equals(name);
                }
            });

        sequi.disableMenuBar();
        fd.setVisible(true);
        sequi.enableMenuBar();
                
        if (fd.getFile() != null)
            {
            FileInputStream stream = null;
            ArrayList<Notes.Event> events = null;
            try
                {
                seq.push();
                lock.lock();
                try
                    {
                    notes.read(stream = new FileInputStream(fd.getDirectory()+fd.getFile()));
                    events = new ArrayList<Notes.Event>(notes.getEvents());         // copy?
                    }
                finally
                    {
                    lock.unlock();
                    }
                if (events != null) 
                    {
                    table.setEvents(events);
                    }
                }
            catch (Exception ex)
                {
                ex.printStackTrace();
                sequi.showSimpleError("Error Loading MIDI File", "An error occurred loading the MIDI File " + fd.getFile());
                }
            finally
                {
                try { if (stream != null) stream.close(); } catch (Exception ex) { }
                }
            }
        }
    
    public void buildInspectors(JScrollPane scroll)
        {
        // Build the notes child inspector holder
        childOuter = new JPanel();
        childOuter.setLayout(new BorderLayout());
        childBorder = BorderFactory.createTitledBorder(null, "Child");
        childOuter.setBorder(childBorder);
                
        // Build the notes inspector holder
        notesOuter = new JPanel();
        notesOuter.setLayout(new BorderLayout());
        notesBorder = BorderFactory.createTitledBorder(null, "Notes");
        notesOuter.setBorder(notesBorder);

        // Add the notes inspector
        notesInspector = new NotesInspector(seq, notes, this);
        notesOuter.add(notesInspector, BorderLayout.CENTER);
                
        // Fill the panes
        inspectorPane = new JPanel();
        inspectorPane.setLayout(new BorderLayout());
        inspectorPane.add(notesOuter, BorderLayout.NORTH);
        inspectorPane.add(childOuter, BorderLayout.CENTER);
                
        scroll.setViewportView(inspectorPane);
        }
    
    public EventTable getTable()
        {
        return table;
        }
        
    public void buildPrimary(JScrollPane scroll)
        {
        table = new EventTable(notes, seq);
        
        table.addListSelectionListener(new javax.swing.event.ListSelectionListener()
            {
            public void valueChanged(javax.swing.event.ListSelectionEvent e)
                {
                // It's a single item selected
                if (table.getSelectedRow() >= 0 && table.getSelectedRowCount() == 1)
                    {
                    setChildInspector(new EventInspector(seq, notes, NotesUI.this, table.getSelectedRow()));
                    }
                else
                    {
                    setChildInspector(null);
                    }
                }
            });
        loadEvents();
        
          gridui = new GridUI(this);
          scroll.setViewportView(gridui);
          scroll.setRowHeaderView(gridui.getKeyboard());
          
          // FIXME
          // This isn't working with two-finger trackpad scrolling.  :-(
          /*
          scroll.setVerticalScrollBar(new JScrollBar()
          {
          public void setValue(int value)
          {
          super.setValue((value / 16) * 16);
          }
          });
          */
                                        
        //tabs = new JTabbedPane();
        //tabs.addTab("Roll", new JScrollPane(gridui));
        //tabs.addTab("Notes", new JScrollPane(table.getTable())); 
        
        //scroll.setViewportView(table.getTable());
        }
                
    public JPanel buildConsole()
        {
        PushButton removeButton = new PushButton(new StretchIcon(PushButton.class.getResource("icons/minus.png")))
            {
            public void perform()
                {
                doRemove();
                }
            };
        removeButton.getButton().setPreferredSize(new Dimension(24, 24));
        removeButton.setToolTipText(REMOVE_BUTTON_TOOLTIP);

        PushButton copyButton = new PushButton(new StretchIcon(PushButton.class.getResource("icons/copy.png")))
            {
            public void perform()
                {
                doCopy();
                }
            };
        copyButton.getButton().setPreferredSize(new Dimension(24, 24));
        copyButton.setToolTipText(COPY_BUTTON_TOOLTIP);

        Image addNoteIcon = getStaticIcon().getImage().getScaledInstance(16, 16,  java.awt.Image.SCALE_SMOOTH); 
        PushButton addNoteButton = new PushButton("")
            {
            public void perform()
                {
                doInsert(TYPE_NOTE);
                }
            };
        addNoteButton.getButton().setIcon(new ImageIcon(addNoteIcon));

        addNoteButton.getButton().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        addNoteButton.getButton().setPreferredSize(new Dimension(24, 24));
        addNoteButton.setToolTipText(ADD_NOTE_BUTTON_TOOLTIP);

        PushButton addBendButton = new PushButton("B")
            {
            public void perform()
                {
                doInsert(TYPE_BEND);
                }
            };
        addBendButton.getButton().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        addBendButton.getButton().setPreferredSize(new Dimension(24, 24));
        addBendButton.setToolTipText(ADD_BEND_BUTTON_TOOLTIP);
        JPanel console = new JPanel();
        console.setLayout(new BorderLayout());
                
        PushButton addAftertouchButton = new PushButton("A")
            {
            public void perform()
                {
                doInsert(TYPE_AFTERTOUCH);
                }
            };
        addAftertouchButton.getButton().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        addAftertouchButton.getButton().setPreferredSize(new Dimension(24, 24));
        addAftertouchButton.setToolTipText(ADD_AFTERTOUCH_BUTTON_TOOLTIP);
        console = new JPanel();
        console.setLayout(new BorderLayout());
                
        PushButton addCCButton = new PushButton("C")
            {
            public void perform()
                {
                doInsert(TYPE_CC);
                }
            };
        addCCButton.getButton().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        addCCButton.getButton().setPreferredSize(new Dimension(24, 24));
        addCCButton.setToolTipText(ADD_CC_BUTTON_TOOLTIP);
        console = new JPanel();
        console.setLayout(new BorderLayout());
                
        PushButton addNRPNButton = new PushButton("N")
            {
            public void perform()
                {
                doInsert(TYPE_NRPN);
                }
            };
        addNRPNButton.getButton().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        addNRPNButton.getButton().setPreferredSize(new Dimension(24, 24));
        addNRPNButton.setToolTipText(ADD_NRPN_BUTTON_TOOLTIP);
        console = new JPanel();
        console.setLayout(new BorderLayout());
                
        PushButton addRPNButton = new PushButton("R")
            {
            public void perform()
                {
                doInsert(TYPE_RPN);
                }
            };
        addRPNButton.getButton().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        addRPNButton.getButton().setPreferredSize(new Dimension(24, 24));
        addRPNButton.setToolTipText(ADD_RPN_BUTTON_TOOLTIP);
        console = new JPanel();
        console.setLayout(new BorderLayout());
                
        Box addRemoveBox = new Box(BoxLayout.X_AXIS);
        addRemoveBox.add(removeButton);
        addRemoveBox.add(copyButton);
        console.add(addRemoveBox, BorderLayout.WEST);   
        
        Box otherBox = new Box(BoxLayout.X_AXIS);
        otherBox.add(addNoteButton);
        otherBox.add(addBendButton);
        otherBox.add(addAftertouchButton);
        otherBox.add(addCCButton);
        otherBox.add(addNRPNButton);
        otherBox.add(addRPNButton);
        console.add(otherBox, BorderLayout.EAST);
                
        return console; 
        }




    public void setChildInspector(EventInspector inspector)
        {
        childInspector = inspector;
        childOuter.removeAll();
        if (inspector != null) 
            {
            childOuter.add(inspector, BorderLayout.NORTH);
            childBorder.setTitle("Event " + (inspector.getIndex() + 1));
            }
        else
            {
            childBorder.setTitle(null);
            }

        childOuter.setBorder(null);             // this has to be done or it won't immediately redraw!
        childOuter.setBorder(childBorder);
        if (inspector!=null) inspector.revise();
        revalidate();
        }

    public void revise()
        {
        if (childInspector != null) 
            {
            childBorder.setTitle("Event " + (childInspector.getIndex() + 1));
            childOuter.setBorder(null);             // this has to be done or it won't immediately redraw!
            childOuter.setBorder(childBorder);
            childInspector.revise();
            }
        }
        
    public void redraw(boolean inResponseToStep) 
        {
        boolean stopped;
        boolean recorded;
        int index;
        NotesClip clip = (NotesClip)getDisplayClip();
        if (clip == null)
            {
//            table.clearSelection();
            }
        else
            {
            ReentrantLock lock = seq.getLock();
            lock.lock();
            try
                {
                recorded = clip.getDidRecord();
                clip.setDidRecord(false);
                stopped = seq.isStopped();
                index = clip.getIndex();
                if (clip.finishedPlaying()) index = -1;
                }
            finally
                {
                lock.unlock();
                }
                                
            if (recorded) 
                {
                // reload table
                loadEvents();
                }
                                
            if (stopped) 
                {
                table.setIndex(-1);
                }
            else 
                {
                table.setIndex(index);
                }
            }
        }

    public void stopped()
        {
        if (isUIBuilt())
            {
            table.setIndex(-1);
            }
        }
                          
    static final String REMOVE_BUTTON_TOOLTIP = "<html><b>Remove Event</b><br>" +
        "Removes the selected event or events from the Notes.</html>";
        
    static final String COPY_BUTTON_TOOLTIP = "<html><b>Copy Event</b><br>" +
        "Duplicates the selected event or events from in the Notes.</html>";
        
    static final String ADD_NOTE_BUTTON_TOOLTIP = "<html><b>Add Note</b><br>" +
        "Adds a note to the Notes.</html>";
        
    static final String ADD_BEND_BUTTON_TOOLTIP = "<html><b>Add Pitch Bend</b><br>" +
        "Adds a Pitch Bend event to the Notes.</html>";
        
    static final String ADD_AFTERTOUCH_BUTTON_TOOLTIP = "<html><b>Add Aftertouch</b><br>" +
        "Adds an Aftertouch (or Pressure) event to the Notes.</html>";
        
    static final String ADD_CC_BUTTON_TOOLTIP = "<html><b>Add CC</b><br>" +
        "Adds a CC (Control Change) event to the Notes.</html>";
        
    static final String ADD_NRPN_BUTTON_TOOLTIP = "<html><b>Add NRPN</b><br>" +
        "Adds an NRPN (Non-Registered Program Number) event to the Notes.</html>";
        
    static final String ADD_RPN_BUTTON_TOOLTIP = "<html><b>Add RPN</b><br>" +
        "Adds an RPN (Registered Program Number) event to the Notes.</html>";
        
        

    }
