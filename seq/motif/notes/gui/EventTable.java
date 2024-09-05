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

// For Drag and Drop
import java.awt.dnd.*;
import java.awt.datatransfer.*;


public class EventTable extends JPanel
    {
    // Columns are:
    // WHEN
    // NOTE
    // UNTIL
    // AFTERTOUCH
    // BEND
    // CC
        
    JTable table = new JTable(0, 5);
    int index = -1;
    Seq seq;
        
    public int getSelectedRow() { return table.getSelectedRow(); }
    public int getSelectedRowCount() { return table.getSelectedRowCount(); }
        
    public void addListSelectionListener(javax.swing.event.ListSelectionListener listener)
        {
        table.getSelectionModel().addListSelectionListener(listener);
        }

    public EventTable(Notes notes, Seq seq)
        {
        this(notes.getEvents(), seq);
        }
        
    public EventTable(ArrayList<Notes.Event> events, Seq seq)
        {
        this.seq = seq;
        ListSelectionModel select = table.getSelectionModel();
        select.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        table.setRowSelectionAllowed(true);             // this is already the case
        table.setColumnSelectionAllowed(false);         
        clearSelection();
        setEvents(events);
                
        // Make not editable.  This is much harder than it should be
        table.setModel(new DefaultTableModel()
            {
            public boolean isCellEditable(int row, int column) { return false; }
            });

        // Set up renderer to display black or red.  This is ALSO far harder than it shoould be.
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer()
            {
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) 
                {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (row == index) 
                    {
                    c.setForeground(Color.WHITE);
                    c.setBackground(Color.GRAY);
                    }
                else if (!isSelected)
                    {
                    c.setForeground(Color.BLACK);
                    c.setBackground(null);
                    }
                return c;
                }
            });

        table.setDefaultRenderer(Notes.Event.class, new DefaultTableCellRenderer()
            {
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) 
                {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (row == index)
                    {
                    c.setForeground(Color.WHITE);
                    c.setBackground(Color.GRAY);
                    }
                else if (!isSelected)
                    {
                    c.setForeground(Color.BLACK);
                    c.setBackground(null);
                    }
                
                return c;
                }
            });
        }

    public void setIndex(int index)
        {
        if (this.index == index) return; // nothing changed
                
        this.index = index;
        if (index >= 0)
            {
            ListSelectionModel select = table.getSelectionModel();
            int min = select.getMinSelectionIndex();
            int max = select.getMaxSelectionIndex();
            JTable tbl = getTable();
            tbl.scrollRectToVisible(new Rectangle(tbl.getCellRect(index, 0, true)));
            ((AbstractTableModel)(table.getModel())).fireTableDataChanged();                // force a repaint
            if (min >= 0) select.setSelectionInterval(min, max);            // this has to be AFTER fireTableDataChanged
            }
        else 
            {
            ((AbstractTableModel)(table.getModel())).fireTableDataChanged();                // force a repaint
            table.repaint();
            }
        }

    public void clearSelection()
        {
        ListSelectionModel select = table.getSelectionModel();
        select.clearSelection();
        }
                
    public void setSelection(int index)
        {
        setSelection(index, index);
        }
                
    public void setSelection(int min, int max)
        {
        table.getSelectionModel().setSelectionInterval(min, max);
        }
                
    public JTable getTable() { return table; }
        
        
    public void setEvents(ArrayList<Notes.Event> events)
        {
        clearSelection();

        Object[][] obj = new Object[events.size()][6];
        int pos = 0;
                
        int _beatsPerBar = 0;
        
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            _beatsPerBar = seq.getBar();
            }
        finally
            {
            lock.unlock();
            }
        final int beatsPerBar = _beatsPerBar;
                
        for(Notes.Event evt : events)
            {
            obj[pos][0] = new Object()              // We have to make a wrapper like this because it's mutable and must be displayed
                {
                public String toString() { return Seq.timeToString(evt.when, beatsPerBar); };
                };
            if (evt instanceof Notes.Note)
                {
                obj[pos][1] = (Notes.Note)evt;
                obj[pos][2] = new Object()              // We have to make a wrapper like this because it's mutable and must be displayed
                    {
                    public String toString() { return Seq.timeToString(evt.when + evt.length, beatsPerBar); };
                    };
                }
            else if (evt instanceof Notes.Bend)
                {
                obj[pos][3] = (Notes.Bend)evt;
                }
            else if (evt instanceof Notes.CC)
                {
                obj[pos][4] = (Notes.CC)evt;
                }
            else if (evt instanceof Notes.Aftertouch)
                {
                obj[pos][5] = (Notes.Aftertouch)evt;
                }
            pos++;
            }
                
        table.setModel(
            new DefaultTableModel(
                obj,
                new Object[] { "When", "Note", "Until", "Bend", "CC", "Aftertouch" })
                {
                public boolean isCellEditable(int row, int column) { return false; }
                });
        }

    }
