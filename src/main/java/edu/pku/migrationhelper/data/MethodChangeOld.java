package edu.pku.migrationhelper.data;

import java.util.List;

@Deprecated
public class MethodChangeOld {

    private long id;

    private byte[] deleteSignatureIds;

    private byte[] addSignatureIds;

    private byte[] deleteGroupArtifactIds;

    private byte[] addGroupArtifactIds;

    private long counter;

    private List<Long> deleteSignatureIdList;

    private List<Long> addSignatureIdList;

    private List<Long> deleteGroupArtifactIdList;

    private List<Long> addGroupArtifactIdList;

    public long getId() {
        return id;
    }

    public MethodChangeOld setId(long id) {
        this.id = id;
        return this;
    }

    public byte[] getDeleteSignatureIds() {
        return deleteSignatureIds;
    }

    public MethodChangeOld setDeleteSignatureIds(byte[] deleteSignatureIds) {
        GetSetHelper.berNumberByteSetter(deleteSignatureIds, e -> this.deleteSignatureIds = e, e -> this.deleteSignatureIdList = e);
        return this;
    }

    public List<Long> getDeleteSignatureIdList() {
        return deleteSignatureIdList;
    }

    public MethodChangeOld setDeleteSignatureIdList(List<Long> deleteSignatureIdList) {
        GetSetHelper.berNumberListSetter(deleteSignatureIdList, e -> this.deleteSignatureIds = e, e -> this.deleteSignatureIdList = e);
        return this;
    }


    public byte[] getAddSignatureIds() {
        return addSignatureIds;
    }

    public MethodChangeOld setAddSignatureIds(byte[] addSignatureIds) {
        GetSetHelper.berNumberByteSetter(addSignatureIds, e -> this.addSignatureIds = e, e -> this.addSignatureIdList = e);
        return this;
    }

    public List<Long> getAddSignatureIdList() {
        return addSignatureIdList;
    }

    public MethodChangeOld setAddSignatureIdList(List<Long> addSignatureIdList) {
        GetSetHelper.berNumberListSetter(addSignatureIdList, e -> this.addSignatureIds = e, e -> this.addSignatureIdList = e);
        return this;
    }


    public byte[] getDeleteGroupArtifactIds() {
        return deleteGroupArtifactIds;
    }

    public MethodChangeOld setDeleteGroupArtifactIds(byte[] deleteGroupArtifactIds) {
        GetSetHelper.berNumberByteSetter(deleteGroupArtifactIds, e -> this.deleteGroupArtifactIds = e, e -> this.deleteGroupArtifactIdList = e);
        return this;
    }

    public List<Long> getDeleteGroupArtifactIdList() {
        return deleteGroupArtifactIdList;
    }

    public MethodChangeOld setDeleteGroupArtifactIdList(List<Long> deleteGroupArtifactIdList) {
        GetSetHelper.berNumberListSetter(deleteGroupArtifactIdList, e -> this.deleteGroupArtifactIds = e, e -> this.deleteGroupArtifactIdList = e);
        return this;
    }


    public byte[] getAddGroupArtifactIds() {
        return addGroupArtifactIds;
    }

    public MethodChangeOld setAddGroupArtifactIds(byte[] addGroupArtifactIds) {
        GetSetHelper.berNumberByteSetter(addGroupArtifactIds, e -> this.addGroupArtifactIds = e, e -> this.addGroupArtifactIdList = e);
        return this;
    }

    public List<Long> getAddGroupArtifactIdList() {
        return addGroupArtifactIdList;
    }

    public MethodChangeOld setAddGroupArtifactIdList(List<Long> addGroupArtifactIdList) {
        GetSetHelper.berNumberListSetter(addGroupArtifactIdList, e -> this.addGroupArtifactIds = e, e -> this.addGroupArtifactIdList = e);
        return this;
    }

    public long getCounter() {
        return counter;
    }

    public MethodChangeOld setCounter(long counter) {
        this.counter = counter;
        return this;
    }
}
