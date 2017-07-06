package com.ratata.dynamicCodeRest.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class ShareResourceFromSpring implements ShareResourceFromSpringInterface {
	@Autowired
	private ApplicationContext appContext;

	public static ApplicationContext shareAppContext;

	public void loadAllSharedObj() {
		ShareResourceFromSpring.shareAppContext = appContext;
	}
}
