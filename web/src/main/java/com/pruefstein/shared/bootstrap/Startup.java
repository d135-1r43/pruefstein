package com.pruefstein.shared.bootstrap;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.pruefstein.compliance.domain.ComplianceGroup;
import com.pruefstein.compliance.domain.ComplianceItem;
import com.pruefstein.compliance.domain.ComplianceResult;
import com.pruefstein.compliance.repository.ComplianceGroupRepository;
import com.pruefstein.compliance.repository.ComplianceItemRepository;
import com.pruefstein.compliance.repository.ComplianceResultRepository;
import com.pruefstein.report.domain.Report;
import com.pruefstein.report.domain.ReportStatus;
import com.pruefstein.report.repository.ReportRepository;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class Startup
{
	@Inject
	ComplianceGroupRepository groupRepository;

	@Inject
	ComplianceItemRepository itemRepository;

	@Inject
	ComplianceResultRepository resultRepository;

	@Inject
	ReportRepository reportRepository;

	@Transactional
	public void start(@Observes StartupEvent evt)
	{
		if (LaunchMode.current() == LaunchMode.DEVELOPMENT)
		{
			seedCompliance();
		}
	}

	private void seedCompliance()
	{
		ComplianceGroup cryptography = new ComplianceGroup();
		cryptography.setName("A.10 Cryptography");
		groupRepository.persist(cryptography);

		ComplianceItem fileVault = addItem(cryptography,
			"FileVault enabled",
			"SELECT filevault_status FROM disk_encryption WHERE filevault_status = 'on' LIMIT 1;",
			"results.size() > 0");

		ComplianceGroup operations = new ComplianceGroup();
		operations.setName("A.12 Operations Security");
		groupRepository.persist(operations);

		ComplianceItem firewall = addItem(operations,
			"Firewall enabled",
			"SELECT global_state FROM alf;",
			"results.size() > 0 && results[0].global_state == '1'");

		ComplianceItem autoUpdates = addItem(operations,
			"Automatic updates enabled",
			"SELECT value FROM preferences WHERE domain = 'com.apple.SoftwareUpdate' AND key = 'AutomaticCheckEnabled';",
			"results.size() > 0 && results[0].value == '1'");

		ComplianceGroup access = new ComplianceGroup();
		access.setName("A.9 Access Control");
		groupRepository.persist(access);

		ComplianceItem screenLock = addItem(access,
			"Screen lock timeout ≤ 300 seconds",
			"SELECT value FROM preferences WHERE domain = 'com.apple.screensaver' AND key = 'idleTime';",
			"results.size() > 0 && results[0].value <= 300");

		seedReports(fileVault, firewall, autoUpdates, screenLock);
	}

	private void seedReports(ComplianceItem fileVault, ComplianceItem firewall,
		ComplianceItem autoUpdates, ComplianceItem screenLock)
	{
		// Report 1: fully compliant, finalized yesterday
		Report compliant = new Report();
		compliant.setDeviceId("MacBook-Pro-Alice.local");
		compliant.setUserId("alice");
		compliant.setKeycloakUser("alice");
		compliant.setCheckedAt(Instant.now().minus(1, ChronoUnit.DAYS));
		compliant.setStatus(ReportStatus.COMPLIANT);
		compliant.setFinalizedAt(Instant.now().minus(1, ChronoUnit.DAYS).plusSeconds(5));
		reportRepository.persist(compliant);

		addResult(compliant, fileVault, true, "[{\"filevault_status\":\"on\"}]");
		addResult(compliant, firewall, true, "[{\"global_state\":\"1\"}]");
		addResult(compliant, autoUpdates, true, "[{\"value\":\"1\"}]");
		addResult(compliant, screenLock, true, "[{\"value\":\"120\"}]");

		// Report 2: non-compliant with deadline, checked an hour ago
		Report nonCompliant = new Report();
		nonCompliant.setDeviceId("MacBook-Air-Bob.local");
		nonCompliant.setUserId("bob");
		nonCompliant.setKeycloakUser("bob");
		nonCompliant.setCheckedAt(Instant.now().minus(1, ChronoUnit.HOURS));
		nonCompliant.setStatus(ReportStatus.NON_COMPLIANT);
		nonCompliant.setDeadline(Instant.now().plus(6, ChronoUnit.DAYS));
		nonCompliant.setFinalizedAt(Instant.now().minus(1, ChronoUnit.HOURS).plusSeconds(5));
		reportRepository.persist(nonCompliant);

		addResult(nonCompliant, fileVault, false, "[]");
		addResult(nonCompliant, firewall, true, "[{\"global_state\":\"1\"}]");
		addResult(nonCompliant, autoUpdates, false, "[{\"value\":\"0\"}]");
		addResult(nonCompliant, screenLock, true, "[{\"value\":\"240\"}]");
	}

	private ComplianceItem addItem(ComplianceGroup group, String name, String query, String expectedExpression)
	{
		ComplianceItem item = new ComplianceItem();
		item.setName(name);
		item.setQuery(query);
		item.setExpectedExpression(expectedExpression);
		item.setGroup(group);
		itemRepository.persist(item);
		return item;
	}

	private void addResult(Report report, ComplianceItem item, boolean passed, String output)
	{
		ComplianceResult result = new ComplianceResult();
		result.setReport(report);
		result.setItem(item);
		result.setPassed(passed);
		result.setOutput(output);
		resultRepository.persist(result);
	}
}
