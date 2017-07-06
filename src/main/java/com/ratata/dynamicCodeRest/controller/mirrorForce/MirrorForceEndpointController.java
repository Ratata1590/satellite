package com.ratata.dynamicCodeRest.controller.mirrorForce;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.ratata.dynamicCodeRest.utils.ErrorUtil;

@RequestMapping("/mirror")
@RestController
public class MirrorForceEndpointController {
	private static ConcurrentHashMap<String, ConcurrentLinkedQueue<byte[]>> queueList = new ConcurrentHashMap<String, ConcurrentLinkedQueue<byte[]>>();

	@RequestMapping(value = "/socketHandler", method = RequestMethod.POST)
	public static Object socketHandler(@RequestBody byte[] data, @RequestHeader(required = true) String sockRestId) {
		try {
			if (data.length != 0) {
				queueList.get(sockRestId).add(data);
			}
		} catch (Exception e) {
			return new ResponseEntity<>(ErrorUtil.printStackTrace(e), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequestMapping(value = "/socketHandler", method = RequestMethod.GET)
	public static Object socketHandler(@RequestHeader(required = true) String sockRestId) {
		try {
			byte[] data = queueList.get(sockRestId).poll();
			return data;
		} catch (Exception e) {
			return new ResponseEntity<>(ErrorUtil.printStackTrace(e), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(value = "/socketControl/create", method = RequestMethod.POST)
	public static String mirrorControlCreate() {
		String tmpsessionId = getSaltString();
		while (queueList.containsKey(tmpsessionId)) {
			tmpsessionId = getSaltString();
		}
		queueList.put(tmpsessionId, new ConcurrentLinkedQueue<byte[]>());
		return tmpsessionId;
	}

	@RequestMapping(value = "/socketControl/disconnect", method = RequestMethod.POST)
	public static void mirrorControlRemove(@RequestHeader String sessionId) {
		queueList.remove(sessionId);
	}

	@RequestMapping(value = "/socketList", method = RequestMethod.GET)
	public static Object socketList() {
		Iterator<String> iter = queueList.keySet().iterator();
		Map<String, Integer> result = new HashMap<String, Integer>();
		while (iter.hasNext()) {
			String key = iter.next();
			result.put(key, queueList.get(key).size());
		}
		return result;
	}

	@RequestMapping(value = "/socketClear", method = RequestMethod.GET)
	public static void socketClear() {
		queueList.clear();
	}

	public static String getSaltString() {
		String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
		StringBuilder salt = new StringBuilder();
		Random rnd = new Random();
		while (salt.length() < 18) { // length of the random string.
			int index = (int) (rnd.nextFloat() * SALTCHARS.length());
			salt.append(SALTCHARS.charAt(index));
		}
		String saltStr = salt.toString();
		return saltStr;
	}
}
