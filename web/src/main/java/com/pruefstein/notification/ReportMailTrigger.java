package com.pruefstein.notification;

/**
 * Fired when a report has reached a status worth telling its owner about.
 * Observed by {@link ReportMailNotifier} after the transaction commits.
 */
public record ReportMailTrigger(long reportId)
{
}
