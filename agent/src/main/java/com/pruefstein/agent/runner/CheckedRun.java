package com.pruefstein.agent.runner;

import com.pruefstein.agent.client.OsVersionVerdict;
import com.pruefstein.agent.client.ReportPayload;

/**
 * A run that has been checked but not reported.
 *
 * @param report
 *            what a report of it would say
 * @param os
 *            how the server judged its OS, or {@code null} when it could not
 *            say — no OS read, or a server too old to ask
 */
public record CheckedRun(ReportPayload report, OsVersionVerdict os)
{
	/** The failed checks, and the OS when it is red, which counts as one. */
	public long failing()
	{
		long checks = report.results().stream().filter(result -> !result.passed()).count();
		return os != null && os.fails() ? checks + 1 : checks;
	}
}
