package de.locked.GpxWerkzeug.gpx;

import de.locked.GpxWerkzeug.tools.GpxParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.Objects;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

public class GpxParserTest {
    private SimpleDateFormat sdf;

    private File getTestFile(String filenme){
        return new File(Objects.requireNonNull(
                getClass().getClassLoader().getResource(filenme)).getFile()
        );
    }

    @BeforeEach
    public void setUp() {
        sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));
    }

    @Test
    public void testTime(){
        try {
            var gpx = GpxParser.toGPX(getTestFile("sample_gpx_1.0.gpx"));
            assertNotNull(gpx.time);
            assertEquals(sdf.parse("2011-01-04T14:41:51Z"), gpx.time);
        } catch (JAXBException | ParserConfigurationException | SAXException | ParseException e) {
            fail("Error parsing sample gpx");
        }

    }

    @Test
    public void parseUTF8_BOM_Test(){
        try {
            GpxParser.toGPX(getTestFile("sample_gpx_1.1_UTF8_BOM.gpx"));
        } catch (JAXBException | ParserConfigurationException | SAXException e) {
            fail("Error reading UTF8 BOM file", e);
        }
        try {
            GpxParser.toGPX(getTestFile("sample_gpx_1.1_UTF8.gpx"));
        } catch (JAXBException | ParserConfigurationException | SAXException e) {
            fail("Error reading UTF8 file", e);
        }
    }

    @Test
    public void parseFileTest_gpx_1_1() throws ParseException {
        try {
            File gpxFile = getTestFile("sample_gpx_1.1.gpx");
            Gpx gpx = GpxParser.toGPX(gpxFile);

            assertNotNull(gpx);
            assertEquals("Dakota 20", gpx.creator);
            assertEquals("1.1", gpx.version);

            // Metadata
            assertNotNull(gpx.metadata, "metadata not set");
            assertEquals(sdf.parse("2013-05-16T18:36:51Z"), gpx.metadata.time);
            // Metadata/Link
            assertNotNull(gpx.metadata.link);
            assertEquals("http://www.garmin.com",gpx.metadata.link.href);
            assertEquals("Garmin International", gpx.metadata.link.text);
            // Trk
            assertNotNull(gpx.trk);
            assertEquals("01-MAI-12 15:18:07", gpx.trk.name);
            assertNotNull(gpx.trk.extensions);
            assertNotNull(gpx.trk.extensions.trackExtension, "gpx.trk.extensions.trackExtension missing");
            // Trk/Extensions
            assertEquals("Black", gpx.trk.extensions.trackExtension.displayColor);
            //Trk/trkseg
            assertNotNull(gpx.trk.trkseg);
            assertEquals(2, gpx.trk.trkseg.size());
            //Trk/trkseg(0)
            assertNotNull(gpx.trk.trkseg.get(0).trkpt);
            assertEquals(3, gpx.trk.trkseg.get(0).trkpt.size());
            //Trk/trkseg(0)/trkpt(0)
            assertEquals(47.7345581912, gpx.trk.trkseg.get(0).trkpt.get(0).getLat(), 0.0000001);
            assertEquals(11.5732874069, gpx.trk.trkseg.get(0).trkpt.get(0).getLon(), 0.0000001);
            assertEquals(645.66, gpx.trk.trkseg.get(0).trkpt.get(0).getEle(), 0.001);
            assertEquals(sdf.parse("2012-05-01T12:32:43Z"), gpx.trk.trkseg.get(0).trkpt.get(0).getTime().get());
            //Trk/trkseg(0)/trkpt(1)
            assertEquals(47.7345607895, gpx.trk.trkseg.get(0).trkpt.get(1).getLat(), 0.0000001);
            assertEquals(11.5732866526, gpx.trk.trkseg.get(0).trkpt.get(1).getLon(), 0.0000001);
            assertEquals(605.66, gpx.trk.trkseg.get(0).trkpt.get(1).getEle(), 0.001);
            assertEquals(sdf.parse("2012-05-01T13:32:45Z"), gpx.trk.trkseg.get(0).trkpt.get(1).getTime().get());

            //Trk/trkseg(1)
            assertNotNull(gpx.trk.trkseg.get(1).trkpt);
            assertEquals(2, gpx.trk.trkseg.get(1).trkpt.size());
        } catch (JAXBException | ParserConfigurationException | SAXException e) {
            fail("JAXB ERROR", e);
        }
    }

    @Test
    public void parseFileTest_gpx_1_0() throws ParseException {
        try {
            File gpxFile = getTestFile("sample_gpx_1.0.gpx");
            Gpx gpx = GpxParser.toGPX(gpxFile);

            assertNotNull(gpx);
            assertEquals("GPSBabel - http://www.gpsbabel.org", gpx.creator);
            assertEquals("1.0", gpx.version);

            // Metadata
            assertNull(gpx.metadata);
            // Trk
            assertNotNull(gpx.trk, "trk not set");
            assertNull(gpx.trk.name);
            assertNull(gpx.trk.extensions);
            //Trk/trkseg
            assertNotNull(gpx.trk.trkseg);
            assertEquals(1, gpx.trk.trkseg.size());
            //Trk/trkseg(0)
            assertNotNull(gpx.trk.trkseg.get(0).trkpt);
            assertEquals(3, gpx.trk.trkseg.get(0).trkpt.size());
            //Trk/trkseg(0)/trkpt(0)
            assertEquals(47.741473333, gpx.trk.trkseg.get(0).trkpt.get(0).getLat(), 0.0000001);
            assertEquals(11.566460000, gpx.trk.trkseg.get(0).trkpt.get(0).getLon(), 0.0000001);
            assertEquals(691.200000, gpx.trk.trkseg.get(0).trkpt.get(0).getEle(), 0.001);
            assertEquals(sdf.parse("2011-01-04T14:07:37Z"), gpx.trk.trkseg.get(0).trkpt.get(0).getTime().get());
        } catch (JAXBException | ParserConfigurationException | SAXException e) {
            fail("JAXB ERROR", e);
        }
    }
}
