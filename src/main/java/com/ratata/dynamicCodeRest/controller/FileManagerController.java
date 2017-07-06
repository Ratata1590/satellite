package com.ratata.dynamicCodeRest.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ratata.dynamicCodeRest.utils.ErrorUtil;
import com.ratata.dynamicCodeRest.utils.MultipartFileSender;

@RestController
public class FileManagerController {

	private static ObjectMapper mapper = new ObjectMapper();

	@RequestMapping(value = "/fileManager/**", method = RequestMethod.POST)
	public static Object postResource(HttpServletRequest request, @RequestBody byte[] body) {
		try {
			String key = getResourcePathFromRequest(request);
			FileOutputStream fos = new FileOutputStream(key);
			fos.write(body);
			fos.close();
		} catch (Exception e) {
			return new ResponseEntity<>(ErrorUtil.printStackTrace(e), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequestMapping(value = "/fileManagerUploadFile", method = RequestMethod.POST)
	public static Object uploadResource(@RequestParam("file") MultipartFile[] file,
			@RequestParam("path") String[] path) {
		try {
			if (file.length != path.length) {
				throw new Exception("invalid request");
			}
			for (int i = 0; i < path.length; i++) {
				file[i].transferTo(new File(path[i] + file[i].getOriginalFilename()));
			}
		} catch (Exception e) {
			return new ResponseEntity<>(ErrorUtil.printStackTrace(e), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequestMapping(value = "/fileManager/**", method = RequestMethod.GET)
	public static Object getResource(HttpServletRequest request, HttpServletResponse response) {
		try {
			String key = getResourcePathFromRequest(request);
			File file = new File(key);
			if (!file.exists()) {
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}
			if (file.isFile()) {
				MultipartFileSender.fromFile(file).with(request).with(response).serveResource();
			}
			return file.list();
		} catch (Exception e) {
			return new ResponseEntity<>(ErrorUtil.printStackTrace(e), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(value = "/fileManager/**", method = RequestMethod.DELETE)
	public static Object deleteResource(HttpServletRequest request) {
		try {
			String key = getResourcePathFromRequest(request);
			File file = new File(key);
			if (!file.exists()) {
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}
			delete(file);
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(ErrorUtil.printStackTrace(e), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(value = "/fileManagerDeleteFile", method = RequestMethod.DELETE)
	public static Object deleteResources(@RequestBody(required = true) List<String> fileList) {
		Map<String, String> result = new HashMap<String, String>();
		for (String file : fileList) {
			try {
				delete(new File(file));
				result.put(file, "DELETED");
			} catch (IOException e) {
				result.put(file, ErrorUtil.printStackTrace(e));
			}
		}
		return result;
	}

	@RequestMapping(value = "/fileManagerFileTree", method = RequestMethod.GET)
	public static Object dynamicFronEndFileTree(
			@RequestHeader(required = false, defaultValue = "false") Boolean jsonOutput,
			@RequestHeader(required = false, defaultValue = "false") Boolean showGoto,
			@RequestHeader(required = false, defaultValue = "/") String basePath) {
		if (jsonOutput) {
			return listFileToJson(resourcesList, basePath, showGoto);
		}
		return resourcesList;
	}

	private static JsonNode listFileToJson(TreeMap<String, String> resourcesList, String basePath, Boolean showGoto) {
		Iterator<String> keyset = resourcesList.keySet().iterator();
		ObjectNode result = mapper.createObjectNode();
		while (keyset.hasNext()) {
			String key = keyset.next();
			String[] path = key.split("/");
			ObjectNode node = result;
			for (int i = 0; i < path.length; i++) {
				if (i == path.length - 1) {
					ObjectNode dataNode = mapper.createObjectNode();
					dataNode.put("<size>", resourcesList.get(key));
					if (showGoto) {
						dataNode.put("<goto>", basePath + key);
					}
					node.set(path[i], dataNode);
					continue;
				}
				if (!node.has(path[i])) {
					ObjectNode newNode = mapper.createObjectNode();
					node.set(path[i], newNode);
					node = newNode;
					continue;
				} else {
					node = (ObjectNode) node.get(path[i]);
				}
			}
		}
		return result;
	}

	@RequestMapping(value = "/fileManagerGetInfo", method = RequestMethod.GET)
	public static Object dynamicFronEndFileTree(@RequestHeader String resource) {
		if (!resources.containsKey(resource)) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		return resourcesList.get(resource);
	}

	@RequestMapping(value = "fileManagerUploadZip", method = RequestMethod.POST)
	public static void uploadZipFrontEnd(@RequestParam("file") MultipartFile[] file) throws Exception {
		for (MultipartFile zipFile : file) {
			processUnzipSingleFile(zipFile);
		}
	}

	private static void processUnzipSingleFile(MultipartFile zipFile) throws Exception {
		ZipInputStream zipInputStream = new ZipInputStream(zipFile.getInputStream());
		ZipEntry zipEntry = zipInputStream.getNextEntry();
		while (zipEntry != null) {
			String fileName = zipEntry.getName();
			byte[] file = StreamUtils.copyToByteArray(zipInputStream);
			resources.put(fileName, file);
			resourcesList.put(fileName, String.valueOf(file.length));
			zipEntry = zipInputStream.getNextEntry();
		}
	}

	@RequestMapping(value = "fileManagerDownloadZip/**", method = RequestMethod.GET)
	public static void downloadZipFrontEnd(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String key = getResourcePathFromRequest(request);
		List<String> fileList = new ArrayList<String>();
		for (String keyItem : resources.keySet()) {
			if (keyItem.startsWith(key)) {
				fileList.add(keyItem);
			}
		}
		zipFiles(fileList, response);
	}

	private static void zipFiles(List<String> fileList, HttpServletResponse response) throws Exception {
		response.setHeader("Content-Disposition", "attachment; filename=\"" + "download.zip" + "\"");
		response.setHeader("Content-Type", "application/zip");
		ZipOutputStream zos = new ZipOutputStream(response.getOutputStream());
		for (String file : fileList) {
			ZipEntry ze = new ZipEntry(file);
			zos.putNextEntry(ze);
			zos.write(resources.get(file));
		}
		zos.closeEntry();
		zos.close();
	}

	private static String getResourcePathFromRequest(HttpServletRequest request) {
		String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
		String expression = bestMatchPattern.split("/")[2];
		String finalPath = path.substring(bestMatchPattern.length() - expression.length());
		return finalPath;
	}

	public static List<String> scanFolder(File file) throws IOException {
		if (file.isDirectory()) {
			if (file.list().length == 0) {
				file.delete();
			} else {
				String files[] = file.list();
				for (String temp : files) {
					File fileDelete = new File(file, temp);
					delete(fileDelete);
				}
				if (file.list().length == 0) {
					file.delete();
				}
			}
		} else {
			file.delete();
		}
	}
}
