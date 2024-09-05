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
    
    EventInspector childInspector;    
    JPanel childOuter;
    TitledBorder childBorder;
    JPanel inspectorPane;
        
    JPanel notesOuter;
    TitledBorder notesBorder;
    NotesInspector notesInspector;
    JMenu menu;
        
    EventTable table;
                
    public static ImageIcon getStaticIcon() { return new ImageIcon(MotifUI.class.getResource("icons/notes.png")); }        // don't ask
    public ImageIcon getIcon() { return getStaticIcon(); }
    public static String getType() { return "Notes"; }
    public static MotifUI create(Seq seq, SeqUI ui)
        {
        return new NotesUI(seq, ui, new Notes(seq));
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
                doDelete();
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

        JMenuItem insert = new JMenuItem("Add Note");
        insert.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        insert.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                doInsert();
                }
            });
        menu.add(insert);
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

        int min = table.getTable().getSelectionModel().getMinSelectionIndex();
        int max = table.getTable().getSelectionModel().getMaxSelectionIndex();
        boolean hasRange = false;
                
        if (min >= 0)
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
                    notes.quantize(min, max, divisor, _ends, _nonNotes, _bias);
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

            reloadTable();
            }
        }


    public void doRandomizeTime()
        {
        JCheckBox range = new JCheckBox("");

        int min = table.getTable().getSelectionModel().getMinSelectionIndex();
        int max = table.getTable().getSelectionModel().getMaxSelectionIndex();
        boolean hasRange = false;
                
        if (min >= 0)
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
                    notes.randomizeTime(min, max, _variance, _lengths, _nonNotes, seq.getDeterministicRandom());
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

            reloadTable();
            }
        }
        
    public void doRandomizeVelocity()
        {
        JCheckBox range = new JCheckBox("");

        int min = table.getTable().getSelectionModel().getMinSelectionIndex();
        int max = table.getTable().getSelectionModel().getMaxSelectionIndex();
        boolean hasRange = false;
                
        if (min >= 0)
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
                    notes.randomizeVelocity(min, max, _variance, _releases, seq.getDeterministicRandom());
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

            reloadTable();
            }
        }

    public void doSetVelocity()
        {
        JCheckBox range = new JCheckBox("");

        int min = table.getTable().getSelectionModel().getMinSelectionIndex();
        int max = table.getTable().getSelectionModel().getMaxSelectionIndex();
        boolean hasRange = false;
                
        if (min >= 0)
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
                    notes.setVelocity(min, max, _velocity);
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

            reloadTable();
            }
        }


    public void doTranspose()
        {
        JCheckBox range = new JCheckBox("");

        int min = table.getTable().getSelectionModel().getMinSelectionIndex();
        int max = table.getTable().getSelectionModel().getMaxSelectionIndex();
        boolean hasRange = false;
                
        if (min >= 0)
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
                    notes.transpose(min, max, by);
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

            reloadTable();
            }
        }

    public void doFilter()
        {
        JCheckBox range = new JCheckBox("");

        int min = table.getTable().getSelectionModel().getMinSelectionIndex();
        int max = table.getTable().getSelectionModel().getMaxSelectionIndex();
        boolean hasRange = false;
                
        if (min >= 0)
            {
            range.setSelected(Prefs.getLastBoolean("FilterRange", true));
            hasRange = true;
            }
        else
            {
            range.setSelected(false);
            range.setEnabled(false);
            }
        String[] names = { "Filter Selection Only", "Remove Notes", "Remove Bend", "Remove CC", "Remove Aftertouch" };
        JCheckBox removeNotes = new JCheckBox("");
        JCheckBox removeBend = new JCheckBox("");
        JCheckBox removeCC = new JCheckBox("");
        JCheckBox removeAftertouch = new JCheckBox("");

        removeNotes.setSelected(Prefs.getLastBoolean("FilterRemoveNotes", false));
        removeBend.setSelected(Prefs.getLastBoolean("FilterRemoveBend", false));
        removeCC.setSelected(Prefs.getLastBoolean("FilterRemoveCC", false));
        removeAftertouch.setSelected(Prefs.getLastBoolean("FilterRemoveAftertouch", false));
        
        JComponent[] components = new JComponent[] { range, removeNotes, removeBend, removeCC, removeAftertouch};
        int result = Dialogs.showMultiOption(this, names, components, new String[] {  "Filter", "Cancel" }, 0, "Filter", "Enter Filter Settings");
        
        if (result == 0)
            {
            seq.push();
            boolean _range = range.isSelected();
            boolean _removeNotes = removeNotes.isSelected();
            boolean _removeBend = removeBend.isSelected();
            boolean _removeCC = removeCC.isSelected();
            boolean _removeAftertouch = removeAftertouch.isSelected();
        
            ReentrantLock lock = seq.getLock();
            lock.lock();
            boolean stopped;
            try
                {
                if (_range)
                    {
                    notes.filter(min, max, _removeNotes, _removeBend, _removeCC, _removeAftertouch);
                    }
                else
                    {
                    notes.filter(_removeNotes, _removeBend, _removeCC, _removeAftertouch);
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
            if (hasRange) Prefs.setLastBoolean("FilterRange", _range);

            reloadTable();
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
        reloadTable();          
        }

    static final int[] TIME_UNITS = { 1, 192 / 16, 192 / 4, 192, 192 * 4 };
    static final String[] TIME_UNIT_NAMES = { "Ticks", "64th-Notes", "16th-Notes", "Quarter Notes", "Measures" };
    public void doShiftTime()
        {
        JCheckBox range = new JCheckBox("");

        int min = table.getTable().getSelectionModel().getMinSelectionIndex();
        int max = table.getTable().getSelectionModel().getMaxSelectionIndex();
        boolean hasRange = false;
                
        if (min >= 0)
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
                    shiftResult = notes.shift(min, max, by);
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
            reloadTable();
            }
        }


    public void doStretchTime()
        {
        JCheckBox range = new JCheckBox("");

        int min = table.getTable().getSelectionModel().getMinSelectionIndex();
        int max = table.getTable().getSelectionModel().getMaxSelectionIndex();
        boolean hasRange = false;
                
        if (min >= 0)
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
                    notes.stretch(min, max, _from, _to);
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
            reloadTable();
            }
        }
    public void doInsert()
        {
        seq.push();
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            notes.getEvents().add(0, new Notes.Note(60, 127, 0, 192, 0x40));                // guarantee it's first. It's at time 0 too so we don't need to sort.
            }
        finally
            {
            lock.unlock();
            }
        reloadTable();
        table.setSelection(0);
        }

    public void doDelete()
        {
        int min = table.getTable().getSelectionModel().getMinSelectionIndex();
        int max = table.getTable().getSelectionModel().getMaxSelectionIndex();
                
        if (min >= 0)
            {
            seq.push();
            ReentrantLock lock = seq.getLock();
            lock.lock();
            try
                {
                notes.remove(min, max);         // do nothing with the result
                }
            finally
                {
                lock.unlock();
                }
            }
        reloadTable();
        }


    public void loadEvents()
        {
        ArrayList<Notes.Event> events = new ArrayList<Notes.Event>(notes.getEvents());          // copy?
        table.setEvents(events);
        }
        
    public void loadMIDIFile()
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
                    table.clearSelection();
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
        scroll.setViewportView(table.getTable());
        }
                
    public JPanel buildConsole()
        {
        return new JPanel();
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
        
    public void redraw() 
        {
        boolean stopped;
        boolean recorded;
        int index;
        NotesClip clip = (NotesClip)getDisplayClip();
        if (clip == null)
            {
            table.clearSelection();
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
            reloadTable();
            }
        }
                          
    public void reloadTable() 
        { 
        ArrayList<Notes.Event> events = null;
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            events = new ArrayList<Notes.Event>(notes.getEvents());         // copy?
            }
        finally
            {
            lock.unlock();
            }
        if (events != null) 
            {
            table.setEvents(events);
            table.clearSelection();
            }
        }

    }
