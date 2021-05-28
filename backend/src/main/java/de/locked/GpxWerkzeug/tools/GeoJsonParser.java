package de.locked.GpxWerkzeug.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.locked.GpxWerkzeug.geoJson.MultiLineString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GeoJsonParser {
    private static Logger LOG = LogManager.getLogger(GeoJsonParser.class);

    public static String toGeoString(MultiLineString multiLineString) throws JsonProcessingException {
        return toGeoString(multiLineString, false);
    }

    public static String toGeoString(MultiLineString multiLineString, boolean prettyPrint) throws JsonProcessingException {
        ObjectMapper om = new ObjectMapper();
        if (prettyPrint) om.enable(SerializationFeature.INDENT_OUTPUT);
        return om.writeValueAsString(multiLineString);
    }
}
