package com.lampo.master.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.NonNull;

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
public class StringUtils {

	private StringUtils() {
	}

	private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules()
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

	public static String pojoToJson(@NonNull Object obj) {
		try {
			return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String getClippedName(@NonNull String name, int maxLength) {
		String[] arr = name.split("\\s+");
		StringBuilder str = new StringBuilder();
		int index = 0;
		while (str.length() <= maxLength && arr.length > 0 && index < arr.length) {
			str.append(arr[index]).append(" ");
			index++;
		}
		return str.toString();
	}

	public static boolean isBlank(String str) {
		return str == null || str.trim().isEmpty();
	}

	public static String getMajorVersion(@NonNull String version) {
		return version.contains(".") ? version.substring(0, version.indexOf(".")) : version;
	}
}
