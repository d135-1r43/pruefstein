package com.pruefstein.report.service;

import java.time.Instant;

import com.pruefstein.notification.ReportMailTrigger;
import com.pruefstein.report.domain.Report;
import com.pruefstein.report.domain.ReportStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

/**
 * Gives a report its verdict. Shared by the workflow callback and the deadline
 * job, which can both arrive for the same report.
 *
 * <p>
 * Callers must already be in a transaction.
 */
@ApplicationScoped
public class ReportFinalizer
{
	@Inject
	Event<ReportMailTrigger> mailTrigger;

	/**
	 * Only an open report gets a verdict, so whichever of the two callers loses
	 * the race changes nothing — no overwritten outcome, and no second mail.
	 *
	 * @return whether this call was the one that decided the report
	 */
	public boolean finalizeReport(Report report, boolean allPassed)
	{
		if (report.getStatus() != ReportStatus.OPEN)
		{
			return false;
		}
		report.setStatus(allPassed ? ReportStatus.COMPLIANT : ReportStatus.NON_COMPLIANT);
		report.setFinalizedAt(Instant.now());
		mailTrigger.fire(new ReportMailTrigger(report.id));
		return true;
	}
}
