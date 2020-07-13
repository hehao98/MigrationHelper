package edu.pku.migrationhelper.data.api;

import java.util.Collections;
import java.util.List;

class FieldOrMethod {
    protected long flags;
    protected String type;
    protected String signature;
    protected String name;
    protected List<Annotation> annotations;

    public FieldOrMethod() {}

    public FieldOrMethod(long flags, String type, String signature, String name, List<Annotation> annotations) {
        this.flags = flags;
        this.type = type;
        this.signature = signature;
        this.name = name;
        this.annotations = annotations;
    }

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

    public String getFlagString() {
        StringBuilder s = new StringBuilder();
        if (isPublic()) s.append("public ");
        else if (isProtected()) s.append("protected ");
        else if (isPrivate()) s.append("private ");
        if (isStatic()) s.append("static ");
        if (isAbstract()) s.append("abstract ");
        if (isFinal()) s.append("final ");
        if (isSynchronized()) s.append("synchronized ");
        if (isVolatile()) s.append("volatile ");
        if (isNative()) s.append("native ");
        if (s.length() > 0) return s.substring(0, s.length() - 1);
        return s.toString();
    }

    public long getFlags() {
        return flags;
    }

    public FieldOrMethod setFlags(long flags) {
        this.flags = flags;
        return this;
    }

    public String getType() {
        return type;
    }

    public FieldOrMethod setType(String type) {
        this.type = type;
        return this;
    }

    public String getSignature() {
        return signature;
    }

    public FieldOrMethod setSignature(String signature) {
        this.signature = signature;
        return this;
    }

    public String getName() {
        return name;
    }

    public FieldOrMethod setName(String name) {
        this.name = name;
        return this;
    }

    public List<Annotation> getAnnotations() {
        return Collections.unmodifiableList(annotations);
    }

    public FieldOrMethod setAnnotations(List<Annotation> annotations) {
        this.annotations = annotations;
        return this;
    }

}
