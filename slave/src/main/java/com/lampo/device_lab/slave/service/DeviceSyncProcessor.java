package com.lampo.device_lab.slave.service;

import static com.lampo.device_lab.slave.utils.ADBUtilities.getConnectedDevices;
import static com.lampo.device_lab.slave.utils.ADBUtilities.uninstallApp;
import static com.lampo.device_lab.slave.utils.AppiumLocalService.getRunningAppiumSessions;
import static com.lampo.device_lab.slave.utils.CommonUtilities.getLocalNetworkIP;
import static com.lampo.device_lab.slave.utils.CommonUtilities.isNotBlank;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.lampo.device_lab.slave.model.Capability;
import com.lampo.device_lab.slave.model.ClearDataRequest;
import com.lampo.device_lab.slave.model.DeviceManagerException;
import com.lampo.device_lab.slave.model.DeviceUpdateRequest;
import com.lampo.device_lab.slave.model.GridConfiguration;
import com.lampo.device_lab.slave.model.GridNodeRegistrationRequest;
import com.lampo.device_lab.slave.model.IDeviceProperty;
import com.lampo.device_lab.slave.model.OpenSTFHeldRequest;
import com.lampo.device_lab.slave.model.UninstallRequest;
import com.lampo.device_lab.slave.utils.ADBUtilities;
import com.lampo.device_lab.slave.utils.AppiumLocalService;
import com.lampo.device_lab.slave.utils.AppiumLocalService.Builder;
import com.lampo.device_lab.slave.utils.CommandLineExecutor;
import com.lampo.device_lab.slave.utils.IOSUtilities;

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
@Component
@Slf4j
public class DeviceSyncProcessor {

	private static final String DEVICE_UPDATE_QUEUE_NAME = "devices";
	private static final ReentrantLock LOCK = new ReentrantLock(true);
	private static final String STF_DEVICE_UPDATE_QUEUE_NAME = "stf-devices";

	@Autowired
	private RabbitTemplate rabbitTemplate;

	private static Collection<String> androidDeviceIds = new TreeSet<>();
	private static Collection<String> iosDeviceIds = new TreeSet<>();

	private static final File NODE_CONFIG_PARENT = new File("node-config");

	@Value("${master.host}")
	private String hubHost;

	@Value("${master.port}")
	private String port;

	@Autowired
	public RestTemplate template;

	@Autowired
	private OpenSTFService openSTFService;

	@PostConstruct
	public void fixHubHost() {
		if (hubHost.contains("//")) {
			hubHost = hubHost.split("//")[1].trim();
		}
		if (hubHost.contains(":")) {
			hubHost = hubHost.split(":")[0].trim();
		}
	}

	@Scheduled(cron = "${cron.stf.device_sync}")
	public void updateSTFDeviceStatusToMaster() {

		OpenSTFHeldRequest request = new OpenSTFHeldRequest(getLocalNetworkIP(), openSTFService.getBusyDevices());
		rabbitTemplate.convertAndSend(STF_DEVICE_UPDATE_QUEUE_NAME, request);

	}

	@Scheduled(cron = "${cron.device_sync}")
	public boolean updateDeviceInfoToMaster() {

		try {
			LOCK.lock();

			long start = System.currentTimeMillis();

			DeviceUpdateRequest request = new DeviceUpdateRequest();
			request.setIp(getLocalNetworkIP());

			request.setAndroidDevices(getConnectedDevices().values());
			request.setIosDevices(IOSUtilities.getConnectedDevices().values());

			rePopulateDeviceIds(request);

			rabbitTemplate.convertAndSend(DEVICE_UPDATE_QUEUE_NAME, request);

			long timeTaken = System.currentTimeMillis() - start;
			log.debug("***** {} ms taken to send data {} *****", timeTaken, getEssentialInfo(request));

			return true;
		} finally {
			LOCK.unlock();
		}
	}

	private String getEssentialInfo(@NonNull DeviceUpdateRequest request) {

		List<String> devices = Stream.of(request.getAndroidDevices(), request.getIosDevices())
				.flatMap(Collection::stream).sorted((e1, e2) -> Boolean.compare(e1.isAndroid(), e2.isAndroid()))
				.map(e -> (e.isAndroid() ? "android" : "ios") + " # " + e.getDeviceId()).collect(Collectors.toList());

		int total = request.getIosDevices().size() + request.getAndroidDevices().size();
		int android = request.getAndroidDevices().size();
		return String.format("ip: %s, total devices: %s, android: %s, ios: %s, devices info: %s ", request.getIp(),
				total, android, total - android, devices);
	}

	private void rePopulateDeviceIds(@NonNull DeviceUpdateRequest request) {
		androidDeviceIds.clear();
		iosDeviceIds.clear();

		Stream.of(request.getAndroidDevices(), request.getIosDevices()).flatMap(Collection::stream).parallel()
				.forEach(this::startAndRegisterSession);

		cleanUpSessions(request);

	}

	private void cleanUpSessions(DeviceUpdateRequest request) {

		Set<String> allDevices = Stream.concat(androidDeviceIds.stream(), iosDeviceIds.stream())
				.collect(Collectors.toSet());

		Map<String, Integer> sessions = getRunningAppiumSessions();

		if (sessions == null || sessions.isEmpty()) {
			return;
		}

		Set<String> diff = Sets.difference(sessions.keySet(), allDevices);
		if (!diff.isEmpty()) {
			diff.forEach(e -> {
				int appiumPort = sessions.get(e);
				log.info("--------- unregistering appium session of device '{}' running on port '{}'", e, appiumPort);
				String cmd = String.format(
						"ps -ef | grep -i '%s' | grep -Ei 'node.*appium' | grep -v grep | awk '{print $2}' | tr -s '\\n' ' ' | xargs kill -9",
						e);
				CommandLineExecutor.exec(cmd);
				removingDeviceOnMaster(e, request.getIp());
			});
		}

	}

	private void removingDeviceOnMaster(@NonNull String deviceId, @NonNull String slaveIp) {
		URI url = URI.create(String.format("http://%s:%s/device/update_status?action=REMOVE", hubHost, port));
		try {
			log.debug("removing device '{}': {}", deviceId, template.postForObject(url,
					ImmutableMap.of("device_id", deviceId, "slave_ip", slaveIp), Boolean.class));
		} catch (Exception ex) {
			log.error("{} error occurred while removing device '{} # {}' on master with message '{}",
					ex.getClass().getName(), slaveIp, deviceId, ex.getMessage());
		}
	}

	public void restartAllDevices() {
		Stream.of(androidDeviceIds, iosDeviceIds).flatMap(Collection::stream).parallel().forEach(this::restartDevice);
	}

	public void restartDevice(@NonNull String deviceId) {
		if (isAndroidDevice(deviceId)) {
			ADBUtilities.rebootDevice(deviceId);
		} else {
			IOSUtilities.rebootDevice(deviceId);
		}
	}

	private synchronized void startAndRegisterSession(@NonNull IDeviceProperty property) {

		(property.isAndroid() ? androidDeviceIds : iosDeviceIds).add(property.getDeviceId());

		Map<String, Integer> sessions = getRunningAppiumSessions();

		if (!sessions.keySet().contains(property.getDeviceId())) {

			Builder builder = AppiumLocalService.builder();

			int freePort = builder.findFreePort();

			String ip = getLocalNetworkIP();

			Capability caps = new Capability().setBrowserName(property.isAndroid() ? "chrome" : "safari")
					.setVersion(property.getSdkVersion())
					.setUDID(property.getDeviceId())
					.setDeviceName(property.getDeviceName())
					.setPlatform(property.isAndroid() ? "android" : "ios")
					.setRealDevice(property.isRealDevice())
					.setSlaveIp(ip)
					.setAutomationName(property.isAndroid() ? "uiautomator2" : "xcuitest");

			GridConfiguration gridConfiguration = new GridConfiguration(hubHost, ip, freePort);
			File nodeConfig = new File(NODE_CONFIG_PARENT, property.getDeviceId() + ".json");

			GridNodeRegistrationRequest request = new GridNodeRegistrationRequest(caps, gridConfiguration);
			request.saveToFile(nodeConfig);

			if (null != builder.isAndroid(property.isAndroid()).deviceId(property.getDeviceId()).ip(ip)
					.nodeConfig(nodeConfig).port(freePort).start()) {

				log.info("------- registering device '{}' running appium session on port '{}' to grid '{}' ----------",
						property.getDeviceId(), freePort, hubHost);
			}
		}

	}

	public boolean isConnectedDevice(@NonNull String deviceId) {
		try {
			LOCK.lock();
			return androidDeviceIds.contains(deviceId) || iosDeviceIds.contains(deviceId);
		} finally {
			LOCK.unlock();
		}
	}

	public static boolean isAndroidDevice(@NonNull String deviceId) {
		if (androidDeviceIds.contains(deviceId)) {
			return true;
		} else if (iosDeviceIds.contains(deviceId)) {
			return false;
		} else {
			throw new DeviceManagerException(String.format("'%s' not connected", deviceId));
		}
	}

	public void uninstallApps(@NonNull UninstallRequest request) {
		if (isNotBlank(request.getDeviceId()) && isAndroidDevice(request.getDeviceId())
				&& request.getPackages() != null) {
			request.getPackages().stream().forEach(e -> uninstallApp(request.getDeviceId(), e));
		}
	}

	public void clearUserData(@NonNull ClearDataRequest request) {
		if (isNotBlank(request.getDeviceId()) && isNotBlank(request.getPackageName())) {
			ADBUtilities.clearUserData(request.getDeviceId(), request.getPackageName());
		}
	}

}
