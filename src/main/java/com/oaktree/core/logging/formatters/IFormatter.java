package com.oaktree.core.logging.formatters;

import com.oaktree.core.logging.ILogRecord;

public interface IFormatter {
	String format(ILogRecord record);
}
