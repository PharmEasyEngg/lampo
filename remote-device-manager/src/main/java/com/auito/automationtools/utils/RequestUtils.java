package com.auito.automationtools.utils;

import static com.auito.automationtools.utils.StringUtils.isBlank;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.http.HttpServletRequest;

public final class RequestUtils {

	private RequestUtils() {
	}

	private static final String LOCAL_IP_V4 = "127.0.0.1";
	private static final String LOCAL_IP_V6 = "0:0:0:0:0:0:0:1";

	public static String getClientIp(HttpServletRequest request) {

		if (request == null) {
			return LOCAL_IP_V4;
		}

		String ipAddress = request.getHeader("X-Forwarded-For");
		if (isBlank(ipAddress) || "unknown".equalsIgnoreCase(ipAddress)) {
			ipAddress = request.getHeader("Proxy-Client-IP");
		}

		if (isBlank(ipAddress) || "unknown".equalsIgnoreCase(ipAddress)) {
			ipAddress = request.getHeader("WL-Proxy-Client-IP");
		}

		if (isBlank(ipAddress) || "unknown".equalsIgnoreCase(ipAddress)) {
			ipAddress = request.getRemoteAddr();
			if (LOCAL_IP_V4.equals(ipAddress) || LOCAL_IP_V6.equals(ipAddress)) {
				try {
					InetAddress inetAddress = InetAddress.getLocalHost();
					ipAddress = inetAddress.getHostAddress();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
			}
		}
		if (!isBlank(ipAddress) && ipAddress.length() > 15 && ipAddress.indexOf(",") > 0) {
			ipAddress = ipAddress.substring(0, ipAddress.indexOf(","));
		}

		return ipAddress;

	}

}
