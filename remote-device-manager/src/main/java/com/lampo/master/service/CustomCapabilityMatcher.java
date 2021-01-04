package com.lampo.master.service;

import static com.lampo.master.utils.StringUtils.isBlank;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.lampo.master.model.AllocationStrategy;
import com.lampo.master.model.Device;
import com.lampo.master.model.DeviceRequest;
import com.lampo.master.repos.IDeviceRepository;
import com.lampo.master.utils.StringUtils;

import lombok.NonNull;

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
public class CustomCapabilityMatcher implements ICapabilityMatcher {

	@Autowired
	private IDeviceRepository deviceRepository;

	@Override
	public Optional<Device> match(@NonNull DeviceRequest request) {

		return deviceRepository.findFreeDevices()
				.stream().parallel()
				.filter(e -> e.getAllocatedFor() == AllocationStrategy.AUTOMATION)
				.filter(e -> checkBlacklistedDevice(e))
				.filter(e -> checkDeviceKind(request, e))
				.filter(e -> checkDeviceType(request, e))
				.filter(e -> checkDeviceName(request, e))
				.filter(e -> checkDeviceVersion(request, e))
				.filter(e -> checkDeviceBrand(request, e))
				.findAny();
	}

	private boolean checkBlacklistedDevice(@NonNull Device device) {
		return !device.isBlacklisted();
	}

	private boolean checkDeviceKind(@NonNull DeviceRequest request, @NonNull Device device) {
		return isBlank(request.getIsAndroid())
				|| String.valueOf(device.getDeviceInformation().isAndroid())
						.equalsIgnoreCase(request.getIsAndroid().trim());
	}

	private boolean checkDeviceType(@NonNull DeviceRequest request, @NonNull Device device) {
		return isBlank(request.getIsRealDevice())
				|| String.valueOf(device.getDeviceInformation().isRealDevice())
						.equalsIgnoreCase(request.getIsRealDevice().trim());
	}

	private boolean checkDeviceBrand(@NonNull DeviceRequest request, @NonNull Device device) {
		return isBlank(request.getBrand())
				|| splitAndMatch(device.getDeviceInformation().getManufacturer(), request.getBrand());
	}

	private static boolean splitAndMatch(String text, String split) {
		if (text == null || split == null) {
			return false;
		}
		return Arrays.stream(split.trim().split(","))
				.map(e -> e.trim().toLowerCase())
				.anyMatch(e -> text.toLowerCase().contains(e));
	}

	private boolean checkDeviceVersion(@NonNull DeviceRequest request, @NonNull Device device) {
		String version = isBlank(request.getVersion()) ? null
				: Arrays.stream(request.getVersion().split(",")).map(String::trim)
						.map(StringUtils::getMajorVersion).collect(Collectors.joining(","));
		return isBlank(version) || splitAndMatch(device.getDeviceInformation().getSdkVersion(), version);
	}

	private boolean checkDeviceName(@NonNull DeviceRequest request, @NonNull Device device) {
		return checkDeviceName(request.getDeviceName(), device);
	}

	private boolean checkDeviceName(String deviceName, Device device) {
		return isBlank(deviceName)
				|| splitAndMatch(device.getDeviceInformation().getMarketName(), deviceName.toLowerCase())
				|| splitAndMatch(device.getDeviceInformation().getModel(), deviceName.toLowerCase());
	}

}
