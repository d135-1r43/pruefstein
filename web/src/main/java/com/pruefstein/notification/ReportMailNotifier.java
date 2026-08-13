package com.pruefstein.notification;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends the outcome mail once the transaction that decided the status has
 * committed — the same {@link TransactionPhase#AFTER_SUCCESS} guarantee the
 * flow events use, so the mail can never describe a state that was rolled back.
 */
@ApplicationScoped
class ReportMailNotifier
{
	private static final Logger LOG = LoggerFactory.getLogger(ReportMailNotifier.class);

	@Inject
	ReportMailService mailService;

	void mailAfterCommit(@Observes(during = TransactionPhase.AFTER_SUCCESS) ReportMailTrigger trigger)
	{
		try
		{
			mailService.sendOutcomeMail(trigger.reportId());
		}
		catch (Exception e)
		{
			LOG.error("Failed to prepare outcome mail for report {}", trigger.reportId(), e);
		}
	}
}
