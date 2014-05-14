package com.oaktree.core.logging.handlers;

import com.oaktree.core.logging.ILogRecord;
import com.oaktree.core.logging.Level;
import com.oaktree.core.logging.formatters.ConsoleFormatter;
import com.oaktree.core.logging.formatters.IFormatter;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Abstraction of handlers that push the data to an output stream. The specialisation only comes in making/getting
 * the output stream to write to in the method getOutputStream. For example, something that wishes to write to
 * the console would return stdout.
 * 
 */

public abstract class AbstractOutputStreamHandler implements IOutputStreamHandler {

	/**
	 * We will write to a file, the name of which is set after construction; we will open the file and 
	 * create the outputstream when this property is set.
	 */
	protected OutputStream stream;	
	
	private String name;

	/**
	 * Formatter we will modify log records into byte sequences (string!)
	 */
	private IFormatter formatter = new ConsoleFormatter();
	/**
	 * Level for handler.
	 */
	protected Level level = Level.INFO;

	private Charset encoding = Charset.defaultCharset();
	public void setEncoding(String charset) {
		this.encoding = Charset.forName(charset);
	}
	
	public AbstractOutputStreamHandler() {
		this.registerJMX();
	}

	public AbstractOutputStreamHandler(IFormatter formatter) {
		this();
		this.formatter = formatter;		
	}
	
	@Override
	public void publish(ILogRecord record) {
		try {
			if (record == null || this.level.intValue() > record.getLevel().intValue()) {
				return;
			}
			this.publish(this.formatter.format(record));			
			record.onProcessed(this);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	/**
	 * write the msg to the stream.
	 * @param msg
	 */
	public void publish(String msg) {
		try {
			this.getOutputStream().write(msg.getBytes(encoding ));			
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void setFormatter(IFormatter f) {
		this.formatter = f;
	}

	@Override
	public void setLevel(Level l) {
		this.level = l;
	}

	@Override
	public void publish(List<ILogRecord> batch) {
		StringBuilder buffer = new StringBuilder();
		for (ILogRecord record:batch) {
			if (record == null || this.level.intValue() > record.getLevel().intValue()) {
			} else {
				buffer.append(this.formatter.format(record));
			}
			record.onProcessed(this); //should be fine to free this now - we have taken the data already.
		}
		this.publish(buffer.toString());
	}

	@Override
	public void setOutputStream(OutputStream stream) {
		this.stream = stream;
	}
	
	@Override
	public OutputStream getOutputStream() {
		return this.stream;
	}
	
	@Override
	public void flush() {
		try {
			this.stream.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void stop() {
		try {
			this.stream.flush();
			this.stream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Attempt to register as jmx. only handlers that correctly implement a named interface
	 * e.g. FileHandlerMBean will qualify for this great honour of jmxability.
	 */
	protected void registerJMX() {
		MBeanServer mbs =ManagementFactory.getPlatformMBeanServer();
		ObjectName name;
		try {
			name = new ObjectName("Logging" + ":type="
					+ "Oaktree" + ",name=" + this.getClass().getSimpleName());
			mbs.registerMBean(this, name);
		} catch (InstanceAlreadyExistsException x) {			
		} 
		catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void start() {}

	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name + " formatter: " + formatter;
	}
}
