package edu.pku.migrationhelper.data.api;

import org.apache.bcel.classfile.Field;

class FieldOrMethod {
    protected long flags;
    protected String type;
    protected String signature;
    protected String name;

    public boolean isPublic() {
        return (flags & ClassSignature.PUBLIC) != 0;
    }

    public boolean isProtected() {
        return (flags & ClassSignature.PROTECTED) != 0;
    }

    public boolean isPackage() {
        return (flags & ClassSignature.PACKAGE) != 0;
    }

    public boolean isPrivate() {
        return (flags & ClassSignature.PRIVATE) != 0;
    }

    public boolean isStatic() {
        return (flags & ClassSignature.STATIC) != 0;
    }

    public boolean isFinal() {
        return (flags & ClassSignature.FINAL) != 0;
    }

    public boolean isAbstract() {
        return (flags & ClassSignature.ABSTRACT) != 0;
    }

    public boolean isSynchronized() {
        return (flags & ClassSignature.SYNCHRONIZED) != 0;
    }

    public boolean isVolatile() {
        return (flags & ClassSignature.VOLATILE) != 0;
    }

    public boolean isNative() {
        return (flags & ClassSignature.NATIVE) != 0;
    }
}
