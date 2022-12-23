package com.lampo.device_lab.slave.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Capability {

	private String browserName;
	private String version;
	private int maxInstances = 1;
	private String platform;
	private String platformVersion;
	private String deviceName;
	private String udid;
	private String automationName;
	private boolean realDevice;
	private String slaveIp;

	public Capability setVersion(@NonNull String version) {
		this.version = version;
		this.platformVersion = version;
		return this;
	}

	public Capability setBrowserName(@NonNull String browserName) {
		this.browserName = browserName;
		return this;
	}

	public Capability setPlatform(@NonNull String platform) {
		this.platform = platform;
		return this;
	}

	public Capability setDeviceName(@NonNull String deviceName) {
		this.deviceName = deviceName;
		return this;
	}

	public Capability setUDID(@NonNull String uuid) {
		this.udid = uuid;
		return this;
	}

	public Capability setAutomationName(@NonNull String automationName) {
		this.automationName = automationName;
		return this;
	}

	public Capability setSlaveIp(@NonNull String slaveIp) {
		this.slaveIp = slaveIp;
		return this;
	}

	public Capability setRealDevice(boolean realDevice) {
		this.realDevice = realDevice;
		return this;
	}

}