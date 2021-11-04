package de.locked.GpxWerkzeug.tools;

import de.locked.GpxWerkzeug.gpx.Gpx;
import de.locked.GpxWerkzeug.gpx.Trkpt;
import de.locked.GpxWerkzeug.gpx.Trkseg;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public class GpxCleaner {
    private final static Logger LOG = LogManager.getLogger(GpxCleaner.class);
    private final static Distance dist = new DistanceHaversine();

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
     * Remove points during a pause. A pause is defined as "all successive points within
     * a certin distance".
     * Iterates through a TrkSeg and removes points with a distance less than maxDistMeters
     * to the preceding point.
     *
     * @param seg
     * @param maxDistMeters
     * @return a new TrkSeg
     */
    public static Optional<Trkseg> collapsPauses(final Trkseg seg, final int maxDistMeters) {
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

        if (LOG.isDebugEnabled() && seg.size() != out.size()) {
            int delta = seg.size() - out.size();
            LOG.debug("{} points removed: {} -> {} (maxDist: {}m)", delta, seg.size(), out.size(), maxDistMeters);
        }

        if (out.size() <= 1) return Optional.empty();
        else return Optional.of(new Trkseg(out));
    }

    /**
     * Remove points during a pause. A pause is defined as "all successive points within
     * a certin distance".
     * Iterates through the TrkSegs of the GPX and removes points with a distance less than maxDistMeters
     * to the preceding point. The GPX object is reused. The TrkSeg is replaced.
     *
     * @param gpx
     * @param maxDistMeters
     * @return the incoming and modified GPX object
     */
    public static Gpx collapsPauses(Gpx gpx, final int maxDistMeters) {
        gpx.trk.trkseg = gpx.trk.trkseg.stream()
                .map(s -> collapsPauses(s, maxDistMeters))
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