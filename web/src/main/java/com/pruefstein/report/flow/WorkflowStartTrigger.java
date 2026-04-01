package com.pruefstein.report.flow;

import io.serverlessworkflow.impl.WorkflowInstance;

/**
 * CDI event fired inside a transaction to request that a workflow instance be
 * started. Observed after commit (AFTER_SUCCESS) so the workflow JPA
 * persistence runs in a fresh transaction and does not share the Hibernate
 * session with the triggering transaction.
 */
public record WorkflowStartTrigger(WorkflowInstance instance)
{
}
