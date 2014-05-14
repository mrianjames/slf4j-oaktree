package com.oaktree.core.logging.handlers;

import java.io.OutputStream;

/**
 * A logging handler that uses an outpustream to write to.
 * 
 * @author jameian
 *
 */
public interface IOutputStreamHandler extends ILoggingHandler {
	OutputStream getOutputStream();
	void setOutputStream(OutputStream os);
	void flush();
}
