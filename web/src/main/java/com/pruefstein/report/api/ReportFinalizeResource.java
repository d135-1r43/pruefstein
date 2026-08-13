package com.pruefstein.report.api;

import java.time.Instant;

import com.pruefstein.notification.ReportMailTrigger;
import com.pruefstein.report.domain.Report;
import com.pruefstein.report.domain.ReportStatus;
import com.pruefstein.report.repository.ReportRepository;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Internal callback endpoint invoked by
 * {@link com.pruefstein.report.flow.ComplianceReportFlow}. Not exposed via
 * Kubernetes ingress; listed under {@code public} HTTP-auth paths so the flow
 * engine (same pod) can call it without a bearer token.
 */
@Path("/internal/reports")
@Consumes(MediaType.APPLICATION_JSON)
@PermitAll
public class ReportFinalizeResource
{
	@Inject
	ReportRepository reportRepository;

	@Inject
	Event<ReportMailTrigger> mailTrigger;

	public record FinalizeRequest(long reportId, boolean allPassed)
	{
	}

	@POST
	@Path("/finalize")
	@Transactional
	public Response finalize(FinalizeRequest req)
	{
		Report report = reportRepository.findById(req.reportId());
		if (report == null)
		{
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		report.setStatus(req.allPassed() ? ReportStatus.COMPLIANT : ReportStatus.NON_COMPLIANT);
		report.setFinalizedAt(Instant.now());

		// The verdict the user has been waiting for since their report went
		// OPEN — mailed after commit by ReportMailNotifier.
		mailTrigger.fire(new ReportMailTrigger(report.id));
		return Response.ok().build();
	}
}
