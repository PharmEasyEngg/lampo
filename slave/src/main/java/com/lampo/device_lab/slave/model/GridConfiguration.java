package com.lampo.device_lab.slave.model;

import static com.lampo.device_lab.slave.utils.CommonUtilities.getLocalNetworkIP;

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
public class GridConfiguration implements ToJson {

	private static final long serialVersionUID = 1L;

	private int cleanUpCycle = 30;
	private int timeout = 30;
	private String proxy = "com.lampo.device_lab.master.grid.CustomRemoteProxy";
	private String url;
	private String host;
	private int port;
	private int maxSession = 1;
	private boolean register = true;
	private int registerCycle = 5000;
	private int hubPort = 4444;
	private String hubHost;
	private String hubProtocol = "http";
	private int unregisterIfStillDownAfter = 10000;
	private boolean debug = true;

	public GridConfiguration(int port) {
		this(getLocalNetworkIP(), port);
	}

	public GridConfiguration(@NonNull String host, int port) {
		this(getLocalNetworkIP(), host, port);
	}

	public GridConfiguration(@NonNull String hubHost, @NonNull String host, int port) {
		this.hubHost = hubHost;
		this.host = host;
		this.port = port;
		this.url = String.format("%s://%s:%s/wd/hub", hubProtocol, host, port);
	}

}
