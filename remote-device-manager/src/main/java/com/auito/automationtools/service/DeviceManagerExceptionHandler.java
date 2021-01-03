package com.auito.automationtools.service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import com.auito.automationtools.model.DeviceManagerException;
import com.google.common.collect.ImmutableMap;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
public class DeviceManagerExceptionHandler {

	@ExceptionHandler(value = DeviceManagerException.class)
	public ResponseEntity<?> deviceManagerExceptionHandler(DeviceManagerException ex, WebRequest request) {
		log.error(ex.getMessage());
		return new ResponseEntity<>(ImmutableMap.of("error", ex.getMessage()),
				ex.getMessage().contains("auth") ? HttpStatus.UNAUTHORIZED
						: (ex.getMessage().contains("refused") ? HttpStatus.BAD_GATEWAY : HttpStatus.BAD_REQUEST));
	}
}
