package com.auito.automationtools.utils;

import lombok.Data;

@Data
public class CommandLineResponse {

	private int exitCode;
	private String stdOut;
	private String errOut;
}
