package com.pruefstein.report.flow;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * Gives the round-trip test its own application instance.
 *
 * <p>
 * Other test classes tear down their reports while the workflow instances that
 * reference them are still parked. Those instances later call back into the
 * finalize endpoint, get a 404, and trip the fault-tolerance circuit breaker —
 * which is shared by every instance of the same workflow task, so a legitimate
 * callback is then rejected too. Isolating this test keeps that debris out of
 * its way.
 */
public class FreshEngineProfile implements QuarkusTestProfile
{
	@Override
	public Map<String, String> getConfigOverrides()
	{
		return Map.of("pruefstein.compliance.remediation-days", "7");
	}
}
