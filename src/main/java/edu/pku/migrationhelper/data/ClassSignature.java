package edu.pku.migrationhelper.data;

import org.apache.bcel.classfile.JavaClass;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.annotation.Id;

import java.util.Arrays;

/**
 * Class for describing APIs of a Java Class.
 * This is intended to be for reading. Intensive use of setters may be slow.
 */
public class ClassSignature {

    public static final long PUBLIC = 1;
    public static final long PROTECTED = 1 << 1;
    public static final long PACKAGE = 1 << 2;
    public static final long PRIVATE = 1 << 3;
    public static final long STATIC = 1 << 4;
    public static final long FINAL = 1 << 5;
    public static final long ABSTRACT = 1 << 6;
    public static final long TRANSIENT = 1 << 7;
    public static final long SYNCHRONIZED = 1 << 8;
    public static final long VOLATILE = 1 << 9;
    public static final long INTERFACE = 1 << 10;
    public static final long NESTED = 1 << 11;
    public static final long ANONYMOUS = 1 << 12;

    public static long getFlagsForClassSignature(JavaClass c) {
        long flag = 0;
        if (c.isPublic()) flag |= ClassSignature.PUBLIC;
        else flag |= ClassSignature.PACKAGE;
        if (c.isAnonymous()) flag |= ClassSignature.ANONYMOUS;
        if (c.isNested()) flag |= ClassSignature.NESTED;
        if (c.isInterface()) flag |= ClassSignature.INTERFACE;
        if (c.isAbstract()) flag |= ClassSignature.ABSTRACT;
        if (c.isFinal()) flag |= ClassSignature.FINAL;
        return flag;
    }

    @Id
    private String id;

    private String className; // along with package name, e.g. java.lang.Object

    private long flags;

    private String superClassName;

    private String[] interfaceNames;

    private long[] methodIds;

    private long[] fieldIds;

    public ClassSignature() {
        this.generateId();
    }

    public ClassSignature(String className, long flags, String superClassName, String[] interfaceNames) {
        this.className = className;
        this.flags = flags;
        this.superClassName = superClassName;
        this.interfaceNames = interfaceNames;
        this.generateId();
    }

    public ClassSignature(JavaClass javaClass) {
        this.className = javaClass.getClassName();
        this.flags = getFlagsForClassSignature(javaClass);
        this.superClassName = javaClass.getSuperclassName();
        this.interfaceNames = javaClass.getInterfaceNames();
        this.generateId();
    }

    @Override
    public String toString() {
        return className + "#" + id.substring(0, 6);
    }

    public String getId() {
        return id;
    }

    private void generateId() {
        this.id = DigestUtils.sha1Hex(
                String.format("%x,%s,%s,%s,%s,%s", flags, className, superClassName,
                    Arrays.toString(interfaceNames), Arrays.toString(methodIds), Arrays.toString(fieldIds)));
    }

    public boolean isPublic() {
        return (flags & PUBLIC) != 0;
    }

    public ClassSignature setPublic(boolean isPublic) {
        if (isPublic) {
            flags = (flags | PUBLIC) & ~PACKAGE;
        } else {
            flags = (flags & ~PUBLIC) | PACKAGE;
        }
        generateId();
        return this;
    }

    public boolean isPackage() {
        return (flags & PACKAGE) != 0;
    }

    public ClassSignature setPackage(boolean isPackage) {
        if (isPackage) {
            flags = (flags | PACKAGE) & ~PUBLIC;
        } else {
            flags = (flags & ~PACKAGE) & ~PUBLIC;
        }
        generateId();
        return this;
    }

    public boolean isFinal() {
        return (flags & FINAL) != 0;
    }

    public ClassSignature setFinal(boolean isFinal) {
        if (isFinal)
            flags = flags | FINAL;
        else
            flags = flags & ~FINAL;
        generateId();
        return this;
    }

    public boolean isAbstract() {
        return (flags & ABSTRACT) != 0;
    }

    public ClassSignature setAbstract(boolean isAbstract) {
        if (isAbstract)
            flags = flags | ABSTRACT;
        else
            flags = flags & ~ABSTRACT;
        generateId();
        return this;
    }

    public boolean isInterface() {
        return (flags & INTERFACE) != 0;
    }

    public ClassSignature setInterface(boolean isInterface) {
        if (isInterface)
            flags = flags | INTERFACE;
        else
            flags = flags & ~INTERFACE;
        generateId();
        return this;
    }

    public boolean isNested() {
        return (flags & NESTED) != 0;
    }

    public ClassSignature setNested(boolean isNested) {
        if (isNested)
            flags = flags | NESTED;
        else
            flags = flags & ~NESTED;
        generateId();
        return this;
    }

    public boolean isAnonymous() {
        return (flags & ANONYMOUS) != 0;
    }

    public ClassSignature setAnonymous(boolean isAnonymous) {
        if (isAnonymous)
            flags = flags | ANONYMOUS;
        else
            flags = flags & ~ANONYMOUS;
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
