package edu.pku.migrationhelper.service;

import com.twitter.hashing.KeyHasher;
import edu.pku.migrationhelper.data.*;
import edu.pku.migrationhelper.mapper.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.HttpClients;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Created by xuyul on 2020/1/2.
 */
@Service
@ConfigurationProperties(prefix = "migration-helper.library-identity")
public class LibraryIdentityService {

    Logger LOG = LoggerFactory.getLogger(getClass());

    private HttpClient httpClient = HttpClients.custom()
            .setMaxConnPerRoute(1000)
            .setMaxConnTotal(1000)
            .build();

    @Autowired
    @Qualifier("CalcThreadPool")
    private ExecutorService calcThreadPool;

    @Autowired
    private JarAnalysisService jarAnalysisService;

    @Autowired
    private LibraryGroupArtifactMapper libraryGroupArtifactMapper;

    @Autowired
    private LibraryVersionMapper libraryVersionMapper;

    @Autowired
    private MethodSignatureMapper methodSignatureMapper;

    @Autowired
    private LibrarySignatureToVersionMapper librarySignatureToVersionMapper;

    @Autowired
    private LibraryVersionToSignatureMapper libraryVersionToSignatureMapper;

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
                    .setVersionExtracted(false)
                    .setParsed(false)
                    .setParseError(false);
            libraryGroupArtifactMapper.insert(Collections.singletonList(groupArtifact));
            groupArtifact = libraryGroupArtifactMapper
                    .findByGroupIdAndArtifactId(groupId, artifactId);
        }
        if(groupArtifact.isParsed()) return;
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
                        .setParsed(false)
                        .setParseError(false)));
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
        Map<Long, Set<Long>> signature2Version = new HashMap<>();
        List<LibraryVersion> versionDatas = libraryVersionMapper.findByGroupArtifactId(groupArtifactId);

        boolean analyzeFirstOnly = false;
        if(versionDatas.size() > 30) {
            //TODO 80% of GroupArtifacts have less than 27 versions, skip and re-run those GroupArtifacts which have more than 30 versions later
            analyzeFirstOnly = true;
        }
        analyzeFirstOnly = true; // TODO
        int versionCount = 1;

        boolean containError = false;
        for (LibraryVersion versionData : versionDatas) {
            if(analyzeFirstOnly && versionCount > 1) {
                break;
            }
            versionCount++;

            long versionId = versionData.getId();
            String version = versionData.getVersion();
            File jarFile = new File(generateJarDownloadPath(groupId, artifactId, version));
            try {
                if (versionData.isParsed() || versionData.isParseError()) {
                    if(versionData.isParseError()) {
                        containError = true;
                    }
                    LibraryVersionToSignature v2s = getVersionToSignature(versionId);
                    if(v2s != null && v2s.getSignatureIdList() != null) {
                        v2s.getSignatureIdList().forEach(sid -> signature2Version
                                .computeIfAbsent(sid, k -> new HashSet<>())
                                .add(versionId));
                    }
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
                long jarSize = jarFile.length() / (1024 * 1024);
                LOG.info("start parse library id = {}, size = {} MB", versionData.getId(), jarSize);
                if(jarSize > 50) {
                    analyzeFirstOnly = true;
                    return; // TODO
                }
                List<MethodSignature> signatureList = parseLibraryJar(groupId, artifactId, version, signatureCache);
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
            LOG.error("save groupArtifact fail groupId = {}, artifactId = {}",
                    groupId, artifactId);
            LOG.error("save groupArtifact fail", e);
            groupArtifact.setParsed(true)
                    .setParseError(true);
            libraryGroupArtifactMapper.update(groupArtifact);
        }
    }

    public List<MethodSignature> parseLibraryJar(String groupId, String artifactId, String version, Map<String, MethodSignature> signatureCache) throws Exception {
        String jarPath = generateJarDownloadPath(groupId, artifactId, version);
        List<MethodSignature> signatures = new LinkedList<>();
        jarAnalysisService.analyzeJar(jarPath, signatures);
        for (MethodSignature signature : signatures) {
            saveMethodSignature(signature, signatureCache);
        }
        return signatures;
    }

    public static int getMethodSignatureSliceKey(String packageName, String className) {
        String key = packageName + ":" + className;
        return (int)(KeyHasher.FNV1A_32().hashKey(key.getBytes()) & (MethodSignatureMapper.MAX_TABLE_COUNT - 1));
    }

    public static int getMethodSignatureSliceKey(long signatureId) {
        return (int)(signatureId >> MethodSignatureMapper.MAX_ID_BIT) & (MethodSignatureMapper.MAX_TABLE_COUNT - 1);
    }

    public static String getMethodSignatureCacheKey(MethodSignature ms) {
        return ms.getPackageName() + ":" + ms.getClassName() + ":" + ms.getMethodName() + ":" + ms.getParamList();
    }

    public Map<Long, List<Long>> getSignatureToGroupArtifact(Collection<Long> signatureIds) {
        Map<Integer, Set<Long>> sliceMap = new HashMap<>();
        for (Long signatureId : signatureIds) {
            int slice = getMethodSignatureSliceKey(signatureId);
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

    public LibrarySignatureToVersion getSignatureToVersion(long signatureId) {
        int slice = getMethodSignatureSliceKey(signatureId);
        return librarySignatureToVersionMapper.findById(slice, signatureId);
    }

    public void saveSignatureToVersion(LibrarySignatureToVersion s2v) {
        int slice = getMethodSignatureSliceKey(s2v.getSignatureId());
        librarySignatureToVersionMapper.insertOne(slice, s2v);
    }

    public LibraryVersionToSignature getVersionToSignature(long versionId) {
        return libraryVersionToSignatureMapper.findById(versionId);
    }

    public void saveVersionToSignature(LibraryVersionToSignature v2s) {
        libraryVersionToSignatureMapper.insertOne(v2s);
    }

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

    public List<MethodSignature> getMethodSignatureList(String packageName, String className, String methodName) {
        int sliceKey = getMethodSignatureSliceKey(packageName, className);
        return methodSignatureMapper.findList(sliceKey, packageName, className, methodName);
    }

    public MethodSignature getMethodSignature(MethodSignature ms, Map<String, MethodSignature> signatureCache) {
        String cacheKey = null;
        if(signatureCache != null) {
            cacheKey = getMethodSignatureCacheKey(ms);
            MethodSignature cacheValue = signatureCache.get(cacheKey);
            if (cacheValue != null) {
                ms.setId(cacheValue.getId());
                return ms;
            }
        }

        try {
            int sliceKey = getMethodSignatureSliceKey(ms.getPackageName(), ms.getClassName());
            Long id = methodSignatureMapper.findId(sliceKey, ms.getPackageName(), ms.getClassName(), ms.getMethodName(), ms.getParamList());
            if(id == null) return null;
            ms.setId(id);

            if (signatureCache != null) {
                MethodSignature cacheValue = new MethodSignature()
                        .setId(ms.getId())
                        .setPackageName(ms.getPackageName())
                        .setClassName(ms.getClassName())
                        .setMethodName(ms.getMethodName())
                        .setParamList(ms.getParamList());
                signatureCache.put(cacheKey, cacheValue);
            }

            return ms;
        } finally {

        }
    }

    public void saveMethodSignature(MethodSignature ms, Map<String, MethodSignature> signatureCache) {
        String cacheKey = null;
        if(signatureCache != null) {
            cacheKey = getMethodSignatureCacheKey(ms);
            MethodSignature cacheValue = signatureCache.get(cacheKey);
            if (cacheValue != null) {
                ms.setId(cacheValue.getId());
                return;
            }
        }

        try {
            int sliceKey = getMethodSignatureSliceKey(ms.getPackageName(), ms.getClassName());
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
                MethodSignature cacheValue = new MethodSignature()
                        .setId(ms.getId())
                        .setPackageName(ms.getPackageName())
                        .setClassName(ms.getClassName())
                        .setMethodName(ms.getMethodName())
                        .setParamList(ms.getParamList());
                signatureCache.put(cacheKey, cacheValue);
            }
        } finally {

        }
    }

    public void downloadLibraryFromMaven(String groupId, String artifactId, String version, OutputStream output) throws IOException {
        Iterator<String> baseIt = mavenUrlBase.iterator();
        try {
            while (baseIt.hasNext()) {
                String url = baseIt.next()
                        + groupId.replace(".", "/") + "/"
                        + artifactId + "/" + version + "/"
                        + artifactId + "-" + version + ".jar";
                HttpGet request = new HttpGet(url);
                try {
                    HttpResponse response = executeHttpRequest(request);
                    response.getEntity().writeTo(output);
                    output.flush();
                    return;
                } catch (Exception e) {
                    if (!baseIt.hasNext()) throw e;
                    LOG.info("http fail, try next, url = {}", url);
                    continue;
                } finally {
                    request.releaseConnection();
                }
            }
        } finally {
            output.close();
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
