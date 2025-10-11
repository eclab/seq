/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.silence;

import seq.engine.*;
import java.util.*;
import java.util.concurrent.*;

public class SilenceClip extends Clip
    {
    private static final long serialVersionUID = 1;

    public SilenceClip(Seq seq, Silence silence, Clip parent)
        {
        super(seq, silence, parent);
        rebuild();
        }
        
    public void rebuild(Motif motif)
        {
        if (this.getMotif() == motif)
            {
            rebuild();
            }
        }
                
    public void rebuild() 
        { 
        version = getMotif().getVersion();
        }
        
    // We allow one step before we declare we're done, so as to avoid
    // infinite zero-length cycles
    public boolean process()
        {
        /*
          System.err.println(getParameterValue(0));
          int v = (int)(getParameterValue(0) * 79);
          for(int j = 0; j < v; j++)
          System.err.print(" ");
          System.err.println("X");
        */

        int length = ((Silence)getMotif()).getLength();
        return getPosition() >= length - 1;
        }
    }
