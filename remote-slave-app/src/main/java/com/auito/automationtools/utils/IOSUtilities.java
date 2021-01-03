package com.auito.automationtools.utils;

import static com.auito.automationtools.utils.CommandLineExecutor.exec;
import static com.auito.automationtools.utils.StringUtils.extractNumbers;
import static com.auito.automationtools.utils.StringUtils.getMatches;
import static com.auito.automationtools.utils.StringUtils.splitLines;
import static java.util.function.Function.identity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.auito.automationtools.model.IDeviceProperty;
import com.auito.automationtools.model.IOSDeviceProperty;
import com.auito.automationtools.model.Platform;
import com.fasterxml.jackson.core.type.TypeReference;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class IOSUtilities {

	private IOSUtilities() {
	}

	private static final Platform PLATFORM = Platform.CURRENT_PLATFORM;

	private static final String REGEX = "([a-zA-Z0-9\\s]+)(?!\\().+?((?<=\\()([A-Z0-9\\-]+)(?=\\) \\(Booted\\)))";

	private static String localSDKVersion;

	static {
		CommandLineResponse response = exec("ios-deploy --version");
		if (response.getExitCode() == 0) {
			int majorVersion = Integer
					.parseInt(response.getStdOut().substring(0, response.getStdOut().indexOf('.')).trim());
			if (majorVersion < 1) {
				throw new RuntimeException("please install `ios-deploy` version 1.X or higher");
			}
		}
	}

	public static String getSDKVersion() {
		CommandLineResponse response = exec("xcodebuild -showsdks | grep -Eoi iphonesimulator[0-9\\.]+");
		if (response.getExitCode() == 0) {
			Double version = extractNumbers(response.getStdOut());
			if (version != 0D) {
				log.debug("local iOS sdk version => {}", version);
				return version.toString();
			}
		}
		log.error("unable to fetch iOS simulator SDK version");
		return null;
	}

	public static Map<String, IDeviceProperty> getConnectedRealDevices() {
		if (PLATFORM != Platform.MACINTOSH) {
			return Collections.emptyMap();
		}
		String cmd = "ios-deploy --detect --json --timeout 1";
		CommandLineResponse response = exec(cmd);
		if (response != null && response.getExitCode() == 0) {
			String regex = "(?<=\"Device\" : \\{).*?(?=\\})";
			String text = response.getStdOut().replaceAll("\\s+", " ");

			List<String> list = StringUtils.getMatches(text, regex).stream().map(e -> String.format("{ %s }", e))
					.collect(Collectors.toList());

			TypeReference<Map<String, String>> type = new TypeReference<Map<String, String>>() {
			};
			return list.stream().map(str -> {
				Map<String, String> map = ParserUtilities.jsonToPojo(str, type);
				IOSDeviceProperty property = new IOSDeviceProperty();
				property.setRealDevice(true);
				property.setDeviceId(map.get("DeviceIdentifier"));
				property.setModel(map.get("modelName"));
				property.setSdkVersion(map.get("ProductVersion"));
				return property;
			}).distinct().filter(Objects::nonNull).collect(Collectors.toMap(IDeviceProperty::getDeviceId, identity()));

		}
		return Collections.emptyMap();
	}

	public static Map<String, IDeviceProperty> getConnectedSimulators() {
		if (PLATFORM != Platform.MACINTOSH) {
			log.error(String.format(
					"current platform is %s - can fetch simulators and real ios devices only on '%s' operating system",
					PLATFORM, Platform.MACINTOSH));
			return Collections.emptyMap();
		}
		CommandLineResponse response = exec("xcrun simctl list devices | grep -i booted");
		if (response.getExitCode() == 0) {
			if (localSDKVersion == null) {
				localSDKVersion = getSDKVersion();
			}
			return splitLines(response.getStdOut()).stream().map(line -> {
				List<String> matches = getMatches(line, REGEX);
				if (matches != null && !matches.isEmpty()) {
					String match = matches.get(0);
					int index = match.lastIndexOf('(');
					String name = match.substring(0, index).trim();
					String id = match.substring(index + 1).trim();
					IOSDeviceProperty property = new IOSDeviceProperty();
					property.setRealDevice(false);
					property.setDeviceId(id);
					property.setModel(name);
					property.setSdkVersion(localSDKVersion);
					return property;
				}
				return null;
			}).distinct().filter(Objects::nonNull).collect(Collectors.toMap(IDeviceProperty::getDeviceId, identity()));
		}
		return Collections.emptyMap();
	}

}
