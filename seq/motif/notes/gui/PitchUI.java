/* 
   Copyright 2025 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.notes.gui;

import seq.engine.*;
import seq.gui.*;
import seq.motif.notes.*;
import seq.util.*;
import seq.gui.Theme;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.locks.*;
import java.awt.geom.*;
import java.util.*;

public class PitchUI extends JLayeredPane
    {
    // Which pitches in the octave are black notes?
    public static final boolean BLACK_KEYS[] = { false, true, false, true, false, false, true, false, true, false, true, false };
    // My background color if I am a black note pitch
    public static final Color BLACK_KEY_BACKGROUND_COLOR = Theme.isDark()? Theme.GRAY_40 : new Color(210, 210, 210);
    // My background color if I am a white note pitch
    public static final Color WHITE_KEY_BACKGROUND_COLOR = Theme.isDark()? Theme.GRAY_50 : new Color(220, 220, 220);
    // The color for vertical lines representing 16th notes
    public static final Color SIXTEENTH_NOTE_COLOR = Theme.isDark()? Theme.SOFT_BLUE_25 :new Color(200, 200, 200);
    // The color for vertical lines representing beats
    public static final Color BEAT_COLOR = Theme.isDark()? Theme.SOFT_BLUE_30 : new Color(180, 180, 180); // new Color(128, 128, 128);
    // The color for vertical lines representing bars
    public static final Color BAR_COLOR = Theme.isDark()? Theme.SOFT_BLUE : new Color(64, 64, 220);
    // The color for the rubber band
    public static final Color RUBBER_BAND_COLOR = Theme.isDark()? Theme.NEON_GREEN : new Color(32, 64, 32);
    // The Stroke for the rubber band
    public static final Stroke RUBBER_BAND_STROKE = new BasicStroke(3.0f);
    // The color for the Start marker
    public static final Color START_COLOR = Theme.isDark()? Theme.RED : new Color(0, 160, 160);
    // The color for the End marker
    public static final Color END_COLOR = Theme.isDark()? Theme.RED : new Color(180, 0, 180);
 
    // The parent GridUI of this PitchUI
    GridUI gridui;
    // The pitch of this PitchUI
    int pitch;
    // Is this PitchUI a black note?
    boolean black;
    // All noteuis in the PitchUI.  Note that these may not be in any order.
    ArrayList<NoteUI> noteuis = new ArrayList<>();
    // All recorded noteuis in the PitchUI.  Note that these may not be in any order.
    ArrayList<NoteUI> recordeduis = new ArrayList<>();

    
    // The rubber band goes from (startx, starty) to (endx, endy).
    // If these values are -1, they have not been set and should not be drawn.
    int rubberBandStartX = -1;
    int rubberBandEndX = -1;
    int rubberBandStartY = -1;
    int rubberBandEndY = -1;
        
    /** Sets the top line of the rubber band */
    public void setRubberBandTop(int startX, int endX, int startY)
        {
        rubberBandStartX = startX;
        rubberBandEndX = endX;
        rubberBandStartY = startY;
        }
    
    /** Sets the bottom line of the rubber band */
    public void setRubberBandBottom(int startX, int endX, int endY)
        {
        rubberBandStartX = startX;
        rubberBandEndX = endX;
        rubberBandEndY = endY;
        }

    /** Clears the rubber band */
    public void clearRubberBand()
        {
        rubberBandStartX = -1;
        rubberBandEndX = -1;
        rubberBandStartY = -1;
        rubberBandEndY = -1;
        }
    
 
    public static final int DEFAULT_PITCH_HEIGHT = 16;
    // How tall am I?
    static int pitchHeight = DEFAULT_PITCH_HEIGHT;
    // How tall am I?
    public static final int getPitchHeight() { return pitchHeight; }
    public static final void setPitchHeight(int val) 
        { 
        pitchHeight = val;
        Prefs.setLastInt("PitchHeight", val);
        }
        
    static
        {
        pitchHeight = Prefs.getLastInt("PitchHeight", DEFAULT_PITCH_HEIGHT);
        }

    /** Returns the GridUI */
    public GridUI getGridUI() { return gridui; }

    /** Returns the NotesUI */
    public NotesUI getNotesUI() { return gridui.getNotesUI(); }

    /** Returns the Seq */
    public Seq getSeq() { return gridui.getSeq(); }

    /** Returns the pitch */
    public int getPitch() { return pitch; }

    /** Returns true if this is a black note. */
    public boolean isBlack() { return black; }
    
    public PitchUI(GridUI gridui, int pitch)
        {
        this.gridui = gridui;
        this.pitch = pitch;
        this.black = BLACK_KEYS[pitch % 12];
        }

    /** Rebuilds the PitchUI entirely.  This discards all the NoteUIs and creates new ones. */
    public void rebuild(ArrayList<Notes.Note> notes)
        {
        this.noteuis.clear();
        this.recordeduis.clear();
        removeAll();
        for(Notes.Note note : notes)
            {
            addNoteUI(new NoteUI(this, note));
            }
        }
        
    /** Returns the NoteUIs. Note that these may not be in any order. */
    public ArrayList<NoteUI> getNoteUIs() { return noteuis; }
    
    /** Removes the NoteUI from the PitchUI */
    public void removeNoteUI(NoteUI noteui)
        {
        remove(noteui);
        noteuis.remove(noteui);
        }
                
    /** Adds the NoteUI to the PitchUI. The NoteUI must already
        have its recorded bit set.
        This assumes you hold the lock already */
    public void addRecordedNoteUI(NoteUI noteui)
        {
        noteui.reload(noteui.getNote().when, noteui.getNote().length);
        add(noteui, 0);
        recordeduis.add(noteui);
        //repaint();
        }
                
    /** Sorts NoteUIs and also the children of this JComponent */
    public void sortNoteUIs()
        {
        removeAll();
        
        // We have to obtain the lock in order to sort the NoteUIs as their compareTo method
        // does not lock got get the underlying note information.
        Seq seq = gridui.getSeq();
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            Collections.sort(noteuis);
            }
        finally
            {
            lock.unlock();
            }
                
        for(NoteUI noteui : noteuis)
            {
            add(noteui, 0);
            }
        }
        
    /** Adds the NoteUI to the PitchUI */
    public void addNoteUI(NoteUI noteui)
        {
        add(noteui, 0);
        noteuis.add(noteui);
        noteui.setPitchUI(this);
        }
                
    /** Finds and returns the NoteUI for the given Note.  This is O(n) */
    public NoteUI getNoteUIFor(Notes.Note note)
        {
        for(NoteUI noteui : noteuis)
            {
            if (noteui.getNote() == note)
                {
                return noteui;
                }
            }
        return null;
        } 

    /** Recaches note information in all the underlying NoteUIs */
    public void reload()
        {
        // We need to copy the noteuis since they could wind up in a new PitchUI and that changes noteuis so it breaks the iterator
        ArrayList<NoteUI> noteuisCopy = new ArrayList<>(noteuis);
        for(NoteUI noteui : noteuisCopy)
            {
            noteui.reload();
            }
        // This will never happen for the recorded uis however, so we don't need to botehr.
        for(NoteUI recordedui : recordeduis)
            {
            recordedui.reload();
            }
        }

    /** Recaches note information in all the underlying NoteUIs */
    public void reload(HashSet<Notes.Event> events)
        {
        // We need to copy the noteuis since they could wind up in a new PitchUI and that changes noteuis so it breaks the iterator
        ArrayList<NoteUI> noteuisCopy = new ArrayList<>(noteuis);

        for(NoteUI noteui : noteuisCopy)
            {
            if (events.contains(noteui.event))
                {
                noteui.reload();
                } 
            }
        }
    
    public Dimension getPreferredSize()
        {
        return getMinimumSize();
        }

    public Dimension getMinimumSize()
        {
        // The width is the largest current time, in pixels
        return new Dimension(gridui.getPixels(gridui.getMaximumTime()), getPitchHeight());
        }
        
    /** Makes the given notes appear on top of other notes. */
    // Notes are drawn in Z-order from front to back.  So notes added EARLIER to a container component
    // are drawn on top of notes added LATER.
    public void moveToPosition(HashSet<NoteUI> move, boolean onTop)
        {
        ArrayList<NoteUI> top = new ArrayList<>();
        ArrayList<NoteUI> bottom = new ArrayList<>();
        for(NoteUI noteui : noteuis)
            {
            if (move.contains(noteui))
                {
                top.add(noteui);
                }
            else 
                {
                bottom.add(noteui);
                }
            }
        noteuis.clear();
        if (onTop)
            {
            noteuis.addAll(top);
            noteuis.addAll(bottom);
            }
        else        
            {
            noteuis.addAll(bottom);
            noteuis.addAll(top);
            }

        removeAll();
        for(NoteUI noteui : noteuis)
            {
            add(noteui);
            }

        repaint();
        }

    /** Makes the given note appear on top of other notes. */
    // Notes are drawn in Z-order from front to back.  So notes added EARLIER to a container component
    // are drawn on top of notes added LATER.
    public void moveToTop(NoteUI move)
        {
        noteuis.remove(move);
        noteuis.add(0, move);
        
        removeAll();
        for(NoteUI noteui : noteuis)
            {
            add(noteui);
            }
            
        repaint();
        }
        
        
    // The line that separates two white notes with no black note in-between, namely B/C and E/F
    static final Line2D.Double cSeparator = new Line2D.Double(0, getPitchHeight() - 1, Integer.MAX_VALUE, getPitchHeight() - 1);
    // Vertical lines
    static final Line2D.Double vertical = new Line2D.Double(0, 0, 0, getPitchHeight());
    // Ruber band lines
    static final Line2D.Double rubberBand = new Line2D.Double(0, 0, 0, 0);
        
    // Draws vertical bars for a given divisor.  The divisors at present are:
    // 192 / 4                  Quarter notes
    // 192                              Beats
    // 192 * numBeats   Bars
    // You indicate what portion of the drawing (in time) should be done, plus the
    // divisor and the color for the bar, plus the current scale and graphics.
    //
    // This method is called multiple times in paintComponent to paint different divisors.
    // Note that the order matters: 16th notes first if any, then beats if any, then bars if any,
    // because they will overwrite each other.
    void drawVerticalBars(int startWhen, int endWhen, int divisor, Color color, double scale, Graphics2D g)
        {
        if (startWhen % divisor != 0)
            {
            startWhen = (startWhen / divisor) * divisor;
            }
        if (startWhen == 0) startWhen = divisor;
                        
        g.setPaint(color);
        for(int i = startWhen; i < endWhen; i += divisor)
            {
            double _i = i / scale;
            vertical.setLine(_i, 0, _i, getPitchHeight());
            g.draw(vertical);
            }
        }

    protected void paintComponent(Graphics _g)
        {
        Graphics2D g = (Graphics2D) _g;
        Rectangle bounds = g.getClipBounds();
        int start = bounds.x;
        int end = bounds.x + bounds.width;
        
        g.setPaint(black ? BLACK_KEY_BACKGROUND_COLOR : WHITE_KEY_BACKGROUND_COLOR);
        g.fill(bounds);
        
        int mod = pitch % 12;
        
        if (mod == 0)
            {
            if (pitch != 0)         // don't draw the first one
                {
                g.setPaint(BLACK_KEY_BACKGROUND_COLOR);
                cSeparator.setLine(0, getPitchHeight() - 1, Integer.MAX_VALUE, getPitchHeight() - 1);
                g.draw(cSeparator);
                }
            g.setPaint(Color.BLACK);
            }

        else if (mod == 5)              // F
            {
            g.setPaint(BLACK_KEY_BACKGROUND_COLOR);
            cSeparator.setLine(0, getPitchHeight() - 1, Integer.MAX_VALUE, getPitchHeight() - 1);
            g.draw(cSeparator);
            } 
        
        int startWhen = gridui.getTime(start);
        int endWhen = gridui.getTime(end);
        Notes notes = gridui.getNotesUI().getNotes();
        
        // What is the minimum size?
        // Zooming in (more micro) makes the scale go DOWN
        // FIXME this is costly.  Should we push the bars onto the PitchUIs?
        double scale = gridui.getScale();
        int beatsPerBar = 0;
        int endTime = 0;
        int startTime = 0;
        Seq seq = getSeq();
        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            beatsPerBar = seq.getBar();
            endTime = notes.getEnd();
            startTime = notes.getStart();
            }
        finally
            {
            lock.unlock();
            }
                        
        // We draw:
        // 16th Notes           (1/4 of PPQ, 48 ticks)  // 4.0 scale or below
        // Quarter Notes        (1 PPQ, 192 ticks)              // 16.0 scale or below?
        // Bars                         (1 * beatsPerPar PPQ, 192 * beatsPerPar ticks)  // 8.0 * beatsPerBar or below?
        
        int divisor = Seq.PPQ * beatsPerBar;    // bars
        if (scale < 16.0) divisor = Seq.PPQ;    // beats
        if (scale < 4.0) divisor = Seq.PPQ / 4;         // 16th notes
        
        if (scale < 4.0)                // draw 16th notes
            {
            drawVerticalBars(startWhen, endWhen, Seq.PPQ / 4, SIXTEENTH_NOTE_COLOR, scale, g);
            }
                
        if (scale < 16.0)               // draw beats
            {
            drawVerticalBars(startWhen, endWhen, Seq.PPQ, BEAT_COLOR, scale, g);
            }

        // draw bars
        drawVerticalBars(startWhen, endWhen, Seq.PPQ * beatsPerBar, BAR_COLOR, scale, g);

        // draw start
        int startX = gridui.getPixels(startTime);
        if (startX > 0)
            {
            g.setColor(START_COLOR);
            vertical.setLine(startX, 0, startX, getPitchHeight());
            g.draw(vertical);
            }

        // draw end
        int endX = gridui.getPixels(endTime);
        if (endX > 0)
            {
            g.setColor(END_COLOR);
            vertical.setLine(endX, 0, endX, getPitchHeight());
            g.draw(vertical);
            }
        }



    // We override paint so we can draw the PitchUI, which in turn draws all the 
    // NoteUIs, but then we can draw on top of the NoteUIs to do the rubber band
    public void paint(Graphics _g)
        {
        super.paint(_g);
        Graphics2D g = (Graphics2D) _g;
            
        /// Draw Rubber Band
        /// FIXME: if we  broke this out to a separate drawing method we might be able to redraw only the notes that mattered
                
        if (rubberBandStartY != -1)
            {
            g.setPaint(RUBBER_BAND_COLOR);
            // FIXME   Make a custom stroke?  I dunno
            g.setStroke(RUBBER_BAND_STROKE);
            rubberBand.setLine(rubberBandStartX, rubberBandStartY, rubberBandEndX, rubberBandStartY);   
            g.draw(rubberBand);
            }

        if (rubberBandEndY != -1)
            {
            g.setPaint(RUBBER_BAND_COLOR);
            // FIXME   Make a custom stroke?  I dunno
            g.setStroke(RUBBER_BAND_STROKE);
            rubberBand.setLine(rubberBandStartX, rubberBandEndY, rubberBandEndX, rubberBandEndY);           
            g.draw(rubberBand);
            }
        } 
    }
