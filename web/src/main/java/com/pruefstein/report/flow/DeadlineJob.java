package com.pruefstein.report.flow;

import java.time.Instant;
import java.util.List;

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
 * The verdict is always {@code NON_COMPLIANT}, and it cannot be anything else:
 * a report is only open because the run it holds had failures, and a later run
 * that fixed them would have closed it on arrival. Reaching the deadline still
 * open means the failures are still there.
 */
@ApplicationScoped
public class DeadlineJob
{
	private static final Logger LOG = LoggerFactory.getLogger(DeadlineJob.class);

	@Inject
	ReportRepository reportRepository;

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
			finalizer.finalizeReport(report, false);
			LOG.debug("Report {} closed non-compliant at its deadline", (Object)report.id);
		}
	}
}
