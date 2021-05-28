package de.locked.GpxWerkzeug.tools;

import de.locked.GpxWerkzeug.gpx.Trkpt;

@FunctionalInterface
public interface Distance {
    double distance(Trkpt a, Trkpt b);
}
