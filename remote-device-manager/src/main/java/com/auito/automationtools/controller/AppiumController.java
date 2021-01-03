package com.auito.automationtools.controller;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.auito.automationtools.model.AppiumSession;
import com.auito.automationtools.model.DeviceInformation;
import com.auito.automationtools.model.DeviceRequest;
import com.auito.automationtools.model.DeviceRestrictionRequest;
import com.auito.automationtools.service.AllocationService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/appium")
public class AppiumController {

	@Autowired
	private AllocationService allocationService;

	@PostMapping("/allocate")
	public CompletableFuture<AppiumSession> allocateDevice(
			@RequestParam(defaultValue = "-1", value = "timeout") int timeoutInSeconds,
			@RequestBody DeviceRequest request, HttpServletRequest servletRequest) {

		return allocationService.allocateDevice(timeoutInSeconds, request, servletRequest);
	}

	@PostMapping("/unallocateAll")
	public boolean unallocateAllDevices(HttpServletRequest servletRequest) {
		return !allocationService.unallocateAllDevices(servletRequest).isEmpty();
	}

	@PostMapping("/unallocate")
	public boolean unallocateDevice(@RequestBody DeviceRestrictionRequest request, HttpServletRequest servletRequest) {
		return !allocationService.unallocateDevice(request, servletRequest).isEmpty();
	}

	@PostMapping("/blacklist")
	public Collection<DeviceInformation> blacklistDevices(@RequestBody DeviceRestrictionRequest request,
			HttpServletRequest servletRequest) {
		Collection<DeviceInformation> devices = allocationService.updateDeviceStatus(request, true, servletRequest);
		log.info("blacklisted devices => {}", devices);
		return devices;
	}

	@PostMapping("/whitelist")
	public Collection<DeviceInformation> whitelistDevices(@RequestBody DeviceRestrictionRequest request,
			HttpServletRequest servletRequest) {
		Collection<DeviceInformation> devices = allocationService.updateDeviceStatus(request, false, servletRequest);
		log.info("whitelisted devices => {}", devices);
		return devices;
	}

}
