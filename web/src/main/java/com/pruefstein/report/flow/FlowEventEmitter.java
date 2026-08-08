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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Emits {@code compliance.outcome.decided} CloudEvents to the Kafka
 * {@code flow-in} topic.
 *
 * <p>
 * Uses {@link TransactionPhase#AFTER_SUCCESS} so the message is only sent after
 * the triggering DB transaction has committed, guaranteeing the flow's HTTP
 * callback will see a consistent DB state.
 */
@ApplicationScoped
public class FlowEventEmitter
{
	private static final Logger LOG = LoggerFactory.getLogger(FlowEventEmitter.class);

	@Channel("flow-events-out")
	MutinyEmitter<String> emitter;

	@Inject
	ObjectMapper objectMapper;

	void afterCommit(@Observes(during = TransactionPhase.AFTER_SUCCESS) FlowTrigger trigger)
	{
		try
		{
			String dataJson = objectMapper.writeValueAsString(
				new OutcomeData(trigger.reportId(), trigger.allPassed()));
			String cloudEvent = buildCloudEvent(trigger.flowInstanceId(), dataJson);
			emitter.sendAndAwait(cloudEvent);
			LOG.debug("Emitted compliance.outcome.decided for report {} (allPassed={})",
				(Object)trigger.reportId(), trigger.allPassed());
		}
		catch (Exception e)
		{
			LOG.error("Failed to emit flow event for report {}", trigger.reportId(), e);
		}
	}

	private static String buildCloudEvent(String flowInstanceId, String dataJson)
	{
		return """
			{"specversion":"1.0","type":"compliance.outcome.decided","source":"pruefstein-web",\
			"id":"%s","time":"%s","flowinstanceid":"%s",\
			"datacontenttype":"application/json","data":%s}"""
			.formatted(UUID.randomUUID(), Instant.now(), flowInstanceId, dataJson);
	}

	private record OutcomeData(long reportId, boolean allPassed)
	{
	}
}
