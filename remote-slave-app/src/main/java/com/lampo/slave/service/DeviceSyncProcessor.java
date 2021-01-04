package com.lampo.slave.service;

import static com.lampo.slave.utils.ADBUtilities.getConnectedDevices;
import static com.lampo.slave.utils.StringUtils.getLocalNetworkIP;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.lampo.slave.model.DeviceUpdateRequest;
import com.lampo.slave.model.IDeviceProperty;
import com.lampo.slave.utils.IOSUtilities;

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
@Component
@Slf4j
public class DeviceSyncProcessor {

	private static final String DEVICE_UPDATE_QUEUE_NAME = "devices";
	private static final ReentrantLock LOCK = new ReentrantLock(true);

	@Autowired
	private RabbitTemplate rabbitTemplate;

	private Collection<String> androidDeviceIds = new HashSet<>();
	private Collection<String> iosDeviceIds = new HashSet<>();

	@Scheduled(cron = "${cron.device_sync}")
	public boolean updateDeviceInfoToMaster() {

		try {
			LOCK.lock();

			long start = System.currentTimeMillis();

			DeviceUpdateRequest request = new DeviceUpdateRequest();
			request.setIp(getLocalNetworkIP());

			Collection<IDeviceProperty> androidDevices = getConnectedDevices().values();
			request.setAndroidEmulators(
					androidDevices.stream().filter(e -> !e.isRealDevice()).collect(toList()));
			request.setAndroidRealDevices(
					androidDevices.stream().filter(IDeviceProperty::isRealDevice).collect(toList()));
			request.setIosRealDevices(IOSUtilities.getConnectedRealDevices().values());
			request.setIosSimulators(IOSUtilities.getConnectedSimulators().values());

			rePopulateDeviceIds(request);

			rabbitTemplate.convertAndSend(DEVICE_UPDATE_QUEUE_NAME, request);

			long timeTaken = System.currentTimeMillis() - start;
			log.info("***** {} ms taken to send data {} *****", timeTaken, request);

			return true;
		} finally {
			LOCK.unlock();
		}
	}

	private void rePopulateDeviceIds(DeviceUpdateRequest request) {
		androidDeviceIds.clear();
		iosDeviceIds.clear();
		request.getAndroidEmulators().forEach(e -> androidDeviceIds.add(e.getDeviceId()));
		request.getAndroidRealDevices().forEach(e -> androidDeviceIds.add(e.getDeviceId()));
		request.getIosRealDevices().forEach(e -> iosDeviceIds.add(e.getDeviceId()));
		request.getIosSimulators().forEach(e -> iosDeviceIds.add(e.getDeviceId()));

	}

	public boolean isAndroid(@NonNull String deviceId) {
		try {
			LOCK.lock();
			return androidDeviceIds.contains(deviceId);
		} finally {
			LOCK.unlock();
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

}
