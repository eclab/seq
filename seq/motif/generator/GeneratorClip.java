/* 
   Copyright 2025 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.generator;

import seq.engine.*;
import seq.util.*;
import java.util.*;
import java.util.concurrent.*;

public class GeneratorClip extends Clip
    {
    private static final long serialVersionUID = 1;
    
    // Our child, if any
    Clip clip;
    AlgorithmNode node = null;
    ArrayList<Note> notes = new ArrayList<>();
    Heap noteQueue = new Heap();
    

    // Note is used both to store incoming Note Off messages stored the Heap, and
    // also to indicated notes being played by the generator.  So not all four
    // of the variables below (pitch, velocity, id, out) are used 
    public static class Note implements Comparable
        {
        int pitch;
        int velocity;
        int id;
        boolean on;

        // this version generates its own id
        public Note(int pitch, int velocity, boolean on)
            {
            this.pitch = pitch;
            this.velocity = velocity;
            this.on = on;
            id = noteID++;
            }
            
        public Note(int pitch, int velocity, int id, boolean on)
            {
            this.pitch = pitch;
            this.velocity = velocity;
            this.id = id;
            this.on = on;
            }
        
        public void setID(int val)
            {
            id = val;
            }
            
        public int compareTo(Object obj)
            {
            if (obj == null) return -1;
            if (!(obj instanceof Note)) return -1;
            Note note = (Note)obj;
            return pitch < note.pitch ? -1 : (pitch > note.pitch ? 1 : 0);
            }
                
        public String toString()
            {
            return "GeneratorClip.Note pitch " + pitch + " vel " + velocity + " id " + id;
            }
        }


    public GeneratorClip(Seq seq, Generator generator, Clip parent)
        {
        super(seq, generator, parent);
        rebuild();
        }
        
    public Seq getSeq() { return seq; }
    
    public void rebuild(Motif motif)
        {
        if (this.getMotif() == motif)
            {
            rebuild();
            }
        }
                
    public void rebuild() 
        { 
        if (version < getMotif().getVersion())
            {
            buildClip();
            version = getMotif().getVersion();
            }
        }
    
    void buildClip()
        {
        Generator generator = (Generator)getMotif();
        ArrayList<Motif.Child> children = generator.getChildren();
        if (children.size() > 0)
            {
            Motif.Child child = children.get(0);
            clip = child.getMotif().buildClip(this);
            }
        }
        
    public Clip getChild()
        {
        if (clip == null)
            {
            buildClip();
            }
        return clip;
        }
    
    public void reset()
        {
        super.reset();
        Generator generator = (Generator)getMotif();
                
        if (clip == null)
            {
            buildClip();
            }
            
        if (node == null)
            {
            node = generator.getAlgorithm().buildNode(seq, this);
            }
        node.reset();

        loadRandomValue(clip, generator.getChildren().get(0));
        clip.reset();
        }
        
    public void loop()
        {
        super.loop();
        Generator generator = (Generator)getMotif();
                
        if (clip == null)
            {
            buildClip();
            }

        if (node == null)
            {
            node = generator.getAlgorithm().buildNode(seq, this);
            }
        node.loop();

        loadRandomValue(clip, generator.getChildren().get(0));
        clip.loop();                                            // FIXME should this be clip.reset()?
        }
        
    public void terminate()
        {
        super.terminate();
        Generator generator = (Generator)getMotif();

        if (clip == null)
            {
            buildClip();
            }
                        
        if (node == null)
            {
            node = generator.getAlgorithm().buildNode(seq, this);
            }
        node.release();
        node.terminate();

        clip.release();
        clip.terminate();
        processNoteOffs(true, true);
        notes.clear();
        }
    
    public void cut() 
        {
        super.cut();
        Generator generator = (Generator)getMotif();

        if (clip == null)
            {
            buildClip();
            }
                        
        if (node == null)
            {
            node = generator.getAlgorithm().buildNode(seq, this);
            }
        node.cut();

        clip.cut();
        }
    

    public void release() 
        { 
        super.release();
        Generator generator = (Generator)getMotif();

        if (clip == null)
            {
            buildClip();
            }
                        
        if (node == null)
            {
            node = generator.getAlgorithm().buildNode(seq, this);
            }
        node.release();

        clip.release();
        }
    
    public void noteOn(int out, int note, double vel, int id) 
        {
        Generator generator = (Generator)getMotif();
        if (generator.isOmni() || out == generator.getOut())
            {
            if (node == null)
                {
                node = generator.getAlgorithm().buildNode(seq, this);
                }
            notes.add(new Note(note, (int)vel, id, true));
            }
        
        if (generator.isPass())
            {
            super.noteOn(out, note, vel, id);
            }
        }
        
    public void passNoteOff(int out, int note, double vel, int id)
        {
        super.noteOn(out, note, vel, id);
        }
        
    public void noteOff(int out, int note, double vel, int id) 
        {
        Generator generator = (Generator)getMotif();
        if (generator.isOmni() || out == generator.getOut())
            {
            if (node == null)
                {
                node = generator.getAlgorithm().buildNode(seq, this);
                }
            notes.add(new Note(note, (int)vel, id, false));
            }
        
        if (generator.isPass())
            {
            super.noteOff(out, note, vel, id);
            }
        }
        
    public void scheduleNoteOff(int out, int note, double vel, int time, int id) 
        {
        Generator generator = (Generator)getMotif();
        if (generator.isOmni() || out == generator.getOut())
            {
            noteQueue.add(new Note(note, (int)vel, id, false), Integer.valueOf(time));
            }
        
        if (generator.isPass())
            {
            super.scheduleNoteOff(out, note, vel, time, id);
            }
        }        

    public int scheduleNoteOn(int out, int note, double vel, int time) 
        {
        Generator generator = (Generator)getMotif();
        int id = -1;
        if (generator.isPass())
            {
            id = super.scheduleNoteOn(out, note, vel, time);
            }
        
        if (id == -1) id = noteID++;

        if (generator.isOmni() || out == generator.getOut())
            {
            noteQueue.add(new Note(note, (int)vel, id, true), Integer.valueOf(time));
            }
        return id;
        }        


    void processNoteOffs(boolean all, boolean noteOffsOnly)
        {
        int time = seq.getTime();
        while(true)
            {
            Integer i = (Integer)(noteQueue.getMinKey());
            if (i == null) return;
            if (all || time >= (i.longValue() / Integer.MAX_VALUE))
                {
                Note note = (Note)(noteQueue.extractMin());
                if (note.on)
                    {
                    if (!noteOffsOnly) notes.add(note);
                    }
                else 
                    {
                    notes.add(note);
                    }
                }
            else break;
            }       
        }

    public void resetAlgorithmNode() 
        { 
        if (node != null)
            {
            node.release();
            node.terminate();               // should I do this?
            }
                        
        node = null; 
        }

    public boolean process()
        {
        Generator generator = (Generator)getMotif();

        notes.clear();
                
        if (clip != null)
            {
            buildClip();
            }

        processNoteOffs(false, false);
                
        loadParameterValues(clip, generator.getChildren().get(0));
        boolean clipdone = clip.advance();
        
        if (node == null)
            {
            node = generator.getAlgorithm().buildNode(seq, this);
            }
        boolean nodedone = node.process(notes);
                
        if (clipdone || nodedone) return true;
        else if (getPosition() >= generator.getEnd() - 1) return true;
        else return false;
        }
    }




