/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.modulation;

import seq.engine.*;
import java.util.*;
import java.util.concurrent.*;

public class ModulationClip extends Clip
    {
    private static final long serialVersionUID = 1;

    public static class Node
        {
        Modulation.Child child;
        Clip clip;
        // This the Node's last position in its internal frame of reference.  We want
        // this to keep up with cumulativeRate.
        int lastPos = 0;
        // This is the internal expected position of the node in its frame of reference.  It's
        // computed by adding the current rate into the cumulativeRate each time we are pulsed.
        double cumulativeRate = 0.0;
        public Clip getClip() { return clip; }
        public Modulation.Data getData() { return (Modulation.Data)(child.getData()); }
        public Node(Clip clip, Modulation.Child node) { this.clip = clip; this.child = node;  }
        }
    
	Node node = null;
	boolean done = true;
	
	public boolean isPlaying() { return !done; }
        
    public ModulationClip(Seq seq, Modulation modulation, Clip parent)
        {
        super(seq, modulation, parent);
        rebuild();
        }
    
    public void terminate() 
        { 
        super.terminate();
        if (node != null) node.getClip().terminate();
        }

    public void rebuild(Motif motif)
        {
        if (this.getMotif() == motif)
            {
            rebuild();
            }
        else
            {
            if (node != null)
                {
                if (node.clip != null) 
                    node.clip.rebuild(motif);
                else System.err.println("Warning: ModulationClip node " + node + " has no clip");
                }
            }
        }

    public void rebuild()
        {
        release();
        terminate();

        node = null;
        version = getMotif().getVersion();
        }
        
    public Node getNode()
        {
        if (node == null)
            {
            Modulation modulation = (Modulation)getMotif();
            Modulation.Child _child = modulation.getChildren().get(0);
            if (_child == null) return null;
            
            Modulation.Data data = (Modulation.Data)(_child.getData());
            node = new Node(_child.getMotif().buildClip(this), _child);
            reset(node);
            }
                        
        return node;
        }
          
    public void cut()  
        {
        if (node != null) node.getClip().cut();
        }
        
    public void release()  
        {
        if (node != null) node.getClip().release();
        }
                
    boolean advance(Node node, double rate)
        {
        loadParameterValues(node.clip, node.child);                                     // this is done every time it's advanced
                   
        if (rate == 1.0) return node.clip.advance();
        else
            {
            boolean result = false;
            node.cumulativeRate += rate;
            for( /* Empty */ ; node.lastPos < node.cumulativeRate; node.lastPos++)
                {
                result = result || node.clip.advance();
                }
            return result;
            }
        }

    public void loop()
        {
        super.loop();
        if (node != null) 
        	{
        	node.getClip().terminate();
	        if (node != null) reset(node);
	        }
        }
                
    public void reset()  
        {
        super.reset();
        if (node != null) reset(node);
        }
                
    void reset(Node node)
        {
        node.clip.reset();
        node.lastPos = -1;
        node.cumulativeRate = 0;
        }
        
    public boolean process()
        {
        Modulation modulation = (Modulation) getMotif();

        Node node = getNode();
        if (node == null) return true;	
        
        done = advance(node, getCorrectedValueDouble(node.getData().getRate(), Modulation.Data.MAX_RATE));                                            // done == we have JUST finished playing notes
    	return done;            
        }
   
    public boolean noteOn(int out, int note, double vel) 
        {
            Node node = getNode();
            if (node == null) return false;
            
            Modulation.Data data = node.getData();
            if (data.getOut() != Modulation.Data.DISABLED)
                {
                out = data.getOut();
                }
            note += getCorrectedValueInt(data.getTranspose(), Modulation.Data.MAX_TRANSPOSE * 2) - Modulation.Data.MAX_TRANSPOSE;
            note = data.adjustNote(note);
            if (note > 127) note = 127;                 // FIXME: should we instead just not play the note?
            if (note < 0) note = 0;                             // FIXME: should we instead just not play the note?
            vel *= getCorrectedValueDouble(data.getGain(), Modulation.Data.MAX_GAIN);
            if (vel > 127) vel = 127;                   // FIXME: should we check for vel = 0?

        return super.noteOn(out, note, vel);
        }
        
    public boolean noteOff(int out, int note, double vel) 
        {
            Node node = getNode();
            if (node == null) return false;

            Modulation.Data data = node.getData();
            if (data.getOut() != Modulation.Data.DISABLED)
                {
                out = data.getOut();
                }
            note += getCorrectedValueInt(data.getTranspose(), Modulation.Data.MAX_TRANSPOSE * 2) - Modulation.Data.MAX_TRANSPOSE;
            note = data.adjustNote(note);
            if (note > 127) note = 127;                 // FIXME: should we instead just not play the note?
            if (note < 0) note = 0;                             // FIXME: should we instead just not play the note?

        return super.noteOff(out, note, vel);
        }
        
    public void scheduleNoteOff(int out, int note, double vel, int time) 
        {
            Node node = getNode();
            if (node == null) return;

            Modulation.Data data = node.getData();
            if (data.getOut() != Modulation.Data.DISABLED)
                {
                out = data.getOut();
                }
            note += getCorrectedValueInt(data.getTranspose(), Modulation.Data.MAX_TRANSPOSE * 2) - Modulation.Data.MAX_TRANSPOSE;
            note = data.adjustNote(note);
            if (note > 127) note = 127;                 // FIXME: should we instead just not play the note?
            if (note < 0) note = 0;                             // FIXME: should we instead just not play the note?
            super.scheduleNoteOff(out, note, vel, (int)(time / getCorrectedValueDouble(data.getRate())));
        }

    }
