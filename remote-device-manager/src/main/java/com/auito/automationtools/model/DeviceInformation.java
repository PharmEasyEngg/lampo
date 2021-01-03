package com.auito.automationtools.model;

import static com.auito.automationtools.utils.StringUtils.pojoToJson;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class DeviceInformation {

	@JsonProperty("device_id")
	private String deviceId;

	@JsonProperty("sdk_version")
	private String sdkVersion;

	private String model;

	@JsonProperty("market_name")
	private String marketName;

	private String manufacturer;

	@JsonProperty("is_android")
	private boolean isAndroid;

	@JsonProperty("is_real_device")
	private boolean isRealDevice;

	@JsonProperty("browser_version")
	private String browserVersion;

	public String toJson() {
		return pojoToJson(this);
	}
}
