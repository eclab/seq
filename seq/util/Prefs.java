// Copyright 2017 by Sean Luke
// Licensed under the Apache 2.0 License

package seq.util;

import java.util.prefs.*;

/**
 * A simple cover class for Java's preferences system.
 */

public class Prefs 
    {
    private static final long serialVersionUID = 1;

    static final String GLOBAL_PREFERENCES = "edu.gmu.seq/global";
    static final String EDITOR_PREFERENCES = "edu.gmu.seq/module";

    /**
     * Returns the preferences object associated with global preferences for the
     * application.
     */
    public static Preferences getGlobalPreferences(String namespace) 
        {
        return Preferences.userRoot().node(GLOBAL_PREFERENCES + "/" + namespace.replace('.', '/'));
        }

    /**
     * Returns the preferences object associated with per-module preferences for the
     * application.
     */
    public static Preferences getAppPreferences(String module, String namespace) 
        {
        return Preferences.userRoot().node(EDITOR_PREFERENCES + "/" + module.replace('.', '/') + "/" + namespace.replace('.', '/'));
        }

    /** Removes global preferences. */
    public static boolean removeGlobalPreferences(String namespace) 
        {
        try
            {
            getGlobalPreferences(namespace).removeNode();
            return true;
            } 
        catch (Exception ex) 
            {
            ex.printStackTrace();
            return false;
            }
        }

    /** Removes per-module preferences. */
    public static boolean removeAppPreferences(String module, String namespace)
        {
        try
            {
            getAppPreferences(module, namespace).removeNode();
            return true;
            }
        catch (Exception ex)
            {
            ex.printStackTrace();
            return false;
            }
        }

    /** Flushes out and saves preferences to disk. */
    public static boolean save(Preferences prefs)
        {
        try
            {
            prefs.sync();
            return true;
            }
        catch (Exception ex)
            {
            ex.printStackTrace();
            return false;
            }
        }

    /**
     * Given a preferences path X for a given module, sets X to have the given
     * value.. Also sets the global path X to the value.
     */
    public static void setLastX(String value, String x, String moduleName)
        {
        if (moduleName != null)
            {
            java.util.prefs.Preferences app_p = Prefs.getAppPreferences(moduleName, "data");
            app_p.put(x, value);
            Prefs.save(app_p);
            }
        }

    /** Given a global preferences path X, sets X to have the given value. */
    public static final void setLastX(String value, String x)
        {
        java.util.prefs.Preferences global_p = Prefs.getGlobalPreferences("data");
        global_p.put(x, value);
        Prefs.save(global_p);
        }

    /**
     * Given a preferences path X for a given module, returns the value stored in X.
     * If there is no such value, then returns null.
     */
    public static final String getLastX(String x, String moduleName)
        {
        if (moduleName != null)
            {
            String prop = System.getProperty("+" + moduleName + ":" + x);
            if (prop != null)
                {
                setLastX(prop, x, moduleName);
                }
            prop = System.getProperty(moduleName + ":" + x);
            if (prop != null)
                {
                return prop;
                }
            else return Prefs.getAppPreferences(moduleName, "data").get(x, null);
            }
        else return null;
        }

    /**
     * Given a preferences path X for a given module, returns the value stored in X.
     * If there is no such value, then returns the value stored in X in the globals.
     * If there again is no such value, returns null. Typically this method is
     * called by a a cover function
     */
    public static final String getLastX(String x)
        {
        String prop = System.getProperty("+" + x);
        if (prop != null)
            {
            setLastX(prop, x);
            }
        prop = System.getProperty(x);
        if (prop != null)
            {
            return prop;
            }
        else return Prefs.getGlobalPreferences("data").get(x, null);
        }

    public static void setLastInt(String key, int val)
        {
        setLastX("" + val, key);
        }

    public static void setLastDouble(String key, double val)
        {
        setLastX("" + val, key);
        }

    public static void setLastBoolean(String key, boolean val)
        {
        setLastX("" + val, key);
        }

    public static int getLastInt(String key, int defaultVal)
        {
        String str = getLastX(key);
        if (str == null) return defaultVal;
        try     
            {
            return Integer.parseInt(str);
            }
        catch(Exception ex)
            {
            System.err.println("Invalid value " + str + " for key " + key);
            return defaultVal;
            }
        }

    public static double getLastDouble(String key, double defaultVal)
        {
        String str = getLastX(key);
        if (str == null) return defaultVal;
        try     
            {
            return Double.parseDouble(str);
            }
        catch(Exception ex)
            {
            System.err.println("Invalid value " + str + " for key " + key);
            return defaultVal;
            }
        }

    public static boolean getLastBoolean(String key, boolean defaultVal)
        {
        String str = getLastX(key);
        if (str == null) return defaultVal;
        try     
            {
            return str.equalsIgnoreCase("true");
            }
        catch(Exception ex)
            {
            System.err.println("Invalid value " + str + " for key " + key);
            return defaultVal;
            }
        }


    // For more examples see flow/Prefs.java

    public static void setLastTupleOut(int index, String value)
        {
        setLastX("" + value, "LastTupleOut" + index);
        }

    public static String getLastTupleOut(int index)
        {
        return getLastX("LastTupleOut" + index);
        }

    public static void setLastTupleIn(int index, String value)
        {
        setLastX("" + value, "LastTupleIn" + index);
        }

    public static String getLastTupleIn(int index)
        {
        return getLastX("LastTupleIn" + index);
        }

    public static final int DEFAULT_TUPLE_OUT_CHANNEL = 1;
    public static final int DEFAULT_TUPLE_IN_CHANNEL = 1;

    public static void setLastTupleOutChannel(int index, int channel)
        {
        setLastX("" + channel, "LastTupleOutChannel" + index);
        }

    public static int getLastTupleOutChannel(int index)
        {
        String s = getLastX("LastTupleOutChannel" + index);
        try
            {
            if (s != null)
                return Integer.parseInt(s);
            }
        catch (NumberFormatException e)
            {
            }
        return DEFAULT_TUPLE_OUT_CHANNEL;
        }

    public static void setLastTupleInChannel(int index, int channel)
        {
        setLastX("" + channel, "LastTupleInChannel" + index);
        }

    public static int getLastTupleInChannel(int index)
        {
        String s = getLastX("LastTupleInChannel" + index);
        try
            {
            if (s != null)
                return Integer.parseInt(s);
            }
        catch (NumberFormatException e)
            {
            }
        return DEFAULT_TUPLE_IN_CHANNEL;
        }




    public static void setLastOutDevice(int index, int device, String module)
        {
        setLastX("" + device, "LastOutDevice" + index, module);
        }

    public static int getLastOutDevice(int index, String module)
        {
        return getLastOutDevice(index, module, 0);
        }

    public static int getLastOutDevice(int index, String module, int _default)
        {
        String s = getLastX("LastOutDevice" + index, module);
        try
            {
            if (s != null)
                return Integer.parseInt(s);
            }
        catch (NumberFormatException e)
            {
            }
        return _default;
        }

    public static void setLastInDevice(int index, int device, String module)
        {
        setLastX("" + device, "LastInDevice" + index, module);
        }

    public static int getLastInDevice(int index, String module)
        {
        return getLastInDevice(index, module, 0);
        }

    public static int getLastInDevice(int index, String module, int _default)
        {
        String s = getLastX("LastInDevice" + index, module);
        try
            {
            if (s != null)
                return Integer.parseInt(s);
            }
        catch (NumberFormatException e)
            {
            }
        return _default;
        }

    public static void setLastControlDevice(int index, int device, String module)
        {
        setLastX("" + device, "LastControlDevice" + index, module);
        }

    public static int getLastControlDevice(int index, String module)
        {
        return getLastControlDevice(index, module, 0);
        }

    public static int getLastControlDevice(int index, String module, int _default)
        {
        String s = getLastX("LastControlDevice" + index, module);
        try
            {
            if (s != null)
                return Integer.parseInt(s);
            }
        catch (NumberFormatException e)
            {
            }
        return _default;
        }

    public static void setLastGridIn(int index, int device, String module)
        {
        setLastX("" + device, "LastGridIn" + index, module);
        }

    public static int getLastGridIn(int index, String module)
        {
        return getLastGridIn(index, module, 0);
        }

    public static int getLastGridIn(int index, String module, int _default)
        {
        String s = getLastX("LastGridIn" + index, module);
        try
            {
            if (s != null)
                return Integer.parseInt(s);
            }
        catch (NumberFormatException e)
            {
            }
        return _default;
        }

    public static void setLastGridOut(int index, int device, String module)
        {
        setLastX("" + device, "LastGridOut" + index, module);
        }

    public static int getLastGridOut(int index, String module)
        {
        return getLastGridOut(index, module, 0);
        }

    public static int getLastGridOut(int index, String module, int _default)
        {
        String s = getLastX("LastGridOut" + index, module);
        try
            {
            if (s != null)
                return Integer.parseInt(s);
            }
        catch (NumberFormatException e)
            {
            }
        return _default;
        }

    public static void setLastGridDevice(int index, int device, String module)
        {
        setLastX("" + device, "LastGridDevice" + index, module);
        }

    public static int getLastGridDevice(int index, String module)
        {
        return getLastGridDevice(index, module, 0);
        }

    public static int getLastGridDevice(int index, String module, int _default)
        {
        String s = getLastX("LastGridDevice" + index, module);
        try
            {
            if (s != null)
                return Integer.parseInt(s);
            }
        catch (NumberFormatException e)
            {
            }
        return _default;
        }

    public static void setLastTupleOutName(int index, String name)
        {
        setLastX(name == null ? "" : name.trim(), "LastTupleOutName" + index);
        }

    public static String getLastTupleOutName(int index)
        {
        String s = getLastX("LastTupleOutName" + index);
        if (s == null) return null;
        if (s.trim().length() == 0) return null;
        return s.trim();
        }

    public static void setLastTupleInName(int index, String name)
        {
        setLastX(name == null ? "" : name.trim(), "LastTupleInName" + index);
        }

    public static String getLastTupleInName(int index)
        {
        String s = getLastX("LastTupleInName" + index);
        if (s == null) return null;
        if (s.trim().length() == 0) return null;
        return s.trim();
        }

    }

