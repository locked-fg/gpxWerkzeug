package de.locked.GpxWerkzeug.tools;

import de.locked.GpxWerkzeug.gpx.Gpx;
import de.locked.GpxWerkzeug.gpx.Trkpt;
import de.locked.GpxWerkzeug.gpx.Trkseg;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.stream.DoubleStream;

import static java.lang.Math.asin;
import static java.lang.Math.tan;

// TODO add Tests
public class GpxStatisticsCalculator {
    private static final Logger LOG = LogManager.getLogger(GpxStatisticsCalculator.class);
    private static final int KERNEL_SIZE = 7;
    private static final Distance DIST = new DistanceHaversine();

    private final List<Double> distanceSeries = new ArrayList<>(); // m distance deltas!
    private final List<Double> elevationSeries = new ArrayList<>(); // m
    private final List<Double> elevationDeltaSeries = new ArrayList<>(); // m
    private final List<Double> velocitySeries = new ArrayList<>(); // km/h
    private final List<Double> ascentSeries = new ArrayList<>(); // %
    private double[] kernel;

    public final GpxStatistics stats = new GpxStatistics();

    public GpxStatisticsCalculator calc(Gpx gpx, int minMetersForMovement) {
        compute(gpx, minMetersForMovement);
        // smooth(KERNEL_SIZE);
        computeMinMaxAvg();
        return this;
    }

    private void initKernel(int k) {
        kernel = DoubleStream.generate(() -> 1d / k)
                .limit(k).toArray();
    }

    private void smooth(int kernel) {
        if (kernel % 2 == 0) throw new IllegalArgumentException("kernel size must be odd but was " + kernel);
        if (kernel < 3) throw new IllegalArgumentException("kernel size must be >= 3 but was " + kernel);
        initKernel(kernel);
        smoothList(elevationSeries);
        smoothList(velocitySeries);
        // smoothList(elevationDeltaSeries);
        // smoothList(ascentSeries);
    }

    private void smoothList(List<Double> src) {
        var dst = new ArrayList<>(src);
        var d = kernel.length / 2;
        for (int i = kernel.length - 2; i < src.size() - kernel.length - 2; i++) {
            var v = 0.;
            for (int j = 0; j < kernel.length; j++) {
                v += src.get(i - d + j) * kernel[j];
            }
            dst.set(i, v);
        }
        src.clear();
        src.addAll(dst);
    }

    private void compute(Gpx gpx, final int minMetersForMovement) {
        Trkpt firstPoint = null;
        Trkpt lastPoint = null;

        for (Trkseg seg : gpx.trk.trkseg) {
            if (firstPoint == null)
                firstPoint = seg.trkpt.get(0);
            lastPoint = seg.getTrkpt().get(seg.getTrkpt().size() - 1);
            compute(seg.getTrkpt(), minMetersForMovement);
        }

        stats.timeTotal = lastPoint.getTime().getTime() - firstPoint.getTime().getTime();
    }

    void compute(List<Trkpt> seg, final int minMetersForMovement) {
        Trkpt firstPoint = null;
        Trkpt lastPoint = null;

        for (Trkpt p : seg) {
            elevationSeries.add(p.getEle());

            if (firstPoint == null) { // first point in this segment
                firstPoint = p;
                elevationDeltaSeries.add(0d);
                ascentSeries.add(0d);
                velocitySeries.add(0d);
                distanceSeries.add(0d);
            } else {
                // elevation
                elevationDeltaSeries.add(lastPoint.getEle() - p.getEle());
                ascentSeries.add(ascent(lastPoint, p));

                // distance & time
                var dist = d(lastPoint, p);
                distanceSeries.add(dist);
                var timeDelta = p.getTime().getTime() - lastPoint.getTime().getTime(); // ms
                var v = (dist / 1000.) / (timeDelta / 3600000.); // km/h
                velocitySeries.add(v);
                stats.length += dist;
                if (dist > minMetersForMovement) {
                    stats.timeMoving += timeDelta;
                }
            }
            lastPoint = p;
        }
    }

    static double d(Trkpt a, Trkpt b) {
        var d = DIST.distance(a, b);
        LOG.debug("dist {}", d);
        return d;
    }

    private void computeMinMaxAvg() {
        // elevation
        var elevStats = stats(elevationSeries);
        stats.heightMin = elevStats.getMin();
        stats.heightMax = elevStats.getMax();
        // elevation Delta
        stats.elevationUp = toStream(elevationDeltaSeries).filter(v -> v > 0.).sum();
        stats.elevationDown = toStream(elevationDeltaSeries).filter(v -> v < 0.).sum();

        // ascent
        var ascentStats = stats(ascentSeries);
        stats.ascentMin = ascentStats.getMin();
        stats.ascentMax = ascentStats.getMax();
        stats.ascentAvg = toStream(ascentSeries) // average should be done over the absolute values
                .map(Math::abs)
                .summaryStatistics().getAverage();

        // velocity
        var vStats = stats(velocitySeries);
        stats.vMin = vStats.getMin();
        stats.vMax = vStats.getMax();
        stats.vAvg = vStats.getAverage();
    }

    /**
     * @param p1 gpx point a
     * @param p2 gpx point b
     * @return the ascent in % (descent is negative)
     */
    static double ascent(Trkpt p1, Trkpt p2) {
        // Steigung in Prozent = tan(Steigungswinkel in Grad) * 100
        // sin alpha = a/c; c= hypothenuse
        var c = d(p1, p2); // Hypothenuse
        var a = p2.getEle() - p1.getEle();
        var alpha = asin(a / c);
        return tan(alpha) * 100;
    }

    public Double[] getElevationArray() {
        return elevationSeries.toArray(new Double[0]);
    }

    public Double[] getVelocityArray() {
        return velocitySeries.toArray(new Double[0]);
    }

    public Double[] getDistanceRunningSumArray() {
        var arr = distanceSeries.toArray(new Double[0]);
        for (int i = 1; i < arr.length; i++) {
            arr[i] = arr[i - 1] + arr[i];
        }
        return arr;
    }

    private DoubleSummaryStatistics stats(List<Double> l) {
        return toStream(l).summaryStatistics();
    }

    private DoubleStream toStream(List<Double> l) {
        return l.stream().mapToDouble(Double::doubleValue);
    }

}
