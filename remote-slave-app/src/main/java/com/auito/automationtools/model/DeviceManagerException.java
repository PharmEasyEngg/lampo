package com.auito.automationtools.model;

import javax.validation.constraints.NotNull;

public class DeviceManagerException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public DeviceManagerException(@NotNull Throwable ex) {
		super(ex);
	}

	public DeviceManagerException(@NotNull String msg) {
		super(msg);
	}
}
