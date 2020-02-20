package edu.pku.migrationhelper.data;

/**
 * Created by xuyul on 2020/2/19.
 */
public class MethodChange {

    private long id;

    private String deleteSignatureIds;

    private String addSignatureIds;

    public long getId() {
        return id;
    }

    public MethodChange setId(long id) {
        this.id = id;
        return this;
    }

    public String getDeleteSignatureIds() {
        return deleteSignatureIds;
    }

    public MethodChange setDeleteSignatureIds(String deleteSignatureIds) {
        this.deleteSignatureIds = deleteSignatureIds;
        return this;
    }

    public String getAddSignatureIds() {
        return addSignatureIds;
    }

    public MethodChange setAddSignatureIds(String addSignatureIds) {
        this.addSignatureIds = addSignatureIds;
        return this;
    }
}
