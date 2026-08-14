package com.pruefstein.report.flow;

import java.time.Instant;

import com.pruefstein.compliance.domain.ComplianceGroup;
import com.pruefstein.compliance.domain.ComplianceResult;
import com.pruefstein.compliance.domain.ExpressionCheck;
import com.pruefstein.compliance.repository.ComplianceGroupRepository;
import com.pruefstein.compliance.repository.ComplianceItemRepository;
import com.pruefstein.compliance.repository.ComplianceResultRepository;
import com.pruefstein.report.domain.Report;
import com.pruefstein.report.domain.ReportStatus;
import com.pruefstein.report.repository.ReportRepository;
import com.pruefstein.report.service.ReportFinalizer;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The deadline job is the last thing that can decide a report, so it has to do
 * so without help from the workflow instance and with the outcome the results
 * on record actually support.
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

	@Inject
	ComplianceResultRepository resultRepository;

	@Inject
	ComplianceItemRepository itemRepository;

	@Inject
	ComplianceGroupRepository groupRepository;

	private Long itemId;
	private Long groupId;

	@BeforeEach
	void setUp()
	{
		Long[] ids = new Long[2];
		QuarkusTransaction.requiringNew().run(() -> {
			ComplianceGroup group = new ComplianceGroup();
			group.setName("Deadline Group");
			groupRepository.persist(group);
			ids[0] = group.id;

			ExpressionCheck item = new ExpressionCheck();
			item.setName("Deadline Check");
			item.setGroup(group);
			itemRepository.persist(item);
			ids[1] = item.id;
		});
		groupId = ids[0];
		itemId = ids[1];
	}

	@AfterEach
	void tearDown()
	{
		QuarkusTransaction.requiringNew().run(() -> {
			resultRepository.delete("report.deviceId", DEVICE);
			reportRepository.delete("deviceId", DEVICE);
			itemRepository.deleteById(itemId);
			groupRepository.deleteById(groupId);
		});
	}

	@Test
	void anExpiredReportWhoseChecksAllPassClosesCompliant()
	{
		// given — the outcome event was lost, but the results on record are
		// clean
		long reportId = expiredReport("flow-1", true);

		// when
		deadlineJob.closeExpiredReports();

		// then — the blanket NON_COMPLIANT would have been the wrong verdict
		assertEquals(ReportStatus.COMPLIANT, statusOf(reportId));
		assertNotNull(finalizedAtOf(reportId));
	}

	@Test
	void anExpiredReportWithAFailingCheckClosesNonCompliant()
	{
		// given
		long reportId = expiredReport("flow-2", false);

		// when
		deadlineJob.closeExpiredReports();

		// then
		assertEquals(ReportStatus.NON_COMPLIANT, statusOf(reportId));
	}

	@Test
	void anExpiredReportClosesEvenWithoutAWorkflowInstance()
	{
		// given — nothing is listening, which is the state after a restart
		long reportId = expiredReport(null, false);

		// when
		deadlineJob.closeExpiredReports();

		// then
		assertEquals(ReportStatus.NON_COMPLIANT, statusOf(reportId));
	}

	@Test
	void aReportInsideItsDeadlineIsLeftAlone()
	{
		// given
		long reportId = QuarkusTransaction.requiringNew().call(() -> {
			Report report = openReport("flow-3");
			report.setDeadline(Instant.now().plusSeconds(3600));
			reportRepository.persist(report);
			return report.id;
		});

		// when
		deadlineJob.closeExpiredReports();

		// then
		assertEquals(ReportStatus.OPEN, statusOf(reportId));
	}

	@Test
	void aLateWorkflowCallbackCannotOverturnTheDeadlineVerdict()
	{
		// given — the job already closed it as non-compliant
		long reportId = expiredReport("flow-4", false);
		deadlineJob.closeExpiredReports();
		Instant decidedAt = finalizedAtOf(reportId);

		// when — the parked instance finally reports a clean run
		boolean decided = QuarkusTransaction.requiringNew()
			.call(() -> finalizer.finalizeReport(reportRepository.findById(reportId), true));

		// then — first verdict stands, and no second mail goes out
		assertFalse(decided);
		assertEquals(ReportStatus.NON_COMPLIANT, statusOf(reportId));
		assertEquals(decidedAt, finalizedAtOf(reportId));
	}

	/**
	 * An OPEN report past its deadline, with one result of the given outcome.
	 */
	private long expiredReport(String flowInstanceId, boolean passed)
	{
		return QuarkusTransaction.requiringNew().call(() -> {
			Report report = openReport(flowInstanceId);
			report.setDeadline(Instant.now().minusSeconds(60));
			reportRepository.persist(report);

			ComplianceResult result = new ComplianceResult();
			result.setReport(report);
			result.setItem(itemRepository.findById(itemId));
			result.setPassed(passed);
			resultRepository.persist(result);
			return report.id;
		});
	}

	private Report openReport(String flowInstanceId)
	{
		Report report = new Report();
		report.setDeviceId(DEVICE);
		report.setUserId("deadline-user");
		report.setKeycloakUser("deadline-user");
		report.setCheckedAt(Instant.now());
		report.setStatus(ReportStatus.OPEN);
		report.setFlowInstanceId(flowInstanceId);
		return report;
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
