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
		// given
		reportRepository.persist(openReport("repo-device", "repo-user"));

		// when
		Optional<Report> found = reportRepository.findOpenByDeviceAndUser("repo-device", "repo-user");

		// then
		assertTrue(found.isPresent());
		assertEquals("repo-device", found.get().getDeviceId());
	}

	@Test
	void findOpenByDeviceAndUserReturnsEmptyForNonOpenReport()
	{
		// given
		Report report = openReport("closed-device", "closed-user");
		report.setStatus(ReportStatus.COMPLIANT);
		reportRepository.persist(report);

		// when
		Optional<Report> found = reportRepository.findOpenByDeviceAndUser("closed-device", "closed-user");

		// then
		assertFalse(found.isPresent());
	}

	@Test
	void findOpenByDeviceAndUserReturnsEmptyForWrongDevice()
	{
		// given
		reportRepository.persist(openReport("other-device", "some-user"));

		// when
		Optional<Report> found = reportRepository.findOpenByDeviceAndUser("wrong-device", "some-user");

		// then
		assertFalse(found.isPresent());
	}

	@Test
	void findExpiredOpenReturnsExpiredReports()
	{
		// given
		Report report = openReport("expired-device", "expired-user");
		report.setDeadline(Instant.now().minusSeconds(60));
		report.setFlowInstanceId("flow-123");
		reportRepository.persist(report);

		// when
		List<Report> expired = reportRepository.findExpiredOpen(Instant.now());

		// then
		assertTrue(expired.stream().anyMatch(r -> "expired-device".equals(r.getDeviceId())));
	}

	@Test
	void findExpiredOpenIncludesReportsWithoutFlowInstanceId()
	{
		// given — nothing is listening for this report, which is exactly why
		// the
		// deadline job has to be the one that closes it
		Report report = openReport("no-flow-device", "no-flow-user");
		report.setDeadline(Instant.now().minusSeconds(60));
		reportRepository.persist(report);

		// when
		List<Report> expired = reportRepository.findExpiredOpen(Instant.now());

		// then
		assertTrue(expired.stream().anyMatch(r -> "no-flow-device".equals(r.getDeviceId())));
	}

	@Test
	void findExpiredOpenIgnoresNonOpenReports()
	{
		// given
		Report report = openReport("compliant-device", "x");
		report.setStatus(ReportStatus.COMPLIANT);
		report.setDeadline(Instant.now().minusSeconds(60));
		report.setFlowInstanceId("flow-456");
		reportRepository.persist(report);

		// when
		List<Report> expired = reportRepository.findExpiredOpen(Instant.now());

		// then
		assertFalse(expired.stream().anyMatch(r -> "compliant-device".equals(r.getDeviceId())));
	}

	@Test
	void findExpiredOpenIgnoresFutureDeadlines()
	{
		// given
		Report report = openReport("future-device", "future-user");
		report.setDeadline(Instant.now().plusSeconds(3600));
		report.setFlowInstanceId("flow-789");
		reportRepository.persist(report);

		// when
		List<Report> expired = reportRepository.findExpiredOpen(Instant.now());

		// then
		assertFalse(expired.stream().anyMatch(r -> "future-device".equals(r.getDeviceId())));
	}

	@Test
	void findDueForReminderReturnsReportsInsideTheWindow()
	{
		// given
		Report report = openReport("reminder-device", "reminder-user");
		report.setDeadline(Instant.now().plusSeconds(36 * 3600));
		reportRepository.persist(report);

		// when
		List<Report> due = reportRepository.findDueForReminder(
			Instant.now(), Instant.now().plusSeconds(48 * 3600));

		// then
		assertTrue(due.stream().anyMatch(r -> "reminder-device".equals(r.getDeviceId())));
	}

	@Test
	void findDueForReminderIgnoresAlreadyRemindedReports()
	{
		// given
		Report report = openReport("reminded-device", "reminded-user");
		report.setDeadline(Instant.now().plusSeconds(36 * 3600));
		report.setReminderSentAt(Instant.now().minusSeconds(3600));
		reportRepository.persist(report);

		// when
		List<Report> due = reportRepository.findDueForReminder(
			Instant.now(), Instant.now().plusSeconds(48 * 3600));

		// then
		assertFalse(due.stream().anyMatch(r -> "reminded-device".equals(r.getDeviceId())));
	}

	@Test
	void findDueForReminderIgnoresDeadlinesBeyondTheWindow()
	{
		// given
		Report report = openReport("far-device", "far-user");
		report.setDeadline(Instant.now().plusSeconds(6 * 24 * 3600));
		reportRepository.persist(report);

		// when
		List<Report> due = reportRepository.findDueForReminder(
			Instant.now(), Instant.now().plusSeconds(48 * 3600));

		// then
		assertFalse(due.stream().anyMatch(r -> "far-device".equals(r.getDeviceId())));
	}

	@Test
	void findDueForReminderIgnoresExpiredDeadlines()
	{
		// given
		Report report = openReport("past-device", "past-user");
		report.setDeadline(Instant.now().minusSeconds(60));
		reportRepository.persist(report);

		// when
		List<Report> due = reportRepository.findDueForReminder(
			Instant.now(), Instant.now().plusSeconds(48 * 3600));

		// then
		assertFalse(due.stream().anyMatch(r -> "past-device".equals(r.getDeviceId())));
	}

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
