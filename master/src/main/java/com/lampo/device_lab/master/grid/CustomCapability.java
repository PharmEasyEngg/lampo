package com.lampo.device_lab.master.grid;

import java.util.Map;
import java.util.Map.Entry;

import org.openqa.selenium.remote.DesiredCapabilities;

import lombok.NonNull;

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
public enum CustomCapability {

	TEAM_NAME("ci.team_name"),

	JOB_LINK("ci.job_link"),

	USER("ci.user"),

	IP("ci.requestor_ip"),

	REQUEST_ID("session.request_id"),

	SESSION_MAX_TIMEOUT("session.wait_timeout"),

	RECORD_VIDEO("session.record_video");

	private String name;

	CustomCapability(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public static <T> T search(@NonNull DesiredCapabilities caps, @NonNull CustomCapability name) {
		return search(caps, name);
	}

	public static <T> T search(@NonNull DesiredCapabilities caps, @NonNull CustomCapability name, T defaultValue) {
		return search(caps.asMap(), name, defaultValue);
	}

	public static <T> T search(@NonNull Map<String, Object> caps, @NonNull CustomCapability name) {
		return search(caps, name, null);
	}

	public static <T> T search(@NonNull Map<String, Object> caps, @NonNull CustomCapability name, T defaultValue) {
		return search(caps, name.name, defaultValue);
	}

	public static <T> T search(@NonNull Map<String, Object> caps, @NonNull String name) {
		return search(caps, name, null);
	}

	@SuppressWarnings("unchecked")
	public static <T> T search(@NonNull Map<String, Object> caps, @NonNull String name, T defaultValue) {

		String _name = name.toLowerCase();
		for (Entry<String, Object> entry : caps.entrySet()) {
			if (entry.getKey().toLowerCase().contains(_name)) {
				return (T) entry.getValue();
			}
		}
		return defaultValue;
	}

}
