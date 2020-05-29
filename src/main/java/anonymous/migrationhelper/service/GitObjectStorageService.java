package anonymous.migrationhelper.service;

import anonymous.migrationhelper.data.BlobInfo;
import anonymous.migrationhelper.data.CommitInfo;
import anonymous.migrationhelper.mapper.BlobInfoMapper;
import anonymous.migrationhelper.mapper.CommitInfoMapper;
import org.apache.tomcat.util.buf.HexUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by xxx on 2020/2/28.
 */
@Service
public class GitObjectStorageService {

    @Autowired
    private BlobInfoMapper blobInfoMapper;

    @Autowired
    private CommitInfoMapper commitInfoMapper;

    public void saveBlob(BlobInfo blobInfo) {
        int slice = getBlobSliceKeyById(blobInfo.getBlobId());
        blobInfoMapper.insertOne(slice, blobInfo);
    }

    public BlobInfo getBlobById(String blobId) {
        byte[] blobIdPrefix = HexUtils.fromHexString(blobId.substring(0, 2));
        int slice = getBlobSliceKeyById(blobIdPrefix);
        return blobInfoMapper.findByBlobId(slice, blobId);
    }

    public void saveCommit(CommitInfo commitInfo) {
        int slice = getCommitSliceKeyById(commitInfo.getCommitId());
        commitInfoMapper.insertOne(slice, commitInfo);
    }

    public CommitInfo getCommitById(String commitId) {
        byte[] commitIdPrefix = HexUtils.fromHexString(commitId.substring(0, 2));
        int slice = getCommitSliceKeyById(commitIdPrefix);
        return commitInfoMapper.findByCommitId(slice, commitId);
    }

    public static int getBlobSliceKeyById(byte[] id) {
        return id[0] & (BlobInfoMapper.MAX_TABLE_COUNT - 1);
    }

    public static int getCommitSliceKeyById(byte[] id) {
        return id[0] & (CommitInfoMapper.MAX_TABLE_COUNT - 1);
    }
}
