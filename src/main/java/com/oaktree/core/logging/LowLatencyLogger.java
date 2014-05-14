package com.oaktree.core.logging;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.Marker;

import com.oaktree.core.logging.handlers.ILoggingHandler;
import com.oaktree.core.logging.pool.IObjectFactory;
import com.oaktree.core.logging.pool.IPool;
import com.oaktree.core.logging.pool.SimplePool;

/**
 * A particularly fast version of a logger. No locking; just gives the log record to 
 * the handlers.
 * This is an slf4j logger which means it can be replaced with other implementations if required.
 * 
 * Adding thread information (thread id and name, unlike other loggers) is an optional cost you
 * can configure to be turned off; this is a better way of doing this compared to logback lazy
 * thread name retreival as this means you cannot do background handling/formatting.
 * 
 * @author jameian
 *
 */
public class LowLatencyLogger implements Logger,ILogger {

	private String name;
	/**
	 * A collection of objects that will do the publication of log records. By default we define
	 * a console handler with its associated basic console formatter.
	 */
	private Set<ILoggingHandler> handlers = new HashSet<ILoggingHandler>();
	
	/**
	 * The logging level filter.
	 */
	private volatile int level;
	/**
	 * Sometimes thread is not required; this safes a native call if disabled in properties.
	 */
	private boolean getThreadInformation = true;
	/**
	 * TODO Might want to turn this off at some point.
	 */
	private static boolean verbose = true;
	
	public LowLatencyLogger(String name,ILoggingHandler defaultHandler) {
		this.name = name;
		if (defaultHandler != null) {
			handlers.add(defaultHandler);
		}
	}
	public LowLatencyLogger(String name, boolean getThreadInformation, ILoggingHandler defaultHandler) {
		this(name, defaultHandler);
		this.getThreadInformation = getThreadInformation;
	}
	public void setHandlers(ILoggingHandler[] handlers) {
		this.handlers.clear();
		for (ILoggingHandler handler:handlers) {
			this.handlers.add(handler);
		}
	}
	public void setLevel(Level level) {
		
        if (level == null) {
            throw new IllegalArgumentException("Invalid (null) logging level supplied");
        }
		this.level = level.intValue();
		if (verbose ) {
			System.out.println("Logger " + getName() + " is set to level " + level);
		}
	}
	public void setLevel(int level) {
		this.level = level;
		if (verbose ) {
			System.out.println("Logger " + getName() + " is set to level " + Level.fromInt(level));
		}
	}
	public void fine(String message) {
		this.log(message, Level.TRACE);
	}
	public void finer(String message) {
		this.log(message, Level.DEBUG);
	}
	public void finest(String message) {
		this.log(message, Level.DEBUG);
	}
	@Override
	public void info(String message) {
		this.log(message, Level.INFO);	
	}
	
	/**
	 * This is where we log; create log record and delegate to all handlers in sequence.
	 * Thread information is dependant on the getThreadInformation configuration option which
	 * defaults to true.
	 * 
	 * @param message
	 * @param level
	 */
	public void log(String message, Level level) {
		if (this.level > level.intValue()) {
			return;
		}
		String tname = null;
		long tid = 0;
		if (getThreadInformation) {
			Thread thread =  Thread.currentThread();
			tname = thread.getName();
			tid = thread.getId();
		}
		ILogRecord rec = makeLogRecord(message,level,(int)tid,tname);
		for (ILoggingHandler handler: this.handlers) {
			handler.publish(rec);
		}

	}
	
	//TODO configure me better please.
	private IPool<ILogRecord> lrs = new SimplePool<ILogRecord>(10,new IObjectFactory<ILogRecord>(){

		@Override
		public ILogRecord make() {
			return new LogRecord();
		}});
	//POOL: Performed 100000 LogWrites on 5 threads in 4,734.18 millis avg: 47us. Thats 0.05 per milli or 47.34 per sec.
	//OLD: Performed 100000 LogWrites on 5 threads in 4,719.02 millis avg: 47us. Thats 0.05 per milli or 47.19 per sec.
	private ILogRecord makeLogRecord(String message, Level level,int tid,String tname) {
//		return new LogRecord(message,level,System.currentTimeMillis(),tid,tname,this);
		ILogRecord lr = lrs.get();
		lr.setLogger(this);
		lr.setLevel(level);
		lr.setMessage(message);
		lr.setThreadId(tid);
		lr.setMillis(getTime());
		lr.setThreadName(tname);
		lr.setCount(handlers.size()); 
		return lr;
	}
	
	protected long getTime() {
		return System.currentTimeMillis(); //TODO enable something else to be specified.
	}
	public String getName() {
		return name;
	}
	public int getLevel() {
		return this.level;
	}
	@Override
	public void debug(String message) {
		this.log(message, Level.DEBUG);
	}
	@Override
	public void debug(String message, Object arg1) {
		this.log(message, Level.DEBUG);
	}
	@Override
	public void debug(String message, Object[] arg1) {
		this.log(message, Level.DEBUG);
	}
	@Override
	public void debug(String message, Throwable arg1) {
		this.log(message, Level.DEBUG);
	}
	@Override
	public void debug(Marker message, String arg1) {
		this.log(message.toString(), Level.DEBUG);
	}
	@Override
	public void debug(String message, Object arg1, Object arg2) {
		this.log(message, Level.DEBUG);
	}
	@Override
	public void debug(Marker mkr, String arg1, Object arg2) {
		this.log(mkr.toString(), Level.DEBUG);
	}
	@Override
	public void debug(Marker arg0, String arg1, Object[] arg2) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void debug(Marker arg0, String arg1, Throwable arg2) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void debug(Marker arg0, String arg1, Object arg2, Object arg3) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void error(String message) {
		this.log(message, Level.ERROR);
	}
	@Override
	public void error(String message, Object arg1) {
		this.log(message, Level.ERROR);
	}
	@Override
	public void error(String message, Object[] arg1) {
		this.log(message, Level.ERROR);
	}
	@Override
	public void error(String message, Throwable arg1) {
		
		for (StackTraceElement ste:arg1.getStackTrace()) {
			message += "\n\t";
			message += ste.getMethodName() + " ("+ste.getClassName()+":"+ste.getLineNumber()+")";
		}
		this.log(message, Level.ERROR);
	}
	@Override
	public void error(Marker arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void error(String message, Object arg1, Object arg2) {
		this.log(message, Level.ERROR);
	}
	@Override
	public void error(Marker arg0, String arg1, Object arg2) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void error(Marker arg0, String arg1, Object[] arg2) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void error(Marker arg0, String arg1, Throwable arg2) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void error(Marker arg0, String arg1, Object arg2, Object arg3) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void info(String message, Object arg1) {
		this.log(message, Level.INFO);
	}
	@Override
	public void info(String message, Object[] arg1) {
		this.log(message, Level.INFO);
	}
	@Override
	public void info(String message, Throwable arg1) {
		this.log(message, Level.INFO);
	}
	@Override
	public void info(Marker arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void info(String message, Object arg1, Object arg2) {
		this.log(message, Level.INFO);
	}
	@Override
	public void info(Marker arg0, String arg1, Object arg2) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void info(Marker arg0, String arg1, Object[] arg2) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void info(Marker arg0, String arg1, Throwable arg2) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void info(Marker arg0, String arg1, Object arg2, Object arg3) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public boolean isDebugEnabled() {
		return this.level <= Level.DEBUG.intValue();
	}
	@Override
	public boolean isDebugEnabled(Marker arg0) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean isErrorEnabled() {
		return this.level <= Level.ERROR.intValue();
	}
	@Override
	public boolean isErrorEnabled(Marker arg0) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean isInfoEnabled() {
		return this.level <= Level.INFO.intValue();
	}
	@Override
	public boolean isInfoEnabled(Marker arg0) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean isTraceEnabled() {
		return this.level <= Level.TRACE.intValue();
	}
	@Override
	public boolean isTraceEnabled(Marker arg0) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean isWarnEnabled() {
		return this.level >= Level.WARN.intValue();
	}
	@Override
	public boolean isWarnEnabled(Marker arg0) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public void trace(String message) {
		this.log(message, Level.TRACE);
	}
	@Override
	public void trace(String message, Object arg1) {
		this.log(message, Level.TRACE);
	}
	@Override
	public void trace(String message, Object[] arg1) {
		this.log(message, Level.TRACE);
	}
	@Override
	public void trace(String message, Throwable arg1) {
		this.log(message, Level.TRACE);
	}
	@Override
	public void trace(Marker arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void trace(String message, Object arg1, Object arg2) {
		this.log(message, Level.TRACE);
	}
	@Override
	public void trace(Marker arg0, String arg1, Object arg2) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void trace(Marker arg0, String arg1, Object[] arg2) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void trace(Marker arg0, String arg1, Throwable arg2) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void trace(Marker arg0, String arg1, Object arg2, Object arg3) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void warn(String message) {
		this.log(message, Level.WARN);
	}
	@Override
	public void warn(String message, Object arg1) {
		this.log(message, Level.WARN);
	}
	@Override
	public void warn(String message, Object[] arg1) {
		this.log(message, Level.WARN);
	}
	@Override
	public void warn(String message, Throwable arg1) {
		this.log(message, Level.WARN);
	}
	@Override
	public void warn(Marker arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void warn(String message, Object arg1, Object arg2) {
		this.log(message, Level.WARN);
	}
	@Override
	public void warn(Marker arg0, String arg1, Object arg2) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void warn(Marker arg0, String arg1, Object[] arg2) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void warn(Marker arg0, String arg1, Throwable arg2) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void warn(Marker arg0, String arg1, Object arg2, Object arg3) {
		// TODO Auto-generated method stub
		
	}
	public void clearHandlers() {
		handlers.clear();
	}
	public void addHandler(ILoggingHandler handler) {
		if (handler == null) {
			return;
		}
		handlers.add(handler);
		System.out.println("Adding handler " + handler.getName() + " to logger " + getName());
	}
	ILoggingHandler[] getHandlers() {
		return handlers.toArray(new ILoggingHandler[handlers.size()]);
	}

	@Override
	public String toString() {
		return name + " handlers: " + handlers;
	}
	@Override
	public void setLevel(java.util.logging.Level level) {
		this.setLevel(Level.parseLegacy(level.getName()));
	}
	@Override
	public void log(String message, java.util.logging.Level level) {
		log(message,Level.parseLegacy(level.getName()));
	}
	@Override
	public void onFree(ILogRecord logRecord) {
		lrs.free(logRecord);
	}
	
}
