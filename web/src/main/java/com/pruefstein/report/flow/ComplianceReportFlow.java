package com.pruefstein.report.flow;

import io.quarkiverse.flow.Flow;
import io.quarkiverse.flow.dsl.FlowWorkflowBuilder;
import io.serverlessworkflow.api.types.Workflow;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import static io.quarkiverse.flow.dsl.FlowDSL.*;

/**
 * Compliance report remediation workflow.
 *
 * <p>
 * The flow starts when a device submits a non-compliant report. It waits
 * durably for a {@code compliance.outcome.decided} CloudEvent, which is emitted
 * either by the agent re-running ({@link FlowEventEmitter}) or by the deadline
 * scheduler ({@link DeadlineJob}).
 *
 * <p>
 * On receipt it POSTs the event data {@code {reportId, allPassed}} to the
 * internal finalize endpoint, which sets the final report status.
 */
@ApplicationScoped
public class ComplianceReportFlow extends Flow
{
	@ConfigProperty(name = "pruefstein.internal.base-url", defaultValue = "http://localhost:8080")
	String internalBaseUrl;

	@Override
	public Workflow descriptor()
	{
		String finalizeUrl = internalBaseUrl + "/internal/reports/finalize";

		return FlowWorkflowBuilder.workflow("compliance-report")
			.tasks(
				listen("waitForOutcome",
					toOne(consumed("compliance.outcome.decided")
						.extensionByInstanceId("flowinstanceid"))),
				// A listen task yields an array of consumed event payloads,
				// even
				// for toOne. The endpoint takes a single object, so unwrap it
				// here or every callback comes back 400.
				call("finalize", http().POST().endpoint(finalizeUrl).body("${ .[0] }")))
			.build();
	}
}
