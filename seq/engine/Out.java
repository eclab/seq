/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.engine;

import seq.util.*;
import javax.sound.midi.*;

/**
   A convenience wrapper for a given output MIDI device in the Seq's Midi.Tuple.
   Out sends to the channel coresponding to the tuple, which is a value 1 ... 16.  
   Sending is is threadsafe.
        
   <p>Out can be globally transposed and have its gain modified.   You can specify
   whether Out sends polyphonic or channel aftertouch.
**/


public class Out
    {
    private static final long serialVersionUID = 1;

    // The Seq
    Seq seq;
    // Which Out am I?  This also corresponds to which slot in the Midi.Tuple I am writing to
    int index;
    // Transpose amount
    int transpose = 0;
    // Gain amount
    double gain = 1.0;
    // Aftertouch is polyphonic
    boolean polyphonicAftertouch = false;
        
    public static final int CHANNEL_AFTERTOUCH = -1;
    
    public void setSeq(Seq val) { seq = val; }
    
    public Out(Seq seq, int index)
        {
        this.seq = seq;
        this.index = index;

        sysex(new byte[] { (byte)0xF0, 0x00, 0x20, 0x29, 0x02, 0x0d, 0x00, 0x7F, (byte)0xF7 });        
        }
    
    public void setName(String val) { seq.tuple.outName[index] = val; }
    public String getName() { return seq.tuple.outName[index]; }
    
    public Midi.MidiDeviceWrapper getWrapper() 
    	{ 
    	Midi.Tuple tuple = seq.tuple;
		return tuple.outWrap[index]; 
		}
    
    public String toString()
        {
        //MidiEmitter emitter = seq.emitter;
        //if (emitter != null) return emitter.toString();
        Midi.Tuple tuple = seq.tuple;
        if (tuple == null) return "Tuple not set up?";
        Midi.MidiDeviceWrapper wrapper = getWrapper();
        if (wrapper == null) return "None";
        if (getName() != null && getName().trim().length() > 0)
            {
            return (getName().trim());  // "Ch " + tuple.outChannel[index] + " " + getName().trim());
            }
        return ("Ch " + tuple.outChannel[index] + " " + wrapper.toString());
        //return ("<html><font size='-2'>Channel " + tuple.outChannel[index] + "<br>" + wrapper.toString() + "</font></html>");
        }
    
    ///// GETTERS AND SETTERS
    
    public int getTranspose() { return transpose; }
    public void setTranspose(int val) { transpose = val; }
    public double getGain() { return gain; }
    public void setGain(double val) { gain = val; }
    public boolean isPolyphonicAftertouch() { return polyphonicAftertouch; }
    public void setPolyphonicAftertouch(boolean val) { polyphonicAftertouch = val; }
                
                
    ///// TRANSPOSING AND CHANGING GAIN
        
    // Modify a value according to the current transposition
    int transpose(int val)
        {
        // we have to test for bounds no matter what because conversion modules may have pushed out of bounds
        val = val + transpose;
        if (val > 127) 
            return 127;
        if (val < 0) return 0;
        return val;
        }
          
    // Modify a value according to the current gain      
    int gain(double val)
        {
        // we have to test for bounds no matter what because conversion modules may have pushed out of bounds
        val = val * gain + 0.5;
        if (val > 127) return 127;
        if (val < 0) return 0;                  // shouldn't happen unless there's a serious error
        return (int)val;
        }
                
                
                
    ///// SENDING MIDI
        
        
    boolean sendMIDI(MidiMessage message)
        {
        Receiver receiver = null;
        //Receiver receiver = seq.emitter;
        Midi.Tuple tuple = seq.tuple;
        if (receiver == null)
            {
            if (tuple == null) return false;
            Midi.MidiDeviceWrapper wrapper = getWrapper();
            if (wrapper == null) return false;
            receiver = wrapper.getReceiver();
            }
        receiver.send(message, -1L); 
        javax.sound.midi.Track[] tracks = seq.getTracks();
        if (tracks != null)
            {
            if (tracks[1] != null)  // it's multi
                {
                tracks[index].add(new javax.sound.midi.MidiEvent(message, seq.getTime()));
                seq.setValidTrack(index, true);
                }
            else
                {
                tracks[0].add(new javax.sound.midi.MidiEvent(message, seq.getTime()));
                }
            }
        return true;
        }
        
    // Send a one-byte message
    boolean send(int command)
        {
        try 
            { 
            return sendMIDI(new ShortMessage(command));
            }
        catch (InvalidMidiDataException ex) { return false; }

/*
  Receiver receiver = seq.emitter;
  Midi.Tuple tuple = seq.tuple;
  if (receiver == null)
  {
  if (tuple == null) return false;
  Midi.MidiDeviceWrapper wrapper = getWrapper();
  if (wrapper == null) return false;
  receiver = wrapper.getReceiver();
  }
  try { receiver.send(new ShortMessage(command), -1L); }
  catch (InvalidMidiDataException ex) { return false; }
  return true;
*/
        }

    // Send a two-byte voiced message
    boolean sendToChannel(int command, int data, int channel)
        {
        return sendToChannel(command, data, 0, channel);		// the 0 is presumably ignored
        /*
        try 
            { 
            return sendMIDI(new ShortMessage(command, channel - 1, data));
            }
        catch (InvalidMidiDataException ex) { return false; }
        */

/*
  Receiver receiver = seq.emitter;
  Midi.Tuple tuple = seq.tuple;
  if (receiver == null)
  {
  if (tuple == null) return false;
  Midi.MidiDeviceWrapper wrapper = getWrapper();
  if (wrapper == null) return false;
  receiver = wrapper.getReceiver();
  }
  try { receiver.send(new ShortMessage(command, channel - 1, data), -1L); } 
  catch (InvalidMidiDataException ex) { return false; }
  return true;
*/
        }

    // Send a three-byte voiced message
    boolean sendToChannel(int command, int data1, int data2, int channel)
        {
        try 
            { 
            return sendMIDI(new ShortMessage(command, channel - 1, data1, data2));
            }
        catch (InvalidMidiDataException ex) { return false; }

/*
  Receiver receiver = seq.emitter;
  Midi.Tuple tuple = seq.tuple;
  if (receiver == null)
  {
  if (tuple == null) return false;
  Midi.MidiDeviceWrapper wrapper = getWrapper();
  if (wrapper == null) return false;
  receiver = wrapper.getReceiver();
  }
  try { receiver.send(new ShortMessage(command, channel - 1, data1, data2), -1L); }
  catch (InvalidMidiDataException ex) { return false; }
  return true;
*/
        }

    // Send a two-byte message
    boolean send(int command, int data)
        {
        Midi.Tuple tuple = seq.tuple;
        return sendToChannel(command, data, tuple.outChannel[index]);
        }

    // Send a three-byte message
    boolean send(int command, int data1, int data2)
        {
        Midi.Tuple tuple = seq.tuple;
        return sendToChannel(command, data1, data2, tuple.outChannel[index]);
        }

    /** Sends a sysex message.   Returns true if the message was successfully sent.  */
    public boolean sysex(byte[] sysex)
        {
        try 
            { 
            return sendMIDI(new SysexMessage(sysex, sysex.length));
            }
        catch (InvalidMidiDataException ex) { return false; }

        /*
          Receiver receiver = seq.emitter;
          Midi.Tuple tuple = seq.tuple;
          if (receiver == null)
          {
          if (tuple == null) return false;
          Midi.MidiDeviceWrapper wrapper = getWrapper();
          if (wrapper == null) return false;
          receiver = wrapper.getReceiver();
          }
          try { receiver.send(new SysexMessage(sysex, sysex.length), -1L); }
          catch (InvalidMidiDataException ex) { return false; }
          return true;
        */
        }
    
    public boolean clockPulse() { return send(ShortMessage.TIMING_CLOCK); }

    public boolean clockStart() { return send(ShortMessage.START); }

    public boolean clockStop() { return send(ShortMessage.STOP); }

    public boolean clockContinue() { return send(ShortMessage.CONTINUE); }
    
    /** Sends a note on.  Note that velocity is expressed as a double,
        but is still a value 0...127 and will be clamped as such.  The note and velocity
        will be modified according to the transpose and gain before emitting the note on
        message. Returns true if the message was successfully sent.  */
    public boolean noteOn(int note, double vel) { return send(ShortMessage.NOTE_ON, transpose(note), gain(vel)); }

    /** Sends a note off.  Note that velocity is expressed as a double,
        but is still a value 0...127 and will be clamped as such.  The note and velocity
        will be modified according to the transpose and gain before emitting the note off
        message. If the velocity ultimately winds up being 64, a noteOn(note, 0) will be sent instead. 
        Returns true if the message was successfully sent.  */
    public boolean noteOff(int note, double vel) 
        {
        int tr = transpose(note);
        int ga = gain(vel);
        if (ga == 64) return send(ShortMessage.NOTE_ON, tr, 0);
        else return send(ShortMessage.NOTE_OFF, tr, ga); 
        }

    /** Sends a note off with default velocity.  The note (but not velocity)
        will be modified according to the transpose before emitting the note off
        message.  Returns true if the message was successfully sent.  */
    public boolean noteOff(int note) { return send(ShortMessage.NOTE_ON, transpose(note), 0); }

    /** Sends a note on to a fixed channel regardless of the Out's channel.  Note that velocity is expressed as a double,
        but is still a value 0...127 and will be clamped as such.  The note and velocity
        will be modified according to the transpose and gain before emitting the note on
        message. Returns true if the message was successfully sent.  */
    public boolean noteOn(int note, double vel, int channel) { return sendToChannel(ShortMessage.NOTE_ON, transpose(note), gain(vel), channel); }

    /** Sends a note off to a fixed channel regardless of the Out's channel.  Note that velocity is expressed as a double,
        but is still a value 0...127 and will be clamped as such.  The note and velocity
        will be modified according to the transpose and gain before emitting the note off
        message. If the velocity ultimately winds up being 64, a noteOn(note, 0) will be sent instead. 
        Returns true if the message was successfully sent.  */
    public boolean noteOff(int note, double vel, int channel) 
        {
        int tr = transpose(note);
        int ga = gain(vel);
        if (ga == 64) return sendToChannel(ShortMessage.NOTE_ON, tr, 0, channel);
        else return sendToChannel(ShortMessage.NOTE_OFF, tr, ga, channel); 
        }

    /** Sends a note off with default velocity to a fixed channel regardless of the Out's channel.  The note (but not velocity)
        will be modified according to the transpose before emitting the note off
        message.  Returns true if the message was successfully sent.  */
    public boolean noteOff(int note, int channel) { return sendToChannel(ShortMessage.NOTE_ON, transpose(note), 0, channel); }

    /** Sends a bend.   Bend goes -8192...8191.  Returns true if the message was successfully sent.  */
    public boolean bend(int val) { val = val + 8192; return send(ShortMessage.PITCH_BEND, val & 127, (val >>> 7) & 127);}

    /** Sends a CC.  Returns true if the message was successfully sent.  */
    public boolean cc(int cc, int val) { return send(ShortMessage.CONTROL_CHANGE, cc, val); }

    /** Sends a bend, associated with a given note (for MPE).   Bend goes -8192...8191.  Returns true if the message was successfully sent.  */
    // for the time being this just does regular bend
    public boolean bend(int note, int val) { return bend(val); }

    /** Sends a CC, associated with a given note (for MPE).  Returns true if the message was successfully sent.  */
    // for the time being this just does regular cc
    public boolean cc(int note, int cc, int val) { return cc(cc, val); }

    /** Sends polyphonic aftertouch.  If the Out is set to send only channel aftertouch, this will
        be modified to channel aftertouch.   Returns true if the message was successfully sent. 
        You can pass in CHANNEL_AFTERTOUCH for the note, and this will force the message to be sent
        as a channel aftertouch message regardless. */
    public boolean aftertouch(int note, int val) 
        {
        if (polyphonicAftertouch && note > CHANNEL_AFTERTOUCH) return send(ShortMessage.POLY_PRESSURE, note, val); 
        else return send(ShortMessage.CHANNEL_PRESSURE, val); 
        }
        
    /** Sends NRPN (MSB+LSB) as four CC messages (99, 98, 6, 32, in that order). 
        Returns true if the message was successfully sent.  */
    public boolean nrpn(int nrpn, int val)  
        {
        if (!cc(99, (nrpn >>> 7))) return false;
        if (!cc(98, (nrpn & 127))) return false;
        if (!cc(6, (val >>> 7))) return false;
        return cc(32, (val & 127));
        }
                
    /** Sends coarse NRPN (MSB only, LSB = 0) as four CC messages (99, 98, 6, 32, in that order). 
        If you send MSB = 42, then the parameter sent will be 42 * 128. 
        Returns true if the message was successfully sent.  */
    public boolean nrpnCoarse(int nrpn, int msb)
        {
        return nrpn(nrpn, msb * 128);
        }

    /** Sends RPN (MSB+LSB) as four CC messages (99, 98, 6, 32, in that order). 
        Returns true if the message was successfully sent.  */
    public boolean rpn(int rpn, int val)  
        {
        if (!cc(101, (rpn >>> 7))) return false;
        if (!cc(100, (rpn & 127))) return false;
        if (!cc(6, (val >>> 7))) return false;
        return cc(32, (val & 127));
        }
    }
