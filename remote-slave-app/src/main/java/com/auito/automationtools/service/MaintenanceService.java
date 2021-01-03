package com.auito.automationtools.service;

import static com.auito.automationtools.utils.ADBUtilities.restartADBServer;
import static com.auito.automationtools.utils.AppiumLocalService.Builder.LOG_DIRECTORY;
import static com.auito.automationtools.utils.CommandLineExecutor.exec;

import java.nio.file.Paths;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.auito.automationtools.utils.AppiumLocalService;
import com.auito.automationtools.utils.STFServiceBuilder;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MaintenanceService {

	/**
	 * cleaning up old logs at 00:00 daily
	 */
	@Scheduled(cron = "${cron.clean_up_logs}")
	public void cleanOldLogs() {
		log.info("********************** cleaning up old logs **********************");
		exec(String.format("rm -rf %s/*/*.log", Paths.get(LOG_DIRECTORY).toString()));
	}

	@Scheduled(cron = "${cron.restart_emulators}")
	public void restartEmulators() {
		log.info("********************** rebooting emulators **********************");
		AppiumLocalService.builder().restartEmulators();
	}

	@Scheduled(cron = "${cron.restart_stf}")
	public void restartSTF() {
		log.info("********************** restarting stf **********************");
		STFServiceBuilder.builder().restart();
	}

	@Scheduled(cron = "${cron.restart_adb}")
	public void restartADB() {
		log.info("********************** restarting adb **********************");
		restartADBServer();
	}

	@Scheduled(cron = "${cron.check_stf_service}")
	public void checkingSTFService() {
		if (!STFServiceBuilder.builder().isSTFRunning()) {
			STFServiceBuilder.builder().restart();
		}
	}

}
