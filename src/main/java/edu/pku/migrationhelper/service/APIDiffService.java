package edu.pku.migrationhelper.service;

import edu.pku.migrationhelper.data.api.APIChange;
import edu.pku.migrationhelper.data.api.ClassSignature;
import edu.pku.migrationhelper.data.lib.LibraryVersionToClass;
import edu.pku.migrationhelper.repository.ClassSignatureRepository;
import edu.pku.migrationhelper.repository.LibraryVersionToClassRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class APIDiffService {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    private ClassSignatureRepository csRepo;

    @Autowired
    private LibraryVersionToClassRepository lv2cRepo;

    /**
     * Given two versions of a same Maven artifact, return all changed APIs
     * This function will first extract all classes in the two versions,
     *   then perform class to class comparison to extract changed fields and changed methods.
     * TODO: Currently it does not detect renaming of classes and fields (method renaming detection is implemented)
     * TODO: Currently it does not detect changes in super classes and interfaces that are NOT in current package
     *
     * @param groupId groupId of the Maven artifact
     * @param artifactId artifactId of the Maven artifact
     * @param fromVersion old version string
     * @param toVersion new version string
     * @return API changes
     */
    public APIChange diff(String groupId, String artifactId, String fromVersion, String toVersion) {
        List<ClassSignature> prev = getLibraryVersionToClass(groupId, artifactId, fromVersion);
        List<ClassSignature> curr = getLibraryVersionToClass(groupId, artifactId, toVersion);
        return new APIChange(groupId, artifactId, fromVersion, toVersion, prev, curr);
    }

    private List<ClassSignature> getLibraryVersionToClass(String groupId, String artifactId, String version) {
        Optional<LibraryVersionToClass> opt = lv2cRepo
                .findByGroupIdAndArtifactIdAndVersion(groupId, artifactId, version);
        if (!opt.isPresent()) {
            throw new IllegalArgumentException(
                    String.format("%s:%s-%s library version to class mapping missing", groupId, artifactId, version));
        }
        LibraryVersionToClass lv2c = opt.get();
        List<ClassSignature> result = new ArrayList<>();
        csRepo.findAllById(lv2c.getClassIds()).forEach(result::add);
        if (lv2c.getClassIds().size() != result.size()) {
            LOG.error("{}:{}-{}, Not all classes can be found in database", groupId, artifactId, version);
        }
        return result;
    }

    /**
     * Return class name to class signature object mapping, given a list of classes
     * For class of the same name, only one version will be retained, as JVM does
     * TODO: resolve all super classes and interfaces from other packages, if they can be found in database
     *
     * @param classes the input classes
     * @return class name to class signature object mapping
     */
    private Map<String, ClassSignature> resolveClasses(Collection<ClassSignature> classes) {
        Map<String, ClassSignature> result = new HashMap<>(classes.size() * 4);
        for (ClassSignature cs : classes) {
            result.put(cs.getClassName(), cs);
        }
        return result;
    }

    private Map<String, ClassSignature> getSHA1ToClass(Collection<ClassSignature> classes) {
        Map<String, ClassSignature> result = new HashMap<>();
        for (ClassSignature cs : classes) {
            result.put(cs.getId(), cs);
        }
        return result;
    }
}
