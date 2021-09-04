package de.locked.GpxWerkzeug.tools;

import de.locked.GpxWerkzeug.gpx.Gpx;
import de.locked.GpxWerkzeug.gpx.Trk;
import de.locked.GpxWerkzeug.gpx.Trkpt;
import de.locked.GpxWerkzeug.gpx.Trkseg;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import static java.lang.Math.asin;
import static java.lang.Math.tan;

public class GpxStatisticsCalculator {
    private static final Logger LOG = LogManager.getLogger(GpxStatisticsCalculator.class);
    private static final Distance DIST = new DistanceHaversine();

    List<Long> timeSeries = new ArrayList<>(); // milliseconds deltas!
    List<Double> distanceSeries = new ArrayList<>(); // m distance deltas!
    List<Double> elevationSeries = new ArrayList<>(); // m
    List<Double> elevationDeltaSeries = new ArrayList<>(); // m
    List<Double> velocitySeries = new ArrayList<>(); // km/h
    List<Double> ascentSeries = new ArrayList<>(); // %

    public final GpxStatistics stats = new GpxStatistics();

    GpxStatisticsCalculator() {}

    public GpxStatisticsCalculator(Gpx gpx, final int minMetersForMovement, final int kernelSize) {
        computeSeries(gpx);
        smoothSeries(kernelSize);
        computeStatsLength();
        computeStatsTimeMoving(minMetersForMovement);
        computeStatsTimeTotal(gpx);
        computeStatsMinMaxAvg();
    }

    void smoothSeries(final int kernelSize) {
        distanceSeries = Utils.smooth(kernelSize, distanceSeries);
        elevationSeries = Utils.smooth(kernelSize, elevationSeries);
        elevationDeltaSeries = Utils.smooth(kernelSize, elevationDeltaSeries);
        velocitySeries = Utils.smooth(kernelSize, velocitySeries);
        ascentSeries = Utils.smooth(kernelSize, ascentSeries);
    }

    private void computeStatsTimeMoving(final int minMetersForMovement) {
        for (int i = 0; i < distanceSeries.size(); i++) {
            if (distanceSeries.get(i) > minMetersForMovement) {
                stats.timeMoving += timeSeries.get(i);
            }
        }
    }

    private void computeStatsLength() {
        stats.length = distanceSeries.stream().mapToDouble(Double::doubleValue).sum();
    }

    void computeSeries(Gpx gpx) {
        gpx.getTrk().stream()
                .map(Trk::getTrkseg)
                .flatMap(Collection::stream)
                .map(Trkseg::getTrkpt)
                .forEach(this::computeSeries);
    }

    void computeStatsTimeTotal(Gpx gpx) {
        // unpack the Trackpoints over all Tracks & Segments
        var dates = gpx.getTrk().stream()
                .flatMap(t -> t.getTrkseg().stream())
                .flatMap(seg -> seg.getTrkpt().stream())
                .parallel()
                .map(Trkpt::getTime)
                .flatMap(Optional::stream)
                .map(Date::getTime)
                .collect(Collectors.toList());
        if (!dates.isEmpty()) {
            var dateMin = Collections.min(dates);
            var dateMax = Collections.max(dates);
            stats.timeTotal = dateMax - dateMin;
        }
    }

    void computeSeries(List<Trkpt> seg) {
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
                timeSeries.add(0L);
            } else {
                // elevation
                elevationDeltaSeries.add(lastPoint.getEle() - p.getEle());
                ascentSeries.add(ascent(lastPoint, p));

                // distance & time
                var dist = d(lastPoint, p);
                distanceSeries.add(dist);

                var timeDelta = timeDelta(p, lastPoint); // ms
                timeSeries.add(timeDelta.orElse(0L)); // or store the optional?
                var v = timeDelta
                        .map(t -> (dist / 1000.) / (t / 3600000.)) // km/h
                        .orElse(0d);
                velocitySeries.add(v); // or store the optional?
            }
            lastPoint = p;
        }
    }

    /**
     * returns the milliseconds delta between the trackpoints (if both have a time)
     *
     * @param a point
     * @param b point
     * @return optional(abs ( a.time - b.time)) or empty if one of the times is missing
     */
    Optional<Long> timeDelta(Trkpt a, Trkpt b) {
        if (a.getTime().isPresent() && b.getTime().isPresent()) {
            var la = a.getTime().get().getTime();
            var lb = b.getTime().get().getTime();
            return Optional.of(Math.abs(lb - la));
        } else {
            return Optional.empty();
        }
    }

    static double d(Trkpt a, Trkpt b) {
        var d = DIST.distance(a, b);
        LOG.debug("dist {}", d);
        return d;
    }

    void computeStatsMinMaxAvg() {
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

    public Double[] getAscendArray() {
        return ascentSeries.toArray(new Double[0]);
    }

    /**
     * returns the running sum of the trip.
     *
     * Steps (distanceSeries) 1,1,1 -> 1,2,3
     *
     * @return array of running sum
     */
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
