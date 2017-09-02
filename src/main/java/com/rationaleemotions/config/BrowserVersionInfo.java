package com.rationaleemotions.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

/**
 * A simple POJO that represents the mapping
 */
public class BrowserVersionInfo {
	private String version;
	private Map<String, JsonElement> target = new HashMap<>();
	private String implementation;

	public String getVersion() {
		return version;
	}

	public String getTargetAttribute(final String attribute) {
		JsonElement result = target.get(attribute);
		if (result == null || result.isJsonNull()) {
			return null;

		}
		return result.getAsString();
	}

	public List<String> getTargetAttributeAsList(final String attribute) {
		JsonElement result = target.get(attribute);
		if (result == null || result.isJsonNull()) {
			return Collections.emptyList();

		}
		JsonArray jsa = result.getAsJsonArray();

		List<String> resultlist = new ArrayList<>();
		Iterator<JsonElement> iteratorJsonArray = jsa.iterator();
		while (iteratorJsonArray.hasNext()) {
			resultlist.add(iteratorJsonArray.next().getAsString());
		}
		return Collections.unmodifiableList(resultlist);
	}

	public String getImplementation() {
		return implementation;
	}

	@Override
	public String toString() {
		return String.format("BrowserVersionInfo{version='%s', implementation='%s', target='%s'}", version,
				implementation, target);
	}
}
