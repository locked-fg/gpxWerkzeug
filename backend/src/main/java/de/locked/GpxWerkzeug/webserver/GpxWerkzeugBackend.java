package de.locked.GpxWerkzeug.webserver;

import de.locked.GpxWerkzeug.gpx.Gpx;
import de.locked.GpxWerkzeug.tools.GpxCleaner;
import de.locked.GpxWerkzeug.tools.GpxParser;
import de.locked.GpxWerkzeug.tools.GpxStatistics;
import de.locked.GpxWerkzeug.tools.GpxStatisticsCalculator;
import de.locked.GpxWerkzeug.webserver.api.ChartData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.AbstractMap.SimpleEntry;
import java.util.*;

import static java.util.stream.Collectors.toList;

@SpringBootApplication
@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class GpxWerkzeugBackend implements ApplicationRunner {
    @Value("${gpxSrc}")
    private List<String> gpxSrcDir = Collections.EMPTY_LIST;
    private static final int MIN_METERS_FOR_MOVEMENT = 10;
    private static final int KERNEL_SIZE = 3;
    private static final int MAX_DIST_METERS_BEFORE_SPLIT = 1000;

    // real vars
    private static final Logger LOG = LogManager.getLogger(GpxWerkzeugBackend.class);
    // the "DB"
    private HashMap<Integer, Path> gpxDB = new HashMap<>();
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
//        LOG.trace("A TRACE Message");
//        LOG.debug("A DEBUG Message");
//        LOG.info("An INFO Message");
//        LOG.warn("A WARN Message");
//        LOG.error("An ERROR Message");
        loadPaths();
    }

    private void loadPaths() throws IOException {
        LOG.info("Loading paths");
        var gpxPaths = GpxScanner.getGpxPaths(gpxSrcDir).collect(toList());
        for (int i = 0; i < gpxPaths.size(); i++) { // make <ID->GPX>-Tuples
            gpxDB.put(i, gpxPaths.get(i));
        }
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
        var tracklist = gpxDB.entrySet().stream()
                .parallel()
                .map(e -> new SimpleEntry<>(e.getKey(), getCleanedGpx(e.getValue())))
                .filter(e -> e.getValue().isPresent())
                .map(e -> {
                    try {
                        var index = e.getKey();
                        var timestamp = e.getValue().get().getDate()
                                .map(Date::getTime)
                                .orElse(null);
                        var name = gpxDB.get(index).getFileName().toString().replace(".gpx", "");
                        return new TracklistTuple(index, name, timestamp);
                    } catch (Throwable t){
                        LOG.error("error in e: {}", e);
                        throw t;
                    }
                })
                .sorted(new TracklistTupleComparator())
                .collect(toList());
        Collections.reverse(tracklist);
        LOG.info("tracklist size: {}", tracklist.size());
        return tracklist;
    }

    @GetMapping("/api/getPolyLine")
    public List<List<Double[]>> getPolyLine(@RequestParam(value = "id") int id, HttpServletResponse response) {
        if (!gpxDB.containsKey(id)) response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return pathToPolyline(gpxDB.get(id));
    }

    @GetMapping("/api/getAllTracksAsPolyLine")
    public List<List<Double[]>> getAllTracksAsPolyLine(HttpServletResponse response) {
        return gpxDB.values().stream().parallel()
                .map(this::pathToPolyline)
                .flatMap(Collection::stream)
                .collect(toList());
    }

    @GetMapping("/api/getStatistics")
    public GpxStatistics getStatistics(@RequestParam(value = "id") int id, HttpServletResponse response) {
        if (!gpxDB.containsKey(id)) response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return getCleanedGpx(gpxDB.get(id))
                .map(g -> new GpxStatisticsCalculator(g, MIN_METERS_FOR_MOVEMENT, KERNEL_SIZE).stats)
                    .orElse(null);
    }

    @GetMapping("/api/getChartData")
    public ChartData getChartData(@RequestParam(value = "id") int id, HttpServletResponse response) {
        if (!gpxDB.containsKey(id)) response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return getCleanedGpx(gpxDB.get(id))
                .map(g -> new GpxStatisticsCalculator(g, MIN_METERS_FOR_MOVEMENT, KERNEL_SIZE))
                .map(s -> new ChartData(
                            s.getElevationArray(),
                            s.getVelocityArray(),
                            s.getDistanceRunningSumArray(),
                            s.getAscendArray()
                ))
                .orElse(null);
    }

    private Optional<Gpx> getCleanedGpx(Path p) {
        var cached = gpxCache.get(p);
        if (cached != null) {
            LOG.info("no GPXCache entry for path {}", p);
            return Optional.of(cached);
        } else {
            var gpxOptional = GpxParser.toGPX(p)
                    .map(gpx -> GpxCleaner.collapsPauses(gpx, MIN_METERS_FOR_MOVEMENT))
                    .map(gpxTrack -> GpxCleaner.splitByDist(gpxTrack, MAX_DIST_METERS_BEFORE_SPLIT));
            gpxOptional.ifPresent(v -> gpxCache.put(p, v));
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

