package edu.pku.migrationhelper.service;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xuyul on 2020/2/4.
 */
@Service
public class PomAnalysisService {

    public static class LibraryInfo {
        public String groupId;
        public String artifactId;
        public String version;
    }

    public List<LibraryInfo> analyzePom(String pom) throws Exception {
        SAXReader reader = new SAXReader();
        Document document = reader.read(pom);
        List<Node> libraryNodes = document.selectNodes("/project/dependencies/dependency");
        List<LibraryInfo> result = new ArrayList<>(libraryNodes.size());
        for (Node node : libraryNodes) {
            LibraryInfo info = new LibraryInfo();
            Node tmp = node.selectSingleNode("groupId");
            if(tmp != null) {
                info.groupId = tmp.getStringValue().trim();
            }
            tmp = node.selectSingleNode("artifactId");
            if(tmp != null) {
                info.artifactId = tmp.getStringValue().trim();
            }
            tmp = node.selectSingleNode("version");
            if(tmp != null) {
                info.version = tmp.getStringValue().trim();
            }
            result.add(info);
        }
        return result;
    }
}
