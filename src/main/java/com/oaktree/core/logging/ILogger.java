package com.oaktree.core.logging;

import java.util.logging.Level;

import org.slf4j.Logger;

import com.oaktree.core.logging.handlers.ILoggingHandler;

public interface ILogger extends Logger{

	void setHandlers(ILoggingHandler[] handlers);
	void setLevel(Level level);
	void setLevel(int level);

	void log(String message, Level level);
	void finest(String message);
	void finer(String message);
	void fine(String message);
	void info(String message);
	int getLevel();
	void onFree(ILogRecord logRecord);	
}
