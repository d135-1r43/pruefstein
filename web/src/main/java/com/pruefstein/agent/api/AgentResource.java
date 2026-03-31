package com.pruefstein.agent.api;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import com.pruefstein.compliance.domain.ComplianceItem;
import com.pruefstein.compliance.domain.ComplianceResult;
import com.pruefstein.compliance.repository.ComplianceItemRepository;
import com.pruefstein.compliance.repository.ComplianceResultRepository;
import com.pruefstein.report.domain.Report;
import com.pruefstein.report.domain.ReportStatus;
import com.pruefstein.report.flow.ComplianceReportFlow;
import com.pruefstein.report.flow.FlowTrigger;
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
import jakarta.ws.rs.core.MediaType;

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
	ComplianceReportFlow complianceReportFlow;

	@Inject
	Event<FlowTrigger> flowTrigger;

	@Inject
	JsonWebToken jwt;

	@ConfigProperty(name = "pruefstein.compliance.remediation-days", defaultValue = "7")
	int remediationDays;

	public record CheckDto(Long id, String name, String query, String expectedExpression)
	{
	}

	public record ResultPayload(Long itemId, boolean passed, String output)
	{
	}

	public record ReportPayload(String deviceId, String userId, Instant checkedAt, List<ResultPayload> results)
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
	public void pushReport(ReportPayload payload)
	{
		boolean allPassed = payload.results().stream().allMatch(ResultPayload::passed);

		Optional<Report> existing = reportRepository.findOpenByDeviceAndUser(
			payload.deviceId(), payload.userId());

		if (existing.isPresent())
		{
			handleResubmission(existing.get(), payload, allPassed);
		}
		else
		{
			handleFirstSubmission(payload, allPassed);
		}
	}

	private void handleFirstSubmission(ReportPayload payload, boolean allPassed)
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

			// Create and start the flow instance — the workflow immediately
			// suspends at listen(), persisting its state via JPA.
			var wi = complianceReportFlow.instance(java.util.Map.of("reportId", report.id));
			report.setFlowInstanceId(wi.id());
			wi.start();
		}
	}

	private void handleResubmission(Report report, ReportPayload payload, boolean allPassed)
	{
		// Replace results with the new run
		resultRepository.delete("report", report);
		persistResults(report, payload.results());
		report.setCheckedAt(payload.checkedAt() != null ? payload.checkedAt() : Instant.now());

		// The flow is waiting for this event; FlowEventEmitter sends it after
		// commit
		flowTrigger.fire(new FlowTrigger(report.id, report.getFlowInstanceId(), allPassed));
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
}
