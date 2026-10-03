package com.pycoder.taczintetra.logic;

/** Declarative cleanup policy; the Forge/Tetra adapter applies these flags to NBT. */
public final class ModuleReplacementPolicy {
    private ModuleReplacementPolicy() {
    }

    public static Clearance forSlot(String slot) {
        if ("magazine".equals(slot)) {
            return new Clearance(true, true, false, true, true, false, true, false);
        }
        if ("body".equals(slot)) {
            return new Clearance(true, true, true, false, false, false, true, false);
        }
        if ("barrel".equals(slot)) {
            return new Clearance(true, true, false, false, false, true, true, false);
        }
        return new Clearance(false, false, false, false, false, false, false, false);
    }

    public record Clearance(boolean clearLoaded, boolean clearHoning,
                            boolean clearStock, boolean clearReserve,
                            boolean clearGrip, boolean clearOptic,
                            boolean clearMinorPart, boolean returnReplacedPart) {
    }
}
