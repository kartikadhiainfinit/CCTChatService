package de.infinit.chatservice.utils;

import java.io.IOException;
import java.io.StringReader;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JsonHelper {

	// Get JSON root node of JSON text
	public static JsonNode getJsonRootNode(String jsonResponse) throws JsonParseException, JsonMappingException, IOException {

		if (jsonResponse == null || jsonResponse.isEmpty())
			return null;

		// Parse JSON text
		ObjectMapper m = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
		StringReader sr = new StringReader(jsonResponse);
		JsonNode rootNode = m.readValue(sr, JsonNode.class);

		// log.debug("CWR: JSON Response:\n" + m.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode));

		return rootNode;
	}

	// Read JSON text node
	public static String getText(JsonNode node) {

		if (node == null || node.asText() == null || node.asText().isEmpty()) {
			return null;
		} else {
			return node.asText();
		}
	}

}
