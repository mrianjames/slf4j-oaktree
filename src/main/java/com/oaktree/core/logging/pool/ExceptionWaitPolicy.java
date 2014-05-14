package com.oaktree.core.logging.pool;

public class ExceptionWaitPolicy<T> implements IWaitPolicy<T> {

	@Override
	public T wait(SimplePool<T> pool) {
		throw new IllegalStateException("No free object could be found");
	}

}
