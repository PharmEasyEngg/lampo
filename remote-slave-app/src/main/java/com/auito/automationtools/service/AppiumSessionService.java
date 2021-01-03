package com.auito.automationtools.service;

import static com.auito.automationtools.model.Header.REQUEST_ID;
import static com.auito.automationtools.utils.ADBUtilities.clearUserData;
import static com.auito.automationtools.utils.ADBUtilities.uninstallApp;
import static com.auito.automationtools.utils.AppiumLocalService.builder;
import static com.auito.automationtools.utils.CommandLineExecutor.isPortListening;
import static com.auito.automationtools.utils.CommandLineExecutor.killAppiumProcesses;
import static com.auito.automationtools.utils.StringUtils.getLocalNetworkIP;
import static com.auito.automationtools.utils.StringUtils.isBlank;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.net.URL;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.auito.automationtools.model.AppiumSessionRequest;
import com.auito.automationtools.model.DeviceManagerException;
import com.auito.automationtools.model.Header;
import com.auito.automationtools.utils.AppiumLocalService.Builder;
import com.auito.automationtools.utils.STFServiceBuilder;
import com.google.common.collect.ImmutableMap;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

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
