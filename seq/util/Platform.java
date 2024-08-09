package seq.util;

public class Platform
    {
    private static final long serialVersionUID = 1;

    /////// OS DISTINGUISHING PROCEDURES

    private static String OS() { return System.getProperty("os.name").toLowerCase(); }

    public static boolean isWindows() 
        {
        return (OS().indexOf("win") >= 0);
        }

    public static boolean isMac() 
        {
        return (OS().indexOf("mac") >= 0 || System.getProperty("mrj.version") != null);
        }
        
    public static boolean nimbus = false;
    public static boolean isNimbus() 
        {
        return nimbus;
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
        return (OS().indexOf("nix") >= 0 || OS().indexOf("nux") >= 0 || OS().indexOf("aix") > 0 );
        }

    // From https://stackoverflow.com/questions/12431148/swing-and-bitmaps-on-retina-displays
    /*
      public static boolean isRetinaDisplay() 
      {
      GraphicsDevice graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
      try {
      java.lang.reflect.Field field = graphicsDevice.getClass().getDeclaredField("scale");
      if (field != null) 
      {
      field.setAccessible(true);
      Object scale = field.get(graphicsDevice);
      if(scale instanceof Integer && ((Integer) scale).intValue() == 2) 
      {
      return true;
      }
      }
      } 
      catch (Exception e) { System.err.println("Couldn't get Retina Display information\n" + e); }
      return false;
      }
    */

    }
