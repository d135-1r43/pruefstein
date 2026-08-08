package com.pruefstein.compliance.service;

import java.util.List;

/**
 * An AI-proposed blocking rule: every way the named application can appear on a
 * macOS device, so one rule catches it however it was installed.
 */
public record BlockedAppSuggestion(
	String label,
	String reason,
	List<String> bundleIds,
	List<String> appNames,
	List<String> homebrewNames)
{
	public List<String> bundleIdsOrEmpty()
	{
		return bundleIds == null ? List.of() : bundleIds;
	}

	public List<String> appNamesOrEmpty()
	{
		return appNames == null ? List.of() : appNames;
	}

	public List<String> homebrewNamesOrEmpty()
	{
		return homebrewNames == null ? List.of() : homebrewNames;
	}
}
