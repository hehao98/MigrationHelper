package edu.pku.migrationhelper.data.lib;

import edu.pku.migrationhelper.data.GetSetHelper;

import java.util.List;

/**
 * Created by xuyul on 2020/2/28.
 */
public class LibrarySignatureToVersion {

    private long signatureId;

    private byte[] versionIds;

    private byte[] groupArtifactIds;

    private List<Long> versionIdList;

    private List<Long> groupArtifactIdList;

    public long getSignatureId() {
        return signatureId;
    }

    public LibrarySignatureToVersion setSignatureId(long signatureId) {
        this.signatureId = signatureId;
        return this;
    }

    public byte[] getVersionIds() {
        return versionIds;
    }

    public LibrarySignatureToVersion setVersionIds(byte[] versionIds) {
        GetSetHelper.berNumberByteSetter(versionIds, e -> this.versionIds = e, e -> this.versionIdList = e);
        return this;
    }

    public byte[] getGroupArtifactIds() {
        return groupArtifactIds;
    }

    public LibrarySignatureToVersion setGroupArtifactIds(byte[] groupArtifactIds) {
        GetSetHelper.berNumberByteSetter(groupArtifactIds, e -> this.groupArtifactIds = e, e -> this.groupArtifactIdList = e);
        return this;
    }

    public List<Long> getVersionIdList() {
        return versionIdList;
    }

    public LibrarySignatureToVersion setVersionIdList(List<Long> versionIdList) {
        GetSetHelper.berNumberListSetter(versionIdList, e -> this.versionIds = e, e -> this.versionIdList = e);
        return this;
    }

    public List<Long> getGroupArtifactIdList() {
        return groupArtifactIdList;
    }

    public LibrarySignatureToVersion setGroupArtifactIdList(List<Long> groupArtifactIdList) {
        GetSetHelper.berNumberListSetter(groupArtifactIdList, e -> this.groupArtifactIds = e, e -> this.groupArtifactIdList = e);
        return this;
    }
}
