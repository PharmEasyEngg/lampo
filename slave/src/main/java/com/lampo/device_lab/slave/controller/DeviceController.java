package com.lampo.device_lab.slave.controller;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lampo.device_lab.slave.model.ClearDataRequest;
import com.lampo.device_lab.slave.model.UninstallRequest;
import com.lampo.device_lab.slave.service.DeviceSyncProcessor;
import com.lampo.device_lab.slave.utils.AppiumLocalService;
import com.lampo.device_lab.slave.utils.STFServiceBuilder;

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
@RestController
@RequestMapping("/device")
public class DeviceController {

	@Autowired
	private DeviceSyncProcessor processor;

	@PostMapping("/restart_device")
	public void restartDevice(@RequestParam("device_id") String deviceId) {
		processor.restartDevice(deviceId);
	}

	@PostMapping("/restart_all_devices")
	public void restartAllDevices() {
		processor.restartAllDevices();
	}

	@PostMapping("/restart_stf")
	public void restartSTF() {
		STFServiceBuilder.builder().restart();
	}

	@PostMapping("/uninstall_apps")
	public void uninstallApps(@RequestBody UninstallRequest request) {
		log.info("request '{}' ::: executing uninstall app request {}", request.getRequestId(), request);
		processor.uninstallApps(request);
	}

	@PostMapping("/clear_user_data")
	public void clearUserData(@RequestBody ClearDataRequest request) {
		log.info("request '{}' ::: executing clear user data request {}", request.getRequestId(), request);
		processor.clearUserData(request);
	}

	@GetMapping("/logs")
	public Path getLogFile(@RequestParam String sessionId, @RequestParam String deviceId, String requestId) {
		return AppiumLocalService.builder().deviceId(deviceId).requestId(requestId).getLogFile(sessionId);
	}

	@PostMapping("/videos")
	public Path getVideoFile(@RequestParam String action, @RequestParam String deviceId, String requestId) {

		if ("start".equalsIgnoreCase(action)) {
			return AppiumLocalService.builder().deviceId(deviceId).startVideoCapture(requestId);

		} else if ("stop".equalsIgnoreCase(action)) {
			return AppiumLocalService.builder().deviceId(deviceId).stopVideoCapture(requestId);
		}
		return null;

	}
}
