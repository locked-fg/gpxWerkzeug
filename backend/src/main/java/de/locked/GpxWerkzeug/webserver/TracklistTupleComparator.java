package de.locked.GpxWerkzeug.webserver;

import java.util.Comparator;

public class TracklistTupleComparator implements Comparator<TracklistTuple> {
    private static final Long DEFAULT_TIMESTAMP = 0L;

    /**
     * Default: compare by timestamp.
     * if one has no timestamp, replace according TS by 0
     * if both have no timestamp, compare by name
     *
     * @param o1
     * @param o2
     * @return
     */
    @Override
    public int compare(TracklistTuple o1, TracklistTuple o2) {
        if (o1.timestamp == null && o2.timestamp == null){
            return o1.name.compareTo(o2.name);
        } else {
            var o1t = o1.timestamp != null ? o1.timestamp : DEFAULT_TIMESTAMP;
            var o2t = o2.timestamp != null ? o2.timestamp : DEFAULT_TIMESTAMP;
            return Long.compare(o1t, o2t);
        }
    }
}
