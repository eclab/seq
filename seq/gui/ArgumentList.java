/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.gui;

import seq.engine.*;
import seq.util.*;
import java.awt.*;
import javax.swing.border.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.concurrent.locks.*;
import java.util.*;
import com.formdev.flatlaf.*;

public class ArgumentList extends JPanel
    {
    JCheckBox deterministic;
    SmallDial[] dials = new SmallDial[Motif.NUM_PARAMETERS + 2];
    Motif.Child child;
    Seq seq;
        
    public static final String[] RANDOM_DEFAULTS = new String[0];
    public String[] defaults = new String[1 + Motif.NUM_PARAMETERS];
    public String[] args = new String[2 + Motif.NUM_PARAMETERS];
    public void buildDefaults(Motif child, Motif parent)
        {
        //defaults[0] = "None";
        defaults[0] = "Rand";
        for(int i = 0; i < Motif.NUM_PARAMETERS; i++)
            {
            String name = parent.getParameterName(i);
            if (name == null || name.length() == 0)
                {
                defaults[1 + i] = "Param " + (i + 1);
                }
            else
                {
                defaults[1 + i] = "" + (i + 1) + ": " + name;
                }
            }
        args[0] = "Rand Min";
        args[1] = "Rand Max";
        for(int i = 0; i < Motif.NUM_PARAMETERS; i++)
            {
            String name = child.getParameterName(i);
            if (name == null || name.length() == 0)
                {
                args[2 + i] = "Arg " + (i + 1);
                }
            else
                {
                args[2 + i] = name;
                }
            }
        }
         
    public ArgumentList(Seq seq, final Motif.Child child, final Motif parent)
        {
        this.child = child;
        this.seq = seq;
        
        JComponent[] comp = new JComponent[Motif.NUM_PARAMETERS + 2];           // Random Min and Max also
        ReentrantLock lock = seq.getLock();
        lock.lock();
        buildDefaults(child.getMotif(), parent);
        try 
            { 
            for(int i = 0; i < comp.length; i++)
                {
                final int _i = i;
                                
                // Determine initial values
                double initialValue = 0;
                int initialDefault = 0;
                if (i == 0)             // random min
                    {
                    initialValue = child.getRandomMin();
                    initialDefault = Dial.NO_DEFAULT;               
                    }
                else if (i == 1)        // random max
                    {
                    initialValue = child.getRandomMax();
                    initialDefault = Dial.NO_DEFAULT;       
                    }
                else
                    {
                    double param = child.getParameter(i - 2);
                    if (param >= 0)         // bound to a ground value
                        {
                        initialValue = param;
                        initialDefault = Dial.NO_DEFAULT; 
                        }
                    /*
                      else if (param == Motif.Child.PARAMETER_UNBOUND)        // -1
                      {
                      initialValue = 0;
                      initialDefault = 0;
                      }
                    */
                    else if (param == Motif.Child.PARAMETER_RANDOM)         // -1
                        {
                        initialValue = 0;
                        initialDefault = 1;
                        }
                    else if (param < Motif.Child.PARAMETER_RANDOM)          // <= -2 Bound to a parent parameter
                        {
                        initialValue = 0;
                        initialDefault = -((int)param) - 1;
                        }
                    else
                        {
                        initialValue = 0;
                        initialDefault = 0;
                        System.err.println("ArgumentList.ArgumentList() error, no valid parameter " + param + " for " + i);
                        }
                    }
                                
                dials[i] = new SmallDial(initialValue, initialDefault, /*i < 2 ? RANDOM_DEFAULTS :*/ defaults)
                    {
                    protected String map(double val) { return String.format("%.4f", val); }
                                        
                    public double getValue() 
                        { 
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try 
                            { 
                            if (_i == 0) return child.getRandomMin();
                            else if (_i == 1) return child.getRandomMax();
                            else return child.getParameter(_i - 2);
                            }
                        finally { lock.unlock(); }
                        }
                                                
                    public void setValue(double val) 
                        { 
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try 
                            {
                            if (_i == 0) child.setRandomMin(val);
                            else if (_i == 1) child.setRandomMax(val);
                            else child.setParameter(_i - 2, val);
                            }
                        finally { lock.unlock(); }
                        }

                    public int getDefault()
                        {
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try 
                            {
                            if (_i == 0)            // Min
                                {
                                double d = child.getRandomMin();
                                if (d == Motif.Child.PARAMETER_RANDOM) return 0;        // "Random"
                                else if (d < Motif.Child.PARAMETER_RANDOM)
                                    {
                                    return (-(int)(d - (Motif.Child.PARAMETER_RANDOM - 1)));        // parameters range 0...n
                                    }
                                else return SmallDial.NO_DEFAULT;
                                }
                            else if (_i == 1)       // Max
                                {
                                double d = child.getRandomMax();
                                if (d == Motif.Child.PARAMETER_RANDOM) return 0;        // "Random"
                                else if (d < Motif.Child.PARAMETER_RANDOM)
                                    {
                                    return (-(int)(d - (Motif.Child.PARAMETER_RANDOM - 1)));        // parameters range 0...n
                                    }
                                else return SmallDial.NO_DEFAULT;
                                }
                            else                            // A parameter
                                {
                                double d = child.getParameter(_i - 2);
                                /*
                                  if (d == Motif.Child.PARAMETER_UNBOUND) return 0;               // "Unbound"
                                  else 
                                */
                                if (d == Motif.Child.PARAMETER_RANDOM) return 0;        // "Random"
                                else if (d < Motif.Child.PARAMETER_RANDOM)
                                    {
                                    return (-(int)(d - (Motif.Child.PARAMETER_RANDOM - 1)));        // parameters range 0...n
                                    }
                                else return SmallDial.NO_DEFAULT;
                                }
                            }
                        finally { lock.unlock(); }
                        }
                                        
                    public void setDefault(int val) 
                        { 
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try 
                            {
                            if (_i == 0)
                                {
                                if (val == SmallDial.NO_DEFAULT) return;
                                else if (val == 0)      // bound to rand
                                    {
                                    child.setRandomMin(Motif.Child.PARAMETER_RANDOM);
                                    }
                                else if (val > 0)
                                    {
                                    child.setRandomMin((-val) + (Motif.Child.PARAMETER_RANDOM - 1));
                                    }
                                else
                                    {
                                    System.err.println("ArgumentList.setDefault() error: random min attempted set to value " + val);
                                    }
                                }
                            else if (_i == 1)
                                {
                                if (val == SmallDial.NO_DEFAULT) return;
                                else if (val == 0)      // bound to rand
                                    {
                                    child.setRandomMax(Motif.Child.PARAMETER_RANDOM);
                                    }
                                else if (val > 0)
                                    {
                                    child.setRandomMax((-val) + (Motif.Child.PARAMETER_RANDOM - 1));
                                    }
                                else
                                    {
                                    System.err.println("ArgumentList.setDefault() error: random max attempted set to value " + val);
                                    }
                                }
                            else
                                {
                                if (val == SmallDial.NO_DEFAULT) return;                // FIXME do I need to set anything?
                                /*
                                  else if (val == 0)      // bound to none
                                  {
                                  child.setParameter(_i - 2, Motif.Child.PARAMETER_UNBOUND);
                                  }
                                */
                                else if (val == 0)      // bound to rand
                                    {
                                    child.setParameter(_i - 2, Motif.Child.PARAMETER_RANDOM);
                                    }
                                else if (val > 0)
                                    {
                                    child.setParameter(_i - 2, (-val) + (Motif.Child.PARAMETER_RANDOM - 1));
                                    }
                                else
                                    {
                                    System.err.println("ArgumentList.setDefault() error: parameter " + (_i - 2) + " attempted set to value " + val);
                                    }
                                }
                            }
                        finally { lock.unlock(); }
                        }
                    };
                                        
                comp[i] = dials[i].getLabelledDial("0.0000  ");
                // comp[i + 1] = dials[i].getLabelledDial("0.0000  ");
                }
            }
        finally { lock.unlock(); }
        // String[] strs = new String[Motif.NUM_PARAMETERS + 3];
        /*
          String[] strs = new String[Motif.NUM_PARAMETERS + 2];
          //strs[0] = "Rand Repeat";
          strs[0] = "Rand Orig";
          strs[1] = "Rand Var";
          for(int i = 2; i < strs.length; i++)
          {
          strs[i] = "Arg " + (i - 1);
          }
        */
        WidgetList list = new WidgetList(args, comp);
        list.setBorder(new javax.swing.border.TitledBorder("<html><i>Arguments</i></html>"));
        setLayout(new BorderLayout());
        add(new DisclosurePanel("Arguments", list), BorderLayout.CENTER);
        }
    
    public void revise()
        {
        Seq old = seq;
        seq = null;
        ReentrantLock lock = old.getLock();
        lock.lock();
        try 
            { 
            // empty
            }
        finally { lock.unlock(); }                              
        seq = old;

        for(int i = 0; i < Motif.NUM_PARAMETERS; i++)
            {
            dials[i].redraw();
            }
        }
    }
