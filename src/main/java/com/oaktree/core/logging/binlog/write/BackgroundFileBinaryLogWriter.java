package com.oaktree.core.logging.binlog.write;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.oaktree.core.logging.ILogRecord;

/**
 * A binary logger that takes the object array and pushes to a bounded
 * or unbounded queue. This is much quicker to push data but comes at the
 * loss of guaranteed write (possible for logs to be enqueued before being
 * flushed) and means you have to have an object array to give to the
 * logger (no object free direct calls). For most in-the-latency-path
 * areas this is fine tradeoff.
 *
 * Note - stop will block until queue is emptied.
 *
 * illegal state exception if queue is full
 * To be unbounded set size = 0
 *
 * Created with IntelliJ IDEA.
 * User: IJ
 * Date: 13/04/13
 * Time: 18:53
 * To change this template use File | Settings | File Templates.
 */
public class BackgroundFileBinaryLogWriter extends LogRecordFileBinaryLogWriter implements Runnable {

    private BlockingQueue<ILogRecord> queue;
    private Thread thread;

    public void start() {
        thread = new Thread(this);
        thread.setName("bin.log.processor");
        thread.start();
        super.start();
    }

    public BackgroundFileBinaryLogWriter(boolean useByteBuffer,String name, String fileName, int maxQueueSize) {
        super(useByteBuffer, name, fileName);
        queue = new LinkedBlockingQueue<ILogRecord>(maxQueueSize);
    }

    @Override
    public void log(ILogRecord record) {
		//log(record.getMillis(), (short)record.getLevel().intValue(), (short)record.getThreadId(), record.getThreadName(), record.getMessage());
    	queue.add(record);
	}
//    public void log(long time, short a, short b, String tname, String msg) {
//    	if (!canLog()) {
//    		return;
//    	}
//    	Object[] o = new Object[5];
//    	o[0] = time;
//    	o[1] = a;
//    	o[2] = b;
//    	o[3] = tname;
//    	o[4] = msg;
//    	queue.add(o);
//    }

    public void stop() {
        System.out.println("Stopping processing thread...queue size: "+ queue.size());
        //TODO queue might not be emptied yet. shame.
        while (queue.size() > 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Pending termination...queue: " + queue.size());
        }
        System.out.println("Queue is empty. Terminating");
        thread.interrupt();
    }

    @Override
    public void run() {
        try {
            System.out.println("Processing thread is started");
            while (true) {
                super.log(queue.take());
            }

        }catch (InterruptedException e) {
            System.out.println("Processing thread terminated");
            super.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    public static void main(String[] args) throws Exception {
//        long TESTS = 500000;
//        boolean useByteBuffer = true;
//        BackgroundFileBinaryLogWriter logger = new BackgroundFileBinaryLogWriter(useByteBuffer,new byte[]{ByteUtils.Types.LONG,ByteUtils.Types.LONG},"GFBL","test.bl",(int)TESTS);
//        logger.start();
//
//        ResultTimer x = new ResultTimer();
//        x.startSample();
//        ResultTimer t = new ResultTimer(10000);
//        for (long l = 0; l < TESTS; l++) {
//            t.startSample();
//            logger.log(new Object[]{12l+l,32l});
//            t.endSample();
//        }
//        System.out.println("Object WriteDuration:" +t.toString(TimeUnit.MICROSECONDS));
//
//        ///Thread.sleep(30000);
//
//        logger.stop();
//        x.endSample();
//        System.out.println("Whole write: " + x.toString(TimeUnit.SECONDS));
//    }

}
