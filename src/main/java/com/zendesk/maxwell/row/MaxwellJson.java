package com.zendesk.maxwell.row;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.zendesk.maxwell.scripting.Scripting;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class MaxwellJson {
	private static final Logger LOGGER = LoggerFactory.getLogger(MaxwellJson.class);

	public static JsonGenerator createJsonGenerator(JsonFactory jsonFactory, ByteArrayOutputStream buffer) throws IOException {
		JsonGenerator g = jsonFactory.createGenerator(buffer);
		g.setRootValueSeparator(null);
		return g;
	}

	public static void writeValueToJSON(JsonGenerator g, boolean includeNullField, String key, Object value) throws IOException {
		if (value == null && !includeNullField)
			return;

		if (value instanceof ScriptObjectMirror) {
			try {
				String json = Scripting.stringify((ScriptObjectMirror) value);
				writeValueToJSON(g, includeNullField, key, new RawJSONString(json));
			} catch (ScriptException e) {
				LOGGER.error("error stringifying json object:", e);
			}
		} else if (value instanceof List) { // sets come back from .asJSON as lists, and jackson can't deal with lists natively.
			List stringList = (List) value;

			g.writeArrayFieldStart(key);
			for (Object s : stringList) {
				g.writeObject(s);
			}
			g.writeEndArray();
		} else if (value instanceof RawJSONString) {
			// JSON column type, using binlog-connector's serializers.
			g.writeFieldName(key);
			g.writeRawValue(((RawJSONString) value).json);
		} else {
			g.writeObjectField(key, value);
		}
	}
}
