package com.oaktree.core.logging;

import com.oaktree.core.logging.handlers.ILoggingHandler;

/**
 * A lock free object for passing around our logging infrastructure, replacing LogRecord which did lots
 * of synchronization.
 * @author ij
 *
 */
public class LogRecord implements ILogRecord {

	private static final long serialVersionUID = 1L;
	private Level level;
	private long millis;
	private int threadId;
	
	private ILogger logger;
	private String threadName;
	private String msg;
	private int count = 0; //number of people to process before we are "done".
	public LogRecord() {}
	
	public LogRecord(final String msg, final Level lvl, final long time, final long threadId,final String threadName,final ILogger logger) {
		this.msg = msg;
		this.millis = time;
		this.level = lvl;
		this.threadId = (int)threadId;
		this.threadName = threadName;
		this.logger = logger;
	}
	public String getMessage() {
		return this.msg;
	}
	public void setMessage(String msg) {
		this.msg = msg;
	}
	
	public Level getLevel() {
		return level;
	}
	
	public ILogger getLogger() {
		return logger;
	}
	
	public String getThreadName() {
		return this.threadName;
	}

	@Override
	public long getMillis() {
		return this.millis;
	}

	@Override
	public int getThreadId() {
		return this.threadId;
	}

	@Override
	public void setLevel(Level level) {
		this.level = level;
	}

	@Override
	public void setLogger(ILogger logger) {
		this.logger = logger;
	}

	@Override
	public void setMillis(long millis) {
		this.millis = millis;
	}

	@Override
	public void setThreadId(int id) {
		this.threadId = id;
	}

	@Override
	public void setThreadName(String name) {
		this.threadName = name;
	}

	@Override
	public java.util.logging.LogRecord toLogRecord() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setCount(int count) {
		this.count = count;
	}
	
	@Override
	public void onProcessed(ILoggingHandler handler) {
		this.count = count  -1;
		if (count == 0) {
			logger.onFree(this);
		}
	}
	
	@Override
	public String toString() {
		return getMillis() + Text.SPACE + getThreadId() + Text.SPACE + getThreadName() + Text.SPACE + getLevel() + Text.SPACE + getMessage();
	}
}