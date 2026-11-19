package com.pycoder.taczintetra.client;

/** Pure result check for a development observation around a Tetra workbench GUI transaction. */
final class WorkbenchGuiCraftAssertion {
    record Result(boolean passed, String reason) {
    }

    private WorkbenchGuiCraftAssertion() {
    }

    static Result evaluate(String beforeModule, String afterModule, String expectedModule,
                           int materialCountBefore, int materialCountAfter) {
        if (beforeModule == null || beforeModule.isBlank()
                || afterModule == null || afterModule.isBlank()
                || expectedModule == null || expectedModule.isBlank()) {
            return new Result(false, "module_snapshot_missing");
        }
        if (materialCountBefore <= 0 || materialCountAfter < 0
                || materialCountAfter >= materialCountBefore) {
            return new Result(false, "material_not_consumed");
        }
        if (expectedModule.equals(beforeModule)) {
            return new Result(false, "expected_module_present_before_interaction");
        }
        if (!expectedModule.equals(afterModule)) {
            return new Result(false, "expected_module_not_observed_after_interaction");
        }
        return new Result(true, "expected_module_changed_and_material_consumed");
    }
}
