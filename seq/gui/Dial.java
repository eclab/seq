/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.gui;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.border.*;
import javax.swing.*;
import java.awt.event.*;


/**
   A special module which presents itself as a GUI dial widget.  Changing the dial with your 
   mouse will change the output of the underlying Module.  You create the Dial first,
   then call getModule() to extract the module that it will control. 
*/


public abstract class Dial extends JPanel
    {
    private static final long serialVersionUID = 1;

    public static final int NO_DEFAULT = -1;
    public static final int DEFAULT = 0;
    // The dial width
    public static final int DIAL_WIDTH = 40;
    // The thickness of the thicker dial stroke.  The thinner dial stroke will be half this width.
    public static final float STROKE_WIDTH = 6.0f;
    // Ths thickness of the thinner dial stroke.
    public static final BasicStroke THIN_STROKE = new BasicStroke(STROKE_WIDTH / 2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
    // Ths thickness of the thicker dial stroke.
    public static final BasicStroke THICK_STROKE = new BasicStroke(STROKE_WIDTH, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
    // The color of the thin portion of the dial when using defaults
    public static final Color USES_DEFAULTS_THIN_COLOR = Color.GRAY;
    // The color of the thin portion of the dial
    public static final Color THIN_COLOR = Color.BLACK;
    // The color of the thick portion of the dial when being changed in real time
    public static final Color DYNAMIC_COLOR = Color.RED;
    // The default color of the thick portion of the dial when not being changed in real time
    public static final Color DEFAULT_STATIC_COLOR = Color.BLUE;
    // The default color of the defaults dot when on
    public static final Color DEFAULTS_ON_DOT_COLOR = DYNAMIC_COLOR;    //DEFAULT_STATIC_COLOR;
    // The default color of the defaults dot when off
    public static final Color DEFAULTS_OFF_DOT_COLOR = USES_DEFAULTS_THIN_COLOR;
    // The thickness of the defaults dot
    public static float DEFAULTS_DOT_THICKNESS = 4;
    // The label font
    public static final Font FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    // The leaky integrator amount used to gradually move the reported value towards the desired value indicated by the user 
    public static final int SCALE = 256;
    // The largest size the drop-down menu can be
    public static final int MAXIMUM_MENU_LENGTH = 24;

    // The color of the thick portion of the dial when not being changed
    Color staticColor = DEFAULT_STATIC_COLOR;
    // Is the user currently setting the dial?
    boolean dynamicallyChanging = false;
    // The value when the mouse was pressed 
    double startValue;
    // The Y mouse position when the mouse was pressed 
    int startY;
    // Is the mouse pressed?  This is part of a mechanism for dealing with
    // a stupidity in Java: if you PRESS in a widget, it'll be told. But if
    // you then drag elsewhere and RELEASE, the widget is never told.
    boolean mouseDown;
    // The Dial's title.  This is null until a LabelledDial is generated.
    JLabel title = null;
    // The Dial's displayed value.   This is null until a LabelledDial is generated.
    JLabel data = null;

    /** Returns the color of the thick portion of the dial when not being changed. */
    public Color getStaticColor() { return staticColor; }
    
    /** Sets the color of the thick portion of the dial when not being changed.  
        If null, then the color is set to DEFAULT_STATIC_COLOR. */
    public void setStaticColor(Color color) { if (color == null) staticColor = DEFAULT_STATIC_COLOR; else staticColor = color; }
    
    /** Returns the preferred size of the Dial */
    public Dimension getPreferredSize() { return new Dimension(DIAL_WIDTH, DIAL_WIDTH); }
    
    /** Sets the preferred size of the Dial */
    public Dimension getMinimumSize() { return new Dimension(DIAL_WIDTH, DIAL_WIDTH); }
        
    double currentVal;
    
    /** Updates the dial to a new value */
    public void update(double val, boolean changing) 
        { 
        if (val < 0) val = 0; 
        if (val > 1) val = 1; 
        currentVal = val;
        setValue(val, changing);

        if (data != null)
            data.setText(mapDefault(val));
        repaint();
        }

    public void redraw()
        {
        if (data != null)
            data.setText(mapDefault(getValue()));
        repaint();
        }
                                
    /** Returns the actual square within which the Dial's circle is drawn. */
    Rectangle getDrawSquare()
        {
        Insets insets = getInsets();
        Dimension size = getSize();
        int width = size.width - insets.left - insets.right;
        int height = size.height - insets.top - insets.bottom;
                
        // How big do we draw our circle?
        if (width > height)
            {
            // base it on height
            int h = height;
            int w = h;
            int y = insets.top;
            int x = insets.left + (width - w) / 2;
            return new Rectangle(x, y, w, h);
            }
        else
            {
            // base it on width
            int w = width;
            int h = w;
            int x = insets.left;
            int y = insets.top + (height - h) / 2;
            return new Rectangle(x, y, w, h);
            }
        }

                        
    // Fixes a bad bug where released isn't sent to the proper widget.  See below.
    AWTEventListener releaseListener = null;
    void mouseReleased(MouseEvent e)
        {          
        if (mouseDown)
            {
            mouseDown = false;
            dynamicallyChanging = false;
            repaint();
            if (releaseListener != null)
                Toolkit.getDefaultToolkit().removeAWTEventListener(releaseListener);
            }
        }
 
    /** Returns the Labelled, Titled version of this Dial. */
    public JPanel getLabelledTitledDial(String title, String maximumLabel)
        {
        JPanel panel = getLabelledDial(maximumLabel);
        this.title = new JLabel(title);
        this.title.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(this.title, BorderLayout.NORTH);
        return panel;
        }
        
    /** Returns the Labelled version of this Dial. */
    public JPanel getLabelledDial(String maximumLabel)
        {
        JPanel panel = new JPanel();
        JPanel subpanel = new JPanel();
        JPanel superpanel = new JPanel();
                        
        final JLabel temp = new JLabel(maximumLabel);
        
        data = new JLabel(mapDefault(getValue()))
            {
            public Dimension getPreferredSize() 
                {
                Dimension d = super.getPreferredSize();
                Dimension d2 = temp.getPreferredSize();
                d.width = d2.width;
                return d;
                }
            public Dimension getMinimumSize() { return getPreferredSize(); }
            public Dimension getMaximumSize() { return getPreferredSize(); }
            };
        data.setFont(FONT);
        data.setHorizontalAlignment(SwingConstants.CENTER);
        data.setVerticalAlignment(SwingConstants.CENTER);
    
        setLayout(new BorderLayout());
        add(data, BorderLayout.CENTER);
        
        panel.setLayout(new BorderLayout());
        panel.add(this, BorderLayout.CENTER);
        return panel;
        }
        
    /** Returns the current value */
    public abstract double getValue();
    
    /** Sets the current value.  The value doesn't have to change dynamically (if changing is TRUE), but when changing is FALSE,
        this is the final value and it must be set.   Either this or setValue(value) must be overridden but not both.   */
    public void setValue(double value, boolean changing)
        {
        setValue(value);
        }
    
    /** Sets the current value non-dynamically.  Either this or setValue(value, changing) must be overridden but not both.  */
    public void setValue(double value) { }
    
    String mapDefault(double value)
        { 
        if (getDefault() > NO_DEFAULT) 
            {
            return (getDefaultsList()[getDefault()]);
            }
        else return map(value);
        }
    
    /** Returns the String to be displayed for the given data value. The default version returns a value of the form x.xxxx */
    protected String map(double value)
        {
        return String.format("%.4f", value); 
        }
    
    /** Returns the String to be displayed for the given data value in the pop-up menu.  Normally this should be a floating-point value. */
    protected String mapMenu(double value)
        {
        return String.format("%.4f", value); 
        }
    
    /** Override this to be informed that a default has been set. 
        NO_DEFAULT (-1) will be passed in if the default has been cleared. */
    public void setDefault(int value) { }
    
    public int getDefault() { return NO_DEFAULT; }
                
    public String[] getDefaultsList() { return defaultsList; }
    public void setDefaultsList(String[] list) { defaultsList = list; }
    
    static final String[] BASIC_DEFAULTS_LIST = new String[] { "<html><i>Default</i></html>" };
    static final String[] EMPTY_DEFAULTS_LIST = new String[0];
    String[] defaultsList = EMPTY_DEFAULTS_LIST;
    boolean usesDefaults() { return defaultsList.length > 0; }
    double lastValue = 0;

    public Dial() { this(0.0); }

    public Dial(double initialValue) { this(initialValue, false); }

    public Dial(double initialValue, boolean usesDefaults)
        {
        this(initialValue, usesDefaults ? BASIC_DEFAULTS_LIST : EMPTY_DEFAULTS_LIST);
        }
                
    public Dial(double initialValue, int initialDefault)
        {
        this(initialValue, true);
        if (initialValue == -1) setDefault(getDefault());
        else if (initialDefault != NO_DEFAULT) setDefault(initialDefault);
        }
    
    public Dial(double initialValue, int initialDefault, String[] defaultsList)
        {
        this(initialValue, defaultsList);
        if (initialValue == -1) setDefault(getDefault());
        else if (initialDefault != NO_DEFAULT) setDefault(initialDefault);
        }
    
    public Dial(double initialValue, String[] defaultsList)
        {
        if (initialValue == -1) initialValue = getValue();
        this.defaultsList = defaultsList;
        currentVal = initialValue;
        
        addMouseListener(new MouseAdapter()
            {                        
            public void mousePressed(MouseEvent e)
                {                        
                if (usesDefaults() && getDefault() > NO_DEFAULT)
                    {
                    setDefault(NO_DEFAULT);
                    setValue(currentVal);           // reset to previous non-default value
                    }

                mouseDown = true;
                startY = e.getY();
                startValue = getValue();
                currentVal = startValue;                // just in case?  Not using defaults right now
                lastValue = startValue;
                dynamicallyChanging = true;
                update(lastValue, true);

                if (releaseListener != null)
                    Toolkit.getDefaultToolkit().removeAWTEventListener(releaseListener);

                // This gunk fixes a BAD MISFEATURE in Java: mouseReleased isn't sent to the
                // same component that received mouseClicked.  What the ... ? Asinine.
                // So we create a global event listener which checks for mouseReleased and
                // calls our own function.  EVERYONE is going to do this.
                                
                Toolkit.getDefaultToolkit().addAWTEventListener( releaseListener = new AWTEventListener()
                    {
                    public void eventDispatched(AWTEvent e)
                        {
                        if (e instanceof MouseEvent && e.getID() == MouseEvent.MOUSE_RELEASED)
                            {
                            Dial.this.mouseReleased((MouseEvent)e);
                            }
                        }
                    }, AWTEvent.MOUSE_EVENT_MASK);
                }
                        
            MouseEvent lastRelease;
            public void mouseReleased(MouseEvent e)
                {
                if (e == lastRelease) // we just had this event because we're in the AWT Event Listener.  So we ignore it
                    return;
                    
                dynamicallyChanging = false;
                if (releaseListener != null)
                    Toolkit.getDefaultToolkit().removeAWTEventListener(releaseListener);
                lastRelease = e;
                update(lastValue, false);
                }

            public void mouseClicked(MouseEvent e)
                {
                if (e.getClickCount() == 2 && usesDefaults())
                    { 
                    if (defaultsList.length == 1)
                        {
                        setDefault(0);
                        }
                    else
                        {
                        JPopupMenu pop = new JPopupMenu();
                        for(int i = 0; i < defaultsList.length; i++)
                            {
                            final int _i = i;
                            JMenuItem item = new JMenuItem(defaultsList[i]);
                            item.addActionListener(new ActionListener()
                                {
                                public void actionPerformed(ActionEvent e)      
                                    {
                                    setDefault(_i);
                                    redraw();
                                    }       
                                });     
                            pop.add(item);
                            }
                        pop.addSeparator();
                        JMenuItem amount = new JMenuItem(mapMenu(getValue())); 
                        int height = (int)pop.getPreferredSize().getHeight() + Dial.this.getBounds().height;
                        add(pop);
                        pop.show(Dial.this, 0 + Dial.this.getBounds().width, Dial.this.getBounds().y);
                        remove(pop);
                        } 
                    redraw();
                    }
                }
            });
                        
        addMouseMotionListener(new MouseMotionAdapter()
            {
            public void mouseDragged(MouseEvent e)
                {
                // Propose a value based on Y
                
                int py = e.getY();                                
                int y = -(py - startY);
                int min = 0;
                int max = 1;
                double range = (max - min);
                double multiplicand = SCALE / range;
                double proposedValue = startValue + y / multiplicand;
                if (proposedValue < 0) proposedValue = 0;
                if (proposedValue > 1) proposedValue = 1;
                            
                lastValue = proposedValue;
                update(proposedValue, true);
                }
            });

        if (getDefault() == NO_DEFAULT)
            {
            update(initialValue, false);
            }
        else            // just update the default
            {
            if (data != null)
                data.setText(mapDefault(initialValue));
            repaint();
            }
        }
        
        
    /** Returns the actual square within which the Dial's circle is drawn. */
    public void paintComponent(Graphics g)
        {
        Graphics2D graphics = (Graphics2D) g;
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

                
        Rectangle rect = getBounds();
        rect.x = 0;
        rect.y = 0;
        graphics.setPaint(new JLabel("").getBackground());
        graphics.fill(rect);
        rect = getDrawSquare();
        boolean def = getDefault() > NO_DEFAULT;

        graphics.setPaint(def ? USES_DEFAULTS_THIN_COLOR : THIN_COLOR);
        graphics.setStroke(THIN_STROKE);
        Arc2D.Double arc = new Arc2D.Double();
        
        double startAngle = 90 + (270 / 2);
        double interval = -270;
                
        arc.setArc(rect.getX() + STROKE_WIDTH / 2, rect.getY() + STROKE_WIDTH/2, rect.getWidth() - STROKE_WIDTH, rect.getHeight() - STROKE_WIDTH, startAngle, interval, Arc2D.OPEN);

        graphics.draw(arc);
        
        if (usesDefaults())
            {
            graphics.setPaint(def ? DEFAULTS_ON_DOT_COLOR : (getDefaultsList().length > 1 ? DEFAULT_STATIC_COLOR : DEFAULTS_OFF_DOT_COLOR));
            double x = rect.getX() + rect.getWidth() / 2.0;
            double y = rect.getY() + rect.getHeight() / 2.0;
            Ellipse2D.Double circle = new Ellipse2D.Double(x - DEFAULTS_DOT_THICKNESS / 2.0, y - DEFAULTS_DOT_THICKNESS / 2.0, DEFAULTS_DOT_THICKNESS, DEFAULTS_DOT_THICKNESS);
            graphics.fill(circle);
            }
        
        if (def) return;        
        
        graphics.setStroke(THICK_STROKE);
        arc = new Arc2D.Double();
                
        double value = (dynamicallyChanging ? lastValue : (currentVal = getValue()));           // update currentVal just in case.  Not using defaults right now.
        double min = 0;
        double max = 1;
        interval = -((value - min) / (double)(max - min) * 265) - 5;

        if (dynamicallyChanging)
            {
            graphics.setPaint(DYNAMIC_COLOR);
            if (value == min)
                {
                interval = -5;
                }
            }
        else
            {
            graphics.setPaint(getStaticColor());
            if (value == min)
                {
                interval = 0;
                }
            }

        arc.setArc(rect.getX() + STROKE_WIDTH / 2, rect.getY() + STROKE_WIDTH/2, rect.getWidth() - STROKE_WIDTH, rect.getHeight() - STROKE_WIDTH, startAngle, interval, Arc2D.OPEN);            
        graphics.draw(arc);
        }
        
    public static void main(String[] args)
        {
        final double[] d = { 0.5 };
        
        JFrame f = new JFrame();
        Dial dial = new Dial(d[0])
            {
            public double getValue() { return d[0]; }
            public void setValue(double val) { d[0] = val; }
            };
        JPanel lab = dial.getLabelledTitledDial("Hi Mom", "0.0000");
        f.add(lab);
        f.pack();
        f.show();
        }
    }
