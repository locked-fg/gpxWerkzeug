package de.locked.GpxWerkzeug.tools;

import de.locked.GpxWerkzeug.gpx.Trkpt;

import java.util.Optional;

import static java.lang.Math.asin;
import static java.lang.Math.tan;

public class TrkptUtils {
    static final Distance DIST = new DistanceHaversine();

    /**
     * returns the milliseconds delta between the trackpoints (if both have a time)
     *
     * @param a point
     * @param b point
     * @return optional(abs ( a.time - b.time)) or empty if one of the times is missing
     */
    public static Optional<Long> timeDelta(Trkpt a, Trkpt b) {
        if (a.getTime().isPresent() && b.getTime().isPresent()) {
            var la = a.getTime().get().getTime();
            var lb = b.getTime().get().getTime();
            return Optional.of(Math.abs(lb - la));
        } else {
            return Optional.empty();
        }
    }

    public static double distance(Trkpt a, Trkpt b) {
        return DIST.distance(a, b);
    }

    /**
     * @param p1 gpx point a
     * @param p2 gpx point b
     * @return the ascent in % (descent is negative)
     */
    public static double ascent(Trkpt p1, Trkpt p2) {
        // Steigung in Prozent = tan(Steigungswinkel in Grad) * 100
        // sin alpha = a/c; c= hypothenuse
        var c = distance(p1, p2); // Hypothenuse
        var a = p2.getEle() - p1.getEle();
        var alpha = asin(a / c);
        return tan(alpha) * 100;
    }

    public static double velocity(Trkpt a, Trkpt b) {
        var time = TrkptUtils.timeDelta(a, b);
        var dist = distance(a, b);
        return time
                .map(t -> (dist / 1000.) / (t / 3600000.)) // km/h
                .orElse(0d);
    }
}
