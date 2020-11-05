package service;

import edu.pku.migrationhelper.service.GroupArtifactService;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GroupArtifactServiceTest {
    @Test
    public void testTrie() {
        GroupArtifactService service = new GroupArtifactService();
        PatriciaTrie<Long> groupArtifactTrie = new PatriciaTrie<>();
        groupArtifactTrie.put("org.json:json", 0L);
        groupArtifactTrie.put("org.apache:junit", 1L);
        groupArtifactTrie.put("com.google.code.gson:gson", 1L);
        groupArtifactTrie.put("com.xxx.yyy.zzz.www.gson:gson", 1L);
        groupArtifactTrie.put("com.google.collections:guava", 1L);
        PatriciaTrie<List<String>> namePartToNames = new PatriciaTrie<>();
        for (String name: groupArtifactTrie.keySet()) {
            for (String part : name.toLowerCase().split("[:\\-.]")) {
                namePartToNames.computeIfAbsent(part, k -> new ArrayList<>()).add(name);
            }
        }
        service.setGroupArtifactTrie(groupArtifactTrie);
        service.setNamePartToNames(namePartToNames);

        List<String> l = service.getNamesWithPrefix("org", 1);
        assertEquals(l.size(), 1);
        assertEquals(l.get(0), "org.apache:junit");
        l = service.getNamesWithPrefix("org", 10);
        assertEquals(l.size(), 2);
        assertEquals(l.get(1), "org.json:json");
        System.out.println(l);
        l = service.getNamesWithPrefix("com.google", 10);
        assertEquals(l.size(), 2);
        System.out.println(l);
        l = service.getNamesWithPrefix("com.json", 10);
        assertEquals(l.size(), 0);

        l = service.getMostSimilarNames("org.google.collectins:guava", 10);
        assertEquals(l.get(0), "com.google.collections:guava");
        System.out.println(l);
        l = service.getMostSimilarNames("com.googl.:hson", 10);
        assertEquals(l.get(0), "com.google.code.gson:gson");
        System.out.println(l);
        System.out.println(service.getMostSimilarNames("com.googl.gson:hson", 10));
    }
}
