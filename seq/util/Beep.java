/* 
   Copyright 2024 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.util;

import javax.sound.sampled.*;

public class Beep
    {
    public static final float SAMPLING_RATE = 44100.0f;
    public static final int CHUNK_SIZE = 128;
    public static final int BUFFER_SIZE = CHUNK_SIZE * 10;
    public static final double DEFAULT_FREQUENCY = 440.0;
    public static final double RAMP = 0.002;                // Set to 0.001 for a smoother fade-in ˙envelope
    AudioFormat audioFormat;
    SourceDataLine sdl;
    Object frequencyLock = new Object[0];
    double oldFrequency = DEFAULT_FREQUENCY;
    double oldAmplitude = 0.0;
    double frequency = DEFAULT_FREQUENCY;
    double amplitude = 0.0;
    double ramp = 0;
        
    /** Sets the Beep Frequency or turns it off.  Default Frequency is 440.
        Frequency must be a value > 0 and &lt;= SAMPLING_RATE / 2.0, else it will be set to the Default Frequency.
        You might want to call setFrequencyAndAmplitude(...) instead, which sets both
        frequency and amplitude in one atomic call. */
    public void setFrequency(double frequency)
        {
        if (frequency <= 0 || frequency > SAMPLING_RATE / 2.0) 
            frequency = DEFAULT_FREQUENCY;
        synchronized(frequencyLock)
            {
            oldAmplitude = this.amplitude;
            oldFrequency = this.frequency;
            this.frequency = Math.max(frequency, 0.0);
            ramp = 0;
            }
        }
    
    /** Returns the Beep Frequency.*/
    public double getFrequency()
        {
        synchronized(frequencyLock)
            {
            return frequency;
            }
        }

    /** Sets the Beep Frequency and Amplitude or turns it off.  Default Frequency is 440.
        Frequency must be a value > 0 and &lt;= SAMPLING_RATE / 2.0, else it will be set to the Default Frequency.
        Amplitude will be bounded to be between 0.0 and 1.0 inclusive.
        If you set frequency or amplitude to <= 0, then the Beep will turn off. */
    public void setFrequencyAndAmplitude(double frequency, double amplitude)
        {
        if (frequency <= 0 || frequency > SAMPLING_RATE / 2.0) 
            frequency = DEFAULT_FREQUENCY;
        if (amplitude < 0) amplitude = 0;
        if (amplitude > 1) amplitude = 1;
        synchronized(frequencyLock)
            {
            oldFrequency = this.frequency;
            oldAmplitude = this.amplitude;
            this.frequency = Math.max(frequency, 0.0);
            this.amplitude = Math.max(amplitude, 0.0);
            ramp = 0;
            }
        }
        
    /** Sets the Beep Amplitude or turns it off.  Default Amplitude is 1.0.
        Amplitude will be bounded to be between 0.0 and 1.0 inclusive.
        You might want to call setFrequencyAndAmplitude(...) instead, which sets both
        frequency and amplitude in one atomic call. */
    public void setAmplitude(double amplitude)
        {
        if (amplitude < 0) amplitude = 0;
        if (amplitude > 1) amplitude = 1;
        synchronized(frequencyLock)
            {
            oldAmplitude = this.amplitude;
            oldFrequency = this.frequency;
            this.amplitude = Math.max(amplitude, 0.0);
            ramp = 0;
            }
        }

    /** Returns the Beep Amplitude. */
    public double getAmplitude()
        {
        synchronized(frequencyLock)
            {
            return amplitude;
            }
        }

        
        
    //// FastSin is inspired from
    //// https://github.com/Bukkit/mc-dev/blob/master/net/minecraft/server/MathHelper.java
    
    static final int SIN_TABLE_LENGTH = 65536;
    static final double SIN_MULTIPLIER = SIN_TABLE_LENGTH / Math.PI / 2;
    final static int SIN_TABLE_LENGTH_MINUS_1 = SIN_TABLE_LENGTH - 1;
    final static int SIN_TABLE_LENGTH_DIV_4 = SIN_TABLE_LENGTH / 4;
    final static double[] sinTable = new double[SIN_TABLE_LENGTH];

    static 
        {
        for (int i = 0; i < SIN_TABLE_LENGTH; ++i) 
            {
            sinTable[i] = (double)Math.sin((double) i * Math.PI * (2.0 / SIN_TABLE_LENGTH));
            }
        }

    /** A fast approximation of Sine using a lookup table.  40x the speed of Math.sin. */
    static final double fastSin(double f) 
        {
        return sinTable[((int) (f * SIN_MULTIPLIER)) & (SIN_TABLE_LENGTH - 1)];
        }

    /**
     * A fast approximation of Sine using a lookup table and Catmull-Rom cubic spline interpolation.  16x the speed of Math.sin.
     */
    static final double fastIntSin(double f)    
        {
        double v = f * SIN_MULTIPLIER;
        int conv = (int) v;
        double alpha = v - conv;
        
        int slot1 = conv & SIN_TABLE_LENGTH_MINUS_1;
        int slot0 = (slot1 - 1) & SIN_TABLE_LENGTH_MINUS_1;
        int slot2 = (slot1 + 1) & SIN_TABLE_LENGTH_MINUS_1;
        int slot3 = (slot2 + 1) & SIN_TABLE_LENGTH_MINUS_1;
        
        double f0 = sinTable[slot0];
        double f1 = sinTable[slot1];
        double f2 = sinTable[slot2];
        double f3 = sinTable[slot3];
        
        return alpha * alpha * alpha * (-0.5 * f0 + 1.5 * f1 - 1.5 * f2 + 0.5 * f3) +
            alpha * alpha * (f0 - 2.5 * f1 + 2 * f2 - 0.5 * f3) +
            alpha * (-0.5 * f0 + 0.5 * f2) +
            f1;
        }

    boolean running = false;
        
    /** Turns on or off the Beep thread */
    public void setRunning(boolean val)
        {
        synchronized(frequencyLock)
            {
            if (val == true)        
                {
                running = true;
                frequencyLock.notifyAll();
                }
            else
                {
                running = false;
                }
            }
        }

    public Beep()
        {
        synchronized(frequencyLock) { running = true; }
        Thread thread = new Thread(new Runnable()
            {
            int pos = Integer.MIN_VALUE;
            public void run()
                {
                try
                    {
                    byte[] chunk = new byte[CHUNK_SIZE];
                    int pos = 0;
                    double freq = 0;
                    double amp = 0;
                    double oldfreq = 0;
                    double oldamp = 0;
                                        
                    audioFormat = new AudioFormat( SAMPLING_RATE, 16, 1, true, false );
                    sdl = AudioSystem.getSourceDataLine( audioFormat );
                    sdl.open(audioFormat, BUFFER_SIZE);
                    sdl.start();
                
                    while(true)
                        {
                        synchronized(frequencyLock)
                            {
                            freq = frequency;
                            amp = amplitude;
                            oldfreq = oldFrequency;
                            oldamp = oldAmplitude;
 
                            if (!running)
                                {
                                try { frequencyLock.wait(); } catch (InterruptedException ex) { return; }
                                }
                            }
                                                        
                        if (ramp == 1)          // steady-state, no ramping
                            {
                            if (amp == 0)
                                {
                                pos = Integer.MIN_VALUE;                // might as well reset
                                for(int i = 0; i < chunk.length; i++)
                                    {
                                    chunk[i] = 0;
                                    }
                                }
                            else
                                {
                                double convertedFrequency = 2 * Math.PI / SAMPLING_RATE * frequency;
                                for(int i = 0; i < chunk.length; i+=2)
                                    {
                                    int val = (int)(Math.sin(pos * convertedFrequency) * Short.MAX_VALUE * amp);
                                    pos++;
                                    chunk[i] = (byte)(val & 255);
                                    chunk[i+1] = (byte)((val >> 8) & 255);
                                    }
                                }
                            }
                        else                    // cross-fade, we're ramping
                            {
                            double convertedFrequency = 2 * Math.PI / SAMPLING_RATE * frequency;
                            double convertedOldFrequency = 2 * Math.PI / SAMPLING_RATE * oldFrequency;
                            for(int i = 0; i < chunk.length; i+=2)
                                {
                                int val = ((int)(Math.sin(pos * convertedFrequency) * Short.MAX_VALUE * amp * ramp)) +
                                    ((int)(Math.sin(pos * convertedOldFrequency) * Short.MAX_VALUE * oldamp * (1.0 - ramp)));
                                pos++;
                                chunk[i] = (byte)(val & 255);
                                chunk[i+1] = (byte)((val >> 8) & 255);
                                ramp += RAMP;
                                if (ramp > 1) ramp = 1;
                                }
                            }
                                                        
                        sdl.write(chunk, 0, chunk.length);
                        }
                    }
                catch (LineUnavailableException ex)
                    {
                    ex.printStackTrace();
                    }
                }
            });
        thread.setName("Beep");
        thread.setDaemon(true);
        thread.start();
        }
                
    public static void main(String[] args) throws InterruptedException
        {
        Beep beep = new Beep();
        beep.setFrequencyAndAmplitude(440, 1.0);
        Thread.currentThread().sleep(1000);
        beep.setFrequency(880);
        Thread.currentThread().sleep(1000);
        beep.setAmplitude(0.0);
        Thread.currentThread().sleep(1000);
        beep.setFrequencyAndAmplitude(220, 1.0);
        Thread.currentThread().sleep(1000);
        beep.setFrequency(880);
        Thread.currentThread().sleep(1000);
        beep.setFrequencyAndAmplitude(880, 0.25);
        Thread.currentThread().sleep(1000);
        beep.setAmplitude(0.0);
        Thread.currentThread().sleep(1000);
        }
    }
