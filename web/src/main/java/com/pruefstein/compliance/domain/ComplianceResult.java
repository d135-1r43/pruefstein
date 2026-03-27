package com.pruefstein.compliance.domain;

import com.pruefstein.report.domain.Report;
import io.quarkus.hibernate.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

@Entity
public class ComplianceResult extends PanacheEntity
{

	@ManyToOne(optional = false)
	private ComplianceItem item;

	@ManyToOne(optional = false)
	private Report report;

	private boolean passed;

	@Column(columnDefinition = "TEXT")
	private String output;

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

	public boolean isPassed()
	{
		return passed;
	}

	public void setPassed(boolean passed)
	{
		this.passed = passed;
	}

	public String getOutput()
	{
		return output;
	}

	public void setOutput(String output)
	{
		this.output = output;
	}
}
