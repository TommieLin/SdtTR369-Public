package com.sdt.annotations.processor;

public class ClassProperty {
    String mPackageName;
    String mClassName;

    public ClassProperty(String packageName, String className) {
        mPackageName = packageName;
        mClassName = className;
    }
}
