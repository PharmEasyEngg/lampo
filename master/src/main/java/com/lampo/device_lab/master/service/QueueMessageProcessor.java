package com.lampo.device_lab.master.service;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.lampo.device_lab.master.model.Device;
import com.lampo.device_lab.master.model.DeviceInformation;
import com.lampo.device_lab.master.model.DeviceUpdateRequest;
import com.lampo.device_lab.master.model.HeldBy;
import com.lampo.device_lab.master.model.OpenSTFHeldRequest;
import com.lampo.device_lab.master.repos.IDeviceRepository;

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
@Component
public class QueueMessageProcessor {

	@Autowired
	private IDeviceRepository deviceRepo;

	@RabbitListener(queues = { "devices" })
	public void process(@NonNull DeviceUpdateRequest request) {

		log.debug("received message from queue => {}", request);

		Collection<DeviceInformation> devices = Stream.of(request.getAndroidDevices(), request.getIosDevices())
				.filter(Objects::nonNull).flatMap(Collection::stream).collect(Collectors.toList());

		devices.stream().parallel().forEach(e -> deviceRepo.save(getDevice(request, e)));

		List<String> connectedDevices = devices.stream().map(DeviceInformation::getDeviceId)
				.collect(Collectors.toList());

		deviceRepo.findBySlaveIp(request.getIp()).stream().filter(e -> !connectedDevices.contains(e.getId()))
				.forEach(deviceRepo::delete);

	}

	@RabbitListener(queues = { "stf-devices" })
	public void processSTFDevices(@NonNull OpenSTFHeldRequest request) {

		log.debug("received message from queue => {}", request);
		String ip = request.getIp();
		Map<String, HeldBy> devices = request.getDevices();
		deviceRepo.findBySlaveIp(ip).stream()
				.map(device -> setHeldBy(devices, device))
				.filter(Objects::nonNull)
				.forEach(deviceRepo::save);
	}

	private Device setHeldBy(Map<String, HeldBy> devices, Device device) {
		HeldBy heldBy = null;
		if (devices != null && devices.containsKey(device.getId())) {
			heldBy = devices.get(device.getId());

			String name = heldBy.getName();
			if (name.contains(" ")) {
				name = name.split("\\s+")[0].trim();
			}
			if (name.contains(".")) {
				name = name.split("\\.")[0].trim();
			}
			heldBy.setName(name.toUpperCase());
		}
		device.setStfSessionHeldBy(heldBy);
		return device;
	}

	private Device getDevice(DeviceUpdateRequest request, DeviceInformation e) {
		Optional<Device> oDevice = deviceRepo.findById(e.getDeviceId(), request.getIp());
		Device device = null;
		if (!oDevice.isPresent()) {
			device = new Device();
			device.setId(e.getDeviceId());
			device.setFree(true);
		} else {
			device = oDevice.get();
		}
		device.setSlaveIp(request.getIp());
		device.setConnected(true);
		device.setDeviceInformation(e);
		device.setLastModifiedTime(new Date());
		return device;
	}

}