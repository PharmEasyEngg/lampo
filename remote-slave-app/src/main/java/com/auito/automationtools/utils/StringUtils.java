package com.auito.automationtools.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CharStreams;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StringUtils {

	private StringUtils() {
	}

	private static final ObjectMapper MAPPER = new ObjectMapper()
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

	public static String pojoToJson(@NonNull Object obj) {
		try {
			return MAPPER.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static List<String> getMatches(@NonNull String text, @NonNull String regex) {
		Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
		Matcher matcher = pattern.matcher(text);
		List<String> list = new ArrayList<>();
		while (matcher.find()) {
			list.add(matcher.group());
		}
		return list;
	}

	public static List<List<String>> getMatchedGroups(@NonNull String text, @NonNull String regex) {
		Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
		Matcher matcher = pattern.matcher(text);
		List<List<String>> list = new ArrayList<>();
		while (matcher.find()) {
			list.add(IntStream.range(1, matcher.groupCount() + 1).mapToObj(matcher::group)
					.collect(Collectors.toList()));
		}
		return list;
	}

	public static List<String> splitLines(@NonNull String str) {
		return Arrays.asList(str.split("\r?\n"));
	}

	public static Double extractNumbers(@NonNull final String str) {
		if (isBlank(str)) {
			return 0D;
		}
		return getDouble(str.trim().replaceAll("[^0-9\\.]", ""));
	}

	public static boolean isBlank(Object str) {
		return str == null || str.toString().trim().isEmpty();
	}

	public static Double getDouble(final Object object) {
		if (object == null) {
			return 0D;
		}
		if (object instanceof Double) {
			return (Double) object;
		}
		try {
			if (object instanceof String) {
				String str = ((String) object).trim();
				return "".equals(str) ? 0D : Double.valueOf(str);
			}
		} catch (NumberFormatException e) {
			log.error(String.format("Failed - parsing '%s' to double", object), e);
		}
		return 0D;
	}

	public static String getLocalNetworkIP() {

		String ip = null;
		try {
			Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
			while (e.hasMoreElements()) {
				if (ip != null) {
					break;
				}
				NetworkInterface n = e.nextElement();
				Enumeration<InetAddress> ee = n.getInetAddresses();
				while (ee.hasMoreElements()) {
					InetAddress i = ee.nextElement();
					if (i.getHostAddress().startsWith("172.") || i.getHostAddress().startsWith("10.")
							|| i.getHostAddress().startsWith("192.")) {
						ip = i.getHostAddress();
						break;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (ip == null) {
			ip = "0.0.0.0";
		}
		return ip;
	}

	public static String streamToString(@NonNull String url) {
		try {
			return streamToString(new URL(url));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String streamToString(@NonNull URL url) {
		try {
			return streamToString(url.openStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String streamToString(@NonNull InputStream stream) {
		try (Reader reader = new InputStreamReader(stream)) {
			return CharStreams.toString(reader);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public static String getProperty(@NonNull String name) {
		return getProperty(name, null);
	}

	public static String getProperty(@NonNull String name, String defaultValue) {
		String value = System.getenv(name);
		if (!isBlank(value)) {
			return value;
		}
		value = System.getProperty(name);
		return isBlank(value) ? System.getProperty(name.toLowerCase().replace('_', '.'), defaultValue) : value;
	}

}
