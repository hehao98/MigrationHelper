package edu.pku.migrationhelper.service;

import edu.pku.migrationhelper.data.MethodSignature;
import org.springframework.stereotype.Service;
import spoon.Launcher;
import spoon.reflect.code.*;
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

    // TODO
    // 标识符识别的方法，难以鉴别带有继承关系的参数，比如用子类作为参数去调用一个以父类为参数的API，会识别成调用了一个以子类为参数的API，最终导致无法查到到
    public List<MethodSignature> analyzeJavaCode(String javaCode) {
        CtClass<?> ctClass = Launcher.parseClass(javaCode);
        Set<String> existSignature = new HashSet<>();
        List<MethodSignature> result = new LinkedList<>();
        StringBuilder stringBuilder = new StringBuilder();
        for (CtMethod<?> method : ctClass.getAllMethods()) {
            CtBlock<?> methodBody = method.getBody();
            Iterable<CtElement> it = methodBody.asIterable();
            it.forEach(element -> {
                if(element instanceof CtInvocation) {
                    CtInvocation invocation = (CtInvocation) element;
                    MethodSignature methodSignature = new MethodSignature();
                    for (CtElement directChild : invocation.getDirectChildren()) {
                        if(directChild instanceof CtVariableAccess) {
                            CtVariableAccess expression = (CtVariableAccess) directChild;
                            methodSignature.setPackageName(expression.getType().getPackage().getSimpleName());
                            methodSignature.setClassName(expression.getType().getSimpleName());
                            break;
                        } else if (directChild instanceof CtTypeAccess) {
                            CtTypeAccess expression = (CtTypeAccess) directChild;
                            methodSignature.setPackageName(expression.getAccessedType().getPackage().getSimpleName());
                            methodSignature.setClassName(expression.getAccessedType().getSimpleName());
                            break;
                        }
                    }

                    CtExecutableReference executableReference = invocation.getExecutable();
                    if(methodSignature.getPackageName() == null) {
                        methodSignature.setPackageName(executableReference.getDeclaringType().getPackage().getSimpleName());
                        methodSignature.setClassName(executableReference.getDeclaringType().getSimpleName());
                    }
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
