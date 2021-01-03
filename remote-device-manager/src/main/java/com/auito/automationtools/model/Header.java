package com.auito.automationtools.model;

public enum Header {

	JENKINS_JOB_LINK("x-jenkins-job-link"),
	REQUEST_ID("x-device-request-id"),
	USER("x-request-user"),
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
