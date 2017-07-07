package com.ratata.dynamicCodeRest.controller;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ratata.dynamicCodeRest.utils.ErrorUtil;
import com.ratata.dynamicCodeRest.utils.MultipartFileSender;

@RestController
public class FileManagerController {

  private static ObjectMapper mapper = new ObjectMapper();

  @RequestMapping(value = "/fileManager/**", method = RequestMethod.POST)
  public static Object postResource(@RequestHeader(required = false) String path,
      HttpServletRequest request, @RequestBody byte[] body) {
    try {
      String key = getResourcePathFromRequest(request);
      if (path != null && !path.isEmpty()) {
        key = path;
      }
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
    } catch (Exception e) {
      return new ResponseEntity<>(ErrorUtil.printStackTrace(e), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    Map<String, String> status = new HashMap<String, String>();
    for (int i = 0; i < path.length; i++) {
      try {
        file[i].transferTo(new File(path[i], file[i].getOriginalFilename()));
        status.put(path[i] + file[i].getOriginalFilename(), "OK");
      } catch (Exception e) {
        status.put(path[i] + file[i].getOriginalFilename(), ErrorUtil.printStackTrace(e));
      }
    }
    return status;
  }

  @RequestMapping(value = "/fileManager/**", method = RequestMethod.GET)
  public static Object getResource(@RequestHeader(required = false) String path,
      HttpServletRequest request, HttpServletResponse response) {
    try {
      String key = getResourcePathFromRequest(request);
      if (path != null && !path.isEmpty()) {
        key = path;
      }
      File file = new File(key);
      if (!file.exists()) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
      }
      if (file.isFile()) {
        MultipartFileSender.fromFile(file).with(request).with(response).serveResource();
      }
      if (file.listFiles() == null) {
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
      }
      return resolveDirectoryView(file);
    } catch (Exception e) {
      return new ResponseEntity<>(ErrorUtil.printStackTrace(e), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private static Object resolveDirectoryView(File dir) {
    Map<String, TreeMap<String, JsonNode>> result =
        new HashMap<String, TreeMap<String, JsonNode>>();
    TreeMap<String, JsonNode> folderList = new TreeMap<String, JsonNode>();
    TreeMap<String, JsonNode> fileList = new TreeMap<String, JsonNode>();
    for (File file : dir.listFiles()) {
      ObjectNode node = mapper.createObjectNode();
      node.put("<goto>", file.getAbsolutePath());
      node.put("<size>", file.length());
      if (file.isDirectory()) {
        folderList.put(file.getName(), node);
        continue;
      }
      if (file.isFile()) {
        fileList.put(file.getName(), node);
        continue;
      }
    }
    result.put("folder", folderList);
    result.put("file", fileList);
    return result;
  }

  @RequestMapping(value = "/fileManager/**", method = RequestMethod.DELETE)
  public static Object deleteResource(@RequestHeader(required = false) String path,
      HttpServletRequest request) {
    try {
      String key = getResourcePathFromRequest(request);
      if (path != null && !path.isEmpty()) {
        key = path;
      }
      File file = new File(key);
      if (!file.exists()) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
      }
      performDeleteFile(file);
      return new ResponseEntity<>(HttpStatus.OK);
    } catch (Exception e) {
      return new ResponseEntity<>(ErrorUtil.printStackTrace(e), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private static void performDeleteFile(File file) {
    List<File> fileList = new ArrayList<File>();
    LinkedList<File> folderList = new LinkedList<File>();
    scanFolder(file, fileList, folderList);
    for (File fileInFileList : fileList) {
      fileInFileList.delete();
    }
    while (!folderList.isEmpty()) {
      folderList.pop().delete();
    }
  }

  @RequestMapping(value = "/fileManagerDeleteFile", method = RequestMethod.DELETE)
  public static Object deleteResources(@RequestBody(required = true) List<String> fileList) {
    Map<String, Object> result = new HashMap<String, Object>();
    for (String file : fileList) {
      try {
        performDeleteFile(new File(file));
        result.put(file, "DONE");
      } catch (Exception e) {
        result.put(file, ErrorUtil.printStackTrace(e));
      }
    }
    return result;
  }


  @RequestMapping(value = "/fileManagerFileTree/**", method = RequestMethod.GET)
  public static Object dynamicFronEndFileTree(
      @RequestHeader(required = false, defaultValue = "false") Boolean showGoto,
      @RequestHeader(required = false, defaultValue = "false") Boolean showSize,
      @RequestHeader(required = false, defaultValue = "false") Boolean showType,
      @RequestHeader(required = false, defaultValue = "2") Double limit,
      @RequestHeader(required = false) String path, HttpServletRequest request) {
    try {
      String key = getResourcePathFromRequest(request);
      if (path != null && !path.isEmpty()) {
        key = path;
      }
      File file = new File(key);
      if (!file.exists()) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
      }
      ObjectNode result = mapper.createObjectNode();
      listFileToJson(file, result, showGoto, showSize, showType, limit);
      return result;
    } catch (Exception e) {
      return new ResponseEntity<>(ErrorUtil.printStackTrace(e), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }


  private static void listFileToJson(File file, ObjectNode result, Boolean showGoto,
      Boolean showSize, Boolean showType, Double limit) {
    if (limit == 0) {
      return;
    }
    if (file.isDirectory()) {
      limit--;
      if (file.list() == null) {
        result.put(file.getName(), "FORBIDDEN");
        return;
      }
      ArrayNode folderNode = mapper.createArrayNode();
      for (File temp : file.listFiles()) {
        ObjectNode dataNode = mapper.createObjectNode();
        listFileToJson(temp, dataNode, showGoto, showSize, showType, limit);
        folderNode.add(dataNode);
      }
      if (showSize) {
        result.put("<size>", FileUtils.sizeOf(file));
      }
      if (showGoto) {
        result.put("<goto>", file.getAbsolutePath());
      }
      if (showType) {
        result.put("<type>", "DIR");
      }
      result.set(file.getName(), folderNode);
    } else {
      ObjectNode dataNode = mapper.createObjectNode();
      if (showSize) {
        dataNode.put("<size>", FileUtils.sizeOf(file));
      }
      if (showGoto) {
        dataNode.put("<goto>", file.getAbsolutePath());
      }
      if (showType) {
        result.put("<type>", "FILE");
      }
      result.set(file.getName(), dataNode);
    }
  }


  @RequestMapping(value = "/fileManagerGetInfo/**", method = RequestMethod.GET)
  public static Object dynamicFronEndFileTree(@RequestHeader(required = false) String path,
      HttpServletRequest request) {
    String key = getResourcePathFromRequest(request);
    if (path != null && !path.isEmpty()) {
      key = path;
    }
    File file = new File(key);
    if (!file.exists()) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    ObjectNode dataNode = mapper.createObjectNode();
    dataNode.put("<size>", FileUtils.sizeOf(file));
    dataNode.put("<isDir>", file.isDirectory());
    dataNode.put("<hidden>", file.isHidden());
    dataNode.put("<canWrite>", file.canWrite());
    dataNode.put("<canWrite>", file.canRead());
    dataNode.put("<canExecute>", file.canExecute());
    dataNode.put("<lastModified>", (new Date(file.lastModified())).toString());
    dataNode.put("<getAbsolutePath>", file.getAbsolutePath());
    return dataNode;
  }


  @RequestMapping(value = "fileManagerUploadZip", method = RequestMethod.POST)
  public static Object uploadZipFrontEnd(@RequestParam("file") MultipartFile[] file,
      @RequestParam("path") String[] path) throws Exception {
    try {
      if (file.length != path.length) {
        throw new Exception("invalid request");
      }
    } catch (Exception e) {
      return new ResponseEntity<>(ErrorUtil.printStackTrace(e), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    for (int i = 0; i < file.length; i++) {
      File tempFile = File.createTempFile(file[i].getOriginalFilename(), "");
      file[i].transferTo(tempFile);
      processUnzipSingleFile(tempFile, path[i]);
    }
    return new ResponseEntity<>(HttpStatus.OK);
  }

  private static void processUnzipSingleFile(File zipFilePath, String destDir) {
    try {
      ZipFile file = new ZipFile(zipFilePath);
      FileSystem fileSystem = FileSystems.getDefault();
      Enumeration<? extends ZipEntry> entries = file.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        if (entry.isDirectory()) {
          Files.createDirectories(fileSystem.getPath(destDir + File.separator + entry.getName()));
        } else {
          InputStream is = file.getInputStream(entry);
          BufferedInputStream bis = new BufferedInputStream(is);
          String uncompressedFileName = destDir + File.separator + entry.getName();
          Path uncompressedFilePath = fileSystem.getPath(uncompressedFileName);
          Files.createFile(uncompressedFilePath);
          FileOutputStream fileOutput = new FileOutputStream(uncompressedFileName);
          while (bis.available() > 0) {
            fileOutput.write(bis.read());
          }
          fileOutput.close();
        }
      }
      file.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @RequestMapping(value = "fileManagerDownloadZip/**", method = RequestMethod.GET)
  public static Object downloadZipFrontEnd(HttpServletRequest request, HttpServletResponse response,
      @RequestHeader(required = false) String path) throws Exception {
    String key = getResourcePathFromRequest(request);
    if (path != null && !path.isEmpty()) {
      key = path;
    }
    File file = new File(key);
    if (!file.exists()) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    List<File> fileList = new ArrayList<File>();
    LinkedList<File> folderList = new LinkedList<File>();
    scanFolder(file, fileList, folderList);
    zipFiles(fileList, response, path);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  private static void zipFiles(List<File> fileList, HttpServletResponse response, String path)
      throws Exception {
    response.setHeader("Content-Disposition", "attachment; filename=\"" + "download.zip" + "\"");
    response.setHeader("Content-Type", "application/zip");
    ZipOutputStream zos = new ZipOutputStream(response.getOutputStream());
    for (File file : fileList) {
      ZipEntry ze = new ZipEntry(file.getAbsolutePath().substring(path.length() + 1));
      zos.putNextEntry(ze);
      zos.write(IOUtils.toByteArray(new FileInputStream(file)));
    }
    zos.closeEntry();
    zos.close();
  }

  private static String getResourcePathFromRequest(HttpServletRequest request) {
    String path =
        (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
    String bestMatchPattern =
        (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
    String expression = bestMatchPattern.split("/")[2];
    String finalPath = path.substring(bestMatchPattern.length() - expression.length());
    return finalPath;
  }

  public static void scanFolder(File file, List<File> fileList, LinkedList<File> folderList) {
    if (file.isDirectory()) {
      if (file.list() == null) {
        return;
      }
      folderList.addFirst(file);
      for (String temp : file.list()) {
        File innerFile = new File(file, temp);
        scanFolder(innerFile, fileList, folderList);
      }
    } else {
      fileList.add(file);
    }
  }
}
