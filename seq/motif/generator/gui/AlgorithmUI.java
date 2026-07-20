/* 
   Copyright 2026 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.generator.gui;

import seq.motif.generator.*;
import seq.engine.*;
import seq.gui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.locks.*;

public class AlgorithmUI extends WidgetList
    {
    public Generator generator;
    public GeneratorUI generatorui;
    public Seq seq;
    public Algorithm algorithm;
    
    public AlgorithmUI(Seq seq, Generator generator, GeneratorUI generatorUI, Algorithm algorithm)
        {
        this.seq = seq;
        this.generator = generator;
        this.generatorui = generatorui;
        this.algorithm = algorithm;
        }
                
    public void revise()
        {
        }
    }
