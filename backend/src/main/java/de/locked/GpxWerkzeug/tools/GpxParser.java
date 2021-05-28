package de.locked.GpxWerkzeug.tools;

import de.locked.GpxWerkzeug.gpx.Gpx;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;

public class GpxParser {
    private static Logger LOG = LogManager.getLogger(GpxParser.class);

    public static Optional<Gpx> toGPX(Path path) {
        try {
            LOG.info("parsing file "+path);
            return of(toGPX(path.toUri()));
        } catch (JAXBException | ParserConfigurationException | SAXException e) {
            LOG.log(Level.ERROR, "error parsing file "+path, e);
            return empty();
        }
    }

    public static Gpx toGPX(File file) throws JAXBException, ParserConfigurationException, SAXException {
        return toGPX(file.toURI());
    }

    public static Gpx toGPX(URI uri) throws JAXBException, ParserConfigurationException, SAXException {
        JAXBContext jaxbContext = JAXBContext.newInstance(Gpx.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        final SAXParserFactory sax = SAXParserFactory.newInstance();
        sax.setNamespaceAware(false);
        final XMLReader reader = sax.newSAXParser().getXMLReader();
        final Source er = new SAXSource(reader, new InputSource(uri.toString()));
        return (Gpx) unmarshaller.unmarshal(er);
    }
}
