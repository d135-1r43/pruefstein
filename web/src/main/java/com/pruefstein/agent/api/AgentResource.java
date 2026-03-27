package com.pruefstein.agent.api;

import java.time.Instant;
import java.util.List;

import com.pruefstein.compliance.domain.ComplianceItem;
import com.pruefstein.compliance.domain.ComplianceResult;
import com.pruefstein.compliance.repository.ComplianceItemRepository;
import com.pruefstein.compliance.repository.ComplianceResultRepository;
import com.pruefstein.report.domain.Report;
import com.pruefstein.report.repository.ReportRepository;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

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
		Report report = new Report();
		report.setDeviceId(payload.deviceId());
		report.setUserId(payload.userId());
		report.setCheckedAt(payload.checkedAt() != null ? payload.checkedAt() : Instant.now());
		reportRepository.persist(report);

		for (ResultPayload rp : payload.results())
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
