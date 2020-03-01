package edu.pku.migrationhelper.service;

import edu.pku.migrationhelper.data.MethodSignature;
import edu.pku.migrationhelper.mapper.MethodSignatureMapper;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Utility;
import org.apache.bcel.generic.Type;
import org.apache.bcel.util.ClassPath;
import org.apache.bcel.util.MemorySensitiveClassPathRepository;
import org.apache.bcel.util.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Service
public class JarAnalysisService {

    Logger LOG = LoggerFactory.getLogger(getClass());

    public List<MethodSignature> analyzeJar(String filePath, List<MethodSignature> result) throws Exception {
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
            LOG.error("analyzeJar fail filePath = {}", filePath);
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

    public void analyzeClass(JavaClass clz, Repository repository, List<MethodSignature> result) {
        String packageName = clz.getPackageName();
        String className = clz.getClassName();
        if(packageName == null) packageName = "";
        className = className.substring(className.lastIndexOf('.') + 1);
        MethodSignature ms = new MethodSignature()
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

    public void analyzeClassMethods(JavaClass clz, String packageName, String className, List<MethodSignature> result) {
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
            MethodSignature ms = new MethodSignature()
                    .setPackageName(packageName)
                    .setClassName(className)
                    .setMethodName(methodName)
                    .setParamList(paramListString);
            result.add(ms);
        }
    }
}
