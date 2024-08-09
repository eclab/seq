package seq.motif.select;

import seq.engine.*;
import java.util.*;
import seq.motif.blank.*;
import java.util.concurrent.*;
import javax.sound.midi.*;

/// FIXME: At present in Multi modes if you launch a clip but it hasn't started yet,
/// then cancel it, it will still play once.


public class SelectClip extends Clip
    {
    private static final long serialVersionUID = 1;

    /// NODES

    public static class Node
        {
        public Clip clip;
        public int index;                               // Where the node is in the children array
        public int state;                               // The play state of the node
        public double randomOrigin;
        public double randomVariance;
        public boolean randomDeterministic;
        int lastPos = 0;
        double cumulativeRate = 0.0;
        
        public Motif.Child getChild(SelectClip parent)
            {
            // FIXME: will this work at the high end?  Is the array fully populated?
            return parent.getMotif().getChild(index);
            }
                
        public Clip getClip() { return clip; }
        public Node(Clip clip, int index) { this.clip = clip; this.index = index; state = OFF; }
        public Select.Data getData(SelectClip parent) { return (Select.Data)(((Select)(parent.getMotif())).getChildren().get(index).getData()); }
        }

    // All nodes.  There are 64 in all.  Some may be null because they haven't been formed yet, or because they are blank.  Use getChild() to get a child from this array.
    ArrayList<Node> children = new ArrayList<>();
    // Currently playing nodes
    ArrayList<Node> playing = new ArrayList<>();
    // Nodes scheduled to start playing at startPos
    ArrayList<Node> next = new ArrayList<>();
    // Nodes scheduled to be removed because the user has cancelled them (in Multi-repeat mode)
    ArrayList<Node> remove = new ArrayList<>();
    // Temporary buffer used to keep playing to the nodes that are still playing
    ArrayList<Node> keep = new ArrayList<>();
    
    // When the next nodes are scheduled to come online
    int startPos = 0;
    // Do I believe I'm finished?
    boolean finished = false;
    // Has the user asked me to declare that I'm finished?
    boolean shouldFinish = false;
    // Has the user asked me to release all current note offs?
    boolean shouldRelease = false;
    
    // The node that was just advanced, so we can tell who's who when it sends us MIDI information
    int current = -1;

    // Current pads that have been pressed in the UI (not the Launchpad) 
    ArrayList<Integer> inputFromUI = null;

    //// NODE STATES
    public static final int OFF = 0;                                    // Node is not playing
    public static final int ON = 1;                                             // Node is playing
    public static final int WAITING = 2;                                // Node is waiting to play on next startPos
    public static final int STOPPING = 3;                               // Node is waiting to stop at the end of its latest iteration
    public static final int UNUSED = 4;                                 // Node is not capable of playing
    
    public void terminate() 
        { 
        super.terminate();
        terminateNodes((Select)getMotif());  
        clearPads();       
        }

    /** Schedule a finish to be issued */
    public void doFinish() { shouldFinish = true; }

    /** Schedule a release to be issued */
    public void doRelease() { shouldRelease = true; }
    
    public SelectClip(Seq seq, Select series, Clip parent)
        {
        super(seq, series, parent);
        rebuild();
        }
    
    public void rebuild(Motif motif)
        {
        if (this.getMotif() == motif)
            {
            rebuild();
            }
        else
            {
            for(Node node : children)
                {
                if (node != null)
                    {
                    node.clip.rebuild(motif);
                    }
                }
            }
        }

    void reset(Node node)
        {
        loadParameterValues(node.clip, node.getChild(this));
        node.clip.reset();
        node.lastPos = -1;
        node.cumulativeRate = 0;
        }

    void loop(Node node)
        {
        node.clip.loop();
        }

    // Sets all the pads to NULL, so they'll have to be rebuilt with getChild() dynamically
    public void rebuild()
        {
        terminate();
        release();                // we may have outstanding note-offs and can't translate them etc. after nodes are deleted 
        children.clear();
        for(int i = 0; i < Select.MAX_CHILDREN; i++)
            {
            children.add(null);
            }
        version = getMotif().getVersion();
        }
        
    public ArrayList<Node> getPlayingNodes() { return playing; }
        
    public ArrayList<Node> getNextNodes() { return next; }

    public ArrayList<Node> getRemoveNodes() { return remove; }
                
    /** Returns Node #child from the children array.  If the motif is Blank, then NULL is returned.
        If the Node has not yet been constructed, it may be built on the fly and then returned. */
    public Node getChild(int child)
        {
        if (children.get(child) == null)                   // there isn't one
            {
            Select select = (Select)getMotif();
            ArrayList<Motif.Child> kids = select.getChildren();
            if (kids.get(child).getMotif() instanceof Blank)
                {
                return null;
                }
            Node node = new Node(kids.get(child).getMotif().buildClip(this), child);
            children.set(child, node);
            return node;
            }
        else return children.get(child);
        }
        
    /// Updates the pads (lights 'em up)
    void updatePads()
        {
        int out = ((Select)getMotif()).getOut();
        int numChildren = children.size();
        for(int i = 0; i < Select.MAX_CHILDREN; i++)
            {
            if (i >= numChildren)               // I don't think this ever happens any more
                {
                setPad(out, Select.getNoteForIndex(i), OFF);
                }
            else
                {
                Node m = children.get(i);
                if (m == null || m.clip.getMotif() instanceof Blank)
                    {
                    setPad(out, Select.getNoteForIndex(i), UNUSED);
                    }
                else
                    {
                    setPad(out, Select.getNoteForIndex(i), children.get(i).state);
                    }
                }
            }
        }

    /// Clear Pads
    void clearPads()
        {
        int out = ((Select)getMotif()).getOut();
        for(int i = 0; i < Select.MAX_CHILDREN; i++)
            {
            setPad(out, Select.getNoteForIndex(i), UNUSED);
            }
        }

    public static final int PAD_UNUSED = 0;
    public static final int PAD_OFF = 1;
    public static final int PAD_WAITING = 45;
    public static final int PAD_ON = 5;
    public static final int PAD_STOPPING = 53;
        
    // Given a Novation Launchpad pad at the provided OUT, sets the pad NOTE to the given STATE
    void setPad(int out, int note, int state)
        {
        if (state == UNUSED)
            {
            seq.forceNoteOn(out, note, PAD_UNUSED, 1);                          // Turn the Light Off
            }
        else if (state == ON)                                                   
            {
            seq.forceNoteOn(out, note, PAD_ON, 1);              // RED
            }
        else if (state == OFF)
            {
            seq.forceNoteOn(out, note, PAD_OFF, 1);                             // Gray
            }
        else if (state == WAITING)
            {
            seq.forceNoteOn(out, note, PAD_WAITING, 1);                         // BLUE
            }
        else if (state == STOPPING)
            {
            seq.forceNoteOn(out, note, PAD_STOPPING, 1);                                // MAGENTA
            }
        }

    // Extracts an OUT and NOTE from a given NODE, sets the node to the given state,
    // then given a Novation Launchpad pad at the provided OUT, sets the pad NOTE to the given STATE
    void setPad(Node node, int state)
        {
        if (node == null) return;
        if (node.clip.getMotif() instanceof Blank) return;    
        if (node.state == state) return;                // Don't set it again 
        node.state = state;
        Select select = ((Select)getMotif());
        setPad(select.getOut(), select.getNoteForIndex(node.index), state);
        }

    // Terminates a node and cuts or releases it.
    void terminateNode(Node node, Select select)
        {
        if (node == null) return;
        node.clip.terminate();
        if (select.getCut())
            node.clip.cut();
        else
            node.clip.release();
        }
        
    // Terminates all nodes and cuts or releases them.
    void terminateNodes(Select select)
        {
        for(Node node : playing)
            {
            terminateNode(node, select);
            }
        for(Node node : remove)
            {
            terminateNode(node, select);
            }
        }

    public void cut()  
        {
        for(Node node : playing)
            {
            node.clip.cut();
            setPad(node, OFF);
            }
        terminateNodes((Select)getMotif());
        playing.clear();
        remove.clear();
        next.clear();
        keep.clear();
        }
        
    public void release()  
        {
        for(Node node : playing)
            {
            node.clip.release();
            setPad(node, OFF);
            }
        terminateNodes((Select)getMotif());
        playing.clear();
        remove.clear();
        next.clear();
        keep.clear();
        shouldRelease = false;
        }
        
    // Determines the next start position, which may be NOW or quantized to the nearest measure etc.        
    void setupStartPos()
        {
        Select select = (Select) getMotif();
        // quantize to next
        int quantization = select.getQuantization();
        int pos = getPosition();
        if (quantization == Select.QUANTIZATION_NONE)
            {
            startPos = pos;
            }
        else
            {
            int q = Select.QUANTIZATIONS[quantization];
            startPos = (pos / q) * q;
            if (startPos < pos) startPos += q;
            }
        }
        
    // Determines the next start position, which may be NOW or may be quantized to the nearest measure etc.        
    boolean advance(Node node, double rate)
        {
        current = node.index;

        if (rate == 1.0) return node.clip.advance();
        else
            {
            boolean result = false;
            node.cumulativeRate += rate;
            for( /* Empty */ ; node.lastPos < node.cumulativeRate; node.lastPos++)
                {
                result = result || node.clip.advance();
                }
            return result;
            }
        }

    public void reset()
        {
        super.reset();
        
        Select select = (Select) getMotif();
        // This magic string puts the Launchpad MK3 into "Programmer Mode" Layout, see "Selecting Layouts",
        // Page 7 of "Launchpad Mini -- Programmers Reference Manual"
        seq.sysex(select.getOut(), new byte[] { (byte)0xF0, 0x00, 0x20, 0x29, 0x02, 0x0d, 0x00, 0x7F, (byte)0xF7 });        

        startPos = 0;
        terminateNodes((Select)getMotif());
        playing.clear();
        remove.clear();
        Node node = getChild(0);
        if (select.getPlayFirst() && node != null)
            {
            next.add(node);
            setupStartPos();
            }
        finished = false;
        shouldRelease = false;
        shouldFinish = false;
        updatePads();
        }
        
    // Indicates that a pad has been pressed.  This in turn will add to inputFromUI the pad
    public void post(int pad)
        {
        if (inputFromUI == null) inputFromUI = new ArrayList<Integer>();
        Node node = getChild(pad);

        if (node == null)                               // it's blank
            {
            // do nothing
            }
        else if (node.state == OFF)             // We want it to play
            {
            inputFromUI.add(pad);
            }
        else if (node.state == ON || node.state == WAITING)             // We want it to stop
            {
            inputFromUI.add(pad);
            }
        else if (node.state == STOPPING)
            {
            // do nothing
            }
        }
    
    // given inputFromUI, extracts pads and either adds their nodes to PUTPLAYINGHERE or PUTSTOPPINGHERE.
    // PUTSTOPPINGHERE may be null, in which case nodes to be added to it are just dropped.
    void getChildrenFromUI(ArrayList<Node> putPlayingHere, ArrayList<Node> putStoppingHere)
        {
        if (inputFromUI != null)
            {
            int len = inputFromUI.size();
            if (len > 0)
                {
                for (int i = 0; i < len; i++)
                    {
                    int c = inputFromUI.get(i);
                    Node child = getChild(c);
                    if (child != null)
                        {
                        if (putStoppingHere != null)
                            {
                            if ((playing.contains(child) || next.contains(child)))        // ugh, O(n)
                                {
                                if (!putStoppingHere.contains(child)) putStoppingHere.add(child);       // ugh, O(n)
                                }
                            else
                                {
                                if (!putPlayingHere.contains(child)) putPlayingHere.add(child); // ugh, O(n)
                                }
                            }
                        else
                            {
                            if (!putPlayingHere.contains(child)) putPlayingHere.add(child); // ugh, O(n)
                            }
                        }
                    else
                        {
                        System.err.println("SelectClip.getChildrenFromUI: Child " + c + " is null");
                        }
                    }
                }
            inputFromUI.clear();
            }
        }

    // given current MIDI messages, extracts pads and either adds their nodes to PUTPLAYINGHERE or PUTSTOPPINGHERE.
    // PUTSTOPPINGHERE may be null, in which case nodes to be added to it are just dropped.
    void getChildrenFromMIDI(ArrayList<Node> putPlayingHere, ArrayList<Node> putStoppingHere)
        {
        Select select = (Select) getMotif();
        if (select.getPlayingClip() != this) return;            // These messages are not for me
        In in = seq.getIn(select.getIn());
        MidiMessage[] messages = in.getMessages();
        for(int i = messages.length - 1; i >= 0; i--)
            {
            if (isCC(messages[i]))
                {
                int cc = ((ShortMessage)messages[i]).getData1();
                int c = select.getIndexForCC(cc);
                if (c == Select.DEFAULT_FINISH_OFFSET)
                    {
                    doFinish();
                    }
                else if (c == Select.DEFAULT_RELEASE_ALL_NOTES_OFFSET)
                    {
                    doRelease();
                    }
                else if (select.getIn() == select.getCCIn())
                    {
                    processCCIn((ShortMessage)messages[i], select);
                    }
                }
            else if (isNoteOn(messages[i]))
                {
                int velocity = ((ShortMessage)messages[i]).getData2();
                if (velocity > 0)       // otherwise it's actually a NOTE OFF
                    {
                    int c = ((Select)getMotif()).getIndexForNote(((ShortMessage)messages[i]).getData1());
                    if (c >= 0 && c < select.getChildren().size()) // we have a child
                        {
                        Node child = getChild(c);
                        if (child != null)
                            {
                            if (putStoppingHere != null)
                                {
                                if (playing.contains(child) || next.contains(child))        // ugh, O(n)
                                    {
                                    if (!putStoppingHere.contains(child)) putStoppingHere.add(child); // ugh, O(n)
                                    }
                                else
                                    {
                                    if (!putPlayingHere.contains(child)) putPlayingHere.add(child); // ugh, O(n)
                                    }
                                }
                            else
                                {
                                if (!putPlayingHere.contains(child)) putPlayingHere.add(child); // ugh, O(n)
                                }
                            }
                        else
                            {
                            System.err.println("SelectClip.getChildrenFromMIDI: Child " + c + " is null");
                            }
                        }
                    }
                }
                                
            }
        }
        



    // The last CC parameter received
    int lastCC = -1;
    // The motif parameter corresponding to the last CC parameter
    int lastParam = -1;
        
    // Processes the given CC message
    void processCCIn(ShortMessage ccMessage, Select select)
        {
        int cc = ccMessage.getData1();
        // First a little caching
        if (lastCC == cc)
            {
            double data = ccMessage.getData2() / 127.0;
            for (SelectClip.Node node : playing)
                {
                node.clip.setParameterValue(lastParam, data);
                }
            }
        else
            {
            // This is probably faster than hashing as there's only 8 parameters
            for(int j = 0; j < Motif.NUM_PARAMETERS; j++)
                {
                if (select.getCC(j) == cc) // got it
                    {
                    lastCC = cc;
                    lastParam = j;
                    double data = ccMessage.getData2() / 127.0;
                    for (SelectClip.Node node : playing)
                        {
                        node.clip.setParameterValue(lastParam, data);
                        }
                    }
                }
            }
        }
        
    // Processes all CC messages
    void processCCIn()
        {
        Select select = (Select) getMotif();
        if (select.getPlayingClip() != this) return;            // These messages are not for me
        if (select.getIn() != select.getCCIn()) // not already processed in getChildrenFromMIDI
            {
            In in = seq.getIn(select.getIn());
            MidiMessage[] messages = in.getMessages();
            for(int i = messages.length - 1; i >= 0; i--)
                {
                if (isCC(messages[i]))
                    {
                    processCCIn((ShortMessage)messages[i], select);
                    }
                }
            }
        }
    
    
    
    //// PROCESSING DIFFERENT MODES
    
    
    void processSingle(Select select, boolean repeating)
        {
        getChildrenFromMIDI(next, repeating ? remove : null);
        getChildrenFromUI(next, repeating ? remove : null);

        processCCIn();
        if (select.getQuantization() == Select.QUANTIZATION_NONE)
            {
            if (shouldRelease) release();
            if (shouldFinish) { finished = true; shouldFinish = false; terminateNodes((Select)getMotif()); }
            }
                
        // remove immediate clips, or mark scheduled clips
        if (!remove.isEmpty())
            {
            // get the last one
            Node last = remove.get(remove.size() - 1);

            // If we're in immediate mode, or last isn't playing yet 
            // (we removed it when it was scheduled to be played), 
            // remove them now
            if (select.getQuantization() == Select.QUANTIZATION_NONE ||
                last.state == WAITING)
                {
                // Terminate and clear the node
                terminateNode(last, select);                        // also cuts/releases the node
                setPad(last, OFF);
                
                remove.clear();
                playing.remove(last);           // should this be playing.clear()?
                next.remove(last);                      // it might be scheduled next
                }
            else
                {
                // Remove other scheduled-to-be-removed nodes
                remove.clear();
                remove.add(last);
                setPad(last, STOPPING);
                }
            }

        // Schedule any new node
        if (!next.isEmpty())
            {
            // get the last one
            Node last = next.get(next.size() - 1);

            // If we're in immediate mode, launch them now
            if (select.getQuantization() == Select.QUANTIZATION_NONE)
                {
                // Terminate and clear all playing and remove nodes
                for(Node node : playing)
                    {
                    terminateNode(node, select);                        // also cuts/releases the node
                    setPad(node, OFF);
                    }

                for(Node node : remove)
                    {
                    terminateNode(node, select);                        // also cuts/releases the node
                    setPad(node, OFF);
                    }
                                
                playing.clear();
                remove.clear();
                next.clear();

                reset(last);
                playing.add(last);
                setPad(last, ON);
                startPos = getPosition();
                }
            else
                {
                for(Node node : next)
                    {
                    if (node != last) setPad(node, OFF);
                    }
                    
                next.clear();
                if (!remove.contains(last)) 
                    {
                    next.add(last);
                    setPad(last, WAITING);
                    setupStartPos();
                    }
                }
            }


        // play the child
        if (!playing.isEmpty())
            {
            // get the last one -- there can really only be one
            int playingNode = playing.size() - 1;               // this is dumb

            Node node = playing.get(playingNode);
            boolean removeContains = remove.contains(node);         // Ugh O(n) FIXME
            Select.Data data = (Select.Data)(node.getChild(this).getData());
                        
            if (advance(node, getCorrectedValueDouble(data.getRate(), Select.Data.MAX_RATE)))
                {
                // Repeating nodes play forever unless they have
                // been either (1) manually removed or (2) someone else has been scheduled to take their place
                // In the second case we remove the repeating node here, but a bit below
                // we will add the next guy at the quantized time
                if (repeating && !removeContains && next.isEmpty())
                    {
                    loop(node);
                    if (startPos <= getPosition())
                        setupStartPos();
                    if (startPos != getPosition())          // gotta schedule for later
                        {
                        playing.remove(node);
                        setPad(node, WAITING);
                        next.add(node);
                        setupStartPos();
                        }
                    else
                        {
                        // Do nothing, we stay in playing
                        }
                    }
                else            // we're done
                    {
                    remove.remove(node);
                    playing.remove(node);
                    terminateNode(node, select);
                    setPad(node, OFF);
                    }
                                
                // I was in remove, get rid of me and terminate me
                if (removeContains)
                    {
                    remove.remove(node);
                    playing.remove(node);
                    terminateNode(node, select);
                    setPad(node, OFF);
                    }
                }
            else
                {
                // do nothing
                }
            }
 
        // add new quantized clips or repeats at quantize position only when the previous clip has finished
        if (startPos <= getPosition())
            {
            if (shouldRelease) release();
            if (shouldFinish) { finished = true; shouldFinish = false; terminateNodes((Select)getMotif()); }

            if (playing.isEmpty() && !next.isEmpty())
                {
                // get the last one
                Node node = next.get(next.size() - 1);
                reset(node);

                playing.add(node);
                setPad(node, ON);
                next.clear();
                }
            }
        }
        
    void processMulti(Select select, boolean repeating)
        {
        // Load into next and remove all the new children being added and removed
        getChildrenFromMIDI(next, repeating ? remove : null);
        getChildrenFromUI(next, repeating ? remove : null);
        processCCIn();

        // If the user asked to release or finish, and we're in immediate mode, do so now
        if (select.getQuantization() == Select.QUANTIZATION_NONE)
            {
            if (shouldRelease) release();
            if (shouldFinish) { finished = true; shouldFinish = false; terminateNodes((Select)getMotif()); }
            }

        // remove immediate clips, or mark scheduled clips
        if (!remove.isEmpty())
            {
            // Check for waiting nodes and remove them.  This is much more involved than in the Single case
            ArrayList<Node> removeFromRemove = null;
            for(Node node : remove)
                {
                if (node.state == WAITING)
                    {
                    terminateNode(node, select);                        // also cuts/releases the node
                    setPad(node, OFF);

                    // Remove the node if it's in next or currently playing
                    // Ugh, this is O(n^2)
                    next.remove(node);
                    playing.remove(node);
                    // add to removeFromRemove so we can remove them SELECTIVELY rather than clearing everyone right now
                    if (removeFromRemove == null) removeFromRemove = new ArrayList<Node>();
                    removeFromRemove.add(node);
                    }
                }
            if (removeFromRemove != null)
                {
                for(Node node : removeFromRemove)
                    {
                    remove.remove(node);
                    }
                }
                        
            // If we're in immediate mode, remove them now
            if (select.getQuantization() == Select.QUANTIZATION_NONE)
                {
                for(Node node : remove)
                    {
                    terminateNode(node, select);                        // also cuts/releases the node
                    setPad(node, OFF);

                    // Remove the node if it's in next or currently playing
                    // Ugh, this is O(n^2)
                    next.remove(node);
                    playing.remove(node);
                    }
                remove.clear();
                }
            else
                {
                // Mark the node as waiting to be removed when it finishes
                for(Node node : remove)
                    {
                    setPad(node, STOPPING);
                    }
                }
            }

        // Launch immediate clips, or mark scheduled clips
        if (!next.isEmpty())
            {
            // If we're in immediate mode, launch them now
            if (select.getQuantization() == Select.QUANTIZATION_NONE)
                {
                for(Node node : next)
                    {
                    // Reset and set the pad on
                    reset(node);            
                    setPad(node, ON);
                    }
                
                // Load into playing
                playing.addAll(next);
                next.clear();
                }
            else
                {
                // Mark the node as waiting to be added
                for(Node node : next)
                    {
                    if (!remove.contains(node))
                        {
                        setPad(node, WAITING);
                        setupStartPos();
                        }
                    }
                }
            }
        
        // add new quantized clips remaining in next if it's time
        if (startPos <= getPosition())
            {
            if (shouldRelease) release();
            if (shouldFinish) { finished = true; shouldFinish = false; terminateNodes((Select)getMotif()); }

            for(Node node : next)
                {
                // If they're not already playing, reset them and prepare them to be added
                // This removes them IF they're already playing but it's okay, we're adding them right back again
                if (!playing.remove(node))                       // Ugh O(n) FIXME
                    {
                    reset(node);            
                    setPad(node, ON);
                    }
                }
            playing.addAll(next);
            next.clear();
            }

        // play or identify clips to remove
        keep.clear();
        ArrayList<Motif.Child> children = getMotif().getChildren();
        for(Node node : playing)
            {
            // Does remove currently contain this node?  If so, it's waiting to be removed when it's finished
            boolean removeContains = remove.contains(node);         // Ugh O(n) FIXME
            Select.Data data = (Select.Data)(getChild(node.index).getData(this));
        
            if (advance(node, getCorrectedValueDouble(data.getRate(), Select.Data.MAX_RATE)))                   // is the node finished?
                {
                terminateNode(node, select);            // terminate and release/cut it
                
                if (repeating && !removeContains)       // we're repeating and didn't ask to delete it
                    {
                    loop(node);
//                              if (startPos <= getPosition())
//                                              setupStartPos();
                    if (startPos == getPosition())
                        keep.add(node);                                     // Keep it to play next time
                    else
                        {
                        setPad(node, WAITING);
                        next.add(node);                                 // Schedule to play in a bit
                        setupStartPos();
                        }
                    }
                else                                                            // It's gone
                    {
                    remove.remove(node);
                    terminateNode(node, select);
                    setPad(node, OFF);
                    }
                    
                if (removeContains)                             // If the node was in remove
                    {
                    remove.remove(node);
                    terminateNode(node, select);
                    setPad(node, OFF);
                    }
                }
            else                                                                        // The node is not finished yet
                {
                keep.add(node);
                }
            }
                
        // remove clips
        if (keep.size() != playing.size())
            {
            playing = keep;
            keep = new ArrayList<Node>();
            }
        }
            
    public boolean process()
        {
        Select select = (Select) getMotif();
        switch(select.getMode())
            {
            case Select.MODE_SINGLE_ONE_SHOT:
                processSingle(select, false);
                break;
            case Select.MODE_SINGLE_REPEATING:
                processSingle(select, true);
                break;
            case Select.MODE_MULTI_ONE_SHOT:
                processMulti(select, false);
                break;
            case Select.MODE_MULTI_REPEATING:               
                processMulti(select, true);
                break;
            }
        return finished;
        }
  

    public boolean noteOn(int out, int note, double vel) 
        {
        if (current >= 0)
            {
            Select.Data data = ((Select.Data)(getMotif().getChildren().get(current).data));
            if (data.getOut() != Select.Data.DISABLED)
                {
                out = data.getOut();
                }
            note += getCorrectedValueInt(data.getTranspose() , Select.Data.MAX_TRANSPOSE * 2) - Select.Data.MAX_TRANSPOSE;
            if (note > 127) note = 127;                 // FIXME: should we instead just not play the note?
            if (note < 0) note = 0;                             // FIXME: should we instead just not play the note?
            vel *= getCorrectedValueDouble(data.getGain(), Select.Data.MAX_GAIN);
            if (vel > 127) vel = 127;                   // FIXME: should we check for vel = 0?
            }
        return super.noteOn(out, note, vel);
        }
        
    public boolean noteOff(int out, int note, double vel) 
        {
        if (current >= 0)
            {
            Select.Data data = ((Select.Data)(getMotif().getChildren().get(current).data));
            if (data.getOut() != Select.Data.DISABLED)
                {
                out = data.getOut();
                }
            note += getCorrectedValueInt(data.getTranspose(), Select.Data.MAX_TRANSPOSE * 2) - Select.Data.MAX_TRANSPOSE;
            if (note > 127) note = 127;                 // FIXME: should we instead just not play the note?
            if (note < 0) note = 0;                             // FIXME: should we instead just not play the note?
            }
        return super.noteOff(out, note, vel);
        }
        
    public void scheduleNoteOff(int out, int note, double vel, int time) 
        {
        if (current >= 0)
            {
            Select.Data data = ((Select.Data)(getMotif().getChildren().get(current).data));
            if (data.getOut() != Select.Data.DISABLED)
                {
                out = data.getOut();
                }
            note += getCorrectedValueInt(data.getTranspose(), Select.Data.MAX_TRANSPOSE * 2) - Select.Data.MAX_TRANSPOSE;
            if (note > 127) note = 127;                 // FIXME: should we instead just not play the note?
            if (note < 0) note = 0;                             // FIXME: should we instead just not play the note?
            vel *= getCorrectedValueDouble(data.getGain(), Select.Data.MAX_GAIN);
            if (vel > 127) vel = 127;                   // FIXME: should we check for vel = 0?
            super.scheduleNoteOff(out, note, vel, (int)(time / getCorrectedValueDouble(data.getRate())));
            }
        else System.err.println("SelectClip.scheduleNoteOff: current was " + current);
        }
        
        
        
    // TESTING
    public static void main(String[] args) throws Exception
        {
        // Set up Seq
        Seq seq = new Seq();            // starts timer running
        seq.setupForMIDI(SelectClip.class, args, 1, 3);   // sets up MIDI in and out
        seq.setOut(0, new Out(seq, 0));         // Out 0 points to device 0 in the tuple.  This is too complex.
        seq.setOut(1, new Out(seq, 1));         // Out 0 points to device 0 in the tuple.  This is too complex.
        seq.setOut(2, new Out(seq, 2));         // Out 0 points to device 0 in the tuple.  This is too complex.
        
        // Set up our module structure
        Select select = new Select(seq);
        select.setQuantization(Select.QUANTIZATION_FOUR_QUARTERS);
        // select.setMode(Select.MODE_SINGLE_ONE_SHOT);
        select.setMode(Select.MODE_SINGLE_REPEATING);
        // select.setMode(Select.MODE_MULTI_ONE_SHOT);
        // select.setMode(Select.MODE_MULTI_REPEATING);
        select.setOut(2);
                
        // Set up first StepSequence
        seq.motif.stepsequence.StepSequence dSeq = new seq.motif.stepsequence.StepSequence(seq, 2, 16);
        
        // Specify notes
        dSeq.setTrackNote(0, 60);
        dSeq.setTrackNote(1, 120);
        dSeq.setTrackOut(0, 0);
        dSeq.setTrackOut(1, 1);
        dSeq.setDefaultSwing(0.33);
        
        // Load the StepSequence with some data
        dSeq.setVelocity(0, 0, 1, true);
        dSeq.setVelocity(0, 4, 5, true);
        dSeq.setVelocity(0, 8, 9, true);
        dSeq.setVelocity(0, 12, 13, true);
        dSeq.setVelocity(1, 1, 2, true);
        dSeq.setVelocity(1, 2, 3, true);
        dSeq.setVelocity(1, 3, 4, true);
        dSeq.setVelocity(1, 5, 6, true);
        dSeq.setVelocity(1, 7, 8, true);
        dSeq.setVelocity(1, 9, 10, true);
        dSeq.setVelocity(1, 10, 11, true);
        dSeq.setVelocity(1, 15, 16, true);
        

        // Set up second StepSequence
        seq.motif.stepsequence.StepSequence dSeq2 = new seq.motif.stepsequence.StepSequence(seq, 2, 16);
        
        // Specify notes
        dSeq2.setTrackNote(0, 45);
        dSeq2.setTrackNote(1, 90);
        dSeq2.setTrackOut(0, 0);
        dSeq2.setTrackOut(1, 1);
        dSeq2.setDefaultSwing(0);
        
        // Load the StepSequence with some data
        dSeq2.setVelocity(0, 0, 1, true);
        dSeq2.setVelocity(0, 2, 5, true);
        dSeq2.setVelocity(0, 4, 9, true);
        dSeq2.setVelocity(0, 6, 13, true);
        dSeq2.setVelocity(1, 8, 2, true);
        dSeq2.setVelocity(1, 9, 3, true);
        dSeq2.setVelocity(1, 10, 4, true);
        dSeq2.setVelocity(1, 11, 6, true);
        dSeq2.setVelocity(1, 12, 2, true);
        dSeq2.setVelocity(1, 13, 3, true);
        dSeq2.setVelocity(1, 14, 4, true);
        dSeq2.setVelocity(1, 15, 6, true);

        // Load into series
        select.replaceChild(dSeq, 0);
        select.replaceChild(dSeq2, 1);
        select.replaceChild(dSeq2, 2);
        
        //select.setClear(true);
                
        // Build Clip Tree
        seq.setData(select);

        seq.reset();
        seq.play();

        seq.waitUntilStopped();
        }

    }
