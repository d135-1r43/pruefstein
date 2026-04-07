package com.pruefstein.report.flow;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.smallrye.reactive.messaging.MutinyEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.jboss.logging.Logger;

/**
 * Emits {@code reporting.cycle.completed} CloudEvents to the Kafka
 * {@code flow-in} topic after the triggering transaction commits.
 */
@ApplicationScoped
public class PeriodicFlowEventEmitter
{
	private static final Logger LOG = Logger.getLogger(PeriodicFlowEventEmitter.class);

	@Channel("flow-events-out")
	MutinyEmitter<String> emitter;

	@Inject
	ObjectMapper objectMapper;

	void afterCommit(@Observes(during = TransactionPhase.AFTER_SUCCESS) PeriodicFlowTrigger trigger)
	{
		try
		{
			String dataJson = objectMapper.writeValueAsString(
				new CycleData(trigger.deviceId(), trigger.reported()));
			String cloudEvent = buildCloudEvent(trigger.flowInstanceId(), dataJson);
			emitter.sendAndAwait(cloudEvent);
			LOG.debugf("Emitted reporting.cycle.completed for device %s (reported=%b)",
				trigger.deviceId(), trigger.reported());
		}
		catch (Exception e)
		{
			LOG.errorf(e, "Failed to emit periodic flow event for device %s", trigger.deviceId());
		}
	}

	private static String buildCloudEvent(String flowInstanceId, String dataJson)
	{
		return """
			{"specversion":"1.0","type":"reporting.cycle.completed","source":"pruefstein-web",\
			"id":"%s","time":"%s","flowinstanceid":"%s",\
			"datacontenttype":"application/json","data":%s}"""
			.formatted(UUID.randomUUID(), Instant.now(), flowInstanceId, dataJson);
	}

	private record CycleData(String deviceId, boolean reported)
	{
	}
}
