package edu.pku.migrationhelper.data;

public class MethodSignature {

    private long id;

    private String packageName;

    private String className;

    private String methodName;

    private String paramList;

    private int startLine; // not persistence, using by analysis of java code only

    private int endLine; // not persistence, using by analysis of java code only

    @Override
    public String toString() {
        return String.format("%s.%s.%s(%s)", packageName, className, methodName, paramList);
    }

    public long getId() {
        return id;
    }

    public MethodSignature setId(long id) {
        this.id = id;
        return this;
    }

    public String getPackageName() {
        return packageName;
    }

    public MethodSignature setPackageName(String packageName) {
        this.packageName = packageName;
        return this;
    }

    public String getClassName() {
        return className;
    }

    public MethodSignature setClassName(String className) {
        this.className = className;
        return this;
    }

    public String getMethodName() {
        return methodName;
    }

    public MethodSignature setMethodName(String methodName) {
        this.methodName = methodName;
        return this;
    }

    public String getParamList() {
        return paramList;
    }

    public MethodSignature setParamList(String paramList) {
        this.paramList = paramList;
        return this;
    }

    public int getStartLine() {
        return startLine;
    }

    public MethodSignature setStartLine(int startLine) {
        this.startLine = startLine;
        return this;
    }

    public int getEndLine() {
        return endLine;
    }

    public MethodSignature setEndLine(int endLine) {
        this.endLine = endLine;
        return this;
    }
}
