package com.auito.automationtools.controller;

import static com.auito.automationtools.utils.RequestUtils.getClientIp;
import static com.auito.automationtools.utils.StringUtils.getClippedName;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.auito.automationtools.model.Device;
import com.auito.automationtools.model.Photo;
import com.auito.automationtools.repos.IDeviceRepository;
import com.auito.automationtools.service.PhoneImageService;
import com.auito.automationtools.service.SessionReaper;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class HomeController {

	@Autowired
	private PhoneImageService phoneImageService;

	@Autowired
	private ApplicationContext context;

	@Autowired
	private SessionReaper reaperController;

	private static final int STF_PORT = 7100;

	@Autowired
	private IDeviceRepository devicesRepo;

	@PostConstruct
	public void initialize() {
		reaperController.reapUnreacheableSlaves();
		uploadDefaultImages();
	}

	private void uploadDefaultImages() {
		try {
			Arrays.stream(context.getResources("classpath:static/images/phones/*.png"))
					.forEach(e -> {
						File file = new File(System.getProperty("java.io.tmpdir"), e.getFilename());
						try (InputStream stream = e.getURL().openStream()) {
							Files.copy(stream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
							phoneImageService.addPhoto(file);
						} catch (IOException ex) {
							ex.printStackTrace();
						} finally {
							file.delete();
						}
					});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@GetMapping({ "/", "home", "index" })
	public String home(HttpServletRequest servletRequest, Model modelAndView) {

		String clientIp = getClientIp(servletRequest);

		Comparator<Map<String, Object>> comparator = (e1, e2) -> Integer
				.valueOf(getMajorVersion(e1.get("sdkVersion").toString()))
				.compareTo(Integer.valueOf(getMajorVersion(e2.get("sdkVersion").toString())));

		List<Map<String, Object>> devices = devicesRepo.findAll().stream()
				.filter(device -> !device.isBlacklisted())
				.map(device -> getDeviceProperties(device, clientIp))
				.collect(Collectors.groupingBy(device -> (boolean) device.get("isAndroid")))
				.values()
				.stream().flatMap(device -> device.stream().sorted(comparator))
				.collect(Collectors.toList());

		modelAndView.addAttribute("devices", devices);

		return "devices";
	}

	private String getMajorVersion(String version) {
		return (version.contains(".") ? version.substring(0, version.indexOf('.')) : version).trim();
	}

	private Map<String, Object> getDeviceProperties(@NonNull Device device, String clientIp) {

		Map<String, Object> props = new HashMap<>();
		try {
			props.put("deviceType", getDeviceType(device));
			props.put("sdkVersion", device.getDeviceInformation().getSdkVersion());
			props.put("manufacturer", capitalize(device.getDeviceInformation().getManufacturer()));
			props.put("marketName", getDisplayName(device));
			props.put("imageUrl", getPhoneImage(device));
			props.put("browserVersion", device.getDeviceInformation().getBrowserVersion());
			props.put("isFree", device.isFree());
			props.put("isAndroid", device.getDeviceInformation().isAndroid());
			props.put("allocatedFor", device.getAllocatedFor());
			props.put("allocatedTo", device.getLastAllocatedTo());
			props.put("url", getUrl(device, clientIp));
			props.put("model",
					getClippedName(
							device.getDeviceInformation().getModel() == null
									? device.getDeviceInformation().getMarketName()
									: device.getDeviceInformation().getModel(),
							15));
		} catch (Exception e) {
			log.error("'%s' occurred while parsing device properties(%s, %s) with message: '%s'", e.getClass(), device,
					clientIp, e.getMessage());
		}
		return props;
	}

	private String capitalize(String str) {
		return str == null ? null : str.substring(0, 1).toUpperCase() + str.substring(1);
	}

	private String getDeviceType(@NonNull Device device) {
		return device.getDeviceInformation().isRealDevice() ? "Real Device"
				: (device.getDeviceInformation().isAndroid() ? "Emulator" : "Simulator");
	}

	@Cacheable
	private String getPhoneImage(@NonNull Device device) {
		String name = (device.getDeviceInformation().isAndroid()
				? device.getDeviceInformation().getMarketName()
				: device.getDeviceInformation().getModel()).replaceAll("[\\(\\)]", " ");
		Optional<Photo> optional = phoneImageService.getPhotoByName(name).stream().findFirst();
		Photo img = optional.isPresent() ? optional.get() : null;
		return img == null ? "" : new String(Base64.getEncoder().encode(img.getImage().getData()));
	}

	private String getUrl(@NonNull Device device, String clientIp) {

		if (!device.getDeviceInformation().isAndroid()) {
			return "#";
		}
		return String.format("http://%s:%s/#!/control/%s",
				clientIp == null || clientIp.equals(device.getSlaveIp()) ? "localhost" : device.getSlaveIp(),
				STF_PORT, device.getDeviceInformation().getDeviceId());
	}

	private String getDisplayName(@NonNull Device device) {

		String name = null;
		try {
			if (device.getDeviceInformation().getMarketName() != null
					&& device.getDeviceInformation().getManufacturer() != null && device.getDeviceInformation()
							.getMarketName().equals(device.getDeviceInformation().getManufacturer())) {
				name = device.getDeviceInformation().getModel();
			} else {
				name = device.getDeviceInformation().getMarketName();
			}
		} catch (Exception e) {
			name = device.getDeviceInformation().getModel();
		}
		return capitalize(name);
	}

}
