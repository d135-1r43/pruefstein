package com.pruefstein.agent.client;

import java.time.Instant;

/**
 * @param deadline
 *            when the report stops being fixable, or {@code null} if the server
 *            decided it on arrival — a clean run, with nothing to remediate.
 */
public record ReportResponse(String reportUrl, Instant deadline)
{
}
