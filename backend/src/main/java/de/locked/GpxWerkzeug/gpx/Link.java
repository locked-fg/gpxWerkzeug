package de.locked.GpxWerkzeug.gpx;

import javax.xml.bind.annotation.XmlAttribute;

public class Link {
    @XmlAttribute
    public final String href = null;
    public final String text = null;

    @Override
    public String toString() {
        return "Link{" +
                "href='" + href + '\'' +
                ", text='" + text + '\'' +
                '}';
    }
}
