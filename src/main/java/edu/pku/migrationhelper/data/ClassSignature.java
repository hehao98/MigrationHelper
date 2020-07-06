package edu.pku.migrationhelper.data;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.annotation.Id;

import java.util.Arrays;
import java.util.Objects;

public class ClassSignature {

    public static final long PUBLIC = 1;
    public static final long PACKAGE = 1 << 1;
    public static final long FINAL = 1 << 2;
    public static final long ABSTRACT = 1 << 3;

    @Id
    private String id;

    private long modifiers;

    private String className; // along with package name, e.g. java.lang.Object

    private String superClassName;

    private String[] interfaceNames;

    private long[] methodIds;

    private long[] fieldIds;

    @Override
    public String toString() {
        return className + "#" + id.substring(0, 6);
    }

    private void generateId() {
        this.id = DigestUtils.sha1Hex(Long.toHexString(modifiers) + className + superClassName
                + Arrays.toString(interfaceNames) + Arrays.toString(methodIds) + Arrays.toString(fieldIds));
    }

    public boolean isPublic() {
        return (modifiers & PUBLIC) != 0;
    }

    public ClassSignature setPublic(boolean isPublic) {
        if (isPublic) {
            modifiers = (modifiers | PUBLIC) & ~PACKAGE;
        } else {
            modifiers = (modifiers & ~PUBLIC) | PACKAGE;
        }
        generateId();
        return this;
    }

    public boolean isPackage() {
        return (modifiers & PACKAGE) != 0;
    }

    public ClassSignature setPackage(boolean isPackage) {
        if (isPackage) {
            modifiers = (modifiers | PACKAGE) & ~PUBLIC;
        } else {
            modifiers = (modifiers & ~PACKAGE) & ~PUBLIC;
        }
        generateId();
        return this;
    }

    public boolean isFinal() {
        return (modifiers & FINAL) != 0;
    }

    public ClassSignature setFinal(boolean isFinal) {
        if (isFinal)
            modifiers = modifiers | FINAL;
        else
            modifiers = modifiers & ~FINAL;
        generateId();
        return this;
    }

    public boolean isAbstract() {
        return (modifiers & ABSTRACT) != 0;
    }

    public ClassSignature setAbstract(boolean isAbstract) {
        if (isAbstract)
            modifiers = modifiers | ABSTRACT;
        else
            modifiers = modifiers & ~ABSTRACT;
        generateId();
        return this;
    }

    public String getPackageName() {
        return className.substring(0, className.lastIndexOf('.'));
    }

    public String getClassName() {
        return className;
    }

    public ClassSignature setClassName(String className) {
        this.className = className;
        generateId();
        return this;
    }

    public String getSuperClassName() {
        return superClassName;
    }

    public ClassSignature setSuperClassName(String superClassName) {
        this.superClassName = superClassName;
        generateId();
        return this;
    }

    public String[] getInterfaceNames() {
        return interfaceNames;
    }

    public ClassSignature setInterfaceNames(String[] interfaceNames) {
        this.interfaceNames = interfaceNames;
        generateId();
        return this;
    }

    public long[] getMethodIds() {
        return methodIds;
    }

    public ClassSignature setMethodIds(long[] methodIds) {
        this.methodIds = methodIds;
        generateId();
        return this;
    }

    public long[] getFieldIds() {
        return fieldIds;
    }

    public ClassSignature setFieldIds(long[] fieldIds) {
        this.fieldIds = fieldIds;
        generateId();
        return this;
    }
}
