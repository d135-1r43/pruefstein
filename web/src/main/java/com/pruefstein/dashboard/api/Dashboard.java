package com.pruefstein.dashboard.api;

import com.pruefstein.compliance.repository.ComplianceItemRepository;
import com.pruefstein.report.domain.ReportStatus;
import com.pruefstein.report.repository.ReportRepository;
import com.pruefstein.user.repository.UserRepository;
import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

@SuppressWarnings("unused")
public class Dashboard extends Controller
{
	@Inject
	UserRepository userRepository;

	@Inject
	ComplianceItemRepository itemRepository;

	@Inject
	ReportRepository reportRepository;

	@CheckedTemplate
	public static class Templates
	{
		public static native TemplateInstance index(
			long compliantCount,
			long nonCompliantCount,
			long missingCount,
			long openCount,
			long totalCount,
			long compliantPct,
			long nonCompliantPct,
			long missingPct,
			long itemCount,
			long userCount);
	}

	@Path("/")
	public TemplateInstance index()
	{
		long total = reportRepository.count();
		long compliant = reportRepository.count("status", ReportStatus.COMPLIANT);
		long nonCompliant = reportRepository.count("status", ReportStatus.NON_COMPLIANT);
		long missing = reportRepository.count("status", ReportStatus.MISSING);
		long open = reportRepository.count("status", ReportStatus.OPEN);

		long compliantPct = total > 0 ? (compliant * 100) / total : 0;
		long nonCompliantPct = total > 0 ? (nonCompliant * 100) / total : 0;
		long missingPct = total > 0 ? (missing * 100) / total : 0;

		return Templates.index(compliant, nonCompliant, missing, open, total,
			compliantPct, nonCompliantPct, missingPct,
			itemRepository.count(), userRepository.count());
	}
}
