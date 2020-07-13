package edu.pku.migrationhelper.data.api;

public class MethodChange {
    private final MethodSignature oldMethod;
    private final MethodSignature newMethod;
    private final boolean isBreakingChange;

    public MethodChange(MethodSignature oldMethod, MethodSignature newMethod) {
        assert oldMethod != null || newMethod != null;
        this.oldMethod = oldMethod;
        this.newMethod = newMethod;
        if (oldMethod == null) {
            this.isBreakingChange = false;
        } else if (newMethod == null) {
            this.isBreakingChange = true;
        } else {
            this.isBreakingChange = !oldMethod.getParameters().equals(newMethod.getParameters())
                    || !oldMethod.getName().equals(newMethod.getName())
                    || !oldMethod.getReturnType().equals(newMethod.getReturnType())
                    || !oldMethod.getExceptions().equals(newMethod.getExceptions())
                    || (!oldMethod.isFinal() && newMethod.isFinal())
                    || oldMethod.isStatic() != newMethod.isStatic()
                    || oldMethod.getVisibilityLevel() > newMethod.getVisibilityLevel();
        }
    }

    public MethodSignature getOldMethod() {
        return oldMethod;
    }

    public MethodSignature getNewMethod() {
        return newMethod;
    }

    public boolean isBreakingChange() {
        return isBreakingChange;
    }
}
