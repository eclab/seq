/* 
   Copyright 2025 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.engine;

public class Pad
    {
    // DEVICES
        
    public static final String[] DEVICE_NAMES = { "Launchpad MK 1", "Launchpad MK 3" };
    public static final int DEVICE_LAUNCHPAD_MKI = 0;
    public static final int DEVICE_LAUNCHPAD_MKIII = 1;

    // PAD STATES
    
    public static final int PAD_STATE_UNUSED = 0;
    public static final int PAD_STATE_OFF = 1;
    public static final int PAD_STATE_ON = 2;
    public static final int PAD_STATE_WAITING = 3;
    public static final int PAD_STATE_STOPPING = 4;
    
    // PAD STATE COLORS FOR DIFFERENT MODELS
        
    public static final int PAD_OFF1_MKIII = 7;                     // dark red
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

    public static final int PAD_WAITING_MKIII = 45;
    public static final int PAD_STOPPING_MKIII = 53;
    
    public static final int PAD_WAITING_MKI = 0x3C;             // yellow
    public static final int PAD_STOPPING_MKI = 63;              // amber

    public static final int[][] PAD_STATES_OFF = new int[][]
    {
    { PAD_OFF1_MKI, PAD_OFF2_MKI, PAD_OFF3_MKI, PAD_OFF4_MKI },
    { PAD_OFF1_MKIII, PAD_OFF2_MKIII, PAD_OFF3_MKIII, PAD_OFF4_MKIII },
    };
                
    public static final int[][] PAD_STATES_ON = new int[][]
    {
    { PAD_ON1_MKI, PAD_ON2_MKI, PAD_ON3_MKI, PAD_ON4_MKI },
    { PAD_ON1_MKIII, PAD_ON2_MKIII, PAD_ON3_MKIII, PAD_ON4_MKIII },
    };

    public static final int[] PAD_STATES_UNUSED = new int[]
    {
    PAD_UNUSED_MKI, PAD_UNUSED_MKIII,
    };

    public static final int[] PAD_STATES_WAITING = new int[]
    {
    PAD_WAITING_MKI, PAD_WAITING_MKIII,
    };

    public static final int[] PAD_STATES_STOPPING = new int[]
    {
    PAD_STOPPING_MKI, PAD_STOPPING_MKIII,
    };
                

    // INDICES AND NOTES    
    public static final int INVALID_INDEX = -256;
    public static final int INVALID_NOTE = -256;
    public static final int PAD_INDEX_UP = -1;
    public static final int PAD_INDEX_DOWN = -2;
    public static final int PAD_INDEX_LEFT = -3;
    public static final int PAD_INDEX_RIGHT = -4;
    public static final int PAD_INDEX_A = -5;
    public static final int PAD_INDEX_B = -6;
    public static final int PAD_INDEX_C = -7;
    public static final int PAD_INDEX_D = -8;
    
    public static final int[] PAD_INDICES = { PAD_INDEX_UP, PAD_INDEX_DOWN, PAD_INDEX_RIGHT, PAD_INDEX_LEFT, PAD_INDEX_A, PAD_INDEX_B, PAD_INDEX_C, PAD_INDEX_D };
    
    public static final int[] PAD_COMMAND_UP = new int[]
    {
    0x68, 0x5B,
    };

    public static final int[] PAD_COMMAND_DOWN = new int[]
    {
    0x69, 0x5C,
    };

    public static final int[] PAD_COMMAND_LEFT = new int[]
    {
    0x6A, 0x5D,
    };

    public static final int[] PAD_COMMAND_RIGHT = new int[]
    {
    0x6B, 0x5E,
    };

    public static final int[] PAD_COMMAND_A = new int[]
    {
    0x6C, 0x5F,
    };

    public static final int[] PAD_COMMAND_B = new int[]
    {
    0x6D, 0x60,
    };

    public static final int[] PAD_COMMAND_C = new int[]
    {
    0x6E, 0x61,
    };

    public static final int[] PAD_COMMAND_D = new int[]
    {
    0x6F, 0x62,
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
        Return values include 0...63 or INVALID_INDEX. */
    public static int getIndexForNote(int device, int note)
        {
        if (device == DEVICE_LAUNCHPAD_MKIII)
            {
            // Launchpad Mini Mk 3
            int row = 9 - (note / 10);              // flip vertically
            int col = (note % 10);
            if (row < 1 || row > 8) return INVALID_INDEX;
            if (col < 1 || col > 8) return INVALID_INDEX;
            return (row - 1) * 8 + (col - 1);
            }
        else if (device == DEVICE_LAUNCHPAD_MKI)
            {
            // The origin of the MK I is in the top left corner starting at Pitch 0
            // and rows are 16 long
            int row = note / 16;
            int col = note % 16;
            if (col >= 8) return INVALID_INDEX;           // top 8 values
            return row * 8 + col;
            }
        else return INVALID_INDEX;            
        }
    
    // Returns the note to send for the given index, or INVALID_NOTE
    static int getNoteForIndex(int device, int index)
        {
        if (index < 0 || index >= 64) 
            {
            return INVALID_NOTE;
            }
        else if (device == DEVICE_LAUNCHPAD_MKIII)
            {
            // Launchpad Mini Mk 3
            int row = index / 8;
            int col = index % 8;
            return (8 - row) * 10 + col + 1;
            }
        else if (device == DEVICE_LAUNCHPAD_MKI)
            {
            // The origin of the MK I is in the top left corner starting at Pitch 0
            // and rows are 16 long
            int row = index / 8;
            int col = index % 8;
            return row * 16 + col;
            }
        else return INVALID_NOTE;            
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
        if (index < 0)
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
                
            switch(state)
                {
                case PAD_STATE_UNUSED:
                    seq.forceNoteOn(out, note, PAD_STATES_UNUSED[device], 1); 
                    break;
                case PAD_STATE_OFF:
                    seq.forceNoteOn(out, note, PAD_STATES_OFF[device][variation], 1); 
                    break;
                case PAD_STATE_ON:
                    seq.forceNoteOn(out, note, PAD_STATES_ON[device][variation], 1); 
                    break;
                case PAD_STATE_WAITING:
                    seq.forceNoteOn(out, note, PAD_STATES_WAITING[device], 1); 
                    break;
                case PAD_STATE_STOPPING:
                    seq.forceNoteOn(out, note, PAD_STATES_STOPPING[device], 1); 
                    break;
                default:
                    seq.forceNoteOn(out, note, PAD_STATES_UNUSED[device], 1);                       // FIXME is this wise?          
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
