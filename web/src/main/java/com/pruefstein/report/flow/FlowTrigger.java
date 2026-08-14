package com.pruefstein.report.flow;

/**
 * CDI event fired inside a transaction; observed after commit to publish the
 * compliance outcome into the workflow engine.
 */
public record FlowTrigger(long reportId, String flowInstanceId, boolean allPassed)
{
}
