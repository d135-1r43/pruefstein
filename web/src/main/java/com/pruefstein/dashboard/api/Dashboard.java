package com.pruefstein.dashboard.api;

import com.pruefstein.compliance.repository.ComplianceItemRepository;
import com.pruefstein.compliance.repository.ComplianceResultRepository;
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

	@Inject
	ComplianceResultRepository resultRepository;

	@CheckedTemplate
	public static class Templates
	{
		public static native TemplateInstance index(long userCount, long itemCount, long reportCount, long issueCount);
	}

	@Path("/")
	public TemplateInstance index()
	{
		return Templates.index(
			userRepository.count(),
			itemRepository.count(),
			reportRepository.count(),
			resultRepository.count("passed = ?1", Boolean.FALSE));
	}
}
