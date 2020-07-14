package edu.pku.migrationhelper.data.api;

import com.google.common.collect.Sets;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class ClassChange {
    private final ClassSignature oldClass;
    private final ClassSignature newClass;
    private final List<MethodChange> changedMethods;
    private final List<FieldChange> changedFields;

    public ClassChange(ClassSignature oldClass, ClassSignature newClass) {
        assert oldClass != null || newClass != null;
        this.oldClass = oldClass;
        this.newClass = newClass;
        if (oldClass == null) {
            this.changedMethods = newClass.getMethods().stream()
                    .map(m -> new MethodChange(null, m))
                    .collect(Collectors.toList());
            this.changedFields = newClass.getFields().stream()
                    .map(f -> new FieldChange(null, f))
                    .collect(Collectors.toList());
        } else if (newClass == null) {
            this.changedMethods = oldClass.getMethods().stream()
                    .map(m -> new MethodChange(m, null))
                    .collect(Collectors.toList());
            this.changedFields = oldClass.getFields().stream()
                    .map(f -> new FieldChange(f, null))
                    .collect(Collectors.toList());
        } else {
            this.changedMethods = getMethodChangesFromClassPair(oldClass, newClass);
            this.changedFields = getFieldChangesFromClassPair(oldClass, newClass);
        }
    }

    public ClassSignature getOldClass() {
        return oldClass;
    }

    public ClassSignature getNewClass() {
        return newClass;
    }

    public List<MethodChange> getChangedMethods() {
        return changedMethods;
    }

    public List<FieldChange> getChangedFields() {
        return changedFields;
    }


    private int getSimilarity(MethodSignature m1, MethodSignature m2) {
        if (m1.equals(m2)) return Integer.MAX_VALUE;
        int sim = 0;
        if (m1.getName().equals(m2.getName())) sim += 10;
        sim += Sets.intersection(Sets.newHashSet(m1.getParameters()), Sets.newHashSet(m2.getParameters())).size();
        sim += Sets.intersection(Sets.newHashSet(m1.getExceptions()), Sets.newHashSet(m2.getExceptions())).size();
        sim += m1.getType().equals(m2.getType()) ? 1 : 0;
        sim += Long.bitCount(m1.getFlags() & m2.getFlags());
        return sim;
    }

    private List<MethodChange> getMethodChangesFromClassPair(ClassSignature oldClass, ClassSignature newClass) {
        Set<MethodSignature> oldMethods = new HashSet<>(oldClass.getMethods());
        Set<MethodSignature> newMethods = new HashSet<>(newClass.getMethods());
        Set<MethodSignature> addedMethods = new HashSet<>(Sets.difference(newMethods, oldMethods));
        Set<MethodSignature> removedMethods = new HashSet<>(Sets.difference(oldMethods, newMethods));
        List<MethodChange> result = new ArrayList<>();
        for (MethodSignature added : addedMethods) {
            int currSim = 7;
            MethodSignature removed = null;
            for (MethodSignature rm : removedMethods) {
                if (getSimilarity(added, rm) >= currSim) {
                    removed = rm;
                    currSim = getSimilarity(added, rm);
                }
            }
            removedMethods.remove(removed);
            result.add(new MethodChange(removed, added));
        }
        for (MethodSignature ms : removedMethods) {
            result.add(new MethodChange(ms, null));
        }
        result.sort((m1, m2) -> {
            if (m1.getOldMethod() == null) return -1;
            if (m2.getOldMethod() == null) return 1;
            return m1.getOldMethod().getNameWithParameters().compareTo(m2.getOldMethod().getNameWithParameters());
        });
        return result;
    }

    private List<FieldChange> getFieldChangesFromClassPair(ClassSignature oldClass, ClassSignature newClass) {
        Map<String, FieldSignature> oldFields = oldClass.getFields().stream()
                .collect(Collectors.toMap(FieldOrMethod::getName, x -> x));
        Map<String, FieldSignature> newFields = newClass.getFields().stream()
                .collect(Collectors.toMap(FieldOrMethod::getName, x -> x));
        Set<String> names = new HashSet<>(oldFields.keySet());
        names.addAll(newFields.keySet());
        return names.stream()
                .filter(n -> oldFields.get(n) == null || !oldFields.get(n).equals(newFields.get(n)))
                .map(n -> new FieldChange(oldFields.get(n), newFields.get(n)))
                .sorted((f1, f2) -> {
                    if (f1.getOldField() == null) return -1;
                    if (f2.getOldField() == null) return 1;
                    return f1.getOldField().getName().compareTo(f2.getOldField().getName());
                })
                .collect(Collectors.toList());
    }
}
