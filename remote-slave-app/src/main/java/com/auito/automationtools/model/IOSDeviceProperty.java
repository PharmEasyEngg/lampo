package com.auito.automationtools.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.ToString;

@Data
public class IOSDeviceProperty implements IDeviceProperty {

	@Expose
	@SerializedName("device_id")
	@JsonProperty("device_id")
	private String deviceId;

	@JsonProperty("sdk_version")
	@SerializedName("sdk_version")
	@Expose
	private String sdkVersion;

	@Expose
	@JsonProperty("model")
	@SerializedName("model")
	@ToString.Exclude
	private String model;

	@JsonProperty("manufacturer")
	@SerializedName("manufacturer")
	@Expose
	private String manufacturer = "Apple";

	@Expose
	@SerializedName("is_android")
	@JsonProperty("is_android")
	private boolean isAndroid = false;

	@Expose
	@JsonProperty("is_real_device")
	@SerializedName("is_real_device")
	private boolean isRealDevice;

	@Expose
	@SerializedName("allocated_to")
	@JsonProperty("allocated_to")
	@ToString.Exclude
	private AllocatedTo allocatedTo;

	@Override
	public boolean isAndroid() {
		return isAndroid;
	}

	@Override
	public boolean isRealDevice() {
		return isRealDevice;
	}
}
