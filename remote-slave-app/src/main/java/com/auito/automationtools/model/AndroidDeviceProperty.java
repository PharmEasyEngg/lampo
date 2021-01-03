package com.auito.automationtools.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.ToString;

@JsonPropertyOrder(alphabetic = true)
@Data
public class AndroidDeviceProperty implements IDeviceProperty {

	@Expose
	@SerializedName("device_id")
	@JsonProperty("device_id")
	private String deviceId;

	@Expose
	@SerializedName("ro.build.version.release")
	@JsonProperty("sdk_version")
	private String sdkVersion;

	@Expose
	@JsonProperty("brand")
	@SerializedName("ro.product.brand")
	@ToString.Exclude
	private String brand;

	@Expose
	@JsonProperty("model")
	@SerializedName("ro.product.model")
	@ToString.Exclude
	private String model;

	@Expose
	@JsonProperty("market_name")
	@SerializedName("ro.config.marketing_name")
	@ToString.Exclude
	private String marketName;

	@Expose
	@JsonProperty("manufacturer")
	@SerializedName("ro.product.manufacturer")
	private String manufacturer;

	@JsonIgnore
	@Expose
	@SerializedName("ro.build.characteristics")
	@ToString.Exclude
	private String buildCharacteristics;

	@Expose
	@SerializedName("is_android")
	@JsonProperty("is_android")
	private boolean isAndroid = true;

	@Expose
	@JsonProperty("is_real_device")
	@SerializedName("is_real_device")
	private boolean isRealDevice;

	@Expose
	@SerializedName("browser_version")
	@JsonProperty("browser_version")
	@ToString.Exclude
	private String chromeVersion;

	@ToString.Exclude
	private String memory;

	@Expose
	@SerializedName("allocated_to")
	@JsonProperty("allocated_to")
	@ToString.Exclude
	private AllocatedTo allocatedTo;

	@Override
	public boolean isRealDevice() {
		return isRealDevice;
	}

	@Override
	public boolean isAndroid() {
		return isAndroid;
	}

}
