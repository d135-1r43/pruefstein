package com.pruefstein.report.flow;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.pruefstein.device.domain.Device;
import com.pruefstein.device.repository.DeviceRepository;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fires every hour to detect devices that have not submitted a report within
 * the configured reporting interval. Emits a {@link PeriodicFlowTrigger} with
 * {@code reported=false} for each overdue device; after the transaction commits
 * {@link PeriodicFlowEventEmitter} hands it to the workflow engine, which
 * resumes the waiting flow instance and triggers creation of a {@code MISSING}
 * report entry.
 */
@ApplicationScoped
public class PeriodicDeadlineJob
{
	private static final Logger LOG = LoggerFactory.getLogger(PeriodicDeadlineJob.class);

	@Inject
	DeviceRepository deviceRepository;

	@Inject
	Event<PeriodicFlowTrigger> periodicFlowTrigger;

	@ConfigProperty(name = "pruefstein.compliance.reporting-interval-days", defaultValue = "7")
	int reportingIntervalDays;

	@Scheduled(every = "1h")
	@Transactional
	void checkOverdueDevices()
	{
		Instant cutoff = Instant.now().minus(reportingIntervalDays, ChronoUnit.DAYS);
		List<Device> overdue = deviceRepository.findOverdue(cutoff);
		if (overdue.isEmpty())
		{
			return;
		}
		LOG.info("Marking {} overdue device(s) as missing", overdue.size());
		for (Device device : overdue)
		{
			periodicFlowTrigger.fire(
				new PeriodicFlowTrigger(device.getDeviceId(), device.getPeriodicFlowInstanceId(), false));
		}
	}
}
