package com.pruefstein.notification;

import io.quarkus.mailer.MailTemplate.MailTemplateInstance;
import io.quarkus.qute.CheckedTemplate;

/**
 * Type-safe bindings for the notification mails in
 * {@code src/main/resources/templates/mails}.
 */
@CheckedTemplate(basePath = "mails")
public class MailTemplates
{
	/**
	 * Sent once a report has a verdict — on upload, or when the flow finalises
	 * it.
	 */
	public static native MailTemplateInstance reportOutcome(ReportMailData report);

	/** Sent shortly before the remediation deadline of a still-open report. */
	public static native MailTemplateInstance deadlineReminder(ReportMailData report);
}
