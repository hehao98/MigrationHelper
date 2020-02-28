package edu.pku.migrationhelper.data;

import java.util.List;

/**
 * Created by xuyul on 2020/2/4.
 */
public class BlobInfo {

    public enum BlobType {
        Java(1),
        POM(2),
        ErrorJava(3),
        ErrorPOM(4),
        Other(5),
        ;

        private int id;

        BlobType(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static BlobType findById(int id) {
            if(id == 0) return null;
            for (BlobType value : BlobType.values()) {
                if(value.id == id) return value;
            }
            return null;
        }
    }

    private byte[] blobId;

    private String blobIdString;

    private BlobType blobTypeEnum;

    private byte[] librarySignatureIds;

    private byte[] libraryVersionIds;

    private byte[] libraryGroupArtifactIds;

    private List<Long> librarySignatureIdList;

    private List<Long> libraryVersionIdList;

    private List<Long> libraryGroupArtifactIdList;

    public byte[] getBlobId() {
        return blobId;
    }

    public BlobInfo setBlobId(byte[] blobId) {
        GetSetHelper.hexByteSetter(blobId, e -> this.blobId = e, e -> this.blobIdString = e);
        return this;
    }

    public String getBlobIdString() {
        return blobIdString;
    }

    public BlobInfo setBlobIdString(String blobIdString) {
        GetSetHelper.hexStringSetter(blobIdString, e -> this.blobId = e, e -> this.blobIdString = e);
        return this;
    }

    public int getBlobType() {
        return blobTypeEnum.id;
    }

    public BlobInfo setBlobType(int id) {
        this.blobTypeEnum = BlobType.findById(id);
        return this;
    }

    public BlobType getBlobTypeEnum() {
        return blobTypeEnum;
    }

    public BlobInfo setBlobTypeEnum(BlobType blobType) {
        this.blobTypeEnum = blobType;
        return this;
    }

    public byte[] getLibrarySignatureIds() {
        return librarySignatureIds;
    }

    public BlobInfo setLibrarySignatureIds(byte[] librarySignatureIds) {
        GetSetHelper.berNumberByteSetter(librarySignatureIds, e -> this.librarySignatureIds = e, e -> this.librarySignatureIdList = e);
        return this;
    }

    public List<Long> getLibrarySignatureIdList() {
        return librarySignatureIdList;
    }

    public BlobInfo setLibrarySignatureIdList(List<Long> librarySignatureIdList) {
        GetSetHelper.berNumberListSetter(librarySignatureIdList, e -> this.librarySignatureIds = e, e -> this.librarySignatureIdList = e);
        return this;
    }


    public byte[] getLibraryVersionIds() {
        return libraryVersionIds;
    }

    public BlobInfo setLibraryVersionIds(byte[] libraryVersionIds) {
        GetSetHelper.berNumberByteSetter(libraryVersionIds, e -> this.libraryVersionIds = e, e -> this.libraryVersionIdList = e);
        return this;
    }

    public List<Long> getLibraryVersionIdList() {
        return libraryVersionIdList;
    }

    public BlobInfo setLibraryVersionIdList(List<Long> libraryVersionIdList) {
        GetSetHelper.berNumberListSetter(libraryVersionIdList, e -> this.libraryVersionIds = e, e -> this.libraryVersionIdList = e);
        return this;
    }


    public byte[] getLibraryGroupArtifactIds() {
        return libraryGroupArtifactIds;
    }

    public BlobInfo setLibraryGroupArtifactIds(byte[] libraryGroupArtifactIds) {
        GetSetHelper.berNumberByteSetter(libraryGroupArtifactIds, e -> this.libraryGroupArtifactIds = e, e -> this.libraryGroupArtifactIdList = e);
        return this;
    }

    public List<Long> getLibraryGroupArtifactIdList() {
        return libraryGroupArtifactIdList;
    }

    public BlobInfo setLibraryGroupArtifactIdList(List<Long> libraryGroupArtifactIdList) {
        GetSetHelper.berNumberListSetter(libraryGroupArtifactIdList, e -> this.libraryGroupArtifactIds = e, e -> this.libraryGroupArtifactIdList = e);
        return this;
    }
}
