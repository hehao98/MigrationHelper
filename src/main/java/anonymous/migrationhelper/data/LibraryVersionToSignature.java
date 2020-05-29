package anonymous.migrationhelper.data;

import java.util.List;

/**
 * Created by xuyul on 2020/2/28.
 */
public class LibraryVersionToSignature {

    private long versionId;

    private byte[] signatureIds;

    private List<Long> signatureIdList;

    public long getVersionId() {
        return versionId;
    }

    public LibraryVersionToSignature setVersionId(long versionId) {
        this.versionId = versionId;
        return this;
    }

    public byte[] getSignatureIds() {
        return signatureIds;
    }

    public LibraryVersionToSignature setSignatureIds(byte[] signatureIds) {
        GetSetHelper.berNumberByteSetter(signatureIds, e -> this.signatureIds = e, e -> this.signatureIdList = e);
        return this;
    }

    public List<Long> getSignatureIdList() {
        return signatureIdList;
    }

    public LibraryVersionToSignature setSignatureIdList(List<Long> signatureIdList) {
        GetSetHelper.berNumberListSetter(signatureIdList, e -> this.signatureIds = e, e -> this.signatureIdList = e);
        return this;
    }
}
