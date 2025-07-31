/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.gui;

import seq.engine.*;
import seq.util.*;
import java.io.*;
import java.util.zip.*;
import java.awt.*;
import javax.swing.border.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.concurrent.locks.*;
import java.util.*;
import javax.sound.midi.*;
import com.formdev.flatlaf.*;
import org.json.*;


/***
    SEQUI is the topmost GUI object for Seq.  It contains a MOTIFLIST, which is
    a list of available Motifs (at left), and a MOTIFUI at center representing the
    currently-edited Motif.
*/

public class SeqUI extends JPanel
    {       
    static
        {
        ToolTipManager.sharedInstance().setDismissDelay(1000000);                       // Our tooltips are long. We don't want an auto-dismiss unless the user navigates away
        }
        
    /*  
        private static class MyEventQueue extends EventQueue {
        public void postEvent(AWTEvent theEvent) {
        System.out.println("Event Posted " + theEvent);
        super.postEvent(theEvent);
        }
        }
    
        static
        {
        Toolkit.getDefaultToolkit().getSystemEventQueue().push(new MyEventQueue()); 
        }
    */

    /** Seq Patches end with this extension. */
    public static final String PATCH_EXTENSION = ".seq";
    
    public static final String MIDI_EXTENSION = ".mid";
    
    public static final int STEP_SEQUENCE_INITIAL = 0; 
    public static final int NOTES_INITIAL = 1; 
    public static final int SERIES_INITIAL = 2; 
    public static final int PARALLEL_INITIAL = 3; 
    
    public static final int MIN_INITIAL_WIDTH = 800;
    public static final int MIN_INITIAL_HEIGHT = 600;
    
    // Seq Menu Bar
    JMenuBar menubar;
    // The menu dedicated for Motifs, if any
    JMenu motifUIMenu = null;
    // The seq duh
    Seq seq;
    // The MotifList at left
    MotifList list;
    // The container holding thelist
    JPanel listContainer;
    // The main MotifUI being displayed
    MotifUI motifui = null;
    // The transport above the MotifList.  This is the Play/Stop/Record/Pause buttons plus the clock options
    Transport transport;
    // Root's parameter settings.  This is below the clock options
    RootParameterList rootParameterList;
    // The window
    JFrame frame;
    // Is the selected motif always the root?
    boolean selectedFrameIsRoot;
    boolean smallButtons;
    boolean showToolTips;
    int initialMotif = STEP_SEQUENCE_INITIAL;
    
    JMenuItem undoItem;
    JMenuItem redoItem;
    JMenuItem pushItem;
    JMenuItem logItem;
    
    int rebuildInspectorsCount = 0;
    
    // Arming
    boolean disarmsAllBeforeArming;

    /** Sets whether we disarm all armed Motifs before arming a new one */
    public void setDisarmsAllBeforeArming(boolean val)
        {
        disarmsAllBeforeArming = val;
        }
                
    /** Returns whether we disarm all armed Motifs before arming a new one */
    public boolean getDisarmsAllBeforeArming()
        {
        return disarmsAllBeforeArming;
        }
                
    public Transport getTransport() { return transport; }
    
    public SeqUI(Seq seq)
        {
        setBackground(Color.YELLOW);
        setOpaque(false);
        reset(seq);
        }
    
    public void clearUndo()
        {
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            {
            seq.clearUndo();
            }
        finally { lock.unlock(); }
        undoItem.setEnabled(false);
        redoItem.setEnabled(false);
        }
    
    public void updateUndoMenus()
        {
        SwingUtilities.invokeLater(new Runnable()
            {
            public void run()
                {
                boolean canUndo = false;
                boolean canRedo = false;
                ReentrantLock lock = seq.getLock();
                lock.lock();
                try 
                    { 
                    canUndo = seq.canUndo(); 
                    canRedo = seq.canRedo(); 
                    }
                finally { lock.unlock(); }
                undoItem.setEnabled(canUndo);
                redoItem.setEnabled(canRedo);
                }
            });
        }
        
    public void push()
        {
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            { 
            seq.push(motifui.getMotif()); 
            }
        finally { lock.unlock(); }
        }

    public void doUndo()
        {
        boolean canUndo = false;
        boolean canRedo = false;
        if (this.seq != null) this.seq.stop();
        Motif display = null;
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            {
            display = seq.undo(motifui.getMotif());
            if (display == null) return;  // failed to undo
            canUndo = seq.canUndo(); 
            canRedo = seq.canRedo(); 
            }
        finally { lock.unlock(); }
        reset(seq);
        setMotifUI(list.getMotifUIFor(display));
        list.setRoot(list.getMotifUIFor(seq.getData()));
        }
 
    public void doRedo()
        {
        if (this.seq != null) this.seq.stop();
        Motif display = null;
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            {
            display = seq.redo(motifui.getMotif());
            if (display == null) return;  // failed to redo
            }
        finally { lock.unlock(); }
        reset(seq);
        setMotifUI(list.getMotifUIFor(display));
        list.setRoot(list.getMotifUIFor(seq.getData()));
        }
   
    public boolean getSelectedFrameIsRoot() { return selectedFrameIsRoot; }
        
    /* Resets the Seq to a pristine state and clears out the SeqUI. */
    void reset(Seq seq)
        {
        removeAll();
        smallButtons = Prefs.getLastBoolean("SmallMotifButtons", false);        // must be before MotifList
        showToolTips = Prefs.getLastBoolean("ShowToolTips", true);

        // Arming
        disarmsAllBeforeArming = Prefs.getLastBoolean("DisarmFirst", true);

        list = new MotifList(seq, this);

        ReentrantLock lock = null;
        lock = seq.getLock();
        lock.lock();
        try
            {
            seq.stop();                     // for good measure
            seq.resetPlayingClips();        // because this might not happen in stop if we're already stopped

            this.seq = seq;
            for(int i = 0; i < seq.getOuts().length; i++)
                seq.getOut(i).setSeq(seq);
        
            // we have to build the lower motifuis first so the higher level ones
            // can add them into themselves.  To do this we get a list sorted
            // with the lower ones first
            
            ArrayList<Motif> topologicallySortedMotifs = Motif.topologicalSort(seq.getMotifs());
            Collections.reverse(topologicallySortedMotifs);
            HashMap<Motif, MotifUI> map = new HashMap<>();
            
            for(Motif motif : topologicallySortedMotifs)
                {
                if (!(motif instanceof seq.motif.blank.Blank))
                    {
                    MotifUI motifui = list.buildMotifUIFor(motif);
                    map.put(motif, motifui);
                    //System.err.println("Mapping " + motif + " -> " + motifui);
                    //list.doAddSimple(motifui);
                    }
                }
                
            // Now we want to reorganize the list in the original order.
            list.removeAll();

            for(Motif motif : seq.getMotifs())
                {
                //System.err.println("Trying to get " + motif + " -> " + map.get(motif));
                list.doAddSimple(map.get(motif));
                }
            }
        finally
            {
            if (lock != null) lock.unlock();
            }
        list.revalidate();
        list.repaint();
        
        setLayout(new BorderLayout());
        listContainer = new JPanel();
        listContainer.setLayout(new BorderLayout());
        listContainer.add(list, BorderLayout.CENTER);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        transport = new Transport(seq, this);
        panel.add(transport, BorderLayout.NORTH);
        rootParameterList = new RootParameterList(seq);
        panel.add(rootParameterList, BorderLayout.CENTER);
        listContainer.add(panel, BorderLayout.NORTH);
        add(listContainer, BorderLayout.WEST);
                
        // at this point, the motifuis have all been created but added to the list in reverse order
        //list.reverse();
        }
    
    /** This must be called very early on, probably the very first thing you do. */
    public static void setupGUI()
        {
        // We want a nice Mac environment
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("apple.awt.application.name", "Seq");
        FlatLightLaf.setup();
        }
        
    /** Returns a string which guarantees that the given filename ends with the given ending. */   
    public static String ensureFileEndsWith(String filename, String ending)
        {
        // do we end with the string?
        if (filename.regionMatches(false,filename.length()-ending.length(),ending,0,ending.length()))
            return filename;
        else return filename + ending;
        }
        
    /* Makes a new document. */
    public void doNew()
        {
        // First confirm
        if (showSimpleConfirm("New Sequence", "Create a new sequence?\n\nThis will erase the current sequence, which cannot be undone.", "New Sequence"))
            {
            Seq s;
            try
                {
                clearUndo();

                ReentrantLock lock = seq.getLock();
                lock.lock();
                try 
                    { 
                    this.seq.stop();
                    this.seq.shutdown();            // kills timer in old sequence
                    }
                finally { lock.unlock(); }

                reset(new Seq(seq));
                Seq.incrementDocument();

                list.removeAll();  // delete old ones

                seq.setFile(null);
                frame.setTitle("Untitled");

                Motif dSeq = null;
                Motif[] temp = new Motif[1];
                MotifUI ssui = setupInitialMotif(temp, seq, this);
                dSeq = temp[0];

                /*
                // We'll start with a blank step sequence
                seq.motif.stepsequence.StepSequence ss = new seq.motif.stepsequence.StepSequence(seq, 16, 16);
                seq.motif.stepsequence.gui.StepSequenceUI ssui = new seq.motif.stepsequence.gui.StepSequenceUI(seq, SeqUI.this, ss);
                */
                addMotifUI(ssui);
                list.setRoot(ssui.getPrimaryButton());          // this also calls setData, which builds the clip
                revalidate();
                repaint();
                }
            catch (Exception ex)
                {
                ex.printStackTrace();
                showSimpleError("Error Creating New Sequence", "An error occurred creating the new sequence.");
                }
            }
        }
    
    /* Saves the sequence to an existing file. */
    boolean doSave()
        {
        if (seq.getFile() == null) { return doSaveAs(); }
        
        // Inform MotifUIs so they can update stuff to save out
        for(MotifUI ui : list.getMotifUIs())
            {
            ui.isSaving();
            }

        PrintWriter p = null;
        try
            {
            p = new PrintWriter(new GZIPOutputStream(new FileOutputStream(seq.getFile())));
            if (seq != null) seq.stop();
            p.println(seq.save(true));          // we just print the JSONObject
            p.flush();
            p.close();
            }
        catch (Exception ex)
            {
            ex.printStackTrace();
            showSimpleError("Error Saving File", "An error occurred saving the sequence " + seq.getFile());
            }
        finally
            {
            try { if (p != null) p.close(); } catch (Exception ex) { }
            }
        return true;
        }

    /* Pops up the save dialog. */
    boolean doSaveAs()
        {
        FileDialog fd = new FileDialog(getFrame(), "Save Sequence As...", FileDialog.SAVE);
                
        disableMenuBar();
        fd.setVisible(true);
        enableMenuBar();
                
        File f = null; // make compiler happy

        if (fd.getFile() != null)
            {
            // Inform MotifUIs so they can update stuff to save out
            for(MotifUI ui : list.getMotifUIs())
                {
                ui.isSaving();
                }

            PrintWriter p = null;
            try
                {
                f = new File(fd.getDirectory(), ensureFileEndsWith(fd.getFile(), PATCH_EXTENSION));
                seq.setFile(new File(fd.getDirectory(), fd.getFile()));
                frame.setTitle(fd.getFile());
                                
                p = new PrintWriter(new GZIPOutputStream(new FileOutputStream(f)));
                if (seq != null) seq.stop();
                p.println(seq.save(true));          // we just print the JSONObject
                p.flush();
                p.close();
                }
            catch (Exception ex)
                {
                ex.printStackTrace();
                showSimpleError("Error Saving File", "An error occurred saving the sequence " + f);
                }
            finally
                {
                try { if (p != null) p.close(); } catch (Exception ex) { }
                }
            return true;
            }
        else
            {
            return false;
            }
        }
        
    File logFile = null;
    Sequence[] logs = null;
    
    void doLog()
        {
        if (logFile != null)            // currently logging
            {
            // clean up
            logItem.setText("Log MIDI ...");
            logFile = null;
            logs = null;
            ReentrantLock lock = seq.getLock();
            lock.lock();
            try 
                { 
                seq.setTracks(null);
                }
            finally { lock.unlock(); }
            }
        else
            {       
            int result = showSimpleChoice("Log MIDI ...", "Some DAWs (like Ableton) cannot properly load\na MIDI file with more than one channel.\n\nBreak out individual channels to separate MIDI files?",
                new String[] { "Keep One File", "Break Out", "Cancel" });
            if (result < 0 || result == 2) return;
                
            if (seq != null) seq.stop();
            boolean multi = (result == 1);                  // multi means we broke out to multiple files

            FileDialog fd = new FileDialog(getFrame(), "Log MIDI ...", FileDialog.SAVE);
                                
            disableMenuBar();
            fd.setVisible(true);
            enableMenuBar();
                                
            if (fd.getFile() != null)
                {
                try
                    {
                    logFile = new File(fd.getDirectory(), ensureFileEndsWith(fd.getFile(), MIDI_EXTENSION));
                    logs = new Sequence[Seq.NUM_OUTS];
                    Track[] tracks = new Track[Seq.NUM_OUTS];
                    if (multi) 
                        {
                        for(int i = 0; i < tracks.length; i++) 
                            {
                            logs[i] = new Sequence(Sequence.PPQ, Seq.PPQ);
                            tracks[i] = logs[i].createTrack();
                            }
                        }
                    else
                        {
                        logs[0] = new Sequence(Sequence.PPQ, Seq.PPQ);
                        tracks[0] = logs[0].createTrack();
                        }
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try 
                        {
                        seq.setTracks(tracks);
                        }
                    finally { lock.unlock(); }
                    logItem.setText("Stop Logging");
                    }
                catch (Exception ex)
                    {
                    logItem.setText("Log MIDI ...");
                    ex.printStackTrace();
                    showSimpleError("Error Creating Log", "An error occurred logging MIDI.");
                                
                    // clean up
                    logFile = null;
                    logs = null;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try 
                        { 
                        seq.setTracks(null);
                        }
                    finally { lock.unlock(); }
                    }
                }
            else
                {
                // cancelled, clean up
                logItem.setText("Log MIDI ...");
                logFile = null;
                logs = null;
                ReentrantLock lock = seq.getLock();
                lock.lock();
                try 
                    { 
                    seq.setTracks(null);
                    }
                finally { lock.unlock(); }
                }
            }
        }



    /* Pops up the export dialog. */
    void doExportRoot()
        {
        FileDialog fd = new FileDialog(getFrame(), "Export Rooted Sequence As...", FileDialog.SAVE);
                
        disableMenuBar();
        fd.setVisible(true);
        enableMenuBar();
                
        File f = null; // make compiler happy

        if (fd.getFile() != null)
            {
            // Inform MotifUIs so they can update stuff to save out
            for(MotifUI ui : list.getMotifUIs())
                {
                ui.isSaving();
                }

            PrintWriter p = null;
            try
                {
                f = new File(fd.getDirectory(), ensureFileEndsWith(fd.getFile(), PATCH_EXTENSION));
                seq.setFile(new File(fd.getDirectory(), fd.getFile()));
                frame.setTitle(fd.getFile());
                                
                p = new PrintWriter(new GZIPOutputStream(new FileOutputStream(f)));
                if (seq != null) seq.stop();
                p.println(seq.save(false));          // we just print the JSONObject
                p.flush();
                p.close();
                }
            catch (Exception ex)
                {
                ex.printStackTrace();
                showSimpleError("Error Saving File", "An error occurred saving the sequence " + f);
                }
            finally
                {
                try { if (p != null) p.close(); } catch (Exception ex) { }
                }
            }
        }
                
    /* Pops up the open (load) dialog, returning a new Seq resulting from loading. */
    void doLoad()
        {
        FileDialog fd = new FileDialog((JFrame)getFrame(), "Load Sequence...", FileDialog.LOAD);
        fd.setFilenameFilter(new FilenameFilter()
            {
            public boolean accept(File dir, String name)
                {
                return ensureFileEndsWith(name, PATCH_EXTENSION).equals(name);
                }
            });

        disableMenuBar();
        fd.setVisible(true);
        enableMenuBar();
                
        if (fd.getFile() != null)
            {
            GZIPInputStream stream = null;
            try
                {
                clearUndo();
                Seq newSeq = Seq.load(seq, new JSONObject(new JSONTokener(stream = new GZIPInputStream(new FileInputStream(fd.getDirectory()+fd.getFile())))));
                newSeq.setFile(new File(fd.getDirectory(), fd.getFile()));
                frame.setTitle(fd.getFile());

                ReentrantLock lock = seq.getLock();
                lock.lock();
                try 
                    { 
                    this.seq.stop();
                    this.seq.shutdown();            // kills timer in old sequence
                    }
                finally { lock.unlock(); }

                list.removeAll();  // delete old ones

                seq = newSeq;                                
                reset(seq);
                Seq.incrementDocument();

                MotifUI motifui = list.getMotifUIFor(seq.getData());
                list.setRoot(motifui);
                list.select(list.getRoot());
                setMotifUI(motifui);
                revalidate();
                
                // Change the notes split
                motifui.displayNotes();
                repaint();
                }
            catch (Exception ex)
                {
                ex.printStackTrace();
                showSimpleError("Error Loading Sequence", "An error occurred loading the sequence " + fd.getFile());
                }
            finally
                {
                try { stream.close(); } catch (Exception ex) { }
                }
            }
        }
                
    /* Pops up the open (load) dialog, returning a new Seq resulting from loading. */
    void doMerge()
        {
        FileDialog fd = new FileDialog((JFrame)getFrame(), "Merge Sequence...", FileDialog.LOAD);
        fd.setFilenameFilter(new FilenameFilter()
            {
            public boolean accept(File dir, String name)
                {
                return ensureFileEndsWith(name, PATCH_EXTENSION).equals(name);
                }
            });

        disableMenuBar();
        fd.setVisible(true);
        enableMenuBar();
                
        if (fd.getFile() != null)
            {
            GZIPInputStream stream = null;
            try
                {
                Seq newSeq = Seq.load(seq, new JSONObject(new JSONTokener(stream = new GZIPInputStream(new FileInputStream(fd.getDirectory()+fd.getFile())))));
                clearUndo();

                ReentrantLock lock = seq.getLock();
                lock.lock();
                try 
                    { 
                    seq.stop();
                    }
                finally { lock.unlock(); }

                list.removeAll();  // delete old ones

                // now merge
                seq.merge(newSeq.getMotifs());
                reset(seq);

                MotifUI motifui = list.getMotifUIFor(seq.getData());
                list.setRoot(motifui);
                list.select(list.getRoot());
                setMotifUI(motifui);
                revalidate();
                repaint();
                }
            catch (Exception ex)
                {
                ex.printStackTrace();
                showSimpleError("Error Mergine Sequence", "An error occurred merging the sequence " + fd.getFile());
                }
            finally
                {
                try { stream.close(); } catch (Exception ex) { }
                }
            }
        }

    public static final int[] ADD_ACCELERATORS = { KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4, KeyEvent.VK_5, KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9, KeyEvent.VK_0 };
    /** Call this immediately after you create the SeqUI JFrame to prepare the menu. */
    public void setupMenu(JFrame frame)
        {
        this.frame = frame;
        
        menubar = new JMenuBar();
        frame.setJMenuBar(menubar);

        // File Menu
        JMenu fileMenu = new JMenu("File");
        menubar.add(fileMenu);
        JMenuItem newItem = new JMenuItem("New Sequence");
        newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        fileMenu.add(newItem);
        newItem.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                doNew();
                }
            });
        fileMenu.addSeparator();
        JMenuItem openItem = new JMenuItem("Load Sequence...");
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        fileMenu.add(openItem);
        openItem.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                doLoad();
                }
            });

        JMenuItem mergeItem = new JMenuItem("Merge Sequence...");
        fileMenu.add(mergeItem);
        mergeItem.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                doMerge();
                }
            });

        fileMenu.addSeparator();
        JMenuItem saveItem = new JMenuItem("Save Sequence");
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        fileMenu.add(saveItem);
        saveItem.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                doSave();
                }
            });
                
        JMenuItem saveAsItem = new JMenuItem("Save Sequence As...");
        saveAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()  | InputEvent.SHIFT_MASK));
        fileMenu.add(saveAsItem);
        saveAsItem.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                doSaveAs();
                }
            });

        JMenuItem exportRootItem = new JMenuItem("Export Root As...");
        fileMenu.add(exportRootItem);
        exportRootItem.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                doExportRoot();
                }
            });
          
        // Edit Menu
        JMenu editMenu = new JMenu("Edit");
        menubar.add(editMenu);
        undoItem = new JMenuItem("Undo");
        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        undoItem.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                doUndo();
                }
            });
        undoItem.setEnabled(false);
        editMenu.add(undoItem);
        redoItem = new JMenuItem("Redo");
        redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        redoItem.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                doRedo();
                }
            });
        redoItem.setEnabled(false);
        editMenu.add(redoItem);
        pushItem = new JMenuItem("Checkpoint");
        pushItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        editMenu.add(pushItem);
        pushItem.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                push();
                }
            });
        
        JMenu addMenu = new JMenu("Add");
        JMenuItem[] items = getMotifList().buildAddMenu();
        for(int i = 0; i < items.length; i++)
            {
            if (i < 10)
                {
                items[i].setAccelerator(KeyStroke.getKeyStroke(ADD_ACCELERATORS[i], Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
                }
            addMenu.add(items[i]);
            }
        menubar.add(addMenu);

        // MIDI Menu
        JMenu midiMenu = new JMenu("MIDI");
        menubar.add(midiMenu);
        JMenuItem midiItem = new JMenuItem("Set MIDI Devices");
        midiMenu.add(midiItem);
        midiItem.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                boolean stopped = false;

                ReentrantLock lock = seq.getLock();
                lock.lock();
                try { stopped = seq.isStopped(); }
                finally { lock.unlock(); }
        
                if (!stopped)
                    {
                    if (showSimpleConfirm("Stop Before Setting Setting MIDI Devices?", "The sequence must be stopped before setting setting MIDI Devices.", "Stop and Set", "Cancel"))
                        {
                        lock.lock();
                        try { seq.stop(); }
                        finally { lock.unlock(); }
                        stopped = true;
                        }
                    else
                        {
                        return;
                        }
                    }

                Midi midi = seq.getMIDI();
                Midi.Tuple old = seq.getMIDITuple();
                Midi.Tuple tuple = midi.getNewTuple(old, SeqUI.this, seq, "Set MIDI Devices", seq.getIns());
                if (tuple != Midi.CANCELLED)
                    {
                    lock.lock();
                    try
                        {
                        seq.setMIDITuple(tuple);
                        incrementRebuildInspectorsCount();
                        }
                    finally
                        {
                        lock.unlock();
                        }
                        
                    if (SeqUI.this.motifui != null) 
                        {
                        SeqUI.this.motifui.rebuildInspectors(rebuildInspectorsCount);
                        }
                    }
                }
            });

        JMenuItem panic = new JMenuItem("Panic");
        midiMenu.add(panic);
        panic.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                ReentrantLock lock = seq.getLock();
                lock.lock();
                try
                    {
                    seq.sendPanic();
                    }
                finally
                    {
                    lock.unlock();
                    }
                }
            });
        panic.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        midiMenu.addSeparator();
        logItem = new JMenuItem("Log MIDI ...");
        midiMenu.add(logItem);
        logItem.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                doLog();
                }
            });
          
        // Motif Menu
        JMenu motifMenu = new JMenu("Motif");
        menubar.add(motifMenu);
        JMenuItem rootItem = new JMenuItem("Make Root");
        rootItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        motifMenu.add(rootItem);
        rootItem.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                getMotifList().doRoot();
                }
            });

        JCheckBoxMenuItem selectedRootItem = new JCheckBoxMenuItem("Set Root When Selecting");
        selectedFrameIsRoot = Prefs.getLastBoolean("SelectedFrameIsRoot", false);
        selectedRootItem.setSelected(selectedFrameIsRoot);
        motifMenu.add(selectedRootItem);
        selectedRootItem.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                selectedFrameIsRoot = selectedRootItem.isSelected();
                Prefs.setLastBoolean("SelectedFrameIsRoot", selectedFrameIsRoot);
                }
            });
        

        JMenu initialMotifMenu = new JMenu("Startup Motif");
        motifMenu.add(initialMotifMenu);
        ButtonGroup group = new ButtonGroup();
                
        JRadioButtonMenuItem initialMotifStepSequenceMenu = new JRadioButtonMenuItem("Step Sequence");
        initialMotifStepSequenceMenu.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                initialMotif = STEP_SEQUENCE_INITIAL;
                Prefs.setLastInt("InitialMotif", initialMotif);
                }
            });
        group.add(initialMotifStepSequenceMenu);
        initialMotifMenu.add(initialMotifStepSequenceMenu);
        
        JRadioButtonMenuItem initialMotifNotesMenu = new JRadioButtonMenuItem("Notes");
        initialMotifNotesMenu.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                initialMotif = NOTES_INITIAL;
                Prefs.setLastInt("InitialMotif", initialMotif);
                }
            });
        group.add(initialMotifNotesMenu);
        initialMotifMenu.add(initialMotifNotesMenu);

        JRadioButtonMenuItem initialMotifSeriesMenu = new JRadioButtonMenuItem("Series");
        initialMotifSeriesMenu.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                initialMotif = SERIES_INITIAL;
                Prefs.setLastInt("InitialMotif", initialMotif);
                }
            });
        group.add(initialMotifSeriesMenu);
        initialMotifMenu.add(initialMotifSeriesMenu);

        JRadioButtonMenuItem initialMotifParallelMenu = new JRadioButtonMenuItem("Parallel");
        initialMotifParallelMenu.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                initialMotif = PARALLEL_INITIAL;
                Prefs.setLastInt("InitialMotif", initialMotif);
                }
            });
        group.add(initialMotifParallelMenu);
        initialMotifMenu.add(initialMotifParallelMenu);

        initialMotif = Prefs.getLastInt("InitialMotif", STEP_SEQUENCE_INITIAL);
        if (initialMotif < STEP_SEQUENCE_INITIAL || initialMotif > PARALLEL_INITIAL) 
            {
            initialMotif = STEP_SEQUENCE_INITIAL;
            }
                        
        if (initialMotif == STEP_SEQUENCE_INITIAL) initialMotifStepSequenceMenu.setSelected(true);
        else if (initialMotif == NOTES_INITIAL) initialMotifNotesMenu.setSelected(true);
        else if (initialMotif == SERIES_INITIAL) initialMotifSeriesMenu.setSelected(true);
        else initialMotifParallelMenu.setSelected(true);

        motifMenu.addSeparator();

        JMenuItem sortItem = new JMenuItem("Sort Motifs");
        motifMenu.add(sortItem);
        sortItem.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                push();
                getMotifList().sort();
                }
            });
            
        JCheckBoxMenuItem smallButtonsItem = new JCheckBoxMenuItem("Small Motif Buttons");
        motifMenu.add(smallButtonsItem);
        smallButtonsItem.setSelected(smallButtons);
        smallButtonsItem.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                getMotifList().setCompressed(smallButtons = smallButtonsItem.isSelected());
                Prefs.setLastBoolean("SmallMotifButtons", smallButtons);
                }
            });

        motifMenu.addSeparator();

        JCheckBoxMenuItem disarmsAllBeforeArmingItem = new JCheckBoxMenuItem("Disarm Motifs Before Arming Next");
        motifMenu.add(disarmsAllBeforeArmingItem);
        disarmsAllBeforeArmingItem.setSelected(disarmsAllBeforeArming);
        disarmsAllBeforeArmingItem.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                disarmsAllBeforeArming = disarmsAllBeforeArmingItem.isSelected();
                Prefs.setLastBoolean("DisarmFirst", disarmsAllBeforeArming);
                }
            });

        JMenuItem disarmItem = new JMenuItem("Disarm All Motifs");
        motifMenu.add(disarmItem);
        disarmItem.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                push();
                ReentrantLock lock = seq.getLock();
                lock.lock();
                try
                    {
                    seq.disarmAll();
                    }
                finally
                    {
                    lock.unlock();
                    }
                    
                setMotifUI(motifui);    // rebuild
                incrementRebuildInspectorsCount();              // show disarmed
                }
            });


        motifMenu.addSeparator();

        JCheckBoxMenuItem showToolTipsItem = new JCheckBoxMenuItem("Show Tooltips");
        motifMenu.add(showToolTipsItem);
        showToolTipsItem.setSelected(showToolTips);
        ToolTipManager.sharedInstance().setEnabled(showToolTips);
        showToolTipsItem.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                ToolTipManager.sharedInstance().setEnabled(showToolTipsItem.isSelected());
                Prefs.setLastBoolean("ShowToolTips", showToolTips);
                }
            });

            
        }
        
    public boolean getSmallButtons() { return smallButtons; }
    
    public void incrementRebuildInspectorsCount() { if (motifui != null) motifui.rebuildInspectors(++rebuildInspectorsCount);        }
        
    /** Returns the window. */
    public JFrame getFrame() { return frame; }
    
    /** Returns the Seq */
    public Seq getSeq() { return seq; }
        
    /** Returns the MotifList */
    public MotifList getMotifList() { return list; }     

    // How often we update the window, in ms
    static final long UPDATE_INTERVAL = 1000 / 50;
    // The last timestep, in ms, when we updated the window
    long lastUpdate = 0;
    
    /** Updates and redraws the MotifList and the current Motif.  If the redrawing is in response to a step,
        it is specified here.  Only redraws if NOT in response to a step, or if the last time
        this method was called was earlier than UPDATE_INTERVAL ago */
    public void redraw(boolean inResponseToStep) 
        { 
        long wallClocktime = System.currentTimeMillis();
        if (!inResponseToStep || wallClocktime < lastUpdate || wallClocktime - lastUpdate > UPDATE_INTERVAL)     // time to update
            {
            lastUpdate = wallClocktime;
            
            int time = 0;   
            ReentrantLock lock = seq.getLock();
            lock.lock();
            try
                {
                time = seq.getTime();
                }
            finally
                {
                lock.unlock();
                }
                                                
            for(MotifUI ui : list.getMotifUIs())
                {
                ui.updateText();
                }

            if (motifui != null) 
                {
                motifui.redraw(inResponseToStep);
                }
                
            if (transport != null) 
                {
                transport.updateClock(time);
                }
            }
        }
    
    /** This is used exclusively by Seq */
    public void message(String title, String message)
        {
        javax.swing.SwingUtilities.invokeLater(new Runnable()
            {
            public void run()
                {
                showSimpleError(SeqUI.this, title, message);
                }
            });
        }

    /** This is used exclusively by Seq */
    public void setCountIn(int val, boolean recording)
        {
        javax.swing.SwingUtilities.invokeLater(new Runnable()
            {
            public void run()
                {
                transport.setCountIn(val, recording);
                }
            });
        }
        
                
    /** Sets the current MotifUI and selects its button in the MotifList */
    public void setMotifUI(MotifUI motifui) 
        { 
        setMotifUI(motifui, true);
        }


    public void setMotifUI(MotifUI motifui, boolean updateStacks) 
        { 
        if (this.motifui != null) this.motifui.uiWasUnset();
        if (this.motifui != null) remove(this.motifui);
        this.motifui = motifui; 
        add(motifui, BorderLayout.CENTER);
        MotifListButton button = motifui.getPrimaryButton();
        if (button != null) getMotifList().select(button, updateStacks);
        motifui.uiWasSet();

        // This must be AFTER uiWasSet(), which calls build() and thus builds the menu...
        
        /// NOTE: this particular configuration, where I don't change the menu if I have
        /// it already up, is necessary to work around a frustrating MacOS Menu bug where if you
        /// remove a menu, then put the same menu object back up, it will often make two
        /// copies (not removing the original!)
        JMenu newMotifUIMenu = motifui.getMenu();
		if (newMotifUIMenu != motifUIMenu)
			{
	        if (motifUIMenu != null) menubar.remove(motifUIMenu);
    		if (newMotifUIMenu != null) menubar.add(newMotifUIMenu);
        	motifUIMenu = newMotifUIMenu;
    		}

        motifui.rebuildInspectors(rebuildInspectorsCount);
        revalidate(); 
        repaint(); 
        }

    /** Returns the current MotifUI */
    public MotifUI getMotifUI() { return motifui; }     
    
    /** Adds the given MotifUI to the MotifList and selects it. */
    public void addMotifUI(MotifUI ui)
        {
        list.doAdd(ui, false);
        //setMotifUI(ui);                       // doAdd already sets it
        }
        
    public void showMotifUI(MotifUI ui)
        {
        setMotifUI(ui);
        list.scrollTo(ui);
        }


    ArrayList<JMenuItem> disabledMenus = null;
    int disableCount;
    /** Disables the menu bar.  disableMenuBar() and enableMenuBar() work in tandem to work around
        a goofy bug in OS X: you can't disable the menu bar and reenable it: it won't reenable
        unless the application loses focus and regains it, and even then sometimes it won't work.
        These functions work properly however.  You want to disable and enable the menu bar because
        in OS X the menu bar still functions even when in a modal dialog!  Bad OS X Java errors.
    */
    public void disableMenuBar()
        {
        if (disabledMenus == null)
            {
            disabledMenus = new ArrayList<JMenuItem>();
            disableCount = 0;
            JFrame ancestor = ((JFrame)(SwingUtilities.getWindowAncestor(this)));
            if (ancestor == null) return;
            JMenuBar bar = ancestor.getJMenuBar();
            if (bar == null) return;
            for(int i = 0; i < bar.getMenuCount(); i++)
                {
                JMenu menu = bar.getMenu(i);
                if (menu != null)
                    {
                    for(int j = 0; j < menu.getItemCount(); j++)
                        {
                        JMenuItem item = menu.getItem(j);
                        if (item != null && item.isEnabled())
                            {
                            disabledMenus.add(item);
                            item.setEnabled(false);
                            }
                        }
                    }
                }
            }
        else
            {
            disableCount++;
            return;
            }
        }       
        
    /** Enables the menu bar.  disableMenuBar() and enableMenuBar() work in tandem to work around
        a goofy bug in OS X: you can't disable the menu bar and reenable it: it won't reenable
        unless the application loses focus and regains it, and even then sometimes it won't work.
        These functions work properly however.  You want to disable and enable the menu bar because
        in OS X the menu bar still functions even when in a modal dialog!  Bad OS X Java errors.
    */
    public void enableMenuBar()
        {
        if (disabledMenus == null) return;
        if (disableCount == 0)
            {
            for(int i = 0; i < disabledMenus.size(); i++)
                {
                disabledMenus.get(i).setEnabled(true);
                }
            disabledMenus = null;
            }
        else
            {
            disableCount--;
            }
        }       
        
    /** Called when we we have stopped.  This allows armed Motif UIs to revise themselves. */
    public void stopped() 
        {
        for(MotifUI motifui : list.getMotifUIs())
            {
            motifui.stopped();
            } 
        
        // Write out sequence log
        if (logs != null)
            {
            try
                {
                Track[] tracks = seq.getTracks();
                if (tracks == null)
                    {
                    System.err.println("SeqUI stopped() ERROR: tracks is null when it should not be.");
                    }
                else if (tracks[1] != null) // multi
                    {
                    for(int i = 0; i < Seq.NUM_OUTS; i++)
                        {
                        if (seq.isValidTrack(i))
                            {
                            File file = new File(logFile.getCanonicalPath() + "." + (i + 1) + ".mid");
                            javax.sound.midi.MidiSystem.write(logs[i], 0, file);
                            }
                        }
                    }
                else
                    {
                    javax.sound.midi.MidiSystem.write(logs[0], 0, logFile);
                    }
                }
            catch (IOException ex)
                {
                ex.printStackTrace();
                showSimpleError("Error Creating Log", "An error occurred logging MIDI.");
                }
            logs = null;
            logFile = null;
            logItem.setText("Log MIDI ...");
            }
        }


    /** Display a simple (OK / Cancel) confirmation message.  Return the result (ok = true, cancel = false). */
    public boolean showSimpleConfirm(String title, String message)
        {
        disableMenuBar();
        boolean ret = (JOptionPane.showConfirmDialog(SeqUI.this, message, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null) == JOptionPane.OK_OPTION);
        enableMenuBar();
        return ret;
        }
        
    /** Display a simple (OK-OPTION / Cancel) confirmation message.  Return the result (okoption = true, cancel = false). */
    public boolean showSimpleConfirm(String title, String message, String okOption)
        {
        return showSimpleConfirm(title, message, okOption, "Cancel"); 
        }

    public boolean showSimpleConfirm(String title, String message, String okOption, String cancelOption)
        {
        disableMenuBar();
        int ret = JOptionPane.showOptionDialog(SeqUI.this, message, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
            new String[] { okOption, cancelOption } , okOption);
        enableMenuBar();
        return (ret == 0);
        }
        
    /** Displays all the given options.  The FIRST option will be the default option.  Returns the option
        selected by the user, or -1 if the user closed the window (this should be
        treated as a cancel) */
    public int showSimpleChoice(String title, String message, String[] options)
        {
        disableMenuBar();
        int ret = JOptionPane.showOptionDialog(SeqUI.this, message, title, JOptionPane.DEFAULT_OPTION, 
            JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        enableMenuBar();
        if (ret == JOptionPane.CLOSED_OPTION) return -1;
        return ret;
        }

    boolean inSimpleError;

    /** Display a simple error message. */
    public void showSimpleError(String title, String message)
        {
        showSimpleError(this, title, message);
        }

    /** Display a simple error message. */
    public void showSimpleError(JComponent parent, String title, String message)
        {
        // A Bug in OS X (perhaps others?) Java causes multiple copies of the same Menu event to be issued
        // if we're popping up a dialog box in response, and if the Menu event is caused by command-key which includes
        // a modifier such as shift.  To get around it, we're just blocking multiple recursive message dialogs here.
        
        if (inSimpleError) return;
        inSimpleError = true;
        disableMenuBar();
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
        enableMenuBar();
        inSimpleError = false;
        }

    /** Display a simple error message. */
    public void showSimpleError(JComponent parent, String title, String message, JComponent extra)
        {
        // A Bug in OS X (perhaps others?) Java causes multiple copies of the same Menu event to be issued
        // if we're popping up a dialog box in response, and if the Menu event is caused by command-key which includes
        // a modifier such as shift.  To get around it, we're just blocking multiple recursive message dialogs here.
        
        if (inSimpleError) return;
        inSimpleError = true;
        disableMenuBar();

        JPanel panel = new JPanel();        
        panel.setLayout(new BorderLayout());
        panel.add(new JLabel(message), BorderLayout.NORTH);

        JPanel inside = new JPanel();        
        inside.setLayout(new BorderLayout());
        inside.add(extra, BorderLayout.NORTH);
        
        JScrollPane pane = new JScrollPane(inside);
        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        pane.setPreferredSize(new Dimension(30, 64));
        panel.add(pane, BorderLayout.CENTER);
        
        JOptionPane.showMessageDialog(parent, panel, title, JOptionPane.ERROR_MESSAGE);
        enableMenuBar();
        inSimpleError = false;
        }

    public static void addResizeListener(JFrame frame)
        {
        frame.addComponentListener(new ComponentAdapter()
            {
            public void componentResized(ComponentEvent event)
                {
                Dimension size = frame.getSize();
                Prefs.setLastInt("InitialWidth", size.width);
                Prefs.setLastInt("InitialHeight", size.height);
                }
            });
        }

    public boolean doQuit()
        {
        int choice = showSimpleChoice("Quit Seq", "Save sequence before quitting?", new String[] { "Cancel", "Save", "Quit" });
        if (choice == 0)        // cancel
            {
            return false;
            }
        else if (choice == 1)   // save
            {
            return doSave();
            }
        else                                    // Quit
            {
            return true;
            }
        }
                
    void doAbout()
        {
        ImageIcon icon = new ImageIcon(SeqUI.class.getResource("about.png"));
        JFrame frame = new JFrame("About Seq");
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().setBackground(Color.BLACK);
        JLabel label = new JLabel(icon);

        frame.getContentPane().add(label, BorderLayout.CENTER);

        JPanel pane = new JPanel()
            {
            public Insets getInsets() { return new Insets(10, 10, 10, 10); }
            };
        pane.setBackground(Color.WHITE);
        pane.setLayout(new BorderLayout());


        JPanel text = new JPanel();
        text.setBackground(Color.WHITE);
        text.setLayout(new BorderLayout());
        JLabel seq = new JLabel(" Seq ");
        seq.setBackground(Color.WHITE);
        seq.setFont(new Font(Font.SERIF, Font.PLAIN, 96));
        text.add(seq, BorderLayout.WEST);
        
        JLabel version = new JLabel(SEQ_ABOUT_TEXT);
        version.setBackground(Color.WHITE);
        version.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        text.add(version, BorderLayout.CENTER);
        
        Collapse collapse = new Collapse(text);
        collapse.setBackground(Color.WHITE);
        pane.add(collapse, BorderLayout.SOUTH);
        
        frame.add(pane, BorderLayout.SOUTH);
        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        }
        
        
    public static MotifUI setupInitialMotif(Motif[] motif, Seq seq, SeqUI ui)
        {
        MotifUI mui;
        
        int initialMotif = Prefs.getLastInt("InitialMotif", STEP_SEQUENCE_INITIAL);
        if (initialMotif < STEP_SEQUENCE_INITIAL || initialMotif > PARALLEL_INITIAL) 
            {
            initialMotif = STEP_SEQUENCE_INITIAL;
            }
                        
        if (initialMotif == STEP_SEQUENCE_INITIAL) 
            {
            motif[0]  = new seq.motif.stepsequence.StepSequence(seq, 16, 16);
            mui = new seq.motif.stepsequence.gui.StepSequenceUI(seq, ui, (seq.motif.stepsequence.StepSequence)motif[0] );
            }
        else if (initialMotif == NOTES_INITIAL)
            {
            boolean autoArm = Prefs.getLastBoolean("ArmNewNotesMotifs", false);
            motif[0]  = new seq.motif.notes.Notes(seq, autoArm);
            mui = new seq.motif.notes.gui.NotesUI(seq, ui, (seq.motif.notes.Notes)motif[0] );
            }
        else if (initialMotif == SERIES_INITIAL)
            {
            motif[0]  = new seq.motif.series.Series(seq);
            mui = new seq.motif.series.gui.SeriesUI(seq, ui, (seq.motif.series.Series)motif[0] );
            }
        else // if (initialMotif == PARALLEL_INITIAL)
            {
            motif[0]  = new seq.motif.parallel.Parallel(seq);
            mui = new seq.motif.parallel.gui.ParallelUI(seq, ui, (seq.motif.parallel.Parallel)motif[0] );
            }
                
        return mui;
        }

    static JFrame buildFrame(SeqUI sequi)
        {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter()
            {
            public void windowClosing(WindowEvent e)
                {
                if (sequi.doQuit())
                    {
                    System.exit(0);
                    }
                }
            });
        return frame;
        }

    // TESTING
    public static void main(String[] args) throws Exception
        {
        // Set up Menu and FlatLAF
        SeqUI.setupGUI();
        
        // Set up Seq
        Seq seq = new Seq();            // starts timer running
        seq.setupForMIDI(); // SeqUI.class, args, 1, 1);   // sets up MIDI in and out
//        seq.setOut(0, new Out(seq, 0));         // Out 0 points to device 0 in the tuple.  This is too complex.

        // Build GUI
        SeqUI ui = new SeqUI(seq);
        
        Mac.setup(ui);
        
        Motif dSeq = null;
        Motif[] temp = new Motif[1];
        MotifUI ssui = setupInitialMotif(temp, seq, ui);
        dSeq = temp[0];
                                
        // Build Clip Tree
        seq.setData(dSeq);

        seq.sequi = ui;
        JFrame frame = buildFrame(seq.sequi);
        seq.setFile(null);
        frame.setTitle("Untitled");
        ui.setupMenu(frame);
        ui.addMotifUI(ssui);
        frame.getContentPane().add(ui);
        
        // figure out the right window size
        int minWidth = Prefs.getLastInt("InitialWidth", MIN_INITIAL_WIDTH);
        int minHeight = Prefs.getLastInt("InitialHeight", MIN_INITIAL_HEIGHT);
        frame.pack();
        Dimension size = frame.getSize();
        if (size.width < minWidth) size.width = minWidth;
        if (size.height < minHeight) size.height = minHeight;
        frame.setSize(size);
        frame.setVisible(true);
        addResizeListener(frame);
                
        seq.reset();
        ssui.revise();
        }
        
        
    public static final String SEQ_ABOUT_TEXT = 
        "<html>A Modular and Hierarchical MIDI Sequencer<br>" + 
        "By Sean Luke<br>" + 
        "With Help from Filippo Carnovalini<br>" + 
        "<b><font color='#3498db'>Version 5</font></b>, July 2025<br>" + 
        "https://github.com/eclab/seq</html>";
    }
