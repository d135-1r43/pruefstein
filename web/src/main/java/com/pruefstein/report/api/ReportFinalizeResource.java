package com.pruefstein.report.api;

import com.pruefstein.report.domain.Report;
import com.pruefstein.report.repository.ReportRepository;
import com.pruefstein.report.service.ReportFinalizer;
import jakarta.annotation.security.PermitAll;
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
	ReportFinalizer finalizer;

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
		// A no-op if the deadline job already decided this report, so a late
		// callback cannot overwrite the outcome or send a second mail.
		finalizer.finalizeReport(report, req.allPassed());
		return Response.ok().build();
	}
}
