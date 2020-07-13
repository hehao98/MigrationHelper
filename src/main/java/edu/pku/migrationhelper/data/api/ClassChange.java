package edu.pku.migrationhelper.data.api;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private List<MethodChange> getMethodChangesFromClassPair(ClassSignature oldClass, ClassSignature newClass) {
        Map<String, MethodSignature> oldMethods = oldClass.getMethods().stream()
                .collect(Collectors.toMap(MethodSignature::toString, x -> x));
        Map<String, MethodSignature> newMethods = newClass.getMethods().stream()
                .collect(Collectors.toMap(MethodSignature::toString, x -> x));
        Set<String> names = new HashSet<>(oldMethods.keySet());
        names.addAll(newMethods.keySet());
        return names.stream()
                .filter(n -> oldMethods.get(n) == null || !oldMethods.get(n).equals(newMethods.get(n)))
                .map(n -> new MethodChange(oldMethods.get(n), newMethods.get(n)))
                .collect(Collectors.toList());
    }

    private List<FieldChange> getFieldChangesFromClassPair(ClassSignature oldClass, ClassSignature newClass) {
        Map<String, FieldSignature> oldFields = oldClass.getFields().stream()
                .collect(Collectors.toMap(FieldOrMethod::getName, x -> x));
        Map<String, FieldSignature> newFields = newClass.getFields().stream()
                .collect(Collectors.toMap(FieldOrMethod::getName, x -> x));
        Set<String> names = new HashSet<>(oldFields.keySet());
        names.addAll(newFields.keySet());
        return names.stream()
                .filter(n -> oldFields.get(n) != null || !oldFields.get(n).equals(newFields.get(n)))
                .map(n -> new FieldChange(oldFields.get(n), newFields.get(n)))
                .collect(Collectors.toList());
    }
}
