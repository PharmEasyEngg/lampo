package com.auito.automationtools.model;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Document("devices")
@Getter
@Setter
@ToString
public class Device {

	@Id
	private String _id;

	private String id;

	@JsonProperty("slave_ip")
	private String slaveIp;

	@JsonProperty("device_info")
	private DeviceInformation deviceInformation;

	@JsonProperty("is_blacklisted")
	private boolean isBlacklisted;

	@JsonProperty("is_free")
	private boolean isFree;

	@JsonProperty("held_by")
	private String heldBy;

	@JsonProperty("allocate_start")
	private Date lastAllocationStart;

	@JsonProperty("allocate_end")
	private Date lastAllocationEnd;

	@JsonProperty("last_session_duration")
	private Long lastSessionDuration;

	@JsonProperty("allocated_for")
	private AllocationStrategy allocatedFor = AllocationStrategy.AUTOMATION;

	@JsonProperty("last_modified_time")
	private Date lastModifiedTime;

	@JsonProperty("last_allocatted_to")
	private AllocatedTo lastAllocatedTo;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((slaveIp == null) ? 0 : slaveIp.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Device other = (Device) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (slaveIp == null) {
			if (other.slaveIp != null)
				return false;
		} else if (!slaveIp.equals(other.slaveIp))
			return false;
		return true;
	}

}
