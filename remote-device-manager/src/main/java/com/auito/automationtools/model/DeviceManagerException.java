package com.auito.automationtools.model;

import lombok.NonNull;

public class DeviceManagerException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public DeviceManagerException(@NonNull String msg) {
		super(msg);
	}

}
