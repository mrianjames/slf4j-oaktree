package com.oaktree.core.logging.handlers;

import com.oaktree.core.logging.ILogRecord;
import com.oaktree.core.logging.Level;
import com.oaktree.core.logging.formatters.IFormatter;

import java.util.List;

/**
 * Handle the publication of oak tree log records. Examples of these are console handler,
 * file handler etc.
 * 
 * 
 * @author Oak Tree Designs Ltd.
 *
 */
public interface ILoggingHandler {
	/**
	 * publish an oak tree log record
	 * @param record
	 */
	void publish(ILogRecord record);

	/**
	 * Inject the formatter
	 * @param f
	 */
	void setFormatter(IFormatter f);

	/**
	 * Set a level below which we do not handle messages
	 * @param l
	 */
	void setLevel(Level l);

	/**
	 * Publish a batch of log records.
	 * @param batch
	 */
	void publish(List<ILogRecord> batch);

	/**
	 * Go. Possibly does nothing but dependant on the handler implementation. You may for example
	 * open a file or other resource you wish to pump log records down to.
	 */
	void start();
	
	/**
	 * Stop this logger (for example a background resource).
	 */
	void stop();

	String getName();
	
	void setName(String name);

}
