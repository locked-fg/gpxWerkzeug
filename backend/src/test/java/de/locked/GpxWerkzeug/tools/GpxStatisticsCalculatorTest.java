package de.locked.GpxWerkzeug.tools;

import de.locked.GpxWerkzeug.gpx.Trkpt;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GpxStatisticsCalculatorTest {
    @Test
    void test_ascent() {
        var a = new Trkpt(47.756445, 11.5608, 0d, new Date(0));
        var b = new Trkpt(47.756445, 11.56093378, 0d, new Date(0)); // d=10.00065m
        var c = new Trkpt(47.756445, 11.56093378, 10d, new Date(0));
        var d = new Trkpt(47.756445, 11.56093378, 6.66d, new Date(0));

        var d_ab = GpxStatisticsCalculator.d(a, b); // d=10.00065m
        var d_ac = GpxStatisticsCalculator.d(a, c); // d=14.14m
        var d_bc = GpxStatisticsCalculator.d(b, c); // d=10m <- height

        var a_ab = GpxStatisticsCalculator.ascent(a, b);
        assertEquals(0, a_ab, .01);

        var a_ac = GpxStatisticsCalculator.ascent(a, c);
        assertEquals(100, a_ac, .01);

        var a_ad = GpxStatisticsCalculator.ascent(a, d);
        assertEquals(66.6, a_ad, .01);
    }
}
