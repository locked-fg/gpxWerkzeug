package de.locked.GpxWerkzeug.webserver;

public class TracklistTuple {
    public final int id;
    public final String name;
    public final Long timestamp;

    public TracklistTuple(int id, String name, Long timestamp) {
        this.id = id;
        this.name = name;
        this.timestamp = timestamp;
    }
}