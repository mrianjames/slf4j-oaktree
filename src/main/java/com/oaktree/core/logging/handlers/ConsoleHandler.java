package com.oaktree.core.logging.handlers;

import java.io.OutputStream;

import com.oaktree.core.logging.formatters.ConsoleFormatter;


/**
 * For writing directly to the console. Flushes immediately to the writer.
 * This handler has no explicit locking though the PrintStream it uses will use some
 * to ensure entries enter the buffer as contiguous entries.
 * 
 * The handler has a level that can be set; messages with a level that is greater than the set
 * level will be ignored.
 */

public class ConsoleHandler extends AbstractOutputStreamHandler implements ConsoleHandlerMBean {
	

	public ConsoleHandler() {
		super();
		/**
		 * We write to STDOUT. Even exception stack traces go here.
		 */
		stream = System.out;

	}
	public ConsoleHandler(ConsoleFormatter formatter) {
		super(formatter);
		/**
		 * We write to STDOUT. Even exception stack traces go here.
		 */
		this.stream = System.out;

	}

	@Override
	public OutputStream getOutputStream() {
		return this.stream;
	}
	@Override
	public void setOutputStream(OutputStream os) {
		this.stream = os;
	}
	
}
