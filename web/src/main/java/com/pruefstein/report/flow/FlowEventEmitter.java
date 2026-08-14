package com.pruefstein.report.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delivers {@code compliance.outcome.decided} CloudEvents to the workflow
 * engine via {@link EngineEvents}.
 *
 * <p>
 * Uses {@link TransactionPhase#AFTER_SUCCESS} so the event is only published
 * after the triggering DB transaction has committed, guaranteeing the flow's
 * HTTP callback will see a consistent DB state.
 */
@ApplicationScoped
public class FlowEventEmitter
{
	private static final Logger LOG = LoggerFactory.getLogger(FlowEventEmitter.class);

	@Inject
	EngineEvents engineEvents;

	@Inject
	ObjectMapper objectMapper;

	void afterCommit(@Observes(during = TransactionPhase.AFTER_SUCCESS) FlowTrigger trigger)
	{
		try
		{
			String dataJson = objectMapper.writeValueAsString(
				new OutcomeData(trigger.reportId(), trigger.allPassed()));
			engineEvents.publish("compliance.outcome.decided", trigger.flowInstanceId(), dataJson);
			LOG.debug("Published compliance.outcome.decided for report {} (allPassed={})",
				(Object)trigger.reportId(), trigger.allPassed());
		}
		catch (Exception e)
		{
			LOG.error("Failed to publish flow event for report {}", trigger.reportId(), e);
		}
	}

	private record OutcomeData(long reportId, boolean allPassed)
	{
	}
}
