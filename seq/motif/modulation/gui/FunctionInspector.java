/* 
   Copyright 2025 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.modulation.gui;

import seq.motif.modulation.*;
import seq.engine.*;
import seq.gui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.locks.*;

public class FunctionInspector extends JPanel
    {
    public static final String[] INSPECTOR_NAMES = { "None", "LFO", "Envelope", "Step Sequence", "Constant", "Same As", "<html><i>Copy From...</i></html>" };
    public static final int COPY_FROM = 5;
    public static final String[] MAP_FUNCTIONS = {"None (X)", "X^2", "X^4", "1-(1-X)^2", "1-(1-X)^4" };
    public static final String[] LFO_TYPES = {"Saw Up", "Saw Down", "Square", "Triangle", "Sine", "Random", "S&H" };
        
    Seq seq;
    SeqUI sequi;
    Modulation modulation;
    int index;
    SubInspector subinspector;
    SmallDial mapLow = null;
    SmallDial mapHigh = null;
    JComboBox mapBy = null;
    
    String[] defaults = new String[1 + Motif.NUM_PARAMETERS];
    public void buildDefaults(Motif parent)
        {
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
        }

    
    public FunctionInspector(Seq seq, SeqUI sequi, Modulation modulation, int index)
        {
        this.seq = seq;
        this.sequi = sequi;
        this.modulation = modulation;
        this.index = index;        
        
        buildDefaults(modulation);

        ReentrantLock lock = seq.getLock();
        String type = null;
        int typeIndex = 0;
        lock.lock();
        try
            {
            type = modulation.getFunction(index).getType();
            typeIndex = modulation.typeIndex(type);
            }
        finally { lock.unlock(); }
        subinspector = buildSubinspector(type, index);
        final int _typeIndex = typeIndex;

        JComboBox subcombo = new JComboBox(INSPECTOR_NAMES);
        subcombo.setSelectedIndex(typeIndex);
        subcombo.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                String type1 = null;
                if (seq == null) return;
                                
                if (subcombo.getSelectedIndex() == COPY_FROM)
                    {
                    String[] names = { "From" };
                    SmallDial from = new SmallDial(0)
                        {
                        double value;
                        public double getValue() { return value; }
                        public void setValue(double val) { value = val; }
                        public String map(double val) { return "" + (int)((val * Motif.NUM_PARAMETERS) + 1); }
                        };
                    JComponent[] components = new JComponent[] { from.getLabelledDial("8") };
                    int result = Dialogs.showMultiOption(sequi, names, components, new String[] { "Copy", "Cancel" }, 0, "Copy Argument", "Enter the Argument to copy from.");
                
                    int _from = index;  // copy myself
                    if (result == 0)
                        {
                        _from = (int)(from.getValue() * Motif.NUM_PARAMETERS);
                        subcombo.setSelectedIndex(modulation.typeIndex(modulation.getFunction(_from).getType()));
                        }       
                    else
                        {
                        // reset the menu
                        subcombo.setSelectedIndex(modulation.typeIndex(modulation.getFunction(_from).getType()));
                        return;
                        }
                                                
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try 
                        {
                        Modulation.Function func1 = modulation.getFunction(_from).copy();
                        type1 = func1.getType();
                        modulation.setFunction(index, func1);
                        }
                    finally { lock.unlock(); }
                    }
                else    
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try 
                        {
                        Modulation.Function func1 = modulation.buildFunction(subcombo.getSelectedIndex());
                        type1 = func1.getType();
                        modulation.setFunction(index, func1);                         
                        Clip clip = modulation.getPlayingClip();
                        if (clip != null && clip instanceof ModulationClip)
                        	{
                        	((ModulationClip) clip).buildNodes(modulation);
                        	}

                        }
                    finally { lock.unlock(); }
                    }
                                
                remove(subinspector);
                subinspector =  buildSubinspector(type1, index);
                add(subinspector, BorderLayout.SOUTH);
                revise();
                revalidate();
                repaint();
                }
            });
            
                        
        lock.lock();
        try 
            {            
            Modulation.Function func = (Modulation.Function)(modulation.getFunction(index));
            mapLow = new SmallDial(func.getMapLow(), defaults)
                {
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try 
                        { 
                        Modulation.Function func = (Modulation.Function)(modulation.getFunction(index));
                        return func.getMapLow(); 
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
                        Modulation.Function func = (Modulation.Function)(modulation.getFunction(index));
                        func.setMapLow(val); 
                        }
                    finally { lock.unlock(); }
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try 
                        { 
                        Modulation.Function func = (Modulation.Function)(modulation.getFunction(index));
                        if (val != SmallDial.NO_DEFAULT) func.setMapLow(-(val + 1)); 
                        }
                    finally { lock.unlock(); }
                    }
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try 
                        { 
                        Modulation.Function func = (Modulation.Function)(modulation.getFunction(index));
                        double val = func.getMapLow(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); 
                        }
                    finally { lock.unlock(); }
                    }
                };

            mapHigh = new SmallDial(func.getMapHigh(), defaults)
                {
                public double getValue() 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try 
                        { 
                        Modulation.Function func = (Modulation.Function)(modulation.getFunction(index));
                        return func.getMapHigh(); 
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
                        Modulation.Function func = (Modulation.Function)(modulation.getFunction(index));
                        func.setMapHigh(val); 
                        }
                    finally { lock.unlock(); }
                    }
                public void setDefault(int val) 
                    { 
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try 
                        { 
                        Modulation.Function func = (Modulation.Function)(modulation.getFunction(index));
                        if (val != SmallDial.NO_DEFAULT) func.setMapHigh(-(val + 1)); 
                        }
                    finally { lock.unlock(); }
                    }
                public int getDefault()
                    {
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try 
                        { 
                        Modulation.Function func = (Modulation.Function)(modulation.getFunction(index));
                        double val = func.getMapHigh(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); 
                        }
                    finally { lock.unlock(); }
                    }
                };

            mapBy = new JComboBox(MAP_FUNCTIONS);
            mapBy.setSelectedIndex(func.getMapBy());
            mapBy.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    if (seq == null) return;
                    ReentrantLock lock = seq.getLock();
                    lock.lock();
                    try 
                        { 
                        Modulation.Function func = (Modulation.Function)(modulation.getFunction(index));
                        func.setMapBy(mapBy.getSelectedIndex()); 
                        }
                    finally { lock.unlock(); }
                    }
                });      
            }
        finally 
            {
            lock.unlock(); 
            }

        WidgetList map = new WidgetList(new String[] { "", "Low", "High", "By", },
            new JComponent[] 
                {
                null,
                mapLow.getLabelledDial("0.0000"),
                mapHigh.getLabelledDial("0.0000"),
                mapBy,
                });
                
        setLayout(new BorderLayout());
        add(subcombo, BorderLayout.NORTH);
        add(map, BorderLayout.CENTER);
        map.setBorder(BorderFactory.createTitledBorder("Map"));
        add(subinspector, BorderLayout.SOUTH);
        setBorder(BorderFactory.createTitledBorder("Argument " + (index + 1)));
        subcombo.setToolTipText(STAGE_TYPE_TOOLTIP);
        }
                
    public void revise()                        
        {
        Seq old = seq;
        seq = null;
        ReentrantLock lock = old.getLock();
        lock.lock();
        try 
            { 
            Modulation.Function func = (Modulation.Function)(modulation.getFunction(index));
            mapBy.setSelectedIndex(func.getMapBy()); 
            }
        finally { lock.unlock(); }                              
        seq = old;
        if (mapLow != null) mapLow.redraw();
        if (mapHigh != null) mapHigh.redraw();

        subinspector.revise();
        }
        
    
    public class SubInspector extends WidgetList
        {
        public void revise() { }
        public String getName() { return "None"; }
        }
    
    public class LFOInspector extends SubInspector
        {
        JComboBox lfoType;
        TimeDisplay period;
        SmallDial phase;
        SmallDial initial;
        TimeDisplay start;
        TimeDisplay fadeIn;
        TimeDisplay length;
        TimeDisplay fadeOut;

        public LFOInspector()
            {
            ReentrantLock lock = seq.getLock();
            lock.lock();
            
            try 
                {
                Modulation.LFO func = (Modulation.LFO)(modulation.getFunction(index));
                        
                lfoType = new JComboBox(LFO_TYPES);
                lfoType.setSelectedIndex(func.getLFOType());
                lfoType.addActionListener(new ActionListener()
                    {
                    public void actionPerformed(ActionEvent e)
                        {
                        if (seq == null) return;
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { func.setLFOType(lfoType.getSelectedIndex()); }
                        finally { lock.unlock(); }
                        }
                    });      

                period = new TimeDisplay(func.getPeriod(), seq)              // default is one quarter note
                    {
                    public int getTime()
                        {
                        return func.getPeriod();
                        }
                        
                    public void setTime(int time)
                        {
                        func.setPeriod(time);
                        }
                    };
                period.setDisplaysTime(false);

                start = new TimeDisplay(func.getStart(), seq)
                    {
                    public int getTime()
                        {
                        return func.getStart();
                        }
                        
                    public void setTime(int time)
                        {
                        func.setStart(time);
                        }
                    };
                start.setDisplaysTime(true);

                length = new TimeDisplay(func.getLength(), seq)             // default is really long
                    {
                    public int getTime()
                        {
                        return func.getLength();
                        }
                        
                    public void setTime(int time)
                        {
                        func.setLength(time);
                        }
                    };
                length.setDisplaysTime(false);

                fadeIn = new TimeDisplay(func.getFadeIn(), seq)
                    {
                    public int getTime()
                        {
                        return func.getFadeIn();
                        }
                        
                    public void setTime(int time)
                        {
                        func.setFadeIn(time);
                        }
                    };
                fadeIn.setDisplaysTime(false);

                fadeOut = new TimeDisplay(func.getFadeOut(), seq)
                    {
                    public int getTime()
                        {
                        return func.getFadeOut();
                        }
                        
                    public void setTime(int time)
                        {
                        func.setFadeOut(time);
                        }
                    };
                fadeOut.setDisplaysTime(false);


                phase = new SmallDial(func.getPhase(), defaults)
                    {
                    public double getValue() 
                        { 
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { return func.getPhase(); }
                        finally { lock.unlock(); }
                        }
                    public void setValue(double val) 
                        { 
                        if (seq == null) return;
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { func.setPhase(val); }
                        finally { lock.unlock(); }
                        }
                    public void setDefault(int val) 
                        { 
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { if (val != SmallDial.NO_DEFAULT) func.setPhase(-(val + 1)); }
                        finally { lock.unlock(); }
                        }
                    public int getDefault()
                        {
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { double val = func.getPhase(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                        finally { lock.unlock(); }
                        }
                    };

                initial = new SmallDial(func.getInitial(), defaults)
                    {
                    public double getValue() 
                        { 
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { return func.getInitial(); }
                        finally { lock.unlock(); }
                        }
                    public void setValue(double val) 
                        { 
                        if (seq == null) return;
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { func.setInitial(val); }
                        finally { lock.unlock(); }
                        }
                    public void setDefault(int val) 
                        { 
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { if (val != SmallDial.NO_DEFAULT) func.setInitial(-(val + 1)); }
                        finally { lock.unlock(); }
                        }
                    public int getDefault()
                        {
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { double val = func.getInitial(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                        finally { lock.unlock(); }
                        }
                    };
                }
            finally 
                {
                lock.unlock(); 
                }

            build(new String[] { "", "Type", "Period", "Phase/Var", "Initial", "Start", "Fade In", "Length", "Fade Out" }, 
                new JComponent[] 
                    {
                    null,
                    lfoType,
                    period,
                    phase.getLabelledDial("0.0000"),
                    initial.getLabelledDial("0.0000"),
                    start,
                    fadeIn,
                    length,
                    fadeOut
                    });

            }
                        
        public void revise() 
            {
            Seq old = seq;
            seq = null;
            ReentrantLock lock = old.getLock();
            lock.lock();
            try 
                { 
                Modulation.LFO func = (Modulation.LFO)(modulation.getFunction(index));

                lfoType.setSelectedIndex(func.getLFOType()); 
                }
            finally { lock.unlock(); }                              
            seq = old;
            if (phase != null) phase.redraw();
            if (initial != null) initial.redraw();                        
            if (period != null) period.revise();
            if (start != null) start.revise();
            if (fadeIn != null) fadeIn.revise();
            if (length != null) length.revise();
            if (fadeOut != null) fadeOut.revise();
            }

        public String getName() { return "LFO"; }
        }




    public class EnvelopeInspector extends SubInspector
        {
        SmallDial initial;
        TimeDisplay start;
        SmallDial numStages;
        JCheckBox repeat;
        JCheckBox hold;
        TimeDisplay[] time = new TimeDisplay[Modulation.MAX_STAGES];
        SmallDial[] target = new SmallDial[Modulation.MAX_STAGES];

        public EnvelopeInspector()
            {
            ReentrantLock lock = seq.getLock();
            lock.lock();
            
            try 
                {
                Modulation.Envelope func = (Modulation.Envelope)(modulation.getFunction(index));
                        
                start = new TimeDisplay(func.getStart(), seq)
                    {
                    public int getTime()
                        {
                        return func.getStart();
                        }
                        
                    public void setTime(int time)
                        {
                        func.setStart(time);
                        }
                    };
                start.setDisplaysTime(true);

                initial = new SmallDial(func.getInitial(), defaults)
                    {
                    public double getValue() 
                        { 
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { return func.getInitial(); }
                        finally { lock.unlock(); }
                        }
                    public void setValue(double val) 
                        { 
                        if (seq == null) return;
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { func.setInitial(val); }
                        finally { lock.unlock(); }
                        }
                    public void setDefault(int val) 
                        { 
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { if (val != SmallDial.NO_DEFAULT) func.setInitial(-(val + 1)); }
                        finally { lock.unlock(); }
                        }
                    public int getDefault()
                        {
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { double val = func.getInitial(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                        finally { lock.unlock(); }
                        }
                    };

                repeat = new JCheckBox();
                repeat.setSelected(func.getRepeat());
                repeat.addActionListener(new ActionListener()
                    {
                    public void actionPerformed(ActionEvent e)
                        {
                        if (seq == null) return;
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { func.setRepeat(repeat.isSelected()); }
                        finally { lock.unlock(); }                              
                        }
                    });

                hold = new JCheckBox();
                hold.setSelected(func.getHold());
                hold.addActionListener(new ActionListener()
                    {
                    public void actionPerformed(ActionEvent e)
                        {
                        if (seq == null) return;
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { func.setHold(hold.isSelected()); }
                        finally { lock.unlock(); }                              
                        }
                    });

                numStages = new SmallDial(func.getNumStages() / (double) Modulation.MAX_STAGES, defaults)
                    {
                    public String map(double val)
                        {
                        return String.valueOf((int)(val * Modulation.MAX_STAGES));
                        }
                    public double getValue() 
                        { 
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { return func.getNumStages() / (double) Modulation.MAX_STAGES; }
                        finally { lock.unlock(); }
                        }
                    public void setValue(double val) 
                        { 
                        if (seq == null) return;
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { func.setNumStages((int)(val * Modulation.MAX_STAGES)); }
                        finally { lock.unlock(); }
                        }
                    public void setDefault(int val) 
                        { 
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { if (val != SmallDial.NO_DEFAULT) func.setNumStages(-(val + 1)); }
                        finally { lock.unlock(); }
                        }
                    public int getDefault()
                        {
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { double val = func.getNumStages(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                        finally { lock.unlock(); }
                        }
                    };
                
                for(int i = 0; i < Modulation.MAX_STAGES; i++)
                    {
                    final int _i = i;
                    time[i] = new TimeDisplay(func.getTime(i), seq)
                        {
                        public int getTime()
                            {
                            return func.getTime(_i);
                            }
                        
                        public void setTime(int time)
                            {
                            func.setTime(_i, time);
                            }
                        };
                    time[i].setDisplaysTime(false);

                    target[i] = new SmallDial(func.getTarget(i), defaults)
                        {
                        public double getValue() 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { return func.getTarget(_i); }
                            finally { lock.unlock(); }
                            }
                        public void setValue(double val) 
                            { 
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { func.setTarget(_i, val); }
                            finally { lock.unlock(); }
                            }
                        public void setDefault(int val) 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { if (val != SmallDial.NO_DEFAULT) func.setTarget(_i, -(val + 1)); }
                            finally { lock.unlock(); }
                            }
                        public int getDefault()
                            {
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { double val = func.getTarget(_i); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                            finally { lock.unlock(); }
                            }
                        };
                    }
                
                
                }
            finally 
                {
                lock.unlock(); 
                }


            build(new String[] { "", "Initial", "Start", "Repeat", "Hold", "Stages", 
                    "Time 1", "Target 1", "Time 2", "Target 2", "Time 3", "Target 3", "Time 4", "Target 4", 
                    "Time 5", "Target 5", "Time 6", "Target 6", "Time 7", "Target 7", "Time 8", "Target 8", }, 
                new JComponent[] 
                    {
                    null,
                    initial.getLabelledDial("0.0000"),
                    start,
                    repeat,
                    hold,
                    numStages.getLabelledDial("8"),
                    time[0],
                    target[0].getLabelledDial("0.0000"),
                    time[1],
                    target[1].getLabelledDial("0.0000"),
                    time[2],
                    target[2].getLabelledDial("0.0000"),
                    time[3],
                    target[3].getLabelledDial("0.0000"),
                    time[4],
                    target[4].getLabelledDial("0.0000"),
                    time[5],
                    target[5].getLabelledDial("0.0000"),
                    time[6],
                    target[6].getLabelledDial("0.0000"),
                    time[7],
                    target[7].getLabelledDial("0.0000"),
                    });

            }
                        
        public void revise() 
            {
            Seq old = seq;
            seq = null;
            ReentrantLock lock = old.getLock();
            lock.lock();
            try 
                { 
                Modulation.Envelope func = (Modulation.Envelope)(modulation.getFunction(index));

                hold.setSelected(func.getHold()); 
                repeat.setSelected(func.getRepeat()); 
                }
            finally { lock.unlock(); }                              
            seq = old;
            if (start != null) start.revise();                        
            if (initial != null) initial.redraw();                        
            if (numStages != null) numStages.redraw();      
            for(int i = 0; i < Modulation.MAX_STAGES; i++)
                {
                time[i].revise();
                target[i].redraw();
                }         
            }

        public String getName() { return "Envelope"; }
        }



    public class StepInspector extends SubInspector
        {
        SmallDial initial;
        TimeDisplay start;
        TimeDisplay period;
        SmallDial numSteps;
        JCheckBox repeat;
        JCheckBox trigger;
        SmallDial[] step = new SmallDial[Modulation.MAX_STEPS];

        public StepInspector()
            {
            ReentrantLock lock = seq.getLock();
            lock.lock();
            
            try 
                {
                Modulation.Step func = (Modulation.Step)(modulation.getFunction(index));
                        
                start = new TimeDisplay(func.getStart(), seq)
                    {
                    public int getTime()
                        {
                        return func.getStart();
                        }
                        
                    public void setTime(int time)
                        {
                        func.setStart(time);
                        }
                    };
                start.setDisplaysTime(true);

                period = new TimeDisplay(func.getPeriod(), seq)
                    {
                    public int getTime()
                        {
                        return func.getPeriod();
                        }
                        
                    public void setTime(int time)
                        {
                        func.setPeriod(time);
                        }
                    };
                period.setDisplaysTime(false);

                initial = new SmallDial(func.getInitial(), defaults)
                    {
                    public double getValue() 
                        { 
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { return func.getInitial(); }
                        finally { lock.unlock(); }
                        }
                    public void setValue(double val) 
                        { 
                        if (seq == null) return;
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { func.setInitial(val); }
                        finally { lock.unlock(); }
                        }
                    public void setDefault(int val) 
                        { 
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { if (val != SmallDial.NO_DEFAULT) func.setInitial(-(val + 1)); }
                        finally { lock.unlock(); }
                        }
                    public int getDefault()
                        {
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { double val = func.getInitial(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                        finally { lock.unlock(); }
                        }
                    };

                repeat = new JCheckBox();
                repeat.setSelected(func.getRepeat());
                repeat.addActionListener(new ActionListener()
                    {
                    public void actionPerformed(ActionEvent e)
                        {
                        if (seq == null) return;
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { func.setRepeat(repeat.isSelected()); }
                        finally { lock.unlock(); }                              
                        }
                    });

                trigger = new JCheckBox();
                trigger.setSelected(func.getTrigger());
                trigger.addActionListener(new ActionListener()
                    {
                    public void actionPerformed(ActionEvent e)
                        {
                        if (seq == null) return;
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { func.setTrigger(trigger.isSelected()); }
                        finally { lock.unlock(); }                              
                        }
                    });

                numSteps = new SmallDial(func.getNumSteps() / (double) Modulation.MAX_STEPS, defaults)
                    {
                    public String map(double val)
                        {
                        return String.valueOf((int)(val * Modulation.MAX_STEPS));
                        }
                    public double getValue() 
                        { 
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { return func.getNumSteps() / (double) Modulation.MAX_STEPS; }
                        finally { lock.unlock(); }
                        }
                    public void setValue(double val) 
                        { 
                        if (seq == null) return;
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { func.setNumSteps((int)(val * Modulation.MAX_STEPS)); }
                        finally { lock.unlock(); }
                        }
                    public void setDefault(int val) 
                        { 
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { if (val != SmallDial.NO_DEFAULT) func.setNumSteps(-(val + 1)); }
                        finally { lock.unlock(); }
                        }
                    public int getDefault()
                        {
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { double val = func.getNumSteps(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                        finally { lock.unlock(); }
                        }
                    };
                
                for(int i = 0; i < Modulation.MAX_STEPS; i++)
                    {
                    final int _i = i;
                    step[i] = new SmallDial(func.getStep(i), defaults)
                        {
                        public double getValue() 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { return func.getStep(_i); }
                            finally { lock.unlock(); }
                            }
                        public void setValue(double val) 
                            { 
                            if (seq == null) return;
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { func.setStep(_i, val); }
                            finally { lock.unlock(); }
                            }
                        public void setDefault(int val) 
                            { 
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { if (val != SmallDial.NO_DEFAULT) func.setStep(_i, -(val + 1)); }
                            finally { lock.unlock(); }
                            }
                        public int getDefault()
                            {
                            ReentrantLock lock = seq.getLock();
                            lock.lock();
                            try { double val = func.getStep(_i); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                            finally { lock.unlock(); }
                            }
                        };
                    }
                
                
                }
            finally 
                {
                lock.unlock(); 
                }


            build(new String[] { "", "Initial", "Start", "Step Length", "Repeat", "Trigger", "Steps", 
                    "Step 1", "Step 2", "Step 3", "Step 4", 
                    "Step 5", "Step 6", "Step 7", "Step 8", 
                    "Step 9", "Step 10", "Step 11", "Step 12", 
                    "Step 13", "Step 14", "Step 15", "Step 16", 
                    }, 
                new JComponent[] 
                    {
                    null,
                    initial.getLabelledDial("0.0000"),
                    start,
                    period,
                    repeat,
                    trigger,
                    numSteps.getLabelledDial("8"),
                    step[0].getLabelledDial("0.0000"),
                    step[1].getLabelledDial("0.0000"),
                    step[2].getLabelledDial("0.0000"),
                    step[3].getLabelledDial("0.0000"),
                    step[4].getLabelledDial("0.0000"),
                    step[5].getLabelledDial("0.0000"),
                    step[6].getLabelledDial("0.0000"),
                    step[7].getLabelledDial("0.0000"),
                    step[8].getLabelledDial("0.0000"),
                    step[9].getLabelledDial("0.0000"),
                    step[10].getLabelledDial("0.0000"),
                    step[11].getLabelledDial("0.0000"),
                    step[12].getLabelledDial("0.0000"),
                    step[13].getLabelledDial("0.0000"),
                    step[14].getLabelledDial("0.0000"),
                    step[15].getLabelledDial("0.0000"),
                    });

            }
                        
        public void revise() 
            {
            Seq old = seq;
            seq = null;
            ReentrantLock lock = old.getLock();
            lock.lock();
            try 
                { 
                Modulation.Step func = (Modulation.Step)(modulation.getFunction(index));

                trigger.setSelected(func.getTrigger()); 
                repeat.setSelected(func.getRepeat()); 
                }
            finally { lock.unlock(); }                              
            seq = old;
            if (initial != null) initial.redraw();                        
            if (numSteps != null) numSteps.redraw();      
            if (period != null) period.revise();      
            if (start != null) start.revise();      
            for(int i = 0; i < Modulation.MAX_STEPS; i++)
                {
                step[i].redraw();
                }         
            }

        public String getName() { return "Step"; }
        }



    public class SameInspector extends SubInspector
        {
        SmallDial as;

        public SameInspector(final int index)
            {
            ReentrantLock lock = seq.getLock();
            lock.lock();
            
            try 
                {
                Modulation.Same func = (Modulation.Same)(modulation.getFunction(index));
                        
                as = new SmallDial(func.getAs() / (Motif.NUM_PARAMETERS - 2), defaults)
                    {
                    public String map(double val)
                        {
                        int _as = (int)(val * (Motif.NUM_PARAMETERS - 2));                      // 0 ... 6
                        if (_as < index) return String.valueOf(_as + 1);
                        else return "Invalid (" + (_as + 1) + ")";
                        }
                    public double getValue() 
                        { 
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { return func.getAs() / (Motif.NUM_PARAMETERS - 2); }
                        finally { lock.unlock(); }
                        }
                    public void setValue(double val) 
                        { 
                        if (seq == null) return;
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { func.setAs((int)(val * (Motif.NUM_PARAMETERS - 2))); }
                        finally { lock.unlock(); }
                        }
                    public void setDefault(int val) 
                        { 
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { if (val != SmallDial.NO_DEFAULT) func.setAs(-(val + 1)); }
                        finally { lock.unlock(); }
                        }
                    public int getDefault()
                        {
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { double val = func.getAs(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                        finally { lock.unlock(); }
                        }
                    };                
                }
            finally 
                {
                lock.unlock(); 
                }


            build(new String[] { "", "Same As", }, 
                new JComponent[] 
                    {
                    null,
                    as.getLabelledDial("Invalid (7)"),
                    });

            }

        public void revise() 
            {
            Seq old = seq;
            seq = null;
            ReentrantLock lock = old.getLock();
            lock.lock();
            try 
                { 
                // Modulation.Same func = (Modulation.Same)(modulation.getFunction(index));
                }
            finally { lock.unlock(); }                              
            seq = old;
            if (as != null) as.redraw();                        
            }

        public String getName() { return "Same"; }
        }

                        
    public class ConstantInspector extends SubInspector
        {
        SmallDial value;

        public ConstantInspector()
            {
            ReentrantLock lock = seq.getLock();
            lock.lock();
            
            try 
                {
                Modulation.Constant func = (Modulation.Constant)(modulation.getFunction(index));
                        
                value = new SmallDial(func.getValue(), defaults)
                    {
                    public double getValue() 
                        { 
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { return func.getValue(); }
                        finally { lock.unlock(); }
                        }
                    public void setValue(double val) 
                        { 
                        if (seq == null) return;
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { func.setValue(val); }
                        finally { lock.unlock(); }
                        }
                    public void setDefault(int val) 
                        { 
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { if (val != SmallDial.NO_DEFAULT) func.setValue(-(val + 1)); }
                        finally { lock.unlock(); }
                        }
                    public int getDefault()
                        {
                        ReentrantLock lock = seq.getLock();
                        lock.lock();
                        try { double val = func.getValue(); return (val < 0 ? -(int)(val + 1) : SmallDial.NO_DEFAULT); }
                        finally { lock.unlock(); }
                        }
                    };                
                }
            finally 
                {
                lock.unlock(); 
                }


            build(new String[] { "", "Value", }, 
                new JComponent[] 
                    {
                    null,
                    value.getLabelledDial("0.0000"),
                    });
        
            }

        public void revise() 
            {
            Seq old = seq;
            seq = null;
            ReentrantLock lock = old.getLock();
            lock.lock();
            try 
                { 
                // Modulation.Same func = (Modulation.Same)(modulation.getFunction(index));
                }
            finally { lock.unlock(); }                              
            seq = old;
            if (value != null) value.redraw();                        
            }

        public String getName() { return "Constant"; }
        }





    public SubInspector buildSubinspector(String type, int index)
        {
        if (type.equals(Modulation.IDENTITY))
            {
            return new SubInspector();
            }
        else if (type.equals(Modulation.LFO))
            {
            return new LFOInspector();
            }
        else if (type.equals(Modulation.ENVELOPE))
            {
            return new EnvelopeInspector();
            }
        else if (type.equals(Modulation.STEP))
            {
            return new StepInspector();
            }
        else if (type.equals(Modulation.CONSTANT))
            {
            return new ConstantInspector();
            }
        else if (type.equals(Modulation.SAME))
            {
            return new SameInspector(index);
            }
        else // uh...
            {
            return new SubInspector();
            }
        }

    // This is to align to top on flow layout in the ModulationUI
    // See https://stackoverflow.com/questions/2743177/top-alignment-for-flowlayout
    public Component.BaselineResizeBehavior getBaselineResizeBehavior() 
        {
        return Component.BaselineResizeBehavior.CONSTANT_ASCENT;
        }

    public int getBaseline(int width, int height) 
        {
        return 0;
        }


    static final String STAGE_TYPE_TOOLTIP = "<html><b>Stage Type</b><br>" +
        "Sets the stage function type, at present one of:" + 
        "<ul>" +
        "<li><b>None</b>&nbsp;&nbsp;No modulation (but the parameter is still mapped)." +
        "<li><b>LFO</b>&nbsp;&nbsp;A low-frequency oscillator." +
        "<li><b>Envelope</b>&nbsp;&nbsp;An up to eight-stage envelope generator with repeats." +
        "<li><b>Step Sequence</b>&nbsp;&nbsp;An up to 16-stage step sequence with repeats." +
        "<li><b>Constant</b>&nbsp;&nbsp;A constant value (the parmaeter is still mapped, though that's useless)." +
        "<li><b>Same As</b>&nbsp;&nbsp;The same as another modulation argument, but with possibly different mapping." +
        "<li><b><i>Copy From</i></b>&nbsp;&nbsp;Copy from another argument." +
        "</ul></html>";

    static final String NOTE_OUT_TOOLTIP = "<html><b>Out</b><br>" +
        "Changes the output device of the incoming MIDI notes.</html>";

    static final String NOTE_ALL_OUT_TOOLTIP = "<html><b>All Out</b><br>" +
        "Sets whether <b>Out</b> sets the output device for all incoming events, not just MIDI notes.</html>";
        
    static final String NOTE_TRANSPOSE_TOOLTIP = "<html><b>Transpose</b><br>" +
        "Transposes the pitch of the incoming MIDI notes.</html>";

    static final String NOTE_TRANSPOSE_VARIANCE_TOOLTIP = "<html><b>Transpose Variance</b><br>" +
        "Sets the variance of random noise with which to transpose the pitch of the incoming MIDI notes.</html>";

    static final String NOTE_GAIN_TOOLTIP = "<html><b>Gain</b><br>" +
        "Sets the gain to be multipled against the velocity (volume) of the incoming MIDI notes.</html>";

    static final String NOTE_GAIN_VARIANCE_TOOLTIP = "<html><b>Gain Variance</b><br>" +
        "Sets the variance of random noise to be multipled against the velocity (volume) of<br>" +
        "the incoming MIDI notes.</html>";

    static final String NOTE_RELEASE_TOOLTIP = "<html><b>Release</b><br>" +
        "Sets the gain to be multipled against the release velocity of the incoming MIDI notes.</html>";

    static final String NOTE_RELEASE_VARIANCE_TOOLTIP = "<html><b>Release Variance</b><br>" +
        "Sets the variance of random noise to be multipled against the release velocity of<br>" +
        "the incoming MIDI notes.</html>";

    static final String NOTE_LENGTH_TOOLTIP = "<html><b>Length</b><br>" +
        "Sets the length of all incoming MIDI notes, if <br>Change Length<b> is checked.<br><br>" +
        "Note that MIDI notes cannot realistically have zero length: it will be essentially<br>" +
        "1 Part per Quarter Note.</html>";

    static final String NOTE_CHANGE_LENGTH_TOOLTIP = "<html><b>Change Length</b><br>" +
        "Sets the whether the length of notes will be changed by <b>Length</b>.</html>";

    static final String DELAY_ORIGINAL_TOOLTIP = "<html><b>Original</b><br>" +
        "When checked, the original MIDI note will be played (along with possible delayed versions).</html>";

    static final String DELAY_INTERVAL_TOOLTIP = "<html><b>Interval</b><br>" +
        "Sets the time interval between successive delayed, repeated notes.</html>";

    static final String DELAY_CUT_TOOLTIP = "<html><b>Cut</b><br>" +
        "Sets the amount of reduction in velocity (volume) of the next delayed note relative to<br>" +
        "the current one.</html>";
        
    static final String DELAY_NUM_DELAYS_TOOLTIP = "<html><b>Num Delays</b><br>" +
        "Sets the number of delayed versions of a note to play.</html>";
        
    static final String DROP_CUT_TOOLTIP = "<html><b>Delete All</b><br>" +
        "If checked, then all events (note and non-note) will be deleted entirely.</html>";
        
    static final String DROP_PROBABILITY_TOOLTIP = "<html><b>Note Probability</b><br>" +
        "Sets the probability that notes (not non-note events) will be deleted.<br><br>" +
        "If <b>Cut</b> is checked, then Probability does nothing.</html>";
        
    static final String PARAMETER_TYPE_TOOLTIP = "<html><b>Type</b><br>" +
        "Specifies the type of parameter to be modified.</html>";

    static final String PARAMETER_PARAM_MSB_TOOLTIP = "<html><b>Param/MSB</b><br>" +
        "Sets the parameter number of the parameter to be modified:" +
        "<ul>" +
        "<li><b>Control Change (CC)</b>&nbsp;&nbsp; Sets the CC parameter number (0-127)." +
        "<li><b>Non-Registered Parameter Numbers (NRPN)</b>&nbsp;&nbsp; Sets the Most Significant Byte<br>" +
        "(MSB) of the parameter number." +
        "<li><b>Registered Parameter Numbers (RPN)</b>&nbsp;&nbsp; Sets the Most Significant Byte<br>" +
        "(MSB) of the parameter number." +
        "<li><b>All Others</b>&nbsp;&nbsp; Has no effect (there is no parameter number)." +
        "</ul>" +
        "</html>";

    static final String PARAMETER_PARAM_LSB_TOOLTIP = "<html><b>LSB</b><br>" +
        "Sets the Least Significant Byte (LSB) of the parameter number to be modified. This is only<br>" +
        "relevant for <b>Non-Registered Parameter Numbers (NRPN)</b> and <b>Registered Parameter Numbers (RPN)</b></html>";
        
    static final String PARAMETER_VARIANCE_TOOLTIP = "<html><b>Variance</b><br>" +
        "Sets the variance of random noise to be added to the parameter value for incoming events.<br><br>" + 
        "Though random noise is added to all values, it only changes every once in a while, according to the <b>Rate</b>.</html>";
        
    static final String PARAMETER_RATE_TOOLTIP = "<html><b>Rate</b><br>" +
        "Sets how often a new random value is chosen to be added to the parameter value for incoming events.<br><br>" + 
        "Though random noise is added to all values, it only changes every once in a while, according to the <b>Rate</b>.</html>";

    }
