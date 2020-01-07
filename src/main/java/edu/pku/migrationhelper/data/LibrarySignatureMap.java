package edu.pku.migrationhelper.data;

public class LibrarySignatureMap {

    private long libraryVersionId;

    private long methodSignatureId;

    public long getLibraryVersionId() {
        return libraryVersionId;
    }

    public LibrarySignatureMap setLibraryVersionId(long libraryVersionId) {
        this.libraryVersionId = libraryVersionId;
        return this;
    }

    public long getMethodSignatureId() {
        return methodSignatureId;
    }

    public LibrarySignatureMap setMethodSignatureId(long methodSignatureId) {
        this.methodSignatureId = methodSignatureId;
        return this;
    }
}
