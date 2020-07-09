package edu.pku.migrationhelper.service;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xuyul on 2020/2/4.
 */
@Service
public class PomAnalysisService {

    Logger LOG = LoggerFactory.getLogger(getClass());

    public static class LibraryInfo {
        public String groupId;
        public String artifactId;
        public String version;
    }

    public List<LibraryInfo> analyzePom(String pom) throws DocumentException {
        return analyzePom(new ByteArrayInputStream(pom.getBytes()));
    }

    public List<LibraryInfo> analyzePom(InputStream stream) throws DocumentException {
        SAXReader reader = new SAXReader();
        Document document = reader.read(stream);
        List<Node> libraryNodes = document.selectNodes("/*[local-name()='project']/*[local-name()='dependencies']/*[local-name()='dependency']");
        //LOG.debug("dependency count = {}", libraryNodes.size());
        List<LibraryInfo> result = new ArrayList<>(libraryNodes.size());
        for (Node node : libraryNodes) {
            LibraryInfo info = new LibraryInfo();
            Node tmp = node.selectSingleNode("*[local-name()='groupId']");
            if(tmp != null) {
                info.groupId = tmp.getStringValue().trim();
            }
            tmp = node.selectSingleNode("*[local-name()='artifactId']");
            if(tmp != null) {
                info.artifactId = tmp.getStringValue().trim();
            }
            tmp = node.selectSingleNode("*[local-name()='version']");
            if(tmp != null) {
                info.version = tmp.getStringValue().trim();
            }
            result.add(info);
        }
        //if(LOG.isDebugEnabled()) {
        //    LOG.debug("analyzePom result");
        //    for (LibraryInfo info : result) {
        //        LOG.debug("groupId = {}, artifactId = {}, version = {}", info.groupId, info.artifactId, info.version);
        //    }
        //}
        return result;
    }
}
