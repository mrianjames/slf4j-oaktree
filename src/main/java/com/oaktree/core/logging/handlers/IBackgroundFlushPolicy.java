package com.oaktree.core.logging.handlers;

/**
 * Created by IntelliJ IDEA.
 * User: IJ
 * Date: 18/02/12
 * Time: 17:56
 * To change this template use File | Settings | File Templates.
 */
public interface IBackgroundFlushPolicy {
    /**
     *  perform action after flushing batch.
     */
    void onBatchProcessed();
}
