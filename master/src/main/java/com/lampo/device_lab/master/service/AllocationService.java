package com.lampo.device_lab.master.service;

import static com.lampo.device_lab.master.utils.CommonUtilities.getMajorVersion;
import static com.lampo.device_lab.master.utils.CommonUtilities.isBlank;
import static com.lampo.device_lab.master.utils.CommonUtilities.isNotBlank;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.lampo.device_lab.master.exception.DeviceManagerException;
import com.lampo.device_lab.master.grid.CustomGridNodeCapabilityMatcher;
import com.lampo.device_lab.master.grid.DeviceFilter;
import com.lampo.device_lab.master.grid.DeviceMatchRequest;
import com.lampo.device_lab.master.model.AllocatedTo;
import com.lampo.device_lab.master.model.Device;
import com.lampo.device_lab.master.model.DeviceInfo;
import com.lampo.device_lab.master.model.DeviceInformation;
import com.lampo.device_lab.master.model.DeviceRestrictionRequest;
import com.lampo.device_lab.master.model.DeviceStatus;
import com.lampo.device_lab.master.model.Header;
import com.lampo.device_lab.master.model.NodeCapability;
import com.lampo.device_lab.master.model.TeamMapping;
import com.lampo.device_lab.master.repos.IDeviceRepository;
import com.lampo.device_lab.master.repos.ISummaryRepository;
import com.lampo.device_lab.master.repos.ITeamRepository;
import com.lampo.device_lab.master.utils.CommonUtilities;
import com.lampo.device_lab.master.utils.RequestUtils;

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
@Service
public class AllocationService {

	private static final String DEFAULT_TEAM = "common";

	@Autowired
	private IDeviceRepository deviceRepository;

	@Autowired
	private ITeamRepository teamRepository;

	@Autowired
	private RestTemplate restTemplate;

	@Value("${slave.auth_token}")
	private String slaveAuthToken;

	@Autowired
	private ISummaryRepository summaryRepository;

	@Value("${slave.port}")
	private int slavePort;

	private final ReentrantLock lock = new ReentrantLock();

	private String getLogPrefix(HttpServletRequest request) {
		String requestId = getHeader(request, Header.REQUEST_ID);
		String clientIp = RequestUtils.getClientIp(request);
		String user = getHeader(request, Header.USER);

		return String.format("request id '%s' for user '%s' from '%s'", requestId, user, clientIp);
	}

	private HttpEntity<?> getHttpEntity(Object body, String requestId) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(Header.AUTH.toString(), this.slaveAuthToken);
		headers.setAccept(Arrays.asList(MediaType.ALL));
		if (requestId != null && !requestId.trim().isEmpty()) {
			headers.set(Header.REQUEST_ID.toString(), requestId);
		}
		return (body == null) ? new HttpEntity<>(headers) : new HttpEntity<>(body, headers);
	}

	public boolean updateDeviceStatus(@NonNull String slaveIp, @NonNull DeviceStatus status, @NonNull String deviceId,
			HttpServletRequest servletRequest) {

		synchronized (AllocationService.class) {
			String requestId = getHeader(servletRequest, Header.REQUEST_ID);
			try {
				Optional<Device> optional = this.deviceRepository.findById(deviceId, slaveIp);
				if (optional.isPresent()) {
					Device device = optional.get();
					if (status == DeviceStatus.REMOVE) {
						deviceRepository.delete(device);
						return true;
					} else {
						device.setFree(status == DeviceStatus.FREE);

						String teamName = getHeader(servletRequest, Header.TEAM);

						if (isBlank(teamName)) {
							log.error(
									"action '{}' ::: team name is empty when releasing device '{}' on slave '{}' - headers - {}",
									status, device.getId(), device.getSlaveIp(),
									servletRequest == null ? ""
											: Collections.list(servletRequest.getHeaderNames()).stream()
													.map(e -> e + " : " + servletRequest.getHeader(e))
													.collect(Collectors.joining(",")));
						}

						if (status == DeviceStatus.BUSY) {
							device.setLastAllocationStart(new Date());
							device.setLastAllocationEnd(null);
							String jobLink = getHeader(servletRequest, Header.JOB_LINK);

							updateSummary(teamName, -1);

							AllocatedTo allocatedTo = new AllocatedTo(getHeader(servletRequest, Header.REQUESTOR_IP),
									getHeader(servletRequest, Header.USER), jobLink, getTeam(teamName, jobLink));
							device.setLastAllocatedTo(allocatedTo);
						} else {
							device.setLastAllocatedTo(null);
							device.setLastAllocationEnd(new Date());

							if (device.getLastAllocationStart() != null && device.getLastAllocationEnd() != null) {

								long durationInSec = device.getLastAllocationEnd().getTime()
										- device.getLastAllocationStart().getTime();

								updateSummary(teamName, durationInSec);

								device.setLastSessionDuration(durationInSec);
							}
						}
						return (null != this.deviceRepository.save(device));
					}
				}
				return false;
			} finally {
				log.info("request '{}' ::: updating status of device '{}' on slave '{}' to '{}'", requestId, deviceId,
						slaveIp, status);
			}
		}
	}

	private void updateSummary(String team, long duration) {

		log.debug("updating summary info => team: {} and duration: {}", team, duration);
		if (duration == -1) {
			summaryRepository.updateSessionCount(team);
		} else {
			summaryRepository.updateSessionDuration(team, duration);
		}

	}

	private static String extractJobName(@NonNull String link) {
		link = link.trim();
		if (link.endsWith("/")) {
			link = link.substring(0, link.length() - 1);
		}
		List<String> split = CommonUtilities.split(link, "/");
		return split.size() >= 2 ? split.get(split.size() - 2) : null;
	}

	private String getTeam(String team, String jobLink) {
		if (!isBlank(team)) {
			return team;
		}
		if (isBlank(jobLink)) {
			return null;
		}
		return teamRepository.findAll().stream()
				.filter(e -> e.getJobs() != null && e.getJobs().keySet().stream().allMatch(jobLink::contains))
				.map(TeamMapping::getName).findAny().orElse("").toUpperCase();
	}

	public Collection<Device> unallocateAllDevices(HttpServletRequest servletRequest) {
		log.info("{} ::: all devices unallocate request", getLogPrefix(servletRequest));
		return unallocateDevice(null, servletRequest);
	}

	public Collection<Device> unallocateDevice(DeviceRestrictionRequest request, HttpServletRequest servletRequest) {

		log.info("{} ::: device unallocate request '{}'", getLogPrefix(servletRequest), request);

		Collection<Device> devices = (request == null) ? this.deviceRepository.findAll()
				: (Collection<Device>) this.deviceRepository.findAll().stream()
						.filter(device -> (!device.isFree() && getDeviceRestrictionRequestFilter(request, device)))
						.collect(Collectors.toList());

		log.info("{} ::: unallocating devices => {}", getLogPrefix(servletRequest), devices);

		devices.stream().parallel()
				.forEach(e -> unallocateDevice(e.getDeviceInformation().getDeviceId(), e.getSlaveIp(),
						servletRequest));

		return devices;

	}

	private boolean unallocateDevice(@NonNull String deviceId, @NonNull String slaveIp,
			HttpServletRequest servletRequest) {

		Optional<Device> device = this.deviceRepository.findById(deviceId, slaveIp);
		if (!device.isPresent()) {
			return false;
		}
		return updateDeviceStatus(slaveIp, DeviceStatus.FREE, deviceId, servletRequest);
	}

	public void restartDevice(@NonNull DeviceInfo request, HttpServletRequest servletRequest) {

		if (isBlank(request.getSlaveIp())) {
			throw new DeviceManagerException("'slave_ip' is missing", HttpStatus.BAD_REQUEST);
		}
		if (isBlank(request.getDeviceId())) {
			throw new DeviceManagerException("'device_id' is missing", HttpStatus.BAD_REQUEST);
		}

		log.info("restarting device '{}' on slave '{}'", request.getDeviceId(), request.getSlaveIp());

		String url = String.format("http://%s:%s/device/restart_device?device_id=%s", request.getSlaveIp(), slavePort,
				request.getDeviceId());

		try {
			restTemplate.exchange(url, HttpMethod.POST, getHttpEntity(null, getRequestId(servletRequest)), Void.class)
					.getBody();
		} catch (ResourceAccessException ex) {
			throw getDeviceException(ex.getMessage());
		}
	}

	/**
	 * Restarting all devices on the given slave
	 * 
	 * @param slaveIp        {@link String}
	 * @param servletRequest {@link HttpServletRequest}
	 */
	public void restartAllDevices(String slaveIp, HttpServletRequest servletRequest) {

		Collection<String> allSlaveIps = deviceRepository.findAll().stream().map(Device::getSlaveIp).distinct()
				.collect(Collectors.toList());

		if (!isBlank(slaveIp) && !allSlaveIps.contains(slaveIp)) {
			throw new DeviceManagerException(String.format("unknown slave ip '%s'", slaveIp), HttpStatus.BAD_REQUEST);
		}

		Collection<String> slaveIps = isBlank(slaveIp) ? allSlaveIps : Arrays.asList(slaveIp);

		log.info("restarting all devices on slaves '{}'", slaveIps);

		slaveIps.stream().parallel().forEach(ip -> {
			String url = String.format("http://%s:%s/device/restart_all_devices", ip, slavePort);
			try {
				restTemplate
						.exchange(url, HttpMethod.POST, getHttpEntity(null, getRequestId(servletRequest)), Void.class)
						.getBody();
			} catch (ResourceAccessException ex) {
				throw getDeviceException(ex.getMessage());
			}
		});
	}

	public void restartSTF(DeviceInfo request, HttpServletRequest servletRequest) {
		String url = String.format("http://%s:%s/device/restart_stf", request.getSlaveIp(), slavePort);
		try {
			restTemplate.exchange(url, HttpMethod.POST, getHttpEntity(null, getRequestId(servletRequest)), Void.class)
					.getBody();
		} catch (ResourceAccessException ex) {
			throw getDeviceException(ex.getMessage());
		}
	}

	private DeviceManagerException getDeviceException(String message) {
		return new DeviceManagerException(message, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	private String getRequestId(HttpServletRequest servletRequest) {
		return getHeader(servletRequest, Header.REQUEST_ID, UUID.randomUUID().toString());
	}

	private String getHeader(HttpServletRequest servletRequest, Header name) {
		return getHeader(servletRequest, name, "");
	}

	private String getHeader(HttpServletRequest servletRequest, Header name, String defaultValue) {
		return (servletRequest == null) ? defaultValue : servletRequest.getHeader(name.toString());
	}

	private boolean getDeviceRestrictionRequestFilter(DeviceRestrictionRequest request, Device device) {

		String slaveIp = request.getSlaveIp();
		List<String> deviceIds = request.getDeviceId();
		String brand = request.getBrand();
		String deviceName = request.getDeviceName();
		String sdkVersion = request.getSdkVersion();

		if (!isBlank(slaveIp) && !slaveIp.equalsIgnoreCase(device.getSlaveIp()))
			return false;

		if (deviceIds != null && deviceIds.contains(device.getId()))
			return true;

		if (!isBlank(brand) && brand.equalsIgnoreCase(device.getDeviceInformation().getManufacturer()))
			return true;

		if (!isBlank(deviceName) && (deviceName.equalsIgnoreCase(device.getDeviceInformation().getModel())
				|| deviceName.equalsIgnoreCase(device.getDeviceInformation().getMarketName())))
			return true;
		if (!isBlank(sdkVersion)
				&& getMajorVersion(sdkVersion).equals(getMajorVersion(device.getDeviceInformation().getSdkVersion()))) {
			return true;
		}
		return false;
	}

	public DeviceInfo getMatchingCapabilityNode(@NonNull DeviceMatchRequest request,
			HttpServletRequest servletRequest) {

		try {
			lock.lock();
			if (request.getNodeCapability() == null) {
				throw new DeviceManagerException("'node_capability' missing in request");
			}
			if (request.getFilter() == null
					|| (isBlank(request.getFilter().getJobLink()) && isBlank(request.getFilter().getTeamName()))) {
				throw new DeviceManagerException("'filter' missing in request");
			}

			String requestId = getRequestId(servletRequest);

			List<DeviceInfo> devices = filterDevice(request, requestId);

			log.info("request '{}' ::: devices matching the criteria '{}' => {}", requestId, request, devices);

			Optional<DeviceInfo> found = Optional.ofNullable(devices.isEmpty() ? null : devices.get(0));

			if (found.isPresent()) {
				log.info("request '{}' ::: picking up device '{}' allocated to '{} team'", requestId, found,
						request.getFilter().getTeamName());
			}
			return found.isPresent() ? found.get() : null;

		} finally {
			lock.unlock();
		}
	}

	private List<DeviceInfo> filterDevice(DeviceMatchRequest request, String requestId) {

		String platform = request.getNodeCapability().getPlatform();
		String team = getTeam(request.getFilter());
		Collection<String> otherTeamDevices = getOtherTeamDevices(request.getFilter(), platform);

		List<DeviceInfo> devices = filter(request, getDevices(team, platform), otherTeamDevices);

		devices.addAll(filter(request, getDevices(DEFAULT_TEAM, platform), otherTeamDevices));

		log.info("request '{}' ::: filtered devices for team '{}' and platform '{}' => {}", requestId, team, platform,
				devices.stream().map(DeviceInfo::getDeviceId).collect(Collectors.toList()));

		TeamMapping mapping = teamRepository.findByName(team);

		if (mapping != null && mapping.getJobs() != null && isNotBlank(request.getFilter().getJobLink())) {

			String jobName = extractJobName(request.getFilter().getJobLink());
			List<String> jobDevices = mapping.getJobs().get(jobName);

			if (jobDevices != null && !jobDevices.isEmpty()) {
				/* pickup devices dedicated to the job */
				log.debug("request '{}' ::: devices for job '{}' identified => {}", requestId, jobName, jobDevices);
				List<DeviceInfo> list = devices.stream().filter(e -> jobDevices.contains(e.getDeviceId()))
						.collect(Collectors.toList());
				log.info("request '{}' ::: devices filtered by job '{}' => {}", requestId, jobName, list);
				return list;
			} else {
				/* restrict using devices allocated to other jobs */
				List<String> allJobDevices = mapping.getJobs().values().stream().flatMap(Collection::stream)
						.collect(Collectors.toList());
				return devices.stream().filter(e -> !allJobDevices.contains(e.getDeviceId()))
						.collect(Collectors.toList());
			}
		}
		return devices;
	}

	private List<DeviceInfo> filter(DeviceMatchRequest request, Collection<String> devices,
			Collection<String> excludeDevices) {

		return deviceRepository
				.findFreeDevices().stream()
				.filter(e -> devices.isEmpty() || devices.stream().anyMatch(f -> e.getId().matches(f)))
				.filter(e -> excludeDevices.isEmpty() || excludeDevices.stream().noneMatch(f -> e.getId().matches(f)))
				.map(this::toNodeCapability)
				.filter(e -> CustomGridNodeCapabilityMatcher.match(e, request.getNodeCapability()))
				.map(e -> {
					DeviceInfo info = new DeviceInfo();
					info.setSlaveIp(e.getIp());
					info.setDeviceId(e.getUdid());
					info.setPlatformVersion(e.getVersion());
					info.setDeviceName(e.getDeviceName());
					return info;
				})
				.collect(Collectors.toList());
	}

	private String getTeam(@NonNull DeviceFilter filter) {
		return getTeam(filter.getTeamName(), filter.getJobLink());
	}

	private Collection<String> getOtherTeamDevices(DeviceFilter filter, String platform) {
		String team = teamRepository.findTeam(filter);
		if (!isBlank(team)) {
			return teamRepository
					.findAll().stream()
					.filter(e -> !(team.equalsIgnoreCase(e.getName()) || DEFAULT_TEAM.equalsIgnoreCase(e.getName())))
					.map(e -> "android".equalsIgnoreCase(platform) ? e.getDevices().getAndroid()
							: e.getDevices().getIos())
					.flatMap(Collection::stream).collect(Collectors.toSet());
		}
		return Collections.emptySet();
	}

	private Collection<String> getDevices(@NonNull String team, String platform) {
		return !isBlank(team) ? teamRepository.getDevicesByTeam(team, platform) : Collections.emptyList();
	}

	private NodeCapability toNodeCapability(@NonNull Device device) {
		DeviceInformation info = device.getDeviceInformation();
		NodeCapability node = new NodeCapability();
		node.setPlatform(info.isAndroid() ? "android" : "ios");
		node.setIsRealDevice(info.isRealDevice());
		node.setUdid(device.getId());
		node.setVersion(info.getSdkVersion());
		node.setIp(device.getSlaveIp());
		String deviceName = String.format("%s %s",
				isBlank(info.getManufacturer()) || "unknown".equalsIgnoreCase(info.getManufacturer())
						? info.getMarketName()
						: info.getManufacturer(),
				info.getModel());
		node.setDeviceName(deviceName);
		log.debug("device '{}' => node '{}'", device.getId(), node);
		return node;
	}

	public String getLogUrl(String requestId, HttpServletRequest servletRequest) {
		return deviceRepository.findSlaves().stream().parallel().map(e -> {
			String url = String.format("http://%s:%s/appium/logs/session-logs/%s.log", e, slavePort, requestId);
			return isPresent(servletRequest, url);
		}).filter(Objects::nonNull).findFirst().orElse(null);
	}

	public String getVideoUrl(String requestId, HttpServletRequest servletRequest) {
		return deviceRepository.findSlaves().stream().parallel().map(e -> {
			String url = String.format("http://%s:%s/appium/logs/session-videos/%s.mp4", e, slavePort, requestId);
			return isPresent(servletRequest, url);
		}).filter(Objects::nonNull).findFirst().orElse(null);
	}

	private String isPresent(HttpServletRequest servletRequest, String url) {
		try {
			ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.HEAD,
					getHttpEntity(null, getRequestId(servletRequest)), String.class);
			return responseEntity.getStatusCode() == HttpStatus.OK ? url : null;
		} catch (Throwable ex) {
			return null;
		}
	}

}
