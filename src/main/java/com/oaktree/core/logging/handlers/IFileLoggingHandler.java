package com.oaktree.core.logging.handlers;

public interface IFileLoggingHandler extends ILoggingHandler {
	String getFilename();
	/**
	 * Set the filename to use in this filehandler. May process the file creation.
	 * @param filename
	 */
	void setFilename(String filename);
}
