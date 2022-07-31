package com.lampo.device_lab.slave.utils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.lampo.device_lab.slave.utils.ADBUtilities.connectedDevices;
import static com.lampo.device_lab.slave.utils.ADBUtilities.getChromeVersion;
import static com.lampo.device_lab.slave.utils.ADBUtilities.restartADBServer;
import static com.lampo.device_lab.slave.utils.ChromeDriverExecutableUtils.getChromeDriverExecutable;
import static com.lampo.device_lab.slave.utils.CommandLineExecutor.exec;
import static com.lampo.device_lab.slave.utils.CommandLineExecutor.killAppiumProcessesByDeviceId;
import static com.lampo.device_lab.slave.utils.CommonUtilities.getLocalNetworkIP;
import static com.lampo.device_lab.slave.utils.CommonUtilities.getMatches;
import static com.lampo.device_lab.slave.utils.CommonUtilities.getProperty;
import static com.lampo.device_lab.slave.utils.CommonUtilities.isBlank;
import static com.lampo.device_lab.slave.utils.CommonUtilities.sleep;
import static com.lampo.device_lab.slave.utils.CommonUtilities.splitLines;
import static com.lampo.device_lab.slave.utils.ProcessUtilities.kill;
import static java.lang.Runtime.getRuntime;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.lampo.device_lab.slave.model.CommandLineResponse;
import com.lampo.device_lab.slave.service.DeviceSyncProcessor;

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
public final class AppiumLocalService {

	private static final String DEFAULT_ANDROID_HOME = "/opt/android-sdk";
	private static final ReentrantLock LOCK = new ReentrantLock(true);

	private AppiumLocalService() {
	}

	public static Builder builder() {
		return new Builder();
	}

	public static synchronized Map<String, Integer> getRunningAppiumSessions() {
		CommandLineResponse response = exec(
				"ps -ef | grep node | grep appium | grep udid | grep -v grep | grep -v bash");
		if (response.getExitCode() == 0) {
			return splitLines(response.getStdOut()).stream().map(e -> {
				List<String> udid = getMatches(e, "(?<=--udid ).*?\\s+");
				List<String> port = getMatches(e, "(?<=--port ).*?(?=\\s+)");
				return new SimpleEntry<>(udid.get(0).trim(), Integer.parseInt(port.get(0).trim()));
			}).collect(Collectors.toMap(Entry::getKey, Entry::getValue, (x, y) -> y, ConcurrentHashMap::new));
		}
		return Collections.emptyMap();
	}

	@Slf4j
	public static final class Builder {

		private static final int MAX_SESSION_START_TIMEOUT = 60;
		private static final int APPIUM_PORT_START = 4723;
		private static final int TOTAL_PORTS = 200;
		public static final String LOG_DIRECTORY = createDirectory(Paths
				.get(getProperty("DEVICE_LAB_LOGS_DIR", new File(System.getProperty("user.dir")).getAbsolutePath()),
						"appium-logs")
				.toFile());

		private static final Map<Integer, String> APPIUM_PORTS_MAPPING = new ConcurrentHashMap<>();

		private static final Queue<Integer> APPIUM_PORTS_QUEUE = IntStream
				.range(APPIUM_PORT_START, APPIUM_PORT_START + TOTAL_PORTS + 1).mapToObj(Integer::valueOf)
				.collect(Collectors.toCollection(LinkedList::new));

		private String deviceId;
		private boolean isAndroid;
		private Integer port;
		private String browserVersion;
		private String ip = getLocalNetworkIP();
		private File nodeConfig;
		private File logFile;
		private String requestId;

		public Builder deviceId(@NonNull String deviceId) {
			this.deviceId = deviceId;
			return this;
		}

		public Builder nodeConfig(@NonNull File nodeConfig) {
			this.nodeConfig = nodeConfig;
			return this;
		}

		public Builder isAndroid(boolean isAndroid) {
			this.isAndroid = isAndroid;
			return this;
		}

		public Builder ip(@NonNull String ip) {
			this.ip = ip;
			return this;
		}

		public Builder port(int port) {
			this.port = port;
			return this;
		}

		public Builder browserVersion(@NonNull String browserVersion) {
			this.browserVersion = browserVersion;
			return this;
		}

		public Builder requestId(@NonNull String requestId) {
			this.requestId = requestId;
			return this;
		}

		public static Collection<String> getDevicesOfRunningAppiumSessions() {
			CommandLineResponse response = exec(
					"ps -ef | grep -v stf | grep -i node | grep -i appium | grep -i 'relaxed-security' | grep -v grep | awk '{print $11}' | tr -s '\\n' ' '");
			if (response.getExitCode() == 0) {
				return Arrays.stream(response.getStdOut().split("\\s+")).map(String::trim).filter(e -> !e.isEmpty())
						.collect(Collectors.toSet());
			}
			return Collections.emptySet();
		}

		/**
		 * deviceId is required
		 */
		public boolean stop() {

			try {
				checkArgument(!isBlank(deviceId), "deviceId cannot be null");
				LOCK.lock();

				int runPort = APPIUM_PORTS_MAPPING.entrySet().stream().filter(e -> e.getValue().contains(deviceId))
						.map(Entry::getKey).findFirst().orElse(-1);
				if (runPort != -1) {
					APPIUM_PORTS_MAPPING.put(runPort, null);
					kill(runPort);
					killAppiumProcessesByDeviceId(deviceId);
					APPIUM_PORTS_QUEUE.add(runPort);
					sleep(1000);
				}
				log.info("********* stopping appium process of device '{}' *********", deviceId);
				return true;
			} catch (Exception ex) {
				log.error("*********  unable to stop appium process for device '{}' - {} *********", deviceId,
						ex.getMessage());
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
				kill(port);
				startProcess(getCommand());
				if (waitFor(port, MAX_SESSION_START_TIMEOUT)) {
					return new URL(String.format("http://%s:%s/wd/hub", ip, port));
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				APPIUM_PORTS_MAPPING.put(port, deviceId);
				LOCK.unlock();
			}
			log.error("********* unable to start appium process for device '{}' *********", deviceId);
			return null;
		}

		private String getCommand() {

			Command cmd = new Command();

			checkArgument(!isBlank(deviceId), "deviceId cannot be null");
			cmd.add("--udid", deviceId);

			port = port == null || port == -1 ? findFreePort() : port;
			checkArgument(port != null && port > -1, "appium port cannot be null");

			String localIp = System.getenv("IP");
			if (isBlank(localIp)) {
				localIp = "0.0.0.0";
			}

			if (logFile == null) {
				logFile = Paths.get(LOG_DIRECTORY, deviceId + ".log").toFile();
			}

			cmd
					.add("--port", port)
					.add("--address", localIp)
					.add("--log-level", "info:debug")
					.add("--log", logFile)
					.add("--log-timestamp")
					.add("--allow-cors")
					.add("--relaxed-security")
					.add("--long-stacktrace")
					.add("--allow-insecure", true);

			if (isAndroid) {

				Integer bootstrapPort = port + 100;
				kill(bootstrapPort);

				Integer chromeDriverPort = port + 200;
				kill(chromeDriverPort);

				Integer systemPort = port + 300;
				kill(systemPort);

				cmd
						.add("--suppress-adb-kill-server")
						.add("--bootstrap-port", bootstrapPort)
						.add("--chromedriver-port", chromeDriverPort)
						.add("--chromedriver-executable", getChromeDriverPath())
						.add("--default-capabilities", String.format("{\\\"systemPort\\\":%s}", systemPort));

			} else {
				Integer wdaProxyPort = port + 400;
				kill(wdaProxyPort);

				Integer wdaLocalPort = port + 500;
				kill(wdaLocalPort);

				cmd
						.add("--webkit-debug-proxy-port", wdaProxyPort)
						.add("--default-capabilities", String.format("{\\\"wdaLocalPort\\\":%s}", wdaLocalPort));
			}

			if (!isBlank(nodeConfig)) {
				cmd.add("--nodeconfig", nodeConfig.getAbsolutePath());
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

		public Integer findFreePort() {
			try {
				LOCK.lock();
				if (!APPIUM_PORTS_QUEUE.isEmpty()) {
					int freePort = APPIUM_PORTS_QUEUE.poll();
					APPIUM_PORTS_MAPPING.put(freePort, "");
					return freePort;
				} else {
					throw new RuntimeException(String.format("no free ports available in range %s - %s",
							APPIUM_PORT_START, APPIUM_PORT_START + TOTAL_PORTS));
				}
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
			String javaHome = getProperty("JAVA_HOME");
			return javaHome.contains("jre") ? new File(javaHome).getParent() : javaHome;
		}

		public void restartEmulators() {
			connectedDevices().stream().parallel().forEach(ADBUtilities::rebootDevice);
		}

		private void startProcess(@NonNull String command) {

			String script = CommonUtilities
					.toString(AppiumLocalService.class.getResourceAsStream("/scripts/wd-session.bash"));
			File scriptFile = new File(String.format("wd-session-%s.bash", System.currentTimeMillis()));
			log.info("******************** starting appium session for device '{}' on port {} ********************",
					deviceId, port);
			new Thread(() -> {
				try {
					String cmd = String.format("bash %s %s %s %s", scriptFile.getAbsolutePath(), getJavaHome(),
							DEFAULT_ANDROID_HOME, command);
					java.nio.file.Files.write(scriptFile.toPath(), script.getBytes());
					getRuntime().exec(cmd).waitFor(30, TimeUnit.SECONDS);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					scriptFile.delete();
				}
			}).start();
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
				return map.entrySet().stream()
						.map(e -> e.getKey() + (isBlank(e.getValue()) ? ""
								: (Boolean.class.equals(e.getValue().getClass())
										|| Integer.class.equals(e.getValue().getClass())) ? " " + e.getValue()
												: " \"" + e.getValue() + "\""))
						.collect(Collectors.joining(" "));
			}
		}

		@SneakyThrows
		public Path getLogFile(@NonNull String sessionId) {

			Path path = Paths.get(LOG_DIRECTORY, "session-logs", requestId + ".log");

			if (path.toFile().exists()) {
				log.info("log path '{}' exists", path);
				return path;
			}

			String input = new String(Files.readAllBytes(Paths.get(LOG_DIRECTORY, deviceId + ".log")));
			String regex = String
					.format("(Request idempotency key((?!Request idempotency key).)*?Removing session %s from our master session list)",
							sessionId);
			Matcher matcher = Pattern.compile(regex, Pattern.DOTALL | Pattern.MULTILINE).matcher(input);
			if (matcher.find()) {

				if (!path.toFile().getParentFile().exists()) {
					path.toFile().getParentFile().mkdirs();
				}
				Files.write(path, matcher.group().getBytes(), StandardOpenOption.CREATE);
				return path;
			}

			return null;
		}

		public Path startVideoCapture(@NonNull String requestId) {
			return runVideoRecording(requestId, "start");
		}

		public Path stopVideoCapture(@NonNull String requestId) {
			return runVideoRecording(requestId, "stop");
		}

		private Path runVideoRecording(@NonNull String requestId, @NonNull String action) {

			log.info("request '{}' ::: {} video recording", requestId, action);

			if (!DeviceSyncProcessor.isAndroidDevice(deviceId) && deviceId.contains("-")) {
				log.error("video recording is not supported for ios simulators");
				return null;
			}

			File file = Paths.get(LOG_DIRECTORY, "session-videos", requestId + ".mp4").toFile();
			if (!file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}

			String[] args = { "flick", "video", "--action", action, "--platform",
					DeviceSyncProcessor.isAndroidDevice(deviceId) ? "android" : "ios",
					"--outdir", file.getParentFile().toString(), "--udid", deviceId, "--name", requestId, "--extend",
					"true", "--count", "1000" };
			new Thread(() -> {
				CommandLineResponse response = exec(String.join(" ", args));
				if (response.getExitCode() != 0) {
					log.info("request '{}' ::: {} video recording failed with exit code '{}' and message '{}'",
							requestId, action, response.getExitCode(), response.getStdOut());
				}
			}).start();
			return file.toPath();
		}

	}

}
