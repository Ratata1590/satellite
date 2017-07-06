package com.ratata.dynamicCodeRest.controller;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ratata.dynamicCodeRest.dynamicObject.ObjectContainer;
import com.ratata.dynamicCodeRest.utils.MultipartFileSender;
import com.ratata.dynamicCodeRest.utils.ThreadUtils;

@RestController
public class GenToolDynamicCode {
	public static final String importList = "importList";
	public static final String autowireList = "autowireList";
	public static final String importString = "import ";
	public static final ObjectMapper mapper = new ObjectMapper();

	@RequestMapping(value = "/checkInputType", method = RequestMethod.POST)
	public Object checkInputType(@RequestBody Object data) {
		return new ObjectContainer(data);
	}

	@RequestMapping(value = "/genDynamicCodeTemplate", method = RequestMethod.POST)
	public String genDynamicCodeTemplate(@RequestBody JsonNode data) {
		StringBuilder result = new StringBuilder();
		if (data.get(importList) != null) {
			String impList = data.get(importList).textValue();
			for (String imp : impList.split(";")) {
				String classNameFull = imp.substring(importString.length(), imp.length());
				String classNameShort = new String(classNameFull).substring(classNameFull.lastIndexOf(".") + 1);
				result.append(
						String.format("public static Object $C_%1$s = \"%2$s\";\n", classNameShort, classNameFull));
			}
			result.append("\n");
		}
		if (data.get(autowireList) != null) {
			String auList = data.get(autowireList).textValue();
			for (String imp : auList.split(";")) {
				String classNameFull = imp.substring(importString.length(), imp.length());
				String classNameShort = new String(classNameFull).substring(classNameFull.lastIndexOf(".") + 1);
				result.append(
						String.format("public static Object $Sp_%1$s = \"%2$s\";\n", classNameShort, classNameFull));
			}
		}
		return result.toString();
	}

	@RequestMapping(value = "/getAllThead", method = RequestMethod.GET)
	public Object getAllThead() throws Exception {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("getAllDaemonThreads", ThreadUtils.getAllThreadInfos());
		return result;
	}

	@RequestMapping(value = "/freeMem", method = RequestMethod.GET)
	public Object freeMem() throws Exception {
		ObjectNode node = mapper.createObjectNode();
		node.put("total", String.valueOf(Runtime.getRuntime().totalMemory()) + "/"
				+ String.valueOf(Runtime.getRuntime().maxMemory()));
		node.put("freed", String.valueOf(Runtime.getRuntime().freeMemory()));
		node.put("processor", Runtime.getRuntime().availableProcessors());
		return node;
	}

	@RequestMapping(value = "/downloadFile", method = RequestMethod.GET)
	public void downloadFile(@RequestParam String path, HttpServletResponse response, HttpServletRequest request)
			throws Exception {
		MultipartFileSender.fromFile(new File(path)).with(request).with(response).serveResource();
		// Streams.copy(new FileInputStream(new File(path)),
		// response.getOutputStream(), true);
	}

	@RequestMapping(value = "/uploadFile", method = RequestMethod.POST)
	public void uploadFile(@RequestParam("file") MultipartFile[] file, @RequestParam("path") String[] path)
			throws Exception {
		if (file.length != path.length) {
			throw new Exception("invalid file path pair!");
		}
		for (int i = 0; i < file.length; i++) {
			file[i].transferTo(new File(path[i] + file[i].getOriginalFilename()));
		}
	}
}
