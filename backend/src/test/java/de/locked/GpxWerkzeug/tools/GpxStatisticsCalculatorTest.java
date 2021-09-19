package de.locked.GpxWerkzeug.tools;

import de.locked.GpxWerkzeug.gpx.Gpx;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class GpxStatisticsCalculatorTest {

    private File getTestFile(String filename) {
        return new File(Objects.requireNonNull(
                getClass().getClassLoader().getResource(filename)).getFile()
        );
    }

    @Test
    void test_smoothSeries() throws JAXBException, ParserConfigurationException, SAXException {
        var f = getTestFile("sample-garmin.gpx");
        var gpx = GpxParser.toGPX(f);
        var calc = new GpxStatisticsCalculator(gpx, 1, 1);
        var before = calc.ascentSeries.stream().mapToDouble(Double::doubleValue).summaryStatistics();

        calc.smoothSeries(3);
        var after = calc.ascentSeries.stream().mapToDouble(Double::doubleValue).summaryStatistics();
        assertTrue(before.getAverage() != after.getAverage(), "stats before / after: " + before.getAverage() + "/" + after.getAverage());
    }

    @Test
    void test_compute_tripDuration_gt0() throws JAXBException, ParserConfigurationException, SAXException {
        var f = getTestFile("sample-garmin.gpx");
        var gpx = GpxParser.toGPX(f);
        var calc = new GpxStatisticsCalculator();
        calc.computeStatsTimeTotal(gpx);
        assertEquals(5354000, calc.stats.timeTotal, "value was " + calc.stats.timeTotal);
    }

    @Test
    void test_compute_tripDuration_empty() {
        var calc = new GpxStatisticsCalculator(new Gpx(), 1, 1);
        assertEquals(0, calc.stats.timeTotal, "value was " + calc.stats.timeTotal);
    }

    @Test
    void test_compute_listsFilled() throws JAXBException, ParserConfigurationException, SAXException {
        var f = getTestFile("sample-garmin.gpx");
        var gpx = GpxParser.toGPX(f);
        var calc = new GpxStatisticsCalculator(gpx, 1, 1);
        // calc.computeSeries(gpx);

        assertFalse(calc.distanceSeries.isEmpty());
        assertFalse(calc.timeSeries.isEmpty());
        assertFalse(calc.elevationSeries.isEmpty());
        assertFalse(calc.elevationDeltaSeries.isEmpty());
        assertFalse(calc.velocitySeries.isEmpty());
        assertFalse(calc.ascentSeries.isEmpty());
    }


    @Test
    void test_computeMinMaxAvg_simpleTest() throws JAXBException, ParserConfigurationException, SAXException {
        var f = getTestFile("sample-garmin.gpx");
        var gpx = GpxParser.toGPX(f);
        var calc = new GpxStatisticsCalculator(gpx, 10, 1);
        calc.computeStatsMinMaxAvg();

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


}
