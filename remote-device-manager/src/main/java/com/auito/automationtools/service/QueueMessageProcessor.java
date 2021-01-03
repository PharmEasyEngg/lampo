package com.auito.automationtools.service;

import static com.auito.automationtools.config.ClientConfiguration.DEVICE_UPDATE_QUEUE_NAME;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.auito.automationtools.model.Device;
import com.auito.automationtools.model.DeviceInformation;
import com.auito.automationtools.model.DeviceUpdateRequest;
import com.auito.automationtools.repos.IDeviceRepository;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class QueueMessageProcessor {

	@Autowired
	private IDeviceRepository deviceRepo;

	@RabbitListener(queues = DEVICE_UPDATE_QUEUE_NAME)
	public void process(@NonNull DeviceUpdateRequest deviceUpdateRequest) {

		log.debug("received message from queue => {}", deviceUpdateRequest);

		Collection<Entry<String, String>> oldDevices = deviceRepo.findBySlaveIp(deviceUpdateRequest.getIp())
				.stream()
				.map(e -> new SimpleEntry<>(e.getId(), e.getSlaveIp()))
				.collect(Collectors.toList());

		Collection<Entry<String, String>> currentDevices = Stream
				.of(deviceUpdateRequest.getAndroidEmulators(), deviceUpdateRequest.getAndroidRealDevices(),
						deviceUpdateRequest.getIosRealDevices(), deviceUpdateRequest.getIosSimulators())
				.filter(e -> e != null && !e.isEmpty())
				.flatMap(Collection::stream)
				.map(info -> getUpdatedDeviceInfo(deviceUpdateRequest, info))
				.collect(Collectors.toList());

		removeDisconnectedDevices(oldDevices, currentDevices);
	}

	private Entry<String, String> getUpdatedDeviceInfo(@NonNull DeviceUpdateRequest deviceUpdateRequest,
			@NonNull DeviceInformation info) {
		Optional<Device> device = deviceRepo.findById(info.getDeviceId(), deviceUpdateRequest.getIp());
		Device _device = null;
		if (!device.isPresent()) {
			_device = new Device();
			_device.setId(info.getDeviceId());
			_device.setFree(true);
		} else {
			_device = device.get();
		}
		_device.setDeviceInformation(info);
		_device.setSlaveIp(deviceUpdateRequest.getIp());
		_device.setLastModifiedTime(new Date());
		deviceRepo.save(_device);
		return new SimpleEntry<>(_device.getId(), _device.getSlaveIp());
	}

	private void removeDisconnectedDevices(@NonNull Collection<Entry<String, String>> oldDevices,
			@NonNull Collection<Entry<String, String>> currentDevices) {
		oldDevices.stream()
				.filter(e -> !currentDevices.contains(e))
				.forEach(e -> deviceRepo.deleteById(e.getKey(), e.getValue()));
	}

}