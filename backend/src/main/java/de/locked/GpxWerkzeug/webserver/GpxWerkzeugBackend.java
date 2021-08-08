package de.locked.GpxWerkzeug.webserver;

import de.locked.GpxWerkzeug.gpx.Gpx;
import de.locked.GpxWerkzeug.tools.GpxCleaner;
import de.locked.GpxWerkzeug.tools.GpxParser;
import de.locked.GpxWerkzeug.tools.GpxStatistics;
import de.locked.GpxWerkzeug.tools.GpxStatisticsCalculator;
import de.locked.GpxWerkzeug.webserver.api.ChartData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.reverse;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;

@SpringBootApplication
@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class GpxWerkzeugBackend implements ApplicationRunner {
    // TODO move to some config
    private List<String> gpxSrcDir = List.of(
            "C://Users/info_000/Pictures/DigiCam Raw/",
            "C://Users/info_000/Pictures/GPS/",
            "F://info_000/Pictures/DigiCam Raw/",
            "Z://Onedrive-Backup-sync/encrypted/DigiCam Raw/");
    private static final int MIN_METERS_FOR_MOVEMENT = 10;
    private static final int MAX_DIST_METERS_BEFORE_SPLIT = 1000;

    // real vars
    private static final Logger LOG = LogManager.getLogger(GpxWerkzeugBackend.class);
    // the "DB"
    private List<Path> gpxPaths = EMPTY_LIST;
    // the cache
    private final WeakHashMap<Path, Gpx> gpxCache = new WeakHashMap<>();

    public static void main(String[] args) throws IOException {
        SpringApplication.run(GpxWerkzeugBackend.class, args);
    }

    /**
     * Runs after startup
     *
     * @param args
     * @throws Exception
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        LOG.info("init SpringBoot");
        List<String> gpxSrc = args.getOptionValues("gpxSrc");
        if (!isEmpty(gpxSrc)) {
            gpxSrcDir = gpxSrc;
        }
//        LOG.trace("A TRACE Message");
//        LOG.debug("A DEBUG Message");
//        LOG.info("An INFO Message");
//        LOG.warn("A WARN Message");
//        LOG.error("An ERROR Message");
        gpxPaths = GpxScanner.getGpxPaths(gpxSrcDir).collect(toList());
    }

    @GetMapping("/hello")
    public String hello(@RequestParam(value = "name", defaultValue = "World") String name) {
        return String.format("Hello %s!", name);
    }

    /**
     * @return List of [id|name] tuples
     */
    @GetMapping("/api/getTracklist")
    public List<TracklistTuple> getTracklist() {
        var out = new ArrayList<TracklistTuple>();
        for (int i = 0; i < gpxPaths.size(); i++) {
            var name = gpxPaths.get(i).getFileName().toString().replace(".gpx", "");
            out.add(new TracklistTuple(i, name));
        }
        out.sort(Comparator.comparing(o -> o.name));
        reverse(out);
        return out;
    }

    @GetMapping("/api/getPolyLine")
    public List<List<Double[]>> getPolyLine(@RequestParam(value = "id") int id, HttpServletResponse response) {
        if (id < 0 || id >= gpxPaths.size()) response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return pathToPolyline(gpxPaths.get(id));
    }

    @GetMapping("/api/getAllTracksAsPolyLine")
    public List<List<Double[]>> getAllTracksAsPolyLine(HttpServletResponse response) {
        return gpxPaths.stream().parallel()
                .map(this::pathToPolyline)
                .flatMap(Collection::stream)
                .collect(toList());
    }

    @GetMapping("/api/getStatistics")
    public GpxStatistics getStatistics(@RequestParam(value = "id") int id, HttpServletResponse response) {
        if (id < 0 || id >= gpxPaths.size()) response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return getCleanedGpx(gpxPaths.get(id))
                .map(g -> new GpxStatisticsCalculator().calc(g, MIN_METERS_FOR_MOVEMENT).stats).orElse(null);
    }

    //    @GetMapping("/api/getChartData")
//    public Double[][] getChartData(@RequestParam(value = "id") int id, HttpServletResponse response) {
//        if (id < 0 || id >= gpxPaths.size()) response.setStatus(HttpServletResponse.SC_NOT_FOUND);
//        return getCleanedGpx(gpxPaths.get(id))
//                .map(g -> new GpxStatisticsCalculator().calc(g, MIN_METERS_FOR_MOVEMENT))
//                .map(s -> new Double[][]{
//                        s.getElevationArray(),
//                        s.getVelocityArray(),
//                        s.getDistanceRunningSumArray()
//                })
//                .orElse(null);
//    }
//
    @GetMapping("/api/getChartData")
    public ChartData getChartData(@RequestParam(value = "id") int id, HttpServletResponse response) {
        if (id < 0 || id >= gpxPaths.size()) response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return getCleanedGpx(gpxPaths.get(id))
                .map(g -> new GpxStatisticsCalculator().calc(g, MIN_METERS_FOR_MOVEMENT))
                .map(s -> new ChartData(
                        s.getElevationArray(),
                        s.getVelocityArray(),
                        s.getDistanceRunningSumArray()
                ))
                .orElse(null);
    }

    private Optional<Gpx> getCleanedGpx(Path p) {
        var gpx = gpxCache.get(p);
        if (gpx != null) {
            return Optional.of(gpx);
        } else {
            var gpxOptional = GpxParser.toGPX(p)
                    // .map(gpx -> GpxCleaner.cleanPauses(gpx, MIN_METERS_FOR_MOVEMENT))
                    .map(GpxCleaner::cleanElevationOutliers)
                    .map(gpxTrack -> GpxCleaner.splitByDist(gpxTrack, MAX_DIST_METERS_BEFORE_SPLIT));
            gpxCache.put(p, gpxOptional.orElse(null));
            return gpxOptional;
        }
    }

    private List<List<Double[]>> pathToPolyline(Path path) {
        return getCleanedGpx(path)
                .map(gpx -> gpx.trk.trkseg.stream()
                        .map(s -> s.trkpt.stream()
                                .map(p -> new Double[]{p.getLat(), p.getLon()})
                                .collect(toList()))
                        .collect(toList()))
                .orElse(null);
    }
}

