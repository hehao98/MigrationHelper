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
import org.springframework.boot.context.properties.ConfigurationProperties;
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
@ConfigurationProperties(prefix = "migration-helper.library-identity")
public class LibraryIdentityService {

    Logger LOG = LoggerFactory.getLogger(getClass());

    private HttpClient httpClient = HttpClients.custom()
            .setMaxConnPerRoute(100)
            .setMaxConnTotal(100)
            .build();

    @Autowired
    private JarAnalysisService jarAnalysisService;

    @Autowired
    private LibraryGroupArtifactMapper libraryGroupArtifactMapper;

    @Autowired
    private LibraryVersionMapper libraryVersionMapper;

    @Autowired
    private LibrarySignatureMapMapper librarySignatureMapMapper;

//    @Value("${migration-helper.library-identity.maven-url-base}")
    private List<String> mavenUrlBase;

//    @Value("${migration-helper.library-identity.download-path}")
    private String downloadPath;

    public List<String> getMavenUrlBase() {
        return mavenUrlBase;
    }

    public LibraryIdentityService setMavenUrlBase(List<String> mavenUrlBase) {
        this.mavenUrlBase = mavenUrlBase;
        return this;
    }

    public String getDownloadPath() {
        return downloadPath;
    }

    public LibraryIdentityService setDownloadPath(String downloadPath) {
        this.downloadPath = downloadPath;
        return this;
    }

    private Timer httpTimer = new Timer("library-identity-http-timer");

    public void parseGroupArtifact(String groupId, String artifactId, boolean extractVersionOnly) throws Exception {
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

        if(extractVersionOnly) {
            return;
        }

        // download and parse each version of library
        Map<String, MethodSignature> signatureCache = new HashMap<>();
        List<LibraryVersion> versionDatas = libraryVersionMapper.findByGroupArtifactId(groupArtifactId);
        for (LibraryVersion versionData : versionDatas) {
            String version = versionData.getVersion();
            File jarFile = new File(generateJarDownloadPath(groupId, artifactId, version));
            try {
                if (versionData.isParsed() || versionData.isParseError()) {
                    continue;
                }
                LOG.info("start download and parse library id = {}", versionData.getId());
                if (jarFile.exists()) { // force download
                    jarFile.delete();
                }
//                if (!versionData.isDownloaded() || !jarFile.exists()) {
                if (!jarFile.exists()) {
                    LOG.info("library need download id = {}", versionData.getId());
                    jarFile.getParentFile().mkdirs();
                    jarFile.createNewFile();
                    downloadLibraryFromMaven(groupId, artifactId, version, new FileOutputStream(jarFile));
                    versionData.setDownloaded(true);
                    libraryVersionMapper.update(versionData);
                }
                LOG.info("start parse library id = {}", versionData.getId());
                parseLibraryJar(groupId, artifactId, version, versionData.getId(), signatureCache);
                versionData.setParsed(true);
                versionData.setParseError(false);
                libraryVersionMapper.update(versionData);
                LOG.info("download and parse library success id = {}", versionData.getId());
            } catch (Exception e) {
                LOG.error("download and parse library fail groupId = {}, artifactId = {}, version = {}",
                        groupId, artifactId, version);
                LOG.error("download and parse library fail", e);
                versionData.setParsed(false);
                versionData.setParseError(true);
                libraryVersionMapper.update(versionData);
            } finally {
                if (jarFile.exists()) {
                    jarFile.delete();
                }
            }
        }
    }

    public void parseLibraryJar(String groupId, String artifactId, String version, long versionId, Map<String, MethodSignature> signatureCache) throws Exception {
        String jarPath = generateJarDownloadPath(groupId, artifactId, version);
        List<MethodSignature> signatures = jarAnalysisService.analyzeJar(jarPath, signatureCache);
        int insertLimit = 1000;
        List<LibrarySignatureMap> mapList = new ArrayList<>(insertLimit);
        for (MethodSignature signature : signatures) {
            mapList.add(new LibrarySignatureMap()
                    .setLibraryVersionId(versionId)
                    .setMethodSignatureId(signature.getId()));
            if(mapList.size() >= insertLimit) {
                librarySignatureMapMapper.insert(mapList);
                mapList.clear();
            }
        }
        if(mapList.size() > 0) {
            librarySignatureMapMapper.insert(mapList);
        }
    }

    public void downloadLibraryFromMaven(String groupId, String artifactId, String version, OutputStream output) throws IOException {
        Iterator<String> baseIt = mavenUrlBase.iterator();
        while(baseIt.hasNext()) {
            String url = baseIt.next()
                    + groupId.replace(".", "/") + "/"
                    + artifactId + "/" + version + "/"
                    + artifactId + "-" + version + ".jar";
            HttpGet request = new HttpGet(url);
            try {
                HttpResponse response = executeHttpRequest(request);
                response.getEntity().writeTo(output);
                output.flush();
                output.close();
                return;
            } catch (Exception e) {
                if(!baseIt.hasNext()) throw e;
                LOG.info("http fail, try next, url = {}", url);
                continue;
            } finally {
                request.releaseConnection();
            }
        }
        throw new RuntimeException("no maven repository available");
    }

    public List<String> extractAllVersionsFromMaven(String groupId, String artifactId) throws IOException, DocumentException {
        Iterator<String> baseIt = mavenUrlBase.iterator();
        while(baseIt.hasNext()) {
            String url = baseIt.next()
                    + groupId.replace(".", "/") + "/"
                    + artifactId + "/maven-metadata.xml";
            HttpGet request = new HttpGet(url);
            try {
                HttpResponse response = executeHttpRequest(new HttpGet(url));
                SAXReader reader = new SAXReader();
                Document document = reader.read(response.getEntity().getContent());
                List<Node> versionNodes = document.selectNodes("/metadata/versioning/versions/version");
                List<String> result = new ArrayList<>(versionNodes.size());
                versionNodes.forEach(e -> result.add(e.getStringValue().trim()));
                return result;
            } catch (Exception e) {
                if(!baseIt.hasNext()) throw e;
                LOG.info("http fail, try next, url = {}", url);
                continue;
            } finally {
                request.releaseConnection();
            }
        }
        throw new RuntimeException("no maven repository available");
    }

    public boolean extractGroupArtifactFromMavenMeta(String metaUrl) throws IOException, DocumentException {
        HttpGet request = new HttpGet(metaUrl);
        try {
            HttpResponse response = executeHttpRequest(new HttpGet(metaUrl));
            SAXReader reader = new SAXReader();
            Document document = reader.read(response.getEntity().getContent());
            Node groupNode = document.selectSingleNode("/metadata/groupId");
            Node artifactNode = document.selectSingleNode("/metadata/artifactId");
            if (groupNode == null || artifactNode == null) {
                return false;
            }
            String groupId = groupNode.getStringValue().trim();
            String artifactId = artifactNode.getStringValue().trim();
            LibraryGroupArtifact libraryGroupArtifact = libraryGroupArtifactMapper
                    .findByGroupIdAndArtifactId(groupId, artifactId);
            if (libraryGroupArtifact == null) {
                libraryGroupArtifact = new LibraryGroupArtifact()
                        .setGroupId(groupId)
                        .setArtifactId(artifactId)
                        .setVersionExtracted(false);
                libraryGroupArtifactMapper.insert(Collections.singletonList(libraryGroupArtifact));
            }
            return true;
        } finally {
            request.releaseConnection();
        }
    }

    private HttpResponse executeHttpRequest(HttpRequestBase request) throws IOException {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if(request != null) {
                    request.abort();
                }
            }
        };
        httpTimer.schedule(timerTask, 30000);
        HttpResponse response = httpClient.execute(request);
        if(response.getStatusLine().getStatusCode() != 200) {
            throw new IOException("http status code " + response.getStatusLine().getStatusCode());
        }
        return response;
    }

    private String generateJarDownloadPath(String groupId, String artifactId, String version) {
        return downloadPath + "/"
                + groupId.replace(".", "/") + "/"
                + artifactId + "-" + version + ".jar";
    }
}
