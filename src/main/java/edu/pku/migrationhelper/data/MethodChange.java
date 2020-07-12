package edu.pku.migrationhelper.data;

import java.util.List;

public class MethodChange {

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

    public MethodChange setId(long id) {
        this.id = id;
        return this;
    }

    public byte[] getDeleteSignatureIds() {
        return deleteSignatureIds;
    }

    public MethodChange setDeleteSignatureIds(byte[] deleteSignatureIds) {
        GetSetHelper.berNumberByteSetter(deleteSignatureIds, e -> this.deleteSignatureIds = e, e -> this.deleteSignatureIdList = e);
        return this;
    }

    public List<Long> getDeleteSignatureIdList() {
        return deleteSignatureIdList;
    }

    public MethodChange setDeleteSignatureIdList(List<Long> deleteSignatureIdList) {
        GetSetHelper.berNumberListSetter(deleteSignatureIdList, e -> this.deleteSignatureIds = e, e -> this.deleteSignatureIdList = e);
        return this;
    }


    public byte[] getAddSignatureIds() {
        return addSignatureIds;
    }

    public MethodChange setAddSignatureIds(byte[] addSignatureIds) {
        GetSetHelper.berNumberByteSetter(addSignatureIds, e -> this.addSignatureIds = e, e -> this.addSignatureIdList = e);
        return this;
    }

    public List<Long> getAddSignatureIdList() {
        return addSignatureIdList;
    }

    public MethodChange setAddSignatureIdList(List<Long> addSignatureIdList) {
        GetSetHelper.berNumberListSetter(addSignatureIdList, e -> this.addSignatureIds = e, e -> this.addSignatureIdList = e);
        return this;
    }


    public byte[] getDeleteGroupArtifactIds() {
        return deleteGroupArtifactIds;
    }

    public MethodChange setDeleteGroupArtifactIds(byte[] deleteGroupArtifactIds) {
        GetSetHelper.berNumberByteSetter(deleteGroupArtifactIds, e -> this.deleteGroupArtifactIds = e, e -> this.deleteGroupArtifactIdList = e);
        return this;
    }

    public List<Long> getDeleteGroupArtifactIdList() {
        return deleteGroupArtifactIdList;
    }

    public MethodChange setDeleteGroupArtifactIdList(List<Long> deleteGroupArtifactIdList) {
        GetSetHelper.berNumberListSetter(deleteGroupArtifactIdList, e -> this.deleteGroupArtifactIds = e, e -> this.deleteGroupArtifactIdList = e);
        return this;
    }


    public byte[] getAddGroupArtifactIds() {
        return addGroupArtifactIds;
    }

    public MethodChange setAddGroupArtifactIds(byte[] addGroupArtifactIds) {
        GetSetHelper.berNumberByteSetter(addGroupArtifactIds, e -> this.addGroupArtifactIds = e, e -> this.addGroupArtifactIdList = e);
        return this;
    }

    public List<Long> getAddGroupArtifactIdList() {
        return addGroupArtifactIdList;
    }

    public MethodChange setAddGroupArtifactIdList(List<Long> addGroupArtifactIdList) {
        GetSetHelper.berNumberListSetter(addGroupArtifactIdList, e -> this.addGroupArtifactIds = e, e -> this.addGroupArtifactIdList = e);
        return this;
    }

    public long getCounter() {
        return counter;
    }

    public MethodChange setCounter(long counter) {
        this.counter = counter;
        return this;
    }
}
