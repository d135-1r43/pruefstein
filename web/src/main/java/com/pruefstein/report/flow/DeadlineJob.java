package com.pruefstein.report.flow;

import java.time.Instant;
import java.util.List;

import com.pruefstein.compliance.repository.ComplianceResultRepository;
import com.pruefstein.report.domain.Report;
import com.pruefstein.report.repository.ReportRepository;
import com.pruefstein.report.service.ReportFinalizer;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fires every hour to close any {@code OPEN} report whose deadline has passed.
 *
 * <p>
 * The verdict comes from the report's own results rather than a blanket
 * failure: if the device fixed everything but the outcome event never reached
 * its workflow instance, the checks on record all pass and the report closes
 * {@code COMPLIANT}. Closing the report is what this job guarantees — it no
 * longer depends on the workflow instance still listening, which after a
 * restart it is not.
 *
 * <p>
 * The instance parked on the corresponding {@code listen} is left behind. It
 * only answers to its own id, so it is inert once the report is decided.
 */
@ApplicationScoped
public class DeadlineJob
{
	private static final Logger LOG = LoggerFactory.getLogger(DeadlineJob.class);

	@Inject
	ReportRepository reportRepository;

	@Inject
	ComplianceResultRepository resultRepository;

	@Inject
	ReportFinalizer finalizer;

	@Scheduled(every = "1h")
	@Transactional
	void closeExpiredReports()
	{
		List<Report> expired = reportRepository.findExpiredOpen(Instant.now());
		if (expired.isEmpty())
		{
			return;
		}
		LOG.info("Closing {} expired OPEN report(s)", expired.size());
		for (Report report : expired)
		{
			boolean allPassed = resultRepository.count("report = ?1 and passed = false", report) == 0;
			finalizer.finalizeReport(report, allPassed);
			LOG.debug("Report {} closed at its deadline (allPassed={})", (Object)report.id, allPassed);
		}
	}
}
