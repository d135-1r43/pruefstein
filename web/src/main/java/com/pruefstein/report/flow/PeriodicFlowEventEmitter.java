package com.pruefstein.report.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delivers {@code reporting.cycle.completed} CloudEvents to the workflow engine
 * via {@link EngineEvents} once the triggering transaction has committed.
 */
@ApplicationScoped
public class PeriodicFlowEventEmitter
{
	private static final Logger LOG = LoggerFactory.getLogger(PeriodicFlowEventEmitter.class);

	@Inject
	EngineEvents engineEvents;

	@Inject
	ObjectMapper objectMapper;

	void afterCommit(@Observes(during = TransactionPhase.AFTER_SUCCESS) PeriodicFlowTrigger trigger)
	{
		try
		{
			String dataJson = objectMapper.writeValueAsString(
				new CycleData(trigger.deviceId(), trigger.reported()));
			engineEvents.publish("reporting.cycle.completed", trigger.flowInstanceId(), dataJson);
			LOG.debug("Published reporting.cycle.completed for device {} (reported={})",
				trigger.deviceId(), trigger.reported());
		}
		catch (Exception e)
		{
			LOG.error("Failed to publish periodic flow event for device {}", trigger.deviceId(), e);
		}
	}

	// Serialized by hand rather than by the REST layer, so nothing registers
	// it for reflection in the native image on its own.
	@RegisterForReflection
	private record CycleData(String deviceId, boolean reported)
	{
	}
}
