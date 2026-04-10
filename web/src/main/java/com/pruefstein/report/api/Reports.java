package com.pruefstein.report.api;

import java.util.List;

import com.pruefstein.compliance.domain.ComplianceResult;
import com.pruefstein.compliance.repository.ComplianceResultRepository;
import com.pruefstein.report.domain.Report;
import com.pruefstein.report.domain.ReportStatus;
import com.pruefstein.report.repository.ReportRepository;
import com.pruefstein.user.web.CurrentUserBean;

import io.quarkiverse.renarde.Controller;
import io.quarkus.panache.common.Sort;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.QueryParam;
import org.jboss.resteasy.reactive.RestPath;

@SuppressWarnings("unused")
@RolesAllowed("**")
public class Reports extends Controller
{
	@Inject
	ReportRepository reportRepository;

	@Inject
	ComplianceResultRepository resultRepository;

	@Inject
	CurrentUserBean currentUser;

	@CheckedTemplate
	public static class Templates
	{
		public static native TemplateInstance index(
			List<Report> reports,
			String statusFilter,
			String q,
			String sort,
			String dir);

		public static native TemplateInstance show(Report report, List<ComplianceResult> results);
	}

	public TemplateInstance index(
		@QueryParam("status") String statusParam,
		@QueryParam("q") String q,
		@QueryParam("sort") String sort,
		@QueryParam("dir") String dir)
	{
		ReportStatus statusFilter = null;
		if (statusParam != null && !statusParam.isBlank())
		{
			try
			{
				statusFilter = ReportStatus.valueOf(statusParam);
			}
			catch (IllegalArgumentException ignored)
			{
			}
		}

		String activeStatus = statusFilter != null ? statusFilter.name() : "";
		String activeQ = q != null ? q : "";
		String activeSort = sort != null ? sort : "checkedAt";
		String activeDir = dir != null ? dir : "desc";

		String ownerFilter = currentUser.isAdmin() ? null : currentUser.getUsername();
		List<Report> reports = reportRepository.listFiltered(statusFilter, activeQ, activeSort, activeDir, ownerFilter);
		return Templates.index(reports, activeStatus, activeQ, activeSort, activeDir);
	}

	public TemplateInstance show(@RestPath Long id)
	{
		Report report = reportRepository.findById(id);
		if (report == null)
		{
			notFound();
			return null;
		}
		if (!currentUser.isAdmin())
		{
			String username = currentUser.getUsername();
			if (username == null || !username.equals(report.getKeycloakUser()))
			{
				throw new ForbiddenException();
			}
		}
		List<ComplianceResult> results = resultRepository.list("report", Sort.by("item.name").ascending(), report);
		return Templates.show(report, results);
	}
}
