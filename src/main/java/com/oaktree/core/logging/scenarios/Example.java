package com.oaktree.core.logging.scenarios;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Example {
	private final static Logger logger = LoggerFactory.getLogger(Example.class);
	
	public static void main(String[] args) throws Exception {
		logger.info("This is a test message");
		Thread.sleep(2000);
	}

}
