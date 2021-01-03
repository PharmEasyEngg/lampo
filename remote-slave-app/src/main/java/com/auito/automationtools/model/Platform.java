package com.auito.automationtools.model;

public enum Platform {

	WINDOWS("win"), MACINTOSH("mac"), LINUX("linux");

	private String name;

	private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
	public static final Platform CURRENT_PLATFORM = getCurrentPlatform();

	Platform(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}

	public String getArchitecture() {
		return System.getProperty("os.arch").contains("64") ? "64" : "32";
	}

	private static Platform getCurrentPlatform() {
		if (OS_NAME.contains("win")) {
			return WINDOWS;
		} else if (OS_NAME.contains("mac")) {
			return Platform.MACINTOSH;
		} else if (OS_NAME.contains("linux")) {
			return LINUX;
		}
		return null;
	}
}
