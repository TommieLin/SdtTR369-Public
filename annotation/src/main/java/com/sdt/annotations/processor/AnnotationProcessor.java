package com.sdt.annotations.processor;

import com.google.auto.service.AutoService;
import com.sdt.annotations.Tr369Get;
import com.sdt.annotations.Tr369Set;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

@AutoService(Processor.class)
@SupportedAnnotationTypes({"com.sdt.annotations.Tr369Get", "com.sdt.annotations.Tr369Set"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class AnnotationProcessor extends AbstractProcessor {
    private Elements mElementUtils;
    private Messager mMessager;
    private Filer mFiler;

    public static final String PACKAGE_NAME = "com.sdt.annotations.policy";

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);

        mElementUtils = processingEnv.getElementUtils();
        mMessager = processingEnv.getMessager();
        mFiler = processingEnv.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        buildTr369Class(roundEnvironment);
        return true;
    }

    private void buildTr369Class(RoundEnvironment roundEnvironment) {
        buildTr369ClassFacotry(roundEnvironment, TypeTr369Set, "Tr369SetPolicy", "mTr369SetMap");
        buildTr369ClassFacotry(roundEnvironment, TypeTr369Get, "Tr369GetPolicy", "mTr369GetMap");
    }

    private ClassProperty createProperty(String packageName, String className) {
        return new ClassProperty(packageName, className);
    }

    private MethodProperty createProperty(String packageName, String className, String methodName) {
        return new MethodProperty(packageName, className, methodName);
    }

    final int TypeTr369Set = 1;
    final int TypeTr369Get = 2;

    private HashMap<String, MethodProperty> getStringMethodPropertyHashMap(RoundEnvironment roundEnvironment, int type) {
        HashMap<String, MethodProperty> actionsMap = new HashMap<>();
        Class<? extends Annotation> aClass = null;
        if (type == TypeTr369Set) {
            aClass = Tr369Set.class;
        }
        if (type == TypeTr369Get) {
            aClass = Tr369Get.class;
        }

        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(aClass);
        if (elements == null || elements.size() < 1) {
            return actionsMap;
        }

        for (Element typeElement : roundEnvironment.getElementsAnnotatedWith(aClass)) {
            String keys = null;
            if (type == TypeTr369Set) {
                Tr369Set ann = (Tr369Set) typeElement.getAnnotation(aClass);
                keys = ann.value();
                mMessager.printMessage(Diagnostic.Kind.NOTE, "keys: " + keys);
            }
            if (type == TypeTr369Get) {
                Tr369Get ann = (Tr369Get) typeElement.getAnnotation(aClass);
                keys = ann.value();
            }
            if (keys == null || keys.length() < 1) {
                return actionsMap;
            }
            String packageName = mElementUtils.getPackageOf(typeElement).getQualifiedName().toString();
            String clazzName = typeElement.getEnclosingElement().getSimpleName().toString();
            String methodName = typeElement.getSimpleName().toString();
            mMessager.printMessage(Diagnostic.Kind.NOTE, "packageName: " + packageName + ", clazzName: " + clazzName + ", methodName: " + methodName);
            String[] names = keys.split(",");
            if (names.length > 0) {
                for (String name : names) {
                    if (name != null && !name.isEmpty()) {
                        actionsMap.put(name, createProperty(packageName, clazzName, methodName));
                    }
                }
            }
        }
        return actionsMap;
    }


    private void buildTr369ClassFacotry(RoundEnvironment roundEnvironment, int type, String className, String varName) {
        HashMap<String, MethodProperty> actionsMap = getStringMethodPropertyHashMap(roundEnvironment, type);
        if (actionsMap.isEmpty()) {
            mMessager.printMessage(Diagnostic.Kind.NOTE, "FactoryMap isEmpty");
            return;
        }
        Tr369MethodFactoryCreater.create(varName, PACKAGE_NAME, className, actionsMap, mFiler);
    }

}
