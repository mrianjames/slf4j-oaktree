package com.oaktree.core.logging;

import com.oaktree.core.logging.formatters.ConsoleFormatter;
import com.oaktree.core.logging.handlers.ConsoleHandler;
import com.oaktree.core.logging.handlers.ILoggingHandler;
import com.oaktree.core.logging.handlers.IOutputStreamHandler;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.concurrent.CountDownLatch;

public class TestLogging {
	
	private final static DecimalFormat format = new DecimalFormat("#,###.##");
	private String propertyFileName = "oaktree.logging.properties";

	@Test
	public void testNoFileButGetLoggerAnyway() {
		LowLatencyLogManager.clear();
        
		LowLatencyLogger logger = (LowLatencyLogger)LoggerFactory.getLogger("com.oaktree.core.logging.testing");
		ILoggingHandler[] handlers = logger.getHandlers(); 
		Assert.assertEquals(1,handlers.length);		
		Assert.assertEquals(ConsoleHandler.class,handlers[0].getClass());
		logger.info("HELLO"); //check i appear.
		logger.debug("SATAN"); //check i dont.
	}
	
	@Test
	public void testHandlerAllocation() {
		System.setProperty(LowLatencyLogManager.LOGGING_FILE, this.propertyFileName );
        LowLatencyLogManager.initialiseFromProperies(true);
        
		LowLatencyLogger logger = (LowLatencyLogger)LoggerFactory.getLogger("com.oaktree.core.logging.testing");
		ILoggingHandler[] handlers = logger.getHandlers(); 
		Assert.assertEquals(1,handlers.length);
		logger = (LowLatencyLogger)LoggerFactory.getLogger("com.oaktree.core.logging");
		handlers = logger.getHandlers(); 
		Assert.assertEquals(handlers.length, 2);		
		
		//test inheriting handlers from parent. com.oaktree.core.logging
		logger = (LowLatencyLogger)LoggerFactory.getLogger("com.oaktree.core.logging.bootycall");
		handlers = logger.getHandlers(); 
		Assert.assertEquals(2,handlers.length);		
		
		//tests a logger handler defined in file after the loggers.
		logger = (LowLatencyLogger)LoggerFactory.getLogger("com.oaktree.core.logging.bape");
		handlers = logger.getHandlers(); 
		Assert.assertEquals(1,handlers.length);	
		Assert.assertEquals("zanother",handlers[0].getName());
		
		logger = (LowLatencyLogger)LoggerFactory.getLogger("");
		handlers = logger.getHandlers(); 
		Assert.assertEquals(1,handlers.length);	
		Assert.assertEquals("CONSOLE",handlers[0].getName());
		
		logger = (LowLatencyLogger)LoggerFactory.getLogger("root");
		handlers = logger.getHandlers(); 
		Assert.assertEquals(1,handlers.length);	
		Assert.assertEquals("FILE",handlers[0].getName());
	}
	
	@Test
	public void testLoggerLoggingLevels() { //doesnt work via maven. no idea why.
        System.setProperty(LowLatencyLogManager.LOGGING_FILE, this.propertyFileName );
        LowLatencyLogManager.initialiseFromProperies(true);        
		LowLatencyLogger logger = (LowLatencyLogger)LoggerFactory.getLogger(LowLatencyLogManager.class.getName());

		Assert.assertEquals(logger.getName(),LowLatencyLogManager.class.getName());
		//default info
		Assert.assertEquals(logger.getLevel(),Level.DEBUG.intValue());
		final StringBuilder buffer = new StringBuilder(100000);
		final String message = "hello";
		logger.addHandler(this.makeDebugHandler(buffer));		
		logger.debug(message);
		String[] bits = buffer.toString().split(" ");
		Assert.assertTrue(bits[1].equals(""+Thread.currentThread().getId()));
		Assert.assertTrue(bits[2].equals(Thread.currentThread().getName()));
		Assert.assertTrue(bits[3].equals("DEBUG"));
		Assert.assertTrue(bits[4].equals(message+"\n"));
		buffer.delete(0, buffer.length());
		
		logger.trace(message);
		bits = buffer.toString().split(" ");
		Assert.assertTrue(bits[1].equals(""+Thread.currentThread().getId()));
		Assert.assertTrue(bits[2].equals(Thread.currentThread().getName()));
		Assert.assertTrue(bits[3].equals("TRACE"));
		Assert.assertTrue(bits[4].equals(message+"\n"));
		buffer.delete(0, buffer.length());
		
		logger.info(message);
		bits = buffer.toString().split(" ");
		Assert.assertTrue(bits[1].equals(""+Thread.currentThread().getId()));
		Assert.assertTrue(bits[2].equals(Thread.currentThread().getName()));
		Assert.assertTrue(bits[3].equals("INFO"));
		Assert.assertTrue(bits[4].equals(message+"\n"));
		buffer.delete(0, buffer.length());

		logger.warn(message);
		bits = buffer.toString().split(" ");
		Assert.assertTrue(bits[1].equals(""+Thread.currentThread().getId()));
		Assert.assertTrue(bits[2].equals(Thread.currentThread().getName()));
		Assert.assertTrue(bits[3].equals("WARN"));
		Assert.assertTrue(bits[4].equals(message+"\n"));
		buffer.delete(0, buffer.length());

		logger.error(message);
		bits = buffer.toString().split(" ");
		Assert.assertTrue(bits[1].equals(""+Thread.currentThread().getId()));
		Assert.assertTrue(bits[2].equals(Thread.currentThread().getName()));
		Assert.assertTrue(bits[3].equals("ERROR"));
		Assert.assertTrue(bits[4].equals(message+"\n"));
		buffer.delete(0, buffer.length());

		/*
		 * change the loggers level to filter out stuff.
		 */
		logger.setLevel(Level.INFO);

		logger.info(message);
		bits = buffer.toString().split(" ");
		Assert.assertTrue(bits[1].equals(""+Thread.currentThread().getId()));
		Assert.assertTrue(bits[2].equals(Thread.currentThread().getName()));
		Assert.assertTrue(bits[3].equals("INFO"));
		Assert.assertTrue(bits[4].equals(message+"\n"));
		buffer.delete(0, buffer.length());
		
		logger.trace(message);
		Assert.assertTrue(buffer.length() == 0);
		
	}

    
	@Test
	public void testNoFileProperties() {
		System.setProperty(LowLatencyLogManager.LOGGING_FILE, "logging.properties.dontexist");
		LowLatencyLogger logger = (LowLatencyLogger) LoggerFactory.getLogger(LowLatencyLogManager.class.getName());
		logger.info("Hello");
	}

	@Test
	public void testNoProperties() {
		LowLatencyLogger logger = (LowLatencyLogger) LoggerFactory.getLogger(LowLatencyLogManager.class.getName());
		logger.info("Hello");
	}

	@Test
	public void testLLLoggingPerformanceMultiThread() {
		/*
		 * console logging, flush every write.
		 */
		System.setProperty(LowLatencyLogManager.LOGGING_FILE, this.propertyFileName);
		int THREADS = 5;
		final int TESTS = 20000;
		final CountDownLatch latch = new CountDownLatch(THREADS);
		final LowLatencyLogger logger = (LowLatencyLogger) LoggerFactory.getLogger(LowLatencyLogManager.class.getName());
		logger.setLevel(Level.DEBUG);
		Thread[] ts = new Thread[THREADS];
		for (int i = 0; i < THREADS; i++) {
			ts[i] = new Thread(new Runnable(){

				@Override
				public void run() {
					for (int i = 0; i < TESTS; i++) {
						logger.trace(Thread.currentThread().getName() + " Bollox to apache logging: " + i);
					}
					latch.countDown();
				}}); 
			ts[i].setName("THREAD" + i);
		}
		
		long s = System.nanoTime();
		for (int i = 0; i < THREADS; i++) {
			ts[i].start();
		}
		try {
			latch.await();
		} catch (InterruptedException e1) {
		}
		long e = System.nanoTime();
		long d = e-s;
		int count = TESTS * THREADS;
		double dm = ((double)d)/1000000;
//		double pm = dm/count; //avg duration in ms.
//		double ps = pm*1000;
		double avg = d/1000/count; //avg duration in us.
		System.out.println("Performed " + count + " LogWrites on " + THREADS + " threads in " + format.format(dm) + " millis avg: " + format.format(avg)+"us.");
	}
	@Test
	public void testLLLoggingPerformanceOneThread() {
		System.setProperty(LowLatencyLogManager.LOGGING_FILE, this.propertyFileName);
		int TESTS = 100000;
		LowLatencyLogger logger = (LowLatencyLogger) LoggerFactory.getLogger(LowLatencyLogManager.class.getName());
		logger.setLevel(Level.DEBUG);
		long s = System.nanoTime();
		for (int i = 0; i < TESTS; i++) {
			logger.trace("Bollox to apache logging: " + i);
		}
		long e = System.nanoTime();
		long d = e-s;
		double dm = ((double)d)/1000000;
		double pm = TESTS/dm;
		double ps = pm*1000;
		double avg = d/1000/((double)(TESTS));
		System.out.println("Performed " + TESTS + " LogWrites in " + format.format(dm) + " millis, avging "+avg+"us per write. Thats " + format.format(pm) + " per milli or " + format.format(ps) + " per sec.");		
	}

	
	/**
	 * Make an output stream that collects the data in the given stringbuilder.
	 * @param buffer
	 * @return
	 */
	private ILoggingHandler makeDebugHandler(final StringBuilder buffer) {
		IOutputStreamHandler handler = new ConsoleHandler(new ConsoleFormatter());
		handler.setLevel(Level.DEBUG);
		handler.setOutputStream(new OutputStream(){

			@Override
			public synchronized void write(int b) throws IOException {
				buffer.append((char)(b));				
			}});
		return handler;
	}


	
	@Test
	public void testConsoleHandler() {
		final StringBuilder buffer = new StringBuilder();
		IOutputStreamHandler handler = new ConsoleHandler(new ConsoleFormatter());
		final String MESSAGE = "hello";
		final CountDownLatch latch = new CountDownLatch(MESSAGE.length());
		handler.setOutputStream(new OutputStream(){

			@Override
			public void write(int b) throws IOException {
				buffer.append((char)(b));
				latch.countDown();
			}});
		handler.publish(this.createTestRecord(MESSAGE));
		try {
			latch.await();
		} catch (InterruptedException e) {			
		}
		Assert.assertTrue(buffer.length()>0);
		Assert.assertTrue(buffer.toString().contains(MESSAGE));
		System.out.print("Buffer: " + buffer.toString());
	}

	@Test
	public void testHandlerLogLevels() {
		final StringBuilder buffer = new StringBuilder();
		IOutputStreamHandler handler = new ConsoleHandler(new ConsoleFormatter());
		handler.setLevel(Level.INFO);
		final String MESSAGE = "hello";
		handler.setOutputStream(new OutputStream(){

			@Override
			public void write(int b) throws IOException {
				buffer.append((char)(b));				
			}});
		handler.publish(this.createDebugTestRecord(MESSAGE));

		Assert.assertTrue(buffer.length()==0);
		System.out.print("Buffer: " + buffer.toString());
		handler.setLevel(Level.DEBUG);
		handler.publish(this.createDebugTestRecord(MESSAGE));
		Assert.assertTrue(buffer.length()>0);
		Assert.assertTrue(buffer.toString().contains(MESSAGE));
		System.out.print("Buffer: " + buffer.toString());	
	}

	
	@Test
	public void testBackgroundConsoleHandler() {
		final StringBuilder buffer = new StringBuilder();
		IOutputStreamHandler handler = new ConsoleHandler(new ConsoleFormatter());
		final String MESSAGE = "hello";
		final CountDownLatch latch = new CountDownLatch(MESSAGE.length());
		handler.setOutputStream(new OutputStream(){

			@Override
			public void write(int b) throws IOException {
				buffer.append((char)(b));
				latch.countDown();
			}});
		handler.publish(this.createTestRecord(MESSAGE));
		try {
			latch.await();
		} catch (InterruptedException e) {			
		}
		Assert.assertTrue(buffer.length()>0);
		Assert.assertTrue(buffer.toString().contains(MESSAGE));
		System.out.print("Buffer: " + buffer.toString());
	}

	@Test
	public void testConsoleFormatter() {
		final String MESSAGE = "hello";
		ConsoleFormatter formatter = new ConsoleFormatter();
		String result = formatter.format(this.createTestRecord(MESSAGE));
		String[] bits = result.split(" ");
		Assert.assertTrue(bits[1].equals(""+Thread.currentThread().getId()));
		Assert.assertTrue(bits[2].equals(Thread.currentThread().getName()));
		Assert.assertTrue(bits[3].equals("INFO"));
		Assert.assertTrue(bits[4].equals(MESSAGE+"\n"));
	}
	
	@Test
	public void testConsoleFormatterSpeed() {
		ConsoleFormatter formatter = new ConsoleFormatter();
		int TESTS = 100000;
		ILogRecord[] msg = new ILogRecord[TESTS];
		for (int i = 0; i < TESTS; i++) {
			msg[i] = this.createTestRecord(""+i);
		}
		long s = System.nanoTime();
		for (int i = 0; i < TESTS; i++) {
			formatter.format(msg[i]);
		}
		long e = System.nanoTime();
		double d = (e-s)/1000000d;
		double x = (TESTS/d)*1000d;
		System.out.println("ConsoleFormatter formats " + TESTS + " in " + format.format(d) + " ms. Rate ps:" + format.format(x));
	}

	
	private ILogRecord createTestRecord(String message) {
		return this.createTestRecord(message,Level.INFO);
	}

	private ILogRecord createDebugTestRecord(String message) {
		return this.createTestRecord(message,Level.DEBUG);
	}
	
	private ILogRecord createTestRecord(String message, Level level) {
		return new LogRecord(message,level,System.currentTimeMillis(),Thread.currentThread().getId(),Thread.currentThread().getName(),null);
	}
}
