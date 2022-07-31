package com.lampo.device_lab.slave.service;

import static com.lampo.device_lab.slave.utils.ADBUtilities.getDevicesGroup;
import static com.lampo.device_lab.slave.utils.ADBUtilities.restartADBServer;
import static com.lampo.device_lab.slave.utils.CommonUtilities.sleep;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.lampo.device_lab.slave.model.IDeviceProperty;
import com.lampo.device_lab.slave.utils.ADBUtilities;
import com.lampo.device_lab.slave.utils.AppiumLocalService;
import com.lampo.device_lab.slave.utils.STFServiceBuilder;

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
@Component
@Slf4j
public class MaintenanceService {

	private static final String EMULATORS_NAME_PREFIX = "pixel-";

	private List<String> emulatorsList;

	@Value("${list.emulators:}")
	private String emulators;

	@Value("${stf.enabled}")
	private boolean isSTFEnabled;

	@Value("${restart_emulator.enabled}")
	private boolean restartEmulatorEnabled;

	@Value("${restart_adb.enabled}")
	private boolean restartADBEnabled;

	@PostConstruct
	public void updateEmulatorsList() {

		try {
			emulatorsList = (emulators == null || emulators.trim().isEmpty())
					? Collections.emptyList()
					: Arrays.stream(emulators.split(","))
							.map(e -> e.trim().split("\\-")[1].trim()).sorted().collect(Collectors.toList());
		} catch (Exception ex) {
			emulatorsList = Collections.emptyList();
		}
	}

	/**
	 * cleaning up old logs at 00:00 daily
	 */
	@Scheduled(cron = "${cron.clean_up_logs}")
	@SneakyThrows
	public void cleanOldLogs() {

		log.info("********************** cleaning up old logs **********************");

		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.add(Calendar.DATE, -1);
		Date threshold = cal.getTime();

		Files.walk(Paths.get(AppiumLocalService.Builder.LOG_DIRECTORY))
				.filter(e -> Files.isRegularFile(e) && new Date(e.toFile().lastModified()).before(threshold))
				.forEach(t -> {
					try {
						Files.deleteIfExists(t);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				});
	}

	@Scheduled(cron = "${cron.restart_emulators}")
	public void restartEmulators() {
		if (!restartEmulatorEnabled) {
			return;
		}
		log.info("********************** rebooting emulators **********************");
		AppiumLocalService.builder().restartEmulators();
	}

	@Scheduled(cron = "${cron.restart_stf}")
	public void restartSTF() {
		if (!isSTFEnabled) {
			return;
		}
		log.info("********************** restarting stf **********************");
		STFServiceBuilder.builder().restart();
	}

	@Scheduled(cron = "${cron.restart_adb}")
	public void restartADB() {

		if (!restartADBEnabled) {
			return;
		}

		log.info("********************** restarting adb **********************");

		Map<String, List<String>> devices = getDevicesGroup();

		Collection<String> devicesOfRunningAppiumSessions = AppiumLocalService.Builder
				.getDevicesOfRunningAppiumSessions();
		log.info("connected devices => {} - running appium sessions - {}", devices, devicesOfRunningAppiumSessions);
		if (!(devices.getOrDefault("unauthorized", Collections.emptyList())).isEmpty() ||
				!(devices.getOrDefault("offline", Collections.emptyList())).isEmpty()
				|| devicesOfRunningAppiumSessions
						.isEmpty()) {
			restartADBServer();
		}
	}

	@Scheduled(cron = "${cron.check_stf_service}")
	public void checkingSTFService() {

		if (!isSTFEnabled) {
			return;
		}

		if (!STFServiceBuilder.builder().isSTFRunning()) {
			STFServiceBuilder.builder().restart();
		}
	}

	@Scheduled(cron = "${cron.check_emulators}")
	public void rebootEmulators() {

		List<String> runningEmulators = getRunningEmulators(null);

		if (!emulatorsList.isEmpty()) {
			log.debug("running emulators: {} and expected emulators: {}", runningEmulators, emulatorsList);

			AtomicBoolean found = new AtomicBoolean(false);
			try {
				emulatorsList.stream().parallel().forEach(str -> {
					if (!runningEmulators.contains(str)) {
						ADBUtilities.restartEmulator(EMULATORS_NAME_PREFIX + str);
						found.set(true);
					}
				});
			} finally {
				if (found.get()) {
					new Thread(() -> {
						long end = System.currentTimeMillis() + 120000;
						while (true) {
							if (System.currentTimeMillis() < end && getRunningEmulators(null).equals(emulatorsList)) {
								sleep(5000);
								break;
							}
							sleep(10000);
						}
					}).start();
				}
			}
		}
	}

	private String getMajorVersion(String value) {
		return value.split("\\.")[0];
	}

	private List<String> getRunningEmulators(Collection<IDeviceProperty> emulators) {
		if (emulators == null) {
			emulators = ADBUtilities.getConnectedEmulators();
		}
		return emulators.stream()
				.map(e -> getMajorVersion(e.getSdkVersion()))
				.sorted()
				.collect(Collectors.toList());
	}
}
