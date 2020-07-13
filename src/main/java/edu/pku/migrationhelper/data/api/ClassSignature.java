package edu.pku.migrationhelper.data.api;

import org.apache.bcel.classfile.JavaClass;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.annotation.Id;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class for describing APIs of a Java Class.
 * This is intended to be for reading. Intensive use of setters may be slow.
 */
public class ClassSignature {

    public static final String ID_NULL = "0000000000000000000000000000000000000000";

    // All class, method, and field properties are aggregated here
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
    public static final long ENUM = 1 << 11;
    public static final long NESTED = 1 << 12;
    public static final long ANONYMOUS = 1 << 13;
    public static final long NATIVE = 1 << 14;

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

    private String superClassId = ID_NULL;

    private List<String> interfaceNames = new LinkedList<>();

    private List<String> interfaceIds = new LinkedList<>();

    private List<MethodSignature> methods = new LinkedList<>();

    private List<FieldSignature> fields = new LinkedList<>();

    public ClassSignature() {
        this.generateId();
    }

    public ClassSignature(JavaClass javaClass) {
        this(javaClass, false);
    }

    public ClassSignature(JavaClass javaClass, boolean publicOnly) {
        this.className = javaClass.getClassName();
        this.flags = getFlagsForClassSignature(javaClass);
        this.superClassName = javaClass.getSuperclassName();
        this.interfaceNames.addAll(Arrays.asList(javaClass.getInterfaceNames()));
        for (String ignored : javaClass.getInterfaceNames()) {
            this.interfaceIds.add(ID_NULL);
        }
        if (publicOnly) {
            this.methods = Arrays.stream(javaClass.getMethods())
                    .filter(m -> m.isPublic() || m.isProtected())
                    .map(MethodSignature::new).collect(Collectors.toList());
            this.fields = Arrays.stream(javaClass.getFields())
                    .filter(f -> f.isPublic() || f.isProtected()).
                    map(FieldSignature::new).collect(Collectors.toList());
        } else {
            this.methods = Arrays.stream(javaClass.getMethods()).map(MethodSignature::new).collect(Collectors.toList());
            this.fields = Arrays.stream(javaClass.getFields()).map(FieldSignature::new).collect(Collectors.toList());
        }
        this.generateId();
    }

    @Override
    public String toString() {
        return className + "#" + id.substring(0, 6);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassSignature that = (ClassSignature) o;
        return id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public String getId() {
        return id;
    }

    public long getFlags() {
        return flags;
    }

    private void generateId() {
        this.id = DigestUtils.sha1Hex(
                String.format("%x,%s,%s,%s,%s,%s,%s,%s", flags, className, superClassName, superClassId,
                    interfaceNames, interfaceIds, methods, fields));
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

    public boolean isEnum() {
        return (flags & ENUM) != 0;
    }

    public ClassSignature setEnum(boolean isEnum) {
        if (isEnum)
            flags = flags | ENUM;
        else
            flags = flags & ~ENUM;
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

    public String getSuperClassId() {
        return superClassId;
    }

    public ClassSignature setSuperClassId(String superClassId) {
        this.superClassId = superClassId;
        generateId();
        return this;
    }

    public Collection<String> getInterfaceNames() {
        return Collections.unmodifiableCollection(interfaceNames);
    }

    public ClassSignature setInterfaceNames(List<String> interfaceNames) {
        this.interfaceNames = interfaceNames;
        generateId();
        return this;
    }

    public Collection<String> getInterfaceIds() {
        return Collections.unmodifiableCollection(interfaceIds);
    }

    public ClassSignature setInterfaceIds(List<String> interfaceIds) {
        this.interfaceIds = interfaceIds;
        generateId();
        return this;
    }

    public String getInterfaceId(String interfaceName) {
        return interfaceIds.get(interfaceNames.indexOf(interfaceName));
    }

    public ClassSignature setInterfaceId(String interfaceName, String interfaceId) {
        int idx = interfaceNames.indexOf(interfaceName);
        if (idx == -1) {
            interfaceNames.add(interfaceName);
            interfaceIds.add(interfaceId);
        } else {
            interfaceIds.set(idx, interfaceId);
        }
        generateId();
        return this;
    }

    public List<String> getSuperClassAndInterfaceNames() {
        List<String> names = new ArrayList<>(interfaceNames);
        names.add(superClassName);
        return names;
    }

    public List<String> getSuperClassAndInterfaceIds() {
        List<String> names = new ArrayList<>(interfaceIds);
        names.add(superClassId);
        return names;
    }

    public List<MethodSignature> getMethods() {
        return Collections.unmodifiableList(methods);
    }

    public List<FieldSignature> getFields() {
        return Collections.unmodifiableList(fields);
    }

    public ClassSignature setMethods(List<MethodSignature> methods) {
        this.methods = methods;
        generateId();
        return this;
    }

    public ClassSignature setFields(List<FieldSignature> fields) {
        this.fields = fields;
        generateId();
        return this;
    }
}
