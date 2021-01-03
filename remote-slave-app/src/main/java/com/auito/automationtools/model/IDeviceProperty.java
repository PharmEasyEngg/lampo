package com.auito.automationtools.model;

import static com.auito.automationtools.utils.StringUtils.pojoToJson;

public interface IDeviceProperty {

	public String getDeviceId();

	public boolean isAndroid();

	public boolean isRealDevice();

	public AllocatedTo getAllocatedTo();

	default String toJson() {
		return pojoToJson(this);
	}
}
