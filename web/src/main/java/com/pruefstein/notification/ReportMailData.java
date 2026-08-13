package com.pruefstein.notification;

import java.util.List;

/**
 * Everything the mail templates render, flattened to plain values.
 *
 * <p>
 * Templates are rendered lazily when the mail is actually sent, which happens
 * after the surrounding transaction has closed. Passing entities would
 * therefore risk lazy-loading failures — this record is built eagerly while the
 * session is still open.
 */
public record ReportMailData(
	long id,
	String deviceId,
	String name,
	String status,
	String checkedAt,
	String deadline,
	long daysLeft,
	List<Failure> failures,
	String url)
{
	/** One failed check, as shown in the mail's issue list. */
	public record Failure(String name, String group, String description)
	{
	}

	public boolean compliant()
	{
		return "COMPLIANT".equals(status);
	}

	public boolean nonCompliant()
	{
		return "NON_COMPLIANT".equals(status);
	}

	public int failureCount()
	{
		return failures.size();
	}

	/**
	 * "1 check" vs "3 checks" — templates have no pluralisation of their own.
	 */
	public String failureLabel()
	{
		return failures.size() == 1 ? "1 check" : failures.size() + " checks";
	}

	public String dayLabel()
	{
		return daysLeft == 1 ? "1 day" : daysLeft + " days";
	}
}
