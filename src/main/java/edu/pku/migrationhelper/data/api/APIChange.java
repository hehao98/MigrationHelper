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
                .collect(Collectors.toMap(ClassSignature::getClassName, x -> x));
        Map<String, ClassSignature> nameToClasses = toClasses.stream()
                .collect(Collectors.toMap(ClassSignature::getClassName, x -> x));
        Set<String> names = Stream.concat(nameFromClasses.keySet().stream(), nameToClasses.keySet().stream())
                .collect(Collectors.toSet());
        this.changedClasses = names.stream()
                .filter(n -> nameFromClasses.get(n) == null || !nameFromClasses.get(n).equals(nameToClasses.get(n)))
                .map(n -> new ClassChange(nameFromClasses.get(n), nameToClasses.get(n)))
                .sorted((c1, c2) -> {
                    if (c1.getOldClass() == null) return -1;
                    if (c2.getOldClass() == null) return 1;
                    return c1.getOldClass().getClassName().compareTo(c2.getOldClass().getClassName());
                })
                .collect(Collectors.toList());
    }

    public void printAPIChange(PrintStream ps) {
        ps.printf("%s:%s %s -> %s\n", groupId, artifactId, fromVersion, toVersion);
        for (ClassChange chg : changedClasses) {
            ClassSignature c1 = chg.getOldClass();
            ClassSignature c2 = chg.getNewClass();
            ps.println(c1 + " -> " + c2);
            for (MethodChange mc : chg.getChangedMethods()) {
                ps.println("-- " + mc.getOldMethod() + " -> " + mc.getNewMethod());
            }
            for (FieldChange fc : chg.getChangedFields()) {
                ps.println("-- " + fc.getOldField() + " -> " + fc.getNewField());
            }
        }
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
