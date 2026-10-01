package com.pruefstein.agent.client;

/**
 * How the server judged the machine's OS, for listing beside the checks.
 *
 * @param verdict
 *            {@code PASS}, {@code HINT}, {@code FAIL}, or {@code UNKNOWN} when
 *            the server had nothing to judge it by
 * @param name
 *            what to call it beside the checks, e.g. {@code macOS up to date}
 * @param text
 *            how it stands and what to do about it
 */
public record OsVersionVerdict(String verdict, String name, String text)
{
	/** A red OS fails the run like a failed check. */
	public boolean fails()
	{
		return "FAIL".equals(verdict);
	}

	/** Worth printing at all: a PASS, a HINT or a FAIL. */
	public boolean judged()
	{
		return "PASS".equals(verdict) || "HINT".equals(verdict) || fails();
	}
}
