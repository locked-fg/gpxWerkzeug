package de.locked.GpxWerkzeug.geoJson;

import java.util.List;

/**
 * { "type": "LineString",
 *     "coordinates": [
 *         [30, 10], [10, 30], [40, 40]
 *     ]
 * }
 */
public class LineString {
    public final String type = "LineString";
    public final List<Double[]> coordinates;

    public LineString(List<Double[]> coordinates) {
        this.coordinates = coordinates;
    }
}
