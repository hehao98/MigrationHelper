package edu.pku.migrationhelper.service;

import edu.pku.migrationhelper.data.lib.LibraryGroupArtifact;
import edu.pku.migrationhelper.repository.LibraryGroupArtifactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GroupArtifactService {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    private LibraryGroupArtifactRepository libraryGroupArtifactRepository;

    private Map<Long, LibraryGroupArtifact> groupArtifactCache;

    private Map<String, Long> groupArtifactNameToId;

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
}
