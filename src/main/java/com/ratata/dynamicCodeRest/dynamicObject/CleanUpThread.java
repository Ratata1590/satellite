package com.ratata.dynamicCodeRest.dynamicObject;

import java.util.concurrent.TimeUnit;

public class CleanUpThread extends Thread {
	private Thread thread;
	private Integer timeOut;

	@SuppressWarnings("deprecation")
	@Override
	public void run() {
		try {
			while (timeOut > 0) {
				TimeUnit.SECONDS.sleep(1);
				timeOut--;
			}
			thread.interrupt();
			if (!thread.isInterrupted()) {
				thread.stop();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	public void setThreadInfo(Thread thread, Integer timeOut) {
		this.thread = thread;
		this.timeOut = timeOut;
		start();
	}
}
