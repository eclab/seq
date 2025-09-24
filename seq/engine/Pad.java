/* 
   Copyright 2025 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.engine;

public class Pad
    {
    /// NOTE: the three pads supported here handle buttons differently:
    /// 1. The MK1 has different lighting values than the other two
    /// 2. The MK1's origin is top left, the other to have an origin at bottom left
    /// 3. The MK1 and MK3 use CC for the right column, the APC uses notes
    /// 4. The APC brightness changes based on channel
    /// 5. The APC irritatingly lights up pads when you press them regardless of what it's told over MIDI
    ///
    /// This class could be rewritten better.  It was originally written for the MK1 and MK3
    /// but now it's pretty messy
    
    /// DEVICES
    /// There are three devices
    /// STATES
    /// The Select Motif has five states.
    /// STATE COLORS
    /// For each device, there are state colors for each of the five states, plus additional
    /// state colors for "high" and "low" for four unique colors.
    /// INDICES
    /// Each pad has an index.  The main grid is 0...7 on the top row, then 8...15 on the next row, etc.
    /// Additionally the very top row buttons have the indices
    ///     PAD_INDEX_UP, PAD_INDEX_DOWN, PAD_INDEX_RIGHT, PAD_INDEX_LEFT, PAD_INDEX_A, PAD_INDEX_B, PAD_INDEX_C, PAD_INDEX_D
    /// And the very right column buttons have the indices
    ///             PAD_INDEX_1, PAD_INDEX_2, PAD_INDEX_3, PAD_INDEX_4, PAD_INDEX_5, PAD_INDEX_6, PAD_INDEX_7, PAD_INDEX_8
    /// Each index is associted with a NOTE or a CC message both incoming to outgoing.  But the APC
    /// uses pure notes, while the MK1 and MK3 use a combination of notes and CC stupidly.
        
    
    // DEVICES
        
    public static final String[] DEVICE_NAMES = { "Launchpad MK 1", "Launchpad MK 3", "Akai APC" };
    public static final int DEVICE_LAUNCHPAD_MKI = 0;
    public static final int DEVICE_LAUNCHPAD_MKIII = 1;
    public static final int DEVICE_AKAI_APC = 2;

    // PAD STATES
    
    public static final int PAD_STATE_UNUSED = 0;
    public static final int PAD_STATE_OFF = 1;
    public static final int PAD_STATE_ON = 2;
    public static final int PAD_STATE_WAITING = 3;
    public static final int PAD_STATE_STOPPING = 4;
    
    // PAD STATE COLORS FOR DIFFERENT MODELS
        
    public static final int PAD_OFF1_APC = 7;                 // dark red
    public static final int PAD_ON1_APC = 5;                  // red
    public static final int PAD_OFF2_APC = 15;                // dark yellow
    public static final int PAD_ON2_APC = 13;                 // yellow
    public static final int PAD_OFF3_APC = 23;                // dark green
    public static final int PAD_ON3_APC = 21;                 // green
    public static final int PAD_OFF4_APC = 39;                // dark cyan
    public static final int PAD_ON4_APC = 47;                 // cyan

    public static final int PAD_OFF1_MKIII = 7;                 // dark red
    public static final int PAD_ON1_MKIII = 5;                  // red
    public static final int PAD_OFF2_MKIII = 15;                // dark yellow
    public static final int PAD_ON2_MKIII = 13;                 // yellow
    public static final int PAD_OFF3_MKIII = 23;                // dark green
    public static final int PAD_ON3_MKIII = 21;                 // green
    public static final int PAD_OFF4_MKIII = 39;                // dark cyan
    public static final int PAD_ON4_MKIII = 47;                 // cyan

    public static final int PAD_OFF1_MKI = 0x0D;                // dim red
    public static final int PAD_ON1_MKI = 0x0F;                 // red
    public static final int PAD_OFF2_MKI = 0x2D;                // medium yellow
    public static final int PAD_ON2_MKI = 0x3E;                 // yellow
    public static final int PAD_OFF3_MKI = 0x1C;                // dark green
    public static final int PAD_ON3_MKI = 0x3C;                 // green
    public static final int PAD_OFF4_MKI = 0x1D;                // dim amber
    public static final int PAD_ON4_MKI = 0x3F;                 // amber

    public static final int PAD_UNUSED_MKI = 0;
    public static final int PAD_UNUSED_MKIII = 0;
    public static final int PAD_UNUSED_APC = 0;

    public static final int PAD_WAITING_MKIII = 45;
    public static final int PAD_STOPPING_MKIII = 53;
    
    public static final int PAD_WAITING_MKI = 0x3C;             // yellow
    public static final int PAD_STOPPING_MKI = 63;              // amber

    public static final int PAD_WAITING_APC = 45;
    public static final int PAD_STOPPING_APC = 53;
    
    public static final int[][] PAD_STATES_OFF = new int[][]
    {
    { PAD_OFF1_MKI, PAD_OFF2_MKI, PAD_OFF3_MKI, PAD_OFF4_MKI },
    { PAD_OFF1_MKIII, PAD_OFF2_MKIII, PAD_OFF3_MKIII, PAD_OFF4_MKIII },
    { PAD_OFF1_APC, PAD_OFF2_APC, PAD_OFF3_APC, PAD_OFF4_APC },
    };
                
    public static final int[][] PAD_STATES_ON = new int[][]
    {
    { PAD_ON1_MKI, PAD_ON2_MKI, PAD_ON3_MKI, PAD_ON4_MKI },
    { PAD_ON1_MKIII, PAD_ON2_MKIII, PAD_ON3_MKIII, PAD_ON4_MKIII },
    { PAD_ON1_APC, PAD_ON2_APC, PAD_ON3_APC, PAD_ON4_APC },
    };

    public static final int[] PAD_STATES_UNUSED = new int[]
    {
    PAD_UNUSED_MKI, PAD_UNUSED_MKIII, PAD_UNUSED_APC,
    };

    public static final int[] PAD_STATES_WAITING = new int[]
    {
    PAD_WAITING_MKI, PAD_WAITING_MKIII, PAD_WAITING_APC
    };

    public static final int[] PAD_STATES_STOPPING = new int[]
    {
    PAD_STOPPING_MKI, PAD_STOPPING_MKIII, PAD_STOPPING_APC
    };
                

    // INDICES AND NOTES    
    public static final int INVALID_INDEX = -256;
    public static final int INVALID_NOTE = -256;
    // The buttons on the top row, left to right
    public static final int PAD_INDEX_UP = -1;
    public static final int PAD_INDEX_DOWN = -2;
    public static final int PAD_INDEX_LEFT = -3;
    public static final int PAD_INDEX_RIGHT = -4;
    public static final int PAD_INDEX_A = -5;
    public static final int PAD_INDEX_B = -6;
    public static final int PAD_INDEX_C = -7;
    public static final int PAD_INDEX_D = -8;
    // The buttons on the right column, top to bottom
    public static final int PAD_INDEX_1 = -9;
    public static final int PAD_INDEX_2 = -10;
    public static final int PAD_INDEX_3 = -11;
    public static final int PAD_INDEX_4 = -12;
    public static final int PAD_INDEX_5 = -13;
    public static final int PAD_INDEX_6 = -14;
    public static final int PAD_INDEX_7 = -15;
    public static final int PAD_INDEX_8 = -16;

    public static final int[] PAD_INDICES = { PAD_INDEX_UP, PAD_INDEX_DOWN, PAD_INDEX_LEFT, PAD_INDEX_RIGHT, PAD_INDEX_A, PAD_INDEX_B, PAD_INDEX_C, PAD_INDEX_D,
        PAD_INDEX_1, PAD_INDEX_2, PAD_INDEX_3, PAD_INDEX_4, PAD_INDEX_5, PAD_INDEX_6, PAD_INDEX_7, PAD_INDEX_8,  };
    
    public static final int[] PAD_COMMAND_UP = new int[]
    {
    0x68, 0x5B, 0,
    };

    public static final int[] PAD_COMMAND_DOWN = new int[]
    {
    0x69, 0x5C, 0,
    };

    public static final int[] PAD_COMMAND_LEFT = new int[]
    {
    0x6A, 0x5D, 0,
    };

    public static final int[] PAD_COMMAND_RIGHT = new int[]
    {
    0x6B, 0x5E, 0,
    };

    public static final int[] PAD_COMMAND_A = new int[]
    {
    0x6C, 0x5F, 0,
    };

    public static final int[] PAD_COMMAND_B = new int[]
    {
    0x6D, 0x60, 0,
    };

    public static final int[] PAD_COMMAND_C = new int[]
    {
    0x6E, 0x61, 0,
    };

    public static final int[] PAD_COMMAND_D = new int[]
    {
    0x6F, 0x62, 0,
    };

    public static final int[][] COMMANDS = 
        { 
        PAD_COMMAND_UP, PAD_COMMAND_DOWN, PAD_COMMAND_LEFT, PAD_COMMAND_RIGHT,
        PAD_COMMAND_A, PAD_COMMAND_B, PAD_COMMAND_C, PAD_COMMAND_D
        };
        

    /** Returns the index value corresponding to a given CC, or INVALID_INDEX if there is no such index. 
        Return values include:
        PAD_INDEX_UP, PAD_INDEX_DOWN, PAD_INDEX_RIGHT, PAD_INDEX_LEFT, 
        PAD_INDEX_A, PAD_INDEX_B, PAD_INDEX_C, PAD_INDEX_D,
        INVALID_INDEX
    */    
    public static int getIndexForCC(int device, int cc)
        {
        if (device == DEVICE_LAUNCHPAD_MKIII)
            {
            if (cc >= 0x5B && cc < 0x5B + 8)
                {
                return (0 - ((cc - 0x5B) + 1));
                }
            }
        else if (device == DEVICE_LAUNCHPAD_MKI)
            {
            if (cc >= 0x68 && cc < 0x68 + 8)
                {
                return (0 - ((cc - 0x68) + 1));
                }
            }
        
        return INVALID_INDEX;            
        }

    /** Returns the index corresponding to a received note, or INVALID_INDEX. 
        Return values include 0...63, or INVALID_INDEX,
        or (for the APC) PAD_INDEX_UP, PAD_INDEX_DOWN, PAD_INDEX_RIGHT, PAD_INDEX_LEFT, 
        PAD_INDEX_A, PAD_INDEX_B, PAD_INDEX_C, PAD_INDEX_D, and PAD_INDEX_1 ... PAD_INDEX 8 */
    public static int getIndexForNote(int device, int note)
        {
        if (device == DEVICE_LAUNCHPAD_MKIII)
            {
            // Launchpad Mini Mk 3
            int row = 9 - (note / 10);              // flip vertically
            int col = (note % 10);
            if (row < 1 || row > 9) return INVALID_INDEX;
            if (col < 1 || col > 9) return INVALID_INDEX;
            return (row - 1) * 8 + (col - 1);
            }
        else if (device == DEVICE_LAUNCHPAD_MKI)
            {
            // The origin of the MK I is in the top left corner starting at Pitch 0
            // and rows are 16 long
            int row = note / 16;
            int col = note % 16;
            if (col >= 9) return INVALID_INDEX;           // top 8 values
            return row * 8 + col;
            }
        else if (device == DEVICE_AKAI_APC)
            {
            if (note >= 0x64 && note <= 0x6B)
                {
                return PAD_INDICES[note - 0x64];
                }
            else if (note >= 0x70 && note <= 0x77)
                {
                return PAD_INDICES[note - 0x70 + 8];
                }
            else
                {
                // Akai APC Series
                int row = 7 - (note / 8);              // flip vertically
                int col = (note % 8);
                if (row < 0 || row > 8) return INVALID_INDEX;
                if (col < 0 || col > 8) return INVALID_INDEX;
                return row * 8 + col;
                }
            }
        else return INVALID_INDEX;            
        }
    
    // Returns the note to send for the given index, or INVALID_NOTE
    static int getNoteForIndex(int device, int index)
        {
        if (device == DEVICE_LAUNCHPAD_MKIII)
            {
            if (index <= PAD_INDEX_1)
                {
                int col = 9;
                int row = 8 - (index - PAD_INDEX_1);
                return (8 - row) * 10 + col + 1;
                }
            else if (index >= 0 && index < 64)
                {
                // Launchpad Mini Mk 3
                int row = index / 8;
                int col = index % 8;
                return (8 - row) * 10 + col + 1;
                }
            }
        else if (device == DEVICE_LAUNCHPAD_MKI)
            {
            if (index <= PAD_INDEX_1)
                {
                int col = 9;
                int row = 0 - (index - PAD_INDEX_1);
                return row * 16 + col;
                }
            else if (index >= 0 && index < 64)
                {
                // The origin of the MK I is in the top left corner starting at Pitch 0
                // and rows are 16 long
                int row = index / 8;
                int col = index % 8;
                return row * 16 + col;
                }
            }
        else if (device == DEVICE_AKAI_APC)
            {
            if (index <= PAD_INDEX_1)
                {
                return 0x70 - (index - PAD_INDEX_1);
                }
            else if (index <= PAD_INDEX_UP)
                {
                return 0x64 - (index - PAD_INDEX_UP);
                }
            else
                {
                int row = index / 8;
                int col = index % 8;
                return (7 - row) * 8 + col;
                }
            }
        return INVALID_NOTE;            
        }
    
    /** Sends MIDI out the given OUT to set a pad on the given DEVICE type, 
        at the given INDEX, to a color corresponding to the given STATE.
        The INDEX is equal to ROW * 8 + COLUMN, or is one of PAD_INDEX_UP,
        PAD_INDEX_DOWN, PAD_INDEX_LEFT, PAD_INDEX_RIGHT, PAD_INDEX_A,
        PAD_INDEX_B, PAD_INDEX_C, or PAD_INDEX_D. 
        
        The STATE is one of PAD_STATE_UNUSED, PAD_STATE_OFF, PAD_STATE_ON,
        PAD_STATE_WAITING, or PAD_STATE_STOPPING. 
    */
    public static void setPad(Seq seq, int out, int device, int index, int state)
        {
        setPad(seq, out, device, index, state, 0);
        }
                
    /** Sends MIDI out the given OUT to set a pad on the given DEVICE type, 
        at the given INDEX, to a color corresponding to the given STATE and VARIATION.
        The INDEX is equal to ROW * 8 + COLUMN, or is one of PAD_INDEX_UP,
        PAD_INDEX_DOWN, PAD_INDEX_LEFT, PAD_INDEX_RIGHT, PAD_INDEX_A,
        PAD_INDEX_B, PAD_INDEX_C, or PAD_INDEX_D. 
        
        The STATE is one of PAD_STATE_UNUSED, PAD_STATE_OFF, PAD_STATE_ON,
        PAD_STATE_WAITING, or PAD_STATE_STOPPING. 
        
        The PAD_STATE_OFF and PAD_STATE_ON states have variations 0...3
    */
    public static void setPad(Seq seq, int out, int device, int index, int state, int variation)
        {
        if (index < 0 && index >= PAD_INDEX_D && device != DEVICE_AKAI_APC)
            {
            int cc = COMMANDS[(0 - index) - 1][device];
                        
            switch(state)
                {
                case PAD_STATE_UNUSED:
                    seq.forceCC(out, cc, PAD_STATES_UNUSED[device], 1); 
                    break;
                case PAD_STATE_OFF:
                    seq.forceCC(out, cc, PAD_STATES_OFF[device][variation], 1); 
                    break;
                case PAD_STATE_ON:
                    seq.forceCC(out, cc, PAD_STATES_ON[device][variation], 1); 
                    break;
                case PAD_STATE_WAITING:
                    seq.forceCC(out, cc, PAD_STATES_WAITING[device], 1); 
                    break;
                case PAD_STATE_STOPPING:
                    seq.forceCC(out, cc, PAD_STATES_STOPPING[device], 1); 
                    break;
                default:
                    seq.forceCC(out, cc, PAD_STATES_UNUSED[device], 1);                     // FIXME is this wise?          
                    break;
                }
            }
        else
            {
            int note = getNoteForIndex(device, index);
            int channel = (device == DEVICE_AKAI_APC ? 7 : 1);
            switch(state)
                {
                case PAD_STATE_UNUSED:
                    seq.forceNoteOn(out, note, PAD_STATES_UNUSED[device], channel); 
                    break;
                case PAD_STATE_OFF:
                    seq.forceNoteOn(out, note, PAD_STATES_OFF[device][variation], channel); 
                    break;
                case PAD_STATE_ON:
                    seq.forceNoteOn(out, note, PAD_STATES_ON[device][variation], channel); 
                    break;
                case PAD_STATE_WAITING:
                    seq.forceNoteOn(out, note, PAD_STATES_WAITING[device], channel); 
                    break;
                case PAD_STATE_STOPPING:
                    seq.forceNoteOn(out, note, PAD_STATES_STOPPING[device], channel); 
                    break;
                default:
                    seq.forceNoteOn(out, note, PAD_STATES_UNUSED[device], channel);                       // FIXME is this wise?          
                    break;
                }
            }
        }
    
    
    /** Initializes the device.  Call this in reset() perhaps */
    public static void initialize(Seq seq, int out)
        {
        // This magic string puts the Launchpad MK3 into "Programmer Mode" Layout, see "Selecting Layouts",
        // Page 7 of "Launchpad Mini -- Programmers Reference Manual"
        seq.sysex(out, new byte[] { (byte)0xF0, 0x00, 0x20, 0x29, 0x02, 0x0d, 0x00, 0x7F, (byte)0xF7 });        
        // This magic string reinitializes the Akai APC,
        // Page 13 of "AKAI PROFESSIONAL APC MINI MK2 COMMUNICATIONS PROTOCOL (v1.0)"
        seq.sysex(out, new byte[] { (byte)0xF0, 0x47, 0x7F, 0x4F, 0x60, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, (byte)0xF7 });        
        }
    
    /** Clear all command buttons */
    public static void clearCommands(Seq seq, int out, int device)
        {
        for(int i = 0; i < PAD_INDICES.length; i++)
            {
            setPad(seq, out, device, PAD_INDICES[i], PAD_STATE_UNUSED);
            }
        }

    /** Clear all pads in the device's grid */
    public static void clearPads(Seq seq, int out, int device)
        {
        for(int i = 0; i < 64; i++)
            {
            setPad(seq, out, device, i, PAD_STATE_UNUSED);
            }
        }
    }
