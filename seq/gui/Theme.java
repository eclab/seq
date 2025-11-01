/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.gui;
import seq.util.*;

public class Theme
    {
    static boolean dark;
        
    static
        {
        dark = Prefs.getLastBoolean("Theme.dark", false);
        }
                
    public static boolean isDark()
        {
        // You could override this to just return TRUE 
        return dark;
        }
                
    public static void setDark(boolean val)
        {
        dark = val;
        Prefs.setLastBoolean("Theme.dark", val);
        }
    }
