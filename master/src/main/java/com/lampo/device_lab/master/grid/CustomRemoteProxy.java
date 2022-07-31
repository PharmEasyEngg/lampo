package com.lampo.device_lab.master.grid;

import static com.lampo.device_lab.master.grid.CustomCapability.search;
import static com.lampo.device_lab.master.model.Header.JOB_LINK;
import static com.lampo.device_lab.master.model.Header.REQUEST_ID;
import static com.lampo.device_lab.master.model.Header.TEAM;
import static com.lampo.device_lab.master.model.Header.USER;
import static com.lampo.device_lab.master.utils.CommonUtilities.isBlank;
import static com.lampo.device_lab.master.utils.CommonUtilities.split;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.openqa.grid.common.RegistrationRequest;
import org.openqa.grid.internal.ExternalSessionKey;
import org.openqa.grid.internal.GridRegistry;
import org.openqa.grid.internal.TestSession;
import org.openqa.grid.selenium.proxy.DefaultRemoteProxy;

import com.lampo.device_lab.master.model.ClearDataRequest;
import com.lampo.device_lab.master.model.DeviceInfo;
import com.lampo.device_lab.master.model.Header;
import com.lampo.device_lab.master.model.UninstallRequest;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;
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
public class CustomRemoteProxy extends DefaultRemoteProxy {

	private static final Properties PROPS = new Properties();

	static {
		try (InputStream stream = CustomRemoteProxy.class.getResourceAsStream("/application.properties")) {
			PROPS.load(stream);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String getProperty(String key) {
		String value = System.getProperty(key, PROPS.getProperty(key));
		return value == null ? value : value.trim();
	}

	private static final String AUTH_TOKEN = getProperty("slave.auth_token");
	private static final String SERVER_PORT = getProperty("server.port");
	private static final String SLAVE_PORT = getProperty("slave.port");

	private static final String BASE_URL = String.format("http://0.0.0.0:%s", SERVER_PORT);

	public CustomRemoteProxy(RegistrationRequest request, GridRegistry registry) {
		super(request, registry);
	}

	@Override
	public TestSession getNewSession(Map<String, Object> requestedCapability) {
		log.debug("request '{}' ::: creating new session => {}", getRequestId(requestedCapability),
				CustomGridNodeCapabilityMatcher.extractCapability(requestedCapability));
		return super.getNewSession(requestedCapability);
	}

	@Override
	public void beforeSession(TestSession session) {

		String requestId = getRequestId(session);

		DeviceInfo info = getDeviceInfo();
		info.setRequestId(requestId);

		executePreRequisites(session);
		String deviceId = getDeviceId();

		Object videoRecording = CustomCapability.search(session.getRequestedCapabilities(),
				CustomCapability.RECORD_VIDEO);
		if (videoRecording != null && "true".equalsIgnoreCase(videoRecording.toString())) {
			startVideoRecording(requestId, deviceId);
		}

		log.info("request '{}' ::: allocating device '{}' successful ? {}", requestId, info,
				post(info, getRequestInfo(session), true));

	}

	@SneakyThrows
	private void executePreRequisites(@NonNull TestSession session) {
		uninstallApps(session);
		clearUserData(session);
	}

	private void clearUserData(@NonNull TestSession session) throws IOException {

		Map<String, Object> caps = session.getRequestedCapabilities();
		Object val = caps.getOrDefault("appium:clearUserData", caps.getOrDefault("clearUserData", false));

		boolean clearUserData = val == null ? false : "true".equalsIgnoreCase(val.toString());
		if (clearUserData) {

			String host = getRemoteHost().getHost();

			String requestId = CustomCapability.search(caps, CustomCapability.REQUEST_ID);

			String packageName = CustomCapability.search(caps, "appPackage");
			String udid = getDeviceId();

			RequestBody body = RequestBody.create(MediaType.parse("application/json"),
					new ClearDataRequest(requestId, udid, packageName).toJson());

			String url = String.format("http://%s:%s/device/clear_user_data", host, SLAVE_PORT);
			Headers headers = Headers.of(Header.AUTH.toString(), AUTH_TOKEN);
			Request request = new Request.Builder().url(url).post(body).headers(headers).build();
			try (Response response = new OkHttpClient().newCall(request).execute()) {
				log.info("{} :::: clearing user data of app package '{}' from device '{}' present on '{}'", requestId,
						packageName, udid, host);
			}
		}
	}

	private void uninstallApps(TestSession session) throws IOException {
		Map<String, Object> caps = session.getRequestedCapabilities();
		String apps = CustomCapability.search(caps, "uninstallOtherPackages");
		String udid = getDeviceId();
		String requestId = CustomCapability.search(caps, CustomCapability.REQUEST_ID);

		if (!isBlank(apps)) {

			Collection<String> packages = new HashSet<>(split(apps));
			String host = getRemoteHost().getHost();

			RequestBody body = RequestBody.create(MediaType.parse("application/json"),
					new UninstallRequest(requestId, udid, packages).toJson());

			String url = String.format("http://%s:%s/device/uninstall_apps", host, SLAVE_PORT);
			Headers headers = Headers.of(Header.AUTH.toString(), AUTH_TOKEN);
			Request request = new Request.Builder().url(url).post(body).headers(headers).build();
			try (Response response = new OkHttpClient().newCall(request).execute()) {
				log.info("{} :::: uninstall apps '{}' from device '{}' present on '{}'", requestId, packages, udid,
						host);
			}
		}
	}

	private String getRequestId(@NonNull TestSession session) {
		return getRequestId(session.getRequestedCapabilities());
	}

	private String getRequestId(@NonNull Map<String, Object> caps) {
		return CustomCapability.search(caps, CustomCapability.REQUEST_ID, UUID.randomUUID().toString());
	}

	private DeviceInfo getDeviceInfo() {
		URL url = getRemoteHost();
		DeviceInfo info = new DeviceInfo();
		info.setSlaveIp(url.getHost());
		info.setDeviceId(getDeviceId());
		info.setPlatformVersion(getPlatformVersion());
		info.setDeviceName(getDeviceName());
		info.setAppiumPort(url.getPort());
		return info;
	}

	@Override
	@SneakyThrows
	public void afterSession(TestSession session) {

		String requestId = getRequestId(session);

		String deviceId = getDeviceId();

		generateLog(requestId, deviceId, session.getExternalKey());
		Object videoRecording = CustomCapability.search(session.getRequestedCapabilities(),
				CustomCapability.RECORD_VIDEO);
		if (videoRecording != null && "true".equalsIgnoreCase(videoRecording.toString())) {
			stopVideoRecording(requestId, deviceId);
		}
		DeviceInfo info = getDeviceInfo();
		info.setRequestId(requestId);

		log.info("request '{}' ::: unallocating device '{}' successful ? {}", requestId, info,
				post(info, getRequestInfo(session), false));

	}

	private String getDeviceId() {
		return (String) getConfig().capabilities.get(0).getCapability("udid");
	}

	private String getPlatformVersion() {
		return (String) getConfig().capabilities.get(0).getCapability("platformVersion");
	}

	private String getDeviceName() {
		return (String) getConfig().capabilities.get(0).getCapability("deviceName");
	}

	private String getLogUrl(String requestId) {
		String host = getConfig().getRemoteHost().substring(0, getConfig().getRemoteHost().lastIndexOf(":"));
		return String.format("%s:%s/appium/logs/%s.log", host, SLAVE_PORT, requestId);
	}

	private String getVideoUrl(String requestId) {
		String host = getConfig().getRemoteHost().substring(0, getConfig().getRemoteHost().lastIndexOf(":"));
		return String.format("%s:%s/appium/videos/%s.mp4", host, SLAVE_PORT, requestId);
	}

	private RequestInfo getRequestInfo(TestSession session) {
		RequestInfo info = new RequestInfo();
		info.setId(session.getInternalKey());
		Map<String, Object> capabilities = session.getRequestedCapabilities();
		info.setId(search(capabilities, CustomCapability.REQUEST_ID, ""));
		info.setUser(search(capabilities, CustomCapability.USER, ""));
		info.setRequestorIp(search(capabilities, CustomCapability.IP, ""));
		info.setJobLink(search(capabilities, CustomCapability.JOB_LINK, ""));
		info.setTeam(search(capabilities, CustomCapability.TEAM_NAME, ""));
		return info;
	}

	private void stopVideoRecording(String requestId, String deviceId) {

		Headers headers = Headers.of(Header.AUTH.toString(), AUTH_TOKEN);

		String host = getConfig().getRemoteHost().substring(0, getConfig().getRemoteHost().lastIndexOf(":"));
		String url = String.format("%s:%s/device/videos?action=stop&deviceId=%s&requestId=%s", host,
				SLAVE_PORT, deviceId, requestId);
		RequestBody body = RequestBody.create(MediaType.parse("application/json"), "");
		Request request = new Request.Builder().url(url).post(body).headers(headers).build();
		try (Response response = new OkHttpClient().newCall(request).execute()) {
			if (response.code() == 200) {
				String videoUrl = getVideoUrl(requestId);
				log.error("request '{}' ::: video recording for appium session => {}", requestId, videoUrl);

				return;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.error("request '{}' ::: stopping video recording for appium session failed", requestId);
	}

	private void startVideoRecording(String requestId, String deviceId) {

		Headers headers = Headers.of(Header.AUTH.toString(), AUTH_TOKEN);

		String host = getConfig().getRemoteHost().substring(0, getConfig().getRemoteHost().lastIndexOf(":"));
		String url = String.format("%s:%s/device/videos?action=start&deviceId=%s&requestId=%s",
				host, SLAVE_PORT, deviceId, requestId);

		RequestBody body = RequestBody.create(MediaType.parse("application/json"), "");
		Request request = new Request.Builder().url(url).post(body).headers(headers).build();
		try (Response response = new OkHttpClient().newCall(request).execute()) {
			if (response.code() == 200) {
				log.error("request '{}' ::: video recording for appium session started", requestId);
				return;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.error("request '{}' ::: starting video recording for appium session failed", requestId);
	}

	private boolean generateLog(String requestId, String deviceId, ExternalSessionKey sessionId) {

		Headers headers = Headers.of(Header.AUTH.toString(), AUTH_TOKEN);

		String host = getConfig().getRemoteHost().substring(0, getConfig().getRemoteHost().lastIndexOf(":"));
		String url = String.format("%s:%s/device/logs?sessionId=%s&deviceId=%s&requestId=%s", host, SLAVE_PORT,
				sessionId, deviceId, requestId);
		Request request = new Request.Builder().url(url).get().headers(headers).build();
		try (Response response = new OkHttpClient().newCall(request).execute()) {
			if (response.code() == 200) {
				String logUrl = getLogUrl(requestId);
				log.info("request '{}' ::: logs generated for appium session '{}' at => {}", requestId,
						sessionId, logUrl);
				return true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.error("request '{}' ::: logs generated for appium session '{}' failed", requestId, sessionId);
		return false;
	}

	private boolean post(@NonNull DeviceInfo info, RequestInfo requestInfo, boolean markBusy) {

		String action = markBusy ? "BUSY" : "FREE";

		try {
			log.info("request '{}' ::: marking device '{}' status to '{}'", requestInfo.getId(), info, action);

			RequestBody body = RequestBody.create(MediaType.parse("application/json"), info.toJson());

			Headers headers = getHeaders(requestInfo);

			String url = String.format("%s/device/update_status?action=%s", BASE_URL, action);
			Request request = new Request.Builder().url(url).post(body).headers(headers).build();
			try (Response response = new OkHttpClient().newCall(request).execute()) {
				return response.code() == 200;
			}

		} catch (Exception ex) {
			log.error(String.format("request '%s' ::: unable to update %s status", requestInfo.getId(), action), ex);
		}
		return false;
	}

	private Headers getHeaders(RequestInfo request) {

		String[] arr = { REQUEST_ID.toString(), request.getId(), JOB_LINK.toString(), request.getJobLink(),
				USER.toString(), request.getUser(), Header.REQUESTOR_IP.toString(),
				request.getRequestorIp(), TEAM.toString(), request.getTeam() };

		log.debug("request '{}' ::: setting headers => {}", request.getId(), Arrays.toString(arr));
		return Headers.of(arr);
	}

}

@Data
@NoArgsConstructor
class RequestInfo {

	private String id;
	private String user;
	private String requestorIp;
	private String jobLink;
	private String team;

}
