package com.auito.automationtools.controller;

import static com.auito.automationtools.model.Header.AUTH;
import static com.auito.automationtools.utils.StringUtils.isBlank;
import static java.util.Base64.getDecoder;

import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auito.automationtools.model.AppiumSessionRequest;
import com.auito.automationtools.model.DeviceManagerException;
import com.auito.automationtools.service.AppiumSessionService;

@RestController
@RequestMapping("/appium")
@PropertySource("classpath:application.properties")
public class AppiumController {

	@Autowired
	private AppiumSessionService service;

	@Value("${slave.auth_token}")
	private String authToken;

	@PostMapping("/create")
	public URL createAppiumSession(@RequestBody AppiumSessionRequest request, HttpServletRequest httpRequest) {
		authenticateRequest(httpRequest);
		return service.createAppiumSession(request, httpRequest);
	}

	@PostMapping("/stop")
	public boolean stopAppiumService(@RequestBody AppiumSessionRequest request, HttpServletRequest httpRequest) {
		authenticateRequest(httpRequest);
		return service.stopAppiumService(request.getDeviceId(), httpRequest);
	}

	private void authenticateRequest(HttpServletRequest request) {
		String value = request.getHeader(AUTH.toString());
		if (isBlank(value)) {
			throw new DeviceManagerException(String.format("'%s' header is missing", AUTH));
		}
		String slave = new String(getDecoder().decode(authToken));
		String master = new String(getDecoder().decode(value));
		if (!slave.equals(master)) {
			throw new DeviceManagerException(String.format("invalid '%s'", AUTH));
		}
	}

}
