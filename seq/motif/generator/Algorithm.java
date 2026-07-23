/* 
   Copyright 2026 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.generator;

import seq.motif.generator.gui.*;
import seq.engine.*;
import seq.motif.blank.*;
import seq.util.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;
import java.io.*;

/** An Algorithm is a procedure for generating music, either using an existing music source
    or on its own. Algorithm subclasses classes, and the names that they should appear with 
    in the menu, appear in the file "algorithms.txt" (ALGORITHM_CLASSES).  
        
    The default Algorithm is blank.  There is one legitimate blank Algorithm, called "None",
    which emits nothing at all and has no features.  When there is an error in loading an
    Algorithm, a blank default Algorithm is also loaded instead, but its Valid flag is set
    to FALSE.
*/
        
        
        
public class Algorithm implements Cloneable
    {
    private static final long serialVersionUID = 1;

    /** The file from which Algorithms are loaded. */
    public static final String ALGORITHM_CLASSES = "alg/algorithms.txt";

    // Classnames of Algorithms in order
    static String[] algorithmClassNames;
    // Text (menu) names of Algorithms in order
    static String[] algorithmNames;
    // Backpointer to the generator that owns the Algorithm
    public Generator generator;
    // Is the Algorithm a valid Algorithm, or is it a stub produced because of an error?
    boolean valid = false;
        
        
    static
        {
        // Load all algorithms and algorithm names
        try
            {
            LineNumberReader reader = new LineNumberReader(new BufferedReader(new InputStreamReader(Algorithm.class.getResourceAsStream(ALGORITHM_CLASSES))));
            ArrayList<String> algs = new ArrayList<String>();
            ArrayList<String> names = new ArrayList<String>();
                        
            while(true)
                {
                String line = reader.readLine();
                if (line == null) break;
                line = line.trim();
                if (line.length() > 0 && !line.startsWith("#"))         // we allow comments
                    {
                    String[] split = line.split("\t");
                    if (split.length == 2)
                        {
                        algs.add(split[0]);
                        names.add(split[1]);
                        }
                    else
                        {
                        System.err.println("Algorithm.static ERROR: bad line " + line);
                        }
                    }
                }
            algorithmClassNames = algs.toArray(new String[0]);
            algorithmNames = names.toArray(new String[0]);
            reader.close();
            }
        catch (IOException ex)
            {
            // At least include "None"
            algorithmClassNames = new String[] { "seq.motif.generator.Algorithm" };
            algorithmNames = new String[] { "None" };
            }
        }
        
    /** Finds the given classname and returns which index it is, or returns -1. */
    public static int findAlgorithmClassName(String name)
        {
        for(int i = 0; i < algorithmClassNames.length; i++)
            {
            if (name.equals(algorithmClassNames[i])) return i;
            }
        return -1;
        }
                
    /** Returns all the algorithm text names suitable to add to a menu. */
    public static String[] getAlgorithmNames()
        {
        return algorithmNames;
        }
                
    /** Is this algorithm valid? */
    public boolean isValid() { return valid; }

    /** Returns the generator backpointer */
    public Generator getGenerator() { return generator; }
        
    /** Copies the Algorithm, returning a new one.  You should override this to clone, but be sure to call super.copy() */
    public Algorithm copy()
        {
        Algorithm other = null;
        try { other = (Algorithm)(this.clone()); }
        catch (CloneNotSupportedException ex) { }
        return other;
        }
        
    /** Produces a new version of Algorithm #algNum, or a stub if there is an error.  */
    public static Algorithm instantiate(Generator generator, int algNum)
        {
        if (algNum < 0 || algNum >= algorithmClassNames.length)
            {
            System.err.println("Algorithm.instantiate(algNum) ERROR: exception thrown when loading alg number " + algNum + ", which is out of bounds.");
            return new Algorithm(generator);
            }

        try
            {
            return (Algorithm)(Class.forName(algorithmClassNames[algNum]).getConstructor(Generator.class).newInstance(generator));
            }
        catch (Exception ex)
            {
            System.err.println("Algorithm.instantiate(algNum) ERROR: exception thrown when loading alg number " + algNum + ", which is \"" + algorithmClassNames[algNum] + "\"" );
            ex.printStackTrace();
            return new Algorithm(generator);
            }
        }
        
    /** Loads an Algorithm from the JSONObject and returns it, or a stub if there is an error.  */
    public static Algorithm load(Generator generator, JSONObject obj) throws JSONException
        {
        try
            {
            String shortName = obj.optString("class", null);
            if (shortName == null) 
                {
                System.err.println("Algorithm.load(JSONObject) ERROR: no \"class\" key in JSONObject.");
                return null;
                }
            else 
                {
                String classname = "seq.motif.generator." + shortName;
                if (findAlgorithmClassName(classname) >= 0)
                    {
                    Algorithm alg = (Algorithm)(Class.forName(classname).getConstructor(Generator.class, JSONObject.class).newInstance(generator, obj));
                    alg.valid = true;
                    return alg;
                    }
                else
                    {
                    System.err.println("Algorithm.load(JSONObject) ERROR: no such algorithm called \"" + classname + "\" known.");
                    return new Algorithm(generator);
                    }
                }
            }
        catch (Exception ex)
            {
            System.err.println("Algorithm.load(JSONObject) ERROR: exception thrown when loading:");
            ex.printStackTrace();
            return new Algorithm(generator);
            }
        }
                
    /** Creates a new Algorithm.  You should override this, but remember to call super(generator)  */
    public Algorithm(Generator generator)
        {
        this.generator = generator;
        }
        
    /** Creates a new Algorithm loaded from the given JSONObject.  You should override this, but remember to call super(generator, obj) */
    public Algorithm(Generator generator, JSONObject obj) throws JSONException
        {
        this.generator = generator;
        }
        
    /** Writes the Algorithm to the the given JSONObject.  You should override this, but remember to call super.save(...) */
    public void save(JSONObject obj) throws JSONException
        {
        obj.put("class", getClass().getSimpleName());
        }

    /** Returns a new AlgorithmNode associated with this Algorithm.  You should override this to return the appropriate AlgorithmNode subclass.  Don't call super.buildNode(...) */
    public AlgorithmNode buildNode(Seq seq, GeneratorClip clip)
        {
        return new AlgorithmNode(seq, generator, clip, this);
        }
                
    /** Returns a new AlgorithmUI associated with this Algorithm.  You should override this to return the appropriate AlgorithmUI subclass.  Don't call super.buildUI(...) */
    public AlgorithmUI buildUI(Seq seq, GeneratorUI ui)
        {
        return new AlgorithmUI(seq, generator, ui, this);
        } 

    /** Returns as a String the text of the file "index.html" located next to the class file of this object.
        You could call this to get a String to return for getHTMLDescription() instead of providing a String directly. */
    protected String getIndexHTMLFileText() 
        { 
        return new Scanner(getClass().getResourceAsStream("index.html")).useDelimiter("\\Z").next(); 
        }
                
    /** Return, as an HTML String, a description of this Algorithm. */
    public String getHTMLDescription() { return "<html><h1>None</h1><p>Select an Algorithm at right.</html>"; }
    }
        
        
        
        
        
        
