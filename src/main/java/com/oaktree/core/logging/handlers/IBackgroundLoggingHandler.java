package com.oaktree.core.logging.handlers;

public interface IBackgroundLoggingHandler extends ILoggingHandler {
	ILoggingHandler getHandler();
	void setHandler(ILoggingHandler handler);
}
