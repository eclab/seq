package seq.engine;

/**
   My best attempt at a higher resolution timer than java.util.Timer, which is only in milliseconds.
   My timer can get significantly higher resolution, at least on MacOS X.  The timer can be set to
   fixed rate or fixed interval (normally you's want fixed rate).
**/

public class HighResolutionTimer
    {
    private static final long serialVersionUID = 1;

    // Is this a fixed-rate timer?
    boolean fixedRate;
        
    // Time interval in nanoseconds
    long nanos;
        
    // Timer thread
    Thread timerThread;
        
    // The timerThread's looped runnable, initially null.
    Runnable job;
        
    // The timerThread's LAST looped runnable.  If null, then no job has run yet.
    // FIXME: This is a hack to enable joining on a Runnable but it's not the right way
    // to do it.  Instead, maybe use a counter.
    Runnable lastJob;
        
    // Has the thread started up yet?  Used in the constructor only. 
    boolean running = false;
        
    // Is the thread daemon?  Used only by isDaemon()
    boolean daemon = false;
        
    // Requests that the thread stop its job.
    boolean pleaseStop = false;
        
    // Synchronization objects
    Object mutex = new Object[0];           // private to the timer, used for coordination and stopping jobs
    Object jobMutex = new Object[0];        // used to access fixedRate, nanos, pleaseStop, lastJob, job

    // Debugging and testing stuff
    boolean slept = false;                  // Did the timer sleep at least once this last iteration?  
    long initialTimeNanos = -1;             // Initial time the timer started
        
    public HighResolutionTimer(long nanos)
        {
        this(true, nanos, false);
        }

    public HighResolutionTimer(boolean fixedRate, long nanos, boolean isDaemon)
        {
        setFixedRate(fixedRate);
        setInterval(nanos);
                
        timerThread = new Thread(new Runnable()
            {
            public void run()
                {
                synchronized(mutex)
                    {
                    Runnable job = null;
                    while(true)
                        {
                        try
                            {
                            running = true;
                            mutex.wait();                                   // this lets go of the mutex so others can block on it
                            job = null;
                            boolean fixedRate = false;
                            long nanos = 0;
                                                
                            synchronized(jobMutex)
                                {
                                job = HighResolutionTimer.this.job;
                                fixedRate = HighResolutionTimer.this.fixedRate;
                                nanos = HighResolutionTimer.this.nanos;
                                pleaseStop = false;
                                }
                                                        
                            if (job != null)
                                {
                                // Do the job until interrupted
                                runJob(job, fixedRate, nanos);
        
                                // At this point we paused or stopped
                                synchronized(jobMutex)
                                    {
                                    if (pleaseStop)
                                        {
                                        if (job != null)
                                            lastJob = job;
                                        }
                                    pleaseStop = false;
                                    }
                                }
                            }
                        catch(InterruptedException ex)
                            {
                            // Destroy the job, indicating that we are now stopped
                            synchronized(jobMutex)
                                {
                                if (job != null)
                                    lastJob = job;
                                pleaseStop = false;
                                }
                            }
                        mutex.notifyAll();      // for join(Runnable)
                        }
                    }
                }
            });
        daemon = isDaemon;
        timerThread.setDaemon(isDaemon);
        timerThread.start();    
                                
        // spin-wait for timerThread to be ready
        while(true)
            {
            synchronized(mutex)             // acquire the mutex -- this either happens before we started or when we're stopped and waiting
                {
                // We're not stopped and waiting yet?  Wait a bit.
                if (!running)
                    {
                    try
                        {
                        Thread.currentThread().sleep(1);
                        }
                    catch (InterruptedException ex)
                        {
                        // Maybe try a little yield?
                        Thread.yield();
                        }
                    }
                else
                    {
                    mutex.notifyAll();              // Start the thread
                    break;
                    }
                }
            }
        }

    /// Called by the timer thread when we have a job to do
    void runJob(Runnable job, boolean fixedRate, long nanos)
        {
        try
            {
            // Set to initial time
            long lastTimeNanos = System.nanoTime();
            if (initialTimeNanos == -1) { initialTimeNanos = lastTimeNanos; }
            while(true)
                {
                // Compute end interval
                long nextTimeNanos = lastTimeNanos + nanos;
                long currentTime = 0;
                slept = false;
                // FIXME: Sleep until then -- will this cause a zeno's paradox hang?  Unclear
                while((currentTime = System.nanoTime()) < nextTimeNanos)
                    {
                    slept = true;
                    long remaining = nextTimeNanos - currentTime;
                    java.util.concurrent.locks.LockSupport.parkUntil(mutex, remaining);
                    }
                // pulse the job once
                job.run();

                synchronized(jobMutex)
                    {
                    if (pleaseStop) 
                        {
                        break;
                        }
                    }
                                        
                // Recompute the initial time
                if (fixedRate)
                    {
                    lastTimeNanos = nextTimeNanos;
                    }
                else
                    {
                    lastTimeNanos = System.nanoTime();
                    }
                }
            }
        finally
            {
            timerFinished(job);
            }
        }

    public void timerFinished(Runnable job) { }

    public boolean isDaemon() { return daemon; }

    public void setFixedRate(boolean val)
        {
        synchronized(jobMutex)
            {
            fixedRate = val;
            }
        }
                
    public boolean getFixedRate()
        {
        synchronized(jobMutex)
            {
            return fixedRate;
            }
        }
        
    public void setInterval(long nanos)
        {
        if (nanos < 0)
            throw new RuntimeException("nanos may not be < 0, was " + nanos);
        synchronized(jobMutex)
            {
            this.nanos = nanos;
            }
        }
                
    public long getInterval() { synchronized(jobMutex) { return nanos; } }
        
    /** Blocks until the timer has stopped.  Do not call this from within
        the timer process, it won't do anything useful.  */
    public void join()
        {
        synchronized(mutex) { }
        }
        
    /** Blocks until the timer has stopped for the given Runnable.
        Do not call this from within
        the timer process, it won't do anything useful.    */
    public void join(Runnable run)
        {
        synchronized(mutex) 
            { 
            while(lastJob != run)
                {
                try
                    {
                    mutex.wait();
                    }
                catch(InterruptedException ex) { }
                }
            }
        }
        
    /** Requests that the timer resume.  This is done by first stopping, then resuming.  */
    public void resume()
        {
        stop();
        // we know that job is not null, so we don't have to check that
        // At this point we can grab the mutex freely, the timer thread is waiting on it
        synchronized(mutex)
            {
            mutex.notifyAll();
            }
        }
                
    /** Requests that the timer stop now, or immediately after it has finished a currently running job. */
    public void stop()
        {
        synchronized(jobMutex)
            {
            pleaseStop = true;
            }
        join();
        }
        
    /** This issues an interupt to ask the underlying process to stop.  This will throw
        an InterruptedException when appropriate and raise the Runnable thread's interrupt flag. 
        Don't call this from inside the timer Runnable, or a RuntimeException will be thrown. 
        You may test Thread.interrupted() within the timer Runnable: it resets the interrupt
        flag but the timer does not need it to stop.  */
    /// FIXME: Is this even necessary?  We can't hard-interrupt a thread anyway...
    public void interruptAndStop()
        {
        if (Thread.currentThread() == timerThread)  // uh...
            throw new RuntimeException("HighResolutionTimer.interruptAndStop() may not be called from within the timer Runnable.");
                         
        synchronized(jobMutex)
            {
            pleaseStop = true;                              // in case interrupted() is checked
            timerThread.interrupt();                // force a wait
            }
        // This must be done outside the jobMutex
        join();                                         // wait until the job is waiting
        }

    /** Stops any current job, then starts a new job. */
    public void start(Runnable runnable)
        {
        stop();
        // set the job
        synchronized(jobMutex)
            {
            job = runnable;
            }
        // At this point we can grab the mutex freely, the timer thread is waiting on it
        synchronized(mutex)
            {
            mutex.notifyAll();
            }
        }

    /** Stops any current job, then starts a new job.  Blocks until the job has stopped or paused. */
    public void startAndJoin(Runnable runnable)
        {
        stop();
        // set the job
        synchronized(jobMutex)
            {
            job = runnable;
            }
        // At this point we can grab the mutex freely, the timer thread is waiting on it
        synchronized(mutex)
            {
            mutex.notifyAll();
            join(runnable);
            }
        }
                
    static void printStats(long[] targets, double divisor)
        {
        double sum = 0;
        double sqsum = 0;
        double min = Double.POSITIVE_INFINITY;
        int minp = 0;
        double max = Double.NEGATIVE_INFINITY;
        int maxp = 0;
                
        for(int i = SKIP; i < targets.length; i++)
            {
            double t = targets[i] / divisor;
            sum += t;
            if (t < min) { min = t; minp = i; }
            if (t > max) { max = t; maxp = i; }
            }
        double mean = sum / (targets.length - SKIP);

        for(int i = SKIP; i < targets.length; i++)
            {
            sqsum += (mean - targets[i] / divisor) * (mean - targets[i] / divisor);
            }
        double _var = sqsum / (targets.length - SKIP - 1);
                
        System.err.println("Mean: " + mean + " Std: " + Math.sqrt(_var) + " " + " Min: " + min + " at " + minp + " Max: " + max + " at " + maxp);
        }
                
    static final int SKIP = 0;
    static final long TEST_INTERVAL_NANOS = 20000L;         // Seems to be the minimum on my M1.  Maybe 20000?
    static void testHighResolutionTimer()
        {
        final boolean[] slept = new boolean[100000];
        final long[] targets = new long[100000];
        final int[] index = { 0 };
        HighResolutionTimer timer = new HighResolutionTimer(true, TEST_INTERVAL_NANOS, false);
        Runnable run = new Runnable() 
            { 
            public void run()
                {
                if (index[0] == targets.length) 
                    {
                    for(int i = 0; i < targets.length; i++)
                        {
                        System.err.println("" + i + "\t" + (targets[i] / 1000000.0) + (slept[i] ? "" : "\tNO SLEEP"));
                        }
                    printStats(targets, 1000000.0);
                                        
                    timer.stop();
                    }
                else 
                    {
                    targets[index[0]] = System.nanoTime() - (timer.initialTimeNanos + TEST_INTERVAL_NANOS * (index[0] + 1L));
                    slept[index[0]] = timer.slept;
                    index[0]++;
                    }
                } 
            };
        timer.startAndJoin(run);
        }

    static Object standardTimerFinishedLock = new Object[0];
    static void testStandardTimer()
        {
        final long[] targets = new long[1000];
        final int[] index = { 0 };
        java.util.Timer timer = new java.util.Timer();
        timer.scheduleAtFixedRate(new java.util.TimerTask()
            {
            long firstTime = -1;
            public void run() 
                {
                if (firstTime == -1) 
                    firstTime = scheduledExecutionTime();
                                        
                if (index[0] == targets.length) 
                    {
                    timer.cancel();

                    for(int i = 0; i < targets.length; i++)
                        {
                        System.err.println("" + i + "\t" + targets[i]);
                        }
                    printStats(targets, 1.0);
                    synchronized(standardTimerFinishedLock) { standardTimerFinishedLock.notifyAll(); }
                    }
                else 
                    {
                    targets[index[0]] = System.currentTimeMillis() - (firstTime + 1 * (index[0] + 0L));
                    index[0]++;
                    }
                } 
            }, 0, 1);
        try { synchronized(standardTimerFinishedLock) { standardTimerFinishedLock.wait(); } }
        catch (InterruptedException ex) { }
        }
                
    static void testStartStop()
        {
        HighResolutionTimer timer = new HighResolutionTimer(true, 1000000L * 100, false);

        timer.start(new Runnable()
            { 
            public void run()
                {
                System.err.println("Testing 1");
                }
            });
        for(int i = 0; i < 5; i++)
            {
            try { Thread.currentThread().sleep(1000); } catch (InterruptedException ex) { }
            timer.stop();
            System.err.println("Paused 1");
            try { Thread.currentThread().sleep(1000); } catch (InterruptedException ex) { }
            timer.resume();
            System.err.println("resumed 1");
            }
                                
        timer.start(new Runnable()
            { 
            public void run()
                {
                System.err.println("Testing 2");
                }
            });
        for(int i = 0; i < 5; i++)
            {
            try { Thread.currentThread().sleep(1000); } catch (InterruptedException ex) { }
            timer.stop();
            System.err.println("Paused 2");
            try { Thread.currentThread().sleep(1000); } catch (InterruptedException ex) { }
            timer.resume();
            System.err.println("resumed 2");
            }

        timer.start(new Runnable()
            { 
            public void run()
                {
                System.err.println("Testing 3");
                }
            });
        try { Thread.currentThread().sleep(1000); } catch (InterruptedException ex) { }
        timer.interruptAndStop();
        System.err.println("Interrupt and Stop 3");
        System.err.println("Speed Up");
        timer.setInterval(1000000L * 10);
        try { Thread.currentThread().sleep(1000); } catch (InterruptedException ex) { }

        timer.start(new Runnable()
            { 
            public void run()
                {
                System.err.println("Testing 4");
                }
            });
        try { Thread.currentThread().sleep(1000); } catch (InterruptedException ex) { }
        timer.interruptAndStop();
        System.err.println("Interrupt and Stop 4");
        System.err.println("Slow Down");
        timer.setInterval(1000000L * 100);
        try { Thread.currentThread().sleep(1000); } catch (InterruptedException ex) { }

        timer.start(new Runnable()
            { 
            public void run()
                {
                System.err.println("Testing 5");
                }
            });
        for(int i = 0; i < 5; i++)
            {
            try { Thread.currentThread().sleep(1000); } catch (InterruptedException ex) { }
            timer.stop();
            System.err.println("Paused 5");
            try { Thread.currentThread().sleep(1000); } catch (InterruptedException ex) { }
            timer.resume();
            System.err.println("resumed 5");
            }
        System.err.println("Exit without a stop (not daemon)");
        }
                
    public static void main(String[] args)
        {
        testHighResolutionTimer();
        testStandardTimer();
        testStartStop();
        System.exit(0);
        }
    }






