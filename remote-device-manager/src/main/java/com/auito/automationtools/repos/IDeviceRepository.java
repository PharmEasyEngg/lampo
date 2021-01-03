package com.auito.automationtools.repos;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.auito.automationtools.model.Device;

import lombok.NonNull;

public interface IDeviceRepository extends MongoRepository<Device, String> {

	@Override
	default Optional<Device> findById(String id) {
		return findAll().stream().filter(e -> e.getId().equals(id)).findFirst();
	}

	public Collection<Device> findBySlaveIp(String slaveIp);

	default Collection<Device> findFreeDevices() {
		return findAll().stream()
				.filter(Device::isFree)
				.collect(Collectors.toList());
	}

	default Collection<Device> findFreeAndroidDevices() {
		return findAll().stream()
				.filter(e -> e.isFree() && e.getDeviceInformation() != null && e.getDeviceInformation().isAndroid())
				.collect(Collectors.toList());
	}

	default Collection<Device> findFreeIosDevices() {
		return findAll().stream()
				.filter(e -> e.isFree() && e.getDeviceInformation() != null && !e.getDeviceInformation().isAndroid())
				.collect(Collectors.toList());
	}

	default Collection<Device> findAndroidDevices() {
		return findAll().stream()
				.filter(e -> e.getDeviceInformation() != null && e.getDeviceInformation().isAndroid())
				.collect(Collectors.toList());
	}

	default Collection<Device> findIosDevices() {
		return findAll().stream()
				.filter(e -> e.getDeviceInformation() != null && !e.getDeviceInformation().isAndroid())
				.collect(Collectors.toList());
	}

	default Optional<Device> findById(String deviceId, String slaveIp) {
		return findAll().stream().filter(e -> e.getId().equals(deviceId) && e.getSlaveIp().equals(slaveIp)).findFirst();
	}

	default void deleteById(@NonNull String deviceId, @NonNull String slaveIp) {
		Optional<Device> device = findById(deviceId, slaveIp);
		if (device.isPresent()) {
			delete(device.get());
		}
	}

}