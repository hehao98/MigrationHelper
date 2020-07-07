package edu.pku.migrationhelper.service;

import edu.pku.migrationhelper.data.ClassSignature;
import edu.pku.migrationhelper.data.MethodSignatureOld;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.Type;
import org.apache.bcel.util.ClassPath;
import org.apache.bcel.util.MemorySensitiveClassPathRepository;
import org.apache.bcel.util.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.Deprecated;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static edu.pku.migrationhelper.data.ClassSignature.getFlagsForClassSignature;

@Service
public class JarAnalysisService {

    Logger LOG = LoggerFactory.getLogger(getClass());

    /**
     * Parse jar file, return list of class signatures
     * @param filePath Jar file path
     * @param publicOnly whether to only retain public classes, public/protected fields and public/protected methods
     * @return list of class signatures
     * @throws IOException when the file does not exist
     */
    public List<ClassSignature> analyzeJar(String filePath, boolean publicOnly) throws IOException {
        List<JavaClass> classList = new LinkedList<>();

        JarFile jarFile = new JarFile(filePath);
        Enumeration<JarEntry> e = jarFile.entries();
        while (e.hasMoreElements()) {
            JarEntry entry = e.nextElement();
            String entryName = entry.getName();
            if (entryName.endsWith(".class") && !entryName.contains("module-info")) {
                ClassParser classParser = new ClassParser(jarFile.getInputStream(entry), entryName);
                try {
                    classList.add(classParser.parse());
                } catch (ClassFormatException ex) {
                    LOG.error("Error when parsing {}/{}, {}", filePath, entryName, ex);
                }
            }
        }

        List<ClassSignature> result = new ArrayList<>();
        for (JavaClass c : classList) {
            if (publicOnly && !c.isPublic())
                continue;
            ClassSignature cs = new ClassSignature(c);
            result.add(cs);
        }
        return result;
    }



    @Deprecated
    public List<MethodSignatureOld> analyzeJar(String filePath, List<MethodSignatureOld> result) throws Exception {
        Repository repository = null;
        JarFile jarFile = null;
        try {
            // store all classes into repository, so that we can analyze their superclass's methods
            repository = new MemorySensitiveClassPathRepository(new ClassPath(filePath));
            List<JavaClass> classList = new LinkedList<>();
            jarFile = new JarFile(filePath);
            Enumeration<JarEntry> e = jarFile.entries();
            while(e.hasMoreElements()) {
                JarEntry entry = e.nextElement();
                String entryName = entry.getName();
                if(entryName.endsWith(".class")) {
                    //LOG.info(entryName);
                    if (entryName.contains("module-info")) continue;
                    ClassParser classParser = new ClassParser(jarFile.getInputStream(entry), entryName);
                    JavaClass clz = classParser.parse();
                    repository.storeClass(clz);
                    classList.add(clz);
                }
            }
            for (JavaClass clz : classList) {
                analyzeClass(clz, repository, result);
            }
        } catch (Exception e) {
            LOG.error("analyzeJar fail filePath = {}, {}", filePath, e);
            throw e;
        } finally {
            if(repository != null) {
                repository.clear();
            }
            if(jarFile != null) {
                jarFile.close();
            }
        }
        return result;
    }

    @Deprecated
    public void analyzeClass(JavaClass clz, Repository repository, List<MethodSignatureOld> result) {
        String packageName = clz.getPackageName();
        String className = clz.getClassName();
        if(packageName == null) packageName = "";
        className = className.substring(className.lastIndexOf('.') + 1);
        MethodSignatureOld ms = new MethodSignatureOld()
                .setPackageName(packageName)
                .setClassName(className)
                .setMethodName("")
                .setParamList("");
        result.add(ms);
        while (clz != null) {
            analyzeClassMethods(clz, packageName, className, result);
            for (String interfaceName : clz.getInterfaceNames()) {
                try {
                    JavaClass interfaceClass = repository.loadClass(interfaceName);
                    if(interfaceClass == null) continue;
                    analyzeClassMethods(interfaceClass, packageName, className, result);
                } catch (ClassNotFoundException e) {
                    // ignore when we can't find the class in this repository
                }
            }
            try {
                clz = clz.getSuperClass();
            } catch (ClassNotFoundException e) {
                // break when we can't find superclass
                clz = null;
            }
        }
    }

    @Deprecated
    public void analyzeClassMethods(JavaClass clz, String packageName, String className, List<MethodSignatureOld> result) {
        StringBuilder paramList = new StringBuilder();
        for (Method method : clz.getMethods()) {
            String methodName = method.getName();
            if(methodName == null) continue;
            paramList.delete(0, paramList.length());
            for (Type parameterType : method.getArgumentTypes()) {
                paramList.append(Utility.typeSignatureToString(parameterType.getSignature(), false));
                paramList.append(",");
            }
            String paramListString = "";
            if(paramList.length() > 0) {
                paramList.deleteCharAt(paramList.length() - 1);
                paramListString = paramList.toString();
            }
            MethodSignatureOld ms = new MethodSignatureOld()
                    .setPackageName(packageName)
                    .setClassName(className)
                    .setMethodName(methodName)
                    .setParamList(paramListString);
            result.add(ms);
        }
    }
}
