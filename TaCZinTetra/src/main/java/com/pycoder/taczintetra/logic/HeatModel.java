package com.pycoder.taczintetra.logic;

/** Lazy Newton cooling model used by the future per-stack heat adapter. */
public final class HeatModel {
    private HeatModel() {
    }

    public static double cool(double oldTemperature, double ambient,
                              double coefficient, double deltaSeconds, double epsilon) {
        double safeAmbient = Double.isFinite(ambient) ? ambient : 0;
        if (!Double.isFinite(oldTemperature)) {
            return safeAmbient;
        }
        double k = Double.isFinite(coefficient) ? Math.max(0, coefficient) : 0;
        double dt = Double.isFinite(deltaSeconds) ? Math.max(0, deltaSeconds) : 0;
        double safeEpsilon = Double.isFinite(epsilon) ? Math.max(0, epsilon) : 0;
        double result = safeAmbient + (oldTemperature - safeAmbient) * Math.exp(-k * dt);
        return Math.abs(result - safeAmbient) <= safeEpsilon ? safeAmbient : result;
    }

    public static boolean isOverheated(double temperature, double threshold, double epsilon) {
        if (!Double.isFinite(temperature) || !Double.isFinite(threshold)) {
            return false;
        }
        double safeEpsilon = Double.isFinite(epsilon) ? Math.max(0, epsilon) : 0;
        return temperature >= threshold - safeEpsilon;
    }

    public static boolean canUnlock(double temperature, double ambient, double epsilon) {
        if (!Double.isFinite(temperature) || !Double.isFinite(ambient)) {
            return false;
        }
        double safeEpsilon = Double.isFinite(epsilon) ? Math.max(0, epsilon) : 0;
        return Math.abs(temperature - ambient) <= safeEpsilon;
    }
}
