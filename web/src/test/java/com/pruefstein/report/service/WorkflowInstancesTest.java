package com.pruefstein.report.service;

import java.util.Map;

import com.pruefstein.report.flow.ComplianceReportFlow;
import io.quarkiverse.flow.persistence.jpa.WorkflowInstanceRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.serverlessworkflow.impl.WorkflowInstance;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A parked instance holds an event registration for the life of the JVM, so
 * discarding one has to actually detach it from the engine rather than just
 * tidy the database.
 */
@QuarkusTest
class WorkflowInstancesTest
{
	@Inject
	WorkflowInstances workflowInstances;

	@Inject
	ComplianceReportFlow complianceReportFlow;

	@Inject
	WorkflowInstanceRepository instanceRepository;

	@Test
	void discardingAParkedInstanceDetachesItFromTheEngine()
	{
		// given — an instance parked on its listen task
		WorkflowInstance instance = complianceReportFlow.instance(Map.of("reportId", -1L));
		instance.start();
		String instanceId = instance.id();
		assertTrue(complianceReportFlow.definition().activeInstance(instanceId).isPresent(),
			"instance should be active before it is discarded");

		// when
		QuarkusTransaction.requiringNew()
			.run(() -> workflowInstances.discard(complianceReportFlow, instanceId));

		// then — cancelling unwinds the parked listen task on an engine thread,
		// which is what releases its event registration
		assertTrue(awaitDetached(instanceId),
			"a discarded instance should no longer be held by the engine");
	}

	/** Cancellation propagates through the listen task's future, not inline. */
	private boolean awaitDetached(String instanceId)
	{
		for (int attempt = 0; attempt < 50; attempt++)
		{
			if (complianceReportFlow.definition().activeInstance(instanceId).isEmpty())
			{
				return true;
			}
			try
			{
				Thread.sleep(100);
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
				throw new IllegalStateException(e);
			}
		}
		return false;
	}

	@Test
	void discardingAnUnknownInstanceIsHarmless()
	{
		// given — the shape of a leftover from a previous JVM
		String instanceId = "no-such-instance";
		assertTrue(complianceReportFlow.definition().activeInstance(instanceId).isEmpty());

		// when / then — no row to remove, and nothing blows up
		QuarkusTransaction.requiringNew()
			.run(() -> workflowInstances.discard(complianceReportFlow, instanceId));
		assertEquals(0, instanceRepository.count("instanceId", instanceId));
	}

	@Test
	void discardingNothingIsHarmless()
	{
		QuarkusTransaction.requiringNew().run(() -> {
			workflowInstances.discard(complianceReportFlow, null);
			workflowInstances.discard(complianceReportFlow, "  ");
		});
	}
}
