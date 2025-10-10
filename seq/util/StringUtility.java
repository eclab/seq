/***
    Copyright 2019 by Sean Luke
    Licensed under the Apache License version 2.0
*/

package seq.util;

import java.util.*;
import java.io.*;

public class StringUtility
    {
    private static final long serialVersionUID = 1;

    // Replace "<" with "[" and replace "&" with "@"
    public static String sanitize(String text)
        {
        if (text.contains("<") || text.contains("&") || text.contains(">"))
            {
            char[] chars = text.toCharArray();
            for(int i = 0; i < chars.length; i++)
                {
                if (chars[i] == '<') chars[i] = '[';
                else if (chars[i] == '>') chars[i] = ']';
                else if (chars[i] == '&') chars[i] = '@';
                }
            return String.valueOf(chars);
            }
        else return text;
        }

    static boolean isWindows() 
        {
        return (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0);
        }


    public static String makeValidFilename(String filename)
        {
        char[] f = filename.toCharArray();
        
        for(int i = 0; i < f.length; i++)
            {
            if (f[i] <= 32 || f[i] >= 127 ||
                f[i] == java.io.File.pathSeparatorChar ||
                f[i] == java.io.File.separatorChar)
                f[i] = '-';
            }

        if (isWindows())
            {
            for(int i = 0; i < f.length; i++)
                {
                // Lots of stuff is invalid in Windows
                if (f[i] < ' ' || f[i] >= 127) f[i] = ' ';
                else if (f[i] == '/') f[i] = '-';
                else if (f[i] == '\\') f[i] = '-';
                else if (f[i] == ':') f[i] = '-';
                else if (f[i] == '*') f[i] = '-';
                else if (f[i] == '?') f[i] = '!';
                else if (f[i] == '"') f[i] = '\'';
                else if (f[i] == '<') f[i] = '[';
                else if (f[i] == '>') f[i] = ']';
                else if (f[i] == '|') f[i] = '-';
                // we're ignoring stuff like PRN and COM etc.
                }
            }
        else
            {
            // technically everything is legal in Unix except for / and \0.  But we'll replace
            // a few more.
            for(int i = 0; i < f.length; i++)
                {
                if (f[i] < ' ' || f[i] >= 127) f[i] = ' ';
                else if (f[i] == '/') f[i] = '-';
                else if (f[i] == '\\') f[i] = '-';
                }
            }
        return String.valueOf(f);
        }
    
    public static int getFirstInt(String string)
        {
        return new Scanner(string).useDelimiter("\\D+").nextInt();
        }

    public static int getSecondInt(String string)
        {
        Scanner scan = new Scanner(string);
        scan.useDelimiter("\\D+");
        scan.nextInt();
        return scan.nextInt();
        }
    
    /** Returns the sole integer embedded within the string.  If
        multiple integers are embedded, they will be concatenated into a larger
        integer, which you probably don't want.  Throws NumberFormatException
        if there is no integer at all. */
    public static int getInt(String string)
        {
        return Integer.parseInt(string.replaceAll("[^0-9]+", " ").trim());
        }
           
    /** Returns the sole integer embedded within the string after a preamble.  
        Does not check to see if the preamble is real -- it better be!  If
        multiple integers are embedded after the preamble, they will be 
        concatenated into a larger
        integer, which you probably don't want.  Throws NumberFormatException
        if there is no integer at all. */
    public static int getIntAfter(String string, String preamble)
        {
        return getInt(string.substring(preamble.length()));
        }

    final static int STATE_FIRST_NUMBER = 0;
    final static int STATE_FIRST_STRING = 1;
    final static int STATE_NUMBER = 2;
    final static int STATE_FINISHED = 3;
        
    public static String reduceDigitsInPreamble(String name, String preamble)
        {
        if (!name.startsWith(preamble)) 
            {
            System.err.println("Warning (Category): Key " + name + " doesn't start with " + preamble);
            return name;
            }

        char[] n = name.toCharArray();
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < preamble.length(); i++)
            {
            if (!Character.isDigit(n[i]))
                sb.append(n[i]);
            }
        for(int i = preamble.length(); i < n.length; i++)
            {
            sb.append(n[i]);
            }
        return sb.toString();
        }

    public static String reduceAllDigitsAfterPreamble(String name, String preamble)
        {
        if (!name.startsWith(preamble)) 
            {
            System.err.println("Warning (Category): Key " + name + " doesn't start with " + preamble);
            return name;
            }

        char[] n = name.toCharArray();
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < preamble.length(); i++)
            {
            sb.append(n[i]);
            }
                        
        int state = STATE_FIRST_STRING;
        for(int i = preamble.length(); i < n.length; i++)
            {
            if (state == STATE_FIRST_STRING)
                {
                if (Character.isDigit(n[i]))
                    {
                    state = STATE_FINISHED;
                    }
                else
                    {
                    sb.append(n[i]);
                    }
                }
            else if (state == STATE_FINISHED)
                {
                if (!Character.isDigit(n[i]))
                    {
                    sb.append(n[i]);
                    }
                }
            }
        return sb.toString();
        }

    /** This function removes the FIRST string of digits in a name after a preamble, returns the resulting name. */
    public static String reduceFirstDigitsAfterPreamble(String name, String preamble)
        {
        if (!name.startsWith(preamble)) 
            {
            System.err.println("Warning (Category): Key " + name + " doesn't start with " + preamble);
            return name;
            }

        char[] n = name.toCharArray();
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < preamble.length(); i++)
            {
            sb.append(n[i]);
            }
                        
        int state = STATE_FIRST_STRING;
        for(int i = preamble.length(); i < n.length; i++)
            {
            if (state == STATE_FIRST_STRING)
                {
                if (Character.isDigit(n[i]))
                    {
                    state = STATE_NUMBER;
                    }
                else
                    {
                    sb.append(n[i]);
                    }
                }
            else if (state == STATE_NUMBER)
                {
                if (!Character.isDigit(n[i]))
                    {
                    sb.append(n[i]);
                    state = STATE_FINISHED;
                    }
                }
            else // state == STATE_FINISHED
                {
                sb.append(n[i]);
                }
            }
        return sb.toString();
        }

    /** This function removes the SECOND string of digits in a name after a preamble, returns the resulting name. */
    public static String reduceSecondDigitsAfterPreamble(String name, String preamble)
        {
        if (!name.startsWith(preamble)) 
            {
            System.err.println("Warning (Category): Key " + name + " doesn't start with " + preamble);
            return name;
            }

        char[] n = name.toCharArray();
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < preamble.length(); i++)
            {
            sb.append(n[i]);
            }
                        
        int state = STATE_FIRST_NUMBER;
        for(int i = preamble.length(); i < n.length; i++)
            {
            if (state == STATE_FIRST_NUMBER)
                {
                if (!Character.isDigit(n[i]))
                    {
                    // add it and jump to next state
                    sb.append(n[i]);
                    state = STATE_FIRST_STRING;
                    }
                else
                    {
                    sb.append(n[i]);
                    }
                }
            else if (state == STATE_FIRST_STRING)
                {
                if (Character.isDigit(n[i]))
                    {
                    // skip it and jump to next state
                    state = STATE_NUMBER;
                    }
                else
                    {
                    sb.append(n[i]);
                    }
                }
            else if (state == STATE_NUMBER)
                {
                if (!Character.isDigit(n[i]))
                    {
                    // add it and jump to next state
                    sb.append(n[i]);
                    state = STATE_FINISHED;
                    }
                }
            else  // state == STATE_FINISHED
                {
                sb.append(n[i]);
                }
            }
        return sb.toString();
        }

    static final int STATE_START = 0;
    static final int STATE_INT = 1;
    static final int STATE_ADD = 2;
        
    /** Removes the preamble and initial number (the number must be present), leaving the remainder. */
    public static String removePreambleAndFirstDigits(String name, String preamble)
        {
        char[] n = name.toCharArray();
        char[] p = preamble.toCharArray();
        StringBuilder sb = new StringBuilder();
        int state = STATE_START;
        for(int i = 0; i < n.length; i++)
            {
            if (state == STATE_START)
                {
                if (i >= p.length) return name; // uh oh, didn't transition to a digit
                if (n[i] != p[i]) return name;
                if (i == p.length - 1 && i >= n.length - 1) return name;// no digit
                if (i == p.length - 1 && i < n.length - 1 && Character.isDigit(n[i + 1]))
                    {
                    state = STATE_INT;
                    }
                }
            else if (state == STATE_INT)
                {
                if (!Character.isDigit(n[i]))
                    {
                    sb.append(n[i]);
                    state = STATE_ADD;
                    }
                }
            else
                {
                sb.append(n[i]);
                }
            }
        return sb.toString();
        }
        
    public static String rightTrim(String str)
        {
        int i = str.length() - 1;
        while (i >= 0 && Character.isWhitespace(str.charAt(i))) 
            {
            i = i - 1;
            }
        return str.substring(0, i + 1);
        }
                        

    /** Shuffles keys randomly in place and returns it using Fisher-Yates */
    public static String[] shuffle(String[] keys, Random random)
        {
        for(int i = keys.length - 1; i > 0; i--)        // don't include i = 0
            {
            int j = random.nextInt(i + 1);          // include j = i
            String temp = keys[i];
            keys[i] = keys[j];
            keys[j] = temp;
            }
        return keys;
        }

    /** Guarantee that the given filename ends with the given ending.  */   
    public static String ensureFileEndsWith(String filename, String ending)
        {
        // do we end with the string?
        if (filename.regionMatches(false,filename.length()-ending.length(),ending,0,ending.length()))
            return filename;
        else return filename + ending;
        }
    
    /** Removes the extension from the given file name.  
    	From https://stackoverflow.com/questions/941272/how-do-i-trim-a-file-extension-from-a-string-in-java */
public static String removeExtension(String s) 
{
    String separator = File.separator;
    String filename;

    // Remove the path upto the filename.
    int lastSeparatorIndex = s.lastIndexOf(separator);
    if (lastSeparatorIndex == -1) 
    {
        filename = s;
    } 
    else 
    {
        filename = s.substring(lastSeparatorIndex + 1);
    }

    // Remove the extension.
    int extensionIndex = filename.lastIndexOf(".");
    if (extensionIndex == -1)
        return filename;

    return filename.substring(0, extensionIndex);
}

    /** Returns the byte as a hex number */
    public static String toHex(byte val)
        {
        return String.format("%02X", val);
        }

    /** Returns the byte as a hex number */
    public static String toHex(byte[] vals)
        {
        String str = "";
        for(int i = 0; i < vals.length; i++)
            str += (" " + toHex(vals[i]));
        return str;
        }

    /** Returns the byte as a hex number with indices */
    public static String toHexWithIndices(byte[] vals)
        {
        String str = "";
        for(int i = 0; i < vals.length; i++)
            str += ("" + i + "\t" + toHex(vals[i]) + "\n");
        return str;
        }

    /** Returns the integer as a hex number, only the first four digits shown */
    public static String toHexFull(int val)
        {
        return String.format("%X", val);
        }


    /** Returns the integer as a hex number, only the first four digits shown */
    public static String toHexShort(int val)
        {
        return String.format("%04X", val);
        }

    /** Returns the integer as a hex number */
    public static String toHexInt(int val)
        {
        return String.format("%08X", val);
        }
    }

