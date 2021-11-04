package de.locked.GpxWerkzeug.elevation;

import de.locked.GpxWerkzeug.gpx.Trkpt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;

public class HgtCache {
    private final static Logger LOG = LogManager.getLogger(HgtCache.class);
    // those filenames were already tested for not being there
    // caching those filenames saves a lot of negative disc lookups and saves a lot of time
    private final HashSet<String> nocacheFile = new HashSet<>();
    private final HashMap<String, SoftReference<short[][]>> cache = new HashMap<>();
    private final File hgtBaseDir;
    private int requests = 0;
    private int cacheHit = 0;

    public HgtCache(File hgtBaseDir) throws IOException {
        if (hgtBaseDir == null) throw new NullPointerException();
        if (!hgtBaseDir.exists()) throw new IOException(hgtBaseDir + " does not exist");
        if (!hgtBaseDir.isDirectory()) throw new IOException(hgtBaseDir + " not a directory");
        this.hgtBaseDir = hgtBaseDir;
    }

    public Optional<Short> getElevation(Trkpt p) throws IOException {
        if (requests++ % 10000 == 0)
            LOG.info("Hgt Cache hit requests/hit: {}/{}. HitRate: {}", requests, cacheHit, cacheHit * 1d / requests);
        var height = getMatrix(p)
                .map(m -> HgtReader.getHeight(m, p.getLat(), p.getLon()))
                .filter(v -> v != Short.MIN_VALUE); // MIN_VALUE means: no data (defined in SRTM/HGT Format)
        if (height.isPresent()) cacheHit++;
        return height;
    }

    /**
     * Retrieves the matrix from cache or loads it from disk -- IF the file is there!
     * A HGTfFile NEED NOT exists!
     * <p>
     * As this can trigger a disc read I better synchronize this.
     * <p>
     * Keep in mind that very close requests can (and will) come here in parallel. So it makes sense to synchronize
     * here. Otherwise, the same HGT file will be read multiple times. Of course by the cost that a file read blocks
     * all getMatrix()-calls.
     *
     * @param p
     * @return a short-matrix of elevation values
     * @throws IOException
     */
    private synchronized Optional<short[][]> getMatrix(Trkpt p) throws IOException {
        var fileBasename = HgtReader.makeFileName(p.getLat(), p.getLon());
        if (nocacheFile.contains(fileBasename)) return Optional.empty();

        var softReference = cache.get(fileBasename);
        if (softReference == null || softReference.get() == null) { // not yet cached, so cache!
            var matrixOptional = loadMatrixFromDisk(fileBasename);
            matrixOptional.map(data -> cache.put(fileBasename, new SoftReference<>(data)));
            return matrixOptional;
        } else {
            return Optional.ofNullable(softReference.get());
        }
    }

    /**
     * loads a matrix from disc and puts it into the cache
     *
     * @param fileBasename
     * @return a short-matrix of elevation values
     * @throws IOException
     */
    private Optional<short[][]> loadMatrixFromDisk(String fileBasename) throws IOException {
        LOG.info("(maybe) reading HGT {}", fileBasename);
        try {
            var src = new File(hgtBaseDir, fileBasename + ".zip");
            if (!src.exists()) {
                LOG.info("HGT.zip not found. Skipping. {}", src);
                nocacheFile.add(fileBasename);
                return Optional.empty();
            }

            File hgtFile = HgtReader.uncompressTempFile(src);
            var matrix = HgtReader.hgtFileToMatrix(hgtFile);
            hgtFile.delete();
            cache.put(fileBasename, new SoftReference<>(matrix));
            return Optional.of(matrix);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

}
