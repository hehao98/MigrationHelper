package edu.pku.migrationhelper.data.api;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Deprecated
public class MethodSignatureOld {

    public static List<String> getPackages(List<MethodSignatureOld> methodSignatureOlds) {
        return methodSignatureOlds.stream().
                map(MethodSignatureOld::getPackageName).distinct().collect(Collectors.toList());
    }

    public static List<String> getClasses(List<MethodSignatureOld> methodSignatureOlds) {
        return methodSignatureOlds.stream()
                .map(ms -> ms.getPackageName() + "." + ms.getClassName())
                .distinct().collect(Collectors.toList());
    }

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

    public MethodSignatureOld setId(long id) {
        this.id = id;
        return this;
    }

    public String getPackageName() {
        return packageName;
    }

    public MethodSignatureOld setPackageName(String packageName) {
        final int PACKAGE_NAME_MAX_LENGTH = 255;
        if (packageName.length() > PACKAGE_NAME_MAX_LENGTH) {
            packageName = packageName.substring(0, PACKAGE_NAME_MAX_LENGTH);
        }
        this.packageName = toAscii(packageName);
        return this;
    }

    public String getClassName() {
        return className;
    }

    public MethodSignatureOld setClassName(String className) {
        final int CLASS_NAME_MAX_LENGTH = 255;
        if (className.length() > CLASS_NAME_MAX_LENGTH) {
            className = className.substring(0, CLASS_NAME_MAX_LENGTH);
        }
        this.className = toAscii(className);
        return this;
    }

    public String getMethodName() {
        return methodName;
    }

    public MethodSignatureOld setMethodName(String methodName) {
        final int METHOD_NAME_MAX_LENGTH = 255;
        if (methodName.length() > METHOD_NAME_MAX_LENGTH) {
            methodName = methodName.substring(0, METHOD_NAME_MAX_LENGTH);
        }
        this.methodName = toAscii(methodName);
        return this;
    }

    public String getParamList() {
        return paramList;
    }

    public MethodSignatureOld setParamList(String paramList) {
        // Under very rare circumstances the length of param_list might be too long to be fit in varchar(2047)
        // Then we have to manually truncate the param_list
        final int PARAM_LIST_MAX_LENGTH = 2047;
        if (paramList.length() > PARAM_LIST_MAX_LENGTH) {
            List<String> pl = Arrays.asList(paramList.split(","));
            int finalIndex = PARAM_LIST_MAX_LENGTH;
            int sum = 0;
            for (int i = 0; i < pl.size(); ++i) {
                sum += pl.get(i).length() + 1;
                if (sum > PARAM_LIST_MAX_LENGTH) {
                    finalIndex = i;
                    break;
                }
            }
            paramList = String.join(",", pl.subList(0, finalIndex));
        }
        this.paramList = toAscii(paramList);
        return this;
    }

    public int getStartLine() {
        return startLine;
    }

    public MethodSignatureOld setStartLine(int startLine) {
        this.startLine = startLine;
        return this;
    }

    public int getEndLine() {
        return endLine;
    }

    public MethodSignatureOld setEndLine(int endLine) {
        this.endLine = endLine;
        return this;
    }

    private String toAscii(String str) {
        StringBuilder sb = new StringBuilder(str.length());
        for (char c : str.toCharArray()) {
            if (c <= '\u007F') sb.append(c);
            else sb.append('?');
        }
        return sb.toString();
    }
}
