package com.sdt.annotations.processor;

public class MethodProperty {
    public String mPackageName;
    public String mClassName;
    public String mMethodName;

    public MethodProperty(String packageName, String className, String methodName) {
        mPackageName = packageName;
        mClassName = className;
        mMethodName = methodName;
    }

}
