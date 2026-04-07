package com.pruefstein.device.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.pruefstein.device.domain.Device;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DeviceRepository implements PanacheRepository<Device>
{
	public Optional<Device> findByDeviceId(String deviceId)
	{
		return find("deviceId", deviceId).firstResultOptional();
	}

	/**
	 * Returns devices whose last report is older than {@code cutoff} and that
	 * have an active periodic flow instance — i.e. devices that are overdue.
	 */
	public List<Device> findOverdue(Instant cutoff)
	{
		return list("lastReportAt < ?1 and periodicFlowInstanceId is not null", cutoff);
	}
}
