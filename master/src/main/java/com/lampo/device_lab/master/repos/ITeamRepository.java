package com.lampo.device_lab.master.repos;

import static com.lampo.device_lab.master.utils.CommonUtilities.isBlank;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.lampo.device_lab.master.grid.DeviceFilter;
import com.lampo.device_lab.master.model.TeamMapping;

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
public interface ITeamRepository extends MongoRepository<TeamMapping, String> {

	TeamMapping findByName(String team);

	default Collection<String> getDevicesByTeam(@NonNull String teamName) {
		Optional<TeamMapping> find = findAll().stream()
				.filter(e -> teamName.equalsIgnoreCase(e.getName()) && e.getDevices() != null).findFirst();
		if (!find.isPresent()) {
			return Collections.emptyList();
		}
		Collection<String> devices = new LinkedHashSet<>();
		devices.addAll(find.get().getDevices().getAndroid());
		devices.addAll(find.get().getDevices().getIos());
		return devices;
	}

	default Collection<String> getDevicesByTeam(@NonNull String teamName, @NonNull String platform) {
		Optional<TeamMapping> mapping = findAll().stream().filter(e -> teamName.equalsIgnoreCase(e.getName()))
				.findFirst();
		if (mapping.isPresent()) {
			return "android".equalsIgnoreCase(platform) ? mapping.get().getDevices().getAndroid()
					: mapping.get().getDevices().getIos();
		}
		return Collections.emptyList();
	}

	default String findTeam(@NonNull DeviceFilter filter) {
		Optional<TeamMapping> mapping = findAll().stream()
				.filter(e -> e.getName().equalsIgnoreCase(filter.getTeamName())
						|| (isBlank(filter.getJobLink()) && (e.getJobs() != null && e.getJobs().keySet().stream()
								.anyMatch(f -> filter.getJobLink() != null && filter.getJobLink().contains(f)))))
				.findFirst();
		return mapping.isPresent() ? mapping.get().getName() : null;
	}

	default Collection<String> getDevicesByJobName(@NonNull String jobName, @NonNull String platform) {
		Optional<TeamMapping> mapping = findAll().stream()
				.filter(e -> e.getJobs() != null && e.getJobs().keySet().stream().allMatch(jobName::contains))
				.findFirst();
		if (mapping.isPresent()) {
			return "android".equalsIgnoreCase(platform) ? mapping.get().getDevices().getAndroid()
					: mapping.get().getDevices().getIos();
		}
		return Collections.emptyList();
	}

	default String getTeam(@NonNull String deviceId) {
		return findAll().stream().filter(e -> e.getDevices() != null
				&& ((e.getDevices().getIos() != null && e.getDevices().getIos().contains(deviceId))
						|| (e.getDevices().getAndroid() != null && e.getDevices().getAndroid().contains(deviceId))))
				.map(TeamMapping::getName).findFirst().orElse("common");
	}

	default Collection<String> getTeamDevices(@NonNull String platform) {
		return findAll().stream().filter(e -> !"common".equalsIgnoreCase(e.getName())).flatMap(
				e -> ("android".equalsIgnoreCase(platform) ? e.getDevices().getAndroid() : e.getDevices().getIos())
						.stream())
				.collect(Collectors.toList());
	}

}