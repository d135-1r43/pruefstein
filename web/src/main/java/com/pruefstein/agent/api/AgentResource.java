package com.pruefstein.agent.api;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import com.pruefstein.compliance.domain.ComplianceItem;
import com.pruefstein.compliance.domain.ComplianceResult;
import com.pruefstein.compliance.repository.ComplianceItemRepository;
import com.pruefstein.compliance.repository.ComplianceResultRepository;
import com.pruefstein.device.domain.Device;
import com.pruefstein.device.repository.DeviceRepository;
import com.pruefstein.report.domain.Report;
import com.pruefstein.report.domain.ReportStatus;
import com.pruefstein.report.flow.ComplianceReportFlow;
import com.pruefstein.report.flow.FlowTrigger;
import com.pruefstein.report.flow.PeriodicFlowTrigger;
import com.pruefstein.report.flow.PeriodicReportingFlow;
import com.pruefstein.report.flow.WorkflowStartTrigger;
import com.pruefstein.report.repository.ReportRepository;
import io.quarkus.oidc.Tenant;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Tenant("api")
@Authenticated
@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
public class AgentResource
{
	@Inject
	ComplianceItemRepository itemRepository;

	@Inject
	ComplianceResultRepository resultRepository;

	@Inject
	ReportRepository reportRepository;

	@Inject
	DeviceRepository deviceRepository;

	@Inject
	ComplianceReportFlow complianceReportFlow;

	@Inject
	PeriodicReportingFlow periodicReportingFlow;

	@Inject
	Event<FlowTrigger> flowTrigger;

	@Inject
	Event<PeriodicFlowTrigger> periodicFlowTrigger;

	@Inject
	Event<WorkflowStartTrigger> workflowStartTrigger;

	@Inject
	JsonWebToken jwt;

	@ConfigProperty(name = "pruefstein.compliance.remediation-days", defaultValue = "7")
	int remediationDays;

	@ConfigProperty(name = "pruefstein.compliance.reporting-interval-days", defaultValue = "7")
	int reportingIntervalDays;

	public record CheckDto(Long id, String name, String query, String expectedExpression)
	{
	}

	public record ResultPayload(Long itemId, boolean passed, String output)
	{
	}

	public record ReportPayload(String deviceId, String userId, Instant checkedAt, List<ResultPayload> results)
	{
	}

	public record ReportResponse(String reportUrl)
	{
	}

	@GET
	@Path("/checks")
	public List<CheckDto> getChecks()
	{
		return itemRepository.listAll().stream()
			.map(i -> new CheckDto(i.id, i.getName(), i.getQuery(), i.getExpectedExpression()))
			.toList();
	}

	@POST
	@Path("/reports")
	@Consumes(MediaType.APPLICATION_JSON)
	@Transactional
	public Response pushReport(ReportPayload payload, @Context UriInfo uriInfo)
	{
		boolean allPassed = payload.results().stream().allMatch(ResultPayload::passed);

		Optional<Report> existing = reportRepository.findOpenByDeviceAndUser(
			payload.deviceId(), payload.userId());

		Report report = existing.isPresent()
			? handleResubmission(existing.get(), payload, allPassed)
			: handleFirstSubmission(payload, allPassed);

		upsertDevice(payload);

		String reportUrl = uriInfo.getBaseUri().resolve("Reports/show/" + report.id).toString();
		return Response.ok(new ReportResponse(reportUrl)).build();
	}

	private Report handleFirstSubmission(ReportPayload payload, boolean allPassed)
	{
		Report report = new Report();
		report.setDeviceId(payload.deviceId());
		report.setUserId(payload.userId());
		report.setKeycloakUser(jwt.<String> claim("preferred_username").orElse(jwt.getSubject()));
		report.setCheckedAt(payload.checkedAt() != null ? payload.checkedAt() : Instant.now());
		reportRepository.persist(report);

		persistResults(report, payload.results());

		if (allPassed)
		{
			report.setStatus(ReportStatus.COMPLIANT);
			report.setFinalizedAt(Instant.now());
		}
		else
		{
			report.setStatus(ReportStatus.OPEN);
			report.setDeadline(Instant.now().plus(remediationDays, ChronoUnit.DAYS));

			// Create the flow instance and fire a CDI event to start it after
			// the current transaction commits. Starting inside @Transactional
			// causes ConcurrentModificationException because ManagedExecutor
			// propagates the JTA context into the async workflow JPA writer.
			var wi = complianceReportFlow.instance(java.util.Map.of("reportId", report.id));
			report.setFlowInstanceId(wi.id());
			workflowStartTrigger.fire(new WorkflowStartTrigger(wi));
		}
		return report;
	}

	private Report handleResubmission(Report report, ReportPayload payload, boolean allPassed)
	{
		// Replace results with the new run
		resultRepository.delete("report", report);
		persistResults(report, payload.results());
		report.setCheckedAt(payload.checkedAt() != null ? payload.checkedAt() : Instant.now());

		// The flow is waiting for this event; FlowEventEmitter sends it after
		// commit
		flowTrigger.fire(new FlowTrigger(report.id, report.getFlowInstanceId(), allPassed));
		return report;
	}

	private void persistResults(Report report, List<ResultPayload> results)
	{
		for (ResultPayload rp : results)
		{
			ComplianceItem item = itemRepository.findById(rp.itemId());
			if (item == null)
			{
				continue;
			}
			ComplianceResult result = new ComplianceResult();
			result.setItem(item);
			result.setReport(report);
			result.setPassed(rp.passed());
			result.setOutput(rp.output());
			resultRepository.persist(result);
		}
	}

	/**
	 * Creates or updates the {@link Device} row for the reporting device and
	 * manages its periodic reporting flow:
	 * <ul>
	 * <li>First report: persists a new Device and starts the first
	 * {@link PeriodicReportingFlow} instance.</li>
	 * <li>Subsequent reports: updates {@code lastReportAt} and fires a
	 * {@link PeriodicFlowTrigger} to notify the waiting flow that the device
	 * reported on time. The flow's internal callback will then restart the flow
	 * for the next cycle.</li>
	 * </ul>
	 */
	private void upsertDevice(ReportPayload payload)
	{
		String keycloakUser = jwt.<String> claim("preferred_username").orElse(jwt.getSubject());
		Device device = deviceRepository.findByDeviceId(payload.deviceId()).orElse(null);

		if (device == null)
		{
			// First time we've seen this device — create it and start the
			// periodic flow
			device = new Device();
			device.setDeviceId(payload.deviceId());
			device.setUserId(payload.userId());
			device.setKeycloakUser(keycloakUser);
			device.setLastReportAt(Instant.now());
			deviceRepository.persist(device);

			var wi = periodicReportingFlow.instance(java.util.Map.of("deviceId", payload.deviceId()));
			device.setPeriodicFlowInstanceId(wi.id());
			workflowStartTrigger.fire(new WorkflowStartTrigger(wi));
		}
		else
		{
			// Device already known — notify the waiting flow and let its
			// callback restart it
			device.setLastReportAt(Instant.now());
			device.setUserId(payload.userId());
			device.setKeycloakUser(keycloakUser);
			periodicFlowTrigger.fire(
				new PeriodicFlowTrigger(device.getDeviceId(), device.getPeriodicFlowInstanceId(), true));
		}
	}
}
