package com.lampo.device_lab.slave.utils;

import static com.lampo.device_lab.slave.utils.CommandLineExecutor.exec;
import static com.lampo.device_lab.slave.utils.CommonUtilities.extractNumbers;
import static com.lampo.device_lab.slave.utils.CommonUtilities.getMatches;
import static com.lampo.device_lab.slave.utils.CommonUtilities.isBlank;
import static com.lampo.device_lab.slave.utils.CommonUtilities.jsonToPojo;
import static com.lampo.device_lab.slave.utils.CommonUtilities.splitLines;

import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.gson.annotations.SerializedName;
import com.lampo.device_lab.slave.model.AndroidDeviceProperty;
import com.lampo.device_lab.slave.model.CommandLineResponse;
import com.lampo.device_lab.slave.model.IDeviceProperty;
import com.lampo.device_lab.slave.model.Platform;

import lombok.NonNull;
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
public final class ADBUtilities {

	private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");

	public static final String ADB_EXECUTABLE = Paths.get(getAndroidHome(), "platform-tools", "adb").toString();

	private static final String GREP_PATTERN = Arrays.stream(AndroidDeviceProperty.class.getDeclaredFields())
			.filter(e -> e.isAnnotationPresent(SerializedName.class))
			.map(e -> e.getAnnotation(SerializedName.class).value())
			.filter(e -> e.contains("."))
			.collect(Collectors.joining("|"));

	public static Map<String, IDeviceProperty> getConnectedDevices() {

		return connectedDevices().stream().map(deviceId -> {
			CommandLineResponse response = CommandLineExecutor
					.exec(String.format("%s -s %s shell getprop | grep -Ei '%s'", ADB_EXECUTABLE, deviceId,
							GREP_PATTERN));

			if (response.getExitCode() == 0) {
				AndroidDeviceProperty prop = jsonToPojo(transformToJson(response.getStdOut()),
						AndroidDeviceProperty.class, true);

				String build = (prop.getBuildCharacteristics() == null) ? ""
						: prop.getBuildCharacteristics().toLowerCase();

				prop.setRealDevice((build.contains("default") || !build.contains("emulator")));
				prop.setDeviceId(deviceId);
				prop.setMarketName(
						(prop.getMarketName() == null && deviceId.toLowerCase().contains("emulator")) ? "Emulator"
								: ((prop.getMarketName() == null) ? prop.getManufacturer() : prop.getMarketName()));

				prop.setChromeVersion(getChromeVersion(deviceId));

				return prop;
			}
			return null;
		}).filter(Objects::nonNull).collect(Collectors.toMap(IDeviceProperty::getDeviceId, e -> e));
	}

	private static String getAndroidHome() {
		String androidHome = System.getenv("ANDROID_HOME");
		return isBlank(androidHome) ? System.getProperty("android.home", "/opt/android-sdk")
				: androidHome
						.trim();
	}

	public static String getChromeVersion(@NonNull String deviceId) {
		return getAppVersion(deviceId, "com.android.chrome");
	}

	public static String getWebViewVersion(@NonNull String deviceId) {
		return getAppVersion(deviceId, "com.google.android.webview");
	}

	public static String getAppVersion(@NonNull String deviceId, String appPackage) {

		String cmd = String.format("%s -s %s shell dumpsys package '%s' | grep versionName | head -n 1 | cut -d'=' -f2",
				ADB_EXECUTABLE, deviceId, appPackage);

		CommandLineResponse response = CommandLineExecutor.exec(cmd);
		if (response.getExitCode() == 0) {
			String version = response.getStdOut().replaceAll("[^0-9\\.]", "");
			log.debug("version of app '{}' installed on device '{}' => {}",
					new Object[] { appPackage, deviceId, version });
			return version;
		}
		return null;
	}

	public static String getMemory(@NonNull String deviceId) {

		String cmd = String.format("%s -s %s shell cat /proc/meminfo | grep -i MemTotal | cut -d ' ' -f 9",
				ADB_EXECUTABLE, deviceId);

		CommandLineResponse response = CommandLineExecutor.exec(cmd);
		if (response.getExitCode() == 0) {
			double memory = extractNumbers(response.getStdOut()).doubleValue() / 1024000.0D;
			log.debug("memory of '{}' => {}", deviceId, Double.valueOf(memory));
			return DECIMAL_FORMAT.format(memory);
		}
		return null;
	}

	public static Collection<IDeviceProperty> getConnectedRealDevices() {
		return filterBy(e -> e.getValue().isAndroid() && e.getValue().isRealDevice());
	}

	public static Collection<IDeviceProperty> getConnectedEmulators() {
		return filterBy(e -> e.getValue().isAndroid() && !e.getValue().isRealDevice());
	}

	private static List<IDeviceProperty> filterBy(Predicate<Entry<String, IDeviceProperty>> predicate) {
		return getConnectedDevices()
				.entrySet()
				.stream()
				.filter(predicate)
				.map(Map.Entry::getValue)
				.collect(Collectors.toList());
	}

	public static List<String> connectedDevices() {
		return getDevices("device");
	}

	public static List<String> offlineDevices() {
		return getDevices("offline");
	}

	public static List<String> unauthorizedDevices() {
		return getDevices("unauthorize");
	}

	public static Map<String, List<String>> getDevicesGroup() {
		Map<String, List<String>> devices = new HashMap<>();
		CommandLineResponse response = CommandLineExecutor.exec(ADB_EXECUTABLE + " devices");
		if (response.getExitCode() == 0) {
			List<String> split = splitLines(response.getStdOut());
			if (split.size() > 1) {
				for (int i = 1; i < split.size(); i++) {
					List<String> spl = Arrays.<String>stream(split.get(i).split("\\s+")).map(String::trim)
							.filter(e -> !isBlank(e)).collect(Collectors.toList());
					if (spl != null && spl.size() == 2) {
						List<String> list = devices.getOrDefault(spl.get(1).trim(), new ArrayList<>());
						list.add(spl.get(0));
						devices.put(spl.get(1).toLowerCase(), list);
					}
				}
			}
		}
		return devices;
	}

	private static List<String> getDevices(String match) {
		CommandLineResponse response = CommandLineExecutor
				.exec(ADB_EXECUTABLE + " devices | tail -n +2 " + (isBlank(match) ? "" : ("| grep -i " + match)));
		if (response.getExitCode() == 0) {
			List<String> split = splitLines(response.getStdOut());
			return split.stream()
					.filter(e -> (e.contains(match) && !e.trim().isEmpty()))
					.map(e -> e.split("\\s+")[0].trim())
					.collect(Collectors.toList());
		}
		return Collections.emptyList();
	}

	public static boolean clearUserData(String deviceId, @NonNull String appPackage) {
		String cmd = String.format("%s -s %s shell pm clear %s", ADB_EXECUTABLE, deviceId, appPackage);
		CommandLineResponse response = CommandLineExecutor.exec(cmd);
		if (response.getExitCode() == 0) {
			return response.getStdOut().toLowerCase().contains("success");
		}
		return false;
	}

	public static void rebootDevice(@NonNull String deviceId) {
		log.info("reboot android device '{}'", deviceId);
		String cmd = String.format("%s -s %s shell reboot", ADB_EXECUTABLE, deviceId);
		CommandLineExecutor.exec(cmd);
	}

	public static void startEmulator(@NonNull String deviceId) {
		String cmd = String.format(
				"%s/emulator/emulator @\"%s\" -no-snapshot-load -netdelay none -netspeed full -dns-server 8.8.8.8 > /tmp/%s.log 2>&1 < /dev/null &",
				getAndroidHome(), deviceId, deviceId);
		CommandLineExecutor.exec(cmd);
	}

	private static boolean killProcess(@NonNull String name) {
		String cmd = String.format(
				"ps -ef | grep -i '%s' | grep -v java | grep -v stf | grep -v grep | awk '{print $2}' | tr -s '\\n' ' ' | xargs kill -9",
				name);
		return 0 == exec(cmd).getExitCode();
	}

	public static void restartEmulator(@NonNull String avdName) {
		log.info("reboot emulator with name '{}'", avdName);
		killProcess(avdName);
		String cmd = String.format(
				"%s/emulator/emulator @\"%s\" -no-snapshot-load -netdelay none -netspeed full -dns-server 8.8.8.8 > /tmp/%s.log 2>&1 < /dev/null &",
				getAndroidHome(), avdName, avdName);
		CommandLineExecutor.exec(cmd);
	}

	public static boolean uninstallApp(String deviceId, @NonNull String... appPackages) {
		if (appPackages.length > 0) {
			boolean status = true;
			for (String appPackage : appPackages) {
				String cmd = String.format("%s -s %s uninstall %s", ADB_EXECUTABLE, deviceId, appPackage);
				CommandLineResponse response = CommandLineExecutor.exec(cmd);
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
		return (0 == CommandLineExecutor.exec(cmd).getExitCode());
	}

	private static final String transformToJson(String properties) {
		String props = splitLines(properties).stream().map(e -> {
			List<String> matches = getMatches(e, "(?<=\\[).+?(?=\\])");
			return matches.isEmpty() ? null
					: String.format("\"%s\" : %s", matches.get(0),
							(matches.size() < 2) ? "\"\""
									: (matches.get(1).trim().startsWith("{") ? matches.get(1)
											: ("\"" + matches.get(1) + "\"")));
		})
				.filter(Objects::nonNull).collect(Collectors.joining(", "));

		return String.format("{ %s }", props);
	}

	public static Map<String, String> getDeviceAVDMapping() {
		if (Platform.CURRENT_PLATFORM != Platform.MACINTOSH) {
			log.error("method 'getDeviceAVDMapping' not implemented for linux or windows");
			return Collections.emptyMap();
		}

		CommandLineResponse response = CommandLineExecutor
				.exec("ps -ef | grep emulator | grep -v node | grep -v emulators | grep -v bash | grep qemu");

		if (response != null && response.getExitCode() == 0) {
			List<String> split = splitLines(response.getStdOut());

			Map<String, String> map = new HashMap<>();
			for (String line : split) {
				line = line.trim();
				String[] arr = line.split("\\s+");
				String avdName = null;
				if (response.getStdOut().contains("avd")) {
					avdName = getMatches(line, "(?<=avd)\\s+.+[\\s|\\n]{0,1}").get(0).trim();
				} else {
					avdName = getMatches(line, "(?<=@).+?(?=\\s)").get(0).trim();
				}
				map.put(arr[1].trim(), avdName);
			}
			Map<String, String> mapping = new HashMap<>();
			for (Entry<String, String> entry : map.entrySet()) {
				String cmd = String.format(
						"lsof -nP -iTCP -sTCP:LISTEN | grep -i  '%s' | grep -i ipv4 | tail -n -1 | awk '{print $(NF-1)}' | cut -d':' -f2  | xargs echo \"emulator-\"  | tr -d ' '",
						entry.getKey());
				CommandLineResponse resp = CommandLineExecutor.exec(cmd);
				(new String[2])[0] = entry.getKey();
				(new String[2])[1] = resp.getStdOut().trim();
				String[] arr = (resp.getExitCode() == 0) ? new String[2] : null;

				if (arr != null) {
					mapping.put(entry.getValue(), arr[1]);
				}
			}
			log.info("running emulators => {} - mapping {}", map, mapping);
			return mapping;
		}
		return Collections.emptyMap();
	}

}
