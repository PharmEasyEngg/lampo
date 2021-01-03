package com.auito.automationtools.service;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.auito.automationtools.model.DeviceRestrictionRequest;
import com.auito.automationtools.repos.IDeviceRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SessionReaper {

	private static final int SOCKET_TIME_OUT_IN_MSEC = 2000;
	private static final int DEFAULT_SLAVE_PORT = 5252;

	@Autowired
	private IDeviceRepository deviceRepo;

	@Autowired
	private AllocationService allocationService;

	@Value("${max_session_duration}")
	private int maxSessionDurationInSec;

	@Scheduled(cron = "${cron.reap_unreachable_slaves}")
	public void reapUnreacheableSlaves() {
		deviceRepo.findAll().stream().parallel().forEach(e -> {
			if (!isReachable(e.getSlaveIp(), DEFAULT_SLAVE_PORT, SOCKET_TIME_OUT_IN_MSEC)) {
				log.info("reaping unreacheable device '{}' on host '{}'", e.getId(), e.getSlaveIp());
				deviceRepo.delete(e);
			}
		});
	}

	@Scheduled(cron = "${cron.reap_long_running_sessions}")
	public void reapLongRunningSessions() {
		deviceRepo.findAll().stream()
				.parallel()
				.filter(e -> !e.isFree() && e.getLastAllocationStart() != null && e.getLastAllocationEnd() == null
						&& (System.currentTimeMillis()
								- e.getLastAllocationStart().getTime() >= maxSessionDurationInSec * 1000))
				.forEach(e -> {
					try {
						log.info("reaping long running session on device '{}' ::: running since '{}'", e);
						allocationService.unallocateDevice(new DeviceRestrictionRequest(e.getId(), e.getSlaveIp()),
								null);
					} catch (Throwable ex) {
						log.error("{} occurred when calling reapLongRunningSessions() with message: '{}'",
								ex.getClass().getName(), ex.getMessage());
					}
				});
	}

	private static boolean isReachable(String address, int port, int timeout) {
		try {
			try (java.net.Socket socket = new java.net.Socket()) {
				socket.setSoTimeout(timeout);
				socket.connect(new java.net.InetSocketAddress(address, port), timeout);
			}
			return true;
		} catch (IOException e) {
			return false;
		}
	}
}
