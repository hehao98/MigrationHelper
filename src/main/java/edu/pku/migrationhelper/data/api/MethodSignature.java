package edu.pku.migrationhelper.data.api;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

    private List<String> exceptions;

    public MethodSignature() {}

    public MethodSignature(Method m) {
        this.flags = getFlagsForMethodSignature(m);
        this.name = m.getName();
        this.type = m.getReturnType().toString();
        this.signature = m.getGenericSignature();
        this.parameters = Arrays.stream(m.getArgumentTypes()).map(Type::toString).collect(Collectors.toList());
        this.annotations = Arrays.stream(m.getAnnotationEntries()).map(Annotation::new).collect(Collectors.toList());
        if (m.getExceptionTable() == null) this.exceptions = new ArrayList<>();
        else this.exceptions = Arrays.asList(m.getExceptionTable().getExceptionNames());
    }

    @Override
    public String toString() {
        String annotationString = annotations.stream().map(Annotation::toString).collect(Collectors.joining(" "));
        String annotationDelimiter = annotationString.length() == 0 ? "" : " ";
        String flagString = getFlagString();
        String flagDelimiter = flagString.length() == 0 ? "" : " ";
        String s = String.format("%s%s%s%s%s %s(%s)", annotationString, annotationDelimiter, flagString, flagDelimiter,
                type, name, String.join(",", parameters));
        if (exceptions.size() > 0)
            s += String.format(" throws %s", String.join(",", exceptions));
        return s;
    }

    public List<String> getParameters() {
        return Collections.unmodifiableList(parameters);
    }

    public List<String> getExceptions() {
        return Collections.unmodifiableList(exceptions);
    }
}
