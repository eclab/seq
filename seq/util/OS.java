/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.util;

public class OS
    {
    /////// OS DISTINGUISHING PROCEDURES

    private static String getOS() { return System.getProperty("os.name").toLowerCase(); }

    public static boolean isWindows() 
        {
        return (getOS().indexOf("win") >= 0);
        }

    public static boolean isMac() 
        {
        return (getOS().indexOf("mac") >= 0 || System.getProperty("mrj.version") != null);
        }
        
    public static boolean isMacOSMonterey()
        {
        return System.getProperty("os.name").equals("Mac OS X") &&
            (System.getProperty("os.version").startsWith("12.") ||
            System.getProperty("os.version").equals("10.16"));              // Java 8 reports Monterey as 10.16  :-(
        }

    public static boolean isMacOSBigSur()
        {
        return System.getProperty("os.name").equals("Mac OS X") &&
            ((System.getProperty("os.version").startsWith("11.") ||
                System.getProperty("os.version").equals("10.16")));             // Java 8 reports Big Sur as 10.16  :-(
        }

    public static boolean isMacOSVentura()
        {
        System.err.println(System.getProperty("os.name"));
        System.err.println(System.getProperty("os.version"));
        return System.getProperty("os.name").equals("Mac OS X") &&
            (System.getProperty("os.version").startsWith("13."));
        }

    public static boolean isUnix() 
        {
        return (getOS().indexOf("nix") >= 0 || getOS().indexOf("nux") >= 0 || getOS().indexOf("aix") > 0 );
        }
    }
