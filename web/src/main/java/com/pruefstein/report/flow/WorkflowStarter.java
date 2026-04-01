package com.pruefstein.report.flow;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;

import org.jboss.logging.Logger;

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
	private static final Logger LOG = Logger.getLogger(WorkflowStarter.class);

	void startAfterCommit(@Observes(during = TransactionPhase.AFTER_SUCCESS) WorkflowStartTrigger trigger)
	{
		try
		{
			trigger.instance().start();
		}
		catch (Exception e)
		{
			LOG.errorf(e, "Failed to start workflow instance %s", trigger.instance().id());
		}
	}
}
