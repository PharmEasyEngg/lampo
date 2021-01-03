package com.auito.automationtools.utils;

import static com.auito.automationtools.utils.CommandLineExecutor.exec;
import static com.auito.automationtools.utils.CommandLineExecutor.execCommand;
import static com.auito.automationtools.utils.StringUtils.getDouble;
import static com.auito.automationtools.utils.StringUtils.getMatches;
import static com.auito.automationtools.utils.StringUtils.splitLines;

import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import com.auito.automationtools.model.AndroidDeviceProperty;
import com.auito.automationtools.model.IDeviceProperty;
import com.google.gson.annotations.SerializedName;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ADBUtilities {

	private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");

	private ADBUtilities() {
	}

	private static final String ADB_EXECUTABLE = Paths.get(getAndroidHome(), "platform-tools", "adb").toString();

	private static final String PROPERTY_REGEX = "(?<=\\[).+?(?=\\])";

	public static Map<String, IDeviceProperty> getConnectedDevices() {
		return connectedDevices().stream().map(deviceId -> {
			CommandLineResponse response = execCommand(ADB_EXECUTABLE, "-s", deviceId, "shell",
					"getprop | grep " + getProperties());
			if (response.getExitCode() == 0) {
				AndroidDeviceProperty prop = ParserUtilities.jsonToPojo(transformToJson(response.getStdOut()),
						AndroidDeviceProperty.class, true);
				String build = prop.getBuildCharacteristics() == null ? ""
						: prop.getBuildCharacteristics().toLowerCase();
				prop.setRealDevice(build.contains("default") || !build.contains("emulator"));
				prop.setDeviceId(deviceId);
				prop.setMemory(getMemory(deviceId));
				prop.setMarketName(
						prop.getMarketName() == null && deviceId.toLowerCase().contains("emulator") ? "Emulator"
								: (prop.getMarketName() == null ? prop.getManufacturer() : prop.getMarketName()));
				prop.setChromeVersion(getChromeVersion(deviceId));
				return prop;
			}
			return null;
		}).filter(Objects::nonNull).collect(Collectors.toMap(IDeviceProperty::getDeviceId, e -> e));
	}

	private static String getAndroidHome() {
		String androidHome = System.getenv("ANDROID_HOME");
		return StringUtils.isBlank(androidHome.isEmpty()) ? System.getProperty("android.home", "/opt/android-sdk")
				: androidHome.trim();
	}

	public static String getChromeVersion(@NonNull String deviceId) {
		return getAppVersion(deviceId, "com.android.chrome");
	}

	public static String getWebViewVersion(@NonNull String deviceId) {
		return getAppVersion(deviceId, "com.google.android.webview");
	}

	public static String getAppVersion(@NonNull String deviceId, String appPackage) {
		String cmd = String.format(
				"%s -s %s shell dumpsys package '%s' | grep versionName | head -n 1 | cut -d'=' -f2", ADB_EXECUTABLE,
				deviceId, appPackage);
		CommandLineResponse response = exec(cmd);
		if (response.getExitCode() == 0) {
			String version = response.getStdOut().replaceAll("[^0-9\\.]", "");
			log.debug("version of app '{}' installed on device '{}' => {}", appPackage, deviceId, version);
			return version;
		}
		return null;
	}

	public static String getMemory(@NonNull String deviceId) {
		String cmd = String.format(
				"%s -s %s shell cat /proc/meminfo | grep -i MemTotal | cut -d ' ' -f 9", ADB_EXECUTABLE, deviceId);
		CommandLineResponse response = exec(cmd);
		if (response.getExitCode() == 0) {
			double memory = getDouble(response.getStdOut()) / (1024 * 1000);
			log.debug("memory of '{}' => {}", deviceId, memory);
			return DECIMAL_FORMAT.format(memory);
		}
		return null;
	}

	public static Collection<IDeviceProperty> getConnectedEmulators() {
		return getConnectedDevices()
				.entrySet()
				.stream()
				.filter(e -> e.getValue().isAndroid() && !e.getValue().isRealDevice())
				.map(Entry::getValue)
				.collect(Collectors.toList());
	}

	public static Collection<IDeviceProperty> getConnectedRealDevices() {
		return getConnectedDevices()
				.entrySet()
				.stream()
				.filter(e -> e.getValue().isAndroid() && e.getValue().isRealDevice())
				.map(Entry::getValue)
				.collect(Collectors.toList());
	}

	public static List<String> connectedDevices() {
		CommandLineResponse response = exec(ADB_EXECUTABLE + " devices | tail -n +2");
		if (response.getExitCode() == 0) {
			List<String> split = splitLines(response.getStdOut());
			return split.stream()
					.filter(e -> e.contains("device") && !e.trim().isEmpty())
					.map(e -> e.split("\\s+")[0].trim())
					.collect(Collectors.toList());
		}
		return Collections.emptyList();
	}

	/**
	 * Clear user data for the app identified by the given package for the given
	 * device
	 *
	 * @param deviceId   {@link String}
	 * @param appPackage {@link String}
	 * @return {@link Boolean}
	 */
	public static boolean clearUserData(final String deviceId, @NonNull final String appPackage) {
		String cmd = String.format("%s -s %s shell pm clear %s", ADB_EXECUTABLE, deviceId, appPackage);
		CommandLineResponse response = exec(cmd);
		if (response.getExitCode() == 0) {
			return response.getStdOut().toLowerCase().contains("success");
		}
		return false;
	}

	public static void rebootEmulator(@NonNull final String deviceId) {
		String cmd = String.format("%s -s %s shell reboot", ADB_EXECUTABLE, deviceId);
		exec(cmd);
	}

	/**
	 * Uninstall the given app
	 *
	 * @param deviceId   {@link String}
	 * @param appPackage {@link String}
	 * @return {@link Boolean}
	 */
	public static boolean uninstallApp(final String deviceId, @NonNull final String... appPackages) {
		if (appPackages.length > 0) {
			boolean status = true;
			for (String appPackage : appPackages) {
				String cmd = String.format("%s -s %s uninstall %s", ADB_EXECUTABLE, deviceId, appPackage);
				CommandLineResponse response = exec(cmd);
				if (response.getExitCode() == 0) {
					status &= response.getStdOut().toLowerCase().contains("success");
				}
			}
			return status;
		}
		return false;
	}

	public static boolean restartADBServer() {
		String cmd = String.format("%s stop-server && %s start-server", ADB_EXECUTABLE, ADB_EXECUTABLE);
		return 0 == exec(cmd).getExitCode();
	}

	private static final String transformToJson(String properties) {
		String props = Arrays.stream(properties.split("\r?\n")).map(e -> {
			List<String> matches = getMatches(e, PROPERTY_REGEX);
			if (matches.isEmpty()) {
				return null;
			}
			return String.format("\"%s\" : %s", matches.get(0), matches.size() < 2 ? "\"\""
					: matches.get(1).trim().startsWith("{") ? matches.get(1) : "\"" + matches.get(1) + "\"");
		}).filter(Objects::nonNull)
				.filter(e -> !e.contains("ro.kernel.") && e.contains("ro.build.") || e.contains("ro.product.")
						|| e.contains("ro.config."))
				.collect(Collectors.joining(", "));
		return String.format("{ %s }", props);
	}

	private static String getProperties() {

		return Arrays.stream(AndroidDeviceProperty.class.getDeclaredFields())
				.filter(e -> e.isAnnotationPresent(SerializedName.class))
				.map(e -> e.getAnnotation(SerializedName.class).value()).filter(e -> e.contains("."))
				.map(e -> String.format("-e '%s'", e)).collect(Collectors.joining(" "));
	}

}
