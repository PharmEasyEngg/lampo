package com.lampo.slave.service;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.lampo.slave.model.Header.REQUEST_ID;
import static com.lampo.slave.utils.ADBUtilities.clearUserData;
import static com.lampo.slave.utils.ADBUtilities.uninstallApp;
import static com.lampo.slave.utils.AppiumLocalService.builder;
import static com.lampo.slave.utils.CommandLineExecutor.isPortListening;
import static com.lampo.slave.utils.CommandLineExecutor.killAppiumProcesses;
import static com.lampo.slave.utils.StringUtils.getLocalNetworkIP;
import static com.lampo.slave.utils.StringUtils.isBlank;

import java.net.URL;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.lampo.slave.model.AppiumSessionRequest;
import com.lampo.slave.model.DeviceManagerException;
import com.lampo.slave.model.Header;
import com.lampo.slave.utils.STFServiceBuilder;
import com.lampo.slave.utils.AppiumLocalService.Builder;

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
@Slf4j
@Component
public class AppiumSessionService {

	private static final int RETHINKDB_PORT = 28015;

	@Autowired
	private DeviceSyncProcessor deviceProcessor;

	@PostConstruct
	public void killExistingAppiumSessions() {
		if (!isPortListening(RETHINKDB_PORT)) {
			log.error("*********** please check whether rethinkdb service is running or not ***********");
		}
		killAppiumProcesses();
		STFServiceBuilder.builder().restart();
	}

	private URL restartAppiumService(@NonNull Map<String, Object> request) {

		log.info("starting appium session for request => {}", request);

		String deviceId = (String) request.get("device_id");
		checkArgument(!isBlank(deviceId), "'device_id' is missing");

		boolean isAndroid = (boolean) request.get("is_android");
		checkNotNull(!isBlank(deviceId), "'is_android' is missing");

		String requestId = String.valueOf(request.get("request-id"));
		return builder()
				.deviceId(deviceId)
				.isAndroid(isAndroid)
				.ip(getLocalNetworkIP())
				.requestId(requestId)
				.restart();
	}

	public URL createAppiumSession(AppiumSessionRequest sessionRequest, HttpServletRequest httpRequest) {

		try {
			String requestId = getHeader(httpRequest, REQUEST_ID);

			log.info("creating appium session for request '{}' with request id '{}'", sessionRequest, requestId);

			if (isBlank(sessionRequest.getDeviceId())) {
				throw new DeviceManagerException("'device_id' is required");
			}

			if (!deviceProcessor.isConnectedDevice(sessionRequest.getDeviceId())) {
				throw new DeviceManagerException(
						String.format("device with id '%s' is not found", sessionRequest.getDeviceId()));
			}

			boolean isAndroid = deviceProcessor.isAndroid(sessionRequest.getDeviceId());

			clearAndroidAppUserData(sessionRequest, isAndroid);

			Map<String, Object> requestBody = ImmutableMap.of("device_id", sessionRequest.getDeviceId(), "is_android",
					isAndroid, "request-id", requestId == null ? System.currentTimeMillis() : requestId);
			URL serviceUrl = restartAppiumService(requestBody);
			if (serviceUrl == null) {
				throw new DeviceManagerException(String.format("unable to start appium service for device '%s'",
						sessionRequest.getDeviceId()));
			}
			return new URL(serviceUrl.toString().replaceAll("0.0.0.0|127.0.0.1", getLocalNetworkIP()));
		} catch (Exception e) {
			e.printStackTrace();
			throw new DeviceManagerException(e.getMessage());
		}
	}

	private void clearAndroidAppUserData(@NonNull AppiumSessionRequest sessionRequest, boolean isAndroid) {
		if (isAndroid && sessionRequest.isCleanUserData()) {
			if (isBlank(sessionRequest.getAppPackage())) {
				throw new DeviceManagerException(String.format(
						"'app_package' is needed when 'clean_user_data' is set to 'true' for device '%s'",
						sessionRequest.getDeviceId()));
			} else {
				clearUserData(sessionRequest.getDeviceId(), sessionRequest.getAppPackage());
			}
			uninstallApp(sessionRequest.getDeviceId(), "io.appium.settings");
		}
	}

	public boolean stopAppiumService(String deviceId, HttpServletRequest httpRequest) {

		log.debug("stopping appium session for request => {}", deviceId);

		if (isBlank(deviceId)) {
			throw new DeviceManagerException("'device_id' is required");
		}

		if (!deviceProcessor.isConnectedDevice(deviceId)) {
			throw new DeviceManagerException(
					String.format("device with id '%s' is not found", deviceId));
		}

		Builder stopRequest = builder()
				.deviceId(deviceId)
				.ip(getLocalNetworkIP())
				.requestId(getHeader(httpRequest, REQUEST_ID));
		return stopRequest.stop() ? true : stopRequest.stop();
	}

	private String getHeader(HttpServletRequest servletRequest, Header name) {
		return getHeader(servletRequest, name, "");
	}

	private String getHeader(HttpServletRequest servletRequest, Header name, String defaultValue) {
		return servletRequest == null ? defaultValue : servletRequest.getHeader(name.toString());
	}

}
