package com.ratata.dynamicCodeRest.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ratata.dynamicCodeRest.dynamicObject.ProcessThread;
import com.ratata.dynamicCodeRest.utils.ErrorUtil;

@RestController
public class CMDController {

	private static final String Mode_OUT = "out";
	private static final String Mode_ERROR = "error";

	public static final ConcurrentHashMap<String, ProcessThread> processList = new ConcurrentHashMap<String, ProcessThread>();

	@RequestMapping(value = "/newProcess", method = RequestMethod.POST)
	public static Object newProcess(@RequestBody List<String> command) throws Exception {
		String processId = getSaltString();
		while (processList.containsKey(processId)) {
			processId = getSaltString();
		}
		ProcessThread newProcess = new ProcessThread(command);
		processList.put(processId, newProcess);
		return processId;
	}

	@RequestMapping(value = "/sendCommand", method = RequestMethod.POST)
	public static Object sendCommand(@RequestHeader String processId, @RequestBody String command) {
		if (!processList.containsKey(processId)) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		try {
			processList.get(processId).sendInput(command);
		} catch (Exception e) {
			return new ResponseEntity<>(ErrorUtil.printStackTrace(e), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequestMapping(value = "/getOutput", method = RequestMethod.GET)
	public static Object getOutput(@RequestParam String processId, @RequestParam(defaultValue = Mode_OUT) String mode) {
		if (!processList.containsKey(processId)) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		try {
			if (mode.equals(Mode_ERROR)) {
				return processList.get(processId).getError();
			} else {
				return processList.get(processId).getOutput();
			}
		} catch (Exception e) {
			return new ResponseEntity<>(ErrorUtil.printStackTrace(e), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(value = "/closeProcess", method = RequestMethod.DELETE)
	public static Object closeProcess(@RequestHeader String processId) {
		if (!processList.containsKey(processId)) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		processList.get(processId).closeProcess();
		processList.remove(processId);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequestMapping(value = "/clearProcess", method = RequestMethod.DELETE)
	public static Object clearProcess() {
		for (String processId : processList.keySet()) {
			processList.get(processId).closeProcess();
			processList.remove(processId);
		}
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequestMapping(value = "/processList", method = RequestMethod.GET)
	public static Object getProcessList() {
		List<Object> result = new ArrayList<Object>();
		for (String key : processList.keySet()) {
			ProcessThread thread = processList.get(key);
			Map<String, Object> node = new HashMap<String, Object>();
			node.put("id", key);
			node.put("command", thread.getCommand());
			node.put("internalError", thread.getInternalError());
			node.put("ended", thread.getEnded());
			result.add(node);
		}
		return result;
	}

	public static String getSaltString() {
		String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
		StringBuilder salt = new StringBuilder();
		Random rnd = new Random();
		while (salt.length() < 100) { // length of the random string.
			int index = (int) (rnd.nextFloat() * SALTCHARS.length());
			salt.append(SALTCHARS.charAt(index));
		}
		String saltStr = salt.toString();
		return saltStr;
	}
}
