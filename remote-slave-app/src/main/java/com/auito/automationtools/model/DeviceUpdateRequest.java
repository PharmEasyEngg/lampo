package com.auito.automationtools.model;

import static com.auito.automationtools.utils.StringUtils.pojoToJson;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Data;

@Data
public class DeviceUpdateRequest {

	@Expose
	@JsonProperty("ip")
	private String ip;

	@Expose
	@SerializedName("android_real_devices")
	@JsonProperty("android_real_devices")
	private Collection<IDeviceProperty> androidRealDevices;

	@Expose
	@SerializedName("android_emulators")
	@JsonProperty("android_emulators")
	private Collection<IDeviceProperty> androidEmulators;

	@Expose
	@SerializedName("ios_real_devices")
	@JsonProperty("ios_real_devices")
	private Collection<IDeviceProperty> iosRealDevices;

	@Expose
	@SerializedName("ios_simulators")
	@JsonProperty("ios_simulators")
	private Collection<IDeviceProperty> iosSimulators;

	public String toJson() {
		return pojoToJson(this);
	}
}
