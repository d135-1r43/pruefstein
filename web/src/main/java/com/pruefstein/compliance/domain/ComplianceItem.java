package com.pruefstein.compliance.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import java.util.List;
import jakarta.persistence.OneToMany;

@Entity
public class ComplianceItem extends PanacheEntity
{
	private String name;

	@Column(columnDefinition = "TEXT")
	private String query;

	@Column(columnDefinition = "TEXT")
	private String expectedExpression;

	@ManyToOne
	private ComplianceGroup group;

	@OneToMany(mappedBy = "item")
	private List<ComplianceResult> results;

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getQuery()
	{
		return query;
	}

	public void setQuery(String query)
	{
		this.query = query;
	}

	public String getExpectedExpression()
	{
		return expectedExpression;
	}

	public void setExpectedExpression(String expectedExpression)
	{
		this.expectedExpression = expectedExpression;
	}

	public ComplianceGroup getGroup()
	{
		return group;
	}

	public void setGroup(ComplianceGroup group)
	{
		this.group = group;
	}

	public List<ComplianceResult> getResults()
	{
		return results;
	}

	public void setResults(List<ComplianceResult> results)
	{
		this.results = results;
	}
}
