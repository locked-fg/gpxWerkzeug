package de.locked.GpxWerkzeug.tools;

import de.locked.GpxWerkzeug.gpx.Trkpt;
import de.locked.GpxWerkzeug.gpx.Trkseg;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GpxCleanerTest {
    static Trkseg seg2 = new Trkseg(Arrays.asList(
            new Trkpt(47.756445, 11.560081, 0d, new Date(0)), // d=0m
            new Trkpt(47.756092, 11.560060, 0d, new Date(0)) // d=39m
    ));
    static Trkseg seg6 = new Trkseg(Arrays.asList(
            new Trkpt(47.756445, 11.560081, 0d, new Date(0)), // d=0m
            new Trkpt(47.756092, 11.560060, 0d, new Date(0)), // d=39m
            new Trkpt(47.755695, 11.560113, 0d, new Date(0)), // d=44m
            new Trkpt(47.749253, 11.564244, 0d, new Date(0)), // d=780
            new Trkpt(47.748305, 11.564752, 0d, new Date(0)), // d=112
            new Trkpt(47.747757, 11.565224, 0d, new Date(0))  // d=70
    ));

//    @Test
//    void test_heightOutlier() throws JAXBException, URISyntaxException, ParserConfigurationException, SAXException {
//        var url = getClass().getClassLoader().getResource("height_outlier.gpx").toURI();
//        var gpx = GpxParser.toGPX(url);
//        var before = gpx.trk.trkseg.get(0).trkpt.stream().map(Trkpt::getEle).collect(Collectors.toList());
//        // 10: 706.23, 11: 700.46, 12: 706.71
//        assertEquals(700.46, before.get(11), 0.001);
//
//        var cleaned = GpxCleaner.cleanElevationOutliers(gpx);
//        var after = cleaned.trk.trkseg.get(0).trkpt.stream().map(Trkpt::getEle).collect(Collectors.toList());
//        // 10: 706.23, 11: 706.47, 12: 706.71
//        assertEquals(706.47, after.get(11), 0.001);
//    }

    @Test
    void test_cleanPauses() {
        Optional<Trkseg> s1 = GpxCleaner.collapsPauses(seg2, 1500);
        assertTrue(s1.isEmpty(), "should be empty");

        Optional<Trkseg> s2 = GpxCleaner.collapsPauses(seg2, 1);
        assertTrue(s2.isPresent(), "should not be empty");
        assertEquals(2, s2.get().size(), "there should be 2 points");

        Optional<Trkseg> s3 = GpxCleaner.collapsPauses(seg6, 1500);
        assertTrue(s3.isEmpty(), "should be empty");
        Optional<Trkseg> s31 = GpxCleaner.collapsPauses(seg6, 1);
        assertEquals(6, s31.get().size(), "there should be 6 points");

        Optional<Trkseg> s4 = GpxCleaner.collapsPauses(seg6, 40);
        assertTrue(s4.isPresent(), "should not be empty");
        assertEquals(seg6.getTrkpt().get(0).getLat(), s4.get().getTrkpt().get(0).getLat(), 0.00001);
        //
        assertEquals(seg6.getTrkpt().get(2).getLat(), s4.get().getTrkpt().get(1).getLat(), 0.00001);
        assertEquals(seg6.getTrkpt().get(3).getLat(), s4.get().getTrkpt().get(2).getLat(), 0.00001);
        assertEquals(seg6.getTrkpt().get(4).getLat(), s4.get().getTrkpt().get(3).getLat(), 0.00001);
        assertEquals(seg6.getTrkpt().get(5).getLat(), s4.get().getTrkpt().get(4).getLat(), 0.00001);
    }

    @Test
    void test_splitByDist() {
        List<Trkseg> tuple = GpxCleaner.splitByDist(seg2, 1500);
        assertEquals(1, tuple.size(), "tuple");
        assertEquals(2, tuple.get(0).size(), "tuple - seg 1");

        List<Trkseg> split_1 = GpxCleaner.splitByDist(seg6, 1500);
        assertEquals(1, split_1.size(), "1500m split");
        assertEquals(6, split_1.get(0).size(), "1500m split - seg 1");

        List<Trkseg> split_2 = GpxCleaner.splitByDist(seg6, 150);
        assertEquals(2, split_2.size(), "150m split");
        assertEquals(3, split_2.get(0).size(), "150m - seg 1");
        assertEquals(3, split_2.get(1).size(), "150m - seg 2");

        List<Trkseg> split_3 = GpxCleaner.splitByDist(seg6, 100);
        assertEquals(3, split_3.size(), "100m split");
        assertEquals(3, split_3.get(0).size(), "100m - seg 1");
        assertEquals(1, split_3.get(1).size(), "100m - seg 2");
        assertEquals(2, split_3.get(2).size(), "100m - seg 3");

    }
}