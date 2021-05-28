package de.locked.GpxWerkzeug.gpx;

import java.util.Date;

public class Metadata {
    public final String name = null;
    public final String desc = null;
    public final String author = null;
    public final String copyright = null;
    public final Link link = null;
    public final Date time = null;
    public final String keywords = null;
    public final String bounds = null;
    public final String extensions = null;

    @Override
    public String toString() {
        return "Metadata{" +
                "name='" + name + '\'' +
                ", desc='" + desc + '\'' +
                ", author='" + author + '\'' +
                ", copyright='" + copyright + '\'' +
                ", link='" + link + '\'' +
                ", time='" + time + '\'' +
                ", keywords='" + keywords + '\'' +
                ", bounds='" + bounds + '\'' +
                ", extensions='" + extensions + '\'' +
                '}';
    }
}
