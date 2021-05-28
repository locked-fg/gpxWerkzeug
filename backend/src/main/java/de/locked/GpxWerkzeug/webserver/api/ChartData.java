package de.locked.GpxWerkzeug.webserver.api;

public class ChartData {
    public final Double[] elevation;
    public final Double[] velocity;
    public final Double[] distance;

    public ChartData(Double[] elevation, Double[] velocity, Double[] distance) {
        this.elevation = elevation;
        this.velocity = velocity;
        this.distance = distance;
    }
}
