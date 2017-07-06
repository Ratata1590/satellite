package com.ratata.dynamicCodeRest.dynamicObject;

public interface FutureResult {
	public boolean isDone();

	public Object getResult();

	public String getLog();

	public Object getInfo();
}
