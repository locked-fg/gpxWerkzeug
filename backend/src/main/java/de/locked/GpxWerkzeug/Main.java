package de.locked.GpxWerkzeug;

import de.locked.GpxWerkzeug.gpx.Gpx;
import de.locked.GpxWerkzeug.tools.*;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;

public class Main {
    public void exec() throws JAXBException, ParserConfigurationException, SAXException {
        var f = new File("c:/Users/info/Documents/Programming/gpxWerkzeug/backend/src/test/resources/ascend-test.gpx");
        var gpx = GpxParser.toGPX(f);

        pp(gpx);
        System.out.println();
        GpxCleaner.collapsPauses(gpx, 10);
        pp(gpx);

    }

    private void pp(Gpx gpx) {
        var a = new GpxStatisticsCalculator(gpx, 1, 5);
        var asc = a.getAscendArray();

        pp("asc: ", asc);
        pp("len: ", a.getDistanceRunningSumArray());
        pp("hei: ", a.getElevationArray());

        var min = Arrays.stream(asc).min(Double::compare).get();
        var max = Arrays.stream(asc).max(Double::compare).get();
        System.out.printf("min: %5.1f | max: %5.1f%n", min, max);
    }

    private void pp(String a, Double[] in) {
        // var arr = Arrays.asList(in).subList(40, in.length-1);
        var arr = in;
        System.out.print(a);
        for (Double d: arr) {
            var f = String.format("%5.1f", d);
            System.out.print(f+" ");
        }
        System.out.println();
    }


    public static void main(String[] args) throws Exception {
//        Configurator.initialize(null, new ConfigurationSource(new FileInputStream("./log4j2.xml")));
        new Main().exec();
    }
}
