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
    public static final int COPY_FROM = 6;
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
                        public String map(double val) { return "" + (int)((val * (Motif.NUM_PARAMETERS - 1)) + 1); }
                        };
                    JComponent[] components = new JComponent[] { from.getLabelledDial("8") };
                    int result = Dialogs.showMultiOption(sequi, names, components, new String[] { "Copy", "Cancel" }, 0, "Copy Argument", "Enter the Argument to copy from.");
                
                    int _from = index;  // copy myself
                    if (result == 0)
                        {
                        _from = (int)(from.getValue() * (Motif.NUM_PARAMETERS - 1));
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
            mapLow.setToolTipText(MAP_LOW_TOOLTIP);

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
            mapHigh.setToolTipText(MAP_HIGH_TOOLTIP);

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
            mapBy.setToolTipText(MAP_BY_TOOLTIP);
            
            }
        finally 
            {
            lock.unlock(); 
            }

        WidgetList map = new WidgetList(new String[] {"Low", "High", "By", },
            new JComponent[] 
                {
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
                lfoType.setToolTipText(LFO_TYPE_TOOLTIP);   

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
                period.setToolTipText(LFO_PERIOD_TOOLTIP);

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
                start.setToolTipText(LFO_START_TOOLTIP);

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
                length.setToolTipText(LFO_LENGTH_TOOLTIP);

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
                fadeIn.setToolTipText(LFO_FADE_IN_TOOLTIP);

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
                fadeOut.setToolTipText(LFO_FADE_OUT_TOOLTIP);

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
                phase.setToolTipText(LFO_PHASE_VAR_TOOLTIP);

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
                initial.setToolTipText(LFO_INTIAL_TOOLTIP);
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
                start.setToolTipText(ENVELOPE_START_TOOLTIP);

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
                initial.setToolTipText(ENVELOPE_INITIAL_TOOLTIP);

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
                repeat.setToolTipText(ENVELOPE_REPEAT_TOOLTIP);

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
                hold.setToolTipText(ENVELOPE_HOLD_TOOLTIP);

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
                numStages.setToolTipText(ENVELOPE_STAGES_TOOLTIP);
                
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
                    time[i].setToolTipText(ENVELOPE_TIME_TOOLTIP);

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
                    target[i].setToolTipText(ENVELOPE_TARGET_TOOLTIP);
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
                start.setToolTipText(STEP_SEQUENCE_START_TOOLTIP);

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
                period.setToolTipText(STEP_SEQUENCE_STEP_LENGTH_TOOLTIP);

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
                initial.setToolTipText(STEP_SEQUENCE_INITIAL_TOOLTIP);

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
                repeat.setToolTipText(STEP_SEQUENCE_REPEAT_TOOLTIP);

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
                trigger.setToolTipText(STEP_SEQUENCE_TRIGGER_TOOLTIP);

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
                numSteps.setToolTipText(STEP_SEQUENCE_NUM_STEPS_TOOLTIP);
                
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
                    step[i].setToolTipText(STEP_SEQUENCE_STEP_TOOLTIP);
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
                as.setToolTipText(SAME_AS_TOOLTIP);           
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
                value.setToolTipText(CONSTANT_VALUE_TOOLTIP);       
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

    static final String MAP_LOW_TOOLTIP = "<html><b>Map Low</b><br>" +
        "The low boundary of the mapping function range.<br><br>" + 
        "After the modulation function has produced a value, in the range from 0.0 to 1.0,<br>" +
        "it is then mapped into the range from <b>Low</b> to <b>High</b>.  0.0 is mapped to Low, 1.0 is<br>" +
        "mapped to High, and the in-between values are interpolated between Low and High<br>" +
        "accordingly.  Thus if Low is higher than High, the modulation function will be<br>" +
        "inverted.  Furthermore, the mapping function is also warped by <b>By</b>, which<br>" +
        "keeps it as-is or warps it into a curve.</html>";

    static final String MAP_HIGH_TOOLTIP = "<html><b>Map High</b><br>" +
        "The high boundary of the mapping function range.<br><br>" + 
        "After the modulation function has produced a value, in the range from 0.0 to 1.0,<br>" +
        "it is then mapped into the range from <b>Low</b> to <b>High</b>.  0.0 is mapped to Low, 1.0 is<br>" +
        "mapped to High, and the in-between values are interpolated between Low and High<br>" +
        "accordingly.  Thus if Low is higher than High, the modulation function will be<br>" +
        "inverted.  Furthermore, the mapping function is also warped by <b>By</b>, which<br>" +
        "keeps it as-is or warps it into a curve.</html>";

    static final String MAP_BY_TOOLTIP = "<html><b>Map By</b><br>" +
        "The warping of the mapping function, one of:" +
        "<ul>" + 
        "<li><b>None (X)</b>&nbsp;&nbsp;The function is not warped." +
        "<li><b>X^2</b>&nbsp;&nbsp;The modulation value X, from 0.0 to 1.0, is squared." +
        "<li><b>X^4</b>&nbsp;&nbsp;The modulation value X, from 0.0 to 1.0, is raised to the fourth power.<br>This is similar to an exponential dropoff." +
        "<li><b>1-(1-X)^2</b>&nbsp;&nbsp;The modulation value X, from 0.0 to 1.0, is inverted, then squared,<br>then inverted again. This is a bulging-outward curve, as opposed to X^2, which bulges inward." +
        "<li><b>1-(1-X)^4</b>&nbsp;&nbsp;The modulation value X, from 0.0 to 1.0, is inverted, then squared,<br>then inverted again. This is a bulging-outward curve, as opposed to X^4, which bulges inward.<br>It is similar to an exponential rise." +
        "</ul>" + 
        "After the modulation function has produced a value, in the range from 0.0 to 1.0,<br>" +
        "it is then mapped into the range from <b>Low</b> to <b>High</b>.  0.0 is mapped to Low, 1.0 is<br>" +
        "mapped to High, and the in-between values are interpolated between Low and High<br>" +
        "accordingly.  Thus if Low is higher than High, the modulation function will be<br>" +
        "inverted.  Furthermore, the mapping function is also warped by <b>By</b>, which<br>" +
        "keeps it as-is or warps it into a curve.</html>";

    static final String LFO_TYPE_TOOLTIP = "<html><b>Type</b><br>" +
        "Sets the LFO waveshape, one of: Sawtooth Rising (Ramp), Sawtooth Falling, Square, Triangle, Sine,<br>" +
        "Random, and Random Sample and Hold (S&H).<br><br>" +
        "A Random LFO selects a new random target value each period, then during the period slowly moves<br>" +
        "towards that target value until it reaches it at the end of the period.<br><br>" +
        "A Random Sample and Hold LFO selects a new random target value each period, but during the period<br>" +
        "holds its existing value, never changing, until it reaches the end of the period, at which time<br>" +
        "it jumps to the new target value and starts holding there.</html>";

    static final String LFO_PERIOD_TOOLTIP = "<html><b>Period</b><br>" +
        "Sets the period (the wave length) of the LFO wave.</html>";
        
    static final String LFO_PHASE_VAR_TOOLTIP = "<html><b>Phase / Variance</b><br>" +
        "Sets the <i>phase</i> of the LFO wave -- how offset it is in time -- or if the wave shape is Random<br>" +
        "or Random Sample and Hold, sets the <i>variance</i> -- how far the random target value is permitted to<br>" +
        "deviate from 0.5.</html>";

    static final String LFO_INTIAL_TOOLTIP = "<html><b>Initial</b><br>" +
        "Sets the initial value of the LFO.<br><br>" +
        "The LFO wave function begins by just outputting the <b>Initial</b> value, with no oscillation.<br>" +
        "Then at the <b>Start</b> time it begins to slowly grow until it reaches full size<br>" +
        "(from 0.0 to 1.0) after <b>Fade In</b> timesteps.  It then continues at full size for<br>" +
        "<b>Length</b> timesteps.  It then fades back out to the Initial value over the course of " +
        "<b>Fade Out</b><br> timesteps, then stays at the Initial value thereafter.</html>";

    static final String LFO_START_TOOLTIP = "<html><b>Start</b><br>" +
        "Sets the start time of the LFO.<br><br>" +
        "The LFO wave function begins by just outputting the <b>Initial</b> value, with no oscillation.<br>" +
        "Then at the <b>Start</b> time it begins to slowly grow until it reaches full size<br>" +
        "(from 0.0 to 1.0) after <b>Fade In</b> timesteps.  It then continues at full size for<br>" +
        "<b>Length</b> timesteps.  It then fades back out to the Initial value over the course of " +
        "<b>Fade Out</b><br> timesteps, then stays at the Initial value thereafter.</html>";

    static final String LFO_FADE_IN_TOOLTIP = "<html><b>Fade In</b><br>" +
        "Sets the Fade In interval length of the LFO.<br><br>" +
        "The LFO wave function begins by just outputting the <b>Initial</b> value, with no oscillation.<br>" +
        "Then at the <b>Start</b> time it begins to slowly grow until it reaches full size<br>" +
        "(from 0.0 to 1.0) after <b>Fade In</b> timesteps.  It then continues at full size for<br>" +
        "<b>Length</b> timesteps.  It then fades back out to the Initial value over the course of " +
        "<b>Fade Out</b><br> timesteps, then stays at the Initial value thereafter.</html>";

    static final String LFO_LENGTH_TOOLTIP = "<html><b>Length</b><br>" +
        "Sets the Length of the LFO.<br><br>" +
        "The LFO wave function begins by just outputting the <b>Initial</b> value, with no oscillation.<br>" +
        "Then at the <b>Start</b> time it begins to slowly grow until it reaches full size<br>" +
        "(from 0.0 to 1.0) after <b>Fade In</b> timesteps.  It then continues at full size for<br>" +
        "<b>Length</b> timesteps.  It then fades back out to the Initial value over the course of " +
        "<b>Fade Out</b><br> timesteps, then stays at the Initial value thereafter.</html>";

    static final String LFO_FADE_OUT_TOOLTIP = "<html><b>Fade Out</b><br>" +
        "Sets the Fade Out interval length of the LFO.<br><br>" +
        "The LFO wave function begins by just outputting the <b>Initial</b> value, with no oscillation.<br>" +
        "Then at the <b>Start</b> time it begins to slowly grow until it reaches full size<br>" +
        "(from 0.0 to 1.0) after <b>Fade In</b> timesteps.  It then continues at full size for<br>" +
        "<b>Length</b> timesteps.  It then fades back out to the Initial value over the course of " +
        "<b>Fade Out</b><br> timesteps, then stays at the Initial value thereafter.</html>";

    static final String ENVELOPE_INITIAL_TOOLTIP = "<html><b>Initial</b><br>" +
        "Sets the Initial value of the Envelope.<br><br>" + 
        "The Envelope function begins by just outputting the <b>Initial</b> value, until it reaches<br>" +
        "the <b>Start</b> time. Then it moves towards the first <b>Target</b> over the course of the<br>" +
        "first <b>Time</b> interval.  When at the end of that interval, it has reached the target: this is<br>" +
        "one <b>Stage</b>.  It then begins to move towards the second target over the second time interval,<br>" +
        "and so on, thus finishing the second <b>Stage</b>.  It continues like this until it has finished all<br>" +
        "the <b>Stages</b> specified.  If <b>Repeat</b> is turned on, then when it has finished all its<br>" +
        "stages, it will loop back and continue with the first stage again, and so on.  If <b>Hold</b> is<br>" +
        "turned on, instead of gradually moving towards its chosen target, it will hold at its current value<br>" +
        "until it reaches the Time, and then jump immediately to the chosen target, similar to a Sample and Hold.</html>";
        
    static final String ENVELOPE_START_TOOLTIP = "<html><b>Start</b><br>" +
        "Sets the Start time of the Envelope.<br><br>" + 
        "The Envelope function begins by just outputting the <b>Initial</b> value, until it reaches<br>" +
        "the <b>Start</b> time. Then it moves towards the first <b>Target</b> over the course of the<br>" +
        "first <b>Time</b> interval.  When at the end of that interval, it has reached the target: this is<br>" +
        "one <b>Stage</b>.  It then begins to move towards the second target over the second time interval,<br>" +
        "and so on, thus finishing the second <b>Stage</b>.  It continues like this until it has finished all<br>" +
        "the <b>Stages</b> specified.  If <b>Repeat</b> is turned on, then when it has finished all its<br>" +
        "stages, it will loop back and continue with the first stage again, and so on.  If <b>Hold</b> is<br>" +
        "turned on, instead of gradually moving towards its chosen target, it will hold at its current value<br>" +
        "until it reaches the Time, and then jump immediately to the chosen target, similar to a Sample and Hold.</html>";

    static final String ENVELOPE_REPEAT_TOOLTIP = "<html><b>Repeat</b><br>" +
        "Turns on the the Repeat feature of the Envelope.<br><br>" + 
        "The Envelope function begins by just outputting the <b>Initial</b> value, until it reaches<br>" +
        "the <b>Start</b> time. Then it moves towards the first <b>Target</b> over the course of the<br>" +
        "first <b>Time</b> interval.  When at the end of that interval, it has reached the target: this is<br>" +
        "one <b>Stage</b>.  It then begins to move towards the second target over the second time interval,<br>" +
        "and so on, thus finishing the second <b>Stage</b>.  It continues like this until it has finished all<br>" +
        "the <b>Stages</b> specified.  If <b>Repeat</b> is turned on, then when it has finished all its<br>" +
        "stages, it will loop back and continue with the first stage again, and so on.  If <b>Hold</b> is<br>" +
        "turned on, instead of gradually moving towards its chosen target, it will hold at its current value<br>" +
        "until it reaches the Time, and then jump immediately to the chosen target, similar to a Sample and Hold.</html>";

    static final String ENVELOPE_HOLD_TOOLTIP = "<html><b>Hold</b><br>" +
        "Turns on the the Hold feature of the Envelope.<br><br>" + 
        "The Envelope function begins by just outputting the <b>Initial</b> value, until it reaches<br>" +
        "the <b>Start</b> time. Then it moves towards the first <b>Target</b> over the course of the<br>" +
        "first <b>Time</b> interval.  When at the end of that interval, it has reached the target: this is<br>" +
        "one <b>Stage</b>.  It then begins to move towards the second target over the second time interval,<br>" +
        "and so on, thus finishing the second <b>Stage</b>.  It continues like this until it has finished all<br>" +
        "the <b>Stages</b> specified.  If <b>Repeat</b> is turned on, then when it has finished all its<br>" +
        "stages, it will loop back and continue with the first stage again, and so on.  If <b>Hold</b> is<br>" +
        "turned on, instead of gradually moving towards its chosen target, it will hold at its current value<br>" +
        "until it reaches the Time, and then jump immediately to the chosen target, similar to a Sample and Hold.</html>";

    static final String ENVELOPE_STAGES_TOOLTIP = "<html><b>Stages</b><br>" +
        "Set the number of Stages of the Envelope.  Later stages are ignored.<br><br>" + 
        "The Envelope function begins by just outputting the <b>Initial</b> value, until it reaches<br>" +
        "the <b>Start</b> time. Then it moves towards the first <b>Target</b> over the course of the<br>" +
        "first <b>Time</b> interval.  When at the end of that interval, it has reached the target: this is<br>" +
        "one <b>Stage</b>.  It then begins to move towards the second target over the second time interval,<br>" +
        "and so on, thus finishing the second <b>Stage</b>.  It continues like this until it has finished all<br>" +
        "the <b>Stages</b> specified.  If <b>Repeat</b> is turned on, then when it has finished all its<br>" +
        "stages, it will loop back and continue with the first stage again, and so on.  If <b>Hold</b> is<br>" +
        "turned on, instead of gradually moving towards its chosen target, it will hold at its current value<br>" +
        "until it reaches the Time, and then jump immediately to the chosen target, similar to a Sample and Hold.</html>";
        
    static final String ENVELOPE_TIME_TOOLTIP = "<html><b>Time</b><br>" +
        "Sets the Time Interval (length) of this stage.<br><br>" + 
        "The Envelope function begins by just outputting the <b>Initial</b> value, until it reaches<br>" +
        "the <b>Start</b> time. Then it moves towards the first <b>Target</b> over the course of the<br>" +
        "first <b>Time</b> interval.  When at the end of that interval, it has reached the target: this is<br>" +
        "one <b>Stage</b>.  It then begins to move towards the second target over the second time interval,<br>" +
        "and so on, thus finishing the second <b>Stage</b>.  It continues like this until it has finished all<br>" +
        "the <b>Stages</b> specified.  If <b>Repeat</b> is turned on, then when it has finished all its<br>" +
        "stages, it will loop back and continue with the first stage again, and so on.  If <b>Hold</b> is<br>" +
        "turned on, instead of gradually moving towards its chosen target, it will hold at its current value<br>" +
        "until it reaches the Time, and then jump immediately to the chosen target, similar to a Sample and Hold.</html>";
        
    static final String ENVELOPE_TARGET_TOOLTIP = "<html><b>Target</b><br>" +
        "Sets the Target Value of this stage.<br><br>" + 
        "The Envelope function begins by just outputting the <b>Initial</b> value, until it reaches<br>" +
        "the <b>Start</b> time. Then it moves towards the first <b>Target</b> over the course of the<br>" +
        "first <b>Time</b> interval.  When at the end of that interval, it has reached the target: this is<br>" +
        "one <b>Stage</b>.  It then begins to move towards the second target over the second time interval,<br>" +
        "and so on, thus finishing the second <b>Stage</b>.  It continues like this until it has finished all<br>" +
        "the <b>Stages</b> specified.  If <b>Repeat</b> is turned on, then when it has finished all its<br>" +
        "stages, it will loop back and continue with the first stage again, and so on.  If <b>Hold</b> is<br>" +
        "turned on, instead of gradually moving towards its chosen target, it will hold at its current value<br>" +
        "until it reaches the Time, and then jump immediately to the chosen target, similar to a Sample and Hold.</html>";
        
    static final String STEP_SEQUENCE_INITIAL_TOOLTIP = "<html><b>Initial</b><br>" +
        "Sets the Initial value of the Step Sequence.<br><br>" + 
        "The Envelope function begins by just outputting the <b>Initial</b> value, until it reaches<br>" +
        "the <b>Start</b> time. Then it starts outputting each step value in turn, each for<br>" +
        "<b>Step Length</b> timesteps, until it has consumed the stated number of <b>Steps</b>.<br>" +
        "It then resumes outputting the Initial Value.</b>  If <b>Repeat</b> is checked, then<br>" +
        "when it finishes all the Steps, it loops back to the first step and continues through the<br>" +
        "steps again.  If <b>Trigger</b> is true, then instead of outputting values for each step,<br>" +
        "it outputs triggers when the values are non-zero.</html>";
        
    static final String STEP_SEQUENCE_START_TOOLTIP = "<html><b>Start</b><br>" +
        "Sets the Start time of the Step Sequence.<br><br>" + 
        "The Envelope function begins by just outputting the <b>Initial</b> value, until it reaches<br>" +
        "the <b>Start</b> time. Then it starts outputting each step value in turn, each for<br>" +
        "<b>Step Length</b> timesteps, until it has consumed the stated number of <b>Steps</b>.<br>" +
        "It then resumes outputting the Initial Value.</b>  If <b>Repeat</b> is checked, then<br>" +
        "when it finishes all the Steps, it loops back to the first step and continues through the<br>" +
        "steps again.  If <b>Trigger</b> is true, then instead of outputting values for each step,<br>" +
        "it outputs triggers when the values are non-zero.</html>";

    static final String STEP_SEQUENCE_STEP_LENGTH_TOOLTIP = "<html><b>Step Length</b><br>" +
        "Sets the length of each Step in the Step Sequence.<br><br>" + 
        "The Envelope function begins by just outputting the <b>Initial</b> value, until it reaches<br>" +
        "the <b>Start</b> time. Then it starts outputting each step value in turn, each for<br>" +
        "<b>Step Length</b> timesteps, until it has consumed the stated number of <b>Steps</b>.<br>" +
        "It then resumes outputting the Initial Value.</b>  If <b>Repeat</b> is checked, then<br>" +
        "when it finishes all the Steps, it loops back to the first step and continues through the<br>" +
        "steps again.  If <b>Trigger</b> is true, then instead of outputting values for each step,<br>" +
        "it outputs triggers when the values are non-zero.</html>";

    static final String STEP_SEQUENCE_REPEAT_TOOLTIP = "<html><b>Repeat</b><br>" +
        "Sets whether the Step Sequence repeats (loops) when it finishes its steps.<br><br>" + 
        "The Envelope function begins by just outputting the <b>Initial</b> value, until it reaches<br>" +
        "the <b>Start</b> time. Then it starts outputting each step value in turn, each for<br>" +
        "<b>Step Length</b> timesteps, until it has consumed the stated number of <b>Steps</b>.<br>" +
        "It then resumes outputting the Initial Value.</b>  If <b>Repeat</b> is checked, then<br>" +
        "when it finishes all the Steps, it loops back to the first step and continues through the<br>" +
        "steps again.  If <b>Trigger</b> is true, then instead of outputting values for each step,<br>" +
        "it outputs triggers when the values are non-zero.</html>";

    static final String STEP_SEQUENCE_TRIGGER_TOOLTIP = "<html><b>Trigger</b><br>" +
        "Sets whether the Step Sequence outputs triggers instead of values.<br><br>" + 
        "The Envelope function begins by just outputting the <b>Initial</b> value, until it reaches<br>" +
        "the <b>Start</b> time. Then it starts outputting each step value in turn, each for<br>" +
        "<b>Step Length</b> timesteps, until it has consumed the stated number of <b>Steps</b>.<br>" +
        "It then resumes outputting the Initial Value.</b>  If <b>Repeat</b> is checked, then<br>" +
        "when it finishes all the Steps, it loops back to the first step and continues through the<br>" +
        "steps again.  If <b>Trigger</b> is true, then instead of outputting values for each step,<br>" +
        "it outputs triggers when the values are non-zero.</html>";
        
    static final String STEP_SEQUENCE_NUM_STEPS_TOOLTIP = "<html><b>Step Length</b><br>" +
        "Sets the number of Steps in the Step Sequence.  Steps beyond this number will be ignored.<br><br>" + 
        "The Envelope function begins by just outputting the <b>Initial</b> value, until it reaches<br>" +
        "the <b>Start</b> time. Then it starts outputting each step value in turn, each for<br>" +
        "<b>Step Length</b> timesteps, until it has consumed the stated number of <b>Steps</b>.<br>" +
        "It then resumes outputting the Initial Value.</b>  If <b>Repeat</b> is checked, then<br>" +
        "when it finishes all the Steps, it loops back to the first step and continues through the<br>" +
        "steps again.  If <b>Trigger</b> is true, then instead of outputting values for each step,<br>" +
        "it outputs triggers when the values are non-zero.</html>";
        
    static final String STEP_SEQUENCE_STEP_TOOLTIP = "<html><b>Rate</b><br>" +
        "Sets the value output during this Step.<br><br>" + 
        "The Envelope function begins by just outputting the <b>Initial</b> value, until it reaches<br>" +
        "the <b>Start</b> time. Then it starts outputting each step value in turn, each for<br>" +
        "<b>Step Length</b> timesteps, until it has consumed the stated number of <b>Steps</b>.<br>" +
        "It then resumes outputting the Initial Value.</b>  If <b>Repeat</b> is checked, then<br>" +
        "when it finishes all the Steps, it loops back to the first step and continues through the<br>" +
        "steps again.  If <b>Trigger</b> is true, then instead of outputting values for each step,<br>" +
        "it outputs triggers when the values are non-zero.</html>";

    static final String CONSTANT_VALUE_TOOLTIP = "<html><b>Value</b><br>" +
        "Sets the value output for this Argument.<br><br>" + 
        "Though this value is put through the Map, it's not very useful for it to so.</html>";

    static final String SAME_AS_TOOLTIP = "<html><b>Same As</b><br>" +
        "Specifies that this Argument is the same as an <b>Earlier Argument</b>.<br><br>" + 
        "For example, we might state that Argument 4's modulation is identical to Argument 2's modulation.<br>" +
        "You cannot state that an Argument is the same as itself, or a <b>Later Argument</b>.<br>" +
        "Such options will be declared to be <b>Invalid</b> and will treated just like <b>None</b><br>" +
        "(no modulation). This means (for example) that <i>all</i> of Argument 1's Same As options are<br>" +
        "invalid, since every Argument is later than Argument 1.</html>";


    }
