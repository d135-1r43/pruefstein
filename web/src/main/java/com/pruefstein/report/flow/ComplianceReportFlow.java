package com.pruefstein.report.flow;

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.*;

import io.quarkiverse.flow.Flow;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

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

		return FuncWorkflowBuilder.workflow("compliance-report")
			.tasks(
				listen("waitForOutcome", toOne("compliance.outcome.decided")),
				call("finalize", http().POST().endpoint(finalizeUrl).body(".")))
			.build();
	}
}
