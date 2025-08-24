/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.stepsequence.gui;

import seq.motif.stepsequence.*;
import seq.engine.*;
import seq.gui.*;
import java.awt.*;
import javax.swing.border.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.concurrent.locks.*;
import java.util.*;
import com.formdev.flatlaf.*;

public class StepSequenceUI extends MotifUI
    {
    StepSequence ss;
    StepSequenceInspector ssInspector;
    TrackInspector trackInspector;
    StepInspector stepInspector;
    
    JPanel inspectorPane;
    JPanel inspectorPane2; 
    JPanel ssOuter;
    JPanel trackOuter;
    JPanel stepOuter;
    TitledBorder ssBorder;
    TitledBorder trackBorder;
    TitledBorder stepBorder;
    Ruler ruler;
    
    int selectedTrackNum = 0;
    int selectedStepNum = 0;
    boolean mouseDown = false;
        
    // at Ceil(1.5) we have
    // 12 
    public final static int MIN_ZOOM = 12;
    public final static int MAX_ZOOM = 315;
    int zoom = 27;
    
    public int getZoom() { return zoom; }

    boolean allDirty = false;
    public void setAllDirty(boolean val) { allDirty = val; }
    public boolean isAllDirty() { return allDirty; }
    public void setDirty(int track, int step, boolean val) { getTrack(track).getStep(step).setDirty(true); }
    
    int[] lastSteps = null;
    int[] currentSteps = null;
    
    public int getTrackWidth()
        {
        return getTrack(0).getWidth();
        }
        
    public int getTrackX()
        {
        return primaryScroll.getViewport().getViewPosition().x;
        }

    protected void paintComponent(Graphics g)
        {
        if (isAllDirty())
            {
            super.paintComponent(g);
            }
        else
            {
            // do NOT paint component, it'll wipe the component entirely
            }
        }
        
    public void updateSizes()
        {
        // First get all the sizes
        int[] numSteps;
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try 
            { 
            numSteps = new int[ss.getNumTracks()];
            for(int i = 0; i < numSteps.length; i++)
                {
                numSteps[i] = ss.getNumSteps(i);
                }
            }
        finally { lock.unlock(); }
        
        // What is the largest?
        int max = numSteps[0];
        for(int i = 1; i < numSteps.length; i++)
            {
            max = Math.max(max, numSteps[i]);
            }
        
        // Multiply by zoom to get the width
        double w = max * getZoom();
        
        // Assign values to everyone
        for(int track = 0; track < numSteps.length; track++)
            {
            TrackUI trackui = tracks.get(track);
            for (int step = 0; step < numSteps[track]; step++)
                {
                StepUI stepui = trackui.getStep(step);
                int start = (int) (w / numSteps[track] * step);
                int end = (int) (w / numSteps[track] * (step + 1));
                stepui.setWidth(end - start);
                }
            }
        }
        
    public static final SimpleColorMap stepVelocityMap = new SimpleColorMap(0, 127, new Color(0, 0, 0, 0), Color.RED);

    public static final Border matte = BorderFactory.createMatteBorder(0,0,1,0, Color.BLACK);

    public static final Border matteSelected = BorderFactory.createMatteBorder(0,0,1,0, Color.RED);
        
    ArrayList<TrackUI> tracks = new ArrayList<>();
    Box trackBox;
    JPanel trackBoxOuter;
    public Box trackHeaders;
    TrackHeaderPanel trackHeadersOuter;
                
    public TrackUI getTrack(int trackNum) { return tracks.get(trackNum); }
    public ArrayList<TrackUI> getTracks() { return tracks; }
                
    public void unlearn(int track)
        {
        getTrack(track).getHeader().getTrackLearn().setSelected(false);
        
        // update track inspectors
        int st = getSelectedStepNum();
        if (st >= 0) setSelectedTrackNum(st);
        int tr = getSelectedTrackNum();
        if (tr >= 0) setSelectedTrackNum(tr);
        }

    public int getSelectedTrackNum() { return selectedTrackNum; }
    public void setSelectedTrackNum(int val) 
        {
        for(TrackUI track : tracks)
            track.setSelected(false);
        selectedTrackNum = val;
        tracks.get(selectedTrackNum).setSelected(true);
        setTrackInspector(tracks.get(selectedTrackNum).getInspector());
        if (tracks.get(selectedTrackNum).getSteps().size() <= selectedStepNum)  // uh oh
            {
            setSelectedStepNum(0);
            }
        else setSelectedStepNum(selectedStepNum);
        }
    public int getSelectedStepNum() { return selectedStepNum; }
    public void setSelectedStepNum(int val) 
        { 
        selectedStepNum = val; 
        setStepInspector(tracks.get(selectedTrackNum).getStep(selectedStepNum).getInspector());
        }
        
    public static MotifUI create(Seq seq, SeqUI ui)
        {
        return new StepSequenceUI(seq, ui, new StepSequence(seq));
        }
        
    public static MotifUI create(Seq seq, SeqUI ui, Motif motif)
        {
        return new StepSequenceUI(seq, ui, (StepSequence)motif);
        }
        
    public SeqUI getSeqUI() { return sequi; }
        
    public StepSequenceUI(Seq seq, SeqUI sequi, StepSequence ss)
        {
        super(seq, sequi, ss);
        this.seq = seq;
        this.ss = ss;
        this.sequi = sequi;
        setOpaque(false);
//        setBackground(Color.WHITE);
        //build();
        }
        
    public static ImageIcon getStaticIcon() { return new ImageIcon(MotifUI.class.getResource("icons/stepsequence.png")); }  // don't ask
    public ImageIcon getIcon() { return getStaticIcon(); }
    public static String getType() { return "Step Sequence"; }
        
    public void buildInspectors(JScrollPane scroll)
        {
        // Build the step inspector holder
        stepOuter = new JPanel();
        stepOuter.setLayout(new BorderLayout());
        stepBorder = BorderFactory.createTitledBorder(null, "Step");
        stepOuter.setBorder(stepBorder);
        setStepInspector(null);
                
        // Build the track inspector holder
        trackOuter = new JPanel();
        trackOuter.setLayout(new BorderLayout());
        trackBorder = BorderFactory.createTitledBorder(null, "Track");
        trackOuter.setBorder(trackBorder);
        setTrackInspector(null);

        // Build the step sequence inspector holder
        ssOuter = new JPanel();
        ssOuter.setLayout(new BorderLayout());
        ssBorder = BorderFactory.createTitledBorder(null, "Sequence");
        ssOuter.setBorder(ssBorder);

        // Add the step sequence inspector
        ssInspector = new StepSequenceInspector(seq, ss, this);
        ssOuter.add(ssInspector, BorderLayout.CENTER);
                
        // Fill the panes
        inspectorPane = new JPanel();
        inspectorPane.setLayout(new BorderLayout());
        inspectorPane2 = new JPanel();
        inspectorPane2.setLayout(new BorderLayout());
        inspectorPane.add(inspectorPane2, BorderLayout.CENTER);
        inspectorPane2.add(trackOuter, BorderLayout.NORTH);
        inspectorPane2.add(stepOuter, BorderLayout.CENTER);
        inspectorPane.add(ssOuter, BorderLayout.NORTH);
                
        scroll.setViewportView(inspectorPane);
        
        setSelectedTrackNum(getSelectedTrackNum());
        setSelectedStepNum(getSelectedStepNum());
        }
    
    
          
    public void updateDirty()
        {
        //updating = true;
        StepSequenceClip clip = ((StepSequenceClip)(getDisplayClip()));
        if (clip != null)
            {
            int numTracks = 0;
            ReentrantLock lock = seq.getLock();
            lock.lock();
            try 
                {
                // Determine the current and previous steps
                numTracks = ss.getNumTracks();
                if (currentSteps == null || currentSteps.length != numTracks)
                    {
                    // Gotta rebuild current steps, there are no last steps
                    currentSteps = new int[numTracks];
                    for(int i = 0; i < numTracks; i++)
                        {
                        currentSteps[i] = clip.getCurrentStep(i);
                        }
                    lastSteps = null;               // they're invalid
                    }
                else
                    {
                    // move over the current steps to last steps and reload
                    if (lastSteps == null || lastSteps.length != numTracks)
                        {
                        lastSteps = new int[numTracks];
                        }
                    for(int i = 0; i < numTracks; i++)
                        {
                        lastSteps[i] = currentSteps[i];
                        currentSteps[i] = clip.getCurrentStep(i);
                        }
                    }
                }
            finally { lock.unlock(); }
                                
            // Now, who's dirty?
            if (lastSteps == null)
                setAllDirty(true);
            else
                {
                for(int i = 0; i < numTracks; i++)
                    {
                    if (lastSteps[i] != currentSteps[i]) // it's changed
                        {
                        setDirty(i, lastSteps[i], true);
                        setDirty(i, currentSteps[i], true);
                        }
                    }
                }
            }
        else
            {
            // uhm, invalid clip I guess, mark us as fully dirty
            allDirty = true;
            }
        }
        
    public void buildPrimary(JScrollPane scroll)
        {
        trackBox = new Box(BoxLayout.Y_AXIS)
            {
            public void paint(Graphics _g)
                {
                updateDirty();
                Graphics2D g = (Graphics2D) _g;
                RenderingHints oldHints = g.getRenderingHints();
                // g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                super.paint(_g);
                setAllDirty(false);
                }
            };
//        trackBox.setBackground(Color.YELLOW);
        trackBoxOuter = new JPanel();
        trackBoxOuter.setOpaque(false);
        trackBoxOuter.setLayout(new BorderLayout());
        trackBoxOuter.add(trackBox, BorderLayout.NORTH);
        trackBoxOuter.setBackground(Color.RED);
        trackHeaders = new Box(BoxLayout.Y_AXIS);
        trackHeaders.setBorder(matte);

        for(int i = 0; i < ss.getNumTracks(); i++)
            {
            TrackUI track = new TrackUI(seq, ss, this, i);
            tracks.add(track);
            trackBox.add(track);
            trackHeaders.add(track.getHeader());
            }
        trackHeadersOuter = new TrackHeaderPanel(seq, ss, this);
        trackHeadersOuter.setLayout(new BorderLayout());
        trackHeadersOuter.add(trackHeaders, BorderLayout.NORTH);

        JPanel boxHolder = new JPanel();
        boxHolder.setLayout(new BorderLayout());
        boxHolder.add(trackBoxOuter, BorderLayout.WEST);
        boxHolder.setBackground(Color.BLUE);
        boxHolder.setOpaque(false);
        scroll.setViewportView(boxHolder);
        scroll.setBackground(Color.GREEN);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getViewport().setBackground(Color.RED);
        scroll.setRowHeaderView(trackHeadersOuter);
        
        ruler = new Ruler(seq, ss, this);
        scroll.setColumnHeaderView(ruler);
        }
                
    public JPanel buildConsole()
        {
        PushButton addButton = new PushButton(new StretchIcon(PushButton.class.getResource("icons/plus.png")))
            {
            public void perform()
                {
                doAdd();
                }
            };
        addButton.getButton().setPreferredSize(new Dimension(24, 24));
        addButton.setToolTipText(ADD_BUTTON_TOOLTIP);

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
                
        Box zoomBox = new Box(BoxLayout.X_AXIS);
        zoomBox.add(zoomInButton);
        zoomBox.add(zoomOutButton);
        console.add(zoomBox, BorderLayout.EAST);        

        Box addRemoveBox = new Box(BoxLayout.X_AXIS);
        addRemoveBox.add(addButton);
        addRemoveBox.add(removeButton);
        addRemoveBox.add(copyButton);
        console.add(addRemoveBox, BorderLayout.WEST);   
                
        return console; 
        }
                
    public void doZoomIn()
        {
        if ((int)(Math.ceil(zoom * 1.5)) <= MAX_ZOOM)
            {
            zoom = (int)(Math.ceil(zoom * 1.5));
            updateSizes();
            for(TrackUI track : tracks)
                {
                ArrayList<StepUI> steps = track.getSteps();
                for(StepUI step : steps)
                    {
                    step.revalidate();
                    }
                }
            primaryScroll.repaint();
            }
        }

    public void doZoomOut()
        {
        if ((int)(zoom / 1.5) >= MIN_ZOOM)
            {
            zoom = (int)(zoom / 1.5);
            updateSizes();
            for(TrackUI track : tracks)
                {
                ArrayList<StepUI> steps = track.getSteps();
                for(StepUI step : steps)
                    {
                    step.revalidate();
                    }
                }
            primaryScroll.repaint();
            }
        }
        
    void rebuildTracks()
        {
        // Revise TrackBox and Track Headers
        trackBox.removeAll();
        trackHeaders.removeAll();
        int num = 0;
        for(TrackUI t : tracks)
            {
            t.setTrackNum(num++);
            trackBox.add(t);
            TrackHeader h = t.getHeader();
            h.updateHandle();                       
            trackHeaders.add(h);
            t.updateLength();
            }
        updateSizes();
        getPrimaryScroll().revalidate();
        getPrimaryScroll().repaint();
        }
        
    public void doCopy()
        { 
        TrackUI track = null;
        int where = selectedTrackNum;

        sequi.push();
        seq.getLock().lock();
        try
            {
            // Add to Step Sequencer
            ss.copyTrack(where);
            ss.setTrackName(where + 1, ss.getTrackName(where + 1) + " copy");
            }
        finally
            {
            seq.getLock().unlock();
            }
                        
        track = new TrackUI(seq, ss, StepSequenceUI.this, where);
                
        // Add to Tracks
        tracks.add(where + 1, track);

        rebuildTracks();                

        setTrackInspector(track.getInspector());
        setStepInspector(null);
        StepSequenceClip clip = ((StepSequenceClip)(getDisplayClip()));
        if (clip != null) clip.rebuild();
        setSelectedTrackNum(where + 1);
        }

    public void doAdd() 
        { 
        TrackUI track = null;
        int where = tracks.size(); // selectedTrackNum + 1;

        sequi.push();
        seq.getLock().lock();
        try
            {
            // Add to Step Sequencer
            ss.addTrack(where - 1);
            }
        finally
            {
            seq.getLock().unlock();
            }
                                    
        track = new TrackUI(seq, ss, StepSequenceUI.this, where);
                
        // Add to Tracks
        tracks.add(where, track);
                
        rebuildTracks();                

        setTrackInspector(track.getInspector());
        setStepInspector(null);
        setSelectedTrackNum(where);
        }
                
                
    public void doRemove() 
        { 
        if (tracks.size() <= 1) return;     //leave at least one track
        int where = selectedTrackNum;   
                
        sequi.push();
        // Remove from step sequence
        seq.getLock().lock();
        try
            {
            ss.removeTrack(selectedTrackNum);
            }
        finally
            {
            seq.getLock().unlock();
            }
                
        // Remove from tracks list
        ArrayList<TrackUI> tracks = getTracks();
        tracks.remove(selectedTrackNum);
                
        rebuildTracks();                

        setTrackInspector(null);
        setStepInspector(null);
        
        setSelectedTrackNum(Math.max(selectedTrackNum - 1, 0));
        }
        
    public void doMove(int at, int after)
        {
        TrackUI track = null;
        seq.getLock().lock();
                
        // 1. Move track in model
        try
            {
            ss.moveTrack(at, after);
            }
        finally
            {
            seq.getLock().unlock();
            }
                        
        // Change Tracks list
        if (at > after + 1)
            {
            for(int i = at; i > after + 1; i--)
                {
                int x = i;
                int y = i - 1;
                // 2. Swap the tracks in the track list
                ArrayList<TrackUI> tracks = getTracks();
                TrackUI xTrack = tracks.get(x);
                TrackUI yTrack = tracks.get(y);
                tracks.set(y, xTrack);
                tracks.set(x, yTrack);
                
                // 3. Swap the track numbers in the track UIs
                xTrack.setTrackNum(y);
                yTrack.setTrackNum(x);

                setSelectedTrackNum(y);
                }
            }
        else if (at < after)
            {
            for(int i = at; i < after; i++)
                {
                int x = i;
                int y = i + 1;
                // 2. Swap the tracks in the track list
                ArrayList<TrackUI> tracks = getTracks();
                TrackUI xTrack = tracks.get(x);
                TrackUI yTrack = tracks.get(y);
                tracks.set(y, xTrack);
                tracks.set(x, yTrack);
                
                // 3. Swap the track numbers in the track UIs
                xTrack.setTrackNum(y);
                yTrack.setTrackNum(x);

                setSelectedTrackNum(y);
                }
            } 
                        
        rebuildTracks();                

        if (getTrackInspector()!= null)
            setTrackInspector(getTrackInspector());
        if (getStepInspector() != null)
            setStepInspector(getStepInspector());
        }
                

/*
  void swapTracks(int x, int y)
  {
  // 1. Swap the tracks in the model
  seq.getLock().lock();
                
  // 1. Move track in model
  try
  {
  ss.swapTracks(x, y);
  Clip playing = ss.getDisplayClip();
  if (playing != null) playing.rebuild();
  }
  finally
  {
  seq.getLock().unlock();
  }
                
  // 2. Swap the tracks in the track list
  ArrayList<TrackUI> tracks = getTracks();
  TrackUI xTrack = tracks.get(x);
  TrackUI yTrack = tracks.get(y);
  tracks.set(y, xTrack);
  tracks.set(x, yTrack);
                
  // 3. Swap the track numbers in the track UIs
  xTrack.setTrackNum(y);
  yTrack.setTrackNum(x);
                
  // 4. Swap the tracks in the track box and header
  trackBox.removeAll();
  trackHeaders.removeAll();
  for(TrackUI track : tracks)
  {
  trackBox.add(track);
  trackHeaders.add(track.getHeader());
  }
        
  trackHeaders.revalidate();
  trackHeaders.repaint();
  }
*/
                
    public void redraw(boolean inResponseToStep) 
        { 
        if (inResponseToStep)
            {
            primaryScroll.getViewport().repaint();                  // just repaint the viewport, not the scroll view
            ruler.repaint();
            }
        else 
            {
            super.redraw(inResponseToStep);
            }
        }
    

    public SimpleColorMap getStepVelocityMap() { return stepVelocityMap; }
        
    public StepSequenceInspector getInspector() { return ssInspector; }

    public void setTrackInspector(TrackInspector inspector)
        {
        trackInspector = inspector;
        trackOuter.removeAll();
        if (inspector != null)
            {
            trackOuter.add(inspector, BorderLayout.NORTH);
            trackBorder.setTitle("Track " + (inspector.getTrackNum() + 1)); //  + " " + ss.getTrackName(inspector.getTrackNum()).trim());
            }
        else
            {
            trackBorder.setTitle("Should be a Track Inspector Here...");
            }
        trackOuter.setBorder(null);             // this has to be done or it won't immediately redraw!
        trackOuter.setBorder(trackBorder);
        if (inspector != null) inspector.revise();
        }

    public TrackInspector getTrackInspector() { return trackInspector; }
    public StepInspector getStepInspector() { return stepInspector; }

    public void setStepInspector(StepInspector inspector)
        {
        stepInspector = inspector;
        stepOuter.removeAll();
        if (inspector!=null) 
            {
            stepOuter.add(inspector, BorderLayout.NORTH);
            stepBorder.setTitle("Step " + (inspector.getTrackNum() + 1) + " / " + (inspector.getStepNum() + 1));
            }
        else
            {
            stepBorder.setTitle("Should be a Step Inspector Here...");
            }

        stepOuter.setBorder(null);              // this has to be done or it won't immediately redraw!
        stepOuter.setBorder(stepBorder);
        if (inspector!=null) inspector.revise();
        }

    public void revise()
        {
        getInspector().revise();
        if (trackInspector != null) 
            {
            trackBorder.setTitle("Track " + (trackInspector.getTrackNum() + 1));    //  + " " + ss.getTrackName(inspector.getTrackNum()).trim());
            trackOuter.setBorder(null);             // this has to be done or it won't immediately redraw!
            trackOuter.setBorder(trackBorder);
            trackInspector.revise();
            }
        if (stepInspector != null) 
            {
            stepBorder.setTitle("Step " + (trackInspector.getTrackNum() + 1) + " / " + (stepInspector.getStepNum() + 1));
            stepOuter.setBorder(null);              // this has to be done or it won't immediately redraw!
            stepOuter.setBorder(stepBorder);
            stepInspector.revise();
            }
        }
        
    public static void main(String[] args) throws Exception
        {
        // Set up FlatLaf
        FlatLightLaf.setup();
                
        // Set up Seq
        Seq seq = new Seq();            // starts timer running
        seq.setupForMIDI(StepSequenceClip.class, args, 0, 2);   // sets up MIDI in and out
        seq.setLooping(true);
        
        // Set up our module structure
        StepSequence dSeq = new StepSequence(seq, 2, 16);
        seq.setOut(0, new Out(seq, 0));         // Out 0 points to device 0 in the tuple.  This is too complex.
        seq.setOut(1, new Out(seq, 1));         // Out 0 points to device 0 in the tuple.  This is too complex.
        
        // Specify notes
        dSeq.setTrackNote(0, 60);
        dSeq.setTrackNote(1, 75);
        dSeq.setTrackOut(0, 0);
        dSeq.setTrackOut(1, 1);
        dSeq.setDefaultSwing(0.33);
        
        // Load the StepSequence with some data
        dSeq.setVelocity(0, 0, 1 * 8);
        dSeq.setVelocity(0, 4, 5 * 8);
        dSeq.setVelocity(0, 8, 9 * 8);
        dSeq.setVelocity(0, 12, 13 * 8);
        dSeq.setVelocity(1, 1, 2 * 8);
        dSeq.setVelocity(1, 2, 3 * 8);
        dSeq.setVelocity(1, 3, 4 * 8);
        dSeq.setVelocity(1, 5, 6 * 8);
        dSeq.setVelocity(1, 7, 8 * 8);
        dSeq.setVelocity(1, 9, 10 * 8);
        dSeq.setVelocity(1, 10, 11 * 8);
        dSeq.setVelocity(1, 15, 127);
        
        // Build Clip Tree
        seq.setData(dSeq);

        seq.setBPM(128);

        // Build GUI
        SeqUI ui = new SeqUI(seq);

        StepSequenceUI ssui = new StepSequenceUI(seq, ui, dSeq);
        seq.sequi = ui;
        ui.addMotifUI(ssui);
        JFrame frame = new JFrame();
        frame.getContentPane().add(ui);
        frame.pack();
        frame.setVisible(true);

        seq.reset();
        ssui.revise();
    
        //Toolkit.getDefaultToolkit().getSystemEventQueue().push(new MyEventQueue());
        
        seq.play();
        
        seq.waitUntilStopped();         // we're looping so this will never exit
        }






    /*** Tooltips ***/
        
    static final String ADD_BUTTON_TOOLTIP = "<html><b>Add Track</b><br>" +
        "Adds a new track to the bottom of the track list.</html>";
        
    static final String REMOVE_BUTTON_TOOLTIP = "<html><b>Remove Track</b><br>" +
        "Removes the selected track.<br><br>" +
        "The selected track has a red track number and name, and a blue selected step.</html>";
        
    static final String COPY_BUTTON_TOOLTIP = "<html><b>Copy Track</b><br>" +
        "Duplicates the selected track, inserting the new one just under it.<br><br>" +
        "The selected track has a red track number and name, and a blue selected step.</html>";
                
    static final String ZOOM_IN_BUTTON_TOOLTIP = "<html><b>Zoom In</b><br>" +
        "Magnifies the view of the step sequence (in time).</html>";
        
    static final String ZOOM_OUT_BUTTON_TOOLTIP = "<html><b>Zoom Out</b><br>" +
        "Demagnifies the view of the step sequence (in time).</html>";
        
    }
