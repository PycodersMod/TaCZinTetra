package com.pycoder.taczintetra.logic;

/** Shared rounding and lower-bound rules for data-driven numeric attributes. */
public final class DefinitionValueSanitizer {
    private DefinitionValueSanitizer() { }

    public static int integer(double value, int minimum) {
        if (!Double.isFinite(value)) return Math.max(0, minimum);
        return Math.max(Math.max(0, minimum), (int) Math.round(value));
    }

    public static double continuous(double value, double minimum) {
        if (!Double.isFinite(value)) return Math.max(0, minimum);
        double bounded = Math.max(Math.max(0, minimum), value);
        return Math.round(bounded * 100d) / 100d;
    }
}
