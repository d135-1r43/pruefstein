package com.pruefstein.report.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.pruefstein.report.domain.Report;
import com.pruefstein.report.domain.ReportStatus;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
class ReportRepositoryTest
{

	@Inject
	ReportRepository reportRepository;

	@Test
	void findOpenByDeviceAndUserReturnsOpenReport()
	{
		Report report = openReport("repo-device", "repo-user");
		reportRepository.persist(report);

		Optional<Report> found = reportRepository.findOpenByDeviceAndUser("repo-device", "repo-user");
		assertTrue(found.isPresent());
		assertEquals("repo-device", found.get().getDeviceId());
	}

	@Test
	void findOpenByDeviceAndUserReturnsEmptyForNonOpenReport()
	{
		Report report = openReport("closed-device", "closed-user");
		report.setStatus(ReportStatus.COMPLIANT);
		reportRepository.persist(report);

		Optional<Report> found = reportRepository.findOpenByDeviceAndUser("closed-device", "closed-user");
		assertFalse(found.isPresent());
	}

	@Test
	void findOpenByDeviceAndUserReturnsEmptyForWrongDevice()
	{
		Report report = openReport("other-device", "some-user");
		reportRepository.persist(report);

		Optional<Report> found = reportRepository.findOpenByDeviceAndUser("wrong-device", "some-user");
		assertFalse(found.isPresent());
	}

	@Test
	void findExpiredOpenReturnsExpiredReports()
	{
		Report report = openReport("expired-device", "expired-user");
		report.setDeadline(Instant.now().minusSeconds(60));
		report.setFlowInstanceId("flow-123");
		reportRepository.persist(report);

		List<Report> expired = reportRepository.findExpiredOpen(Instant.now());
		assertTrue(expired.stream().anyMatch(r -> "expired-device".equals(r.getDeviceId())));
	}

	@Test
	void findExpiredOpenIgnoresReportsWithoutFlowInstanceId()
	{
		Report report = openReport("no-flow-device", "no-flow-user");
		report.setDeadline(Instant.now().minusSeconds(60));
		reportRepository.persist(report);

		List<Report> expired = reportRepository.findExpiredOpen(Instant.now());
		assertFalse(expired.stream().anyMatch(r -> "no-flow-device".equals(r.getDeviceId())));
	}

	@Test
	void findExpiredOpenIgnoresNonOpenReports()
	{
		Report report = openReport("compliant-device", "x");
		report.setStatus(ReportStatus.COMPLIANT);
		report.setDeadline(Instant.now().minusSeconds(60));
		report.setFlowInstanceId("flow-456");
		reportRepository.persist(report);

		List<Report> expired = reportRepository.findExpiredOpen(Instant.now());
		assertFalse(expired.stream().anyMatch(r -> "compliant-device".equals(r.getDeviceId())));
	}

	@Test
	void findExpiredOpenIgnoresFutureDeadlines()
	{
		Report report = openReport("future-device", "future-user");
		report.setDeadline(Instant.now().plusSeconds(3600));
		report.setFlowInstanceId("flow-789");
		reportRepository.persist(report);

		List<Report> expired = reportRepository.findExpiredOpen(Instant.now());
		assertFalse(expired.stream().anyMatch(r -> "future-device".equals(r.getDeviceId())));
	}

	// ── helpers
	// ───────────────────────────────────────────────────────────────

	private Report openReport(String deviceId, String userId)
	{
		Report report = new Report();
		report.setDeviceId(deviceId);
		report.setUserId(userId);
		report.setCheckedAt(Instant.now());
		report.setStatus(ReportStatus.OPEN);
		return report;
	}
}
