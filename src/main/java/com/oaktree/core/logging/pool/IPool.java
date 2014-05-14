package com.oaktree.core.logging.pool;

public interface IPool<T> {
	public T get();
	public void free(T record);
	public int getCapacity();
	public void setCapacity(int capacity);
}
