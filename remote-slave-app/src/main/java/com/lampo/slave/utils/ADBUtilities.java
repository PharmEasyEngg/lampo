package com.lampo.slave.utils;

import static com.lampo.slave.utils.CommandLineExecutor.exec;
import static com.lampo.slave.utils.CommandLineExecutor.execCommand;
import static com.lampo.slave.utils.StringUtils.getDouble;
import static com.lampo.slave.utils.StringUtils.getMatches;
import static com.lampo.slave.utils.StringUtils.splitLines;

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

import com.google.gson.annotations.SerializedName;
import com.lampo.slave.model.AndroidDeviceProperty;
import com.lampo.slave.model.IDeviceProperty;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * MIT License <br/>
 * <br/>
 * 
 * Copyright (c) [2021] [PharmEasyEngg] <br/>
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
 * Commercial distributors of software may accept certain responsibilities with
 * respect to end users, business partners and the like. While this license is
 * intended to facilitate the commercial use of the Program, the Contributor who
 * includes the Program in a commercial product offering should do so in a
 * manner which does not create potential liability for other Contributors.
 * <br/>
 * <br/>
 * 
 * This License does not grant permission to use the trade names, trademarks,
 * service marks, or product names of the Licensor, except as required for
 * reasonable and customary use in describing the origin of the Work and
 * reproducing the content of the NOTICE file. <br/>
 * <br/>
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
 * the usage agreement.
 * 
 *
 */
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
