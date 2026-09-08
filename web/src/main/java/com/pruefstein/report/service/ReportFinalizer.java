package com.pruefstein.report.service;

import java.time.Instant;

import com.pruefstein.notification.ReportMailDispatcher;
import com.pruefstein.report.domain.Report;
import com.pruefstein.report.domain.ReportStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Gives an open report its verdict. Shared by the submission that fixes one and
 * the deadline job, which can both arrive for the same report.
 *
 * <p>
 * Callers must already be in a transaction.
 */
@ApplicationScoped
public class ReportFinalizer
{
	@Inject
	ReportMailDispatcher mailDispatcher;

	/**
	 * Only an open report gets a verdict, so whichever of the two callers loses
	 * the race changes nothing — no overwritten outcome, and no second mail. A
	 * device that fixes its checks in the last minute of the window is the case
	 * that makes this a race rather than a formality.
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
		mailDispatcher.request(report);
		return true;
	}
}
