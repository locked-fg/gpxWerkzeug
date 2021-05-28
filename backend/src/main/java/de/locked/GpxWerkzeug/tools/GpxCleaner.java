package de.locked.GpxWerkzeug.tools;

import de.locked.GpxWerkzeug.gpx.Gpx;
import de.locked.GpxWerkzeug.gpx.Trkpt;
import de.locked.GpxWerkzeug.gpx.Trkseg;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.Math.abs;
import static java.util.stream.Collectors.toList;

public class GpxCleaner {
    private final static Logger LOG = LogManager.getLogger(GpxCleaner.class);
    private final static Distance dist = new DistanceHaversine();

    private final static double Z_THRESHOLD_ELEV = 2; // https://statisticsbyjim.com/basics/outliers/
    private final static int MIN_METERS_MOVEMENT = 10;

    public static Gpx cleanElevationOutliers(Gpx gpx) {
        gpx.trk.trkseg = gpx.trk.trkseg.stream()
                .map(trkSeg -> new Trkseg(cleanElevationOutliers(trkSeg.trkpt)))
                .collect(toList());
        return gpx;
    }

    /**
     * Removes outliers in height. An outlier is replaced by the mean of the surrounding points
     * TODO: Could/Should become obsolete as soon as height is adjusted by SRTM data
     *
     * @param in list of points
     * @return list of points with (possibly) adjusted heights
     */
    public static List<Trkpt> cleanElevationOutliers(final List<Trkpt> in) {
        var elevs = cleanOutlier(in.stream().map(Trkpt::getEle).collect(toList()));
        var out = new ArrayList<Trkpt>(in.size());
        for (int i = 0; i < in.size(); i++) {
            var p = in.get(i);
            out.add(new Trkpt(p.getLat(), p.getLon(), elevs.get(i), p.getTime()));
        }
        return out;
    }

    private static List<Double> cleanOutlier(final List<Double> input) {
        final var deltas = toDelta(input);
        final var stats = new SummaryStatistics();
        for (Double v : deltas) {
            stats.addValue(v);
        }
        final var sigma = stats.getStandardDeviation();
        final var mu = stats.getMean();
        final var out = new ArrayList<Double>(deltas.size());

        for (int i = 0; i < deltas.size(); i++) {
            var delta = deltas.get(i);
            var z = (deltas.get(i) - mu) / sigma;
            var value = input.get(i);
            if (abs(z) > Z_THRESHOLD_ELEV) { // handle outlier
                LOG.debug("outlier? i: {}, value:{}, delta: {}, z: {}", new Object[]{i, value, delta, z});
                if (i == 0) {
                    value = input.get(1); // TODO: quite ad Hoc
                } else if (i == deltas.size() - 1) {
                    value = input.get(deltas.size() - 2); // TODO: quite ad Hoc
                } else {
                    // correct the current value: take the mean between left and right
                    // left is taken from the (possibly) corrected values, right from the source
                    value = (out.get(i - 1) + input.get(i + 1)) / 2d;
                }
            }
            out.add(value);
        }
        return out;
    }

    private static List<Double> toDelta(List<Double> in) {
        var out = new ArrayList<Double>(in.size());
        out.add(0d);
        for (int i = 1; i < in.size(); i++) {
            out.add(in.get(i) - in.get(i - 1));
        }
        return out;
    }

    public static List<Trkseg> splitByDist(final Trkseg seg, final int maxDistMeters) {
        List<Trkpt> points = seg.getTrkpt();
        List<Trkseg> result = new ArrayList<>();

        int firstIndex = 0;
        for (int i = 0; i < points.size() - 1; i++) {
            double d = dist.distance(points.get(i), points.get(i + 1));
            if (d >= maxDistMeters) { // split due to distance
                result.add(toTrkSeg(points, firstIndex, i + 1));
                firstIndex = i + 1;
            }
        }
        result.add(toTrkSeg(points, firstIndex, points.size())); // finalize last open segment

        if (result.size() > 1) {
            LOG.debug("split into {} segments occured (maxdist: {}m)", result.size(), maxDistMeters);
        }

        return result;
    }

    /**
     * create a TrkSeg from a source list by .sublist(a,b).
     *
     * @param in source list
     * @param a  index (including)
     * @param b  index (exclusive)
     * @return a new Trkseg
     */
    private static Trkseg toTrkSeg(List<Trkpt> in, int a, int b) {
        return new Trkseg(new ArrayList<>(in.subList(a, b)));
    }

    private static double d(Trkpt a, Trkpt b) {
        var d = dist.distance(a, b);
        return d;
    }

    /**
     * remove points during a pause
     *
     * @param seg
     * @param maxDistMeters
     * @return
     */
    public static Optional<Trkseg> cleanPauses(final Trkseg seg, final int maxDistMeters) {
        var in = seg.getTrkpt();
        if (in.size() <= 1) return Optional.empty();

        var out = new ArrayList<Trkpt>(seg.size());
        var last = in.get(0);
        out.add(last);
        for (int i = 1; i < in.size(); i++) {
            var now = in.get(i);
            double d = d(last, now);
            if (d >= maxDistMeters) {
                last = now;
                out.add(last);
            }
        }

        if (seg.size() != out.size()) {
            int delta = seg.size() - out.size();
            LOG.debug("{} points removed: {} -> {} (maxDist: {}m)", delta, seg.size(), out.size(), maxDistMeters);
        }

        if (out.size() <= 1) return Optional.empty();
        else return Optional.of(new Trkseg(out));
    }

    public static Gpx cleanPauses(Gpx gpx, final int threshold) {
        gpx.trk.trkseg = gpx.trk.trkseg.stream()
                .map(s -> cleanPauses(s, threshold))
                .flatMap(Optional::stream)
                .collect(toList());
        return gpx;
    }

    public static Gpx splitByDist(Gpx gpx, final int minDistMeters) {
        gpx.trk.trkseg = gpx.trk.trkseg.stream()
                .flatMap(s -> splitByDist(s, minDistMeters).stream())
                .collect(toList());
        return gpx;
    }

}