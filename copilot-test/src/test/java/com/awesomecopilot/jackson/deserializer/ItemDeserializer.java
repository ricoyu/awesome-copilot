package com.awesomecopilot.jackson.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.IntNode;
import com.awesomecopilot.jackson.Item;
import com.awesomecopilot.jackson.User;

import java.io.IOException;
		
public class ItemDeserializer extends StdDeserializer<Item> {

	public ItemDeserializer() {
		this(null);
	}

	public ItemDeserializer(Class<?> vc) {
		super(vc);
	}

	@Override
	public Item deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		JsonNode node = jp.getCodec().readTree(jp);
		int id = (Integer) ((IntNode) node.get("id")).numberValue();
		String itemName = node.get("itemName").asText();
		int userId = (Integer) ((IntNode) node.get("createdBy")).numberValue();

		return new Item(id, itemName, new User(userId, null));
	}
}