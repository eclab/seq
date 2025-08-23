/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.engine;

import java.util.*;

public class Undo<T>
    {
    ArrayList<T> undoStack = new ArrayList<T>();
    ArrayList<T> redoStack = new ArrayList<T>();
        
    public void clear()
        {
        undoStack.clear();
        redoStack.clear();
        }
                
    public void push(T current)
        {
        redoStack.clear();
        undoStack.add(current);
        }
          
    public T onUndo()
    	{
    	return undoStack.get(undoStack.size() - 1);
    	}

    public T onRedo()
    	{
    	return redoStack.get(redoStack.size() - 1);
    	}
    	      
    public T undo(T current)
        {
        if (canUndo())
            {
            redoStack.add(current);
            return undoStack.remove(undoStack.size() - 1);
            }
        else return null;       
        }
                
    public T redo(T current)
        {
        if (canRedo())
            {
            undoStack.add(current);
            return redoStack.remove(redoStack.size() - 1);
            }
        else return null;
        }
                
    public boolean canUndo()
        {
        return undoStack.size() > 0;
        }
                
    public boolean canRedo()
        {
        return redoStack.size() > 0;
        }
    }
