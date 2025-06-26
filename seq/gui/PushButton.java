/***
    Copyright 2017 by Sean Luke
    Licensed under the Apache License version 2.0
*/

package seq.gui;

import seq.util.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.border.*;
import javax.swing.*;
import java.awt.event.*;
import javax.accessibility.*;

/**
   A simple button with some useful features.  
   
   1. If you construct the button as new PushButton(text), it's just a button 
   which calls perform() when pressed.  Override perform() as you see fit.
      
   2. If you construct the button as new PushButton(text, String[]), pressing
   the button will pop up a menu with the various String[] options, and call
   perform(int) if one of the options is selected.
      
   3. If you construct the button as new PushButton(text, JMenuItem[]), pressing
   the button will pop up a menu with those JMenuItem[] options. 

   @author Sean Luke
*/

public class PushButton extends JPanel
    {
    JButton button;
    JPopupMenu pop;
    String text;
    boolean popsUpAbove = false;
    
    
    public Insets getInsets() { return new Insets(0,0,0,0); }
    
    public JButton getButton() { return button; }
    
    public String getText() { return text; }
    public void setText(String val) 
        {
        text = val; 
        if (text != null)
            {
            button.setText("<html>"+text+"</html>"); 
            // we need to de-htmlify the text for accessibility
            button.getAccessibleContext().setAccessibleName(text.replaceAll("<.*?>", ""));
            }
        }
    
    public void setToolTipText(String text) { super.setToolTipText(text); button.setToolTipText(text); }
    public boolean getPopsUpAbove() { return popsUpAbove; }
    public void setPopsUpAbove(boolean val) { popsUpAbove = val; }
    
    public AWTEventListener releaseListener = null;
        
    public PushButton(final String text) { this(text, (ImageIcon)null); }
    public PushButton(final ImageIcon icon) { this((String)null, icon); }
    
    public PushButton(final String text, final ImageIcon icon)
        {
        if (icon == null) button = new JButton(text);
        else if (text == null) button = new JButton(icon);
        else button = new JButton(text, icon);
        /*
          {
          AccessibleContext accessibleContext = null;

          // Generate and provide the context information when asked
          public AccessibleContext getAccessibleContext()
          {
          if (accessibleContext == null)
          {
          accessibleContext = new AccessibleJButton()
          {
          public String getAccessibleName()
          {
          String name = super.getAccessibleName();
          // Find enclosing Category
          Component obj = button;
          while(obj != null)
          {
          if (obj instanceof Category)
          {
          return name + " " + ((Category)obj).getName();
          }
          else obj = obj.getParent();
          }
          return name;
          }
          };
          }
          return accessibleContext;
          }
          };
        */

        setText(text);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        final Color foreground = button.getForeground();
        
        button.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                _perform();
                }
            });
        setLayout(new BorderLayout());
        JPanel inner = new JPanel()
            {
            public Insets getInsets() { return new Insets(0,0,0,0); }
            };
        inner.setLayout(new BorderLayout());
        inner.add(button, BorderLayout.NORTH);
        add(inner, BorderLayout.WEST);
        }
    
    public PushButton(String text, ImageIcon icon, String[] options)
        {
        this(text, icon, options, null);
        }

    public PushButton(String text, String[] options)
        {
        this(text, null, options, null);
        }

    public PushButton(ImageIcon icon, String[] options)
        {
        this(null, icon, options, null);
        }

    public void setEnabled(boolean val)
        {
        button.setEnabled(val);
        }

/*
  public void setName(String text)
  {
  button.setText(text);
  }
*/

    public void setOptions(String[] options)
        {
        setOptions(options, null);
        }

    public void setOptions(String[] options, boolean[] enabled)
        {
        pop.removeAll();
        for(int i = 0; i < options.length; i++)
            {
            if (options[i] == null)
                {
                pop.addSeparator();
                }
            else
                {
                JMenuItem menu = new JMenuItem(options[i]);
                if (enabled != null)
                    {
                    menu.setEnabled(enabled[i]);
                    }
                final int _i = i;
                menu.addActionListener(new ActionListener()
                    {
                    public void actionPerformed(ActionEvent e)      
                        {
                        perform(_i);
                        }       
                    });     
                pop.add(menu);
                }
            }
        }

    public void setOptions(JMenuItem[] menuItems)
        {
        pop.removeAll();
        for(int i = 0; i < menuItems.length; i++)
            {
            if (menuItems[i] == null)
                pop.addSeparator();
            else
                pop.add(menuItems[i]);
            }
        }

    public PushButton(String text, String[] options, boolean[] enabled)
        {
        this(text);
        pop = new JPopupMenu();
        setOptions(options, enabled);
        }

    public PushButton(ImageIcon icon, String[] options, boolean[] enabled)
        {
        this(icon);
        pop = new JPopupMenu();
        setOptions(options, enabled);
        }
    
    public PushButton(String text, ImageIcon icon, String[] options, boolean[] enabled)
        {
        this(text, icon);
        pop = new JPopupMenu();
        setOptions(options, enabled);
        }
    
    
    public PushButton(String text, JMenuItem[] menuItems)
        {
        this(text);
        pop = new JPopupMenu();
        setOptions(menuItems);
        }
    
    public PushButton(ImageIcon icon, JMenuItem[] menuItems)
        {
        this(icon);
        pop = new JPopupMenu();
        setOptions(menuItems);
        }
    
    public PushButton(String text, ImageIcon icon, JMenuItem[] menuItems)
        {
        this(text, icon);
        pop = new JPopupMenu();
        setOptions(menuItems);
        }
    
    void _perform()
        {
        if (pop != null)
            {
            button.add(pop);
            Insets insets = button.getInsets();
            if (OS.isMac())
                {
                int height = (int)pop.getPreferredSize().getHeight() + button.getBounds().height - insets.bottom;
                // Mac buttons have strange insets, and only the top and bottom match the
                // actual border.
                pop.show(button, 0 + insets.top, button.getBounds().y + button.getBounds().height - insets.bottom - height);
                }
            else
                {
                int height = (int)pop.getPreferredSize().getHeight() + button.getBounds().height;
                pop.show(button, 0, button.getBounds().y + button.getBounds().height - height);
                }
            button.remove(pop);
            }
        else
            {
            perform();
            }
        }
    
    public void perform()
        {
        }
        
    public void perform(int i)
        {
        }
    
    
    /// The purpose of this method is to make a custom Mouse Adapter which underlines the text in the button
    /// when pressed as an additional cue that the button has been pressed due to the extremely muted button
    /// shade change in MacOS Monterey and Ventura.  I'd like to instead change the background color to something
    /// darker but this is very difficult to do in MacOS.
    
    MouseAdapter buildUnderliningMouseAdapter(final JButton button)
        {
        final AWTEventListener[] releaseListener = { null };
            
        return new MouseAdapter()
            {
            public void mouseExited(MouseEvent e)
                {
                button.setText("<html>"+getText().trim()+"</html>");
                repaint();
                }

            public void mouseEntered(MouseEvent e)
                {
                if (releaseListener[0] != null)
                    {
                    button.setText("<html><u>"+getText().trim()+"</u></html>");
                    repaint();
                    }
                }
                
            public void mousePressed(MouseEvent e) 
                {
                button.setText("<html><u>"+getText().trim()+"</u></html>");
                
                // This gunk fixes a BAD MISFEATURE in Java: mouseReleased isn't sent to the
                // same component that received mouseClicked.  What the ... ? Asinine.
                // So we create a global event listener which checks for mouseReleased and
                // calls our own private function.  EVERYONE is going to do this.
                                                        
                Toolkit.getDefaultToolkit().addAWTEventListener( releaseListener[0] = new AWTEventListener()
                    {
                    public void eventDispatched(AWTEvent evt)
                        {
                        if (evt instanceof MouseEvent && evt.getID() == MouseEvent.MOUSE_RELEASED)
                            {
                            MouseEvent e = (MouseEvent) evt;
                            if (releaseListener[0] != null)
                                {
                                Toolkit.getDefaultToolkit().removeAWTEventListener( releaseListener[0] );
                                releaseListener[0] = null;
                                button.setText("<html>"+getText().trim()+"</html>");
                                repaint();
                                }
                            }
                        }
                    }, AWTEvent.MOUSE_EVENT_MASK);
                }
                
            public void mouseReleased(MouseEvent e) 
                {
                Toolkit.getDefaultToolkit().removeAWTEventListener( releaseListener[0] );
                releaseListener[0] = null;
                button.setText("<html>"+getText().trim()+"</html>");
                repaint();
                }
            };
        }
    }
