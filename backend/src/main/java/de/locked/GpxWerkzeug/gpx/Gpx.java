package de.locked.GpxWerkzeug.gpx;

import javax.xml.bind.annotation.*;
import java.util.Optional;

@XmlRootElement
public class Gpx {

    @XmlAttribute
    public final String creator = null;
    @XmlAttribute
    public final String version = null;
    public final Metadata metadata = null;
    @XmlElement(name = "trk")
    public Trk trk = null;

    public Optional<Trk> getTrk() {
        return Optional.ofNullable(trk);
    }

    @Override
    public String toString() {
        return "Gpx{" +
                "creator='" + creator + '\'' +
                ", version='" + version + '\'' +
                ", metadata=" + metadata +
                ", trk=" + trk +
                '}';
    }
}
