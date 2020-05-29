package anonymous.migrationhelper.data;

public class LibraryOverlap {

    private long groupArtifactId1;

    private long groupArtifactId2;

    private int signatureCount;

    public LibraryOverlap() {
    }

    public LibraryOverlap(long groupArtifactId1, long groupArtifactId2, int signatureCount) {
        this.groupArtifactId1 = groupArtifactId1;
        this.groupArtifactId2 = groupArtifactId2;
        this.signatureCount = signatureCount;
    }

    public LibraryOverlap addSignatureCount(int count) {
        this.signatureCount += count;
        return this;
    }

    public long getGroupArtifactId1() {
        return groupArtifactId1;
    }

    public LibraryOverlap setGroupArtifactId1(long groupArtifactId1) {
        this.groupArtifactId1 = groupArtifactId1;
        return this;
    }

    public long getGroupArtifactId2() {
        return groupArtifactId2;
    }

    public LibraryOverlap setGroupArtifactId2(long groupArtifactId2) {
        this.groupArtifactId2 = groupArtifactId2;
        return this;
    }

    public int getSignatureCount() {
        return signatureCount;
    }

    public LibraryOverlap setSignatureCount(int signatureCount) {
        this.signatureCount = signatureCount;
        return this;
    }
}
