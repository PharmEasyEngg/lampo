package com.lampo.slave.utils;

import static com.lampo.slave.utils.CommandLineExecutor.exec;
import static com.lampo.slave.utils.StringUtils.extractNumbers;
import static com.lampo.slave.utils.StringUtils.getMatches;
import static com.lampo.slave.utils.StringUtils.splitLines;
import static java.util.function.Function.identity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lampo.slave.model.IDeviceProperty;
import com.lampo.slave.model.IOSDeviceProperty;
import com.lampo.slave.model.Platform;

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
