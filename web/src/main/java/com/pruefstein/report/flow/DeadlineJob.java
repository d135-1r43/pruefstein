package com.pruefstein.report.flow;

import java.time.Instant;
import java.util.List;

import com.pruefstein.report.domain.Report;
import com.pruefstein.report.repository.ReportRepository;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fires every hour to close any {@code OPEN} reports whose deadline has passed.
 * Emits a {@link FlowTrigger} with {@code allPassed=false}; after the
 * transaction commits the event is forwarded to Kafka, which resumes the
 * waiting flow instance and finalises the report as {@code NON_COMPLIANT}.
 */
@ApplicationScoped
public class DeadlineJob
{
	private static final Logger LOG = LoggerFactory.getLogger(DeadlineJob.class);

	@Inject
	ReportRepository reportRepository;

	@Inject
	Event<FlowTrigger> flowTrigger;

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
			flowTrigger.fire(new FlowTrigger(report.id, report.getFlowInstanceId(), false));
		}
	}
}
