package de.locked.GpxWerkzeug.tools;

import de.locked.GpxWerkzeug.gpx.Gpx;
import de.locked.GpxWerkzeug.gpx.Trk;
import de.locked.GpxWerkzeug.gpx.Trkpt;
import de.locked.GpxWerkzeug.gpx.Trkseg;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

public class GpxStatisticsCalculator {
    private static final Logger LOG = LogManager.getLogger(GpxStatisticsCalculator.class);

    List<Long> timeSeries = new ArrayList<>(); // milliseconds deltas!
    List<Double> distanceSeries = new ArrayList<>(); // m distance deltas!
    List<Double> elevationSeries = new ArrayList<>(); // m
    List<Double> elevationDeltaSeries = new ArrayList<>(); // m
    List<Double> velocitySeries = new ArrayList<>(); // km/h
    List<Double> ascentSeries = new ArrayList<>(); // %

    public final GpxStatistics stats = new GpxStatistics();

    GpxStatisticsCalculator() {
    }

    public GpxStatisticsCalculator(Gpx gpx, final int minMetersForMovement, final int kernelSize) {
        computeSeries(gpx);
        smoothSeries(kernelSize); // this should probably not be done over ALL series
        computeStatsLength();
        computeStatsTimeMoving(minMetersForMovement);
        computeStatsTimeTotal(gpx);
        computeStatsMinMaxAvg();
    }

    void computeSeries(Gpx gpx) {
        computeTimeSeries(gpx);
        computeDistanceSeries(gpx);
        computeElevationSeries(gpx);
        computeElevationDeltaSeries(gpx);
        computeVelocitySeries(gpx);
        computeAscentSeries(gpx);
    }

    private Stream<List<Trkpt>> toTrksegPointStream(Gpx gpx) {
        return gpx.getTrk().stream()
                .map(Trk::getTrkseg)
                .flatMap(Collection::stream)
                .map(Trkseg::getTrkpt);
    }

    void computeTimeSeries(Gpx gpx) {
        toTrksegPointStream(gpx).forEach(seg -> this.computeGenericSeries(seg, timeSeries,
                a -> 0L,
                (a, b) -> TrkptUtils.timeDelta(a, b).orElse(0L)));
    }

    void computeDistanceSeries(Gpx gpx) {
        toTrksegPointStream(gpx).forEach(seg -> this.computeGenericSeries(seg, distanceSeries,
                a -> 0d,
                TrkptUtils::distance));
    }

    void computeElevationSeries(Gpx gpx) {
        toTrksegPointStream(gpx).forEach(seg -> this.computeGenericSeries(seg, elevationSeries,
                Trkpt::getEle,
                (a, b) -> a.getEle()));
    }

    void computeElevationDeltaSeries(Gpx gpx) {
        toTrksegPointStream(gpx).forEach(seg -> this.computeGenericSeries(seg, elevationDeltaSeries,
                a -> 0d,
                (a, b) -> b.getEle() - a.getEle()));
    }

    void computeVelocitySeries(Gpx gpx) {
        toTrksegPointStream(gpx).forEach(seg -> this.computeGenericSeries(seg, velocitySeries,
                a -> 0d,
                TrkptUtils::velocity));
    }

    void computeAscentSeries(Gpx gpx) {
        toTrksegPointStream(gpx).forEach(seg -> this.computeGenericSeries(seg, ascentSeries,
                a -> 0d,
                TrkptUtils::ascent));
    }

    <T extends Number> void computeGenericSeries(List<Trkpt> seg, List<T> out, Function<Trkpt, T> firstVal, BiFunction<Trkpt, Trkpt, T> func) {
        Trkpt prevPoint = null;
        for (Trkpt p : seg) {
            if (prevPoint == null) { // first point in this segment
                prevPoint = p;
                out.add(firstVal.apply(prevPoint));
            } else {
                out.add(func.apply(p, prevPoint));
            }
            prevPoint = p;
        }
    }


    void smoothSeries(final int kernelSize) {
        // distanceSeries = KernelUtils.smooth(kernelSize, distanceSeries); // Why?
        elevationSeries = KernelUtils.smooth(kernelSize, elevationSeries); // FIXME fix underlying GPS data
        elevationDeltaSeries = KernelUtils.smooth(kernelSize, elevationDeltaSeries); // shouldn't be necessary after fixing base data
        velocitySeries = KernelUtils.smooth(kernelSize, velocitySeries); // shouldn't be necessary after fixing base data
        ascentSeries = KernelUtils.smooth(kernelSize, ascentSeries); // shouldn't be necessary after fixing base data
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

    void computeStatsTimeTotal(Gpx gpx) {
        var streamStats = toTrksegPointStream(gpx)
                .flatMap(Collection::stream)
                .map(Trkpt::getTime)
                .flatMap(Optional::stream)
                .map(Date::getTime)
                .mapToLong(Long::valueOf)
                .summaryStatistics(); // a bit overkill, but we need both min and max, so ...
        if (streamStats.getCount() > 0) {
            stats.timeTotal = streamStats.getMax() - streamStats.getMin();
        }
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
     * <p>
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
