package com.auito.automationtools.model;

import static com.auito.automationtools.utils.StringUtils.pojoToJson;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class DeviceRestrictionRequest {

	@JsonProperty("device_id")
	private List<String> deviceId;

	@JsonProperty("slave_ip")
	private String slaveIp;

	private String brand;

	@JsonProperty("device_name")
	private String deviceName;

	@JsonProperty("sdk_version")
	private String sdkVersion;

	public String toJson() {
		return pojoToJson(this);
	}

	public DeviceRestrictionRequest(@NonNull String deviceId, @NonNull String slaveIp) {
		this(Arrays.asList(deviceId), slaveIp, null, null, null);
	}
}
