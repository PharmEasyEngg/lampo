package com.auito.automationtools;

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

import com.auito.automationtools.controller.AppiumController;
import com.auito.automationtools.model.AppiumSessionRequest;
import com.auito.automationtools.model.DeviceManagerException;
import com.auito.automationtools.model.Header;
import com.auito.automationtools.service.AppiumSessionService;
import com.auito.automationtools.service.DeviceManagerExceptionHandler;
import com.auito.automationtools.service.DeviceSyncProcessor;

import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMq;
import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMqConfig;

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
