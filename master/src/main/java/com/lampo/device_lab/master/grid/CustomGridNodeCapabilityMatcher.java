package com.lampo.device_lab.master.grid;

import static com.lampo.device_lab.master.utils.CommonUtilities.getMajorVersion;
import static com.lampo.device_lab.master.utils.CommonUtilities.isBlank;
import static com.lampo.device_lab.master.utils.CommonUtilities.search;

import java.util.Map;

import org.openqa.grid.internal.utils.CapabilityMatcher;

import com.lampo.device_lab.master.model.NodeCapability;

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
public class CustomGridNodeCapabilityMatcher implements CapabilityMatcher {

	@Override
	public boolean matches(Map<String, Object> currentCapability, Map<String, Object> requestedCapability) {

		NodeCapability current = extractCapability(currentCapability);
		NodeCapability requested = extractCapability(requestedCapability);
		boolean matchFound = match(current, requested);
		log.debug("request '{}' ::: requested '{}' and current '{}' ::: match found ? {}", requested.getRequestId(),
				requested, current, matchFound);
		return matchFound;
	}

	public static boolean match(NodeCapability current, NodeCapability requested) {

		if (!isBlank(requested.getUdid()) && !current.getUdid().equals(requested.getUdid())) {
			log.debug("request '{}' ::: current node udid '{}' != requested node udid '{}'", requested.getRequestId(),
					current.getUdid(), requested.getUdid());
			return false;
		}
		if (!isBlank(requested.getPlatform()) && !current.getPlatform().equalsIgnoreCase(requested.getPlatform())) {
			log.debug("request '{}' ::: current node platform '{}' != requested node platform '{}'",
					current.getPlatform(), requested.getPlatform());
			return false;
		}

		if (!isBlank(requested.getDeviceName())
				&& !toLowerCase(current.getDeviceName()).contains(toLowerCase(requested.getDeviceName()))) {
			log.debug("request '{}' ::: current node device name '{}' != requested node device name '{}'",
					requested.getRequestId(), current.getDeviceName(), requested.getDeviceName());
			return false;
		}

		if (!isBlank(requested.getBrand())
				&& !toLowerCase(current.getDeviceName()).contains(toLowerCase(requested.getBrand()))) {
			log.debug("request '{}' ::: current node brand '{}' != requested node brand '{}'", requested.getRequestId(),
					current.getBrand(), requested.getBrand());
			return false;
		}

		if (!isBlank(requested.getVersion())) {
			String requestedMajorVersion = getMajorVersion(requested.getVersion());
			String currentMajorVersion = getMajorVersion(current.getVersion());
			if (!requestedMajorVersion.equals(currentMajorVersion)) {
				log.debug("request '{}' ::: current node OS major version '{}' != requested node OS major version '{}'",
						requested.getRequestId(), currentMajorVersion, requestedMajorVersion);
				return false;
			}
		}
		boolean realDeviceMatch = requested.getIsRealDevice() == null
				|| requested.getIsRealDevice().equals(current.getIsRealDevice());

		if (!realDeviceMatch) {
			log.debug("request '{}' ::: current node real device '{}' != requested node real device '{}'",
					requested.getRequestId(), current.getIsRealDevice(), requested.getIsRealDevice());
		}
		return realDeviceMatch;

	}

	private static String toLowerCase(String str) {
		return str == null ? null : str.toLowerCase();
	}

	public static NodeCapability extractCapability(Map<String, Object> caps) {
		NodeCapability cap = new NodeCapability();
		cap.setDeviceName(search(caps, "devicename"));
		cap.setBrand(search(caps, "brand"));
		cap.setVersion(search(caps, "platformversion"));
		Object platform = search(caps, "platformname");
		cap.setPlatform(platform == null ? null : platform.toString());
		cap.setUdid(search(caps, "udid"));
		Object isRealDevice = search(caps, "realDevice");
		if (isRealDevice != null) {
			isRealDevice = isRealDevice.toString();
		}
		cap.setIsRealDevice(isRealDevice == null || isBlank(isRealDevice.toString()) ? null
				: "true".equalsIgnoreCase(isRealDevice.toString()));
		cap.setRequestId(CustomCapability.search(caps, CustomCapability.REQUEST_ID));
		return cap;
	}

}
