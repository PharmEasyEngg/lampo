package com.auito.automationtools.model;

public enum Header {

	REQUEST_ID("x-device-request-id"),
	AUTH("x-auth-token");

	private String name;

	Header(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}
}
