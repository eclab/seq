/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.gui;

import seq.util.*;
import javax.swing.*;
import java.awt.*;
import java.util.*;


public class Dialogs
    {
    private static final long serialVersionUID = 1;

    static boolean inSimpleError;

    /** Display a simple error message. */
    public static void showSimpleError(String title, String message)
        {
        showSimpleError(null, title, message);
        }

    /** Display a simple error message. */
    public static void showSimpleError(JComponent parent, String title, String message)
        {
        // A Bug in OS X (perhaps others?) Java causes multiple copies of the same Menu event to be issued
        // if we're popping up a dialog box in response, and if the Menu event is caused by command-key which includes
        // a modifier such as shift.  To get around it, we're just blocking multiple recursive message dialogs here.
        
        if (inSimpleError) return;
        inSimpleError = true;
        disableMenuBar(parent);
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
        enableMenuBar();
        inSimpleError = false;
        }

    /** Display a simple error message. */
    public static void showSimpleError(JComponent parent, String title, String message, JComponent extra)
        {
        // A Bug in OS X (perhaps others?) Java causes multiple copies of the same Menu event to be issued
        // if we're popping up a dialog box in response, and if the Menu event is caused by command-key which includes
        // a modifier such as shift.  To get around it, we're just blocking multiple recursive message dialogs here.
        
        if (inSimpleError) return;
        inSimpleError = true;
        disableMenuBar(parent);

        JPanel panel = new JPanel();        
        panel.setLayout(new BorderLayout());
        panel.add(new JLabel(message), BorderLayout.NORTH);

        JPanel inside = new JPanel();        
        inside.setLayout(new BorderLayout());
        inside.add(extra, BorderLayout.NORTH);
        
        JScrollPane pane = new JScrollPane(inside);
        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        pane.setPreferredSize(new Dimension(30, 64));
        panel.add(pane, BorderLayout.CENTER);
        
        JOptionPane.showMessageDialog(parent, panel, title, JOptionPane.ERROR_MESSAGE);
        enableMenuBar();
        inSimpleError = false;
        }


    /** Display a simple error message. */
    public static void showErrorWithStackTraceUnsafe(Throwable error, String message, String title)
        {
        String[] options = new String[] { "Okay", "Save Error" };
        int ret = JOptionPane.showOptionDialog(null, message, title, JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[0]);                
        if (ret == 1)
            {
            ExceptionDump.saveThrowable(error);
            }
        }


    /** Display a simple error message. If ExceptionDump.lastThrowableExists() then it will be used as the error.
        Otherwise if error is null, new RuntimeException(message) will be used.  Otherwise the error itself will be used. */
    public static void showErrorWithStackTrace(JComponent parent, Throwable error, String title, String message)
        {
        if (!ExceptionDump.lastThrowableExists())
            {
            System.out.println("WARNING: error with stack trace requested but there's no Throwable");
            ExceptionDump.handleException(error == null ? new RuntimeException("" + message) : error);
            showSimpleError(title, message);
            }
        else
            {
            // A Bug in OS X (perhaps others?) Java causes multiple copies of the same Menu event to be issued
            // if we're popping up a dialog box in response, and if the Menu event is caused by command-key which includes
            // a modifier such as shift.  To get around it, we're just blocking multiple recursive message dialogs here.
                
            if (inSimpleError) return;
            inSimpleError = true;
            disableMenuBar(parent);
            String[] options = new String[] { "Okay", "Save Error" };
            int ret = JOptionPane.showOptionDialog(parent, message, title, JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[0]);
            enableMenuBar();
            inSimpleError = false;
                
            if (ret == 1)
                {
                ExceptionDump.saveThrowable(parent);
                }
            }
        }

    /** Display a simple error message. If ExceptionDump.lastThrowableExists() then it will be used as the error.
        Otherwise if error is null, new RuntimeException(message) will be used.  Otherwise the error itself will be used. */
    public static void showErrorWithStackTrace(Throwable error, String title, String message)
        {
        showErrorWithStackTrace(null, error, title, message);
        }

    /** Display a simple error message.  If ExceptionDump.lastThrowableExists() then it will be used as the error.
        Otherwise if error is null, new RuntimeException(message) will be used.  */
    public static void showErrorWithStackTrace(JComponent parent, String title, String message)
        {
        showErrorWithStackTrace(parent, null, title, message);
        }

    /** Display a simple error message.  If ExceptionDump.lastThrowableExists() then it will be used as the error.
        Otherwise if error is null, new RuntimeException(message) will be used.  */
    public static void showErrorWithStackTrace(String title, String message)
        {
        showErrorWithStackTrace(null, null, title, message);
        }

    /** Display a simple error message. */
    public static void showSimpleMessage(JComponent parent, String title, String message)
        {
        // A Bug in OS X (perhaps others?) Java causes multiple copies of the same Menu event to be issued
        // if we're popping up a dialog box in response, and if the Menu event is caused by command-key which includes
        // a modifier such as shift.  To get around it, we're just blocking multiple recursive message dialogs here.
        
        if (inSimpleError) return;
        inSimpleError = true;
        disableMenuBar(parent);
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
        enableMenuBar();
        inSimpleError = false;
        }

    /** Display a simple error message. */
    public static void showSimpleMessage(String title, String message)
        {
        showSimpleMessage(null, title, message);
        }

    /** Display a simple (OK / Cancel) confirmation message.  Return the result (ok = true, cancel = false). */
    public static boolean showSimpleConfirm(JComponent parent, String title, String message)
        {
        disableMenuBar(parent);
        boolean ret = (JOptionPane.showConfirmDialog(parent, message, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null) == JOptionPane.OK_OPTION);
        enableMenuBar();
        return ret;
        }
        
    /** Display a simple (OK-OPTION / Cancel) confirmation message.  Return the result (ok = true, cancel = false). */
    public static boolean showSimpleConfirm(JComponent parent, String title, String message, String okOption)
        {
        disableMenuBar(parent);
        int ret = JOptionPane.showOptionDialog(parent, message, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
            new String[] { okOption, "Cancel" } , okOption);
        enableMenuBar();
        return (ret == 0);
        }
        
    /** Display a simple (OK / Cancel) confirmation message.  Return the result (ok = true, cancel = false). */
    public static boolean showSimpleConfirm(String title, String message)
        {
        return showSimpleConfirm((JComponent)null, title, message);
        }
        
    /** Display a simple (OK-OPTION / Cancel) confirmation message.  Return the result (ok = true, cancel = false). */
    public static boolean showSimpleConfirm(String title, String message, String okOption)
        {
        return showSimpleConfirm(null, title, message, okOption);
        }
        
    /** Perform a JOptionPane confirm dialog with MUTLIPLE widgets that the user can select.  The widgets are provided
        in the array WIDGETS, and each has an accompanying label in LABELS.   You specify what BUTTONS appear along the bottom
        as the OPTIONS, which (on the Mac) appear right-to-left. You also get to specify well as the default option -- what
        button is chosen if the user presses RETURN.  On the Mac, classically this is the first (rightmost) option. 
        Returns the option number selected; otherwise returns -1 if the user clicked the close box. */
    public static int showMultiOption(JComponent parent, String[] labels, JComponent[] widgets, String[] options, int defaultOption, String title, JComponent message)
        {
        WidgetList list = new WidgetList(labels, widgets);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        p.add(new JLabel("    "), BorderLayout.NORTH);
        p.add(message, BorderLayout.CENTER);
        p.add(new JLabel("    "), BorderLayout.SOUTH);
        panel.add(list, BorderLayout.CENTER);
        panel.add(p, BorderLayout.NORTH);

        disableMenuBar(parent);
        int ret = JOptionPane.showOptionDialog(parent, panel, title, JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[defaultOption]);
        enableMenuBar();
        return ret;
        }

    /** Perform a JOptionPane confirm dialog with MUTLIPLE widgets that the user can select.  The widgets are provided
        in the array WIDGETS, and each has an accompanying label in LABELS.   You specify what BUTTONS appear along the bottom
        as the OPTIONS, which (on the Mac) appear right-to-left. You also get to specify well as the default option -- what
        button is chosen if the user presses RETURN.  On the Mac, classically this is the first (rightmost) option. 
        Returns the option number selected; otherwise returns -1 if the user clicked the close box. */
    public static int showMultiOption(JComponent parent, String[] labels, JComponent[] widgets, String[] options, int defaultOption, String title, String message)
        {
        return showMultiOption(parent, labels, widgets, options, defaultOption, title, new JLabel(message));
        }


    /** Perform a JOptionPane confirm dialog with MUTLIPLE widgets that the user can select.  The widgets are provided
        in the array WIDGETS, and each has an accompanying label in LABELS.   Returns TRUE if the user performed
        the operation, FALSE if cancelled. */
    public static boolean showMultiOption(JComponent parent, String[] labels, JComponent[] widgets, String title, String message)
        {
        return showMultiOption(parent, labels, widgets, new String[] { "Okay", "Cancel" }, 0, title, message) == 0;
        }


    /** Perform a JOptionPane confirm dialog with MUTLIPLE widgets that the user can select.  The widgets are provided
        in the array WIDGETS, and each has an accompanying label in LABELS.   Returns TRUE if the user performed
        the operation, FALSE if cancelled. */
    public static boolean showMultiOption(JComponent parent, String[] labels, JComponent[] widgets, String title, JComponent message)
        {
        return showMultiOption(parent, labels, widgets, new String[] { "Okay", "Cancel" }, 0, title, message) == 0;
        }


    static ArrayList<JMenuItem> disabledMenus = null;
    static int disableCount;
    /** Disables the menu bar.  disableMenuBar(parent) and enableMenuBar() work in tandem to work around
        a goofy bug in OS X: you can't disable the menu bar and reenable it: it won't reenable
        unless the application loses focus and regains it, and even then sometimes it won't work.
        These functions work properly however.  You want to disable and enable the menu bar because
        in OS X the menu bar still functions even when in a modal dialog!  Bad OS X Java errors.
    */
    public static void disableMenuBar(JComponent parent)
        {
        if (parent == null) return;
        
        if (disabledMenus == null)
            {
            disabledMenus = new ArrayList<JMenuItem>();
            disableCount = 0;
            JFrame ancestor = ((JFrame)(SwingUtilities.getWindowAncestor(parent)));
            if (ancestor == null) return;
            JMenuBar bar = ancestor.getJMenuBar();
            for(int i = 0; i < bar.getMenuCount(); i++)
                {
                JMenu menu = bar.getMenu(i);
                if (menu != null)
                    {
                    for(int j = 0; j < menu.getItemCount(); j++)
                        {
                        JMenuItem item = menu.getItem(j);
                        if (item != null && item.isEnabled())
                            {
                            disabledMenus.add(item);
                            item.setEnabled(false);
                            }
                        }
                    }
                }
            }
        else
            {
            disableCount++;
            return;
            }
        }       
        
    /** Enables the menu bar.  disableMenuBar(parent) and enableMenuBar() work in tandem to work around
        a goofy bug in OS X: you can't disable the menu bar and reenable it: it won't reenable
        unless the application loses focus and regains it, and even then sometimes it won't work.
        These functions work properly however.  You want to disable and enable the menu bar because
        in OS X the menu bar still functions even when in a modal dialog!  Bad OS X Java errors.
    */
    public static void enableMenuBar()
        {
        if (disableCount == 0)
            {
            for(int i = 0; i < disabledMenus.size(); i++)
                {
                disabledMenus.get(i).setEnabled(true);
                }
            disabledMenus = null;
            }
        else
            {
            disableCount--;
            }
        }       

    }
