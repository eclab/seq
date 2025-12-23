/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.automaton.gui;

import seq.motif.automaton.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.border.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.*;
import java.awt.font.*;
import seq.gui.Theme;

public abstract class InputOutput extends JPanel
    {
    /** Width of the set region in Dials etc.  Should be a multiple of 2, ideally 4*/
    public static float DIAL_STROKE_WIDTH() { return 4.0f; }
    /** Width of the dial **/
    public static int LABELLED_DIAL_WIDTH() { return 20; }
    /** Color of the set region in Dials etc. when being updated. */
    public static Color DIAL_DYNAMIC_COLOR() { return DYNAMIC_COLOR(); }
    /** The stroke for the set region in Dials etc. */
    public static BasicStroke DIAL_THIN_STROKE() { return new BasicStroke(DIAL_STROKE_WIDTH() / 2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL); }
    /** The stroke for the unset region in Dials etc. */
    public static BasicStroke DIAL_THICK_STROKE() { return new BasicStroke(DIAL_STROKE_WIDTH(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL); }
    public static Color DEFAULT_DYNAMIC_COLOR = Theme.isDark()? Theme.RED : Color.RED;
    static Color DYNAMIC_COLOR = DEFAULT_DYNAMIC_COLOR;
    public static Color DYNAMIC_COLOR() { return DYNAMIC_COLOR; }
    /** Small font, primarily for labels, button and combo box text. */
    public static Font SMALL_FONT() { return new Font(Font.SANS_SERIF, Font.PLAIN, isUnix() ? 9 : 10); }

    /** Updates the graphics rendering hints before drawing.  Called by a few widgets.  */
    public static void prepareGraphics(Graphics g)
        {
        Graphics2D graphics = (Graphics2D) g;
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        }

         
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

    public static boolean isUnix() 
        {
        return (OS().indexOf("nix") >= 0 || OS().indexOf("nux") >= 0 || OS().indexOf("aix") > 0 );
        }

    JLabel title;
    Jack jack;
    protected JComponent paintComponent;
    AutomatonButton button;
    
    // Which number output jack am I?
    protected int number;
    
    public AutomatonButton getButton() { return button; }
    public void setButton(AutomatonButton button) { this.button = button; }
    
    /** Returns the actual title JLabel */
    public JLabel getTitle() { return title; }
    
    /** Returns the text of the title JLabel */
    public String getTitleText() { return title.getText(); }
        
    /** Sets the text of the title JLabel.  If changeUnderlying = true, then this includes the underlying modulation/unit input/output name */
    public void setTitleText(String val)
        {
        title.setText(" " + val);
        }

    // Defines jacks for input/output modules
    class Jack extends JPanel
        {
        InputJack input;
        public Jack(InputJack in) { input = in; }
        public Jack() { input = null; }
        public InputJack getInput() { return input; }
        
        public Dimension getPreferredSize() { return new Dimension(LABELLED_DIAL_WIDTH(), LABELLED_DIAL_WIDTH()); }
        public Dimension getMinimumSize() { return new Dimension(LABELLED_DIAL_WIDTH(), LABELLED_DIAL_WIDTH()); }
                
        /** Returns the InputOutput which owns this Jack. */
        public InputOutput getParent() { return InputOutput.this; }
        
        /** Returns the actual square within which the Jack's circle is drawn. */
        public Rectangle getDrawSquare()
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
    
        /** Returns the standard fill color. */
        public Color getFillColor()
            {
            Color c = getDrawColor();
            return new Color(c.getRed(), c.getGreen(), c.getBlue(), 64);
            }
     
        /** Returns the standard draw color. */
        public Color getDrawColor()
            {
            return Color.BLACK;
            }
     
        public void paintComponent(Graphics g)
            {
            super.paintComponent(g);

            prepareGraphics(g);
                
            Graphics2D graphics = (Graphics2D) g;
                
            Rectangle rect = getBounds();
            rect.x = 0;
            rect.y = 0;
            graphics.setPaint(title.getBackground());
            graphics.fill(rect);
            rect = getDrawSquare();
            
            Ellipse2D.Double e = new Ellipse2D.Double(rect.getX() + DIAL_STROKE_WIDTH() / 2, rect.getY() + DIAL_STROKE_WIDTH()/2, rect.getWidth() - DIAL_STROKE_WIDTH(), rect.getHeight() - DIAL_STROKE_WIDTH());
            graphics.setPaint(getFillColor());
            graphics.fill(e);

            graphics.setPaint(getDrawColor());
            graphics.setStroke(DIAL_THIN_STROKE());
            graphics.draw(e);
            
            graphics.setPaint(getDrawColor());
            e.x += e.width / 2.5;
            e.y += e.height / 2.5;
            e.width -= e.width * 2.0 / 2.5;
            e.height -= e.height * 2.0 / 2.5;
            graphics.fill(e);
            }
        }


    }
