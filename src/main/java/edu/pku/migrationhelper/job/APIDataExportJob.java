package edu.pku.migrationhelper.job;

import edu.pku.migrationhelper.data.LibraryGroupArtifact;
import edu.pku.migrationhelper.data.LibraryVersion;
import edu.pku.migrationhelper.data.LibraryVersionToSignature;
import edu.pku.migrationhelper.data.MethodSignature;
import edu.pku.migrationhelper.mapper.LibraryGroupArtifactMapper;
import edu.pku.migrationhelper.mapper.LibraryVersionMapper;
import edu.pku.migrationhelper.mapper.LibraryVersionToSignatureMapper;
import edu.pku.migrationhelper.mapper.MethodSignatureMapper;
import edu.pku.migrationhelper.service.LibraryIdentityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "migration-helper.job.enabled", havingValue = "APIDataExportJob")
public class APIDataExportJob implements CommandLineRunner {

    Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    private LibraryGroupArtifactMapper libraryGroupArtifactMapper;

    @Autowired
    private LibraryVersionMapper libraryVersionMapper;

    @Autowired
    private LibraryVersionToSignatureMapper libraryVersionToSignatureMapper;

    @Autowired
    private MethodSignatureMapper methodSignatureMapper;

    @Override
    public void run(String... args) throws Exception {
        if (args.length < 3) {
            LOG.info("Usage: <InputFile> <LibraryOutputFile> <APIOutputFile>");
            return;
        }
        BufferedReader reader = new BufferedReader(new FileReader(args[0]));
        FileWriter libraryWriter = new FileWriter(args[1]);
        FileWriter apiWriter = new FileWriter(args[2]);
        libraryWriter.write("groupArtifactId,groupArtifactName,signatureIds\n");
        String line;
        Set<Long> allSigIds = new HashSet<>();
        while((line = reader.readLine()) != null) {
            String[] ga = line.split(":");
            LibraryGroupArtifact groupArtifact = libraryGroupArtifactMapper.findByGroupIdAndArtifactId(ga[0], ga[1]);
            if(groupArtifact == null) {
                LOG.warn("groupArtifact not found: {}", line);
                continue;
            }
            List<LibraryVersion> versions = libraryVersionMapper.findByGroupArtifactId(groupArtifact.getId());
            List<LibraryVersionToSignature> v2sList = libraryVersionToSignatureMapper.findByIdIn(
                    versions.stream().map(LibraryVersion::getId).collect(Collectors.toList()));
            Set<Long> sigIds = new HashSet<>();
            for (LibraryVersionToSignature v2s : v2sList) {
                sigIds.addAll(v2s.getSignatureIdList());
            }
            allSigIds.addAll(sigIds);
            libraryWriter.write(groupArtifact.getId() + "," + line + ",");
            int i = 0;
            for (Long sigId : sigIds) {
                if(i > 0) {
                    libraryWriter.write(";");
                }
                libraryWriter.write(String.valueOf(sigId));
                i++;
            }
            libraryWriter.write("\n");
        }
        apiWriter.write("signatureId,packageName,className,methodName,paramList\n");
        for (Long sigId : allSigIds) {
            int slice = LibraryIdentityService.getMethodSignatureSliceKey(sigId);
            MethodSignature ms = methodSignatureMapper.findById(slice, sigId);
            if(ms == null) {
                LOG.warn("MethodSignature not found: {}", sigId);
                continue;
            }
            apiWriter.write(String.valueOf(sigId));
            apiWriter.write(",");
            apiWriter.write(ms.getPackageName());
            apiWriter.write(",");
            apiWriter.write(ms.getClassName());
            apiWriter.write(",");
            apiWriter.write(ms.getMethodName());
            apiWriter.write(",");
            apiWriter.write(ms.getParamList());
            apiWriter.write("\n");
        }
        reader.close();
        libraryWriter.close();
        apiWriter.close();
    }
}
