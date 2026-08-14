package com.pruefstein.report.service;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.quarkiverse.flow.Flow;
import io.quarkiverse.flow.persistence.jpa.WorkflowInstanceEntity;
import io.quarkiverse.flow.persistence.jpa.WorkflowInstanceRepository;
import io.quarkus.runtime.StartupEvent;
import io.serverlessworkflow.impl.WorkflowInstance;
import io.serverlessworkflow.impl.WorkflowStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
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

	/** Everything the engine has not finished with, one way or another. */
	private static final Set<WorkflowStatus> UNFINISHED = EnumSet.of(
		WorkflowStatus.PENDING, WorkflowStatus.RUNNING,
		WorkflowStatus.WAITING, WorkflowStatus.SUSPENDED);

	@Inject
	WorkflowInstanceRepository instanceRepository;

	@Inject
	Instance<Flow> flows;

	/**
	 * Instances left unfinished by an earlier process are swept once, at
	 * startup. The engine does not re-establish them, so from this process on
	 * nothing can resume them, cancel them or ever look at them again.
	 */
	@Transactional
	void sweepOnStartup(@Observes StartupEvent event)
	{
		long removed = sweep(Instant.now());
		if (removed > 0)
		{
			LOG.info("Swept {} unfinished workflow instance(s) left by an earlier run", removed);
		}
	}

	/**
	 * Removes unfinished instance rows started before {@code startedBefore},
	 * skipping any the engine is currently holding — belt and braces, since an
	 * instance this process created cannot predate its own startup.
	 *
	 * @return how many rows were removed
	 */
	long sweep(Instant startedBefore)
	{
		List<WorkflowInstanceEntity> candidates = instanceRepository.list(
			"startedAt < ?1 and (status is null or status in ?2)", startedBefore, UNFINISHED);

		long removed = 0;
		for (WorkflowInstanceEntity candidate : candidates)
		{
			if (heldByEngine(candidate.getInstanceId()))
			{
				continue;
			}
			instanceRepository.delete(candidate);
			removed++;
		}
		return removed;
	}

	private boolean heldByEngine(String instanceId)
	{
		return flows.stream().anyMatch(flow -> flow.definition().activeInstance(instanceId).isPresent());
	}

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
