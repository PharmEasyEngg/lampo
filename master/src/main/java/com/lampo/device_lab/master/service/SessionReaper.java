package com.lampo.device_lab.master.service;

import static com.lampo.device_lab.master.utils.CommonUtilities.getMatches;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.lampo.device_lab.master.grid.GridSetupService;
import com.lampo.device_lab.master.model.DeviceRestrictionRequest;
import com.lampo.device_lab.master.repos.IDeviceRepository;

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
@Slf4j
@Component
public class SessionReaper {

	private static final int SOCKET_TIME_OUT_IN_MSEC = 2000;
	private static final String GRID_STATUS_URL = String.format("http://127.0.0.1:%s/grid/console",
			GridSetupService.GRID_PORT);

	@Value("${slave.port}")
	private int slavePort;

	@Autowired
	private IDeviceRepository deviceRepo;

	@Autowired
	private AllocationService allocationService;

	@Value("${custom.max_session_duration}")
	private int maxSessionDurationInSec;

	@Value("${custom.maintenance.enabled}")
	private boolean isMaintenanceEnabled;

	@Value("${custom.reap_sessions.enabled}")
	private boolean isSessionReapEnabled;

	@Scheduled(cron = "${custom.cron.reap_unreachable_slaves}")
	public void reapUnreacheableSlaves() {
		if (!isMaintenanceEnabled) {
			log.debug("'custom.maintenance.enabled' is set to false");
			return;
		}
		deviceRepo.findAll().stream().parallel().forEach(e -> {
			if (e.getSlaveIp() != null && !isReachable(e.getSlaveIp(), slavePort, SOCKET_TIME_OUT_IN_MSEC)) {
				log.debug("reaping unreacheable device '{}' on host '{}'", e.getId(), e.getSlaveIp());
				deviceRepo.delete(e);
			}
		});
	}

	public void reapAll() {
		this.reapUnreacheableSlaves();
		this.reapDeadSessions();
		this.reapLongRunningSessions();
	}

	@Scheduled(cron = "${custom.cron.reap_long_running_sessions}")
	public void reapLongRunningSessions() {
		if (!isSessionReapEnabled) {
			log.debug("'custom.maintenance.enabled' is set to false");
			return;
		}
		deviceRepo
				.findAll().stream().parallel().filter(
						e -> !e.isFree() && e.getLastAllocationStart() != null && e.getLastAllocationEnd() == null
								&& (System.currentTimeMillis()
										- e.getLastAllocationStart().getTime() >= maxSessionDurationInSec * 1000))
				.forEach(e -> {
					try {
						log.info("reaping long running session on device '{}' ::: running since '{}'", e);
						allocationService.unallocateDevice(new DeviceRestrictionRequest(e.getId(), e.getSlaveIp()),
								null);
					} catch (Throwable ex) {
						log.error("{} occurred when calling reapLongRunningSessions() with message: '{}'",
								ex.getClass().getName(), ex.getMessage());
						ex.printStackTrace();
					}
				});
	}

	@Scheduled(cron = "${custom.cron.reap_dead_sessions}")
	@SneakyThrows
	public void reapDeadSessions() {
		reapDeadSessions(false);
	}

	public void reapDeadSessions(boolean overrideFlag) {
		if (!isSessionReapEnabled && !overrideFlag) {
			log.debug("'custom.reap_sessions.enabled' is set to false");
			return;
		}

		try {

			log.debug("reaping dead sessions.......");

			Document doc = Jsoup.connect(GRID_STATUS_URL).get();

			Map<String, List<String>> busyDevices = new HashMap<>();

			doc.select("div.proxy").stream().forEach(elm -> {
				Element session = elm.selectFirst("div.content_detail > p > img");

				String config = elm.selectFirst("div[type=config]").text();
				String deviceId = getMatches(config, "(?<=udid: ).*?(?=,)").get(0);

				boolean isBusy = session.hasClass("busy") || session.attr("title").contains("executing");
				if (isBusy) {
					String slaveIp = getMatches(elm.text(), "(?<=host: ).*?(?= )").get(0).trim();
					if (!busyDevices.containsKey(slaveIp)) {
						busyDevices.put(slaveIp, new ArrayList<>());
					}
					List<String> devices = busyDevices.get(slaveIp);
					devices.add(deviceId);
					busyDevices.put(slaveIp, devices);
				}
			});

			log.debug("busy devices on grid => {}", busyDevices);

			deviceRepo.findAll().forEach(device -> {
				if (!device.isFree() && (busyDevices.isEmpty() || (busyDevices.containsKey(device.getSlaveIp())
						&& !busyDevices.get(device.getSlaveIp()).contains(device.getId())))) {
					allocationService
							.unallocateDevice(new DeviceRestrictionRequest(device.getId(), device.getSlaveIp()), null);
				}
			});
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private boolean isReachable(@NonNull String address, int port, int timeout) {
		try {
			try (java.net.Socket socket = new java.net.Socket()) {
				socket.setSoTimeout(timeout);
				socket.connect(new java.net.InetSocketAddress(address, port), timeout);
			}
			return true;
		} catch (IOException e) {
			return false;
		}
	}
}
