package com.pruefstein.shared.bootstrap;

import com.pruefstein.compliance.domain.ComplianceGroup;
import com.pruefstein.compliance.domain.ComplianceItem;
import com.pruefstein.compliance.repository.ComplianceGroupRepository;
import com.pruefstein.compliance.repository.ComplianceItemRepository;
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

		addItem(cryptography,
			"FileVault enabled",
			"SELECT filevault_status FROM disk_encryption WHERE filevault_status = 'on' LIMIT 1;",
			"results.size() > 0");

		ComplianceGroup operations = new ComplianceGroup();
		operations.setName("A.12 Operations Security");
		groupRepository.persist(operations);

		addItem(operations,
			"Firewall enabled",
			"SELECT global_state FROM alf;",
			"results[0].global_state == \"1\"");

		addItem(operations,
			"Automatic updates enabled",
			"SELECT value FROM preferences WHERE domain = 'com.apple.SoftwareUpdate' AND key = 'AutomaticCheckEnabled';",
			"results[0].value == \"1\"");

		ComplianceGroup access = new ComplianceGroup();
		access.setName("A.9 Access Control");
		groupRepository.persist(access);

		addItem(access,
			"Screen lock timeout ≤ 300 seconds",
			"SELECT value FROM preferences WHERE domain = 'com.apple.screensaver' AND key = 'idleTime';",
			"results.size() > 0 && Integer.parseInt(results[0].value) <= 300");
	}

	private void addItem(ComplianceGroup group, String name, String query, String expectedExpression)
	{
		ComplianceItem item = new ComplianceItem();
		item.setName(name);
		item.setQuery(query);
		item.setExpectedExpression(expectedExpression);
		item.setGroup(group);
		itemRepository.persist(item);
	}
}
