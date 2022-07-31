package com.lampo.device_lab.slave.utils;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.lampo.device_lab.slave.model.CommandLineResponse;
import com.lampo.device_lab.slave.model.Platform;

import lombok.NonNull;
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
public class CommandLineExecutor {

	private CommandLineExecutor() {
	}

	public static CommandLineResponse exec(final String cmd) {

		log.debug("executing command : {}", cmd);
		CommandLineResponse response = new CommandLineResponse();
		Process process = null;
		try {
			ProcessBuilder builder = new ProcessBuilder("bash", "-c", cmd);

			Map<String, String> env = builder.environment();
			env.put("PATH",
					env.get("PATH") + ":/usr/local/bin:" + System.getenv("HOME") + "/.linuxbrew/bin:/opt/homebrew/bin");
			process = builder.start();

			process.waitFor(60, TimeUnit.SECONDS);
			response.setStdOut(CommonUtilities.toString(process.getInputStream()).trim());
			response.setErrOut(CommonUtilities.toString(process.getErrorStream()).trim());
			response.setExitCode(process.exitValue());
			log.trace("response: {}", response);
		} catch (Throwable e) {
			log.error("{} occurred while running command '{}'", e.getClass().getName(), cmd);
		} finally {
			if (process != null) {
				process.destroy();
			}
		}
		return response;
	}

	public static boolean killProcess(@NonNull String name) {
		return killProcess(name, false);
	}

	public static boolean killProcess(@NonNull String name, boolean killParent) {
		String cmd = killParent ? String.format(
				"ps -ef | grep -i '%s' | grep -v grep | awk '{print $2}' | tr -s '\\n' ' ' | xargs pkill -TERM -P",
				name)
				: String.format(
						"ps -ef | grep -i '%s' | grep -v stf | grep -v grep | awk '{print $2}' | tr -s '\\n' ' ' | xargs kill -9",
						name);
		return 0 == exec(cmd).getExitCode();
	}

	public static boolean killAppiumProcesses() {
		String cmd = "ps -ef | grep -v stf | grep -i 'appium' | grep -i 'node' | grep -i 'relaxed-security' | grep -v grep | awk '{print $2}' | tr -s '\\n' ' ' | xargs kill -9";
		return 0 == exec(cmd).getExitCode();
	}

	public static boolean killAppiumProcessesByDeviceId(@NonNull String deviceId) {
		String cmd = String.format(
				"ps -ef | grep -v stf | grep -i 'appium' | grep -i 'node' | grep -i 'relaxed-security' | grep -i '%s' | grep -v grep | awk '{print $2}' | tr -s '\\n' ' ' | xargs kill -9",
				deviceId);
		return 0 == exec(cmd).getExitCode();
	}

	public static boolean killProcessListeningAtPort(int port) {
		String cmd = String.format(
				Platform.CURRENT_PLATFORM == Platform.MACINTOSH ? "lsof -nti:%s | xargs kill -9" : "fuser -k %s/tcp",
				port);
		return 0 == exec(cmd).getExitCode();
	}
}
