package com.lampo.slave;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.lampo.slave.controller.AppiumController;
import com.lampo.slave.model.AppiumSessionRequest;
import com.lampo.slave.model.DeviceManagerException;
import com.lampo.slave.model.Header;
import com.lampo.slave.service.AppiumSessionService;
import com.lampo.slave.service.DeviceManagerExceptionHandler;
import com.lampo.slave.service.DeviceSyncProcessor;

import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMq;
import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMqConfig;

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
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class RemoteSlaveAppTest {

	@Autowired
	private AppiumSessionService appiumSessionService;

	private static EmbeddedRabbitMq rabbitMq;

	@Autowired
	private DeviceSyncProcessor deviceSyncProcessor;

	@Autowired
	private AppiumController appiumController;

	@BeforeClass
	public static void initServices() throws IOException {
		setupRabbitMQ();
	}

	@Value("${slave.auth_token}")
	private String authToken;

	@AfterClass
	public static void tearDownServices() {
		rabbitMq.stop();
	}

	private static void setupRabbitMQ() {
		EmbeddedRabbitMqConfig config = new EmbeddedRabbitMqConfig.Builder()
				.useCachedDownload(true)
				.rabbitMqServerInitializationTimeoutInMillis(30000)
				.defaultRabbitMqCtlTimeoutInMillis(30000).build();
		rabbitMq = new EmbeddedRabbitMq(config);
		rabbitMq.start();
	}

	@Test
	public void createSessionNullRequest() {

		AppiumSessionRequest request = new AppiumSessionRequest();

		assertThrows(DeviceManagerException.class,
				() -> appiumSessionService.createAppiumSession(request, null), "'device_id' is required");
	}

	@Test
	public void createSessionUnconnectedDevice() {

		String deviceId = "unknown";

		AppiumSessionRequest request = new AppiumSessionRequest();
		request.setDeviceId(deviceId);

		assertThrows(
				DeviceManagerException.class,
				() -> appiumSessionService.createAppiumSession(request, null),
				String.format("device with id '%s' is not found", deviceId));

	}

	@Test
	public void stopSessionNullDeviceId() {

		assertThrows(
				DeviceManagerException.class,
				() -> appiumSessionService.stopAppiumService(null, null),
				"'device_id' is required");

	}

	@Test
	public void stopSessionUnconnectedDevice() {

		String deviceId = "unknown";

		assertThrows(DeviceManagerException.class,
				() -> appiumSessionService.stopAppiumService(deviceId, null),
				String.format("device with id '%s' is not found", deviceId));

	}

	@Test
	public void controllerCreateSessionUnAuthorized() throws Exception {
		MockMvc mvc = MockMvcBuilders.standaloneSetup(appiumController)
				.setControllerAdvice(new DeviceManagerExceptionHandler()).build();
		AppiumSessionRequest request = new AppiumSessionRequest("emulator-5554", false, null);
		MockHttpServletResponse response = mvc.perform(
				post("/appium/create")
						.contentType("application/json")
						.content(request.toJson()))
				.andReturn()
				.getResponse();
		assertThat(response.getStatus()).isEqualTo(401);
	}

	@Test
	public void controllerStopSessionUnAuthorized() throws Exception {
		MockMvc mvc = MockMvcBuilders.standaloneSetup(appiumController)
				.setControllerAdvice(new DeviceManagerExceptionHandler()).build();
		AppiumSessionRequest request = new AppiumSessionRequest("emulator-5554", false, null);
		MockHttpServletResponse response = mvc.perform(
				post("/appium/stop")
						.contentType("application/json")
						.content(request.toJson()))
				.andReturn()
				.getResponse();
		assertThat(response.getStatus()).isEqualTo(401);
	}

	@Test
	public void controllerCreateSessionAuthorized() throws Exception {
		MockMvc mvc = MockMvcBuilders.standaloneSetup(appiumController)
				.setControllerAdvice(new DeviceManagerExceptionHandler()).build();
		AppiumSessionRequest request = new AppiumSessionRequest("emulator-5554", false, null);
		MockHttpServletResponse response = mvc.perform(
				post("/appium/create")
						.header(Header.AUTH.toString(), authToken)
						.contentType("application/json")
						.content(request.toJson()))
				.andReturn()
				.getResponse();
		assertThat(response.getStatus()).isEqualTo(400);
	}

	@Test
	public void controllerStopSessionAuthorized() throws Exception {
		MockMvc mvc = MockMvcBuilders.standaloneSetup(appiumController)
				.setControllerAdvice(new DeviceManagerExceptionHandler()).build();
		AppiumSessionRequest request = new AppiumSessionRequest("emulator-5554", false, null);
		MockHttpServletResponse response = mvc.perform(
				post("/appium/stop")
						.header(Header.AUTH.toString(), authToken)
						.contentType("application/json")
						.content(request.toJson()))
				.andReturn()
				.getResponse();
		assertThat(response.getStatus()).isEqualTo(400);
	}

	@Test
	public void syncDevices() {
		assertThat(deviceSyncProcessor.updateDeviceInfoToMaster()).isTrue();
	}

}
