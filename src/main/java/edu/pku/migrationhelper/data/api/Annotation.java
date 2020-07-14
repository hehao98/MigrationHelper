package edu.pku.migrationhelper.data.api;

import javafx.util.Pair;
import org.apache.bcel.classfile.AnnotationEntry;
import org.apache.bcel.classfile.ElementValuePair;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Annotation {
    // For unknown reason in BCEL the className is JAR signature format
    //  e.g. java.lang.Deprecated will be stored as @Ljava/lang/Deprecated;
    private String className;
    private boolean isRuntimeVisible;
    private List<String> valuePairs;

    /**
     * For Spring data reflection, should not be used
     */
    public Annotation() {

    }

    public Annotation(String className, boolean isRuntimeVisible, List<String> valuePairs) {
        this.className = className;
        this.isRuntimeVisible = isRuntimeVisible;
        this.valuePairs = valuePairs;
    }

    public Annotation(AnnotationEntry entry) {
        this.className  = entry.getAnnotationType();
        this.isRuntimeVisible = entry.isRuntimeVisible();
        this.valuePairs = Arrays.stream(entry.getElementValuePairs())
                .map(ElementValuePair::toShortString)
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        String s = "";
        if (valuePairs.size() > 0) {
            s = String.format("(%s)", String.join(",", valuePairs));
        }
        return String.format("@%s%s", getClassName(), s);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Annotation that = (Annotation) o;
        return isRuntimeVisible == that.isRuntimeVisible &&
                className.equals(that.className) &&
                valuePairs.equals(that.valuePairs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, isRuntimeVisible, valuePairs);
    }

    public String getClassName() {
        int i = className.indexOf("L");
        int j = className.indexOf(";");
        return className.substring(i + 1, j).replace("/", ".");
    }

    public List<String> getValuePairs() {
        return valuePairs;
    }

    public Annotation setClassName(String className) {
        this.className = className;
        return this;
    }

    public boolean isRuntimeVisible() {
        return isRuntimeVisible;
    }

    public Annotation setRuntimeVisible(boolean runtimeVisible) {
        isRuntimeVisible = runtimeVisible;
        return this;
    }

    public Annotation setValuePairs(List<String> valuePairs) {
        this.valuePairs = valuePairs;
        return this;
    }
}
