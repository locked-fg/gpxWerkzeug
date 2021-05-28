package de.locked.GpxWerkzeug.webserver;

import de.locked.GpxWerkzeug.gpx.Gpx;
import de.locked.GpxWerkzeug.tools.GpxParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

public class GpxScanner {
    private static final Logger LOG = LogManager.getLogger(GpxScanner.class.getName());

    Stream<Gpx> init(String[] dirs) throws IOException {
        LOG.info("start scanning fpr GPX traces");
        var start = System.currentTimeMillis();

        // get all *.gpx in subdirs
        Stream<Path> gpxPathStream = getGpxPaths(dirs).parallel();
        Stream<Gpx> gpxStream = toGPX(gpxPathStream);
        long end = System.currentTimeMillis();
        LOG.info("GPX data processed in " + (end - start) + "ms");

        return gpxStream;
    }

    /**
     * Files.walk() with a stream result
     * @param v
     * @return
     */
    private static Stream<Path> filesWalk(String v){
        try {
            LOG.debug("scanning directory: " + v);
            return Files.walk(Paths.get(v));
        } catch (IOException e) {
            LOG.error("Unable to scan: "+v, e);
            return Stream.empty();
        }
    }

    public static Stream<Path> getGpxPaths(String... paths) throws IOException {
        Stream<Path> gpxStream = Stream.of(paths)
                .parallel()
                .flatMap(GpxScanner::filesWalk)
                .filter(p -> p.toString().endsWith(".gpx"));
        return gpxStream;
    }

    private Stream<Gpx> toGPX(Stream<Path> paths) throws IOException {
        Stream<Gpx> gpxStream = paths
                .map(GpxParser::toGPX)
                .flatMap(Optional::stream);
        return gpxStream;
    }
}
