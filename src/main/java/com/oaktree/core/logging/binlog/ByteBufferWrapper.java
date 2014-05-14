package com.oaktree.core.logging.binlog;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Byte buffers are unpoolable as their equals matches any other same sized buffer thats empty.
 * This wraps it and provides an equals that is useful for checking.
 * TODO make this generic wrapper we always use?
 * 
 * @author ianjames
 *
 */
public class ByteBufferWrapper {
	private ByteBuffer buffer;
	private static AtomicInteger idgen = new AtomicInteger(0);
	private int id;
	public int getId() {
		return id;
	}
	public ByteBufferWrapper(ByteBuffer buffer) {
		this.buffer = buffer;
		this.id = idgen.getAndIncrement();		
	}
	public ByteBuffer getByteBuffer() {
		return buffer;
	}
	@Override
	public int hashCode() {
		return id;
	}
	@Override
	public boolean equals(Object o) {
		if (o instanceof ByteBufferWrapper) {
			ByteBufferWrapper bbw = (ByteBufferWrapper)(o);
			return bbw.getId() == id;
		}
		return false;
	}
}
