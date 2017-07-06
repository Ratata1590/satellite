package com.ratata.dynamicCodeRest.controller;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

@RestController
public class GenToolMavenRepo {

	private final XmlMapper xmlMapper = new XmlMapper();
	public static final ObjectMapper mapper = new ObjectMapper();

	public static final String keyGroupId = "groupId";
	public static final String keyArtifactId = "artifactId";
	public static final String keyVersion = "version";

	@RequestMapping(value = "/lookUpClass", method = RequestMethod.POST)
	public Object lookUpClass(@RequestBody(required = true) JsonNode classNameList,
			@RequestHeader String classLoaderName) throws Exception {
		Map<String, Object> result = new HashMap<String, Object>();
		ClassLoader theClassLoader = DynamicCodeRestEndpointController.classLoaderList.get(classLoaderName);
		for (JsonNode cln : classNameList) {
			try {
				Class<?> theClass = theClassLoader.loadClass(cln.asText());
				result.put(cln.asText(), listClassItems(theClass));
			} catch (Exception e) {
				result.put(cln.asText(), false);
			}
		}
		System.gc();
		return result;
	}

	/**
	 * junit,junit,3.8.1,junit-3.8.1.jar
	 * org,springframework,spring-webmvc,4.0.1.RELEASE,spring-webmvc-4.0.1.RELEASE.jar
	 * org,springframework,spring-beans,4.0.1.RELEASE,spring-beans-4.0.1.RELEASE.jar
	 * org,springframework,spring-context,4.0.1.RELEASE,spring-context-4.0.1.RELEASE.jar
	 * org,springframework,spring-core,4.0.1.RELEASE,spring-core-4.0.1.RELEASE.jar
	 **/
	@RequestMapping(value = "/convertJarListToJson", method = RequestMethod.POST)
	public Object convertJarListToJson(@RequestBody String jarList) throws Exception {
		ArrayNode result = mapper.createArrayNode();
		String[] lines = jarList.split("\r\n");
		for (String line : lines) {
			ObjectNode item = mapper.createObjectNode();
			String[] lineItems = line.split(",");
			List<String> groupId = Arrays.asList(Arrays.copyOfRange(lineItems, 0, lineItems.length - 3));
			item.put(keyGroupId, String.join(".", groupId));
			item.put(keyArtifactId, lineItems[lineItems.length - 3]);
			item.put(keyVersion, lineItems[lineItems.length - 2]);
			result.add(item);
		}
		return result;
	}

	@RequestMapping(value = "/convertMavenToJson", method = RequestMethod.POST)
	public Object convertMavenToJson(@RequestBody String mavenDependencies) throws Exception {
		return xmlMapper.readTree(mavenDependencies);
	}

	private Object listClassItems(Class<?> theClass) {
		Map<String, Object> result = new HashMap<String, Object>();
		List<Object> cnstList = new ArrayList<Object>();
		for (Constructor<?> cnst : theClass.getDeclaredConstructors()) {
			Map<String, Object> cnstDetail = new HashMap<String, Object>();
			List<String> listParam = new ArrayList<String>();
			for (Class<?> param : cnst.getParameterTypes()) {
				listParam.add(param.getName());
			}
			cnstDetail.put(cnst.getName(), listParam);
			cnstList.add(cnstDetail);
		}
		result.put("constructors", cnstList);

		List<Object> fieldList = new ArrayList<Object>();
		for (Field field : theClass.getDeclaredFields()) {
			Map<String, Object> fieldDetail = new HashMap<String, Object>();
			fieldDetail.put(field.getName(), field.getAnnotatedType().getType().getTypeName());
			fieldList.add(fieldDetail);
		}
		result.put("fields", fieldList);

		List<Object> methodList = new ArrayList<Object>();
		for (Method method : theClass.getDeclaredMethods()) {
			Map<String, Object> methodDetail = new HashMap<String, Object>();
			List<String> listParam = new ArrayList<String>();
			for (Class<?> param : method.getParameterTypes()) {
				listParam.add(param.getName());
			}
			methodDetail.put(method.getName(), listParam);
			methodList.add(methodDetail);
		}
		result.put("methods", methodList);
		return result;
	}
}
