package com.pruefstein.report.api;

import com.pruefstein.device.domain.Device;
import com.pruefstein.device.repository.DeviceRepository;
import com.pruefstein.report.flow.PeriodicReportingFlow;
import com.pruefstein.report.service.PeriodicCycleService;
import jakarta.annotation.security.PermitAll;
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
@PermitAll
public class PeriodicReportingResource
{
	@Inject
	DeviceRepository deviceRepository;

	@Inject
	PeriodicCycleService cycleService;

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

		cycleService.completeCycle(device, req.reported());
		return Response.ok().build();
	}
}
