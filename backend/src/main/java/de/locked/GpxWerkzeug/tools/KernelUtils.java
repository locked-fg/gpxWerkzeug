package de.locked.GpxWerkzeug.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.DoubleStream;

/**
 * currently class to hold unused code that I moved from somewhere else
 * TODO rename!
 */
public class KernelUtils {

    /**
     * initialize a evenly distributed kernel array
     *
     * @param k size of kernel
     * @return
     */
    private static double[] initKernel(int k) {
        if (k <= 0) throw new IllegalArgumentException("k must be >= 1 but was " + k);
        return DoubleStream.generate(() -> 1d / k)
                .limit(k).toArray();
    }

    /**
     * Smoothes a double array with a kernel of the given size and returns a smoothed copy.
     *
     * @param kernelSize
     * @param src
     * @return a smothed array
     */
    public static List<Double> smooth(int kernelSize, List<Double> src) {
        // TODO a lot of new arrays here
        var arr = src.toArray(new Double[]{});
        var smoothed = smooth(kernelSize, arr);
        return new ArrayList<Double>(Arrays.asList(smoothed));
    }

    /**
     * Smoothes a double array with a kernel of the given size and returns a smoothed copy.
     *
     * @param kernelSize
     * @param src
     * @return a smoothed array
     */
    public static Double[] smooth(final int kernelSize, final Double[] src) {
        if (kernelSize <= 0) throw new IllegalArgumentException("Kernel must be > 0 but was "+kernelSize);
        if (kernelSize %2 == 0) throw new IllegalArgumentException("Kernelsize must be odd but was "+kernelSize);

        var dst = Arrays.copyOf(src, src.length);
        if(kernelSize == 1){
            return dst;
        }

        var kernel = initKernel(kernelSize);
        var d = kernel.length / 2; // floors result
        for (int i = kernel.length - 2; i < src.length - kernel.length - 2; i++) {
            var v = 0.;
            for (int j = 0; j < kernel.length; j++) {
                // should SRC be used or DST (dst is already smoothed from the previous step)
                v += src[i - d + j] * kernel[j];
            }
            dst[i] = v;
        }
        return dst;
    }
}
