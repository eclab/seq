/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.engine;

import seq.util.*;
import java.util.*;
import javax.sound.midi.*;

/**
   A convenience wrapper for a given input MIDI device in the Seq's Midi.Tuple.
   In has a channel, which can be Midi.Tuple.IN_CHANNEL_OMNI (0).  
   When incoming messages arrive they are stored in the in, and can
   then can be retrieved and cleared via the getMessages() method, which
   is threadsafe.
**/

public class In implements Receiver
    {
    private static final long serialVersionUID = 1;

    Seq seq;
    int index;
    // Returned by default when there are no messages, rather than building new empty arrays each time.
    static final MidiMessage[] EMPTY = new MidiMessage[0];
    // Message mailbox
    ArrayList<MidiMessage> messages = new ArrayList<>();
    // The tuple's device wrapper
    Midi.MidiDeviceWrapper wrapper;
    // The tuple's channel
    int channel;
    MidiMessage[] latestMessages = EMPTY;
    
    /** WARNING this will throw an exception if the tuple has not yet been set up */
    public In(Seq seq, int index)
        {
        this.seq = seq;
        this.index = index;
        wrapper = seq.tuple.inWrap[index];
        channel = seq.tuple.inChannel[index];
        // if (wrapper != null) wrapper.addToTransmitter(this);
        }

    public Midi.MidiDeviceWrapper getWrapper() { return wrapper; }
    
    // Also removes the receiver from the wrapper just in case
    public void setWrapper(Midi.MidiDeviceWrapper wrapper) 
        { 
        if (this.wrapper != null) this.wrapper.removeFromTransmitter(this); 
        this.wrapper = wrapper; 
        //if (wrapper != null) wrapper.addToTransmitter(this); 
        }
        
    /** Returns the channel */
    public int getChannel() { return channel; }
        
        
    
    public void setName(String val) { seq.tuple.inName[index] = val; }
    public String getName() { return seq.tuple.inName[index]; }

    /** Closes the In (required because In is a Receiver, but this method does nothing) */
    public void close() { }     // we don't care
        
    /** Receives the given message and adds it to the mailbox. */
    public void send(MidiMessage message, long timestamp)
        {
        synchronized(this)
            {
            messages.add(message);
            }
        if (!Clip.isNoteOff(message)) seq.fireMIDIIn();
        }
        
    /** Returns all current messages in the mailbox.  The mailbox will be cleared and
        updated each time the sequencer steps. */
    public MidiMessage[] getMessages()
        {
        return latestMessages;
        }
    
    // Pulls all current messages in the mailbox into latestMessages, then clears the mailbox.
    void pullMessages()
        {
        synchronized(this)
            {
            if (messages.isEmpty()) 
                {
                latestMessages = EMPTY;
                }
            latestMessages = ((MidiMessage[])(messages.toArray(EMPTY)));
/*
            if (latestMessages.length > 0) 
                {
                seq.getOut(0).sendMIDI(latestMessages[0]);
                }
*/
            messages.clear();
            }
        }
    
    public String toString()
        {
        if (wrapper == null) return "None";
        if (getName() != null && getName().trim().length() > 0)
            {
            return (getName().trim());  // channel == 0 ? "O" : ("Ch " + channel)) + " " + getName().trim();
            }
        return (channel == 0 ? "O" : ("Ch " + channel)) + " " + wrapper.toString();
        //return ("<html><font size='-2'>" + (channel == 0 ? "O" : ("Channel " + channel)) + "<br>" + wrapper.toString() + "</font></html>");
        }
    }
