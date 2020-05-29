package anonymous.migrationhelper.service;

import anonymous.migrationhelper.data.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import spoon.Launcher;
import spoon.reflect.code.*;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.compiler.FileSystemFile;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

/**
 * Created by xuyul on 2020/1/13.
 */
@Service
public class JavaCodeAnalysisService {

    Logger LOG = LoggerFactory.getLogger(getClass());

    // TODO
    // 标识符识别的方法，难以鉴别带有继承关系的参数，比如用子类作为参数去调用一个以父类为参数的API，会识别成调用了一个以子类为参数的API，最终导致无法查到到
    public List<MethodSignature> analyzeJavaCode(String javaCode) {
        List<MethodSignature> result = new LinkedList<>();
        if(javaCode == null || "".equals(javaCode)) return result;
        StringBuilder stringBuilder = new StringBuilder();
        File tmpFile = null;

        try {
            tmpFile = File.createTempFile("java_code_analysis", ".java");
            tmpFile.deleteOnExit();
            try(FileOutputStream fileOutputStream = new FileOutputStream(tmpFile)) {
                fileOutputStream.write(javaCode.getBytes());
            }

            Launcher launcher = new Launcher();
            launcher.addInputResource(new FileSystemFile(tmpFile));
            launcher.getEnvironment().setNoClasspath(true);
            launcher.getEnvironment().setAutoImports(true);
            Collection<CtType<?>> allTypes = launcher.buildModel().getAllTypes();
            for (CtType<?> type : allTypes) {
                if (!(type instanceof CtClass)) continue;
                CtClass<?> ctClass = (CtClass) type;
                for (CtMethod<?> method : ctClass.getAllMethods()) {
                    CtBlock<?> methodBody = method.getBody();
                    if (methodBody == null) continue;
                    Iterable<CtElement> it = methodBody.asIterable();
                    it.forEach(element -> {
                        if (element instanceof CtInvocation) {
                            CtInvocation invocation = (CtInvocation) element;

                            SourcePosition position = element.getPosition();
                            if (!position.isValidPosition()) return;

                            MethodSignature methodSignature = new MethodSignature();

                            methodSignature.setStartLine(position.getLine());
                            methodSignature.setEndLine(position.getEndLine());

                            for (CtElement directChild : invocation.getDirectChildren()) {
                                if (directChild instanceof CtVariableAccess) {
                                    CtVariableAccess expression = (CtVariableAccess) directChild;
                                    methodSignature.setPackageName(getTypePackageName(expression.getType()));
                                    methodSignature.setClassName(getTypeName(expression.getType()));
                                    break;
                                } else if (directChild instanceof CtTypeAccess) {
                                    CtTypeAccess expression = (CtTypeAccess) directChild;
                                    methodSignature.setPackageName(getTypePackageName(expression.getAccessedType()));
                                    methodSignature.setClassName(getTypeName(expression.getAccessedType()));
                                    break;
                                }
                            }

                            CtExecutableReference executableReference = invocation.getExecutable();
                            if (methodSignature.getPackageName() == null) {
                                methodSignature.setPackageName(getTypePackageName(executableReference.getDeclaringType()));
                                methodSignature.setClassName(getTypeName(executableReference.getDeclaringType()));
                            }
                            methodSignature.setMethodName(executableReference.getSimpleName());
                            stringBuilder.delete(0, stringBuilder.length());
                            for (Object parameter : executableReference.getParameters()) {
                                CtTypeReference<?> p = (CtTypeReference<?>) parameter;
                                if (p.getPackage() != null) {
                                    stringBuilder.append(getTypePackageName(p));
                                    stringBuilder.append(".");
                                }
                                stringBuilder.append(p.getSimpleName());
                                stringBuilder.append(",");
                            }
                            if (stringBuilder.length() > 0) {
                                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                            }
                            methodSignature.setParamList(stringBuilder.toString());

                            result.add(methodSignature);
                        }
                    });
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if(tmpFile != null) {
                tmpFile.delete();
            }
        }
    }

    private String getTypeName(CtTypeReference tr) {
        if(tr == null) return "";
        return tr.getSimpleName();
    }

    private String getTypePackageName(CtTypeReference tr) {
        if(tr == null) return "";
        CtPackageReference pr = tr.getPackage();
        if(pr == null) return "";
        return pr.getSimpleName();
    }
}
