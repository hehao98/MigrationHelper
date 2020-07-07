package edu.pku.migrationhelper.data.api;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Type;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MethodSignature extends FieldOrMethod {

    public static long getFlagsForMethodSignature(Method m) {
        long flag = 0;

        // Visibility
        if (m.isPublic()) flag |= ClassSignature.PUBLIC;
        else if (m.isProtected()) flag |= ClassSignature.PROTECTED;
        else if (m.isPrivate()) flag |= ClassSignature.PRIVATE;
        else flag |= ClassSignature.PACKAGE;

        // Other properties
        if (m.isAbstract()) flag |= ClassSignature.ABSTRACT;
        if (m.isFinal()) flag |= ClassSignature.FINAL;
        if (m.isStatic()) flag |= ClassSignature.STATIC;
        if (m.isVolatile()) flag |= ClassSignature.VOLATILE;
        if (m.isSynchronized()) flag |= ClassSignature.SYNCHRONIZED;
        if (m.isTransient()) flag |= ClassSignature.TRANSIENT;
        if (m.isNative()) flag |= ClassSignature.NATIVE;

        return flag;
    }

    private List<String> parameters;

    public MethodSignature() {}

    public MethodSignature(Method m) {
        this.flags = getFlagsForMethodSignature(m);
        this.name = m.getName();
        this.type = m.getReturnType().toString();
        this.signature = m.getGenericSignature();
        this.parameters = Arrays.stream(m.getArgumentTypes()).map(Type::toString).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return String.format("%s %s(%s)", type, name, String.join(",", parameters));
    }

    public List<String> getParameters() {
        return parameters;
    }

    public MethodSignature setParameters(List<String> parameters) {
        this.parameters = parameters;
        return this;
    }
}
