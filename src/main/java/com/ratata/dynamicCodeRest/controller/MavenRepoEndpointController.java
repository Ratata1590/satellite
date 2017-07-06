package com.ratata.dynamicCodeRest.controller;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;

@RestController
public class MavenRepoEndpointController {

	public static Class<?> mavenUtilClass;
	public static URLClassLoader classLoader;

	@RequestMapping(value = "/uploadJarToRepo", method = RequestMethod.POST)
	public static void uploadJarToRepo(@RequestBody(required = true) MultipartFile file,
			@RequestHeader(required = true) String groupId, @RequestHeader(required = true) String artifactId,
			@RequestHeader(required = true) String version) throws Exception {
		File tempFile = File.createTempFile(file.getOriginalFilename(), "");
		file.transferTo(tempFile);
		mavenUtilClass.getMethod("uploadJarToRepo", File.class, String.class, String.class, String.class).invoke(null,
				tempFile, groupId, artifactId, version);
	}

	@RequestMapping(value = "/getLocalMetadata", method = RequestMethod.GET)
	public static Object getMetadata(@RequestHeader(required = true) String groupId,
			@RequestHeader(required = true) String artifactId) throws Exception {
		return mavenUtilClass.getMethod("getMetadata", String.class, String.class).invoke(null, groupId, artifactId);
	}

	@RequestMapping(value = "/createClassLoader", method = RequestMethod.POST)
	public static void createClassLoader(@RequestHeader(required = true) String classLoaderName,
			@RequestBody JsonNode mavenDependencies) throws Exception {

		mavenUtilClass.getMethod("createClassLoader", String.class, JsonNode.class, Map.class, Map.class).invoke(null,
				classLoaderName, mavenDependencies, DynamicCodeRestEndpointController.configList,
				DynamicCodeRestEndpointController.classLoaderList);
	}

	@RequestMapping(value = "/removeClassLoader", method = RequestMethod.GET)
	public static void removeClassLoader(@RequestHeader String classLoaderName) throws Exception {
		mavenUtilClass.getMethod("removeClassLoader", String.class, Map.class, Map.class).invoke(null, classLoaderName,
				DynamicCodeRestEndpointController.configList, DynamicCodeRestEndpointController.classLoaderList);
		System.gc();
	}

	@RequestMapping(value = "/getClassLoaderList", method = RequestMethod.GET)
	public static Object getClassLoaderList() throws Exception {
		return DynamicCodeRestEndpointController.configList;
	}

	@RequestMapping(value = "/initMavenUtil", method = RequestMethod.POST)
	private void initRepositorySession(@RequestHeader String path) throws Exception {
		if (classLoader != null) {
			classLoader.close();
		}
		classLoader = new URLClassLoader(new URL[] { (new File(path)).toURI().toURL() },
				Thread.currentThread().getContextClassLoader());
		mavenUtilClass = classLoader.loadClass("ratata.maven.MavenRepoUtil");
		mavenUtilClass.getMethod("initRepositorySession").invoke(null);
	}
}
