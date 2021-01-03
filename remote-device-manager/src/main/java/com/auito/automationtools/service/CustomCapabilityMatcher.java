package com.auito.automationtools.service;

import static com.auito.automationtools.utils.StringUtils.isBlank;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.auito.automationtools.model.AllocationStrategy;
import com.auito.automationtools.model.Device;
import com.auito.automationtools.model.DeviceRequest;
import com.auito.automationtools.repos.IDeviceRepository;
import com.auito.automationtools.utils.StringUtils;

import lombok.NonNull;

@Component
public class CustomCapabilityMatcher implements ICapabilityMatcher {

	@Autowired
	private IDeviceRepository deviceRepository;

	@Override
	public Optional<Device> match(@NonNull DeviceRequest request) {

		return deviceRepository.findFreeDevices()
				.stream().parallel()
				.filter(e -> e.getAllocatedFor() == AllocationStrategy.AUTOMATION)
				.filter(e -> checkBlacklistedDevice(e))
				.filter(e -> checkDeviceKind(request, e))
				.filter(e -> checkDeviceType(request, e))
				.filter(e -> checkDeviceName(request, e))
				.filter(e -> checkDeviceVersion(request, e))
				.filter(e -> checkDeviceBrand(request, e))
				.findAny();
	}

	private boolean checkBlacklistedDevice(@NonNull Device device) {
		return !device.isBlacklisted();
	}

	private boolean checkDeviceKind(@NonNull DeviceRequest request, @NonNull Device device) {
		return isBlank(request.getIsAndroid())
				|| String.valueOf(device.getDeviceInformation().isAndroid())
						.equalsIgnoreCase(request.getIsAndroid().trim());
	}

	private boolean checkDeviceType(@NonNull DeviceRequest request, @NonNull Device device) {
		return isBlank(request.getIsRealDevice())
				|| String.valueOf(device.getDeviceInformation().isRealDevice())
						.equalsIgnoreCase(request.getIsRealDevice().trim());
	}

	private boolean checkDeviceBrand(@NonNull DeviceRequest request, @NonNull Device device) {
		return isBlank(request.getBrand())
				|| splitAndMatch(device.getDeviceInformation().getManufacturer(), request.getBrand());
	}

	private static boolean splitAndMatch(String text, String split) {
		if (text == null || split == null) {
			return false;
		}
		return Arrays.stream(split.trim().split(","))
				.map(e -> e.trim().toLowerCase())
				.anyMatch(e -> text.toLowerCase().contains(e));
	}

	private boolean checkDeviceVersion(@NonNull DeviceRequest request, @NonNull Device device) {
		String version = isBlank(request.getVersion()) ? null
				: Arrays.stream(request.getVersion().split(",")).map(String::trim)
						.map(StringUtils::getMajorVersion).collect(Collectors.joining(","));
		return isBlank(version) || splitAndMatch(device.getDeviceInformation().getSdkVersion(), version);
	}

	private boolean checkDeviceName(@NonNull DeviceRequest request, @NonNull Device device) {
		return checkDeviceName(request.getDeviceName(), device);
	}

	private boolean checkDeviceName(String deviceName, Device device) {
		return isBlank(deviceName)
				|| splitAndMatch(device.getDeviceInformation().getMarketName(), deviceName.toLowerCase())
				|| splitAndMatch(device.getDeviceInformation().getModel(), deviceName.toLowerCase());
	}

}
