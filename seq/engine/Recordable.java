package seq.engine;

/**
   Clips whose data can be read in from MIDI In must implement this interface.
   At present it specifies whether the Clip is armed for recording or not.
**/

public interface Recordable
    {
    public void setArmed(boolean val);
    public boolean isArmed();
    }
