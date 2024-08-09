package seq.util;
import java.util.*;


/** Stuff ArrayList and Collections should have */

public class ArrayLists
    {
    @SuppressWarnings("unchecked")
    public static boolean removeFast(ArrayList list, Object obj)            // O(n) search, O(1) remove
        {
        int index = list.indexOf(obj);
        if (index >= 0) 
            {
            removeFast(list, index);
            return true;
            }
        return false;
        }
                
    @SuppressWarnings("unchecked")
    public static void removeFast(ArrayList list, int pos)                  // O(1)
        {
        int size = list.size();
        if (pos < 0 || pos >= size)
            throw new IndexOutOfBoundsException("Value " + pos + " is outside bounds of array " + list + " of size " + size);
        if (pos != size - 1)
            {
            // Swap in top
            list.set(pos, list.get(size - 1));
            }
        list.remove(size - 1);
        }
    }
