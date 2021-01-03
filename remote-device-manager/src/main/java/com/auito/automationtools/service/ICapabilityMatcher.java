package com.auito.automationtools.service;

import java.util.Optional;

import com.auito.automationtools.model.Device;
import com.auito.automationtools.model.DeviceRequest;

import lombok.NonNull;

public interface ICapabilityMatcher {

	public Optional<Device> match(@NonNull DeviceRequest request);
}
