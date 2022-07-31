package com.lampo.device_lab.master.grid;

import static com.lampo.device_lab.master.grid.CustomCapability.JOB_LINK;
import static com.lampo.device_lab.master.grid.CustomCapability.REQUEST_ID;
import static com.lampo.device_lab.master.grid.CustomCapability.SESSION_MAX_TIMEOUT;
import static com.lampo.device_lab.master.grid.CustomCapability.TEAM_NAME;
import static com.lampo.device_lab.master.grid.CustomCapability.search;
import static com.lampo.device_lab.master.grid.CustomGridNodeCapabilityMatcher.extractCapability;
import static com.lampo.device_lab.master.utils.CommonUtilities.isBlank;
import static com.lampo.device_lab.master.utils.CommonUtilities.jsonToPojo;
import static com.lampo.device_lab.master.utils.CommonUtilities.pojoToJson;
import static com.lampo.device_lab.master.utils.CommonUtilities.sleep;
import static com.lampo.device_lab.master.utils.CommonUtilities.split;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.http.HttpStatus;
import org.openqa.grid.internal.DefaultGridRegistry;
import org.openqa.grid.web.servlet.handler.RequestHandler;
import org.openqa.grid.web.servlet.handler.SeleniumBasedRequest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lampo.device_lab.master.exception.DeviceManagerException;
import com.lampo.device_lab.master.model.DeviceInfo;
import com.lampo.device_lab.master.model.Header;
import com.lampo.device_lab.master.model.NodeCapability;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

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
public class CustomGridRegistry extends DefaultGridRegistry {

	private static final TypeReference<Map<String, Object>> CLAZZ = new TypeReference<Map<String, Object>>() {
	};
	private static final int SERVER_PORT = Integer.parseInt(CustomRemoteProxy.getProperty("server.port"));
	private static final String BASE_URL = String.format("http://127.0.0.1:%s", SERVER_PORT);
	private static final long SESSION_MAX_TIMEOUT_IN_SEC = Integer.getInteger("custom.session.wait_timeout", 300);
	private static final int SESSION_POLLING_IN_SEC = Integer.getInteger("custom.session.polling_timeout", 5);
	private static final String CHECK_CAPABILITY_URL = BASE_URL + "/device/check_capability";
	private static final String GRID_STATUS_URL = "http://127.0.0.1:4444/grid/api/hub";

	private static final String DEFAULT_UNINSTALL_PACKAGES = "com.appium.settings,io.appium.uiautomator2.server,io.appium.uiautomator2.server.test";

	@Override
	@SneakyThrows
	public void addNewSessionRequest(RequestHandler handler) {

		Map<String, Object> caps = handler.getRequest().getDesiredCapabilities();

		if (caps.isEmpty()) {
			return;
		}

		DeviceInfo node = getMatchingCapabilityNode(caps);

		if (node == null) {
			log.info("unable to find a matching node for requested capability '{}'", caps);
			return;
		}

		Map<String, String> mergeCaps = new HashMap<>();
		mergeCaps.put("appium:udid", node.getDeviceId());
		mergeCaps.put("appium:deviceName", node.getDeviceName());
		mergeCaps.put("appium:platformVersion", node.getPlatformVersion());
		mergeCaps.put("appium:uninstallOtherPackages", getUninstallPackages(caps));

		caps.putAll(mergeCaps);

		log.info("request '{}' ::: setting device udid '{}' and other capabilities {}", getRequestId(caps),
				node.getDeviceId(), mergeCaps);

		SeleniumBasedRequest request = handler.getRequest();
		request.setBody(pojoToJson(caps));

		super.addNewSessionRequest(handler);

	}

	private String getUninstallPackages(Map<String, Object> requiredCapabilities) {
		String uninstallOtherPackages = search(requiredCapabilities, "uninstallOtherPackages");
		return split(DEFAULT_UNINSTALL_PACKAGES
				+ (!isBlank(uninstallOtherPackages) ? ("," + uninstallOtherPackages) : ""))
				.stream().distinct().collect(Collectors.joining(","));
	}

	private DeviceInfo getMatchingCapabilityNode(@NonNull Map<String, Object> capabilities) {

		if (!isFreeSlotAvailable()) {
			log.debug("no free slot available");
			return null;
		}

		Object value = search(capabilities, SESSION_MAX_TIMEOUT, SESSION_MAX_TIMEOUT_IN_SEC);
		long timeout = Long.parseLong(value.toString());

		DeviceMatchRequest request = getDeviceAllocationRequest(capabilities);

		String requestId = getRequestId(capabilities);
		log.info("request '{}' ::: waiting to match node with capability '{}' with maximum timeout '{} seconds'",
				requestId, request, timeout);

		long end = System.currentTimeMillis() + timeout * 1000;
		while (true) {
			if (System.currentTimeMillis() > end) {
				String msg = String.format(
						"request '%s' ::: unable to find node with capability '%s' within '%s seconds'",
						requestId, request, timeout);
				log.error(msg);
				throw new DeviceManagerException(msg);
			}
			DeviceInfo info = getMatch(request, requestId);
			if (info != null) {
				return info;
			}
			sleep(SESSION_POLLING_IN_SEC);
		}
	}

	private DeviceMatchRequest getDeviceAllocationRequest(Map<String, Object> capabilities) {

		String jobName = search(capabilities, JOB_LINK);
		String team = search(capabilities, TEAM_NAME);

		if (isBlank(team)) {
			throw new DeviceManagerException(
					String.format("'%s' property is missing in request", TEAM_NAME.getName()));
		}
		DeviceFilter filter = new DeviceFilter(jobName, team);
		NodeCapability capability = toNodeCapability(capabilities);
		return new DeviceMatchRequest(filter, capability);
	}

	private boolean isFreeSlotAvailable() {
		Request req = new Request.Builder().url(GRID_STATUS_URL).get().build();
		try (Response response = new OkHttpClient().newCall(req).execute()) {
			if (response.code() != HttpStatus.SC_OK) {
				return false;
			}
			String reponseBody = response.body().string();
			Map<String, Object> map = jsonToPojo(reponseBody, CLAZZ);
			@SuppressWarnings("unchecked")
			Map<String, Integer> slotCounts = (Map<String, Integer>) map.get("slotCounts");
			return slotCounts.getOrDefault("free", 0) > 0;
		} catch (Exception ex) {
			log.error(String.format("'%s' occurred while calling isFreeSlotAvailable()", ex.getClass().getName()), ex);
			return false;
		}
	}

	private String getRequestId(Map<String, Object> capabilities) {
		String requestId = search(capabilities, REQUEST_ID);
		if (isBlank(requestId)) {
			capabilities.put(REQUEST_ID.getName(), UUID.randomUUID().toString());
		}
		return requestId;
	}

	private DeviceInfo getMatch(@NonNull DeviceMatchRequest request, String requestId) {

		RequestBody body = RequestBody.create(MediaType.parse("application/json"), request.toJson());
		Request req = new Request.Builder().url(CHECK_CAPABILITY_URL)
				.header(Header.REQUEST_ID.toString(), requestId)
				.post(body).build();

		try (Response response = new OkHttpClient().newCall(req).execute()) {
			String responseBody = response.body().string();
			return response.code() == HttpStatus.SC_OK ? jsonToPojo(responseBody, DeviceInfo.class) : null;
		} catch (Exception ex) {
			log.error(String.format("'%s' occurred while calling getMatch(%s, %s)", ex.getClass().getName(), request,
					requestId), ex);
			return null;
		}
	}

	private NodeCapability toNodeCapability(Map<String, Object> capabilities) {
		return extractCapability(capabilities);
	}

}
