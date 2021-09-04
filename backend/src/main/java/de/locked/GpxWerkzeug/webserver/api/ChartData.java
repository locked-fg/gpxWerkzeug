package de.locked.GpxWerkzeug.webserver.api;

public class ChartData {
    public final Double[] elevation;
    public final Double[] velocity;
    public final Double[] distance;
    public final Double[] ascend;

    public ChartData(Double[] elevation, Double[] velocity, Double[] distance, Double[] ascend) {
        this.elevation = elevation;
        this.velocity = velocity;
        this.distance = distance;
        this.ascend = ascend;
    }
}
