/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.series.gui;

import seq.motif.series.*;
import seq.engine.*;
import seq.gui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.locks.*;

public class SeriesInspector extends WidgetList
    {
    Seq seq;
    Series series;
    SeriesUI seriesui;

    StringField name;
    JComboBox mode;
    TimeDisplay rate;

    public static final String[] MODE_STRINGS = { "Series", "Shuffle", "Random", "Random Markov", "Round Robin", "Variation", "Rand Variation" };         // , "Round Robin Shared" };
    public static final String[] TYPE_STRINGS = { "None", "CC", "14-Bit CC", "NRPN", "NRPN Coarse", "RPN", "Bend", "Aftertouch" };

    public SeriesInspector(Seq seq, Series series, SeriesUI seriesui)
        {
        this.seq = seq;
        this.series = series;
        this.seriesui = seriesui;

        ReentrantLock lock = seq.getLock();
        lock.lock();
        try
            {
            name = new StringField(series.getName())
                {
                public String newValue(String newValue)
                    {
                    newValue = newValue.trim();
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { series.setName(newValue); }
                    finally { lock.unlock(); }
                    seriesui.updateText();
                    return newValue;
                    }
                                
                public String getValue() 
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { return series.getName(); }
                    finally { lock.unlock(); }
                    }
                };
            name.setColumns(MotifUI.INSPECTOR_NAME_DEFAULT_SIZE);
            name.setToolTipText(NAME_TOOLTIP);
            mode = new JComboBox(MODE_STRINGS);
            mode.setSelectedIndex(series.getMode());
            mode.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try { series.setMode(mode.getSelectedIndex()); }
                    finally { lock.unlock(); }                              
                    }
                });
            mode.setToolTipText(MODE_TOOLTIP);
            }
        finally { lock.unlock(); }

        JPanel result = build(new String[] { "Name", "Mode" }, 
            new JComponent[] 
                {
                name,
                mode,
                });
        remove(result);
        add(result, BorderLayout.NORTH);               // re-add it as center


        final JComboBox[] types = new JComboBox[Series.NUM_PARAMETERS];
        final Box[] paramsBox = new Box[Series.NUM_PARAMETERS];
        final SmallDial[] params = new SmallDial[Series.NUM_PARAMETERS];
        final SmallDial[] paramsMSB = new SmallDial[Series.NUM_PARAMETERS];
        final SmallDial[] paramsLSB = new SmallDial[Series.NUM_PARAMETERS];
        final JComponent[] paramsL = new JComponent[Series.NUM_PARAMETERS];
        final JComponent[] paramsMSBL = new JComponent[Series.NUM_PARAMETERS];
        final JComponent[] paramsLSBL = new JComponent[Series.NUM_PARAMETERS];

        lock.lock();
        try
            {
            for(int i = 0; i < Series.NUM_PARAMETERS; i++)
                {
                final int _i = i;

                double initialVal = series.getMIDIType(_i) == Series.CC7 ? series.getMIDIParameter(i) / 127.0 :
                    series.getMIDIType(_i) == Series.CC14 ? series.getMIDIParameter(i) / 31.0 : 0;
                params[_i] = new SmallDial(initialVal)
                    {
                    public double getValue() 
                        { 
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        int param = series.getMIDIParameter(_i);
                        try 
                            {
                            switch(series.getMIDIType(_i))
                                {
                                case Series.CC7:
                                {
                                return param / 127.0;
                                }
                                case Series.CC14:
                                {
                                return param / 31.0;
                                }
                                default:
                                {
                                return 0;                       // should not happen
                                }
                                }
                            }
                        finally { lock.unlock(); }
                        }
                        
                    public void setValue(double val) 
                        { 
                        if (seq == null) return;
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try 
                            { 
                            switch(series.getMIDIType(_i))
                                {
                                case Series.CC7:
                                {
                                series.setMIDIParameter(_i, (int)(val * 127.0));
                                break;
                                }
                                case Series.CC14:
                                {
                                series.setMIDIParameter(_i, (int)(val * 31.0));
                                break;
                                }
                                default:
                                {
                                break;
                                }
                                }
                            }
                        finally { lock.unlock(); }
                        }
                        
                    protected String map(double val)
                        {
                        switch(series.getMIDIType(_i))
                            {
                            case Series.CC7:
                            {
                            return "" + (int)series.getMIDIParameter(_i);
                            }
                            case Series.CC14:
                            {
                            return "" + (int)series.getMIDIParameter(_i);
                            }
                            default:
                            {
                            return "Foo";                   // should not happen
                            }
                            }
                        }
                    };              //.getLabelledDial("127");
                paramsL[_i] = params[_i].getLabelledDial("127");
                        
                initialVal = series.getMIDIType(_i) == Series.NRPN || series.getMIDIType(_i) == Series.NRPN_COARSE || series.getMIDIType(_i) == Series.RPN ? 
                    (series.getMIDIParameter(i) / 128 ) / 127.0 : 0;
                paramsMSB[_i] = new SmallDial(initialVal)
                    {
                    public double getValue() 
                        { 
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        int param = series.getMIDIParameter(_i);
                        try 
                            {
                            switch(series.getMIDIType(_i))
                                {
                                case Series.NRPN:
                                case Series.NRPN_COARSE:
                                case Series.RPN:
                                {
                                return (param / 128) / 127.0;
                                }
                                default:
                                {
                                return 0;                       // should not happen
                                }
                                }
                            }
                        finally { lock.unlock(); }
                        }
                        
                    public void setValue(double val) 
                        { 
                        if (seq == null) return;
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try 
                            { 
                            switch(series.getMIDIType(_i))
                                {
                                case Series.NRPN:
                                case Series.NRPN_COARSE:
                                case Series.RPN:
                                {
                                int lsb = series.getMIDIParameter(_i) % 128;
                                series.setMIDIParameter(_i, ((int)(val * 127.0)) * 128 + lsb);
                                if (((SmallDial)paramsLSB[_i]) != null) ((SmallDial)paramsLSB[_i]).redraw();
                                break;
                                }
                                default:
                                {
                                break;
                                }
                                }
                            }
                        finally { lock.unlock(); }
                        }
                        
                    protected String map(double val)
                        {
                        return "";
                        }
                    };      //.getLabelledDial("Yo");
                paramsMSBL[_i] = paramsMSB[_i].getLabelledDial("");


                initialVal = series.getMIDIType(_i) == Series.NRPN || series.getMIDIType(_i) == Series.NRPN_COARSE || series.getMIDIType(_i) == Series.RPN ? 
                    (series.getMIDIParameter(i) % 128 ) / 127.0 : 0;
                paramsLSB[_i] = new SmallDial(initialVal)
                    {
                    public double getValue() 
                        { 
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        int param = series.getMIDIParameter(_i);
                        try 
                            {
                            switch(series.getMIDIType(_i))
                                {
                                case Series.NRPN:
                                case Series.NRPN_COARSE:
                                case Series.RPN:
                                {
                                return (param % 128) / 127.0;
                                }
                                default:
                                {
                                return 0;                       // should not happen
                                }
                                }
                            }
                        finally { lock.unlock(); }
                        }
                        
                    public void setValue(double val) 
                        { 
                        if (seq == null) return;
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try 
                            { 
                            switch(series.getMIDIType(_i))
                                {
                                case Series.NRPN:
                                case Series.NRPN_COARSE:
                                case Series.RPN:
                                {
                                int msb = series.getMIDIParameter(_i) / 128;
                                series.setMIDIParameter(_i, ((int)(val * 127.0)) + msb * 128);
                                break;
                                }
                                default:
                                {
                                break;
                                }
                                }
                            }
                        finally { lock.unlock(); }
                        }
                        
                    protected String map(double val)
                        {
                        switch(series.getMIDIType(_i))
                            {
                            case Series.NRPN:
                            case Series.NRPN_COARSE:
                            case Series.RPN:
                            {
                            return "" + series.getMIDIParameter(_i);
                            }
                            default:
                            {
                            return "Bar";
                            }
                            }
                        }
                    };              //.getLabelledDial("00000");
                paramsLSBL[_i] = paramsLSB[_i].getLabelledDial("00000");
                        
                types[i] = new JComboBox(TYPE_STRINGS);
                types[i].setSelectedIndex(series.getMIDIType(i));
                types[i].addActionListener(new ActionListener()
                    {
                    public void actionPerformed(ActionEvent e)
                        {
                        if (seq == null) return;
                        int type =  types[_i].getSelectedIndex();
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try 
                            { 
                            series.setMIDIType(_i, type); 
                            }
                        finally { lock.unlock(); }       
                                           
                        paramsBox[_i].remove(paramsMSBL[_i]);
                        paramsBox[_i].remove(paramsLSBL[_i]);
                        paramsBox[_i].remove(paramsL[_i]);
                        if (type == Series.CC7 || type == Series.CC14) { paramsBox[_i].add(paramsL[_i]); if (params[_i] != null) params[_i].redraw(); }
                        else if (type == Series.NRPN || type == Series.NRPN_COARSE || type == Series.RPN) 
                            { 
                            paramsBox[_i].add(paramsMSBL[_i]);
                            if (paramsMSB[_i] != null) paramsMSB[_i].redraw(); 
                            paramsBox[_i].add(paramsLSBL[_i]);  
                            if (paramsLSB[_i] != null) paramsLSB[_i].redraw(); 
                            }
                        paramsBox[_i].revalidate();
                        seriesui.revalidate();                            
                        }
                    });
                
                paramsBox[_i] = new Box(BoxLayout.X_AXIS);
                paramsBox[_i].add(new JLabel(" "));                                 // spacer
                int type =  types[_i].getSelectedIndex();
                if (type == Series.CC7 || type == Series.CC14) { paramsBox[_i].add(paramsL[_i]); if (params[_i] != null) params[_i].redraw(); }
                else if (type == Series.NRPN || type == Series.NRPN_COARSE || type == Series.RPN) 
                    { 
                    paramsBox[_i].add(paramsMSBL[_i]);
                    if (paramsMSB[_i] != null) paramsMSB[_i].redraw(); 
                    paramsBox[_i].add(paramsLSBL[_i]);  
                    if (paramsLSB[_i] != null) paramsLSB[_i].redraw(); 
                    }
                }

            rate = new TimeDisplay(series.getMIDIParameterRate(), seq)
                {
                public int getTime()
                    {
                    return series.getMIDIParameterRate();
                    }
                                        
                public void setTime(int time)
                    {
                    series.setMIDIParameterRate(time);
                    }
                };
            rate.setDisplaysTime(false);
            }
        finally { lock.unlock(); }

        Out[] seqOuts = seq.getOuts();
        String[] outs = new String[seqOuts.length];
        for(int i = 0; i < seqOuts.length; i++)
            {
            // We have to make these strings unique, or else the combo box doesn't give the right selected index, Java bug
            outs[i] = "" + (i) + ": " + seqOuts[i].toString();
            }
                        
        JComboBox out = new JComboBox(outs);
        out.setMaximumRowCount(outs.length);
        out.setSelectedIndex(series.getMIDIParameterOut());
        out.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                if (seq == null) return;
                ReentrantLock lock = seq.getLock();
                lock.lock();
                try { series.setMIDIParameterOut(out.getSelectedIndex()); }
                finally { lock.unlock(); }
                }
            });            


        JComponent[] components = new JComponent[Series.NUM_PARAMETERS + 2];
        String[] labels = new String[Series.NUM_PARAMETERS + 2];
        labels[0] = "Rate";
        components[0] = rate;
        labels[1] = "Out";
        components[1] = out;
        for(int i = 2; i < labels.length; i++) 
            {
            labels[i] = "Param " + String.valueOf(i - 1);
            JPanel comp2 = new JPanel();
            comp2.setLayout(new BorderLayout());
            JPanel comp = new JPanel();
            comp.setLayout(new BorderLayout());
            comp.add(types[i-2], BorderLayout.WEST);
            comp.add(paramsBox[i-2], BorderLayout.CENTER);
            comp2.add(comp, BorderLayout.WEST);
            comp2.add(new JPanel(), BorderLayout.CENTER);
            components[i] = comp2;
            }
            
        WidgetList cc = new WidgetList(labels, components);
        cc.makeBorder("MIDI Parameters");
        DisclosurePanel midiParameters = new DisclosurePanel("MIDI Parameters", cc);
        midiParameters.setParentComponent(seriesui);
        add(midiParameters, BorderLayout.CENTER);
        add(new DefaultParameterList(seq, seriesui), BorderLayout.SOUTH);
        seriesui.revalidate();                            
        }
                
    public void revise()
        {
        Seq old = seq;
        seq = null;
        ReentrantLock lock = old.getLock();
        lock.lock();
        try 
            { 
            mode.setSelectedIndex(series.getMode()); 
            }
        finally { lock.unlock(); }                              
        seq = old;
        name.update();
        rate.revise();
        }

    static final String NAME_TOOLTIP = "<html><b>Name</b><br>" +
        "Sets the name of the Series.  This will appear in the Motif List at left.</html>";

    static final String MODE_TOOLTIP = "<html><b>Mode</b><br>" +
        "The Series's operational mode: how it decides the order of playing its children.  One of:" +
        "<ul>" +
        "<li><b>Series</b> Each child is played in order." + 
        "<li><b>Shuffle</b> The children order is shuffled, and then each child is played in order." + 
        "<li><b>Random</b> A child is picked at random to play.  When it is done, another child is<br>" + 
        "picked at random to play, and so on forever."+
        "<li><b>Random Markov</b> A child is picked at random to play.  When it is done, another child is<br>" + 
        "picked at random to play, using the distribution from the <b>Markov Weights</b> of the previous<br>" +
        "child, and so on forever.  Note that different children can have different Markov Weight<br>" +
        "distributions.  A child's outgoing Markov Weight distributions are set in its Inspector below." +
        "<li><b>Round Robin</b> The first child is played, and then the Series terminates.  When the series<br>" +
        "is next played, the second child is then played, and then the Series terinates again, and so on,<br>" +
        "ultimately wrapping around to the first child again." +
        "<li><b>Variation</b> The child is specified by Parameter 1.  When it finishes, the Series terminates." +
        "<li><b>Random Variation</b> The child is specified by the <b>Random Parameter</b>.  When it finishes,<br>" +
        "the Series terminates."+
        "</ul>" +
        "Each child may repeat multiple times before it is finished playing, as determined by its<br>" +
        "<b>Fixed Repeats</b> and <b>Repeat Probability</b> settings in its inspector.</html>";
    }
