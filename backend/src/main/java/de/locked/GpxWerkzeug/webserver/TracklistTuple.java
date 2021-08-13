package de.locked.GpxWerkzeug.webserver;

// TODO convert to Java15 records
public class TracklistTuple{
    public final int id;
    public final String name;
    public final long timestamp;

    public TracklistTuple(int id, String name, long timestamp) {
        this.id = id;
        this.name = name;
        this.timestamp = timestamp;
    }
}