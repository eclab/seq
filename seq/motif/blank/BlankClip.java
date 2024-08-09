package seq.motif.blank;

import seq.engine.*;
import java.util.*;
import java.util.concurrent.*;

public class BlankClip extends Clip
    {
    private static final long serialVersionUID = 1;

    public BlankClip(Seq seq, Blank blank, Clip parent)
        {
        super(seq, blank, parent);
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
        return getPosition() > 0;
        }
    }
