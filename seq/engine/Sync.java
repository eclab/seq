/* 
   Copyright 2025 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.engine;

public class Sync
	{
	double rate = 0;			// in ppq per ms
	boolean startSync = false;
	boolean rateStarted = false;
	double lastPrediction = -1;
	long lastActual = -1;
	long timeOffset = 0;
	long tick = -1;
	long syncs = 0;
	int ppq;
	double initialBPM;
	static final int SYNCS_PER_BEAT = 24;
	public static final double RATE_ALPHA = 0.03;
	public static final double PREDICTION_ALPHA = 0.5;
	public static final double WARMUP = 0;
	public static final int TICKS_PER_SYNC = 8;
	public static final int MAX_SANE_RATE = 500;
	public static final int MIN_SANE_RATE = 20;
	boolean synced;
	
	public Sync(double initialBPM, int ppq)
		{
		this.initialBPM = initialBPM;
		this.ppq = ppq;
		reset();
		}
	
	public void reset()
		{
		lastPrediction = -1;
		lastActual = -1;
		tick = -1;
		startSync = false;
		// ppq/ms = 1.0 / (beat/ppq * min/beat * sec/min * ms/sec)
		// ppq/ms = ppq/beat * beat/min / min/sec / sec/ms
		rate = ppq * initialBPM / 60.0 / 1000.0;
		rateStarted = false;
		syncs = 0;
		}
		
	public void start()
		{
		startSync = true;
		}
	
	double rates[] = { -1, -1, -1, -1, -1 };
	
	void updateRates(double rate)
		{
		for(int i = 1; i < rates.length; i++)
			{
			rates[i] = rates[i -1];
			}
		rates[0] = rate;
		}
		
	double meanRate()
		{
		int count = 0;
		double sum = 0.0;
		for(int i = 0; i < rates.length; i++)
			{
			if (rates[i] != -1)
				{
				sum += rates[i];
				count++;
				}
			}
		return sum / count;
		}
		
	// happens at a rate of SYNCS_PER_BEAT 
	public void sync()
		{
		synced = true;
		long time = System.currentTimeMillis();

		if (!rateStarted)
			{
			timeOffset = time;
			lastPrediction = 0;
			lastActual = 0;
			rateStarted = true;
			rate = ppq * initialBPM / 60.0 / 1000.0;
			}
		else
			{
			time = time - timeOffset;
			syncs++;

			// 24 ticks between the current time and the last actual time
			double actualRate = toRate(time, lastActual);
			double bpm = toBPM(actualRate);
			if (bpm > MAX_SANE_RATE || bpm < MIN_SANE_RATE) { System.err.println("WAS " + bpm + " " + (time - lastActual)); actualRate = rate; } // this time...
			double alpha =  RATE_ALPHA; // Math.max(RATE_ALPHA, 1.0 - (syncs - 1)/WARMUP);
			//double alpha = 1.0 - ((1.0 - RATE_ALPHA) * Math.min(1.0, syncs / WARMUP));
			rate = (1.0 - alpha) * rate + alpha * actualRate;				// this is a P-controller.  Is it enough?  Do I need to go to PD?
			updateRates(rate);
			System.err.println("SYNC " + syncs + " " + lastActual + " " + lastPrediction + " " + (time - lastActual) + " " + toBPM(actualRate) + " " + toBPM(rate) + " " + toBPM(meanRate()));

			lastActual = time;
			// update by 1 sync.  ms/sync = 1.0 / (ppq/ms * beat/ppq * sync/beat)
			//                    ms/sync = (ms/ppq) / (beat/ppq * sync/beat)
			//                    ms/sync = ms/ppq * ppq/beat / sync/beat
			lastPrediction += (1.0 / rate) * ppq / SYNCS_PER_BEAT;
			lastPrediction += (lastActual - lastPrediction) * PREDICTION_ALPHA;

			}
			
		if (startSync)
			{
			startSync = false;
			tick = 0;
			}
		else if (tick >= 0)		// should happen when the rate has started
			{
			tick += TICKS_PER_SYNC;
			}
		}
		
	/** Returns an estimate of the number of ticks that have transpired. */
	public long poll()
		{
		// overage on ticks is
		// ppq / ms * ms
		return (long)(tick + Math.round(1.0 / rate * (System.currentTimeMillis() - timeOffset - lastPrediction)));
		}
	
	double toRate(long time, long lastTime)
		{
		long diff = time - lastTime;
		// ppq/ms = 1.0 / (ms/sync * sync/beat) * ppq
		return ppq * 1.0 / (diff * SYNCS_PER_BEAT);
		}
		
	double toBPM(double rate)
		{
		// beats/min = ppq/ms * ms/sec * sec/min / ppq
		return rate * 1000 * 60 / ppq;
		}
		
	/** Returns an estimate of the rate in beats per minute. */
	public double getBPM()
		{
		return toBPM(rate);
		}
		
	public static void main(String[] args)
		{
		Sync sync = new Sync(120, 192);
		
		java.util.TimerTask readTask = new java.util.TimerTask()
			{
			public void run()
				{
				long stick = 0;
				long spoll = 0;
				double sbpm = 0;
				boolean synced;

				synchronized(sync)
					{
					synced = sync.synced;
					//if (synced)
						{
				 stick = sync.tick;
				 spoll = sync.poll();
				 sbpm = sync.getBPM();
				 sync.synced = false;
				 }
				 }
				 
				if (synced)
					{
				System.err.println("POLL " + stick + " " + spoll + " " +  sbpm);
				}
				}
			};
		java.util.Timer readTimer = new java.util.Timer();
		readTimer.scheduleAtFixedRate(readTask, 0, 1);
		
		java.util.TimerTask writeTask = new java.util.TimerTask()
			{
			int count = 0;
		//long lastTime = -1;
			public void run()
				{
				synchronized(sync)
					{
					/*
					long time = System.currentTimeMillis();
					if (lastTime == time) System.err.println("SAME");
					else System.err.println("TIME " + " " + time + " " + lastTime + " " + (time - lastTime));
					lastTime = time;
					*/
					
					if (count++ > 50)
					sync.sync();
					}
				}
			};
		java.util.Timer writeTimer = new java.util.Timer();
		writeTimer.scheduleAtFixedRate(writeTask, 0, 500 / 24);
		
		sync.start();
		}
	}
