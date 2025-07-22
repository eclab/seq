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

public class Notes extends Motif
    {
    private static final long serialVersionUID = 1;

    public static final String MIDI_FILE_EXTENSION = ".mid";
    public static final int NUM_EVENT_PARAMETERS = 4;
    int end = 0;

        
    // TYPES are in the following order:
    // 0-127            Note by Pitch
    // 128-255          CC
    // 256-383          Poly Aftertouch by Pitch
    // 384                      Channel Aftertouch
    // 385                      Pitch Bend
    // 386-16383        [RESERVED] -- future use, 16-bit CC, PC, and sysex perhaps
    // 16384-32767      NRPN
    // 32768-49151      RPN
    
    public static final int TYPE_NOTE = 0;
    public static final int TYPE_CC = 128;
    public static final int TYPE_POLYPHONIC_AFTERTOUCH = 256;
    public static final int TYPE_CHANNEL_AFTERTOUCH = 384;
    public static final int TYPE_PITCH_BEND = 385;
    public static final int TYPE_NRPN = 16384;
    public static final int TYPE_RPN = 32768;
    
    public static final String[] NOTES = { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B" };
    
    public static Event buildEvent(int type, int when, double value)
        {
        if (!isValidType(type)) return null;
        if (type < TYPE_CC) return new Note(type - TYPE_NOTE, (int)(value * 128), when, 0);
        if (type < TYPE_POLYPHONIC_AFTERTOUCH) return new CC(type - TYPE_CC, (int)(value * 128), when);
        if (type < TYPE_CHANNEL_AFTERTOUCH) return new Aftertouch(type - TYPE_POLYPHONIC_AFTERTOUCH, (int)(value * 128), when);
        if (type == TYPE_CHANNEL_AFTERTOUCH) return new Aftertouch((int)(value * 128), when);
        if (type == TYPE_PITCH_BEND) return new Bend((int)(value * 16384) - 8192, when);
        if (type < TYPE_RPN) return new NRPN(type - TYPE_NRPN, (int)(value * 16384), when);
        return new RPN(type - TYPE_RPN, (int)(value * 16384), when);
        }
    
    public static String getTypeInitialShort(int type)  
        {
        if (!isValidType(type)) return "ERR";
        if (type < TYPE_CC) return "Note";
        if (type < TYPE_POLYPHONIC_AFTERTOUCH) return "CC";
        if (type < TYPE_CHANNEL_AFTERTOUCH) return "P AT";
        if (type == TYPE_CHANNEL_AFTERTOUCH) return "Ch AT";
        if (type == TYPE_PITCH_BEND) return "Bend";
        if (type < TYPE_RPN) return "NRPN";
        return "RPN";
        }

    public static String getTypeInitial(int type)       
        {
        if (!isValidType(type)) return "<error>";
        if (type < TYPE_CC) return "Note";
        if (type < TYPE_POLYPHONIC_AFTERTOUCH) return "Control Change";
        if (type < TYPE_CHANNEL_AFTERTOUCH) return "Polyphonic Aftertouch";
        if (type == TYPE_CHANNEL_AFTERTOUCH) return "Channel Aftertouch";
        if (type == TYPE_PITCH_BEND) return "Pitch Bend";
        if (type < TYPE_RPN) return "NRPN";
        return "RPN";
        }

    public static String getTypeFinal(int type) 
        {
        if (!isValidType(type)) return "<error>";
        if (type < TYPE_CC) return "" + (type - TYPE_NOTE);
        if (type < TYPE_POLYPHONIC_AFTERTOUCH) return "" + (type - TYPE_CC);
        if (type < TYPE_CHANNEL_AFTERTOUCH) return NOTES[(type - TYPE_POLYPHONIC_AFTERTOUCH) % 12] + ((type - TYPE_POLYPHONIC_AFTERTOUCH) / 12);
        if (type == TYPE_CHANNEL_AFTERTOUCH) return "";
        if (type == TYPE_PITCH_BEND) return "";
        if (type < TYPE_RPN) return "" + (type - TYPE_NRPN);
        return "" + (type - TYPE_RPN);
        }

    public static String getTypeName(int type)   
        {
        if (!isValidType(type)) return "<error>";

        String header = getTypeInitial(type);
        String footer = getTypeFinal(type);
        
        if (type < TYPE_CC) return header + " " + footer;
        if (type < TYPE_POLYPHONIC_AFTERTOUCH) return header + " " + footer;
        if (type < TYPE_CHANNEL_AFTERTOUCH) return header + " " + footer;
        if (type == TYPE_CHANNEL_AFTERTOUCH) return header;
        if (type == TYPE_PITCH_BEND) return header;
        if (type < TYPE_RPN) return header + " " + footer;
        return header + " " + footer;
        }
        

    public static boolean isValidType(int type)
        {
        if (type < TYPE_NOTE) return false;
        if (type > TYPE_PITCH_BEND && type < TYPE_NRPN) return false;
        if (type > TYPE_RPN + 16384) return false;
        return true;
        }

    public abstract static class Event
        {
        public static final int MAX_LENGTH = Seq.PPQ * 256;
        public int when;
        public int length;
        // Is this event selected?  This is stored here even though it's not part of the model
        // because the NoteUI etc. objects where it would more naturally be stored can get
        // destroyed and rebuilt from the Event at any time and need to recover this information.
        public boolean selected;
        public Event(int when, int length)
            {
            this.when = when;
            this.length = length;
            }
        public Event(JSONObject obj)
            {
            when = obj.optInt("w", 0);
            length = obj.optInt("l", 0);
            }
                        
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
        public abstract void write(Track track, Notes notes) throws InvalidMidiDataException;
                        
        public JSONObject save() throws JSONException
            {
            JSONObject obj = new JSONObject();
            obj.put("w", when);
            obj.put("l", length);
            return obj;
            }
        
        public abstract Event copy();
        
        public abstract double getNormalizedValue(boolean log);
        public abstract void setNormalizedValue(double value, boolean log);
        public abstract int getParameter();
        public abstract int getType();
        }

    public static class Note extends Event
        {
        public int pitch;
        public int velocity;
        public int release;             // release velocity, default is 0x40 (64)
        public Note(int pitch, int velocity, int when, int length, int release)
            {
            super(when, length);
            this.pitch = pitch;
            this.velocity = velocity;
            this.release = release;
            }
        public Note(int pitch, int velocity, int when, int length)
            {
            super(when, length);
            this.pitch = pitch;
            this.velocity = velocity;
            this.release = 64;
            }
        public Note(JSONObject obj)
            {
            super(obj);
            pitch = obj.optInt("p", 0);
            velocity = obj.optInt("v", 64);
            release = obj.optInt("r", 64);
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
            return obj;
            }
        
        public String toString() { return NOTES[pitch % 12] + (pitch / 12) + " : " +  velocity; }       // We don't include Length because it would appear in the table

        public double getNormalizedValue(boolean log) { return velocity / 128.0; }
        public void setNormalizedValue(double value, boolean log) { velocity = (int)(value * 128); }
        public int getParameter() { return pitch; }
        public int getType() { return TYPE_NOTE + pitch; }
        }
        
    public static class Bend extends Event
        {
        public int value;
        public Bend(int value, int when)
            {
            super(when, 0);
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
        public String toString() { return "Bend[" + value + "]"; }

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
			
        public double getNormalizedValue(boolean log) { if (log) return getNormalizedLogValue(); else return (value + 8192) / 16384.0; }
        public void setNormalizedValue(double value, boolean log) { if (log) setNormalizedLogValue(value); else this.value = ((int)(value * 16384) - 8192); }
        public int getParameter() { return 0; }
        public int getType() { return TYPE_PITCH_BEND; }
        }

    public static class NRPN extends Event
        {
        public int parameter;
        public int value;
        public NRPN(int parameter, int value, int when)
            {
            super(when, 0);
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
        public String toString() { return "NRPN[" + parameter + "->" + value + "]"; }

        public double getNormalizedValue(boolean log) { return value / 16384.0; }
        public void setNormalizedValue(double value, boolean log) { this.value = (int)(value * 16384); }
        public int getParameter() { return parameter; }
        public int getType() { return TYPE_NRPN + parameter; }
        }

    public static class RPN extends Event
        {
        public int parameter;
        public int value;
        public RPN(int parameter, int value, int when)
            {
            super(when, 0);
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
        public String toString() { return "RPN[" + parameter + "->" + value + "]"; }

        public double getNormalizedValue(boolean log) { return value / 16384.0; }
        public void setNormalizedValue(double value, boolean log) { this.value = (int)(value * 16384); }
        public int getParameter() { return parameter; }
        public int getType() { return TYPE_RPN + parameter; }
        }

    public static class CC extends Event
        {
        public int parameter;
        public int value;
        public CC(int parameter, int value, int when)
            {
            super(when, 0);
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
        public String toString() { return "CC[" + parameter + "->" + value + "]"; }

        public double getNormalizedValue(boolean log) { return value / 128.0; }
        public void setNormalizedValue(double value, boolean log) { this.value = (int)(value * 128); }
        public int getParameter() { return parameter; }
        public int getType() { return TYPE_CC + parameter; }
        }

    public static class Aftertouch extends Event
        {
        public int pitch;
        public int value;
        public Aftertouch(int pitch, int value, int when)
            {
            super(when, 0);
            this.value = value;
            this.pitch = pitch;
            }
        public Aftertouch(int value, int when)
            {
            super(when, 0);
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
        public String toString() { return "AT[" + 
                    (pitch == Out.CHANNEL_AFTERTOUCH ? "" : 
                    "" + (NOTES[pitch % 12] + (pitch / 12)) + "->")  + value + "]"; }

        public double getNormalizedValue(boolean log) { return value / 128.0; }
        public void setNormalizedValue(double value, boolean log) { this.value = (int)(value * 128); }
        public int getParameter() { return pitch; }
        public int getType() { return (pitch == Out.CHANNEL_AFTERTOUCH ? 384 : TYPE_POLYPHONIC_AFTERTOUCH + pitch); }
        }
        


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
                
        
        
    ArrayList<Event> events = new ArrayList<>();                // FIXME maybe this should be a list of lists to allow for fragmentation
    ArrayList<Event> recording = new ArrayList<>();
    boolean recordBend;
    boolean recordCC;
    boolean recordAftertouch;
    boolean convertNRPNRPN;
    int out;
    int in;
    int maxEventPosition = 0;
    int maxNoteOnPosition = 0;
    int maxNoteOffPosition = 0;
    // Do I play out notes as I am recording them?
    boolean echo = true;
    // Do I display bend as a log or linearly?
    boolean log = true;
    
    public boolean getLog() { return log; }
    public void setLog(boolean val) { log = val; }
        
    
    /*
      public static final int NO_MIDI_PARAMETER = 0;
      public static final int BEND = 1;
      public static final int CC_7 = 2;
      public static final int CC_14 = 3;
      public static final int NRPN = 4;
      public static final int RPN = 5;
      int[] midiParameterType = new int[NUM_PARAMETERS];
      int[] midiParameterMSB = new int[NUM_PARAMETERS];
      int[] midiParameterLSB = new int[NUM_PARAMETERS];
    */

    public static final int EVENT_PARAMETER_NONE = 0;
    public static final int EVENT_PARAMETER_CC = 1;
    public static final int EVENT_PARAMETER_POLY_AT = 2;
    public static final int EVENT_PARAMETER_CHANNEL_AT = 3;
    public static final int EVENT_PARAMETER_BEND = 4;
    public static final int EVENT_PARAMETER_NRPN = 5;
    public static final int EVENT_PARAMETER_RPN = 6;
    
    int[] eventParameterType = new int[NUM_EVENT_PARAMETERS];
    int[] eventParameterMSB = new int[NUM_EVENT_PARAMETERS];
    int[] eventParameterLSB = new int[NUM_EVENT_PARAMETERS];

    /*
      public int getMIDIParameterType(int param) { return midiParameterType[param]; }
      public void setMIDIParameterType(int param, int val) { midiParameterType[param] = val; }
      public int getMIDIParameterMSB(int param) { return midiParameterMSB[param]; }
      public void setMIDIParameterMSB(int param, int val) { midiParameterMSB[param] = val; }
      public int getMIDIParameterLSB(int param) { return midiParameterLSB[param]; }
      public void setMIDIParameterLSB(int param, int val) { midiParameterLSB[param] = val; }
    */
        
    public int getEventParameterType(int param) { return eventParameterType[param]; }
    public void setEventParameterType(int param, int val) { eventParameterType[param] = val; }
    public int getEventParameterMSB(int param) { return eventParameterMSB[param]; }
    public void setEventParameterMSB(int param, int val) { eventParameterMSB[param] = val; }
    public int getEventParameterLSB(int param) { return eventParameterLSB[param]; }
    public void setEventParameterLSB(int param, int val) { eventParameterLSB[param] = val; }

    
    public boolean getRecordBend() { return recordBend; }
    public void setRecordBend(boolean val) { recordBend = val; Prefs.setLastBoolean("seq.motif.notes.Notes.recordbend", val); }

    public boolean getRecordCC() { return recordCC; }
    public void setRecordCC(boolean val) { recordCC = val; Prefs.setLastBoolean("seq.motif.notes.Notes.recordcc", val); }

    public boolean getConvertNRPNRPN() { return convertNRPNRPN; }
    public void setConvertNRPNRPN(boolean val) { convertNRPNRPN = val; Prefs.setLastBoolean("seq.motif.notes.Notes.convertNRPNRPN", val); }

    public boolean getRecordAftertouch() { return recordAftertouch; }
    public void setRecordAftertouch(boolean val) { recordAftertouch = val; Prefs.setLastBoolean("seq.motif.notes.Notes.recordaftertouch", val); }

    public int getOut() { return out; }
    public void setOut(int val) { out = val; Prefs.setLastOutDevice(0, val, "seq.motif.notes.Notes"); }

    public int getIn() { return in; }
    public void setIn(int val) { in = val; Prefs.setLastInDevice(0, val, "seq.motif.notes.Notes"); }
        
    public boolean getEcho() { return echo; }
    public void setEcho(boolean val) { echo = val; }
    
    public Motif copy()
        {
        Notes other = (Notes)(super.copy());
        other.events = new ArrayList();
        for(Event event : events)
            other.events.add(event.copy());
        clearRecording();
        return other;
        }
        
    public Notes(Seq seq) { this(seq, false); }         // this version is called by the file loader
    
    public Notes(Seq seq, boolean arm)                          // this version is called when we make a Notes window
        {
        super(seq);

        // Load devices. Note I'm not using setOut(...) etc. which would write the device to prefs
        out = (Prefs.getLastOutDevice(0, "seq.motif.notes.Notes"));
        in = (Prefs.getLastInDevice(0, "seq.motif.notes.Notes"));
        setArmed(arm);
        recordBend = Prefs.getLastBoolean("seq.motif.notes.Notes.recordbend", true); 
        recordCC = Prefs.getLastBoolean("seq.motif.notes.Notes.recordcc", true); 
        recordAftertouch = Prefs.getLastBoolean("seq.motif.notes.Notes.recordaftertouch", true); 
        convertNRPNRPN = Prefs.getLastBoolean("seq.motif.notes.Notes.convertNRPNRPN", true); 
        // for(int i = 0; i < NUM_PARAMETERS; i++) { midiParameterType[i] = NO_MIDI_PARAMETER; }
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
    
    public boolean setEvents(ArrayList<Event> val) 
        {
        if (getConvertNRPNRPN())
            {
            EventParser parser = new EventParser(val, true);
            events = parser.getParsedEvents();
            computeMaxTime();     
            return parser.getError();    
            }
        else
            {
            events = val;
            computeMaxTime();     
            return true;    
            }
        }

    public void clearRecording()
        {
        recording = new ArrayList<Event>();
        }

    public ArrayList<Event> getRecording()
        {
        return recording;
        }
        
    public void setEnd(int end) { this.end = end; }
    public int getEnd() { return end; }
    
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
                
    public int getMaxEventPosition() { return maxEventPosition; }
    public int getMaxNoteOnPosition() { return maxNoteOnPosition; }
    public int getMaxNoteOffPosition() { return maxNoteOffPosition; }
        
    /// FIXME: did I break this?
    
    //// FIXME: This feels wrong.  maxNoteOffPosition is the time we do a release.
    //// If it is exactly at a certain beat, then wouldn't we actually be doing the first
    //// tick of the next motif AFTER that beat?  Same thing for end...
    public int getEndTime()
        {
        int maxPos = (maxNoteOffPosition > maxEventPosition ? maxNoteOffPosition : maxEventPosition);
        int endTime = (maxPos > end ? maxPos : (end >= 0 ? end : 0)) - 1;                       // FIXME: So I'm subtracting 1....
        if (endTime < 0) endTime = 0;
        return endTime;
        }
        
    public void sortEvents()
        {
        sortEvents(events);
        }
    

    public void sortEvents(ArrayList<Event> events)
        {
        Collections.sort(events, new Comparator<Event>()
                {
                public int compare(Event o1, Event o2) { return o1.when - o2.when; }
                public boolean equals(Comparator<Event> other) { return (this == other); }
            });
        }
    
    /** Removes elements by index, not by time */
/*
  public ArrayList<Event> remove(int[] indices)        // endIndex is inclusive
  {
  ArrayList<Event> newEvents = new ArrayList<Event>();
  ArrayList<Event> cut = new ArrayList<Event>();

  HashSet<Integer> hash = buildIndexHash(indices);
        
  for(int i = 0; i < events.size(); i++)
  {
  if (hash.contains(i))
  {
  Event event = events.get(i);
  cut.add(event);
  }
  else
  {
  Event event = events.get(i);
  newEvents.add(event);
  }
  }
  events = newEvents;
  computeMaxTime();
  return cut;
  }
*/

    /** Remove multiple elements */
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

    /** Removes elements by index, not by time */
    public ArrayList<Event> filter(boolean removeNotes, boolean removeBend, boolean removeCC, boolean removeNRPN, boolean removeRPN, boolean removeAftertouch)
        {
        ArrayList<Event> newEvents = new ArrayList<Event>();
        ArrayList<Event> cut = new ArrayList<Event>();
        
        for(Event event : events)
            {
            if (
                (event instanceof Note && removeNotes) ||
                (event instanceof Bend && removeBend) ||
                (event instanceof CC && removeCC) ||
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
    
    /** Removes elements by index, not by time */
    public ArrayList<Event> filter(ArrayList<Event> eventsIn, boolean removeNotes, boolean removeBend, boolean removeCC, boolean removeNRPN, boolean removeRPN, boolean removeAftertouch)        // endIndex is inclusive
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
    
    public ArrayList<Event> cut(int start, int end, boolean allowStartOverlaps, boolean allowEndOverlaps)
        {
        ArrayList<Event> newEvents = new ArrayList<Event>();
        ArrayList<Event> cut = new ArrayList<Event>();

        // remove overlapping events
        for(int i = 0; i < events.size(); i++)
            {
            Event event = events.get(i);
            // If fully outside...
            if (event.when + event.length < start || event.when >= end)
                {
                newEvents.add(event);
                }
            // if started early but we're allowing start overlaps
            else if (event.when < start && event.when + event.length >= start && allowStartOverlaps)
                {
                newEvents.add(event);
                }
            else if (event.when < end && event.when + event.length >= end && allowEndOverlaps)
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

    public ArrayList<Event> copy(int start, int end, boolean allowStartOverlaps, boolean allowEndOverlaps)
        {
        ArrayList<Event> copy = new ArrayList<Event>();

        // remove overlapping events
        for(int i = 0; i < events.size(); i++)
            {
            Event event = events.get(i);
            // If fully outside...
            if (event.when + event.length < start || event.when >= end)
                {
                // do nothing
                }
            // if started early but we're allowing start overlaps
            else if (event.when < start && event.when + event.length >= start && allowStartOverlaps)
                {
                // do nothing
                }
            else if (event.when < end && event.when + event.length >= end && allowEndOverlaps)
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

    public void merge(ArrayList<Event> from)
        {
        if (from.isEmpty()) return;
        ArrayList<Event> newEvents = new ArrayList<Event>();
        int plen = events.size();
        int rlen = from.size();
        int pi = 0;
        int ri = 0;
        while(pi < plen && ri < rlen)
            {
            if (pi == plen)
                {
                Event r = from.get(ri);
                newEvents.add(r);
                ri++;
                }
            else if (ri == rlen)
                {
                Event p = events.get(pi);
                newEvents.add(p);
                pi++;
                }
            else
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
        computeMaxTime(); 
        events = newEvents;
        }

    public void overwrite(ArrayList<Event> from, boolean allowStartOverlaps, boolean allowEndOverlaps)
        {
        if (from.isEmpty()) return;
        int start = Integer.MAX_VALUE;
        int end = 0;
        for(Event event : from)
            {
            if (start > event.when) start = event.when;
            if (end < event.when + event.length) end = event.when + event.length;
            }
        cut(start, end, allowStartOverlaps, allowEndOverlaps);
        merge(from);
        }


    public void trim()
        {
        if (events.size() > 0) 
            {
            shift(events, 0 - events.get(0).when, false);
            }
        }

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
        sortEvents(events);
        }
        

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
              

    public void stretch(ArrayList<Event> events, int stretchFrom, int stretchTo, boolean lockToStart)
        {
        if (stretchFrom == stretchTo) return;
        if (events.isEmpty()) return;
        
        int startTime = -1;
        Event startEvent = null;
        for(Event event : events)
            {
            if (lockToStart)
                {
                if (startTime == -1 || event.when < startTime)
                    {
                    startTime = event.when;
                    startEvent = event;
                    }
                }
            event.when = (int)Math.round((event.when * stretchTo) / (double)stretchFrom);
            if (event instanceof Note)
                {
                Note note = (Note)event;
                note.length = (int)Math.round((event.length * stretchTo) / (double)stretchFrom);
                }
            }
            
        if (lockToStart && startTime != -1)
            {
            for(Event event : events)
                {
                event.when -= (startEvent.when - startTime);    // reset to start time
                }
            }
        
        sortEvents(events);
        computeMaxTime(); 
        }

    public Clip buildClip(Clip parent)
        {
        return new NotesClip(seq, this, parent);
        }

    public void read(File midiFile) { try { read(MidiSystem.getSequence(midiFile)); } catch (InvalidMidiDataException ex) { }  catch (IOException ex) { }}
    public void read(InputStream stream) { try { read(MidiSystem.getSequence(stream));  } catch (InvalidMidiDataException ex) { }  catch (IOException ex) { }}
    public void read(URL url) { try { read(MidiSystem.getSequence(url));  } catch (InvalidMidiDataException ex) { } catch (IOException ex) { }}
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
    
    //// INDEX SET MANIPULATION
    
    // Build an array of indices from FROM to TO inclusive
    int[] buildIndices(int from, int to)
        {
        int[] idx = new int[to - from + 1];
        for(int i = from; i <= to; i++) idx[i] = i;
        return idx;
        }

    // Return the minimum index among the provided indices
    int minimum(int[] indices)
        {
        if (indices.length == 0) return 0;      // uhm...
        int min = indices[0];
        for(int i = 1; i < indices.length; i++)
            {
            if (indices[i] < min) min = indices[i];
            }
        return min;
        }

    // Return as HashSet of the given indices
    HashSet<Integer> buildIndexHash(int[] indices)
        {
        HashSet<Integer> hash = new HashSet<>();
        for(int i : indices)
            {
            hash.add(i);
            }
        return hash;
        }
        
    
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
                noteOffPos = event.when + event.length;
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
                event.length = noteOffPos - event.when;
                if (event.length < 0)
                    {
                    System.err.println("QUANTIZE ERROR: event length went negative: " + event.length); 
                    event.length = 0;
                    }
                }
            }
        sortEvents(events);
        computeMaxTime(); 
        }

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
                int val = event.length + getRandomTimeNoise(random, max);
                while(val < 0)
                    {
                    val = event.length + getRandomTimeNoise(random, max);
                    }
                event.length = val;
                }
            }
        sortEvents(events);     
        computeMaxTime(); 
        }

    public void setVelocity(int val)
        {
        setVelocity(events, val);
        }

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

    int getRandomVelocityNoise(Random random, double max)
        {
        return (int)((random.nextDouble() * 127 - 63) * max);           // So we have enough space to fit in above 1...
        }
                
    /** Quantize the velocity, and possibly release velocity, of all events, possibly including non-notes, by the given
        variance (from 0..255). */
    public void randomizeVelocity(double max, boolean randomizeReleases, Random random)
        {
        randomizeVelocity(events, max, randomizeReleases, random);
        }

    static final int NUM_TRIES = 8;
        
    /** Quantize the velocity, and possibly release velocity, of all events, possibly including non-notes, by the given
        variance (from 0..255), in the event range from FROM to TO. */
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


    // FIXME: UNTESTED
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

    
    int lastNRPN = -1;
    boolean wasRPN = false;
    
    Sequence write() throws InvalidMidiDataException
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

    public void write(File out) throws IOException, InvalidMidiDataException
        {
        MidiSystem.write(write(), 0, out);
        }

    public void write(OutputStream out) throws IOException, InvalidMidiDataException
        {
        MidiSystem.write(write(), 0, out);
        }

    public void load(JSONObject obj) throws JSONException
        {
        setEnd(obj.optInt("end", 0));
        setEcho(obj.optBoolean("echo", false));
        setOut(obj.optInt("out", 0));
        setIn(obj.optInt("in", 0));
        
        /*
          JSONArray typeArray = obj.optJSONArray("mtype");
          if (typeArray != null)
          {
          for(int i = 0; i < NUM_PARAMETERS; i++)
          {
          midiParameterType[i] = typeArray.optInt(i, 0);
          }
          JSONArray msbArray = obj.getJSONArray("msb");
          for(int i = 0; i < NUM_PARAMETERS; i++)
          {
          midiParameterMSB[i] = msbArray.optInt(i, 0);
          }
          JSONArray lsbArray = obj.getJSONArray("lsb");
          for(int i = 0; i < NUM_PARAMETERS; i++)
          {
          midiParameterLSB[i] = lsbArray.optInt(i, 0);
          }
          }
        */
            
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
        
    public void save(JSONObject obj) throws JSONException
        {
        obj.put("end", getEnd());
        obj.put("echo", getEcho());
        obj.put("out", getOut());
        obj.put("in", getIn());
        
        /*
        // MIDI Parameters
        JSONArray typeArray = new JSONArray();
        for(int i = 0; i < NUM_PARAMETERS; i++)
        {
        typeArray.put(i, midiParameterType[i]);
        }
        obj.put("mtype", typeArray);
        JSONArray msbArray = new JSONArray();
        for(int i = 0; i < NUM_PARAMETERS; i++)
        {
        msbArray.put(i, midiParameterMSB[i]);
        }
        obj.put("msb", msbArray);
        JSONArray lsbArray = new JSONArray();
        for(int i = 0; i < NUM_PARAMETERS; i++)
        {
        lsbArray.put(i, midiParameterLSB[i]);
        }
        obj.put("lsb", lsbArray);
        */

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
    }
