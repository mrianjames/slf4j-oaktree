package com.oaktree.core.logging.handlers;

/**
 * A handler that takes messages and runs the serialising to the real handler in a background thread.
 * 
 * @author Oak Tree Designs Ltd
 *
 */
public class BackgroundConsoleHandler extends AbstractBackgroundHandler {

	public BackgroundConsoleHandler() {
		handler = new ConsoleHandler();
	}	
}
