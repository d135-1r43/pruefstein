package com.pruefstein.report.flow;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * Forces a dedicated application instance for the round-trip test.
 *
 * <p>
 * Workflow instances parked by other test classes stay registered for the life
 * of the JVM, and an event is delivered to every instance listening for its
 * type — so without isolation they all wake on this test's event, flood the
 * HTTP callback and trip its circuit breaker before the instance under test is
 * served. See the disabled test in {@link ComplianceFlowRoundTripTest} for the
 * underlying defect.
 */
public class FreshEngineProfile implements QuarkusTestProfile
{
	@Override
	public Map<String, String> getConfigOverrides()
	{
		return Map.of("pruefstein.compliance.remediation-days", "7");
	}
}
