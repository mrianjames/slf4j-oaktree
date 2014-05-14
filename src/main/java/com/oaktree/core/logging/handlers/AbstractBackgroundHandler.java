package com.oaktree.core.logging.handlers;

import com.oaktree.core.logging.ILogRecord;
import com.oaktree.core.logging.Level;
import com.oaktree.core.logging.formatters.IFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A handler that performs formatting and flush in a background thread. 
 * Publications to the real handler will occur in batches; if batches of > 1 are specified (10 default)
 * then we will sleep between batches; this delay uses Thread.sleep which may not be entirely accurate.
 * Note that due to the very nature of backgrounding publication is not guaranteed; if an application terminates
 * abruptly then records waiting to be flushed (especially relevant with batches > 1) may be missed.
 * 
 * @author Oak Tree Designs Ltd
 *
 */
public abstract class AbstractBackgroundHandler implements IBackgroundLoggingHandler {

	/**
	 * Terminate background processing.
	 */
	public void stop() {
		this.processor.terminate();
		if (this.handler != null) {
			this.handler.stop();
		}
	}
	
	/**
	 * We will only log at levels better than this...
	 */
	private Level level = Level.INFO;
	
	private String name;
	
	/**
	 * The delegate handler we shall use.
	 */
	protected ILoggingHandler handler;
	
	@Override
	public ILoggingHandler getHandler() {
		return this.handler;
	}
	
	@Override
	public void setHandler(ILoggingHandler handler) {
		this.handler = handler;
	}
	
	/**
	 * A queue that all the incoming and as yet unwritten records are written to. 
	 */
	private LinkedBlockingQueue<ILogRecord> queue = new LinkedBlockingQueue<ILogRecord>();
	
	/**
	 * Batch size for processing. More immediate writes are obtained with batch of 1 though
	 * this is not as efficient as larger batch sizes.
	 */
	private int batchSize = 10;

    /**
     * flushing policy - wait, yield, busy etc.
     */
    private IBackgroundFlushPolicy policy = new SleepingFlushPolicy(1);

	/**
	 * background thread processor of events.
	 */
	private Processor processor;
	
	/**
	 * The processing thread. Reads off the queue and hands to the delegate.
	 * @author Oak Tree Designs Ltd
	 *
	 */
	private class Processor extends Thread {
		/**
		 * Wrapper round the actual handler.
		 */
		private AbstractBackgroundHandler handler;
		/**
		 * The queue records are being put onto.
		 */
		private BlockingQueue<ILogRecord> queue;
		private int batchSize;
		public Processor(AbstractBackgroundHandler h, LinkedBlockingQueue<ILogRecord> queue, int batchSize) {
			if (batchSize <= 0) {
				throw new IllegalArgumentException("Batchsize must be > 0");
			}
			if (h == null) {
				throw new IllegalArgumentException("Null handler supplied");
			}
			this.handler = h;
			this.batchSize = batchSize;
			this.queue = queue;
		}
		public void terminate() {
			work = false;
			this.interrupt();
			List<ILogRecord> batcher = new ArrayList<ILogRecord>();
			this.queue.drainTo(batcher);
			this.handler.getHandler().publish(batcher);
		}
		
		private volatile boolean work = true;
		@Override
		public void run() {
			 List<ILogRecord> batch = new ArrayList<ILogRecord>();
				try {
				while (work) {
					//batches of 1 will just use the standard blocking take call.
					if (this.batchSize == 1) {
						this.handler.getHandler().publish(this.queue.take());
					} else {
						int sz = queue.size() ;
						int bs = this.batchSize;
						if (sz > 100) {
							System.err.println("Queue size is bloated: " + sz);
							bs = 20000;
						}
						if (sz> 0) {
							this.queue.drainTo(batch,bs);
						
							this.handler.getHandler().publish(batch);
							batch.clear();
						}
						
                       policy.onBatchProcessed();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.err.println("Exiting processing loop");
		}
		public int getBatchSize() {
			return batchSize;
		}
		public void setBatchSize(int batchSize) {
			this.batchSize = batchSize;
		}
	}
	
	public AbstractBackgroundHandler() {		
	}
	
	@Override
	public void publish(ILogRecord record) {
		if (record == null || this.level.intValue() > record.getLevel().intValue()) {
			return;
		}
		queue.add(record);
	}

	@Override
	public void setFormatter(IFormatter f) {
		this.getHandler().setFormatter(f);
	}

	@Override
	public void setLevel(Level l) {
		level = l;
		this.getHandler().setLevel(l);
	}

	@Override
	public void publish(List<ILogRecord> batch) {
		for (ILogRecord record:batch) {
			this.publish(record);
		}
	}
	
	public void setBatchSize(String batchSize) {
		this.batchSize = Integer.valueOf(batchSize);
	}

    public void setFlushPolicy(IBackgroundFlushPolicy flushPolicy) {
        this.policy = flushPolicy;
    }
	
	@Override
	public void start() {
		//create process in start rather than in constructor...
		this.processor = new Processor(this,this.queue,this.batchSize );
		processor.setName("LogProcessor");
		//set as daemon if batch sizes are small; if large then you may well lose records when
		//an application terminates normally.		
		processor.setDaemon(true);
		processor.start();
        System.out.println("Configuring background handler: " + getClass().getName() + " with batchsize: " + this.batchSize + " flushpolicy: " + this.policy.getClass().getName());
		if (this.handler != null) {
			this.handler.start();
		}
	}


	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name + " delegate: " + handler;
	}

}
