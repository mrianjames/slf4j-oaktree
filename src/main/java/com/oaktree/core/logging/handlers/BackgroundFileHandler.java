package com.oaktree.core.logging.handlers;

/**
 * A handler that takes messages and runs the serialising to the real handler in a background thread.
 * 
 * @author Oak Tree Designs Ltd
 *
 */
public class BackgroundFileHandler extends AbstractBackgroundHandler implements IFileLoggingHandler {

	public BackgroundFileHandler() {
		this.handler = new FileHandler();
	}

	@Override
	public String getFilename() {
		return ((IFileLoggingHandler)handler).getFilename();
	}

	@Override
	public void setFilename(String filename) {
		((IFileLoggingHandler)this.handler).setFilename(filename);
	}

}
