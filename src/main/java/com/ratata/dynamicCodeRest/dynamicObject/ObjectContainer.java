package com.ratata.dynamicCodeRest.dynamicObject;

public class ObjectContainer {
	private Object obj;

	private String objType;

	private int objHashCode;

	public ObjectContainer() {
	}

	public ObjectContainer(Object obj) {
		super();
		setObj(obj);
	}

	public String getObjType() {
		objType = obj.getClass().getTypeName();
		return objType;
	}

	public int getObjHashCode() {
		this.objHashCode = obj.hashCode();
		return objHashCode;
	}

	public void setObj(Object obj) {
		this.objType = obj.getClass().getTypeName();
		this.objHashCode = obj.hashCode();
		this.obj = obj;
	}

}
