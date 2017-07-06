package com.ratata.dynamicCodeRest.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ratata.dynamicCodeRest.controller.DynamicCodeRestEndpointController;

public class DynamicCodeUtil {
	public static final String SPECIAL_CHAR = "$";
	public static final String SPECIAL_METHOD_CHAR = "$M_";
	public static final String SPECIAL_IMPORT_CHAR = "$C_";
	public static final String SPECIAL_IMPORT_CHAR_LAZY = "$CL_";
	public static final String SPECIAL_AUTOWIRE_CHAR = "$Sp_";
	public static final String CLASSLOADER_SEPARATOR = ":";

	public static final List<String> UTILNAME_LIST = Arrays.asList(
			new String[] { "clearDynObj", "getDynObj", "newDynObj", "getClass", "getBean", "newObj", "getStaticProp",
					"setStaticProp", "callStaticMethod", "getObjProp", "setObjProp", "callObjMethod" });

	public static void loadAllClass(Class<?> theClass, ClassLoader classLoader) throws Exception {
		Field[] fields = theClass.getDeclaredFields();
		Method[] thisMethods = DynamicCodeUtil.class.getMethods();
		for (Field field : fields) {
			if (!field.getName().startsWith(SPECIAL_CHAR)) {
				continue;
			}
			if (field.getName().startsWith(SPECIAL_METHOD_CHAR)) {
				String theName = field.getName().substring(SPECIAL_METHOD_CHAR.length());
				if (UTILNAME_LIST.contains(theName)) {
					for (Method mth : thisMethods) {
						if (mth.getName().equals(theName)) {
							field.set(null, mth);
							break;
						}
					}
				}
			}
			if (field.getName().startsWith(SPECIAL_IMPORT_CHAR)) {
				field.set(null, getClass(field.get(null)));
			}
			if (field.getName().startsWith(SPECIAL_AUTOWIRE_CHAR)) {
				field.set(null, ShareResourceFromSpring.shareAppContext
						.getBean(classLoader.loadClass((String) field.get(null))));
			}
		}
	}

	public static Object getClass(Object lazyClass) throws Exception {
		String className = (String) lazyClass;
		if (!className.contains(CLASSLOADER_SEPARATOR)) {
			return Thread.currentThread().getContextClassLoader().loadClass(className);
		} else {
			String[] pckName = className.split(CLASSLOADER_SEPARATOR);
			String classLoaderName = pckName[0];
			String clsName = pckName[1];
			if (!DynamicCodeRestEndpointController.classLoaderList.containsKey(classLoaderName)) {
				throw new Exception("classLoaderName " + classLoaderName + " not found");
			}
			return DynamicCodeRestEndpointController.classLoaderList.get(classLoaderName).loadClass(clsName);
		}
	}

	public static String newDynObj(String dynClassName, Object... param) throws Exception {
		Map<String, List<Object>> paramToPass = new HashMap<String, List<Object>>();
		List<Object> paramType = new ArrayList<Object>();
		for (Object obj : param) {
			paramType.add(obj.getClass());
		}
		paramToPass.put("paramType", paramType);
		paramToPass.put("paramData", Arrays.asList(param));
		return DynamicCodeRestEndpointController.newObj(dynClassName, paramToPass);
	}

	public static Object getDynObj(String instanceId) throws Exception {
		return DynamicCodeRestEndpointController.objList.get(instanceId);
	}

	public static Object clearDynObj(String instanceId) throws Exception {
		return DynamicCodeRestEndpointController.objList.remove(instanceId);
	}

	public static Object getBean(Object theClass) {
		return ShareResourceFromSpring.shareAppContext.getBean((Class<?>) theClass);
	}

	public static Object newObj(Object theClass, Class<?>[] paramType, Object[] paramData) throws Exception {
		if (paramType == null || paramType.length == 0) {
			return ((Class<?>) theClass).getConstructor().newInstance();
		}
		return ((Class<?>) theClass).getConstructor(paramType).newInstance(paramData);
	}

	public static Object getStaticProp(Object theClass, String propName) throws Exception {
		return ((Class<?>) theClass).getDeclaredField(propName).get(null);
	}

	public static void setStaticProp(Object theClass, String propName, Object value) throws Exception {
		((Class<?>) theClass).getDeclaredField(propName).set(null, value);
	}

	public static Object getObjProp(Object obj, String propName) throws Exception {
		return obj.getClass().getDeclaredField(propName).get(obj);
	}

	public static void setObjProp(Object obj, String propName, Object value) throws Exception {
		obj.getClass().getDeclaredField(propName).set(obj, value);
	}

	public static Object callStaticMethod(Object theClass, String methodName, Class<?>[] paramType, Object[] paramData)
			throws Exception {
		if (paramType == null || paramType.length == 0) {
			return ((Class<?>) theClass).getMethod(methodName).invoke(null);
		}
		return ((Class<?>) theClass).getMethod(methodName, paramType).invoke(null, paramData);
	}

	public static Object callObjMethod(Object obj, String methodName, Class<?>[] paramType, Object[] paramData)
			throws Exception {
		if (paramType == null || paramType.length == 0) {
			return obj.getClass().getMethod(methodName).invoke(obj);
		}
		return obj.getClass().getMethod(methodName, paramType).invoke(obj, paramData);
	}

}
