package de.locked.GpxWerkzeug.gpx;

import javax.xml.bind.annotation.XmlElement;

public class Extensions {
    // TODO any way to get rid of the namespace here?
    @XmlElement(name = "gpxx:TrackExtension")
    public final TrackExtension trackExtension = null;

    @Override
    public String toString() {
        return "Extensions{" +
                "trackExtension=" + trackExtension +
                '}';
    }
}
