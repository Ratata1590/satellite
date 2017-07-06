package com.ratata.dynamicCodeRest.controller.mirrorForce;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BotPoolEndpointController {
	private static ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> botConnectionPool = new ConcurrentHashMap<String, ConcurrentLinkedQueue<String>>();

	@RequestMapping(value = "/botInit", method = RequestMethod.POST)
	public static void botInit(@RequestHeader String botName) {
		if (!botConnectionPool.containsKey(botName)) {
			botConnectionPool.put(botName, new ConcurrentLinkedQueue<String>());
		}
	}

	@RequestMapping(value = "/botCleanPool", method = RequestMethod.POST)
	public static void botCleanPool(@RequestHeader String botName) {
		if (botConnectionPool.containsKey(botName)) {
			for (String conn : botConnectionPool.get(botName)) {
				MirrorForceEndpointController.mirrorControlRemove(conn);
			}
			botConnectionPool.get(botName).clear();
		}
	}

	@RequestMapping(value = "/botCleanAllPool", method = RequestMethod.POST)
	public static void botCleanAllPool() {
		Iterator<String> iter = botConnectionPool.keySet().iterator();
		while (iter.hasNext()) {
			botCleanPool(iter.next());
		}
	}

	@RequestMapping(value = "/botRemove", method = RequestMethod.POST)
	public static void botRemove(@RequestHeader String botName) {
		botConnectionPool.remove(botName);
	}

	@RequestMapping(value = "/botList", method = RequestMethod.GET)
	public static Object botList() {
		return botConnectionPool;
	}

	@RequestMapping(value = "/botAddConnection", method = RequestMethod.POST)
	public static String botAddConnection(@RequestHeader String botName) {
		botInit(botName);
		String conn = MirrorForceEndpointController.mirrorControlCreate();
		botConnectionPool.get(botName).add(conn);
		return conn;
	}

	@RequestMapping(value = "/botGetConnection", method = RequestMethod.POST)
	public static String botGetConnection(@RequestHeader String botName) throws Exception {
		String botNode = botConnectionPool.get(botName).poll();
		String respNode = MirrorForceEndpointController.mirrorControlCreate();
		return botNode + ":" + respNode;
	}

	@RequestMapping(value = "/botPoll", method = RequestMethod.POST)
	public static String botPoll(@RequestHeader String botName) throws Exception {
		return botConnectionPool.get(botName).poll();
	}
}
