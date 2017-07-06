package com.ratata.dynamicCodeRest.controller;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.ratata.dynamicCodeRest.utils.ErrorUtil;

@RestController
public class SocketRestEndpointController {

	private static Map<String, Socket> sessionList = new ConcurrentHashMap<String, Socket>();
	private static Map<String, String> configList = new ConcurrentHashMap<String, String>();

	@RequestMapping(value = "/socketHandler", method = RequestMethod.POST)
	public static Object socketHandler(@RequestBody byte[] data, @RequestHeader(required = true) String sockRestId) {
		try {
			Socket socket = sessionList.get(sockRestId);
			if (data.length != 0) {
				try {
					OutputStream out = socket.getOutputStream();
					out.write(data);
					out.flush();
				} catch (Exception e) {
					try {
						socket.close();
					} catch (Exception e2) {
					}
					sessionList.remove(sockRestId);
					throw new Exception("socket closed");
				}
			}
		} catch (Exception e) {
			return new ResponseEntity<>(ErrorUtil.printStackTrace(e), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequestMapping(value = "/socketHandler", method = RequestMethod.GET)
	public static Object socketHandler(@RequestHeader(required = true) String sockRestId) {
		try {
			Socket socket = sessionList.get(sockRestId);
			byte[] resultBuff;
			byte[] tmpbuff = null;
			try {
				resultBuff = new byte[socket.getReceiveBufferSize()];
				tmpbuff = new byte[socket.getInputStream().read(resultBuff, 0, resultBuff.length)];
				System.arraycopy(resultBuff, 0, tmpbuff, 0, tmpbuff.length);
			} catch (Exception e) {
				try {
					socket.close();
				} catch (Exception e2) {
				}
				sessionList.remove(sockRestId);
				throw new Exception("socket closed");
			}
			return tmpbuff;
		} catch (Exception e) {
			return new ResponseEntity<>(ErrorUtil.printStackTrace(e), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(value = "/socketControl/connect", method = RequestMethod.POST)
	public static String socketControlConnect(@RequestHeader String host, @RequestHeader Integer port)
			throws Exception {
		String tmpsessionId = getSaltString();
		while (sessionList.containsKey(tmpsessionId)) {
			tmpsessionId = getSaltString();
		}
		Socket sock = openSocket(host, port);
		sessionList.put(tmpsessionId, sock);
		configList.put(tmpsessionId, host + ":" + port);
		return tmpsessionId;
	}

	@RequestMapping(value = "/socketControl/disconnect", method = RequestMethod.POST)
	public static void socketControlDisconnect(@RequestHeader String sessionId) throws Exception {
		try {
			sessionList.get(sessionId).close();
		} catch (Exception e) {
		}
		sessionList.remove(sessionId);
		configList.remove(sessionId);
	}

	@RequestMapping(value = "/socketList", method = RequestMethod.GET)
	public static Object socketList() {
		return configList;
	}

	@RequestMapping(value = "/socketClear", method = RequestMethod.GET)
	public static void socketClear() {
		for (String id : sessionList.keySet()) {
			try {
				sessionList.get(id).close();
			} catch (Exception e) {
			}
			sessionList.remove(id);
			configList.remove(id);
		}
	}

	private static Socket openSocket(String server, int port) throws Exception {
		Socket socket;
		try {
			InetAddress inteAddress = InetAddress.getByName(server);
			SocketAddress socketAddress = new InetSocketAddress(inteAddress, port);
			socket = new Socket();
			int timeoutInMs = 10 * 1000;
			socket.connect(socketAddress, timeoutInMs);
			return socket;
		} catch (SocketTimeoutException ste) {
			ste.printStackTrace();
			throw ste;
		}
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
