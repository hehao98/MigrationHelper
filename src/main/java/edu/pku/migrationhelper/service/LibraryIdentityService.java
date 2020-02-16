package edu.pku.migrationhelper.service;

import edu.pku.migrationhelper.data.LibraryGroupArtifact;
import edu.pku.migrationhelper.data.LibrarySignatureMap;
import edu.pku.migrationhelper.data.LibraryVersion;
import edu.pku.migrationhelper.data.MethodSignature;
import edu.pku.migrationhelper.mapper.LibraryGroupArtifactMapper;
import edu.pku.migrationhelper.mapper.LibrarySignatureMapMapper;
import edu.pku.migrationhelper.mapper.LibraryVersionMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClients;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

/**
 * Created by xuyul on 2020/1/2.
 */
@Service
public class LibraryIdentityService {

    Logger LOG = LoggerFactory.getLogger(getClass());

    private HttpClient httpClient = HttpClients.createDefault();

    @Autowired
    private JarAnalysisService jarAnalysisService;

    @Autowired
    private LibraryGroupArtifactMapper libraryGroupArtifactMapper;

    @Autowired
    private LibraryVersionMapper libraryVersionMapper;

    @Autowired
    private LibrarySignatureMapMapper librarySignatureMapMapper;

    @Value("${migration-helper.library-identity.download-path}")
    private String downloadPath;

    @Value("${migration-helper.library-identity.cool-down-ms}")
    private int coolDownMs = 3000;

    private long lastHttpRequestTime = 0;

    private boolean lastHttpRequestFail = false;

    private Timer httpTimer = new Timer("library-identity-http-timer");

    public void parseGroupArtifact(String groupId, String artifactId) throws Exception {
        // create or find library information
        LibraryGroupArtifact groupArtifact = libraryGroupArtifactMapper
                .findByGroupIdAndArtifactId(groupId, artifactId);
        if(groupArtifact == null) {
            groupArtifact = new LibraryGroupArtifact()
                    .setGroupId(groupId)
                    .setArtifactId(artifactId)
                    .setVersionExtracted(false);
            libraryGroupArtifactMapper.insert(Collections.singletonList(groupArtifact));
            groupArtifact = libraryGroupArtifactMapper
                    .findByGroupIdAndArtifactId(groupId, artifactId);
        }
        long groupArtifactId = groupArtifact.getId();
        LOG.info("parse group artifact start id = {}, groupId = {}, artifactId = {}",
                groupArtifactId, groupId, artifactId);
        // extract all library versions if version data not exist
        if(!groupArtifact.isVersionExtracted()) {
            LOG.info("start extract library versions id = {}", groupArtifactId);
            try {
                List<String> versions = extractAllVersionsFromMaven(groupId, artifactId);
                List<LibraryVersion> versionDatas = new ArrayList<>(versions.size());
                versions.forEach(version -> versionDatas.add(new LibraryVersion()
                        .setGroupArtifactId(groupArtifactId)
                        .setVersion(version)
                        .setDownloaded(false)
                        .setParsed(false)));
                libraryVersionMapper.insert(versionDatas);
                groupArtifact.setVersionExtracted(true);
                libraryGroupArtifactMapper.update(groupArtifact);
                LOG.info("extract library versions success id = {}", groupArtifactId);
            } catch (Exception e) {
                LOG.error("extract library versions fail id = " + groupArtifactId, e);
                // go on to download and parse is ok
            }
        }
        // download and parse each version of library
        List<LibraryVersion> versionDatas = libraryVersionMapper.findByGroupArtifactId(groupArtifactId);
        for (LibraryVersion versionData : versionDatas) {
            String version = versionData.getVersion();
            File jarFile = new File(generateJarDownloadPath(groupId, artifactId, version));
            try {
                if (versionData.isParsed()) {
                    continue;
                }
                LOG.info("start download and parse library id = {}", versionData.getId());
//                if (!versionData.isDownloaded() || !jarFile.exists()) {
                if (!jarFile.exists()) {
                    LOG.info("library need download id = {}", versionData.getId());
                    if (jarFile.exists()) {
                        jarFile.delete();
                    }
                    jarFile.getParentFile().mkdirs();
                    jarFile.createNewFile();
                    downloadLibraryFromMaven(groupId, artifactId, version, new FileOutputStream(jarFile));
                    versionData.setDownloaded(true);
                    libraryVersionMapper.update(versionData);
                }
                LOG.info("start parse library id = {}", versionData.getId());
                parseLibraryJar(groupId, artifactId, version, versionData.getId());
                versionData.setParsed(true);
                libraryVersionMapper.update(versionData);
                LOG.info("download and parse library success id = {}", versionData.getId());
            } catch (Exception e) {
                LOG.error("download and parse library fail groupId = {}, artifactId = {}, version = {}",
                        groupId, artifactId, version);
                LOG.error("download and parse library fail", e);
            } finally {
                jarFile.delete();
            }
        }
    }

    public void parseLibraryJar(String groupId, String artifactId, String version, long versionId) throws Exception {
        String jarPath = generateJarDownloadPath(groupId, artifactId, version);
        List<MethodSignature> signatures = jarAnalysisService.analyzeJar(jarPath);
        List<LibrarySignatureMap> mapList = new ArrayList<>(signatures.size());
        for (MethodSignature signature : signatures) {
            mapList.add(new LibrarySignatureMap()
                    .setLibraryVersionId(versionId)
                    .setMethodSignatureId(signature.getId()));
        }
        if(mapList.size() > 0) {
            librarySignatureMapMapper.insert(mapList);
        }
    }

    public void downloadLibraryFromMaven(String groupId, String artifactId, String version, OutputStream output) throws IOException {
        String url = "https://repo1.maven.org/maven2/"
                + groupId.replace(".", "/") + "/"
                + artifactId + "/" + version + "/"
                + artifactId + "-" + version + ".jar";
        HttpResponse response = executeHttpRequest(new HttpGet(url));
        response.getEntity().writeTo(output);
        output.flush();
        output.close();
    }

    public List<String> extractAllVersionsFromMaven(String groupId, String artifactId) throws IOException, DocumentException {
        String url = "https://repo1.maven.org/maven2/"
                + groupId.replace(".", "/") + "/"
                + artifactId + "/maven-metadata.xml";
        HttpResponse response = executeHttpRequest(new HttpGet(url));
        SAXReader reader = new SAXReader();
        Document document = reader.read(response.getEntity().getContent());
        List<Node> versionNodes = document.selectNodes("/metadata/versioning/versions/version");
        List<String> result = new ArrayList<>(versionNodes.size());
        versionNodes.forEach(e -> result.add(e.getStringValue().trim()));
        return result;
    }

    public boolean extractGroupArtifactFromMavenMeta(String metaUrl) throws IOException, DocumentException {
        HttpResponse response = executeHttpRequest(new HttpGet(metaUrl));
        SAXReader reader = new SAXReader();
        Document document = reader.read(response.getEntity().getContent());
        Node groupNode = document.selectSingleNode("/metadata/groupId");
        Node artifactNode = document.selectSingleNode("/metadata/artifactId");
        if(groupNode == null || artifactNode == null) {
            return false;
        }
        String groupId = groupNode.getStringValue().trim();
        String artifactId = artifactNode.getStringValue().trim();
        LibraryGroupArtifact libraryGroupArtifact = libraryGroupArtifactMapper
                .findByGroupIdAndArtifactId(groupId, artifactId);
        if(libraryGroupArtifact == null) {
            libraryGroupArtifact = new LibraryGroupArtifact()
                    .setGroupId(groupId)
                    .setArtifactId(artifactId)
                    .setVersionExtracted(false);
            libraryGroupArtifactMapper.insert(Collections.singletonList(libraryGroupArtifact));
        }
        return true;
    }

    private synchronized HttpResponse executeHttpRequest(HttpRequestBase request) throws IOException {
        long currTime = System.currentTimeMillis();
        if(lastHttpRequestFail && currTime - lastHttpRequestTime < coolDownMs) {
            long sleepMs = coolDownMs - (currTime - lastHttpRequestTime);
            if(sleepMs < 0) sleepMs = 0;
            if(sleepMs > 100000) sleepMs = 100000;
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException ie) {
                // ignore
            }
        }
        lastHttpRequestTime = currTime;
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if(request != null) {
                    request.abort();
                }
            }
        };
        httpTimer.schedule(timerTask, 10000);
        HttpResponse response = httpClient.execute(request);
        if(response.getStatusLine().getStatusCode() != 200) {
            lastHttpRequestFail = true;
            throw new IOException("http status code " + response.getStatusLine().getStatusCode());
        } else {
            lastHttpRequestFail = false;
        }
        return response;
    }

    private String generateJarDownloadPath(String groupId, String artifactId, String version) {
        return downloadPath + "/"
                + groupId.replace(".", "/") + "/"
                + artifactId + "-" + version + ".jar";
    }
}
