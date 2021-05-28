package de.locked.GpxWerkzeug.gpx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class Trk {
    public String name = null;
    public Extensions extensions = null;
    public List<Trkseg> trkseg = new ArrayList<>();

    public List<Trkseg> getTrkseg() {
        return Collections.unmodifiableList(trkseg);
    }

    @Override
    public String toString() {
        return "Trk{" +
                "name='" + name + '\'' +
                ", extensions=" + extensions +
                ", trkseg=" + trkseg +
                '}';
    }
}
