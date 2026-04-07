package com.pruefstein.device.domain;

import java.time.Instant;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

/**
 * Tracks the last-seen timestamp and active periodic reporting flow instance
 * for each known device. One row per unique {@code deviceId}.
 */
@Entity
public class Device extends PanacheEntity
{
	@Column(unique = true, nullable = false)
	private String deviceId;

	private String userId;

	private String keycloakUser;

	private Instant lastReportAt;

	@Column(length = 64)
	private String periodicFlowInstanceId;

	public String getDeviceId()
	{
		return deviceId;
	}

	public void setDeviceId(String deviceId)
	{
		this.deviceId = deviceId;
	}

	public String getUserId()
	{
		return userId;
	}

	public void setUserId(String userId)
	{
		this.userId = userId;
	}

	public String getKeycloakUser()
	{
		return keycloakUser;
	}

	public void setKeycloakUser(String keycloakUser)
	{
		this.keycloakUser = keycloakUser;
	}

	public Instant getLastReportAt()
	{
		return lastReportAt;
	}

	public void setLastReportAt(Instant lastReportAt)
	{
		this.lastReportAt = lastReportAt;
	}

	public String getPeriodicFlowInstanceId()
	{
		return periodicFlowInstanceId;
	}

	public void setPeriodicFlowInstanceId(String periodicFlowInstanceId)
	{
		this.periodicFlowInstanceId = periodicFlowInstanceId;
	}
}
