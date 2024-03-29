package com.mule.lookup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public class WeightLookup {
	public void lookupWeight(String categoryName, String messageLevel) {
		Path fileName = Path.of("weightRules.json");

		String jsonReport = null;
		try {
			jsonReport = Files.readString(fileName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		JsonElement element = JsonParser.parseString(jsonReport);
		JsonArray rows = element.getAsJsonArray();
		Optional op = StreamSupport.stream(rows.spliterator(), false).map(JsonElement::getAsJsonObject)
				.filter(jsonObject -> jsonObject.get("componentName").getAsString().equals(categoryName)).findFirst();
		if (!op.isEmpty()) {
			JsonObject lookup = (JsonObject) op.get();
			System.out.println(lookup.get(messageLevel).getAsInt());
		} else {
			System.out.println("Lookup not found");
		}
	}

}
