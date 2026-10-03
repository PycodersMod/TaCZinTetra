package com.pycoder.taczintetra.logic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Immutable normalized heat curve. Values are linearly interpolated between configured points. */
public record HeatCurve(List<Point> points) {
    public HeatCurve {
        List<Point> safe = new ArrayList<>();
        if (points != null) {
            for (Point point : points) {
                if (point != null && Double.isFinite(point.heat()) && Double.isFinite(point.value())) {
                    safe.add(new Point(Math.max(0, Math.min(1, point.heat())), Math.max(0.01, point.value())));
                }
            }
        }
        safe.sort(Comparator.comparingDouble(Point::heat));
        points = List.copyOf(safe);
    }

    public static HeatCurve linear(double cold, double hot) {
        double safeCold = finitePositive(cold, 1);
        double safeHot = finitePositive(hot, safeCold);
        return new HeatCurve(List.of(new Point(0, safeCold), new Point(1, safeHot)));
    }

    public double valueAt(double normalizedHeat) {
        if (points.isEmpty()) return 1;
        double heat = Double.isFinite(normalizedHeat) ? Math.max(0, Math.min(1, normalizedHeat)) : 0;
        if (points.size() == 1 || heat <= points.get(0).heat()) return points.get(0).value();
        for (int i = 1; i < points.size(); i++) {
            Point right = points.get(i);
            if (heat <= right.heat()) {
                Point left = points.get(i - 1);
                double span = right.heat() - left.heat();
                if (span <= 0) return right.value();
                double t = (heat - left.heat()) / span;
                return left.value() + (right.value() - left.value()) * t;
            }
        }
        return points.get(points.size() - 1).value();
    }

    private static double finitePositive(double value, double fallback) {
        return Double.isFinite(value) && value > 0 ? value : fallback;
    }

    public record Point(double heat, double value) { }
}
