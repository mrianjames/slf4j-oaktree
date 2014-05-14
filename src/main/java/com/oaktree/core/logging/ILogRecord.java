package com.oaktree.core.logging;

import java.util.logging.LogRecord;

import com.oaktree.core.logging.handlers.ILoggingHandler;

public interface ILogRecord {
	void setMillis(long millis);
	long getMillis();
	String getThreadName();
	void setThreadName(String name);
	int getThreadId();
	void setThreadId(int id);
	void setLogger(ILogger logger);
	ILogger getLogger();
	
	Level getLevel();
	void setLevel(Level level);
	LogRecord toLogRecord();
	void setMessage(String msg);
	String getMessage();
	void setCount(int size);
	void onProcessed(ILoggingHandler handler);
}
