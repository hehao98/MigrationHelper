package edu.pku.migrationhelper.data.api;

import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class APIChange {

    private final String groupId;

    private final String artifactId;

    private final String fromVersion;

    private final String toVersion;

    private final List<ClassSignature> fromClasses;

    private final List<ClassSignature> toClasses;

    private final List<ClassChange> changedClasses;

    /**
     * Compare class to class API changes without considering super classes
     * If you want to use this with super class resolution,
     *   you need to have all the super classes and interfaces in the input lists
     */
    public APIChange(String groupId, String artifactId, String fromVersion, String toVersion,
                     List<ClassSignature> fromClasses, List<ClassSignature> toClasses) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.fromVersion = fromVersion;
        this.toVersion = toVersion;
        this.fromClasses = fromClasses;
        this.toClasses = toClasses;
        Map<String, ClassSignature> nameFromClasses = fromClasses.stream()
                .collect(Collectors.toMap(ClassSignature::getClassName, x -> x, (x, y) -> x));
        Map<String, ClassSignature> nameToClasses = toClasses.stream()
                .collect(Collectors.toMap(ClassSignature::getClassName, x -> x, (x, y) -> x));
        Set<String> names = Stream.concat(nameFromClasses.keySet().stream(), nameToClasses.keySet().stream())
                .collect(Collectors.toSet());
        this.changedClasses = names.stream()
                .filter(n -> nameFromClasses.get(n) == null || !nameFromClasses.get(n).equals(nameToClasses.get(n)))
                .map(n -> new ClassChange(nameFromClasses.get(n), nameToClasses.get(n)))
                .sorted((c1, c2) -> {
                    String str1 = "";
                    String str2 = "";
                    if (c1.getOldClass() != null) str1 = c1.getOldClass().getClassName();
                    if (c2.getOldClass() != null) str2 = c2.getOldClass().getClassName();
                    return str1.compareTo(str2);
                })
                .collect(Collectors.toList());
    }

    public void printAPIChange(PrintStream ps) {
        ps.printf("%s:%s %s -> %s\n", groupId, artifactId, fromVersion, toVersion);
        for (ClassChange chg : changedClasses) {
            ClassSignature c1 = chg.getOldClass();
            ClassSignature c2 = chg.getNewClass();
            ps.println("@@@ " + c1 + " -> " + c2);
            if (c1 != null && c2 != null && !c1.getSuperClassId().equals(c2.getSuperClassId())) {
                ps.printf("Super class changed: %s#%s -> %s#%s\n",
                        c1.getSuperClassName(), c1.getSuperClassId().substring(0, 6),
                        c2.getSuperClassName(), c2.getSuperClassId().substring(0, 6));
            }
            for (MethodChange mc : chg.getChangedMethods()) {
                if (mc.getOldMethod() != null) {
                    ps.println("- " + mc.getOldMethod());
                }
                if (mc.getNewMethod() != null) {
                    ps.println("+ " + mc.getNewMethod());
                }
            }
            for (FieldChange fc : chg.getChangedFields()) {
                if (fc.getOldField() != null) {
                    ps.println("- " + fc.getOldField());
                }
                if (fc.getNewField() != null) {
                    ps.println("+ " + fc.getNewField());
                }
            }
            ps.println();
        }
    }

    public long getBreakingChangeCount() {
        return changedClasses.stream()
                .mapToLong(chg ->
                        chg.getChangedFields().stream().filter(FieldChange::isBreakingChange).count()
                        + chg.getChangedMethods().stream().filter(MethodChange::isBreakingChange).count())
                .sum();
    }

    public long getAddedFieldCount() {
        return changedClasses.stream()
                .mapToLong(chg -> chg.getChangedFields().stream().filter(x -> x.getOldField() == null).count())
                .sum();
    }

    public long getRemovedFieldCount() {
        return changedClasses.stream()
                .mapToLong(chg -> chg.getChangedFields().stream().filter(x -> x.getNewField() == null).count())
                .sum();
    }

    public long getChangedFieldCount() {
        return changedClasses.stream()
                .mapToLong(chg -> chg.getChangedFields().stream()
                        .filter(x -> x.getOldField() != null && x.getNewField() != null)
                        .count())
                .sum();
    }

    public long getAddedMethodCount() {
        return changedClasses.stream()
                .mapToLong(chg -> chg.getChangedMethods().stream().filter(x -> x.getOldMethod() == null).count())
                .sum();
    }

    public long getRemovedMethodCount() {
        return changedClasses.stream()
                .mapToLong(chg -> chg.getChangedMethods().stream().filter(x -> x.getNewMethod() == null).count())
                .sum();
    }

    public long getChangedMethodCount() {
        return changedClasses.stream()
                .mapToLong(chg -> chg.getChangedMethods().stream()
                        .filter(x -> x.getOldMethod() != null && x.getNewMethod() != null)
                        .count())
                .sum();
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getFromVersion() {
        return fromVersion;
    }

    public String getToVersion() {
        return toVersion;
    }

    public List<ClassSignature> getFromClasses() {
        return Collections.unmodifiableList(fromClasses);
    }

    public List<ClassSignature> getToClasses() {
        return Collections.unmodifiableList(toClasses);
    }

    public List<ClassChange> getChangedClasses() {
        return Collections.unmodifiableList(changedClasses);
    }
}
