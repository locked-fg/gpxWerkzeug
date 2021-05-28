package de.locked.GpxWerkzeug.gpx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.EMPTY_LIST;

public class Trkseg {
    public List<Trkpt> trkpt = new ArrayList<>();

    public Trkseg() {}

    public Trkseg(List<Trkpt> trkpt) {
        this.trkpt = trkpt;
    }

    public List<Trkpt> getTrkpt() {
        return Collections.unmodifiableList(trkpt);
    }

    /**
     * @return amount of points in this segment
     */
    public int size() {
        return trkpt == null ? 0 : trkpt.size();
    }
}
