package com.oaktree.core.logging.binlog.write;

import java.util.List;

import com.oaktree.core.logging.ILogRecord;
import com.oaktree.core.logging.Level;
import com.oaktree.core.logging.formatters.IFormatter;
import com.oaktree.core.logging.handlers.ILoggingHandler;

/**
 * A handler that writes to a binary format. In theory this is way quicker than a conventional
 * logger.
 * This particular logger writes the binary to a file.
 * 
 * TODO batch processing can be better.
 * TODO separate the schema/formatting.
 * 
 * @author ianjames
 *
 */
public class BinaryLoggingHandler implements ILoggingHandler {

	private LogRecordFileBinaryLogWriter lrfb;
	private int level;
//	private boolean background = false;
//	public void setBackground(String bg) {
//		this.background = Boolean.valueOf(bg);
//	}
	private boolean useByteBuffer = true;
	public void setUseByteBuffer(String ubb) {
		this.useByteBuffer = Boolean.valueOf(ubb);
	}
	private String filename = "binlog.out";
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public String getFilename() {
		return filename;
	}
	private int maxQueueSize = 1000;
	private String name;
	public void setMaxQueueSize(String maxQueueSize){
		this.maxQueueSize = Integer.valueOf(maxQueueSize);
	}
	
	@Override
	public void publish(ILogRecord record) {
		if (record == null || this.level > record.getLevel().intValue()) {
			return;
		}
		//lrfb.log(record.getMillis(), (short)record.getLevel().intValue(), (short)record.getThreadId(), record.getThreadName(), record.getMessage());
		lrfb.log(record);
		record.onProcessed(this);
	}

	@Override
	public void setFormatter(IFormatter f) {
		//Irrelevant for this. TODO maybe way of getting schema in?
		throw new IllegalStateException("BinaryLoggingHandler does not consume a formatter");
	}

	@Override
	public void setLevel(Level l) {
		this.level = l.intValue();
	}

	@Override
	public void publish(List<ILogRecord> batch) {
		//TODO probably inefficient. we should be able to flush once per batch.
		
		for (ILogRecord r:batch) {
			publish(r);
		}
	}

	
	@Override
	public void start() {
		String nm = getName()+"BinLog";
//		if (background) {
//			lrfb = new BackgroundFileBinaryLogWriter(useByteBuffer, nm, filename, maxQueueSize);			
//		} else {
			lrfb = new LogRecordFileBinaryLogWriter(useByteBuffer, nm, filename);			
//		}
		lrfb.setUseByteBuffer(useByteBuffer);
		lrfb.start();
	}

	@Override
	public void stop() {
		lrfb.stop();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name + " filename: " + filename + " queueSize: "+maxQueueSize + " useByteBuffer: "+useByteBuffer;
	}

}
