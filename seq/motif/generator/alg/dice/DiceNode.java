/* 
   Copyright 2026 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.generator.alg.dice;

import seq.engine.*;
import seq.motif.blank.*;
import seq.motif.generator.*;
import seq.util.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;


public class DiceNode extends AlgorithmNode
    {
    private static final long serialVersionUID = 1;
    
    public static final int RATE_MULTIPLIER = 2;
    public static final String[] KEYS = { "C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B" };

    class Note implements Comparable<Note>
        {
        public int pitch;
        public int velocity;
        public int onset;
        public int interval;
        public String toString() { return "Note " + KEYS[pitch % 12] + (pitch / 12) + "(" + pitch + "): " + velocity + " at " + onset + " for " + interval; }
        public Note(int pitch, int velocity, int onset, int interval) { this.pitch = pitch; this.velocity = velocity; this.onset = onset; this.interval = interval; }
        public int compareTo(Note o)
            {
            if (onset < o.onset) return -1;
            if (onset > o.onset) return 1;
            return 0;
            }
        }

    LinkedList<Note> notes = new LinkedList<>();
    int latestNoteOff = 0;
        
    boolean reloaded = false;
        
    public void reset()
        {
        reloaded = false;
        }
    
    public void reloadNotes()
        {
        Dice dice = (Dice)getAlgorithm();
        Random rand = getRandom();

        notes.clear();
        int[][][] indices = dice.getIndices();
        int[][][] treble = dice.getTreble();
        int[][][] bass = dice.getBass();
        int velocity = dice.getVelocity();
        int rate = dice.getRate() * RATE_MULTIPLIER;
        int treblePos = 0;
        int bassPos = 0;
        int transpose = dice.getTranspose();
        ArrayList<Note> m = new ArrayList<>();
        
        for(int song = 0; song < indices.length; song++)
            {
            for(int iteration = 0; iteration < 2; iteration++)
                {
                for(int measure = 0; measure < indices[song].length; measure++)
                    {
                    // Compute next fragment
                    int fragment = 0;
                    if (song == 0 && iteration == 1 && measure == indices[song].length - 1)
                        {
                        fragment = treble.length - 1;           // should be 176
                        }
                    else
                        {
                        int num = rand.nextInt(indices[song][measure].length) / 2 +
                            rand.nextInt(indices[song][measure].length) / 2;
                        fragment = indices[song][measure][num] - 1;                     // offset
                        }
                    //System.err.println("Measure " + (measure + 1) + " Fragment " + (fragment + 1));
                                        
                    bassPos = treblePos;                    // just to make sure we're in sync
                                        
                    // update treble
                    for(int chord = 0; chord < treble[fragment].length; chord++)
                        {
                        int length = treble[fragment][chord][treble[fragment][chord].length - 1];
                        int interval = (length == 16 ? rate / 2 : (length == 8 ? rate : rate * 2));

                        for(int note = 0; note < treble[fragment][chord].length - 1; note++)
                            {
                            Note n = null;
                            int pitch = Math.max(Math.min(treble[fragment][chord][note] + transpose - 24, 127), 0);
                            m.add(n = new Note(pitch, velocity, treblePos, interval));
                            //System.err.println(n);
                            if (latestNoteOff < treblePos + interval) latestNoteOff = treblePos + interval;
                            }

                        treblePos += interval;
                        } 

                    // update bass
                    for(int chord = 0; chord < bass[fragment].length; chord++)
                        {
                        int length = bass[fragment][chord][bass[fragment][chord].length - 1];
                        int interval = (length == 16 ? rate / 2 : (length == 8 ? rate : rate * 2));

                        for(int note = 0; note < bass[fragment][chord].length - 1; note++)
                            {
                            Note n = null;
                            int pitch = Math.max(Math.min(bass[fragment][chord][note] + transpose - 24, 127), 0);
                            m.add(n = new Note(pitch, velocity, bassPos, interval));
                            //System.err.println(n);
                            if (latestNoteOff < bassPos + interval) latestNoteOff = bassPos + interval;
                            }

                        bassPos += interval;
                        }
                    Collections.sort(m);
                    notes.addAll(m);
                    m.clear();
                    }
                }
            }
        }
        
    public DiceNode(Seq seq, Generator generator, GeneratorClip clip, Algorithm algorithm)
        {
        super(seq, generator, clip, algorithm);
        reset();
        }
        
    public DiceNode copy()
        {
        DiceNode lmn = (DiceNode)(super.copy());
        return lmn;
        }
    
    public void loop()
        {
        reloadNotes();
        }
        
    public boolean process(ArrayList<GeneratorClip.Note> incomingNotes)
        {
        if (!reloaded)
            {
            reloadNotes();
            reloaded = true;
            }
                
        Dice dice = (Dice)getAlgorithm();
        
        int pos = getPosition();
        int releaseVelocity = 64;
        
        if (notes.isEmpty())
            {
            return true;
            }
        else
            {
            while(!notes.isEmpty() && notes.getFirst().onset <= pos)
                {
                Note note = notes.removeFirst();
                note(note.pitch + dice.getTranspose(), note.velocity, note.interval, releaseVelocity);
                }
                
            return pos >= latestNoteOff;
            }
        }
    }
        
        
        
        
        
        
