package edu.pku.migrationhelper.service;

import edu.pku.migrationhelper.data.MethodSignature;
import org.springframework.stereotype.Service;
import spoon.Launcher;
import spoon.reflect.code.CtBlock;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Created by xuyul on 2020/1/13.
 */
@Service
public class JavaCodeAnalysisService {

    public List<MethodSignature> analyzeJavaCode(String javaCode) {
        CtClass<?> ctClass = Launcher.parseClass(javaCode);
        Set<String> existSignature = new HashSet<>();
        List<MethodSignature> result = new LinkedList<>();
        StringBuilder stringBuilder = new StringBuilder();
        for (CtMethod<?> method : ctClass.getAllMethods()) {
            CtBlock<?> methodBody = method.getBody();
            Iterable<CtElement> it = methodBody.asIterable();
            it.forEach(element -> {
                if(element instanceof CtExecutableReference) {
                    CtExecutableReference executableReference = (CtExecutableReference) element;
                    MethodSignature methodSignature = new MethodSignature();
                    methodSignature.setPackageName(executableReference.getType().getPackage().getSimpleName());
                    methodSignature.setClassName(executableReference.getType().getSimpleName());
                    methodSignature.setMethodName(executableReference.getSimpleName());
                    stringBuilder.delete(0, stringBuilder.length());
                    for (Object parameter : executableReference.getParameters()) {
                        CtTypeReference<?> p = (CtTypeReference<?>) parameter;
                        if(p.getPackage() != null) {
                            stringBuilder.append(p.getPackage().getSimpleName());
                            stringBuilder.append(".");
                        }
                        stringBuilder.append(p.getSimpleName());
                        stringBuilder.append(",");
                    }
                    if(stringBuilder.length() > 0) {
                        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                    }
                    methodSignature.setParamList(stringBuilder.toString());

                    stringBuilder.delete(0, stringBuilder.length());
                    stringBuilder.append(methodSignature.getPackageName());
                    stringBuilder.append("|");
                    stringBuilder.append(methodSignature.getClassName());
                    stringBuilder.append("|");
                    stringBuilder.append(methodSignature.getMethodName());
                    stringBuilder.append("|");
                    stringBuilder.append(methodSignature.getParamList());
                    String methodSignatureString = stringBuilder.toString();
                    if(!existSignature.contains(methodSignatureString)) {
                        existSignature.add(methodSignatureString);
                        result.add(methodSignature);
                    }
                }
            });
        }
        return result;
    }

}
