package de.locked.GpxWerkzeug.tools;

import de.locked.GpxWerkzeug.elevation.HgtCache;
import de.locked.GpxWerkzeug.gpx.Gpx;
import de.locked.GpxWerkzeug.gpx.Trkpt;
import de.locked.GpxWerkzeug.gpx.Trkseg;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static java.util.stream.Collectors.toList;

public class GpxFixHeightCleaner {
    private final static Logger LOG = LogManager.getLogger(GpxFixHeightCleaner.class);

    public static Gpx fixHeight(final Gpx gpx, final HgtCache cache) {
        gpx.trk.trkseg = gpx.trk.getTrkseg().stream()
                .map(s -> fixHeight(s, cache))
                .collect(toList());
        return gpx;
    }

    private static Trkseg fixHeight(final Trkseg s, final HgtCache cache) {
        var points = s.getTrkpt().stream()
                .map(p -> new Trkpt(p.getLat(), p.getLon(), getElevation(p, cache), p.getTime())
                ).collect(toList());
        return new Trkseg(points);
    }

    private static Double getElevation(final Trkpt p, final HgtCache cache) {
        try {
            var cached = cache.getElevation(p);
            if (cached.isPresent()) {
                return cached.get().doubleValue();
            }
        } catch (Exception e) {
            LOG.error("error processing point " + p, e);
        }
        return p.getEle();
    }
}
