package com.lampo.slave.utils;

import static com.lampo.slave.utils.CommandLineExecutor.exec;
import static com.lampo.slave.utils.CommandLineExecutor.killProcess;
import static com.lampo.slave.utils.StringUtils.getLocalNetworkIP;
import static com.lampo.slave.utils.StringUtils.getProperty;
import static java.lang.Runtime.getRuntime;

import java.io.File;
import java.util.concurrent.TimeUnit;

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
 * Commercial distributors of software may accept certain responsibilities with
 * respect to end users, business partners and the like. While this license is
 * intended to facilitate the commercial use of the Program, the Contributor who
 * includes the Program in a commercial product offering should do so in a
 * manner which does not create potential liability for other Contributors.
 * <br/>
 * <br/>
 * 
 * This License does not grant permission to use the trade names, trademarks,
 * service marks, or product names of the Licensor, except as required for
 * reasonable and customary use in describing the origin of the Work and
 * reproducing the content of the NOTICE file. <br/>
 * <br/>
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
 * the usage agreement.
 * 
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
			CommandLineResponse response = exec("ps -ef | grep -i 'stf' | grep -v grep | wc -l");
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
			killProcess("start_stf.bash", true);
		}

		public void start() {
			String script = StringUtils
					.streamToString(STFServiceBuilder.class.getResourceAsStream("/scripts/stf.bash"));
			File scriptFile = new File("start_stf.bash");
			log.info("starting STF service");
			new Thread(() -> {
				try {
					java.nio.file.Files.write(scriptFile.toPath(), script.getBytes());
					String cmd = String.format("bash %s %s %s", scriptFile.getAbsolutePath(), getLocalNetworkIP(),
							getProperty("STF_SCREENSHOT_QUALITY", DEFAULT_STF_SCREENSHOT_QUALITY));
					getRuntime().exec(cmd).waitFor(5, TimeUnit.SECONDS);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					scriptFile.delete();
				}
			}).start();
		}

	}
}
