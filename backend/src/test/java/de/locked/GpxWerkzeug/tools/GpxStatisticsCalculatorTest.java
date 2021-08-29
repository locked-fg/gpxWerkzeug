package de.locked.GpxWerkzeug.tools;

import de.locked.GpxWerkzeug.gpx.Gpx;
import de.locked.GpxWerkzeug.gpx.Trkpt;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class GpxStatisticsCalculatorTest {

    private File getTestFile(String filename) {
        return new File(Objects.requireNonNull(
                getClass().getClassLoader().getResource(filename)).getFile()
        );
    }

    @Test
    void test_compute_simple() throws JAXBException, ParserConfigurationException, SAXException {
        var f = getTestFile("sample_gpx_1.0.gpx");
        var gpx = GpxParser.toGPX(f);
        var calc = new GpxStatisticsCalculator();

        calc.compute(gpx, 10);
        assertEquals(4.36, calc.stats.length, 0.01);
        calc.compute(gpx, 10);
        assertEquals(4.36 * 2, calc.stats.length, 0.01);
    }

    @Test
    void test_compute_tripDuration_gt0() throws JAXBException, ParserConfigurationException, SAXException {
        var f = getTestFile("sample-garmin.gpx");
        var gpx = GpxParser.toGPX(f);
        var calc = new GpxStatisticsCalculator();
        calc.computeTripDuration(gpx);
        assertEquals(5354000, calc.stats.timeTotal, "value was " + calc.stats.timeTotal);
    }

    @Test
    void test_compute_tripDuration_empty() {
        var calc = new GpxStatisticsCalculator();
        calc.computeTripDuration(new Gpx());
        assertEquals(0, calc.stats.timeTotal, "value was " + calc.stats.timeTotal);
    }

    @Test
    void test_compute_listsFilled() throws JAXBException, ParserConfigurationException, SAXException {
        var f = getTestFile("sample-garmin.gpx");
        var gpx = GpxParser.toGPX(f);
        var calc = new GpxStatisticsCalculator();
        calc.compute(gpx.trk.trkseg.get(0).trkpt, 10);

        assertFalse(calc.distanceSeries.isEmpty());
        assertFalse(calc.elevationSeries.isEmpty());
        assertFalse(calc.elevationDeltaSeries.isEmpty());
        assertFalse(calc.velocitySeries.isEmpty());
        assertFalse(calc.ascentSeries.isEmpty());
    }

    @Test
    void test_timedelta() {
        var calc = new GpxStatisticsCalculator();
        var a = new Trkpt(1d, 1d, 1d, new Date(0L));
        var b = new Trkpt(1d, 1d, 1d, new Date(1000L));
        var c = new Trkpt(1d, 1d, 1d, Optional.empty());
        assertTrue(calc.timeDelta(a, b).isPresent());
        assertTrue(calc.timeDelta(a, b).get() > 0);
        assertEquals(0d, calc.timeDelta(a, a).get(), 0.01);
        assertFalse(calc.timeDelta(a, c).isPresent());
        assertFalse(calc.timeDelta(c, c).isPresent());
    }

    @Test
    void test_computeMinMaxAvg_simpleTest() throws JAXBException, ParserConfigurationException, SAXException {
        var f = getTestFile("sample-garmin.gpx");
        var gpx = GpxParser.toGPX(f);
        var calc = new GpxStatisticsCalculator();
        calc.calc(gpx, 10);
        calc.computeMinMaxAvg();

        assertTrue(calc.stats.heightMin > 0);
        assertTrue(calc.stats.heightMax > 0);
        assertTrue(calc.stats.length > 0);
        assertTrue(calc.stats.timeTotal > 0);
        assertTrue(calc.stats.timeMoving > 0);
        assertTrue(calc.stats.elevationUp > 0);
        assertTrue(calc.stats.elevationDown < 0, "value was: " + calc.stats.elevationDown);
        assertTrue(calc.stats.ascentMin < 0, "value was: " + calc.stats.ascentMin);
        assertTrue(calc.stats.ascentMax > 0);
        assertTrue(calc.stats.ascentAvg > 0);
        // TODO this might be subject to cleaning
        // assertTrue(calc.stats.vMin > 0, "value was: "+calc.stats.vMin);
        assertTrue(calc.stats.vMin >= 0, "value was: " + calc.stats.vMin);
        assertTrue(calc.stats.vMax > 0);
        assertTrue(calc.stats.vAvg > 0);
    }

    @Test
    void test_getDistanceRunningSumArray() {
        var calc = new GpxStatisticsCalculator();
        calc.distanceSeries.add(0d);
        calc.distanceSeries.add(1d);
        calc.distanceSeries.add(2d);
        calc.distanceSeries.add(1d);
        var dists = calc.getDistanceRunningSumArray();
        assertEquals(0d, dists[0], 0.0001);
        assertEquals(1d, dists[1], 0.0001);
        assertEquals(3d, dists[2], 0.0001);
        assertEquals(4d, dists[3], 0.0001);
    }

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
