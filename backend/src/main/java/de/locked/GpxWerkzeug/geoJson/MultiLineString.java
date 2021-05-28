package de.locked.GpxWerkzeug.geoJson;

import java.util.List;

/**
 * { "type": "MultiLineString",
 *     "coordinates": [
 *         [[10, 10], [20, 20], [10, 40]],
 *         [[40, 40], [30, 30], [40, 20], [30, 10]]
 *     ]
 * }
 */
public class MultiLineString {
    public final String type = "MultiLineString";
    public final List<List<Double[]>> coordinates;

    public MultiLineString(List<List<Double[]>> coordinates) {
        this.coordinates = coordinates;
    }
}
