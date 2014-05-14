package org.slf4j.impl;

import org.slf4j.ILoggerFactory;

import com.oaktree.core.logging.LowLatencyLogManager;

public class StaticLoggerBinder {
	private StaticLoggerBinder() {
	}

	public ILoggerFactory getLoggerFactory() {
		return loggerFactory;
	}

	public String getLoggerFactoryClassStr() {
		return loggerFactoryClassStr;
	}

	static Class _mthclass$(String x0) throws Throwable {
		try {
			return Class.forName(x0);
		} catch (ClassNotFoundException x1) {
			throw new NoClassDefFoundError().initCause(x1);
		}
	}

	public static final StaticLoggerBinder SINGLETON = new StaticLoggerBinder();
	private static final String loggerFactoryClassStr;
	private final ILoggerFactory loggerFactory = new LowLatencyLogManager();

	static {
		loggerFactoryClassStr = LowLatencyLogManager.class.getName();
	}
	
	public static StaticLoggerBinder getSingleton() {
		return SINGLETON;
	}
}
