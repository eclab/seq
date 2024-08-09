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
