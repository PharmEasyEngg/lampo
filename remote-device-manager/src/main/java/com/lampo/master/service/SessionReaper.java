package com.lampo.master.service;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.lampo.master.model.DeviceRestrictionRequest;
import com.lampo.master.repos.IDeviceRepository;

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
@Component
public class SessionReaper {

	private static final int SOCKET_TIME_OUT_IN_MSEC = 2000;
	private static final int DEFAULT_SLAVE_PORT = 5252;

	@Autowired
	private IDeviceRepository deviceRepo;

	@Autowired
	private AllocationService allocationService;

	@Value("${max_session_duration}")
	private int maxSessionDurationInSec;

	@Scheduled(cron = "${cron.reap_unreachable_slaves}")
	public void reapUnreacheableSlaves() {
		deviceRepo.findAll().stream().parallel().forEach(e -> {
			if (!isReachable(e.getSlaveIp(), DEFAULT_SLAVE_PORT, SOCKET_TIME_OUT_IN_MSEC)) {
				log.info("reaping unreacheable device '{}' on host '{}'", e.getId(), e.getSlaveIp());
				deviceRepo.delete(e);
			}
		});
	}

	@Scheduled(cron = "${cron.reap_long_running_sessions}")
	public void reapLongRunningSessions() {
		deviceRepo.findAll().stream()
				.parallel()
				.filter(e -> !e.isFree() && e.getLastAllocationStart() != null && e.getLastAllocationEnd() == null
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
					}
				});
	}

	private static boolean isReachable(String address, int port, int timeout) {
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
