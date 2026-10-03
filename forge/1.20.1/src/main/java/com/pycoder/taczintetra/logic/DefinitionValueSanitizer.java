package com.pycoder.taczintetra.logic;

/** 数据驱动数值属性共用的舍入规则与下限规则。 */
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
