package de.locked.GpxWerkzeug;

import de.locked.GpxWerkzeug.geoJson.MultiLineString;
import de.locked.GpxWerkzeug.gpx.Gpx;
import de.locked.GpxWerkzeug.gpx.Trkseg;
import de.locked.GpxWerkzeug.tools.GeoJsonParser;
import de.locked.GpxWerkzeug.tools.GpxCleaner;
import de.locked.GpxWerkzeug.tools.GpxParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class Main {
    public static final String OUTFILE = "/static/multiline.json";

    Logger LOG = LogManager.getLogger(Main.class.getName());

    private void init(String[] dirs) throws IOException {
        LOG.info("start");
        var start = System.currentTimeMillis();

        // get all *.gpx in subdirs
        Stream<Path> gpxPathStream = getGpxPaths(dirs).parallel();
        Stream<Gpx> gpxStream = toGPX(gpxPathStream);
        Stream<Trkseg> trkSegStream = unpackTrkSegments(gpxStream);

        // beautify track segments (split / remove long jumps etc)
        Stream<Trkseg> splitStream = trkSegStream
                .flatMap(s -> GpxCleaner.splitByDist(s, 1000).stream())
                .flatMap(s -> GpxCleaner.cleanPauses(s, 5).stream());

        List<List<Double[]>> lines = convertToJsInput(splitStream);
//        writeGeoJson(lines, "./src/main/resources"+OUTFILE);

        long end = System.currentTimeMillis();
        LOG.info("GPX data processed in " + (end - start) + "ms");
    }

    private void writeGeoJson(List<List<Double[]>> lines, String target) throws IOException {
        // make GeoJSON
        String file = getClass().getResource(Main.OUTFILE).getFile();
        try (var w = new BufferedWriter(new FileWriter(file))) {
            var multiLineString = new MultiLineString(lines);
            var s = GeoJsonParser.toGeoString(multiLineString, true);
            w.write(s);
        }
    }

    private List<List<Double[]>> convertToJsInput(Stream<Trkseg> splitStream) {
        return splitStream
                    .map(seg -> segment2PointList(seg))
                    .collect(toList());
    }

    private List<Double[]> segment2PointList(Trkseg seg) {
        return seg
                .getTrkpt().stream()
                .map(p -> new Double[]{p.getLat(), p.getLon()})
                .collect(toList());
    }

    private Stream<Trkseg> unpackTrkSegments(Stream<Gpx> gpxStream) {
        return gpxStream
                    .map(Gpx::getTrk)
                    .flatMap(Optional::stream)
                    .flatMap(t -> t.getTrkseg().stream());
    }

    /**
     * Files.walk() with a stream result
     * @param v
     * @return
     */
    private Stream<Path> filesWalk(String v){
        try {
            return Files.walk(Paths.get(v));
        } catch (IOException e) {
            LOG.error("Unable to walk in "+v, e);
            return Stream.empty();
        }
    }

    private Stream<Path> getGpxPaths(String... paths) throws IOException {
        Stream<Path> gpxStream = Stream.of(paths)
                .parallel() // 24s -> 18s
                .flatMap(this::filesWalk)
                .filter(p -> p.toString().endsWith(".gpx"));
        return gpxStream;
    }

    private Stream<Gpx> toGPX(Stream<Path> paths) throws IOException {
        Stream<Gpx> gpxStream = paths
                .map(GpxParser::toGPX)
                .flatMap(Optional::stream);
        return gpxStream;
    }

    public static void main(String[] args) throws Exception {
        Configurator.initialize(null, new ConfigurationSource(new FileInputStream("./log4j2.xml")));
        args = args != null ? args : new String[]{"./data"};
        new Main().init(args);
    }
}
