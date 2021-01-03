package com.auito.automationtools.model;

import static com.auito.automationtools.utils.StringUtils.pojoToJson;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class DeviceRequest {

	@JsonProperty("is_android")
	private String isAndroid;

	@JsonProperty("is_real_device")
	private String isRealDevice;

	private String brand;

	private String version;

	@JsonProperty("device_name")
	private String deviceName;

	@JsonProperty("clear_user_data")
	private boolean clearUserData;

	@JsonProperty("app_package")
	private String appPackage;

	public String toJson() {
		return pojoToJson(this);
	}

}
