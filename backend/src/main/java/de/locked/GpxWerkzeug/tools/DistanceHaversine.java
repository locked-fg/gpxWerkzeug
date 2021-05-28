package de.locked.GpxWerkzeug.tools;

import de.locked.GpxWerkzeug.gpx.Trkpt;

import static java.lang.Math.*;

public class DistanceHaversine implements Distance {
    /**
     * Calculate distance between two points in latitude and longitude taking
     * into account height difference. If you are not interested in height
     * difference pass 0.0. Uses Haversine method as its base.
     *
     * lat1, lon1 Start point lat2, lon2 End point el1 Start altitude in meters
     * el2 End altitude in meters
     * @returns DistanceHaversine in Meters
     * @link https://stackoverflow.com/questions/3694380/calculating-distance-between-two-points-using-latitude-longitude
     * @link https://en.wikipedia.org/wiki/Haversine_formula
     */
    private double distanceHaversine(double lat1, double lat2, double lon1,
                                  double lon2, double el1, double el2) {
        final int R = 6371; // Radius of the earth

        double latDistance = toRadians(lat2 - lat1);
        double lonDistance = toRadians(lon2 - lon1);
        double a = sin(latDistance / 2) * sin(latDistance / 2)
                + cos(toRadians(lat1)) * cos(toRadians(lat2))
                * sin(lonDistance / 2) * sin(lonDistance / 2);
        double c = 2 * atan2(sqrt(a), sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = pow(distance, 2) + pow(height, 2);
        // distance = distance*distance + height*height;
        return sqrt(distance);
    }

    public double distance(Trkpt a, Trkpt b) {
        if (a == null) throw new NullPointerException("a must not be null");
        if (b == null) throw new NullPointerException("b must not be null");
        return distanceHaversine(a.getLat(), b.getLat(), a.getLon(), b.getLon(), a.getEle(), b.getEle());
    }
}
