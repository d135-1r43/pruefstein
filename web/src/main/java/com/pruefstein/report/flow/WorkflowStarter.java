package com.pruefstein.report.flow;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts a workflow instance after the triggering transaction has committed.
 *
 * <p>
 * Calling {@code WorkflowInstance.start()} inside a {@code @Transactional}
 * method causes a {@code ConcurrentModificationException} because
 * {@code ManagedExecutor} propagates the JTA transaction context into the async
 * workflow JPA writer, making it share the same Hibernate session. Deferring to
 * {@link TransactionPhase#AFTER_SUCCESS} ensures the workflow runs in a clean
 * context.
 */
@ApplicationScoped
class WorkflowStarter
{
	private static final Logger LOG = LoggerFactory.getLogger(WorkflowStarter.class);

	void startAfterCommit(@Observes(during = TransactionPhase.AFTER_SUCCESS) WorkflowStartTrigger trigger)
	{
		try
		{
			trigger.instance().start();
		}
		catch (Exception e)
		{
			LOG.error("Failed to start workflow instance {}", trigger.instance().id(), e);
		}
	}
}
