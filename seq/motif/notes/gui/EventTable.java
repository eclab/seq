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
    // OTHER
       
    public static final Color BONDI_BLUE = new Color(62, 116, 186);		// currently color used for selection
     
    JTable table = new JTable(0, 3);
    int index = -1;						// the currently playing event
    Seq seq;
    Notes notes;
        
    public int getSelectedRow() { return table.getSelectedRow(); }
    public int getSelectedRowCount() { return table.getSelectedRowCount(); }
        
    public void addListSelectionListener(javax.swing.event.ListSelectionListener listener)
        {
        table.getSelectionModel().addListSelectionListener(listener);
        }

    public EventTable(Notes notes, Seq seq)
        {
        this.notes = notes;
        this.seq = seq;
        ArrayList<Notes.Event> events = notes.getEvents();

        ListSelectionModel select = table.getSelectionModel();
        //select.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        select.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setRowSelectionAllowed(true);             // this is already the case
        table.setColumnSelectionAllowed(false);         
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
                else if (isSelected)
                    {
                    c.setForeground(Color.WHITE);
                    c.setBackground(BONDI_BLUE);
                    }
                else
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
        if (this.index == index) return; // nothing changed  -- note that without this line, scrollRectToVisible creates many selection artifacts
         
        this.index = index;

        if (index >= 0)
            {
            JTable tbl = getTable();
            tbl.scrollRectToVisible(new Rectangle(tbl.getCellRect(index, 0, true)));

//            ListSelectionModel select = table.getSelectionModel();
//            ((AbstractTableModel)(table.getModel())).fireTableDataChanged();                // force a repaint
//            if (min >= 0) select.setSelectionInterval(min, max);            // this has to be AFTER fireTableDataChanged
            }
        else 
            {
//            ((AbstractTableModel)(table.getModel())).fireTableDataChanged();                // force a repaint
//            table.repaint();
            }
        table.repaint();
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
                obj[pos][3] = (Notes.CC)evt;
                }
            else if (evt instanceof Notes.Aftertouch)
                {
                obj[pos][3] = (Notes.Aftertouch)evt;
                }
            else if (evt instanceof Notes.NRPN)
                {
                obj[pos][3] = (Notes.NRPN)evt;
                }
            else if (evt instanceof Notes.RPN)
                {
                obj[pos][3] = (Notes.RPN)evt;
                }
            pos++;
            }
                
        table.setModel(
            new DefaultTableModel(
                obj,
                new Object[] { "When", "Note", "Until", "Other" })
                {
                public boolean isCellEditable(int row, int column) { return false; }
                });
        }

    public Notes.Event[] getSelectedEvents()
    	{
    	return getSelectedEvents(getSelectedIndices());
		}
		
    // Builds a list of all currently selected Events
    public Notes.Event[] getSelectedEvents(int[] currentIndices)
    	{
    	Notes.Event[] evt = new Notes.Event[currentIndices.length];
		ReentrantLock lock = seq.getLock();
		lock.lock();
		try
			{
			ArrayList<Notes.Event> events = notes.getEvents();
			int num = 0;
			for(int i = 0; i < currentIndices.length; i++)
				{
				evt[num++] = events.get(currentIndices[i]);
				}
			}
		finally
			{
			lock.unlock();
			}
		return evt;
    	}

    // Selects the provided Events in the table
    public void setSelectedEvents(Notes.Event[] events)
    	{
    	if (events.length == 0) return;
    	
    	ListSelectionModel model = table.getSelectionModel();
    	model.clearSelection();
    	// Where are the events now?
    	HashSet<Notes.Event> hash = new HashSet<>();
    	for(Notes.Event evt : events)
    		{
    		hash.add(evt);
    		}
    	
    	// Get current selected indices
		int num = 0;
		int[] selections = new int[events.length];
		ReentrantLock lock = seq.getLock();
		lock.lock();
		try
			{
			ArrayList<Notes.Event> allEvents = notes.getEvents();
			for(int i = 0; i < allEvents.size(); i++)
				{
				Notes.Event evt = allEvents.get(i);
				if (hash.contains(evt)) 	// found one
					{
					selections[num++] = i;
					}
				}
			}
		finally
			{
			lock.unlock();
			}
    	Arrays.sort(selections);
    	
    	// Submit Intervals
    	int start = selections[0];
    	int end;
    	model.setValueIsAdjusting(true);
    	// Here we're going to attempt to only add contiguous intervals, not individual
    	// rows, in the hopes that this causes the selection model to be more efficient
    	for(int i = 0; i < selections.length; i++)
    		{
    		if (i == selections.length - 1 || selections[i + 1] - selections[i] > 1)
    			// If we're at the end of the list OR if the next item skips ahead
    			{
    			end = selections[i];
    			model.addSelectionInterval(start, end);
    			if (i < selections.length - 1)
    				{
    				start = selections[i + 1];		// don't worry, it'll get skipped unless it's last
    				}
    			}
    		}
    	model.setValueIsAdjusting(false);
    	}
    	
    public int[] getSelectedIndices()
    	{
    	int num = 0;
    	ListSelectionModel model = table.getSelectionModel();
        int min = model.getMinSelectionIndex();
        int max = model.getMaxSelectionIndex();
        if (min == -1 || max == -1)	// empty
        	{
        	return new int[0];
        	}
        	
    	for(int i = min; i <= max; i++)		// note <=
    		{
    		if (model.isSelectedIndex(i)) num++;
    		}
    		
    	int[] idx = new int[num];
    	num = 0;
    	for(int i = min; i <= max; i++)		// note <=
    		{
    		if (model.isSelectedIndex(i)) idx[num++] = i;
    		}
    	return idx;
    	}
        
    public void reload() 
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
            setEvents(events);		// does a clearSelection()
            }
        else
        	{
        	clearSelection();
        	}
        }
    }
