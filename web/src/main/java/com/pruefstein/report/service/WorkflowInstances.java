package com.pruefstein.report.service;

import java.util.Optional;

import io.quarkiverse.flow.Flow;
import io.quarkiverse.flow.persistence.jpa.WorkflowInstanceRepository;
import io.serverlessworkflow.impl.WorkflowInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Disposes of workflow instances that will never be resumed.
 *
 * <p>
 * Once a deadline job has decided a report itself, the instance parked on the
 * corresponding {@code listen} is dead weight: it holds an event registration
 * for the life of the JVM and a row for longer than that.
 */
@ApplicationScoped
public class WorkflowInstances
{
	private static final Logger LOG = LoggerFactory.getLogger(WorkflowInstances.class);

	@Inject
	WorkflowInstanceRepository instanceRepository;

	/**
	 * Cancels the instance if the engine still holds it, which also tears down
	 * its event registration. An instance the engine no longer knows about is a
	 * leftover from a previous JVM — nothing will ever cancel it, so its row is
	 * removed directly. Never both, so this cannot race the engine's own write.
	 *
	 * <p>
	 * Callers must already be in a transaction.
	 */
	public void discard(Flow flow, String instanceId)
	{
		if (instanceId == null || instanceId.isBlank())
		{
			return;
		}

		Optional<WorkflowInstance> active = flow.definition().activeInstance(instanceId);
		if (active.isPresent())
		{
			active.get().cancel();
			LOG.debug("Cancelled workflow instance {}", instanceId);
			return;
		}

		long removed = instanceRepository.find("instanceId", instanceId).firstResultOptional()
			.map(entity -> {
				instanceRepository.delete(entity);
				return 1L;
			})
			.orElse(0L);
		if (removed > 0)
		{
			LOG.debug("Removed orphaned workflow instance row {}", instanceId);
		}
	}
}
