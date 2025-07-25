/***
    Copyright 2017 by Sean Luke
    Licensed under the Apache License version 2.0
*/

package seq.engine;

import seq.util.*;
import seq.gui.*;
import javax.sound.midi.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.lang.reflect.*;
import org.json.*;

/**** 
      Static class which contains methods for handling the global MIDI device facility.
      Edisyn uses a single MIDI device repository created (presently) at launch time
      to which synth panels may retrieve transmitters and receivers. This is done because
      the original approach (letting each synth panel build its own devices) triggers
      low-level bugs in the OS X Java CoreMIDI4J implementation which hangs the system.
      The disadvantage of the current approach is that (presently) you must have your
      devices' USB connections plugged in BEFORE you launch Edisyn.  Otherwise it's not too bad.

      @author Sean Luke
*/



public class Midi
    {
    private static final long serialVersionUID = 1;

    public static int numOutDevices = 1;                // don't like these being static
    public static int numInDevices = 1;                         // don't like these being static
    public static final int OMNI = 0;                   // for input channels
        
    public Midi(int numOutDevices, int numInDevices)
        {
        this.numOutDevices = numOutDevices;
        this.numInDevices = numInDevices;
        }
                
    /** Thru is A MIDI pipe.  Thru is a Receiver which attaches to other
        Receivers.  When it gets a message, it forwards it to ALL
        the other receivers.  Additionally, sending is synchronized,
        so you can guaranted that if multiple transmitters send to
        the Thru, they won't have a race condition. */
                
    public static class Thru implements Receiver
        {
        ArrayList receivers = new ArrayList();
                
        public synchronized void close()
            {
            removeAllReceivers();
            }
                        
        public synchronized void send(MidiMessage message, long timeStamp)
            {
            for(int i = 0; i < receivers.size(); i++)
                {
                try { ((Receiver)(receivers.get(i))).send(message, timeStamp); }
                catch (Exception ex) { System.err.println(ex); }
                }
            }
                        
        /** Add a receiver to get routed to. */
        public synchronized void addReceiver(Receiver receiver)
            {
            //System.err.println("" + this + " addReceiver " + receiver + " " + receiver.getClass());
            receivers.add(receiver);
            }
                        
        /** Sets the only receiver to get routed to. */
        public synchronized void setReceiver(Receiver receiver)
            {
            //System.err.println("" + this + " setReceiver " + receiver);
            removeAllReceivers();
            receivers.add(receiver);
            }
                        
        /** Remove a receiver that was routed to. */
        public synchronized void removeReceiver(Receiver receiver)
            {
            //System.err.println("" + this + " removeReceiver " + receiver);
            receivers.remove(receiver);
            }

        /** Remove all receivers. */
        public synchronized void removeAllReceivers()
            {
            //System.err.println("" + this + " removeAllReceivers ");
            for(int i = 0; i < receivers.size(); i++)
                {
                //System.err.println("Closing " +  ((Receiver)(receivers.get(i))));
                ((Receiver)(receivers.get(i))).close();
                }
            receivers = new ArrayList();
            }
        }


    /** MidiDeviceWrapper is a wrapper for a MIDI device which displays its name 
        in a pleasing and useful format for the user.  Additionally the wrapper 
        wraps the transmitter and receiver for the device with two Thrus so you can
        attach multiple transmitters and receivers to it: these two Thrus are also
        threadsafe. You can thus add transmitters and receivers to the MidiDeviceWrapper,
        and remove the same, using methods in this class.
    */
                
    public static class MidiDeviceWrapper
        {
        MidiDevice device;
        Thru in;
        Thru out;
        Transmitter transmitter;
        Receiver receiver;

        public MidiDeviceWrapper(MidiDevice device)
            {
            this.device = device;
            }
                                    
        public String toString() 
            { 
            String desc = device.getDeviceInfo().getDescription().trim();
            String name = device.getDeviceInfo().getName();
            
            if (name == null) 
                name = "";
            if (desc == null || desc.equals("")) 
                desc = "MIDI Device";
                
            if (Platform.isUnix()) // Linux names don't permit spaces, so we need the description instead, which is of the form A, B, A
                {
                String[] descs = desc.split(",");
                String[] names = name.split(" ");
                String d = (descs.length > 1 ? descs[0] : desc);
                String n = (names.length > 1 ? names[1] : "(" + names + ")");
                name = d + " " + n;
                }
                
            // All CoreMIDI4J names begin with "CoreMIDI4J - "
            if (name.startsWith("CoreMIDI4J - "))
                name = name.substring(13).trim();
            else
                name = name.trim();

            if (name.equals(""))
                return desc.trim(); 
            else 
                return name;
            }
                
        public MidiDevice getDevice() { return device; }

        /** 
            You provide a Receiver to attach to the device's Transmitter.  Returns true if successful
        */
        public boolean addToTransmitter(Receiver receiver) 
            { 
            if (in == null) 
                {
                try
                    {
                    // we use a thru here so we can add many receivers to it
                    if (!device.isOpen()) 
                        device.open();
                    Thru _in = new Thru();
                    Transmitter _transmitter = device.getTransmitter();
                    _transmitter.setReceiver(_in);
                    transmitter = _transmitter;         // we set it last in case of an exception
                    in = _in;                           // we set it last in case of an exception
                    }
                catch(Exception e) { ExceptionDump.handleException(e); return false; }
                }
            
            in.addReceiver(receiver);
            return true;
            }

        public boolean removeFromTransmitter(Receiver receiver) 
            {
            if (in == null) 
                {
                try
                    {
                    // we use a thru here so we can add many receivers to it
                    if (!device.isOpen()) 
                        device.open();
                    Thru _in = new Thru();
                    Transmitter _transmitter = device.getTransmitter();
                    _transmitter.setReceiver(_in);
                    transmitter = _transmitter;         // we set it last in case of an exception
                    in = _in;                                           // we set it last in case of an exception
                    }
                catch(Exception e) { ExceptionDump.handleException(e); return false; }
                }
            
            in.removeReceiver(receiver);
            return true;
            }

        public boolean removeAllFromTransmitter() 
            {
            if (in == null) 
                {
                try
                    {
                    // we use a thru here so we can add many receivers to it
                    if (!device.isOpen()) 
                        device.open();
                    Thru _in = new Thru();
                    Transmitter _transmitter = device.getTransmitter();
                    _transmitter.setReceiver(_in);
                    transmitter = _transmitter;         // we set it last in case of an exception
                    in = _in;                                           // we set it last in case of an exception
                    }
                catch (MidiUnavailableException ex)     // no input sources at all
                    {
                    return false;
                    }
                catch(Exception e) { ExceptionDump.handleException(e); return false; }
                }
            
            in.removeAllReceivers();
            return true;
            }

        /** 
            You provide a Receiver to solely attach to the Transmitter.  Returns true if successful
        */
        public boolean connectToTransmitter(Receiver receiver) 
            {
            if (in == null) 
                {
                try
                    {
                    // we use a thru here so we can add many receivers to it
                    if (!device.isOpen()) 
                        device.open();
                    Thru _in = new Thru();
                    Transmitter _transmitter = device.getTransmitter();
                    _transmitter.setReceiver(_in);
                    transmitter = _transmitter;         // we set it last in case of an exception
                    in = _in;                                           // we set it last in case of an exception
                    }
                catch (MidiUnavailableException ex)     // no input sources at all
                    {
                    return false;
                    }
                catch(Exception e) { ExceptionDump.handleException(e); return false; }
                }
            
            in.removeAllReceivers();
            in.addReceiver(receiver);
            return true;
            }
                        
        /** Returns a threadsafe Receiver, or null if not successful. */
        public Receiver getReceiver() 
            { 
            if (out == null) 
                {
                try
                    {
                    // we use a secret Thru here so it's lockable
                    if (!device.isOpen()) 
                        device.open();
                    Thru _out = new Thru();
                    Receiver _receiver = device.getReceiver();
                    _out.addReceiver(_receiver);
                    receiver = _receiver;
                    out = _out;         // we set it last in case of an exception
                    }
                catch(Exception e) { ExceptionDump.handleException(e); return null; }
                }

            return out; 
            }
        
        public void close()
            {
            if (transmitter != null) transmitter.close();
            if (out != null) out.close();
            // don't close "in", it'll just close all my own receivers
            }
        }


    static Object findDevice(String name, ArrayList devices)
        {
        if (name == null) return null;
        for(int i = 0; i < devices.size(); i++)
            {
            if (devices.get(i) instanceof String)
                {
                if (((String)devices.get(i)).equals(name))
                    return devices.get(i);
                }
            else
                {
                MidiDeviceWrapper mdn = (MidiDeviceWrapper)(devices.get(i));
                if (mdn.toString().equals(name))
                    return mdn;
                }
            }
        return null;
        }

    /**
     * Returns all incoming MIDI Devices
     */
    public ArrayList getInDevices()             // can also include "None"
        {
        updateDevices();
        return inDevices;
        }

    /**
     * Returns all incoming MIDI Devices
     */
    public ArrayList getOutDevices()            // can also include "None"
        {
        updateDevices();
        return outDevices;
        }

    // Returns all MIDI Devices period, incoming or outgoing */
    ArrayList getAllDevices()                   // can also include "None"
        {
        updateDevices();
        return allDevices;
        }

    public void displayDevices() 
        {
        System.err.println("MIDI IN DEVICES:");
        ArrayList<MidiDeviceWrapper> in = getInDevices();
        for (int i = 0; i < in.size(); i++)
            System.err.println("" + i + ":\t" + in.get(i));
        System.err.println("MIDI OUT DEVICES:");
        ArrayList<MidiDeviceWrapper> out = getOutDevices();
        for (int i = 0; i < in.size(); i++)
            System.err.println("" + i + ":\t" + out.get(i));
        }



    static void updateDevices()
        {
        MidiDevice.Info[] midiDevices;
        
        if (Platform.isMac())
            //if (true)
            {
            try
                {
                Class c = Class.forName("uk.co.xfactorylibrarians.coremidi4j.CoreMidiDeviceProvider");
                Method m = c.getMethod("getMidiDeviceInfo", new Class[0]);
                midiDevices = (MidiDevice.Info[])(m.invoke(null));
                //                midiDevices = uk.co.xfactorylibrarians.coremidi4j.CoreMidiDeviceProvider.getMidiDeviceInfo();
                }
            catch (Throwable ex)
                {
                System.out.println("WARNING (Midi.java): error on obtaining CoreMIDI4J, but we think we're a Mac.  This should never happen.");
                ExceptionDump.handleException(ex);
                midiDevices = MidiSystem.getMidiDeviceInfo();
                }
            }
        else
            {
            midiDevices = MidiSystem.getMidiDeviceInfo();
            }

        ArrayList allDevices = new ArrayList();
        for(int i = 0; i < midiDevices.length; i++)
            {
            try
                {
                MidiDevice d = MidiSystem.getMidiDevice(midiDevices[i]);
                // get rid of java devices
                if (d instanceof javax.sound.midi.Sequencer ||
                    d instanceof javax.sound.midi.Synthesizer)
                    continue;
                if (d.getMaxTransmitters() != 0 || d.getMaxReceivers() != 0)
                    {
                    allDevices.add(new MidiDeviceWrapper(d));
                    }
                }
            catch(Exception e) { ExceptionDump.postThrowable(e); }
            }
            
        // Do they hold the same exact devices?
        if (Midi.allDevices != null && Midi.allDevices.size() == allDevices.size())
            {
            Set set = new HashSet();
            for(int i = 0; i < Midi.allDevices.size(); i++)
                {
                set.add(((MidiDeviceWrapper)(Midi.allDevices.get(i))).device);
                }
                
            boolean same = true;
            for(int i = 0; i < allDevices.size(); i++)
                {
                if (!set.contains(((MidiDeviceWrapper)(allDevices.get(i))).device))
                    {
                    same = false;  // something's different
                    break;
                    }
                }
                
            if (same)
                {
                return;  // they're identical
                }
            }

        Midi.allDevices = allDevices;

        inDevices = new ArrayList();
        inDevices.add("None");
        for(int i = 0; i < allDevices.size(); i++)
            {
            try
                {
                MidiDeviceWrapper mdn = (MidiDeviceWrapper)(allDevices.get(i));
                if (mdn.device.getMaxTransmitters() != 0)
                    {
                    inDevices.add(mdn);
                    }
                }
            catch(Exception e) { ExceptionDump.postThrowable(e); }
            }

        outDevices = new ArrayList();
        outDevices.add("None");
        for(int i = 0; i < allDevices.size(); i++)
            {
            try
                {
                MidiDeviceWrapper mdn = (MidiDeviceWrapper)(allDevices.get(i));
                if (mdn.device.getMaxReceivers() != 0)
                    {
                    outDevices.add(mdn);
                    }
                }
            catch(Exception e) { ExceptionDump.postThrowable(e); }
            }
        
        sortDevices();
        }

    static ArrayList allDevices;
    static ArrayList outDevices;
    static ArrayList inDevices;
    
    static void sortDevices()
        {
        Comparator c = new Comparator()
            {
            public boolean equals(Object obj) { return false; }
            public int compare(Object o1, Object o2) 
                {
                // this shouldn't happen but just to be safe....
                if (o1 == null) return -1;
                if (o2 == null) return 1;
                        
                // Deal with "None".  It should appear first.
                if (o1 instanceof String && o2 instanceof String) // uh...
                    return ((String)o1).compareTo((String)o2);
                if (o1 instanceof String) return -1;
                if (o2 instanceof String) return 1;
                        
                // Else we have two non-strings
                String str1 = ((MidiDeviceWrapper)o1).toString();
                String str2 = ((MidiDeviceWrapper)o2).toString();
                        
                return str1.compareTo(str2);
                }
            };
                
        allDevices.sort(c);
        outDevices.sort(c);
        inDevices.sort(c);
        }
        
        
    static
        {
        updateDevices();
        }


    /** A tuple is a representation of the current MIDI state: MIDIDevicerWrappers for in-devices and
        out-devices, plus channels for each one of them.  */
                
    public static class Tuple
        {
        /** Represents "any channel" in the Tuple. */
        public static final int IN_CHANNEL_OMNI = 0;

        /** The current output device wrapper */
        public MidiDeviceWrapper[] outWrap;
        /** The channel to send voiced messages to on the output. */
        public int[] outChannel;
        /** Nicknames for outs */
        public String[] outName;
                
        /** The current keyboard/controller input device's wrapper */
        public MidiDeviceWrapper[] inWrap;
        /** The channel to receive voiced messages from on the keyboard/controller input. */
        public int[] inChannel;
        /** Nicknames for ins */
        public String[] inName;
        /** The actual receivers */
        public Receiver[] inReceiver;
        
        public Tuple(MidiDeviceWrapper[] inWrap, int[] inChannel, MidiDeviceWrapper[] outWrap, int[] outChannel, String[] inName, String[] outName)
            {
            this.inWrap = inWrap;
            this.outWrap = outWrap;
            this.inName = inName;
            this.outName = outName;
            this.inChannel = inChannel;
            this.outChannel = outChannel;
            this.inReceiver = new Receiver[inWrap.length];
            }
        
        public Tuple() 
            {
            outWrap = new MidiDeviceWrapper[numOutDevices];
            outChannel = new int[numOutDevices];
            for(int i = 0; i < outWrap.length; i++)
                {
                outWrap[i] = null;
                outChannel[i] = 1;
                }

            inWrap = new MidiDeviceWrapper[numInDevices];
            inChannel = new int[numInDevices];
            for(int i = 0; i < inWrap.length; i++)
                {
                inWrap[i] = null;
                inChannel[i] = IN_CHANNEL_OMNI;
                }
                
            inReceiver = new Receiver[numInDevices];

            outName = new String[numOutDevices];
            inName = new String[numInDevices];
            }
                    
        public void close()
            {
            if (inReceiver != null)
                {
                for(int i = 0; i < inReceiver.length; i++)
                    {
                    inWrap[i].removeFromTransmitter(inReceiver[i]);
                    }
                }
            inReceiver = null;
            }
        }

    public static final Tuple CANCELLED = new Tuple();
    public static final Tuple FAILED = new Tuple();

    static final String[] inChannelOptions = new String[] { "Any", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16" };
    static final String[] outChannelOptions = new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16" };


    public static Tuple loadTupleFromJSON(JSONArray outDevs, JSONArray inDevs, In[] inReceiver)
        {
        updateDevices();
        Tuple tuple = new Tuple();
                                                                
        for(int i = 0; i < numOutDevices; i++)
            {
            JSONObject jsonobj = outDevs.getJSONObject(i);
            
            Object obj = findDevice(jsonobj.optString("dev", "None"), outDevices);
            int channel = jsonobj.optInt("ch", 0);
            String name = jsonobj.optString("name", "");
            if (channel > 0 && obj != null && (obj instanceof MidiDeviceWrapper))
                {
                tuple.outChannel[i] = channel;
                tuple.outWrap[i] = ((MidiDeviceWrapper)(obj));
                tuple.outName[i] = name;
                }
            else
                {
                tuple.outChannel[i] = channel;
                tuple.outWrap[i] = null;
                tuple.outName[i] = name;
                }
            }

        for(int i = 0; i < numInDevices; i++)
            {
            JSONObject jsonobj = inDevs.getJSONObject(i);

            Object obj = findDevice(jsonobj.optString("dev", "None"), inDevices);
            int channel = jsonobj.optInt("ch", 0);
            String name = jsonobj.optString("name", "");
            if (channel > -1 && obj != null && (obj instanceof MidiDeviceWrapper))
                {
                tuple.inChannel[i] = channel;
                tuple.inWrap[i] = ((MidiDeviceWrapper)(obj));
                inReceiver[i].setWrapper(tuple.inWrap[i]);      // do this first so the old one is removed
                tuple.inReceiver[i] = inReceiver[i];
                tuple.inWrap[i].addToTransmitter(inReceiver[i]);
                tuple.inName[i] = name;
                }
            else
                {
                tuple.inChannel[i] = channel;
                tuple.inWrap[i] = null;
                inReceiver[i].setWrapper(tuple.inWrap[i]);
                tuple.inName[i] = name;
                }
            }
                        
        return tuple;
        }

    // assumes outDevs and inDevs are empty
    public static void saveTupleToJSON(Tuple tuple, JSONArray outDevs, JSONArray inDevs)
        {
        for(int i = 0; i < numOutDevices; i++)
            {
            JSONObject obj = new JSONObject();
            outDevs.put(obj);
            if (tuple.outWrap == null || tuple.outWrap[i] == null)
                obj.put("dev", "None");
            else
                obj.put("dev", tuple.outWrap[i].toString());
            obj.put("ch", tuple.outChannel[i]);
            obj.put("name", tuple.outName[i] == null ? "" : tuple.outName[i].trim());
            }
                                                                        
        for(int i = 0; i < numInDevices; i++)
            {
            JSONObject obj = new JSONObject();
            inDevs.put(obj);
            if (tuple.inWrap == null || tuple.inWrap[i] == null)
                obj.put("dev", "None");
            else
                obj.put("dev", tuple.inWrap[i].toString());
            obj.put("ch", tuple.inChannel[i]);
            obj.put("name", tuple.inName[i] == null ? "" : tuple.inName[i].trim());
            }
        }

    /** 
        Loads a tuple from Preferences from the first time, using the provided In[]
    */
    public static Tuple loadTupleFromPreferences(Seq seq, In[] inReceiver)
        {
        updateDevices();
        Tuple tuple = new Tuple();
                                                                
        for(int i = 0; i < numOutDevices; i++)
            {
            Object obj = findDevice(Prefs.getLastTupleOut(i), outDevices);
            int channel = Prefs.getLastTupleOutChannel(i);
            String name = Prefs.getLastTupleOutName(i);
            if (channel > 0 && obj != null && (obj instanceof MidiDeviceWrapper))
                {
                tuple.outChannel[i] = channel;
                tuple.outWrap[i] = ((MidiDeviceWrapper)(obj));
                tuple.outName[i] = name;
                }
            else
                {
                tuple.outChannel[i] = channel;
                tuple.outWrap[i] = null;
                tuple.outName[i] = name;
                }
            }

        for(int i = 0; i < numInDevices; i++)
            {
            Object obj = findDevice(Prefs.getLastTupleIn(i), inDevices);
            int channel = Prefs.getLastTupleInChannel(i);
            String name = Prefs.getLastTupleInName(i);
            if (channel > -1 && obj != null && (obj instanceof MidiDeviceWrapper))
                {
                tuple.inChannel[i] = channel;
                tuple.inWrap[i] = ((MidiDeviceWrapper)(obj));
                inReceiver[i].setWrapper(tuple.inWrap[i]);              // Do this first so the old one is removed 
                tuple.inReceiver[i] = inReceiver[i];
                tuple.inWrap[i].addToTransmitter(inReceiver[i]);
                tuple.inName[i] = name;
                }
            else
                {
                tuple.inChannel[i] = channel;
                tuple.inWrap[i] = null;
                inReceiver[i].setWrapper(tuple.inWrap[i]);
                tuple.inName[i] = name;
                }
            }
                        
        return tuple;
        }
                
                

    /** Works with the user to generate a new Tuple holding new MIDI connections.
        You may provide the old tuple for defaults or pass in null.  You also
        provide the inReceiver and inReceiver and in2Receiver to be attached to the input and keyboard/controller
        input.  You get these with Synth.buildInReceiver() and Synth.buildInReceiver() 
        If the old Tuple is the previous tuple of
        this synthesizer, then you will want to set removeReceiversFromOldTuple to TRUE so that when
        new receivers are attached, the old ones are eliminated.  However if old Tuple is from another
        active synthesizer editor, and so is just being used to provide defaults, then you should set
        removeReceiversFromOldTuple to FALSE so it doesn't muck with the previous synthesizer.
    */ 
    public static Tuple getNewTuple(Tuple old, JComponent parent, Seq seq, String message, In[] inReceiver)
        {
        updateDevices();
        
        boolean[] changed = new boolean[1];
        
/*
  if (inDevices.size() == 0)
  {
  Dialogs.disableMenuBar(parent);
  JOptionPane.showOptionDialog(parent, "There are no MIDI devices available to receive from.",  
  "Cannot Connect", JOptionPane.DEFAULT_OPTION, 
  JOptionPane.WARNING_MESSAGE, null,
  new String[] { "Run Disconnected" }, "Run Disconnected");
  Dialogs.enableMenuBar();
  return CANCELLED;
  }
  else if (outDevices.size() == 0)
  {
  Dialogs.disableMenuBar(parent);
  JOptionPane.showOptionDialog(parent, "There are no MIDI devices available to send to.",  
  "Cannot Connect", JOptionPane.DEFAULT_OPTION, 
  JOptionPane.WARNING_MESSAGE, null,
  new String[] { "Run Disconnected" }, "Run Disconnected");
  Dialogs.enableMenuBar();
  return CANCELLED;
  }
  else
*/
            {
            JComboBox outCombo[] = new JComboBox[numOutDevices];
            for(int i = 0; i < outCombo.length; i++)
                {
                outCombo[i] = new JComboBox(outDevices.toArray());
                outCombo[i].getAccessibleContext().setAccessibleName("Output Device " + i);
                outCombo[i].setMaximumRowCount(16);
                if (old != null && old.outWrap != null && outDevices.indexOf(old.outWrap[i]) != -1)
                    outCombo[i].setSelectedIndex(outDevices.indexOf(old.outWrap[i]));
                else if (findDevice(Prefs.getLastTupleOut(i), outDevices) != null)
                    outCombo[i].setSelectedItem(findDevice(Prefs.getLastTupleOut(i), outDevices));
                
                outCombo[i].addActionListener(new ActionListener() 
                    {
                    public void actionPerformed(ActionEvent e)
                        {
                        changed[0] = true;
                        }
                    });
                }
                
            JComboBox[] inCombo = new JComboBox[numInDevices];
            for(int i = 0; i < inCombo.length; i++)
                {
                inCombo[i] = new JComboBox(inDevices.toArray());
                inCombo[i].getAccessibleContext().setAccessibleName("Input Device " + i);
                inCombo[i].setMaximumRowCount(17);
                inCombo[i].setSelectedIndex(0);  // "none"
                if (old != null && old.inWrap != null && inDevices.indexOf(old.inWrap[i]) != -1)
                    inCombo[i].setSelectedIndex(inDevices.indexOf(old.inWrap[i]));
                else if (findDevice(Prefs.getLastTupleIn(i), inDevices) != null)
                    inCombo[i].setSelectedItem(findDevice(Prefs.getLastTupleIn(i), inDevices));

                inCombo[i].addActionListener(new ActionListener() 
                    {
                    public void actionPerformed(ActionEvent e)
                        {
                        changed[0] = true;
                        }
                    });
                }

            JComboBox[] outChannelsCombo = new JComboBox[numOutDevices];
            for(int i = 0; i < outChannelsCombo.length; i++)
                {
                outChannelsCombo[i] = new JComboBox(outChannelOptions);
                outChannelsCombo[i].getAccessibleContext().setAccessibleName("Output Channel");
                outChannelsCombo[i].setMaximumRowCount(16);
                if (old != null)
                    outChannelsCombo[i].setSelectedIndex(old.outChannel[i] - 1);
                else if (Prefs.getLastTupleOutChannel(i) > 0)
                    outChannelsCombo[i].setSelectedIndex(Prefs.getLastTupleOutChannel(i) - 1);
                else 
                    outChannelsCombo[i].setSelectedIndex(0);

                outChannelsCombo[i].addActionListener(new ActionListener() 
                    {
                    public void actionPerformed(ActionEvent e)
                        {
                        changed[0] = true;
                        }
                    });
                }
                                
            JComboBox[] inChannelsCombo = new JComboBox[numInDevices];
            for(int i = 0; i < inChannelsCombo.length; i++)
                {
                inChannelsCombo[i] = new JComboBox(inChannelOptions);
                inChannelsCombo[i].getAccessibleContext().setAccessibleName("Input Channel");
                inChannelsCombo[i].setMaximumRowCount(17);
                if (old != null)
                    inChannelsCombo[i].setSelectedIndex(old.inChannel[i]);
                else if (Prefs.getLastTupleInChannel(i) > 0)
                    inChannelsCombo[i].setSelectedIndex(Prefs.getLastTupleInChannel(i));
                else 
                    inChannelsCombo[i].setSelectedIndex(0);

                inChannelsCombo[i].addActionListener(new ActionListener() 
                    {
                    public void actionPerformed(ActionEvent e)
                        {
                        changed[0] = true;
                        }
                    });
                }
                
            StringField[] outNicknames = new StringField[numOutDevices];
            for(int i = 0; i < outNicknames.length; i++)
                {
                outNicknames[i] = new StringField("")
                    {
                    public String newValue(String val) { changed[0] = (!getValue().equals(val)); return val; }
                    };
                String nick = null;
                if (old != null) nick = old.outName[i];
                if (nick == null || nick.trim().length() == 0)
                    {
                    String str = Prefs.getLastTupleOutName(i);
                    if (str != null && str.trim().length() > 0)
                        {
                        outNicknames[i].setValue(str.trim());
                        }
                    }
                else
                    {
                    outNicknames[i].setValue(nick.trim());
                    }
                }

            StringField[] inNicknames = new StringField[numInDevices];
            for(int i = 0; i < inNicknames.length; i++)
                {
                inNicknames[i] = new StringField("")
                    {
                    public String newValue(String val) { changed[0] = (!getValue().equals(val)); return val; }
                    };
                String nick = null;
                if (old != null) nick = old.inName[i];
                if (nick == null || nick.trim().length() == 0)
                    {
                    String str = Prefs.getLastTupleInName(i);
                    if (str != null && str.trim().length() > 0)
                        {
                        inNicknames[i].setValue(str.trim());
                        }
                    }
                else
                    {
                    inNicknames[i].setValue(nick.trim());
                    }
                }

            Dialogs.disableMenuBar(parent);
            
            String[] names = new String[numOutDevices + numInDevices + 1];
            JComponent[] components = new JComponent[numOutDevices + numInDevices + 1];
            for(int i = 0; i < numOutDevices; i++)
                {
                names[i] = "Output " + (i + 1);
                Box box = new Box(BoxLayout.X_AXIS);
                box.add(outCombo[i]);
                box.add(new JLabel("    Channel "));
                box.add(outChannelsCombo[i]);
                box.add(new JLabel("    Nickname " ));
                box.add(outNicknames[i]);
                components[i] = box;
                }
                
            // blank separator
            components[numOutDevices] = new JLabel(" ");
            names[numOutDevices] = "";
            
            for(int i = 0; i < numInDevices; i++)
                {
                names[i + numOutDevices + 1] = "Input " + (i + 1);
                Box box = new Box(BoxLayout.X_AXIS);
                box.add(inCombo[i]);
                box.add(new JLabel("    Channel "));
                box.add(inChannelsCombo[i]);
                box.add(new JLabel("    Nickname " ));
                box.add(inNicknames[i]);
                components[i + numOutDevices + 1] = box;
                }
            
            int result = Dialogs.showMultiOption(parent, names, components, new String[] {  "Set", "Reload", "Cancel" }, 0, "MIDI Devices", message);
                                        
            if (result == 1)                        // "Reload"
                {
                Tuple tup = getNewTuple(old, parent, seq, message, inReceiver);
                Dialogs.enableMenuBar();
                return tup;
                }         
            else if (result == 0)           // "Set"
                {
                // we ALWAYS change because devices can be plugged in and we want to set to them
                // even though we don't frob any widget at all.
                /*
                if (!changed[0]) 
                    { 
                    Dialogs.enableMenuBar();
                    return old; 
                    }
                */
                    
                // we need to build a tuple
                                                                
                Tuple tuple = new Tuple();
                                                                
                for(int i = 0; i < numOutDevices; i++)
                    {
                    tuple.outChannel[i] = outChannelsCombo[i].getSelectedIndex() + 1;
                    String str = outNicknames[i].getText();
                    if (str != null) 
                        {
                        str = str.trim();
                        if (str.length() == 0) str = null;
                        }
                    tuple.outName[i] = str;
                                
                    if (outCombo[i].getSelectedItem() instanceof String)    // "None"
                        {
                        tuple.outWrap[i] = null;
                        }
                    else
                        {
                        tuple.outWrap[i] = ((MidiDeviceWrapper)(outCombo[i].getSelectedItem()));
                        }
                    }

                for(int i = 0; i < numInDevices; i++)
                    {
                    tuple.inChannel[i] = inChannelsCombo[i].getSelectedIndex();
                    String str = inNicknames[i].getText();
                    if (str != null) 
                        {
                        str = str.trim();
                        if (str.length() == 0) str = null;
                        }
                    tuple.inName[i] = str;

                    if (inCombo[i].getSelectedItem() instanceof String)     // "None"
                        {
                        tuple.inWrap[i] = null;
                        }
                    else
                        {
                        tuple.inWrap[i] = ((MidiDeviceWrapper)(inCombo[i].getSelectedItem()));
                        }
                    }
                                
                                
                // attachReceivers
                tuple.inReceiver = new Receiver[tuple.inWrap.length];
                for(int i = 0; i < tuple.inWrap.length; i++)
                    {
                    if (inCombo[i].getSelectedItem() instanceof String)
                        {
                        inReceiver[i].setWrapper(null);
                        }
                    else
                        {
                        tuple.inReceiver[i] = inReceiver[i];
                        tuple.inWrap[i] = ((MidiDeviceWrapper)(inCombo[i].getSelectedItem()));
                        inReceiver[i].setWrapper(tuple.inWrap[i]);          // Do this first so the old one is removed
                        tuple.inWrap[i].addToTransmitter(inReceiver[i]);
                        }
                    }

                for(int i = 0; i < numOutDevices; i++)
                    {
                    if (tuple.outWrap == null || tuple.outWrap[i] == null)
                        Prefs.setLastTupleOut(i, "None");
                    else
                        Prefs.setLastTupleOut(i, tuple.outWrap[i].toString());
                    Prefs.setLastTupleOutChannel(i, tuple.outChannel[i]);
                    Prefs.setLastTupleOutName(i, tuple.outName[i] == null ? "" : tuple.outName[i].trim());
                    }
                                                
                for(int i = 0; i < numInDevices; i++)
                    {
                    if (tuple.inWrap == null || tuple.inWrap[i] == null)
                        Prefs.setLastTupleIn(i, "None");
                    else
                        Prefs.setLastTupleIn(i, tuple.inWrap[i].toString());
                    Prefs.setLastTupleInChannel(i, tuple.inChannel[i]);
                    Prefs.setLastTupleInName(i, tuple.inName[i] == null ? "" : tuple.inName[i].trim());
                    }

                Dialogs.enableMenuBar();
                return tuple;
                }
            else                    // "Cancel"
                {
                Dialogs.enableMenuBar();
                return CANCELLED;
                }
            }
        }



    public static final int CCDATA_TYPE_RAW_CC = 0;      
    public static final int CCDATA_TYPE_NRPN = 1;      
    public static final int CCDATA_TYPE_RPN = 2;      




    public static class CCData
        {
        public int type;
        public int number;
        public int value;
        public int channel;
        public boolean increment;
        public CCData(int type, int number, int value, int channel, boolean increment)
            { this.type = type; this.number = number; this.value = value; this.increment = increment; this.channel = channel; }
        }
        

        
        
    public static class Parser
        {


        ///// INTRODUCTION TO THE CC/RPN/NRPN PARSER
        ///// The parser is located in handleGeneralControlChange(...), which
        ///// can be set up to be the handler for CC messages by the MIDI library.
        /////
        ///// CC messages take one of a great many forms, which we handle in the parser
        /////
        ///// 7-bit CC messages:
        ///// 1. number >=64 and < 96 or >= 102 and < 120, with value
        /////           -> handleControlChange(channel, number, value, VALUE_7_BIT_ONLY)
        /////
        ///// Potentially 7-bit CC messages, with MSB:
        ///// 1. number >= 0 and < 32, other than 6, with value
        /////           -> handleControlChange(channel, number, value * 128 + 0, VALUE_MSB_ONLY)
        /////
        ///// Full 14-bit CC messages:
        ///// 1. number >= 0 and < 32, other than 6, with MSB
        ///// 2. same number + 32, with LSB
        /////           -> handleControlChange(channel, number, MSB * 128 + LSB, VALUE)
        /////    NOTE: this means that a 14-bit CC message will have TWO handleControlChange calls.
        /////          There's not much we can do about this, as we simply don't know if the LSB will arrive.  
        /////
        ///// Continuing 14-bit CC messages:
        ///// 1. number >= 32 and < 64, other than 38, with LSB, where number is 32 more than the last MSB.
        /////           -> handleControlChange(channel, number, former MSB * 128 + LSB, VALUE)
        /////
        ///// Lonely 14-bit CC messages (LSB only)
        ///// 1. number >= 32 and < 64, other than 38, with LSB, where number is NOT 32 more than the last MSB.
        /////           -> handleControlChange(channel, number, 0 + LSB, VALUE)
        /////           
        /////
        ///// NRPN Messages:
        ///// All NRPN Messages start with:
        ///// 1. number == 99, with MSB of NRPN parameter
        ///// 2. number == 98, with LSB of NRPN parameter
        /////           At this point NRPN MSB is set to 0
        /////
        ///// NRPN Messages then may have any sequence of:
        ///// 3.1 number == 6, with value   (MSB)
        /////           -> handleNRPN(channel, parameter, value * 128 + 0, VALUE_MSB_ONLY)
        /////                           At this point we set the NRPN MSB
        ///// 3.2 number == 38, with value   (LSB)
        /////           -> handleNRPN(channel, parameter, current NRPN MSB * 128 + value, VALUE_MSB_ONLY)
        ///// 3.3 number == 96, with value   (Increment)
        /////       If value == 0
        /////                   -> handleNRPN(channel, parameter, 1, INCREMENT)
        /////       Else
        /////                   -> handleNRPN(channel, parameter, value, INCREMENT)
        /////       Also reset current NRPN MSB to 0
        ///// 3.4 number == 97, with value
        /////       If value == 0
        /////                   -> handleNRPN(channel, parameter, 1, DECREMENT)
        /////       Else
        /////                   -> handleNRPN(channel, parameter, value, DECREMENT)
        /////       Also reset current NRPN MSB to 0
        /////
        /////
        ///// RPN Messages:
        ///// All RPN Messages start with:
        ///// 1. number == 99, with MSB of RPN parameter
        ///// 2. number == 98, with LSB of RPN parameter
        /////           At this point RPN MSB is set to 0
        /////
        ///// RPN Messages then may have any sequence of:
        ///// 3.1 number == 6, with value   (MSB)
        /////           -> handleRPN(channel, parameter, value * 128 + 0, VALUE_MSB_ONLY)
        /////                           At this point we set the RPN MSB
        ///// 3.2 number == 38, with value   (LSB)
        /////           -> handleRPN(channel, parameter, current RPN MSB * 128 + value, VALUE_MSB_ONLY)
        ///// 3.3 number == 96, with value   (Increment)
        /////       If value == 0
        /////                   -> handleRPN(channel, parameter, 1, INCREMENT)
        /////       Else
        /////                   -> handleRPN(channel, parameter, value, INCREMENT)
        /////       Also reset current RPN MSB to 0
        ///// 3.4 number == 97, with value
        /////       If value == 0
        /////                   -> handleRPN(channel, parameter, 1, DECREMENT)
        /////       Else
        /////                   -> handleRPN(channel, parameter, value, DECREMENT)
        /////       Also reset current RPN MSB to 0
        /////

        ///// NULL messages:            [RPN 127 with value of 127]
        ///// 1. number == 101, value = 127
        ///// 2. number == 100, value = 127
        /////           [nothing happens, but parser resets]
        /////
        /////
        ///// The big problem we have is that the MIDI spec allows a bare MSB or LSB to arrive and that's it!
        ///// We don't know if another one is coming.  If a bare LSB arrives we're supposed to assume the MSB is 0.
        ///// But if the bare MSB comes we don't know if the LSB is next.  So we either have to ignore it when it
        ///// comes in (bad bad bad) or send two messages, one MSB-only and one MSB+LSB.  
        ///// This happens for CC, RPN, and NRPN.
        /////
        /////
        ///// Our parser maintains four bytes in a struct called Parser:
        /////
        ///// 0. status.  This is one of:
        /////             INVALID: the struct holds junk.  CC: the struct is building a CC.  
        /////                     RPN_START, RPN_END: the struct is building an RPN.
        /////                     NRPN_START, NRPN_END: the struct is building an NRPN.
        ///// 1. controllerNumberMSB.  In the low 7 bits.
        ///// 2. controllerNumberLSB.  In the low 7 bits.
        ///// 3. controllerValueMSB.  In the low 7 bits. This holds the previous MSB for potential "continuing" messages.

        // Parser status values
        public static final int  INVALID = 0;
        public static final int  NRPN_START = 1;
        public static final int  NRPN_END = 2;
        public static final int  RPN_START = 2;
        public static final int  RPN_END = 3;

        int[] status = new int[16];  //  = INVALID;
                
        // The high bit of the controllerNumberMSB is either
        // NEITHER_RPN_NOR_NRPN or it is RPN_OR_NRPN. 
        int[] controllerNumberMSB = new int[16];
                
        // The high bit of the controllerNumberLSB is either
        // RPN or it is NRPN
        int[] controllerNumberLSB = new int[16];
                
        // The controllerValueMSB[channel] is either a valid MSB or it is (-1).
        int[] controllerValueMSB = new int[16];

        // The controllerValueLSB is either a valid LSB or it is  (-1).
        int[] controllerValueLSB = new int[16];
  

        // we presume that the channel never changes
        CCData parseCC(int channel, int number, int value, boolean requireLSB, boolean requireMSB)
            {
            // BEGIN PARSER

            // Start of NRPN
            if (number == 99)
                {
                status[channel] = NRPN_START;
                controllerNumberMSB[channel] = value;
                return null;
                }

            // End of NRPN
            else if (number == 98)
                {
                controllerValueMSB[channel] = 0;
                if (status[channel] == NRPN_START)
                    {
                    status[channel] = NRPN_END;
                    controllerNumberLSB[channel] = value;
                    controllerValueLSB[channel]  = -1;
                    controllerValueMSB[channel]  = -1;
                    }
                else status[channel] = INVALID;
                return null;
                }
                
            // Start of RPN or NULL
            else if (number == 101)
                {
                if (value == 127)  // this is the NULL termination tradition, see for example http://www.philrees.co.uk/nrpnq.htm
                    {
                    status[channel] = INVALID;
                    }
                else
                    {
                    status[channel] = RPN_START;
                    controllerNumberMSB[channel] = value;
                    }
                return null;
                }

            // End of RPN or NULL
            else if (number == 100)
                {
                controllerValueMSB[channel] = 0;
                if (value == 127)  // this is the NULL termination tradition, see for example http://www.philrees.co.uk/nrpnq.htm
                    {
                    status[channel] = INVALID;
                    }
                else if (status[channel] == RPN_START)
                    {
                    status[channel] = RPN_END;
                    controllerNumberLSB[channel] = value;
                    controllerValueLSB[channel]  = -1;
                    controllerValueMSB[channel]  = -1;
                    }
                return null;
                }

            else if ((number == 6 || number == 38 || number == 96 || number == 97) && (status[channel] == NRPN_END || status[channel] == RPN_END))  // we're currently parsing NRPN or RPN
                {
                int controllerNumber =  (((int) controllerNumberMSB[channel]) << 7) | controllerNumberLSB[channel] ;
                        
                if (number == 6)
                    {
                    controllerValueMSB[channel] = value;
                    if (requireLSB && controllerValueLSB[channel] == -1)
                        return null;
                    if (status[channel] == NRPN_END)
                        return handleNRPN(channel, controllerNumber, controllerValueLSB[channel] == -1 ? 0 : controllerValueLSB[channel], controllerValueMSB[channel]);
                    else
                        return handleRPN(channel, controllerNumber, controllerValueLSB[channel] == -1 ? 0 : controllerValueLSB[channel], controllerValueMSB[channel]);
                    }
                                                                                                                        
                // Data Entry LSB for RPN, NRPN
                else if (number == 38)
                    {
                    controllerValueLSB[channel] = value;
                    if (requireMSB && controllerValueMSB[channel] == -1)
                        return null;          
                    if (status[channel] == NRPN_END)
                        return handleNRPN(channel, controllerNumber, controllerValueLSB[channel], controllerValueMSB[channel] == -1 ? 0 : controllerValueMSB[channel]);
                    else
                        return handleRPN(channel, controllerNumber, controllerValueLSB[channel], controllerValueMSB[channel] == -1 ? 0 : controllerValueMSB[channel]);
                    }
                                                                                                                        
                // Data Increment for RPN, NRPN
                else if (number == 96)
                    {
                    if (value == 0)
                        value = 1;
                    if (status[channel] == NRPN_END)
                        return handleNRPNIncrement(channel, controllerNumber, value);
                    else
                        return handleRPNIncrement(channel, controllerNumber, value);
                    }

                // Data Decrement for RPN, NRPN
                else // if (number == 97)
                    {
                    if (value == 0)
                        value = -1;
                    if (status[channel] == NRPN_END)
                        return handleNRPNIncrement(channel, controllerNumber, -value);
                    else
                        return handleRPNIncrement(channel, controllerNumber, -value);
                    }
                                
                }
                        
            else  // Some other CC
                {
                // status[channel] = INVALID;           // I think it's fine to send other CC in the middle of NRPN or RPN
                return handleRawCC(channel, number, value);
                }
            }
        
        public CCData processCC(ShortMessage message, boolean requireLSB, boolean requireMSB)
            {
            int num = message.getData1();
            int val = message.getData2();
            int channel = message.getChannel();
            return parseCC(channel, num, val, requireLSB, requireMSB);
            }
        
        public CCData handleNRPN(int channel, int controllerNumber, int _controllerValueLSB, int _controllerValueMSB)
            {
            if (_controllerValueLSB < 0 || _controllerValueMSB < 0)
                System.out.println("Warning (Midi): " + "LSB or MSB < 0.  RPN: " + controllerNumber + "   LSB: " + _controllerValueLSB + "  MSB: " + _controllerValueMSB);
            return new CCData(CCDATA_TYPE_NRPN, controllerNumber, _controllerValueLSB | (_controllerValueMSB << 7), channel, false);
            }
        
        public CCData handleNRPNIncrement(int channel, int controllerNumber, int delta)
            {
            return new CCData(CCDATA_TYPE_NRPN, controllerNumber, delta, channel, true);
            }

        public CCData handleRPN(int channel, int controllerNumber, int _controllerValueLSB, int _controllerValueMSB)
            {
            if (_controllerValueLSB < 0 || _controllerValueMSB < 0)
                System.out.println("Warning (Midi): " + "LSB or MSB < 0.  RPN: " + controllerNumber + "   LSB: " + _controllerValueLSB + "  MSB: " + _controllerValueMSB);
            return new CCData(CCDATA_TYPE_RPN, controllerNumber, _controllerValueLSB | (_controllerValueMSB << 7), channel, false);
            }
        
        public CCData handleRPNIncrement(int channel, int controllerNumber, int delta)
            {
            return new CCData(CCDATA_TYPE_RPN, controllerNumber, delta, channel, true);
            }

        public CCData handleRawCC(int channel, int controllerNumber, int value)
            {
            return new CCData(CCDATA_TYPE_RAW_CC, controllerNumber, value, channel, false);
            }
        }
                    
    
    public static String format(MidiMessage message)
        {
        if (message == null)
            {
            return "null";
            }
        else if (message instanceof MetaMessage)
            {
            return "A MIDI File MetaMessage (shouldn't happen)";
            }
        else if (message instanceof SysexMessage)
            {
            //System.err.println("-->" + StringUtility.toHex(((SysexMessage)message).getMessage()));
            if (((SysexMessage)message).getMessage()[0] == (byte)0xF0)  // First one
                {
                return "Sysex (" + getManufacturerForSysex(((SysexMessage)message).getData()) + ")" + 
                    "\n\t" + StringUtility.toHex(((SysexMessage)message).getMessage());
                }
            else
                {
                return "Sysex Fragment";
                }
            }
        else // ShortMessage
            {
            ShortMessage s = (ShortMessage) message;
            int c = s.getChannel();
            String type = "Unknown";
            switch(s.getStatus())
                {
                case ShortMessage.ACTIVE_SENSING: type = "Active Sensing"; c = -1; break;
                case ShortMessage.CHANNEL_PRESSURE: type = "Channel Pressure"; break;
                case ShortMessage.CONTINUE: type = "Continue"; c = -1; break;
                case ShortMessage.CONTROL_CHANGE: type = "Control Change"; break;
                case ShortMessage.END_OF_EXCLUSIVE: type = "End of Sysex Marker"; c = -1; break;
                case ShortMessage.MIDI_TIME_CODE: type = "Midi Time Code"; c = -1; break;
                case ShortMessage.NOTE_OFF: type = "Note Off"; break;
                case ShortMessage.NOTE_ON: type = "Note On"; break;
                case ShortMessage.PITCH_BEND: type = "Pitch Bend"; break;
                case ShortMessage.POLY_PRESSURE: type = "Poly Pressure"; break;
                case ShortMessage.PROGRAM_CHANGE: type = "Program Change"; break;
                case ShortMessage.SONG_POSITION_POINTER: type = "Song Position Pointer"; c = -1; break;
                case ShortMessage.SONG_SELECT: type = "Song Select"; c = -1; break;
                case ShortMessage.START: type = "Start"; c = -1; break;
                case ShortMessage.STOP: type = "Stop"; c = -1; break;
                case ShortMessage.SYSTEM_RESET: type = "System Reset"; c = -1; break;
                case ShortMessage.TIMING_CLOCK: type = "Timing Clock"; c = -1; break;
                case ShortMessage.TUNE_REQUEST: type = "Tune Request"; c = -1; break;
                }
            return type + (c == -1 ? "" : (" (Channel " + c + ")" + 
                    "\t" + (s.getData1() & 0xFF) + " " + (s.getData2() & 0xFF)));
            }
        }

    static HashMap manufacturers = null;
    
    static HashMap getManufacturers()
        {
        if (manufacturers != null)
            return manufacturers;
                        
        manufacturers = new HashMap();
        Scanner scan = new Scanner(Midi.class.getResourceAsStream("Manufacturers.txt"));
        while(scan.hasNextLine())
            {
            String nextLine = scan.nextLine().trim();
            if (nextLine.equals("")) continue;
            if (nextLine.startsWith("#")) continue;
                        
            int id = 0;
            Scanner scan2 = new Scanner(nextLine);
            int one = scan2.nextInt(16);  // in hex
            if (one == 0x00)  // there are two more to read
                {
                id = id + (scan2.nextInt(16) << 8) + (scan2.nextInt(16) << 16);
                }
            else
                {
                id = one;
                }
            manufacturers.put(Integer.valueOf(id), scan.nextLine().trim());
            }
        return manufacturers;
        }

    public static String getManufacturerForDeviceInquiry(byte[] data)
        {
        if (data.length >= 15 &&
            data[0] == (byte)0xF0 &&
            data[1] == (byte)0x7E &&
            data[3] == (byte)0x06 &&
            data[4] == (byte)0x02)
            {
            if (data[5] == (byte)0x00 &&            // extended manufacturer ID
                data.length == 17 &&
                data[16] == (byte)0xF7)
                {
                String manufacturer = getManufacturerForSysex(new byte[] { data[5], data[6], data[7] });
                String family = StringUtility.toHex(new byte[] { data[8], data[9] });
                String member = StringUtility.toHex(new byte[] { data[10], data[11] });
                String revision = StringUtility.toHex(new byte[] { data[12], data[13], data[14], data[15] });
                return "Manufacturer:  " + manufacturer + "\nFamily: " + family + "\nMember: " + member + "\nRevision: " + revision;
                }
            else if (data[14] == (byte)0xF7)                // short manufacturer ID
                {
                String manufacturer = getManufacturerForSysex(new byte[] { data[5] });
                String family = StringUtility.toHex(new byte[] { data[6], data[7] });
                String member = StringUtility.toHex(new byte[] { data[8], data[9] });
                String revision = StringUtility.toHex(new byte[] { data[10], data[11], data[12], data[13] });
                return "Manufacturer:  " + manufacturer + "\nFamily: " + family + "\nMember: " + member + "\nRevision: " + revision;
                }
            else return null;
            }
        else return null;
        }
    
    /** This works with or without F0 as the first data byte */
    public static String getManufacturerForSysex(byte[] data)
        {
        int offset = 0;
        if (data[0] == (byte)0xF0)
            offset = 1;
        HashMap map = getManufacturers();
        if (data[0 + offset] == (byte)0x7D)             // educational use
            {
            return (String)(map.get(Integer.valueOf(data[0 + offset]))) + 
                "<br><br>Note that unregistered manufacturers or developers typically<br>use this system exclusive region.";
            }
        else if (data[0 + offset] == (byte)0x00)
            {
            return (String)(map.get(Integer.valueOf(
                        0x00 + 
                        ((data[1 + offset] < 0 ? data[1 + offset] + 256 : data[1 + offset]) << 8) + 
                        ((data[2 + offset] < 0 ? data[2 + offset] + 256 : data[2 + offset]) << 16))));
            }
        else
            {
            return (String)(map.get(Integer.valueOf(data[0 + offset])));
            }
        }
    }
