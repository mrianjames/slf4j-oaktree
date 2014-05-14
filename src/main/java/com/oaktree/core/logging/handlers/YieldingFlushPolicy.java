package com.oaktree.core.logging.handlers;

/**
 * Created by IntelliJ IDEA.
 * User: IJ
 * Date: 18/02/12
 * Time: 17:58
 * To change this template use File | Settings | File Templates.
 */
public class YieldingFlushPolicy implements IBackgroundFlushPolicy{
    @Override
    public void onBatchProcessed() {
        Thread.yield();
    }
}
