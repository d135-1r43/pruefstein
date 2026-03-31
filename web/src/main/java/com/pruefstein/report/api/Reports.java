package com.pruefstein.report.api;

import java.util.List;

import com.pruefstein.compliance.domain.ComplianceResult;
import com.pruefstein.compliance.repository.ComplianceResultRepository;
import com.pruefstein.report.domain.Report;
import com.pruefstein.report.repository.ReportRepository;

import io.quarkiverse.renarde.Controller;
import io.quarkus.panache.common.Sort;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import org.jboss.resteasy.reactive.RestPath;

@SuppressWarnings("unused")
public class Reports extends Controller
{
	@Inject
	ReportRepository reportRepository;

	@Inject
	ComplianceResultRepository resultRepository;

	@CheckedTemplate
	public static class Templates
	{
		public static native TemplateInstance index(List<Report> reports);

		public static native TemplateInstance show(Report report, List<ComplianceResult> results);
	}

	public TemplateInstance index()
	{
		return Templates.index(reportRepository.listAll(Sort.by("checkedAt").descending()));
	}

	public TemplateInstance show(@RestPath Long id)
	{
		Report report = reportRepository.findById(id);
		if (report == null)
		{
			notFound();
			return null;
		}
		List<ComplianceResult> results = resultRepository.list("report", Sort.by("item.name").ascending(), report);
		return Templates.show(report, results);
	}
}
