package com.lampo.device_lab.master.controller;

import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lampo.device_lab.master.grid.DeviceMatchRequest;
import com.lampo.device_lab.master.model.DeviceInfo;
import com.lampo.device_lab.master.model.DeviceStatus;
import com.lampo.device_lab.master.service.AllocationService;
import com.lampo.device_lab.master.service.SessionReaper;

import reactor.core.publisher.Mono;

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
@RestController
@RequestMapping("/device")
public class DeviceController {

	@Autowired
	private AllocationService allocationService;

	@Autowired
	private SessionReaper reaper;

	@PostMapping("/update_status")
	public boolean updateStatus(@RequestBody DeviceInfo request,
			@RequestParam(defaultValue = "FREE") DeviceStatus action, HttpServletRequest servletRequest) {
		return allocationService.updateDeviceStatus(request.getSlaveIp(), action, request.getDeviceId(),
				servletRequest);
	}

	@PostMapping("/restart_device")
	public void restartDevice(@RequestBody DeviceInfo request, HttpServletRequest servletRequest) {
		allocationService.restartDevice(request, servletRequest);
	}

	@PostMapping("/restart_all_devices")
	public void restartAllDevice(@RequestParam(value = "slave_ip", defaultValue = "") String slaveIp,
			HttpServletRequest servletRequest) {
		allocationService.restartAllDevices(slaveIp, servletRequest);
	}

	@PostMapping("/restart_stf")
	public void restartSTF(@RequestBody DeviceInfo request, HttpServletRequest servletRequest) {
		allocationService.restartSTF(request, servletRequest);
	}

	@PostMapping("/cleanup_dead_sessions")
	public void triggerDeadSessionCleanUp() {
		reaper.reapDeadSessions(true);
	}

	@PostMapping("/check_capability")
	public DeviceInfo isCapabilityFound(@RequestBody DeviceMatchRequest request, HttpServletRequest servletRequest) {
		return allocationService.getMatchingCapabilityNode(request, servletRequest);
	}

	@GetMapping(path = "/logs/{request_id}", produces = "text/plain")
	public Mono<Resource> getLogs(@PathVariable("request_id") String requestId, HttpServletRequest servletRequest)
			throws Throwable {
		return getResource(allocationService.getLogUrl(requestId, servletRequest));
	}

	private Mono<Resource> getResource(String url) throws MalformedURLException {
		if (url == null) {
			return Mono.empty();
		}
		URL _url = new URL(url);
		return Mono.fromSupplier(() -> new UrlResource(_url));
	}

	@GetMapping(path = "/videos/{request_id}", produces = "video/mp4")
	public Mono<Resource> getVideos(@PathVariable("request_id") String requestId, HttpServletRequest servletRequest)
			throws Throwable {
		return getResource(allocationService.getVideoUrl(requestId, servletRequest));
	}

}
