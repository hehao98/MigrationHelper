package edu.pku.migrationhelper.service;

import edu.pku.migrationhelper.data.api.*;
import edu.pku.migrationhelper.data.lib.*;
import edu.pku.migrationhelper.mapper.*;
import edu.pku.migrationhelper.repository.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.HttpClients;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

@Service
@ConfigurationProperties(prefix = "migration-helper.library-identity")
public class LibraryIdentityService {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private final HttpClient httpClient = HttpClients.custom()
            .setMaxConnPerRoute(1000)
            .setMaxConnTotal(1000)
            .build();

    @Autowired
    private MongoDbUtilService mongoUtilService;

    @Autowired
    private JarAnalysisService jarAnalysisService;

    @Autowired
    private MavenService mavenService;

    @Autowired
    private LibraryGroupArtifactRepository libraryGroupArtifactRepository;

    @Autowired
    private LibraryVersionRepository libraryVersionRepository;

    @Autowired
    private ClassSignatureRepository classSignatureRepository;

    @Autowired
    private LibraryVersionToClassRepository libraryVersionToClassRepository;

    @Autowired
    private ClassToLibraryVersionRepository classToLibraryVersionRepository;

    @Autowired
    private LibraryVersionToDependenciesRepository libraryVersionToDependenciesRepository;

    @Value("${migration-helper.library-identity.maven-url-base}")
    private final String mavenUrlBase = "https://repo1.maven.org/maven2/";

    @Value("${migration-helper.library-identity.download-path}")
    private String downloadPath;

    public void extractVersions(String groupId, String artifactId) {
        // Create or find library information
        LibraryGroupArtifact groupArtifact;
        Optional<LibraryGroupArtifact> opt = libraryGroupArtifactRepository.findByGroupIdAndArtifactId(groupId, artifactId);
        if (!opt.isPresent()) {
            groupArtifact = new LibraryGroupArtifact()
                    .setId(mongoUtilService.getNextIdForCollection("libraryGroupArtifact"))
                    .setGroupId(groupId)
                    .setArtifactId(artifactId)
                    .setVersionExtracted(false)
                    .setParsed(false)
                    .setParseError(false);
            groupArtifact = libraryGroupArtifactRepository.save(groupArtifact);
        } else {
            groupArtifact = opt.get();
        }

        if (groupArtifact.isVersionExtracted()) {
            LOG.info("Skip {} because its version is extracted", groupArtifact);
            return;
        }

        // Extract all library versions if version data not exist
        long groupArtifactId = groupArtifact.getId();
        LOG.info("start extract library versions id = {}", groupArtifactId);
        try {
            List<String> versions = extractAllVersionsFromMaven(groupId, artifactId);
            List<LibraryVersion> versionData = new ArrayList<>(versions.size());
            versions.forEach(version -> versionData.add(new LibraryVersion()
                    .setId(mongoUtilService.getNextIdForCollection("libraryVersion"))
                    .setGroupArtifactId(groupArtifactId)
                    .setVersion(version)
                    .setDownloaded(false)
                    .setParsed(false)
                    .setParseError(false)));
            libraryVersionRepository.saveAll(versionData);
            groupArtifact.setVersionExtracted(true);
            libraryGroupArtifactRepository.save(groupArtifact);
            LOG.info("extract library versions success id = {}", groupArtifactId);
        } catch (Exception e) {
            LOG.error("extract library versions fail id = {}, {}", groupArtifactId, e);
        }
    }

    public void extractDependencies(String groupId, String artifactId) {
        LibraryGroupArtifact groupArtifact;
        Optional<LibraryGroupArtifact> opt = libraryGroupArtifactRepository.findByGroupIdAndArtifactId(groupId, artifactId);
        if (!opt.isPresent() || !opt.get().isVersionExtracted()) {
            extractVersions(groupId, artifactId);
        }
        groupArtifact = libraryGroupArtifactRepository.findByGroupIdAndArtifactId(groupId, artifactId).get();

        List<LibraryVersion> versions = libraryVersionRepository.findByGroupArtifactId(groupArtifact.getId());
        for (LibraryVersion v : versions) {
            LibraryVersionToDependency lv2d = new LibraryVersionToDependency()
                    .setId(v.getId())
                    .setGroupId(groupId)
                    .setArtifactId(artifactId)
                    .setVersion(v.getVersion())
                    .setHasError(false);

            try {
                List<MavenService.LibraryInfo> libs = extractDependenciesFromMaven(groupId, artifactId, v.getVersion());
                lv2d.setDependencies(libs);
            } catch (XmlPullParserException|IOException e) {
                LOG.error("Error while extracting dependencies for {}:{}-{}", groupId, artifactId, v.getVersion());
                lv2d.setHasError(true);
            }

            libraryVersionToDependenciesRepository.save(lv2d);
        }
    }

    public void parseGroupArtifact(String groupId, String artifactId) {
        Optional<LibraryGroupArtifact> opt = libraryGroupArtifactRepository.findByGroupIdAndArtifactId(groupId, artifactId);
        LibraryGroupArtifact groupArtifact;

        if (!opt.isPresent())
            extractVersions(groupId, artifactId);
        groupArtifact = libraryGroupArtifactRepository.findByGroupIdAndArtifactId(groupId, artifactId).get();
        if (groupArtifact.isVersionExtracted())
            extractVersions(groupId, artifactId);
        groupArtifact = libraryGroupArtifactRepository.findByGroupIdAndArtifactId(groupId, artifactId).get();

        if (groupArtifact.isVersionExtracted() && groupArtifact.isParsed() && !groupArtifact.isParseError())  {
            LOG.info("skip {} because it is parsed and does not contain error", groupArtifact);
            return;
        }

        long groupArtifactId = groupArtifact.getId();
        LOG.info("parse group artifact start id = {}, groupId = {}, artifactId = {}",
                groupArtifactId, groupId, artifactId);

        boolean containError = false;
        List<LibraryVersion> versions = libraryVersionRepository.findByGroupArtifactId(groupArtifactId);
        Map<String, List<Long>> classToVersions = new HashMap<>();
        Map<Long, List<String>> versionToClasses = new HashMap<>();
        for (LibraryVersion versionData : versions) {
            String version = versionData.getVersion();
            File jarFile = new File(generateJarDownloadPath(groupId, artifactId, version));
            List<ClassSignature> classes;
            Set<String> classNamesInJar = new HashSet<>();

            try {
                if (!jarFile.exists()) {
                    List<String> availableFiles = extractAvailableFilesFromMaven(groupId, artifactId, version);
                    if (!availableFiles.contains(String.format("%s-%s.jar", artifactId, version))) {
                        LOG.info("{}:{}-{} do not have corresponding JAR file, skipping...", groupId, artifactId, version);
                        versionData.setParsed(false).setParseError(false).setDownloaded(false);
                        containError = true;
                        versionData = libraryVersionRepository.save(versionData);
                        continue;
                    }
                    LOG.info("library need download {}-{}", groupArtifact.getGroupArtifactId(), versionData.getVersion());
                    if (!(jarFile.getParentFile().exists() || jarFile.getParentFile().mkdirs()))
                        throw new IOException("Fail in making directory for jarFile " + jarFile);
                    if (!jarFile.createNewFile())
                        throw new IOException("Fail in creating new jarFile " + jarFile);
                    downloadLibraryFromMaven(groupId, artifactId, version, new FileOutputStream(jarFile));
                }
                versionData.setDownloaded(true);
                versionData = libraryVersionRepository.save(versionData);
                double jarSize = jarFile.length() / (1024.0 * 1024.0);
                LOG.info("start parse library {}-{}, size = {} MB",
                        groupArtifact.getGroupArtifactId(), versionData.getVersion(), jarSize);
                classes = jarAnalysisService.analyzeJar(jarFile.getPath(), true, classNamesInJar);
            } catch (IOException ex) {
                LOG.error("IOException when downloading/parsing JAR {}, {}", jarFile, ex);
                containError = true;
                versionData.setDownloaded(false).setParsed(false).setParseError(true);
                libraryVersionRepository.save(versionData);
                continue;
            }

            versionToClasses.computeIfAbsent(versionData.getId(), l -> new LinkedList<>());
            for (ClassSignature cs : classes) {
                if (!classSignatureRepository.findById(cs.getId()).isPresent())
                    classSignatureRepository.save(cs);
                if (classNamesInJar.contains(cs.getClassName())) {
                    classToVersions.computeIfAbsent(cs.getId(), s -> new LinkedList<>());
                    classToVersions.get(cs.getId()).add(versionData.getId());
                    versionToClasses.get(versionData.getId()).add(cs.getId());
                }
            }

            versionData.setParsed(true).setParseError(false);
            libraryVersionRepository.save(versionData);
        }

        for (String classId : classToVersions.keySet()) {
            ClassToLibraryVersion c2lv = new ClassToLibraryVersion();
            Optional<ClassToLibraryVersion> c2lvo = classToLibraryVersionRepository.findById(classId);
            if (c2lvo.isPresent()) {
                c2lv = c2lvo.get();
                c2lv.addVersionIds(classToVersions.get(classId));
            } else {
                c2lv.setClassId(classId).setVersionIds(classToVersions.get(classId));
            }
            classToLibraryVersionRepository.save(c2lv);
        }
        for (long versionId : versionToClasses.keySet()) {
            LibraryVersionToClass lv2c = new LibraryVersionToClass();
            Optional<LibraryVersionToClass> lv2co = libraryVersionToClassRepository.findById(versionId);
            if (lv2co.isPresent()) {
                lv2c = lv2co.get();
                lv2c.addClassIds(versionToClasses.get(versionId));
            } else {
                lv2c.setId(versionId).setClassIds(versionToClasses.get(versionId));
            }
            String version = libraryVersionRepository.findById(versionId).get().getVersion();
            lv2c.setGroupId(groupId).setArtifactId(artifactId).setVersion(version);
            libraryVersionToClassRepository.save(lv2c);
        }

        groupArtifact.setParsed(true).setParseError(containError);
        libraryGroupArtifactRepository.save(groupArtifact);
        LOG.info("Download and parse {}:{} success!", groupId, artifactId);
    }

    public void downloadLibraryFromMaven(String groupId, String artifactId, String version, OutputStream output) throws IOException {
        String url = mavenUrlBase
                + groupId.replace(".", "/") + "/"
                + artifactId + "/" + version + "/"
                + artifactId + "-" + version + ".jar";

        HttpGet request = new HttpGet(url);
        LOG.info("Downloading JAR from {}", url);
        try {
            HttpResponse response = executeHttpRequest(request);
            response.getEntity().writeTo(output);
            output.flush();
        } catch (Exception e) {
            LOG.error("Download library fail, url = {}", url);
            throw e;
        } finally {
            request.releaseConnection();
            output.close();
        }
    }

    public List<String> extractAllVersionsFromMaven(String groupId, String artifactId) throws IOException, DocumentException {
        String url = mavenUrlBase
                + groupId.replace(".", "/") + "/"
                + artifactId + "/maven-metadata.xml";
        HttpGet request = new HttpGet(url);
        try {
            HttpResponse response = executeHttpRequest(request);
            SAXReader reader = new SAXReader();
            Document document = reader.read(response.getEntity().getContent());
            List<Node> versionNodes = document.selectNodes("/metadata/versioning/versions/version");
            List<String> result = new ArrayList<>(versionNodes.size());
            versionNodes.forEach(e -> result.add(e.getStringValue().trim()));
            return result;
        } catch (Exception e) {
            LOG.error("Download version information fail, url = {}", url);
            throw e;
        } finally {
            request.releaseConnection();
        }
    }

    public List<MavenService.LibraryInfo> extractDependenciesFromMaven(
            String groupId, String artifactId, String version) throws IOException, XmlPullParserException {
        String url = mavenUrlBase
                + groupId.replace(".", "/") + "/"
                + artifactId + "/" + version + "/"
                + artifactId + "-" + version + ".pom";
        HttpGet request = new HttpGet(url);
        List<MavenService.LibraryInfo> infos;
        try {
            HttpResponse response = executeHttpRequest(request);
            infos = mavenService.analyzePom(response.getEntity().getContent());
        } catch (Exception e) {
            LOG.error("Extract dependencies fail, url = {}", url);
            throw e;
        } finally {
            request.releaseConnection();
        }
        return infos;
    }

    public boolean extractGroupArtifactFromMavenMeta(String metaUrl) throws IOException, DocumentException {
        HttpGet request = new HttpGet(metaUrl);
        try {
            HttpResponse response = executeHttpRequest(request);
            SAXReader reader = new SAXReader();
            Document document = reader.read(response.getEntity().getContent());
            Node groupNode = document.selectSingleNode("/metadata/groupId");
            Node artifactNode = document.selectSingleNode("/metadata/artifactId");
            if (groupNode == null || artifactNode == null) {
                return false;
            }
            String groupId = groupNode.getStringValue().trim();
            String artifactId = artifactNode.getStringValue().trim();
            Optional<LibraryGroupArtifact> opt = libraryGroupArtifactRepository
                    .findByGroupIdAndArtifactId(groupId, artifactId);
            LibraryGroupArtifact libraryGroupArtifact;
            if (!opt.isPresent()) {
                libraryGroupArtifact = new LibraryGroupArtifact()
                        .setGroupId(groupId)
                        .setArtifactId(artifactId)
                        .setVersionExtracted(false);
                libraryGroupArtifactRepository.save(libraryGroupArtifact);
            }
            return true;
        } finally {
            request.releaseConnection();
        }
    }

    public List<String> extractAvailableFilesFromMaven(String groupId, String artifactId, String version) throws IOException {
        String url = mavenUrlBase
                + groupId.replace(".", "/") + "/"
                + artifactId + "/" + version + "/";
        List<String> result = new ArrayList<>();
        HttpGet request = new HttpGet(url);
        try {
            org.jsoup.nodes.Document doc = Jsoup.connect(url).get();
            Elements files = doc.select("a");
            for (org.jsoup.nodes.Element file : files) {
                result.add(file.attr("href"));
            }
        } catch (HttpStatusException e) {
            if (e.getStatusCode() == 404) {
                // In very rare cases the version string does not match the folder on Maven Central,
                //   such as 8.0.7 of mysql-connector-java, which will cause status code 404
                // In this case, we just consider the folder as empty and return empty list
                LOG.warn("Url {} does not exist which may be a bug in Maven Central", url);
                return result;
            } else {
                LOG.error("Http status code {} while extracting available files from maven, url = {}", e.getStatusCode(), url);
                throw e;
            }
        } catch (Exception e) {
            LOG.error("Error while extracting available files from maven, url = {}", url);
            throw e;
        } finally {
            request.releaseConnection();
        }
        return result;
    }

    private HttpResponse executeHttpRequest(HttpRequestBase request) throws IOException {
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

    @Autowired
    @Deprecated
    private LibraryGroupArtifactMapper libraryGroupArtifactMapper;

    @Autowired
    @Deprecated
    private LibraryVersionMapper libraryVersionMapper;

    @Deprecated
    @Autowired
    private MethodSignatureMapper methodSignatureMapper;

    @Deprecated
    @Autowired
    private LibrarySignatureToVersionMapper librarySignatureToVersionMapper;

    @Deprecated
    @Autowired
    private LibraryVersionToSignatureMapper libraryVersionToSignatureMapper;

    @Deprecated
    public void parseGroupArtifact(String groupId, String artifactId, boolean extractVersionOnly) {
        // create or find library information
        LibraryGroupArtifact groupArtifact = libraryGroupArtifactMapper
                .findByGroupIdAndArtifactId(groupId, artifactId);
        if(groupArtifact == null) {
            groupArtifact = new LibraryGroupArtifact()
                    .setGroupId(groupId)
                    .setArtifactId(artifactId)
                    .setVersionExtracted(false)
                    .setParsed(false)
                    .setParseError(false);
            libraryGroupArtifactMapper.insert(Collections.singletonList(groupArtifact));
            groupArtifact = libraryGroupArtifactMapper
                    .findByGroupIdAndArtifactId(groupId, artifactId);
        }
        if(groupArtifact.isParsed() && groupArtifact.isVersionExtracted() && !groupArtifact.isParseError())  {
            LOG.info("skip {} because it is parsed and does not contain error", groupArtifact);
            return;
        }
        long groupArtifactId = groupArtifact.getId();
        LOG.info("parse group artifact start id = {}, groupId = {}, artifactId = {}",
                groupArtifactId, groupId, artifactId);

        // extract all library versions if version data not exist
        if(!groupArtifact.isVersionExtracted()) {
            LOG.info("start extract library versions id = {}", groupArtifactId);
            try {
                List<String> versions = extractAllVersionsFromMaven(groupId, artifactId);
                List<LibraryVersion> versionData = new ArrayList<>(versions.size());
                versions.forEach(version -> versionData.add(new LibraryVersion()
                        .setGroupArtifactId(groupArtifactId)
                        .setVersion(version)
                        .setDownloaded(false)
                        .setParsed(false)
                        .setParseError(false)));
                libraryVersionMapper.insert(versionData);
                groupArtifact.setVersionExtracted(true);
                libraryGroupArtifactMapper.update(groupArtifact);
                LOG.info("extract library versions success id = {}", groupArtifactId);
            } catch (Exception e) {
                LOG.error("extract library versions fail id = {}, {}", groupArtifactId, e);
                return;
            }
        }

        if(extractVersionOnly) {
            return;
        }

        // download and parse each version of library
        Map<String, MethodSignatureOld> signatureCache = new HashMap<>();
        Map<Long, Set<Long>> signature2Version = new HashMap<>();
        List<LibraryVersion> versionDataList = libraryVersionMapper.findByGroupArtifactId(groupArtifactId);

        boolean analyzeFirstOnly = false; // 如果需要在某种条件下只分析最新版本的库，跳过其他版本，则将这个值设置为true
        int versionCount = 1;

        boolean containError = false;
        for (LibraryVersion versionData : versionDataList) {
            if(analyzeFirstOnly && versionCount > 1) {
                break;
            }
            versionCount++;

            long versionId = versionData.getId();
            String version = versionData.getVersion();
            File jarFile = new File(generateJarDownloadPath(groupId, artifactId, version));
            try {
                if (versionData.isParsed() || versionData.isParseError()) {
                    LibraryVersionToSignature v2s = getVersionToSignature(versionId);
                    if(v2s != null && v2s.getSignatureIdList() != null) {
                        v2s.getSignatureIdList().forEach(sid -> signature2Version
                                .computeIfAbsent(sid, k -> new HashSet<>())
                                .add(versionId));
                    }
                    if (!versionData.isParseError()) {
                        LOG.info("Skipping {}:{}-{} because it is parsed and does not contain error",
                                groupId, artifactId, versionData.getVersion());
                        continue;
                    }
                }
                LOG.info("start download and parse library id = {}", versionData.getId());
                if (jarFile.exists()) { // force download
                    jarFile.delete();
                }
//                if (!versionData.isDownloaded() || !jarFile.exists()) {
                if (!jarFile.exists()) {
                    List<String> availableFiles = extractAvailableFilesFromMaven(groupId, artifactId, version);
                    if (!availableFiles.contains(String.format("%s-%s.jar", artifactId, version))) {
                        LOG.info("{}:{}-{} do not have corresponding JAR file, skipping...", groupId, artifactId, version);
                        versionData.setParsed(false).setParseError(false).setDownloaded(false);
                        libraryVersionMapper.update(versionData);
                        continue;
                    }
                    LOG.info("library need download id = {}", versionData.getId());
                    jarFile.getParentFile().mkdirs();
                    jarFile.createNewFile();
                    downloadLibraryFromMaven(groupId, artifactId, version, new FileOutputStream(jarFile));
                    versionData.setDownloaded(true);
                    libraryVersionMapper.update(versionData);
                }

                double jarSize = jarFile.length() / (1024.0 * 1024.0);
                LOG.info("start parse library id = {}, size = {} MB", versionData.getId(), jarSize);
                // 如果某个版本超过50MB 则只分析最新版本
//                if(jarSize > 50) {
//                    analyzeFirstOnly = true;
//                }
                List<MethodSignatureOld> signatureList = parseLibraryJar(groupId, artifactId, version, signatureCache);
                Set<Long> signatureIds = new HashSet<>();
                signatureList.forEach(e -> signatureIds.add(e.getId()));
                signatureIds.forEach(sid -> signature2Version
                        .computeIfAbsent(sid, k -> new HashSet<>())
                        .add(versionId));

                LibraryVersionToSignature v2s = getVersionToSignature(versionId);
                if(v2s == null) {
                    v2s = new LibraryVersionToSignature()
                            .setVersionId(versionId);
                }
                if(v2s.getSignatureIdList() != null) {
                    signatureIds.addAll(v2s.getSignatureIdList());
                }
                v2s.setSignatureIdList(new ArrayList<>(signatureIds));
                saveVersionToSignature(v2s);

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
                versionCount--;
                containError = true;
            } finally {
                if (jarFile.exists()) {
                    jarFile.delete();
                }
            }
        }

        // save signature2Version and groupArtifact
        try {
            signature2Version.forEach((signatureId, versionIds) -> {
                LibrarySignatureToVersion s2v = getSignatureToVersion(signatureId);
                if(s2v == null) {
                    s2v = new LibrarySignatureToVersion()
                            .setSignatureId(signatureId);
                }
                Set<Long> gaIds = new HashSet<>();
                gaIds.add(groupArtifactId);
                if(s2v.getGroupArtifactIdList() != null) {
                    gaIds.addAll(s2v.getGroupArtifactIdList());
                }
                s2v.setGroupArtifactIdList(new ArrayList<>(gaIds));
                if(s2v.getVersionIdList() != null) {
                    versionIds.addAll(s2v.getVersionIdList());
                }
                s2v.setVersionIdList(new ArrayList<>(versionIds));
                saveSignatureToVersion(s2v);
            });
            groupArtifact.setParsed(true)
                    .setParseError(containError || analyzeFirstOnly);
            libraryGroupArtifactMapper.update(groupArtifact);
        } catch (Exception e) {
            LOG.error("save groupArtifact fail groupId = {}, artifactId = {}, {}",
                    groupId, artifactId, e);
            groupArtifact.setParsed(true)
                    .setParseError(true);
            libraryGroupArtifactMapper.update(groupArtifact);
        }
    }

    @Deprecated
    public List<MethodSignatureOld> parseLibraryJar(String groupId, String artifactId, String version, Map<String, MethodSignatureOld> signatureCache) throws Exception {
        String jarPath = generateJarDownloadPath(groupId, artifactId, version);
        List<MethodSignatureOld> signatures = new LinkedList<>();
        jarAnalysisService.analyzeJar(jarPath, signatures);
        for (MethodSignatureOld signature : signatures) {
            saveMethodSignature(signature, signatureCache);
        }
        return signatures;
    }

    @Deprecated
    public Map<Long, List<Long>> getSignatureToGroupArtifact(Collection<Long> signatureIds) {
        Map<Integer, Set<Long>> sliceMap = new HashMap<>();
        for (Long signatureId : signatureIds) {
            int slice = MapperUtilService.getMethodSignatureSliceKey(signatureId);
            sliceMap.computeIfAbsent(slice, k -> new HashSet<>()).add(signatureId);
        }
        Map<Long, List<Long>> result = new HashMap<>();
        sliceMap.forEach((slice, ids) -> {
            List<LibrarySignatureToVersion> s2vList = librarySignatureToVersionMapper.findGroupArtifactByIdIn(slice, ids);
            for (LibrarySignatureToVersion s2v : s2vList) {
                result.put(s2v.getSignatureId(), s2v.getGroupArtifactIdList());
            }
        });
        for (Long signatureId : signatureIds) {
            if(!result.containsKey(signatureId)) {
                result.put(signatureId, new ArrayList<>(0));
            }
        }
        return result;
    }

    @Deprecated
    public LibrarySignatureToVersion getSignatureToVersion(long signatureId) {
        int slice = MapperUtilService.getMethodSignatureSliceKey(signatureId);
        return librarySignatureToVersionMapper.findById(slice, signatureId);
    }

    @Deprecated
    public void saveSignatureToVersion(LibrarySignatureToVersion s2v) {
        int slice = MapperUtilService.getMethodSignatureSliceKey(s2v.getSignatureId());
        librarySignatureToVersionMapper.insertOne(slice, s2v);
    }

    @Deprecated
    public LibraryVersionToSignature getVersionToSignature(long versionId) {
        return libraryVersionToSignatureMapper.findById(versionId);
    }

    @Deprecated
    public void saveVersionToSignature(LibraryVersionToSignature v2s) {
        libraryVersionToSignatureMapper.insertOne(v2s);
    }

    @Deprecated
    public List<Long>[] getVersionIdsAndGroupArtifactIdsBySignatureIds(Collection<Long> signatureIds) {
        List<LibrarySignatureToVersion> s2vList = new ArrayList<>(signatureIds.size());
        for (long signatureId : signatureIds) {
            LibrarySignatureToVersion s2v = getSignatureToVersion(signatureId);
            if(s2v != null) s2vList.add(s2v);
        }
        Set<Long> versionIds = new HashSet<>();
        Set<Long> gaIds = new HashSet<>();
        for (LibrarySignatureToVersion s2v : s2vList) {
            if(s2v.getVersionIdList() != null) {
                versionIds.addAll(s2v.getVersionIdList());
            }
            if(s2v.getGroupArtifactIdList() != null) {
                gaIds.addAll(s2v.getGroupArtifactIdList());
            }
        }
        return new List[]{new ArrayList<>(versionIds), new ArrayList<>(gaIds)};
    }

    @Deprecated
    public List<MethodSignatureOld> getMethodSignatureList(String packageName, String className, String methodName) {
        int sliceKey = MapperUtilService.getMethodSignatureSliceKey(packageName, className);
        return methodSignatureMapper.findList(sliceKey, packageName, className, methodName);
    }

    @Deprecated
    public MethodSignatureOld getMethodSignature(MethodSignatureOld ms, Map<String, MethodSignatureOld> signatureCache) {
        String cacheKey = null;
        if(signatureCache != null) {
            cacheKey = MapperUtilService.getMethodSignatureCacheKey(ms);
            MethodSignatureOld cacheValue = signatureCache.get(cacheKey);
            if (cacheValue != null) {
                ms.setId(cacheValue.getId());
                return ms;
            }
        }

        try {
            int sliceKey = MapperUtilService.getMethodSignatureSliceKey(ms.getPackageName(), ms.getClassName());
            Long id = methodSignatureMapper.findId(sliceKey, ms.getPackageName(), ms.getClassName(), ms.getMethodName(), ms.getParamList());
            if(id == null) return null;
            ms.setId(id);

            if (signatureCache != null) {
                MethodSignatureOld cacheValue = new MethodSignatureOld()
                        .setId(ms.getId())
                        .setPackageName(ms.getPackageName())
                        .setClassName(ms.getClassName())
                        .setMethodName(ms.getMethodName())
                        .setParamList(ms.getParamList());
                signatureCache.put(cacheKey, cacheValue);
            }

            return ms;
        } catch (UncategorizedSQLException ex) {
            LOG.error("Error get method {} into database, {}", ms.toString(), ex);
            return ms;
        }
    }

    @Deprecated
    public void saveMethodSignature(MethodSignatureOld ms, Map<String, MethodSignatureOld> signatureCache) {
        String cacheKey = null;
        if(signatureCache != null) {
            cacheKey = MapperUtilService.getMethodSignatureCacheKey(ms);
            MethodSignatureOld cacheValue = signatureCache.get(cacheKey);
            if (cacheValue != null) {
                ms.setId(cacheValue.getId());
                return;
            }
        }

        try {
            int sliceKey = MapperUtilService.getMethodSignatureSliceKey(ms.getPackageName(), ms.getClassName());
            // there will be many duplicate records when analyzing different versions of the same library
            // insertOne is time-consuming, so we do findId first
            Long id = methodSignatureMapper.findId(sliceKey, ms.getPackageName(), ms.getClassName(), ms.getMethodName(), ms.getParamList());
            if (id == null) {
                methodSignatureMapper.insertOne(sliceKey, ms);
                if (ms.getId() == 0) {
                    id = methodSignatureMapper.findId(sliceKey, ms.getPackageName(), ms.getClassName(), ms.getMethodName(), ms.getParamList());
                    if (id == null) { // maybe caused by no transaction
                        id = methodSignatureMapper.findId(sliceKey, ms.getPackageName(), ms.getClassName(), ms.getMethodName(), ms.getParamList());
                    }
                } else {
                    id = ms.getId();
                }
            }
            ms.setId(id);

            if (signatureCache != null) {
                MethodSignatureOld cacheValue = new MethodSignatureOld()
                        .setId(ms.getId())
                        .setPackageName(ms.getPackageName())
                        .setClassName(ms.getClassName())
                        .setMethodName(ms.getMethodName())
                        .setParamList(ms.getParamList());
                signatureCache.put(cacheKey, cacheValue);
            }
        } catch (UncategorizedSQLException ex) {
            LOG.error("Error inserting method {} into database, {}", ms.toString(), ex);
        }
    }
}
