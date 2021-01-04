package com.lampo.slave.utils;

import static com.lampo.slave.utils.StringUtils.streamToString;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.lampo.slave.model.Platform;

import lombok.NonNull;
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
@Slf4j
public final class CommandLineExecutor {

	private static final Platform PLATFORM = Platform.CURRENT_PLATFORM;

	private CommandLineExecutor() {
	}

	/**
	 * Run system commands and get response back.
	 *
	 * @param file {@link String}
	 * @param args {@link String}[] containing arguments that are to be passed to
	 *             executable
	 * @return {@link CommandLineResponse}
	 */
	public static CommandLineResponse execFile(final String file, final String... args) {
		if (isEmpty(file)) {
			return null;
		}

		switch (PLATFORM) {
		case LINUX:
		case MACINTOSH:
			return execCommand(mergeArrays(new String[] { "bash", file.trim() }, args));
		case WINDOWS:
			return execCommand(mergeArrays(new String[] { "cmd", "/c", file.trim() }, args));
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static <T> T[] mergeArrays(T[] arrA, T[] arrB) {
		return (T[]) Stream.of(arrA, arrB).flatMap(Stream::of).toArray();
	}

	private static boolean isEmpty(String str) {
		return str == null || str.trim().isEmpty();
	}

	/**
	 * Run system commands and get response back.
	 *
	 * @param command {@link String}
	 * @return {@link CommandLineResponse}
	 */
	public static CommandLineResponse exec(final String command) {
		if (isEmpty(command)) {
			return null;
		}
		if (Platform.CURRENT_PLATFORM == Platform.WINDOWS) {
			return execCommand("cmd", "/c", command);
		}
		return execCommand("bash", "-c", command.trim());
	}

	public static CommandLineResponse execCommand(final String... command) {
		if (command == null || command.length == 0) {
			return null;
		}
		String _cmd = String.join(" ", command);
		log.debug("executing command : {}", _cmd);
		Process process = null;
		try {
			ProcessBuilder builder = new ProcessBuilder(command);
			CommandLineResponse response = new CommandLineResponse();
			if (Platform.CURRENT_PLATFORM != Platform.WINDOWS) {
				Map<String, String> env = builder.environment();
				env.put("PATH", env.get("PATH") + ":/usr/local/bin:" + System.getenv("HOME") + "/.linuxbrew/bin");
				process = builder.start();
			} else {
				Map<String, String> env = builder.environment();
				env.put("PATH", System.getenv("Path") == null ? System.getenv("PATH") : System.getenv("Path"));
				process = Runtime.getRuntime().exec("cmd /C " + String.join(" ", _cmd));
			}
			process.waitFor(60, TimeUnit.SECONDS);
			response.setStdOut(streamToString(process.getInputStream()).trim());
			response.setErrOut(streamToString(process.getErrorStream()).trim());
			response.setExitCode(process.exitValue());
			log.trace("response: {}", response);
			return response;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if (process != null) {
				process.destroy();
			}
		}
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
		String cmd = String
				.format(Platform.CURRENT_PLATFORM == Platform.MACINTOSH ? "lsof -nti:%s | xargs kill -9"
						: "fuser -k %s/tcp", port);
		return 0 == exec(cmd).getExitCode();
	}

	public static boolean isPortListening(int port) {
		if (Platform.CURRENT_PLATFORM == Platform.WINDOWS) {
			return false;
		}
		String cmd = String
				.format(Platform.CURRENT_PLATFORM == Platform.MACINTOSH ? "lsof -nti:%s | wc -l"
						: "netstat -ntlp | grep LISTEN | grep %s | wc -l", port);
		return Integer.parseInt(exec(cmd).getStdOut().trim()) > 0;
	}

}