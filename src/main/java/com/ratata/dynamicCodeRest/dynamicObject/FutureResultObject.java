package com.ratata.dynamicCodeRest.dynamicObject;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.ratata.dynamicCodeRest.controller.DynamicCodeRestEndpointController;

public class FutureResultObject extends Thread implements FutureResult {

	private DynamicObject obj;
	private String methodName;
	private Class<?>[] paramType;
	private Object[] paramData;
	private Object result;

	private boolean done = false;

	private Thread thread;
	private Integer timeOut;
	// logging
	private ByteArrayOutputStream baos = new ByteArrayOutputStream();
	private PrintStream log = new PrintStream(baos);

	public boolean isDone() {
		return done;
	}

	public String getLog() {
		return new String(baos.toByteArray(), StandardCharsets.UTF_8);
	}

	public String getObj() {
		return obj.getObjName();
	}

	public String getMethodName() {
		return methodName;
	}

	public Class<?>[] getParamType() {
		return paramType;
	}

	public Object[] getParamData() {
		return paramData;
	}

	public Integer getTimeOut() {
		return timeOut;
	}

	@Override
	public void run() {
		thread = Thread.currentThread();
		if (timeOut != 0) {
			CleanUpThread timeOutThread = new CleanUpThread();
			timeOutThread.setThreadInfo(thread, timeOut);
		}
		DynamicCodeRestEndpointController.futureResult.put(this.getName(), this);
		try {
			result = obj.callObjMethod(methodName, paramType, paramData);
		} catch (Exception e) {
			e.printStackTrace(log);
		}
		done = true;
	}

	public void setThreadInfo(DynamicObject obj, String methodName, Class<?>[] paramType, Object[] paramData,
			Integer timeOut) {
		this.obj = obj;
		this.methodName = methodName;
		this.paramType = paramType;
		this.paramData = paramData;
		this.timeOut = timeOut;
		setName(this.getObj().concat(":").concat(this.methodName).concat(":").concat(this.getClass().getTypeName())
				.concat(":").concat(String.valueOf(this.hashCode())));
		start();
	}

	public Object getResult() {
		return result;
	}

	@Override
	public Object getInfo() {
		Map<String, Object> info = new HashMap<String, Object>();
		info.put("objName", getObj());
		info.put("methodName", methodName);
		info.put("paramType", paramType);
		info.put("paramData", paramData);
		info.put("timeOut", timeOut);
		info.put("log", getLog());
		info.put("done", done);
		return info;
	}

}
