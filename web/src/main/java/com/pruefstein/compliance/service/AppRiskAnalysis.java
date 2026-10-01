package com.pruefstein.compliance.service;

import java.util.List;

/**
 * The applications the AI considers risky out of one numbered batch. Only
 * flagged ones are listed; the numbers point back into the batch it was given.
 */
public record AppRiskAnalysis(List<Finding> risky)
{
	public record Finding(Integer number, String category, String reason)
	{
	}

	public List<Finding> riskyOrEmpty()
	{
		return risky == null ? List.of() : risky;
	}
}
