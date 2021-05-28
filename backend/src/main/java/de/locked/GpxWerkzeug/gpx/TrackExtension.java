package de.locked.GpxWerkzeug.gpx;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(namespace = "http://www.garmin.com/xmlschemas/GpxExtensions/v3")
public class TrackExtension {
    @XmlElement(name = "gpxx:DisplayColor")
    public final String displayColor = null;

    @Override
    public String toString() {
        return "TrackExtension{" +
                "displayColor='" + displayColor + '\'' +
                '}';
    }
}
