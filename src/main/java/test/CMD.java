package test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class CMD {

  // logging
  private static ByteArrayOutputStream baos = new ByteArrayOutputStream();
  private static PrintStream log = new PrintStream(baos);

  public static String getLog() {
    return new String(baos.toByteArray(), StandardCharsets.UTF_8);
  }

  public static void clearLog() {
    baos.reset();
  }

  // util method
  public static Method $M_getClass;

  public static Object getClass(Object theClass) throws Exception {
    return $M_getClass.invoke(null, theClass);
  }

	public static Method $M_getBean;

  public static Object getBean(Object theClass) throws Exception {
    return $M_getBean.invoke(null, theClass);
  }

  public static Method $M_newObj;

  public static Object newObj(Object theClass, Object... param) throws Exception {
    return $M_newObj.invoke(null, theClass, param);
  }

  public static Method $M_getStaticProp;

  public static Object getStaticProp(Object theClass, String propName) throws Exception {
    return $M_getStaticProp.invoke(null, theClass, propName);
  }

  public static Method $M_setStaticProp;

  public static Object setStaticProp(Object theClass, String propName, Object value)
      throws Exception {
    return $M_setStaticProp.invoke(null, theClass, propName, value);
  }

  public static Method $M_getObjProp;

  public static Object getObjProp(Object obj, String propName) throws Exception {
    return $M_getObjProp.invoke(null, obj, propName);
  }

  public static Method $M_setObjProp;

  public static Object setObjProp(Object obj, String propName, Object value) throws Exception {
    return $M_setObjProp.invoke(null, obj, propName, value);
  }

  public static Method $M_callStaticMethod;

  public static Object callStaticMethod(Object theClass, String methodName, Object... param)
      throws Exception {
    return $M_callStaticMethod.invoke(null, theClass, methodName, param);
  }

  public static Method $M_callObjMethod;

  public static Object callObjMethod(Object obj, String methodName, Object... param)
      throws Exception {
    return $M_callObjMethod.invoke(null, obj, methodName, param);
  }

  // import class
  // public static Object $C_ObjectMapper = "com.fasterxml.jackson.databind.ObjectMapper";
  // public static Object $C_JsonNode = "com.fasterxml.jackson.databind.JsonNode";
  // public static Object $C_ArrayNode = "com.fasterxml.jackson.databind.node.ArrayNode";
  // public static Object $C_Mapper = "com.ratata.nativeQueryRest.utils.Mapper";
  // public static Object $C_NativeQueryParam = "com.ratata.nativeQueryRest.pojo.NativeQueryParam";
  // public static Object $C_CoreDAO = "com.ratata.nativeQueryRest.dao.CoreDAO";

  // public static Object $C_EmbeddedJettyMain =
  // "hello:com.javacodegeeks.snippets.enterprise.embeddedjetty.EmbeddedJettyMain";
  // public static List<Thread> currentThreads = new ArrayList<Thread>();
  // public static Object $Sp_TargetSystemRepository =
  // "com.bosch.portal.app.repository.TargetSystemRepository";
  // main code


  public static String psUser(String user) throws Exception {
    return executeCommand("ps -u " + user + " -o pid,ppid,pcpu,pmem,args,etime,time,vsz,comm");
  }

  public static String psAll() throws Exception {
    return executeCommand("ps -e -o pid,ppid,pcpu,pmem,args");
  }

  public static String executeCommand(ArrayList<String> command) {
    return executeCommand(command.toArray(new String[command.size()]));
  }

  public static String executeCommand(String[] command) {
    StringBuffer output = new StringBuffer();
    Process p;
    try {
      p = Runtime.getRuntime().exec(command);
      BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
      BufferedReader errorReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
      String line = "";
      while ((line = reader.readLine()) != null) {
        output.append(line + "\n");
      }
      output.append(line + "\n------error------\n");
      while ((line = errorReader.readLine()) != null) {
        output.append(line + "\n");
      }
      p.waitFor();
    } catch (Exception e) {
      e.printStackTrace();
    }

    return output.toString();
  }
  
  public static String executeCommand(String command) {
    StringBuffer output = new StringBuffer();
    Process p;
    try {
      p = Runtime.getRuntime().exec(command);
      BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
      BufferedReader errorReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
      String line = "";
      while ((line = reader.readLine()) != null) {
        output.append(line + "\n");
      }
      output.append(line + "\n------error------\n");
      while ((line = errorReader.readLine()) != null) {
        output.append(line + "\n");
      }
      p.waitFor();
    } catch (Exception e) {
      e.printStackTrace();
    }

    return output.toString();
  }

  public static void tree() throws Exception {
    executeCommand(new String[] {"/bin/sh","-c","ls -R -la />testdir/report.txt"});
  }
  
  public static InputStream downloadtree() throws Exception {
    FileInputStream file = new FileInputStream(new File("testdir/report.txt"));
    return file;
  }
}
