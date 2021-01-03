package com.auito.automationtools.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class ParserUtilities {

	private ParserUtilities() {
	}

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.enable(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
			.findAndRegisterModules();

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation()
			.create();

	public static final <T> T jsonToPojo(String json, TypeReference<T> clazz) {
		try {
			return OBJECT_MAPPER.readValue(json, clazz);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static final <T> T jsonToPojo(String json, Class<T> clazz) {
		return jsonToPojo(json, clazz, false);
	}

	public static final <T> T jsonToPojo(String json, Class<T> clazz, boolean useGson) {
		try {
			return useGson ? GSON.fromJson(json, clazz) : OBJECT_MAPPER.readValue(json, clazz);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static final String pojoToJson(Object object) {
		return pojoToJson(object, false);
	}

	public static final String pojoToJson(Object object, boolean useGson) {
		try {
			return useGson ? GSON.toJson(object)
					: OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return null;
	}
}
