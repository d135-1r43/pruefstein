package com.pruefstein.compliance.domain;

import com.pruefstein.report.domain.Report;
import io.quarkus.hibernate.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

@Entity
public class ComplianceResult extends PanacheEntity
{
	@OneToOne
	private ComplianceItem item;

	@ManyToOne
	private Report report;

	public ComplianceItem getItem()
	{
		return item;
	}

	public void setItem(ComplianceItem item)
	{
		this.item = item;
	}

	public Report getReport()
	{
		return report;
	}

	public void setReport(Report report)
	{
		this.report = report;
	}
}
