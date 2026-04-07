package com.pruefstein.report.api;

import java.time.Instant;
import java.util.Map;

import com.pruefstein.device.domain.Device;
import com.pruefstein.device.repository.DeviceRepository;
import com.pruefstein.report.domain.Report;
import com.pruefstein.report.domain.ReportStatus;
import com.pruefstein.report.flow.PeriodicReportingFlow;
import com.pruefstein.report.flow.WorkflowStartTrigger;
import com.pruefstein.report.repository.ReportRepository;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Internal callback invoked by {@link PeriodicReportingFlow} at the end of each
 * reporting cycle. Creates a {@code MISSING} report when a device is overdue
 * and always restarts the flow for the next cycle.
 */
@Path("/internal/reporting")
@Consumes(MediaType.APPLICATION_JSON)
public class PeriodicReportingResource
{
	@Inject
	DeviceRepository deviceRepository;

	@Inject
	ReportRepository reportRepository;

	@Inject
	PeriodicReportingFlow periodicReportingFlow;

	@Inject
	Event<WorkflowStartTrigger> workflowStartTrigger;

	public record CycleRequest(String deviceId, boolean reported)
	{
	}

	@POST
	@Path("/cycle")
	@Transactional
	public Response cycle(CycleRequest req)
	{
		Device device = deviceRepository.findByDeviceId(req.deviceId()).orElse(null);
		if (device == null)
		{
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		if (!req.reported())
		{
			Report missing = new Report();
			missing.setDeviceId(device.getDeviceId());
			missing.setUserId(device.getUserId());
			missing.setKeycloakUser(device.getKeycloakUser());
			missing.setCheckedAt(Instant.now());
			missing.setStatus(ReportStatus.MISSING);
			missing.setFinalizedAt(Instant.now());
			reportRepository.persist(missing);
		}

		// Reset the clock so the next hourly tick does not immediately re-fire
		device.setLastReportAt(Instant.now());

		var wi = periodicReportingFlow.instance(Map.of("deviceId", device.getDeviceId()));
		device.setPeriodicFlowInstanceId(wi.id());
		workflowStartTrigger.fire(new WorkflowStartTrigger(wi));

		return Response.ok().build();
	}
}
