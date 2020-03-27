package edu.pku.migrationhelper.job;

import edu.pku.migrationhelper.data.LibraryOverlap;
import edu.pku.migrationhelper.data.LibrarySignatureToVersion;
import edu.pku.migrationhelper.mapper.LibraryOverlapMapper;
import edu.pku.migrationhelper.mapper.LibrarySignatureToVersionMapper;
import edu.pku.migrationhelper.mapper.MethodSignatureMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@ConditionalOnProperty(name = "migration-helper.job.enabled", havingValue = "CalcLibraryOverlapJob")
public class CalcLibraryOverlapJob implements CommandLineRunner {

    Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    private LibraryOverlapMapper libraryOverlapMapper;

    @Autowired
    private LibrarySignatureToVersionMapper librarySignatureToVersionMapper;

    public void run(String... args) throws Exception {
        LOG.info("CalcLibraryOverlapJob start");
        int limit = 1000;
        int insertLimit = 1000;
        if(args.length > 0) {
            limit = Integer.parseInt(args[0]);
        }
        if(args.length > 1) {
            insertLimit = Integer.parseInt(args[1]);
        }
        Map<List<Long>, LibraryOverlap> overlapMap = new HashMap<>(insertLimit * 2);
        for (int tableNum = 0; tableNum < MethodSignatureMapper.MAX_TABLE_COUNT; tableNum++) {
            int offset = 0;
            boolean hasNext = true;
            while(hasNext) {
                List<LibrarySignatureToVersion> s2vList = librarySignatureToVersionMapper.findList(tableNum, offset, limit);
                int len = s2vList.size();
                LOG.info("find s2vList tableNum = {}, offset = {}, limit = {}, len = {}", tableNum, offset, limit, len);
                offset += len;
                if(len < limit) {
                    hasNext = false;
                }
                for (LibrarySignatureToVersion s2v : s2vList) {
                    List<Long> groupArtifactIds = s2v.getGroupArtifactIdList();
                    if(groupArtifactIds == null) continue;
                    int gaLen = groupArtifactIds.size();
                    if(gaLen <= 1) continue;
                    groupArtifactIds.sort(Long::compare);
                    for (int i = 0; i < gaLen; i++) {
                        for (int j = i + 1; j < gaLen; j++) {
                            long gaId1 = groupArtifactIds.get(i);
                            long gaId2 = groupArtifactIds.get(j);
                            overlapMap.computeIfAbsent(Arrays.asList(gaId1, gaId2),
                                    k -> new LibraryOverlap(gaId1, gaId2, 0))
                                    .addSignatureCount(1);
                        }
                    }
                }
                if(overlapMap.size() > insertLimit) {
                    insertAndClear(overlapMap, insertLimit);
                }
            }
        }
        insertAndClear(overlapMap, insertLimit);
        LOG.info("CalcLibraryOverlapJob success");
    }

    private void insertAndClear(Map<List<Long>, LibraryOverlap> overlapMap, int insertLimit) {
        if(overlapMap.size() <= 0) return;
        LOG.info("insert and clear map size = {}", overlapMap.size());
        if(overlapMap.size() > insertLimit) {
            List<LibraryOverlap> insertList = new ArrayList<>(insertLimit);
            for (LibraryOverlap value : overlapMap.values()) {
                insertList.add(value);
                if(insertList.size() >= insertLimit) {
                    libraryOverlapMapper.insert(insertList);
                    insertList.clear();
                }
            }
            if(!insertList.isEmpty()) {
                libraryOverlapMapper.insert(insertList);
            }
        } else {
            libraryOverlapMapper.insert(overlapMap.values());
        }
        overlapMap.clear();
    }

}