package de.locked.GpxWerkzeug.gpx;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.Date;

public final class Trkpt {
    @XmlAttribute
    private final Double lat;
    @XmlAttribute
    private final Double lon;
    @XmlElement
    private final Double ele;
    @XmlElement
    private final Date time;

    public Trkpt(Double lat, Double lon, Double ele, Date time) {
        this.lat = lat;
        this.lon = lon;
        this.ele = ele;
        this.time = time;
    }

    public Trkpt() {
        this.lat = null;
        this.lon = null;
        this.ele = null;
        this.time = null;
    }

    public Double getLat() {
        return lat;
    }

    public Double getLon() {
        return lon;
    }

    public Double getEle() {
        return ele == null ? 0 : ele;
    }

    public Date getTime() {
        return time;
    }

    @Override
    public final String toString() {
        return "Trkpt{" +
                "lat=" + lat +
                ", lon=" + lon +
                ", ele=" + ele +
                ", time=" + time +
                '}';
    }
}
