package de.locked.GpxWerkzeug.tools;

import de.locked.GpxWerkzeug.gpx.Trkpt;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class TrkptUtilsTest {

    @Test
    void test_timedelta() {
        var a = new Trkpt(1d, 1d, 1d, new Date(0L));
        var b = new Trkpt(1d, 1d, 1d, new Date(1000L));
        var c = new Trkpt(1d, 1d, 1d, Optional.empty());
        assertTrue(TrkptUtils.timeDelta(a, b).isPresent());
        assertTrue(TrkptUtils.timeDelta(a, b).get() > 0);
        assertEquals(0d, TrkptUtils.timeDelta(a, a).get(), 0.01);
        assertFalse(TrkptUtils.timeDelta(a, c).isPresent());
        assertFalse(TrkptUtils.timeDelta(c, c).isPresent());
    }


    @Test
    void test_ascent() {
        var a = new Trkpt(47.756445, 11.5608, 0d, new Date(0));
        var b = new Trkpt(47.756445, 11.56093378, 0d, new Date(0)); // d=10.00065m
        var c = new Trkpt(47.756445, 11.56093378, 10d, new Date(0));
        var d = new Trkpt(47.756445, 11.56093378, 6.66d, new Date(0));

        var d_ab = TrkptUtils.DIST.distance(a, b); // d=10.00065m
        var d_ac = TrkptUtils.DIST.distance(a, c); // d=14.14m
        var d_bc = TrkptUtils.DIST.distance(b, c); // d=10m <- height

        var a_ab = TrkptUtils.ascent(a, b);
        assertEquals(0, a_ab, .01);

        var a_ac = TrkptUtils.ascent(a, c);
        assertEquals(100, a_ac, .01);

        var a_ad = TrkptUtils.ascent(a, d);
        assertEquals(66.6, a_ad, .01);
    }
}
