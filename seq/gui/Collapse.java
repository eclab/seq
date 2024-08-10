/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.gui;

import java.awt.*;
import javax.swing.*;

public class Collapse extends JPanel
    {
    public Collapse(JComponent component)
        {
        setLayout(new GridBagLayout());
        add(component, new GridBagConstraints());
        }
    }
