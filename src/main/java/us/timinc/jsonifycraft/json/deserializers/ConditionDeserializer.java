package us.timinc.jsonifycraft.json.deserializers;

import java.lang.reflect.*;
import java.util.*;

import com.google.gson.*;

import us.timinc.jsonifycraft.*;
import us.timinc.jsonifycraft.json.*;

public class ConditionDeserializer implements JsonDeserializer<ConditionDescription> {
	private static HashMap<String, Type> conditionClasses = new HashMap<>();

	public static void registerCondition(String name, Type type) {
		JsonifyCraft.LOGGER.info("Registering condition: " + name);
		conditionClasses.put(name, type);
	}

	@Override
	public ConditionDescription deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		JsonObject jsonObject = json.getAsJsonObject();

		String type = jsonObject.get("type").getAsString();

		return context.deserialize(jsonObject, conditionClasses.get(type));
	}

}
