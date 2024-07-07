package com.sdt.annotations.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.HashMap;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;

public class Tr369MethodFactoryCreater {
    public static void create(String mapName, String packageName, String simpleName, HashMap<String, MethodProperty> actionsMap, Filer mFiler) {
        ClassName cArrayMap = ClassName.get("android.util", "ArrayMap");
        ClassName cMethodProperty = ClassName.get("com.sdt.annotations.processor", "MethodProperty");

        TypeName mapFieldType = ParameterizedTypeName.get(cArrayMap, TypeName.get(String.class), cMethodProperty);
        FieldSpec mapField = FieldSpec.builder(mapFieldType, mapName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .build();

        MethodSpec.Builder constructorBuilder =
                MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addStatement("this.$N = new $T<$T,$T>()", mapName, cArrayMap, String.class, cMethodProperty);

        for (String key : actionsMap.keySet()) {
            MethodProperty property = actionsMap.get(key);
            constructorBuilder.addStatement("$N.put($S,new $T($S,$S,$S))", mapName, key, cMethodProperty, property.mPackageName, property.mClassName, property.mMethodName);
        }
        MethodSpec constructor = constructorBuilder.build();

        TypeSpec mainClass = TypeSpec.classBuilder(simpleName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc("Tis file is automatically generated with comments,not be modified\n")
                .addField(mapField)
                .addMethod(constructor)
                .build();
        JavaFile javaFile = JavaFile.builder(packageName, mainClass).build();

        try {
            javaFile.writeTo(mFiler);
        } catch (IOException e) {

        }
    }
}
