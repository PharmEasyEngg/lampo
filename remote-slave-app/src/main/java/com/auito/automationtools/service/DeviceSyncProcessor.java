package com.auito.automationtools.service;

import static com.auito.automationtools.utils.ADBUtilities.getConnectedDevices;
import static com.auito.automationtools.utils.StringUtils.getLocalNetworkIP;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.auito.automationtools.model.DeviceUpdateRequest;
import com.auito.automationtools.model.IDeviceProperty;
import com.auito.automationtools.utils.IOSUtilities;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DeviceSyncProcessor {

	private static final String DEVICE_UPDATE_QUEUE_NAME = "devices";
	private static final ReentrantLock LOCK = new ReentrantLock(true);

	@Autowired
	private RabbitTemplate rabbitTemplate;

	private Collection<String> androidDeviceIds = new HashSet<>();
	private Collection<String> iosDeviceIds = new HashSet<>();

	@Scheduled(cron = "${cron.device_sync}")
	public boolean updateDeviceInfoToMaster() {

		try {
			LOCK.lock();

			long start = System.currentTimeMillis();

			DeviceUpdateRequest request = new DeviceUpdateRequest();
			request.setIp(getLocalNetworkIP());

			Collection<IDeviceProperty> androidDevices = getConnectedDevices().values();
			request.setAndroidEmulators(
					androidDevices.stream().filter(e -> !e.isRealDevice()).collect(toList()));
			request.setAndroidRealDevices(
					androidDevices.stream().filter(IDeviceProperty::isRealDevice).collect(toList()));
			request.setIosRealDevices(IOSUtilities.getConnectedRealDevices().values());
			request.setIosSimulators(IOSUtilities.getConnectedSimulators().values());

			rePopulateDeviceIds(request);

			rabbitTemplate.convertAndSend(DEVICE_UPDATE_QUEUE_NAME, request);

			long timeTaken = System.currentTimeMillis() - start;
			log.info("***** {} ms taken to send data {} *****", timeTaken, request);

			return true;
		} finally {
			LOCK.unlock();
		}
	}

	private void rePopulateDeviceIds(DeviceUpdateRequest request) {
		androidDeviceIds.clear();
		iosDeviceIds.clear();
		request.getAndroidEmulators().forEach(e -> androidDeviceIds.add(e.getDeviceId()));
		request.getAndroidRealDevices().forEach(e -> androidDeviceIds.add(e.getDeviceId()));
		request.getIosRealDevices().forEach(e -> iosDeviceIds.add(e.getDeviceId()));
		request.getIosSimulators().forEach(e -> iosDeviceIds.add(e.getDeviceId()));

	}

	public boolean isAndroid(@NonNull String deviceId) {
		try {
			LOCK.lock();
			return androidDeviceIds.contains(deviceId);
		} finally {
			LOCK.unlock();
		}
	}

	public boolean isConnectedDevice(@NonNull String deviceId) {
		try {
			LOCK.lock();
			return androidDeviceIds.contains(deviceId) || iosDeviceIds.contains(deviceId);
		} finally {
			LOCK.unlock();
		}
	}

}
