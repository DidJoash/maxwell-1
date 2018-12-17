package com.zendesk.maxwell.row;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.api.client.json.Json;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class RowIdentity implements Serializable {
	private static ThreadLocalBuffer threadLocalBuffer = new ThreadLocalBuffer();
	private static final JsonFactory jsonFactory = new JsonFactory();

	private String database;
	private String table;
	private final List<Map.Entry<String, Object>> primaryKeyColumns;

	// slightly less verbose pair construction
	public static Map.Entry<String, Object> pair(String key, Object value) {
		return new AbstractMap.SimpleImmutableEntry<>(key, value);
	}

	public static List<Map.Entry<String, Object>> pairs(Map.Entry<String, Object> ... entries) {
		return Arrays.asList(entries);
	}

	public RowIdentity(String database, String table, List<Map.Entry<String, Object>> primaryKeyColumns) {
		this.database = database;
		this.table = table;
		this.primaryKeyColumns = primaryKeyColumns;
	}

	public String getDatabase() {
		return database;
	}

	public String getTable() {
		return table;
	}

	public void writeKeyJson(RowMap.KeyFormat keyFormat, JsonGenerator g) throws IOException {
		if ( keyFormat == RowMap.KeyFormat.HASH )
			writeKeyJsonHash(g);
		else
			writeKeyJsonArray(g);
	}

	public String toKeyJson(RowMap.KeyFormat keyFormat) throws IOException {
		JsonGenerator g = MaxwellJson.createJsonGenerator(jsonFactory, threadLocalBuffer.reset());
		writeKeyJson(keyFormat, g);
		return threadLocalBuffer.consume();
	}

	private void writeKeyJsonHash(JsonGenerator g) throws IOException {
		writeStartCommon(g);
		if (primaryKeyColumns.isEmpty()) {
			g.writeStringField(FieldNames.UUID, UUID.randomUUID().toString());
		} else {
			for (Map.Entry<String,Object> pk : primaryKeyColumns) {
				writePrimaryKey(g, "pk." + pk.getKey().toLowerCase(), pk.getValue());
			}
		}
		writeEndCommon(g);
	}

	private void writeKeyJsonArray(JsonGenerator g) throws IOException {
		g.writeStartArray();
		g.writeString(database);
		g.writeString(table);

		g.writeStartArray();
		for (Map.Entry<String,Object> pk : primaryKeyColumns) {
			g.writeStartObject();
			MaxwellJson.writeValueToJSON(g, true, pk.getKey().toLowerCase(), pk.getValue());
			g.writeEndObject();
		}
		g.writeEndArray();
		g.writeEndArray();
	}

	public String toConcatString() {
		if (primaryKeyColumns.isEmpty()) {
			return database + table;
		}
		StringBuilder keys = new StringBuilder();
		for (Map.Entry<String, Object> pk : primaryKeyColumns) {
			Object pkValue = pk.getValue();
			if (pkValue != null) {
				keys.append(pkValue.toString());
			}
		}
		if (keys.length() == 0) {
			return "None";
		}
		return keys.toString();
	}

	private void writePrimaryKey(JsonGenerator g, String jsonKey, Object value) throws IOException {
		MaxwellJson.writeValueToJSON(g, true, jsonKey, value);
	}

	private void writePrimaryKey(JsonGenerator g, Map.Entry<String,Object> pk) throws IOException {
		writePrimaryKey(g, pk.getKey(), pk.getValue());
	}

	private JsonGenerator writeStartCommon(JsonGenerator g) throws IOException {
		g.writeStartObject(); // start of row {

		g.writeStringField(FieldNames.DATABASE, database);
		g.writeStringField(FieldNames.TABLE, table);
		return g;
	}

	private void writeEndCommon(JsonGenerator g) throws IOException {
		g.writeEndObject();
		g.flush();
	}

	@Override
	public String toString() {
		return database + ":" + table + ":" + primaryKeyColumns;
	}
}
