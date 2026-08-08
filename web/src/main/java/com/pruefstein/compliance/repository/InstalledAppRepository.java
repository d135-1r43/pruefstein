package com.pruefstein.compliance.repository;

import java.util.List;

import com.pruefstein.compliance.domain.InstalledApp;
import com.pruefstein.report.domain.Report;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class InstalledAppRepository implements PanacheRepository<InstalledApp>
{
	public List<InstalledApp> listForReport(Report report)
	{
		return list("report", Sort.by("source").and("name"), report);
	}
}
