package de.locked.GpxWerkzeug.webserver;

// TODO convert to Java15 records
public class TracklistTuple{
    public final int id;
    public final String name;

    public TracklistTuple(int id, String name) {
        this.id = id;
        this.name = name;
    }
}