package com.oaktree.core.logging.formatters;

import com.oaktree.core.logging.ILogRecord;
import com.oaktree.core.logging.Level;
import com.oaktree.core.logging.Text;


/**
 * A formatter of log records; this has a non synchronized format method that keeps a map of
 * date formatters, 1 per thread to ensure thread safety. This format method
 * is very heavily used and has to operate as fast as possible with many threads using
 * it simultaneously.
 * 
 * @author Oak Tree Designs Ltd
 */

public class MinimalFormatter implements IFormatter{


		
	private long today;

	/**
	 * Create a new formatter.
	 */
	public MinimalFormatter() {
		super();
		this.today = Text.getToday();
	}


	
	protected void processNewLine(StringBuilder buffer) {
		buffer.append('\n');
	}

	@Override
	public String format(ILogRecord record) {
		
		StringBuilder buffer = new StringBuilder(240);
		/*
		 * Format the time using the reusable date object.
		 */
		long millis = record.getMillis() - this.today;
		buffer.append(millis);
		buffer.append(Text.SPACE);
		/*
		 * The thread.
		 */
		buffer.append(record.getThreadId());
		buffer.append(Text.SPACE);
		
		buffer.append(record.getMessage());
		buffer.append(Text.NEW_LINE);
		return buffer.toString();
	}

	@Override
	public String toString() {
		return getClass().getName();
	}

}
