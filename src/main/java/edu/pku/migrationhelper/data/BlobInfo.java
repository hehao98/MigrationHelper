package edu.pku.migrationhelper.data;

/**
 * Created by xuyul on 2020/2/4.
 */
public class BlobInfo {

    public enum BlobType {
        Java,
        POM,
        Other,
    }

    private String blobId;

    private BlobType blobType;

    private String librarySignatureIds;

    private String libraryVersionIds;

    public String getBlobId() {
        return blobId;
    }

    public BlobInfo setBlobId(String blobId) {
        this.blobId = blobId;
        return this;
    }

    public BlobType getBlobType() {
        return blobType;
    }

    public BlobInfo setBlobType(BlobType blobType) {
        this.blobType = blobType;
        return this;
    }

    public String getLibrarySignatureIds() {
        return librarySignatureIds;
    }

    public BlobInfo setLibrarySignatureIds(String librarySignatureIds) {
        this.librarySignatureIds = librarySignatureIds;
        return this;
    }

    public String getLibraryVersionIds() {
        return libraryVersionIds;
    }

    public BlobInfo setLibraryVersionIds(String libraryVersionIds) {
        this.libraryVersionIds = libraryVersionIds;
        return this;
    }
}
