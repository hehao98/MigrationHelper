package edu.pku.migrationhelper.service;

import edu.pku.migrationhelper.data.api.ClassSignature;
import edu.pku.migrationhelper.data.api.MethodSignatureOld;
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
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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

        // Load JAR file
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

        // Load classes
        Repository repository = new MemorySensitiveClassPathRepository(new ClassPath(filePath));
        List<ClassSignature> classSignatures = new ArrayList<>();
        Map<String, Integer> className2Index = new HashMap<>(100000);
        for (JavaClass c : classList) {
            repository.storeClass(c); // public class might inherit package class
            if (publicOnly && !c.isPublic())
                continue;
            ClassSignature cs = new ClassSignature(c, publicOnly);
            classSignatures.add(cs);
            className2Index.put(cs.getClassName(), classSignatures.size() - 1);
        }

        // Try to add any super class if it is resolvable, using breadth first search
        Queue<String> queue = new LinkedList<>();
        for (ClassSignature cs : classSignatures) {
            List<String> superClasses = new ArrayList<>(cs.getInterfaceNames());
            superClasses.add(cs.getSuperClassName());
            for (String superName : superClasses) {
                if (!className2Index.containsKey(superName)) {
                    queue.add(superName);
                }
            }
        }
        while (!queue.isEmpty()) {
            String curr = queue.poll();
            if (className2Index.containsKey(curr))
                continue;
            JavaClass c = repository.findClass(curr);
            if (c == null) {
                try {
                    // Try to load the class in current JRE, for finding java.util.*, etc
                    // It might accidentally load same name classes with a wrong version though
                    c = repository.loadClass(curr);
                } catch (ClassNotFoundException ex) {
                    LOG.error("{} not found in this JAR file and runtime environment", curr);
                    continue;
                }
            }
            ClassSignature cs = new ClassSignature(c);
            classSignatures.add(cs);
            className2Index.put(curr, classSignatures.size() - 1);

            List<String> superClasses = new ArrayList<>(cs.getInterfaceNames());
            superClasses.add(cs.getSuperClassName());
            for (String superName : superClasses) {
                if (!className2Index.containsKey(superName)) {
                    queue.add(superName);
                }
            }
        }
        assert classSignatures.size() == className2Index.size();

        // Build graph for topological sort
        Map<String, List<String>> className2Children = new HashMap<>(100000);
        Map<String, List<String>> className2Parent = new HashMap<>(100000);
        for (ClassSignature cs : classSignatures) {
            List<String> classes = new ArrayList<>(cs.getInterfaceNames());
            classes.add(cs.getSuperClassName());
            classes.add(cs.getClassName());
            for (String className : classes) {
                className2Children.computeIfAbsent(className, name -> new LinkedList<>());
                className2Parent.computeIfAbsent(className, name -> new LinkedList<>());
            }
        }
        for (ClassSignature cs : classSignatures) {
            if (cs.getClassName().equals("java.lang.Object")) {
                continue;
            }
            className2Children.get(cs.getSuperClassName()).add(cs.getClassName());
            for (String interfaceName : cs.getInterfaceNames()) {
                className2Children.get(interfaceName).add(cs.getClassName());
            }
            className2Parent.get(cs.getClassName()).add(cs.getSuperClassName());
            className2Parent.get(cs.getClassName()).addAll(cs.getInterfaceNames());
        }
        assert className2Children.containsKey("java.lang.Object");
        assert className2Parent.containsKey("java.lang.Object");

        // TODO: Solve inheritance relationship across JARs
        // Solve inheritance, we only consider classes within this JAR and Java standard libraries
        // Since the SHA will be changed if we modify any of its properties,
        //   we must fill in inheritance information in topological order
        // Here we assume that in one JAR, there cannot be classes of the same name
        List<String> classNamesInTopologicalOrder = new ArrayList<>();
        Queue<String> current = new LinkedList<>();
        for (ClassSignature cs : classSignatures) {
            if (className2Parent.get(cs.getClassName()).size() == 0) {
                current.add(cs.getClassName());
            }
        }
        while (!current.isEmpty()) {
            String className = current.poll();
            classNamesInTopologicalOrder.add(className);
            for (String childName : className2Children.get(className)) {
                className2Parent.get(childName).remove(className);
                if (className2Parent.get(childName).size() == 0) {
                    current.add(childName);
                }
            }
        }
        for (String className : classNamesInTopologicalOrder) {
            if (className.equals("java.lang.Object"))
                continue;
            if (className2Index.containsKey(className)) {
                int idx = className2Index.get(className);
                ClassSignature cs = classSignatures.get(idx);
                String superClassId = ClassSignature.ID_NULL;
                if (className2Index.containsKey(cs.getSuperClassName())) {
                    superClassId = classSignatures.get(className2Index.get(cs.getSuperClassName())).getId();
                }
                classSignatures.get(idx).setSuperClassId(superClassId);
                List<String> interfaceNames = new ArrayList<>(cs.getInterfaceNames());
                for (String interfaceName : interfaceNames) {
                    String id = ClassSignature.ID_NULL;
                    if (className2Index.containsKey(interfaceName)) {
                        id = classSignatures.get(className2Index.get(interfaceName)).getId();
                    }
                    classSignatures.get(idx).getInterfaceIds().add(id);
                }
            }
        }

        return classSignatures;
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
