package com.pruefstein.report.flow;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.pruefstein.notification.ReportMailService;
import com.pruefstein.report.domain.Report;
import com.pruefstein.report.repository.ReportRepository;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fires every hour to nudge users whose remediation deadline is close. Runs
 * alongside {@link DeadlineJob}, which handles the deadlines that have already
 * passed.
 *
 * <p>
 * {@code reminderSentAt} is stamped whether or not the SMTP handover succeeds —
 * a broken mail server must not turn an hourly job into an hourly mail storm
 * once it recovers.
 */
@ApplicationScoped
public class DeadlineReminderJob
{
	private static final Logger LOG = LoggerFactory.getLogger(DeadlineReminderJob.class);

	@Inject
	ReportRepository reportRepository;

	@Inject
	ReportMailService mailService;

	@ConfigProperty(name = "pruefstein.compliance.reminder-days-before", defaultValue = "2")
	int reminderDaysBefore;

	@Scheduled(every = "1h")
	@Transactional
	void remindBeforeDeadline()
	{
		Instant now = Instant.now();
		List<Report> due = reportRepository.findDueForReminder(
			now, now.plus(reminderDaysBefore, ChronoUnit.DAYS));
		if (due.isEmpty())
		{
			return;
		}
		LOG.info("Reminding about {} report(s) approaching their deadline", due.size());
		for (Report report : due)
		{
			mailService.sendDeadlineReminder(report);
			report.setReminderSentAt(now);
		}
	}
}
