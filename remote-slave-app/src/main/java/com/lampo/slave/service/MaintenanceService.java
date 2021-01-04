package com.lampo.slave.service;

import static com.lampo.slave.utils.ADBUtilities.restartADBServer;
import static com.lampo.slave.utils.AppiumLocalService.Builder.LOG_DIRECTORY;
import static com.lampo.slave.utils.CommandLineExecutor.exec;

import java.nio.file.Paths;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.lampo.slave.utils.AppiumLocalService;
import com.lampo.slave.utils.STFServiceBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * MIT License <br/>
 * <br/>
 * 
 * Copyright (c) [2021] [PharmEasyEngg] <br/>
 * <br/>
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, prepare derivatives of the work, and to permit
 * persons to whom the Software is furnished to do so, subject to the following
 * conditions: <br/>
 * <br/>
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software. <br/>
 * <br/>
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE. <br/>
 * <br/>
 * 
 * 
 * This software uses open-source dependencies that are listed under the
 * licenses - {@link <a href="https://www.eclipse.org/legal/epl-2.0/">Eclipse
 * Public License v2.0</a>},
 * {@link <a href="https://www.apache.org/licenses/LICENSE-2.0.html">Apache
 * License 2.0</a>}, {@link <a href=
 * "https://www.mongodb.com/licensing/server-side-public-license">Server Side
 * Public License</a>},
 * {@link <a href="https://www.mozilla.org/en-US/MPL/2.0/">Mozilla Public
 * License 2.0</a>} and {@link <a href="https://opensource.org/licenses/MIT">MIT
 * License</a>}. Please go through the description of the licenses to understand
 * the usage agreement. <br/>
 * <br/>
 * 
 * By using the license, you agree that you have read, understood and agree to
 * be bound by, including without any limitation by these terms and that the
 * entire risk as to the quality and performance of the software is with you.
 *
 */
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
