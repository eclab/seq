/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.util;

import seq.gui.*;
import java.awt.*;
import javax.swing.*;
import java.io.*;

public class ExceptionDump
    {
    private static final long serialVersionUID = 1;

    static Throwable lastThrowable = null;
    static String auxillary = null;
        
    public static boolean lastThrowableExists() { return (lastThrowable != null); }
        
    public static void postThrowable(Throwable th)
        {
        postThrowable(th, null);
        }
                
    public static void postThrowable(Throwable th, String aux)
        {
        lastThrowable = th;
        auxillary = aux;
        }
                
    public static void saveThrowable(JComponent parent, Throwable th)
        {
        postThrowable(th);
        saveThrowable(parent);
        }

    public static void saveThrowable(JComponent parent, Throwable th, String auxillary)
        {
        postThrowable(th, auxillary);
        saveThrowable(parent);
        }

    public static void saveThrowable(Throwable th)
        {
        postThrowable(th);
        saveThrowable();
        }

    public static void saveThrowable(Throwable th, String auxillary)
        {
        postThrowable(th, auxillary);
        saveThrowable();
        }

    public static void saveThrowable()
        {
        saveThrowable((JComponent)null);
        }

    /** Call this to log exceptions.  By default this method just prints the exception to the
        command line, but it can be modified to open a file and log the exception that way
        so a user can mail the developer a debug file. */
    public static void handleException(Throwable ex) { if (ex != null) ex.printStackTrace(); }    
    

    public static void saveThrowable(JComponent component)
        {
        if (lastThrowable != null)
            {
            FileDialog fd = new FileDialog((Frame)null, "Save Error File...", FileDialog.SAVE);
            fd.setFile("EdisynError.txt");
            
            Dialogs.disableMenuBar(component);
            fd.setVisible(true);
            Dialogs.enableMenuBar();
                        
            File f = null; // make compiler happy
            PrintStream ps = null;
            if (fd.getFile() != null)
                {
                try
                    {
                    f = new File(fd.getDirectory(), StringUtility.ensureFileEndsWith(fd.getFile(), ".txt"));
                    ps = new PrintStream(new FileOutputStream(f));
                    if (auxillary != null)
                        ps.println(auxillary);
                    lastThrowable.printStackTrace(ps);
                    ps.close();
                    } 
                catch (IOException e) // fail
                    {
                    Dialogs.showSimpleError(component, "File Error", "An error occurred while saving to the file " + (f == null ? " " : f.getName()));
                    e.printStackTrace();
                    }
                finally
                    {
                    if (ps != null)
                        ps.close();
                    }
                }
            }
        lastThrowable = null;
        }
    }
        
        
