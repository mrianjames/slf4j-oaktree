package com.oaktree.core.logging;

/**
 * Exposed methods of an oaktree log manager in jmx.
 * @author Oak Tree Designs Ltd
 *
 */
public interface LowLatencyLogManagerMBean {
	boolean doesLoggerExist(String logger);
	void setLoggerLevel(String loggerLevel, int level);
	void setLoggerLevel(String loggerLevel, String level);
	int getLoggerLevel(String loggerLevel);
}
