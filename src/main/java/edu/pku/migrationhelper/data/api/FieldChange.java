package edu.pku.migrationhelper.data.api;

public class FieldChange {
    private final FieldSignature oldField;
    private final FieldSignature newField;
    private final boolean isBreakingChange;

    public FieldChange(FieldSignature oldField, FieldSignature newField) {
        assert oldField != null || newField != null;
        this.oldField = oldField;
        this.newField = newField;
        if (oldField == null) {
            this.isBreakingChange = false;
        } else if (newField == null) {
            this.isBreakingChange = true;
        } else {
            this.isBreakingChange = !oldField.getName().equals(newField.getName())
                    || !oldField.getType().equals(newField.getType())
                    || (!oldField.isFinal() && newField.isFinal())
                    || oldField.getVisibilityLevel() > newField.getVisibilityLevel();
        }
    }

    public FieldSignature getOldField() {
        return oldField;
    }

    public FieldSignature getNewField() {
        return newField;
    }

    public boolean isBreakingChange() {
        return isBreakingChange;
    }
}
