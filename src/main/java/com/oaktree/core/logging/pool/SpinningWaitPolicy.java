package com.oaktree.core.logging.pool;

public class SpinningWaitPolicy<T> implements IWaitPolicy<T> {

	@Override
	public T wait(SimplePool<T> pool) {
		T obj = null;
		while (obj == null) {
			obj = pool.getFreeObject();
		}
		return obj;
	}

}
