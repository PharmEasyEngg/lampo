package com.auito.automationtools.model;

import com.auito.automationtools.utils.StringUtils;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class AppiumSessionRequest {

	@JsonProperty("device_id")
	private String deviceId;

	@JsonProperty("clean_user_data")
	private boolean cleanUserData;

	@JsonProperty("app_package")
	private String appPackage;

	public String toJson() {
		return StringUtils.pojoToJson(this);
	}
}
