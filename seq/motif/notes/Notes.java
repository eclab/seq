/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.notes;

import seq.engine.*;
import seq.util.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;
import javax.sound.midi.*;
import javax.sound.midi.spi.*;
import java.io.*;
import java.net.*;
import java.awt.*;

public class Notes extends Motif
    {
    private static final long serialVersionUID = 1;

    /** What do MIDI files end with? */
    public static final String MIDI_FILE_EXTENSION = ".mid";
    
    /** The maximum number of permitted event parameters displayed.
        This is here because the current event parameters displayed are
        stored with the Notes object when saved. */
    public static final int NUM_EVENT_PARAMETERS = 4;
    /** A list of note strings */
    public static final String[] NOTES = { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B" };






    //// NOTE TYPES
    //
    //        
    // TYPES are in the following order:
    // 0-127            Note by Pitch
    // 128-255          CC
    // 256-383          Poly Aftertouch by Pitch
    // 384              Channel Aftertouch
    // 385              Pitch Bend
    // 386-16383        [RESERVED] -- future use, 16-bit CC, PC, and sysex perhaps
    // 16384-32767      NRPN
    // 32768-49151      RPN
    
    public static final int TYPE_NOTE = 0;
    public static final int TYPE_CC = 128;
    public static final int TYPE_POLYPHONIC_AFTERTOUCH = 256;
    public static final int TYPE_CHANNEL_AFTERTOUCH = 384;
    public static final int TYPE_PITCH_BEND = 385;
    public static final int TYPE_PC = 386;
    public static final int TYPE_NRPN = 16384;
    public static final int TYPE_RPN = 32768;
    
    /** Creates a new Event for the given type, onset, and value (or pitch) */
    public static Event buildEvent(int type, int when, double value)
        {
        if (!isValidType(type)) return null;
        if (type < TYPE_CC) return new Note(type - TYPE_NOTE, (int)(value * 128), when, 0);
        if (type < TYPE_POLYPHONIC_AFTERTOUCH) return new CC(type - TYPE_CC, (int)(value * 128), when);
        if (type < TYPE_CHANNEL_AFTERTOUCH) return new Aftertouch(type - TYPE_POLYPHONIC_AFTERTOUCH, (int)(value * 128), when);
        if (type == TYPE_CHANNEL_AFTERTOUCH) return new Aftertouch((int)(value * 128), when);
        if (type == TYPE_PITCH_BEND) return new Bend((int)(value * 16384) - 8192, when);
        if (type == TYPE_PC) return new PC((int)(value * 128), when);
        if (type < TYPE_RPN) return new NRPN(type - TYPE_NRPN, (int)(value * 16384), when);
        return new RPN(type - TYPE_RPN, (int)(value * 16384), when);
        }
    
    /** Returns the short name for this type */
    public static String getTypeInitialShort(int type)  
        {
        if (!isValidType(type)) return "ERR";
        if (type < TYPE_CC) return "Note";
        if (type < TYPE_POLYPHONIC_AFTERTOUCH) return "CC";
        if (type < TYPE_CHANNEL_AFTERTOUCH) return "P AT";
        if (type == TYPE_CHANNEL_AFTERTOUCH) return "Ch AT";
        if (type == TYPE_PITCH_BEND) return "Bend";
        if (type == TYPE_PC) return "PC";
        if (type < TYPE_RPN) return "NRPN";
        return "RPN";
        }

    /** Returns the long name for this type */
    public static String getTypeInitial(int type)       
        {
        if (!isValidType(type)) return "<error>";
        if (type < TYPE_CC) return "Note";
        if (type < TYPE_POLYPHONIC_AFTERTOUCH) return "Control Change";
        if (type < TYPE_CHANNEL_AFTERTOUCH) return "Polyphonic Aftertouch";
        if (type == TYPE_CHANNEL_AFTERTOUCH) return "Channel Aftertouch";
        if (type == TYPE_PITCH_BEND) return "Pitch Bend";
        if (type == TYPE_PC) return "Program Change";
        if (type < TYPE_RPN) return "NRPN";
        return "RPN";
        }

    /** Returns, as a string, the parameter for the type to be attached to the Initial. */
    public static String getTypeFinal(int type) 
        {
        if (!isValidType(type)) return "<error>";
        if (type < TYPE_CC) return "" + (type - TYPE_NOTE);
        if (type < TYPE_POLYPHONIC_AFTERTOUCH) return "" + (type - TYPE_CC);
        if (type < TYPE_CHANNEL_AFTERTOUCH) return NOTES[(type - TYPE_POLYPHONIC_AFTERTOUCH) % 12] + ((type - TYPE_POLYPHONIC_AFTERTOUCH) / 12);
        if (type == TYPE_CHANNEL_AFTERTOUCH) return "";
        if (type == TYPE_PITCH_BEND) return "";
        if (type == TYPE_PC) return "";
        if (type < TYPE_RPN) return "" + (type - TYPE_NRPN);
        return "" + (type - TYPE_RPN);
        }

    /** Returns, as a string, the (long) initial and final together. */
    public static String getTypeName(int type)   
        {
        if (!isValidType(type)) return "<error>";

        String header = getTypeInitial(type);
        String footer = getTypeFinal(type);
        
        if (type < TYPE_CC) return header + " " + footer;
        if (type < TYPE_POLYPHONIC_AFTERTOUCH) return header + " " + footer;
        if (type < TYPE_CHANNEL_AFTERTOUCH) return header ; // " " + footer;
        if (type == TYPE_CHANNEL_AFTERTOUCH) return header;
        if (type == TYPE_PITCH_BEND) return header;
        if (type == TYPE_PC) return header;
        if (type < TYPE_RPN) return header + " " + footer;
        return header + " " + footer;
        }
        

    /** Returns TRUE if the given type number is in the valid range. */
    public static boolean isValidType(int type)
        {
        if (type < TYPE_NOTE) return false;
        if (type > TYPE_PC && type < TYPE_NRPN) return false;
        if (type > TYPE_RPN + 16384) return false;
        return true;
        }







    /// EVENTS
    //
    // There are event classes for each event type: Notes, Bend, CC, NRPN, RPN, Aftertouch



    // Used by NRPN and RPN to determine if we need to rewrite the parameter when writing to a sequence
    int lastNRPN = -1;
    boolean wasRPN = false;
    


    public abstract static class Event
        {
        // The longest permissible event length
        public static final int MAX_LENGTH = Seq.PPQ * 256;
        // The onset of the event
        public int when;
        // Is this event selected?  This is stored here even though it's not part of the model
        // because the NoteUI etc. objects where it would more naturally be stored can get
        // destroyed and rebuilt from the Event at any time and need to recover this information.
        public boolean selected;
        
        public Event(int when)
            {
            this.when = when;
            }
            
        public Event(JSONObject obj)
            {
            when = obj.optInt("w", 0);
            }
        
        /** Loads the Event from JSON */
        public static Event load(JSONObject obj) throws JSONException
            {
            String type = obj.getString("t");
            if (type.equals("n"))
                return new Note(obj);
            else if (type.equals("b"))
                return new Bend(obj);
            else if (type.equals("c"))
                return new CC(obj);
            else if (type.equals("a"))
                return new Aftertouch(obj);
            else if (type.equals("p"))
                return new PC(obj);
            else if (type.equals("N"))
                return new NRPN(obj);
            else if (type.equals("R"))
                return new RPN(obj);
            else 
                {
                System.err.println("Notes.Event.load: unknown type " + type);
                return null;
                }
            }
 
        /** Writes the Event to the given MIDI File Track. */
        public abstract void write(Track track, Notes notes) throws InvalidMidiDataException;
                        
        public JSONObject save() throws JSONException
            {
            JSONObject obj = new JSONObject();
            obj.put("w", when);
            return obj;
            }
        
        /** Copies the event. */
        public abstract Event copy();
        
        /** Converts and sets the event's parameter value (or velocity in the case of Notes) from a value [0.0, 1.0). */
        public abstract double getNormalizedValue(boolean log);
        /** Returns the event's parameter value (or velocity in the case of Notes) as a value [0.0, 1.0). */
        public abstract void setNormalizedValue(double value, boolean log);
        /** Returns the event's parameter value. */
        public abstract int getParameter();
        /** Returns the event's type (see NOTE TYPES above) */
        public abstract int getType();
        /** Returns the event's length in time. */
        public int getLength() { return 0; }
        }

    public static class Note extends Event
        {
        // The note pitch
        public int pitch;
        // The note velocity, cannot be 0
        public int velocity;
        // The note release velocity, default is 0x40 (64)
        public int release;
        // Returns the note length, should be > 0
        public int length;
        
        public Note(int pitch, int velocity, int when, int length, int release)
            {
            super(when);
            this.pitch = pitch;
            this.velocity = velocity;
            this.release = release;
            this.length = length;
            }
            
        public Note(int pitch, int velocity, int when, int length)
            {
            super(when);
            this.pitch = pitch;
            this.velocity = velocity;
            this.release = 64;
            this.length = length;
            }
            
        public Note(JSONObject obj)
            {
            super(obj);
            pitch = obj.optInt("p", 0);
            velocity = obj.optInt("v", 64);
            release = obj.optInt("r", 64);
            length = obj.optInt("l", 0);
            }
            
        public Event copy()
            {
            return new Note(pitch, velocity, when, length, release);
            }
            
        public void write(Track track, Notes notes) throws InvalidMidiDataException
            {
            track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, pitch, velocity), when));
            if (release == 64)
                track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, pitch, 0), when + length));
            else
                track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, pitch, release), when + length));                       
            }
                        
        public JSONObject save() throws JSONException
            {
            JSONObject obj = super.save();
            obj.put("t", "n");
            obj.put("p", pitch);
            obj.put("v", velocity);
            obj.put("r", release);
            obj.put("l", length);
            return obj;
            }
        
        public String toString() { return NOTES[pitch % 12] + (pitch / 12) + ":" +  velocity + "[" + when + "-" + (when + length) + "]"; }       // We don't include Length because it would appear in the table
	
        public double getNormalizedValue(boolean log) { return (velocity < 0 ? velocity : velocity / 128.0); }
        public void setNormalizedValue(double value, boolean log) { velocity = (int)(value * 128); }
        public int getParameter() { return pitch; }
        public int getType() { return TYPE_NOTE + pitch; }
        public int getLength() { return length; }
        }
        
    public static class Bend extends Event
        {
        // Bend value from -8192 to +8191
        public int value;
        
        public Bend(int value, int when)
            {
            super(when);
            this.value = value;
            }
        public Bend(JSONObject obj)
            {
            super(obj);
            value = obj.optInt("v", 0);
            }
        public Event copy()
            {
            return new Bend(value, when);
            }
        public void write(Track track, Notes notes) throws InvalidMidiDataException
            {
            int v = value + 8192;
            track.add(new MidiEvent(new ShortMessage(ShortMessage.PITCH_BEND, (v & 127), ((v >>> 7) & 127)), when));
            }
        public JSONObject save() throws JSONException
            {
            JSONObject obj = super.save();
            obj.put("t", "b");
            obj.put("v", value);
            return obj;
            }
        public String toString() { return "Bend:" + value + "[" + when + "]"; }

        /** Translates -8192 ... +8191 to 0.0 ... 1.0 using logs so that small changes
            have much bigger impact.  This is useful for displaying */
        public static double toNormalizedLog(double value) 
            { 
            if (value == 0) return 0.5;
            else if (value > 0) return Math.min(1.0, Math.log(value + 1) / Math.log(2.0) / 13.0) / 2.0 + 0.5;
            else return (Math.max(-1.0, 0 - Math.log((0 - value) + 1) / Math.log(2.0) / 13.0) / 2.0) + 0.5;
            }
                        
        /** Translates  0.0 ... 1.0 to -8192 ... +8191 using logs so that small changes
            have much bigger impact.  This is useful for displaying */
        public double fromNormalizedLog(double value)
            {
            double v = 0.0;
            value = value - 0.5;
            if (value == 0) v = 0.0;
            else if (value > 0) v = Math.pow(2.0, value * 2.0 * 13.0) - 1;
            else v = 0 - (Math.pow(2.0, (0.0 - value) * 2.0 * 13.0) - 1);
            return v;
            }

        /** Translates -8192 ... +8191 to 0.0 ... 1.0 using logs so that small changes
            have much bigger impact.  This is useful for displaying */
        public double getNormalizedLogValue() 
            { 
            return toNormalizedLog(value);
            }
                        
        /** Translates  0.0 ... 1.0 to -8192 ... +8191 using logs so that small changes
            have much bigger impact.  This is useful for displaying */
        public void setNormalizedLogValue(double value)
            {
            this.value = (int)fromNormalizedLog(value);
            }
                        
        public double getNormalizedValue(boolean log) { if (value < -8192) return (value + 8192); else if (log) return getNormalizedLogValue(); else return (value + 8192) / 16384.0; }
        public void setNormalizedValue(double value, boolean log) { if (value < 0) this.value = ((int)value) - 8192; else if (log) setNormalizedLogValue(value); else this.value = ((int)(value * 16384) - 8192); }
        public int getParameter() { return 0; }
        public int getType() { return TYPE_PITCH_BEND; }
        }

    public static class NRPN extends Event
        {
        // NRPN parameter number from 0 to 16383
        public int parameter;
        // NRPN value from 0 to 16383
        public int value;
        
        public NRPN(int parameter, int value, int when)
            {
            super(when);
            this.value = value;
            this.parameter = parameter;
            }
        public NRPN(JSONObject obj)
            {
            super(obj);
            parameter = obj.optInt("p", 0);
            value = obj.optInt("v", 0);
            }
        public Event copy()
            {
            return new NRPN(parameter, value, when);
            }
        public void write(Track track, Notes notes) throws InvalidMidiDataException
            {
            if (notes.wasRPN || notes.lastNRPN != parameter)
                {
                // Write out the parameter first
                track.add(new MidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, 99, (parameter >>> 7)), when));
                track.add(new MidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, 98, (parameter & 127)), when));
                notes.lastNRPN = parameter;
                notes.wasRPN = false;
                }
            // Now write out value, MSB first
            track.add(new MidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, 6, (value >>> 7)), when));
            track.add(new MidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, 38, (value & 127)), when));
            }
        public JSONObject save() throws JSONException
            {
            JSONObject obj = super.save();
            obj.put("t", "N");
            obj.put("p", parameter);
            obj.put("v", value);
            return obj;
            }
        public String toString() { return "NRPN:" + parameter + "->" + value + "[" + when + "]"; }

        public double getNormalizedValue(boolean log) { return value < 0 ? value : value / 16384.0; }
        public void setNormalizedValue(double value, boolean log) { this.value = (int)(value * 16384); }
        public int getParameter() { return parameter; }
        public int getType() { return TYPE_NRPN + parameter; }
        }

    public static class RPN extends Event
        {
        // RPN parameter number from 0 to 16383
        public int parameter;
        // RPN parameter value from 0 to 16383
        public int value;

        public RPN(int parameter, int value, int when)
            {
            super(when);
            this.value = value;
            this.parameter = parameter;
            }
        public RPN(JSONObject obj)
            {
            super(obj);
            parameter = obj.optInt("p", 0);
            value = obj.optInt("v", 0);
            }
        public Event copy()
            {
            return new RPN(parameter, value, when);
            }
        public void write(Track track, Notes notes) throws InvalidMidiDataException
            {
            if (!notes.wasRPN || notes.lastNRPN != parameter)
                {
                // Write out the parameter first
                track.add(new MidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, 101, (parameter >>> 7)), when));
                track.add(new MidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, 100, (parameter & 127)), when));
                notes.lastNRPN = parameter;
                notes.wasRPN = true;
                }
            // Now write out value, MSB first
            track.add(new MidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, 6, (value >>> 7)), when));
            track.add(new MidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, 38, (value & 127)), when));
            }
        public JSONObject save() throws JSONException
            {
            JSONObject obj = super.save();
            obj.put("t", "R");
            obj.put("p", parameter);
            obj.put("v", value);
            return obj;
            }
        public String toString() { return "RPN:" + parameter + "->" + value + "[" + when + "]"; }

        public double getNormalizedValue(boolean log) { return value < 0 ? value : value / 16384.0; }
        public void setNormalizedValue(double value, boolean log) { this.value = (int)(value * 16384); }
        public int getParameter() { return parameter; }
        public int getType() { return TYPE_RPN + parameter; }
        }

    public static class CC extends Event
        {
        // CC parameter number from 0 to 127.  We do not yet support 14-bit CC in Notes
        public int parameter;
        // CC parameter value from 0 to 127.  We do not yet support 14-bit CC in Notes
        public int value;

        public CC(int parameter, int value, int when)
            {
            super(when);
            this.value = value;
            this.parameter = parameter;
            }
        public CC(JSONObject obj)
            {
            super(obj);
            parameter = obj.optInt("p", 0);
            value = obj.optInt("v", 0);
            }
        public Event copy()
            {
            return new CC(parameter, value, when);
            }
        public void write(Track track, Notes notes) throws InvalidMidiDataException
            {
            track.add(new MidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, parameter, value), when));
            }
        public JSONObject save() throws JSONException
            {
            JSONObject obj = super.save();
            obj.put("t", "c");
            obj.put("p", parameter);
            obj.put("v", value);
            return obj;
            }
        public String toString() { return "CC:" + parameter + "->" + value + "[" + when + "]"; }

        public double getNormalizedValue(boolean log) { return value < 0 ? value : value / 128.0; }
        public void setNormalizedValue(double value, boolean log) { this.value = (int)(value * 128); }
        public int getParameter() { return parameter; }
        public int getType() { return TYPE_CC + parameter; }
        }


    public static class PC extends Event
        {
        // CC parameter value from 0 to 127.  We do not yet support 14-bit CC in Notes
        public int value;

        public PC(int value, int when)
            {
            super(when);
            this.value = value;
            }
        public PC(JSONObject obj)
            {
            super(obj);
            value = obj.optInt("v", 0);
            }
        public Event copy()
            {
            return new PC(value, when);
            }
        public void write(Track track, Notes notes) throws InvalidMidiDataException
            {
            track.add(new MidiEvent(new ShortMessage(ShortMessage.PROGRAM_CHANGE, value, 0), when));
            }
        public JSONObject save() throws JSONException
            {
            JSONObject obj = super.save();
            obj.put("t", "p");
            obj.put("v", value);
            return obj;
            }
        public String toString() { return "PC:" + value + "[" + when + "]"; }

        public double getNormalizedValue(boolean log) { return value < 0 ? value : value / 128.0; }
        public void setNormalizedValue(double value, boolean log) { this.value = (int)(value * 128); }
        public int getParameter() { return 0; }
        public int getType() { return TYPE_PC; }
        }

    public static class Aftertouch extends Event
        {
        // Aftertouch note pitch, or Out.CHANNEL_AFTERTOUCH if this is channel aftertouch.
        public int pitch;
        // Aftertouch value
        public int value;
        public Aftertouch(int pitch, int value, int when)
            {
            super(when);
            this.value = value;
            this.pitch = pitch;
            }
        public Aftertouch(int value, int when)
            {
            super(when);
            this.value = value;
            this.pitch = Out.CHANNEL_AFTERTOUCH;
            }
        public Aftertouch(JSONObject obj)
            {
            super(obj);
            pitch = obj.optInt("p", 0);
            value = obj.optInt("v", 0);
            }
        public Event copy()
            {
            return new Aftertouch(pitch, value, when);
            }
        public void write(Track track, Notes notes) throws InvalidMidiDataException
            {
            if (pitch == Out.CHANNEL_AFTERTOUCH)
                track.add(new MidiEvent(new ShortMessage(ShortMessage.CHANNEL_PRESSURE, value, 0), when));
            else
                track.add(new MidiEvent(new ShortMessage(ShortMessage.POLY_PRESSURE, pitch, value), when));
            }
        public JSONObject save() throws JSONException
            {
            JSONObject obj = super.save();
            obj.put("t", "a");
            obj.put("p", pitch);
            obj.put("v", value);
            return obj;
            }
        public String toString() { return "AT" + 
                    (pitch == Out.CHANNEL_AFTERTOUCH ? ":" : ":" + (NOTES[pitch % 12] + (pitch / 12)) + "->")
                      + value + "[" + when + "]"; }

        public double getNormalizedValue(boolean log) { return value < 0 ? value : value / 128.0; }
        public void setNormalizedValue(double value, boolean log) { this.value = (int)(value * 128); }
        public int getParameter() { return pitch; }
        public int getType() { return (pitch == Out.CHANNEL_AFTERTOUCH ? 384 : TYPE_POLYPHONIC_AFTERTOUCH + pitch); }
        }
        
        
        
    /// THE EVENT CC->NRPN/RPN PARSER        
    //
    //
    // This class converts the CCs, when appropriate, into NRPN or RPN messages in an ArrayList of events.
        
    static class EventParser
        {
        boolean rpn = false;                                    // Whether the last NRPN/RPN parameter is RPN
        int lastParamMSB = -1;                                      // The MSB of the last NRPN/RPN parameter
        int lastParamLSB = -1;                                      // The LSB of the last NRPN/RPN parameter
        int lastValueMSB = -1;                                      // The MSB of the last NRPN/RPN value
        boolean bareValueMSB = false;                       // True if an MSB for the NRPN/RPN value has been issued but not an LSB yet
        int bareValueMSBWhen = 0;                           // The timestamp of the last NRPN/RPN value
        boolean error;
    
        public boolean getError() { return error; }
    
        ArrayList<Event> parsed = new ArrayList<>();                // The resulting list of events
        
        // If there is a bare MSB value with no LSB pair, it is assumed to have LSB = 0.  Add it to the list.
        void dumpBareMSBValue()
            {
            if (lastParamMSB > 0 && lastParamLSB > 0 && lastValueMSB > 0 && bareValueMSB)
                {
                if (rpn)
                    {
                    parsed.add(new Notes.RPN((lastParamMSB << 7) | lastParamLSB, lastValueMSB << 7, bareValueMSBWhen));
                    }
                else
                    {
                    parsed.add(new Notes.NRPN((lastParamMSB << 7) | lastParamLSB, lastValueMSB << 7, bareValueMSBWhen));
                    }
                bareValueMSB = false;
                lastValueMSB = -1;
                bareValueMSBWhen = 0;
                }
            }
                
        public EventParser(ArrayList<Event> input, boolean convertNRPN)
            {
            for(Event evt : input)
                {
                if (convertNRPN && evt instanceof CC)
                    {
                    CC cc = (CC)evt;
                    if (cc.parameter == 99)
                        {
                        dumpBareMSBValue();
                        if (rpn) lastParamLSB = -1;
                        lastParamMSB = cc.value;
                        rpn = false;
                        }
                    else if (cc.parameter == 98)
                        {
                        dumpBareMSBValue();
                        if (rpn) lastParamMSB = -1;
                        lastParamLSB = cc.value;
                        rpn = false;
                        }
                    else if (cc.parameter == 101)
                        {
                        dumpBareMSBValue();
                        if (rpn) lastParamLSB = -1;
                        lastParamMSB = cc.value;
                        rpn = true;
                        }
                    else if (cc.parameter == 100)
                        {
                        dumpBareMSBValue();
                        if (rpn) lastParamMSB = -1;
                        lastParamLSB = cc.value;
                        rpn = true;
                        }
                    else if (cc.parameter == 6)
                        {
                        dumpBareMSBValue();
                        if (lastParamMSB > 0 && lastParamLSB > 0)
                            {
                            lastValueMSB = cc.value;
                            bareValueMSB = true;
                            bareValueMSBWhen = cc.when;
                            }
                        else    
                            {
                            error = true;
                            }
                        }
                    else if (cc.parameter == 38)
                        {
                        if (lastParamMSB > 0 && lastParamLSB > 0)
                            {
                            if (rpn)
                                {
                                parsed.add(new Notes.RPN((lastParamMSB << 7) | lastParamLSB, (lastValueMSB << 7) | cc.value, cc.when));
                                }
                            else
                                {
                                parsed.add(new Notes.NRPN((lastParamMSB << 7) | lastParamLSB, (lastValueMSB << 7) | cc.value, cc.when));
                                }
                            bareValueMSB = false;
                            bareValueMSBWhen = 0;
                            }
                        else
                            {
                            error = true;
                            }
                        }
                    else  
                        {
                        dumpBareMSBValue();
                        parsed.add(evt);
                        }
                    }
                else
                    {
                    dumpBareMSBValue();
                    parsed.add(evt);
                    }
                }
            dumpBareMSBValue();
            }
        
        public ArrayList<Event> getParsedEvents()
            {
            return parsed;
            }
        }
                
        
        
    
    /// NOTES VARIABLES    
    
    // Event Parameter Indices.  Not to be confused with EVENT TYPES above.
    public static final int EVENT_PARAMETER_NONE = 0;
    public static final int EVENT_PARAMETER_CC = 1;
    public static final int EVENT_PARAMETER_POLY_AT = 2;
    public static final int EVENT_PARAMETER_CHANNEL_AT = 3;
    public static final int EVENT_PARAMETER_BEND = 4;
    public static final int EVENT_PARAMETER_PC = 5;
    public static final int EVENT_PARAMETER_NRPN = 6;
    public static final int EVENT_PARAMETER_RPN = 7;
    
    // Types of event parameters
    int[] eventParameterType = new int[NUM_EVENT_PARAMETERS];
    // MSB parameter of event parameters
    int[] eventParameterMSB = new int[NUM_EVENT_PARAMETERS];
    // LSB parameter of event parameters
    int[] eventParameterLSB = new int[NUM_EVENT_PARAMETERS];
        
    // All events in the Notes
    ArrayList<Event> events = new ArrayList<>();
    // Events currently being recorded, to be moved to the main events
    ArrayList<Event> recording = new ArrayList<>();
    // For cut, copy, and paste
    static ArrayList<Event> pasteboard = new ArrayList<>();
    
    // Do we record program change?
    boolean recordPC;
    // Do we record pitch bend?
    boolean recordBend;
    // Do we record CC?
    boolean recordCC;
    // Do we record aftertouch?
    boolean recordAftertouch;
    // Do we convert CC to NRP and RPN after recording?
    boolean convertNRPNRPN;
    // Do we quantize on recording?
    boolean quantize;
    // What do we quantize to?
    int quantizeTo;
    // Do we quantize note ends as well?
    boolean quantizeNoteEnds;
    // Do we quantize non-note messages as well?
    boolean quantizeNonNotes;
    // The quanization bias
    double quantizeBias;
    // The out device
    int out;
    // The in device
    int in;
    // The highest timestamp for any event
    int maxEventPosition = 0;
    // The highest timestamp for a note
    int maxNoteOnPosition = 0;
    // The highest time for a note OFF
    int maxNoteOffPosition = 0;
    // Do I play out notes as I am recording them?
    boolean echo = true;
    // Do I display bend as a log or linearly?
    boolean log = true;
    // The start marker for the Notes
    int start = 0;
    // The end marker for the Notes
    int end = 0;
    // Default velocity for new Notes
    int defaultVelocity;
    // Default release velocity for new Notes
    int defaultReleaseVelocity;
    // Window view position, used for undo and not saved
    Point viewPosition;
    
    public static final int INTEGRATE_REPLACE = 0;
    public static final int INTEGRATE_REPLACE_TRIM = 1;
    public static final int INTEGRATE_MERGE = 2;
    public static final int INTEGRATE_PUNCH_IN = 3;                             // doesn't work for now
    
    int recordIntegration = INTEGRATE_REPLACE;


	/** Returns the current view position undo cache */
	public Point getViewPosition() { return viewPosition; }
	/** Sets the current view position undo cache */
	public void setViewPosition(Point val) { viewPosition = val; }
	
    /** Returns the recorded note integration method. */
    public int getRecordIntegration() { return recordIntegration; }
    /** Sets the recorded note integration method. */
    public void setRecordIntegration(int val) { recordIntegration = val; Prefs.setLastInt("seq.motif.notes.Notes.recordintegration", val); }
        
    /** Returns whether pitch bend is displayed logarithmicaly */
    public boolean getLog() { return log; }
    /** Sets whether pitch bend is displayed logarithmicaly */
    public void setLog(boolean val) { log = val; }
        
    /** Returns the given event parameter type. */
    public int getEventParameterType(int param) { return eventParameterType[param]; }
    /** Sets the given event parameter type. */
    public void setEventParameterType(int param, int val) { eventParameterType[param] = val; }
    
    /** Returns the given event parameter MSB. */
    public int getEventParameterMSB(int param) { return eventParameterMSB[param]; }
    /** Sets the given event parameter MSB. */
    public void setEventParameterMSB(int param, int val) { eventParameterMSB[param] = val; }
    
    /** Returns the given event parameter LSB. */
    public int getEventParameterLSB(int param) { return eventParameterLSB[param]; }
    /** Sets the given event parameter LSB. */
    public void setEventParameterLSB(int param, int val) { eventParameterLSB[param] = val; }

    /** Returns whether we record pitch bend. */
    public boolean getRecordBend() { return recordBend; }
    /** Sets whether we record pitch bend. */
    public void setRecordBend(boolean val) { recordBend = val; Prefs.setLastBoolean("seq.motif.notes.Notes.recordbend", val); }

    /** Returns whether we record CC. */
    public boolean getRecordCC() { return recordCC; }
    /** Sets whether we record CC. */
    public void setRecordCC(boolean val) { recordCC = val; Prefs.setLastBoolean("seq.motif.notes.Notes.recordcc", val); }

    /** Returns whether we record PC. */
    public boolean getRecordPC() { return recordPC; }
    /** Sets whether we record PC. */
    public void setRecordPC(boolean val) { recordPC = val; Prefs.setLastBoolean("seq.motif.notes.Notes.recordpc", val); }

    /** Returns whether we record Aftertouch. */
    public boolean getRecordAftertouch() { return recordAftertouch; }
    /** Sets whether we record Aftertouich. */
    public void setRecordAftertouch(boolean val) { recordAftertouch = val; Prefs.setLastBoolean("seq.motif.notes.Notes.recordaftertouch", val); }

    /** Returns whether we quantize on recording. */
    public boolean getConvertNRPNRPN() { return convertNRPNRPN; }
    /** Sets whether we quantize on recording. */
    public void setConvertNRPNRPN(boolean val) { convertNRPNRPN = val; Prefs.setLastBoolean("seq.motif.notes.Notes.convertNRPNRPN", val); }

    /** Returns whether we quantize on recording. */
    public boolean getQuantize() { return quantize; }
    /** Sets whether we quantize on recording. */
    public void setQuantize(boolean val) { quantize = val; Prefs.setLastBoolean("seq.motif.notes.Notes.quantize", val); }

    /** Returns our quantization amount on recording. */
    public int getQuantizeTo() { return quantizeTo; }
    /** Sets our quantization amount on recording. */
    public void setQuantizeTo(int val) { quantizeTo = val; Prefs.setLastInt("seq.motif.notes.Notes.quantizeTo", val); }

    /** Returns whether we quantize note ends on recording. */
    public boolean getQuantizeNoteEnds() { return quantizeNoteEnds; }
    /** Sets whether we quantize note ends on recording. */
    public void setQuantizeNoteEnds(boolean val) { quantizeNoteEnds = val; Prefs.setLastBoolean("seq.motif.notes.Notes.quantizeNoteEnds", val); }

    /** Returns whether we quantize non-notes on recording. */
    public boolean getQuantizeNonNotes() { return quantizeNonNotes; }
    /** Sets whether we quantize non-notes on recording. */
    public void setQuantizeNonNotes(boolean val) { quantizeNonNotes = val; Prefs.setLastBoolean("seq.motif.notes.Notes.quantizeNonNotes", val); }

    /** Returns the quantization bias on recording. */
    public double getQuantizeBias() { return quantizeBias; }
    /** Sets the quantization bias on recording. */
    public void setQuantizeBias(double val) { quantizeBias = val; Prefs.setLastDouble("seq.motif.notes.Notes.quantizeBias", val); }

    /** Returns the output device. */
    public int getOut() { return out; }
    /** Sets the output device. */
    public void setOut(int val) { out = val; Prefs.setLastOutDevice(0, val, "seq.motif.notes.Notes.out"); }

    /** Returns the input device. */
    public int getIn() { return in; }
    /** Sets the input device. */
    public void setIn(int val) { in = val; Prefs.setLastInDevice(0, val, "seq.motif.notes.Notes.in"); }
        
    /** Returns whether we echo when recording. */
    public boolean getEcho() { return echo; }
    /** Sets whether we echo when recording. */
    public void setEcho(boolean val) { echo = val; }
    
    /** Returns the endpoint. */
    public void setEnd(int end) { this.end = end; }
    /** Sets the endpoint. */
    public int getEnd() { return end; }
    
    /** Returns the start point. */
    public void setStart(int start) { this.start = start; }
    /** Sets the start point. */
    public int getStart() { return start; }
    
    /** Returns the highest timestamp of the onset of any event. */
    public int getMaxEventPosition() { return maxEventPosition; }
    /** Returns the highest timestamp of the onset of any note. */
    public int getMaxNoteOnPosition() { return maxNoteOnPosition; }
    /** Returns the highest time of the NOTE OFF of any note. */
    public int getMaxNoteOffPosition() { return maxNoteOffPosition; }
    
    /** Returns the default velocity of new notes drawn by hand. */
    public int getDefaultVelocity() { return defaultVelocity; }
    /** Sets the default release velocity of new notes drawn by hand. */
    public void setDefaultVelocity(int val) { defaultVelocity = val; Prefs.setLastInt("seq.motif.notes.Notes.defaultVelocity", val); }
    /** Returns the default velocity of new notes drawn by hand. */
    public int getDefaultReleaseVelocity() { return defaultReleaseVelocity; }
    /** Sets the default release velocity of new notes drawn by hand. */
    public void setDefaultReleaseVelocity(int val) { defaultReleaseVelocity = val; Prefs.setLastInt("seq.motif.notes.Notes.defaultReleaseVelocity", val); }
        
        

    /** Copies the Notes */
    public Motif copy()
        {
        Notes other = (Notes)(super.copy());
        other.events = new ArrayList();
        for(Event event : events)
            other.events.add(event.copy());
        System.arraycopy(eventParameterType, 0, other.eventParameterType, 0, eventParameterType.length);
        System.arraycopy(eventParameterMSB, 0, other.eventParameterMSB, 0, eventParameterMSB.length);
        System.arraycopy(eventParameterLSB, 0, other.eventParameterLSB, 0, eventParameterLSB.length);
        clearRecording();
        return other;
        }
        
    public Notes(Seq seq) { this(seq, false); }         // this version is called by the file loader
    
    public Notes(Seq seq, boolean arm)                          // this version is called when we make a Notes window
        {
        super(seq);

        // Load devices. Note I'm not using setOut(...) etc. which would write the device to prefs
        out = (Prefs.getLastOutDevice(0, "seq.motif.notes.Notes.out"));
        in = (Prefs.getLastInDevice(0, "seq.motif.notes.Notes.in"));
        setArmed(arm);
        recordBend = Prefs.getLastBoolean("seq.motif.notes.Notes.recordbend", true); 
        recordCC = Prefs.getLastBoolean("seq.motif.notes.Notes.recordcc", true); 
        recordPC = Prefs.getLastBoolean("seq.motif.notes.Notes.recordpc", true); 
        recordAftertouch = Prefs.getLastBoolean("seq.motif.notes.Notes.recordaftertouch", true); 
        convertNRPNRPN = Prefs.getLastBoolean("seq.motif.notes.Notes.convertNRPNRPN", true); 
        quantize = Prefs.getLastBoolean("seq.motif.notes.Notes.quantize", true); 
        quantizeTo = Prefs.getLastInt("seq.motif.notes.Notes.quantizeTo", 3);                   // 16th notes 
        quantizeNoteEnds = Prefs.getLastBoolean("seq.motif.notes.Notes.quantizeNoteEnds", true); 
        quantizeNonNotes = Prefs.getLastBoolean("seq.motif.notes.Notes.quantizeNonNotes", true); 
        quantizeBias = Prefs.getLastDouble("seq.motif.notes.Notes.quantizeBias", 0.5); 
        recordIntegration = Prefs.getLastInt("seq.motif.notes.Notes.recordintegration", INTEGRATE_REPLACE); 
        defaultVelocity = Prefs.getLastInt("seq.motif.notes.Notes.defaultVelocity", 64);         
        defaultReleaseVelocity = Prefs.getLastInt("seq.motif.notes.Notes.defaultReleaseVelocity", 64); 
        }

    /** Returns all events */
    public ArrayList<Event> getEvents() { return events; }
    
    /** Builds and returns a LinkedHashMap of ArrayLists of events by type, for all events stored
        in the Notes object. */
    public LinkedHashMap<Integer, ArrayList<Event>> getEventsByType()
        {
        LinkedHashMap<Integer, ArrayList<Event>> map = new LinkedHashMap<>();
        for(Event event : getEvents())
            {
            int type = event.getType();
            ArrayList<Event> events = map.get(type);
            if (events == null)
                {
                events = new ArrayList<>();
                map.put(type, events);
                }
            events.add(event);
            }
        return map;
        }
        
    /** Builds and returns a sorted list of unique types included among the Notes, 
        including Note objects if includeNotes is true. */
    public ArrayList<Integer> getTypes(boolean includeNotes)
        {
        HashSet<Integer> set = new HashSet<>();
        for(Event event : getEvents())
            {
            int type = event.getType();
            if (type >= TYPE_CC || includeNotes)
                {
                set.add(type);
                }
            }
        ArrayList<Integer> list = new ArrayList<>(set);
        Collections.sort(list);
        return list;
        }

    /** Builds and returns all events of the given types, divided up by type.  
        This is an O(n * m) operation.  If types[i] is not a valid type,
        null will be provided for it. */
    public ArrayList<ArrayList<Event>> getEventsOfTypes(int[] types)
        {
        ArrayList<ArrayList<Event>> list = new ArrayList<>();
        for(int type : types)
            {
            list.add(getEventsOfType(type));
            }
        return list;
        }

    /** Builds and returns all events of the given type,
        If types is not a valid type, null will be provided for it. */
    public ArrayList<Event> getEventsOfType(int type)
        {
        if (isValidType(type))
            {
            ArrayList<Event> evt = new ArrayList<>();
            for(Event event : getEvents())
                {
                int t = event.getType();
                if (t == type)
                    {
                    evt.add(event);
                    }
                }
            return evt;
            }
        else
            {
            return null;
            }
        }
    
    /** Returns all Notes, broken out by pitch.  */
    public ArrayList<ArrayList<Note>> getNotesByPitch()
        {
        ArrayList<ArrayList<Note>> all = new ArrayList<>();
        for(int i = 0; i < 128; i++)
            {
            all.add(new ArrayList<Note>());
            } 
                
        for(Event event : getEvents())
            {
            if (event instanceof Note)
                {
                Note note = (Note) event;
                all.get(note.pitch).add(note);
                }
            }
        return all;
        }
        
    /** Parses the given events, returning new ones.  error[0] is set to TRUE
        if there was an error, else it is set to FALSE. */
    public ArrayList<Event> parseEvents(ArrayList<Event> val, boolean[] error)
        {
        if (getConvertNRPNRPN())
            {
            EventParser parser = new EventParser(val, true);
            ArrayList<Event> parsed = parser.getParsedEvents();
            error[0] = parser.getError();
            return parsed;
            }
        else return val;
        }
    
    /** Sets the events.  */
    public void setEvents(ArrayList<Event> val) 
        {
        events = val;
        computeMaxTime();     
        }

    /** Erases recorded events.  */
    public void clearRecording()
        {
        recording = new ArrayList<Event>();
        }

    /** Returns the recorded events.  */
    public ArrayList<Event> getRecording()
        {
        return recording;
        }
        
    /** Sets whether we are armed.  This may also clear the recording.  */
    public void setArmed(boolean val) 
        {
        boolean wasArmed = isArmed();
        boolean isArmed = val;
        super.setArmed(val);
        
        // Handle changes in arming
        if (!wasArmed && isArmed)
            {
            // clear recorded notes
            clearRecording();
            }
        else if (wasArmed && !isArmed)
            {
            // clear recorded notes
            clearRecording();
            }
        }
    
    /** Recomputes and stores the maxNoteOnPosition, maxNoteOffPosition, and maxEventPosition */
    public void computeMaxTime()
        {
        maxNoteOnPosition = 0;
        maxNoteOffPosition = 0;
        maxEventPosition = 0;
        for(Event event : events)
            {
            maxEventPosition = Math.max(maxEventPosition, event.when);
            if (event instanceof Note)
                {
                Note note = (Note) event;
                maxNoteOnPosition = Math.max(maxNoteOnPosition, note.when);
                maxNoteOffPosition = Math.max(maxNoteOffPosition, note.when + note.length);
                }
            }
        }
            
    /// FIXME: did I break this?
    
    //// FIXME: This feels wrong.  maxNoteOffPosition is the time we do a release.
    //// If it is exactly at a certain beat, then wouldn't we actually be doing the first
    //// tick of the next motif AFTER that beat?  Same thing for end...

    /** Returns the time at which we should declare we are no longer playing.   */
    public int getEndTime()
        {
        int maxPos = (maxNoteOffPosition > maxEventPosition ? maxNoteOffPosition : maxEventPosition);
        int endTime = (maxPos > end ? maxPos : (end >= 0 ? end : 0)) - 1;                       // FIXME: So I'm subtracting 1....
        if (endTime < 0) endTime = 0;
        return endTime;
        }
        
    /** Sorts the events by onset time. */
    public void sortEvents()
        {
        sortEvents(events);
        }
    
    /** Sorts the provided events by onset time. */
    public void sortEvents(ArrayList<Event> events)
        {
        Collections.sort(events, new Comparator<Event>()
                {
                public int compare(Event o1, Event o2) { return o1.when - o2.when; }
                public boolean equals(Comparator<Event> other) { return (this == other); }
            });
        }
    

    /** Remove the given events. */
    public void remove(HashSet<Event> cut)
        {
        ArrayList<Event> newEvents = new ArrayList<>();
        
        for(Event event : events)
            {
            if (!cut.contains(event))
                {
                newEvents.add(event);
                }
            }

        events = newEvents;
        computeMaxTime();
        }

    /** Filters events from all events. */
    public ArrayList<Event> filter(boolean removeNotes, boolean removeBend, boolean removeCC, boolean removeNRPN, boolean removeRPN, boolean removePC, boolean removeAftertouch)
        {
        ArrayList<Event> newEvents = new ArrayList<Event>();
        ArrayList<Event> cut = new ArrayList<Event>();
        
        for(Event event : events)
            {
            if (
                (event instanceof Note && removeNotes) ||
                (event instanceof Bend && removeBend) ||
                (event instanceof CC && removeCC) ||
                (event instanceof PC && removePC) ||
                (event instanceof Aftertouch && removeAftertouch) ||
                (event instanceof NRPN && removeNRPN) ||
                (event instanceof RPN && removeRPN)
                )            
                {
                cut.add(event);
                }
            else        
                {
                newEvents.add(event);
                }
            }
        
        events = newEvents;
        computeMaxTime();
        return cut;
        }
    
    /** Filters events from the given events. */
    public ArrayList<Event> filter(ArrayList<Event> eventsIn, boolean removeNotes, boolean removeBend, boolean removeCC, boolean removeNRPN, boolean removeRPN, boolean removePC, boolean removeAftertouch)        // endIndex is inclusive
        {
        ArrayList<Event> newEvents = new ArrayList<Event>();
        ArrayList<Event> cut = new ArrayList<Event>();
        
        HashSet<Event> hash = new HashSet(eventsIn);
        
        for(Event event : events)
            {
            if (hash.contains(event))
                {
                if (
                    (event instanceof Note && removeNotes) ||
                    (event instanceof Bend && removeBend) ||
                    (event instanceof CC && removeCC) ||
                    (event instanceof PC && removePC) ||
                    (event instanceof Aftertouch && removeAftertouch) ||
                    (event instanceof NRPN && removeNRPN) ||
                    (event instanceof RPN && removeRPN)
                    )            
                    {
                    cut.add(event);
                    }
                else        
                    {
                    newEvents.add(event);
                    }
                }
            else
                {
                newEvents.add(event);
                }
            }
        
        events = newEvents;
        computeMaxTime();
        return cut;
        }
    
    /** Removes and returns events between start and end. */
    public ArrayList<Event> cut(int start, int end, boolean allowStartOverlaps, boolean allowEndOverlaps)
        {
        ArrayList<Event> newEvents = new ArrayList<Event>();
        ArrayList<Event> cut = new ArrayList<Event>();

        // remove overlapping events
        for(int i = 0; i < events.size(); i++)
            {
            Event event = events.get(i);
            // If fully outside...
            if (event.when + event.getLength() < start || event.when >= end)
                {
                newEvents.add(event);
                }
            // if started early but we're allowing start overlaps
            else if (event.when < start && event.when + event.getLength() >= start && allowStartOverlaps)
                {
                newEvents.add(event);
                }
            else if (event.when < end && event.when + event.getLength() >= end && allowEndOverlaps)
                {
                newEvents.add(event);
                }
            else
                {
                cut.add(event);
                }
            }
        events = newEvents;
        computeMaxTime(); 
        return cut;
        }

    /** Copies and returns events between start and end. */
    public ArrayList<Event> copy(int start, int end, boolean allowStartOverlaps, boolean allowEndOverlaps)
        {
        ArrayList<Event> copy = new ArrayList<Event>();

        // remove overlapping events
        for(int i = 0; i < events.size(); i++)
            {
            Event event = events.get(i);
            // If fully outside...
            if (event.when + event.getLength() < start || event.when >= end)
                {
                // do nothing
                }
            // if started early but we're allowing start overlaps
            else if (event.when < start && event.when + event.getLength() >= start && allowStartOverlaps)
                {
                // do nothing
                }
            else if (event.when < end && event.when + event.getLength() >= end && allowEndOverlaps)
                {
                // do nothing
                }
            else
                {
                copy.add(event);
                }
            }
        computeMaxTime(); 
        return copy;
        }

    public static void setPasteboard(ArrayList<Event> events)
        {
        pasteboard.clear();
        pasteboard.addAll(events);
        }

    public static ArrayList<Event> getPasteboard()
        {
        ArrayList<Event> copy = new ArrayList<>(pasteboard.size());
        for(Event event : pasteboard)
            {
            copy.add(event.copy());
            }
        return copy;
        }

/*
  public ArrayList<Event> copyEvents(int[] indices)
  {
  ArrayList<Event> copy = new ArrayList<Event>();
        
  for(int i : indices)
  {
  Event event = events.get(i);
            
  copy.add(event);
  }
  computeMaxTime(); 
  return copy;
  }
*/

    /** Inserts the given events in sorted order. */
    public void merge(ArrayList<Event> from)
        {
        if (from.isEmpty()) return;
        ArrayList<Event> newEvents = new ArrayList<Event>();
        
        int plen = events.size();
        int rlen = from.size();
        int pi = 0;
        int ri = 0;
        while(pi < plen || ri < rlen)           // As long as a stream still has events
            {
            if (pi >= plen)                                     // only r left
                {
                Event r = from.get(ri);
                newEvents.add(r);
                ri++;
                }
            else if (ri >= rlen)                        // only p left
                {
                Event p = events.get(pi);
                newEvents.add(p);
                pi++;
                }
            else                                                        // both have events left
                {
                Event p = events.get(pi);
                Event r = from.get(ri);
                if (p.when < r.when)
                    {
                    newEvents.add(p);
                    pi++;
                    }
                else
                    {
                    newEvents.add(r);
                    ri++;
                    }
                }
            }
        setEvents(newEvents);
        }

    /** Replaces with the given events all events whose timestamp overlaps with them.  Returns the replaced events. */
    public ArrayList<Event> overwrite(ArrayList<Event> from, boolean allowStartOverlaps, boolean allowEndOverlaps)
        {
        if (from.isEmpty()) return new ArrayList<Event>();
        int start = Integer.MAX_VALUE;
        int end = 0;
        for(Event event : from)
            {
            if (start > event.when) start = event.when;
            if (end < event.when + event.getLength()) end = event.when + event.getLength();
            }
        ArrayList<Event> cut = cut(start, end, allowStartOverlaps, allowEndOverlaps);
        merge(from);
        return cut;
        }

    /** Returns the minimum time of the events. */
    public int getMinimumTime(ArrayList<Event> events)
        {
        if (events.size() == 0)
            {
            System.err.println("Note.getMinimumTime(): called with empty events");
            return 0;
            }
                
        int minTime = -1;
        for(Event event: events)
            {
            int time = event.when;
            if (minTime == -1 || time < minTime)
                {
                minTime = time;
                }
            }
        return minTime;
        }

    /** Returns the maximum time of the events. */
    public int getMaximumTime(ArrayList<Event> events)
        {
        if (events.size() == 0)
            {
            System.err.println("Note.getMaximumTime(): called with empty events");
            return 0;
            }
                
        int maxTime = -1;
        for(Event event: events)
            {
            int time = event.when + event.getLength();
            if (time > maxTime)
                {
                maxTime = time;
                }
            }
        return maxTime;
        }
        
    /** Shifts all events in time so that the first event's onset is at timestep 0. */
    public void trim()
        {
        if (events.size() > 0) 
            {
            shift(events, 0 - getMinimumTime(events), false);
            }
        }

    /** Shifts all events in time by the given number of steps. */
    public void shift(int by)
        {
        shift(events, by, false);
        }
                

    /** Shifts the provided events in time by the given number of steps, and sorts all events afterwards */
    public void shift(ArrayList<Event> events, int by)
        {
        shift(events, by, true);
        }
                
    // Shifts the provided events in time by the given number of steps and optionally sorts afterwards
    void shift(ArrayList<Event> events, int by, boolean sort)
        {
        if (events.size() == 0) return;
        if (by == 0) return;
        
        for(Event event : events)
            {
            event.when += by;
            if (event.when < 0)     // uh on
                {
                event.when = 0;
                }
            /// FIXME: we don't have an upper bound
            }
        // Need to sort
        if (sort) sortEvents(events);
		computeMaxTime(); 
        }
        
    /** Transposes the given events by the provided semitones. */
    public void transpose(ArrayList<Event> events, int by)
        {
        if (by == 0) return;
        
        // I don't think should be able to change the order, so we're probably still okay?
        for(Event event : events)
            {
            if (event instanceof Note)
                {
                Note note = (Note) event;
                note.pitch += by;
                if (note.pitch > 127) // uh oh
                    {
                    note.pitch = 127;
                    }
                if (note.pitch < 0) // uh oh
                    {
                    note.pitch = 0;
                    }
                }
            }
        }
              

    /** Stretches the onset and length of the provided events so that the previous time interval STRETCHFROM
        is stretched to fill the time interval STRETCHTO.  The stretched events
        are shifted in time afterwards so that the first event's onset is the same as it originally was. */
    public void stretch(ArrayList<Event> events, int stretchFrom, int stretchTo)
        {
        if (stretchFrom == stretchTo) return;
        if (events.isEmpty()) return;
        
        // Find start time
        int startTime = -1;
        Event startEvent = null;
        for(Event event : events)
            {
			if (startTime == -1 || event.when < startTime)
				{
				startTime = event.when;
				startEvent = event;
				}
            }

		// Shift to start time 0
        for(Event event : events)
            {
			event.when -= startTime;
            }

		// Multiply
        for(Event event : events)
			{
            event.when = (int)Math.round((event.when * stretchTo) / (double)stretchFrom);
            if (event instanceof Note)
                {
                Note note = (Note)event;
                note.length = (int)Math.round((event.getLength() * stretchTo) / (double)stretchFrom);
                }
            }
        
        // Shift back
        int newStartTime = startEvent.when;
        for(Event event : events)
            {
			event.when = event.when - newStartTime + startTime;
            }

        sortEvents(events);
        computeMaxTime(); 
        }


    /// Some quantization divisors
    public static final int[] QUANTIZE_DIVISORS = { 
        Seq.PPQ / 12,           // 32 Triplet
        Seq.PPQ / 8,            // 32
        Seq.PPQ / 6,            // 16 Triplet
        Seq.PPQ / 4,            // 16
        Seq.PPQ / 3,            // 8 Triplet
        Seq.PPQ / 2,            // 8
        (Seq.PPQ * 2) / 3,      // 4 Triplet
        Seq.PPQ                         // 4
        };
        
    // Standard quantization divisor strings
    public static final String[] QUANTIZE_STRINGS = { "32nd Trip", "32nd Note", "16th Trip", "16th Note", "8th Trip", "8th Note", "Quarter Trip", "Quarter Note" };
    
    /** Quantize all events in the given from...to range (inclusive)
        to the nearest DIVISOR NOTE in ticks.  For each note we compute the DIVISOR NOTE
        that is LESS THAN OR EQUAL to the note.  Then if the note is less than DIVISOR * PERCENTAGE later
        than the DIVISOR NOTE, we push it to the divisor note, else we push it to the next divisor note.
        Optionally also quantize the ends, and non-notes.  Percentage can be any value 0.0 to 1.0 inclusive.
    */
    public void quantize(ArrayList<Event> events, int divisor, boolean quantizeEnds, boolean quantizeNonNotes, double percentage)
        {
        for(Event event: events)
            {
            // Zero, stash the note off position for later use
            int noteOffPos = 0;
            if (event instanceof Note && quantizeEnds)
                {
                noteOffPos = event.when + event.getLength();
                }
                        
            // First handle onsets
            if (event instanceof Note || quantizeNonNotes)
                {
                int quantizedTime = (event.when / divisor) * divisor;
                double pivot = (event.when - quantizedTime) / (double)divisor;
                        
                if (pivot < percentage) event.when = quantizedTime; // pull down
                else event.when = quantizedTime + divisor;                // push up
                }
                
            // Next handle NOTE OFF
            if (event instanceof Note && quantizeEnds)
                {
                int quantizedTime = (noteOffPos / divisor) * divisor;
                double pivot = (noteOffPos - quantizedTime) / (double)divisor;
                        
                if (pivot < percentage) noteOffPos = quantizedTime; // pull down
                else noteOffPos = quantizedTime + divisor;                // push up
                        
                // change back to length
                ((Note)event).length = noteOffPos - event.when;
                if (event.getLength() < 0)
                    {
                    System.err.println("QUANTIZE ERROR: event length went negative: " + event.getLength()); 
                    ((Note)event).length = 0;
                    }
                }
            }
        sortEvents(events);
        computeMaxTime(); 
        }

    // Provide a random number of ticks scaled for randomizing time
    int getRandomTimeNoise(Random random, double max)
        {
//              return (int)Math.round(random.nextGaussian() * stdev);
        return (int)((random.nextDouble() * 2.0 - 1.0) * max * Seq.PPQ);              // Go up to 1 beat max
        }
                
    /** Quantize the onset, and possibly release time, of all events, possibly including non-notes, by the given
        variance (in ticks), in the event range from FROM to TO. */
    public void randomizeTime(ArrayList<Event> events, double max, boolean randomizeLengths, boolean randomizeNonNotes, Random random)
        {
        for(Event event : events)
            {
            // First handle onsets
            if (event instanceof Note || randomizeNonNotes)
                {
                int val = event.when + getRandomTimeNoise(random, max);
                while(val < 0)
                    {
                    val = event.when + getRandomTimeNoise(random, max);
                    }
                event.when = val;
                }
                
            // Next handle LENGTHS
            if (event instanceof Note && randomizeLengths)
                {
                int val = event.getLength() + getRandomTimeNoise(random, max);
                while(val < 0)
                    {
                    val = event.getLength() + getRandomTimeNoise(random, max);
                    }
                ((Note)event).length = val;
                }
            }
        sortEvents(events);     
        computeMaxTime(); 
        }

    /** Sets the velocity of all events to the given value */
    public void setVelocity(int val)
        {
        setVelocity(events, val);
        }

    /** Sets the velocity of the provided events to the given value */
    public void setVelocity(ArrayList<Event> events, int val)
        {
        for(Event event : events)
            {
            // First handle onsets (which CANNOT have 0 velocity)
            if (event instanceof Note)
                {
                Note note = (Note)event;
                note.velocity = val;
                }
            }
        }

    // Provide a random velocity delta
    int getRandomVelocityNoise(Random random, double max)
        {
        return (int)((random.nextDouble() * 127 - 63) * max);           // So we have enough space to fit in above 1...
        }
                
    /** Randomize the velocity, and possibly release velocity, of all events, by the given
        variance (from 0..255). */
    public void randomizeVelocity(double max, boolean randomizeReleases, Random random)
        {
        randomizeVelocity(events, max, randomizeReleases, random);
        }

    // How often should we try to randomize the note velocity and stay in the proper time?
    static final int NUM_TRIES = 8;
        
    /** Randomize the velocity, and possibly release velocity, of the provided events, by the given
        variance (from 0..255). */
    public void randomizeVelocity(ArrayList<Event> events, double max, boolean randomizeReleases, Random random)
        {
        for(Event event : events)
            {
            // First handle onsets (which CANNOT have 0 velocity)
            if (event instanceof Note)
                {
                Note note = (Note)event;
                int val = 0;
                boolean success = false;
                for(int tries = 0; tries < NUM_TRIES; tries++)
                    {
                    int rnd = getRandomVelocityNoise(random, max);
                    val = note.velocity + rnd;
                    if (val >= 1 && val <= 127) { success = true; break; }
                                        
                    val = note.velocity - rnd;      // let's try flipping
                    if (val >= 1 && val <= 127) { success = true; break; }
                    }
                if (success) note.velocity = val;
                else {System.err.println("Panic"); note.velocity = random.nextInt(127) + 1;     }       // I don't think this can happen?

                // Next handle releases (which can have 0 velocity)
                if (randomizeReleases)
                    {
                    val = 0;
                    success = false;
                    for(int tries = 0; tries < NUM_TRIES; tries++)
                        {
                        int rnd = getRandomVelocityNoise(random, max);
                        val = note.release + rnd;
                        if (val >= 0 && val <= 127) { success = true; break; }
                                                
                        val = note.release - rnd;       // let's try flipping
                        if (val >= 0 && val <= 127) { success = true; break; }
                        }
                    if (success) note.release = val;
                    else {System.err.println("Panic"); note.release = random.nextInt(128);  }       // I don't think this can happen?
                    }
                }
            }
        }


    /** Builds the clip. */
    public Clip buildClip(Clip parent)
        {
        return new NotesClip(seq, this, parent);
        }

    /** Read Notes from a MIDI File, displacing the originals. */
    public void read(File midiFile) { try { read(MidiSystem.getSequence(midiFile)); } catch (InvalidMidiDataException ex) { }  catch (IOException ex) { }}

    /** Read Notes from an InputStream, displacing the originals. */
    public void read(InputStream stream) { try { read(MidiSystem.getSequence(stream));  } catch (InvalidMidiDataException ex) { }  catch (IOException ex) { }}

    /** Read Notes from a URL, displacing the originals. */
    public void read(URL url) { try { read(MidiSystem.getSequence(url));  } catch (InvalidMidiDataException ex) { } catch (IOException ex) { }}

    /** Read Notes from a Sequence, displacing the originals. */
    public void read(javax.sound.midi.Sequence sequence)
        {
        Track[] tracks = sequence.getTracks();
        if (tracks.length == 0) return;
        if (sequence.getDivisionType() != Sequence.PPQ) return;         // uh oh
        int resolution = sequence.getResolution();
                
        Notes.Note[] recordedNoteOn = new Notes.Note[128];
        ArrayList<Notes.Event> readEvents = new ArrayList<>();  
        
        // FIXME: At present we only load a single track
        for(int i = 0; i < tracks[0].size(); i++)
            {
            MidiEvent e = tracks[0].get(i);
            int pos = (int)((e.getTick() * Seq.PPQ) / resolution);                  // e.getTick() is a long
            MidiMessage message = e.getMessage();
            if (message instanceof ShortMessage)
                {
                ShortMessage shortmessage = (ShortMessage)message;

                if (Clip.isNoteOn(shortmessage))
                    {
                    int pitch = shortmessage.getData1();
                    Notes.Note noteOn = new Notes.Note(pitch,
                        shortmessage.getData2(),
                        pos, 1);             // gotta have something for length
                    readEvents.add(noteOn);
                    recordedNoteOn[pitch] = noteOn;
                    }
                else if (Clip.isNoteOff(shortmessage))
                    {
                    int pitch = shortmessage.getData1();
                    Notes.Note noteOn = recordedNoteOn[pitch];
                    int release = 64;
                    if (noteOn != null)
                        {
                        noteOn.length = pos - noteOn.when;
                        noteOn.release = shortmessage.getData2();
                        release = noteOn.release;
                        recordedNoteOn[pitch] = null;
                        }
                    }
                else if (Clip.isPitchBend(shortmessage) && getRecordBend())
                    {
                    int lsb = shortmessage.getData1();
                    int msb = shortmessage.getData2();

                    Notes.Bend bend = new Notes.Bend(msb * 128 + lsb - 8192, pos);
                    readEvents.add(bend);
                    }
                else if (Clip.isCC(shortmessage) && getRecordCC())
                    {
                    int parameter = shortmessage.getData1();
                    int value = shortmessage.getData2();

                    Notes.CC cc = new Notes.CC(parameter, value, pos);
                    readEvents.add(cc);
                    }
                else if (Clip.isPC(shortmessage) && getRecordPC())
                    {
                    int value = shortmessage.getData1();

                    Notes.PC pc = new Notes.PC(value, pos);
                    readEvents.add(pc);
                    }
                else if (Clip.isChannelAftertouch(shortmessage) && getRecordAftertouch())
                    {
                    int value = shortmessage.getData1();

                    Notes.Aftertouch aftertouch = new Notes.Aftertouch(value, pos);
                    readEvents.add(aftertouch);
                    }
                else if (Clip.isPolyphonicAftertouch(shortmessage) && recordAftertouch)
                    {
                    int pitch = shortmessage.getData1();
                    int value = shortmessage.getData2();

                    Notes.Aftertouch aftertouch = new Notes.Aftertouch(pitch, value, pos);
                    readEvents.add(aftertouch);
                    }
                }
            }
        events = readEvents;
        computeMaxTime();
        } 
    
    
    
    /** Writes Notes to a Sequence */
    public Sequence write() throws InvalidMidiDataException
        {
        javax.sound.midi.Sequence sequence = new javax.sound.midi.Sequence(javax.sound.midi.Sequence.PPQ, Seq.PPQ);
        Track track = sequence.createTrack();
        lastNRPN = -1;
        wasRPN = false;
        for(Event event : events)
            {
            event.write(track, this);
            }
        return sequence;
        }

    /** Writes notes to a MIDI File */
    public void write(File out) throws IOException, InvalidMidiDataException
        {
        MidiSystem.write(write(), 0, out);
        }

    /** Writes notes to an Output Stream */
    public void write(OutputStream out) throws IOException, InvalidMidiDataException
        {
        MidiSystem.write(write(), 0, out);
        }

    /** Loads the Notes object from JSON */
    public void load(JSONObject obj) throws JSONException
        {
        setStart(obj.optInt("start", 0));
        setEnd(obj.optInt("end", 0));
        setEcho(obj.optBoolean("echo", false));
        setOut(obj.optInt("out", 0));
        setIn(obj.optInt("in", 0));
        setLog(obj.optBoolean("log", true));
        
        JSONArray etypeArray = obj.optJSONArray("eparam");
        if (etypeArray != null)
            {
            for(int i = 0; i < NUM_EVENT_PARAMETERS; i++)
                {
                eventParameterType[i] = etypeArray.optInt(i, 0);
                }
            JSONArray emsbArray = obj.getJSONArray("emsb");
            for(int i = 0; i < NUM_EVENT_PARAMETERS; i++)
                {
                eventParameterMSB[i] = emsbArray.optInt(i, 0);
                }
            JSONArray elsbArray = obj.getJSONArray("elsb");
            for(int i = 0; i < NUM_EVENT_PARAMETERS; i++)
                {
                eventParameterLSB[i] = elsbArray.optInt(i, 0);
                }
            }
            
        JSONArray eventsArray = obj.getJSONArray("events");
        events = new ArrayList<Event>();
        for(int i = 0; i < eventsArray.length(); i++)
            {
            Event event = Event.load(eventsArray.getJSONObject(i));
            events.add(event);
            }
        computeMaxTime();               // computs maxNoteOnPosition and maxNoteOffPosition
        }
        
    /** Saves the Notes object to JSON */
    public void save(JSONObject obj) throws JSONException
        {
        obj.put("start", getStart());
        obj.put("end", getEnd());
        obj.put("echo", getEcho());
        obj.put("out", getOut());
        obj.put("in", getIn());
        obj.put("log", getLog());
        
        // Event Display Parameters
        JSONArray etypeArray = new JSONArray();
        for(int i = 0; i < NUM_EVENT_PARAMETERS; i++)
            {
            etypeArray.put(i, eventParameterType[i]);
            }
        obj.put("eparam", etypeArray);
        JSONArray emsbArray = new JSONArray();
        for(int i = 0; i < NUM_EVENT_PARAMETERS; i++)
            {
            emsbArray.put(i, eventParameterMSB[i]);
            }
        obj.put("emsb", emsbArray);
        JSONArray elsbArray = new JSONArray();
        for(int i = 0; i < NUM_EVENT_PARAMETERS; i++)
            {
            elsbArray.put(i, eventParameterLSB[i]);
            }
        obj.put("elsb", elsbArray);


        // All Events and Notes
        JSONArray eventsArray = new JSONArray();
        for(Event event : events)
            {
            eventsArray.put(event.save());
            }
        obj.put("events", eventsArray);
        }



    static int document = 0;
    static int counter = 1;
    public int getNextCounter() { if (document < Seq.getDocument()) { document = Seq.getDocument(); counter = 1; } return counter++; }
        
    public String getBaseName() { return "Notes"; }










    ////// UNTESTED
    ////// BINARY SEARCH METHODS




    // FIXME: UNTESTED
    // Finds the index of the given event using binary search
    public int indexOf(Event event)
        {
        if (events.size() == 0) return -1;
        
        int low = 0;
        int high = events.size() - 1;
        Event mid = null;
        while(low <= high)
            {
            int middle = (low + high) / 2;
            mid = events.get(middle);
            if (mid.when == event.when)
                {
                return middle;
                }
            else if (mid.when < event.when)
                {
                low = middle + 1;
                }
            else
                {
                high = middle - 1;
                }
            }
        return -1;              // FAILURE
        }

    // FIXME: UNTESTED
    // Finds the first event whose onset is at or after the given time, using binary search
    public int firstEventStartingAtOrAfter(int time)
        {
        if (events.size() == 0) return -1;
        if (events.get(events.size() - 1).when > time) return -1;
        
        // First find an event at 
        int low = 0;
        int high = events.size() - 1;
        Event mid = null;
        while(low <= high)
            {
            int middle = (low + high) / 2;
            mid = events.get(middle);
            if (mid.when == time)
                {
                break;
                }
            else if (mid.when < time)
                {
                low = middle + 1;
                }
            else
                {
                high = middle - 1;
                }
            }
                
        while (low < events.size() - 1 && events.get(low).when < time)
            {
            low++;
            }
                
        while (low > 0 && events.get(low).when > time)
            {
            low--;
            }
                
        while (low > 0 && events.get(low).when == time)
            {
            if (events.get(low).when == time) low--;
            }
                
        return low;
        }

    // FIXME: UNTESTED
    // Finds the last event whose onset is before the given time, using binary search
    public int lastEventEndingLessThan(int time)
        {
        if (events.size() == 0) return -1;
        
        // First find an event at 
        int low = 0;
        int high = events.size() - 1;
        Event mid = null;
        while(low <= high)
            {
            int middle = (low + high) / 2;
            mid = events.get(middle);
            int point = mid.when + (mid instanceof Note ? ((Note)mid).length : 0);
            if (point == time)
                {
                break;
                }
            else if (point < time)
                {
                low = middle + 1;
                }
            else
                {
                high = middle - 1;
                }
            }
                
        while (low > 0 && events.get(low).when + (mid instanceof Note ? ((Note)mid).length : 0) > time)
            {
            low--;
            }
                
        while (low < events.size() - 1 && events.get(low).when + (mid instanceof Note ? ((Note)mid).length : 0) < time)
            {
            if (events.get(low).when + (mid instanceof Note ? ((Note)mid).length : 0) >= time) break;
            low++;
            }
                
        return low;
        }

    
    
    }
