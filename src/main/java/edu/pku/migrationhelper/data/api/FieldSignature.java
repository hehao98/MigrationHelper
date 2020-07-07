package edu.pku.migrationhelper.data.api;

import org.apache.bcel.classfile.Field;

public class FieldSignature extends FieldOrMethod {
    public static long getFlagsForFieldSignature(Field f) {
        long flag = 0;

        // Visibility
        if (f.isPublic()) flag |= ClassSignature.PUBLIC;
        else if (f.isProtected()) flag |= ClassSignature.PROTECTED;
        else if (f.isPrivate()) flag |= ClassSignature.PRIVATE;
        else flag |= ClassSignature.PACKAGE;

        // Other properties
        if (f.isAbstract()) flag |= ClassSignature.ABSTRACT;
        if (f.isFinal()) flag |= ClassSignature.FINAL;
        if (f.isStatic()) flag |= ClassSignature.STATIC;
        if (f.isVolatile()) flag |= ClassSignature.VOLATILE;
        if (f.isSynchronized()) flag |= ClassSignature.SYNCHRONIZED;
        if (f.isTransient()) flag |= ClassSignature.TRANSIENT;
        if (f.isNative()) flag |= ClassSignature.NATIVE;

        return flag;
    }

    public FieldSignature() {}

    public FieldSignature(Field f) {
        this.name = f.getName();
        this.signature = f.getGenericSignature();
        this.type = f.getType().toString();
        this.flags = getFlagsForFieldSignature(f);
    }

    @Override
    public String toString() {
        return String.format("%s %s", type, name);
    }

}
