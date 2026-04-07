package com.pruefstein.report.flow;

/**
 * CDI event fired inside a transaction to notify the active
 * {@code PeriodicReportingFlow} instance for a device that a cycle has
 * completed. Observed after commit by {@link PeriodicFlowEventEmitter}.
 *
 * @param reported
 *            {@code true} if the device submitted a report on time;
 *            {@code false} if the deadline job fired because the device was
 *            overdue.
 */
public record PeriodicFlowTrigger(String deviceId, String flowInstanceId, boolean reported)
{
}
