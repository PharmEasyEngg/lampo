package com.auito.automationtools.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.NonNull;

public class StringUtils {

	private StringUtils() {
	}

	private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules()
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

	public static String pojoToJson(@NonNull Object obj) {
		try {
			return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String getClippedName(@NonNull String name, int maxLength) {
		String[] arr = name.split("\\s+");
		StringBuilder str = new StringBuilder();
		int index = 0;
		while (str.length() <= maxLength && arr.length > 0 && index < arr.length) {
			str.append(arr[index]).append(" ");
			index++;
		}
		return str.toString();
	}

	public static boolean isBlank(String str) {
		return str == null || str.trim().isEmpty();
	}

	public static String getMajorVersion(@NonNull String version) {
		return version.contains(".") ? version.substring(0, version.indexOf(".")) : version;
	}
}
