package seq.gui;

import java.awt.event.*;
import javax.swing.*;
import java.awt.*;

public class StringField extends JComponent
    {
    private static final long serialVersionUID = 1;

    JTextField valField = new JTextField();
    JLabel fieldLabel;
    String initialValue;
    protected String currentValue;

    Color defaultColor;
    Color editedColor = new Color(225,225,255);
        
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
                valField.setBackground(editedColor);
                }
            else
                {
                valField.setBackground(defaultColor);
                }
            }
        }
    
    public void setColumns(int val) { valField.setColumns(val); }
    public int getColumns() { return valField.getColumns(); }
    public void setSelectionStart(int val) { valField.setSelectionStart(val); }
    public void setSelectionEnd(int val) { valField.setSelectionEnd(val); }
    
    public void submit()
        {
        if (edited)
            {
            setValue(newValue(valField.getText()));
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
        public void keyPressed(KeyEvent keyEvent) {
            if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER)
                {
                submit();
                }
            else if (keyEvent.getKeyCode() == KeyEvent.VK_ESCAPE)  // reset
                {
                update();
                }
            else
                {
                setEdited(true);
                }
            }
        };
    
    FocusAdapter focusAdapter = new FocusAdapter()
        {
        public void focusLost ( FocusEvent e )
            {
            submit();
            }
        };

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
        
    public JTextField getField() { return valField; }
        
    public void setInitialValue(String initialValue)
        {
        this.initialValue = initialValue;
        setValue(initialValue);
        }
    
    public String getInitialValue() { return initialValue; }
    
    /** Creates a NumberTextField which does not display the belly button or arrows. */
    public StringField(String initialValue)
        {
        this(null,initialValue);
        }
    
    /** Creates a NumberTextField which does not display the belly button or arrows. */
    public StringField(String label, String initialValue)
        {
        defaultColor = valField.getBackground();

        this.initialValue = initialValue;        
        currentValue = initialValue;
        
        setLayout(new BorderLayout());

        if (label!=null && label.length() != 0)
            add(fieldLabel = new JLabel(label),BorderLayout.WEST);
        
        valField.addKeyListener(listener);
        valField.addFocusListener(focusAdapter);
        setValue(initialValue);
        add(valField,BorderLayout.CENTER);
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
