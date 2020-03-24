package edu.pku.migrationhelper.data;

import java.util.List;

public class RepositoryDepSeq {

    private long id;

    private byte[] depSeq;

    private List<Long> depSeqList;

    public long getId() {
        return id;
    }

    public RepositoryDepSeq setId(long id) {
        this.id = id;
        return this;
    }

    public byte[] getDepSeq() {
        return depSeq;
    }

    public RepositoryDepSeq setDepSeq(byte[] depSeq) {
        GetSetHelper.berNumberByteSetter(depSeq, e -> this.depSeq = e, e -> this.depSeqList = e);
        return this;
    }

    public List<Long> getDepSeqList() {
        return depSeqList;
    }

    public RepositoryDepSeq setDepSeqList(List<Long> depSeqList) {
        GetSetHelper.berNumberListSetter(depSeqList, e -> this.depSeq = e, e -> this.depSeqList = e);
        return this;
    }
}
