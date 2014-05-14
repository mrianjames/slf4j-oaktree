package com.oaktree.core.logging.binlog.write;

import com.oaktree.core.logging.handlers.AbstractBackgroundHandler;
import com.oaktree.core.logging.handlers.IFileLoggingHandler;

/**
 * A handler that takes messages and runs the serialising to the real handler in a background thread.
 * 
 * @author Oak Tree Designs Ltd
 *
 */
public class BackgroundBinaryLogFileHandler extends AbstractBackgroundHandler implements IFileLoggingHandler {

	public BackgroundBinaryLogFileHandler() {
		this.handler = new BinaryLoggingHandler();
	}

	@Override
	public String getFilename() {
		return ((BinaryLoggingHandler)handler).getFilename();
	}

	@Override
	public void setFilename(String filename) {
		((BinaryLoggingHandler)handler).setFilename(filename);	
	}

}
