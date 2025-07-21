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
    public static final int DEFAULT_NOTE_VELOCITY = 64;
    public static final int DEFAULT_NOTE_LENGTH = 192;
    public static final String[] SNAP_OPTIONS = { "No Snap", "Snap to 64th", "Snap to 16th", "Snap to Triplet", "Snap to Beat", "Snap by 64th", "Snap by 16th", "Snap by Triplet", "Snap by Beat" };
    public static final int[] SNAP_QUANTIZATIONS = { 192 / 192, 192 / 16, 192 / 4, 192 / 3, 192, -192 / 16, -192 / 4, -192 / 3, -192 };
    public static final int SNAP_DEFAULT_OPTION = 4;
    public static final String[] MAX_OPTIONS = { "64 Bars", "256 Bars", "1024 Bars", "4096 Bars", "16384 Bars", "65536 Bars" };
    public static final int[] MAX_MEASURES = { 192 * 4 * 64, 192 * 4 * 256, 192 * 4 * 1024, 192 * 4 * 4096, 192 * 4 * 16384, 192 * 4 * 65536 };
    public static final int MAX_DEFAULT_OPTION = 1;
    
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
    Ruler ruler;
    //EventTable table;
    GridUI gridui;
    EventsUI eventsui;
    JScrollPane scroll;
    //JTabbedPane tabs;
    
    JComboBox snapBox;
    JComboBox maxBox;
    
    public Notes getNotes() { return notes; }
    public EventsUI getEventsUI() { return eventsui; }
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

    public EventUI getEventUIFor(Notes.Event event, int type)
        {
        return eventsui.getEventUIFor(event, type);
        } 
     
    public Ruler getRuler() { return ruler; }
     
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

/*
  JMenuItem shiftTime = new JMenuItem("Shift Time...");
  shiftTime.addActionListener(new ActionListener()
  {
  public void actionPerformed(ActionEvent event)
  {
  doShiftTime();
  }
  });
  menu.add(shiftTime);
*/
  
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
        
        /*
          JMenuItem transpose = new JMenuItem("Transpose...");
          transpose.addActionListener(new ActionListener()
          {
          public void actionPerformed(ActionEvent event)
          {
          doTranspose();
          }
          });
          menu.add(transpose);
        */

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
/*
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
*/

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
        
    public void reload()
        {
        reload(notes.getEvents());
        
        // at this point nothing has repainted yet, so we do it in bulk
        gridui.repaint();
        ruler.repaint();
        eventsui.repaint();
        }
    
    public void reload(ArrayList<Notes.Event> events)
        {
        HashSet<Notes.Event> hash = new HashSet(events);
        gridui.reload(hash);
        eventsui.reload(hash);
        updateChildInspector(true);
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
        ArrayList<Notes.Event> events = gridui.getSelectedOrRangeEvents();
        boolean hasRange = false;
        boolean ruler = getRuler().getHasRange();
                
        if (events.size() > 0)
            {
            hasRange = true;
            }

        String[] names = { "To Nearest", "Note Ends", "Non-Note Events", "Bias" };
        JComboBox toNearest = new JComboBox(new String[] {"32nd Trip", "32nd Note", "16th Trip", "16th Note", "8th Trip", "8th Note", "Quarter Trip", "Quarter Note"});
        toNearest.setSelectedIndex(Prefs.getLastInt("QuantizeTo", 1));
        JCheckBox noteEnds = new JCheckBox("");
        if (gridui.getSelectedSource() != GridUI.SELECTED_SOURCE_NOTES &&
            gridui.getSelectedSource() != GridUI.SELECTED_SOURCE_NONE)
            {
            noteEnds.setEnabled(false);
            }
        noteEnds.setSelected(Prefs.getLastBoolean("QuantizeEnds", true));
        JCheckBox nonNoteEvents = new JCheckBox("");
        if (hasRange && !ruler) // They're selected
            {
            nonNoteEvents.setEnabled(false);
            }
        nonNoteEvents.setSelected(Prefs.getLastBoolean("QuantizeNonNotes", true));
        SmallDial bias = new SmallDial(Prefs.getLastDouble("QuantizeBias", 0.5))
            {
            double value;
            public double getValue() { return value; }
            public void setValue(double val) { value = val; }
            public String map(double val) { return String.format("%.4f", val - 0.5); }
            };

        JComponent[] components = new JComponent[] { toNearest, noteEnds, nonNoteEvents, bias.getLabelledDial("-0.0000") };
        int result = Dialogs.showMultiOption(sequi, names, components, new String[] {  "Quantize", "Cancel" }, 0, "Quantize", "Enter Quantization Settings");
        
        if (result == 0)
            {
            seq.push();
            int _toNearest = toNearest.getSelectedIndex();
            boolean _ends = noteEnds.isSelected();
            boolean _nonNotes = nonNoteEvents.isSelected();
            double _bias = bias.getValue();
            int divisor = quantizeDivisors[_toNearest];
        
            ReentrantLock lock = seq.getLock();
            lock.lock();
            boolean stopped;
            try
                {
                notes.quantize(events, divisor, _ends, _nonNotes, _bias);
                }
            finally
                {
                lock.unlock();
                }
        
            Prefs.setLastInt("QuantizeTo", _toNearest);
            Prefs.setLastBoolean("QuantizeEnds", _ends);
            Prefs.setLastBoolean("QuantizeNonNotes", _nonNotes);
            Prefs.setLastDouble("QuantizeBias", _bias);
            
            rebuild();
                        
            //       table.reload();             // we could change ordering
            //       table.setSelection(evts);
            }
        }


    public void doRandomizeTime()
        {
        ArrayList<Notes.Event> events = gridui.getSelectedOrRangeEvents();
        boolean hasRange = false;
        boolean ruler = getRuler().getHasRange();

        if (events.size() > 0)
            {
            hasRange = true;
            }

        String[] names = { "Note Lengths", "Non-Note Events", "Noise" };
        JCheckBox noteLengths = new JCheckBox("");
        if (gridui.getSelectedSource() != GridUI.SELECTED_SOURCE_NOTES &&
            gridui.getSelectedSource() != GridUI.SELECTED_SOURCE_NONE)
            {
            noteLengths.setEnabled(false);
            }
        noteLengths.setSelected(Prefs.getLastBoolean("RandomizeTimeLengths", true));
        JCheckBox nonNoteEvents = new JCheckBox("");
        if (hasRange && !ruler) // They're selected
            {
            nonNoteEvents.setEnabled(false);
            }
        nonNoteEvents.setSelected(Prefs.getLastBoolean("RandomizeTimeNonNotes", true));
        SmallDial variance = new SmallDial(Prefs.getLastDouble("RandomizeTimeVariance", 0.1))
            {
            double value;
            public double getValue() { return value; }
            public void setValue(double val) { value = val; }
            public String map(double val) { return String.format("%.4f", val); }
            };
        JComponent[] components = new JComponent[] { noteLengths, nonNoteEvents, variance.getLabelledDial("0.0000") };
        int result = Dialogs.showMultiOption(sequi, names, components, new String[] {  "Randomize", "Cancel" }, 0, "Randomize", "Enter Time Randomization Settings");
        
        if (result == 0)
            {
            seq.push();
            boolean _lengths = noteLengths.isSelected();
            boolean _nonNotes = nonNoteEvents.isSelected();
            double _variance = variance.getValue();
        
            ReentrantLock lock = seq.getLock();
            lock.lock();
            boolean stopped;
            try
                {
                notes.randomizeTime(events, _variance, _lengths, _nonNotes, seq.getDeterministicRandom());
                }
            finally
                {
                lock.unlock();
                }
        
            Prefs.setLastBoolean("RandomizeTimeLengths", _lengths);
            Prefs.setLastBoolean("RandomizeTimeNonNotes", _nonNotes);
            Prefs.setLastDouble("RandomizeTimeVariance", _variance);

            rebuild();
                        
            //       table.reload();             // we could change ordering
            //       table.setSelection(evts);
            }
        }
        
    public void doRandomizeVelocity()
        {
        ArrayList<Notes.Event> events = gridui.getSelectedOrRangeEvents();
        boolean ruler = getRuler().getHasRange();
        boolean hasRange = false;
                
        if (events.size() > 0)
            {
            hasRange = true;
            }

        String[] names = { "Release Velocities", "Noise" };
        JCheckBox noteReleases = new JCheckBox("");
        noteReleases.setSelected(Prefs.getLastBoolean("RandomizeVelocityReleases", true));
        SmallDial variance = new SmallDial(Prefs.getLastDouble("RandomizeVelocityVariance", 0.1))
            {
            double value;
            public double getValue() { return value; }
            public void setValue(double val) { value = val; }
            public String map(double val) { return String.format("%.4f", val); }
            };
        JComponent[] components = new JComponent[] { noteReleases, variance.getLabelledDial("0.0000") };
        int result = Dialogs.showMultiOption(sequi, names, components, new String[] {  "Randomize", "Cancel" }, 0, "Randomize", "Enter Velocity Randomization Settings");
        
        if (result == 0)
            {
            seq.push();
            boolean _releases = noteReleases.isSelected();
            double _variance = variance.getValue();
        
            ReentrantLock lock = seq.getLock();
            lock.lock();
            boolean stopped;
            try
                {
                notes.randomizeVelocity(events, _variance, _releases, seq.getDeterministicRandom());
                }
            finally
                {
                lock.unlock();
                }
        
            Prefs.setLastBoolean("RandomizeVelocityReleases", _releases);
            Prefs.setLastDouble("RandomizeVelocityVariance", _variance);
            }

        rebuild();
        }

    public void doSetVelocity()
        {
        ArrayList<Notes.Event> events = gridui.getSelectedOrRangeEvents();
        boolean hasRange = false;
        boolean ruler = getRuler().getHasRange();

        if (events.size() > 0)
            {
            hasRange = true;
            }

        String[] names = { "Velocity" };
        SmallDial velocity = new SmallDial(Prefs.getLastDouble("SetVelocity", 1.0))
            {
            double value;
            public double getValue() { return value; }
            public void setValue(double val) { value = val; }
            public String map(double val) { return "" + (int)((val * 126) + 1); }
            };
        JComponent[] components = new JComponent[] { velocity.getLabelledDial("126") };
        int result = Dialogs.showMultiOption(sequi, names, components, new String[] {  "Set", "Cancel" }, 0, "Set", "Enter Velocity Settings");
        
        if (result == 0)
            {
            seq.push();
            int _velocity = (int)(velocity.getValue() * 126) + 1;
        
            ReentrantLock lock = seq.getLock();
            lock.lock();
            boolean stopped;
            try
                {
                notes.setVelocity(events, _velocity);
                }
            finally
                {
                lock.unlock();
                }
        
            Prefs.setLastDouble("SetVelocity", _velocity);
            }
        
        reload(events);
        }


    public void doFilter()
        {
        JCheckBox range = new JCheckBox("");

        ArrayList<Notes.Event> events = gridui.getSelectedOrRangeEvents();
        boolean hasRange = false;
        boolean ruler = getRuler().getHasRange();

        if (events.size() > 0)
            {
            hasRange = true;
            }

        String[] names = { "Remove Notes", "Remove Bend", "Remove CC", "Remove NRPN", "Remove RPN", "Remove Aftertouch" };
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
        
        JComponent[] components = new JComponent[] { removeNotes, removeBend, removeCC, removeNRPN, removeRPN, removeAftertouch};
        int result = Dialogs.showMultiOption(sequi, names, components, new String[] {  "Filter", "Cancel" }, 0, "Filter", "Enter Filter Settings");
        
        if (result == 0)
            {
            seq.push();
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
                notes.filter(events, _removeNotes, _removeBend, _removeCC, _removeNRPN, _removeRPN, _removeAftertouch);
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

            rebuild();
            // we could change everything
//            table.reload();
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
        reload();
        
        }
    public void doStretchTime()
        {
        ArrayList<Notes.Event> events = gridui.getSelectedOrRangeEvents();
        boolean hasRange = false;

        if (events.size() > 0)
            {
            hasRange = true;
            }
                
        String[] names = { "From", "To"};
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


        JComponent[] components = new JComponent[] { from.getLabelledDial("32"), to.getLabelledDial("32"), };
        int result = Dialogs.showMultiOption(sequi, names, components, new String[] {  "Stretch", "Cancel" }, 0, "Stretch Time", "Enter Stretch Settings");
        
        if (result == 0)
            {
            seq.push();
            int _from = (int)(from.getValue() * 31 + 1);
            int _to = (int)(to.getValue() * 31 + 1);
        
            seq.push();
            ReentrantLock lock = seq.getLock();
            lock.lock();
            try
                {
                notes.stretch(events, _from, _to, hasRange);
                }
            finally
                {
                lock.unlock();
                }
        
            Prefs.setLastDouble("StretchTimeFrom", from.getValue());
            Prefs.setLastDouble("StretchTimeTo", to.getValue());
            
            rebuild();
            //table.reload();                                     // we could change time
            //table.setSelection(evts);
            }
        }
        
    public GridUI getGridUI() { return gridui; }
        
    public void doZoomIn()
        {
        gridui.setScale(gridui.getScale() / 2.0);
        gridui.reload();
        rebuildSizes();
        gridui.repaint();
        }

    public void doZoomOut()
        {
        gridui.setScale(gridui.getScale() * 2.0);
        gridui.reload();
        rebuildSizes();
        gridui.repaint();
        }
        
    public void doScrollToSelected()
        {
        Rectangle rect = gridui.getEventBoundingBox(true);
        
        if (rect == null)
            {
            rect = gridui.getEventBoundingBox(false);
            }
                
        if (rect != null)
            {
            if (rect.y < 0)     // not a note
                {
                double scale = 1.0 / gridui.getScale();
                rect.y = 64;
                rect.height = 0;
                rect.x = (int)(rect.x * scale);
                rect.y *= PitchUI.PITCH_HEIGHT;
                rect.width = (int)(rect.width * scale);
                }
            else
                {
                double scale = 1.0 / gridui.getScale();
                rect.y = 127 - rect.y;
                rect.y -= rect.height;          // because we're flipped, we need the top left corner, not bottom left
                rect.height += 1;
                rect.x = (int)(rect.x * scale);
                rect.y *= PitchUI.PITCH_HEIGHT;
                rect.height *= PitchUI.PITCH_HEIGHT;
                rect.width = (int)(rect.width * scale);
                }
                                                                
            /// BUG IN JAVA (at least MacOS)
            /// scrollRectToVisible is broken.  So we have to fake it here.
            /// getPrimaryScroll().getViewport().scrollRectToVisible(rect);

            int viewportWidth = (int)(getPrimaryScroll().getViewport().getViewRect().getWidth());
            int viewportHeight = (int)(getPrimaryScroll().getViewport().getViewRect().getHeight());

            int posx = Math.max(0, rect.x);
            int posy = Math.max(0, rect.y + (rect.height - viewportHeight) / 2);

            getPrimaryScroll().getViewport().setViewPosition(new Point(posx, posy));
            }       
        }
        
    /*
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
      notes.getEvents().add(where, insertedNote = new Notes.Note(60, DEFAULT_NOTE_VELOCITY, when, 192, DEFAULT_NOTE_VELOCITY));                // guarantee it's first. It's at time 0 too so we don't need to sort.
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

      // now comes the costly part
      notes.sortEvents();
            
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
      gridui.clearSelected();
      gridui.addEventToSelected(getNoteUIFor(insertedNote, insertedPitch));
      }
      }
    */

    public void doRemove()
        {
        /*
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
        */
        
        if (gridui.getSelected().size() == 0)
            {
            sequi.showSimpleError("No Events Selected", "No events are selected, and so none were deleted.");
            return;
            }
                
        setChildInspector(null);
        gridui.deleteSelectedEvents();
        gridui.repaint();
        eventsui.repaint();
        }


    public void doCopy()
        {
        /*
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
        */

        if (gridui.getSelected().size() == 0)
            {
            sequi.showSimpleError("No Events Selected", "No events are selected, and so none were copied.");
            return;
            }
                
        gridui.copySelectedEvents();
        gridui.repaint();
        eventsui.repaint();
        }


/*
  public void loadEvents()
  {
  ArrayList<Notes.Event> events = new ArrayList<Notes.Event>(notes.getEvents());          // copy?
  table.setEvents(events);
  }
*/        
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
                    notes.read(stream = new FileInputStream(fd.getDirectory() + fd.getFile()));
                    events = new ArrayList<Notes.Event>(notes.getEvents());         // copy?
                    }
                finally
                    {
                    lock.unlock();
                    }
                if (events != null) 
                    {
                    rebuild();
//                    table.setEvents(events);
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
        if (childInspector != null) setChildInspector(childInspector);
                
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
    
/*
  public EventTable getTable()
  {
  return table;
  }
*/
        
    public void buildPrimary(JScrollPane scroll)
        {
        /*
          table = new EventTable(notes, seq);
        
          table.addListSelectionListener(new javax.swing.event.ListSelectionListener()
          {
          public void valueChanged(javax.swing.event.ListSelectionEvent e)
          {
          // Find event
          Notes.Event event;
          ReentrantLock lock = seq.getLock();
          lock.lock();
          try
          {
          ArrayList<Notes.Event> evts = notes.getEvents();
          event = evts.get(table.getSelectedRow());
          }
          finally
          {
          lock.unlock();
          }

          // It's a single item selected
          if (table.getSelectedRow() >= 0 && table.getSelectedRowCount() == 1)
          {
          setChildInspector(new EventInspector(seq, notes, NotesUI.this, event));
          }
          else
          {
          setChildInspector(null);
          }
          }
          });
          loadEvents();
        */

        gridui = new GridUI(this);
        scroll.setViewportView(gridui);
        scroll.setRowHeaderView(gridui.buildKeyboard());
        ruler = gridui.buildRuler();
        eventsui = new EventsUI(gridui);
        JComponent box = new JComponent()               // Can't use JPanel, Box, or BoxLayout, they max out at 32768 width
            {
            };
        box.setLayout(new BorderLayout());
        box.add(ruler, BorderLayout.NORTH);
        box.add(eventsui, BorderLayout.CENTER);
        scroll.setColumnHeaderView(box);
        //scroll.setCorner(JScrollPane.UPPER_LEFT_CORNER, eventsui.getHeader());
                
        // For some reason, the notes don't appear right now, perhaps the JScrollView hasn't been installed yet?
        // So we delay with an invokeLater and it works...
        SwingUtilities.invokeLater(new Runnable()
            {
            public void run()
                {
                gridui.reload();
                rebuildSizes();
                gridui.repaint();

		        // Scroll to Middle C-ish?  Or Selected?
		        if (gridui.getSelected().size() > 0)
		        	{
		        	doScrollToSelected();
		        	}
		        else
		        	{
		        	/// FIXME THIS ISN'T WORKING!!!
					JScrollBar vert = getPrimaryScroll().getVerticalScrollBar();
		        	vert.setValue((vert.getMaximum() - vert.getMinimum()) / 4);
				    }
				}
            });
                                
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
        
        this.scroll = scroll;
        }

    void rebuildSizes()
        {
        for(PitchUI pitchui : gridui.getPitchUIs())
            {
            pitchui.revalidate();
            pitchui.repaint();
            }
        ruler.revalidate();
        ruler.repaint();
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

        PushButton zoomInButton = new PushButton(new StretchIcon(PushButton.class.getResource("icons/zoomin.png")))
            {
            public void perform()
                {
                doZoomIn();
                }
            };
        zoomInButton.getButton().setPreferredSize(new Dimension(24, 24));
        zoomInButton.setToolTipText(ZOOM_IN_BUTTON_TOOLTIP);

        PushButton zoomOutButton = new PushButton(new StretchIcon(PushButton.class.getResource("icons/zoomout.png")))
            {
            public void perform()
                {
                doZoomOut();
                }
            };
        zoomOutButton.getButton().setPreferredSize(new Dimension(24, 24));
        zoomOutButton.setToolTipText(ZOOM_OUT_BUTTON_TOOLTIP);

        JPanel console = new JPanel();
        console.setLayout(new BorderLayout());

/*
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
*/

        PushButton scrollButton = new PushButton("S")
            {
            public void perform()
                {
                doScrollToSelected();
                }
            };
        scrollButton.getButton().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        scrollButton.getButton().setPreferredSize(new Dimension(24, 24));

        snapBox = new JComboBox(SNAP_OPTIONS);
        snapBox.setSelectedIndex(Prefs.getLastInt("SnapTo", SNAP_DEFAULT_OPTION));
        gridui.setSnap(SNAP_QUANTIZATIONS[snapBox.getSelectedIndex()]);
        snapBox.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                int snap = snapBox.getSelectedIndex();
                if (snap <= 4)          // Snap-To Range
                    {
                    gridui.setSnap(SNAP_QUANTIZATIONS[snap]);
                    gridui.setSnapBy(false);
                    }
                else                            // Snap-By Range
                    {
                    gridui.setSnap(SNAP_QUANTIZATIONS[snap - 4]);
                    gridui.setSnapBy(true);
                    }
                Prefs.setLastInt("SnapTo", snapBox.getSelectedIndex());
                }
            });

        maxBox = new JComboBox(MAX_OPTIONS);
        maxBox.setSelectedIndex(Prefs.getLastInt("MaxBar", MAX_DEFAULT_OPTION));
        gridui.setMaximumTime(MAX_MEASURES[maxBox.getSelectedIndex()]);
        maxBox.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                gridui.setMaximumTime(MAX_MEASURES[maxBox.getSelectedIndex()]);
                Prefs.setLastInt("MaxBar", maxBox.getSelectedIndex());
                rebuildSizes();
                }
            });

        console = new JPanel();
        console.setLayout(new BorderLayout());
                
        Box addRemoveBox = new Box(BoxLayout.X_AXIS);
        addRemoveBox.add(removeButton);
        addRemoveBox.add(copyButton);
        console.add(addRemoveBox, BorderLayout.WEST);   
        
        Box otherBox = new Box(BoxLayout.X_AXIS);
        otherBox.add(maxBox);
        otherBox.add(snapBox);
//        otherBox.add(addNoteButton);
//        otherBox.add(addBendButton);
//        otherBox.add(addAftertouchButton);
//        otherBox.add(addCCButton);
//        otherBox.add(addNRPNButton);
//        otherBox.add(addRPNButton);
        otherBox.add(zoomInButton);
        otherBox.add(zoomOutButton);
        otherBox.add(scrollButton);
        console.add(otherBox, BorderLayout.EAST);
                
        return console; 
        }

    /** Revises the child inspector's values to reflect the current Note values.  If the */
    public void updateChildInspector(boolean forceRebuild)
        {
        if (gridui.selected.size() == 1)
            {
            updateChildInspector(gridui.getSelectedIterator().next(), forceRebuild);                // get the first (and only) one
            }
        else
            {
            setChildInspector(null);
            }
        }

    public void updateChildInspector(EventUI eventui, boolean forceRebuild)
        {
        if (childInspector == null || childInspector.getEvent() != eventui.event || forceRebuild)
            {
            int index = -1;
            /*
              ReentrantLock lock = seq.getLock();
              lock.lock();
              try
              {
              index = notes.getEvents().indexOf(eventui.event);
              }
              finally
              {
              lock.unlock();
              }
            */
            setChildInspector(new EventInspector(seq, notes, this, eventui.event));
            }
        else
            {
            // just update the inspector
            if (childInspector != null)
                {
                childInspector.revise();
                }
            }
        }

    public void setChildInspector(EventInspector inspector)
        {
        childInspector = inspector;
        childOuter.removeAll();
        if (inspector != null) 
            {
            childOuter.add(inspector, BorderLayout.NORTH);
            childBorder.setTitle(inspector.getName());
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
            childBorder.setTitle(childInspector.getName());
            childOuter.setBorder(null);             // this has to be done or it won't immediately redraw!
            childOuter.setBorder(childBorder);
            childInspector.revise();
            }
        }
        
    public void redraw(boolean inResponseToStep) 
        {
        boolean stopped;
        boolean recorded = false;
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
                                
/*
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
*/
            }

        
        
        // When we build the gridui, it sends an updateUI() message which goes to SwingUtilities.invokeLater,
        // which in turn redraws everything including the ruler in a separate thread -- but it is occasionally 
        // possible that the ruler has not yet been built quite yet!  So to avoid this race condition (and it
        // does happen) we only repaint the ruler here if it's non-null.
        if (recorded)
            {
            rebuild();
            super.redraw(inResponseToStep);
            }
        // JScrollPanes ignore repaint().  We have to repaint the objects inside them maybe.  
        else if (inResponseToStep)
            {
            // primaryScroll.getViewport().repaint();                  // just repaint the viewport, not the scroll view
            if (ruler != null) 
                {
                ruler.repaint();
                }
            }
        else
            {
            if (gridui != null)
                {
                gridui.repaint();
                }
            if (eventsui != null)
                {
                eventsui.repaint();
                }
            if (ruler != null) 
                {
                ruler.repaint();
                }
            super.redraw(inResponseToStep);
            }
        }
        
    public void rebuild()
        {
        if (gridui != null) 
            {
            gridui.rebuild();
            gridui.repaint();
            }
        if (eventsui != null)
            {
            eventsui.rebuild();
            eventsui.repaint();
            }
        if (ruler != null) 
            {
            ruler.repaint();
            }
        }


    public void stopped()
        {
        if (isUIBuilt())
            {	
			// we may have just recorded.  We have to display the new notes
			rebuild();
            }
        }
        

                          
    static final String REMOVE_BUTTON_TOOLTIP = "<html><b>Remove Event</b><br>" +
        "Removes the selected event or events from the Notes.</html>";
        
    static final String COPY_BUTTON_TOOLTIP = "<html><b>Copy Event</b><br>" +
        "Duplicates the selected event or events from in the Notes.</html>";
/*
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
*/       
    static final String ZOOM_IN_BUTTON_TOOLTIP = "<html><b>Zoom In</b><br>" +
        "Magnifies the view of the Notes timeeline.</html>";
        
    static final String ZOOM_OUT_BUTTON_TOOLTIP = "<html><b>Zoom Out</b><br>" +
        "Demagnifies the view of the Notes timeeline.</html>";

    }
