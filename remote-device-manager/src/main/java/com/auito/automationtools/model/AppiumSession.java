package com.auito.automationtools.model;

import static com.auito.automationtools.utils.StringUtils.pojoToJson;

import java.net.URL;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class AppiumSession {

	@JsonProperty("device_id")
	private String deviceId;

	@JsonProperty("device_name")
	private String deviceName;

	@JsonProperty("is_android")
	private boolean isAndroid;

	@JsonProperty("is_real_device")
	private boolean isRealDevice;

	@JsonProperty("sdk_version")
	private String sdkVersion;

	@JsonProperty("slave_ip")
	private String slaveIp;

	@JsonProperty("session_url")
	private URL sessionUrl;

	@JsonProperty("appium_logs_url")
	private URL appiumLogs;

	public String toJson() {
		return pojoToJson(this);
	}
}
