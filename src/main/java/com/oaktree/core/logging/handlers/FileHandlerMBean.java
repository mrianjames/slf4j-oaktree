package com.oaktree.core.logging.handlers;

public interface FileHandlerMBean {
	void roll();
	void publish(String msg);
}
