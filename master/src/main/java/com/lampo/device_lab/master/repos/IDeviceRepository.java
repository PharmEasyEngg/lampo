package com.lampo.device_lab.master.repos;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.lampo.device_lab.master.model.Device;

import lombok.NonNull;

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
public interface IDeviceRepository extends MongoRepository<Device, String> {

	@Override
	default Optional<Device> findById(String id) {
		return findAll().stream().filter(e -> e.getId().equals(id)).findFirst();
	}

	public Collection<Device> findBySlaveIp(String slaveIp);

	default Collection<Device> findFreeDevices() {
		return findAll().stream()
				.filter(e -> e.isFree() && e.isConnected())
				.collect(Collectors.toList());
	}

	default Collection<Device> findFreeAndroidDevices() {
		return findAll().stream()
				.filter(e -> e.isFree() && e.isConnected() && e.getDeviceInformation() != null
						&& e.getDeviceInformation().isAndroid())
				.collect(Collectors.toList());
	}

	default Collection<Device> findFreeIosDevices() {
		return findAll().stream()
				.filter(e -> e.isFree() && e.isConnected() && e.getDeviceInformation() != null
						&& !e.getDeviceInformation().isAndroid())
				.collect(Collectors.toList());
	}

	default Collection<Device> findAndroidDevices() {
		return findAll().stream()
				.filter(e -> e.getDeviceInformation() != null && e.getDeviceInformation().isAndroid())
				.collect(Collectors.toList());
	}

	default Collection<Device> findIosDevices() {
		return findAll().stream()
				.filter(e -> e.getDeviceInformation() != null && !e.getDeviceInformation().isAndroid())
				.collect(Collectors.toList());
	}

	default Optional<Device> findById(String deviceId, String slaveIp) {
		return findAll().stream().filter(e -> e.getId().equals(deviceId) && e.getSlaveIp().equals(slaveIp)).findFirst();
	}

	default void deleteById(@NonNull String deviceId, @NonNull String slaveIp) {
		Optional<Device> device = findById(deviceId, slaveIp);
		if (device.isPresent()) {
			delete(device.get());
		}
	}

	default Collection<String> findSlaves() {
		return findAll().stream().map(Device::getSlaveIp).collect(Collectors.toSet());
	}

}