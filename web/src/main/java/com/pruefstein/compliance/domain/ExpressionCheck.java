package com.pruefstein.compliance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * A check whose osquery SQL and pass condition are written by an admin. This is
 * the ordinary kind — the query runs on the device and the JEXL expression is
 * evaluated against its rows.
 */
@Entity
@DiscriminatorValue("EXPRESSION")
public class ExpressionCheck extends ComplianceItem
{
	@Column(columnDefinition = "TEXT")
	private String query;

	@Column(columnDefinition = "TEXT")
	private String expectedExpression;

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
}
