package edu.pku.migrationhelper.service;

import edu.pku.migrationhelper.data.LibraryMigrationCandidate;
import edu.pku.migrationhelper.data.lib.LibraryGroupArtifact;
import edu.pku.migrationhelper.repository.LibraryGroupArtifactRepository;
import edu.pku.migrationhelper.repository.LibraryMigrationCandidateRepository;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GroupArtifactService {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    private LibraryGroupArtifactRepository libraryGroupArtifactRepository;

    @Autowired
    private LibraryMigrationCandidateRepository libraryMigrationCandidateRepository;

    private Map<Long, LibraryGroupArtifact> groupArtifactCache;

    private Map<String, Long> groupArtifactNameToId;

    // The trie only contains library with recommendation results, as an optimization
    private PatriciaTrie<Long> groupArtifactTrie;
    private PatriciaTrie<List<String>> namePartToNames;

    @PostConstruct
    public synchronized void initializeGroupArtifactCache() {
        LOG.info("Initializing group artifact cache...");
        List<LibraryGroupArtifact> list = libraryGroupArtifactRepository.findAll();
        Map<Long, LibraryGroupArtifact> map = new HashMap<>(list.size() * 2);
        Map<String, Long> name2id = new HashMap<>(list.size() * 2);
        for (LibraryGroupArtifact groupArtifact : list) {
            map.put(groupArtifact.getId(), groupArtifact);
            name2id.put(groupArtifact.getGroupArtifactId(), groupArtifact.getId());
        }
        groupArtifactCache = Collections.unmodifiableMap(map);
        groupArtifactNameToId = Collections.unmodifiableMap(name2id);

        Map<String, Long> nameWithRec2id = new HashMap<>(list.size());
        for (LibraryMigrationCandidate candidate : libraryMigrationCandidateRepository.findAll()) {
            String name = map.get(candidate.fromId).getGroupArtifactId();
            nameWithRec2id.put(name, candidate.fromId);
        }
        groupArtifactTrie = new PatriciaTrie<>(nameWithRec2id);
        namePartToNames = new PatriciaTrie<>();
        for (String name : nameWithRec2id.keySet()) {
            for (String part : name.toLowerCase().split("[:\\-.]")) {
                namePartToNames.computeIfAbsent(part, k -> new ArrayList<>()).add(name);
            }
        }
        LOG.info("{} libraries has recommendation results", nameWithRec2id.size());
    }

    public LibraryGroupArtifact getGroupArtifactById(long id) {
        return groupArtifactCache.get(id);
    }

    public LibraryGroupArtifact getGroupArtifactByName(String name) {
        if (!groupArtifactNameToId.containsKey(name)) {
            return null;
        }
        return groupArtifactCache.get(groupArtifactNameToId.get(name));
    }

    public boolean exist(String name) {
        return groupArtifactNameToId.containsKey(name);
    }

    public long getIdByName(String name) {
        if (groupArtifactNameToId.containsKey(name)) {
            return groupArtifactNameToId.get(name);
        } else {
            LOG.warn("{} does not exist in database", name);
            return -1;
        }
    }

    /**
     * Only prefix libraries with recommendation results are returned
     */
    public List<String> getNamesWithPrefix(String prefix, int topK) {
        return groupArtifactTrie.prefixMap(prefix).keySet().stream().limit(topK).collect(Collectors.toList());
    }

    /**
     * Only similar libraries with recommendation results are returned
     */
    public List<String> getMostSimilarNames(String name, int topK) {
        Map<String, Long> map = new TreeMap<>(Comparator.reverseOrder());
        for (String part : name.toLowerCase().split("[:\\-.]")) {
            if (!namePartToNames.containsKey(part)) continue;
            for (List<String> libs : namePartToNames.prefixMap(part).values()) {
                for (String lib : libs) {
                    if (!map.containsKey(lib)) {
                        map.put(lib, 1L);
                    } else {
                        map.put(lib, map.get(lib) + 1);
                    }
                }
            }
        }
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .sorted(Comparator.comparing(s -> LevenshteinDistance.getDefaultInstance().apply(name, s)))
                .limit(topK)
                .collect(Collectors.toList());
    }

    public PatriciaTrie<Long> getGroupArtifactTrie() {
        return groupArtifactTrie;
    }

    public void setGroupArtifactTrie(PatriciaTrie<Long> groupArtifactTrie) {
        this.groupArtifactTrie = groupArtifactTrie;
    }

    public PatriciaTrie<List<String>> getNamePartToNames() {
        return namePartToNames;
    }

    public void setNamePartToNames(PatriciaTrie<List<String>> namePartToNames) {
        this.namePartToNames = namePartToNames;
    }
}
