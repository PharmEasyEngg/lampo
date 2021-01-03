package com.auito.automationtools.model;

import static com.auito.automationtools.utils.StringUtils.pojoToJson;

import java.util.Collection;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class DeviceUpdateRequest {

	@JsonProperty("last_modified_time")
	private Date lastModifiedTime = new Date();

	@JsonProperty("ip")
	private String ip;

	@JsonProperty("android_real_devices")
	private Collection<DeviceInformation> androidRealDevices;

	@JsonProperty("android_emulators")
	private Collection<DeviceInformation> androidEmulators;

	@JsonProperty("ios_real_devices")
	private Collection<DeviceInformation> iosRealDevices;

	@JsonProperty("ios_simulators")
	private Collection<DeviceInformation> iosSimulators;

	public String toJson() {
		return pojoToJson(this);
	}
}
