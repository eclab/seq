/* 
   Copyright 2026 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.gui;

import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

public class StringArea extends JComponent
    {
    private static final long serialVersionUID = 1;

    JTextArea valField = new JTextArea();
    JScrollPane textScroll;
    JButton enterButton = new JButton("Enter");
    JButton revertButton = new JButton("Revert");
    JLabel fieldLabel;
    String initialValue;
    protected String currentValue;

    Color editedColor = Theme.isDark()? Theme.GRAY_40 : new Color(225,225,255);
        
    Border plainBorder = BorderFactory.createCompoundBorder(
        BorderFactory.createEmptyBorder(2, 2, 2, 2),
        BorderFactory.createLineBorder(Theme.GRAY_40)
        );
        
    Border editedBorder = BorderFactory.createCompoundBorder(
        BorderFactory.createEmptyBorder(2, 2, 2, 2),
        BorderFactory.createLineBorder(new Color(255, 128, 128))
        );

    public void setEditedColor(Color c) { editedColor = c; }
    public Color getEditedColor() { return editedColor; }

    boolean edited = false;
    void setEdited(boolean edited)
        {
        if (this.edited != edited)
            {
            this.edited = edited;
            if (edited)
                {
                textScroll.setBorder(editedBorder);
                }
            else
                {
                textScroll.setBorder(plainBorder);
                }
            }
        }
    
/*   
     public void setColumns(int val) { valField.setColumns(val); }
     public int getColumns() { return valField.getColumns(); }

*/
    public void setSelectionStart(int val) { valField.setSelectionStart(val); }
    public void setSelectionEnd(int val) { valField.setSelectionEnd(val); }
    
    public boolean verifyValue(String val)
        {
        return true;
        }
        
    public void submit()
        {
        if (edited)
            {
            if (verifyValue(valField.getText()))
                {
                setValue(newValue(valField.getText()));
                }
            }
        }
        
    public void update()
        {
        setValue(getValue());
        }

    KeyListener listener = new KeyListener()
        {
        public void keyReleased(KeyEvent keyEvent) { }
        public void keyTyped(KeyEvent keyEvent) { }
        public void keyPressed(KeyEvent keyEvent) 
            {
            setEdited(true);
            }
        };
    
/*
  FocusAdapter focusAdapter = new FocusAdapter()
  {
  public void focusLost ( FocusEvent e )
  {
  submit();
  }
  };
*/

    /** Sets the value without filtering first. */
    public void setValue(String val)
        {
        valField.setText("" + val);
        currentValue = val;
        setEdited(false);
        setSelectionStart(0);
        setSelectionEnd(0);
        }
    
    /** Returns the most recently set value. */
    public String getValue()
        {
        return currentValue;
        }
        
    public JTextArea getField() { return valField; }
        
    public void setInitialValue(String initialValue)
        {
        this.initialValue = initialValue;
        setValue(initialValue);
        }
    
    public String getInitialValue() { return initialValue; }
    
    /** Creates a StringArea. */
    public StringArea(String initialValue)
        {
        this(null, initialValue);
        }
    
    /** Creates a StringArea. */
    public StringArea(String label, String initialValue)
        {
        this.initialValue = initialValue;        
        currentValue = initialValue;
        
        setLayout(new BorderLayout());

        if (label!=null && label.length() != 0)
            add(fieldLabel = new JLabel(label),BorderLayout.WEST);
        
        valField.setRows(8);
        valField.addKeyListener(listener);
        valField.setLineWrap(true);
        valField.setWrapStyleWord(true);
        valField.setText(initialValue);
        textScroll = new JScrollPane(valField);
        /*
          textScroll = new JScrollPane(valField)
          {
          public Dimension getPreferredSize()
          {
          Dimension max = super.getMaximumSize();
          Dimension pref = super.getPreferredSize();
          return new Dimension(pref.width, max.height);
          }
          };
        */
//        textScroll.setMinimumSize(new Dimension(0, 22));
//        textScroll.setPreferredSize(textScroll.getMinimumSize());
//        valField.addFocusListener(focusAdapter);
        add(textScroll,BorderLayout.CENTER);
        Box box = new Box(BoxLayout.X_AXIS);
        box.add(enterButton);
        box.add(revertButton);
        box.add(box.createGlue());
        add(box, BorderLayout.NORTH);
        textScroll.setBorder(plainBorder);
        enterButton.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                submit();
                }
            });
        revertButton.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                update();
                }
            });
        setValue(initialValue);
        }
            
    /** Override this to be informed when a new value has been set.
        The return value should be the value you want the display to show 
        instead. */
    public String newValue(String newValue)
        {
        return newValue;
        }
    
    public void setToolTipText(String text)
        {
        super.setToolTipText(text);
        if (valField!=null) valField.setToolTipText(text);
        if (fieldLabel!=null) fieldLabel.setToolTipText(text);
        }
        
    public void setEnabled(boolean b)
        {
        if (valField!=null) valField.setEnabled(b);
        if (fieldLabel!=null) fieldLabel.setEnabled(b);
        }
        
    public String getText()
        {
        return valField.getText();
        }
    }
