package com.lampo.slave.utils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.lampo.slave.utils.ADBUtilities.connectedDevices;
import static com.lampo.slave.utils.ADBUtilities.getChromeVersion;
import static com.lampo.slave.utils.ADBUtilities.rebootEmulator;
import static com.lampo.slave.utils.ADBUtilities.restartADBServer;
import static com.lampo.slave.utils.ChromeDriverExecutableUtils.getChromeDriverExecutable;
import static com.lampo.slave.utils.CommandLineExecutor.exec;
import static com.lampo.slave.utils.CommandLineExecutor.killAppiumProcessesByDeviceId;
import static com.lampo.slave.utils.CommandLineExecutor.killProcessListeningAtPort;
import static com.lampo.slave.utils.StringUtils.getLocalNetworkIP;
import static com.lampo.slave.utils.StringUtils.isBlank;
import static java.lang.Runtime.getRuntime;
import static java.lang.System.currentTimeMillis;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
public final class AppiumLocalService {

	private static final String DEFAULT_ANDROID_HOME = "/opt/android-sdk";
	private static final ReentrantLock LOCK = new ReentrantLock(true);

	private AppiumLocalService() {
	}

	public static Builder builder() {
		return new Builder();
	}

	@Slf4j
	public static final class Builder {

		private static final int MAX_SESSION_START_TIMEOUT = 60;
		private static final int APPIUM_PORT_START = 4723;
		private static final int TOTAL_PORTS = 30;
		public static final String LOG_DIRECTORY = createDirectory(
				Paths.get(
						StringUtils.getProperty("DEVICE_LAB_LOGS_DIR",
								new File(System.getProperty("user.dir")).getAbsolutePath()),
						"appium-logs").toFile());

		private static final Map<Integer, String> APPIUM_PORTS = IntStream
				.range(APPIUM_PORT_START, APPIUM_PORT_START + TOTAL_PORTS + 1)
				.mapToObj(Integer::valueOf)
				.collect(Collectors.toMap(e -> e, e -> ""));

		private String deviceId;
		private boolean isAndroid;
		private Integer port;
		private String browserVersion;
		private String ip = getLocalNetworkIP();
		private String requestId;

		public Builder deviceId(@NonNull String deviceId) {
			this.deviceId = deviceId;
			return this;
		}

		public Builder isAndroid(boolean isAndroid) {
			this.isAndroid = isAndroid;
			return this;
		}

		public Builder ip(String ip) {
			this.ip = ip;
			return this;
		}

		public Builder browserVersion(String browserVersion) {
			this.browserVersion = browserVersion;
			return this;
		}

		private static Collection<String> getDevicesOfRunningAppiumSessions() {
			CommandLineResponse response = exec(
					"ps -ef | grep -v stf | grep -i node | grep -i appium | grep -i 'relaxed-security' | grep -v grep | awk '{print $11}' | tr -s '\\n' ' '");
			if (response.getExitCode() == 0) {
				return Arrays.stream(response.getStdOut().split("\\s+"))
						.map(String::trim).filter(e -> !e.isEmpty())
						.collect(Collectors.toSet());
			}
			return Collections.emptySet();
		}

		private static int getPort(@NonNull String deviceId) {
			Optional<Integer> optional = APPIUM_PORTS.entrySet().stream()
					.filter(entry -> deviceId.equals(entry.getValue())).map(Entry::getKey).findFirst();
			return optional.isPresent() ? optional.get() : -1;
		}

		/**
		 * deviceId is required
		 */
		public boolean stop() {

			try {
				checkArgument(!isBlank(deviceId), "deviceId cannot be null");
				LOCK.lock();

				int runPort = getPort(deviceId);
				if (runPort != -1) {
					APPIUM_PORTS.put(runPort, null);
				}
				sleep(1000);
				killAppiumProcessesByDeviceId(deviceId);
				log.info("********* {} ::: stopping appium process of device '{}' *********", requestId, deviceId);
				return true;
			} catch (Exception ex) {
				log.error("********* {} ::: unable to stop appium process for device '{}' *********", requestId,
						deviceId);
				return false;
			} finally {
				sleep(1000);
				LOCK.unlock();
			}

		}

		public URL restart() {
			stop();
			stop();
			return start();
		}

		/**
		 * deviceId, isAndroid are required
		 *
		 * @return {@link URL}
		 */
		public URL start() {
			try {
				LOCK.lock();
				log.info("********* {} ::: starting appium process for device '{}' *********", requestId, deviceId);
				sleep(1000);
				startProcess(getCommand());
				if (waitFor(port, MAX_SESSION_START_TIMEOUT)) {
					return new URL(String.format("http://%s:%s/wd/hub", ip, port));
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				APPIUM_PORTS.put(port, deviceId);
				sleep(1000);
				LOCK.unlock();
			}
			log.error("********* {} ::: unable to start appium process for device '{}' *********", requestId, deviceId);
			return null;
		}

		private static void sleep(int millisec) {
			try {
				TimeUnit.MILLISECONDS.sleep(millisec);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private String getCommand() {

			Command cmd = new Command();

			checkArgument(!isBlank(deviceId), "deviceId cannot be null");
			cmd.add("--udid", deviceId);

			port = findFreePort();
			checkNotNull(port, "appium port cannot be null");

			cmd.add("--port", port.toString()).add("--address", "0.0.0.0").add("--log-level", "info:debug");

			String logFile = Paths.get(LOG_DIRECTORY, deviceId,
					(isBlank(requestId) ? currentTimeMillis() : requestId) + ".log").toString();

			cmd.add("--log", logFile).add("--relaxed-security");

			if (isAndroid) {
				Integer bootstrapPort = port + 100;
				killProcessListeningAtPort(bootstrapPort);
				cmd.add("--bootstrap-port", bootstrapPort.toString());

				Integer chromeDriverPort = port + 200;
				killProcessListeningAtPort(chromeDriverPort);

				cmd.add("--chromedriver-port", chromeDriverPort.toString()).add("--chromedriver-executable",
						getChromeDriverPath());
			} else {
				Integer wdaProxyPort = port + 300;
				killProcessListeningAtPort(wdaProxyPort);
				cmd.add("--webkit-debug-proxy-port", wdaProxyPort.toString());
			}

			return cmd.toString();

		}

		private String getChromeDriverPath() {
			return getChromeDriverExecutable(isBlank(this.browserVersion) ? getChromeVersion(deviceId) : browserVersion)
					.getAbsolutePath();
		}

		private static String createDirectory(@NonNull File directory) {
			if (!directory.exists()) {
				directory.mkdirs();
			}
			return directory.getAbsolutePath();
		}

		private Integer findFreePort() {
			try {
				LOCK.lock();
				Optional<Entry<Integer, String>> optional = APPIUM_PORTS.entrySet().stream()
						.filter(e -> isBlank(e.getValue())).findFirst();
				if (optional.isPresent()) {
					APPIUM_PORTS.put(optional.get().getKey(), deviceId);
				} else {
					throw new RuntimeException(String.format("no free ports available in range %s - %s",
							APPIUM_PORT_START, APPIUM_PORT_START + TOTAL_PORTS));
				}
				return optional.get().getKey();
			} finally {
				LOCK.unlock();
			}
		}

		public boolean restartABDServer() {

			try {
				LOCK.lock();
				if (!getDevicesOfRunningAppiumSessions().isEmpty()) {
					return false;
				}
				log.info("******************** restarting adb server ********************");
				return restartADBServer();
			} finally {
				sleep(5000);
				LOCK.unlock();
			}
		}

		private static String getJavaHome() {
			String javaHome = StringUtils.getProperty("JAVA_HOME");
			return javaHome.contains("jre") ? new File(javaHome).getParent() : javaHome;
		}

		public static String getAndroidHome() {
			String androidHome = StringUtils.getProperty("ANDROID_HOME", DEFAULT_ANDROID_HOME).trim();
			if (androidHome.endsWith("/")) {
				androidHome = androidHome.substring(0, androidHome.length() - 1);
			}
			return androidHome;
		}

		public void restartEmulators() {
			connectedDevices().stream().parallel()
					.forEach(device -> new Thread(() -> rebootEmulator(device)));
		}

		private void startProcess(@NonNull String command) {

			String script = StringUtils
					.streamToString(AppiumLocalService.class.getResourceAsStream("/scripts/wd-session.bash"));
			File scriptFile = new File(String.format("wd-session-%s.bash", System.currentTimeMillis()));
			log.info("starting appium session");
			new Thread(() -> {
				try {
					String cmd = String.format("bash %s %s %s %s", scriptFile.getAbsolutePath(),
							getJavaHome(), getAndroidHome(), command);
					java.nio.file.Files.write(scriptFile.toPath(), script.getBytes());
					getRuntime().exec(cmd).waitFor(30, TimeUnit.SECONDS);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					scriptFile.delete();
				}
			}).start();

		}

		public Builder requestId(String requestId) {
			this.requestId = requestId;
			return this;
		}

		private boolean waitFor(int port, int maxTimeout) {
			long end = System.currentTimeMillis() + (maxTimeout * 1000);
			try {
				URL url = new URL(String.format("http://127.0.0.1:%s/wd/hub/status", port));
				do {
					if (isUrlListening(url)) {
						return true;
					} else {
						sleep(1000);
					}
				} while (System.currentTimeMillis() < end);
				log.error("listening on port '{}' timed out after '{} seconds'", port, maxTimeout);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return false;
		}

		private boolean isUrlListening(URL url) {
			HttpURLConnection connection = null;
			try {
				connection = (HttpURLConnection) url.openConnection();
				connection.setRequestMethod("GET");
				connection.connect();
				return 200 == connection.getResponseCode();
			} catch (IOException e) {
				return false;
			} finally {
				if (connection != null) {
					connection.disconnect();
				}
			}
		}

		class Command {

			private Map<String, Object> map = new HashMap<>();

			public Command add(@NonNull String key) {
				return add(key, null);
			}

			public Command add(@NonNull String key, Object value) {
				map.put(key, value);
				return this;
			}

			@Override
			public String toString() {
				return map.entrySet()
						.stream()
						.map(e -> String.format("%s %s", e.getKey(),
								isBlank(e.getValue()) ? "" : "\"" + e.getValue() + "\""))
						.collect(Collectors.joining(" "));
			}
		}

	}

}
