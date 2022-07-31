package com.lampo.device_lab.master.controller;

import static com.lampo.device_lab.master.utils.CommonUtilities.getClippedName;
import static java.util.stream.Collectors.groupingBy;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.lampo.device_lab.master.config.MVCConfiguration;
import com.lampo.device_lab.master.model.Device;
import com.lampo.device_lab.master.model.DeviceStatusModel;
import com.lampo.device_lab.master.model.HeldBy;
import com.lampo.device_lab.master.model.ModelDevice;
import com.lampo.device_lab.master.repos.IDeviceRepository;
import com.lampo.device_lab.master.repos.IDeviceStatusRepository;
import com.lampo.device_lab.master.repos.ITeamRepository;
import com.lampo.device_lab.master.service.PhoneImageService;
import com.lampo.device_lab.master.service.SessionReaper;
import com.lampo.device_lab.master.utils.CommonUtilities;
import com.lampo.device_lab.master.utils.RequestUtils;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

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
@Controller
public class HomeController {

	private static final String LOCAL_NETWORK_IP = CommonUtilities.getLocalNetworkIP();

	@Autowired
	private PhoneImageService phoneImageService;

	@Autowired
	private SessionReaper reaperController;

	private static final int STF_PORT = 7100;

	@Autowired
	private IDeviceRepository devicesRepo;

	@Autowired
	private ITeamRepository teamRepository;

	@Autowired
	private IDeviceStatusRepository deviceStatusRepo;

	@Value("${server.port}")
	private int serverPort;

	@Value("${custom.upload_dirs}")
	private String directories;

	@PostConstruct
	public void initialize() {
		reaperController.reapAll();
	}

	@GetMapping({ "/status" })
	public @ResponseBody String status() {
		return "app is working!";
	}

	@GetMapping({ "/", "home", "index" })
	public String home(HttpServletRequest servletRequest, Model modelAndView) {
		modelAndView.addAttribute("devices", getDevices(servletRequest));
		modelAndView.addAttribute("directories",
				Arrays.stream(directories.split(",")).map(String::trim).collect(Collectors.toList()));
		return "devices";
	}

	private final Comparator<ModelDevice> comparator = (e1, e2) -> Integer.valueOf(getMajorVersion(e1.getSdkVersion()))
			.compareTo(Integer.valueOf(getMajorVersion(e2.getSdkVersion())));

	@GetMapping({ "/devices" })
	@ResponseBody
	public List<ModelDevice> getDevices(HttpServletRequest servletRequest) {

		String clientIp = RequestUtils.getClientIp(servletRequest);

		return this.devicesRepo.findAll().stream().filter(device -> {
			Optional<DeviceStatusModel> optional = deviceStatusRepo.findById(device.getId());
			return !optional.isPresent() || !optional.get().isBlacklisted();
		}).map(device -> getDeviceProperties(device, clientIp))
				.collect(groupingBy(ModelDevice::isAndroid)).values().stream()
				.flatMap(device -> device.stream().sorted(comparator))
				.sorted((e1, e2) -> e1.getOwnedBy().compareTo(e2.getOwnedBy()))
				.collect(Collectors.toList());
	}

	@GetMapping({ "/refresh" })
	public String refresh(HttpServletRequest servletRequest, Model modelAndView) {
		modelAndView.addAttribute("devices", getDevices(servletRequest));
		return "fragments/card :: devices";
	}

	private String getMajorVersion(String version) {
		return (version.contains(".") ? version.substring(0, version.indexOf('.')) : version).trim();
	}

	private ModelDevice getDeviceProperties(@NonNull Device device, String clientIp) {

		ModelDevice mDevice = new ModelDevice();

		try {
			mDevice.setIp(device.getSlaveIp());
			mDevice.setConnected(device.isConnected());
			mDevice.setDeviceType(getDeviceType(device));
			mDevice.setSdkVersion(device.getDeviceInformation().getSdkVersion());
			mDevice.setManufacturer(capitalize(device.getDeviceInformation().getManufacturer()));
			mDevice.setMarketName(getDisplayName(device));
			mDevice.setImageUrl(getPhoneImage(device));
			mDevice.setBrowserVersion(device.getDeviceInformation().getBrowserVersion());
			mDevice.setFree(device.isFree());
			mDevice.setAndroid(device.getDeviceInformation().isAndroid());
			mDevice.setAllocatedTo(device.getLastAllocatedTo());
			mDevice.setUrl(getUrl(device, clientIp));
			mDevice.setOwnedBy(teamRepository.getTeam(device.getId()).toUpperCase());
			HeldBy sessionHeldBy = device.getStfSessionHeldBy();
			if (sessionHeldBy != null) {
				mDevice.setStfSessionHeldBy(sessionHeldBy);
			}
			mDevice.setModel(getClippedName(
					(device.getDeviceInformation().getModel() == null) ? device.getDeviceInformation().getMarketName()
							: device.getDeviceInformation().getModel(),
					15));
		} catch (Exception e) {
			log.error(String.format("'%s' occurred while parsing device properties(%s, %s) with message: '%s'",
					e.getClass(), device,
					clientIp, e.getMessage()), e);
		}
		return mDevice;
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
		String name = (device.getDeviceInformation().isAndroid() ? device.getDeviceInformation().getMarketName()
				: device.getDeviceInformation().getModel()).replaceAll("[\\(\\)]", " ");
		return phoneImageService.getPhotoByName(name).stream().findFirst().orElseGet(() -> "default");
	}

	private String getUrl(@NonNull Device device, String clientIp) {

		if (!device.getDeviceInformation().isAndroid()) {
			return "#";
		}
		return String.format("http://%s:%s/#!/control/%s",
				clientIp == null || clientIp.equals(device.getSlaveIp()) ? "localhost" : device.getSlaveIp(), STF_PORT,
				device.getDeviceInformation().getDeviceId());
	}

	@PostMapping("/upload")
	public @ResponseBody ResponseEntity<String> uploadApp(@RequestParam("file") MultipartFile file,
			@RequestParam String directory) {
		String originalFileName = file.getOriginalFilename();
		if (originalFileName == null || !(originalFileName.endsWith(".apk") || originalFileName.endsWith(".zip")
				|| originalFileName.endsWith(".ipa"))) {
			return ResponseEntity.badRequest().body("allowed file types - *.zip, *.ipa, *.apk");
		}
		originalFileName = originalFileName.replaceAll("\\s+", "_").toLowerCase();
		try (InputStream stream = file.getInputStream()) {
			File path = Paths.get(MVCConfiguration.RESOURCE_CREATION_DIR.getAbsolutePath(), directory, originalFileName)
					.toFile();
			if (path.getParentFile() != null && !path.getParentFile().exists()) {
				path.getParentFile().mkdirs();
			}
			Files.copy(stream, path.toPath(), StandardCopyOption.REPLACE_EXISTING);
			return ResponseEntity.ok(
					String.format("http://%s:%s/apps/%s/%s", LOCAL_NETWORK_IP, serverPort, directory,
							originalFileName));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
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

	@GetMapping("/files")
	public @ResponseBody List<String> getFiles(@RequestParam String prefix) {
		File file = new File(MVCConfiguration.RESOURCE_CREATION_DIR, prefix);
		if (!file.exists()) {
			return Collections.emptyList();
		}
		File[] files = file.listFiles(
				e -> e.getName().endsWith(".ipa") || e.getName().endsWith(".apk") || e.getName().endsWith(".zip"));
		Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
		return Arrays.stream(files).map(
				e -> e.toString().replace("files", String.format("http://%s:%s", LOCAL_NETWORK_IP, serverPort)))
				.collect(Collectors.toList());
	}

}
