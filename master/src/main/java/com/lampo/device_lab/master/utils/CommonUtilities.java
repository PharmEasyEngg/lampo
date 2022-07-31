package com.lampo.device_lab.master.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * MIT License <br/>
 * <br/>
 * 
 * Copyright (c) [2022] [PharmEasyEngg] <br/>
 * <br/>
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, prepare derivatives of the work, and to permit
 * persons to whom the Software is furnished to do so, subject to the following
 * conditions: <br/>
 * <br/>
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software. <br/>
 * <br/>
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE. <br/>
 * <br/>
 * 
 * 
 * This software uses open-source dependencies that are listed under the
 * licenses - {@link <a href="https://www.eclipse.org/legal/epl-2.0/">Eclipse
 * Public License v2.0</a>},
 * {@link <a href="https://www.apache.org/licenses/LICENSE-2.0.html">Apache
 * License 2.0</a>}, {@link <a href=
 * "https://www.mongodb.com/licensing/server-side-public-license">Server Side
 * Public License</a>},
 * {@link <a href="https://www.mozilla.org/en-US/MPL/2.0/">Mozilla Public
 * License 2.0</a>} and {@link <a href="https://opensource.org/licenses/MIT">MIT
 * License</a>}. Please go through the description of the licenses to understand
 * the usage agreement. <br/>
 * <br/>
 * 
 * By using the license, you agree that you have read, understood and agree to
 * be bound by, including without any limitation by these terms and that the
 * entire risk as to the quality and performance of the software is with you.
 *
 */
@Slf4j
public class CommonUtilities {

	private CommonUtilities() {
	}

	private static final ObjectMapper MAPPER = new ObjectMapper()
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).findAndRegisterModules();

	public static <T> T jsonToPojo(@NonNull String json, @NonNull Class<T> clazz) {
		if (json.isEmpty()) {
			return null;
		}
		try {
			System.out.println(">>>>>>>>>>>>>>> jsonToPojo 1: " + json);
			return MAPPER.readValue(json, clazz);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static <T> T jsonToPojo(@NonNull String json, @NonNull TypeReference<T> clazz) {
		if (json.isEmpty()) {
			return null;
		}
		try {
			System.out.println(">>>>>>>>>>>>>>> jsonToPojo 2: " + json);
			return MAPPER.readValue(json, clazz);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String pojoToJson(@NonNull Object obj) {

		try {
			return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			log.error("{} occurred while calling pojoToJson with message '{}'", e.getClass().getName(), e.getMessage());
			return null;
		}
	}

	public static String toString(@NonNull InputStream stream) {
		try (Reader reader = new InputStreamReader(stream)) {
			return CharStreams.toString(reader);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static boolean isBlank(String str) {
		return str == null || str.trim().isEmpty();
	}

	public static boolean isNotBlank(String str) {
		return !isBlank(str);
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

	public static String getMajorVersion(@NonNull String version) {
		return version.contains(".") ? version.substring(0, version.indexOf(".")) : version;
	}

	public static void sleep(int seconds) {
		try {
			TimeUnit.SECONDS.sleep(seconds);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static String getLocalNetworkIP() {

		if (isNotBlank(System.getenv("LOCAL_IP"))) {
			return System.getenv("LOCAL_IP").trim();
		}

		try {
			Enumeration<NetworkInterface> networkInterfaceEnumeration = NetworkInterface.getNetworkInterfaces();
			while (networkInterfaceEnumeration.hasMoreElements()) {
				for (InterfaceAddress interfaceAddress : networkInterfaceEnumeration.nextElement()
						.getInterfaceAddresses())
					if (interfaceAddress.getAddress().isSiteLocalAddress())
						return interfaceAddress.getAddress().getHostAddress();
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static <T> T search(@NonNull Map<String, Object> map, @NonNull String key) {
		return search(map, key, null);
	}

	@SuppressWarnings("unchecked")
	public static <T> T search(@NonNull Map<String, Object> map, @NonNull String key, T defaultValue) {
		return (T) map.entrySet().stream()
				.filter(e -> (e.getKey().contains(":") ? e.getKey().split(":")[1] : e.getKey()).equalsIgnoreCase(key))
				.map(Entry::getValue).findFirst().orElse(defaultValue);
	}

	public static void setField(@NonNull Object obj, @NonNull String field, @NonNull Object value) {
		try {
			Field f = obj.getClass().getField(field);
			boolean isAccessible = f.isAccessible();
			try {
				if (!isAccessible) {
					f.setAccessible(true);
					f.set(obj, value);
				}
			} finally {
				f.setAccessible(isAccessible);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static List<String> split(@NonNull String str) {
		return split(str, ",");
	}

	public static List<String> split(@NonNull String str, @NonNull String delimiter) {
		return Arrays.stream(str.split(delimiter)).map(String::trim).collect(Collectors.toList());
	}

	/**
	 * Get all the pattern matching the given regular expression in the given text.
	 *
	 * @param text  {@link String}
	 * @param regex {@link String}
	 * @return {@link List}&lt;{@link String}&gt;
	 */
	public static List<String> getMatches(@NonNull String text, @NonNull String regex) {
		return getMatches(text, regex, Pattern.DOTALL);
	}

	/**
	 * Get all the pattern matching the given regular expression in the given text.
	 *
	 * @param text  {@link String}
	 * @param regex {@link String}
	 * @param flag  {@link Pattern#flags()}
	 * @return {@link List}&lt;{@link String}&gt;
	 */
	public static List<String> getMatches(@NonNull String text, @NonNull String regex, int flag) {
		if (isBlank(text) || isBlank(regex)) {
			return Collections.emptyList();
		}
		Pattern pattern = Pattern.compile(regex, flag);
		Matcher matcher = pattern.matcher(text);

		List<String> matches = new ArrayList<>();
		while (matcher.find()) {
			matches.add(matcher.group());
		}
		return matches;
	}

	@SneakyThrows
	public static byte[] readFromUrl(String url) {
		try (InputStream stream = new URL(url).openStream()) {
			return ByteStreams.toByteArray(stream);
		}
	}

}
