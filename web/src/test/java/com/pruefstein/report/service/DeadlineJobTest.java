package com.pruefstein.report.service;

import java.time.Instant;

import com.pruefstein.report.domain.Report;
import com.pruefstein.report.domain.ReportStatus;
import com.pruefstein.report.repository.ReportRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The deadline is what ends an open report, and it is the only thing left that
 * can: a device that fixes its checks closes the report by reporting again, and
 * one that does not runs out of time here.
 */
@QuarkusTest
class DeadlineJobTest
{
	private static final String DEVICE = "deadline-job-device";

	@Inject
	DeadlineJob deadlineJob;

	@Inject
	ReportFinalizer finalizer;

	@Inject
	ReportRepository reportRepository;

	@AfterEach
	void tearDown()
	{
		QuarkusTransaction.requiringNew().run(() -> reportRepository.delete("deviceId", DEVICE));
	}

	/**
	 * The results are not consulted, and there is nothing they could say: a
	 * report is open because the run it holds failed, and a run that fixed
	 * those failures would have closed it on arrival.
	 */
	@Test
	void anExpiredReportClosesNonCompliant()
	{
		// given
		long reportId = openReport(Instant.now().minusSeconds(60));

		// when
		deadlineJob.closeExpiredReports();

		// then
		assertEquals(ReportStatus.NON_COMPLIANT, statusOf(reportId));
		assertNotNull(finalizedAtOf(reportId), "a closed report should carry when it was closed");
	}

	@Test
	void aReportInsideItsDeadlineIsLeftAlone()
	{
		// given — the whole window is still there to fix things in
		long reportId = openReport(Instant.now().plusSeconds(3600));

		// when
		deadlineJob.closeExpiredReports();

		// then
		assertEquals(ReportStatus.OPEN, statusOf(reportId));
	}

	/**
	 * A device that fixes everything in the last minute of its window races the
	 * hourly job. Whichever loses must change nothing — no overwritten verdict,
	 * and no second mail about the same report.
	 */
	@Test
	void aRunThatArrivesAfterTheDeadlineCannotOverturnTheVerdict()
	{
		// given — the job already closed it
		long reportId = openReport(Instant.now().minusSeconds(60));
		deadlineJob.closeExpiredReports();
		Instant decidedAt = finalizedAtOf(reportId);

		// when — a clean run lands a moment too late
		boolean decided = QuarkusTransaction.requiringNew()
			.call(() -> finalizer.finalizeReport(reportRepository.findById(reportId), true));

		// then
		assertFalse(decided, "the report was already decided");
		assertEquals(ReportStatus.NON_COMPLIANT, statusOf(reportId));
		assertEquals(decidedAt, finalizedAtOf(reportId));
	}

	private long openReport(Instant deadline)
	{
		return QuarkusTransaction.requiringNew().call(() -> {
			Report report = new Report();
			report.setDeviceId(DEVICE);
			report.setUserId("deadline-user");
			report.setKeycloakUser("deadline-user");
			report.setCheckedAt(Instant.now());
			report.setStatus(ReportStatus.OPEN);
			report.setDeadline(deadline);
			reportRepository.persist(report);
			return report.id;
		});
	}

	private ReportStatus statusOf(long reportId)
	{
		return QuarkusTransaction.requiringNew()
			.call(() -> reportRepository.findById(reportId).getStatus());
	}

	private Instant finalizedAtOf(long reportId)
	{
		return QuarkusTransaction.requiringNew()
			.call(() -> reportRepository.findById(reportId).getFinalizedAt());
	}
}
