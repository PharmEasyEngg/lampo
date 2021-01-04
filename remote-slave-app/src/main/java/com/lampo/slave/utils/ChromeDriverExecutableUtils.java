package com.lampo.slave.utils;

import static com.lampo.slave.utils.StringUtils.streamToString;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.rauschig.jarchivelib.ArchiveFormat;
import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;

import com.google.common.collect.Maps;
import com.lampo.slave.model.Platform;

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
@Slf4j
public final class ChromeDriverExecutableUtils {

	private ChromeDriverExecutableUtils() {
	}

	private static final String DOWNLOAD_FOLDER = Paths.get(System.getProperty("user.home"), "driver", "chrome")
			.toString();

	private static final Platform PLATFORM = Platform.CURRENT_PLATFORM;
	private static final Map<String, List<Integer>> OLDER_CHROME_VERSION_MAPPING = new LinkedHashMap<>();

	public static File getChromeDriverExecutable(String chromeVersion) {
		String version = getMajorVersion(
				StringUtils.isBlank(chromeVersion) ? getLatestChromeDriverVersion() : chromeVersion);
		String versionToDownload = null;
		int _version = getInt(version.trim());
		if (_version > 0 && _version < 70) {
			Optional<Entry<String, List<Integer>>> match = getOlderChromeDriverMapping().entrySet().stream()
					.filter(e -> e.getValue().contains(_version)).findFirst();
			versionToDownload = match.isPresent() ? match.get().getKey() : getLatestChromeDriverVersion();
		} else {
			String url = String.format("https://chromedriver.storage.googleapis.com/LATEST_RELEASE_%s", _version);
			if ((versionToDownload = streamToString(url)) == null) {
				versionToDownload = getLatestChromeDriverVersion();
				log.info("unable to get version '{}'", version);
			}
		}
		assert versionToDownload != null;

		File file = checkFileExists(versionToDownload);
		if (file != null) {
			log.debug("file '{}' already exists", file);
			return file;
		}
		File execArchiveFile = downloadExecutable(versionToDownload);
		return unzip(execArchiveFile);
	}

	private static String getMajorVersion(String version) {
		int index = version.indexOf('.');
		if (index > -1) {
			version = version.substring(0, index);
		}
		return version;
	}

	private static String getLatestChromeDriverVersion() {
		String version = streamToString("https://chromedriver.storage.googleapis.com/LATEST_RELEASE");
		log.debug("getting the latest version '{}'", version);
		return version;
	}

	private static int getInt(String str) {
		try {
			return Integer.parseInt(str);
		} catch (NumberFormatException ex) {
			log.error("unknown chrome version => {}", str);
		}
		return -1;
	}

	private static File checkFileExists(String version) {
		String suffix = getExecSuffix();
		if ("64".equalsIgnoreCase(PLATFORM.getArchitecture())) {
			File _64 = Paths.get(DOWNLOAD_FOLDER, version, PLATFORM.getName() + "64" + suffix).toFile();
			if (_64.exists()) {
				return _64;
			} else {
				File _32 = Paths.get(DOWNLOAD_FOLDER, version, PLATFORM.getName() + "32" + suffix).toFile();
				if (_32.exists()) {
					return _32;
				}
			}
		} else {
			File _32 = Paths.get(DOWNLOAD_FOLDER, version, PLATFORM.getName() + "32" + suffix).toFile();
			if (_32.exists()) {
				return _32;
			}
		}
		return null;
	}

	private static File downloadExecutable(String version) {
		try {
			String url = String.format("https://chromedriver.storage.googleapis.com/%s/chromedriver_%s%s.zip",
					version, PLATFORM.getName(), PLATFORM.getArchitecture());
			return downloadFile(version, url);
		} catch (IOException ex) {
			try {
				String url = String.format("https://chromedriver.storage.googleapis.com/%s/chromedriver_%s%s.zip",
						version, PLATFORM.getName(), "32");
				return downloadFile(version, url);
			} catch (IOException io) {
				io.printStackTrace();
			}
			return null;
		}
	}

	private static File downloadFile(String version, String url) throws IOException {
		File file = Paths.get(DOWNLOAD_FOLDER, version, url.substring(url.lastIndexOf('_') + 1)).toFile();
		if (file.getParentFile() != null && !file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		URL _url = new URL(url);
		log.debug("downloading from '{}' to '{}'", _url, file);
		try (InputStream stream = _url.openStream()) {
			Files.copy(stream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		return file;
	}

	private static Map<String, List<Integer>> getOlderChromeDriverMapping() {
		if (OLDER_CHROME_VERSION_MAPPING.isEmpty()) {
			String text = streamToString("https://chromedriver.storage.googleapis.com/2.46/notes.txt");
			OLDER_CHROME_VERSION_MAPPING.putAll(StringUtils
					.getMatchedGroups(text, "(?im)ChromeDriver v([0-9\\.]+).+?\\nSupports Chrome v([0-9]+-[0-9]+)")
					.stream()
					.map(line -> {
						String key = line.get(0);
						String[] range = line.get(1).split("\\-");
						List<Integer> values = IntStream
								.range(Integer.parseInt(range[0].trim()), 1 + Integer.parseInt(range[1].trim()))
								.mapToObj(Integer::valueOf)
								.collect(Collectors.toList());
						return Maps.immutableEntry(key, values);
					})
					.collect(toLinkedHashMap(Entry::getKey, Entry::getValue)));
		}
		return OLDER_CHROME_VERSION_MAPPING;
	}

	private static <T, K, U> Collector<T, ?, Map<K, U>> toLinkedHashMap(
			Function<? super T, ? extends K> keyMapper,
			Function<? super T, ? extends U> valueMapper) {
		return Collectors.toMap(keyMapper, valueMapper, (u, v) -> u, LinkedHashMap::new);
	}

	private static File unzip(File zipFile) {
		if (!zipFile.getName().endsWith("zip")) {
			log.error("expected .zip file but found {}", zipFile.getName());
			return null;
		}

		String name = zipFile.getName().replace(".zip", "");
		File file = new File(zipFile.getParentFile(), name);
		Archiver archiver = ArchiverFactory.createArchiver(ArchiveFormat.ZIP);
		if (archiver != null) {
			try {
				archiver.extract(zipFile, zipFile.getParentFile());
			} catch (IOException e) {
				e.printStackTrace();
			}
			zipFile.delete();
		}
		String suffix = getExecSuffix();
		File execFile = new File(zipFile.getParentFile(), name + suffix);
		try {
			com.google.common.io.Files.move(new File(zipFile.getParentFile(), "chromedriver" + suffix), execFile);
			execFile.setExecutable(true);
		} catch (IOException e) {
		}
		return file;
	}

	private static String getExecSuffix() {
		return PLATFORM == Platform.WINDOWS ? ".exe" : "";
	}
}
