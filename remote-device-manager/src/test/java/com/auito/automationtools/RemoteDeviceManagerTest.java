package com.auito.automationtools;

import static com.auito.automationtools.config.ClientConfiguration.DEVICE_UPDATE_QUEUE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.auito.automationtools.controller.AppiumController;
import com.auito.automationtools.controller.HomeController;
import com.auito.automationtools.controller.PhoneImageController;
import com.auito.automationtools.model.Device;
import com.auito.automationtools.model.DeviceInformation;
import com.auito.automationtools.model.DeviceManagerException;
import com.auito.automationtools.model.DeviceRequest;
import com.auito.automationtools.model.DeviceRestrictionRequest;
import com.auito.automationtools.model.DeviceUpdateRequest;
import com.auito.automationtools.repos.IDeviceRepository;
import com.auito.automationtools.service.AllocationService;
import com.auito.automationtools.service.DeviceManagerExceptionHandler;
import com.auito.automationtools.service.PhoneImageService;
import com.auito.automationtools.service.SessionReaper;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMq;
import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMqConfig;
import io.arivera.oss.embedded.rabbitmq.bin.RabbitMqPlugins;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class RemoteDeviceManagerTest {

	@Autowired
	private AllocationService allocationService;

	@Autowired
	private PhoneImageService phoneImageService;

	private static EmbeddedRabbitMq rabbitMq;

	@Autowired
	private RabbitTemplate template;

	@Autowired
	private AppiumController appiumController;
	@Autowired
	private PhoneImageController phoneImageController;

	@Autowired
	private HomeController homeController;

	@Autowired
	private IDeviceRepository deviceRepo;

	@Autowired
	private SessionReaper sessionReaper;

	@BeforeClass
	public static void initServices() throws IOException {
		setupRabbitMQ();
		setupMongo();

	}

	@Before
	public void addDevice() {

		Device device = new Device();
		device.setFree(true);
		device.setSlaveIp("0.0.0.0");
		device.setId("emulator-5554");
		device.setFree(false);
		device.setDeviceInformation(getDeviceInfo("emulator-5554", "11"));
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MINUTE, -30);
		device.setLastAllocationStart(calendar.getTime());
		deviceRepo.save(device);

		device = new Device();
		device.setFree(true);
		device.setSlaveIp("0.0.0.0");
		device.setId("emulator-5556");
		device.setFree(true);
		device.setDeviceInformation(getDeviceInfo("emulator-5556", "11"));
		deviceRepo.save(device);

	}

	private static void setupRabbitMQ() {
		EmbeddedRabbitMqConfig config = new EmbeddedRabbitMqConfig.Builder()
				.useCachedDownload(true)
				.rabbitMqServerInitializationTimeoutInMillis(30000)
				.defaultRabbitMqCtlTimeoutInMillis(30000).build();
		RabbitMqPlugins rabbitMqPlugins = new RabbitMqPlugins(config);
		rabbitMqPlugins.enable("rabbitmq_management");
		rabbitMq = new EmbeddedRabbitMq(config);
		rabbitMq.start();
	}

	private static final MongodStarter starter = MongodStarter.getDefaultInstance();

	private static MongodExecutable mongodExe;
	private static MongodProcess mongoD;

	private static int port;

	private static void setupMongo() {
		try {
			port = 27017;
			mongodExe = starter.prepare(new MongodConfigBuilder()
					.version(Version.Main.PRODUCTION)
					.net(new Net(port, Network.localhostIsIPv6()))
					.build());
			mongoD = mongodExe.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@AfterClass
	public static void tearDownServices() {
		rabbitMq.stop();
		mongoD.stop();
		mongodExe.stop();
	}

	@Test
	public void updateDevices() {

		DeviceUpdateRequest request = getDeviceUpdateRequest("emulator-5554");
		List<DeviceInformation> devices = new ArrayList<>(request.getAndroidEmulators());
		devices.add(getDeviceInfo("emulator-5556", "11"));
		request.setAndroidEmulators(devices);
		template.convertAndSend(DEVICE_UPDATE_QUEUE_NAME, request);
		sleep(1000);
		assertThat(deviceRepo.count()).isGreaterThan(0);
	}

	private DeviceUpdateRequest getDeviceUpdateRequest(String deviceId) {
		DeviceUpdateRequest updateRequest = new DeviceUpdateRequest();
		updateRequest.setIp("0.0.0.0");
		updateRequest.setAndroidEmulators(Arrays.asList(getDeviceInfo(deviceId, "10")));
		return updateRequest;
	}

	private DeviceInformation getDeviceInfo(String deviceId, String sdkVersion) {
		DeviceInformation deviceInfo = new DeviceInformation();
		deviceInfo.setSdkVersion(sdkVersion);
		deviceInfo.setAndroid(true);
		deviceInfo.setRealDevice(false);
		deviceInfo.setManufacturer("Google");
		deviceInfo.setMarketName("Emulator");
		deviceInfo.setModel("Emulator");
		deviceInfo.setDeviceId(deviceId);
		return deviceInfo;
	}

	@Test
	public void longRunningSessions() {
		sessionReaper.reapLongRunningSessions();
		assertThat(deviceRepo.findFreeDevices().size()).isGreaterThan(0);
	}

	@Test
	public void unreacheableHosts() {
		sessionReaper.reapUnreacheableSlaves();
		assertThat(deviceRepo.count()).isEqualTo(0);
	}

	@Test
	public void clearAndroidPackage() {

		long timeout = 5;

		DeviceRequest request = new DeviceRequest();
		request.setIsAndroid("true");
		request.setDeviceName("emulator,simulator");
		request.setIsRealDevice("false");
		request.setBrand("Google");
		request.setVersion("10,11,12");
		request.setClearUserData(true);

		assertThrows(Exception.class,
				() -> allocationService.allocateDevice(timeout, request, null).get(timeout + 1, TimeUnit.SECONDS),
				"'app_package' is needed when 'clean_user_data' is set to 'true'");

	}

	@Test
	public void unableToAllocate() {

		long timeout = 5;

		DeviceRequest request = new DeviceRequest();
		request.setIsAndroid("true");
		request.setDeviceName("emulator,simulator");
		request.setIsRealDevice("false");
		request.setBrand("Google");
		request.setVersion("10,11,12");

		assertThrows(Exception.class,
				() -> allocationService.allocateDevice(timeout, request, null).get(timeout + 1, TimeUnit.SECONDS),
				String.format("unable to allocate device for request '.*' even after '%s seconds'", timeout));

	}

	@Test
	public void unallocateDevice() {

		DeviceRestrictionRequest request = new DeviceRestrictionRequest();
		request.setSdkVersion("11");
		request.setDeviceId(Arrays.asList("emulator-5554"));
		request.setDeviceName("emulator");
		request.setSlaveIp("0.0.0.0");

		assertThrows(DeviceManagerException.class, () -> allocationService.unallocateDevice(request, null),
				"Connection refused");

	}

	@Test
	public void unallocateAllDevices() {

		assertThrows(DeviceManagerException.class, () -> allocationService.unallocateAllDevices(null),
				"Connection refused");
	}

	@Test
	public void blacklist() {

		DeviceRestrictionRequest request = new DeviceRestrictionRequest();
		request.setSdkVersion("11");
		request.setDeviceId(Arrays.asList("emulator-5554"));
		request.setDeviceName("emulator");
		request.setBrand("Google");
		request.setSlaveIp("0.0.0.0");

		assertThat(allocationService.updateDeviceStatus(request, true, null)).isNotNull();
	}

	@Test
	public void whitelist() {

		DeviceRestrictionRequest request = new DeviceRestrictionRequest();
		request.setSdkVersion("11");
		request.setDeviceId(Arrays.asList("emulator-5554"));
		request.setDeviceName("emulator");
		request.setBrand("Google");

		assertThat(allocationService.updateDeviceStatus(request, false, null)).isNotNull();
	}

	@Test
	public void addPhoto() throws IOException {
		assertThat(phoneImageService.addPhoto(new File("src/test/resources/phone.png"))).isNotNull();
	}

	@Test
	public void getPhotoByName() throws Exception {
		assertThat(phoneImageService.getPhotoByName("ktdshfgjhk")).isNotEmpty();
	}

	@Test
	public void allocate() throws Exception {

		MockMvc mvc = MockMvcBuilders.standaloneSetup(appiumController)
				.setControllerAdvice(new DeviceManagerExceptionHandler()).build();
		DeviceRequest request = new DeviceRequest();
		request.setIsAndroid("true");
		request.setDeviceName("emulator,simulator");
		request.setIsRealDevice("false");
		request.setBrand("Google");
		request.setVersion("10,11,12");

		MockHttpServletResponse response = mvc.perform(
				post("/appium/allocate?timeout=10")
						.contentType("application/json")
						.content(request.toJson()))
				.andReturn()
				.getResponse();
		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	public void controllerUnallocateValidDevice() throws Exception {
		MockMvc mvc = MockMvcBuilders.standaloneSetup(appiumController)
				.setControllerAdvice(new DeviceManagerExceptionHandler()).build();
		DeviceRestrictionRequest request = new DeviceRestrictionRequest();
		request.setDeviceName("emulator");
		MockHttpServletResponse response = mvc.perform(
				post("/appium/unallocate")
						.contentType("application/json")
						.content(request.toJson()))
				.andReturn()
				.getResponse();
		assertThat(response.getStatus()).isEqualTo(502);
	}

	@Test
	public void controllerUnallocateInvalidDevice() throws Exception {
		MockMvc mvc = MockMvcBuilders.standaloneSetup(appiumController)
				.setControllerAdvice(new DeviceManagerExceptionHandler()).build();
		DeviceRestrictionRequest request = new DeviceRestrictionRequest();
		request.setDeviceName("dhfgjkhgfd");
		MockHttpServletResponse response = mvc.perform(
				post("/appium/unallocate")
						.contentType("application/json")
						.content(request.toJson()))
				.andReturn()
				.getResponse();
		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	public void controllerUnallocateAll() throws Exception {
		MockMvc mvc = MockMvcBuilders.standaloneSetup(appiumController)
				.setControllerAdvice(new DeviceManagerExceptionHandler()).build();
		MockHttpServletResponse response = mvc.perform(
				post("/appium/unallocateAll")
						.contentType("application/json"))
				.andReturn()
				.getResponse();
		assertThat(response.getStatus()).isEqualTo(502);
	}

	@Test
	public void controllerBlacklist() throws Exception {
		MockMvc mvc = MockMvcBuilders.standaloneSetup(appiumController)
				.setControllerAdvice(new DeviceManagerExceptionHandler()).build();
		DeviceRestrictionRequest request = new DeviceRestrictionRequest();
		request.setDeviceName("emulator");
		MockHttpServletResponse response = mvc.perform(
				post("/appium/blacklist")
						.contentType("application/json")
						.content(request.toJson()))
				.andReturn()
				.getResponse();
		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	public void controllerWhitellist() throws Exception {
		MockMvc mvc = MockMvcBuilders.standaloneSetup(appiumController)
				.setControllerAdvice(new DeviceManagerExceptionHandler()).build();
		DeviceRestrictionRequest request = new DeviceRestrictionRequest();
		request.setDeviceName("emulator");
		MockHttpServletResponse response = mvc.perform(
				post("/appium/whitelist")
						.contentType("application/json")
						.content(request.toJson()))
				.andReturn()
				.getResponse();
		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	public void controllerAddPhoto() throws Exception {
		MockMvc mvc = MockMvcBuilders.standaloneSetup(phoneImageController)
				.setControllerAdvice(new DeviceManagerExceptionHandler()).build();

		MockMultipartHttpServletRequestBuilder builder = MockMvcRequestBuilders.multipart("/photos/add");

		MockHttpServletResponse response = mvc.perform(builder.file(new MockMultipartFile("file",
				java.nio.file.Files.readAllBytes(new File("src/test/resources/phone.png").toPath())))
				.param("name", "default")).andReturn().getResponse();

		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	public void home() throws Exception {

		MockMvc mvc = MockMvcBuilders.standaloneSetup(homeController).build();
		MockHttpServletResponse response = mvc.perform(get("/")).andReturn().getResponse();
		assertThat(response.getStatus()).isEqualTo(200);
	}

	private void sleep(int milli) {
		try {
			TimeUnit.MILLISECONDS.sleep(milli);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
