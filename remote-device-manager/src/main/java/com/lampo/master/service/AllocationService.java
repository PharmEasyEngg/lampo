package com.lampo.master.service;

import static com.lampo.master.model.Header.AUTH;
import static com.lampo.master.model.Header.JENKINS_JOB_LINK;
import static com.lampo.master.model.Header.REQUEST_ID;
import static com.lampo.master.model.Header.USER;
import static com.lampo.master.utils.RequestUtils.getClientIp;
import static com.lampo.master.utils.StringUtils.getMajorVersion;
import static com.lampo.master.utils.StringUtils.isBlank;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.google.common.collect.ImmutableMap;
import com.lampo.master.model.AllocatedTo;
import com.lampo.master.model.AppiumSession;
import com.lampo.master.model.AppiumSessionRequest;
import com.lampo.master.model.Device;
import com.lampo.master.model.DeviceInformation;
import com.lampo.master.model.DeviceManagerException;
import com.lampo.master.model.DeviceRequest;
import com.lampo.master.model.DeviceRestrictionRequest;
import com.lampo.master.model.DeviceStatus;
import com.lampo.master.model.Header;
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
@Service
public class AllocationService {

	private static final int POLLING_FREQ_IN_SEC = Integer
			.parseInt(System.getProperty("devices.polling_freq", "5").trim());
	private static final int SLAVE_APP_PORT = 5252;
	private static final int MAX_TIMEOUT_IN_SECONDS = 900;

	@Autowired
	private IDeviceRepository deviceRepository;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private ICapabilityMatcher matcher;

	@Value("${slave.auth_token}")
	private String slaveAuthToken;

	@Async("threadPoolTaskExecutor")
	public CompletableFuture<AppiumSession> allocateDevice(long timeoutInSeconds, @NonNull DeviceRequest request,
			HttpServletRequest httpRequest) {

		long maxTimeout = timeoutInSeconds < 0 ? MAX_TIMEOUT_IN_SECONDS : timeoutInSeconds;

		log.info("{} ::: device allocate request '{}' with timeout of {} seconds", getLogPrefix(httpRequest), request,
				maxTimeout);

		return CompletableFuture.supplyAsync(() -> pollForFreeDevice(request, maxTimeout, httpRequest))
				.applyToEither(failAfter(request, httpRequest, maxTimeout), session -> session);
	}

	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	private CompletableFuture<AppiumSession> failAfter(@NonNull DeviceRequest request,
			HttpServletRequest httpRequest, long timeout) {
		final CompletableFuture<AppiumSession> promise = new CompletableFuture<>();
		executor.schedule(() -> promise.completeExceptionally(new DeviceManagerException(
				String.format("%s ::: unable to allocate device for request '%s' even after '%s seconds'",
						getLogPrefix(httpRequest), request, timeout))),
				timeout, TimeUnit.SECONDS);
		return promise;
	}

	private String getLogPrefix(HttpServletRequest request) {

		String requestId = getHeader(request, REQUEST_ID);
		String clientIp = getClientIp(request);
		String user = getHeader(request, USER);

		return String.format("request id '%s' for user '%s' from '%s'", requestId, user, clientIp);
	}

	private AppiumSession pollForFreeDevice(@NonNull DeviceRequest request, long timeout,
			HttpServletRequest servletRequest) {

		if (request.isClearUserData()
				&& (!isBlank(request.getIsAndroid()) && "true".equals(request.getIsAndroid().toLowerCase().trim()))
				&& isBlank(request.getAppPackage())) {
			throw new DeviceManagerException("'app_package' is needed when 'clean_user_data' is set to 'true'");
		}
		long maxTimeout = System.currentTimeMillis() + (timeout * 1000);
		do {
			try {
				AppiumSession sessionInfo = getFreeDevice(request, servletRequest);
				if (sessionInfo != null) {
					log.info("{} ::: allocating {}", getLogPrefix(servletRequest), sessionInfo);
					return sessionInfo;
				} else {
					sleep(POLLING_FREQ_IN_SEC);
				}
			} catch (Throwable e) {
				sleep(POLLING_FREQ_IN_SEC);
			}
		} while (System.currentTimeMillis() < maxTimeout);
		String msg = String.format("%s ::: unable to allocate device for request '%s' even after '%s seconds'",
				getLogPrefix(servletRequest), request, timeout);
		throw new DeviceManagerException(msg);
	}

	private void sleep(int sec) {
		try {
			TimeUnit.SECONDS.sleep(sec);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private synchronized AppiumSession getFreeDevice(@NonNull DeviceRequest request,
			HttpServletRequest servletRequest) throws IOException {

		Optional<Device> device = matcher.match(request);
		if (device.isPresent()) {
			String requestId = getHeader(servletRequest, REQUEST_ID);
			URL sessionUrlResponse = getAppiumSessionURL(device.get().getId(), device.get().getSlaveIp(),
					requestId, request.isClearUserData(),
					request.getAppPackage());

			updateDeviceStatus(device.get().getSlaveIp(), DeviceStatus.BUSY, device.get().getId(), servletRequest);

			String deviceId = device.get().getId();
			String deviceName = device.get().getDeviceInformation().getMarketName() == null
					? device.get().getDeviceInformation().getModel()
					: device.get().getDeviceInformation().getMarketName();
			boolean isAndroid = device.get().getDeviceInformation().isAndroid();
			boolean isRealDevice = device.get().getDeviceInformation().isRealDevice();
			String sdkVersion = device.get().getDeviceInformation().getSdkVersion();

			return new AppiumSession(deviceId, deviceName, isAndroid, isRealDevice, sdkVersion,
					device.get().getSlaveIp(), sessionUrlResponse,
					getLogsUrl(device.get().getSlaveIp(), deviceId, requestId));

		}
		return null;
	}

	private URL getLogsUrl(String slaveIp, String deviceId, String requestId) throws MalformedURLException {
		return new URL(
				String.format("http://%s:5252/remote-slave/appium/logs/%s/%s.log", slaveIp, deviceId, requestId));
	}

	private void killAppiumServiceOnSlave(@NonNull Device device, String requestId) {
		log.info("killing appium session of device '{}' on slave '{}'", device.getId(), device.getSlaveIp());
		Map<String, Object> body = ImmutableMap.of("device_id", device.getId(), "is_android",
				device.getDeviceInformation().isAndroid());
		String url = String.format("http://%s:%s/remote-slave/appium/stop", device.getSlaveIp(), SLAVE_APP_PORT);
		try {
			restTemplate.exchange(url, HttpMethod.POST, getHttpEntity(body, requestId), String.class);
		} catch (ResourceAccessException ex) {
			throw new DeviceManagerException(ex.getMessage());
		}
	}

	private URL getAppiumSessionURL(@NonNull @RequestParam String deviceId,
			@NonNull @RequestParam String slaveIp, String requestId, boolean clearUserData, String appPackage) {

		String url = String.format("http://%s:%s/remote-slave/appium/create", slaveIp, SLAVE_APP_PORT);
		AppiumSessionRequest req = new AppiumSessionRequest(deviceId, clearUserData,
				appPackage == null ? "" : appPackage.trim());
		try {
			return restTemplate.exchange(url, HttpMethod.POST, getHttpEntity(req, requestId), URL.class).getBody();
		} catch (ResourceAccessException ex) {
			throw new DeviceManagerException(ex.getMessage());
		}
	}

	private HttpEntity<?> getHttpEntity(Object body, String requestId) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(AUTH.toString(), slaveAuthToken);
		headers.setAccept(Arrays.asList(MediaType.ALL));
		if (requestId != null && !requestId.trim().isEmpty()) {
			headers.set(REQUEST_ID.toString(), requestId);
		}
		return body == null ? new HttpEntity<>(headers) : new HttpEntity<>(body, headers);
	}

	private synchronized boolean updateDeviceStatus(@NonNull String slaveIp, @NonNull DeviceStatus status,
			@NonNull String deviceId, HttpServletRequest servletRequest) {

		Optional<Device> optional = deviceRepository.findById(deviceId, slaveIp);

		if (optional.isPresent()) {
			Device device = optional.get();
			device.setFree(status == DeviceStatus.FREE);
			if (status == DeviceStatus.BUSY) {
				device.setLastAllocationStart(new Date());
				device.setLastAllocationEnd(null);
				device.setLastAllocatedTo(
						new AllocatedTo(getClientIp(servletRequest), getHeader(servletRequest, USER),
								getHeader(servletRequest, JENKINS_JOB_LINK)));
			} else {
				device.setLastAllocatedTo(null);
				device.setLastAllocationEnd(new Date());
				if (device.getLastAllocationStart() != null && device.getLastAllocationEnd() != null) {
					device.setLastSessionDuration(
							device.getLastAllocationEnd().getTime() - device.getLastAllocationStart().getTime());
				}
			}
			return null != deviceRepository.save(device);
		}
		return false;
	}

	public Collection<Device> unallocateAllDevices(HttpServletRequest servletRequest) {
		return unallocateDevice(null, servletRequest);
	}

	public Collection<Device> unallocateDevice(DeviceRestrictionRequest request, HttpServletRequest servletRequest) {

		if (request != null) {
			isRequestValid(request);
		}

		Collection<Device> devices = request == null ? deviceRepository.findAll()
				: deviceRepository.findAll().stream()
						.filter(device -> !device.isFree())
						.filter(device -> getDeviceRestrictionRequestFilter(request, device))
						.collect(Collectors.toList());

		log.info("{} ::: unallocating devices => {}", getLogPrefix(servletRequest), devices);

		devices.stream().parallel()
				.forEach(e -> unallocateDevice(e.getDeviceInformation().getDeviceId(), e.getSlaveIp(), servletRequest));

		return devices;
	}

	private boolean unallocateDevice(@NonNull String deviceId, @NonNull String slaveIp,
			HttpServletRequest servletRequest) {
		Optional<Device> device = deviceRepository.findById(deviceId, slaveIp);
		if (!device.isPresent()) {
			return false;
		}
		killAppiumServiceOnSlave(device.get(), getHeader(servletRequest, REQUEST_ID));
		return updateDeviceStatus(slaveIp, DeviceStatus.FREE, deviceId, servletRequest);
	}

	private String getHeader(HttpServletRequest servletRequest, Header name) {
		return getHeader(servletRequest, name, "");
	}

	private String getHeader(HttpServletRequest servletRequest, Header name, String defaultValue) {
		return servletRequest == null ? defaultValue : servletRequest.getHeader(name.toString());
	}

	public Collection<DeviceInformation> updateDeviceStatus(@NonNull DeviceRestrictionRequest request,
			boolean isBlacklist, HttpServletRequest servletRequest) {

		log.info("{} ::: {} devices matching request {}", getLogPrefix(servletRequest),
				isBlacklist ? "blacklisting" : "whitelisting", request);

		if (!isRequestValid(request)) {
			throw new DeviceManagerException(
					String.format("alteast one attribute is needed in '%s'", request.toJson()));
		}

		return deviceRepository.findAll().stream()
				.filter(device -> (!isBlacklist && device.isBlacklisted()) || (isBlacklist && !device.isBlacklisted()))
				.filter(device -> getDeviceRestrictionRequestFilter(request, device))
				.map(device -> {
					device.setBlacklisted(isBlacklist);
					return deviceRepository.save(device).getDeviceInformation();
				})
				.collect(Collectors.toList());
	}

	private boolean getDeviceRestrictionRequestFilter(DeviceRestrictionRequest request, Device device) {
		if (!(request.getDeviceId() == null || request.getDeviceId().isEmpty()
				|| request.getDeviceId().contains(device.getId()))) {
			return false;
		} else if (!(isBlank(request.getSlaveIp()) || device.getSlaveIp().equalsIgnoreCase(request.getSlaveIp()))) {
			return false;
		} else if (!(isBlank(request.getBrand())
				|| device.getDeviceInformation().getManufacturer().equalsIgnoreCase(request.getBrand().trim()))) {
			return false;
		} else if (!(isBlank(request.getDeviceName())
				|| device.getDeviceInformation().getModel().equalsIgnoreCase(request.getDeviceName().trim())
				|| device.getDeviceInformation().getMarketName().equalsIgnoreCase(request.getDeviceName().trim()))) {
			return false;
		} else if (!(isBlank(request.getSdkVersion()) || getMajorVersion(device.getDeviceInformation().getSdkVersion())
				.equals(getMajorVersion(request.getSdkVersion())))) {
			return false;
		} else {
			return true;
		}
	}

	private boolean isRequestValid(DeviceRestrictionRequest request) {
		int noOfAttributes = 0;
		if (request.getDeviceId() != null && !request.getDeviceId().isEmpty()) {
			noOfAttributes++;
		}
		if (!isBlank(request.getBrand())) {
			noOfAttributes++;
		}
		if (!isBlank(request.getDeviceName())) {
			noOfAttributes++;
		}
		if (!isBlank(request.getSdkVersion())) {
			noOfAttributes++;
		}
		if (!isBlank(request.getSlaveIp())) {
			noOfAttributes++;
		}
		return noOfAttributes > 0;
	}

}
