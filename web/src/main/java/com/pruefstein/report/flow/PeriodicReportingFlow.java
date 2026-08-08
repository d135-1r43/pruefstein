package com.pruefstein.report.flow;

import io.quarkiverse.flow.Flow;
import io.quarkiverse.flow.dsl.FlowWorkflowBuilder;
import io.serverlessworkflow.api.types.Workflow;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import static io.quarkiverse.flow.dsl.FlowDSL.*;

/**
 * Periodic device reporting workflow.
 *
 * <p>
 * One instance runs per device at all times. It waits for a
 * {@code reporting.cycle.completed} CloudEvent, then POSTs the event data to
 * the internal cycle endpoint.
 *
 * <p>
 * The event is emitted by two sources:
 * <ul>
 * <li>{@link PeriodicFlowEventEmitter} — when the device submits a report on
 * time ({@code reported=true}).</li>
 * <li>{@link PeriodicDeadlineJob} — when the device is overdue
 * ({@code reported=false}).</li>
 * </ul>
 *
 * <p>
 * The cycle endpoint creates a {@code MISSING} report entry for
 * {@code reported=false} and always restarts a fresh flow instance for the next
 * cycle.
 */
@ApplicationScoped
public class PeriodicReportingFlow extends Flow
{
	@ConfigProperty(name = "pruefstein.internal.base-url", defaultValue = "http://localhost:8080")
	String internalBaseUrl;

	@Override
	public Workflow descriptor()
	{
		String cycleUrl = internalBaseUrl + "/internal/reporting/cycle";

		return FlowWorkflowBuilder.workflow("periodic-reporting")
			.tasks(
				listen("waitForCycle", toOne("reporting.cycle.completed")),
				call("handleCycle", http().POST().endpoint(cycleUrl).body(".")))
			.build();
	}
}
