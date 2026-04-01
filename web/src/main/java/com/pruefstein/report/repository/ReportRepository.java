package com.pruefstein.report.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.pruefstein.report.domain.Report;
import com.pruefstein.report.domain.ReportStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ReportRepository implements PanacheRepository<Report>
{
	public Optional<Report> findOpenByDeviceAndUser(String deviceId, String userId)
	{
		return find("deviceId = ?1 and userId = ?2 and status = ?3",
			deviceId, userId, ReportStatus.OPEN).firstResultOptional();
	}

	public List<Report> findExpiredOpen(Instant now)
	{
		return list("status = ?1 and deadline < ?2 and flowInstanceId is not null",
			ReportStatus.OPEN, now);
	}
}
