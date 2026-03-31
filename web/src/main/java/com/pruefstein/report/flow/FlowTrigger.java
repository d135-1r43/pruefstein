package com.pruefstein.report.flow;

/**
 * CDI event fired inside a transaction; observed after commit to emit the Kafka
 * message.
 */
public record FlowTrigger(long reportId, String flowInstanceId, boolean allPassed)
{
}
