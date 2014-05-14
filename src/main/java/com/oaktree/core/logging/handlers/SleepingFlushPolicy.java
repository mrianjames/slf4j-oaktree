package com.oaktree.core.logging.handlers;

/**
 * Sleep between batch flushes for x milliseconds.
 *
 */
public class SleepingFlushPolicy implements IBackgroundFlushPolicy {
    /**
     * Time in ms to sleep for.
     */
    private long sleep = 10;
    public SleepingFlushPolicy(){}
    public SleepingFlushPolicy(long sleep) {
        this.sleep = sleep;
    }
    @Override
    public void onBatchProcessed() {
        try {
            Thread.sleep(sleep);
        } catch (Exception e) {
        	e.printStackTrace();
        }
    }
}
