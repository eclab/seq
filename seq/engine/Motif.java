package seq.engine;

import seq.util.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.*;
import org.json.*;

/**
   Abstract superclass of the Static Data portion of all modules in the sequencer.
        
   <p>"Static Data" means data and parameters set by the user, as opposed to "Dynamic Data",
   which is the current play state information (such as the current timestep, or which
   sub-module is presently playing).  Static Data is handled by Motifs, and Dynamic Data 
   is handled by Clips.
        
   <p>A Motif's static data may be used multiple times in a sequence, even simultaneously,
   and thus is paired with multiple Dynamic Data objects and thus with multiple Clips.
        
   <p>A Motif may have child Motifs.  This is a DAG: it's entirely possible that a given 
   child Motif may appear multiple times as a child of a given parent Motif.  We need a
   way to keep these multiple appearances distinct and unique.  This is where the Motif.Child
   class comes in: it represents children and is unique.  A Motif.Child holds a Motif; thus
   a single Motif might be held by multiple Motif.Child objects.
        
   <p>A Motif also has parent Motifs.  This is still a DAG of course, but it's not important
   to the sequencer to maintain and distinguish unique parent objects, so we just have a list
   of parent Motif objects.
        
**/

public abstract class Motif implements Cloneable
    {
    private static final long serialVersionUID = 1;

    /** The wrapper for child Motifs which allows the same Motif to be referenced distinctly
        and uniquely in the children ArrayList. */
    public static class Child
        {
        /** The motif stored in the Child */
        Motif motif;
        /** The child's nickname, if any, initially null */
        public String nickname = null;
        /** The data object of the Child */
        public Object data = null;
        
        
        /// PARAMETERS
        
        /// parameter[x] can be one of several things:
        /// >=0: a ground value
        /// -1: bound to random
        /// -2 ... -(NUM_PARAMETERS + 1) inclusive: a link to a parent parameter
        /// FIXME: Maybe a MIDI CC or something later?
        public static final double PARAMETER_RANDOM = -1;
        double[] parameters = new double[NUM_PARAMETERS];

        public double getParameter(int param) { return parameters[param]; }
        public double[] getParameters() { return parameters; }
        public void setParameter(int param, double val) { parameters[param] = val; }


        /// THE RANDOM PARAMETER
        /// The Random Parameter is updated during clip.reset(), by calling Motif.Child.generateRandomValue()
        /// and storing the result.  This value is computed by taking the Motif.Child's RANDOM MIN and MAX
        // and selecting a value under the resulting distribution.
        
        /// random min and random max can each be one of several things:
        /// >=0: a ground value
        /// -1: bound to random
        /// -2 ... -(NUM_PARAMETERS + 1) inclusive: a link to a parent parameter

        double randomMin = 0.0;
        double randomMax = 1.0;
        public void setRandomMin(double val) { randomMin = val; }
        public double getRandomMin() { return randomMin; }
        public void setRandomMax(double val) { randomMax = val; }
        public double getRandomMax() { return randomMax; }
                
        public double generateRandomValue(double min, double max)
            {
            return motif.generateRandomValue(min, max);
            }
                        

        /** Returns the motif */
        public Motif getMotif() { return motif; }
        /** Sets the motif */
        public void setMotif(Motif val) { this.motif = val; }
        /** Returns the nickname, which is a user-settable name for this child (as opposed to the Motif's name, which is specified by the program). 
            The nickname is initially null. */
        public void setNickname(String nickname) { this.nickname = nickname; }
        /** Sets the nickname, which is a user-settable name for this child (as opposed to the Motif's name, which is specified by the program). 
            The nickname is initially null. */
        public String getNickname() { return nickname; }
        public String getCurrentName() { if (nickname != null && (!nickname.trim().equals(""))) return nickname; else return motif.getName(); }
        /** Returns the custom data object of the Child.  This is set by Motif subclasses to add additional parameters. */
        public Object getData() { return data; }
        /** Sets the custom data object of the Child.  This is set by Motif subclasses to add additional parameters. */
        public void setData(Object obj) { data = obj; }
        
        // Used when loading from JSON
        Child() { }

        public Child(Motif motif, Motif parent) 
            { 
            for(int i = 0; i < parameters.length; i++) parameters[i] = 0.0;  // PARAMETER_UNBOUND; 
            this.motif = motif; 
            data = parent.buildData(motif);
            }


        /** Copies a Child.  Data is copied using copyData(motif, other.data).  */
        public Child(Child other, Motif parent)
            {
            motif = other.motif;
            nickname = other.nickname;
            data = parent.copyData(motif, other.data);
            }
                
        /** Save the child to TO, with an ID for its motif.  It will also attempt to save the motif to MOTIFS,
            unless the motif is already located in SAVEMODITFS.  Override this method in your
            Child subclass, but be sure to call super.save(...) first. */
        protected void save(HashSet<Motif> savedMotifs, JSONArray motifs, JSONObject to, int[] nextID, Motif parent) throws JSONException
            {
            if (!savedMotifs.contains(motif))       // it's not been saved yet
                {
                motif.save(savedMotifs, motifs, nextID);
                savedMotifs.add(motif);
                }
            to.put("motif", motif.id);
            to.put("nick", nickname);                           // note can be null, see load(...)
            to.put("rmin", randomMin);
            to.put("rmax", randomMax);
            JSONArray param = new JSONArray();
            for(int i = 0; i < NUM_PARAMETERS; i++)
                {
                param.put(parent.getParameterName(i));
                }
            to.put("param", param);
            JSONObject d = new JSONObject();
            parent.saveData(data, motif, d);
            to.put("data", d);
            }

        /** Load the child from FROM.  If the Motif is not stored in LOADEDMOTIFS, indexed by id,
            then the motif is first loaded from MOTIFS and then connected.  Override this method in your
            Child subclass, but be sure to call super.load(...) first.  */  
        protected void load(HashMap<Integer, Motif> loadedMotifs, JSONArray motifs, JSONObject from, Motif parent) throws JSONException
            {
            int id = from.getInt("motif");
            motif = loadedMotifs.get(id);
            if (motif == null)
                {
                motif = Motif.load(parent.getSeq(), loadedMotifs, motifs, id);
                loadedMotifs.put(id, motif);
                }
            nickname = from.optString("nick", null);                    // checks for nullity
            randomMin = from.optDouble("rmin", 0.0);
            randomMax = from.optDouble("rmax", 1.0);
            JSONArray param = from.getJSONArray("param");
            for(int i = 0; i < NUM_PARAMETERS; i++)
                {
                // We can't call param.getString() directly because the underlying
                // object could be null.  So we do this:
                if (param.isNull(i))
                    {
                    parent.setParameterName(i, null);
                    }
                else
                    {
                    parent.setParameterName(i, param.getString(i));
                    }
                }
            data = parent.loadData(motif, from.getJSONObject("data"));
            }
            
        public String toString() { return super.toString() + "[" + motif + "]"; }
        }
    
    //// OVERRIDE THESE METHODS AS APPROPRIATE
    
    /** Constructs a new Data object for a Child given the Motif. */
    protected Object buildData(Motif motif) { return null; }
    /** Copies a Data object for a new Child given the Motif. */
    protected Object copyData(Motif motif, Object data) { return null; }
    /** Saves a Data object to TO given the motif. */
    protected void saveData(Object data, Motif motif, JSONObject to) { }
    /** Loads a new Data object from FROM given the motif, and returns it.  */
    protected Object loadData(Motif motif, JSONObject from) { return null; }
        
        
        
    
    /** Represents infinitely long intervals */
    public static final int INFINITY = -2;
    public static final int UNKNOWN = -3;
    // The Seq
    protected Seq seq;
    // The name of the Motif
    String name;
    // The motif's children.  This is an ArrayList of UNIQUE CHILD objects, each of which holds one motif, a nickname, and custom data. 
    ArrayList<Child> children = new ArrayList<>();
    // The motif's parents.  This is an ArrayList of Motifs.  
    ArrayList<Motif> parents = new ArrayList<>();
    // Is this Motif armed for recording?
    boolean armed = false;
    // What is the length of this Motif?  It can be a value 0...(Integer.MAX_VALUE) or INFINITY or UNKNOWN.  It may not be 0.  Set this in during recomputeLength()
    protected int length = 1;
    int id;             // temporary only
    // The current Motif version, so clips can stay in sync when playing or recording
    int version;
    // how many clips are playing right now?
    int playCount;
    // unique ID for motif.  NOT saved out.
    int uniqueID;

    public Motif(Seq seq)
        {
        this.seq = seq;
        name = "";
        uniqueID = getNextUniqueID();
        }

	public Seq getSeq() { return seq; }
    public int getUniqueID() { return uniqueID; }
    public void incrementPlayCount() { playCount++; }
    public void decrementPlayCount() { playCount = Math.max(0, playCount - 1); }
    public int getPlayCount() { return playCount; }
    public void resetPlayCount() { playCount = 0; }
    
    ///// VERSION
    /** Increments the current Motif version so clips can stay in sync.
        Note you don't have to do this for EVERY change you make for Motif,
        only those which will affect a clip's ability to play, such as
        changing the number of children or the number of step sequence tracks. */
    public void incrementVersion() { version++; }
    /** Returns the current Motif version so clips can stay in sync. */
    public int getVersion() { return version; }
    
    ///// NAME
    /** Returns the Motif's name */
    public String getName() { return name; }
    /** Returns the Motif's displayed name, which in rare cases is the name plus some embellishment. */
    public String getDisplayedName() { return getName(); }
    /** Sets the Motif's name */
    public void setName(String val) {if (val == null) val = ""; name = val; }  
          
    /** This is used to return a counter to augment getBaseName() to create a moderately unique
        name from a UI perspective.  Counters are static and per-class.  You should implement 
        this in your Motif subclass as:
        <pre>
        static int counter = 1;
        public int getNextCounter() { return counter++; }
        </pre>
    */
    public abstract int getNextCounter(); 
    /** Returns a basic name that can be displayed in buttons etc. if the name is null or empty.
        For example, MacroChild would return "Macro Child" */ 
    public abstract String getBaseName();
    
    
    static int uniqueIDCounter = 0;
    int getNextUniqueID() { return uniqueIDCounter++; }
        


    ///// ARMING
    /** Returns whether the Motif is armed for recording */
    public boolean isArmed() { return armed; }
    /** Sets whether the Motif is armed for recording */
    public void setArmed(boolean val) { armed = val; }
        
    /// FIXME: at present we're just gonna assume that this collapses the recordings to the last one,
    /// But we should change it to lazily let the user choose.
    public void endArmed() 
        { 
        for(Child child : children)
            child.motif.endArmed();
        }
        
    ///// PARAMETER NAMES

    public static final int NUM_PARAMETERS = 8;
    String[] parameterNames = new String[NUM_PARAMETERS];
    public String getParameterName(int param) { return parameterNames[param]; }
    public void setParameterName(int param, String val) { parameterNames[param] = val; }

    ///// TEXTUAL NOTES
    String text = null;
    public String getText() { return text; }
    public void setText(String val) { text = val; }

    ///// RANDOM NUMBER GENERATION
                
    /** This method is only called if the Motif is the root. */
    /// FIXME -- right now we're assuming repeatability at the top level.  
    public double generateRandomValue() { return generateRandomValue(0.0, 1.0); }  
        
    public static final int RANDOM_TRIES = 4;

    // Generates a random value 0...1 INCLUSIVE
    double generateUniformRandom()
        {
        Random random = seq.getDeterministicRandom();
        for(int i = 0; i < RANDOM_TRIES; i++)
            {
            double d = random.nextDouble() * 2.0;
            if (d <= 1.0) return d;
            }
        return random.nextDouble();
        }

    double generateRandomValue(double min, double max)
        {
        if (min == max) return min;
                
        if (min == 0.0 && max == 1.0) return generateUniformRandom();

        // Doing a rectangular distribution for the time being
        if (min < max)
            {
            return generateUniformRandom() * (max - min) + min;
            }
        else
            {
            return generateUniformRandom() * (min - max) + max;
            }
        }
        

    ///// LENGTH INFORMATION
    ///// Motifs, for the time being, can have a known length.

    /** Returns the length, which is a value 1...(Integer.MAX_VALUE) or INFINITY or UNKNOWN.  It may not be 0.  */
    public int getLength() { return length; }
    
    /** Recompute the length. */
    public abstract void recomputeLength();             // set the length variable
                    
    /// Any clip which is currently playing the Motif.  There could be multiple such clips,
    /// but only one will be stored here.
    volatile Clip playingClip = null;
    
    /** Sets the current playing Clip, if any, or null.   This method is intended
        only for the GUI and so you the GUI does not need a lock for it. */
    public boolean setPlayingClip(Clip clip) 
        { 
        if (playingClip == null || playingClip == clip) 
            { 
            playingClip = clip; 
            return true;
            }
        else 
            {
            //System.err.println("Set FAILED\n\tMotif\t" + this + "\n\tHas\t" + playingClip + "\n\tNot\t" + clip);
            return false;
            }
        }
    /** Removes the current playing clip if it matches the given clip.  This method is intended
        only for the GUI and so you the GUI does not need a lock for it. */
    public boolean removePlayingClip(Clip clip) 
        { 
        if (playingClip == clip) 
            {
            playingClip = null; 
            return true;
            }
        else 
            {
            //System.err.println("Remove FAILED\n\tMotif\t" + this + "\n\tHas\t" + playingClip + "\n\tNot\t" + clip);
            return false;
            }
        }
    /** Removes the current playing Clip, setting it to null.  */ 
    public void removePlayingClip() { playingClip = null; }
    /** Returns the current playing Clip, if any, or null.  
        Rebuilds the Clip if necessary.   */ 
    public Clip getPlayingClip() 
        { 
        if (playingClip != null && playingClip.getVersion() < version) 
            {
            playingClip.rebuild();
            }
        return playingClip; 
        }


    ///// COPYING
        
    /** Light copies the Motif*/
    @SuppressWarnings("unchecked")              // other.parents = (ArrayList<Motif>)(other.parents.clone());
    public Motif copy()
        {
        Motif other = null;
        try { other = (Motif)(super.clone()); }
        catch (CloneNotSupportedException ex) { }
        other.children = new ArrayList<Child>();
        for(Child child : children)                                     // light-copy the children
            other.children.add(new Child(child, other));
        other.parents = new ArrayList<Motif>();         // don't copy over the parents
        other.parameterNames = copy(parameterNames);
        return other;
        }

    /** Copies the entire DAG of descendants. */
    public Motif copyDAG()
        {
        return copyDAG(new HashMap<Motif, Motif>());
        }
        
    /** Copies the entire DAG of descendants given the provided old->new map. */
    public Motif copyDAG(HashMap<Motif, Motif> existing)
        {
        Motif e = existing.get(this);
        if (e != null) return e;                // all done

        e = copy();
        existing.put(this, e);
        
        // we have light-copied the children in copy(), but we need to modify them to hold onto
        // the new motif copies, and also add me as the motif's parents
        for(Child child : e.getChildren())
            {
            Motif newMotif = child.getMotif().copyDAG(existing);
            child.setMotif(newMotif);
            newMotif.addParent(e);
            }

        return e;
        }



    ///// DAG QUERIES

    /** Returns all descendants of the Motif */
    public ArrayList<Motif> getDescendants()
        {
        ArrayList<Motif> list = new ArrayList<>();
        getDescendants(new HashSet<Motif>(), list);
        return list;
        }

    void getDescendants(HashSet<Motif> existing, ArrayList<Motif> putInHere)
        {
        for(Child child : children)
            {
            if (existing.contains(child.motif)) continue;
            existing.add(child.motif);
            putInHere.add(child.motif);
            child.motif.getDescendants(existing, putInHere);
            }
        }

    /** Returns all ancestors of the Motif */
    public ArrayList<Motif> getAncestors()
        {
        ArrayList<Motif> list = new ArrayList<>();
        getAncestors(new HashSet<Motif>(), list);
        return list;
        }

    void getAncestors(HashSet<Motif> existing, ArrayList<Motif> putInHere)
        {
        for(Motif parent : parents)
            {
            if (existing.contains(parent)) return;
            existing.add(parent);
            putInHere.add(parent);
            parent.getAncestors(existing, putInHere);
            }
        }

    /** Returns the eldest ancestors of the Motif */
    public ArrayList<Motif> getEldest()
        {
        ArrayList<Motif> list = new ArrayList<>();
        getEldest(new HashSet<Motif>(), list);
        return list;
        }

    void getEldest(HashSet<Motif> existing, ArrayList<Motif> putInHere)
        {
        for(Motif parent : parents)
            {
            if (existing.contains(parent)) return;
            existing.add(parent);
            if (parent.parents.isEmpty()) putInHere.add(parent);
            else parent.getAncestors(existing, putInHere);
            }
        }



    /** Returns true if the provided Motif is equal to or among the descendants of this Motif. */
    public boolean containsOrIs(Motif descendant)
        {
        return (this == descendant || contains(descendant));
        }
                
    /** Returns true if the provided Motif is among the descendants of this Motif. */
    public boolean contains(Motif descendant)
        {
        return contains(descendant, new HashSet<Motif>());
        }
                
    boolean contains(Motif descendant, HashSet<Motif> checked)
        {
        for(Child child : children)
            {
            if (!checked.contains(child.motif))
                {
                checked.add(child.motif);
                if (child.motif == descendant) return true;
                else if (child.motif.contains(descendant, checked)) return true;
                }
            }
        return false;
        }

    static void topologicalSort(Motif motif, ArrayList<Motif> stack, HashSet<Motif> visited)
        {
        visited.add(motif);
        for(Child child : motif.getChildren())
            {
            if (!visited.contains(child.motif))
                {
                topologicalSort(child.motif, stack, visited);
                }
            }
        stack.add(motif);
        }

    /** Given a list of Motifs, sorts them topologically, returning the result.  A topological sort is one which guarantees that A is before B in the list 
        if B is a descendant of A. Note that this is not a total ordering. */
    public static ArrayList<Motif> topologicalSort(ArrayList<Motif> motifs)
        {
        HashSet<Motif> visited = new HashSet<>();
        ArrayList<Motif> stack = new ArrayList<>();
        for(Motif motif : motifs)
            {
            if (!visited.contains(motif))
                {
                topologicalSort(motif, stack, visited);
                }
            }
        Collections.reverse(stack);
        return stack;
        }




    ///// RELATIONSHIP WITH CLIPS
        
    /** Builds and returns a new Clip of an appropriate class that points to to this Motif. 
        Calls Clip.rebuildClip() */
    public abstract Clip buildClip(Clip parent);




    ///// RELATIONSHIP WITH CHILDREN

    /** Returns the children list. */
    public ArrayList<Child> getChildren() { return children; }

    public ArrayList<Motif> getChildrenAsMotifs() 
        {
        ArrayList<Motif> done = new ArrayList<>();
        HashSet<Motif> kids = new HashSet<>();
        for(Child child : getChildren())
            {
            if (!kids.contains(child.getMotif()))
                done.add(child.getMotif());
            }
        return done;
        }
    
    public Child getChild(int index)
        {
        return children.get(index);
        }
        
    /** Returns true if the children list contains the given motif.  O(n). */
    public boolean containsChild(Motif motif)
        {
        for(Child child : children)
            {
            if (child.motif == motif) return true;
            }
        return false;
        }

    /** Adds a child.  Marks the Motif as dirty so it must update its lengths etc.
        Returns the generated Child object. */
    public Child addChild(Motif motif) 
        { 
        Child child = new Child(motif, this);
        addChild(child);
        return child;
        }

    /** Adds a child.  Marks the underlying Motif as dirty so it must update its lengths etc.
        Returns the generated Child object. */
    public void addChild(Child child) 
        { 
        children.add(child); 
        // Add me as a parent
        child.getMotif().addParent(this);
        recomputeLength();
        recomputeAncestorLengths(); 
        incrementVersion();
        }
        
    /** Replaces the child with a new one at the given position.  Marks the Motif as dirty so it must update its lengths etc.
        Returns the OLD Child object. */
    public Child replaceChild(Motif motif, int at) 
        { 
        Child oldChild = children.get(at);
        children.set(at, new Child(motif, this));
        if (!containsChild(oldChild.getMotif()))   // (O(n) :-()  If I don't have more pointers to the child
            {
            // Remove me as a parent
            oldChild.getMotif().removeParent(this);
            }

        // Add me as a parent
        motif.addParent(this);
        recomputeLength();
        recomputeAncestorLengths(); 
        incrementVersion();
        return oldChild;
        }
        
    /** Copies the child.  Marks the Motif as dirty so it must update its lengths etc.
        Returns the generated Child object, or null if it could not set at the given position (because
        there was nothing to replace typically). */
    public Child copyChild(int from, int to) 
        { 
        Child oldChild = children.get(to);
        Child child = new Child(children.get(from), this);
        children.set(to, child);
        recomputeLength();
        recomputeAncestorLengths(); 
        incrementVersion();
        return oldChild;
        }
        
    /** Moves a child.  If toBefore == getChildren().size(), then the child is moved
        to the end of the children  Marks the Motif as dirty so it must update its lengths etc. 
        Returns the removed Child object.   O(n). */
    public void moveChild(int from, int toBefore)
        {
        if (from == toBefore || from == toBefore - 1) return;       // no effect
        Child child = children.remove(from);
        if (from < toBefore) 
            toBefore--;       // it was shifted down
        children.add(toBefore, child);
        recomputeLength();
        incrementVersion();
        recomputeAncestorLengths(); 
        }

    /** Swaps two children.  Marks the Motif as dirty so it must update its lengths etc. 
        Returns the removed Child object.   O(n). */
    public void swapChild(int from, int to)
        {
        if (from == to) return;      // no effect
        Child _from = children.get(from);
        Child _to = children.get(to);
        children.set(to, _from);
        children.set(from, _to);
        recomputeLength();
        incrementVersion();
        recomputeAncestorLengths(); 
        }

    /** Removes a child.  Marks the Motif as dirty so it must update its lengths etc. 
        Returns the removed Child object.   O(n). */
    public Child removeChild(int index)
        {
        Child child = children.remove(index);
        if (!containsChild(child.getMotif()))   // (O(n) :-()  If I don't have more pointers to the child
            {
            // Remove me as a parent
            child.getMotif().removeParent(this);
            }
        recomputeLength();
        incrementVersion();
        recomputeAncestorLengths(); 
        return child; 
        }
                        
    /** Removes a child or null if there is nonel
        Marks the Motif as dirty so it must update its lengths etc.
        Returns the removed Child object.   O(n).  */
    public Child removeChild(Child child) 
        {
        int val = children.indexOf(child);
        if (val < 0) return null;
        return removeChild(val);
        }

    /** Removes a child everywhere it appears in its parent.   O(n^2).  */
    public void removeChild(Motif child) 
        {
        while (true)
            {
            boolean removed = false;
            for(Child c : children)
                {
                if (c.motif == child)
                    {
                    removeChild(c);         // Yeah, yeah, O(n) again...
                    removed = true;
                    break;
                    }
                }
            if (!removed) return;
            }
        }


    ///// RELATIONSHIP WITH PARENTS
    ///// Note: while the children arraylist may contain a child multiple times,
    ///// the parent arraylist may contain a parent only once (it's a set).
    ///// Parents are added and removed via removeChild, not by calling the add/remove 
    ///// methods below directly.

    /** Returns the parent list. */
    public ArrayList<Motif> getParents() { return parents; }
    
    // Adds a Parent.  This is not public because it's only called by addChild()
    void addParent(Motif motif) 
        { 
        if (!parents.contains(motif)) 
            {
            parents.add(motif); 
            }
        }
        
    // Removes a Parent.  This is not public because it's only called by removeChild()
    void removeParent(Motif parent)  
        { 
        parents.remove(parent);
        }
        
    // Informs all parents that they may need to update their lengths 
    public void recomputeAncestorLengths() 
        {
        // Get parents
        ArrayList<Motif> ancestors = getAncestors();
        // Sort
        ancestors = topologicalSort(ancestors);
        // Reverse -- do children first
        Collections.reverse(ancestors);
        // recompute lengths bottom up
        for(Motif ancestor : ancestors)
            ancestor.recomputeLength();
        }
        
        
    /** Removes a motif everywhere it appears in its parent and children, in preparation for deleting it. */
    public void disconnect() 
        {
        // Copy the parents list.  We're going through the parents and deleting me as their child,
        // and that causes them to ask me to remove them as my parent, which changes my parents list.
        // So just in case...
        ArrayList<Motif> pn = ((ArrayList<Motif>)(getParents().clone()));
        for(Motif parent : pn)
            {
            parent.removeChild(this);
            }
                
        // Now we remove all my children.  Same trick.
        ArrayList<Child> cn = ((ArrayList<Child>)(children.clone()));
        for(Child child : cn)
            {
            removeChild(child);
            }
        }


        
    ///// UTILITY
    ///// This is mostly copying code.  See the Step Sequencer for heavy use of this code.

    /** Copies an int[][] array exactly. */
    public static int[][] copy(int[][] array) 
        {
        if (array == null) return null;
        int[][] copy = (int[][])(array.clone());
        for(int i = 0; i < copy.length; i++)
            {
            if (array[i] != null)
                {
                copy[i] = (int[])(array[i].clone());
                }
            }
        return copy;
        }

    /** Copies a double[][] array exactly. */
    public static double[][] copy(double[][] array) 
        {
        if (array == null) return null;
        double[][] copy = (double[][])(array.clone());
        for(int i = 0; i < copy.length; i++)
            {
            if (array[i] != null)
                {
                copy[i] = (double[])(array[i].clone());
                }
            }
        return copy;
        }

    /** Copies a boolean[][] array exactly. */
    public static boolean[][] copy(boolean[][] array) 
        {
        if (array == null) return null;
        boolean[][] copy = (boolean[][])(array.clone());
        for(int i = 0; i < copy.length; i++)
            {
            if (array[i] != null)
                {
                copy[i] = (boolean[])(array[i].clone());
                }
            }
        return copy;
        }

    /** Copies an int[] array exactly. */
    public static int[] copy(int[] array) 
        {
        if (array == null) return null;
        else return (int[])(array.clone());
        }

    /** Copies a double[] array exactly. */
    public static double[] copy(double[] array) 
        {
        if (array == null) return null;
        else return (double[])(array.clone());
        }

    /** Copies a String[] array exactly. */
    public static String[] copy(String[] array) 
        {
        if (array == null) return null;
        else return (String[])(array.clone());
        }

    /** Copies a boolean[] array exactly. */
    public static boolean[] copy(boolean[] array) 
        {
        if (array == null) return null;
        else return (boolean[])(array.clone());
        }


    /////// JSON SERIALIZATION
    
    /** Saves the Motif and the DAG of all descendants.  The root is Motif is 0. */
    public JSONArray saveRoot() throws JSONException
        {
        JSONArray motifs = new JSONArray();
        save(new HashSet<Motif>(), motifs, new int[] { 0 });
        return motifs;
        } 

    /** Saves additional data for the the Motif into MOTIFS.  
        Override this if you do not override save(HashSet, JSONArray, int[]). */
    public void save(JSONObject to) throws JSONException { }

    /** Saves the Motif into MOTIFS keyed with integer ID drawn from NEXTID.  Assumes that the Motif
        has not already been saved (you can pre-check for this by looking for the object in SAVEDMOTIFS
        before calling this method).  Override this to save additional information to the provided
        JSONObject from your Motif subclass. */
    public JSONObject save(HashSet<Motif> savedMotifs, JSONArray motifs, int[] nextID) throws JSONException
        {
        if (savedMotifs.contains(this))       // already saved
            {
            throw new RuntimeException("Motif already saved: " + this);
            }

        id = nextID[0]++;

        // Build our JSONObject
        JSONObject obj = new JSONObject();
        
        // class
        obj.put("type", this.getClass().getCanonicalName());

        // name
        obj.put("name", getName());
        
        // text
        obj.put("text", getText());
        
        // params
        /*
          JSONArray paramArray = new JSONArray();
          for(int i = 0; i < defaultValues.length; i++)
          {
          paramArray.put(defaultValues[i]);
          }
          obj.put("def", paramArray);
        */

        // store with id
        motifs.put(obj);
        savedMotifs.add(this);
        
        // Save more data
        save(obj);

        // process all children that haven't been processed by someone else yet
        JSONArray childArray = new JSONArray();
        for(Child child : children)
            {
            JSONObject c = new JSONObject();
            child.save(savedMotifs, motifs, c, nextID, this);
            childArray.put(c);
            }

        obj.put("children", childArray);

        return obj;     
        }
        
    /** Loads all Motifs in the DAG, in order with root as first, from the given JSONObject. 
        Returns the non-BLANK motifs. If loadAll is FALSE, then only the rooted DAG is loaded. */
    public static ArrayList<Motif> load(Seq seq, JSONArray motifs, boolean loadAll) throws JSONException
        {
        HashMap<Integer, Motif> loaded = new HashMap<>();
        load(seq, loaded, motifs, 0);
    
        if (loadAll)
            {
            // Now load remainder
            for(int i = 1; i < motifs.length(); i++)
                {
                if (loaded.get(i) == null)  // not loaded yet
                    {
                    load(seq, loaded, motifs, i);
                    }
                }
            }
        
        ArrayList<Motif> result = new ArrayList<>();
        for(Integer i : loaded.keySet())
            {
            if (!(loaded.get(i) instanceof seq.motif.blank.Blank))
                {
                result.add(loaded.get(i));
                }
            }
        return result;
        }

    /** Loads all Motifs in the ROOTED DAG ONLY, in order with root as first, from the given JSONObject. 
        Returns the non-BLANK motifs. */
    public static ArrayList<Motif> load(Seq seq, JSONArray motifs) throws JSONException
        {
        return load(seq, motifs, false);
        }
        
    /** Loads a Motif from slot ID in MOTIFS, and adds it to LOADEDMOTIFS.  Returns the Motif.   */
    static Motif load(Seq seq, HashMap<Integer, Motif> loadedMotifs, JSONArray motifs, int id) throws JSONException
        {
        JSONObject obj = motifs.getJSONObject(id);
        Motif motif = null;
        try
            {
            motif = (Motif)(Class.forName(obj.getString("type")).getConstructor(Seq.class).newInstance(seq));
            }
        catch (Exception ex)            // probably missing
            {
            System.err.println("Motif.load(): Error loading object id " + obj.getInt("id") + " of broken class " + obj.getString("type") + ":\n" + ex);
            motif = new seq.motif.blank.Blank(seq);
            ((seq.motif.blank.Blank)motif).setWasToClass(obj.getString("type"));
            }
        loadedMotifs.put(id, motif);
        motif.load(loadedMotifs, motifs, obj);
        return motif;
        }

    /** Loads additional data for a Motif from FROM.  Override this to load additional information in your
        Motif subclass, if you do not override load(HashMap, JSONArray, JSONObject) */  
    public void load(JSONObject from) throws JSONException { }

    /** Loads a Motif from FROM and returns it.  Override this to load additional information in your
        Motif subclass (be sure to call super.load(...))  */  
    protected void load(HashMap<Integer, Motif> loadedMotifs, JSONArray motifs, JSONObject from) throws JSONException
        {
        parents = new ArrayList<Motif>();
        children = new ArrayList<Child>();

        // load children
        JSONArray childArray = from.getJSONArray("children");
        for(int i = 0; i < childArray.length(); i++)
            {
            Child child = new Child(); 
            child.load(loadedMotifs, motifs, childArray.getJSONObject(i), this);
            addChild(child);
            }
        
        // name
        name = from.optString("name", "");
        
        // text
        text = from.optString("text", "");

        load(from);
        }
        
    //// JSON SERIALIZATION UTILITY CODE
    
    /** Converts an int[] to a JSONArray */
    public static JSONArray intToJSONArray(int[] vals)
        {
        JSONArray array = new JSONArray();
        for(int i = 0; i < vals.length; i++)
            array.put(vals[i]);
        return array;
        }

    /** Converts a double[] to a JSONArray */
    public static JSONArray doubleToJSONArray(double[] vals)
        {
        JSONArray array = new JSONArray();
        for(int i = 0; i < vals.length; i++)
            array.put(vals[i]);
        return array;
        }

    /** Converts an int[][] to a JSONArray */
    public static JSONArray intToJSONArray2(int[][] vals)
        {
        JSONArray array = new JSONArray();
        for(int i = 0; i < vals.length; i++)
            array.put(intToJSONArray(vals[i]));
        return array;
        }

    /** Converts a boolean[] to a JSONArray */
    public static JSONArray booleanToJSONArray(boolean[] vals)
        {
        JSONArray array = new JSONArray();
        for(int i = 0; i < vals.length; i++)
            array.put(vals[i]);
        return array;
        }

    /** Converts a boolean[][] to a JSONArray */
    public static JSONArray booleanToJSONArray2(boolean[][] vals)
        {
        JSONArray array = new JSONArray();
        for(int i = 0; i < vals.length; i++)
            array.put(booleanToJSONArray(vals[i]));
        return array;
        }

    /** Converts a String[] to a JSONArray */
    public static JSONArray stringToJSONArray(String[] vals)
        {
        JSONArray array = new JSONArray();
        for(int i = 0; i < vals.length; i++)
            array.put(vals[i]);
        return array;
        }

    /** Converts a JSONArray to an int[] */
    public static int[] JSONToIntArray(JSONArray array)
        {
        int[] vals = new int[array.length()];
        for(int i = 0; i < vals.length; i++)
            vals[i] = array.getInt(i);
        return vals;
        }

    /** Converts a JSONArray to an int[][] */
    public static int[][] JSONToIntArray2(JSONArray array)
        {
        int[][] vals = new int[array.length()][];
        for(int i = 0; i < vals.length; i++)
            vals[i] = JSONToIntArray(array.getJSONArray(i));
        return vals;
        }

    /** Converts a JSONArray to a boolean[] */
    public static boolean[] JSONToBooleanArray(JSONArray array)
        {
        boolean[] vals = new boolean[array.length()];
        for(int i = 0; i < vals.length; i++)
            vals[i] = array.getBoolean(i);
        return vals;
        }

    /** Converts a JSONArray to a boolean[][] */
    public static boolean[][] JSONToBooleanArray2(JSONArray array)
        {
        boolean[][] vals = new boolean[array.length()][];
        for(int i = 0; i < vals.length; i++)
            vals[i] = JSONToBooleanArray(array.getJSONArray(i));
        return vals;
        }

    /** Converts a JSONArray to a double[] */
    public static double[] JSONToDoubleArray(JSONArray array)
        {
        double[] vals = new double[array.length()];
        for(int i = 0; i < vals.length; i++)
            vals[i] = array.getDouble(i);
        return vals;
        }

    /** Converts a JSONArray to a String[] */
    public static String[] JSONToStringArray(JSONArray array)
        {
        String[] vals = new String[array.length()];
        for(int i = 0; i < vals.length; i++)
            vals[i] = array.getString(i);
        return vals;
        }
        
    }
