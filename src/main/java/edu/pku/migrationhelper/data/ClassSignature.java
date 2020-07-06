package edu.pku.migrationhelper.data;

public class ClassSignature {

    public final long PUBLIC = 1;
    public final long PACKAGE = 1 << 1;
    public final long FINAL = 1 << 2;
    public final long ABSTRACT = 1 << 3;

    private long id;

    private long modifiers;

    private String className; // along with package name, e.g. java.lang.Object

    private String superClassName;

    private String[] interfaceNames;

    private long[] methodIds;

    private long[] fieldIds;

    @Override
    public String toString() {
        return className;
    }

    public long getId() {
        return id;
    }

    public ClassSignature setId(long id) {
        this.id = id;
        return this;
    }

    public boolean isPublic() {
        return (modifiers & PUBLIC) != 0;
    }

    public ClassSignature setPublic(boolean isPublic) {
        if (isPublic) {
            modifiers = modifiers | PUBLIC;
            modifiers = modifiers & ~PACKAGE;
        } else {
            modifiers = modifiers & ~PUBLIC;
            modifiers = modifiers | PACKAGE;
        }
        return this;
    }

    public boolean isPackage() {
        return (modifiers & PACKAGE) != 0;
    }

    public ClassSignature setPackage(boolean isPackage) {
        if (isPackage) {
            modifiers = modifiers | PACKAGE;
            modifiers = modifiers & ~PUBLIC;
        } else {
            modifiers = modifiers & ~PACKAGE;
            modifiers = modifiers | PUBLIC;
        }
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
        return this;
    }

    public String getSuperClassName() {
        return superClassName;
    }

    public ClassSignature setSuperClassName(String superClassName) {
        this.superClassName = superClassName;
        return this;
    }

    public String[] getInterfaceNames() {
        return interfaceNames;
    }

    public ClassSignature setInterfaceNames(String[] interfaceNames) {
        this.interfaceNames = interfaceNames;
        return this;
    }

    public long[] getMethodIds() {
        return methodIds;
    }

    public ClassSignature setMethodIds(long[] methodIds) {
        this.methodIds = methodIds;
        return this;
    }

    public long[] getFieldIds() {
        return fieldIds;
    }

    public ClassSignature setFieldIds(long[] fieldIds) {
        this.fieldIds = fieldIds;
        return this;
    }
}
