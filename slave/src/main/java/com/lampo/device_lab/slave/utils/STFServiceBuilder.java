package com.lampo.device_lab.slave.utils;

import static com.lampo.device_lab.slave.utils.CommandLineExecutor.exec;
import static com.lampo.device_lab.slave.utils.CommonUtilities.getLocalNetworkIP;
import static com.lampo.device_lab.slave.utils.CommonUtilities.getProperty;
import static java.lang.Runtime.getRuntime;

import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import com.lampo.device_lab.slave.model.CommandLineResponse;

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
public final class STFServiceBuilder {

	private static final String DEFAULT_STF_SCREENSHOT_QUALITY = "10";

	private STFServiceBuilder() {
	}

	public static Builder builder() {
		return new Builder();
	}

	@Slf4j
	public static final class Builder {

		public boolean isSTFRunning() {
			CommandLineResponse response = exec(
					"ps -ef | grep -i stf | grep -i cli | grep -i 'device --serial' | grep -v grep | wc -l | xargs");
			boolean isRunning = Integer.parseInt(response.getStdOut().trim()) > 0;
			if (isRunning) {
				log.debug("STF service is running");
			} else {
				log.error("STF service is not running");
			}
			return isRunning;
		}

		public void restart() {
			stop();
			start();
		}

		public void stop() {
			log.info("stopping STF service");
			exec("ps -ef | grep -i 'stf local' | grep -v grep | awk '{print $2}' | tr -s '\\n' ' ' | xargs kill -9");
		}

		public void start() {
			File file = Paths.get(System.getProperty("user.home"), "mobile-device-lab", "start_stf.bash").toFile();
			log.info("starting STF service");
			new Thread(() -> {
				try {
					if (!file.exists()) {
						if (file.getParentFile() != null && !file.getParentFile().exists()) {
							file.getParentFile().mkdirs();
						}
						String script = CommonUtilities
								.toString(STFServiceBuilder.class.getResourceAsStream("/scripts/stf.bash"));
						java.nio.file.Files.write(file.toPath(), script.getBytes());
					}
					String cmd = String.format("bash %s %s %s", file.getAbsolutePath(), getLocalNetworkIP(),
							getProperty("STF_SCREENSHOT_QUALITY", DEFAULT_STF_SCREENSHOT_QUALITY));
					getRuntime().exec(cmd).waitFor(5, TimeUnit.SECONDS);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					file.delete();
				}
			}).start();
		}

	}
}
