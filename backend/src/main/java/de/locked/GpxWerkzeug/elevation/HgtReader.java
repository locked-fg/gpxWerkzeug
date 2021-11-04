package de.locked.GpxWerkzeug.elevation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URISyntaxException;
import java.util.zip.ZipFile;

/**
 * var hgt = new File("./src/main/resources/N47E011.hgt");
 * var zip = new File("./src/main/resources/N47E011.zip");
 * System.out.println(hgt.getAbsolutePath() + " " + hgt.exists());
 * System.out.println(zip.getAbsolutePath() + " " + zip.exists());
 * // https://elevationmap.net/?latlng=(11.574481,47.7296917)
 * <p>
 * var lon = 11.574481;
 * var lat = 47.7296917;
 * var filename = makeFileName(lat, lon);
 * var hgt = new File("./src/main/resources/" + filename + ".hgt");
 * var zip = new File("./src/main/resources/" + filename + ".zip");
 * <p>
 * var height1 = getHeight(hgt, lat, lon);
 * System.out.println(height1);
 * <p>
 * var array = hgtFileToMatrix(hgt);
 * var height2 = getCachedHeight(array, lat, lon);
 * System.out.println(height2);
 * <p>
 * var tmpFile = uncompressTempFile(zip);
 * var height3 = getHeight(tmpFile, lat, lon);
 * System.out.println(height3);
 */
public class HgtReader {
    private final static Logger LOG = LogManager.getLogger(HgtReader.class);
    private final static int n = 3601;

    /**
     * uncompresses a ziped hgt to a temporary HGT file which should get deleted after program exit
     *
     * @param zipFile
     * @return
     * @throws URISyntaxException
     * @throws IOException
     */
    public static File uncompressTempFile(File zipFile) throws URISyntaxException, IOException {
        LOG.info("uncompressing {}", zipFile);
        var fileBasename = zipFile.getName().replace(".zip", "");
        var target = File.createTempFile(fileBasename, ".hgt");
        target.deleteOnExit();

        var zip = new ZipFile(zipFile);
        var buffer = 1024 * 1024;
        try (var binp = new BufferedInputStream(zip.getInputStream(zip.getEntry(fileBasename + ".hgt")), buffer);
             var boutp = new BufferedOutputStream(new FileOutputStream(target), buffer)) {
            binp.transferTo(boutp);
        }
        return target;
    }

    public static short getHeight(short[][] array, double lat, double lon) {
        var y = lat % 1;
        var x = lon % 1;
        var iY = Math.min(Math.round(n - (y * n)), n - 1); // rounding can cause indexOutOfBounds
        var iX = Math.min(Math.round(x * n), n - 1);
        return array[(int) iX][(int) iY];
    }

    public static short[][] hgtFileToMatrix(File hgt) throws IOException {
        var buffer = n * n * 2; // the full file
        var matrix = new short[n][n];
        try (var bis = new BufferedInputStream(new FileInputStream(hgt), buffer)) {
            int ch1, ch2;
            for (int y = 0; y < n; y++) {
                for (int x = 0; x < n; x++) {
                    ch1 = bis.read();
                    ch2 = bis.read();
                    var height = (short) ((ch1 << 8) + (ch2));
                    matrix[x][y] = height;
                }
            }
            return matrix;
        }
    }

    private static int getIndex(double lat, double lon) { // lat = y
        var y = lat % 1;
        var x = lon % 1;
        var iY = Math.round(n - (y * n));
        var iX = Math.round(x * n);

        var index = (iY * n) + iX;
        return (int) index;
    }

    public static int getHeight(File hgt, double lat, double lon) throws IOException {
        var index = getIndex(lat, lon);
        int buffer = index * 2 * 8;
        try (var bis = new BufferedInputStream(new FileInputStream(hgt), buffer)) {
            bis.skip(index * 2); // *2 because 2 bytes per number
            var ch1 = bis.read();
            var ch2 = bis.read();
            short height = (short) ((ch1 << 8) + (ch2));
            return height;
        }
    }

    /**
     * creates the BaseName like 'N47E011'
     *
     * @param lat
     * @param lon
     * @return
     */
    public static String makeFileName(double lat, double lon) {
        var NS = lat > 0 ? "N" : "S";
        var WE = lon > 0 ? "E" : "W";
        return String.format("%s%02.0f%s%03.0f", NS, Math.floor(Math.abs(lat)), WE, Math.floor(Math.abs(lon)));
    }
}
