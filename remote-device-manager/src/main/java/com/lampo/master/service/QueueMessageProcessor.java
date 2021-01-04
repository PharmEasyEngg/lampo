package com.lampo.master.service;

import static com.lampo.master.config.ClientConfiguration.DEVICE_UPDATE_QUEUE_NAME;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.lampo.master.model.Device;
import com.lampo.master.model.DeviceInformation;
import com.lampo.master.model.DeviceUpdateRequest;
import com.lampo.master.repos.IDeviceRepository;

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

	@RabbitListener(queues = DEVICE_UPDATE_QUEUE_NAME)
	public void process(@NonNull DeviceUpdateRequest deviceUpdateRequest) {

		log.debug("received message from queue => {}", deviceUpdateRequest);

		Collection<Entry<String, String>> oldDevices = deviceRepo.findBySlaveIp(deviceUpdateRequest.getIp())
				.stream()
				.map(e -> new SimpleEntry<>(e.getId(), e.getSlaveIp()))
				.collect(Collectors.toList());

		Collection<Entry<String, String>> currentDevices = Stream
				.of(deviceUpdateRequest.getAndroidEmulators(), deviceUpdateRequest.getAndroidRealDevices(),
						deviceUpdateRequest.getIosRealDevices(), deviceUpdateRequest.getIosSimulators())
				.filter(e -> e != null && !e.isEmpty())
				.flatMap(Collection::stream)
				.map(info -> getUpdatedDeviceInfo(deviceUpdateRequest, info))
				.collect(Collectors.toList());

		removeDisconnectedDevices(oldDevices, currentDevices);
	}

	private Entry<String, String> getUpdatedDeviceInfo(@NonNull DeviceUpdateRequest deviceUpdateRequest,
			@NonNull DeviceInformation info) {
		Optional<Device> device = deviceRepo.findById(info.getDeviceId(), deviceUpdateRequest.getIp());
		Device _device = null;
		if (!device.isPresent()) {
			_device = new Device();
			_device.setId(info.getDeviceId());
			_device.setFree(true);
		} else {
			_device = device.get();
		}
		_device.setDeviceInformation(info);
		_device.setSlaveIp(deviceUpdateRequest.getIp());
		_device.setLastModifiedTime(new Date());
		deviceRepo.save(_device);
		return new SimpleEntry<>(_device.getId(), _device.getSlaveIp());
	}

	private void removeDisconnectedDevices(@NonNull Collection<Entry<String, String>> oldDevices,
			@NonNull Collection<Entry<String, String>> currentDevices) {
		oldDevices.stream()
				.filter(e -> !currentDevices.contains(e))
				.forEach(e -> deviceRepo.deleteById(e.getKey(), e.getValue()));
	}

}