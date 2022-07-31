package com.lampo.device_lab.master.grid;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.openqa.grid.selenium.GridLauncherV3;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.lampo.device_lab.master.utils.ProcessUtilities;

import lombok.extern.slf4j.Slf4j;

/**
 * MIT License <br/>
 * <br/>
 * 
 * Copyright (c) [2022] [PharmEasyEngg] <br/>
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
@Slf4j
@Service
public class GridSetupService {

	@Value("${custom.grid.download_url}")
	private String url;

	@Value("${custom.session.wait_timeout}")
	private int sessionTimeout;

	public static final int GRID_PORT = 4444;
	private static final File SELENIUM_SERVER_LOG = new File("logs", "selenium-server.txt");
	private static final String MATCHER = CustomGridNodeCapabilityMatcher.class.getName();
	private static final String REGISTRY = CustomGridRegistry.class.getName();

	@PostConstruct
	public void restartServer() {
		if (SELENIUM_SERVER_LOG.getParentFile() != null && !SELENIUM_SERVER_LOG.getParentFile().exists()) {
			SELENIUM_SERVER_LOG.getParentFile().mkdirs();
		}

		String[] args = { "-role", "hub", "-port", String.valueOf(GRID_PORT), "-matcher", MATCHER, "-log",
				SELENIUM_SERVER_LOG.getAbsolutePath(), "-registry", REGISTRY, "-newSessionWaitTimeout",
				String.valueOf(5000), "-browserTimeout", String.valueOf(sessionTimeout), "-cleanUpCycle",
				String.valueOf(sessionTimeout * 1000), "-throwOnCapabilityNotPresent", "false", "-timeout",
				String.valueOf(sessionTimeout) };

		log.info("--------- starting selenium grid server with args '{}' ---------",
				Arrays.stream(args).collect(Collectors.joining(" ")));

		GridLauncherV3.main(args);
	}

	@Scheduled(cron = "${custom.cron.check_grid_service}")
	public void checkGridServerRunning() {
		if (!ProcessUtilities.isPortListening(GRID_PORT)) {
			restartServer();
		}
	}
}
