package com.ratata.dynamicCodeRest.controller;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ratata.dynamicCodeRest.dynamicObject.CleanUpThread;
import com.ratata.dynamicCodeRest.dynamicObject.DynamicObject;
import com.ratata.dynamicCodeRest.dynamicObject.FutureResult;
import com.ratata.dynamicCodeRest.dynamicObject.FutureResultClass;
import com.ratata.dynamicCodeRest.dynamicObject.FutureResultObject;
import com.ratata.dynamicCodeRest.inmemCompiler.InMemoryJavaCompiler;
import com.ratata.dynamicCodeRest.utils.DynamicCodeUtil;
import com.ratata.dynamicCodeRest.utils.ShareResourceFromSpringInterface;

@RestController
public class DynamicCodeRestEndpointController {
	@Autowired
	private ShareResourceFromSpringInterface shareResourceFromSpring;

	@PostConstruct
	private void initResource() {
		shareResourceFromSpring.loadAllSharedObj();
	}

	public static Map<String, String> sourceCodeList = new ConcurrentHashMap<String, String>();

	public static Map<String, Class<?>> classList = new ConcurrentHashMap<String, Class<?>>();

	public static Map<String, DynamicObject> objList = new ConcurrentHashMap<String, DynamicObject>();

	public static Map<String, FutureResult> futureResult = new ConcurrentHashMap<String, FutureResult>();

	public static final Map<String, JsonNode> configList = new HashMap<String, JsonNode>();

	public static final Map<String, ClassLoader> classLoaderList = new HashMap<String, ClassLoader>();

	@RequestMapping(value = "/newClass", method = RequestMethod.POST)
	public static void newClass(@RequestBody String classbody, @RequestHeader String className,
			@RequestHeader(required = false, defaultValue = "true") Boolean saveSource) throws Exception {
		Class<?> theClass = InMemoryJavaCompiler.compile(className, classbody);
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		DynamicCodeUtil.loadAllClass(theClass, classLoader);
		if (classList.containsKey(className)) {
			removeClass(className);
		}
		classList.put(className, theClass);
		if (saveSource) {
			sourceCodeList.put(className, classbody);
		}
		System.gc();
	}

	@RequestMapping(value = "/removeClass", method = RequestMethod.GET)
	public static void removeClass(@RequestHeader String className) throws Exception {
		classList.remove(className);
		sourceCodeList.remove(className);
		for (String obj : objList.keySet()) {
			if (obj.startsWith(className.concat(DynamicObject.SEPARATOR))) {
				objList.remove(obj);
			}
		}
		System.gc();
	}

	@RequestMapping(value = "/callClassMethod", method = RequestMethod.POST)
	public static Object callClassMethod(@RequestHeader(required = true) String className,
			@RequestHeader(required = true) String methodName,
			@RequestHeader(required = false, defaultValue = "false") Boolean download,
			@RequestHeader(required = false, defaultValue = "10") Integer timeOut,
			@RequestHeader(required = false, defaultValue = "false") Boolean async, HttpServletResponse response,
			@RequestBody Map<String, List<Object>> param) throws Exception {
		if (!classList.containsKey(className)) {
			throw new Exception("class " + className + " not found");
		}
		if (param.get("paramType").size() != param.get("paramData").size()) {
			throw new Exception("invalid type data pair");
		}
		Class<?>[] paramType = stringToClass(param.get("paramType"));
		Object[] paramData = resolveParam(paramType, param.get("paramData"));
		if (async) {
			FutureResultClass future = new FutureResultClass();
			future.setThreadInfo(className, methodName, paramType, paramData, timeOut);
			Thread.sleep(100);
			return future.getName();
		}
		if (timeOut != 0) {
			CleanUpThread timeOutThread = new CleanUpThread();
			timeOutThread.setThreadInfo(Thread.currentThread(), timeOut);
		}
		Object result = DynamicObject.callClassMethod(classList.get(className), methodName, paramType, paramData);

		if (download) {
			serveDownload(result, response);
			return null;
		}
		return result;
	}

	@RequestMapping(value = "/classList", method = RequestMethod.GET)
	public static Object classList(@RequestHeader(required = false, defaultValue = "false") Boolean showSource)
			throws Exception {
		ArrayList<Object> result = new ArrayList<Object>();
		if (showSource) {
			result.add(sourceCodeList);
		}
		String[] clazzList = classList.keySet().toArray(new String[classList.keySet().size()]);
		Arrays.sort(clazzList);
		result.add(clazzList);
		return result;
	}

	@RequestMapping(value = "/newObj", method = RequestMethod.POST)
	public static String newObj(@RequestHeader String className, @RequestBody Map<String, List<Object>> param)
			throws Exception {
		if (!classList.containsKey(className)) {
			throw new Exception("class " + className + " not found");
		}
		if (param.get("paramType").size() != param.get("paramData").size()) {
			throw new Exception("invalid type data pair");
		}
		Class<?>[] paramType = stringToClass(param.get("paramType"));
		Object[] paramData = resolveParam(paramType, param.get("paramData"));
		Object ob = DynamicCodeUtil.newObj(classList.get(className), paramType, paramData);
		String instanceId = className + DynamicObject.SEPARATOR + ob.hashCode();
		objList.put(instanceId, new DynamicObject(ob, instanceId));
		System.gc();
		return instanceId;
	}

	@RequestMapping(value = "/removeObj", method = RequestMethod.GET)
	public static void removeObj(@RequestHeader(required = true) String instanceId) throws Exception {
		objList.remove(instanceId);
		System.gc();
	}

	@RequestMapping(value = "/objList", method = RequestMethod.GET)
	public static Object objList() throws Exception {
		String[] result = objList.keySet().toArray(new String[objList.keySet().size()]);
		Arrays.sort(result);
		return result;
	}

	@RequestMapping(value = "/callObjMethod", method = RequestMethod.POST)
	public static Object callObjMethod(@RequestHeader(required = true) String instanceId,
			@RequestHeader(required = true) String methodName,
			@RequestHeader(required = false, defaultValue = "false") Boolean download,
			@RequestHeader(required = false, defaultValue = "10") Integer timeOut,
			@RequestHeader(required = false, defaultValue = "false") Boolean async, HttpServletResponse response,
			@RequestBody Map<String, List<Object>> param) throws Exception {
		if (!objList.containsKey(instanceId)) {
			throw new Exception("object " + instanceId + " not found");
		}
		if (param.get("paramType").size() != param.get("paramData").size()) {
			throw new Exception("invalid type data pair");
		}
		Class<?>[] paramType = stringToClass(param.get("paramType"));
		Object[] paramData = resolveParam(paramType, param.get("paramData"));
		DynamicObject obj = objList.get(instanceId);
		if (async) {
			FutureResultObject future = new FutureResultObject();
			future.setThreadInfo(obj, methodName, paramType, paramData, timeOut);
			Thread.sleep(100);
			return future.getName();
		}
		if (timeOut != 0) {
			CleanUpThread timeOutThread = new CleanUpThread();
			timeOutThread.setThreadInfo(Thread.currentThread(), timeOut);
		}
		Object result = obj.callObjMethod(methodName, paramType, paramData);
		if (download) {
			serveDownload(result, response);
			return null;
		}
		return result;
	}

	private static void serveDownload(Object obj, HttpServletResponse response) throws Exception {
		response.setContentType("application/x-msdownload");
		response.setHeader("Content-disposition", "attachment; filename=result");
		StreamUtils.copy((InputStream) obj, response.getOutputStream());
	}

	@RequestMapping(value = "/futureReturnList", method = RequestMethod.GET)
	public static Object futureReturnList() throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		for (String key : futureResult.keySet()) {
			resultMap.put(key, futureResult.get(key).getInfo());
		}
		return resultMap;
	}

	@RequestMapping(value = "/futureReturnGetLog", method = RequestMethod.GET)
	public static String futureReturnGetLog(@RequestHeader(required = true) String instanceId) throws Exception {
		if (!futureResult.containsKey(instanceId)) {
			throw new Exception("object " + instanceId + " not found");
		}
		return futureResult.get(instanceId).getLog();
	}

	@RequestMapping(value = "/futureReturnGetInfo", method = RequestMethod.GET)
	public static Object futureReturnGetInfo(@RequestHeader(required = true) String instanceId) throws Exception {
		if (!futureResult.containsKey(instanceId)) {
			throw new Exception("object " + instanceId + " not found");
		}
		return futureResult.get(instanceId).getInfo();
	}

	@RequestMapping(value = "/futureReturnGetResult", method = RequestMethod.GET)
	public static Object futureReturnGetResult(@RequestHeader(required = true) String instanceId,
			@RequestHeader(required = false, defaultValue = "false") Boolean download,
			@RequestHeader(required = false, defaultValue = "false") Boolean keep, HttpServletResponse response)
			throws Exception {
		if (!futureResult.containsKey(instanceId)) {
			throw new Exception("object " + instanceId + " not found");
		}
		if (download) {
			serveDownload(futureResult.get(instanceId).getResult(), response);
			return null;
		}
		Object result = futureResult.get(instanceId).getResult();
		if (!keep && futureResult.get(instanceId).isDone()) {
			futureResult.remove(instanceId);
		}
		return result;
	}

	private static Class<?>[] stringToClass(List<Object> data) throws Exception {
		Class<?>[] result = new Class<?>[data.size()];
		for (int i = 0; i < data.size(); i++) {
			result[i] = Thread.currentThread().getContextClassLoader().loadClass((String) data.get(i));
		}
		return result;
	}

	private static Object[] resolveParam(Class<?>[] dataType, List<Object> data) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		Object[] result = new Object[dataType.length];
		for (int i = 0; i < dataType.length; i++) {
			result[i] = mapper.convertValue(data.get(i), dataType[i]);
		}
		return result;
	}
}
